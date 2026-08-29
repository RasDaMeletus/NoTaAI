package com.vinote.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
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
fun QuickSetupScreen(
    viewModel: ViNoteViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val focusManager = LocalFocusManager.current

    var step by remember { mutableIntStateOf(1) } // 1: Income & Budget, 2: Savings & Goal, 3: Nota Companion

    // Setup fields
    var monthlyIncomeText by remember { mutableStateOf("5000000") }
    var dailyLimitText by remember { mutableStateOf("180000") }
    var savingsPercentage by remember { mutableFloatStateOf(20f) }
    var selectedGoalPreset by remember { mutableStateOf("Emergency Fund") }
    var goalTargetText by remember { mutableStateOf("3000000") }
    var selectedColor by remember { mutableStateOf(NotaBaseColor.SOFT_PINK) }

    val monthlyIncome = monthlyIncomeText.toLongOrNull() ?: 5000000L
    val dailyLimit = dailyLimitText.toLongOrNull() ?: 180000L
    val goalTarget = goalTargetText.toLongOrNull() ?: 3000000L
    val calculatedMonthlySavings = (monthlyIncome * (savingsPercentage / 100f)).toLong()
    val calculatedSafeMoney = (monthlyIncome - calculatedMonthlySavings - (dailyLimit * 20)).coerceAtLeast(500000L)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Step Indicator Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Setup",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ViNoteSecondaryFixed)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Step $step of 3",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNotePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Companion Reaction Avatar
            NotaAvatar(
                size = 88.dp,
                eyeState = when (step) {
                    1 -> NotaEyeState.CURIOUS
                    2 -> NotaEyeState.PROUD
                    else -> NotaEyeState.EXCITED
                },
                baseColor = selectedColor,
                showSparkle = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "quick_setup_step_transition"
            ) { currentStep ->
                when (currentStep) {
                    1 -> {
                        // STEP 1: Income & Daily Budget Limit
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "What is your monthly income and daily spending cap?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ViNoteCard(padding = 18.dp) {
                                Text(
                                    text = "MONTHLY INCOME",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = monthlyIncomeText,
                                    onValueChange = { monthlyIncomeText = it.filter { ch -> ch.isDigit() } },
                                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ViNotePrimary) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ViNotePrimary,
                                        unfocusedBorderColor = Color(0xFFDDE3EA),
                                        focusedContainerColor = ViNoteSurfaceContainerLowest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quicksetup_income_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "DAILY SPENDING BUDGET (MAX)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = dailyLimitText,
                                    onValueChange = { dailyLimitText = it.filter { ch -> ch.isDigit() } },
                                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ViNotePrimary) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ViNotePrimary,
                                        unfocusedBorderColor = Color(0xFFDDE3EA),
                                        focusedContainerColor = ViNoteSurfaceContainerLowest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quicksetup_daily_budget_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "💡 Nota will keep you within ${FormatUtils.formatRupiah(dailyLimit)} / day and alert you if exceeded!",
                                    fontSize = 12.sp,
                                    color = ViNotePrimary
                                )
                            }
                        }
                    }

                    2 -> {
                        // STEP 2: Savings Goal & Target %
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Set your savings target & first dream goal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ViNoteCard(padding = 18.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target Savings Rate",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = "${savingsPercentage.toInt()}% (${FormatUtils.formatRupiah(calculatedMonthlySavings)}/mo)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = ViNotePrimary
                                    )
                                }

                                Slider(
                                    value = savingsPercentage,
                                    onValueChange = { savingsPercentage = it },
                                    valueRange = 5f..50f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ViNotePrimary,
                                        activeTrackColor = ViNotePrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "PICK YOUR FIRST GOAL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val goalPresets = listOf(
                                    "Emergency Fund" to 3000000L,
                                    "Trip to Japan" to 15000000L,
                                    "New Laptop" to 8000000L,
                                    "New Headphones" to 1500000L
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    goalPresets.forEach { (title, target) ->
                                        val isSelected = selectedGoalPreset == title
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) ViNoteSecondaryFixed else ViNoteSurfaceContainerLowest)
                                                .border(
                                                    1.5.dp,
                                                    if (isSelected) ViNotePrimary else Color(0xFFE2E8F0),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    selectedGoalPreset = title
                                                    goalTargetText = target.toString()
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = ViNoteTextPrimary
                                                    )
                                                    Text(
                                                        text = "Target: ${FormatUtils.formatRupiah(target)}",
                                                        fontSize = 12.sp,
                                                        color = ViNoteTextSecondary
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = ViNotePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // STEP 3: Customize NoTa Companion & Summary
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Choose NoTa's companion aura & review",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ViNoteCard(padding = 18.dp) {
                                Text(
                                    text = "COMPANION COLOR PALETTE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteTextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val colors = listOf(
                                        NotaBaseColor.SOFT_PINK to ("Soft Pink" to ViNoteSoftPink),
                                        NotaBaseColor.SOFT_BLUE to ("Sky Blue" to ViNoteSecondaryFixed),
                                        NotaBaseColor.WARM_YELLOW to ("Warm Sun" to ViNoteWarmYellow),
                                        NotaBaseColor.MINT_GREEN to ("Mint Zen" to Color(0xFFE8F5E9))
                                    )

                                    colors.forEach { (colorEnum, info) ->
                                        val isSelected = selectedColor == colorEnum
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedColor = colorEnum }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(info.second)
                                                    .border(
                                                        2.5.dp,
                                                        if (isSelected) ViNotePrimary else Color.Transparent,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = ViNotePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = info.first,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = ViNoteTextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Financial Safety Summary
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ViNoteSecondaryFixed.copy(alpha = 0.5f))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "🛡️ Est. Safe Money (Uang Aman):", fontSize = 13.sp, color = ViNoteTextPrimary)
                                            Text(text = FormatUtils.formatRupiah(calculatedSafeMoney), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ViNotePrimary)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "🎯 First Goal Target:", fontSize = 13.sp, color = ViNoteTextPrimary)
                                            Text(text = FormatUtils.formatRupiah(goalTarget), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ViNoteTextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 1) {
                    Box(modifier = Modifier.weight(1f)) {
                        ViNoteButton(
                            text = "Back",
                            type = ViNoteButtonType.SECONDARY,
                            onClick = { step-- },
                            testTag = "quicksetup_back_step_btn"
                        )
                    }
                }

                Box(modifier = Modifier.weight(if (step > 1) 1.5f else 1f)) {
                    ViNoteButton(
                        text = if (step < 3) "Continue" else "Launch ViNote 🚀",
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                viewModel.completeQuickSetup(
                                    monthlyIncome = monthlyIncome,
                                    dailyBudgetLimit = dailyLimit,
                                    savingsPercentage = savingsPercentage.toInt(),
                                    firstGoalTitle = selectedGoalPreset,
                                    firstGoalTarget = goalTarget,
                                    startingColor = selectedColor
                                )
                                onSetupComplete()
                            }
                        },
                        testTag = "quicksetup_next_step_btn"
                    )
                }
            }
        }
    }
}
