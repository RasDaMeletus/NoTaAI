package com.example.services.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioSpeechRecorderService(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb = _audioRmsDb.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit = {},
        onErrorCallback: (String) -> Unit = {}
    ) {
        mainHandler.post {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("AudioSpeechRecorder", "Speech recognizer ready for speech")
                            _isRecording.value = true
                            _errorMessage.value = null
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d("AudioSpeechRecorder", "User began speaking")
                            _isRecording.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB usually ranges from -2 to 10 dB
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioRmsDb.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d("AudioSpeechRecorder", "User stopped speaking")
                            _isRecording.value = false
                        }

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
                                SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer to mic."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                                else -> "Recognition error: $error"
                            }
                            Log.w("AudioSpeechRecorder", "Recognition error: $msg ($error)")
                            _isRecording.value = false
                            _errorMessage.value = msg
                            onErrorCallback(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            _isRecording.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestResult = matches?.firstOrNull() ?: ""
                            if (bestResult.isNotBlank()) {
                                _recognizedText.value = bestResult
                                onResult(bestResult)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                _recognizedText.value = partial
                                onPartialResult(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara transaksi kamu (contoh: Makan siang 30rb GoPay)")
                }

                speechRecognizer?.startListening(intent)
                _isRecording.value = true
            } catch (e: Exception) {
                Log.e("AudioSpeechRecorder", "Failed to start listening", e)
                _isRecording.value = false
                _errorMessage.value = e.message ?: "Failed to start speech recognizer"
                onErrorCallback(_errorMessage.value ?: "Error")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isRecording.value = false
            } catch (e: Exception) {
                Log.e("AudioSpeechRecorder", "Error stopping listening", e)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _isRecording.value = false
            } catch (e: Exception) {
                Log.e("AudioSpeechRecorder", "Error cancelling speech", e)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                _isRecording.value = false
            } catch (e: Exception) {
                Log.e("AudioSpeechRecorder", "Error destroying recognizer", e)
            }
        }
    }
}
