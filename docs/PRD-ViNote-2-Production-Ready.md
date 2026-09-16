# ViNote-2 — Production-Ready PRD

**Document status:** Implementation-ready
**Date:** 2026-09-16
**Target:** Android application + Firebase backend
**Objective:** Turn the existing ViNote-2/NoTa codebase into a production-ready, honest, maintainable application by removing or redesigning features that cannot be reliably supported in a normal Android + Firebase environment.

---

## 1. Executive Decision

The current repository contains a strong prototype foundation, but the architecture has accumulated conflicting backend/authentication approaches and several integrations that should not be presented as production capabilities.

The production target is therefore simplified to:

```text
Android / Jetpack Compose
        |
        | Firebase Auth
        | Firestore
        | Cloud Functions
        | FCM
        | App Check
        v
Firebase
        |
        +--> OpenRouter / Gemini gateway
        +--> server-side integration adapters (only where legitimately supported)

Android local source of truth:
Room + WorkManager
```

**Firebase is the only cloud backend/authentication platform.** Supabase Auth, Supabase database/API access, Auth.js/NextAuth, and a separate Next.js backend are not part of the production architecture.

The app's defining automatic-finance feature remains **notification-based transaction detection**, not credential-based e-wallet scraping.

---

# 2. Codebase Audit — What Must Change

The audit of `main` shows that the repository already contains Room, Firebase/Firestore, Firebase Auth references, Firebase Functions, notification listener infrastructure, wallet adapters, OCR/voice/AI services, goals, budgets, sync queues, and extensive UI/domain code. However, the implementation is currently inconsistent.

### 2.1 Conflicting authentication stacks — REMOVE

The code currently mixes FirebaseAuth with a `SupabaseClientProvider` and an `AuthRepositoryImpl` that explicitly implements Supabase Authentication. The repository also contains older Auth.js/NextAuth-oriented abstractions/docs.

**Production decision:**

- Firebase Authentication is the sole authentication authority.
- Use Google Sign-In and email/password only if both are actually configured and tested.
- Remove Supabase Auth dependencies and client initialization.
- Remove Auth.js/NextAuth references from application code and production documentation.
- Remove fake/direct-profile authentication such as generated IDs like `user_<timestamp>`.
- Never fall back to `user_default` for authenticated data.
- Canonical user ID = `FirebaseUser.uid`.

A user must never appear authenticated merely because a local placeholder ID exists.

### 2.2 Supabase integration — REMOVE FROM PRODUCTION PATH

The current code still uses `SupabaseClientProvider` for authentication and gateway URLs, including code paths used by `ViNoteViewModel` and dependency injection.

**Decision:** remove the Supabase client/provider and all Supabase Edge Function dependencies from the Android runtime.

Do not maintain two cloud backends merely for compatibility.

### 2.3 OpenRouter — KEEP, BUT ONLY THROUGH Firebase Functions

A Firebase callable function already exists for OpenRouter and stores the API key as a Firebase secret. This is the correct direction.

Production requirements:

- Android never stores an OpenRouter API key.
- Android calls the Firebase callable function.
- Require Firebase Auth.
- Enable Firebase App Check enforcement before release.
- Allowlist supported models instead of accepting arbitrary client-selected models in production.
- Limit message count and payload size.
- Add server-side timeout and bounded retry behavior.
- Do not log user prompts, financial records, or model responses in production logs.
- Apply per-user rate limits/quotas.
- Return stable, sanitized errors.

The current callable function has `enforceAppCheck: false`; this must become an explicit release blocker until App Check is enabled and tested.

### 2.4 E-wallet account linking / OTP — REMOVE AS A CLAIMED CAPABILITY

The current UI contains an E-wallet OTP linking flow and gateway services named `UnofficialDanaService`, `UnofficialGoPayService`, and `UnofficialOvoService`. This is not a dependable production foundation for a consumer finance app.

**Do not build production around:**

- asking users for e-wallet credentials;
- pretending OTP input creates an official account connection;
- reverse-engineering private APIs;
- unofficial login/session scraping;
- storing third-party authentication tokens without an official integration agreement;
- claiming that ViNote can query arbitrary DANA/GoPay/OVO balances in real time.

