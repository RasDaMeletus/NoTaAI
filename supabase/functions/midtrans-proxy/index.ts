// Supabase Edge Function: midtrans-proxy
// Handles server-side API calls to Midtrans & unofficial gateways for balance inquiry
// Deploy to Supabase via: supabase functions deploy midtrans-proxy

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { requireAuth } from "../_shared/identity.ts"

const MIDTRANS_SERVER_KEY = Deno.env.get("MIDTRANS_SERVER_KEY") || ""
const MIDTRANS_IS_PRODUCTION = Deno.env.get("MIDTRANS_IS_PRODUCTION") === "true"

const MIDTRANS_BASE_URL = MIDTRANS_IS_PRODUCTION
  ? "https://api.midtrans.com"
  : "https://api.sandbox.midtrans.com"

serve(async (req) => {
  // CORS headers
  const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  }

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const callerId = await requireAuth(req)
    if (!callerId) {
      return new Response(JSON.stringify({ error: "Unauthorized: valid access token required" }), {
        status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
      })
    }

    const payload = await req.json()
    const { action, provider, accountId, accessToken, phoneNumber } = payload

    console.log(`[midtrans-proxy] Action: ${action}, Provider: ${provider}`)

    if (action === "balance-inquiry") {
      // 1. Official Midtrans GoPay Tokenization Balance Inquiry
      if (provider === "GOPAY" && accountId) {
        const authHeader = "Basic " + btoa(`${MIDTRANS_SERVER_KEY}:`)
        const response = await fetch(`${MIDTRANS_BASE_URL}/v2/pay/account/${accountId}`, {
          method: "GET",
          headers: {
            "Accept": "application/json",
            "Authorization": authHeader,
          },
        })

        const data = await response.json()
        console.log(`[Midtrans GoPay Response]`, JSON.stringify(data))

        if (data.status_code === "200" && data.metadata?.payment_options) {
          const gopayWallet = data.metadata.payment_options.find(
            (opt: any) => opt.name === "GOPAY_WALLET"
          )
          const balance = gopayWallet?.balance?.value ? parseFloat(gopayWallet.balance.value) : -1

          return new Response(
            JSON.stringify({
              status: "SUCCESS",
              balance: balance >= 0 ? Math.round(balance) : -1,
              currency: "IDR",
              provider: "GoPay",
            }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" } }
          )
        }

        return new Response(
          JSON.stringify({
            status: data.status_code || "FAILED",
            message: data.status_message || "Balance inquiry failed",
            balance: -1,
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        )
      }

      // Fallback response for unconfigured gateway credentials
      return new Response(
        JSON.stringify({
          status: "NOT_CONFIGURED",
          message: "Midtrans Server Key is not configured. Set MIDTRANS_SERVER_KEY secret in Supabase.",
          balance: -1,
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    if (action === "link-account") {
      // Initiate Midtrans Pay Account Linking for GoPay
      if (provider === "GOPAY" && phoneNumber) {
        const authHeader = "Basic " + btoa(`${MIDTRANS_SERVER_KEY}:`)
        const body = {
          payment_type: "gopay",
          gopay_partner: {
            phone_number: phoneNumber,
            country_code: "62",
            redirect_url: "https://nota.finance/callback",
          },
        }

        const response = await fetch(`${MIDTRANS_BASE_URL}/v2/pay/account`, {
          method: "POST",
          headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": authHeader,
          },
          body: JSON.stringify(body),
        })

        const data = await response.json()
        const activationUrl = data.actions?.find(
          (act: any) => act.name === "activation-link-url"
        )?.url

        return new Response(
          JSON.stringify({
            status: data.status_code || "FAILED",
            accountId: data.account_id || "",
            linkingUrl: activationUrl || "",
            message: data.channel_response_message || "Linking initiated",
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        )
      }
    }

    return new Response(
      JSON.stringify({ error: `Unsupported action: ${action}` }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  } catch (error) {
    console.error(`[midtrans-proxy] Error:`, error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
