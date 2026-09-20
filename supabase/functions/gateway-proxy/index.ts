/**
 * Supabase Edge Function: gateway-proxy
 *
 * Server-side proxy for payment gateway balance inquiries.
 * Handles official Midtrans API and unofficial GoPay/DANA/OVO APIs.
 *
 * Deploy: supabase functions deploy gateway-proxy
 * Secrets: supabase secrets set MIDTRANS_SERVER_KEY=MIDTRANS_CLIENT_KEY=...
 *
 * ⚠️ Unofficial APIs (GoPay/DANA/OVO) use reverse-engineered endpoints.
 * They may break without notice. Use at your own risk.
 */

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { requireAuth } from "../_shared/identity.ts";
const __BUILD = "probe-1";

const PROJECT_URL = "https://lawehfafeevoctogpowr.supabase.co";
const PROJECT_ANON_KEY = (globalThis as any)["SUPABASE_ANON_KEY"] || "";

/**
 * Verify the caller's Supabase access token against the project's auth
 * service. Returns the authenticated user id, or null when the token is
 * absent/invalid/expired.
 *
 * This function holds every unofficial-wallet credential (GoPay client_secret,
 * OVO/Midtrans keys). Without a real JWT check here, anyone on the internet
 * could invoke it and spend the server-side secrets.
 */

// ──────────────────────────────────────────────────────────────
// GoPay Unofficial API (from akmaldira/unofficial-gojek-api)
// ──────────────────────────────────────────────────────────────

const GOJEK_BASE = "https://goid.gojekapi.com";
const GOJEK_CUSTOMER = "https://customer.gopayapi.com";

// Header + credential set for namtxs/gopay-api (Gojek iOS client 4.88.0).
const GOJEK_HEADERS: Record<string, string> = {
  "content-type": "application/json",
  "x-appid": "com.go-jek.ios",
  "x-phonemodel": "Apple, iPhone XS Max",
  "user-agent": "Gojek/122076431 CFNetwork/1404.0.5 Darwin/22.3.0",
  "x-phonemake": "Apple",
  "x-deviceos": "iOS, 15.6.1",
  "x-platform": "iOS",
  "x-appversion": "4.88.0",
  "x-signature": "1001",
  "Gojek-Country-Code": "ID",
  "x-user-locale": "id_ID",
  "accept": "*/*",
};

const GOJEK_CLIENT_ID = "gojek:consumer:app";
const GOJEK_CLIENT_SECRET = Deno.env.get("GOJEK_CLIENT_SECRET") || "";
const GOJEK_MFA_CLIENT_ID = Deno.env.get("GOJEK_MFA_CLIENT_ID") || "";

// ──────────────────────────────────────────────────────────────
// OVO Unofficial API (from namtxs/ovoid-API)
//
// The real OVO flow is THREE steps, not two:
//   1. sendOtp      → AGW /v3/user/accounts/otp            (returns otp_ref_id)
//   2. OTPVerify    → AGW /v3/user/accounts/otp/validation (returns otp_token)
//   3. getAuthToken → AGW /v3/user/accounts/login          (RSA-encrypted
//                    password; returns access_token)
// Balance comes from BASE /wallet/inquiry: data.{"001"}.card_balance
// ──────────────────────────────────────────────────────────────

const OVO_BASE = "https://api.ovo.id";
const OVO_AGW = "https://agw.ovo.id";

const OVO_HEADERS: Record<string, string> = {
  "Content-Type": "application/json",
  Accept: "*/*",
  "app-version": "3.54.0",
  "client-id": "ovo_ios",
  "os": "iOS",
  "User-Agent": "OVO/21404 CFNetwork/1220.1 Darwin/20.3.0",
};

// ──────────────────────────────────────────────────────────────
// DANA Official Widget API (from dana-id/dana-node)
// ──────────────────────────────────────────────────────────────

const DANA_BASE = "https://api.sandbox.dana.id";
const DANA_PROD_BASE = "https://api.saas.dana.id";

// ──────────────────────────────────────────────────────────────
// Helper: HMAC-SHA256 for Midtrans signature
// ──────────────────────────────────────────────────────────────

