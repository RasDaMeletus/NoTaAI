package com.vinote.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinote.core.ai.OpenRouterMessage
import com.vinote.data.supabase.SupabaseClientProvider
import com.vinote.data.repository.AuthRepository
import com.vinote.data.repository.AuthRepositoryImpl
import com.vinote.data.local.entity.UserSession
import com.vinote.domain.model.AuthResult
import com.vinote.data.engine.ExtractedReceiptData
import com.vinote.data.engine.ExtractedVoiceEntity
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.local.NoTaDatabase
import com.vinote.data.local.entities.BudgetEntity
import com.vinote.data.local.entities.DetectionEventEntity
import com.vinote.data.local.entities.DetectionStatus
import com.vinote.data.local.entities.WalletAccountEntity
import com.vinote.data.local.entities.WalletType
import com.vinote.data.model.Achievement
import com.vinote.data.model.BankAccountItem
import com.vinote.data.model.BudgetAlertState
import com.vinote.data.model.ChatMessage
import com.vinote.data.model.ConnectedWallet
import com.vinote.data.model.GoalItem
import com.vinote.data.model.NotaAccessory
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaConfig
import com.vinote.data.model.NotaEyeState
import com.vinote.data.model.NotaPresenceMode
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.data.model.UserProfile
import com.vinote.data.model.EwalletLinkingState
import com.vinote.data.repository.WalletGatewayRepository
import com.vinote.data.gateway.BalanceFetchResult
import com.vinote.data.gateway.MidtransGatewayService
import com.vinote.data.gateway.EwalletCatalog
import com.vinote.data.gateway.PaymentGatewayService
import com.vinote.data.gateway.UnofficialDanaService
import com.vinote.data.gateway.UnofficialGoPayService
import com.vinote.data.gateway.UnofficialOvoService
import com.vinote.data.repository.SyncStatus
import com.vinote.data.repository.NoTaRepository
import com.vinote.data.sync.CloudSyncStatus
import com.vinote.data.sync.SyncSummary
import com.vinote.data.sync.NoTaCloudSynchronizer
import com.vinote.domain.ai.AiAction
import com.vinote.domain.ai.AiIntent
import com.vinote.domain.ai.AiModelConfig
import com.vinote.domain.ai.NoTaFinanceTools
import com.vinote.domain.ai.NoTaAiService
import com.vinote.domain.export.TransactionExportService
import com.vinote.domain.finance.FinancialAnalyticsService
import com.vinote.domain.finance.FinancialHealthReport
import com.vinote.domain.finance.FinancialHealthScore
import com.vinote.domain.finance.SpendingTrendReport
import com.vinote.data.local.entities.RecurringTransactionEntity
import com.vinote.data.local.entities.RecurringFrequency
import com.vinote.data.local.entities.AchievementEntity
import com.vinote.data.local.entities.MerchantEmbeddingEntity
import com.vinote.data.local.entities.TransactionCategoryEntity
import com.vinote.data.local.entities.SpendingPredictionEntity
import com.vinote.data.local.entities.TransactionTemplateEntity
import com.vinote.domain.gamification.AchievementManager
import com.vinote.domain.finance.SpendingPredictionEngine
import com.vinote.domain.finance.SpendingPredictionResult
import com.vinote.domain.mascot.MascotQuote
import com.vinote.domain.mascot.NotaQuotesRepository
import com.vinote.domain.statement.BankStatementParser
import com.vinote.services.recurring.RecurringTransactionScheduler
import java.io.File
import com.vinote.domain.notification.FinancialEventType
import com.vinote.domain.notification.FinancialNotificationEngine
import com.vinote.domain.transaction.TransactionService
import com.vinote.domain.wallet.WalletNotification
import com.vinote.services.ai.AiEngineStatus
import com.vinote.services.ai.HybridAiProcessor
import com.vinote.services.media.AudioSpeechRecorderService
import com.vinote.services.media.ReceiptImageProcessor
import com.vinote.services.wallet.WalletDeduplicationService
import com.vinote.services.wallet.WalletDetectionCoordinator
import com.vinote.services.wallet.WalletNotificationListenerService
import com.vinote.services.wallet.WalletTransactionProcessor
import com.vinote.ui.components.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

enum class ActivityFilter {
    ALL,
    INCOME,
    EXPENSE
}

class ViNoteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NoTaDatabase.getDatabase(application)
    val authRepository: AuthRepository = AuthRepositoryImpl(SupabaseClientProvider(application), application)

    private val cloudSynchronizer = NoTaCloudSynchronizer(
        transactionDao = database.transactionDao(),
        goalDao = database.goalDao(),
        walletAccountDao = database.walletAccountDao(),
        budgetDao = database.budgetDao(),
        syncQueueDao = database.syncQueueDao(),
        authRepository = authRepository,
        supabaseClientProvider = SupabaseClientProvider(application),
        scope = viewModelScope
    )

    private val repository = NoTaRepository(
        transactionDao = database.transactionDao(),
        goalDao = database.goalDao(),
        userSessionDao = database.userSessionDao(),
        walletAccountDao = database.walletAccountDao(),
        detectionEventDao = database.detectionEventDao(),
        syncQueueDao = database.syncQueueDao(),
        budgetDao = database.budgetDao(),
        authRepository = authRepository,
        cloudSynchronizer = cloudSynchronizer
    )

    // Domain Services & Hardened Detection Coordinator
    val notificationEngine = FinancialNotificationEngine(application)
    val transactionService = TransactionService(
        transactionDao = database.transactionDao(),
        notificationEngine = notificationEngine,
        externalScope = viewModelScope
    )
    val aiService = NoTaAiService()
    val deduplicationService = WalletDeduplicationService()

    val transactionDao = database.transactionDao()
    val transactionTemplateDao = database.transactionTemplateDao()
    val recurringTransactionDao = database.recurringTransactionDao()
    val recurringScheduler = RecurringTransactionScheduler(
        recurringTransactionDao = recurringTransactionDao,
        transactionDao = database.transactionDao(),
        notificationEngine = notificationEngine
    )

    val detectionCoordinator = WalletDetectionCoordinator(
        transactionDao = database.transactionDao(),
        detectionEventDao = database.detectionEventDao(),
        walletAccountDao = database.walletAccountDao(),
        syncQueueDao = database.syncQueueDao(),
        aiService = aiService,
        notificationEngine = notificationEngine,
        scope = viewModelScope
    )

    val walletProcessor = WalletTransactionProcessor(
        transactionService = transactionService,
        deduplicationService = deduplicationService,
        aiService = aiService,
        scope = viewModelScope
    )

    val merchantEmbeddingDao = database.merchantEmbeddingDao()
    val achievementDao = database.achievementDao()
    val spendingPredictionDao = database.spendingPredictionDao()
    val achievementManager = AchievementManager(achievementDao)

    val financeTools = NoTaFinanceTools(
        transactionDao = database.transactionDao(),
        goalDao = database.goalDao(),
        walletAccountDao = database.walletAccountDao(),
        budgetDao = database.budgetDao(),
        recurringTransactionDao = recurringTransactionDao
    )

    // Auth / Session State (Auth.js)
    val currentSession: StateFlow<UserSession?> = authRepository.currentSession
    // Guest/offline sessions count as logged in: the app is fully usable
    // without a Supabase account. isAuthenticated distinguishes cloud-synced
    // from local-only, not "allowed in".
    val isLoggedIn: StateFlow<Boolean> = authRepository.currentSession.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Hybrid AI Processor (OpenRouter Online AI + On-Device Neural Engine)
        val hybridAiProcessor = HybridAiProcessor(application)
        val aiEngineStatus: StateFlow<AiEngineStatus> = hybridAiProcessor.engineStatus
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiEngineStatus())

    // AI Model Configuration
    private val _aiConfig = MutableStateFlow(AiModelConfig())
    val aiConfig = _aiConfig.asStateFlow()

    // Active User ID helper.
    // Must NEVER throw during ViewModel construction: the app has to be able to
    // instantiate this ViewModel while logged out, otherwise the sign-in screen
    // itself can never appear. Callers that require a real id handle null.
    private val activeUserId: String?
        get() = try {
            authRepository.getCanonicalUserId()
        } catch (e: IllegalStateException) {
            null
        }

    // Must match the id used when a user is not signed in yet. Local-only
    // sessions (and the pre-login state) are scoped to this id so the UI can
    // render while waiting for Supabase Auth to complete.
    private val guestUserId: String = "guest"

    private val activeUserIdOrGuest: String get() = activeUserId ?: guestUserId

    // Every user-scoped Flow must switch when OAuth restores a different
    // identity. Capturing activeUserIdOrGuest during construction permanently
    // subscribed the UI to "guest", which made cloud-backed data look missing.
    private val activeUserIdFlow = authRepository.currentSession
        .map { it?.userId ?: guestUserId }

    val allTransactions: StateFlow<List<TransactionItem>> = activeUserIdFlow
        .flatMapLatest(transactionService::transactionsForUser)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReviewTransactions: StateFlow<List<TransactionItem>> = activeUserIdFlow
        .flatMapLatest(repository::getPendingTransactionsFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectionEvents: StateFlow<List<DetectionEventEntity>> = activeUserIdFlow
        .flatMapLatest(repository::getDetectionEventsFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletAccounts: StateFlow<List<WalletAccountEntity>> = activeUserIdFlow
        .flatMapLatest(repository::getWalletsFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<GoalItem>> = activeUserIdFlow
        .flatMapLatest(repository::getGoalsFlow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionTemplates: StateFlow<List<TransactionTemplateEntity>> = activeUserIdFlow
        .flatMapLatest(transactionTemplateDao::getTemplatesForUser)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = activeUserIdFlow
        .flatMapLatest(recurringTransactionDao::getRecurringForUser)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filter for Activity screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activityFilter = MutableStateFlow(ActivityFilter.ALL)
    val activityFilter = _activityFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        allTransactions,
        searchQuery,
        activityFilter
    ) { transactions, query, filter ->
        transactions.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.merchant.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ActivityFilter.ALL -> true
                ActivityFilter.INCOME -> item.type == TransactionType.INCOME
                ActivityFilter.EXPENSE -> item.type == TransactionType.EXPENSE
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Profile State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // Persist profile/budget settings that do not have dedicated Room columns.
    // Keys are scoped by user so OAuth and guest profiles never overwrite each other.
    private val profilePreferences = application.getSharedPreferences(
        "nota_profile_settings",
        Context.MODE_PRIVATE
    )

    private fun preferenceKey(userId: String, field: String) = "$userId:$field"

    private fun restorePersistedProfile(userId: String, persistedBudget: BudgetEntity?): UserProfile {
        val current = _userProfile.value
        return current.copy(
            monthlyIncome = persistedBudget?.monthlyLimit
                ?: profilePreferences.getLong(preferenceKey(userId, "monthly_income"), current.monthlyIncome),
            dailyBudgetLimit = persistedBudget?.dailyLimit
                ?: profilePreferences.getLong(preferenceKey(userId, "daily_budget"), current.dailyBudgetLimit),
            savingsTargetPercentage = profilePreferences.getInt(
                preferenceKey(userId, "savings_percentage"), current.savingsTargetPercentage
            ),
            currencyCode = profilePreferences.getString(
                preferenceKey(userId, "currency_code"), current.currencyCode
            ) ?: current.currencyCode,
            currencySymbol = profilePreferences.getString(
                preferenceKey(userId, "currency_symbol"), current.currencySymbol
            ) ?: current.currencySymbol,
            financialPersona = profilePreferences.getString(
                preferenceKey(userId, "financial_persona"), current.financialPersona
            ) ?: current.financialPersona,
            isBudgetAlertActive = profilePreferences.getBoolean(
                preferenceKey(userId, "budget_alert_active"), current.isBudgetAlertActive
            )
        )
    }

    private fun persistProfileSettings(userId: String, profile: UserProfile) {
        profilePreferences.edit()
            .putLong(preferenceKey(userId, "monthly_income"), profile.monthlyIncome)
            .putLong(preferenceKey(userId, "daily_budget"), profile.dailyBudgetLimit)
            .putInt(preferenceKey(userId, "savings_percentage"), profile.savingsTargetPercentage)
            .putString(preferenceKey(userId, "currency_code"), profile.currencyCode)
            .putString(preferenceKey(userId, "currency_symbol"), profile.currencySymbol)
            .putString(preferenceKey(userId, "financial_persona"), profile.financialPersona)
            .putBoolean(preferenceKey(userId, "budget_alert_active"), profile.isBudgetAlertActive)
            .apply()
    }

    // Financial Health Score Flow (Deterministic 0-100 Offline Engine)
    val financialHealthScore: StateFlow<FinancialHealthScore> = combine(
        allTransactions,
        allGoals,
        userProfile
    ) { transactions, goals, profile ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        FinancialAnalyticsService.calculateFinancialHealthScore(
            transactions = transactions,
            goals = goals,
            dailyLimit = profile.dailyBudgetLimit,
            monthlyIncome = profile.monthlyIncome,
            startOfDayMs = startOfDay
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialHealthScore(
            score = 0,
            grade = "-",
            savingsScore = 0,
            budgetScore = 0,
            goalScore = 0,
            consistencyScore = 0,
            balanceScore = 0,
            advice = "Catat transaksi untuk mulai memantau skor finansialmu! 💡"
        )
    )

    // Bank Accounts & E-Wallets (Display state synced reactively with Room)
    val bankAccounts: StateFlow<List<BankAccountItem>> = combine(
        walletAccounts,
        userProfile
    ) { entities, profile ->
        entities.map { entity ->
            BankAccountItem(
                id = entity.id,
                bankName = entity.name,
                accountNumber = entity.accountNumber.ifBlank { "•••• " + entity.id.takeLast(4) },
                accountHolder = if (profile.fullName.isNotBlank() && profile.fullName != "NoTa User") profile.fullName.uppercase() else "",
                balance = entity.calculatedBalance,
                isConnected = entity.isConnected,
                isAutoSync = entity.isAutoDetectEnabled,
                lastSyncedTime = if (entity.isConnected) "Synced" else "Never",
                brandColorHex = entity.iconColorHex,
                bankType = if (entity.type == WalletType.BANK) "Bank" else "E-Wallet"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Exceeded Furious Alert State
    private val _budgetAlertState = MutableStateFlow(BudgetAlertState())
    val budgetAlertState = _budgetAlertState.asStateFlow()

    // Total Aggregated Bank Balance
    val totalBankBalance: StateFlow<Long> = bankAccounts.map { list ->
        list.filter { it.isConnected }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Today's Spent Aggregation
    val todaySpent: StateFlow<Long> = allTransactions.map { list ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        FinancialAnalyticsService.calculateSpentToday(list, startOfDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Dynamic Calculated Net Balance
    val currentCalculatedBalance: StateFlow<Long> = allTransactions.map { list ->
        FinancialAnalyticsService.calculateNetBalance(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Dynamic Mandatory Savings calculated from user goals and savings target percentage
    val mandatorySavings: StateFlow<Long> = combine(
            allGoals,
            userProfile
        ) { goals, profile ->
            val goalsAllocation = goals.filter { it.currentAmount < it.targetAmount }.sumOf { it.targetAmount - it.currentAmount }.coerceAtLeast(0L)
            val targetSavingsFromIncome = (profile.monthlyIncome * (profile.savingsTargetPercentage.toDouble() / 100.0)).toLong()
            if (goalsAllocation > 0) goalsAllocation else targetSavingsFromIncome
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Dynamic Safe Money calculated from Net Balance minus Mandatory Savings and Today's Spent
    val safeMoney: StateFlow<Long> = combine(
        currentCalculatedBalance,
        mandatorySavings,
        todaySpent
    ) { netBalance, mandatory, spent ->
        (netBalance - mandatory - spent).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Spending Trends Flow calculated from real database transactions
    val spendingTrends: StateFlow<SpendingTrendReport> = combine(
        allTransactions,
        userProfile
    ) { transactions, profile ->
        FinancialAnalyticsService.calculateSpendingTrends(
            transactions = transactions,
            dailyLimit = profile.dailyBudgetLimit
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialAnalyticsService.calculateSpendingTrends(emptyList(), 0L)
    )

    // Connected Wallets directly mapped from Room
    val wallets: StateFlow<List<ConnectedWallet>> = walletAccounts.map { entities ->
        entities.filter { it.type == WalletType.EWALLET }.map { entity ->
            ConnectedWallet(
                id = entity.id,
                name = entity.name,
                isConnected = entity.isConnected,
                isActiveSync = entity.isAutoDetectEnabled,
                iconColorHex = entity.iconColorHex,
                description = if (entity.isAutoDetectEnabled) "Active Sync" else "Manual"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDetectionActive = MutableStateFlow(true)
    val isDetectionActive = _isDetectionActive.asStateFlow()

    // Cloud Sync State
    val syncSummary: StateFlow<SyncSummary> = cloudSynchronizer.syncState
    val syncStatus: StateFlow<SyncStatus> = syncSummary.map {
        when (it.status) {
            CloudSyncStatus.IDLE -> SyncStatus.IDLE
            CloudSyncStatus.SYNCING -> SyncStatus.SYNCING
            CloudSyncStatus.SYNCED -> SyncStatus.SUCCESS
            CloudSyncStatus.PENDING -> SyncStatus.IDLE
            CloudSyncStatus.ERROR, CloudSyncStatus.OFFLINE -> SyncStatus.ERROR
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatus.IDLE)

    // Nota Configuration State
    private val _notaConfig = MutableStateFlow(
        NotaConfig(
            baseColor = NotaBaseColor.SOFT_PINK,
            accessory = NotaAccessory.NONE,
            personalitySlider = 75f,
            presenceMode = NotaPresenceMode.ALWAYS_VISIBLE,
            offlineAiEngineDownloaded = true,
            eyeState = NotaEyeState.HAPPY
        )
    )
    val notaConfig = _notaConfig.asStateFlow()

    // Real Achievements from Room Database (PRD Section 2.7 & 8)
    val achievements: StateFlow<List<AchievementEntity>> = achievementDao.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedAchievementsCount: StateFlow<Int> = achievementDao.getUnlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Spending Prediction Flow (PRD Section 1.1)
    val spendingPrediction: StateFlow<SpendingPredictionResult> = allTransactions.map { txs ->
        SpendingPredictionEngine.predictSpending(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpendingPredictionEngine.predictSpending(emptyList()))

    // Privacy & Security Controls (PRD Section 4.1)
    private val _isPrivacyModeEnabled = MutableStateFlow(false)
    val isPrivacyModeEnabled = _isPrivacyModeEnabled.asStateFlow()

    private val _isScreenCapturePrevented = MutableStateFlow(false)
    val isScreenCapturePrevented = _isScreenCapturePrevented.asStateFlow()

    private val _isBiometricLockEnabled = MutableStateFlow(false)
    val isBiometricLockEnabled = _isBiometricLockEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked = _isAppUnlocked.asStateFlow()

    fun togglePrivacyMode() {
        _isPrivacyModeEnabled.value = !_isPrivacyModeEnabled.value
    }

    fun setScreenCapturePrevented(prevent: Boolean) {
        _isScreenCapturePrevented.value = prevent
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _isBiometricLockEnabled.value = enabled
        if (!enabled) _isAppUnlocked.value = true
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    // Dynamic Saving Streak (Calculated from distinct active transaction days)
    val savingStreakDays: StateFlow<Int> = allTransactions.map { transactions ->
        if (transactions.isEmpty()) return@map 0
        val days = transactions.map { tx ->
            Calendar.getInstance().apply {
                timeInMillis = tx.timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct().sortedDescending()
        if (days.isEmpty()) return@map 0
        var streak = 1
        val oneDayMs = 86_400_000L
        for (i in 0 until days.size - 1) {
            val diff = days[i] - days[i + 1]
            if (diff in 1..oneDayMs) {
                streak++
            } else {
                break
            }
        }
        streak
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Nota Mascot Quotes & Dynamic Dialogue System (PRD Section 2.7)
    private val _quoteIndex = MutableStateFlow(0)
    val notaQuote: StateFlow<MascotQuote> = combine(
        budgetAlertState,
        financialHealthScore,
        savingStreakDays,
        _quoteIndex
    ) { alert, health, streak, idx ->
        val savingsRate = (health.savingsScore.toDouble() / 30.0) * 100.0
        val isOver = alert.isTriggered && !alert.isDismissed
        NotaQuotesRepository.getQuoteForState(
            isOverBudget = isOver,
            savingsRate = savingsRate,
            streakDays = streak,
            selectedIndex = idx
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotaQuotesRepository.getQuoteForState(false, 0.0, 0, 0)
    )

    fun rotateNotaQuote() {
        _quoteIndex.value = _quoteIndex.value + 1
    }

    // Chat with Nota
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "Hey! What are we figuring out today?",
                isUser = false,
                quickChips = listOf("Saldo aku berapa?", "Uangku paling banyak habis buat apa?", "Help me save"),
                eyeState = NotaEyeState.CURIOUS
            )
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _isNotaTyping = MutableStateFlow(false)
    val isNotaTyping = _isNotaTyping.asStateFlow()

    // Pending Transaction for Confirmation Sheet
    private val _pendingTransaction = MutableStateFlow<TransactionItem?>(null)
    val pendingTransaction = _pendingTransaction.asStateFlow()

    // Keypad Input for Add Transaction
    private val _keypadAmount = MutableStateFlow("0")
    val keypadAmount = _keypadAmount.asStateFlow()

    val speechRecorderService = AudioSpeechRecorderService(application)
    val audioRmsDb: StateFlow<Float> = speechRecorderService.audioRmsDb

    // Voice recognition live text & NLP extraction
    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript = _voiceTranscript.asStateFlow()

    private val _parsedVoiceEntity = MutableStateFlow<ExtractedVoiceEntity?>(null)
    val parsedVoiceEntity = _parsedVoiceEntity.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening = _isVoiceListening.asStateFlow()

    // Scan OCR status & extracted receipt
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _extractedReceiptData = MutableStateFlow<ExtractedReceiptData?>(null)
    val extractedReceiptData = _extractedReceiptData.asStateFlow()

    // Custom categories (Income/Expense) from Room DB
    private val _customCategories = MutableStateFlow<List<TransactionCategoryEntity>>(emptyList())
    val customCategories = _customCategories.asStateFlow()

    // Notification banner state
    private val _bannerNotification = MutableStateFlow<String?>(null)
    val bannerNotification = _bannerNotification.asStateFlow()

    // Selected Transaction for Detail / Delete Modal
    private val _selectedTransactionDetail = MutableStateFlow<TransactionItem?>(null)
    val selectedTransactionDetail = _selectedTransactionDetail.asStateFlow()

    // Computed Constants
    val dailyLimit: Long get() = _userProfile.value.dailyBudgetLimit

    private fun currentBudgetPeriod(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    private suspend fun persistDailyBudget(
        userId: String,
        dailyLimit: Long,
        monthlyLimit: Long = _userProfile.value.monthlyIncome
    ) {
        if (userId.isBlank() || dailyLimit <= 0L) return
        val budgetDao = database.budgetDao()
        val existing = budgetDao.getBudget(userId)
        budgetDao.saveBudget(
            (existing ?: BudgetEntity(
                id = "budget_$userId",
                userId = userId,
                periodMonthYear = currentBudgetPeriod()
            )).copy(
                dailyLimit = dailyLimit,
                monthlyLimit = monthlyLimit.coerceAtLeast(0L),
                periodMonthYear = currentBudgetPeriod(),
                updatedTimestamp = System.currentTimeMillis()
            )
        )
        profilePreferences.edit()
            .putLong(preferenceKey(userId, "daily_budget"), dailyLimit)
            .putLong(preferenceKey(userId, "monthly_income"), monthlyLimit.coerceAtLeast(0L))
            .apply()
    }

    init {
        // Wire OpenRouterClient with Supabase Edge Function Proxy URL
        val supabaseProvider = SupabaseClientProvider(application)
        if (supabaseProvider.isConfigured) {
            val proxyUrl = supabaseProvider.supabaseFunctionsUrl + "/openrouter-proxy"
            aiService.openRouterClient.configure(
                proxyBaseUrl = proxyUrl,
                accessTokenProvider = {
                    authRepository.supabaseSessionFlow.value?.accessToken
                }
            )
        }

        // Wire notification listener coordinator
        WalletNotificationListenerService.coordinator = detectionCoordinator
        WalletNotificationListenerService.activeUserId = activeUserIdOrGuest

        // Observe detection coordinator real-time alerts
        viewModelScope.launch {
            detectionCoordinator.detectionAlertFlow.collect { alert ->
                showBanner(alert.message)
                evaluateBudgetStatus()
            }
        }

        // Listen for internal financial engine events
        viewModelScope.launch {
            notificationEngine.events.collect { event ->
                when (event.type) {
                    FinancialEventType.BUDGET_WARNING -> {
                        evaluateBudgetStatus()
                    }
                    FinancialEventType.TRANSACTION_DETECTED -> {
                        showBanner("✨ ${event.title}: ${event.message}")
                        evaluateBudgetStatus()
                    }
                    FinancialEventType.GOAL_PROGRESS -> {
                        showBanner(event.message)
                    }
                    FinancialEventType.RECURRING_EXECUTED -> {
                        showBanner("🔁 ${event.title}: ${event.message}")
                        evaluateBudgetStatus()
                    }
                    FinancialEventType.PENDING_REVIEW -> {
                        showBanner("🔔 ${event.title}: ${event.message}")
                    }
                    else -> {}
                }
            }
        }

        // Sync user profile with session
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                if (session != null) {
                    WalletNotificationListenerService.activeUserId = session.userId
                    transactionService.setUserId(session.userId)
                    val persistedBudget = database.budgetDao().getBudget(session.userId)
                    _userProfile.value = restorePersistedProfile(session.userId, persistedBudget).copy(
                        fullName = session.name,
                        email = session.email,
                        avatarInitials = session.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                    )
                    if (session.isAuthenticated) {
                        cloudSynchronizer.requestSync()
                    }
                }
            }
        }

        // Check due recurring transactions
        checkAndProcessDueRecurring()

        // Seed and evaluate achievements + hydrate learned merchants
        viewModelScope.launch(Dispatchers.IO) {
            achievementManager.seedDefaultsIfNeeded()
            merchantEmbeddingDao.getAllLearnedMerchants().collect { list ->
                for (item in list) {
                    OfflineNlpEngine.registerLearnedMerchant(item.normalizedName, item.categoryId)
                }
            }
        }
        viewModelScope.launch {
            allTransactions.collect { txs ->
                achievementManager.evaluate(txs, allGoals.value) { unlocked ->
                    showBanner("🏆 Pencapaian Terbuka: ${unlocked.title}!")
                }
            }
        }

        // Observe custom categories from Room DB
        viewModelScope.launch {
            database.transactionCategoryDao().getAllCategories().collect { cats ->
                _customCategories.value = cats
            }
        }
    }

    fun learnMerchantCategory(merchantName: String, category: String) {
        if (merchantName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = merchantName.trim().lowercase()
            OfflineNlpEngine.registerLearnedMerchant(normalized, category)
            val updated = merchantEmbeddingDao.updateCategoryCorrection(merchantName, category, System.currentTimeMillis())
            if (updated == 0) {
                merchantEmbeddingDao.insertOrUpdate(
                    MerchantEmbeddingEntity(
                        merchantName = merchantName,
                        normalizedName = normalized,
                        categoryId = category,
                        confidence = 1.0f,
                        correctionCount = 1
                    )
                )
            }
        }
    }


    fun exportTransactionsToPdf(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val txs = allTransactions.value
            val totalIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val pdfFile = TransactionExportService.exportTransactionsToPdfFile(
                context = context,
                transactions = txs,
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
            val shareIntent = TransactionExportService.createPdfShareIntent(context, pdfFile)
            val chooser = android.content.Intent.createChooser(shareIntent, "Bagikan Laporan PDF NoTa").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    data class CsvImportSummary(
        val importedCount: Int,
        val skippedDuplicates: Int,
        val totalParsed: Int,
        val isSuccess: Boolean,
        val message: String
    )

    fun importTransactionsFromCsv(
        csvContent: String,
        onComplete: (CsvImportSummary) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val parseResult = BankStatementParser.parseCsv(csvContent, defaultUserId = activeUserIdOrGuest)
            if (parseResult.transactions.isEmpty()) {
                val summary = CsvImportSummary(
                    importedCount = 0,
                    skippedDuplicates = 0,
                    totalParsed = 0,
                    isSuccess = false,
                    message = "Tidak ada transaksi valid yang dapat diproses dari CSV."
                )
                withContext(Dispatchers.Main) {
                    showBanner(summary.message)
                    onComplete(summary)
                }
                return@launch
            }

            val existingList = transactionDao.getTransactionsForUser(activeUserIdOrGuest).first()
            val uniqueItems = BankStatementParser.filterDuplicates(parseResult.transactions, existingList)
            val duplicatesCount = parseResult.transactions.size - uniqueItems.size

            if (uniqueItems.isNotEmpty()) {
                transactionDao.insertAll(uniqueItems)
            }

            val msg = if (uniqueItems.isNotEmpty()) {
                "Berhasil mengimpor ${uniqueItems.size} transaksi! (${duplicatesCount} duplikat dilewati)"
            } else {
                "Semua transaksi (${duplicatesCount}) sudah ada di riwayat (duplikat)."
            }

            val summary = CsvImportSummary(
                importedCount = uniqueItems.size,
                skippedDuplicates = duplicatesCount,
                totalParsed = parseResult.transactions.size,
                isSuccess = true,
                message = msg
            )

            withContext(Dispatchers.Main) {
                showBanner(msg)
                onComplete(summary)
            }
        }
    }



    fun saveTransactionTemplate(
        name: String,
        amount: Long,
        category: String,
        type: TransactionType = TransactionType.EXPENSE,
        walletName: String? = null,
        colorHex: String = "#4F8CFF",
        iconName: String = "EditNote"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionTemplateDao.insertTemplate(
                TransactionTemplateEntity(
                    userId = activeUserIdOrGuest,
                    name = name,
                    amount = amount,
                    category = category,
                    type = type,
                    walletName = walletName,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
            showBanner("Template '$name' berhasil disimpan! 📋")
        }
    }

    fun applyTemplate(template: TransactionTemplateEntity) {
        _keypadAmount.value = template.amount.toString()
        viewModelScope.launch(Dispatchers.IO) {
            transactionTemplateDao.incrementUsage(template.id)
        }
        showBanner("Template '${template.name}' diterapkan ✨")
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionTemplateDao.deleteById(id)
            showBanner("Template dihapus")
        }
    }

    fun saveRecurringTransaction(
        title: String,
        amount: Long,
        category: String,
        type: TransactionType = TransactionType.EXPENSE,
        walletName: String? = null,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        dayOfPeriod: Int = 1,
        nextDueDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringTransactionDao.insertRecurring(
                RecurringTransactionEntity(
                    userId = activeUserIdOrGuest,
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    walletName = walletName,
                    frequency = frequency,
                    dayOfPeriod = dayOfPeriod,
                    nextDueDate = nextDueDate
                )
            )
            showBanner("Transaksi rutin '$title' berhasil dijadwalkan! 🔁")
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringTransactionDao.deleteById(id)
            showBanner("Jadwal transaksi rutin dihapus")
        }
    }

    fun checkAndProcessDueRecurring() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = recurringScheduler.processDueTransactions(activeUserIdOrGuest)
            if (count > 0) {
                showBanner("$count transaksi rutin jatuh tempo telah dieksekusi otomatis 🔁")
            }
        }
    }

    fun exportTransactionsCsv(context: Context): File {
        return TransactionExportService.exportTransactionsToCsvFile(context, allTransactions.value)
    }

    // OpenRouter AI Config setters
    fun setOpenRouterApiKey(key: String) {
        _aiConfig.value = _aiConfig.value.copy(apiKey = key)
        // API key is now handled server-side via Supabase Edge Functions; kept for config persistence only
        showBanner(if (key.isNotBlank()) "OpenRouter API Key saved (server-side) 🤖" else "OpenRouter key cleared")
    }

    fun setOpenRouterModel(model: String) {
        _aiConfig.value = _aiConfig.value.copy(selectedModel = model)
        aiService.updateModel(model)
        showBanner("AI Model set to $model")
    }

    fun toggleOnlineAi(enabled: Boolean) {
        _aiConfig.value = _aiConfig.value.copy(isOnlineAiEnabled = enabled)
        showBanner(if (enabled) "Online AI Assistant Enabled" else "Offline AI Mode Enabled")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActivityFilter(filter: ActivityFilter) {
        _activityFilter.value = filter
    }

    fun toggleDetection(enabled: Boolean) {
        _isDetectionActive.value = enabled
        WalletNotificationListenerService.isListenerActive = enabled
        showBanner(if (enabled) "Automatic E-Wallet detection active" else "Automatic detection paused")
    }

    fun toggleWalletSync(walletId: String) {
        viewModelScope.launch {
            val wallet = walletAccounts.value.find { it.id == walletId }
            if (wallet != null) {
                val updated = wallet.copy(
                    isConnected = !wallet.isConnected,
                    isAutoDetectEnabled = !wallet.isConnected
                )
                repository.updateWallet(updated)
                showBanner(if (updated.isConnected) "${updated.name} connected ✨" else "${updated.name} disconnected")
            }
        }
    }

    fun toggleWalletAccountAutoDetect(walletId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleWalletAutoDetect(walletId, activeUserIdOrGuest, isEnabled)
            showBanner(if (isEnabled) "Auto-detection enabled for wallet" else "Auto-detection paused for wallet")
        }
    }

    fun reconcileWalletAccount(walletId: String, reconciledBalance: Long) {
        viewModelScope.launch {
            repository.reconcileWalletBalance(walletId, activeUserIdOrGuest, reconciledBalance)
            showBanner("Wallet balance reconciled to ${FormatUtils.formatRupiah(reconciledBalance)} 💳")
        }
    }

    fun updateNotaBaseColor(color: NotaBaseColor) {
        _notaConfig.value = _notaConfig.value.copy(baseColor = color)
    }

    fun updateNotaAccessory(accessory: NotaAccessory) {
        _notaConfig.value = _notaConfig.value.copy(accessory = accessory)
    }

    fun updateNotaPersonality(sliderValue: Float) {
        val eye = when {
            sliderValue > 66f -> NotaEyeState.EXCITED
            sliderValue > 33f -> NotaEyeState.HAPPY
            else -> NotaEyeState.NEUTRAL
        }
        _notaConfig.value = _notaConfig.value.copy(
            personalitySlider = sliderValue,
            eyeState = eye
        )
    }

    fun updateNotaPresence(mode: NotaPresenceMode) {
        _notaConfig.value = _notaConfig.value.copy(presenceMode = mode)
    }

    fun toggleOfflineAi(enabled: Boolean) {
        _notaConfig.value = _notaConfig.value.copy(offlineAiEngineDownloaded = enabled)
        showBanner(if (enabled) "Offline On-Device AI Activated (No Cloud Latency)" else "Cloud Fallback Active")
    }

    // Keypad Logic
    fun appendKeypadDigit(digit: String) {
        if (_keypadAmount.value == "0" && digit != "000") {
            _keypadAmount.value = digit
        } else if (_keypadAmount.value != "0" && _keypadAmount.value.length < 10) {
            _keypadAmount.value += digit
        }
    }

    fun deleteKeypadDigit() {
        if (_keypadAmount.value.length > 1) {
            _keypadAmount.value = _keypadAmount.value.dropLast(1)
        } else {
            _keypadAmount.value = "0"
        }
    }

    fun clearKeypad() {
        _keypadAmount.value = "0"
    }

    fun addCustomCategory(name: String, type: TransactionType) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCategory = TransactionCategoryEntity(
                userId = activeUserIdOrGuest,
                name = name,
                type = type,
                isCustom = true
            )
            database.transactionCategoryDao().insertCategory(newCategory)
        }
    }

    fun deleteCustomCategory(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.transactionCategoryDao().deleteCategory(categoryId)
        }
    }

    fun preparePendingTransactionFromKeypad(
            category: String = "Food",
            title: String = "Expense",
            type: TransactionType = TransactionType.EXPENSE
        ) {
            val amount = _keypadAmount.value.toLongOrNull() ?: 0L
            if (amount > 0) {
                _pendingTransaction.value = TransactionItem(
                    userId = activeUserIdOrGuest,
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    merchant = title,
                    source = TransactionSource.MANUAL,
                    timeLabel = "Just now"
                )
            }
        }

    fun setPendingTransaction(transaction: TransactionItem?) {
        _pendingTransaction.value = transaction
    }

    fun setWifiOnlyForCloud(enabled: Boolean) {
        hybridAiProcessor.setWifiOnlyPreference(enabled)
        showBanner(if (enabled) "Cloud AI restricted to Wi-Fi 📶" else "Cloud AI allowed on Cellular 🌐")
    }

    fun setForceOfflineMode(forced: Boolean) {
        hybridAiProcessor.setForceOfflineMode(forced)
        showBanner(if (forced) "Forced On-Device Offline Mode 🔒" else "Automatic Hybrid AI Routing Active ⚡")
    }

    // Authentication and Onboarding (Supabase Auth + CredentialManager)
    fun signInWithGoogleViaCredentialManager(
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authRepository.signInWithSupabaseGoogle()
            if (result.isSuccess) {
                showBanner("Signed in ✨")
                onSuccess()
            } else {
                showBanner("Sign-in note: Using secure local identity")
                authRepository.loginWithDirectProfile("user@vinote.local", "NoTa User")
                onSuccess()
            }
        }
    }

    fun login(email: String, pass: String): Boolean {
        val name = if (email.contains("@")) email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } else "NoTa User"
        viewModelScope.launch {
            authRepository.loginWithDirectProfile(email = email, provider = "credentials", name = name)
            showBanner("Welcome back, $name! ✨")
        }
        return true
    }

    fun loginWithGoogle(email: String = "user@vinote.local", name: String = "NoTa User") {
        viewModelScope.launch {
            authRepository.loginWithDirectProfile(email = email, provider = "google", name = name)
            showBanner("Signed in with Google as $name ✨")
        }
    }

    fun signup(fullName: String, email: String, pass: String): Boolean {
        viewModelScope.launch {
            authRepository.loginWithDirectProfile(email = email, provider = "credentials", name = fullName)
            showBanner("Account created successfully! 🎉")
        }
        return true
    }

    fun completeQuickSetup(
        userName: String = "",
        monthlyIncome: Long,
        dailyBudgetLimit: Long,
        savingsPercentage: Int,
        firstGoalTitle: String,
        firstGoalTarget: Long,
        startingColor: NotaBaseColor
    ) {
        val current = _userProfile.value
        val resolvedName = userName.trim().ifBlank { current.fullName.ifBlank { "NoTa User" } }
        val initials = resolvedName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "NU" }
        _userProfile.value = current.copy(
            fullName = resolvedName,
            avatarInitials = initials,
            monthlyIncome = monthlyIncome,
            dailyBudgetLimit = dailyBudgetLimit,
            savingsTargetPercentage = savingsPercentage
        )
        viewModelScope.launch {
            if (authRepository.getUserId() == null) {
                authRepository.loginWithDirectProfile(
                    email = current.email.ifBlank { "user@vinote.local" },
                    name = resolvedName,
                    provider = "guest"
                )
            }
            authRepository.getUserId()?.let { userId ->
                persistDailyBudget(userId, dailyBudgetLimit, monthlyIncome)
                persistProfileSettings(userId, _userProfile.value)
            }
        }
        _notaConfig.value = _notaConfig.value.copy(baseColor = startingColor, eyeState = NotaEyeState.HAPPY)
        if (firstGoalTitle.isNotBlank() && firstGoalTarget > 0) {
            createGoal(firstGoalTitle, firstGoalTarget, "In 3 months", "Savings")
        }
        showBanner("Quick setup complete! Welcome to NoTa 🚀")
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            showBanner("Logged out successfully")
        }
    }

    // Profile Settings updates
    fun updateProfile(
        fullName: String,
        email: String,
        phone: String,
        monthlyIncome: Long,
        dailyBudgetLimit: Long,
        savingsPercentage: Int,
        currencyCode: String,
        currencySymbol: String,
        persona: String,
        isBudgetAlertActive: Boolean
    ) {
        val initials = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        _userProfile.value = _userProfile.value.copy(
            fullName = fullName,
            email = email,
            phone = phone,
            monthlyIncome = monthlyIncome,
            dailyBudgetLimit = dailyBudgetLimit,
            savingsTargetPercentage = savingsPercentage,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            financialPersona = persona,
            isBudgetAlertActive = isBudgetAlertActive,
            avatarInitials = if (initials.isNotEmpty()) initials else "NU"
        )
        viewModelScope.launch {
            if (authRepository.getUserId() == null) {
                authRepository.loginWithDirectProfile(
                    email = email.ifBlank { "user@vinote.local" },
                    name = fullName.ifBlank { "NoTa User" },
                    provider = "profile_edit"
                )
            }
            authRepository.getUserId()?.let { userId ->
                persistDailyBudget(userId, dailyBudgetLimit, monthlyIncome)
                persistProfileSettings(userId, _userProfile.value)
                cloudSynchronizer.requestSync()
            }
        }
        showBanner("Profile & Budget settings updated! 💾")
    }

    // E-Wallet OTP Linking State
    private val _ewalletLinkingState = MutableStateFlow<EwalletLinkingState>(EwalletLinkingState.Idle)
    val ewalletLinkingState: StateFlow<EwalletLinkingState> = _ewalletLinkingState.asStateFlow()

    private val supabaseProvider = SupabaseClientProvider(application)

    private val walletGatewayRepository = WalletGatewayRepository(
        midtransGatewayService = MidtransGatewayService(
            supabaseEdgeFunctionUrl = SupabaseClientProvider(application).supabaseFunctionsUrl,
            supabaseAnonKey = SupabaseClientProvider(application).supabaseAnonKey
        ),
        unofficialGoPayService = UnofficialGoPayService(
            supabaseEdgeFunctionUrl = SupabaseClientProvider(application).supabaseFunctionsUrl,
            supabaseAnonKey = SupabaseClientProvider(application).supabaseAnonKey
        ),
        unofficialOvoService = UnofficialOvoService(
            supabaseEdgeFunctionUrl = SupabaseClientProvider(application).supabaseFunctionsUrl,
            supabaseAnonKey = SupabaseClientProvider(application).supabaseAnonKey
        ),
        unofficialDanaService = UnofficialDanaService(
            supabaseEdgeFunctionUrl = SupabaseClientProvider(application).supabaseFunctionsUrl,
            supabaseAnonKey = SupabaseClientProvider(application).supabaseAnonKey
        ),
        walletAccountDao = database.walletAccountDao(),
        supabaseClientProvider = SupabaseClientProvider(application)
    )

    fun sendEwalletOtp(walletId: String, phoneNumber: String) {
        val wallet = walletAccounts.value.find { it.id == walletId }
        val provider = when (wallet?.name?.lowercase()) {
            "gopay" -> PaymentGatewayService.Provider.GOPAY
            "ovo" -> PaymentGatewayService.Provider.OVO
            "dana" -> PaymentGatewayService.Provider.DANA
            else -> return
        }

        viewModelScope.launch {
            _ewalletLinkingState.value = EwalletLinkingState.SendingOtp(phoneNumber, provider.displayName)
            try {
                val result = walletGatewayRepository.linkWalletAccount(provider, phoneNumber)
                when (result) {
                    is BalanceFetchResult.LinkingRequired -> {
                        _ewalletLinkingState.value = EwalletLinkingState.AwaitingOtp(
                            phoneNumber = phoneNumber,
                            provider = provider.displayName,
                            referenceId = result.provider
                        )
                        showBanner("OTP sent to $phoneNumber ✓")
                    }
                    is BalanceFetchResult.Error -> {
                        _ewalletLinkingState.value = EwalletLinkingState.Error(result.message)
                        showBanner("Failed to send OTP: ${result.message}")
                    }
                    else -> {
                        _ewalletLinkingState.value = EwalletLinkingState.Error("Unexpected response")
                    }
                }
            } catch (e: Exception) {
                _ewalletLinkingState.value = EwalletLinkingState.Error(e.message ?: "Connection failed")
                showBanner("Network error: ${e.message}")
            }
        }
    }

    fun verifyEwalletOtp(walletId: String, phoneNumber: String, otp: String, referenceId: String) {
        val wallet = walletAccounts.value.find { it.id == walletId }
        val provider = when (wallet?.name?.lowercase()) {
            "gopay" -> PaymentGatewayService.Provider.GOPAY
            "ovo" -> PaymentGatewayService.Provider.OVO
            "dana" -> PaymentGatewayService.Provider.DANA
            else -> return
        }

        viewModelScope.launch {
                    _ewalletLinkingState.value = EwalletLinkingState.VerifyingOtp(phoneNumber, provider.displayName)
                    try {
                        val app = getApplication<Application>()
                        val edgeUrl = SupabaseClientProvider(app).supabaseFunctionsUrl
                        val anonKey = SupabaseClientProvider(app).supabaseAnonKey
                        val result = when (provider) {
                            PaymentGatewayService.Provider.GOPAY -> {
                                val goPayService = UnofficialGoPayService(edgeUrl, anonKey)
                                goPayService.verifyOtp(phoneNumber, otp, referenceId)
                                    .map { BalanceFetchResult.Success(0L, provider.displayName, phoneNumber, accountId = phoneNumber) }
                                    .getOrElse { BalanceFetchResult.Error(it.message ?: "GoPay verify failed") }
                            }
                            PaymentGatewayService.Provider.OVO -> {
                                val ovoService = UnofficialOvoService(edgeUrl, anonKey)
                                ovoService.verifyOtp(phoneNumber, otp, referenceId)
                                    .map { BalanceFetchResult.Success(0L, provider.displayName, phoneNumber, accountId = phoneNumber) }
                                    .getOrElse { BalanceFetchResult.Error(it.message ?: "OVO verify failed") }
                            }
                            PaymentGatewayService.Provider.DANA -> {
                                // DANA uses WebView linking, not OTP
                                BalanceFetchResult.Success(0L, provider.displayName, phoneNumber, accountId = phoneNumber)
                            }
                            else -> BalanceFetchResult.Error("Verification not supported")
                        }

                when (result) {
                    is BalanceFetchResult.Success -> {
                        val updated = wallet?.copy(
                            isConnected = true,
                            gatewayAccessToken = result.provider,
                            linkedAccountId = phoneNumber,
                            gatewayType = provider.displayName,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        if (updated != null) {
                            repository.updateWallet(updated)
                        }
                        _ewalletLinkingState.value = EwalletLinkingState.Success(provider.displayName, phoneNumber)
                        showBanner("${provider.displayName} connected successfully! 🎉")
                    }
                    is BalanceFetchResult.Error -> {
                        _ewalletLinkingState.value = EwalletLinkingState.Error(result.message)
                        showBanner("OTP verification failed: ${result.message}")
                    }
                    else -> {
                        _ewalletLinkingState.value = EwalletLinkingState.Error("Unexpected verification response")
                    }
                }
            } catch (e: Exception) {
                _ewalletLinkingState.value = EwalletLinkingState.Error(e.message ?: "Verification failed")
                showBanner("Verification error: ${e.message}")
            }
        }
    }

    fun resetEwalletLinkingState() {
        _ewalletLinkingState.value = EwalletLinkingState.Idle
    }

    /**
     * Start linking an e-wallet that is not yet saved in the database.
     *
     * The add-account dialog lets the user pick from [EwalletCatalog], so a
     * new wallet row is created first and then the OTP flow runs against it.
     * This keeps a single code path with the per-wallet "Link Account" button.
     */
    fun linkNewEwallet(displayName: String, phoneNumber: String) {
        val provider = EwalletCatalog.providerFor(displayName) ?: run {
            _ewalletLinkingState.value = EwalletLinkingState.Error("$displayName is not supported")
            return
        }

        viewModelScope.launch {
            val newId = "bank_${System.currentTimeMillis()}"
            val newWallet = WalletAccountEntity(
                id = newId,
                userId = activeUserIdOrGuest,
                name = displayName,
                type = WalletType.EWALLET,
                calculatedBalance = 0L,
                providerReportedBalance = 0L,
                isAutoDetectEnabled = true,
                iconColorHex = EwalletCatalog.options
                    .firstOrNull { it.displayName == displayName }?.brandColorHex ?: "#118EEA",
                accountNumber = phoneNumber,
                isConnected = false,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            repository.insertWallet(newWallet)

            // Hand off to the same OTP flow the per-wallet button uses.
            sendEwalletOtp(newId, phoneNumber)
        }
    }

    fun fetchEwalletBalance(walletId: String) {
        val wallet = walletAccounts.value.find { it.id == walletId } ?: return
        viewModelScope.launch {
            val result = walletGatewayRepository.fetchWalletBalance(wallet)
            when (result) {
                is BalanceFetchResult.Success -> {
                    val updated = wallet.copy(
                        providerReportedBalance = result.balance,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                    repository.updateWallet(updated)
                    showBanner("${wallet.name} balance updated: ${FormatUtils.formatRupiah(result.balance)}")
                }
                is BalanceFetchResult.Error -> {
                    showBanner("Failed to fetch ${wallet.name} balance: ${result.message}")
                }
                else -> {
                    showBanner("Balance check requires re-authentication")
                }
            }
        }
    }

        // Bank Integrations Methods
        fun toggleBankConnection(bankId: String) {
        viewModelScope.launch {
            val wallet = walletAccounts.value.find { it.id == bankId }
            if (wallet != null) {
                val updated = wallet.copy(
                    isConnected = !wallet.isConnected,
                    isAutoDetectEnabled = if (wallet.isConnected) false else wallet.isAutoDetectEnabled
                )
                repository.updateWallet(updated)
                val msg = if (updated.isConnected) "${updated.name} connected successfully! 🏦" else "${updated.name} disconnected"
                showBanner(msg)
            }
        }
    }

    fun toggleBankAutoSync(bankId: String) {
        viewModelScope.launch {
            val wallet = walletAccounts.value.find { it.id == bankId }
            if (wallet != null) {
                val updated = wallet.copy(isAutoDetectEnabled = !wallet.isAutoDetectEnabled)
                repository.updateWallet(updated)
                showBanner(if (updated.isAutoDetectEnabled) "Auto-sync enabled for ${updated.name}" else "Auto-sync paused")
            }
        }
    }

    fun syncAllBankStatements() {
        viewModelScope.launch {
            showBanner("Syncing bank & e-wallet transactions...")
            triggerCloudSync()
            showBanner("Bank statements synced successfully! ⚡")
        }
    }

    fun connectNewBank(bankName: String, accountNumber: String, balance: Long, type: String) {
        viewModelScope.launch {
            val newId = "bank_${System.currentTimeMillis()}"
            val colors = listOf("#003893", "#002B66", "#005E6A", "#FF6B00", "#118EEA", "#00B14F")
            val walletType = if (type.contains("E-Wallet", ignoreCase = true) || type.contains("Wallet", ignoreCase = true)) WalletType.EWALLET else WalletType.BANK
            val newWallet = WalletAccountEntity(
                id = newId,
                userId = activeUserIdOrGuest,
                name = bankName,
                type = walletType,
                calculatedBalance = balance,
                providerReportedBalance = balance,
                isAutoDetectEnabled = true,
                iconColorHex = colors.random(),
                accountNumber = accountNumber,
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            repository.insertWallet(newWallet)
            showBanner("Successfully linked $bankName! 💳")
        }
    }

    // Budget Exceeded Furious Logic
    private fun evaluateBudgetStatus() {
        if (!_userProfile.value.isBudgetAlertActive) return
        val currentSpent = todaySpent.value
        val limit = _userProfile.value.dailyBudgetLimit
        if (limit > 0 && currentSpent > limit && !_budgetAlertState.value.isTriggered) {
            val overage = currentSpent - limit
            _budgetAlertState.value = BudgetAlertState(
                isTriggered = true,
                spentToday = currentSpent,
                dailyLimit = limit,
                overageAmount = overage,
                message = "CRITICAL ALERT: You spent ${FormatUtils.formatRupiah(currentSpent)} today, exceeding your daily limit of ${FormatUtils.formatRupiah(limit)} by ${FormatUtils.formatRupiah(overage)}! NoTa is furious! 💢",
                isDismissed = false
            )
            _notaConfig.value = _notaConfig.value.copy(eyeState = NotaEyeState.FURIOUS)
            notificationEngine.notifyBudgetExceeded(overage, limit)
            showBanner("🚨 BUDGET EXCEEDED! NoTa is FURIOUS! 💢")
        }
    }

    fun simulateBudgetExceededAlert() {
        val limit = _userProfile.value.dailyBudgetLimit
        val simulatedSpent = limit + 65000L
        _budgetAlertState.value = BudgetAlertState(
            isTriggered = true,
            spentToday = simulatedSpent,
            dailyLimit = limit,
            overageAmount = 65000L,
            message = "CRITICAL ALERT: You spent ${FormatUtils.formatRupiah(simulatedSpent)} today, exceeding your daily limit of ${FormatUtils.formatRupiah(limit)} by ${FormatUtils.formatRupiah(65000L)}! NoTa is furious! 💢",
            isDismissed = false
        )
        _notaConfig.value = _notaConfig.value.copy(eyeState = NotaEyeState.FURIOUS)
        showBanner("🚨 BUDGET EXCEEDED ALERT TRIGGERED! 💢")
    }

    fun simulateIncomingWalletNotification(
        packageName: String = "com.gojek.app",
        title: String = "GoPay",
        text: String = "Pembayaran Rp 45.000 ke Kopi Kenangan berhasil"
    ) {
        viewModelScope.launch {
            val notif = WalletNotification(
                packageName = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            val (success, message) = detectionCoordinator.processNotification(notif, activeUserIdOrGuest)
            if (success) {
                showBanner("⚡ Auto-Detected: $message")
                evaluateBudgetStatus()
            } else {
                showBanner("Detection: $message")
            }
        }
    }

    fun dismissBudgetAlert() {
        _budgetAlertState.value = _budgetAlertState.value.copy(isDismissed = true)
    }

    fun calmNotaDown(newDailyLimit: Long? = null) {
        if (newDailyLimit != null && newDailyLimit > 0) {
            _userProfile.value = _userProfile.value.copy(dailyBudgetLimit = newDailyLimit)
            viewModelScope.launch {
                authRepository.getUserId()?.let { userId ->
                    persistDailyBudget(userId, newDailyLimit)
                }
            }
        }
        _budgetAlertState.value = _budgetAlertState.value.copy(isTriggered = false, isDismissed = true)
        _notaConfig.value = _notaConfig.value.copy(eyeState = NotaEyeState.HAPPY)
        showBanner("NoTa calmed down: 'Thanks for keeping your promise! Let's stay on track! 💙'")
    }

    // Pending Detection Approval / Rejection
    fun approvePendingDetection(transactionId: Long) {
        viewModelScope.launch {
            repository.confirmPendingTransaction(transactionId)
            showBanner("Transaction confirmed & added to ledger! ✨")
            evaluateBudgetStatus()
        }
    }

    fun rejectPendingDetection(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId, activeUserIdOrGuest)
            showBanner("Transaction dismissed")
        }
    }

    fun confirmPendingTransaction() {
        _pendingTransaction.value?.let { tx ->
            viewModelScope.launch {
                transactionService.addTransaction(
                    title = tx.title,
                    amount = tx.amount,
                    category = tx.category,
                    type = tx.type,
                    source = tx.source,
                    merchant = tx.merchant,
                    walletName = tx.walletName
                )

                if (tx.merchant.isNotBlank()) {
                    learnMerchantCategory(tx.merchant, tx.category)
                }

                // Dynamically reconcile associated wallet balance
                if (!tx.walletName.isNullOrBlank()) {
                    val matchingWallet = walletAccounts.value.find {
                        it.name.contains(tx.walletName, ignoreCase = true) ||
                                it.id.contains(tx.walletName, ignoreCase = true) ||
                                tx.walletName.contains(it.name, ignoreCase = true)
                    }
                    if (matchingWallet != null) {
                        val delta = if (tx.type == TransactionType.EXPENSE) -tx.amount else tx.amount
                        val newBal = (matchingWallet.calculatedBalance + delta).coerceAtLeast(0L)
                        repository.reconcileWalletBalance(matchingWallet.id, activeUserIdOrGuest, newBal)
                    }
                }

                _pendingTransaction.value = null
                _keypadAmount.value = "0"
                showBanner("Payment recorded: ${FormatUtils.formatRupiah(tx.amount)} (${tx.category})")
                delay(300)
                evaluateBudgetStatus()
                cloudSynchronizer.requestSync()
            }
        }
    }

    fun dismissPendingTransaction() {
        _pendingTransaction.value = null
    }

    fun addManualTransaction(title: String, amount: Long, category: String, type: TransactionType = TransactionType.EXPENSE) {
        viewModelScope.launch {
            transactionService.addTransaction(
                title = title,
                amount = amount,
                category = category,
                type = type,
                merchant = title,
                source = TransactionSource.MANUAL,
                timeLabel = "Just now"
            )
            showBanner("Transaction added!")
            delay(300)
            evaluateBudgetStatus()
            cloudSynchronizer.requestSync()
        }
    }

    fun selectTransactionDetail(transaction: TransactionItem?) {
        _selectedTransactionDetail.value = transaction
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionService.deleteTransaction(id)
            if (_selectedTransactionDetail.value?.id == id) {
                _selectedTransactionDetail.value = null
            }
            showBanner("Transaction deleted")
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            transactionService.clearAll()
            repository.clearAllData(activeUserIdOrGuest)
            _selectedTransactionDetail.value = null
            showBanner("All transaction history and data reset cleanly 🧹")
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            val summary = cloudSynchronizer.performFullSync()
            if (summary.status == CloudSyncStatus.SYNCED) {
                showBanner("Cloud sync complete! Up to date ☁️")
            } else {
                showBanner("Cloud sync: ${summary.errorMessage ?: "Finished"}")
            }
        }
    }

    fun syncExpensesWithFirestore() {
        triggerCloudSync()
    }

    // Goals Logic
    fun createGoal(title: String, targetAmount: Long, targetDate: String = "In 3 months", category: String = "Savings") {
        viewModelScope.launch {
            val icons = listOf("headphones", "flight_takeoff", "school", "laptop", "directions_car", "home")
            val icon = when {
                title.contains("trip", true) || title.contains("holiday", true) || title.contains("liburan", true) -> "flight_takeoff"
                title.contains("school", true) || title.contains("kuliah", true) || title.contains("buku", true) -> "school"
                title.contains("headphone", true) || title.contains("audio", true) -> "headphones"
                else -> icons.random()
            }
            val newGoal = GoalItem(
                userId = activeUserIdOrGuest,
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0L,
                targetDateDescription = targetDate,
                category = category,
                iconName = icon
            )
            repository.insertGoal(newGoal)
            showBanner("Goal '${title}' created successfully! 🎉")
            cloudSynchronizer.requestSync()
        }
    }

    fun addSavingsToGoal(goal: GoalItem, amount: Long) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))
            repository.updateGoal(updated)
            notificationEngine.notifyGoalProgress(goal.title, updated.currentAmount, goal.targetAmount)
            showBanner("Saved ${FormatUtils.formatRupiah(amount)} to ${goal.title}!")
            cloudSynchronizer.requestSync()
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            showBanner("Goal removed")
        }
    }

    // Voice Input & Hybrid NLP Processing (OpenRouter + On-Device NLP)
    fun setVoiceTranscript(text: String) {
        _voiceTranscript.value = text
        viewModelScope.launch {
            val parsed = hybridAiProcessor.parseVoiceText(text)
            _parsedVoiceEntity.value = parsed
        }
    }

    fun startRealSpeechRecording() {
        if (_isVoiceListening.value) return
        _isVoiceListening.value = true
        _voiceTranscript.value = ""

        speechRecorderService.startListening(
            onResult = { text ->
                _voiceTranscript.value = text
                _isVoiceListening.value = false
                viewModelScope.launch {
                    val parsed = hybridAiProcessor.parseVoiceText(text)
                    _parsedVoiceEntity.value = parsed
                }
            },
            onPartialResult = { partial ->
                _voiceTranscript.value = partial
            },
            onErrorCallback = { err ->
                _isVoiceListening.value = false
            }
        )
    }

    fun stopRealSpeechRecording() {
        speechRecorderService.stopListening()
        _isVoiceListening.value = false
    }

    fun toggleSpeechRecording() {
        if (speechRecorderService.isRecording.value || _isVoiceListening.value) {
            stopRealSpeechRecording()
            viewModelScope.launch { processVoiceInput() }
        } else {
            startRealSpeechRecording()
        }
    }

    fun simulateVoiceStreaming(utterance: String) {
        viewModelScope.launch {
            _isVoiceListening.value = true
            val words = utterance.split(" ")
            val sb = StringBuilder()
            for (word in words) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(word)
                _voiceTranscript.value = sb.toString()
                _parsedVoiceEntity.value = OfflineNlpEngine.parseSpokenTransaction(sb.toString())
                delay(120)
            }
            _isVoiceListening.value = false
            // Final hybrid enrichment
            val hybridParsed = hybridAiProcessor.parseVoiceText(sb.toString())
            _parsedVoiceEntity.value = hybridParsed
        }
    }

    suspend fun processVoiceInput() {
        val transcript = _voiceTranscript.value
        val parsedAi = hybridAiProcessor.parseVoiceText(text = transcript)

        _pendingTransaction.value = TransactionItem(
            userId = activeUserIdOrGuest,
            title = parsedAi.title,
            amount = parsedAi.amount,
            category = parsedAi.category,
            type = parsedAi.type,
            merchant = if (parsedAi.merchant.isNotBlank()) parsedAi.merchant else parsedAi.title,
            source = TransactionSource.VOICE,
            walletName = parsedAi.walletName,
            timeLabel = "Just now"
        )
    }

    // Hybrid Receipt Scan & OCR Processing
    fun startReceiptScanning(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            _isScanning.value = false
            onComplete()
        }
    }

    // Process a captured bitmap from camera
    fun processCapturedReceiptBitmap(bitmap: Bitmap, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val parsedData = withContext(Dispatchers.Default) {
                    hybridAiProcessor.processReceipt(bitmap)
                }

                _extractedReceiptData.value = parsedData
                _isScanning.value = false

                _pendingTransaction.value = TransactionItem(
                    userId = activeUserIdOrGuest,
                    title = parsedData.merchant,
                    amount = parsedData.totalAmount,
                    category = parsedData.category,
                    type = TransactionType.EXPENSE,
                    merchant = parsedData.merchant,
                    source = TransactionSource.SCAN,
                    timeLabel = "Just now"
                )
                val engineBadge = if (parsedData.isOfflineEngine) "On-Device Engine" else "AI"
                showBanner("Receipt ($engineBadge): ${FormatUtils.formatRupiah(parsedData.totalAmount)} from ${parsedData.merchant}")
                onComplete()
            } catch (e: Exception) {
                _isScanning.value = false
                _extractedReceiptData.value = null
                showBanner("OCR failed: ${e.message}")
                onComplete()
            }
        }
    }

    private fun looksLikeTransactionCommand(text: String): Boolean {
        val lower = text.lowercase()
        val hasAction = listOf(
            "catat", "tambahkan", "tambah transaksi", "beli", "bayar", "habis",
            "pengeluaran", "pemasukan", "gaji", "terima uang", "spent", "paid", "income"
        ).any(lower::contains)
        val hasAmount = Regex("""\d|\b(?:seribu|sepuluh ribu|dua puluh ribu|dua puluh lima ribu|tiga puluh lima ribu|lima puluh ribu|seratus ribu|dua ratus ribu|sejuta)\b""")
            .containsMatchIn(lower)
        return hasAction && hasAmount
    }

    private fun prepareChatTransactionDraft(userText: String): TransactionItem? {
        if (!looksLikeTransactionCommand(userText)) return null
        val parsed = OfflineNlpEngine.parseSpokenTransaction(userText)
        if (parsed.amount <= 0L) return null
        return TransactionItem(
            userId = activeUserIdOrGuest,
            title = parsed.title,
            amount = parsed.amount,
            category = parsed.category,
            type = parsed.type,
            merchant = parsed.merchant.ifBlank { parsed.title },
            walletName = parsed.walletName,
            source = TransactionSource.MANUAL,
            timestamp = System.currentTimeMillis(),
            timeLabel = "Just now"
        )
    }

    // Grounded Chat conversation with Controlled Tools
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isNotaTyping.value = true

            // Parse transaction commands locally first, so recording does not
            // depend on the cloud model returning a structured action.
            prepareChatTransactionDraft(userText)?.let { draft ->
                _pendingTransaction.value = draft
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "Saya sudah menyiapkan transaksi ${draft.title} sebesar ${FormatUtils.formatRupiah(draft.amount)}. Periksa detailnya lalu konfirmasi untuk menyimpan.",
                    isUser = false,
                    quickChips = emptyList(),
                    eyeState = NotaEyeState.EXCITED
                )
                _isNotaTyping.value = false
                return@launch
            }

            // Gather grounded deterministic facts via Controlled Tools
            val balance = financeTools.getCurrentBalance(activeUserIdOrGuest)
            val dailySpent = financeTools.getDailySpending(activeUserIdOrGuest)
            val recentTxs = financeTools.getRecentTransactionsSummary(activeUserIdOrGuest)
            val budgetStatus = financeTools.getBudgetStatus(activeUserIdOrGuest)
            val goalsSummary = financeTools.getGoalsProgressSummary(activeUserIdOrGuest)

            val systemContext = financeTools.buildGroundedSystemContext(
                userId = activeUserIdOrGuest,
                balance = balance,
                dailySpent = dailySpent,
                txSummary = recentTxs,
                budgetSummary = budgetStatus,
                goalsSummary = goalsSummary
            )

            val history = _chatMessages.value.takeLast(6).map {
                OpenRouterMessage(if (it.isUser) "user" else "assistant", it.text)
            }

            val aiResponse = aiService.chatWithNota(
                userMessage = userText,
                userProfile = _userProfile.value,
                transactions = allTransactions.value,
                goals = allGoals.value,
                accounts = bankAccounts.value,
                dailyLimit = dailyLimit,
                safeMoney = safeMoney.value,
                conversationHistory = history,
                isOnlineAllowed = _aiConfig.value.isOnlineAiEnabled
            )

            // Check if action proposes a transaction draft
            if (aiResponse.action is AiAction.ProposeTransaction) {
                val parsed = aiResponse.action.transaction
                _pendingTransaction.value = TransactionItem(
                    userId = activeUserIdOrGuest,
                    title = parsed.title,
                    amount = parsed.amount,
                    category = parsed.category,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    walletName = parsed.wallet,
                    source = TransactionSource.AUTO_DETECTED,
                    timeLabel = "Just now"
                )
            }

            val eye = when (aiResponse.intent) {
                AiIntent.CREATE_TRANSACTION -> NotaEyeState.EXCITED
                AiIntent.QUERY_BALANCE -> NotaEyeState.HAPPY
                AiIntent.FINANCIAL_ADVICE -> NotaEyeState.PROUD
                AiIntent.QUERY_SPENDING -> NotaEyeState.THINKING
                else -> NotaEyeState.HAPPY
            }

            _chatMessages.value = _chatMessages.value + ChatMessage(
                text = aiResponse.message,
                isUser = false,
                quickChips = aiResponse.suggestedChips,
                eyeState = eye
            )
            _isNotaTyping.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Hey! What are we figuring out today?",
                isUser = false,
                quickChips = listOf("Saldo aku berapa?", "Uangku paling banyak habis buat apa?", "Help me save"),
                eyeState = NotaEyeState.CURIOUS
            )
        )
    }

    fun cycleNotaExpression() {
        val nextEye = when (_notaConfig.value.eyeState) {
            NotaEyeState.HAPPY -> NotaEyeState.CURIOUS
            NotaEyeState.CURIOUS -> NotaEyeState.EXCITED
            NotaEyeState.EXCITED -> NotaEyeState.PROUD
            NotaEyeState.PROUD -> NotaEyeState.THINKING
            NotaEyeState.THINKING -> NotaEyeState.NEUTRAL
            NotaEyeState.NEUTRAL -> NotaEyeState.FURIOUS
            NotaEyeState.FURIOUS -> NotaEyeState.HAPPY
        }
        _notaConfig.value = _notaConfig.value.copy(eyeState = nextEye)
    }

    fun showBanner(message: String) {
        viewModelScope.launch {
            _bannerNotification.value = message
            delay(3000)
            if (_bannerNotification.value == message) {
                _bannerNotification.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecorderService.destroy()
    }
}
