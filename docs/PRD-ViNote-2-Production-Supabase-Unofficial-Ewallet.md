# PRD — ViNote-2 Production Ready: Supabase Only + Hybrid AI

**Status:** Implementation specification  
**Target:** Android production release  
**Primary backend:** Supabase  
**E-wallet strategy:** retain existing unofficial DANA / GoPay / OVO services behind adapters  
**AI strategy:** hybrid — deterministic local assistant first, online AI when reasoning/generation is actually needed  
**Core rule:** no mock/demo/placeholder financial data in production paths.

---

## 1. Product Goal

Make ViNote-2 a real finance application rather than a prototype that only looks functional.

The production version must:

- use Supabase as the only authentication and cloud backend authority;
- retain the existing unofficial DANA, GoPay, and OVO integrations where they currently work;
- use Room for local-first financial data and Supabase Postgres for cloud persistence;
- provide a **Hybrid Chatbot** that can answer deterministic finance questions without calling an AI route;
- call online AI only for tasks that genuinely require natural-language reasoning, explanation, summarization, or generation;
- support camera and voice input with online-first processing and honest offline fallbacks;
- contain zero fabricated financial values;
- degrade honestly when network, AI, or unofficial wallet integrations fail.

Unofficial e-wallet APIs are inherently fragile and may stop working when providers change private APIs, authentication, anti-abuse controls, or app protocols. ViNote must isolate that risk.

---

## 2. Non-Goals

Do not retain or introduce:

- Firebase Authentication as an auth authority;
- Firebase Firestore as a second production database;
- Auth.js / NextAuth;
- fake wallet connections or balances;
- fake successful API responses;
- generated fallback users such as `user_default`;
- large bundled LLMs solely to make offline chat appear intelligent;
- an AI request for every chatbot message;
- silent creation or modification of financial transactions by AI/OCR/voice.

Mocks/fakes are allowed only in tests and isolated UI previews.

---

## 3. Target Architecture

```text
Android App
├── Compose UI
├── ViewModels
├── Domain / Use Cases
├── Repository interfaces
│
├── Room DB ---------------------- Local financial source of truth
├── WorkManager ------------------ Background sync
├── Supabase Auth ---------------- Identity + session
├── Supabase Postgres ------------ Cloud persistence / recovery
├── Supabase Storage ------------- Optional receipt backup
│
├── Hybrid Assistant Engine
│   ├── Intent Router
│   ├── Deterministic Finance Engine
│   ├── Local Context Builder
│   └── Online AI Gateway
│        └── Supabase Edge Function → OpenRouter → LLM
│
├── Media Input
│   ├── CameraX
│   │   ├── Online OCR/AI path
│   │   └── Offline ML Kit OCR path
│   └── Voice
│       ├── Online STT/AI path
│       └── Offline on-device SpeechRecognizer path
│
└── EWallet Provider Layer
    ├── DANA unofficial adapter
    ├── GoPay unofficial adapter
    └── OVO unofficial adapter
```

No UI layer should directly call infrastructure services.

---

## 4. Authentication — Supabase Only

`Supabase Auth` is the single identity authority.

Use:

- Supabase email/password and/or supported OAuth;
- Supabase access/refresh session handling;
- canonical user ID from the authenticated Supabase session;
- RLS on every user-owned table.

Remove production dependencies on:

- `FirebaseAuth.getInstance()`;
- Firebase user IDs;
- `user_default` or timestamp-generated users;
- duplicate Firebase/Supabase auth state.

Target abstraction:

```kotlin
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshSession(): Result<Session>
    fun currentUserId(): String?
}
```

No repository may invent a user ID.

---

## 5. Supabase Database

Suggested tables:

```text
profiles
wallets
transactions
goals
budgets
wallet_connections
provider_sync_state
notification_events
```

Every user-owned row must contain `user_id uuid references auth.users(id)`.

RLS policy principle:

```sql
user_id = auth.uid()
```

Service-role access is allowed only inside trusted Edge Functions and must never be shipped to Android.

---

## 6. E-Wallet Integration — KEEP UNOFFICIAL APIs

Existing unofficial services remain and are wrapped behind a stable abstraction:

```kotlin
interface EWalletProvider {
    val provider: EWalletType
    suspend fun login(credentials: ProviderCredentials): Result<ProviderSession>
    suspend fun refreshSession(session: ProviderSession): Result<ProviderSession>
    suspend fun getBalance(session: ProviderSession): Result<ProviderBalance>
    suspend fun getTransactions(
        session: ProviderSession,
        since: Instant?
    ): Result<List<ProviderTransaction>>
    suspend fun logout(session: ProviderSession): Result<Unit>
}
```

