package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AudioRoutingMode
import com.example.model.ConversationChannel
import com.example.model.TranslateLanguage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "DualChannelAudio"

data class AudioHardwareState(
    val hasExternalHeadset: Boolean = false,
    val headsetDeviceName: String = "کپسول گوشی (بدون هدفون خارجی)",
    val speakerDeviceName: String = "اسپیکر اصلی گوشی",
    val activeRouteLabel: String = "آماده تفکیک صوتی",
    val isSpeakingNow: Boolean = false,
    val speakingChannel: ConversationChannel? = null,
    val ttsInitialized: Boolean = false
)

/**
 * Manages physical dual-channel audio routing for simultaneous interpretation:
 * 1. When Host (Persian user) speaks -> Translation plays out loud on the Phone Speaker for the Partner.
 * 2. When Partner (English/Russian/Turkish/Spanish/Arabic) speaks -> Translation plays in the Host's Earbud (or Earpiece).
 * Also handles real-time 16kHz PCM audio recording with Voice Activity Detection (VAD) and WAV encoding for Gemini Live.
 */
class DualChannelAudioRouter(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _hardwareState = MutableStateFlow(AudioHardwareState())
    val hardwareState: StateFlow<AudioHardwareState> = _hardwareState.asStateFlow()

    private val _waveformBars = MutableStateFlow(List(18) { 0.08f })
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0f)
    val recordingSeconds: StateFlow<Float> = _recordingSeconds.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRate: Float = 1.0f

    private var recordingJob: Job? = null
    private var currentPcmBuffer = ByteArrayOutputStream()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioDevices()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        refreshAudioDevices()
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _hardwareState.value = _hardwareState.value.copy(isSpeakingNow = true)
                        }

                        override fun onDone(utteranceId: String?) {
                            _hardwareState.value = _hardwareState.value.copy(
                                isSpeakingNow = false,
                                speakingChannel = null
                            )
                            restoreDefaultAudioRoute()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _hardwareState.value = _hardwareState.value.copy(
                                isSpeakingNow = false,
                                speakingChannel = null
                            )
                            restoreDefaultAudioRoute()
                        }
                    })
                    _hardwareState.value = _hardwareState.value.copy(ttsInitialized = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    fun setSpeechSpeed(rate: Float) {
        speechRate = rate.coerceIn(0.65f, 1.45f)
        tts?.setSpeechRate(speechRate)
    }

    fun refreshAudioDevices() {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val headsetDevice = outputs.firstOrNull { it.isHeadsetOrEarbud() }
            val speakerDevice = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }

            val headsetLabel = if (headsetDevice != null) {
                val productName = headsetDevice.productName?.toString()?.takeIf { it.isNotBlank() }
                val typeLabel = when (headsetDevice.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> "هدفون بلوتوث"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "هدفون سیمی/USB"
                    else -> "هدفون متصل"
                }
                if (productName != null && !productName.equals(Build.MODEL, ignoreCase = true)) {
                    "$typeLabel ($productName)"
                } else {
                    typeLabel
                }
            } else {
                "کپسول بالای گوشی (هدفون خارجی متصل نیست)"
            }

            val speakerLabel = if (speakerDevice != null) {
                "اسپیکر بلندگوی گوشی (فعال برای مخاطب)"
            } else {
                "خروجی صوتی اصلی دستگاه"
            }

            _hardwareState.value = _hardwareState.value.copy(
                hasExternalHeadset = headsetDevice != null,
                headsetDeviceName = headsetLabel,
                speakerDeviceName = speakerLabel
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning audio devices", e)
        }
    }

    /**
     * Routes audio output based on who needs to hear the translation:
     * - HOST_EARBUD_TO_SPEAKER -> Translated for Partner -> Forced to Built-in Phone Speaker!
     * - PARTNER_PHONE_TO_EARBUD -> Translated for User (Persian) -> Forced to User's Earbud or Phone Earpiece!
     */
    fun speakTranslation(
        text: String,
        language: TranslateLanguage,
        channel: ConversationChannel,
        routingMode: AudioRoutingMode,
        phoneticFallback: String = ""
    ): String {
        val routeToSpeaker = when (routingMode) {
            AudioRoutingMode.SPLIT_EARBUD_SPEAKER ->
                channel == ConversationChannel.HOST_EARBUD_TO_SPEAKER
            AudioRoutingMode.SPEAKER_ONLY -> true
            AudioRoutingMode.EARBUD_ONLY -> false
        }

        val routeDescription = if (routeToSpeaker) {
            "پخش از اسپیکر گوشی برای مخاطب (${language.persianName})"
        } else {
            val targetName = if (_hardwareState.value.hasExternalHeadset) {
                "پخش در هدفون شما (${language.persianName})"
            } else {
                "پخش در کپسول گوشی کنار گوش شما (${language.persianName})"
            }
            targetName
        }

        _hardwareState.value = _hardwareState.value.copy(
            activeRouteLabel = routeDescription,
            isSpeakingNow = true,
            speakingChannel = channel
        )

        scope.launch(Dispatchers.Main) {
            try {
                configureOutputRoute(routeToSpeaker = routeToSpeaker)
                // Play a subtle directional acoustic cue so the user/partner immediately perceives which channel is active
                playDirectionalAcousticCue(routeToSpeaker = routeToSpeaker)

                val engine = tts
                if (engine != null && isTtsReady) {
                    engine.setSpeechRate(speechRate)
                    val langResult = engine.setLanguage(language.locale)
                    val audioAttrs = if (routeToSpeaker) {
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    } else {
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    }
                    engine.setAudioAttributes(audioAttrs)

                    val params = Bundle().apply {
                        putInt(
                            TextToSpeech.Engine.KEY_PARAM_STREAM,
                            if (routeToSpeaker) AudioManager.STREAM_MUSIC else AudioManager.STREAM_VOICE_CALL
                        )
                    }

                    val textToSpeak = if (
                        langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        // Fallback if emulator lacks specific locale voice data (e.g., fa-IR)
                        engine.setLanguage(TranslateLanguage.ENGLISH.locale)
                        phoneticFallback.ifBlank { text }
                    } else {
                        text
                    }

                    val speakCode = engine.speak(
                        textToSpeak,
                        TextToSpeech.QUEUE_FLUSH,
                        params,
                        "utt_${UUID.randomUUID()}"
                    )
                    if (speakCode != TextToSpeech.SUCCESS) {
                        delay(600)
                        _hardwareState.value = _hardwareState.value.copy(
                            isSpeakingNow = false,
                            speakingChannel = null
                        )
                    }
                } else {
                    delay(800)
                    _hardwareState.value = _hardwareState.value.copy(
                        isSpeakingNow = false,
                        speakingChannel = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error routing TTS speech", e)
                _hardwareState.value = _hardwareState.value.copy(
                    isSpeakingNow = false,
                    speakingChannel = null
                )
            }
        }

        return routeDescription
    }

    private fun configureOutputRoute(routeToSpeaker: Boolean) {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            if (routeToSpeaker) {
                audioManager.mode = AudioManager.MODE_NORMAL
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speaker = outputs.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        audioManager.setCommunicationDevice(speaker)
                    }
                }
            } else {
                // Route to User's Earbud / Headset if connected, otherwise Phone Earpiece
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val targetDevice = outputs.firstOrNull { it.isHeadsetOrEarbud() }
                        ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    if (targetDevice != null) {
                        audioManager.setCommunicationDevice(targetDevice)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not switch communication device explicitly", e)
        }
    }

    private fun restoreDefaultAudioRoute() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Error restoring default audio route", e)
        }
    }

    /**
     * Synthesizes a short, pleasant stereo/directional chime on the target AudioTrack stream
     * so even before TTS finishes or on emulators without TTS voices, the user hears the physical channel switch.
     */
    private suspend fun playDirectionalAcousticCue(routeToSpeaker: Boolean) {
        kotlinx.coroutines.withContext(Dispatchers.Default) {
            try {
                val sampleRate = 16000
                val durationMs = 180
                val numSamples = (sampleRate * durationMs) / 1000
                val pcm = ShortArray(numSamples)
                // Higher warm chime (880Hz -> 1046Hz) for Speaker/Partner; softer cyan chime (523Hz -> 659Hz) for Earbud/Host
                val freqStart = if (routeToSpeaker) 784.0 else 523.25
                val freqEnd = if (routeToSpeaker) 1046.5 else 659.25

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = if (progress < 0.5) freqStart else freqEnd
                    val envelope = sin(progress * Math.PI)
                    val sample = (sin(2.0 * Math.PI * i * freq / sampleRate) * 0.25 * envelope * Short.MAX_VALUE).toInt()
                    pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val attrs = AudioAttributes.Builder()
                    .setUsage(
                        if (routeToSpeaker) AudioAttributes.USAGE_MEDIA
                        else AudioAttributes.USAGE_VOICE_COMMUNICATION
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                delay(durationMs.toLong() + 30L)
                track.stop()
                track.release()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Acoustic cue skipped", e)
            }
        }
    }

    /**
     * Starts live 16kHz 16-bit mono PCM microphone capture for Gemini Live multimodal audio input.
     * Supports automatic Voice Activity Detection (VAD) silence cutoff when autoStopOnSilence = true.
     */
    @SuppressLint("MissingPermission")
    fun startLiveRecording(
        preferredChannel: ConversationChannel,
        autoStopOnSilence: Boolean,
        onAutoSilenceDetected: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        stopRecordingInternal()
        currentPcmBuffer.reset()
        _recordingSeconds.value = 0f

        recordingJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(3200)

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 2
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    onError("میکروفون دستگاه آماده نشد. لطفاً دسترسی میکروفون را بررسی کنید.")
                    return@launch
                }

                // Route input microphone based on active channel when headset is present
                selectPreferredInputMicrophone(audioRecord, preferredChannel)

                audioRecord.startRecording()
                val readBuffer = ShortArray(1024)
                val byteChunk = ByteArray(readBuffer.size * 2)

                var hasDetectedSpeech = false
                var silentFramesCount = 0
                var totalFramesCount = 0
                val maxRecordingSeconds = 25

                while (isActive) {
                    val readCount = audioRecord.read(readBuffer, 0, readBuffer.size)
                    if (readCount > 0) {
                        totalFramesCount++
                        val elapsedSec = (totalFramesCount * readCount).toFloat() / sampleRate
                        _recordingSeconds.value = elapsedSec

                        // Convert shorts to little-endian PCM bytes and compute RMS amplitude
                        var sumSquares = 0.0
                        ByteBuffer.wrap(byteChunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            .put(readBuffer, 0, readCount)
                        currentPcmBuffer.write(byteChunk, 0, readCount * 2)

                        for (i in 0 until readCount) {
                            val normalized = readBuffer[i] / 32768.0
                            sumSquares += normalized * normalized
                        }
                        val rms = sqrt(sumSquares / readCount).toFloat()
                        updateWaveformFromChunk(readBuffer, readCount, rms)

                        // Voice Activity Detection (VAD) for hands-free Gemini Live turn-taking
                        if (rms > 0.035f) {
                            hasDetectedSpeech = true
                            silentFramesCount = 0
                        } else if (hasDetectedSpeech) {
                            silentFramesCount++
                        }

                        // ~1.4 seconds of silence after speech triggers automatic turn completion in Live mode
                        if (autoStopOnSilence && hasDetectedSpeech && silentFramesCount >= 22 && elapsedSec >= 1.2f) {
                            val wavBytes = encodePcmToWav(currentPcmBuffer.toByteArray(), sampleRate)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                onAutoSilenceDetected(wavBytes)
                            }
                            break
                        }

                        if (elapsedSec >= maxRecordingSeconds) {
                            val wavBytes = encodePcmToWav(currentPcmBuffer.toByteArray(), sampleRate)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                onAutoSilenceDetected(wavBytes)
                            }
                            break
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord failure", e)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError("خطا در ضبط صدای میکروفون: ${e.localizedMessage ?: "نامشخص"}")
                }
            } finally {
                try {
                    audioRecord?.stop()
                } catch (_: Exception) {
                }
                try {
                    audioRecord?.release()
                } catch (_: Exception) {
                }
                _waveformBars.value = List(18) { 0.08f }
            }
        }
    }

    private fun selectPreferredInputMicrophone(
        audioRecord: AudioRecord,
        preferredChannel: ConversationChannel
    ) {
        try {
            val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val targetInput = when (preferredChannel) {
                ConversationChannel.HOST_EARBUD_TO_SPEAKER -> {
                    // Prefer Headset/Earbud mic for Host if available
                    inputs.firstOrNull { it.isHeadsetOrEarbud() }
                        ?: inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                }
                ConversationChannel.PARTNER_PHONE_TO_EARBUD -> {
                    // Prefer Built-in Phone mic for Partner holding the phone
                    inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                }
                ConversationChannel.AUTO_DETECT_LIVE -> null
            }
            if (targetInput != null) {
                audioRecord.setPreferredDevice(targetInput)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set preferred input mic", e)
        }
    }

    private fun updateWaveformFromChunk(buffer: ShortArray, count: Int, rms: Float) {
        val barsCount = 18
        val step = (count / barsCount).coerceAtLeast(1)
        val bars = MutableList(barsCount) { idx ->
            var localPeak = 0f
            val start = idx * step
            val end = min(count, start + step)
            for (j in start until end) {
                val amp = abs(buffer[j].toInt()) / 32768f
                if (amp > localPeak) localPeak = amp
            }
            val boosted = (localPeak * 3.8f + rms * 1.5f).coerceIn(0.08f, 1.0f)
            boosted
        }
        _waveformBars.value = bars
    }

    /**
     * Stops recording and returns a valid 16kHz 16-bit Mono WAV file byte array ready for Gemini multimodal inference.
     */
    fun stopLiveRecordingAndGetWav(): ByteArray {
        stopRecordingInternal()
        val rawPcm = currentPcmBuffer.toByteArray()
        currentPcmBuffer.reset()
        return if (rawPcm.isNotEmpty()) {
            encodePcmToWav(rawPcm, 16000)
        } else {
            ByteArray(0)
        }
    }

    private fun stopRecordingInternal() {
        recordingJob?.cancel()
        recordingJob = null
        _waveformBars.value = List(18) { 0.08f }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            _hardwareState.value = _hardwareState.value.copy(
                isSpeakingNow = false,
                speakingChannel = null
            )
            restoreDefaultAudioRoute()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        }
    }

    fun release() {
        stopRecordingInternal()
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        restoreDefaultAudioRoute()
    }

    private fun encodePcmToWav(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmData.size + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)

        val out = ByteArrayOutputStream(44 + pcmData.size)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }

    private fun AudioDeviceInfo.isHeadsetOrEarbud(): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET -> true
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                } else {
                    false
                }
            }
        }
    }
}
