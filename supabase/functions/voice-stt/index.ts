import { serve } from "https://deno.land/std@0.177.0/http/server.ts"
import { requireAuth } from "../_shared/identity.ts"
import { decode } from "https://deno.land/std@0.177.0/encoding/base64.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { audioBase64, languageCode } = await req.json()
    if (!audioBase64) {
      throw new Error("Missing audioBase64 in request body.")
    }

    // Verify the caller's access token. The publishable key alone is not an
    // identity: it ships inside the APK.
    const callerId = await requireAuth(req)
    if (callerId == null) {
      return new Response(JSON.stringify({ error: "Unauthorized: valid access token required" }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const OPENROUTER_API_KEY = Deno.env.get('OPENROUTER_API_KEY')
    if (!OPENROUTER_API_KEY) {
      throw new Error("Server Configuration Error: OPENROUTER_API_KEY not set.")
    }

    // No free transcription model is available on OpenRouter; the app uses
    // on-device Android SpeechRecognizer for STT. This endpoint is kept only
    // as a future hook and reports the limitation honestly instead of
    // charging a paid whisper call.
    return new Response(
      JSON.stringify({
        transcript: "",
        error: "Cloud STT is not available on the free tier. Using on-device speech recognition instead.",
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
    )
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})