**Replacement:**

The E-wallet screen represents a **local tracking integration**, not an authenticated account connection.

User selects:

- DANA
- GoPay
- OVO
- other supported notification source

Then enables/disables notification detection for that provider.

The app obtains transaction events from Android notifications where available and permitted. No third-party password or OTP is requested by ViNote.

### 2.5 Real-time e-wallet balance — REDEFINE

The app cannot guarantee a real-time provider balance simply from notification access.

Production terminology must distinguish:

1. **Calculated balance** — derived from the ViNote ledger.
2. **Provider-reported balance** — a balance explicitly present in a trustworthy notification/import/integration.
3. **Manual balance** — user-entered starting/reconciliation balance.

Never display a calculated balance as if it were an official provider balance.

Never subtract an expense twice because both a notification and a reported balance were received.

### 2.6 Notification listener — KEEP, BUT MAKE IT BEST-EFFORT

Android `NotificationListenerService` is suitable for notification-based detection, but the app cannot guarantee uninterrupted background operation on every device/OEM.

Production behavior:

- process notifications off the main thread;
- persist the event before acting on it;
- use deterministic parsing;
- fingerprint and deduplicate events;
- use WorkManager for deferred/retry work;
- expose listener health and last-seen time;
- explain that Android/OEM battery restrictions may affect detection;
- provide a Settings recovery path;
- never claim detection continues after a user explicitly force-stops the app;
- never request accessibility privileges as a workaround.

### 2.7 AI must NOT control money

AI may classify, explain, summarize, and create drafts.

AI must never be the authoritative source for:

- balances;
- transaction totals;
- budget arithmetic;
- ledger mutations;
- transfer accounting;
- financial reconciliation.

All consequential financial state is deterministic and stored in Room/Firestore.

### 2.8 Overloaded ViewModel / service construction — REFACTOR

`ViNoteViewModel` currently constructs many repositories and services directly, including database, auth, sync, notification, wallet, AI, analytics, recurring, and gateway components.

Production target:

```text
Composable
  -> ViewModel
      -> Use Case / Domain Service
          -> Repository Interface
              -> Room / Firebase / Android platform service
```

Use Hilt consistently. The ViewModel should not create infrastructure objects manually.

Application-scoped components:

- `WalletDetectionCoordinator`
- notification processing coordinator
- sync coordinator
- AI gateway
- repositories

must be injected and lifecycle-safe.

---

# 3. Production Architecture

## 3.1 Android

```text
Jetpack Compose UI
       |
ViewModel
       |
Use Cases / Domain Services
       |
Repository Interfaces
   /       |        \
Room   Firebase   Platform APIs
 |         |           |
Local     Auth      NotificationListener
Ledger    Firestore   Camera / Mic
 |         |           |
 +---- WorkManager ---+
```

## 3.2 Firebase

```text
Firebase Auth
      |
      +--> Firestore
      |      +--> users/{uid}/profile
      |      +--> users/{uid}/wallets/{walletId}
      |      +--> users/{uid}/transactions/{transactionId}
      |      +--> users/{uid}/budgets/{budgetId}
      |      +--> users/{uid}/goals/{goalId}
      |      +--> users/{uid}/integrations/{integrationId}
      |      +--> users/{uid}/aiConversations/{conversationId}
      |
      +--> Cloud Functions
      |      +--> openRouterChat
      |      +--> optional server-side integration endpoints
      |
      +--> App Check
      +--> FCM
```

Firestore may be accessed directly from Android for user-owned CRUD **only when rules can enforce ownership and schema constraints**. Cloud Functions are required for privileged operations and external secrets.

---

# 4. Data Ownership and Security

Every user-owned document is nested under the authenticated Firebase UID wherever practical.

Rules must enforce:

```text
request.auth != null
AND
request.auth.uid == uid
```

Never trust a client-provided `userId` field as authorization.

Prefer document paths that make ownership explicit:

```text
/users/{uid}/transactions/{transactionId}
/users/{uid}/wallets/{walletId}
/users/{uid}/budgets/{budgetId}
/users/{uid}/goals/{goalId}
```

If a redundant `userId` field remains for local serialization, Firestore rules must require it to equal the path UID.

### Release security checklist

