import { serve } from "https://deno.land/std@0.177.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
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

    // Create a Supabase client using the ANON_KEY passed in the Authorization header
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') // Or handle auth properly
    
    const authHeader = req.headers.get('Authorization')!
    const supabaseAnonKey = req.headers.get('apikey')!

    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })

    // Validate the user is authenticated
    const { data: { user }, error: authError } = await supabase.auth.getUser()
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
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