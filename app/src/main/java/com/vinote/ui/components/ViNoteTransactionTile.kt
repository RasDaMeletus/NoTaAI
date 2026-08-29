package com.vinote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.TransactionItem
import com.vinote.data.model.TransactionType
import com.vinote.ui.theme.ViNoteError
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurfaceContainerHigh
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow

@Composable
fun ViNoteTransactionTile(
    transaction: TransactionItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (iconVector, iconBgColor, iconTint) = when (transaction.category.lowercase()) {
        "food", "makanan", "dining" -> Triple(
            Icons.Default.Restaurant,
            ViNoteWarmYellow.copy(alpha = 0.5f),
            Color(0xFF8C4B00)
        )
        "coffee", "kopi" -> Triple(
            Icons.Default.Coffee,
            ViNoteSurfaceContainerHigh,
            ViNoteTextPrimary
        )
        "transport", "goride", "ojek" -> Triple(
            Icons.Default.DirectionsCar,
            ViNoteSecondaryFixed,
            ViNotePrimary
        )
        "income", "allowance", "gaji" -> Triple(
            Icons.Default.Payments,
            ViNoteMintSuccess.copy(alpha = 0.25f),
            ViNoteMintSuccess
        )
        "shopping", "belanja" -> Triple(
            Icons.Default.ShoppingBag,
            Color(0xFFFFD6E5),
            Color(0xFFBA1A1A)
        )
        else -> Triple(
            Icons.Default.AccountBalanceWallet,
            ViNoteSecondaryFixed,
            ViNotePrimary
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ViNoteSurfaceContainerLowest)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("transaction_tile_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left circular icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = transaction.category,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Title & Timestamp / Category
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )
            Text(
                text = transaction.timeLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = ViNoteTextSecondary
            )
        }

        // Right Amount in Bold
        val isExpense = transaction.type == TransactionType.EXPENSE
        val formattedAmount = (if (isExpense) "-" else "+") + FormatUtils.formatRupiah(transaction.amount)
        val amountColor = if (isExpense) {
            ViNoteTextPrimary
        } else {
            ViNoteMintSuccess
        }

        Text(
            text = formattedAmount,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