- [ ] Firebase Auth only.
- [ ] No Supabase credentials/runtime dependency.
- [ ] No Auth.js/NextAuth runtime dependency.
- [ ] No OpenRouter API key in APK.
- [ ] No service-account key in APK/repository.
- [ ] App Check enforced in production.
- [ ] Firestore rules deny unauthenticated access.
- [ ] Cross-user reads/writes tested and denied.
- [ ] Production financial notification contents are not logged.
- [ ] Secrets stored in Firebase Secret Manager.
- [ ] Release build uses proper signing configuration.
- [ ] Debug/test endpoints are disabled from release builds.

---

# 5. Authentication Requirements

## First launch

1. Onboarding.
2. Firebase authentication.
3. Create/load user profile.
4. Initialize local user database state.
5. Restore cloud data.
6. Continue to Home.

## Session rules

- Firebase Auth state is the single source of authenticated identity.
- Persist only the minimum required local session state.
- On logout, stop user-specific background work where possible.
- Clear/invalidate the previous user's local data boundary before another account is loaded.
- A failed network connection must not create a fake authenticated session.

## Acceptance tests

- New user can sign up/sign in.
- Existing user can restore a session after app restart.
- Expired/revoked sessions are handled.
- User A cannot access User B's Firestore data.
- Logout followed by another login never displays the previous account's data.

---

# 6. Transaction Ledger — Production Contract

Use integer minor units (`Long`) for money.

Required transaction fields:

```text
id
walletId
amountMinor
currency
transactionType
category
title
merchant
source
sourceEventId
fingerprint
occurredAt
createdAt
updatedAt
confidence
confirmationState
syncState
```

Supported transaction types:

- INCOME
- EXPENSE
- TRANSFER
- ADJUSTMENT

Transfers must not count as spending.

### Ledger invariants

1. One source event can create at most one transaction.
2. Editing a transaction updates all derived aggregates.
3. Deleting a transaction updates all derived aggregates.
4. Provider balance is never treated as a second transaction.
5. AI cannot bypass transaction validation.
6. Historical transactions are immutable with respect to parser-version changes; editing creates an explicit user action.

---

# 7. Automatic Notification Detection

## Pipeline

```text
Android notification
      |
Normalize
      |
Identify supported provider
      |
Provider parser
      |
Validate amount/type/time
      |
Fingerprint
      |
Deduplicate
      |
Confidence
  /      |      \
High   Medium    Low
 |       |         |
auto   pending    ignore/review
record confirmation
 |
Atomic Room transaction + detection event
 |
Recalculate deterministic balance/budget
 |
Queue sync
```

## Provider support

Initial production providers should be limited to adapters that can be tested with real notification samples.

Do not add a provider merely because a class exists in the repository.

Each adapter requires:

- known Android package names;
- sample notification fixtures;
- parser tests;
- amount extraction tests;
- income/expense classification tests;
- fingerprint tests;
- duplicate tests;
- malformed-notification tests.

Unsupported notifications are ignored safely.

---

# 8. Sync Architecture

Room remains the **local source of truth for immediate UI/offline behavior**.

Firestore is the cloud recovery/cross-device source.

```text
Local mutation
    |
Room transaction
    |
SyncQueue
    |
WorkManager
    |
Firestore
    |
Acknowledgement
    |
Mark synced
```

### Required sync properties

- persistent queue;
- retry with exponential backoff;
- network constraints;
- idempotent operations;
- stable IDs;
- tombstones for deletions where necessary;
- deterministic conflict resolution;
- no data loss on app restart;
- no infinite retry loop;
- sync status visible to the user when relevant.

### Conflict rule

For ordinary profile/settings entities, latest server/local version can be resolved deterministically.

For financial transactions, do not silently overwrite conflicting records. Preserve the conflict and reconcile explicitly.

### Wallet/budget requirement

Wallets and budgets must use the same production sync mechanism as transactions/goals. They must not have a separate ad-hoc synchronization path with hard-coded `user_default` behavior.

---

# 9. Wallets / E-wallets

The wallet model should represent an account tracked by ViNote, not a credential-connected payment account.

Fields:

