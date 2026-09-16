# PRD — ViNote-2 Production Ready: Supabase Only + Unofficial E-Wallet

**Status:** Implementation specification  
**Target:** Android production release  
**Primary backend:** Supabase  
**E-wallet strategy:** retain existing unofficial DANA / GoPay / OVO services, isolated behind a provider abstraction  
**Core rule:** remove every mock, demo, placeholder, fake, seeded, and hard-coded financial value from production paths.

---

## 1. Product Goal

Make ViNote-2 a real, usable finance application rather than a prototype that only looks functional.

The production version must:

- use **Supabase as the only authentication and cloud backend authority**;
- retain the existing **unofficial DANA, GoPay, and OVO integrations** where they currently work;
- keep credentials, sessions, and provider-specific implementation details out of UI/domain code;
- maintain a deterministic local financial ledger with Room;
- synchronize authorized user data with Supabase Postgres;
- contain **zero mock financial data** in release builds;
- degrade honestly when an unofficial provider integration stops working;
- never present fabricated balance, transaction, budget, or AI results as real data.

> Important: unofficial e-wallet APIs are inherently fragile. They may break because a provider changes its private API, authentication, anti-abuse controls, or app protocol. ViNote must isolate this risk rather than pretending the integration is guaranteed.

---

## 2. Non-Goals

Do not build or retain:

- Firebase Authentication as an auth authority;
- Firebase Firestore as a second production database;
- Auth.js / NextAuth;
- Supabase + Firebase dual-authentication;
- hard-coded demo transactions/balances;
- fake successful API responses;
- fake wallet connections;
- placeholder OTP/session success states;
- generated IDs such as `user_default` or timestamp-based fake users;
- a second backend solely to proxy the same Supabase data;
- UI that displays sample financial values when a user has no data.

Firebase may remain only if a clearly isolated non-core dependency is proven necessary; it must not become a second source of truth. The preferred production target is **Supabase-only**.

---

## 3. Target Architecture

```text
Android App
├── Compose UI
├── ViewModels
├── Domain / Use Cases
├── Repository interfaces
│
├── Room DB ---------------------- Local source of truth
├── WorkManager ------------------ Reliable background sync
├── Supabase Auth ---------------- Authentication + sessions
├── Supabase Postgres ------------ Cloud source / recovery
├── Supabase Storage ------------- Receipt/image storage if required
│
└── EWallet Provider Layer
    ├── DANA unofficial adapter
    ├── GoPay unofficial adapter
    └── OVO unofficial adapter
             │
             └── Provider-specific API/session implementation
```

### Data flow

```text
User
 ↓
Supabase Auth
 ↓
Firebase-free Android session
 ↓
Room ←→ SyncCoordinator ←→ Supabase Postgres
                         
                         └→ EWalletProvider
                              ├→ DANA
                              ├→ GoPay
                              └→ OVO
```

For privileged server-side operations, use **Supabase Edge Functions** rather than Firebase Cloud Functions. Secrets must never be bundled into the APK.

---

## 4. Authentication — Supabase Only

### Requirements

`Supabase Auth` is the single identity authority.

Use:

- Supabase email/password and/or supported OAuth providers;
- Supabase access token + refresh token/session handling;
- one canonical user ID: `supabase.auth.currentUser.id`;
- Row Level Security (RLS) on all user-owned tables.

### Remove

From production code:

- `FirebaseAuth.getInstance()`;
- Firebase user IDs;
- `user_default`;
- `user_<timestamp>` fallback IDs;
- AuthRepository implementations backed by Firebase;
- duplicate auth state between Firebase and Supabase.

### Target repository

```kotlin
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshSession(): Result<Session>
    fun currentUserId(): String?
}
```

No repository may invent a user ID. If there is no authenticated Supabase user, user-scoped cloud operations must fail safely.

---

## 5. Supabase Database

Use Postgres as the cloud source for synchronized application data.

Suggested tables:

```text
profiles
wallets
transactions
goals
budgets
sync_queue / sync_events (only if needed server-side)
ewallet_connections
provider_sync_state
notification_events
```

Every user-owned table must include:

```text
user_id uuid references auth.users(id)
```

### RLS rule

A user can only read/write rows where:

```sql
user_id = auth.uid()
```

Server/service-role operations must be restricted to Edge Functions and never exposed to Android.

---

## 6. E-Wallet Integration — KEEP UNOFFICIAL APIs

