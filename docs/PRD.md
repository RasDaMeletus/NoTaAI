# NoTa — Product Requirements Document

**Status:** Production release (v1.0.1)
**Repository:** `RasDaMeletus/NoTaAI`
**Target:** Android application + Supabase backend
**Application ID:** `com.nota.finance`

NoTa is an AI-powered Indonesian personal-finance companion for students and
young professionals. It captures transactions by receipt scan, voice, chat, or
manual entry, then grounds its advice in the user's real ledger.

---

## 1. Architecture

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Auth | Supabase Auth (GoTrue) — the only production login |
| Local DB | Room (offline-first, guest mode persists across relaunch) |
| Sync | Room ↔ Supabase Postgres, RLS per `user_id` |
| AI | OpenRouter, free models only, called via Edge Functions |
| Cloud functions | Supabase Edge Functions (Deno) |
| Wallet linking | Unofficial DANA / GoPay / OVO adapters, proxied server-side |
| Payments | Midtrans (official gateway) |

### Security model

Only the Supabase **publishable key** is compiled into the APK. Every private
credential lives exclusively in Edge Function secrets:

- `OPENROUTER_API_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`
- GoPay / OVO client secrets and MFA client ids
- `MIDTRANS_SERVER_KEY`

The GoPay client secret and MFA client id were previously hardcoded in the
Android source; they were removed and now resolve server-side in
`gateway-proxy`. Unofficial wallet credentials must never return to the APK.

---

## 2. AI — OpenRouter, free tier only

All AI features use **free OpenRouter models**. No paid slug is used anywhere.

| Feature | Model |
| --- | --- |
| Chat + transaction parsing | `inclusionai/ling-3.0-flash-fin:free` |
| Receipt OCR (vision) | `inclusionai/ling-3.0-flash-vl:free` |
| Voice STT | On-device Android `SpeechRecognizer` |

Both the app (`OpenRouterClient`) and `openrouter-proxy` walk a free-model
chain — `ling-3.0-flash-fin` → `nex-n2.5-mini` → `ling-3.0-flash-vl` →
`lfm-2.5-2.6b` — retrying on HTTP 402/429/503 before failing. Free models are
frequently rate-limited, so the chain is what keeps the assistant usable.

No free transcription model exists on OpenRouter, so `voice-stt` reports that
limitation honestly instead of billing a paid Whisper call; the app's
`SpeechRecognizer` is the real STT path.

HuggingFace is not used. OpenRouter is the sole model provider.

### Offline fallback

When the online call fails or the user is offline, NoTa falls back to
`OfflineNlpEngine` + deterministic assistant rules grounded in real Room data.
No response fabricates a balance or amount.

---

## 3. E-wallet linking

The e-wallet picker lets the user choose which provider to integrate
(GoPay / OVO / DANA), each behind an unofficial adapter.

### OVO — 3-step auth (per `namtxs/ovoid-API`)

1. `sendOtp` → `AGW /v3/user/accounts/otp`
2. `OTPVerify` → `AGW /v3/user/accounts/otp/validation` (returns `otp_token`)
3. `getAuthToken` → `AGW /v3/user/accounts/login` (RSA-hashed security code)

Balance: `BASE /wallet/inquiry`, `data.{"001"}.card_balance` (OVO Cash),
`data.{"600"}` (OVO Points). App version `3.54.0`, `OVO/21404 CFNetwork/1220.1
Darwin/20.3.0`.

### GoPay (per `namtxs/gopay-api`)

`/goid/login/request` → `/goid/token` (OTP grant) →
`/v1/payment-options/balances`. iOS client `4.88.0`, `com.go-jek.ios`.

Gojek rejects stale client versions with *"Perbarui sekarang"*. If wallet
linking starts failing, bump `x-appversion` and the header set in
`gateway-proxy` before assuming a code bug.

All wallet calls route through `gateway-proxy`. The APK holds no wallet
credential.

---

## 4. Release

- Signed APK, R8 minified with mandatory keep rules for Hilt / Room / Compose /
  ML Kit. Without these the release build crashes at startup; debug builds hide
  it because minification is off.
- Build: `./gradlew assembleRelease` with `JAVA_HOME` pointed at the Android
  Studio JBR. Output lands in `app/build_out/` (not `build/`).
- Distributed as a **GitHub Release asset**, never as a committed git blob.
- Namespace is `com.example` while `applicationId` is `com.nota.finance`, so
  generated `BuildConfig` is `com.example.BuildConfig`.

---

## 5. Configuration

`SUPABASE_URL` and `SUPABASE_ANON_KEY` are compiled from `.env` by the Google
`secrets` Gradle plugin into `BuildConfig`. The project is
`lawehfafeevoctogpowr` (ap-southeast-2).

---

## 6. Known limitations

- Free-tier AI is rate-limited under load; the model chain mitigates but does
  not eliminate it.
- Cloud STT is unavailable on the free tier; on-device recognition is used.
- Unofficial wallet adapters depend on third-party API contracts and can break
  when Gojek/OVO change them.
- Confirmation is always required before any ledger mutation (OCR, voice, or
  chatbot proposal).
