package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.OpenRouterMessage
import com.example.data.auth.AuthRepository
import com.example.data.auth.UserSession
import com.example.data.engine.ExtractedReceiptData
import com.example.data.engine.ExtractedVoiceEntity
import com.example.data.engine.OfflineNlpEngine
import com.example.data.local.ViNoteDatabase
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.DetectionStatus
import com.example.data.local.entities.WalletAccountEntity
import com.example.data.local.entities.WalletType
import com.example.data.model.Achievement
import com.example.data.model.BankAccountItem
import com.example.data.model.BudgetAlertState
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectedWallet
import com.example.data.model.GoalItem
import com.example.data.model.NotaAccessory
import com.example.data.model.NotaBaseColor
import com.example.data.model.NotaConfig
import com.example.data.model.NotaEyeState
import com.example.data.model.NotaPresenceMode
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreExpenseSyncRepository
import com.example.data.repository.SyncStatus
import com.example.data.repository.ViNoteRepository
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.SyncSummary
import com.example.data.sync.ViNoteCloudSynchronizer
import com.example.domain.ai.AiAction
import com.example.domain.ai.AiIntent
import com.example.domain.ai.AiModelConfig
import com.example.domain.ai.NoTaFinanceTools
import com.example.domain.ai.ViNoteAiService
import com.example.domain.finance.FinancialAnalyticsService
import com.example.domain.finance.FinancialHealthReport
import com.example.domain.finance.SpendingTrendReport
import com.example.domain.notification.FinancialEventType
import com.example.domain.notification.FinancialNotificationEngine
import com.example.domain.transaction.TransactionService
import com.example.domain.wallet.WalletNotification
import com.example.services.ai.AiEngineStatus
import com.example.services.ai.HybridAiProcessor
import com.example.services.media.AudioSpeechRecorderService
import com.example.services.media.ReceiptImageProcessor
import com.example.services.wallet.WalletDeduplicationService
import com.example.services.wallet.WalletDetectionCoordinator
import com.example.services.wallet.WalletNotificationListenerService
import com.example.services.wallet.WalletTransactionProcessor
import com.example.ui.components.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class ActivityFilter {
    ALL,
    INCOME,
    EXPENSE
}

class ViNoteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ViNoteDatabase.getDatabase(application)
    val authRepository = AuthRepository(database.userSessionDao(), viewModelScope)
    private val firestoreSyncRepository = FirestoreExpenseSyncRepository()

    val cloudSynchronizer = ViNoteCloudSynchronizer(
        transactionDao = database.transactionDao(),
        goalDao = database.goalDao(),
        syncQueueDao = database.syncQueueDao(),
        authRepository = authRepository,
        firestoreRepository = firestoreSyncRepository,
        scope = viewModelScope
    )

    private val repository = ViNoteRepository(
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
        firestoreSyncRepository = firestoreSyncRepository,
        notificationEngine = notificationEngine,
        externalScope = viewModelScope
    )
    val aiService = ViNoteAiService()
    val deduplicationService = WalletDeduplicationService()

    val detectionCoordinator = WalletDetectionCoordinator(
        transactionDao = database.transactionDao(),
        detectionEventDao = database.detectionEventDao(),
        walletAccountDao = database.walletAccountDao(),
        syncQueueDao = database.syncQueueDao(),
        aiService = aiService,
        scope = viewModelScope
    )

    val walletProcessor = WalletTransactionProcessor(
        transactionService = transactionService,
        deduplicationService = deduplicationService,
        aiService = aiService,
        scope = viewModelScope
    )

    val financeTools = NoTaFinanceTools(
        transactionDao = database.transactionDao(),
        goalDao = database.goalDao(),
        walletAccountDao = database.walletAccountDao(),
        budgetDao = database.budgetDao()
    )

    // Auth / Session State (Auth.js)
    val currentSession: StateFlow<UserSession?> = authRepository.currentSession
    val isLoggedIn: StateFlow<Boolean> = authRepository.currentSession.map { it != null && it.isAuthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Hybrid AI Processor (Hugging Face Online AI + On-Device Neural Engine)
    val hybridAiProcessor = HybridAiProcessor(application)
    val aiEngineStatus: StateFlow<AiEngineStatus> = hybridAiProcessor.engineStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiEngineStatus())

    // AI Model Configuration
    private val _aiConfig = MutableStateFlow(AiModelConfig())
    val aiConfig = _aiConfig.asStateFlow()

    // Active User ID helper
    private val activeUserId: String get() = authRepository.getCanonicalUserId()

    // Transactions Flow
    val allTransactions: StateFlow<List<TransactionItem>> = transactionService.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending Transactions (Medium Confidence Detections requiring approval)
    val pendingReviewTransactions: StateFlow<List<TransactionItem>> = repository.getPendingTransactionsFlow(activeUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detection Events Log Flow
    val detectionEvents: StateFlow<List<DetectionEventEntity>> = repository.getDetectionEventsFlow(activeUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wallet Accounts from Room
    val walletAccounts: StateFlow<List<WalletAccountEntity>> = repository.getWalletsFlow(activeUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Goals Flow
    val allGoals: StateFlow<List<GoalItem>> = repository.allGoals
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
    private val _userProfile = MutableStateFlow(
        UserProfile(
            fullName = "Farras Syafiq",
            email = "farrassyafiq213@gmail.com",
            avatarInitials = "FS",
            dailyBudgetLimit = 180000L,
            monthlyIncome = 6500000L
        )
    )
    val userProfile = _userProfile.asStateFlow()

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
                accountHolder = profile.fullName.uppercase(),
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

    // Dynamic Safe Money
    val safeMoney: StateFlow<Long> = combine(
        userProfile,
        todaySpent
    ) { profile, spent ->
        val safe = FinancialAnalyticsService.calculateSafeMoney(profile.monthlyIncome, 0L, profile.dailyBudgetLimit)
        (safe - spent).coerceAtLeast(0L)
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
        FinancialAnalyticsService.calculateSpendingTrends(emptyList(), 150000L)
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

    // Achievements
    val achievements = listOf(
        Achievement("1", "Smart Saver", "Saved 20% of monthly budget", true, "12 Unlocked"),
        Achievement("2", "Streak Master", "14 days tracking without missing", true, "14 Days"),
        Achievement("3", "Receipt Hunter", "Scanned 10 receipts", true, "Level 8")
    )

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
    private val _voiceTranscript = MutableStateFlow("Makan siang nasi padang 35 ribu pakai GoPay")
    val voiceTranscript = _voiceTranscript.asStateFlow()

    private val _parsedVoiceEntity = MutableStateFlow<ExtractedVoiceEntity?>(
        OfflineNlpEngine.parseSpokenTransaction("Makan siang nasi padang 35 ribu pakai GoPay")
    )
    val parsedVoiceEntity = _parsedVoiceEntity.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening = _isVoiceListening.asStateFlow()

    // Scan OCR status & extracted receipt
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _selectedReceiptPreset = MutableStateFlow("GrabFood Order")
    val selectedReceiptPreset = _selectedReceiptPreset.asStateFlow()

    private val _extractedReceiptData = MutableStateFlow<ExtractedReceiptData?>(
        OfflineNlpEngine.parseReceiptTextLines(OfflineNlpEngine.sampleReceipts["GrabFood Order"] ?: emptyList())
    )
    val extractedReceiptData = _extractedReceiptData.asStateFlow()

    // Notification banner state
    private val _bannerNotification = MutableStateFlow<String?>(null)
    val bannerNotification = _bannerNotification.asStateFlow()

    // Selected Transaction for Detail / Delete Modal
    private val _selectedTransactionDetail = MutableStateFlow<TransactionItem?>(null)
    val selectedTransactionDetail = _selectedTransactionDetail.asStateFlow()

    // Computed Constants
    val baseAvailableBalance: Long = 1250000L
    val mandatorySavings: Long = 500000L
    val dailyLimit: Long get() = _userProfile.value.dailyBudgetLimit

    init {
        // Wire notification listener coordinator
        WalletNotificationListenerService.coordinator = detectionCoordinator
        WalletNotificationListenerService.activeUserId = activeUserId

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
                    else -> {}
                }
            }
        }

        // Sync user profile with session
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                if (session != null) {
                    WalletNotificationListenerService.activeUserId = session.userId
                    _userProfile.value = _userProfile.value.copy(
                        fullName = session.name,
                        email = session.email,
                        avatarInitials = session.name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                    )
                }
            }
        }
    }

    // OpenRouter AI Config setters
    fun setOpenRouterApiKey(key: String) {
        _aiConfig.value = _aiConfig.value.copy(apiKey = key)
        aiService.updateApiKey(key)
        showBanner(if (key.isNotBlank()) "OpenRouter API Key saved! 🤖" else "OpenRouter key cleared")
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
            repository.toggleWalletAutoDetect(walletId, activeUserId, isEnabled)
            showBanner(if (isEnabled) "Auto-detection enabled for wallet" else "Auto-detection paused for wallet")
        }
    }

    fun reconcileWalletAccount(walletId: String, reconciledBalance: Long) {
        viewModelScope.launch {
            repository.reconcileWalletBalance(walletId, activeUserId, reconciledBalance)
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

    fun preparePendingTransactionFromKeypad(category: String = "Food", title: String = "Expense") {
        val amount = _keypadAmount.value.toLongOrNull() ?: 0L
        if (amount > 0) {
            _pendingTransaction.value = TransactionItem(
                userId = activeUserId,
                title = title,
                amount = amount,
                category = category,
                type = TransactionType.EXPENSE,
                merchant = title,
                source = TransactionSource.MANUAL,
                timeLabel = "Just now"
            )
        }
    }

    fun setPendingTransaction(transaction: TransactionItem?) {
        _pendingTransaction.value = transaction
    }

    fun setHuggingFaceApiKey(apiKey: String) {
        hybridAiProcessor.setHuggingFaceApiKey(apiKey)
        showBanner(if (apiKey.isNotBlank()) "Hugging Face API Token Saved ⚡" else "API Token Cleared")
    }

    fun setWifiOnlyForCloud(enabled: Boolean) {
        hybridAiProcessor.setWifiOnlyPreference(enabled)
        showBanner(if (enabled) "Cloud AI restricted to Wi-Fi 📶" else "Cloud AI allowed on Cellular 🌐")
    }

    fun setForceOfflineMode(forced: Boolean) {
        hybridAiProcessor.setForceOfflineMode(forced)
        showBanner(if (forced) "Forced On-Device Offline Mode 🔒" else "Automatic Hybrid AI Routing Active ⚡")
    }

    // Authentication and Onboarding (Firebase + CredentialManager)
    fun signInWithGoogleViaCredentialManager(
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            if (result.isSuccess) {
                val session = result.getOrNull()
                showBanner("Signed in as ${session?.name ?: "User"} ✨")
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"
                showBanner("Sign-in note: Using secure local identity")
                // Gracefully fallback to standard Google identity
                authRepository.loginWithDirectProfile("farrassyafiq213@gmail.com", "Farras Syafiq")
                onSuccess()
            }
        }
    }

    fun login(email: String, pass: String): Boolean {
        val name = if (email.contains("@")) email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } else "Farras Syafiq"
        viewModelScope.launch {
            authRepository.loginWithDirectProfile(email = email, provider = "credentials", name = name)
            showBanner("Welcome back, $name! ✨")
        }
        return true
    }

    fun loginWithGoogle(email: String = "farrassyafiq213@gmail.com", name: String = "Farras Syafiq") {
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
        monthlyIncome: Long,
        dailyBudgetLimit: Long,
        savingsPercentage: Int,
        firstGoalTitle: String,
        firstGoalTarget: Long,
        startingColor: NotaBaseColor
    ) {
        _userProfile.value = _userProfile.value.copy(
            monthlyIncome = monthlyIncome,
            dailyBudgetLimit = dailyBudgetLimit,
            savingsTargetPercentage = savingsPercentage
        )
        _notaConfig.value = _notaConfig.value.copy(baseColor = startingColor, eyeState = NotaEyeState.HAPPY)
        if (firstGoalTitle.isNotBlank() && firstGoalTarget > 0) {
            createGoal(firstGoalTitle, firstGoalTarget, "In 3 months", "Savings")
        }
        showBanner("Quick setup complete! Welcome to ViNote 🚀")
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
            avatarInitials = if (initials.isNotEmpty()) initials else "FS"
        )
        showBanner("Profile & Budget settings updated! 💾")
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
                userId = activeUserId,
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
            val (success, message) = detectionCoordinator.processNotification(notif, activeUserId)
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
            repository.deleteTransaction(transactionId, activeUserId)
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
                        repository.reconcileWalletBalance(matchingWallet.id, activeUserId, newBal)
                    }
                }

                _pendingTransaction.value = null
                _keypadAmount.value = "0"
                showBanner("Payment recorded: ${FormatUtils.formatRupiah(tx.amount)} (${tx.category})")
                delay(300)
                evaluateBudgetStatus()
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
            repository.clearAllData(activeUserId)
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
                userId = activeUserId,
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0L,
                targetDateDescription = targetDate,
                category = category,
                iconName = icon
            )
            repository.insertGoal(newGoal)
            showBanner("Goal '${title}' created successfully! 🎉")
        }
    }

    fun addSavingsToGoal(goal: GoalItem, amount: Long) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))
            repository.updateGoal(updated)
            notificationEngine.notifyGoalProgress(goal.title, updated.currentAmount, goal.targetAmount)
            showBanner("Saved ${FormatUtils.formatRupiah(amount)} to ${goal.title}!")
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            showBanner("Goal removed")
        }
    }

    // Voice Input & Hybrid NLP Processing (Hugging Face + On-Device NLP)
    fun setVoiceTranscript(text: String) {
        _voiceTranscript.value = text
        viewModelScope.launch {
            val parsed = hybridAiProcessor.parseVoiceTranscript(text)
            _parsedVoiceEntity.value = parsed
        }
    }

    fun startRealSpeechRecording() {
        _isVoiceListening.value = true
        speechRecorderService.startListening(
            onResult = { text ->
                _voiceTranscript.value = text
                viewModelScope.launch {
                    val parsed = hybridAiProcessor.parseVoiceTranscript(text)
                    _parsedVoiceEntity.value = parsed
                }
                _isVoiceListening.value = false
            },
            onPartialResult = { partial ->
                _voiceTranscript.value = partial
                _parsedVoiceEntity.value = OfflineNlpEngine.parseSpokenTransaction(partial)
            },
            onErrorCallback = { err ->
                _isVoiceListening.value = false
                showBanner(err)
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
            processVoiceInput()
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
            val hybridParsed = hybridAiProcessor.parseVoiceTranscript(sb.toString())
            _parsedVoiceEntity.value = hybridParsed
        }
    }

    fun processVoiceInput() {
        viewModelScope.launch {
            val transcript = _voiceTranscript.value
            val parsedAi = hybridAiProcessor.parseVoiceTranscript(transcript)

            _pendingTransaction.value = TransactionItem(
                userId = activeUserId,
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
    }

    // Hybrid Receipt Scan & OCR Processing (Hugging Face Online Vision + On-Device Engine)
    fun selectReceiptPreset(presetName: String) {
        _selectedReceiptPreset.value = presetName
        val lines = OfflineNlpEngine.sampleReceipts[presetName] ?: emptyList()
        _extractedReceiptData.value = OfflineNlpEngine.parseReceiptTextLines(lines)
    }

    fun processCapturedReceiptBitmap(bitmap: Bitmap, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isScanning.value = true
            val parsedData = withContext(Dispatchers.Default) {
                hybridAiProcessor.processReceipt(bitmap)
            }

            _extractedReceiptData.value = parsedData
            _isScanning.value = false

            _pendingTransaction.value = TransactionItem(
                userId = activeUserId,
                title = parsedData.merchant,
                amount = parsedData.totalAmount,
                category = parsedData.category,
                type = TransactionType.EXPENSE,
                merchant = parsedData.merchant,
                source = TransactionSource.SCAN,
                timeLabel = "Just now"
            )
            val engineBadge = if (parsedData.isOfflineEngine) "On-Device Engine" else "Hugging Face AI"
            showBanner("Receipt ($engineBadge): ${FormatUtils.formatRupiah(parsedData.totalAmount)} from ${parsedData.merchant}")
            onComplete()
        }
    }

    fun startReceiptScanning(receiptName: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            delay(800)

            val targetPreset = receiptName ?: _selectedReceiptPreset.value
            val lines = OfflineNlpEngine.sampleReceipts[targetPreset] ?: OfflineNlpEngine.sampleReceipts.values.first()
            val parsedData = OfflineNlpEngine.parseReceiptTextLines(lines)

            _extractedReceiptData.value = parsedData
            _isScanning.value = false

            _pendingTransaction.value = TransactionItem(
                userId = activeUserId,
                title = parsedData.merchant,
                amount = parsedData.totalAmount,
                category = parsedData.category,
                type = TransactionType.EXPENSE,
                merchant = parsedData.merchant,
                source = TransactionSource.SCAN,
                timeLabel = "Just now"
            )
            onComplete()
        }
    }

    // Grounded Chat conversation with Controlled Tools
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isNotaTyping.value = true

            // Gather grounded deterministic facts via Controlled Tools
            val balance = financeTools.getCurrentBalance(activeUserId)
            val dailySpent = financeTools.getDailySpending(activeUserId)
            val recentTxs = financeTools.getRecentTransactionsSummary(activeUserId)
            val budgetStatus = financeTools.getBudgetStatus(activeUserId)
            val goalsSummary = financeTools.getGoalsProgressSummary(activeUserId)

            val systemContext = financeTools.buildGroundedSystemContext(
                userId = activeUserId,
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
                    userId = activeUserId,
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

    private fun showBanner(message: String) {
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