The existing unofficial services are **not removed**. Instead, they become replaceable adapters.

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

Implement:

```text
DanaEWalletProvider
GoPayEWalletProvider
OvoEWalletProvider
```

wrapping the existing:

```text
UnofficialDanaService
UnofficialGoPayService
UnofficialOvoService
```

### Critical separation

The UI must not directly call `UnofficialDanaService`, etc.

Instead:

```text
UI
 ↓
ConnectEWalletUseCase
 ↓
EWalletRepository
 ↓
EWalletProvider
 ↓
Unofficial provider implementation
```

This allows the provider implementation to change without rewriting the app.

### Unofficial API reality requirements

The app must explicitly handle:

- expired sessions;
- invalid credentials;
- OTP/challenge requirements when legitimately required by the provider flow;
- rate limiting;
- provider API schema changes;
- network failures;
- authentication failures;
- unavailable endpoints;
- temporary provider outages;
- duplicate transaction responses;
- partial synchronization.

Do not claim that an unofficial integration is guaranteed or officially supported by the wallet provider.

Do not attempt to bypass security controls, CAPTCHAs, device verification, or other access restrictions.

---

## 7. E-Wallet Credentials and Sessions

If the existing unofficial API genuinely requires credentials, the architecture must keep them out of ordinary UI state and logs.

Preferred flow:

```text
Android
 ↓ authenticated Supabase user
Supabase Edge Function
 ↓ provider credentials/session handling
Unofficial provider API
```

Never:

- log passwords;
- log OTPs;
- store credentials in plaintext SharedPreferences;
- put provider secrets in Git;
- put provider credentials in BuildConfig constants;
- send credentials to analytics/crash reporting;
- display provider tokens in UI.

Use encrypted local storage only when local persistence is unavoidable. Prefer short-lived provider sessions and server-side secret handling where technically possible.

---

## 8. Wallet Balance Semantics

Every wallet must distinguish between:

```text
calculatedBalance
providerReportedBalance
lastSyncedAt
syncStatus
```

### calculatedBalance

Deterministically calculated by ViNote from the ledger.

```text
opening balance
+ income
- expense
± adjustment
```

Transfers must not be counted as spending.

### providerReportedBalance

The balance returned by the e-wallet integration at the latest successful synchronization.

### UI rule

Never silently treat stale or unavailable provider data as current.

Example:

```text
DANA
Balance: Rp250.000
Synced 4 minutes ago
```

If synchronization fails:

```text
DANA
Last known balance: Rp250.000
Could not sync just now
[Retry]
```

Not:

```text
DANA
Rp250.000
```

with no indication that it is stale.

---

## 9. Transaction Synchronization

Provider transactions must be normalized into the internal ledger.

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

Each imported provider transaction must have:

- internal UUID;
- wallet ID;
- provider name;
- provider transaction ID when available;
- deterministic fingerprint;
- amount in integer minor units (`Long`);
- transaction type;
- occurredAt;
- import source;
- sync status.

The same provider transaction must never create two ledger entries.

---

## 10. Notification Detection

Notification listening can remain as a complementary mechanism for wallet detection.

Use it for:

- detecting payment notifications;
- detecting incoming money notifications;
- creating transaction candidates;
- reconciling with provider synchronization.

Do not automatically create a duplicate transaction when the same transaction has already been imported from the e-wallet API.

Use a deterministic fingerprint based on available fields such as:

```text
provider + amount + timestamp window + merchant/reference
```

Persist the raw notification event ID/fingerprint before processing so retries cannot duplicate transactions.

Because Android/OEM restrictions can stop background execution, the UI must expose:

- listener enabled/disabled;
- last detected notification;
- last processing time;
- processing errors;
- recovery instructions.

Never promise that detection works after the user force-stops the app or the OEM kills its process.

---

## 11. Room + Supabase Sync

Room remains the immediate local source of truth for the app experience.

Supabase provides cloud persistence and cross-device recovery.

### Write path

```text
User action
 ↓
Room transaction
 ↓
SyncQueue
 ↓
WorkManager
 ↓
Supabase Postgres
```

### Pull path

```text
Supabase
 ↓
SyncCoordinator
 ↓
Validation
 ↓
Room
 ↓
UI
```

Requirements:

- stable UUIDs generated once;
- no fake user fallback;
- idempotent upserts;
- retry with exponential backoff;
- network constraints;
- tombstones for deletes;
- conflict detection;
- observable sync state;
- no infinite retry loops.

