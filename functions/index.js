const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");

const OPENROUTER_API_KEY = defineSecret("OPENROUTER_API_KEY");
const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const DEFAULT_MODEL = "google/gemini-2.5-flash";

exports.openRouterChat = onCall(
  {
    region: "asia-southeast2",
    secrets: [OPENROUTER_API_KEY],
    enforceAppCheck: false,
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const data = request.data || {};
    const messages = Array.isArray(data.messages) ? data.messages : [];
    if (messages.length === 0 || messages.length > 20) {
      throw new HttpsError("invalid-argument", "messages must contain 1-20 items.");
    }

    const sanitizedMessages = messages.map((message) => {
      if (!message || typeof message.role !== "string" || typeof message.content !== "string") {
        throw new HttpsError("invalid-argument", "Each message needs role and content.");
      }
      if (!["system", "user", "assistant"].includes(message.role)) {
        throw new HttpsError("invalid-argument", "Unsupported message role.");
      }
      return {
        role: message.role,
        content: message.content.slice(0, 12000),
      };
    });

    const model = typeof data.model === "string" && data.model.trim()
      ? data.model.trim().slice(0, 200)
      : DEFAULT_MODEL;
    const temperature = typeof data.temperature === "number"
      ? Math.max(0, Math.min(1, data.temperature))
      : 0.3;

    const body = {
      model,
      temperature,
      messages: sanitizedMessages,
    };

    if (data.responseFormatJson === true) {
      body.response_format = { type: "json_object" };
    }

    const response = await fetch(OPENROUTER_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${OPENROUTER_API_KEY.value()}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://vinote.app",
        "X-Title": "ViNote Financial Companion",
      },
      body: JSON.stringify(body),
    });

    const responseText = await response.text();
    if (!response.ok) {
      console.error("OpenRouter request failed", response.status, responseText.slice(0, 1000));
      throw new HttpsError("internal", `OpenRouter request failed (${response.status}).`);
    }

    let payload;
    try {
      payload = JSON.parse(responseText);
    } catch (_) {
      throw new HttpsError("internal", "OpenRouter returned invalid JSON.");
    }

    const content = payload?.choices?.[0]?.message?.content;
    if (typeof content !== "string" || !content.trim()) {
      throw new HttpsError("internal", "OpenRouter returned no message content.");
    }

    return { content };
  }
);
