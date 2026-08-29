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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
                        text = userProfile.fullName,
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

                    // Level 8 Bar
                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Level 8",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "80%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNotePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ViNoteProgressBar(
                            progress = 0.8f,
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
                            subtitle = "12 Unlocked",
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )

                        // Bento 2: Saving Streak
                        BentoCard(
                            icon = Icons.Default.LocalFireDepartment,
                            iconBg = ViNoteTertiaryFixed,
                            iconTint = ViNoteTertiaryContainer,
                            title = "Saving Streak",
                            subtitle = "14 Days",
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
                            subtitle = "3 Linked",
                            onClick = onNavigateToEWallets,
                            showDivider = true
                        )
                        MenuItemRow(
                            icon = Icons.Default.AccountBalance,
                            iconBg = ViNoteMintSuccess.copy(alpha = 0.2f),
                            iconTint = ViNoteMintSuccess,
                            title = "Bank Accounts & Open Banking",
                            subtitle = "BCA, Mandiri, Jago Linked",
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
    }
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
