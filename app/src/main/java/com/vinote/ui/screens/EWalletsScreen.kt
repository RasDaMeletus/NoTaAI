package com.vinote.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.ConnectedWallet
import com.vinote.data.model.NotaEyeState
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionSource
import com.vinote.data.model.TransactionType
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.components.ViNoteTransactionTile
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun EWalletsScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.wallets.collectAsState()
    val isDetectionActive by viewModel.isDetectionActive.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

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
                        text = "E-Wallets & Sync",
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
                            .size(90.dp)
                            .shadow(
                                elevation = 12.dp,
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
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DETECTION ACTIVE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ViNoteTextPrimary,
                        letterSpacing = 0.05.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reading notifications securely on-device",
                        fontSize = 14.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            // CONNECTED WALLETS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONNECTED WALLETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        wallets.forEachIndexed { index, wallet ->
                            WalletRow(
                                wallet = wallet,
                                onToggle = { viewModel.toggleWalletSync(wallet.id) },
                                showDivider = index < wallets.size - 1
                            )
                        }

                        // Add Wallet row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Add DANA or other wallet
                                    viewModel.toggleWalletSync("dana")
                                }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ViNotePrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Wallet",
                                    tint = ViNotePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "+ Add Another Wallet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNotePrimary
                            )
                        }
                    }
                }
            }

            // LAST DETECTION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LAST DETECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    val demoDetection = TransactionItem(
                        title = "GrabFood",
                        amount = 25000L,
                        category = "Food",
                        type = TransactionType.EXPENSE,
                        timeLabel = "Just now",
                        merchant = "GrabFood",
                        source = TransactionSource.E_WALLET,
                        walletName = "GoPay",
                        userId = "demo",
                        syncState = "LOCAL_ONLY"
                    )

                    ViNoteTransactionTile(
                        transaction = demoDetection,
                        onClick = {
                            viewModel.setPendingTransaction(demoDetection)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nota Insight speech bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ViNoteSoftPink.copy(alpha = 0.35f))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NotaAvatar(
                                size = 36.dp,
                                eyeState = NotaEyeState.HAPPY,
                                baseColor = notaConfig.baseColor,
                                isAnimated = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "I categorized this under Food & Drinks! ✨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ViNoteTextPrimary
                            )
                        }
                    }
                }
            }

            // Simulate Detection Trigger Test Action
            item {
                ViNoteButton(
                    text = "Simulate Inbound Payment Notification",
                    onClick = {
                        val simulated = TransactionItem(
                            title = "GrabFood",
                            amount = 25000L,
                            category = "Food",
                            type = TransactionType.EXPENSE,
                            merchant = "GrabFood",
                            source = TransactionSource.E_WALLET,
                            walletName = "GoPay",
                            timeLabel = "Just now",
                            userId = "demo",
                            syncState = "LOCAL_ONLY"
                        )
                        viewModel.setPendingTransaction(simulated)
                    },
                    type = ViNoteButtonType.GHOST,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Simulate",
                            tint = ViNotePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    testTag = "simulate_detection_btn"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun WalletRow(
    wallet: ConnectedWallet,
    onToggle: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (wallet.isActiveSync) ViNoteMintSuccess else Color.Gray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = wallet.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                    Text(
                        text = if (wallet.isActiveSync) "Active Sync" else "Paused",
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            Switch(
                checked = wallet.isActiveSync,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ViNotePrimary
                )
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 18.dp)
                    .background(Color(0x1F747789))
            )
        }
    }
}
