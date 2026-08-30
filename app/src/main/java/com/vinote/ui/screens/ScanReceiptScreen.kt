package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.engine.OfflineNlpEngine
import com.example.data.model.NotaEyeState
import com.example.ui.components.FormatUtils
import com.example.ui.components.NotaAvatar
import com.example.ui.components.ViNoteButton
import com.example.ui.components.camera.CameraPreviewView
import com.example.ui.components.camera.toBitmap
import com.example.ui.theme.ViNoteMintSuccess
import com.example.ui.theme.ViNotePrimary
import com.example.ui.theme.ViNoteSurfaceContainerLowest
import com.example.ui.theme.ViNoteTextPrimary
import com.example.ui.theme.ViNoteTextSecondary
import com.example.viewmodel.ViNoteViewModel
import java.util.concurrent.Executors

@Composable
fun ScanReceiptScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedPreset by viewModel.selectedReceiptPreset.collectAsState()
    val extractedReceipt by viewModel.extractedReceiptData.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()
    val aiEngineStatus by viewModel.aiEngineStatus.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .testTag("scan_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Live Receipt Camera",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch Camera Button
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("scan_switch_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Torch Button
                    IconButton(
                        onClick = { isTorchEnabled = !isTorchEnabled },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isTorchEnabled) ViNotePrimary else Color.White.copy(alpha = 0.15f))
                            .testTag("scan_torch_btn")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Real Camera / OCR Vision Badge (Hybrid AI)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1B382B))
                    .border(1.dp, ViNoteMintSuccess.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ViNoteMintSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!hasCameraPermission) "⚠️ Camera Permission Required"
                        else if (aiEngineStatus.isOnline) "⚡ Hugging Face OCR Active (${aiEngineStatus.connectionType})"
                        else "🔒 100% On-Device OCR Engine",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ViNoteMintSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Preset Receipts Horizontal Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OfflineNlpEngine.sampleReceipts.keys.forEach { preset ->
                    val isSelected = preset == selectedPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) ViNotePrimary else Color.White.copy(alpha = 0.15f))
                            .clickable { viewModel.selectReceiptPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Viewfinder Box (Real Live Camera Feed)
            Box(
                modifier = Modifier
                    .size(310.dp, 240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1C23))
                    .border(1.5.dp, ViNoteMintSuccess.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    // Live CameraX Preview
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        isTorchEnabled = isTorchEnabled,
                        cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                        onImageCaptureReady = { capture ->
                            imageCaptureInstance = capture
                        },
                        onError = { err ->
                            Log.e("ScanReceiptScreen", "Camera error: $err")
                        }
                    )
                } else {
                    // Permission Prompt Fallback
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera Permission",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera access is needed to scan physical paper receipts",
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ViNotePrimary)
                                .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Grant Permission",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Scanning Laser Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = laserY.dp)
                        .height(2.5.dp)
                        .background(ViNoteMintSuccess)
                        .shadow(6.dp, spotColor = ViNoteMintSuccess)
                )

                // Corner Focus Target Marks
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ALIGN RECEIPT WITHIN FRAME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Extracted Details & Real Shutter Action Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(ViNoteSurfaceContainerLowest)
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Nota Status Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NotaAvatar(
                            size = 38.dp,
                            eyeState = if (isScanning) NotaEyeState.THINKING else NotaEyeState.HAPPY,
                            baseColor = notaConfig.baseColor,
                            isAnimated = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isScanning) "Processing receipt vision..." else "Camera Ready",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "Snap photo to extract items & total",
                                fontSize = 12.sp,
                                color = ViNoteTextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "OCR Ready",
                                tint = ViNoteMintSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Live",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteMintSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Extracted Data Preview Box
                    extractedReceipt?.let { data ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF6F6F9))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = data.merchant,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ViNoteTextPrimary
                                        )
                                        Text(
                                            text = "${data.category} • ${data.date}",
                                            fontSize = 11.sp,
                                            color = ViNoteTextSecondary
                                        )
                                    }

                                    Text(
                                        text = FormatUtils.formatRupiah(data.totalAmount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNotePrimary
                                    )
                                }

                                if (data.items.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "EXTRACTED ITEMS (${data.items.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextSecondary,
                                        letterSpacing = 0.05.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    data.items.take(2).forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${item.name}",
                                                fontSize = 11.sp,
                                                color = ViNoteTextPrimary,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = FormatUtils.formatRupiah(item.price),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ViNoteTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shutter Capture & Confirm Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Real Camera Shutter Snap Button
                        ViNoteButton(
                            text = if (isScanning) "Scanning..." else "Snap & Analyze Photo 📸",
                            onClick = {
                                val capture = imageCaptureInstance
                                if (capture != null && hasCameraPermission) {
                                    capture.takePicture(
                                        cameraExecutor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val bitmap = image.toBitmap()
                                                image.close()
                                                if (bitmap != null) {
                                                    viewModel.processCapturedReceiptBitmap(bitmap, onComplete = onBack)
                                                } else {
                                                    viewModel.startReceiptScanning(selectedPreset, onComplete = onBack)
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("ScanReceiptScreen", "Photo capture failed", exception)
                                                viewModel.startReceiptScanning(selectedPreset, onComplete = onBack)
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.startReceiptScanning(selectedPreset, onComplete = onBack)
                                }
                            },
                            enabled = !isScanning,
                            leadingIcon = {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Capture",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "capture_receipt_btn"
                        )
                    }
                }
            }
        }
    }
}
