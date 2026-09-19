# NoTaAI

AI-powered Indonesian personal-finance companion for Android.

Capture transactions by receipt scan, voice, chat, or manual entry — NoTa
grounds its advice in your real ledger.

## Stack

- **UI** Jetpack Compose + Material 3
- **Auth / DB** Supabase (GoTrue + Postgres, RLS-protected), Room offline-first
- **AI** OpenRouter, **free models only**, called through Supabase Edge Functions
- **Wallets** GoPay / OVO / DANA unofficial adapters, proxied server-side
- **Payments** Midtrans

## Security

Only the Supabase publishable key is compiled into the APK. All private
credentials (`OPENROUTER_API_KEY`, service role key, wallet client secrets,
Midtrans server key) live exclusively in Edge Function secrets.

## Build

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew assembleRelease
```

Output lands in `app/build_out/outputs/apk/release/`. Release builds are R8
minified — the Hilt/Room/Compose/ML Kit keep rules in `app/proguard-rules.pro`
are mandatory; without them the app crashes at startup.

## Documentation

- [`docs/PRD.md`](docs/PRD.md) — product requirements and architecture
