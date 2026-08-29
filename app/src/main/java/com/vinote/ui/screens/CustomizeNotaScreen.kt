package com.vinote.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaAccessory
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaPresenceMode
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun CustomizeNotaScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ViNoteSurfaceContainerLow)
                            .testTag("customize_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ViNoteTextPrimary
                        )
                    }

                    Text(
                        text = "Customize Nota",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )

                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // Live Preview Avatar Center
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NotaAvatar(
                        size = 130.dp,
                        eyeState = notaConfig.eyeState,
                        baseColor = notaConfig.baseColor,
                        accessory = notaConfig.accessory,
                        showSparkle = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Nota",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )

                    Text(
                        text = "Tap your companion to interact!",
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary
                    )
                }
            }

            // BASE COLOR
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BASE COLOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ColorOption(
                                color = ViNoteSoftPink,
                                label = "Soft Pink",
                                selected = notaConfig.baseColor == NotaBaseColor.SOFT_PINK,
                                onClick = { viewModel.updateNotaBaseColor(NotaBaseColor.SOFT_PINK) }
                            )
                            ColorOption(
                                color = ViNoteSecondaryFixed,
                                label = "Soft Blue",
                                selected = notaConfig.baseColor == NotaBaseColor.SOFT_BLUE,
                                onClick = { viewModel.updateNotaBaseColor(NotaBaseColor.SOFT_BLUE) }
                            )
                            ColorOption(
                                color = ViNoteWarmYellow,
                                label = "Warm Yellow",
                                selected = notaConfig.baseColor == NotaBaseColor.WARM_YELLOW,
                                onClick = { viewModel.updateNotaBaseColor(NotaBaseColor.WARM_YELLOW) }
                            )
                            ColorOption(
                                color = Color(0xFFE8F5E9),
                                label = "Mint Green",
                                selected = notaConfig.baseColor == NotaBaseColor.MINT_GREEN,
                                onClick = { viewModel.updateNotaBaseColor(NotaBaseColor.MINT_GREEN) }
                            )
                        }
                    }
                }
            }

            // ACCESSORIES
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACCESSORIES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccessoryChip(
                                label = "None",
                                selected = notaConfig.accessory == NotaAccessory.NONE,
                                onClick = { viewModel.updateNotaAccessory(NotaAccessory.NONE) },
                                modifier = Modifier.weight(1f)
                            )
                            AccessoryChip(
                                label = "Glasses",
                                selected = notaConfig.accessory == NotaAccessory.GLASSES,
                                onClick = { viewModel.updateNotaAccessory(NotaAccessory.GLASSES) },
                                modifier = Modifier.weight(1f)
                            )
                            AccessoryChip(
                                label = "Bowtie",
                                selected = notaConfig.accessory == NotaAccessory.BOWTIE,
                                onClick = { viewModel.updateNotaAccessory(NotaAccessory.BOWTIE) },
                                modifier = Modifier.weight(1f)
                            )
                            AccessoryChip(
                                label = "Headphones",
                                selected = notaConfig.accessory == NotaAccessory.HEADPHONES,
                                onClick = { viewModel.updateNotaAccessory(NotaAccessory.HEADPHONES) },
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }

            // PERSONALITY & TONE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PERSONALITY & TONE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Calm & Analytical",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (notaConfig.personalitySlider < 50f) ViNotePrimary else ViNoteTextSecondary
                            )
                            Text(
                                text = "Playful & Chatty",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (notaConfig.personalitySlider >= 50f) ViNotePrimary else ViNoteTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = notaConfig.personalitySlider,
                            onValueChange = { viewModel.updateNotaPersonality(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = ViNotePrimary,
                                activeTrackColor = ViNotePrimary,
                                inactiveTrackColor = ViNoteSurfaceContainerLow
                            )
                        )
                    }
                }
            }

            // PRESENCE MODE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PRESENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PresenceOption(
                                title = "Always Visible",
                                description = "Nota stays on your screen",
                                selected = notaConfig.presenceMode == NotaPresenceMode.ALWAYS_VISIBLE,
                                onClick = { viewModel.updateNotaPresence(NotaPresenceMode.ALWAYS_VISIBLE) },
                                modifier = Modifier.weight(1f)
                            )
                            PresenceOption(
                                title = "Minimal",
                                description = "Only when needed",
                                selected = notaConfig.presenceMode == NotaPresenceMode.MINIMAL,
                                onClick = { viewModel.updateNotaPresence(NotaPresenceMode.MINIMAL) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Save changes button
            item {
                ViNoteButton(
                    text = "Save & Apply",
                    onClick = onBack,
                    testTag = "save_nota_btn"
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (selected) 3.dp else 1.dp, if (selected) ViNotePrimary else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = ViNotePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = ViNoteTextPrimary
        )
    }
}

@Composable
private fun AccessoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ViNotePrimary else ViNoteSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else ViNoteTextPrimary
        )
    }
}

@Composable
private fun PresenceOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ViNoteSecondaryFixed.copy(alpha = 0.5f) else ViNoteSurfaceContainerLow)
            .border(if (selected) 1.5.dp else 0.dp, if (selected) ViNotePrimary else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = ViNoteTextSecondary
            )
        }
    }
}
