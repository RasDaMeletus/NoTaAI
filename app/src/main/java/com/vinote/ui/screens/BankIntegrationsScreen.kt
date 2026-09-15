package com.vinote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.data.model.BankAccountItem
import com.vinote.data.model.EwalletLinkingState
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.components.ViNoteCard
import com.vinote.ui.theme.ViNoteError
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.ViNoteViewModel

enum class BankFilterTab {
    ALL,
    BANKS,
    E_WALLETS
}

@Composable
fun BankIntegrationsScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val totalBankBalance by viewModel.totalBankBalance.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

    val linkingState by viewModel.ewalletLinkingState.collectAsState()
    val focusManager = LocalFocusManager.current
    var activeLinkingWalletId by remember { mutableStateOf<String?>(null) }
    var activeLinkingWalletName by remember { mutableStateOf("") }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputOtpCode by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var activeLinkingReferenceId by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf(BankFilterTab.ALL) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var isSyncingAll by remember { mutableStateOf(false) }

    // Dialog state for adding a bank
    var newBankName by remember { mutableStateOf("Bank Central Asia (BCA)") }
    var newAccountNumber by remember { mutableStateOf("•••• 9988") }
    var newInitialBalanceText by remember { mutableStateOf("1500000") }
    var newAccountType by remember { mutableStateOf("Bank") }

    val filteredList = bankAccounts.filter { account ->
        when (selectedTab) {
            BankFilterTab.ALL -> true
            BankFilterTab.BANKS -> account.bankType == "Bank"
            BankFilterTab.E_WALLETS -> account.bankType == "E-Wallet"
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bank_sync_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "spin_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ViNoteSurfaceContainerLow)
                                .testTag("bank_integrations_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ViNoteTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Bank Integrations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ViNoteTextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ViNotePrimary)
                            .testTag("bank_add_account_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bank",
                            tint = Color.White
                        )
                    }
                }
            }

            // Total Aggregated Balance Card
            item {
                ViNoteCard(
                    padding = 20.dp,
                    backgroundColor = ViNotePrimary
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL CONNECTED BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 0.05.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "256-bit Encrypted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = FormatUtils.formatRupiah(totalBankBalance),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.02).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Button: Sync All Statements
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable {
                                    isSyncingAll = true
                                    viewModel.syncAllBankStatements()
                                }
                                .padding(vertical = 10.dp, horizontal = 14.dp)
                                .testTag("bank_sync_all_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(if (isSyncingAll) spinAngle else 0f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sync All Bank & E-Wallet Statements",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs (All, Banks, E-Wallets)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(ViNoteSurfaceContainerLow)
                        .padding(4.dp)
                ) {
                    val tabs = listOf(
                        BankFilterTab.ALL to "All (${bankAccounts.size})",
                        BankFilterTab.BANKS to "Banks (${bankAccounts.count { it.bankType == "Bank" }})",
                        BankFilterTab.E_WALLETS to "E-Wallets (${bankAccounts.count { it.bankType == "E-Wallet" }})"
                    )

                    tabs.forEach { (tab, title) ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) ViNotePrimary else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp)
                                .testTag("bank_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else ViNoteTextSecondary
                            )
                        }
                    }
                }
            }

            // Accounts List
            items(filteredList, key = { it.id }) { item ->
                ViNoteCard(padding = 16.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(item.brandColorHex))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.bankType == "Bank") Icons.Default.AccountBalance else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.bankName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                    val holderSuffix = if (item.accountHolder.isNotBlank()) " • ${item.accountHolder}" else ""
                                    Text(
                                        text = "${item.accountNumber}$holderSuffix",
                                        fontSize = 12.sp,
                                        color = ViNoteTextSecondary
                                    )
                                }
                            }

                            // Connection status pill / Toggle
                            if (item.isConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ViNoteMintSuccess.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteMintSuccess
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ViNoteSecondaryFixed)
                                        .clickable {
                                            if (item.bankType == "E-Wallet") {
                                                activeLinkingWalletId = item.id
                                                activeLinkingWalletName = item.bankName
                                                inputPhoneNumber = ""
                                                inputOtpCode = ""
                                                activeLinkingReferenceId = ""
                                                viewModel.resetEwalletLinkingState()
                                                showOtpDialog = true
                                            } else {
                                                viewModel.toggleBankConnection(item.id)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("bank_link_${item.id}_btn")
                                ) {
                                    Text(
                                        text = "Link Account",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNotePrimary
                                    )
                                }
                            }
                        }

                        if (item.isConnected) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Balance",
                                        fontSize = 11.sp,
                                        color = ViNoteTextSecondary
                                    )
                                    Text(
                                        text = FormatUtils.formatRupiah(item.balance),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Auto-Sync",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ViNoteTextPrimary
                                        )
                                        Text(
                                            text = item.lastSyncedTime,
                                            fontSize = 10.sp,
                                            color = ViNoteTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = item.isAutoSync,
                                        onCheckedChange = { viewModel.toggleBankAutoSync(item.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = ViNotePrimary
                                        ),
                                        modifier = Modifier.testTag("bank_autosync_${item.id}_toggle")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Unlink option
                            Text(
                                text = "Disconnect account",
                                fontSize = 11.sp,
                                color = ViNoteError,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { viewModel.toggleBankConnection(item.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialog for adding a new bank / wallet
        if (showAddAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAddAccountDialog = false },
                title = {
                    Text(
                        text = "Link Bank or E-Wallet",
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select financial institution & details:",
                            fontSize = 13.sp,
                            color = ViNoteTextSecondary
                        )

                        val bankOptions = listOf(
                            "Bank Central Asia (BCA)" to "Bank",
                            "Bank Mandiri (Livin')" to "Bank",
                            "Bank BNI" to "Bank",
                            "Bank BRI (BRImo)" to "Bank",
                            "Bank Jago" to "Bank",
                            "SeaBank" to "Bank",
                            "GoPay" to "E-Wallet",
                            "OVO" to "E-Wallet",
                            "DANA" to "E-Wallet",
                            "ShopeePay" to "E-Wallet"
                        )

                        OutlinedTextField(
                            value = newAccountNumber,
                            onValueChange = { newAccountNumber = it },
                            label = { Text("Account Number / Phone", color = ViNoteTextSecondary) },
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ViNoteTextPrimary,
                                unfocusedTextColor = ViNoteTextPrimary,
                                focusedBorderColor = ViNotePrimary,
                                unfocusedBorderColor = Color(0xFFDDE3EA),
                                focusedContainerColor = ViNoteSurfaceContainerLowest,
                                unfocusedContainerColor = ViNoteSurfaceContainerLowest,
                                cursorColor = ViNotePrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bank_dialog_account_field")
                        )

                        OutlinedTextField(
                            value = newInitialBalanceText,
                            onValueChange = { newInitialBalanceText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Initial Balance (Rp)", color = ViNoteTextSecondary) },
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ViNoteTextPrimary,
                                unfocusedTextColor = ViNoteTextPrimary,
                                focusedBorderColor = ViNotePrimary,
                                unfocusedBorderColor = Color(0xFFDDE3EA),
                                focusedContainerColor = ViNoteSurfaceContainerLowest,
                                unfocusedContainerColor = ViNoteSurfaceContainerLowest,
                                cursorColor = ViNotePrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bank_dialog_balance_field")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val bal = newInitialBalanceText.toLongOrNull() ?: 1000000L
                            viewModel.connectNewBank(
                                bankName = newBankName,
                                accountNumber = newAccountNumber,
                                balance = bal,
                                type = newAccountType
                            )
                            showAddAccountDialog = false
                        }
                    ) {
                        Text("Connect & Link", color = ViNotePrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAccountDialog = false }) {
                        Text("Cancel", color = ViNoteTextSecondary)
                    }
                }
            )
        }

        // E-Wallet OTP Linking Dialog
        if (showOtpDialog && activeLinkingWalletId != null) {
            AlertDialog(
                onDismissRequest = {
                    showOtpDialog = false
                    viewModel.resetEwalletLinkingState()
                },
                confirmButton = {},
                title = {
                    Text(
                        text = "Link $activeLinkingWalletName",
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (val state = linkingState) {
                            is EwalletLinkingState.Idle -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Masukkan nomor telepon terdaftar di $activeLinkingWalletName untuk menerima OTP:",
                                        fontSize = 13.sp,
                                        color = ViNoteTextSecondary
                                    )
                                    OutlinedTextField(
                                        value = inputPhoneNumber,
                                        onValueChange = { inputPhoneNumber = it.filter { it.isDigit() } },
                                        label = { Text("Nomor Telepon", color = ViNoteTextSecondary) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = ViNoteTextPrimary,
                                            unfocusedTextColor = ViNoteTextPrimary,
                                            focusedBorderColor = ViNotePrimary,
                                            unfocusedBorderColor = Color(0xFFDDE3EA),
                                            focusedContainerColor = ViNoteSurfaceContainerLowest,
                                            unfocusedContainerColor = ViNoteSurfaceContainerLowest,
                                            cursorColor = ViNotePrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ewallet_otp_phone_field")
                                    )
                                    ViNoteButton(
                                        text = "Kirim OTP",
                                        onClick = {
                                            if (inputPhoneNumber.length >= 10) {
                                                viewModel.sendEwalletOtp(activeLinkingWalletId!!, inputPhoneNumber)
                                            }
                                        },
                                        type = ViNoteButtonType.PRIMARY,
                                        enabled = inputPhoneNumber.length >= 10,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ewallet_send_otp_btn")
                                    )
                                }
                            }
                            is EwalletLinkingState.SendingOtp -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(color = ViNotePrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Mengirim OTP...", color = ViNoteTextPrimary, fontSize = 13.sp)
                                }
                            }
                            is EwalletLinkingState.AwaitingOtp -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ViNoteMintSuccess,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = "OTP terkirim ke ${inputPhoneNumber.take(3)}****${inputPhoneNumber.takeLast(3)}",
                                        fontSize = 13.sp,
                                        color = ViNoteTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedTextField(
                                        value = inputOtpCode,
                                        onValueChange = { inputOtpCode = it.filter { it.isDigit() }.take(6) },
                                        label = { Text("Kode OTP (6 digit)", color = ViNoteTextSecondary) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (inputOtpCode.length == 6) {
                                                activeLinkingReferenceId = state.referenceId
                                                viewModel.verifyEwalletOtp(activeLinkingWalletId!!, inputPhoneNumber, inputOtpCode, state.referenceId)
                                            }
                                        }),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = ViNoteTextPrimary,
                                            unfocusedTextColor = ViNoteTextPrimary,
                                            focusedBorderColor = ViNotePrimary,
                                            unfocusedBorderColor = Color(0xFFDDE3EA),
                                            focusedContainerColor = ViNoteSurfaceContainerLowest,
                                            unfocusedContainerColor = ViNoteSurfaceContainerLowest,
                                            cursorColor = ViNotePrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ewallet_otp_code_field")
                                    )
                                    ViNoteButton(
                                        text = "Verifikasi & Link",
                                        onClick = {
                                            if (inputOtpCode.length == 6) {
                                                viewModel.verifyEwalletOtp(activeLinkingWalletId!!, inputPhoneNumber, inputOtpCode, state.referenceId)
                                            }
                                        },
                                        type = ViNoteButtonType.PRIMARY,
                                        enabled = inputOtpCode.length == 6,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ewallet_verify_otp_btn")
                                    )
                                }
                            }
                            is EwalletLinkingState.VerifyingOtp -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(color = ViNotePrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Memverifikasi OTP...", color = ViNoteTextPrimary, fontSize = 13.sp)
                                }
                            }
                            is EwalletLinkingState.Success -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ViNoteMintSuccess,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "$activeLinkingWalletName berhasil ditautkan!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteMintSuccess
                                    )
                                    Text(
                                        text = "Saldo akan tersinkron otomatis.",
                                        fontSize = 13.sp,
                                        color = ViNoteTextSecondary
                                    )
                                    ViNoteButton(
                                        text = "Tutup",
                                        onClick = {
                                            showOtpDialog = false
                                            viewModel.resetEwalletLinkingState()
                                        },
                                        type = ViNoteButtonType.PRIMARY,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ewallet_linked_close_btn")
                                    )
                                }
                            }
                            is EwalletLinkingState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Gagal menautkan $activeLinkingWalletName",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteError
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 13.sp,
                                        color = ViNoteTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ViNoteButton(
                                            text = "Coba Lagi",
                                            onClick = {
                                                viewModel.resetEwalletLinkingState()
                                            },
                                            type = ViNoteButtonType.SECONDARY,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ViNoteButton(
                                            text = "Tutup",
                                            onClick = {
                                                showOtpDialog = false
                                                viewModel.resetEwalletLinkingState()
                                            },
                                            type = ViNoteButtonType.PRIMARY,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
