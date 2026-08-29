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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.theme.ViNoteError
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetExceededDialog(
    viewModel: ViNoteViewModel,
    modifier: Modifier = Modifier
) {
    val alertState by viewModel.budgetAlertState.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

    var showAdjustLimit by remember { mutableStateOf(false) }
    var newLimitText by remember(alertState.spentToday) {
        mutableStateOf((alertState.spentToday + 50000L).toString())
    }

    if (alertState.isTriggered && !alertState.isDismissed) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.dismissBudgetAlert() },
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(2.dp, ViNoteError.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Close button top right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { viewModel.dismissBudgetAlert() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ViNoteSurfaceContainerLow)
                                .testTag("budget_alert_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ViNoteTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // FURIOUS NOTA AVATAR
                    NotaAvatar(
                        size = 100.dp,
                        eyeState = NotaEyeState.FURIOUS,
                        baseColor = notaConfig.baseColor,
                        accessory = notaConfig.accessory,
                        showSparkle = false
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "BUDGET EXCEEDED! 💢",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ViNoteError,
                        letterSpacing = (-0.02).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Nota is furious because your spending crossed the daily budget safety barrier!",
                        fontSize = 13.sp,
                        color = ViNoteTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Overage Breakdown Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF0F0))
                            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent Today:",
                                    fontSize = 13.sp,
                                    color = ViNoteTextPrimary
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(alertState.spentToday),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteError
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Limit Cap:",
                                    fontSize = 13.sp,
                                    color = ViNoteTextPrimary
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(alertState.dailyLimit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ViNoteTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFFFCDD2))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Over Budget By:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNoteError
                                )
                                Text(
                                    text = "+${FormatUtils.formatRupiah(alertState.overageAmount)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ViNoteError
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showAdjustLimit) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Set New Daily Budget Limit (Rp):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            OutlinedTextField(
                                value = newLimitText,
                                onValueChange = { newLimitText = it.filter { ch -> ch.isDigit() } },
                                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ViNotePrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            ViNoteButton(
                                text = "Confirm & Calm Nota 💙",
                                onClick = {
                                    val newLim = newLimitText.toLongOrNull() ?: alertState.spentToday
                                    viewModel.calmNotaDown(newLim)
                                    showAdjustLimit = false
                                },
                                testTag = "budget_confirm_new_limit_btn"
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ViNoteButton(
                                text = "I Promise to Stop (Calm Nota) 🕊️",
                                onClick = { viewModel.calmNotaDown() },
                                testTag = "budget_calm_nota_btn"
                            )

                            ViNoteButton(
                                text = "Adjust Daily Limit",
                                type = ViNoteButtonType.SECONDARY,
                                onClick = { showAdjustLimit = true },
                                testTag = "budget_adjust_limit_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}
