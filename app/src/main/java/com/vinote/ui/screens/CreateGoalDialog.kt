package com.vinote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    viewModel: ViNoteViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var targetDurationMonths by remember { mutableStateOf("3") }

    val targetAmount = targetAmountStr.toLongOrNull() ?: 0L
    val months = targetDurationMonths.toIntOrNull() ?: 3
    val dailySavingEstimate = if (months > 0 && targetAmount > 0) targetAmount / (months * 30L) else 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ViNoteSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Savings Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ViNoteSurfaceContainerLow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ViNoteTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Title Input
            Text(
                text = "WHAT ARE YOU SAVING FOR?",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextSecondary,
                letterSpacing = 0.05.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. New Laptop, Tokyo Trip") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Target Amount Input
            Text(
                text = "TARGET AMOUNT (IDR)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextSecondary,
                letterSpacing = 0.05.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = targetAmountStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) targetAmountStr = it },
                placeholder = { Text("e.g. 2000000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_amount_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Duration in months
            Text(
                text = "TIMELINE (MONTHS)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextSecondary,
                letterSpacing = 0.05.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = targetDurationMonths,
                onValueChange = { if (it.all { char -> char.isDigit() }) targetDurationMonths = it },
                placeholder = { Text("3") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Nota Tip Box
            if (targetAmount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ViNoteSecondaryFixed.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ViNotePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Tip",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nota's Calculation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                            Text(
                                text = "Save ~${FormatUtils.formatRupiah(dailySavingEstimate)}/day to reach this goal in $months months!",
                                fontSize = 12.sp,
                                color = ViNoteTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ViNoteButton(
                text = "Create Goal",
                onClick = {
                    if (title.isNotBlank() && targetAmount > 0) {
                        viewModel.createGoal(
                            title = title,
                            targetAmount = targetAmount,
                            targetDate = "In $months months"
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && targetAmount > 0,
                testTag = "submit_goal_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
