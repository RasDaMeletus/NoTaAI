package com.vinote.domain.mascot

enum class MascotFinancialMood {
    ON_BUDGET,
    OVER_BUDGET,
    HIGH_SAVINGS,
    STREAK,
    NEUTRAL
}

data class MascotQuote(
    val text: String,
    val mood: MascotFinancialMood,
    val eyeEmoji: String = "✨"
)

object NotaQuotesRepository {

    private val QUOTES_MAP: Map<MascotFinancialMood, List<MascotQuote>> = mapOf(
        MascotFinancialMood.ON_BUDGET to listOf(
            MascotQuote("Disiplin hari ini adalah kebebasan finansial di masa depan! 🌿", MascotFinancialMood.ON_BUDGET, "✨"),
            MascotQuote("Keren banget! Pengeluaranmu masih jauh di bawah batas anggaran.", MascotFinancialMood.ON_BUDGET, "😊"),
            MascotQuote("Pengeluaran tenang, hati pun senang. Pertahankan ya!", MascotFinancialMood.ON_BUDGET, "🕊️"),
            MascotQuote("Kamu hebat bisa menahan godaan belanja impulsif hari ini!", MascotFinancialMood.ON_BUDGET, "🌟"),
            MascotQuote("Kondisi dompetmu hari ini aman dan sangat terkendali 👍", MascotFinancialMood.ON_BUDGET, "🎯")
        ),
        MascotFinancialMood.OVER_BUDGET to listOf(
            MascotQuote("Waduh, anggaran harian terlewati! Yuk rem dulu belanjanya 🛑", MascotFinancialMood.OVER_BUDGET, "💢"),
            MascotQuote("Jangan cemas, besok kita seimbangkan lagi pengeluaran ya!", MascotFinancialMood.OVER_BUDGET, "🥺"),
            MascotQuote("Ingat tujuan tabunganmu. Coba prioritaskan kebutuhan pokok dulu.", MascotFinancialMood.OVER_BUDGET, "⚠️"),
            MascotQuote("Hati-hati dengan 'latte factor' atau jajan kecil yang menumpuk!", MascotFinancialMood.OVER_BUDGET, "☕"),
            MascotQuote("Tarik napas, evaluasi pengeluaran hari ini, dan rencanakan esok lebih baik.", MascotFinancialMood.OVER_BUDGET, "🧘")
        ),
        MascotFinancialMood.HIGH_SAVINGS to listOf(
            MascotQuote("Tabunganmu makin tebal! Bangga banget sama dedikasimu 💰", MascotFinancialMood.HIGH_SAVINGS, "🎉"),
            MascotQuote("Sedikit demi sedikit, lama-lama jadi bukit. Target makin dekat!", MascotFinancialMood.HIGH_SAVINGS, "🏔️"),
            MascotQuote("Kamu membuktikan kalau konsisten menabung itu sangat mungkin!", MascotFinancialMood.HIGH_SAVINGS, "🚀"),
            MascotQuote("Dana darurat yang kuat adalah perlindungan terbaik untuk masa depan.", MascotFinancialMood.HIGH_SAVINGS, "🛡️"),
            MascotQuote("Rasio tabunganmu di atas rata-rata! Teruskan gaya hidup sehat ini ✨", MascotFinancialMood.HIGH_SAVINGS, "👑")
        ),
        MascotFinancialMood.STREAK to listOf(
            MascotQuote("Konsistensi mencatatmu luar biasa! Catatan rapi bikin hidup tenang 🔥", MascotFinancialMood.STREAK, "🔥"),
            MascotQuote("Kebiasaan finansial yang baik sedang terbentuk dengan sempurna!", MascotFinancialMood.STREAK, "⚡"),
            MascotQuote("Setiap transaksi tercatat berarti tidak ada kebocoran yang lolos 🔍", MascotFinancialMood.STREAK, "💡"),
            MascotQuote("Disiplin pencatatanmu adalah modal utama kesuksesan finansial!", MascotFinancialMood.STREAK, "📈")
        ),
        MascotFinancialMood.NEUTRAL to listOf(
            MascotQuote("Uang yang bijak adalah uang yang teralokasi dengan sadar 📝", MascotFinancialMood.NEUTRAL, "✨"),
            MascotQuote("Setiap rupiah yang kamu catat punya cerita dan tujuannya masing-masing.", MascotFinancialMood.NEUTRAL, "📖"),
            MascotQuote("Beli apa yang kamu butuhkan, bukan yang sekadar diinginkan.", MascotFinancialMood.NEUTRAL, "🌱"),
            MascotQuote("Punya rencana keuangan yang jelas adalah bentuk mencintai diri sendiri 💖", MascotFinancialMood.NEUTRAL, "🌸"),
            MascotQuote("Hai! Jangan lupa catat pengeluaran kopi atau makan siangmu hari ini ya 🍜", MascotFinancialMood.NEUTRAL, "☕")
        )
    )

    fun getQuoteForState(
        isOverBudget: Boolean,
        savingsRate: Double,
        streakDays: Int,
        selectedIndex: Int = 0
    ): MascotQuote {
        val mood = when {
            isOverBudget -> MascotFinancialMood.OVER_BUDGET
            savingsRate >= 25.0 -> MascotFinancialMood.HIGH_SAVINGS
            streakDays >= 5 -> MascotFinancialMood.STREAK
            !isOverBudget -> MascotFinancialMood.ON_BUDGET
            else -> MascotFinancialMood.NEUTRAL
        }

        val quotes = QUOTES_MAP[mood] ?: QUOTES_MAP[MascotFinancialMood.NEUTRAL]!!
        val index = (selectedIndex % quotes.size).coerceAtLeast(0)
        return quotes[index]
    }
}