```text
id
name
type
calculatedBalance
providerReportedBalance?
openingBalance
currency
isAutoDetectEnabled
isEnabled
lastDetectedAt?
lastReconciledAt?
```

### User actions

- Add wallet.
- Edit wallet.
- Set opening/current balance.
- Enable notification detection.
- Disable detection.
- Reconcile balance.
- View calculation history.

### Remove from UI/code

- OTP login/linking for DANA/GoPay/OVO.
- Fake "connected account" claims.
- Credential fields for third-party wallets.
- Unsupported real-time balance promises.

---

# 10. Home Dashboard

Home must be derived from real Room data.

Required:

- total tracked balance;
- today's spending;
- budget progress;
- recent transactions;
- pending detections;
- wallet status;
- sync status when relevant.

Do not ship placeholder financial numbers such as fake balances or demo transactions.

If there is no data, show an intentional empty state and onboarding action.

---

# 11. Budgets and Goals

## Budget

Support:

- monthly limit;
- daily limit;
- warning threshold;
- category limits if implemented reliably;
- current period;
- deterministic spending calculation;
- warning notifications.

Budget calculations must use the ledger only.

## Goals

Support:

- target amount;
- current amount;
- deadline;
- contribution;
- progress;
- archive/delete.

Goal progress must never depend on an LLM response.

---

# 12. NoTa Hybrid AI

The product keeps **Hybrid AI** as a differentiating capability, but production behavior must be graceful when AI is unavailable.

## Online AI

```text
Android
  -> Firebase Callable Function
      -> OpenRouter
          -> approved model
```

Use AI for:

- spending explanations;
- summaries;
- financial education;
- category suggestions;
- transaction drafts;
- goal drafts;
- conversational guidance.

## Offline AI

An on-device model may be offered only when the model is successfully downloaded/loaded and device capability is sufficient.

Do not make a specific local model a hard dependency for app startup.

If offline AI is unavailable:

```text
Offline deterministic finance features -> continue normally
Offline AI -> show unavailable state
```

### AI data minimization

Only send the minimum transaction context required for the requested answer.

Do not send raw notification histories by default.

Never send passwords, OTPs, authentication tokens, or third-party credentials to AI.

---

# 13. Receipt OCR

Receipt processing remains a useful offline-first feature.

```text
Camera
 -> OCR
 -> extracted candidates
 -> validation
 -> editable draft
 -> user confirmation
 -> transaction
```

OCR must never automatically create a financial transaction without confirmation unless the input is already a trusted structured source.

Required states:

- camera unavailable;
- permission denied;
- OCR failed;
- low confidence;
- partial extraction;
- successful editable draft.

---

# 14. Voice Input

Voice is an input convenience, not an authoritative parser.

```text
Speech
 -> transcript
 -> deterministic extraction / AI suggestion
 -> editable draft
 -> confirmation
 -> transaction
```

If speech recognition is unavailable or permission is denied, manual input remains available.

---

# 15. Notifications and Permissions

Permissions are contextual and explain why they are required.

Relevant permissions may include:

- notification listener access;
- POST_NOTIFICATIONS;
- camera;
- microphone.

Every denied permission requires a recovery route to Android Settings.

### Notification policy

ViNote may notify users about:

- successfully recorded transactions;
- pending detections;
- budget warnings;
- goal progress;
- sync problems that require action.

Notifications must be configurable and non-spammy.

---

# 16. Recurring Transactions

Use WorkManager for scheduled/background work.

Do not promise exact execution time because Android background scheduling is not real-time.

Recurring transaction generation must be:

- idempotent;
- date-aware;
- timezone-aware;
- recoverable after missed execution;
- tested across app restarts.

---

# 17. Production UX States

Every important screen must support:

- loading;
- success;
- empty;
- offline;
- error;
- permission denied;
- retry;
- stale data;
- sync pending;
- sync failed.

No screen may silently show zero as if it were a real financial value when the data has not loaded.

---

# 18. What Is Explicitly Removed / Deferred

The following are **not production requirements** for the first release:

