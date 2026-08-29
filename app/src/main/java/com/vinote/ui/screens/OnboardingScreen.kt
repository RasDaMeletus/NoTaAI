package com.vinote.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vinote.data.model.NotaBaseColor
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.components.ViNoteButtonType
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurface
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import com.vinote.viewmodel.AuthState
import com.vinote.viewmodel.AuthViewModel
import com.vinote.domain.auth.GoogleSignInManager

@Composable
fun OnboardingScreen(
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSignInSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViNoteSurface)
    ) {
        // Decorative radial blobs
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ViNoteSecondaryFixed.copy(alpha = 0.5f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ViNoteWarmYellow.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Character Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NotaAvatar(
                    size = 140.dp,
                    eyeState = NotaEyeState.HAPPY,
                    baseColor = NotaBaseColor.SOFT_PINK,
                    showSparkle = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Meet Nota,",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ViNoteTextPrimary,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "Your Financial Companion",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNotePrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Track expenses automatically, reach your dream goals, and keep your budget safe with your lively companion.",
                    fontSize = 15.sp,
                    color = ViNoteTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                ViNoteButton(
                    text = "Sign in with Google",
                    onClick = { launcher.launch(viewModel.getSignInIntent()) },
                    testTag = "onboarding_google_sign_in_btn"
                )
            }
        }
    }
}
