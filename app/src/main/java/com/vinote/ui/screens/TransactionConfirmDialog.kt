package com.vinote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.NotaEyeState
import com.vinote.data.model.TransactionItem
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionConfirmDialog(
    transaction: TransactionItem,
    viewModel: ViNoteViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notaConfig by viewModel.notaConfig.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ViNoteSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nota Notification Hero
            NotaAvatar(
                size = 80.dp,
                eyeState = NotaEyeState.HAPPY,
                baseColor = notaConfig.baseColor,
                accessory = notaConfig.accessory,
                showCheckBadge = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "I noticed a payment!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )

            Text(
                text = "Would you like me to log this transaction?",
                fontSize = 14.sp,
                color = ViNoteTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Transaction Detail Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(ViNoteSurfaceContainerLow)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = FormatUtils.formatRupiah(transaction.amount),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ViNoteTextPrimary,
                        letterSpacing = (-0.02).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${transaction.merchant.ifEmpty { transaction.title }} • ${transaction.walletName ?: "Manual"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ViNotePrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ViNoteSecondaryFixed)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Category: ${transaction.category}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ViNotePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            ViNoteButton(
                text = "Confirm Transaction",
                onClick = {
                    viewModel.confirmPendingTransaction()
                },
                testTag = "confirm_tx_dialog_btn"
            )

            Spacer(modifier = Modifier.height(10.dp))

            ViNoteButton(
                text = "Not me / Cancel",
                onClick = {
                    viewModel.dismissPendingTransaction()
                },
                type = ViNoteButtonType.GHOST,
                testTag = "cancel_tx_dialog_btn"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
