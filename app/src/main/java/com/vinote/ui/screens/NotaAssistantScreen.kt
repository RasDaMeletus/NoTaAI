package com.vinote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.ChatMessage
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNoteOnPrimary
import com.vinote.ui.theme.ViNoteOutlineVariant
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNotePrimaryContainer
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSecondaryFixedDim
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainer
import com.vinote.ui.theme.ViNoteSurfaceContainerHigh
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun NotaAssistantScreen(
    viewModel: ViNoteViewModel,
    onNavigateToVoice: () -> Unit,
    onNavigateToCustomizeNota: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isNotaTyping by viewModel.isNotaTyping.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()
    val safeMoney by viewModel.safeMoney.collectAsState()
    val calculatedBalance by viewModel.currentCalculatedBalance.collectAsState()
    val todaySpent by viewModel.todaySpent.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Determine if we are in Hero (Empty) state or Active Conversation state
    val isHeroMode = chatMessages.size <= 1

    // Dynamic eyes reactivity based on user interaction & state
    val dynamicCustomEyes: String? = when {
        isNotaTyping -> "● ◌"
        inputText.isNotBlank() -> "● ◡ ●"
        isInputFocused -> "● ●"
        else -> null
    }

    val activeEyeState = when {
        isNotaTyping -> NotaEyeState.THINKING
        isInputFocused -> NotaEyeState.NEUTRAL
        else -> chatMessages.lastOrNull { !it.isUser }?.eyeState ?: NotaEyeState.CURIOUS
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(chatMessages.size, isNotaTyping) {
        if (!isHeroMode && chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size)
        }
    }

    // Ambient floating glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "assistant_motion")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_bubble"
    )
    val micRippleScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mic_ripple"
    )
    val micRippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mic_ripple_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
            .imePadding()
    ) {
        // 1. Subtle Immersive Ambient Glow Behind Nota
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .size(320.dp)
                .scale(pulseGlow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ViNoteSecondaryFixedDim.copy(alpha = 0.45f),
                            ViNoteSecondaryFixed.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 2. MINIMAL TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular Reset / Close button
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ViNoteSurfaceContainer)
                        .testTag("nota_close_chat_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close conversation",
                        tint = ViNoteTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Contextual Uppercase Tag
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ASSISTANT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 1.2.sp
                    )
                    if (!isHeroMode) {
                        Text(
                            text = if (isNotaTyping) "Analyzing finances..." else "NoTa is online",
                            fontSize = 10.sp,
                            color = if (isNotaTyping) ViNotePrimary else ViNoteMintSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right: Customize Theme Button
                IconButton(
                    onClick = onNavigateToCustomizeNota,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ViNoteSurfaceContainer)
                        .testTag("nota_customize_shortcut_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Customize NoTa",
                        tint = ViNoteTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // 3. MAIN CONVERSATIONAL CANVAS
            if (isHeroMode) {
                // ==================== HERO LANDING STATE ====================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Central Large Nota Mascot
                    Box(
                        modifier = Modifier.padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NotaAvatar(
                            size = 140.dp,
                            eyeState = activeEyeState,
                            customEyes = dynamicCustomEyes,
                            baseColor = notaConfig.baseColor,
                            accessory = notaConfig.accessory,
                            showSparkle = true,
                            isAnimated = true,
                            onClick = { viewModel.cycleNotaExpression() }
                        )
                    }

                    // Floating Speech Bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .offset(y = floatAnim.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color(0x14171827),
                                spotColor = Color(0x1A171827)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(ViNoteSurfaceContainerLowest)
                            .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hey! What are we figuring out today?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary,
                            lineHeight = 28.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Quick Action Chips (Pill shape, 46dp min height)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroQuickChip(
                                icon = Icons.Default.Payments,
                                label = "Can I afford this?",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.sendChatMessage("Can I afford dinner tonight?") }
                            )
                            HeroQuickChip(
                                icon = Icons.Default.QueryStats,
                                label = "Where did my money go?",
                                modifier = Modifier.weight(1.1f),
                                onClick = { viewModel.sendChatMessage("Where did my money go?") }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroQuickChip(
                                icon = Icons.Default.Savings,
                                label = "Help me save",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.sendChatMessage("Help me save money") }
                            )
                            HeroQuickChip(
                                icon = Icons.Default.TrackChanges,
                                label = "Check Safe Money",
                                modifier = Modifier.weight(1.1f),
                                onClick = { viewModel.sendChatMessage("What is my safe money?") }
                            )
                        }
                    }
                }
            } else {
                // ==================== ACTIVE CONVERSATION STATE ====================
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
                ) {
                    // Mini Mascot Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            NotaAvatar(
                                size = 52.dp,
                                eyeState = activeEyeState,
                                customEyes = dynamicCustomEyes,
                                baseColor = notaConfig.baseColor,
                                accessory = notaConfig.accessory,
                                isAnimated = true,
                                onClick = { viewModel.cycleNotaExpression() }
                            )
                        }
                    }

                    // Chat messages
                    items(chatMessages) { message ->
                        TalkToNotaBubble(
                            message = message,
                            notaEye = message.eyeState,
                            baseColor = notaConfig.baseColor,
                            accessory = notaConfig.accessory,
                            safeMoney = safeMoney,
                            calculatedBalance = calculatedBalance,
                            todaySpent = todaySpent,
                            onChipClicked = { chipText ->
                                viewModel.sendChatMessage(chipText)
                            }
                        )
                    }

                    // Typing Indicator
                    if (isNotaTyping) {
                        item {
                            NotaTypingBubble()
                        }
                    }
                }
            }

            // 4. PINNED FLOATING BOTTOM INPUT DOCK
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                ViNoteSurface.copy(alpha = 0.95f),
                                ViNoteSurface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Floating Rounded Pill Input Wrapper with focus glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color(0x14171827),
                            spotColor = if (isInputFocused) ViNotePrimary.copy(alpha = 0.25f) else Color(0x1F171827)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(ViNoteSurfaceContainerLowest)
                        .border(
                            width = if (isInputFocused) 1.5.dp else 1.dp,
                            color = if (isInputFocused) ViNotePrimary.copy(alpha = 0.45f) else ViNoteOutlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Text Field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ask Nota...",
                                    fontSize = 15.sp,
                                    color = ViNoteTextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = ViNoteTextPrimary,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(ViNotePrimary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            val query = inputText
                                            inputText = ""
                                            viewModel.sendChatMessage(query)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isInputFocused = it.isFocused }
                                    .testTag("chat_input_field")
                            )
                        }

                        // Right Action: Pulsing Voice Mic Button or Send Button
                        val hasText = inputText.isNotBlank()
                        if (hasText) {
                            IconButton(
                                onClick = {
                                    val query = inputText
                                    inputText = ""
                                    viewModel.sendChatMessage(query)
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ViNotePrimary)
                                    .testTag("chat_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = ViNoteOnPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        } else {
                            // Pulsing Voice Button with ripple
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable { onNavigateToVoice() }
                            ) {
                                // Animated Ripple layer
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .scale(micRippleScale)
                                        .clip(CircleShape)
                                        .background(ViNotePrimary.copy(alpha = micRippleAlpha))
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = CircleShape,
                                            ambientColor = ViNotePrimary.copy(alpha = 0.4f),
                                            spotColor = ViNotePrimary.copy(alpha = 0.4f)
                                        )
                                        .clip(CircleShape)
                                        .background(ViNotePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = ViNoteOnPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(68.dp)) // Bottom navigation spacing
        }
    }
}

