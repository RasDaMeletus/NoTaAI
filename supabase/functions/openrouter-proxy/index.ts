import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { requireAuth } from "../_shared/identity.ts";

const PROJECT_URL = "https://lawehfafeevoctogpowr.supabase.co";
const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const openRouterApiKey = Deno.env.get("OPENROUTER_API_KEY")!;
const PROJECT_ANON_KEY = (globalThis as any)["SUPABASE_ANON_KEY"] || "";

// Free OpenRouter models only, per project requirement. The app passes the
// model it wants; this default is only used when the request omits one.
const FREE_MODELS = [
  "inclusionai/ling-3.0-flash-fin:free",
  "nex-agi/nex-n2.5-mini:free",
  "inclusionai/ling-3.0-flash-vl:free",
  "liquid/lfm-2.5-2.6b:free",
];
const DEFAULT_FREE_MODEL = FREE_MODELS[0];

serve(async (req) => {
  try {
    if (!openRouterApiKey) {
      return new Response(JSON.stringify({ error: "OPENROUTER_API_KEY not configured on server" }), { status: 500 });
    }

    const callerId = await requireAuth(req);
    if (!callerId) {
      return new Response(JSON.stringify({ error: "Unauthorized: valid access token required" }), { status: 401 });
    }

    const { messages, model = DEFAULT_FREE_MODEL, temperature = 0.3, response_format } = await req.json();

    // Try the requested model, then walk the free chain on rate limits.
    const candidates = [model, ...FREE_MODELS.filter((m) => m !== model)];
    let lastError = "";

    for (const currentModel of candidates) {
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${openRouterApiKey}`,
          "HTTP-Referer": "https://vinote.app",
          "X-Title": "NoTa",
          "Content-Type": "application/json"
        },
        // Free reasoning models need headroom to finish thinking before they
        // emit the answer content.
        body: JSON.stringify({ model: currentModel, messages, temperature, response_format, max_tokens: 800 })
      });

      if (!response.ok) {
        lastError = `OpenRouter HTTP ${response.status}`;
        // Only retry on transient/rate-limit errors; bail on anything else.
        if (response.status !== 402 && response.status !== 429 && response.status !== 503) {
          return new Response(JSON.stringify({ error: lastError }), { status: response.status });
        }
        continue;
      }

      const data = await response.json();
      const content = data?.choices?.[0]?.message?.content ?? "";
      if (!content) {
        lastError = `Empty content from ${currentModel}`;
        continue;
      }
      return new Response(JSON.stringify({ content }), {
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response(JSON.stringify({ error: lastError || "No free model available" }), { status: 503 });
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500 });
  }
});