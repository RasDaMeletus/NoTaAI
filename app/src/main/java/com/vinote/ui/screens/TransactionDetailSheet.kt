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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.theme.ViNoteError
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionItem,
    viewModel: ViNoteViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Amount
            val isExpense = transaction.type == TransactionType.EXPENSE
            val prefix = if (isExpense) "-" else "+"
            val amountColor = if (isExpense) ViNoteTextPrimary else ViNoteMintSuccess

            Text(
                text = "$prefix${FormatUtils.formatRupiah(transaction.amount)}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
                letterSpacing = (-0.02).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = transaction.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ViNoteSurfaceContainerLow)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = transaction.category
                    )
                    DetailRow(
                        icon = Icons.Default.Store,
                        label = "Merchant",
                        value = transaction.merchant.ifEmpty { transaction.title }
                    )
                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Time",
                        value = transaction.timeLabel
                    )
                    DetailRow(
                        icon = Icons.Default.Payment,
                        label = "Source",
                        value = "${transaction.source.name} (${transaction.walletName ?: "Cash / Card"})"
                    )
                    DetailRow(
                        icon = Icons.Default.CloudDone,
                        label = "Cloud Sync",
                        value = "Synced with Firestore"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Transaction Button
            ViNoteButton(
                text = "Delete Transaction",
                onClick = { showDeleteConfirmDialog = true },
                type = ViNoteButtonType.SECONDARY,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ViNoteError,
                        modifier = Modifier.size(18.dp)
                    )
                },
                testTag = "delete_transaction_btn"
            )

            Spacer(modifier = Modifier.height(10.dp))

            ViNoteButton(
                text = "Close",
                onClick = onDismiss,
                type = ViNoteButtonType.GHOST,
                testTag = "close_detail_sheet_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Transaction?",
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this transaction from your local database and Firestore remote sync.",
                    color = ViNoteTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTransaction(transaction.id)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_delete_dialog_btn")
                ) {
                    Text("Delete", color = ViNoteError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = ViNoteTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ViNoteSecondaryFixed.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ViNotePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = ViNoteTextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ViNoteTextPrimary
        )
    }
}
