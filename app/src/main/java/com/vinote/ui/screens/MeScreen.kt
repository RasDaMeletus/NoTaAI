package com.vinote.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import android.content.Intent
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import com.vinote.domain.export.TransactionExportService
import com.vinote.data.local.entities.WalletType
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.components.ViNoteProgressBar
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTertiaryContainer
import com.vinote.ui.theme.ViNoteTertiaryFixed
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun MeScreen(
    viewModel: ViNoteViewModel,
    onNavigateToCustomizeNota: () -> Unit,
    onNavigateToEWallets: () -> Unit,
    onNavigateToBankIntegrations: () -> Unit = onNavigateToEWallets,
    onNavigateToProfileSettings: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notaConfig by viewModel.notaConfig.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val unlockedCount by viewModel.unlockedAchievementsCount.collectAsState()
    val achievementsList by viewModel.achievements.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val savingStreakDays by viewModel.savingStreakDays.collectAsState()
    val walletAccounts by viewModel.walletAccounts.collectAsState()

    val userLevel = 1 + unlockedCount + (allTransactions.size / 5)
    val levelProgress = ((allTransactions.size % 5) / 5f).coerceIn(0.1f, 1.0f)
    val eWalletCount = walletAccounts.count { it.type == WalletType.EWALLET && it.isConnected }
    val bankCount = walletAccounts.count { it.type == WalletType.BANK && it.isConnected }

    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                // Profile & Nota Presence Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NotaAvatar(
                        size = 96.dp,
                        eyeState = NotaEyeState.HAPPY,
                        baseColor = notaConfig.baseColor,
                        accessory = notaConfig.accessory,
                        onClick = onNavigateToCustomizeNota
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = userProfile.fullName.ifBlank { "NoTa User" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Badge: Financial Persona / Profile Settings link
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ViNoteSecondaryFixed)
                            .clickable { onNavigateToProfileSettings() }
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                            .testTag("me_profile_settings_badge")
                    ) {
                        Text(
                            text = "${userProfile.financialPersona} • Edit Profile ✏️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNotePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Level Bar
                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Level $userLevel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "${(levelProgress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNotePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ViNoteProgressBar(
                            progress = levelProgress,
                            fillColor = ViNotePrimary
                        )
                    }
                }
            }

            // FINANCIAL WINS (Bento Row)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "FINANCIAL WINS",
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
                        // Bento 1: Achievements
                        BentoCard(
                            icon = Icons.Default.EmojiEvents,
                            iconBg = ViNoteWarmYellow.copy(alpha = 0.5f),
                            iconTint = Color(0xFF8C4B00),
                            title = "Achievements",
                            subtitle = "$unlockedCount Terbuka",
                            modifier = Modifier.weight(1f),
                            onClick = { showAchievementsDialog = true }
                        )

                        // Bento 2: Saving Streak
                        BentoCard(
                            icon = Icons.Default.LocalFireDepartment,
                            iconBg = ViNoteTertiaryFixed,
                            iconTint = ViNoteTertiaryContainer,
                            title = "Saving Streak",
                            subtitle = if (savingStreakDays > 0) "$savingStreakDays Days" else "0 Days",
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                }
            }

            // CONNECTIONS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONNECTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        MenuItemRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBg = ViNoteSecondaryFixed,
                            iconTint = ViNotePrimary,
                            title = "E-Wallets",
                            subtitle = if (eWalletCount > 0) "$eWalletCount Linked" else "Not Linked",
                            onClick = onNavigateToEWallets,
                            showDivider = true
                        )
                        MenuItemRow(
                            icon = Icons.Default.AccountBalance,
                            iconBg = ViNoteMintSuccess.copy(alpha = 0.2f),
                            iconTint = ViNoteMintSuccess,
                            title = "Bank Accounts & Open Banking",
                            subtitle = if (bankCount > 0) "$bankCount Linked" else "Not Linked",
                            onClick = onNavigateToBankIntegrations,
                            showDivider = false
                        )
                    }
                }
            }

            // NOTA SETTINGS
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NOTA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        MenuItemRow(
                            icon = Icons.Default.Palette,
                            iconBg = ViNoteSoftPink,
                            iconTint = Color(0xFFBA1A1A),
                            title = "Customize Nota",
                            subtitle = "Colors & Accessories",
                            onClick = onNavigateToCustomizeNota,
                            showDivider = true
                        )
                        MenuItemRow(
                            icon = Icons.Default.Psychology,
                            iconBg = ViNoteSecondaryFixed,
                            iconTint = ViNotePrimary,
                            title = "Personality & Tone",
                            subtitle = if (notaConfig.personalitySlider > 50f) "Playful & Supportive" else "Calm & Analytical",
                            onClick = onNavigateToCustomizeNota,
                            showDivider = false
                        )
                    }
                }
            }

            // APP PREFERENCES
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "APP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        MenuItemRow(
                            icon = Icons.Default.Settings,
                            iconBg = ViNoteSurfaceContainerLow,
                            iconTint = ViNoteTextPrimary,
                            title = "Settings",
                            subtitle = "Preferences & Security",
                            onClick = onNavigateToSettings,
                            showDivider = true
                        )

                        val context = LocalContext.current
                        MenuItemRow(
                            icon = Icons.Default.FileDownload,
                            iconBg = ViNoteSecondaryFixed,
                            iconTint = ViNotePrimary,
                            title = "Ekspor Laporan Transaksi (CSV)",
                            subtitle = "Download & bagikan file data offline",
                            onClick = {
                                try {
                                    val file = viewModel.exportTransactionsCsv(context)
                                    val shareIntent = TransactionExportService.createShareIntent(context, file)
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Transaksi (CSV)"))
                                } catch (e: Exception) {
                                    viewModel.showBanner("Gagal mengekspor: ${e.message}")
                                }
                            },
                            showDivider = true
                        )

                        MenuItemRow(
                            icon = Icons.Default.Description,
                            iconBg = ViNoteSecondaryFixed,
                            iconTint = ViNotePrimary,
                            title = "Ekspor Laporan Transaksi (PDF)",
                            subtitle = "Dokumen resmi & tabel transaksi siap cetak",
                            onClick = {
                                try {
                                    viewModel.exportTransactionsToPdf(context)
                                } catch (e: Exception) {
                                    viewModel.showBanner("Gagal mengekspor PDF: ${e.message}")
                                }
                            },
                            showDivider = true
                        )

                        MenuItemRow(
                            icon = Icons.Default.UploadFile,
                            iconBg = ViNoteTertiaryContainer,
                            iconTint = ViNotePrimary,
                            title = "Impor Mutasi Transaksi (CSV)",
                            subtitle = "Impor data e-statement atau backup NoTa",
                            onClick = { showImportCsvDialog = true },
                            showDivider = true
                        )

                        // Offline AI Engine Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(ViNoteMintSuccess.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = "Offline AI",
                                            tint = ViNoteMintSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Offline AI Engine",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ViNoteTextPrimary
                                        )
                                        Text(
                                            text = if (notaConfig.offlineAiEngineDownloaded) "Downloaded (240MB)" else "Disabled",
                                            fontSize = 13.sp,
                                            color = if (notaConfig.offlineAiEngineDownloaded) ViNoteMintSuccess else ViNoteTextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = notaConfig.offlineAiEngineDownloaded,
                                    onCheckedChange = { viewModel.toggleOfflineAi(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ViNotePrimary
                                    )
                                )
                            }

                            if (notaConfig.offlineAiEngineDownloaded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ViNoteSurfaceContainerLow)
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "• ASR Engine: Offline Speech-to-Text (ID/EN)",
                                            fontSize = 11.sp,
                                            color = ViNoteTextSecondary
                                        )
                                        Text(
                                            text = "• OCR Vision: Local Line & Total Recognizer",
                                            fontSize = 11.sp,
                                            color = ViNoteTextSecondary
                                        )
                                        Text(
                                            text = "• NLP Engine: Indonesian Entity Extractor",
                                            fontSize = 11.sp,
                                            color = ViNoteTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showAchievementsDialog) {
            AlertDialog(
                onDismissRequest = { showAchievementsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Prestasi Finansial ($unlockedCount/${achievementsList.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(achievementsList.size) { idx ->
                            val ach = achievementsList[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (ach.isUnlocked) ViNoteSecondaryFixed.copy(alpha = 0.35f) else ViNoteSurfaceContainerLow)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ach.icon, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ach.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ViNoteTextPrimary
                                        )
                                        Text(
                                            text = if (ach.isUnlocked) "Terbuka 🎉" else "${ach.progress}/${ach.target}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (ach.isUnlocked) ViNoteMintSuccess else ViNoteTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ach.description,
                                        fontSize = 11.sp,
                                        color = ViNoteTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                    if (!ach.isUnlocked && ach.target > 1) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { (ach.progress.toFloat() / ach.target.toFloat()).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = ViNotePrimary,
                                            trackColor = ViNoteSurfaceContainerLowest
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAchievementsDialog = false }) {
                        Text("Tutup", fontWeight = FontWeight.Bold, color = ViNotePrimary)
                    }
                }
            )
        }

        if (showImportCsvDialog) {
            ImportCsvDialog(
                onDismiss = { showImportCsvDialog = false },
                onImport = { csvText ->
                    viewModel.importTransactionsFromCsv(csvText) {
                        showImportCsvDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun ImportCsvDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var csvText by remember { mutableStateOf("") }
    val sampleCsv = "Tanggal,Keterangan,Nominal,Tipe\n" +
            "2026-09-06,Kopi Janji Jiwa,22000,Pengeluaran\n" +
            "2026-09-06,Makan Siang Soto,30000,Pengeluaran\n" +
            "2026-09-05,Transfer Masuk Bonus,500000,Pemasukan"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📥", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Impor Mutasi Transaksi (CSV)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tempel teks mutasi rekening/e-wallet format CSV atau ekspor NoTa di bawah.",
                    fontSize = 13.sp,
                    color = ViNoteTextSecondary
                )

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = {
                        Text(
                            text = "Tanggal,Keterangan,Nominal,Tipe\n2026-09-06,Kopi Kenangan,25000,Pengeluaran",
                            fontSize = 12.sp,
                            color = ViNoteTextSecondary.copy(alpha = 0.5f)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                TextButton(
                    onClick = { csvText = sampleCsv },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Isi Contoh Sampel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ViNotePrimary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(csvText) },
                enabled = csvText.isNotBlank()
            ) {
                Text(
                    text = "Impor Data",
                    fontWeight = FontWeight.Bold,
                    color = if (csvText.isNotBlank()) ViNotePrimary else ViNoteTextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = ViNoteTextSecondary)
            }
        }
    )
}

@Composable
private fun BentoCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
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
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ViNotePrimary
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ViNoteTextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = ViNoteTextSecondary,
                modifier = Modifier.size(20.dp)
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
