package com.vinote.domain.ai

import android.util.Log
import com.vinote.core.ai.OpenRouterClient
import com.vinote.core.ai.OpenRouterMessage
import com.vinote.data.engine.ExtractedReceiptData
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.model.BankAccountItem
import com.vinote.data.model.GoalItem
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.data.model.UserProfile
import org.json.JSONObject

/**
 * ViNote AI Core Service
 * Directs natural language interpretation, receipt parsing, and conversational financial assistance
 * to OpenRouter (online) or OfflineNlpEngine (offline).
 */
class ViNoteAiService(
    private val openRouterClient: OpenRouterClient = OpenRouterClient()
) {

    fun updateModel(model: String) {
        openRouterClient.setModel(model)
    }

    /**
     * Parse natural language user utterance to structured transaction candidate.
     */
    suspend fun parseNaturalLanguageTransaction(
        text: String,
        isOnlineAllowed: Boolean = true
    ): ParsedAiTransaction {
        // Fast offline parsing baseline
        val offlineResult = OfflineNlpEngine.parseSpokenTransaction(text)

        if (!isOnlineAllowed) {
            return ParsedAiTransaction(
                title = offlineResult.title,
                amount = offlineResult.amount,
                type = offlineResult.type,
                category = offlineResult.category,
                merchant = offlineResult.merchant,
                wallet = offlineResult.walletName,
                confidence = offlineResult.confidence,
                description = text
            )
        }

        // Online OpenRouter Structured Extraction
        val systemPrompt = """
You are a precise financial NLP parser for Indonesian & English transactions.
Extract structured transaction data from the user's input.
Return ONLY raw JSON with keys:
{
  "title": "Short title e.g. Nasi Padang / GoRide",
  "amount": 35000,
  "type": "expense" or "income",
  "category": "Food" | "Transport" | "Shopping" | "Bills" | "Entertainment" | "General",
  "merchant": "Merchant or store name",
  "wallet": "GoPay" | "OVO" | "DANA" | "BCA" | "Cash",
  "confidence": 0.98
}
""".trimIndent()

        val messages = listOf(
            OpenRouterMessage("system", systemPrompt),
            OpenRouterMessage("user", text)
        )

        val result = openRouterClient.chatCompletion(messages, responseFormatJson = true)
        if (result.isSuccess) {
            try {
                val jsonStr = result.getOrNull()?.trim() ?: ""
                val cleanJson = if (jsonStr.startsWith("```json")) {
                    jsonStr.removePrefix("```json").removeSuffix("```").trim()
                } else if (jsonStr.startsWith("```")) {
                    jsonStr.removePrefix("```").removeSuffix("```").trim()
                } else jsonStr

                val obj = JSONObject(cleanJson)
                val typeStr = obj.optString("type", "expense").lowercase()
                val type = if (typeStr == "income") TransactionType.INCOME else TransactionType.EXPENSE
                val amount = obj.optLong("amount", offlineResult.amount)
                val title = obj.optString("title", offlineResult.title).ifBlank { offlineResult.title }
                val category = obj.optString("category", offlineResult.category).ifBlank { offlineResult.category }
                val merchant = obj.optString("merchant", offlineResult.merchant)
                val wallet = obj.optString("wallet", offlineResult.walletName ?: "GoPay")
                val confidence = obj.optDouble("confidence", 0.95).toFloat()

                return ParsedAiTransaction(
                    title = title,
                    amount = if (amount > 0) amount else offlineResult.amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    wallet = wallet,
                    confidence = confidence,
                    description = text
                )
            } catch (e: Exception) {
                Log.w("ViNoteAiService", "Failed to parse OpenRouter JSON response, falling back to offline NLP", e)
            }
        }

        return ParsedAiTransaction(
            title = offlineResult.title,
            amount = offlineResult.amount,
            type = offlineResult.type,
            category = offlineResult.category,
            merchant = offlineResult.merchant,
            wallet = offlineResult.walletName,
            confidence = offlineResult.confidence,
            description = text
        )
    }

    /**
     * Ask conversational NoTa with full financial context.
     */
    suspend fun chatWithNota(
        userMessage: String,
        userProfile: UserProfile,
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        accounts: List<BankAccountItem>,
        dailyLimit: Long,
        safeMoney: Long,
        conversationHistory: List<OpenRouterMessage> = emptyList(),
        isOnlineAllowed: Boolean = true
    ): AiResponse {
        val clean = userMessage.trim()

        // 1. Check if user is asking to create a transaction directly
        val lower = clean.lowercase()
        val isExplicitTransaction = (lower.contains("beli ") || lower.contains("makan ") ||
                lower.contains("bayar ") || lower.contains("goride") || lower.contains("gofood") ||
                lower.contains("gaji ") || lower.contains("topup") || lower.contains("transaksi")) &&
                (lower.contains("ribu") || lower.contains("rb") || lower.contains("k") || lower.contains("000") || lower.contains("juta"))

        if (isExplicitTransaction) {
            val parsedTx = parseNaturalLanguageTransaction(clean, isOnlineAllowed)
            return AiResponse(
                intent = AiIntent.CREATE_TRANSACTION,
                message = "I found a transaction for **${parsedTx.title}** (${com.vinote.ui.components.FormatUtils.formatRupiah(parsedTx.amount)}). Would you like to confirm and record this?",
                structuredTransaction = parsedTx,
                action = AiAction.ProposeTransaction(parsedTx),
                suggestedChips = listOf("Confirm & Save", "Cancel")
            )
        }

        // If offline, use rich deterministic assistant rules
        if (!isOnlineAllowed) {
            return generateOfflineAssistantResponse(clean, transactions, goals, dailyLimit, safeMoney)
        }

        // Online OpenRouter Conversational Reasoning
        val systemPrompt = FinancialContextBuilder.buildSystemPrompt(
            userProfile = userProfile,
            transactions = transactions,
            goals = goals,
            accounts = accounts,
            dailyLimit = dailyLimit,
            safeMoney = safeMoney
        )

        val messages = mutableListOf<OpenRouterMessage>()
        messages.add(OpenRouterMessage("system", systemPrompt))
        messages.addAll(conversationHistory.takeLast(6))
        messages.add(OpenRouterMessage("user", clean))

        val result = openRouterClient.chatCompletion(messages, temperature = 0.4)
        if (result.isSuccess) {
            val reply = result.getOrNull()?.trim() ?: "I'm here to help with your finances!"
            val intent = classifyIntent(clean)
            return AiResponse(
                intent = intent,
                message = reply,
                action = AiAction.AnswerQuestion(reply),
                suggestedChips = listOf("Check my budget", "Where did my money go?", "Help me save")
            )
        }

        // Fallback to offline rule-based response
        return generateOfflineAssistantResponse(clean, transactions, goals, dailyLimit, safeMoney)
    }

    /**
     * Interpret receipt lines from OCR image scanning.
     */
    suspend fun parseReceiptLines(
        lines: List<String>,
        isOnlineAllowed: Boolean = true
    ): ExtractedReceiptData {
        val offlineData = OfflineNlpEngine.parseReceiptTextLines(lines)

        if (!isOnlineAllowed) {
            return offlineData
        }

        val prompt = """
Analyze these OCR lines from a store receipt:
${lines.joinToString("\n")}

Extract structured details as valid JSON:
{
  "merchant": "Store Name",
  "date": "dd/MM/yyyy",
  "category": "Food" | "Shopping" | "Bills" | "Transport" | "General",
  "totalAmount": 35000,
  "subtotal": 30000,
  "taxOrFee": 5000,
  "walletOrPayment": "GoPay / QRIS / Cash"
}
""".trimIndent()

        val messages = listOf(
            OpenRouterMessage("system", "You are an OCR receipt parser. Output JSON only."),
            OpenRouterMessage("user", prompt)
        )

        val result = openRouterClient.chatCompletion(messages, responseFormatJson = true)
        if (result.isSuccess) {
            try {
                val jsonStr = result.getOrNull()?.trim() ?: ""
                val clean = if (jsonStr.startsWith("```json")) {
                    jsonStr.removePrefix("```json").removeSuffix("```").trim()
                } else if (jsonStr.startsWith("```")) {
                    jsonStr.removePrefix("```").removeSuffix("```").trim()
                } else jsonStr

                val obj = JSONObject(clean)
                return ExtractedReceiptData(
                    merchant = obj.optString("merchant", offlineData.merchant),
                    date = obj.optString("date", offlineData.date),
                    items = offlineData.items,
                    subtotal = obj.optLong("subtotal", offlineData.subtotal),
                    taxOrFee = obj.optLong("taxOrFee", offlineData.taxOrFee),
                    totalAmount = obj.optLong("totalAmount", offlineData.totalAmount),
                    category = obj.optString("category", offlineData.category),
                    walletOrPayment = obj.optString("walletOrPayment", offlineData.walletOrPayment),
                    rawLines = lines,
                    confidence = 0.98f,
                    isOfflineEngine = false
                )
            } catch (_: Exception) {}
        }

        return offlineData
    }

    private fun classifyIntent(text: String): AiIntent {
        val lower = text.lowercase()
        return when {
            lower.contains("saldo") || lower.contains("balance") || lower.contains("uang aman") -> AiIntent.QUERY_BALANCE
            lower.contains("boros") || lower.contains("pengeluaran") || lower.contains("spending") || lower.contains("habis") -> AiIntent.QUERY_SPENDING
            lower.contains("budget") || lower.contains("limit") -> AiIntent.QUERY_BUDGET
            lower.contains("riwayat") || lower.contains("history") || lower.contains("transaksi") -> AiIntent.QUERY_HISTORY
            lower.contains("nabung") || lower.contains("save") || lower.contains("goal") || lower.contains("target") -> AiIntent.FINANCIAL_ADVICE
            else -> AiIntent.GENERAL_CHAT
        }
    }

    private fun generateOfflineAssistantResponse(
        query: String,
        transactions: List<TransactionItem>,
        goals: List<GoalItem>,
        dailyLimit: Long,
        safeMoney: Long
    ): AiResponse {
        val lower = query.lowercase()
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val (reply, chips) = when {
            lower.contains("saldo") || lower.contains("balance") -> {
                "Your current net balance is **${com.vinote.ui.components.FormatUtils.formatRupiah(balance)}**, with **${com.vinote.ui.components.FormatUtils.formatRupiah(safeMoney)}** in safe spendable money." to listOf("Where did my money go?", "Check my daily limit")
            }
            lower.contains("habis") || lower.contains("boros") || lower.contains("spending") -> {
                val topCat = transactions.filter { it.type == TransactionType.EXPENSE }.groupBy { it.category }.maxByOrNull { it.value.sumOf { tx -> tx.amount } }
                if (topCat != null) {
                    val catAmt = topCat.value.sumOf { it.amount }
                    "Most of your spending went to **${topCat.key}** (${com.vinote.ui.components.FormatUtils.formatRupiah(catAmt)}). Total expenses recorded: ${com.vinote.ui.components.FormatUtils.formatRupiah(totalExpense)}." to listOf("Help me save", "Set budget")
                } else {
                    "You haven't recorded any major expenses yet! Looking great!" to listOf("Add transaction", "Check goals")
                }
            }
            lower.contains("kategori") || lower.contains("category") -> {
                val catBreakdown = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                if (catBreakdown.isNotEmpty()) {
                    val summary = catBreakdown.take(4).joinToString("\n") { (cat, amt) ->
                        "- $cat: ${com.vinote.ui.components.FormatUtils.formatRupiah(amt)}"
                    }
                    "Breakdown pengeluaranmu per kategori:\n$summary" to listOf("Uangku paling banyak habis buat apa?", "Saran budget")
                } else {
                    "Belum ada pengeluaran berdasarkan kategori yang tercatat." to listOf("Add transaction", "Check balance")
                }
            }
            lower.contains("saran") || lower.contains("50/30/20") || lower.contains("alokasi") -> {
                val income = if (totalIncome > 0) totalIncome else 5000000L
                val needs = (income * 0.50).toLong()
                val wants = (income * 0.30).toLong()
                val savings = (income * 0.20).toLong()
                "Saran alokasi budget cerdas 50/30/20:\n- Kebutuhan Pokok (50%): ${com.vinote.ui.components.FormatUtils.formatRupiah(needs)}\n- Hiburan/Keinginan (30%): ${com.vinote.ui.components.FormatUtils.formatRupiah(wants)}\n- Tabungan & Investasi (20%): ${com.vinote.ui.components.FormatUtils.formatRupiah(savings)}" to listOf("Check my budget", "Help me save")
            }
            lower.contains("nabung") || lower.contains("save") || lower.contains("goal") -> {
                val goalCount = goals.size
                val savingsRate = if (totalIncome > 0) (((totalIncome - totalExpense).coerceAtLeast(0L).toDouble() / totalIncome.toDouble()) * 100).toInt() else 0
                "Rasio tabunganmu saat ini: **$savingsRate%** dengan **$goalCount target tabungan aktif**. Pertahankan disiplin mencatat ya! ✨" to listOf("View goals", "Check balance")
            }
            else -> {
                "Hi! I'm NoTa 🌟 Tanya aku seputar saldo, pengeluaran per kategori, tips menabung 50/30/20, atau ucapkan 'Makan 25rb pakai GoPay' untuk catat cepat!" to listOf("Saldo aku berapa?", "Kategori pengeluaran", "Saran budget")
            }
        }

        return AiResponse(
            intent = classifyIntent(query),
            message = reply,
            action = AiAction.AnswerQuestion(reply, chips),
            suggestedChips = chips,
            isFromOfflineEngine = true
        )
    }
}
