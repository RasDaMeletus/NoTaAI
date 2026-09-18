package com.vinote.ui.components.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.ImageCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isTorchEnabled: Boolean = false,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onImageCaptureReady: (ImageCapture) -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // One ImageCapture use case for the lifetime of this composable. It is the
    // same instance handed to onImageCaptureReady and the one bound to the camera,
    // so takePicture() always targets a live use case.
    val imageCapture = remember { ImageCapture.Builder().build() }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Publish the capture use case as soon as it exists.
    LaunchedEffect(imageCapture) {
        onImageCaptureReady(imageCapture)
    }

    // (Re)bind only when the view or the camera selector / torch state actually
    // changes. Keying on these avoids unbind/rebind churn on every recomposition
    // (the scan-laser animation recomposes this subtree continuously).
    LaunchedEffect(previewView, cameraSelector, isTorchEnabled) {
        val pv = previewView ?: return@LaunchedEffect
        try {
            val cameraProvider = withContext(Dispatchers.IO) { cameraProviderFuture.get() }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            camera.cameraControl.enableTorch(isTorchEnabled)
        } catch (exc: Exception) {
            onError(exc)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }.also { pv -> previewView = pv }
        }
    )
}

fun ImageProxy.toBitmap(): Bitmap? {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return bitmap?.let {
        val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        if (rotated != it) {
            it.recycle()
        }
        rotated
    }
}