Financial transaction conflicts must not be silently overwritten.

---

## 12. Remove ALL Mock Data

This is a hard production gate.

Search the entire repository for patterns including:

```text
mock
Mock
fake
Fake
sample
Sample
demo
Demo
placeholder
Placeholder
seed
Seed
hardcoded
user_default
Rp100.000
Rp250.000
Rp500.000
example transactions
sample transactions
```

Also inspect:

- ViewModels;
- repositories;
- Room DAOs/database callbacks;
- Compose previews/default parameters;
- fake API responses;
- test fixtures accidentally used by production code;
- JSON assets;
- hard-coded chart datasets;
- dashboard fallback values;
- fake wallet balances;
- fake transaction history;
- fake goals/budgets;
- fake AI responses;
- debug-only bypasses accidentally enabled in release.

### Allowed

Mock/fake data is allowed **only inside automated tests and Compose previews**, and it must be structurally isolated from production dependency injection.

### Forbidden

```kotlin
if (transactions.isEmpty()) {
    transactions = demoTransactions
}
```

Instead:

```text
transactions.isEmpty()
        ↓
proper empty state
```

Example:

```text
Belum ada transaksi

Catat transaksi pertama kamu untuk mulai
mengelola keuangan.

[Tambah Transaksi]
```

The dashboard must never invent numbers to make the UI look complete.

---

## 13. Production Dashboard

All displayed values must come from actual repositories.

Dashboard:

- current wallet balances;
- today's spending;
- budget progress;
- recent transactions;
- wallet sync status;
- pending transaction candidates;
- goals;
- cloud sync state.

Loading state must not be represented by `Rp0` unless the actual balance is zero.

---

## 14. AI Architecture

AI remains an assistant, not the financial authority.

Allowed:

- categorize a transaction;
- classify notification text;
- summarize spending;
- explain patterns;
- suggest budget adjustments;
- generate educational feedback.

Not allowed:

- invent balances;
- calculate authoritative ledger totals;
- silently create transactions;
- silently modify transactions;
- approve transfers;
- override provider data;
- fabricate missing transaction information.

### Online AI

```text
Android
 ↓
Supabase Edge Function
 ↓
OpenRouter
 ↓
LLM
```

The OpenRouter key must never be shipped in the APK.

Use:

- authentication checks;
- App Check equivalent where applicable;
- request size limits;
- model allowlist;
- rate limiting/quotas;
- bounded retries;
- sanitized errors;
- no sensitive financial logging.

---

## 15. OCR and Voice

### OCR

```text
Camera
 ↓
ML Kit / OCR
 ↓
Extract candidates
 ↓
User edits
 ↓
User confirms
 ↓
Create transaction
```

### Voice

```text
Speech
 ↓
Transcript
 ↓
Deterministic extraction / AI suggestion
 ↓
Editable draft
 ↓
User confirms
 ↓
Create transaction
```

Neither OCR nor voice may silently create an incorrect financial transaction.

---

## 16. Security Requirements

Before production:

- Supabase RLS enabled on every user-owned table;
- no service-role key in Android;
- no OpenRouter key in Android;
- no provider passwords/tokens in logs;
- no sensitive credentials in Git;
- secure session refresh;
- certificate/network security reviewed;
- exported Android components reviewed;
- notification data minimized;
- crash reporting scrubbed of financial credentials;
- release build disables debug bypasses.

---

## 17. Codebase Refactor

### `ViNoteViewModel.kt`

Remove direct infrastructure construction such as:

```kotlin
FirebaseAuth.getInstance()
AuthRepositoryImpl(SupabaseClientProvider(...))
```

Inject interfaces through Hilt:

```text
ViewModel
 ↓
UseCase
 ↓
Repository
```

### `AppModule.kt`

Remove Firebase/Supabase mixed wiring.

Provide:

- Supabase client;
- Supabase Auth repository;
- Postgres repositories;
- Room database;
- sync coordinator;
- e-wallet repository;
- DANA/GoPay/OVO adapters;
- AI gateway;
- WorkManager workers.

No ViewModel should instantiate service implementations manually.

---

## 18. Required E-Wallet UI

Wallet screen:

```text
My Wallets

[DANA]
Rp xxx.xxx
● Connected
Last sync: ...

[GoPay]
Rp xxx.xxx
● Connected
Last sync: ...

[OVO]
Not connected
[Connect]
```

Connection states:

