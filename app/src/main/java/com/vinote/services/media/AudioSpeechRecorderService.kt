package com.vinote.services.media

import android.app.Application
import com.vinote.services.ai.AndroidSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioSpeechRecorderService(private val application: Application) {
    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null
    private val recognizer = AndroidSpeechRecognizer(application)

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        _isRecording.value = true
        job?.cancel()
        job = scope.launch {
            try {
                val text = recognizer.startListening(
                    languageCode = "id-ID",
                    onRmsChanged = { rms ->
                        _audioRmsDb.value = rms
                    }
                )
                if (!text.isNullOrBlank()) {
                    onResult(text)
                } else {
                    onErrorCallback("No speech detected")
                }
            } catch (e: Exception) {
                onErrorCallback(e.message ?: "Speech recognition error")
            } finally {
                _isRecording.value = false
                _audioRmsDb.value = 0f
            }
        }
    }

    fun stopListening() {
        job?.cancel()
        recognizer.destroy()
        _isRecording.value = false
    }

    fun destroy() {
        scope.cancel()
        recognizer.destroy()
        _isRecording.value = false
    }
}