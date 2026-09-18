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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.TransactionType
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

// Default categories for Expense
val defaultExpenseCategories = listOf(
    "Food", "Transport", "Shopping", "Bills",
    "Coffee", "Entertainment", "Health", "Education"
)

// Default categories for Income
val defaultIncomeCategories = listOf(
    "Salary", "Freelance", "Gift", "Investment",
    "Refund", "Bonus", "Other"
)

@Composable
fun AddTransactionScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keypadAmount by viewModel.keypadAmount.collectAsState()
    val templates by viewModel.transactionTemplates.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedTransactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var newCustomCategoryName by remember { mutableStateOf("") }

    // Merge default + custom categories based on selected type
    val activeCategories = remember(selectedTransactionType, customCategories) {
        val defaults = if (selectedTransactionType == TransactionType.EXPENSE) {
            defaultExpenseCategories
        } else {
            defaultIncomeCategories
        }
        val custom = customCategories.filter {
            it.type == selectedTransactionType
        }.map { it.name }
        (defaults + custom).distinct()
    }

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

            // Amount display
            Text(
                text = FormatUtils.formatRupiah(amountLong),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ViNotePrimary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Transaction Type Toggle (Income / Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(ViNoteSurfaceContainerLow)
                    .padding(4.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income"
                ).forEach { (type, label) ->
                    val isSelected = selectedTransactionType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) ViNotePrimary else Color.Transparent)
                            .clickable {
                                selectedTransactionType = type
                                // Reset selection if current category doesn't belong to new type
                                if (activeCategories.none { it == selectedCategory }) {
                                    selectedCategory = activeCategories.firstOrNull() ?: ""
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else ViNoteTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(activeCategories) { cat ->
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
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else ViNoteTextSecondary
                        )
                    }
                }
                // Add custom category chip
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ViNoteSecondaryFixed)
                            .clickable { showCustomCategoryDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("add_custom_category_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom category",
                                tint = ViNotePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNotePrimary
                            )
                        }
                    }
                }
            }

            // Templates
            if (templates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(templates) { tmpl ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ViNoteSurfaceContainerLowest)
                                .border(1.dp, Color(0x1F747789), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.applyTemplate(tmpl)
                                    selectedCategory = tmpl.category
                                    selectedTransactionType = tmpl.type
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tmpl.name,
                                fontSize = 11.sp,
                                color = ViNoteTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Numpad (bottom area)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onNavigateToVoice) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = ViNotePrimary)
                }
                IconButton(onClick = onNavigateToScan) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Scan", tint = ViNotePrimary)
                }
                IconButton(onClick = { viewModel.clearKeypad() }) {
                    Icon(Icons.Default.Backspace, contentDescription = "Clear", tint = ViNoteTextSecondary)
                }
            }

            // Numeric keypad: the only way to enter an amount manually.
            // 3 columns x 4 rows -> 1-9, then 000 / 0 / backspace.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val keypadKeys = listOf(
                    "1", "2", "3",
                    "4", "5", "6",
                    "7", "8", "9",
                    "000", "0", "<-"
                )
                keypadKeys.chunked(3).forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ViNoteSurfaceContainerLow)
                                    .clickable {
                                        when (key) {
                                            "<-" -> viewModel.deleteKeypadDigit()
                                            else -> viewModel.appendKeypadDigit(key)
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ViNoteTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            ViNoteButton(
                text = if (selectedTransactionType == TransactionType.EXPENSE) "Confirm Expense" else "Confirm Income",
                onClick = {
                    if (amountLong > 0) {
                        viewModel.preparePendingTransactionFromKeypad(
                            category = selectedCategory,
                            title = selectedCategory,
                            type = selectedTransactionType
                        )
                        viewModel.clearKeypad()
                    }
                },
                type = ViNoteButtonType.PRIMARY
            )
        }
    }

    // Custom Category Dialog
    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomCategoryDialog = false
                newCustomCategoryName = ""
            },
            title = {
                Text(
                    "Add Custom Category",
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Category type: ${if (selectedTransactionType == TransactionType.EXPENSE) "Expense" else "Income"}",
                        fontSize = 12.sp,
                        color = ViNoteTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCustomCategoryName,
                        onValueChange = { newCustomCategoryName = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ViNotePrimary,
                            unfocusedBorderColor = Color(0xFFDDE3EA),
                            cursorColor = ViNotePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCustomCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCustomCategoryName.trim(),
                                type = selectedTransactionType
                            )
                            selectedCategory = newCustomCategoryName.trim()
                            showCustomCategoryDialog = false
                            newCustomCategoryName = ""
                        }
                    }
                ) {
                    Text("Add", color = ViNotePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomCategoryDialog = false
                        newCustomCategoryName = ""
                    }
                ) {
                    Text("Cancel", color = ViNoteTextSecondary)
                }
            }
        )
    }
}
