package com.vinote.data.model

enum class NotaBaseColor {
    SOFT_PINK,
    SOFT_BLUE,
    WARM_YELLOW,
    MINT_GREEN
}

enum class NotaAccessory {
    NONE,
    GLASSES,
    BOWTIE,
    HEADPHONES
}

enum class NotaPresenceMode {
    ALWAYS_VISIBLE,
    MINIMAL
}

enum class NotaEyeState {
    NEUTRAL,      // ● ●
    CURIOUS,      // ⊙ ⊙ or ◉ ◉
    EXCITED,      // ★ ★
    HAPPY,        // ◡ ◡
    THINKING,     // ● ◌
    PROUD,        // ✦ ✦
    FURIOUS       // ◣ ◢ or > < (Furious over budget)
}

data class UserProfile(
    val fullName: String = "Farras Syafiq",
    val email: String = "farrassyafiq213@gmail.com",
    val phone: String = "+62 812-3456-7890",
    val monthlyIncome: Long = 5000000L,
    val dailyBudgetLimit: Long = 180000L,
    val savingsTargetPercentage: Int = 20,
    val currencyCode: String = "IDR",
    val currencySymbol: String = "Rp",
    val financialPersona: String = "Strategic Saver",
    val isBudgetAlertActive: Boolean = true,
    val avatarInitials: String = "FS"
)

data class BankAccountItem(
    val id: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val balance: Long,
    val isConnected: Boolean,
    val isAutoSync: Boolean,
    val lastSyncedTime: String,
    val brandColorHex: String,
    val bankType: String // "Bank" or "E-Wallet"
)

data class BudgetAlertState(
    val isTriggered: Boolean = false,
    val spentToday: Long = 0L,
    val dailyLimit: Long = 180000L,
    val overageAmount: Long = 0L,
    val message: String = "You have exceeded your daily budget limit!",
    val isDismissed: Boolean = false
)

data class NotaConfig(
    val baseColor: NotaBaseColor = NotaBaseColor.SOFT_PINK,
    val accessory: NotaAccessory = NotaAccessory.NONE,
    val personalitySlider: Float = 75f, // 0 (Calm & Analytical) to 100 (Playful & Chatty)
    val presenceMode: NotaPresenceMode = NotaPresenceMode.ALWAYS_VISIBLE,
    val offlineAiEngineDownloaded: Boolean = true,
    val eyeState: NotaEyeState = NotaEyeState.HAPPY
)
