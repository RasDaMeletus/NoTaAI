import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const openRouterApiKey = Deno.env.get("OPENROUTER_API_KEY")!;

serve(async (req) => {
  try {
    if (!openRouterApiKey) {
      return new Response(JSON.stringify({ error: "OPENROUTER_API_KEY not configured on server" }), { status: 500 });
    }

    const { messages, model = "google/gemini-2.5-flash", temperature = 0.3, response_format } = await req.json();

    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openRouterApiKey}`,
        "HTTP-Referer": "https://vinote.app",
        "X-Title": "NoTa",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ model, messages, temperature, response_format })
    });

    if (!response.ok) {
      return new Response(JSON.stringify({ error: `OpenRouter HTTP ${response.status}` }), { status: response.status });
    }

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content || "";
    return new Response(JSON.stringify({ content }), { headers: { "Content-Type": "application/json" } });
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500 });
  }
});