Adapters:

```text
DanaEWalletProvider → UnofficialDanaService
GoPayEWalletProvider → UnofficialGoPayService
OvoEWalletProvider → UnofficialOvoService
```

Flow:

```text
UI → ConnectEWalletUseCase → EWalletRepository → EWalletProvider → provider service
```

Handle expired sessions, authentication failures, challenges, rate limits, schema changes, outages, duplicate responses, and partial syncs. Never bypass provider security controls.

---

## 7. Wallet Credentials and Sessions

Keep credentials/session material out of UI state, Git, logs, analytics, and crash reports.

Where technically possible:

```text
Android
 ↓ authenticated Supabase session
Supabase Edge Function
 ↓
Unofficial provider API
```

Never store passwords or OTPs in plaintext. Never place provider secrets or tokens in `BuildConfig` or APK assets.

---

## 8. Wallet Balance Semantics

Every wallet distinguishes:

```text
calculatedBalance
providerReportedBalance
lastSyncedAt
syncStatus
```

`calculatedBalance` is derived from the actual ViNote ledger. `providerReportedBalance` is the latest successfully returned provider value.

If provider synchronization fails, show the last-known value with a stale/error indicator rather than pretending it is current.

---

## 9. Transaction Synchronization

```text
ProviderTransaction
 ↓
Normalization
 ↓
Deterministic fingerprint
 ↓
Duplicate check
 ↓
Validation
 ↓
Room transaction
 ↓
Supabase sync
```

Imported transactions require an internal UUID, wallet ID, provider, provider transaction ID when available, deterministic fingerprint, amount in integer minor units, type, timestamp, source, and sync state.

The same provider transaction must never create duplicate ledger entries.

---

## 10. Notification Detection

Notification listening remains complementary to provider synchronization.

It can detect payment/incoming-money notifications and create transaction candidates. It must reconcile against imported provider transactions using deterministic fingerprints.

Expose listener status, last processed event, processing errors, and recovery state. Do not promise background detection after force-stop or OEM process termination.

---

## 11. Room + Supabase Sync

Room is the immediate local source of truth for app interaction. Supabase is the cloud persistence/recovery layer.

Write path:

```text
User action → Room → SyncQueue → WorkManager → Supabase
```

Pull path:

```text
Supabase → SyncCoordinator → validation → Room → UI
```

Requirements:

- stable UUIDs;
- idempotent upserts;
- retry/backoff;
- network constraints;
- tombstones for deletes;
- conflict detection;
- observable sync state;
- no infinite retry loops.

Financial conflicts must not be silently overwritten.

---

## 12. Remove ALL Mock Data

Production must contain no fake financial state.

Audit for:

```text
mock / fake / sample / demo / placeholder / seed / hardcoded
user_default
hard-coded balances
hard-coded transactions
fake AI responses
```

Inspect ViewModels, repositories, Room callbacks, Compose defaults, JSON assets, chart datasets, dashboard fallbacks, wallet services, goals, budgets, and debug bypasses.

Empty data must produce an honest empty state. Loading must not be represented as `Rp0` unless the real balance is zero.

---

## 13. Production Dashboard

All values must come from real repositories:

- wallet balances;
- today's spending;
- budget progress;
- recent transactions;
- goals;
- wallet sync status;
- pending transaction candidates;
- cloud sync state.

No demo numbers are allowed to make the dashboard look populated.

---

# 14. Hybrid Chatbot — CORE REQUIREMENT

The chatbot must **not** route every message to an online AI model.

Its architecture is a local-first intent router with deterministic finance tools and an online AI fallback.

## 14.1 High-Level Flow

```text
User message
     ↓
Normalize / language detection
     ↓
Intent Router
     │
     ├── Deterministic intent
     │      ↓
     │   Local Finance Engine
     │      ↓
     │   Immediate answer
     │
     ├── Contextual finance intent
     │      ↓
     │   Local data query + rule engine
     │      ↓
     │   Immediate answer OR AI enhancement
     │
     └── Generative / ambiguous intent
            ↓
        Online AI Gateway
            ↓
        Supabase Edge Function
            ↓
        OpenRouter / allowed model
```

The router is the key component. It decides whether an AI call is necessary.

## 14.2 Questions That MUST Work Without AI

The following should be answered directly from Room/database/rules when enough local data exists:

