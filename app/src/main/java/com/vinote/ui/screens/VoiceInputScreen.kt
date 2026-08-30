package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.engine.OfflineNlpEngine
import com.example.ui.components.FormatUtils
import com.example.ui.theme.ViNoteMintSuccess
import com.example.ui.theme.ViNoteOnPrimary
import com.example.ui.theme.ViNoteOutlineVariant
import com.example.ui.theme.ViNotePrimary
import com.example.ui.theme.ViNotePrimaryContainer
import com.example.ui.theme.ViNotePrimaryFixed
import com.example.ui.theme.ViNotePrimaryFixedDim
import com.example.ui.theme.ViNoteSecondaryContainer
import com.example.ui.theme.ViNoteSecondaryFixed
import com.example.ui.theme.ViNoteSurface
import com.example.ui.theme.ViNoteSurfaceContainerHigh
import com.example.ui.theme.ViNoteSurfaceContainerLow
import com.example.ui.theme.ViNoteSurfaceContainerLowest
import com.example.ui.theme.ViNoteTertiaryFixed
import com.example.ui.theme.ViNoteTextPrimary
import com.example.ui.theme.ViNoteTextSecondary
import com.example.viewmodel.ViNoteViewModel

@Composable
fun VoiceInputScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceTranscript by viewModel.voiceTranscript.collectAsState()
    val parsedEntity by viewModel.parsedVoiceEntity.collectAsState()
    val isListening by viewModel.isVoiceListening.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()
    val aiEngineStatus by viewModel.aiEngineStatus.collectAsState()

    var isEditingTranscript by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startRealSpeechRecording()
        }
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission) {
            viewModel.startRealSpeechRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Animations for Ambient Background and Mic Fluid Ripple Waves
    val infiniteTransition = rememberInfiniteTransition(label = "voice_screen_motion")

    // Ambient background pulses
    val ambientPulse1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient1"
    )
    val ambientPulse2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient2"
    )

    // Cursor blink animation
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    // 3 Concentric Fluid Waves for the central mic
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val waveAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha1"
    )

    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    val waveAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha2"
    )

    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )
    val waveAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha3"
    )

    // Dynamic scale bonus derived from real-time microphone RMS audio volume
    val liveRmsBonus = if (isListening) (audioRmsDb * 0.45f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        // 1. AMBIENT BACKGROUND GLOW (Soft Sky Blue top-left, Mint-Success bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = 80.dp)
                .size(280.dp)
                .scale(ambientPulse1)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ViNotePrimaryFixedDim.copy(alpha = 0.35f),
                            ViNoteSecondaryFixed.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (-80).dp)
                .size(320.dp)
                .scale(ambientPulse2)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ViNoteMintSuccess.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. SCREEN CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp))

                Text(
                    text = "LIVE SPEECH INPUT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextSecondary,
                    letterSpacing = 1.5.sp
                )

                // Close Button
                IconButton(
                    onClick = {
                        viewModel.stopRealSpeechRecording()
                        onBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ViNoteSurfaceContainerHigh)
                        .testTag("voice_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ViNoteTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nota Avatar - Listening / Excited State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(ViNoteSecondaryFixed)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x1A171827)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isListening) "★ ★" else "⊙ ⊙",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001C38),
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isListening) ViNoteMintSuccess.copy(alpha = 0.2f) else ViNotePrimaryFixed.copy(alpha = 0.6f))
                        .border(
                            1.dp,
                            if (isListening) ViNoteMintSuccess else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isListening) ViNoteMintSuccess else ViNotePrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isListening) "MIC ACTIVE • SPEAK NOW" else "TAP MIC TO RECORD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) Color(0xFF0F6E3B) else ViNotePrimary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Transcription Bubble with Real-Time Speech Display
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0x14171827),
                        spotColor = Color(0x1A171827)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(ViNoteSurfaceContainerLowest)
                    .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayTranscript = voiceTranscript.ifBlank {
                        if (isListening) "Bicara transaksi kamu (contoh: Makan siang 35 ribu GoPay)..." else "Tap mic to start speaking"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"$displayTranscript",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (voiceTranscript.isBlank()) ViNoteTextSecondary else ViNoteTextPrimary,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center
                        )
                        if (isListening) {
                            Text(
                                text = "|",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ViNotePrimary.copy(alpha = cursorAlpha)
                            )
                        }
                        Text(
                            text = "\"",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (voiceTranscript.isBlank()) ViNoteTextSecondary else ViNoteTextPrimary
                        )
                    }

                    if (voiceTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEditingTranscript) "Done Editing" else "Edit Text ✏️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNotePrimary,
                            modifier = Modifier
                                .clickable { isEditingTranscript = !isEditingTranscript }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Editable input if toggled
            if (isEditingTranscript) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = voiceTranscript,
                    onValueChange = { viewModel.setVoiceTranscript(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ViNotePrimary,
                        unfocusedBorderColor = ViNoteSecondaryFixed
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { isEditingTranscript = false })
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Real Mic Button & Volume-Reactive Waves
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clickable {
                        if (!hasAudioPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleSpeechRecording()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Fluid Ripple Waves Reacting to Live Microphone Input Volume
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(waveScale1 + liveRmsBonus)
                            .clip(CircleShape)
                            .background(ViNoteMintSuccess.copy(alpha = waveAlpha1 * 0.35f))
                            .border(1.dp, ViNoteMintSuccess.copy(alpha = waveAlpha1 * 0.4f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(waveScale2 + (liveRmsBonus * 0.7f))
                            .clip(CircleShape)
                            .background(ViNotePrimaryContainer.copy(alpha = waveAlpha2 * 0.25f))
                            .border(1.dp, ViNotePrimaryContainer.copy(alpha = waveAlpha2 * 0.3f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(waveScale3)
                            .clip(CircleShape)
                            .background(ViNoteSecondaryContainer.copy(alpha = waveAlpha3 * 0.25f))
                            .border(1.dp, ViNoteSecondaryContainer.copy(alpha = waveAlpha3 * 0.3f), CircleShape)
                    )
                }

                // Pulsing Mic Core
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(1f + (liveRmsBonus * 0.25f))
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = ViNotePrimary.copy(alpha = 0.5f),
                            spotColor = ViNotePrimary.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(if (isListening) ViNoteMintSuccess else ViNotePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sample Utterances Quick Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "TRY SPEAKING (OR TAP SAMPLE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextSecondary,
                    letterSpacing = 0.08.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "🍛 Makan Padang 35rb GoPay" to "Tadi makan siang nasi padang 35 ribu pakai GoPay",
                        "☕ Kopi Kenangan 25rb BCA" to "Beli kopi kenangan 25 ribu pakai BCA",
                        "🚕 Grab ke Kantor 28rb OVO" to "Grab ke kantor 28 ribu pakai OVO",
                        "🛒 Supermarket 150rb Mandiri" to "Belanja bulanan 150 ribu debit Mandiri"
                    )
                    presets.forEach { (label, fullText) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ViNoteSurfaceContainerLow)
                                .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(50))
                                .clickable {
                                    viewModel.setVoiceTranscript(fullText)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNoteTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Parsed Entity Output Card
            parsedEntity?.let { entity ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = Color(0x14171827)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(ViNoteSurfaceContainerLowest)
                        .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSACTION DETECTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextSecondary,
                                letterSpacing = 0.08.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = ViNoteMintSuccess,
                                    modifier = Modifier.size(15.dp)
                                    )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (aiEngineStatus.isOnline) "⚡ Hugging Face NLP" else "🔒 On-Device NLP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F6E3B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VoiceEntityPill(
                                icon = Icons.Default.Payments,
                                label = "Amount",
                                value = FormatUtils.formatRupiah(entity.amount),
                                bgColor = ViNoteTertiaryFixed,
                                tint = Color(0xFF6E2900),
                                modifier = Modifier.weight(1f)
                            )
                            VoiceEntityPill(
                                icon = Icons.Default.Storefront,
                                label = "Merchant",
                                value = entity.merchant,
                                bgColor = ViNoteSecondaryFixed,
                                tint = ViNotePrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VoiceEntityPill(
                                icon = Icons.Default.Category,
                                label = "Category",
                                value = entity.category,
                                bgColor = ViNoteSurfaceContainerLow,
                                tint = ViNoteTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            VoiceEntityPill(
                                icon = Icons.Default.Payments,
                                label = "Wallet",
                                value = entity.walletName ?: "GoPay",
                                bgColor = ViNoteMintSuccess.copy(alpha = 0.2f),
                                tint = Color(0xFF0F6E3B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action: Confirm and Add to Ledger Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(50), ambientColor = ViNotePrimary.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(50))
                    .background(ViNotePrimary)
                    .clickable {
                        viewModel.stopRealSpeechRecording()
                        viewModel.processVoiceInput()
                        viewModel.confirmPendingTransaction()
                        onBack()
                    }
                    .testTag("voice_done_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirm",
                        tint = ViNoteOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Save Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteOnPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(28.dp))
        }
    }
}

@Composable
private fun VoiceEntityPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    bgColor: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = tint.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}