@Composable
private fun HeroQuickChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(50), ambientColor = Color(0x0A171827))
            .clip(RoundedCornerShape(50))
            .background(ViNoteSurfaceContainerLow)
            .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ViNotePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ViNoteTextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TalkToNotaBubble(
    message: ChatMessage,
    notaEye: NotaEyeState,
    baseColor: NotaBaseColor,
    accessory: com.vinote.data.model.NotaAccessory,
    safeMoney: Long,
    calculatedBalance: Long,
    todaySpent: Long,
    onChipClicked: (String) -> Unit
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                NotaAvatar(
                    size = 24.dp,
                    eyeState = notaEye,
                    baseColor = baseColor,
                    accessory = accessory,
                    isAnimated = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NoTa",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNotePrimary
                )
            }
        }

        // Main Bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(if (isUser) ViNotePrimary else ViNoteSurfaceContainerLowest)
                .then(
                    if (!isUser) {
                        Modifier.border(
                            1.dp,
                            ViNoteOutlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 20.dp
                            )
                        )
                    } else Modifier
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .fillMaxWidth(if (isUser) 0.8f else 0.9f)
        ) {
            Column {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isUser) Color.White else ViNoteTextPrimary,
                    lineHeight = 22.sp
                )

                // Contextual Structured Financial Card if message answers financial status
                if (!isUser && (message.text.contains("Safe Money", ignoreCase = true) || message.text.contains("Uang Aman", ignoreCase = true))) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StructuredFinancialCard(
                        title = "Guilt-Free Spending (Uang Aman)",
                        amount = FormatUtils.formatRupiah(safeMoney),
                        subtitle = "Calculated after locked savings & daily limits",
                        badge = "Optimal 🛡️",
                        badgeColor = ViNoteMintSuccess.copy(alpha = 0.2f),
                        badgeTextColor = Color(0xFF00796B)
                    )
                } else if (!isUser && (message.text.contains("spent", ignoreCase = true) || message.text.contains("Food", ignoreCase = true) || message.text.contains("habis", ignoreCase = true))) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StructuredFinancialCard(
                        title = "Today's Total Expenditure",
                        amount = FormatUtils.formatRupiah(todaySpent),
                        subtitle = "Tracked across connected e-wallets & cards",
                        badge = "Discretionary ⚡",
                        badgeColor = ViNoteSecondaryFixed.copy(alpha = 0.6f),
                        badgeTextColor = ViNotePrimary
                    )
                }
            }
        }

        // Quick action chips below NoTa messages
        if (!isUser && message.quickChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                message.quickChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ViNoteSecondaryFixed.copy(alpha = 0.6f))
                            .border(1.dp, ViNotePrimary.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .clickable { onChipClicked(chip) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ViNotePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StructuredFinancialCard(
    title: String,
    amount: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ViNoteSurfaceContainerLow)
            .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ViNoteTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = ViNoteTextSecondary
            )
        }
    }
}

@Composable
private fun NotaTypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ViNoteSurfaceContainerLow)
            .border(1.dp, ViNoteOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot1.dp)
                    .clip(CircleShape)
                    .background(ViNotePrimary)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot2.dp)
                    .clip(CircleShape)
                    .background(ViNotePrimary)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = dot3.dp)
                    .clip(CircleShape)
                    .background(ViNotePrimary)
            )
        }
        Text(
            text = "NoTa is thinking...",
            fontSize = 13.sp,
            color = ViNoteTextSecondary
        )
    }
}