| User intent | Example | Route |
|---|---|---|
| Current balance | “Saldo saya berapa?” | Local |
| Today's spending | “Hari ini aku habis berapa?” | Local |
| Monthly spending | “Bulan ini pengeluaran berapa?” | Local |
| Transaction lookup | “Transaksi terakhir saya apa?” | Local |
| Category total | “Bulan ini makan habis berapa?” | Local |
| Budget status | “Budget makan masih berapa?” | Local |
| Budget percentage | “Budget saya sudah berapa persen?” | Local |
| Goal progress | “Tabungan target saya sudah berapa?” | Local |
| Wallet balance | “Saldo DANA saya?” | Local/provider cache |
| Simple arithmetic | “Rp50.000 + Rp25.000 berapa?” | Local |
| Help/navigation | “Cara tambah transaksi?” | Local knowledge |
| App status | “Kenapa transaksi belum sync?” | Local status engine |

These requests should not consume AI quota or require an internet connection.

## 14.3 Questions That MAY Use AI

Use online AI when the request requires interpretation or generation beyond deterministic local capabilities:

- “Kenapa pengeluaran saya bulan ini terasa besar?”
- “Beri saya strategi supaya pengeluaran makan lebih terkontrol.”
- “Apa pola keuangan saya dari tiga bulan terakhir?”
- “Jelaskan kondisi keuangan saya dengan bahasa sederhana.”
- ambiguous natural-language questions;
- multi-step reasoning across several finance metrics;
- personalized educational explanations;
- conversational follow-up where deterministic intent confidence is low.

Before AI is called, the app should build a **minimal structured context** from local data rather than uploading the entire database.

## 14.4 Local Finance Engine

Create a deterministic service such as:

```kotlin
interface FinanceQueryEngine {
    suspend fun currentBalance(): Money
    suspend fun spending(period: Period, category: Category?): Money
    suspend fun recentTransactions(limit: Int): List<Transaction>
    suspend fun budgetStatus(category: Category?): BudgetStatus
    suspend fun goalProgress(goalId: String?): GoalProgress
    suspend fun walletBalance(walletId: String): WalletBalance
}
```

The engine is the authoritative source for numerical answers.

AI must never calculate or invent the ledger when the local engine can provide the value.

## 14.5 Intent Router

Use deterministic rules/keyword matching first, then structured parsing for more flexible Indonesian language.

Example:

```text
“saldo aku berapa?”
→ BALANCE_QUERY
→ FinanceQueryEngine.currentBalance()
→ answer locally
```

```text
“bulan ini aku boros gak?”
→ SPENDING_ANALYSIS
→ collect local metrics
→ if simple rule can answer, answer locally
→ otherwise AI enhancement
```

```text
“gimana cara nabung buat beli laptop?”
→ FINANCIAL_ADVICE
→ online AI
```

Router output should include:

```text
intent
confidence
requiredData
route = LOCAL | HYBRID | AI
```

## 14.6 Hybrid Route

Some questions should use local computation first and AI only for wording/reasoning.

```text
User question
 ↓
Local Finance Engine
 ↓
Structured facts
 ↓
AI receives facts, not raw database
 ↓
Natural-language explanation
```

Example:

```text
Local engine:
monthly_spending = 1_250_000
food = 650_000
food_share = 52%
budget_food = 500_000
budget_exceeded = true
```

AI may explain the pattern, but it must not change these facts.

## 14.7 Chatbot Context and Memory

The chatbot should maintain lightweight conversation context locally for the current session.

Example:

```text
User: “Bulan ini aku habis berapa?”
Bot: “Rp1.250.000.”
User: “Kalau makan?”
→ resolve “makan” against previous monthly spending context
```

Do not send the entire chat history or full financial database to the model by default.

Use a bounded context window and redact unnecessary sensitive fields.

## 14.8 Offline Chatbot Behavior

Offline mode must remain useful.

Available offline:

- balance queries;
- spending totals;
- category totals;
- budget/goal progress;
- transaction lookup;
- simple calculations;
- app help;
- deterministic explanations/templates;
- transaction draft extraction from voice/OCR.

Unavailable offline:

- generative AI reasoning;
- cloud-only knowledge;
- server-side model inference.

When AI is unavailable, the chatbot must clearly state that it can still answer data-based questions locally rather than showing a generic failure for every request.

## 14.9 Chatbot Response Safety

The chatbot must:

- read financial numbers from trusted local/domain services;
- never fabricate missing values;
- distinguish calculated balance from provider-reported balance;
- never silently create/edit/delete transactions;
- ask for confirmation before a chatbot-created transaction is committed;
- never expose provider credentials/tokens;
- show a clear “AI unavailable” state when online AI cannot be reached.

---

# 15. AI Gateway

Online AI must use:

