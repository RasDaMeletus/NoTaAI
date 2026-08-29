package com.vinote.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ProfileSettingsScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToBankIntegrations: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var monthlyIncomeText by remember(profile) { mutableStateOf(profile.monthlyIncome.toString()) }
    var dailyLimitText by remember(profile) { mutableStateOf(profile.dailyBudgetLimit.toString()) }
    var savingsPercentage by remember(profile) { mutableFloatStateOf(profile.savingsTargetPercentage.toFloat()) }
    var currencyCode by remember(profile) { mutableStateOf(profile.currencyCode) }
    var financialPersona by remember(profile) { mutableStateOf(profile.financialPersona) }
    var isBudgetAlertActive by remember(profile) { mutableStateOf(profile.isBudgetAlertActive) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    val monthlyIncome = monthlyIncomeText.toLongOrNull() ?: profile.monthlyIncome
    val dailyLimit = dailyLimitText.toLongOrNull() ?: profile.dailyBudgetLimit

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ViNoteSurfaceContainerLow)
                            .testTag("profile_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ViNoteTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Profile & Budget Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                }
            }

            // User Identity Hero Card
            item {
                ViNoteCard(padding = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ViNoteSecondaryFixed)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(ViNotePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.avatarInitials,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.fullName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = profile.email,
                                fontSize = 13.sp,
                                color = ViNoteTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ViNoteSecondaryFixed)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = financialPersona,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ViNotePrimary
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 1: PERSONAL INFORMATION
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PERSONAL INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name", color = ViNoteTextSecondary) },
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = ViNotePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                                    .testTag("profile_name_field")
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address", color = ViNoteTextSecondary) },
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = ViNotePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                                    .testTag("profile_email_field")
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number", color = ViNoteTextSecondary) },
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = ViNotePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                                    .testTag("profile_phone_field")
                            )
                        }
                    }
                }
            }

            // SECTION 2: BUDGET & FINANCIAL RULES
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BUDGET & FINANCIAL RULES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = monthlyIncomeText,
                                onValueChange = { monthlyIncomeText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Monthly Income", color = ViNoteTextSecondary) },
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ViNotePrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
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
                                    .testTag("profile_monthly_income_field")
                            )

                            OutlinedTextField(
                                value = dailyLimitText,
                                onValueChange = { dailyLimitText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Daily Spending Limit", color = ViNoteTextSecondary) },
                                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = ViNoteTextPrimary),
                                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = ViNotePrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
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
                                    .testTag("profile_daily_limit_field")
                            )

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target Savings Rate",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = "${savingsPercentage.toInt()}% (${FormatUtils.formatRupiah((monthlyIncome * (savingsPercentage / 100f)).toLong())})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
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
                            }
                        }
                    }
                }
            }

            // SECTION 3: AUTOMATIC BUDGET EXCEEDED ALERT
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BUDGET OVERAGE ALERTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Automatic Furious Alert",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "💢")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nota becomes furious and alerts you if today's spending exceeds ${FormatUtils.formatRupiah(dailyLimit)}",
                                    fontSize = 12.sp,
                                    color = ViNoteTextSecondary
                                )
                            }

                            Switch(
                                checked = isBudgetAlertActive,
                                onCheckedChange = { isBudgetAlertActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ViNotePrimary
                                ),
                                modifier = Modifier.testTag("profile_furious_alert_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Overspending Button
                        ViNoteButton(
                            text = "Test Furious Alert (Trigger Warning) 🚨",
                            type = ViNoteButtonType.SECONDARY,
                            onClick = {
                                viewModel.simulateBudgetExceededAlert()
                            },
                            testTag = "profile_test_furious_alert_btn"
                        )
                    }
                }
            }

            // SECTION 4: CURRENCY PREFERENCE
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CURRENCY & LOCALE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 14.dp) {
                        val currencies = listOf("IDR (Rp)", "USD ($)", "EUR (€)")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currencies.forEach { curr ->
                                val isSelected = currencyCode == curr.substringBefore(" ")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ViNoteSecondaryFixed else ViNoteSurfaceContainerLowest)
                                        .clickable { currencyCode = curr.substringBefore(" ") }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = curr,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ViNotePrimary else ViNoteTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: AI & OPENROUTER CONFIGURATION
            item {
                val aiConfig by viewModel.aiConfig.collectAsState()
                var apiKeyInput by remember(aiConfig.apiKey) { mutableStateOf(aiConfig.apiKey) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AI ASSISTANT ENGINE (OPENROUTER)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextSecondary,
                        letterSpacing = 0.05.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    ViNoteCard(padding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cloud AI Reasoning (LLM)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextPrimary
                                    )
                                    Text(
                                        text = if (aiConfig.isOnlineAiEnabled) "Online OpenRouter intelligence active" else "Fast offline NLP fallback active",
                                        fontSize = 12.sp,
                                        color = ViNoteTextSecondary
                                    )
                                }
                                Switch(
                                    checked = aiConfig.isOnlineAiEnabled,
                                    onCheckedChange = { viewModel.toggleOnlineAi(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ViNotePrimary
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    viewModel.setOpenRouterApiKey(it)
                                },
                                label = { Text("OpenRouter API Key (sk-or-v1-...)") },
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
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Model Selection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ViNoteTextSecondary
                            )

                            val models = listOf(
                                "google/gemini-2.5-flash",
                                "meta-llama/llama-3.3-70b-instruct:free",
                                "deepseek/deepseek-chat"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                models.forEach { modelName ->
                                    val isSelected = aiConfig.selectedModel == modelName
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ViNoteSecondaryFixed else ViNoteSurfaceContainerLowest)
                                            .clickable { viewModel.setOpenRouterModel(modelName) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = modelName,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ViNotePrimary else ViNoteTextPrimary
                                            )
                                            if (isSelected) {
                                                Text("✓", color = ViNotePrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Action Button
            item {
                ViNoteButton(
                    text = "Save Profile & Budget",
                    onClick = {
                        viewModel.updateProfile(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            monthlyIncome = monthlyIncome,
                            dailyBudgetLimit = dailyLimit,
                            savingsPercentage = savingsPercentage.toInt(),
                            currencyCode = currencyCode,
                            currencySymbol = if (currencyCode == "USD") "$" else if (currencyCode == "EUR") "€" else "Rp",
                            persona = financialPersona,
                            isBudgetAlertActive = isBudgetAlertActive
                        )
                        onBack()
                    },
                    testTag = "profile_save_btn"
                )
            }

            // Account Actions (Sign Out)
            item {
                ViNoteButton(
                    text = "Sign Out",
                    type = ViNoteButtonType.GHOST,
                    onClick = { showSignOutDialog = true },
                    testTag = "profile_sign_out_btn"
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = {
                    Text(
                        text = "Sign Out?",
                        fontWeight = FontWeight.Bold,
                        color = ViNoteTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "You will be returned to the login screen. Your local data remains securely saved.",
                        fontSize = 14.sp,
                        color = ViNoteTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.logout()
                            onSignOut()
                        }
                    ) {
                        Text("Sign Out", color = ViNoteError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel", color = ViNoteTextSecondary)
                    }
                }
            )
        }
    }
}