| Feature | Decision | Reason |
|---|---|---|
| Supabase Auth | Remove | Conflicts with Firebase-only architecture |
| Auth.js / NextAuth | Remove | Unnecessary second authentication stack |
| Supabase Edge Functions | Remove | Firebase Functions is the production serverless layer |
| Direct OpenRouter key in APK | Prohibited | Secret exposure |
| DANA/GoPay/OVO OTP linking | Remove | Not a reliable/official integration model |
| Unofficial e-wallet APIs | Remove from default production path | Fragile, potentially blocked/changed, and not a dependable product contract |
| Guaranteed real-time e-wallet balance | Remove claim | Notification access cannot guarantee provider balance |
| Arbitrary bank/e-wallet credential scraping | Prohibited | Security, privacy, and reliability risk |
| Exact background execution times | Remove claim | Android scheduling is best-effort |
| AI as ledger authority | Prohibited | Financial state must be deterministic |
| Offline AI as mandatory | Defer/optional | Device/model availability varies |
| Large feature expansion before core stability | Defer | Production reliability has priority |

---

# 19. Refactoring Requirements

## Phase A — Architecture cleanup

- [ ] Remove Supabase Auth runtime dependency.
- [ ] Remove Auth.js/NextAuth runtime abstractions.
- [ ] Introduce `FirebaseAuthRepository`.
- [ ] Make Firebase UID canonical everywhere.
- [ ] Remove `user_default` and generated fake user IDs from production paths.
- [ ] Remove Supabase URL/configuration from gateway construction.
- [ ] Replace Supabase Edge Function OpenRouter routing with Firebase callable function.
- [ ] Make OpenRouter model selection server-controlled/allowlisted.
- [ ] Enable App Check in production.

## Phase B — Domain correctness

- [ ] Define one authoritative transaction creation use case.
- [ ] Make transaction writes atomic with detection-event deduplication.
- [ ] Ensure balance calculation has one definition.
- [ ] Separate calculated/provider/manual balance.
- [ ] Remove duplicate wallet-processing instances.
- [ ] Make notification coordinator application-scoped and injected.
- [ ] Make all financial calculations use `Long` minor units.

## Phase C — Sync

- [ ] Unify transaction/wallet/budget/goal sync.
- [ ] Make SyncQueue persistent and idempotent.
- [ ] Add WorkManager sync worker.
- [ ] Implement retry/backoff.
- [ ] Handle deletes/tombstones.
- [ ] Add conflict tests.
- [ ] Verify cross-device recovery.

## Phase D — E-wallet realism

- [ ] Delete OTP linking UI/state.
- [ ] Delete unofficial login/session handling.
- [ ] Keep provider selection only as a notification-detection configuration.
- [ ] Rename "connected" terminology to "tracking enabled" where applicable.
- [ ] Implement manual balance reconciliation.
- [ ] Only show provider-reported balance when a trustworthy source actually provides it.

## Phase E — ViewModel and DI cleanup

- [ ] Inject repositories/services with Hilt.
- [ ] Reduce `ViNoteViewModel` responsibilities.
- [ ] Remove infrastructure object construction from ViewModels.
- [ ] Ensure singleton/application-scoped services are not duplicated.
- [ ] Keep UI state in ViewModels and business logic in domain services.

---

# 20. Testing Requirements

## Unit tests — mandatory

### Authentication

- signed-out state;
- signed-in state;
- session restoration;
- logout;
- account switching.

### Detection

- DANA fixtures;
- GoPay fixtures;
- supported provider identification;
- amount parsing;
- income/expense classification;
- malformed notification;
- duplicate event;
- same notification processed twice;
- process restart recovery.

### Finance

- opening balance;
- income;
- expense;
- transfer;
- adjustment;
- edit;
- delete;
- budget calculation;
- goal progress;
- reconciliation.

### Sync

- offline queue;
- successful push;
- retry;
- duplicate push;
- deletion;
- conflict;
- user isolation.

### AI

- unavailable AI;
- timeout;
- malformed structured response;
- valid draft;
- prompt-size limits;
- no direct ledger mutation.

## Instrumented tests

- Firebase Auth flow;
- Firestore emulator rules;
- App Check-compatible environment where practical;
- Room + Firestore sync;
- notification listener integration;
- logout/account-switch isolation.

## Real-device acceptance

Test at minimum:

