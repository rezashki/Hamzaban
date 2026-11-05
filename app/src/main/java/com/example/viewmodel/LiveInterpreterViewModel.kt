package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioHardwareState
import com.example.audio.DualChannelAudioRouter
import com.example.model.AudioRoutingMode
import com.example.model.ConversationChannel
import com.example.model.ConversationTurn
import com.example.model.QuickPhrase
import com.example.model.TranslateLanguage
import com.example.model.TranslationTone
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "LiveInterpreterVM"
private const val PREFS_NAME = "hamzaban_interpreter_prefs"
private const val KEY_PARTNER_LANG = "partner_lang"
private const val KEY_ROUTING_MODE = "routing_mode"
private const val KEY_TONE = "translation_tone"
private const val KEY_AUTO_VAD = "auto_vad_enabled"
private const val KEY_FLIP_PARTNER = "flip_partner_card"
private const val KEY_SPEECH_RATE = "speech_rate"
private const val KEY_SAVED_HISTORY = "saved_history_json"

sealed interface LiveTranslationUiState {
    data object Idle : LiveTranslationUiState
    data class Listening(
        val activeChannel: ConversationChannel,
        val hintText: String
    ) : LiveTranslationUiState
    data class ProcessingAudio(
        val activeChannel: ConversationChannel
    ) : LiveTranslationUiState
    data class StreamingPreview(
        val activeChannel: ConversationChannel,
        val partialTranslation: String
    ) : LiveTranslationUiState
    data class Success(
        val latestTurn: ConversationTurn
    ) : LiveTranslationUiState
    data class Error(
        val message: String
    ) : LiveTranslationUiState
}

class LiveInterpreterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioRouter = DualChannelAudioRouter(application, viewModelScope)

    val hardwareState: StateFlow<AudioHardwareState> = audioRouter.hardwareState
    val waveformBars: StateFlow<List<Float>> = audioRouter.waveformBars
    val recordingSeconds: StateFlow<Float> = audioRouter.recordingSeconds

    private val _uiState = MutableStateFlow<LiveTranslationUiState>(LiveTranslationUiState.Idle)
    val uiState: StateFlow<LiveTranslationUiState> = _uiState.asStateFlow()

    // Host language is Persian (فارسی); Partner language is selectable among English, Russian, Turkish, Spanish, Arabic
    private val _partnerLanguage = MutableStateFlow(loadSavedPartnerLanguage())
    val partnerLanguage: StateFlow<TranslateLanguage> = _partnerLanguage.asStateFlow()

    private val _routingMode = MutableStateFlow(loadSavedRoutingMode())
    val routingMode: StateFlow<AudioRoutingMode> = _routingMode.asStateFlow()

    private val _selectedTone = MutableStateFlow(loadSavedTone())
    val selectedTone: StateFlow<TranslationTone> = _selectedTone.asStateFlow()

    private val _autoVadEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_VAD, true))
    val autoVadEnabled: StateFlow<Boolean> = _autoVadEnabled.asStateFlow()

    private val _continuousLiveSession = MutableStateFlow(false)
    val continuousLiveSession: StateFlow<Boolean> = _continuousLiveSession.asStateFlow()

    private val _flipPartnerCard180 = MutableStateFlow(prefs.getBoolean(KEY_FLIP_PARTNER, false))
    val flipPartnerCard180: StateFlow<Boolean> = _flipPartnerCard180.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _conversationTurns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationTurns: StateFlow<List<ConversationTurn>> = _conversationTurns.asStateFlow()

    private var activeRecordingChannel: ConversationChannel? = null

    // Structured JSON schema for simultaneous interpretation
    private val translationJsonSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "detectedSourceLanguageCode" to Schema.enumeration(
                    values = listOf("fa", "en", "ru", "tr", "es", "ar"),
                    description = "ISO code of the detected input language: fa, en, ru, tr, es, or ar"
                ),
                "targetLanguageCode" to Schema.enumeration(
                    values = listOf("fa", "en", "ru", "tr", "es", "ar"),
                    description = "ISO code of the target language: fa, en, ru, tr, es, or ar"
                ),
                "originalTranscript" to Schema.string(
                    description = "Exact transcription of what the speaker said in the source language"
                ),
                "translatedText" to Schema.string(
                    description = "Natural simultaneous translation in the target language"
                ),
                "phoneticGuide" to Schema.string(
                    description = "Pronunciation guide of the translatedText written in easy-to-read Persian script (and Latin phonetics in parentheses)"
                ),
                "culturalOrContextNote" to Schema.string(
                    description = "Optional brief tip in Persian about tone, idiom, or politeness (empty string if none)"
                ),
                "isSilenceOrNoiseOnly" to Schema.boolean(
                    description = "True ONLY if the audio contains no discernible human speech at all"
                )
            ),
            optionalProperties = listOf("culturalOrContextNote")
        )
    }

    // Lazy initialization of structured Gemini 3.8 Flash model per firebase-ai-logic-android skill
    private val structuredTranslationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationJsonSchema
            }
        )
    }

    // Lazy initialization of streaming Gemini 3.8 Flash model for ultra-low TTFT live preview
    private val streamingPreviewModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    init {
        audioRouter.setSpeechSpeed(_speechRate.value)
        loadConversationHistory()
    }

    fun selectPartnerLanguage(language: TranslateLanguage) {
        if (language == TranslateLanguage.PERSIAN) return
        _partnerLanguage.value = language
        prefs.edit().putString(KEY_PARTNER_LANG, language.code).apply()
    }

    fun selectRoutingMode(mode: AudioRoutingMode) {
        _routingMode.value = mode
        prefs.edit().putString(KEY_ROUTING_MODE, mode.name).apply()
    }

    fun selectTone(tone: TranslationTone) {
        _selectedTone.value = tone
        prefs.edit().putString(KEY_TONE, tone.name).apply()
    }

    fun setAutoVadEnabled(enabled: Boolean) {
        _autoVadEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_VAD, enabled).apply()
    }

    fun toggleFlipPartnerCard180() {
        val next = !_flipPartnerCard180.value
        _flipPartnerCard180.value = next
        prefs.edit().putBoolean(KEY_FLIP_PARTNER, next).apply()
    }

    fun updateSpeechRate(rate: Float) {
        val coerced = rate.coerceIn(0.7f, 1.4f)
        _speechRate.value = coerced
        audioRouter.setSpeechSpeed(coerced)
        prefs.edit().putFloat(KEY_SPEECH_RATE, coerced).apply()
    }

    fun refreshAudioHardware() {
        audioRouter.refreshAudioDevices()
    }

    /**
     * Starts live microphone capture for one of the 3 channels:
     * 1. HOST_EARBUD_TO_SPEAKER (User speaks Persian -> Plays foreign translation on Phone Speaker)
     * 2. PARTNER_PHONE_TO_EARBUD (Partner speaks English/Russian/Turkish/Spanish/Arabic -> Plays Persian in User's Earbud)
     * 3. AUTO_DETECT_LIVE (Gemini Live detects language automatically and routes to the opposite channel)
     */
    fun startChannelListening(channel: ConversationChannel, isContinuousLoop: Boolean = false) {
        audioRouter.stopSpeaking()
        activeRecordingChannel = channel
        _continuousLiveSession.value = isContinuousLoop

        val partner = _partnerLanguage.value
        val hint = when (channel) {
            ConversationChannel.HOST_EARBUD_TO_SPEAKER ->
                "در حال شنیدن صدای شما (فارسی) از هدفون/میکروفون ⬅ ترجمه به ${partner.persianName} برای پخش از اسپیکر گوشی…"
            ConversationChannel.PARTNER_PHONE_TO_EARBUD ->
                "در حال شنیدن صدای مخاطب (${partner.persianName}) از میکروفون گوشی ⬅ ترجمه به فارسی در هدفون شما…"
            ConversationChannel.AUTO_DETECT_LIVE ->
                "جمینای لایو فعال است: به فارسی یا ${partner.persianName} (و سایر زبان‌ها) صحبت کنید…"
        }

        _uiState.value = LiveTranslationUiState.Listening(
            activeChannel = channel,
            hintText = hint
        )

        audioRouter.startLiveRecording(
            preferredChannel = channel,
            autoStopOnSilence = _autoVadEnabled.value,
            onAutoSilenceDetected = { wavBytes ->
                processCapturedAudioWav(wavBytes, channel)
            },
            onError = { errorMsg ->
                _continuousLiveSession.value = false
                _uiState.value = LiveTranslationUiState.Error(errorMsg)
            }
        )
    }

    fun stopListeningAndTranslate() {
        val channel = activeRecordingChannel ?: ConversationChannel.AUTO_DETECT_LIVE
        val wavBytes = audioRouter.stopLiveRecordingAndGetWav()
        if (wavBytes.size <= 44) {
            _continuousLiveSession.value = false
            _uiState.value = LiveTranslationUiState.Idle
            return
        }
        processCapturedAudioWav(wavBytes, channel)
    }

    fun cancelListening() {
        _continuousLiveSession.value = false
        activeRecordingChannel = null
        audioRouter.stopLiveRecordingAndGetWav()
        _uiState.value = LiveTranslationUiState.Idle
    }

    /**
     * Sends recorded 16kHz Mono WAV audio directly to Gemini 3.8 Flash via Firebase AI Logic
     * for simultaneous speech recognition + translation + dual-channel hardware routing.
     */
    private fun processCapturedAudioWav(
        wavBytes: ByteArray,
        channel: ConversationChannel
    ) {
        viewModelScope.launch {
            _uiState.value = LiveTranslationUiState.ProcessingAudio(channel)
            val partnerLang = _partnerLanguage.value
            val tone = _selectedTone.value

            val directionInstruction = when (channel) {
                ConversationChannel.HOST_EARBUD_TO_SPEAKER -> """
                    The speaker is the Persian host speaking Persian (fa) (or occasionally ${partnerLang.englishName}).
                    Transcribe their speech accurately and translate it into ${partnerLang.englishName} (${partnerLang.code}).
                    Set targetLanguageCode to "${partnerLang.code}".
                """.trimIndent()

                ConversationChannel.PARTNER_PHONE_TO_EARBUD -> """
                    The speaker is the partner speaking ${partnerLang.englishName} (${partnerLang.code}) (or another supported language: en, ru, tr, es, ar).
                    Transcribe their speech accurately and translate it into natural Persian (fa).
                    Set targetLanguageCode to "fa".
                """.trimIndent()

                ConversationChannel.AUTO_DETECT_LIVE -> """
                    Auto-detect the spoken language in the audio:
                    - If the speaker is speaking Persian (fa), translate it into ${partnerLang.englishName} (${partnerLang.code}) and set targetLanguageCode = "${partnerLang.code}".
                    - If the speaker is speaking English (en), Russian (ru), Turkish (tr), Spanish (es), or Arabic (ar), transcribe it in that language and translate it into natural Persian (fa), setting targetLanguageCode = "fa".
                """.trimIndent()
            }

            val prompt = """
                You are Gemini Live Simultaneous Interpreter (هم‌زبان لایو) connecting a Persian speaker (wearing an earbud) and a partner holding the phone.
                $directionInstruction
                Style/Register instruction: ${tone.promptInstruction}
                Provide a clear phonetic pronunciation guide (phoneticGuide) so the listener or speaker can also read the pronunciation easily.
                If the audio has no human speech at all (only silence or background static), set isSilenceOrNoiseOnly = true.
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    structuredTranslationModel.generateContent(
                        content {
                            inlineData(wavBytes, "audio/wav")
                            text(prompt)
                        }
                    )
                }

                val jsonString = response.text
                if (jsonString.isNullOrBlank()) {
                    _uiState.value = LiveTranslationUiState.Error("پاسخی از مدل جمینای دریافت نشد. دوباره تلاش کنید.")
                    return@launch
                }

                val json = JSONObject(jsonString)
                val isSilence = json.optBoolean("isSilenceOrNoiseOnly", false)
                val transcript = json.optString("originalTranscript", "").trim()
                val translated = json.optString("translatedText", "").trim()

                if (isSilence || transcript.isBlank() || translated.isBlank()) {
                    _continuousLiveSession.value = false
                    _uiState.value = LiveTranslationUiState.Error(
                        "صدای واضحی در میکروفون تشخیص داده نشد. لطفاً نزدیک‌تر به میکروفون صحبت کنید یا از نوار تایپ/جملات آماده استفاده نمایید."
                    )
                    return@launch
                }

                val detectedSourceCode = json.optString("detectedSourceLanguageCode", "fa")
                val targetCode = json.optString("targetLanguageCode", partnerLang.code)
                val phonetic = json.optString("phoneticGuide", "")
                val culturalNote = json.optString("culturalOrContextNote", "").takeIf { it.isNotBlank() }

                val sourceLang = TranslateLanguage.fromCode(detectedSourceCode)
                    ?: if (channel == ConversationChannel.HOST_EARBUD_TO_SPEAKER) TranslateLanguage.PERSIAN else partnerLang
                val targetLang = TranslateLanguage.fromCode(targetCode)
                    ?: if (sourceLang == TranslateLanguage.PERSIAN) partnerLang else TranslateLanguage.PERSIAN

                // Determine physical audio output channel
                val resolvedChannel = when {
                    targetLang == TranslateLanguage.PERSIAN -> ConversationChannel.PARTNER_PHONE_TO_EARBUD
                    else -> ConversationChannel.HOST_EARBUD_TO_SPEAKER
                }

                // If partner spoke another supported language (e.g. Russian/Turkish/Spanish/Arabic/English), update active partner language automatically
                if (sourceLang != TranslateLanguage.PERSIAN) {
                    selectPartnerLanguage(sourceLang)
                }

                val routeDesc = audioRouter.speakTranslation(
                    text = translated,
                    language = targetLang,
                    channel = resolvedChannel,
                    routingMode = _routingMode.value,
                    phoneticFallback = phonetic
                )

                val turn = ConversationTurn(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    channel = resolvedChannel,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    originalText = transcript,
                    translatedText = translated,
                    phoneticGuide = phonetic,
                    culturalNote = culturalNote,
                    routedToDescription = routeDesc
                )

                prependTurn(turn)
                _uiState.value = LiveTranslationUiState.Success(turn)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Audio translation timed out", e)
                _continuousLiveSession.value = false
                _uiState.value = LiveTranslationUiState.Error(
                    "زمان پاسخگویی جمینای به پایان رسید (۶۰ ثانیه). لطفاً اتصال اینترنت را بررسی و مجدداً تلاش کنید."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI audio translation failed", e)
                _continuousLiveSession.value = false
                _uiState.value = LiveTranslationUiState.Error(
                    e.localizedMessage ?: "خطا در ارتباط با جمینای لایو."
                )
            }
        }
    }

    /**
     * Translates typed text or a tapped conversation phrase using Gemini streaming + structured JSON
     * and routes the spoken audio to the appropriate hardware output (Speaker for Partner, Earbud for Host).
     */
    fun translateTextAndRouteAudio(
        inputText: String,
        preferredChannel: ConversationChannel = ConversationChannel.AUTO_DETECT_LIVE
    ) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val partnerLang = _partnerLanguage.value
            val tone = _selectedTone.value
            _uiState.value = LiveTranslationUiState.StreamingPreview(
                activeChannel = preferredChannel,
                partialTranslation = "در حال ترجمه همزمان با جمینای…"
            )

            try {
                // Step 1: Stream live translation preview for ultra-fast Time-To-First-Token (TTFT)
                val streamPrompt = buildString {
                    append("Translate the following sentence simultaneously. ")
                    when (preferredChannel) {
                        ConversationChannel.HOST_EARBUD_TO_SPEAKER ->
                            append("Source is Persian (fa), translate into ${partnerLang.englishName} (${partnerLang.nativeName}). ")
                        ConversationChannel.PARTNER_PHONE_TO_EARBUD ->
                            append("Source is ${partnerLang.englishName}, translate into natural Persian (فارسی). ")
                        ConversationChannel.AUTO_DETECT_LIVE ->
                            append("If the text is in Persian, translate it into ${partnerLang.englishName}. Otherwise, translate it into natural Persian (فارسی). ")
                    }
                    append("Output ONLY the translated sentence without quotes:\n")
                    append(trimmed)
                }

                val streamedBuilder = StringBuilder()
                withTimeout(60_000L) {
                    streamingPreviewModel.generateContentStream(streamPrompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            streamedBuilder.append(part)
                            _uiState.value = LiveTranslationUiState.StreamingPreview(
                                activeChannel = preferredChannel,
                                partialTranslation = streamedBuilder.toString().trim()
                            )
                        }
                    }
                }

                // Step 2: Fetch rich structured metadata (detected language, phonetic pronunciation, cultural note)
                val structuredPrompt = """
                    Analyze and translate this utterance for a live dual-channel interpreter between Persian (fa) and ${partnerLang.englishName} (${partnerLang.code}) (also supporting en, ru, tr, es, ar):
                    Utterance: "$trimmed"
                    Preferred direction: ${preferredChannel.name}
                    Tone instruction: ${tone.promptInstruction}
                    Remember:
                    - If source is Persian (fa), translate to ${partnerLang.englishName} (${partnerLang.code}).
                    - If source is English (en), Russian (ru), Turkish (tr), Spanish (es), or Arabic (ar), translate to Persian (fa).
                    - Set isSilenceOrNoiseOnly = false.
                    - Write phoneticGuide so a Persian speaker can read the pronunciation easily.
                """.trimIndent()

                val structuredResponse = withTimeout(60_000L) {
                    structuredTranslationModel.generateContent(structuredPrompt)
                }

                val jsonText = structuredResponse.text
                if (jsonText.isNullOrBlank()) {
                    _uiState.value = LiveTranslationUiState.Error("پاسخ ساختاریافته از جمینای دریافت نشد.")
                    return@launch
                }

                val json = JSONObject(jsonText)
                val detectedSourceCode = json.optString("detectedSourceLanguageCode", "fa")
                val targetCode = json.optString("targetLanguageCode", partnerLang.code)
                val finalTranslated = json.optString("translatedText", streamedBuilder.toString().trim())
                    .ifBlank { streamedBuilder.toString().trim() }
                val phonetic = json.optString("phoneticGuide", "")
                val culturalNote = json.optString("culturalOrContextNote", "").takeIf { it.isNotBlank() }

                val sourceLang = TranslateLanguage.fromCode(detectedSourceCode) ?: TranslateLanguage.PERSIAN
                val targetLang = TranslateLanguage.fromCode(targetCode)
                    ?: if (sourceLang == TranslateLanguage.PERSIAN) partnerLang else TranslateLanguage.PERSIAN

                if (sourceLang != TranslateLanguage.PERSIAN) {
                    selectPartnerLanguage(sourceLang)
                }

                val resolvedChannel = if (targetLang == TranslateLanguage.PERSIAN) {
                    ConversationChannel.PARTNER_PHONE_TO_EARBUD
                } else {
                    ConversationChannel.HOST_EARBUD_TO_SPEAKER
                }

                val routeDesc = audioRouter.speakTranslation(
                    text = finalTranslated,
                    language = targetLang,
                    channel = resolvedChannel,
                    routingMode = _routingMode.value,
                    phoneticFallback = phonetic
                )

                val turn = ConversationTurn(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    channel = resolvedChannel,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    originalText = trimmed,
                    translatedText = finalTranslated,
                    phoneticGuide = phonetic,
                    culturalNote = culturalNote,
                    routedToDescription = routeDesc
                )

                prependTurn(turn)
                _uiState.value = LiveTranslationUiState.Success(turn)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Text translation timed out", e)
                _uiState.value = LiveTranslationUiState.Error(
                    "ترجمه بیش از ۶۰ ثانیه زمان برد. لطفاً دوباره تلاش کنید."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI text translation failed", e)
                _uiState.value = LiveTranslationUiState.Error(
                    e.localizedMessage ?: "خطا در ترجمه با جمینای."
                )
            }
        }
    }

    /**
     * Immediately plays a preset phrase or existing turn through the Dual-Channel Audio Router.
     */
    fun playQuickPhrase(phrase: QuickPhrase, asPartnerToEarbud: Boolean) {
        val partner = _partnerLanguage.value
        val foreignText = phrase.translations[partner] ?: phrase.translations[TranslateLanguage.ENGLISH].orEmpty()
        val phonetic = phrase.phonetics[partner].orEmpty()

        val channel = if (asPartnerToEarbud) {
            ConversationChannel.PARTNER_PHONE_TO_EARBUD
        } else {
            ConversationChannel.HOST_EARBUD_TO_SPEAKER
        }

        val sourceLang = if (asPartnerToEarbud) partner else TranslateLanguage.PERSIAN
        val targetLang = if (asPartnerToEarbud) TranslateLanguage.PERSIAN else partner
        val origText = if (asPartnerToEarbud) foreignText else phrase.persianText
        val transText = if (asPartnerToEarbud) phrase.persianText else foreignText

        val routeDesc = audioRouter.speakTranslation(
            text = transText,
            language = targetLang,
            channel = channel,
            routingMode = _routingMode.value,
            phoneticFallback = phonetic
        )

        val turn = ConversationTurn(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            channel = channel,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            originalText = origText,
            translatedText = transText,
            phoneticGuide = phonetic,
            culturalNote = "عبارت آماده (${phrase.categoryPersian})",
            routedToDescription = routeDesc
        )
        prependTurn(turn)
        _uiState.value = LiveTranslationUiState.Success(turn)
    }

    fun replayTurnAudio(turn: ConversationTurn) {
        audioRouter.speakTranslation(
            text = turn.translatedText,
            language = turn.targetLanguage,
            channel = turn.channel,
            routingMode = _routingMode.value,
            phoneticFallback = turn.phoneticGuide
        )
    }

    fun testSpeakerChannelForPartner() {
        val partner = _partnerLanguage.value
        audioRouter.speakTranslation(
            text = partner.sampleGreeting,
            language = partner,
            channel = ConversationChannel.HOST_EARBUD_TO_SPEAKER,
            routingMode = AudioRoutingMode.SPLIT_EARBUD_SPEAKER,
            phoneticFallback = partner.sampleGreeting
        )
    }

    fun testEarbudChannelForHost() {
        val partner = _partnerLanguage.value
        audioRouter.speakTranslation(
            text = partner.sampleGreetingPersian,
            language = TranslateLanguage.PERSIAN,
            channel = ConversationChannel.PARTNER_PHONE_TO_EARBUD,
            routingMode = AudioRoutingMode.SPLIT_EARBUD_SPEAKER,
            phoneticFallback = "Salaam, man az motarjemeh hamzaman estefadeh mikonam."
        )
    }

    fun toggleBookmark(turnId: String) {
        val updated = _conversationTurns.value.map {
            if (it.id == turnId) it.copy(isBookmarked = !it.isBookmarked) else it
        }
        _conversationTurns.value = updated
        saveConversationHistory(updated)
    }

    fun clearHistory() {
        _conversationTurns.value = emptyList()
        prefs.edit().remove(KEY_SAVED_HISTORY).apply()
    }

    fun dismissError() {
        if (_uiState.value is LiveTranslationUiState.Error) {
            _uiState.value = LiveTranslationUiState.Idle
        }
    }

    private fun prependTurn(turn: ConversationTurn) {
        val updated = (listOf(turn) + _conversationTurns.value).take(50)
        _conversationTurns.value = updated
        saveConversationHistory(updated)
    }

    private fun loadSavedPartnerLanguage(): TranslateLanguage {
        val code = prefs.getString(KEY_PARTNER_LANG, TranslateLanguage.ENGLISH.code)
            ?: TranslateLanguage.ENGLISH.code
        return TranslateLanguage.fromCode(code)?.takeIf { it != TranslateLanguage.PERSIAN }
            ?: TranslateLanguage.ENGLISH
    }

    private fun loadSavedRoutingMode(): AudioRoutingMode {
        val name = prefs.getString(KEY_ROUTING_MODE, AudioRoutingMode.SPLIT_EARBUD_SPEAKER.name)
        return AudioRoutingMode.entries.find { it.name == name }
            ?: AudioRoutingMode.SPLIT_EARBUD_SPEAKER
    }

    private fun loadSavedTone(): TranslationTone {
        val name = prefs.getString(KEY_TONE, TranslationTone.CONVERSATIONAL.name)
        return TranslationTone.entries.find { it.name == name }
            ?: TranslationTone.CONVERSATIONAL
    }

    private fun saveConversationHistory(turns: List<ConversationTurn>) {
        try {
            val array = JSONArray()
            turns.forEach { turn ->
                val obj = JSONObject().apply {
                    put("id", turn.id)
                    put("timestampMillis", turn.timestampMillis)
                    put("channel", turn.channel.name)
                    put("sourceLanguage", turn.sourceLanguage.code)
                    put("targetLanguage", turn.targetLanguage.code)
                    put("originalText", turn.originalText)
                    put("translatedText", turn.translatedText)
                    put("phoneticGuide", turn.phoneticGuide)
                    put("culturalNote", turn.culturalNote ?: "")
                    put("routedToDescription", turn.routedToDescription)
                    put("isBookmarked", turn.isBookmarked)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist conversation history", e)
        }
    }

    private fun loadConversationHistory() {
        try {
            val raw = prefs.getString(KEY_SAVED_HISTORY, null) ?: return
            val array = JSONArray(raw)
            val list = mutableListOf<ConversationTurn>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val channelName = obj.optString("channel", ConversationChannel.HOST_EARBUD_TO_SPEAKER.name)
                val channel = ConversationChannel.entries.find { it.name == channelName }
                    ?: ConversationChannel.HOST_EARBUD_TO_SPEAKER
                val src = TranslateLanguage.fromCode(obj.optString("sourceLanguage", "fa"))
                    ?: TranslateLanguage.PERSIAN
                val tgt = TranslateLanguage.fromCode(obj.optString("targetLanguage", "en"))
                    ?: TranslateLanguage.ENGLISH
                list.add(
                    ConversationTurn(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
                        channel = channel,
                        sourceLanguage = src,
                        targetLanguage = tgt,
                        originalText = obj.optString("originalText", ""),
                        translatedText = obj.optString("translatedText", ""),
                        phoneticGuide = obj.optString("phoneticGuide", ""),
                        culturalNote = obj.optString("culturalNote", "").takeIf { it.isNotBlank() },
                        routedToDescription = obj.optString("routedToDescription", ""),
                        isBookmarked = obj.optBoolean("isBookmarked", false)
                    )
                )
            }
            _conversationTurns.value = list
        } catch (e: Exception) {
            Log.w(TAG, "Could not load conversation history", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRouter.release()
    }
}
