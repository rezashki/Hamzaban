package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConversationChannel
import com.example.model.ConversationTurn
import com.example.model.PresetPhrasesRepository
import com.example.model.TranslateLanguage
import com.example.ui.components.AudioSetupAndTestTab
import com.example.ui.components.DualChannelStatusBanner
import com.example.ui.components.LanguageSelectorBar
import com.example.ui.components.LiveWaveformVisualizer
import com.example.ui.components.PhrasesAndHistoryTab
import com.example.ui.theme.EarbudCyan
import com.example.ui.theme.EarbudCyanContainer
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GeminiLiveEmerald
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.MidnightCardElevated
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceVariant
import com.example.ui.theme.SpeakerAmber
import com.example.ui.theme.SpeakerAmberContainer
import com.example.viewmodel.LiveInterpreterViewModel
import com.example.viewmodel.LiveTranslationUiState
import java.util.Locale

@Composable
fun LiveInterpreterScreen(
    viewModel: LiveInterpreterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hardwareState by viewModel.hardwareState.collectAsStateWithLifecycle()
    val partnerLanguage by viewModel.partnerLanguage.collectAsStateWithLifecycle()
    val routingMode by viewModel.routingMode.collectAsStateWithLifecycle()
    val selectedTone by viewModel.selectedTone.collectAsStateWithLifecycle()
    val autoVadEnabled by viewModel.autoVadEnabled.collectAsStateWithLifecycle()
    val flipPartnerCard180 by viewModel.flipPartnerCard180.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val waveformBars by viewModel.waveformBars.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val conversationTurns by viewModel.conversationTurns.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingChannelAfterPermission by remember { mutableStateOf<ConversationChannel?>(null) }
    var micPermissionDeniedBanner by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            micPermissionDeniedBanner = false
            pendingChannelAfterPermission?.let { ch ->
                viewModel.startChannelListening(ch)
            }
            pendingChannelAfterPermission = null
        } else {
            micPermissionDeniedBanner = true
        }
    }

    fun triggerVoiceChannel(channel: ConversationChannel) {
        val currentState = uiState
        if (currentState is LiveTranslationUiState.Listening) {
            viewModel.stopListeningAndTranslate()
            return
        }
        val grantedNow = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        hasMicPermission = grantedNow
        if (grantedNow) {
            viewModel.startChannelListening(channel)
        } else {
            pendingChannelAfterPermission = channel
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MidnightBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    containerColor = MidnightSurface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "مترجم زنده جمینای"
                            )
                        },
                        label = { Text("مترجم زنده") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EarbudCyan,
                            selectedTextColor = EarbudCyan,
                            indicatorColor = EarbudCyanContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_live_interpreter")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "تنظیمات هدفون و اسپیکر"
                            )
                        },
                        label = { Text("هدفون و صدا") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpeakerAmber,
                            selectedTextColor = SpeakerAmber,
                            indicatorColor = SpeakerAmberContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_audio_setup")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "جملات آماده و تاریخچه"
                            )
                        },
                        label = { Text("عبارات و تاریخچه") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeminiLiveEmerald,
                            selectedTextColor = GeminiLiveEmerald,
                            indicatorColor = EarbudCyanContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_phrases_history")
                    )
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                ) {
                    // App Top Header
                    TopLiveHeader(
                        partnerLanguage = partnerLanguage,
                        isListening = uiState is LiveTranslationUiState.Listening,
                        isSpeaking = hardwareState.isSpeakingNow
                    )

                    // Language Selector Strip (English, Russian, Turkish, Spanish, Arabic <-> Persian)
                    LanguageSelectorBar(
                        selectedPartnerLanguage = partnerLanguage,
                        onSelectLanguage = viewModel::selectPartnerLanguage,
                        flipPartner180 = flipPartnerCard180,
                        onToggleFlip180 = viewModel::toggleFlipPartnerCard180
                    )

                    when (selectedTab) {
                        0 -> LiveDualStageContent(
                            uiState = uiState,
                            hardwareState = hardwareState,
                            partnerLanguage = partnerLanguage,
                            routingMode = routingMode,
                            flipPartnerCard180 = flipPartnerCard180,
                            waveformBars = waveformBars,
                            recordingSeconds = recordingSeconds,
                            conversationTurns = conversationTurns,
                            micPermissionDeniedBanner = micPermissionDeniedBanner,
                            onRequestMicPermission = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onTriggerChannel = ::triggerVoiceChannel,
                            onCancelListening = viewModel::cancelListening,
                            onSendText = viewModel::translateTextAndRouteAudio,
                            onReplayTurn = viewModel::replayTurnAudio,
                            onPlayQuickPhrase = viewModel::playQuickPhrase,
                            onDismissError = viewModel::dismissError,
                            onOpenAudioTab = { selectedTab = 1 }
                        )

                        1 -> AudioSetupAndTestTab(
                            hardwareState = hardwareState,
                            routingMode = routingMode,
                            selectedTone = selectedTone,
                            partnerLanguage = partnerLanguage,
                            autoVadEnabled = autoVadEnabled,
                            speechRate = speechRate,
                            onSelectRoutingMode = viewModel::selectRoutingMode,
                            onSelectTone = viewModel::selectTone,
                            onToggleAutoVad = viewModel::setAutoVadEnabled,
                            onUpdateSpeechRate = viewModel::updateSpeechRate,
                            onRefreshDevices = viewModel::refreshAudioHardware,
                            onTestSpeakerForPartner = viewModel::testSpeakerChannelForPartner,
                            onTestEarbudForHost = viewModel::testEarbudChannelForHost
                        )

                        2 -> PhrasesAndHistoryTab(
                            partnerLanguage = partnerLanguage,
                            conversationTurns = conversationTurns,
                            onPlayQuickPhrase = viewModel::playQuickPhrase,
                            onReplayTurn = viewModel::replayTurnAudio,
                            onToggleBookmark = viewModel::toggleBookmark,
                            onClearHistory = viewModel::clearHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopLiveHeader(
    partnerLanguage: TranslateLanguage,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EarbudCyan, SpeakerAmber)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color(0xFF070D1B),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "هم‌زبان لایو • Gemini Live",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "فارسی ⇄ ${partnerLanguage.persianName} (${partnerLanguage.nativeName})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = when {
                isListening -> ErrorContainerDark
                isSpeaking -> EarbudCyanContainer
                else -> MidnightSurfaceVariant
            },
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                when {
                    isListening -> ErrorCoral
                    isSpeaking -> EarbudCyan
                    else -> GeminiLiveEmerald.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isListening -> ErrorCoral
                                isSpeaking -> EarbudCyan
                                else -> GeminiLiveEmerald
                            }
                        )
                )
                Text(
                    text = when {
                        isListening -> "در حال شنیدن…"
                        isSpeaking -> "پخش صوتی…"
                        else -> "جمینای آماده"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LiveDualStageContent(
    uiState: LiveTranslationUiState,
    hardwareState: com.example.audio.AudioHardwareState,
    partnerLanguage: TranslateLanguage,
    routingMode: com.example.model.AudioRoutingMode,
    flipPartnerCard180: Boolean,
    waveformBars: List<Float>,
    recordingSeconds: Float,
    conversationTurns: List<ConversationTurn>,
    micPermissionDeniedBanner: Boolean,
    onRequestMicPermission: () -> Unit,
    onTriggerChannel: (ConversationChannel) -> Unit,
    onCancelListening: () -> Unit,
    onSendText: (String, ConversationChannel) -> Unit,
    onReplayTurn: (ConversationTurn) -> Unit,
    onPlayQuickPhrase: (com.example.model.QuickPhrase, Boolean) -> Unit,
    onDismissError: () -> Unit,
    onOpenAudioTab: () -> Unit
) {
    var manualInputText by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val latestToSpeakerTurn = remember(conversationTurns, partnerLanguage) {
        conversationTurns.firstOrNull {
            it.channel == ConversationChannel.HOST_EARBUD_TO_SPEAKER
        }
    }
    val latestToEarbudTurn = remember(conversationTurns) {
        conversationTurns.firstOrNull {
            it.channel == ConversationChannel.PARTNER_PHONE_TO_EARBUD
        }
    }

    val rotationDegrees by animateFloatAsState(
        targetValue = if (flipPartnerCard180) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "partnerCardRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dual-Channel Hardware Status Banner
        DualChannelStatusBanner(
            hardwareState = hardwareState,
            routingMode = routingMode,
            partnerLanguage = partnerLanguage,
            onOpenAudioTab = onOpenAudioTab
        )

        // Optional Microphone Permission Banner
        AnimatedVisibility(visible = micPermissionDeniedBanner) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorContainerDark),
                border = BorderStroke(1.dp, ErrorCoral)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            tint = ErrorCoral
                        )
                        Text(
                            text = "برای ضبط صدای زنده، مجوز میکروفون را فعال کنید یا از کادر متنی پایین استفاده نمایید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestMicPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral),
                        modifier = Modifier.testTag("grant_mic_permission_button")
                    ) {
                        Text("فعالسازی", color = Color.Black)
                    }
                }
            }
        }

        // Error Banner (Transparent Error Propagation per Firebase AI Logic skill)
        if (uiState is LiveTranslationUiState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("ai_error_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorContainerDark),
                border = BorderStroke(1.dp, ErrorCoral)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFDAD6),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پیام خطا",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 1. PARTNER PHONE STAGE (گوشی در دست طرف مقابل • خروجی اسپیکر گوشی)
        // =========================================================================
        val isPartnerListening = (uiState as? LiveTranslationUiState.Listening)?.activeChannel ==
            ConversationChannel.PARTNER_PHONE_TO_EARBUD

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .graphicsLayer { rotationZ = rotationDegrees }
                .testTag("partner_channel_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCardElevated),
            border = BorderStroke(
                width = if (isPartnerListening) 2.dp else 1.dp,
                color = if (isPartnerListening) SpeakerAmber else SpeakerAmber.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SpeakerAmberContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SpeakerPhone,
                                    contentDescription = null,
                                    tint = SpeakerAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "بخش طرف مقابل (${partnerLanguage.persianName} • ${partnerLanguage.nativeName})",
                                style = MaterialTheme.typography.titleMedium,
                                color = SpeakerAmber
                            )
                            Text(
                                text = "پخش صدای ترجمه‌شده شما از اسپیکر گوشی • دریافت صدای مخاطب",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (latestToSpeakerTurn != null) {
                        IconButton(
                            onClick = { onReplayTurn(latestToSpeakerTurn) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SpeakerAmberContainer)
                                .size(38.dp)
                                .testTag("replay_partner_speaker_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "پخش مجدد از اسپیکر برای مخاطب",
                                tint = SpeakerAmber
                            )
                        }
                    }
                }

                // Translation box shown to Partner in their language
                Surface(
                    color = Color(0xFF0A1222),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (latestToSpeakerTurn != null) {
                            Text(
                                text = latestToSpeakerTurn.translatedText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    textDirection = if (partnerLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                                ),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "جمله شما (فارسی): ${latestToSpeakerTurn.originalText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (latestToSpeakerTurn.phoneticGuide.isNotBlank()) {
                                Text(
                                    text = "تلفظ: ${latestToSpeakerTurn.phoneticGuide}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SpeakerAmber
                                )
                            }
                        } else {
                            Text(
                                text = partnerLanguage.sampleGreeting,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = if (partnerLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = partnerLanguage.sampleGreetingPersian,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Partner Mic Action Button (Partner speaks -> Translated to Persian in User's Earbud)
                Button(
                    onClick = {
                        onTriggerChannel(ConversationChannel.PARTNER_PHONE_TO_EARBUD)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPartnerListening) ErrorCoral else SpeakerAmber,
                        contentColor = Color(0xFF1A1100)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("partner_mic_button")
                ) {
                    Icon(
                        imageVector = if (isPartnerListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPartnerListening) {
                            "پایان صحبت مخاطب و ترجمه به فارسی (${String.format(Locale.US, "%.1f", recordingSeconds)}s)"
                        } else {
                            "میکروفون طرف مقابل (${partnerLanguage.nativeName} ⬅ هدفون فارسی شما)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 2. GEMINI LIVE BIDIRECTIONAL HUB & STREAMING WAVEFORM STAGE
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
            border = BorderStroke(1.dp, GeminiLiveEmerald.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activeChannel = when (uiState) {
                    is LiveTranslationUiState.Listening -> uiState.activeChannel
                    is LiveTranslationUiState.ProcessingAudio -> uiState.activeChannel
                    is LiveTranslationUiState.StreamingPreview -> uiState.activeChannel
                    else -> null
                }

                LiveWaveformVisualizer(
                    bars = waveformBars,
                    isActive = uiState is LiveTranslationUiState.Listening || hardwareState.isSpeakingNow,
                    activeChannel = activeChannel
                )

                when (uiState) {
                    is LiveTranslationUiState.Listening -> {
                        Text(
                            text = uiState.hintText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GeminiLiveEmerald,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onTriggerChannel(uiState.activeChannel) },
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiLiveEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("finish_recording_translate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ترجمه فوری با جمینای (${String.format(Locale.US, "%.1f", recordingSeconds)}s)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelListening,
                                border = BorderStroke(1.dp, ErrorCoral),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("لغو", color = ErrorCoral)
                            }
                        }
                    }

                    is LiveTranslationUiState.ProcessingAudio -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = EarbudCyan,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "جمینای لایو در حال تشخیص گفتار و ترجمه همزمان صوتی…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EarbudCyan
                            )
                        }
                    }

                    is LiveTranslationUiState.StreamingPreview -> {
                        Surface(
                            color = Color(0xFF0A1526),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "در حال استریم زنده از جمینای:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GeminiLiveEmerald
                                )
                                Text(
                                    text = uiState.partialTranslation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                onTriggerChannel(ConversationChannel.AUTO_DETECT_LIVE)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GeminiLiveEmerald,
                                contentColor = Color(0xFF032215)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("gemini_live_auto_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جمینای لایو هوشمند (تشخیص خودکار فارسی ⇄ ${partnerLanguage.persianName})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. HOST EARBUD STAGE (شما با هدفون در گوش • شنیدن ترجمه فارسی)
        // =========================================================================
        val isHostListening = (uiState as? LiveTranslationUiState.Listening)?.activeChannel ==
            ConversationChannel.HOST_EARBUD_TO_SPEAKER

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("host_channel_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCardElevated),
            border = BorderStroke(
                width = if (isHostListening) 2.dp else 1.dp,
                color = if (isHostListening) EarbudCyan else EarbudCyan.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = EarbudCyanContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = EarbudCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "بخش شما (هدفون در گوش • فارسی)",
                                style = MaterialTheme.typography.titleMedium,
                                color = EarbudCyan
                            )
                            Text(
                                text = "شنیدن ترجمه فارسی مخاطب در هدفون • صحبت شما به فارسی",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (latestToEarbudTurn != null) {
                        IconButton(
                            onClick = { onReplayTurn(latestToEarbudTurn) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EarbudCyanContainer)
                                .size(38.dp)
                                .testTag("replay_host_earbud_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "پخش مجدد ترجمه فارسی در هدفون شما",
                                tint = EarbudCyan
                            )
                        }
                    }
                }

                // Translation box heard by Host in Persian
                Surface(
                    color = Color(0xFF0A1222),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (latestToEarbudTurn != null) {
                            Text(
                                text = latestToEarbudTurn.translatedText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    textDirection = TextDirection.Rtl
                                ),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "جمله مخاطب (${latestToEarbudTurn.sourceLanguage.persianName}): ${latestToEarbudTurn.originalText}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDirection = if (latestToEarbudTurn.sourceLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!latestToEarbudTurn.culturalNote.isNullOrBlank()) {
                                Text(
                                    text = "💡 ${latestToEarbudTurn.culturalNote}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EarbudCyan
                                )
                            }
                        } else {
                            Text(
                                text = "وقتی طرف مقابل به ${partnerLanguage.persianName} صحبت کند، ترجمه فارسی بلافاصله در هدفون شما پخش و اینجا نمایش داده می‌شود.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Host Mic Action Button (Host speaks Persian -> Translated to Foreign Language on Phone Speaker)
                Button(
                    onClick = {
                        onTriggerChannel(ConversationChannel.HOST_EARBUD_TO_SPEAKER)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHostListening) ErrorCoral else EarbudCyan,
                        contentColor = Color(0xFF00222B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("host_mic_button")
                ) {
                    Icon(
                        imageVector = if (isHostListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHostListening) {
                            "پایان صحبت شما و پخش ${partnerLanguage.persianName} از اسپیکر (${String.format(Locale.US, "%.1f", recordingSeconds)}s)"
                        } else {
                            "میکروفون شما (صحبت به فارسی ⬅ پخش ${partnerLanguage.persianName} از اسپیکر)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 4. QUICK ONE-TAP CONVERSATION CHIPS & INSTANT TEXT TRANSLATION BAR
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ارسال سریع متن یا جملات پرتکرار به جمینای (با پخش صوتی در کانال مربوطه):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPhrasesRepository.phrases.take(4).forEach { quickPhrase ->
                        Surface(
                            color = Color(0xFF0D182C),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EarbudCyan.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlayQuickPhrase(quickPhrase, false) }
                                .testTag("quick_chip_${quickPhrase.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SpeakerPhone,
                                    contentDescription = null,
                                    tint = SpeakerAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = quickPhrase.categoryPersian,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = {
                            Text(
                                text = "به فارسی یا ${partnerLanguage.persianName} بنویسید تا جمینای ترجمه و پخش کند…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (manualInputText.isNotBlank()) {
                                    onSendText(manualInputText, ConversationChannel.AUTO_DETECT_LIVE)
                                    manualInputText = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EarbudCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color(0xFF0A1222),
                            unfocusedContainerColor = Color(0xFF0A1222),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_text_input")
                    )

                    Button(
                        onClick = {
                            if (manualInputText.isNotBlank()) {
                                onSendText(manualInputText, ConversationChannel.AUTO_DETECT_LIVE)
                                manualInputText = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = manualInputText.isNotBlank() &&
                            uiState !is LiveTranslationUiState.ProcessingAudio &&
                            uiState !is LiveTranslationUiState.StreamingPreview,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EarbudCyan,
                            contentColor = Color(0xFF00242C)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("send_text_translate_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال برای ترجمه همزمان"
                        )
                    }
                }
            }
        }
    }
}
