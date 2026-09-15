package com.vinote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.ui.components.ViNoteBottomNavigation
import com.vinote.ui.components.ViNoteNavTab
import com.vinote.ui.screens.ActivityScreen
import com.vinote.ui.screens.AddTransactionScreen
import com.vinote.ui.screens.BankIntegrationsScreen
import com.vinote.ui.screens.BudgetExceededDialog
import com.vinote.ui.screens.CreateGoalDialog
import com.vinote.ui.screens.CustomizeNotaScreen
import com.vinote.ui.screens.EWalletsScreen
import com.vinote.ui.screens.GoalsScreen
import com.vinote.ui.screens.HomeScreen
import com.vinote.ui.screens.MeScreen
import com.vinote.ui.screens.NotaAssistantScreen
import com.vinote.ui.screens.OnboardingScreen
import com.vinote.ui.screens.ProfileSettingsScreen
import com.vinote.ui.screens.QuickSetupScreen
import com.vinote.ui.screens.ScanReceiptScreen
import com.vinote.ui.screens.SettingsScreen
import com.vinote.ui.screens.TransactionConfirmDialog
import com.vinote.ui.screens.TransactionDetailSheet
import com.vinote.ui.screens.VoiceInputScreen
import com.vinote.ui.theme.MyApplicationTheme
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.viewmodel.ViNoteViewModel
import com.vinote.domain.security.BiometricSecurityManager
import com.vinote.domain.security.ShakeDetector
import com.vinote.ui.widget.NoTaQuickWidgetProvider
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