- Android 10+ supported device range;
- screen locked;
- Activity closed;
- app process recreated;
- device reboot;
- notification access revoked;
- POST_NOTIFICATIONS denied;
- offline -> online recovery;
- battery optimization enabled;
- battery optimization disabled;
- low-storage condition;
- camera permission denied;
- microphone permission denied.

---

# 21. CI/CD and Release Gates

Every production PR must pass:

```text
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
```

Where configured, also run:

```text
./gradlew connectedCheck
```

Firebase Functions must pass its own install/lint/test/build pipeline.

### Release gate

A release cannot ship if any of the following remain:

- fake authentication;
- `user_default` production data path;
- client-side OpenRouter secret;
- unauthenticated private Firestore access;
- cross-user data access;
- duplicate financial ledger entries;
- wallet balance double-counting;
- broken offline recovery;
- production crash in notification listener path;
- unfinished OTP/unofficial integration presented as working;
- fake financial dashboard values.

---

# 22. Observability

Use privacy-safe diagnostics only.

Record technical metadata such as:

- app version;
- sync failure category;
- detection parser version;
- detection success/failure counts without raw financial text;
- AI latency/error category;
- crash reports.

Do not record:

- raw financial notification contents;
- passwords;
- OTPs;
- authentication tokens;
- full transaction histories in logs;
- raw AI conversations in production logs.

---

# 23. Production Definition of Done

ViNote-2 is considered production-ready only when all of the following are true:

### Architecture

- [ ] Firebase is the sole backend/auth stack.
- [ ] Room is the offline/local source of truth.
- [ ] WorkManager handles durable background sync.
- [ ] Hilt manages production dependencies.
- [ ] No duplicate competing service instances exist.

### Authentication

- [ ] Real Firebase authentication works.
- [ ] No fake user IDs exist in production paths.
- [ ] Account switching is isolated.

### Finance

- [ ] Ledger arithmetic is deterministic.
- [ ] Money uses integer minor units.
- [ ] Notification duplicates cannot create duplicate transactions.
- [ ] Transfers do not count as expenses.
- [ ] Wallet balances reconcile correctly.

### Cloud

- [ ] Transactions, wallets, budgets, goals, and profiles sync.
- [ ] Offline writes recover after reconnection.
- [ ] Firestore rules enforce user ownership.
- [ ] Cross-user access tests fail as expected.

### E-wallets

- [ ] No unofficial credential/OTP login is required.
- [ ] Provider tracking is based on permitted notifications or legitimate integrations.
- [ ] Balance labels accurately describe calculated/provider/manual values.

### AI

- [ ] OpenRouter key is server-side only.
- [ ] Firebase App Check is enforced.
- [ ] AI failures do not break finance functionality.
- [ ] AI cannot directly mutate financial state without explicit validated user confirmation.

### UX

- [ ] Loading/empty/error/offline/sync states are implemented.
- [ ] Permission denial has recovery paths.
- [ ] No fake production financial values remain.

### Release

- [ ] Debug secrets/configurations are excluded from release.
- [ ] Release build installs and starts on a clean device.
- [ ] Core flows pass real-device acceptance tests.
- [ ] Crash-free smoke test completed.

---

# 24. Implementation Order

Do **not** implement features randomly. Work in this order:

```text
1. Architecture cleanup
       ↓
2. Firebase Auth migration
       ↓
3. Firestore security + schema
       ↓
4. Room/local ledger correctness
       ↓
5. Unified sync queue + WorkManager
       ↓
6. Notification detection reliability
       ↓
7. Wallet/budget reconciliation
       ↓
8. Remove unrealistic OTP/unofficial integrations
       ↓
9. AI gateway + App Check + quotas
       ↓
10. OCR/voice hardening
       ↓
11. UI production states
       ↓
12. Automated tests
       ↓
13. Real-device acceptance
       ↓
14. Release build + final security audit
```

**Rule:** If a feature cannot meet the production contract, remove/defer it instead of creating a fake implementation.

---

# 25. Final Product Contract

ViNote-2 should make one promise it can actually keep:

> **ViNote records the money you authorize or that it can reliably detect, keeps your local ledger consistent, syncs it securely to your account, and uses AI to help you understand it — without pretending to have access to financial systems it cannot legitimately access.**

That principle takes priority over feature count.