```text
DISCONNECTED
CONNECTING
CONNECTED
SYNCING
STALE
AUTH_REQUIRED
ERROR
```

The app must not show `CONNECTED` merely because a local preference says the wallet was enabled. It requires a verified successful provider session.

---

## 19. Error Handling

Every external integration must produce actionable states.

Examples:

```text
Provider unavailable
→ Retry
```

```text
Session expired
→ Reconnect wallet
```

```text
Provider response changed
→ Sync temporarily unavailable
```

```text
No transactions found
→ Empty state, not demo data
```

Never convert provider errors into fabricated successful responses.

---

## 20. Testing Strategy

### Unit tests

- balance calculation;
- transaction classification;
- duplicate fingerprinting;
- transfer handling;
- budget calculation;
- goal calculation;
- provider response normalization;
- sync conflict handling.

### Integration tests

- Supabase Auth;
- RLS isolation;
- Room ↔ Supabase sync;
- wallet import;
- notification import;
- duplicate prevention;
- expired provider session;
- provider outage;
- offline → online recovery.

### UI tests

- first-run empty dashboard;
- no transactions;
- no wallets;
- wallet connected;
- wallet stale;
- provider error;
- sync pending;
- sync failure;
- OCR confirmation;
- voice confirmation.

Mock data may exist in tests, but production DI must never load test fixtures.

---

## 21. Production Acceptance Criteria

A release is blocked if any of these remain:

- Firebase is still an auth authority;
- Supabase and Firebase user IDs can diverge;
- `user_default` exists in production paths;
- fake/demo financial values appear in the app;
- empty states show invented transactions;
- fake wallet connection succeeds without provider verification;
- unofficial API failures are converted to fake success;
- provider transactions can duplicate;
- wallet balance is double-counted;
- provider credentials are logged or shipped insecurely;
- service-role keys are inside the APK;
- RLS allows cross-user data access;
- offline edits disappear after reconnect;
- sync loops forever;
- AI fabricates financial totals;
- release build contains demo bypasses.

---

## 22. Implementation Order

### Phase 1 — Authentication migration

1. Remove Firebase Auth usage.
2. Implement Supabase Auth repository.
3. Update DI/Hilt.
4. Make Supabase user ID canonical.
5. Remove all fake user IDs.

### Phase 2 — Database

1. Define Supabase Postgres schema.
2. Add RLS.
3. Implement CRUD repositories.
4. Verify cross-user isolation.

### Phase 3 — Local ledger

1. Audit Room entities.
2. Use `Long` minor units for money.
3. Remove seeded/demo rows.
4. Fix transaction/transfer semantics.

### Phase 4 — Sync

1. Implement SyncQueue.
2. Add WorkManager workers.
3. Implement idempotent upserts.
4. Implement conflict handling.
5. Add sync status UI.

### Phase 5 — E-wallet

1. Preserve existing unofficial services.
2. Wrap them in `EWalletProvider`.
3. Implement DANA/GoPay/OVO connection state machine.
4. Secure credentials/session handling.
5. Normalize provider transactions.
6. Add deduplication.
7. Implement balance reconciliation.
8. Handle expired sessions and provider failures.

### Phase 6 — Mock-data purge

Perform repository-wide audit and delete every production mock/demo path.

### Phase 7 — AI/OCR/Voice

Harden these features only after the core ledger and wallet flows are reliable.

### Phase 8 — Release hardening

Run security, offline, sync, provider failure, and real-device tests.

---

## 23. Definition of Done

ViNote-2 is production-ready when a clean installation behaves like this:

1. User signs up/logs in through Supabase.
2. No demo financial information appears.
3. Empty screens are genuinely empty.
4. User can create a real wallet.
5. User can connect an e-wallet through the implemented unofficial provider flow.
6. Provider balance is fetched only after successful synchronization.
7. Provider transactions are imported without duplicates.
8. Notification detection can complement provider synchronization.
9. Room works offline.
10. Changes sync to Supabase when connectivity returns.
11. User data is isolated through RLS.
12. Provider/API failure is shown honestly.
13. No credentials or secrets leak into the APK/logs.
14. AI assists the user without becoming the source of financial truth.
15. Release build contains no production mock/demo data.

---

## 24. Product Principle

> **NoTa should show what it actually knows, not what makes the interface look complete.**

The unofficial e-wallet integrations may be fragile, but the rest of the application must remain reliable. Provider failure should degrade one integration, not corrupt the user's ledger, authentication, or financial history.