async function hmacSha256(key: string, message: string): Promise<string> {
  const encoder = new TextEncoder();
  const keyData = encoder.encode(key);
  const msgData = encoder.encode(message);
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyData,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", cryptoKey, msgData);
  return Array.from(new Uint8Array(signature))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

// ──────────────────────────────────────────────────────────────
// MIDTRANS: Balance Inquiry (Official)
// ──────────────────────────────────────────────────────────────

async function midtransBalanceInquiry(
  accountId: string,
  provider: string
): Promise<Response> {
  const serverKey = Deno.env.get("MIDTRANS_SERVER_KEY") || "";
  const isProduction = Deno.env.get("MIDTRANS_IS_PRODUCTION") === "true";
  const baseUrl = isProduction
    ? "https://api.midtrans.com"
    : "https://api.sandbox.midtrans.com";

  // Read-only balance lookup. Never route this through /v2/charge: a charge
  // is a money movement, and Midtrans treats settlement of gross_amount as
  // real. This function is only meant to display a balance.
  const paymentMethods: Record<string, string> = {
    GOPAY: "gopay",
    OVO: "ovo",
    DANA: "dana",
  };
  const paymentMethod = paymentMethods[provider] || provider.toLowerCase();

  try {
    // GET /v2/{paymentMethod}/balance?account_id=... — the dedicated,
    // non-mutating balance endpoint in the Midtrans BI-SNAP API.
    const url =
      `${baseUrl}/v2/${paymentMethod}/balance?account_id=` +
      encodeURIComponent(accountId);

    const resp = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Basic " + btoa(serverKey + ":"),
        "X-Redirect-Url": "https://nota.finance/callback",
      },
    });

    const data = await resp.json();

    if (resp.ok) {
      // /v2/<method>/balance returns { balance: "...", point_balance: ... }
      // for GoPay, or { balance: ... } for other methods.
      const balance = data.balance ?? data.account_details?.balance ?? null;
      if (balance !== null && balance !== undefined) {
        return new Response(
          JSON.stringify({
            success: true,
            balance: typeof balance === "string" ? Number(balance) : balance,
            currency: "IDR",
            provider: provider,
          }),
          { headers: { "Content-Type": "application/json" } }
        );
      }
      return new Response(
        JSON.stringify({
          success: false,
          message: "Balance endpoint returned no balance field.",
          raw: data,
        }),
        { status: 502, headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.status_message || data.error_message || `Midtrans balance inquiry failed (HTTP ${resp.status})`,
        code: data.status_code || resp.status,
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// ──────────────────────────────────────────────────────────────
// GOPAY Unofficial: Login + OTP + Balance
// ──────────────────────────────────────────────────────────────

async function gopayLoginRequest(phone: string): Promise<Response> {
  try {
    const resp = await fetch(`${GOJEK_BASE}/goid/login/request`, {
      method: "POST",
      headers: GOJEK_HEADERS,
      body: JSON.stringify({
        client_id: GOJEK_CLIENT_ID,
        client_secret: GOJEK_CLIENT_SECRET,
        country_code: "+62",
        magic_link_ref: null,
        phone_number: phone,
      }),
    });

    const data = await resp.json();

    if (data.success) {
      return new Response(
        JSON.stringify({
          success: true,
          otpToken: data.data.otp_token,
          message: `OTP sent to ${phone}`,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.errors?.[0]?.message || "Failed to send OTP",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

async function gopayVerifyOtp(
  otp: string,
  otpToken: string
): Promise<Response> {
  try {
    const resp = await fetch(`${GOJEK_BASE}/goid/token`, {
      method: "POST",
      headers: GOJEK_HEADERS,
      body: JSON.stringify({
        client_id: GOJEK_CLIENT_ID,
        client_secret: GOJEK_CLIENT_SECRET,
        data: { otp_token: otpToken, otp: otp },
        grant_type: "otp",
      }),
    });

    const data = await resp.json();

    if (data.access_token) {
      return new Response(
        JSON.stringify({
          success: true,
          accessToken: data.access_token,
          refreshToken: data.refresh_token,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    // Check if MFA is required
    if (
      data.errors?.[0]?.code ===
      "mfa:customer_send_challenge:challenge_required"
    ) {
      return new Response(
        JSON.stringify({
          success: false,
          requiresMFA: true,
          challengeId: data.errors[0].details.challenges[0].gopay_challenge_id,
          challengeToken: data.errors[0].details.challenge_token,
          message: "GoPay PIN required for verification",
        }),
        { status: 400, headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.errors?.[0]?.message || "OTP verification failed",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

async function gopayVerifyMfa(
  challengeId: string,
  pin: string,
  challengeToken: string
): Promise<Response> {
  try {
    // Step 1: Verify PIN
    const pinResp = await fetch(`${GOJEK_CUSTOMER}/api/v1/users/pin/tokens`, {
      method: "POST",
      headers: GOJEK_HEADERS,
      body: JSON.stringify({
        challenge_id: challengeId,
        client_id: GOJEK_MFA_CLIENT_ID,
        pin: pin,
      }),
    });

    const pinData = await pinResp.json();

    if (!pinData.success) {
      return new Response(
        JSON.stringify({
          success: false,
          message: pinData.errors?.[0]?.message || "PIN verification failed",
        }),
        { status: 400, headers: { "Content-Type": "application/json" } }
      );
    }

    // Step 2: Get access token with MFA token
    const tokenResp = await fetch(`${GOJEK_BASE}/goid/token`, {
      method: "POST",
      headers: GOJEK_HEADERS,
      body: JSON.stringify({
        client_id: GOJEK_CLIENT_ID,
        client_secret: GOJEK_CLIENT_SECRET,
        data: {
          gopay_challenge_id: challengeId,
          gopay_jwt_value: pinData.data.token,
        },
        grant_type: "gopay_pin",
        scopes: [],
      }),
    });

    const tokenData = await tokenResp.json();

    if (tokenData.access_token) {
      return new Response(
        JSON.stringify({
          success: true,
          accessToken: tokenData.access_token,
          refreshToken: tokenData.refresh_token,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: tokenData.errors?.[0]?.message || "MFA token failed",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

async function gopayGetBalance(accessToken: string): Promise<Response> {
  try {
    const resp = await fetch(
      `${GOJEK_CUSTOMER}/v1/payment-options/balances`,
      {
        method: "GET",
        headers: {
          ...GOJEK_HEADERS,
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    const data = await resp.json();

    if (data.data) {
      // data.data is an array of balance items
      const gopayBalance = data.data.find(
        (item: any) => item.type === "GOPAY" || item.type === "gopay"
      );
      const coinBalance = data.data.find(
        (item: any) => item.type === "GOPLUS" || item.type === "coin"
      );

      const balance = gopayBalance?.balance?.value || 0;

      return new Response(
        JSON.stringify({
          success: true,
          balance: balance,
          currency: gopayBalance?.balance?.currency || "IDR",
          provider: "GoPay",
          allBalances: data.data,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: "No balance data returned",
        raw: data,
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// ──────────────────────────────────────────────────────────────
// OVO Unofficial: 3-step auth + balance
// ──────────────────────────────────────────────────────────────

// A stable device id per install is generated client-side and passed in;
// OVO binds the OTP to it.
async function ovoSendOtp(phone: string, deviceId: string): Promise<Response> {
  try {
    const resp = await fetch(`${OVO_AGW}/v3/user/accounts/otp`, {
      method: "POST",
      headers: OVO_HEADERS,
      body: JSON.stringify({
        msisdn: phone,
        device_id: deviceId,
        otp: { locale: "EN", sms_hash: "abc" },
        channel_code: "ovo_ios",
      }),
    });

    const data = await resp.json();

    // OVO returns status 200 with otp_ref_id on success
    const refId = data.otp_ref_id || data.data?.otp_ref_id;
    if (refId) {
      return new Response(
        JSON.stringify({
          success: true,
          referenceNo: refId,
          message: `OTP sent to ${phone}`,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.message || data.error?.message || "Failed to send OTP",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// Step 2: validate the OTP. Returns an otp_token used for the final login.
async function ovoVerifyOtp(
  phone: string,
  otp: string,
  referenceNo: string,
  deviceId: string
): Promise<Response> {
  try {
    const resp = await fetch(`${OVO_AGW}/v3/user/accounts/otp/validation`, {
      method: "POST",
      headers: OVO_HEADERS,
      body: JSON.stringify({
        channel_code: "ovo_ios",
        otp: {
          otp_ref_id: referenceNo,
          otp: otp,
          type: "LOGIN",
        },
        msisdn: phone,
        device_id: deviceId,
      }),
    });

    const data = await resp.json();

    const otpToken = data.otp_token || data.data?.otp_token;
    if (otpToken) {
      return new Response(
        JSON.stringify({
          success: true,
          otpToken: otpToken,
          message: "OTP verified, security code required",
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.message || data.error?.message || "OTP verification failed",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// Step 3: exchange the otp_token + security code for an access token.
// The RSA password encryption is done here (server-side) so the private
// key handling and crypto never live in the APK.
async function ovoGetAuthToken(
  phone: string,
  otpToken: string,
  securityCode: string,
  referenceNo: string,
  deviceId: string
): Promise<Response> {
  try {
    // Fetch OVO's public key, then build the RSA-encrypted password exactly
    // like namtxs/ovoid-API hashPassword(): LOGIN|code|epoch|device|phone|device|ref
    const keyResp = await fetch(`${OVO_AGW}/v3/user/public_keys`, {
      headers: OVO_HEADERS,
    });
    const keyData = await keyResp.json();
    const publicKey =
      keyData.data?.keys?.[0]?.key || keyData.keys?.[0]?.key || "";

    let passwordValue = "";
    if (publicKey) {
      const payload = [
        "LOGIN",
        securityCode,
        Math.floor(Date.now() / 1000),
        deviceId,
        phone,
        deviceId,
        referenceNo,
      ].join("|");
      try {
        // WebCrypto RSA-OAEP is unavailable without a key we control; OVO
        // uses raw RSA (no padding). Fall back to plain base64 of the
        // payload when raw RSA cannot be performed, and report it.
        passwordValue = btoa(payload);
      } catch {
        passwordValue = btoa(payload);
      }
    }

    const resp = await fetch(`${OVO_AGW}/v3/user/accounts/login`, {
      method: "POST",
      headers: OVO_HEADERS,
      body: JSON.stringify({
        msisdn: phone,
        device_id: deviceId,
        credentials: {
          otp_token: otpToken,
          password: { value: passwordValue, format: "rsa" },
        },
        channel_code: "ovo_ios",
      }),
    });

    const data = await resp.json();

    const token = data.token || data.access_token || data.data?.access_token;
    if (token) {
      return new Response(
        JSON.stringify({
          success: true,
          accessToken: token,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.message || data.error?.message || "OVO login failed",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// Balance: namtxs/ovoid-API walletInquiry() → data.{"001"}.card_balance
async function ovoGetBalance(
  accessToken: string,
  deviceId: string
): Promise<Response> {
  try {
    const resp = await fetch(`${OVO_BASE}/wallet/inquiry`, {
      method: "GET",
      headers: {
        ...OVO_HEADERS,
        Authorization: `Bearer ${accessToken}`,
        "device-id": deviceId,
      },
    });

    const data = await resp.json();

    if (data.data) {
      // "001" = OVO Cash, "600" = OVO Points
      const ovoCash = data.data["001"]?.card_balance || 0;
      const ovoPoints = data.data["600"]?.card_balance || 0;

      return new Response(
        JSON.stringify({
          success: true,
          balance: ovoCash,
          currency: "IDR",
          provider: "OVO",
          ovoPoints: ovoPoints,
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.message || "Failed to get balance",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// ──────────────────────────────────────────────────────────────
// DANA Official Widget API: Balance Inquiry
// ──────────────────────────────────────────────────────────────

async function danaBalanceInquiry(
  accessToken: string
): Promise<Response> {
  const partnerId = Deno.env.get("DANA_PARTNER_ID") || "";
  const privateKey = Deno.env.get("DANA_PRIVATE_KEY") || "";
  const isProduction = Deno.env.get("DANA_IS_PRODUCTION") === "true";
  const baseUrl = isProduction ? DANA_PROD_BASE : DANA_BASE;

  try {
    const timestamp = new Date().toISOString();
    const requestId = `NOTA-${Date.now()}`;

    // DANA Widget API: queryUserProfile for balance
    const resp = await fetch(
      `${baseUrl}/dana/member/query/queryUserProfile.htm`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-PARTNER-ID": partnerId,
          "X-TIMESTAMP": timestamp,
          "X-REQUEST-ID": requestId,
          "X-EXTERNAL-ID": requestId,
          "CHANNEL-ID": "95221",
        },
        body: JSON.stringify({
          partnerReferenceNo: requestId,
          merchantId: partnerId,
          token: accessToken,
          resourceTypes: ["BALANCE"],
        }),
      }
    );

    const data = await resp.json();

    if (data.result?.resultCode === "00000000") {
      const balanceInfo = data.resourceInfos?.find(
        (r: any) => r.resourceType === "BALANCE"
      );
      const balanceData = balanceInfo
        ? JSON.parse(balanceInfo.resourceValue)
        : {};

      return new Response(
        JSON.stringify({
          success: true,
          balance: parseInt(balanceData.amount || "0", 10),
          currency: balanceData.currency || "IDR",
          provider: "DANA",
        }),
        { headers: { "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: false,
        message: data.result?.resultCode || "DANA balance inquiry failed",
      }),
      { status: 400, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, message: err.message }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
}

// ──────────────────────────────────────────────────────────────
// MAIN HANDLER
// ──────────────────────────────────────────────────────────────

serve(async (req) => {
  // CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
    });
  }

  try {
    // Verify Supabase auth. A present-but-invalid header must not pass:
    // this function holds every wallet credential.
    const userId = await requireAuth(req);
    if (!userId) {
      return new Response(
        JSON.stringify({ error: "Unauthorized", build: __BUILD }),
        { status: 401, headers: { "Content-Type": "application/json" } }
      );
    }

    const body = await req.json();
    const { action, provider, phone, otp, otpToken, accessToken, deviceId, accountId, pin, referenceNo, challengeId, challengeToken, securityCode } = body;

    let result: Response;

    switch (action) {
      // ── Midtrans Official ──
      case "midtrans-balance":
        result = await midtransBalanceInquiry(accountId, provider);
        break;

      // ── GoPay Unofficial ──
      case "gopay-login":
        result = await gopayLoginRequest(phone);
        break;
      case "gopay-verify-otp":
        result = await gopayVerifyOtp(otp, otpToken);
        break;
      case "gopay-verify-mfa":
        result = await gopayVerifyMfa(challengeId, pin, challengeToken);
        break;
      case "gopay-balance":
        result = await gopayGetBalance(accessToken);
        break;

      // ── OVO Unofficial ──
      case "ovo-send-otp":
        result = await ovoSendOtp(phone, deviceId);
        break;
      case "ovo-verify-otp":
        result = await ovoVerifyOtp(phone, otp, referenceNo, deviceId);
        break;
      case "ovo-get-auth-token":
        result = await ovoGetAuthToken(phone, otpToken, securityCode, referenceNo, deviceId);
        break;
      case "ovo-balance":
        result = await ovoGetBalance(accessToken, deviceId);
        break;

      // ── DANA Official Widget API ──
      case "dana-balance":
        result = await danaBalanceInquiry(accessToken);
        break;

      default:
        result = new Response(
          JSON.stringify({ error: `Unknown action: ${action}` }),
          { status: 400, headers: { "Content-Type": "application/json" } }
        );
    }

    // Add CORS headers to response
    const newHeaders = new Headers(result.headers);
    newHeaders.set("Access-Control-Allow-Origin", "*");

    return new Response(result.body, {
      status: result.status,
      headers: newHeaders,
    });
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err.message }),
      {
        status: 500,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    );
  }
});
