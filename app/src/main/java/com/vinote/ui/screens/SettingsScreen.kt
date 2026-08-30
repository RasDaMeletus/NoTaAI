package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncStatus
import com.example.ui.components.ViNoteButton
import com.example.ui.components.ViNoteButtonType
import com.example.ui.components.ViNoteCard
import com.example.ui.theme.ViNoteError
import com.example.ui.theme.ViNoteMintSuccess
import com.example.ui.theme.ViNotePrimary
import com.example.ui.theme.ViNoteSecondaryFixed
import com.example.ui.theme.ViNoteSurface
import com.example.ui.theme.ViNoteSurfaceContainerLow
import com.example.ui.theme.ViNoteSurfaceContainerLowest
import com.example.ui.theme.ViNoteTextPrimary
import com.example.ui.theme.ViNoteTextSecondary
import com.example.viewmodel.ViNoteViewModel

@Composable
fun SettingsScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToProfileSettings: () -> Unit = {},
    onNavigateToBankIntegrations: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showHfKeyDialog by remember { mutableStateOf(false) }
    var hfApiKeyInput by remember { mutableStateOf("") }

    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncSummary by viewModel.syncSummary.collectAsState()
    val lastSyncTimestamp = syncSummary.lastSyncTimestamp
    val userProfile by viewModel.userProfile.collectAsState()
    val aiEngineStatus by viewModel.aiEngineStatus.collectAsState()

    var wifiOnlyCloud by remember { mutableStateOf(aiEngineStatus.isWifiOnlyPreferred) }
    var forceOfflineMode by remember { mutableStateOf(aiEngineStatus.isForceOffline) }

    val infiniteTransition = rememberInfiniteTransition(label = "settings_sync_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "settings_sync_angle"
    )

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
                            .testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ViNoteTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                }
            }

            // HYBRID AI & HUGGING FACE ENGINE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "HYBRID AI & OFFLINE ENGINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        // Current Active Routing Status
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (aiEngineStatus.isOnline) ViNoteMintSuccess.copy(alpha = 0.2f) else ViNoteSecondaryFixed.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (aiEngineStatus.isOnline) Icons.Default.AutoAwesome else Icons.Default.Psychology,
                                        contentDescription = "AI Engine",
                                        tint = if (aiEngineStatus.isOnline) ViNoteMintSuccess else ViNotePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Active Inference Engine",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = if (aiEngineStatus.isOnline) "Hugging Face Online ⚡ (${aiEngineStatus.connectionType})" else "On-Device Neural Engine 🔒 (Offline)",
                                        fontSize = 13.sp,
                                        color = if (aiEngineStatus.isOnline) ViNoteMintSuccess else ViNoteTextSecondary
                                    )
                                }
                            }
                        }

                        // Wi-Fi Only Switch
                        SettingsToggleRow(
                            icon = Icons.Default.NetworkWifi,
                            title = "Wi-Fi Only for Cloud AI",
                            subtitle = "Route to Hugging Face only on Wi-Fi (Saves data)",
                            checked = wifiOnlyCloud,
                            onCheckedChange = {
                                wifiOnlyCloud = it
                                viewModel.setWifiOnlyForCloud(it)
                            }
                        )

                        // Force Offline Mode Switch
                        SettingsToggleRow(
                            icon = Icons.Default.Lock,
                            title = "Force On-Device Offline Mode",
                            subtitle = "Run 100% locally with zero internet requests",
                            checked = forceOfflineMode,
                            onCheckedChange = {
                                forceOfflineMode = it
                                viewModel.setForceOfflineMode(it)
                            }
                        )

                        // Hugging Face API Token config
                        SettingsRow(
                            icon = Icons.Default.Key,
                            title = "Hugging Face API Token",
                            subtitle = if (aiEngineStatus.hasApiKey) "Custom API Token Configured 🔑" else "Tap to set Hugging Face API Token",
                            onClick = { showHfKeyDialog = true }
                        )
                    }
                }
            }

            // CLOUD SYNC (FIREBASE FIRESTORE)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CLOUD SYNC (FIREBASE FIRESTORE)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        val syncSubtitle = when (syncStatus) {
                            SyncStatus.IDLE -> if (lastSyncTimestamp != null) "Last synced: Just now" else "Connected to Firebase"
                            SyncStatus.SYNCING -> "Synchronizing with Firestore..."
                            SyncStatus.SUCCESS -> "All expenses backed up to Cloud"
                            SyncStatus.ERROR -> "Sync error, tap to retry"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.syncExpensesWithFirestore() }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ViNoteSecondaryFixed.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Firestore Sync",
                                        tint = ViNotePrimary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .then(
                                                if (syncStatus == SyncStatus.SYNCING) Modifier.rotate(rotationAngle)
                                                else Modifier
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Cloud Expense Backup",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = syncSubtitle,
                                        fontSize = 13.sp,
                                        color = if (syncStatus == SyncStatus.ERROR) ViNoteError else if (syncStatus == SyncStatus.SUCCESS) ViNoteMintSuccess else ViNoteTextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ViNotePrimary)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("sync_now_settings_btn")
                            ) {
                                Text(
                                    text = if (syncStatus == SyncStatus.SYNCING) "Syncing" else "Sync Now",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ACCOUNT & SECURITY
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACCOUNT & SECURITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        SettingsRow(
                            icon = Icons.Default.Person,
                            title = "Profile & Budget Settings",
                            subtitle = "${userProfile.fullName} (${userProfile.email})",
                            onClick = onNavigateToProfileSettings
                        )
                        SettingsRow(
                            icon = Icons.Default.MonetizationOn,
                            title = "Bank Integrations & E-Wallets",
                            subtitle = "Link BCA, Mandiri, GoPay & Auto-Sync",
                            onClick = onNavigateToBankIntegrations
                        )
                        SettingsToggleRow(
                            icon = Icons.Default.Fingerprint,
                            title = "Biometrics & Passcode",
                            subtitle = "Require fingerprint on open",
                            checked = biometricEnabled,
                            onCheckedChange = { biometricEnabled = it }
                        )
                    }
                }
            }

            // APP PREFERENCES
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "APP PREFERENCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        SettingsRow(
                            icon = Icons.Default.MonetizationOn,
                            title = "Currency",
                            subtitle = "IDR (Rp)",
                            onClick = {}
                        )
                        SettingsRow(
                            icon = Icons.Default.Language,
                            title = "Language",
                            subtitle = "Indonesian / English",
                            onClick = {}
                        )
                        SettingsToggleRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            subtitle = "Daily insights & alerts",
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
            }

            // FINANCIAL DATA & PRIVACY
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DATA & PRIVACY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 0.dp) {
                        SettingsRow(
                            icon = Icons.Default.FileDownload,
                            title = "Export Transactions",
                            subtitle = "Download CSV / PDF",
                            onClick = {}
                        )
                        SettingsRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Transaction History",
                            subtitle = "Wipe local and remote cloud expenses",
                            iconTint = ViNoteError,
                            iconBg = ViNoteError.copy(alpha = 0.12f),
                            onClick = { showClearHistoryDialog = true }
                        )
                    }
                }
            }

            // ABOUT
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ViNote v3.0.1",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                    Text(
                        text = "Your Soft & Alive Financial Companion",
                        fontSize = 12.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            // Sign Out
            item {
                ViNoteButton(
                    text = "Sign Out",
                    onClick = onSignOut,
                    type = ViNoteButtonType.GHOST,
                    testTag = "sign_out_btn"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showHfKeyDialog) {
        AlertDialog(
            onDismissRequest = { showHfKeyDialog = false },
            title = {
                Text(
                    text = "Hugging Face API Token",
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Hugging Face User Access Token (hf_...) to unlock high-accuracy cloud models (TrOCR & IndoBERT/Flan-T5) when connected to Wi-Fi. ViNote automatically falls back to 100% on-device offline models when offline or on mobile data.",
                        color = ViNoteTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = hfApiKeyInput,
                        onValueChange = { hfApiKeyInput = it },
                        placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setHuggingFaceApiKey(hfApiKeyInput.trim())
                        showHfKeyDialog = false
                    }
                ) {
                    Text("Save Token", color = ViNotePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHfKeyDialog = false }) {
                    Text("Cancel", color = ViNoteTextSecondary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Delete All Transaction History?",
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all your expense and income records from local storage and remote Firestore cloud sync. This cannot be undone.",
                    color = ViNoteTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        viewModel.clearAllTransactions()
                    },
                    modifier = Modifier.testTag("confirm_delete_all_settings_btn")
                ) {
                    Text("Delete All", color = ViNoteError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = ViNoteTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = ViNotePrimary,
    iconBg: Color = ViNoteSecondaryFixed.copy(alpha = 0.5f),
    onClick: () -> Unit
) {
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
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ViNoteSecondaryFixed.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ViNotePrimary,
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ViNotePrimary
            )
        )
    }
}