```text
Android
 ↓ authenticated request
Supabase Edge Function
 ↓
OpenRouter
 ↓
allowlisted model
```

The OpenRouter API key must never be shipped in the APK.

Use authentication, request-size limits, model allowlists, quotas/rate limits, bounded retries, sanitized errors, and minimal sensitive logging.

The Edge Function should accept structured context and user intent rather than unrestricted database access.

---

# 16. Camera / Receipt Input — ONLINE + OFFLINE

Camera input must be online-first but remain useful without connectivity.

## 16.1 Capture

Use CameraX for capture.

Local preprocessing should:

- normalize orientation;
- crop/deskew where practical;
- resize excessively large images;
- compress before network upload;
- avoid retaining unnecessary full-resolution copies.

## 16.2 Online Path

```text
CameraX
 ↓
Local preprocessing
 ↓
Supabase Edge Function
 ↓
Cloud OCR / vision processing
 ↓
Structured transaction candidate
 ↓
Editable draft
 ↓
User confirmation
 ↓
Room
```

Do not place third-party OCR/AI secrets in the APK.

## 16.3 Offline Path

```text
CameraX
 ↓
Local preprocessing
 ↓
On-device ML Kit OCR
 ↓
Deterministic receipt parser
 ↓
Editable transaction draft
 ↓
User confirmation
 ↓
Room
```

Prefer one lightweight on-device OCR implementation rather than bundling multiple OCR engines by default. Do not bundle a large LLM solely for receipt parsing.

If offline OCR fails, provide manual editing instead of fabricating fields.

## 16.4 Receipt Media Lifecycle

By default, raw receipt images are temporary until extraction/confirmation completes.

Optional Supabase Storage backup may be offered as a user-controlled feature. Temporary files should be deleted after processing when no longer required.

---

# 17. Audio / Voice Input — ONLINE + OFFLINE

Voice input follows the same hybrid principle.

## 17.1 Online Path

```text
Microphone
 ↓
Speech-to-Text
 ↓
Transcript
 ↓
Intent / transaction extraction
 ↓
AI enhancement if required
 ↓
Editable draft
 ↓
User confirmation
 ↓
Room
```

Use Supabase Edge Functions for cloud STT providers when a provider API key is required.

## 17.2 Offline Path

Prefer Android's on-device `SpeechRecognizer` capability where the device supports it.

```text
Microphone
 ↓
On-device SpeechRecognizer
 ↓
Transcript
 ↓
Deterministic parser
 ↓
Editable draft
 ↓
User confirmation
 ↓
Room
```

The app must check on-device recognition availability rather than assuming every Android device has an offline recognizer.

If unavailable, show manual input as the fallback.

Test Indonesian recognition using `id-ID`.

## 17.3 Audio Privacy

Raw recordings should not be retained after transcription unless a future feature explicitly requires user-controlled recording storage.

Do not send audio to cloud services when local processing is sufficient.

---

# 18. APK / Model Size Strategy

Hybrid functionality must not make the APK unnecessarily large.

Rules:

- no large LLM bundled in the base APK;
- no duplicate OCR engines without measured benefit;
- use platform/on-device capabilities where practical;
- keep optional models downloadable rather than mandatory when technically possible;
- measure release APK/AAB size and installed model footprint in CI/release checks;
- remove unused ML dependencies and assets.

Any future offline AI model must have an explicit storage/download budget and measurable product benefit before inclusion.

---

# 19. Codebase Refactor

### `ViNoteViewModel.kt`

ViewModels must depend on use cases/interfaces rather than constructing Firebase, Supabase, e-wallet, or AI services directly.

Target:

```text
ViewModel
 ↓
UseCase
 ↓
Repository / Engine
 ↓
Infrastructure
```

### `AppModule.kt`

Provide through dependency injection:

- Supabase client/auth repository;
- Room database/DAOs;
- sync coordinator;
- e-wallet repository + DANA/GoPay/OVO adapters;
- `FinanceQueryEngine`;
- `ChatIntentRouter`;
- `ChatbotRepository`;
- AI gateway;
- OCR service;
- voice service;
- WorkManager workers.

No ViewModel should instantiate service implementations manually.

---

# 20. Required UI States

## Chatbot

```text
READY
PROCESSING_LOCAL
PROCESSING_AI
AI_UNAVAILABLE_LOCAL_ONLY
NO_DATA
ERROR
```

The UI should not imply that every response came from AI. A small route indicator may be used:

```text
Answered from your data
AI-assisted
Offline
```

## Camera

