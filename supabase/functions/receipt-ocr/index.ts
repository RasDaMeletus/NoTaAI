
import { serve } from 'https://deno.land/std@0.177.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.4.1'

// Load environment variables from .env file
import 'https://deno.land/x/dotenv@v3.2.0/load.ts'

const RESEMBLE_AI_API_KEY = Deno.env.get('RESEMBLE_AI_API_KEY')
const RESEMBLE_AI_PROJECT_ID = Deno.env.get('RESEMBLE_AI_PROJECT_ID')

// Free OpenRouter models only, per project requirement. Ling Flash VL is the
// vision-capable model in the free chain, so it is used for receipt OCR.
const OPENROUTER_API_KEY = Deno.env.get('OPENROUTER_API_KEY')
const OPENROUTER_MODEL = 'inclusionai/ling-3.0-flash-vl:free'

serve(async (req) => {
    try {
        // Create a Supabase client with the Auth context of the user making the request
        const supabaseClient = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_ANON_KEY') ?? '',
            {
                global: {
                    headers: { Authorization: req.headers.get('Authorization')! },
                },
            }
        )
        const { data: { user } } = await supabaseClient.auth.getUser()

        if (!user) {
            return new Response(
                JSON.stringify({ error: 'Unauthorized: No user session found.' }),
                { headers: { 'Content-Type': 'application/json' }, status: 401 }
            )
        }

        const { image_base64 } = await req.json()
        if (!image_base64) {
            return new Response(
                JSON.stringify({ error: 'Missing image_base64 in request body.' }),
                { headers: { 'Content-Type': 'application/json' }, status: 400 }
            )
        }

        if (!OPENROUTER_API_KEY) {
      return new Response(
        JSON.stringify({ error: 'Server Configuration Error: OPENROUTER_API_KEY not set.' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
      )
    }

    // Real OpenRouter Vision Call for Receipt Analysis
    const openRouterResponse = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${OPENROUTER_API_KEY}`,
        'Content-Type': 'application/json',
        'HTTP-Referer': 'https://vinote.app',
        'X-Title': 'ViNote-2 Receipt Scanner',
      },
      body: JSON.stringify({
        model: OPENROUTER_MODEL,
        messages: [
          {
            role: 'system',
            content: 'You are an expert OCR receipt parser for Indonesian financial receipts. Return ONLY a valid JSON object matching the requested schema. No markdown formatting, no explanatory text.'
          },
          {
            role: 'user',
            content: [
              {
                type: 'text',
                text: 'Parse this receipt image and extract details into JSON with keys: merchant (string), date (YYYY-MM-DD string), items (array of {name: string, qty: number, price: number}), subtotal (number), taxOrFee (number), totalAmount (number), category (Food/Groceries/Shopping/Bills/Transport/General), walletOrPayment (string), rawLines (array of strings).'
              },
              {
                type: 'image_url',
                image_url: {
                  url: image_base64.startsWith('data:') ? image_base64 : `data:image/jpeg;base64,${image_base64}`
                }
              }
            ]
          }
        ],
        temperature: 0.1
      })
    })

    if (!openRouterResponse.ok) {
      const errText = await openRouterResponse.text()
      console.error('OpenRouter Vision Error:', errText)
      return new Response(
        JSON.stringify({ error: `Vision OCR processing failed: ${openRouterResponse.statusText}` }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 502 }
      )
    }

    const aiResult = await openRouterResponse.json()
    const contentText = aiResult.choices?.[0]?.message?.content?.trim() || '{}'

    // Clean JSON response
    const jsonMatch = contentText.match(/\{[\s\S]*\}/)
    const cleanedJson = jsonMatch ? jsonMatch[0] : contentText
    const parsedData = JSON.parse(cleanedJson)

    return new Response(
      JSON.stringify(parsedData),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )

    } catch (error) {
        console.error('OCR Edge Function error:', error.message)
        return new Response(
            JSON.stringify({ error: error.message }),
            { headers: { 'Content-Type': 'application/json' }, status: 500 }
        )
    }
})
