package com.vinote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.components.ViNoteProgressBar
import com.vinote.ui.components.ViNoteTransactionTile
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainer
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTertiaryContainer
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun HomeScreen(
    viewModel: ViNoteViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()
        val notaConfig by viewModel.notaConfig.collectAsState()
        val userProfile by viewModel.userProfile.collectAsState()
        val budgetAlertState by viewModel.budgetAlertState.collectAsState()
        val calculatedBalance by viewModel.currentCalculatedBalance.collectAsState()
        val safeMoney by viewModel.safeMoney.collectAsState()
        val pendingList by viewModel.pendingReviewTransactions.collectAsState()
        val financialHealth by viewModel.financialHealthScore.collectAsState()
        val notaQuote by viewModel.notaQuote.collectAsState()
        val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsState()
        val spendingPrediction by viewModel.spendingPrediction.collectAsState()
        val mandatorySavings by viewModel.mandatorySavings.collectAsState()

    // Ambient floating background glow
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        // Decorative ambient top-left blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ViNoteSecondaryFixed.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Decorative ambient right blob
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = 160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ViNoteWarmYellow.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToSettings() }
                    ) {
                        // User Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ViNoteSecondaryFixed)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(ViNotePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.avatarInitials.ifBlank { "U" }.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val displayName = userProfile.fullName.trim().ifBlank { "NoTa User" }
                        val firstName = if (displayName.contains(" ")) displayName.substringBefore(" ") else displayName
                        Text(
                            text = "Hi, $firstName 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ViNoteSurfaceContainerLow)
                            .testTag("notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = ViNoteTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Hero: Expressive Nota Character with Speech Bubble
            item {
                val isFurious = budgetAlertState.isTriggered && !budgetAlertState.isDismissed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Speech Bubble
                    Box(
                        modifier = Modifier
                            .offset(x = 18.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp),
                                ambientColor = if (isFurious) Color(0x33BA1A1A) else Color(0x1A171827)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                            .background(if (isFurious) Color(0xFFFFF0F0) else ViNoteSurfaceContainerLowest)
                            .clickable {
                                if (isFurious) {
                                    viewModel.calmNotaDown()
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isFurious)
                                "💢 OVER BUDGET! You exceeded the daily limit! Tap to calm me 🕊️"
                            else
                                notaQuote.text,
                            fontSize = 14.sp,
                            fontWeight = if (isFurious) FontWeight.Bold else FontWeight.Medium,
                            color = if (isFurious) Color(0xFFBA1A1A) else ViNoteTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nota Character Avatar
                    NotaAvatar(
                        size = 110.dp,
                        eyeState = if (isFurious) NotaEyeState.FURIOUS else notaConfig.eyeState,
                        baseColor = notaConfig.baseColor,
                        accessory = notaConfig.accessory,
                        onClick = {
                            if (isFurious) {
                                viewModel.calmNotaDown()
                            } else {
                                viewModel.rotateNotaQuote()
                            }
                        }
                    )
                }
            }

            // Balance Card
            item {
                ViNoteCard(
                    padding = 22.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AVAILABLE BALANCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextSecondary,
                            letterSpacing = 0.08.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.togglePrivacyMode() }
                        ) {
                            Text(
                                text = FormatUtils.formatRupiah(calculatedBalance, isPrivacyMode),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ViNoteTextPrimary,
                                letterSpacing = (-0.02).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Sub balance cards (Uang Aman & Wajib Tabung)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Uang Aman
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ViNoteSurfaceContainerLow)
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(ViNoteMintSuccess.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Uang Aman",
                                            tint = ViNoteMintSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "UANG AMAN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = FormatUtils.formatRupiah(safeMoney, isPrivacyMode),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                }
                            }

                            // Wajib Tabung
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ViNoteSurfaceContainerLow)
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(ViNoteTertiaryContainer.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = "Wajib Tabung",
                                            tint = ViNoteTertiaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "WAJIB TABUNG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = FormatUtils.formatRupiah(mandatorySavings, isPrivacyMode),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Today Progress Card
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TODAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    val todaySpent by viewModel.todaySpent.collectAsState()
                    val dailyLimit = userProfile.dailyBudgetLimit.coerceAtLeast(1L)
                    val progress = (todaySpent.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f)
                    val percentage = (progress * 100).toInt()

                    ViNoteCard(padding = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${FormatUtils.formatRupiah(todaySpent, isPrivacyMode)} spent",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextPrimary
                                )
                                Text(
                                    text = "$percentage% of daily target",
                                    fontSize = 13.sp,
                                    color = ViNoteTextSecondary
                                )
                            }

                            Text(
                                text = "${FormatUtils.formatRupiah(dailyLimit, isPrivacyMode)} limit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ViNoteProgressBar(
                            progress = progress,
                            fillColor = if (progress >= 1.0f) Color(0xFFBA1A1A) else ViNotePrimary
                        )
                    }
                }
            }

            // Financial Health Score Card (0-100 Offline Engine)
            item {
                ViNoteCard(padding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SKOR KESEHATAN KEUANGAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary,
                                    letterSpacing = 0.05.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (financialHealth.score >= 70) ViNoteMintSuccess.copy(alpha = 0.2f) else Color(0x33E65100))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Grade ${financialHealth.grade}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (financialHealth.score >= 70) ViNoteMintSuccess else Color(0xFFE65100)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = financialHealth.advice,
                                fontSize = 12.sp,
                                color = ViNoteTextPrimary,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Score Circle Badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ViNotePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${financialHealth.score}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ViNotePrimary
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Spending Forecast Card (PRD Section 1.1)
            item {
                ViNoteCard(padding = 16.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRAKIRAAN PENGELUARAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextSecondary,
                                letterSpacing = 0.05.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F0FE))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Puncak: ${spendingPrediction.topRiskDay}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1967D2)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FormatUtils.formatRupiah(spendingPrediction.weeklyProjectedExpense, isPrivacyMode),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "Kategori: ${spendingPrediction.topCategory}",
                                fontSize = 12.sp,
                                color = ViNoteTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = spendingPrediction.insightText,
                            fontSize = 12.sp,
                            color = ViNoteTextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Pending Transactions Requiring Review (Medium confidence detection)
            if (pendingList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ PENDING PAYMENT CONFIRMATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE65100),
                                letterSpacing = 0.05.sp
                            )
                            Text(
                                text = "${pendingList.size} new",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        pendingList.forEach { pendingTx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pendingTx.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = "Rp ${pendingTx.amount} • via ${pendingTx.walletName ?: "E-Wallet"}",
                                        fontSize = 12.sp,
                                        color = ViNoteTextSecondary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { viewModel.rejectPendingDetection(pendingTx.id) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFEBEE))
                                    ) {
                                        Text("✕", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { viewModel.approvePendingDetection(pendingTx.id) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(ViNoteMintSuccess)
                                    ) {
                                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Quick Add Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK ADD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Manual
                        QuickAddButton(
                            title = "Manual",
                            icon = Icons.Default.EditNote,
                            onClick = onNavigateToAdd,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_add_manual"
                        )
                        // Scan
                        QuickAddButton(
                            title = "Scan",
                            icon = Icons.Default.ReceiptLong,
                            onClick = onNavigateToScan,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_add_scan"
                        )
                        // Voice
                        QuickAddButton(
                            title = "Voice",
                            icon = Icons.Default.Mic,
                            onClick = onNavigateToVoice,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_add_voice"
                        )
                    }
                }
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp
                    )

                    if (transactions.isNotEmpty()) {
                        Text(
                            text = "SEE ALL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNotePrimary,
                            modifier = Modifier.clickable { onNavigateToActivity() }
                        )
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    ViNoteCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NotaAvatar(
                                eyeState = NotaEyeState.CURIOUS,
                                baseColor = notaConfig.baseColor,
                                isAnimated = false
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions yet!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "Speak, scan a receipt, or tap + to record your first real transaction",
                                fontSize = 12.sp,
                                color = ViNoteTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Transaction items (take first 3)
                items(transactions.take(3), key = { it.id }) { transaction ->
                    ViNoteTransactionTile(
                        transaction = transaction,
                        onClick = {
                            viewModel.selectTransactionDetail(transaction)
                        }
                    )
                }
            }

            // Nota Recommends Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ViNoteSecondaryFixed.copy(alpha = 0.45f))
                        .clickable { onNavigateToActivity() }
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "^ ^",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ViNoteSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Nota Recommends",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "You're getting close to your savings goal.",
                                fontSize = 13.sp,
                                color = ViNoteTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // clearance for bottom navigation
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x0F171827)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(ViNoteSurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ViNotePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ViNotePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ViNoteTextPrimary
            )
        }
    }
}
