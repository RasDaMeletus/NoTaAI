package com.vinote.ui.screens

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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.vinote.data.engine.OfflineNlpEngine
import com.vinote.data.model.NotaEyeState
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.components.NotaAvatar
import com.vinote.ui.components.ViNoteButton
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSecondaryFixed
import com.vinote.ui.theme.ViNoteSurfaceContainerLowest
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.viewmodel.ViNoteViewModel

@Composable
fun ScanReceiptScreen(
    viewModel: ViNoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedPreset by viewModel.selectedReceiptPreset.collectAsState()
    val extractedReceipt by viewModel.extractedReceiptData.collectAsState()
    val notaConfig by viewModel.notaConfig.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111216))
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
                    text = "Offline Receipt OCR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            }

            // Offline OCR Badge
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
                        text = "⚡ Local OCR Vision + Heuristic NLP Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ViNoteMintSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset Selector for instantaneous testing
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

            Spacer(modifier = Modifier.height(12.dp))

            // Viewfinder Camera Reticle with simulated receipt lines
            Box(
                modifier = Modifier
                    .size(300.dp, 240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1C23).copy(alpha = 0.85f))
                    .border(1.5.dp, ViNoteMintSuccess.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                // Scanning Laser
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = laserY.dp)
                        .height(2.5.dp)
                        .background(ViNoteMintSuccess)
                        .shadow(6.dp, spotColor = ViNoteMintSuccess)
                )

                // Simulated OCR detected bounding box markers inside frame
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sampleLines = OfflineNlpEngine.sampleReceipts[selectedPreset] ?: emptyList()
                    sampleLines.take(6).forEachIndexed { index, line ->
                        val isHighlight = line.contains("TOTAL", true) || index == 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isHighlight) ViNoteMintSuccess.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    0.8.dp,
                                    if (isHighlight) ViNoteMintSuccess else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = line,
                                color = if (isHighlight) ViNoteMintSuccess else Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Extracted Details & Action Card
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
                                text = if (isScanning) "Reading receipt on-device..." else "Receipt Analyzed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteTextPrimary
                            )
                            Text(
                                text = "Zero cloud upload • 100% Local OCR",
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
                                text = "98%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ViNoteMintSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "EXTRACTED ITEMS (${data.items.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ViNoteTextSecondary,
                                        letterSpacing = 0.05.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Capture / Confirm Button
                    ViNoteButton(
                        text = if (isScanning) "Analyzing with Local OCR..." else "Confirm Extracted Receipt",
                        onClick = {
                            viewModel.startReceiptScanning(selectedPreset, onComplete = onBack)
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
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Capture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        testTag = "capture_receipt_btn"
                    )
                }
            }
        }
    }
}