enum class ActiveScreen {
    SPLASH,
    MAIN_TABS,
    ADD_TRANSACTION,
    VOICE_INPUT,
    SCAN_RECEIPT,
    E_WALLETS,
    CUSTOMIZE_NOTA,
    SETTINGS,
    ONBOARDING,
    QUICK_SETUP,
    PROFILE_SETTINGS,
    BANK_INTEGRATIONS
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: ViNoteViewModel by viewModels()
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Privacy: Prevent screenshot & task switcher capture when enabled (PRD Section 4.1)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isScreenCapturePrevented.collect { prevented ->
                    if (prevented) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        // Panic Mode: Shake 3x to toggle privacy masking (PRD Section 1.5 & 4.1)
        shakeDetector = ShakeDetector(this) {
            viewModel.togglePrivacyMode()
            Toast.makeText(this, "Mode Privasi dialihkan (Panic Mode)", Toast.LENGTH_SHORT).show()
        }

        // Biometric Security Lock (PRD Section 1.5)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.isBiometricLockEnabled.collect { isEnabled ->
                    if (isEnabled && !viewModel.isAppUnlocked.value) {
                        BiometricSecurityManager.authenticate(
                            activity = this@MainActivity,
                            onSuccess = {
                                viewModel.setAppUnlocked(true)
                            },
                            onError = { reason ->
                                Toast.makeText(this@MainActivity, "Kunci Biometrik: $reason", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        val initialDestination = intent?.getStringExtra(NoTaQuickWidgetProvider.EXTRA_NAVIGATE_TO)

        setContent {
            MyApplicationTheme {
                ViNoteApp(
                    viewModel = viewModel,
                    initialDestination = initialDestination
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.startListening()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stopListening()
    }
}

@Composable
fun ViNoteApp(
    viewModel: ViNoteViewModel,
    initialDestination: String? = null
) {
    val startScreen = if (initialDestination == NoTaQuickWidgetProvider.DESTINATION_ADD_TRANSACTION) {
        ActiveScreen.ADD_TRANSACTION
    } else {
        ActiveScreen.SPLASH
    }
    var currentScreen by remember { mutableStateOf(startScreen) }
    var currentTab by remember { mutableStateOf(ViNoteNavTab.HOME) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }

    val pendingTx by viewModel.pendingTransaction.collectAsState()
    val selectedDetailTx by viewModel.selectedTransactionDetail.collectAsState()
    val bannerText by viewModel.bannerNotification.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Splash → Auth routing
    LaunchedEffect(isLoggedIn) {
        if (currentScreen == ActiveScreen.SPLASH) {
            delay(1800)
            currentScreen = if (isLoggedIn) ActiveScreen.MAIN_TABS else ActiveScreen.ONBOARDING
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            ActiveScreen.SPLASH -> {
                SplashScreen()
            }
            ActiveScreen.ONBOARDING -> {
                OnboardingScreen(
                    onSignInSuccess = { currentScreen = ActiveScreen.QUICK_SETUP }
                )
            }
            ActiveScreen.QUICK_SETUP -> {
                QuickSetupScreen(
                    viewModel = viewModel,
                    onSetupComplete = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.PROFILE_SETTINGS -> {
                ProfileSettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS },
                    onSignOut = { currentScreen = ActiveScreen.ONBOARDING },
                    onNavigateToBankIntegrations = { currentScreen = ActiveScreen.BANK_INTEGRATIONS }
                )
            }
            ActiveScreen.BANK_INTEGRATIONS -> {
                BankIntegrationsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.ADD_TRANSACTION -> {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS },
                    onNavigateToScan = { currentScreen = ActiveScreen.SCAN_RECEIPT },
                    onNavigateToVoice = { currentScreen = ActiveScreen.VOICE_INPUT }
                )
            }
            ActiveScreen.VOICE_INPUT -> {
                VoiceInputScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.SCAN_RECEIPT -> {
                ScanReceiptScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.E_WALLETS -> {
                EWalletsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.CUSTOMIZE_NOTA -> {
                CustomizeNotaScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS }
                )
            }
            ActiveScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = ActiveScreen.MAIN_TABS },
                    onSignOut = { currentScreen = ActiveScreen.ONBOARDING },
                    onNavigateToProfileSettings = { currentScreen = ActiveScreen.PROFILE_SETTINGS },
                    onNavigateToBankIntegrations = { currentScreen = ActiveScreen.BANK_INTEGRATIONS }
                )
            }
            ActiveScreen.MAIN_TABS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Active Tab Content
                    when (currentTab) {
                        ViNoteNavTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { currentScreen = ActiveScreen.ADD_TRANSACTION },
                                onNavigateToScan = { currentScreen = ActiveScreen.SCAN_RECEIPT },
                                onNavigateToVoice = { currentScreen = ActiveScreen.VOICE_INPUT },
                                onNavigateToActivity = { currentTab = ViNoteNavTab.ACTIVITY },
                                onNavigateToSettings = { currentScreen = ActiveScreen.SETTINGS }
                            )
                        }
                        ViNoteNavTab.ACTIVITY -> {
                            ActivityScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { }
                            )
                        }
                        ViNoteNavTab.NOTA -> {
                            NotaAssistantScreen(
                                viewModel = viewModel,
                                onNavigateToVoice = { currentScreen = ActiveScreen.VOICE_INPUT },
                                onNavigateToCustomizeNota = { currentScreen = ActiveScreen.CUSTOMIZE_NOTA }
                            )
                        }
                        ViNoteNavTab.GOALS -> {
                            GoalsScreen(
                                viewModel = viewModel,
                                onNavigateToCreateGoal = { showCreateGoalDialog = true },
                                onNavigateToSettings = { currentScreen = ActiveScreen.SETTINGS }
                            )
                        }
                        ViNoteNavTab.ME -> {
                            MeScreen(
                                viewModel = viewModel,
                                onNavigateToCustomizeNota = { currentScreen = ActiveScreen.CUSTOMIZE_NOTA },
                                onNavigateToEWallets = { currentScreen = ActiveScreen.E_WALLETS },
                                onNavigateToBankIntegrations = { currentScreen = ActiveScreen.BANK_INTEGRATIONS },
                                onNavigateToProfileSettings = { currentScreen = ActiveScreen.PROFILE_SETTINGS },
                                onNavigateToSettings = { currentScreen = ActiveScreen.SETTINGS }
                            )
                        }
                    }

                    // Floating Bottom Navigation Bar
                    ViNoteBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // Budget Exceeded Furious NoTa Dialog Alert
        BudgetExceededDialog(viewModel = viewModel)

        // Pending Transaction Confirmation Dialog
        pendingTx?.let { tx ->
            TransactionConfirmDialog(
                transaction = tx,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissPendingTransaction() }
            )
        }

        // Transaction Detail Sheet (View & Delete)
        selectedDetailTx?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                viewModel = viewModel,
                onDismiss = { viewModel.selectTransactionDetail(null) }
            )
        }

        // Create Goal Sheet
        if (showCreateGoalDialog) {
            CreateGoalDialog(
                viewModel = viewModel,
                onDismiss = { showCreateGoalDialog = false }
            )
        }

        // In-App Toast/Banner Notification
        AnimatedVisibility(
            visible = bannerText != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            bannerText?.let { text ->
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color(0x1F171827)
                        )
                        .clip(RoundedCornerShape(50))
                        .background(ViNotePrimary)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ViNotePrimary.copy(alpha = 0.95f),
                        ViNotePrimary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ViNotePrimary
                )
            }
            Text(
                text = "NoTa",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Text(
                text = "Your Financial Companion",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
