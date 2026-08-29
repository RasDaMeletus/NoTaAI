package com.vinote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vinote.core.ai.OpenRouterMessage
import com.vinote.data.engine.ExtractedReceiptData
import com.vinote.data.engine.ExtractedVoiceEntity
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.local.ViNoteDatabase
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
import com.vinote.data.repository.FirestoreExpenseSyncRepository
import com.vinote.data.repository.SyncResult
import com.vinote.data.repository.SyncStatus
import com.vinote.data.repository.ViNoteRepository
import com.vinote.domain.ai.AiAction
import com.vinote.domain.ai.AiIntent
import com.vinote.domain.ai.AiModelConfig
import com.vinote.domain.ai.ViNoteAiService
import com.vinote.domain.finance.FinancialAnalyticsService
import com.vinote.domain.finance.FinancialHealthReport
import com.vinote.domain.finance.SpendingTrendReport
import com.vinote.domain.notification.FinancialEventType
import com.vinote.domain.notification.FinancialNotificationEngine
import com.vinote.domain.transaction.TransactionService
import com.vinote.domain.wallet.WalletNotification
import com.vinote.services.wallet.WalletDeduplicationService
import com.vinote.services.wallet.WalletNotificationListenerService
import com.vinote.services.wallet.WalletTransactionProcessor
import com.vinote.ui.components.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ActivityFilter {
    ALL,
    INCOME,
    EXPENSE
}

class ViNoteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ViNoteDatabase.getDatabase(application)
    private val repository = ViNoteRepository(database.transactionDao(), database.goalDao())
    private val firestoreSyncRepository = FirestoreExpenseSyncRepository()

    // Domain Services
    val notificationEngine = FinancialNotificationEngine(application)
    val transactionService = TransactionService(
        transactionDao = database.transactionDao(),
        firestoreSyncRepository = firestoreSyncRepository,
        notificationEngine = notificationEngine,
        externalScope = viewModelScope
    )
    val aiService = ViNoteAiService()
    val deduplicationService = WalletDeduplicationService()
    val walletProcessor = WalletTransactionProcessor(
        transactionService = transactionService,
        deduplicationService = deduplicationService,
        aiService = aiService,
        scope = viewModelScope
    )

    // AI Model Configuration
    private val _aiConfig = MutableStateFlow(AiModelConfig())
    val aiConfig = _aiConfig.asStateFlow()

    // Transactions Flow
    val allTransactions: StateFlow<List<TransactionItem>> = transactionService.allTransactions
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
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    // Auth State
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // Bank Accounts & E-Wallets
    private val _bankAccounts = MutableStateFlow(
        listOf(
            BankAccountItem("bca_1", "Bank Central Asia (BCA)", "•••• 8821", "FARRAS SYAFIQ", 4850000L, isConnected = true, isAutoSync = true, "Just now", "#003893", "Bank"),
            BankAccountItem("mandiri_1", "Bank Mandiri (Livin')", "•••• 4102", "FARRAS SYAFIQ", 2300000L, isConnected = true, isAutoSync = true, "10 mins ago", "#002B66", "Bank"),
            BankAccountItem("jago_1", "Bank Jago", "•••• 7731", "FARRAS SYAFIQ", 1150000L, isConnected = true, isAutoSync = false, "Today", "#FF6B00", "Bank"),
            BankAccountItem("bni_1", "Bank BNI", "•••• 2290", "FARRAS SYAFIQ", 0L, isConnected = false, isAutoSync = false, "Never", "#005E6A", "Bank"),
            BankAccountItem("bri_1", "Bank BRI (BRImo)", "•••• 5519", "FARRAS SYAFIQ", 0L, isConnected = false, isAutoSync = false, "Never", "#00529C", "Bank"),
            BankAccountItem("seabank_1", "SeaBank", "•••• 9021", "FARRAS SYAFIQ", 0L, isConnected = false, isAutoSync = false, "Never", "#FF5722", "Bank"),
            BankAccountItem("gopay_1", "GoPay", "0812-3456-7890", "FARRAS SYAFIQ", 185000L, isConnected = true, isAutoSync = true, "Just now", "#00B14F", "E-Wallet"),
            BankAccountItem("ovo_1", "OVO", "0812-3456-7890", "FARRAS SYAFIQ", 92500L, isConnected = true, isAutoSync = true, "1 hr ago", "#4C3494", "E-Wallet"),
            BankAccountItem("dana_1", "DANA", "0812-3456-7890", "FARRAS SYAFIQ", 45000L, isConnected = false, isAutoSync = false, "Never", "#118EEA", "E-Wallet"),
            BankAccountItem("shopee_1", "ShopeePay", "0812-3456-7890", "FARRAS SYAFIQ", 0L, isConnected = false, isAutoSync = false, "Never", "#EE4D2D", "E-Wallet")
        )
    )
    val bankAccounts = _bankAccounts.asStateFlow()

    // Budget Exceeded Furious Alert State
    private val _budgetAlertState = MutableStateFlow(BudgetAlertState())
    val budgetAlertState = _budgetAlertState.asStateFlow()

    // Total Aggregated Bank Balance
    val totalBankBalance: StateFlow<Long> = _bankAccounts.map { list ->
        list.filter { it.isConnected }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8577500L)

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1250000L)

    // Dynamic Safe Money
    val safeMoney: StateFlow<Long> = combine(
        userProfile,
        todaySpent
    ) { profile, spent ->
        val safe = FinancialAnalyticsService.calculateSafeMoney(profile.monthlyIncome, 0L, profile.dailyBudgetLimit)
        (safe - spent).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 750000L)

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
        FinancialAnalyticsService.calculateSpendingTrends(emptyList(), 180000L)
    )

    // Connected Wallets
    private val _wallets = MutableStateFlow(
        listOf(
            ConnectedWallet("gopay", "GoPay", isConnected = true, isActiveSync = true, iconColorHex = "#00B14F", description = "Active Sync"),
            ConnectedWallet("ovo", "OVO", isConnected = true, isActiveSync = true, iconColorHex = "#4C3494", description = "Active Sync"),
            ConnectedWallet("dana", "DANA", isConnected = false, isActiveSync = false, iconColorHex = "#118EEA", description = "Ready to connect")
        )
    )
    val wallets = _wallets.asStateFlow()

    private val _isDetectionActive = MutableStateFlow(true)
    val isDetectionActive = _isDetectionActive.asStateFlow()

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

    // Firestore Sync Status
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp = _lastSyncTimestamp.asStateFlow()

    // Selected Transaction for Detail / Delete Modal
    private val _selectedTransactionDetail = MutableStateFlow<TransactionItem?>(null)
    val selectedTransactionDetail = _selectedTransactionDetail.asStateFlow()

    // Computed Constants
    val baseAvailableBalance: Long = 1250000L
    val mandatorySavings: Long = 500000L
    val dailyLimit: Long get() = _userProfile.value.dailyBudgetLimit

    init {
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
        showBanner(if (enabled) "Automatic E-Wallet detection active" else "Automatic detection paused")
    }

    fun toggleWalletSync(walletId: String) {
        _wallets.value = _wallets.value.map {
            if (it.id == walletId) it.copy(isActiveSync = !it.isActiveSync, isConnected = !it.isConnected) else it
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
                title = title,
                amount = amount,
                category = category,
                type = TransactionType.EXPENSE,
                merchant = title,
                source = TransactionSource.MANUAL,
                timeLabel = "Just now",
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                syncState = "PENDING_SYNC"
            )
        }
    }

    fun setPendingTransaction(transaction: TransactionItem?) {
        _pendingTransaction.value = transaction
    }

    // Authentication and Onboarding
    fun login(email: String, pass: String): Boolean {
        _isLoggedIn.value = true
        _userProfile.value = _userProfile.value.copy(
            email = email,
            fullName = if (email.contains("@")) email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } else "Farras Syafiq"
        )
        showBanner("Welcome back, ${_userProfile.value.fullName}! ✨")
        return true
    }

    fun signup(fullName: String, email: String, pass: String): Boolean {
        _isLoggedIn.value = true
        val initials = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        _userProfile.value = _userProfile.value.copy(
            fullName = fullName,
            email = email,
            avatarInitials = if (initials.isNotEmpty()) initials else "FS"
        )
        showBanner("Account created successfully! 🎉")
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
        _isLoggedIn.value = false
        showBanner("Logged out successfully")
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
        _bankAccounts.value = _bankAccounts.value.map {
            if (it.id == bankId) {
                val newStatus = !it.isConnected
                it.copy(
                    isConnected = newStatus,
                    isAutoSync = if (!newStatus) false else it.isAutoSync,
                    lastSyncedTime = if (newStatus) "Just now" else "Never"
                )
            } else it
        }
        val target = _bankAccounts.value.find { it.id == bankId }
        val msg = if (target?.isConnected == true) "${target.bankName} connected successfully! 🏦" else "${target?.bankName} disconnected"
        showBanner(msg)
    }

    fun toggleBankAutoSync(bankId: String) {
        _bankAccounts.value = _bankAccounts.value.map {
            if (it.id == bankId) it.copy(isAutoSync = !it.isAutoSync) else it
        }
        val target = _bankAccounts.value.find { it.id == bankId }
        showBanner(if (target?.isAutoSync == true) "Auto-sync enabled for ${target.bankName}" else "Auto-sync paused")
    }

    fun syncAllBankStatements() {
        viewModelScope.launch {
            showBanner("Syncing bank transactions via Open Banking API...")
            delay(1000)
            _bankAccounts.value = _bankAccounts.value.map {
                if (it.isConnected) it.copy(lastSyncedTime = "Just now") else it
            }
            // Add a simulated bank auto-recorded transaction to showcase the sync live
            transactionService.addTransaction(
                title = "Coffee Bean & Tea Leaf",
                amount = 48000L,
                category = "Food",
                type = TransactionType.EXPENSE,
                merchant = "Coffee Bean",
                source = TransactionSource.BANK_SYNC,
                timeLabel = "Synced from BCA"
            )
            evaluateBudgetStatus()
            showBanner("Bank statements synced: 1 new transaction imported! ⚡")
        }
    }

    fun connectNewBank(bankName: String, accountNumber: String, balance: Long, type: String) {
        val newId = "bank_${System.currentTimeMillis()}"
        val colors = listOf("#003893", "#002B66", "#005E6A", "#FF6B00", "#118EEA", "#00B14F")
        val newBank = BankAccountItem(
            id = newId,
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = _userProfile.value.fullName.uppercase(),
            balance = balance,
            isConnected = true,
            isAutoSync = true,
            lastSyncedTime = "Just now",
            brandColorHex = colors.random(),
            bankType = type
        )
        _bankAccounts.value = _bankAccounts.value + newBank
        showBanner("Successfully linked $bankName! 💳")
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
            val (success, message) = walletProcessor.processNotification(notif)
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
            _selectedTransactionDetail.value = null
            showBanner("All transaction history deleted")
        }
    }

    fun syncExpensesWithFirestore() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            val result = repository.syncExpensesWithFirestore()
            if (result.isSuccess) {
                _syncStatus.value = SyncStatus.SUCCESS
                _lastSyncTimestamp.value = System.currentTimeMillis()
                val countMsg = if (result.uploadedCount > 0 || result.downloadedCount > 0) {
                    "Cloud sync complete (↑${result.uploadedCount} ↓${result.downloadedCount})"
                } else {
                    "Cloud sync complete (Up to date)"
                }
                showBanner(countMsg)
            } else {
                _syncStatus.value = SyncStatus.ERROR
                showBanner("Sync failed: ${result.errorMessage ?: "Network issue"}")
            }
        }
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
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0L,
                targetDateDescription = targetDate,
                category = category,
                iconName = icon,
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                syncState = "PENDING_SYNC"
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

    // Voice Input & NLP Processing
    fun setVoiceTranscript(text: String) {
        _voiceTranscript.value = text
        _parsedVoiceEntity.value = OfflineNlpEngine.parseSpokenTransaction(text)
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
        }
    }

    fun processVoiceInput() {
        viewModelScope.launch {
            val transcript = _voiceTranscript.value
            val parsedAi = aiService.parseNaturalLanguageTransaction(transcript, _aiConfig.value.isOnlineAiEnabled)

            _pendingTransaction.value = TransactionItem(
                title = parsedAi.title,
                amount = parsedAi.amount,
                category = parsedAi.category,
                type = parsedAi.type,
                merchant = parsedAi.merchant,
                source = TransactionSource.VOICE,
                timeLabel = "Just now",
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                syncState = "PENDING_SYNC"
            )
        }
    }

    // Receipt Scan & OCR Processing
    fun selectReceiptPreset(presetName: String) {
        _selectedReceiptPreset.value = presetName
        val lines = OfflineNlpEngine.sampleReceipts[presetName] ?: emptyList()
        _extractedReceiptData.value = OfflineNlpEngine.parseReceiptTextLines(lines)
    }

    fun startReceiptScanning(receiptName: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            delay(1000)

            val targetPreset = receiptName ?: _selectedReceiptPreset.value
            val lines = OfflineNlpEngine.sampleReceipts[targetPreset] ?: OfflineNlpEngine.sampleReceipts.values.first()
            val parsedData = aiService.parseReceiptLines(lines, _aiConfig.value.isOnlineAiEnabled)

            _extractedReceiptData.value = parsedData
            _isScanning.value = false

            _pendingTransaction.value = TransactionItem(
                title = parsedData.merchant,
                amount = parsedData.totalAmount,
                category = parsedData.category,
                type = TransactionType.EXPENSE,
                merchant = parsedData.merchant,
                source = TransactionSource.SCAN,
                timeLabel = "Just now",
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                syncState = "PENDING_SYNC"
            )
            onComplete()
        }
    }

    // Chat conversation
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isNotaTyping.value = true

            // Build history messages
            val history = _chatMessages.value.takeLast(6).map {
                OpenRouterMessage(if (it.isUser) "user" else "assistant", it.text)
            }

            val aiResponse = aiService.chatWithNota(
                userMessage = userText,
                userProfile = _userProfile.value,
                transactions = allTransactions.value,
                goals = allGoals.value,
                accounts = _bankAccounts.value,
                dailyLimit = dailyLimit,
                safeMoney = safeMoney.value,
                conversationHistory = history,
                isOnlineAllowed = _aiConfig.value.isOnlineAiEnabled
            )

            // Check if action proposes a transaction
            if (aiResponse.action is AiAction.ProposeTransaction) {
                val parsed = aiResponse.action.transaction
                _pendingTransaction.value = TransactionItem(
                    title = parsed.title,
                    amount = parsed.amount,
                    category = parsed.category,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    walletName = parsed.wallet,
                    source = TransactionSource.AUTO_DETECTED,
                    timeLabel = "Just now",
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    syncState = "PENDING_SYNC"
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
}

