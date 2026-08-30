package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.DetectionEventEntity
import com.example.data.local.entities.DetectionStatus
import com.example.data.local.entities.WalletAccountEntity
import com.example.data.model.NotaEyeState
import com.example.ui.components.FormatUtils
import com.example.ui.components.NotaAvatar
import com.example.ui.components.ViNoteButton
import com.example.ui.components.ViNoteButtonType
import com.example.ui.components.ViNoteCard
import com.example.ui.theme.ViNoteMintSuccess
import com.example.ui.theme.ViNotePrimary
import com.example.ui.theme.ViNoteSoftPink
import com.example.ui.theme.ViNoteSurface
import com.example.ui.theme.ViNoteSurfaceContainerLow
import com.example.ui.theme.ViNoteSurfaceContainerLowest
import com.example.ui.theme.ViNoteTextPrimary
import com.example.ui.theme.ViNoteTextSecondary
import com.example.viewmodel.ViNoteViewModel

@Composable
fun EWalletsScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val walletAccounts by viewModel.walletAccounts.collectAsState()
    val isDetectionActive by viewModel.isDetectionActive.collectAsState()
    val detectionEvents by viewModel.detectionEvents.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

    var reconcilingWallet by remember { mutableStateOf<WalletAccountEntity?>(null) }
    var reconcileInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ViNoteSurfaceContainerLow)
                            .testTag("wallets_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ViNoteTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "E-Wallets & Auto-Detection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                }
            }

            // Status Hero Card: Detection Active
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                ambientColor = ViNoteMintSuccess.copy(alpha = 0.4f),
                                spotColor = ViNoteMintSuccess.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(ViNoteMintSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "HARDENED AUTO-DETECTION ACTIVE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ViNoteTextPrimary,
                        letterSpacing = 0.05.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Real-time, on-device SHA-256 deduplicated parsing",
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            // CONNECTED WALLET ACCOUNTS & RECONCILIATION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONNECTED ACCOUNTS & BALANCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        if (walletAccounts.isEmpty()) {
                            Text(
                                text = "No wallets connected yet.",
                                modifier = Modifier.padding(16.dp),
                                color = ViNoteTextSecondary
                            )
                        } else {
                            walletAccounts.forEachIndexed { index, wallet ->
                                WalletAccountRow(
                                    wallet = wallet,
                                    onToggle = {
                                        viewModel.toggleWalletAccountAutoDetect(wallet.id, !wallet.isAutoDetectEnabled)
                                    },
                                    onReconcile = {
                                        reconcilingWallet = wallet
                                        reconcileInput = wallet.calculatedBalance.toString()
                                    },
                                    showDivider = index < walletAccounts.size - 1
                                )
                            }
                        }
                    }
                }
            }

            // RECENT DETECTION EVENTS LOG
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETECTION AUDIT TRAIL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextSecondary,
                            letterSpacing = 0.05.sp
                        )
                        Text(
                            text = "${detectionEvents.size} events",
                            fontSize = 12.sp,
                            color = ViNoteTextSecondary
                        )
                    }

                    if (detectionEvents.isEmpty()) {
                        ViNoteCard {
                            Text(
                                text = "No financial notifications detected recently. Tap the button below to test detection.",
                                fontSize = 13.sp,
                                color = ViNoteTextSecondary
                            )
                        }
                    } else {
                        ViNoteCard(padding = 0.dp) {
                            detectionEvents.take(5).forEachIndexed { idx, event ->
                                DetectionEventRow(event = event)
                                if (idx < detectionEvents.take(5).size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0x1F747789))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SIMULATION & TEST BUTTONS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ViNoteButton(
                        text = "Simulate DANA QRIS (Auto-Record)",
                        onClick = {
                            viewModel.simulateIncomingWalletNotification(
                                packageName = "id.dana",
                                title = "DANA",
                                text = "Pembayaran QRIS Berhasil ke Janji Jiwa sebesar Rp 28.000"
                            )
                        },
                        type = ViNoteButtonType.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        testTag = "simulate_dana_btn"
                    )

                    ViNoteButton(
                        text = "Simulate GoPay Medium Confidence (Pending Review)",
                        onClick = {
                            viewModel.simulateIncomingWalletNotification(
                                packageName = "com.gojek.app",
                                title = "GoPay",
                                text = "Transaksi GoJek baru: total Rp 35.000"
                            )
                        },
                        type = ViNoteButtonType.GHOST,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = ViNotePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        testTag = "simulate_gopay_btn"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Reconcile Balance Dialog
        if (reconcilingWallet != null) {
            val wallet = reconcilingWallet!!
            Dialog(onDismissRequest = { reconcilingWallet = null }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ViNoteSurfaceContainerLowest)
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Reconcile ${wallet.name} Balance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary
                        )

                        Text(
                            text = "Set the actual reported balance in your ${wallet.name} app. ViNote will calibrate the ledger without duplicating expense entries.",
                            fontSize = 13.sp,
                            color = ViNoteTextSecondary
                        )

                        OutlinedTextField(
                            value = reconcileInput,
                            onValueChange = { reconcileInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Balance (IDR)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ViNoteButton(
                                text = "Cancel",
                                onClick = { reconcilingWallet = null },
                                type = ViNoteButtonType.GHOST,
                                modifier = Modifier.width(100.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ViNoteButton(
                                text = "Save",
                                onClick = {
                                    val amount = reconcileInput.toLongOrNull() ?: 0L
                                    viewModel.reconcileWalletAccount(wallet.id, amount)
                                    reconcilingWallet = null
                                },
                                type = ViNoteButtonType.PRIMARY,
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletAccountRow(
    wallet: WalletAccountEntity,
    onToggle: () -> Unit,
    onReconcile: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (wallet.isAutoDetectEnabled) ViNoteMintSuccess else Color.Gray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = wallet.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                    Text(
                        text = "Ledger: ${FormatUtils.formatRupiah(wallet.calculatedBalance)}",
                        fontSize = 12.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onReconcile, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconcile",
                        tint = ViNotePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Switch(
                    checked = wallet.isAutoDetectEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ViNotePrimary
                    )
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(Color(0x1F747789))
            )
        }
    }
}

@Composable
private fun DetectionEventRow(event: DetectionEventEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${event.provider} - ${event.rawSnippet}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ViNoteTextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${event.type} • Confidence: ${(event.confidence * 100).toInt()}%",
                fontSize = 11.sp,
                color = ViNoteTextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val statusColor = when (event.status) {
            DetectionStatus.AUTO_RECORDED -> ViNoteMintSuccess
            DetectionStatus.PENDING_REVIEW -> Color(0xFFE65100)
            DetectionStatus.REJECTED -> Color(0xFFC62828)
            else -> ViNoteTextSecondary
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = event.status.name.replace("_", " "),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