```text
READY
CAPTURING
PROCESSING_ONLINE
PROCESSING_OFFLINE
NEEDS_REVIEW
CONFIRMED
OCR_FAILED
PERMISSION_DENIED
SYNC_PENDING
```

## Voice

```text
READY
LISTENING
TRANSCRIBING_ONLINE
TRANSCRIBING_OFFLINE
NEEDS_REVIEW
CONFIRMED
ON_DEVICE_UNAVAILABLE
PERMISSION_DENIED
ERROR
```

No fake “success” state may be shown when extraction or synchronization actually failed.

---

# 21. Error Handling

Every external integration must return an actionable state.

Examples:

```text
AI unavailable
→ Answer data-based questions locally
→ Retry AI
```

```text
Offline voice unavailable on this device
→ Use manual input
```

```text
Receipt OCR failed
→ Edit transaction manually
```

```text
Provider unavailable
→ Keep last-known balance with stale indicator
→ Retry
```

```text
Session expired
→ Reconnect wallet
```

Never convert failures into fabricated success responses.

---

# 22. Security Requirements

Before production:

- Supabase RLS enabled on all user-owned tables;
- no service-role key in Android;
- no OpenRouter/STT provider secrets in Android;
- no provider passwords/tokens in logs;
- secure session refresh;
- notification data minimized;
- temporary media cleaned up;
- crash reporting scrubbed of sensitive financial data;
- release builds disable debug bypasses;
- Edge Functions validate authenticated user identity and request shape.

---

# 23. Testing Strategy

## Unit tests

- balance calculations;
- spending/category queries;
- budget and goal calculations;
- intent classification;
- local chatbot responses;
- ambiguous intent routing;
- hybrid context generation;
- transaction extraction;
- receipt parsing;
- voice parsing;
- duplicate fingerprints;
- provider normalization;
- sync conflict handling.

## Integration tests

- Supabase Auth;
- RLS isolation;
- Room ↔ Supabase sync;
- wallet import;
- notification import;
- duplicate prevention;
- provider outage/session expiry;
- online AI gateway;
- AI failure → local chatbot fallback;
- online OCR → offline OCR fallback;
- online voice → offline voice fallback;
- offline → online recovery.

## Production acceptance tests

1. A user with no transactions sees an empty state, not sample data.
2. “Saldo saya berapa?” is answered locally without an AI request.
3. “Bulan ini aku habis berapa?” uses actual Room data.
4. A complex financial question can use online AI when connected.
5. If AI fails, deterministic finance questions continue to work.
6. Camera works online and falls back to on-device OCR offline when supported.
7. Voice works online and falls back to on-device recognition when supported.
8. Unsupported offline voice capability falls back to manual entry.
9. OCR/voice never commits a transaction without user confirmation.
10. No provider/API secret exists in the APK.
11. DANA/GoPay/OVO integrations remain accessible through provider adapters.
12. Stale e-wallet balances are visibly marked.
13. Identical provider/notification transactions cannot duplicate the ledger.
14. Release builds contain no production mock/demo financial data.

---

# 24. Implementation Priority

### P0 — Correctness and production safety

- Supabase-only auth/data path;
- remove Firebase production authority;
- remove fake financial data;
- secure AI gateway;
- preserve and isolate unofficial e-wallet adapters;
- Room/Supabase synchronization.

### P1 — Hybrid intelligence

- `FinanceQueryEngine`;
- deterministic `ChatIntentRouter`;
- local chatbot answers;
- AI fallback/generative route;
- bounded contextual memory;
- offline chatbot states.

### P2 — Hybrid media input

- CameraX capture/preprocessing;
- online receipt OCR path;
- offline ML Kit OCR path;
- online voice/STT path;
- offline on-device SpeechRecognizer path;
- editable drafts and confirmation.

### P3 — Hardening

- provider failure handling;
- duplicate reconciliation;
- privacy/media cleanup;
- APK/model-size measurement;
- comprehensive offline/online integration tests;
- release audit for secrets, mocks, and debug bypasses.

---

# 25. Definition of Done

ViNote-2 is production-ready only when:

- Supabase is the sole production auth/cloud authority;
- all financial values originate from real data;
- unofficial DANA/GoPay/OVO services remain functional through adapters;
- the chatbot has a functioning local deterministic route and does not unnecessarily call AI;
- complex questions can use the online AI route safely;
- AI failure does not break basic chatbot finance queries;
- camera and voice both support online processing plus honest offline fallback;
- large AI models are not unnecessarily bundled into the APK;
- OCR/voice/chatbot suggestions require confirmation before ledger mutation;
- Room and Supabase synchronize reliably;
- security and RLS checks pass;
- production builds contain no fake/demo financial state.
