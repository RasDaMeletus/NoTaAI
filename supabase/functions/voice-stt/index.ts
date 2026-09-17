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

    // Convert base64 audio to blob/form-data for Whisper STT
    const binaryAudio = decode(audioBase64)
    const formData = new FormData()
    const audioFile = new File([binaryAudio], "voice.wav", { type: "audio/wav" })
    formData.append("file", audioFile)
    formData.append("model", "openai/whisper-large-v3")
    formData.append("language", languageCode || "id")

    const sttResponse = await fetch("https://openrouter.ai/api/v1/audio/transcriptions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${OPENROUTER_API_KEY}`,
      },
      body: formData,
    })

    if (!sttResponse.ok) {
      const errText = await sttResponse.text()
      console.error("OpenRouter STT Error:", errText)
      throw new Error(`Cloud STT service error: ${sttResponse.statusText}`)
    }

    const sttResult = await sttResponse.json()
    const transcript = sttResult.text || "" 

    return new Response(JSON.stringify({ transcript }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})