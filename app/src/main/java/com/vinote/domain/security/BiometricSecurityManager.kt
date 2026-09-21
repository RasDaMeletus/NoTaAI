package com.vinote.domain.security

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

object BiometricSecurityManager {

    fun isBiometricAvailable(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager?.isDeviceSecure == true
        } else {
            false
        }
    }

    fun authenticate(
        activity: ComponentActivity,
        title: String = "Buka Kunci NoTa",
        subtitle: String = "Gunakan sidik jari atau kunci layar untuk mengakses data keuangan",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancellationSignal = CancellationSignal()

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "Autentikasi dibatalkan")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autentikasi sidik jari tidak cocok")
                }
            }

            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton("Batal", executor, DialogInterface.OnClickListener { _, _ ->
                    cancellationSignal.cancel()
                    onError("Dibatalkan oleh pengguna")
                })
                .build()

            prompt.authenticate(cancellationSignal, executor, callback)
        } else {
            // Fallback for older Android versions
            onSuccess()
        }
    }
}
