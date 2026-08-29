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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun AddTransactionScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keypadAmount by viewModel.keypadAmount.collectAsState()
    var selectedCategory by remember { mutableStateOf("Food") }
    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Coffee", "Entertainment")

    val amountLong = keypadAmount.toLongOrNull() ?: 0L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ViNoteSurfaceContainerLow)
                        .testTag("add_tx_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ViNoteTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Add Transaction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Switcher Tabs (Manual, Scan, Voice)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ViNoteSurfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModeTab(
                    title = "Manual",
                    icon = Icons.Default.EditNote,
                    selected = true,
                    onClick = {}
                )
                ModeTab(
                    title = "Scan",
                    icon = Icons.Default.ReceiptLong,
                    selected = false,
                    onClick = onNavigateToScan
                )
                ModeTab(
                    title = "Voice",
                    icon = Icons.Default.Mic,
                    selected = false,
                    onClick = onNavigateToVoice
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Keypad Display
            Text(
                text = "AMOUNT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextSecondary,
                letterSpacing = 0.08.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = FormatUtils.formatRupiah(amountLong),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ViNoteTextPrimary,
                letterSpacing = (-0.02).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) ViNotePrimary else ViNoteSurfaceContainerLowest)
                            .then(
                                if (!isSelected) {
                                    Modifier.border(1.dp, Color(0x1F747789), RoundedCornerShape(50))
                                } else Modifier
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("cat_chip_$cat")
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else ViNoteTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Numeric Keypad (3x4 Grid)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeypadKey("1", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("1") }
                    KeypadKey("2", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("2") }
                    KeypadKey("3", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("3") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeypadKey("4", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("4") }
                    KeypadKey("5", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("5") }
                    KeypadKey("6", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("6") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeypadKey("7", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("7") }
                    KeypadKey("8", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("8") }
                    KeypadKey("9", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("9") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeypadKey("000", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("000") }
                    KeypadKey("0", modifier = Modifier.weight(1f)) { viewModel.appendKeypadDigit("0") }
                    KeypadBackspace(modifier = Modifier.weight(1f)) { viewModel.deleteKeypadDigit() }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Confirm Button
            ViNoteButton(
                text = "Confirm Amount",
                onClick = {
                    if (amountLong > 0) {
                        viewModel.preparePendingTransactionFromKeypad(category = selectedCategory, title = selectedCategory)
                    }
                },
                enabled = amountLong > 0,
                testTag = "confirm_amount_btn"
            )

            Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ViNotePrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color.White else ViNoteTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else ViNoteTextSecondary
            )
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ViNoteSurfaceContainerLowest)
            .clickable(onClick = onClick)
            .testTag("keypad_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ViNoteTextPrimary
        )
    }
}

@Composable
private fun KeypadBackspace(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ViNoteSurfaceContainerLowest)
            .clickable(onClick = onClick)
            .testTag("keypad_backspace"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Backspace,
            contentDescription = "Backspace",
            tint = ViNoteTextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
