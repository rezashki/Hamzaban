package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioHardwareState
import com.example.model.AudioRoutingMode
import com.example.model.ConversationChannel
import com.example.model.TranslateLanguage
import com.example.model.TranslationTone
import com.example.ui.theme.EarbudCyan
import com.example.ui.theme.EarbudCyanContainer
import com.example.ui.theme.GeminiLiveEmerald
import com.example.ui.theme.MidnightCardElevated
import com.example.ui.theme.MidnightSurfaceVariant
import com.example.ui.theme.SpeakerAmber
import com.example.ui.theme.SpeakerAmberContainer

@Composable
fun LanguageSelectorBar(
    selectedPartnerLanguage: TranslateLanguage,
    onSelectLanguage: (TranslateLanguage) -> Unit,
    flipPartner180: Boolean,
    onToggleFlip180: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = EarbudCyanContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EarbudCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "زبان شما: فارسی (FA)",
                        style = MaterialTheme.typography.labelLarge,
                        color = EarbudCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "⇄",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "زبان طرف مقابل:",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpeakerAmber
                )
            }

            Surface(
                color = if (flipPartner180) SpeakerAmberContainer else MidnightSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (flipPartner180) SpeakerAmber else Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleFlip180() }
                    .testTag("toggle_flip_partner_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "چرخش ۱۸۰ درجه کارت مخاطب",
                        tint = if (flipPartner180) SpeakerAmber else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (flipPartner180) "حالت روبه‌رو فعال" else "چرخش مخاطب",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (flipPartner180) SpeakerAmber else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TranslateLanguage.partnerLanguages.forEach { lang ->
                val isSelected = lang == selectedPartnerLanguage
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectLanguage(lang) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (isSelected) SpeakerAmber else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = lang.shortBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF1F1300) else Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${lang.persianName} (${lang.nativeName})",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpeakerAmberContainer,
                        selectedLabelColor = SpeakerAmber,
                        containerColor = MidnightSurfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.12f),
                        selectedBorderColor = SpeakerAmber,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.testTag("lang_chip_${lang.code}")
                )
            }
        }
    }
}

@Composable
fun DualChannelStatusBanner(
    hardwareState: AudioHardwareState,
    routingMode: AudioRoutingMode,
    partnerLanguage: TranslateLanguage,
    onOpenAudioTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speakingChannel = hardwareState.speakingChannel
    val earbudActive = hardwareState.isSpeakingNow &&
        speakingChannel == ConversationChannel.PARTNER_PHONE_TO_EARBUD
    val speakerActive = hardwareState.isSpeakingNow &&
        speakingChannel == ConversationChannel.HOST_EARBUD_TO_SPEAKER

    val earbudBorder by animateColorAsState(
        targetValue = if (earbudActive) EarbudCyan else EarbudCyan.copy(alpha = 0.35f),
        label = "earbudBorder"
    )
    val speakerBorder by animateColorAsState(
        targetValue = if (speakerActive) SpeakerAmber else SpeakerAmber.copy(alpha = 0.35f),
        label = "speakerBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onOpenAudioTab() }
            .testTag("dual_channel_status_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel 1: Earbud / Earpiece (for Persian host)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (earbudActive) EarbudCyanContainer else Color(0xFF0B1528),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (earbudActive) 2.dp else 1.dp, earbudBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hardwareState.hasExternalHeadset) {
                                Icons.Default.Headphones
                            } else {
                                Icons.Default.Hearing
                            },
                            contentDescription = "کانال هدفون شما",
                            tint = EarbudCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (hardwareState.hasExternalHeadset) {
                                    "هدفون در گوش شما"
                                } else {
                                    "گوشی کنار گوش شما"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = EarbudCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "شنیدن ترجمه فارسی",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Channel 2: Phone Speaker (for Foreign partner)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = if (speakerActive) SpeakerAmberContainer else Color(0xFF0B1528),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (speakerActive) 2.dp else 1.dp, speakerBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpeakerPhone,
                            contentDescription = "کانال اسپیکر مخاطب",
                            tint = SpeakerAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "اسپیکر برای مخاطب",
                                style = MaterialTheme.typography.labelLarge,
                                color = SpeakerAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "پخش ترجمه ${partnerLanguage.persianName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (hardwareState.isSpeakingNow) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = GeminiLiveEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hardwareState.activeRouteLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = GeminiLiveEmerald
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${routingMode.title} • ${hardwareState.headsetDeviceName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LiveWaveformVisualizer(
    bars: List<Float>,
    isActive: Boolean,
    activeChannel: ConversationChannel?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idleWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val animatedBars = bars.mapIndexed { index, raw ->
        val target = if (isActive) {
            raw.coerceIn(0.12f, 1f)
        } else {
            0.14f + 0.08f * kotlin.math.sin(phase + index * 0.45f)
        }
        val state by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 90),
            label = "bar_$index"
        )
        state
    }

    val primaryColor = when (activeChannel) {
        ConversationChannel.HOST_EARBUD_TO_SPEAKER -> EarbudCyan
        ConversationChannel.PARTNER_PHONE_TO_EARBUD -> SpeakerAmber
        ConversationChannel.AUTO_DETECT_LIVE, null -> GeminiLiveEmerald
    }
    val secondaryColor = when (activeChannel) {
        ConversationChannel.HOST_EARBUD_TO_SPEAKER -> SpeakerAmber
        ConversationChannel.PARTNER_PHONE_TO_EARBUD -> EarbudCyan
        ConversationChannel.AUTO_DETECT_LIVE, null -> EarbudCyan
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val count = animatedBars.size.coerceAtLeast(1)
        val totalSpacing = size.width * 0.32f
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(4f)
        val gap = if (count > 1) totalSpacing / (count - 1) else 0f

        animatedBars.forEachIndexed { idx, fraction ->
            val barHeight = (size.height * fraction).coerceAtLeast(6.dp.toPx())
            val x = idx * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioSetupAndTestTab(
    hardwareState: AudioHardwareState,
    routingMode: AudioRoutingMode,
    selectedTone: TranslationTone,
    partnerLanguage: TranslateLanguage,
    autoVadEnabled: Boolean,
    speechRate: Float,
    onSelectRoutingMode: (AudioRoutingMode) -> Unit,
    onSelectTone: (TranslationTone) -> Unit,
    onToggleAutoVad: (Boolean) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onRefreshDevices: () -> Unit,
    onTestSpeakerForPartner: () -> Unit,
    onTestEarbudForHost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner illustrating Earbud + Phone Speaker simultaneous setup
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightCardElevated),
            border = BorderStroke(1.dp, EarbudCyan.copy(alpha = 0.3f))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_live_interpreter_hero_1793851554658),
                        contentDescription = "تصویر راهنمای اتصال هدفون و اسپیکر جمینای لایو",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xE6070D1B)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "نحوه کارکرد هدفون + اسپیکر در ترجمه همزمان",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "بدون نیاز به دست‌به‌دست کردن مداوم گوشی",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EarbudCyan
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SetupStepRow(
                        stepNumber = "۱",
                        title = "یک هدفون در گوش خودتان بگذارید",
                        description = "هدفون بلوتوث یا سیمی را در گوش خود قرار دهید (یا در صورت نبود هدفون، کپسول بالای گوشی را کنار گوش خود نگه دارید).",
                        accentColor = EarbudCyan
                    )
                    SetupStepRow(
                        stepNumber = "۲",
                        title = "گوشی را مقابل یا در دست طرف مقابل قرار دهید",
                        description = "وقتی شما به فارسی صحبت می‌کنید، صدای ترجمه‌شده (${partnerLanguage.persianName}) از اسپیکر بلندگوی گوشی برای طرف مقابل پخش می‌شود.",
                        accentColor = SpeakerAmber
                    )
                    SetupStepRow(
                        stepNumber = "۳",
                        title = "وقتی طرف مقابل صحبت می‌کند، ترجمه در گوش شماست",
                        description = "وقتی مخاطب به ${partnerLanguage.persianName} با گوشی صحبت می‌کند، ترجمه فارسی مستقیماً در هدفون/گوشی شما پخش می‌شود.",
                        accentColor = GeminiLiveEmerald
                    )
                }
            }
        }

        // Hardware Detection & Live Route Test Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = EarbudCyan
                        )
                        Text(
                            text = "وضعیت سخت‌افزار صوتی و تست کانال‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.testTag("refresh_audio_devices_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی وضعیت هدفون",
                            tint = EarbudCyan
                        )
                    }
                }

                Surface(
                    color = Color(0xFF0B1325),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hardwareState.hasExternalHeadset) GeminiLiveEmerald else SpeakerAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "کانال گوش شما: ${hardwareState.headsetDeviceName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = SpeakerAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "کانال طرف مقابل: ${hardwareState.speakerDeviceName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestEarbudForHost,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EarbudCyanContainer,
                            contentColor = EarbudCyan
                        ),
                        border = BorderStroke(1.dp, EarbudCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_earbud_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تست صدای هدفون (فارسی)",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onTestSpeakerForPartner,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpeakerAmberContainer,
                            contentColor = SpeakerAmber
                        ),
                        border = BorderStroke(1.dp, SpeakerAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_speaker_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpeakerPhone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تست اسپیکر (${partnerLanguage.persianName})",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Audio Routing Mode Selector
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حالت مسیردهی صدا (Audio Routing)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                AudioRoutingMode.entries.forEach { mode ->
                    val selected = mode == routingMode
                    Surface(
                        color = if (selected) EarbudCyanContainer.copy(alpha = 0.7f) else Color(0xFF0B1325),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selected) EarbudCyan else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectRoutingMode(mode) }
                            .testTag("routing_mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectRoutingMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = EarbudCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Voice Activity Detection (Auto Turn-Taking) & Speech Speed & Tone
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تشخیص خودکار پایان جمله (Auto-VAD)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "وقتی گوینده مکث می‌کند، صدای ضبط‌شده به صورت خودکار برای ترجمه همزمان به جمینای ارسال شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoVadEnabled,
                        onCheckedChange = onToggleAutoVad,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EarbudCyan,
                            checkedTrackColor = EarbudCyanContainer
                        ),
                        modifier = Modifier.testTag("auto_vad_switch")
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "سرعت پخش صوتی مترجم",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", speechRate)}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = EarbudCyan
                        )
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = onUpdateSpeechRate,
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = EarbudCyan,
                            activeTrackColor = EarbudCyan
                        ),
                        modifier = Modifier.testTag("speech_rate_slider")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "لحن و حوزه تخصصی ترجمه جمینای:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TranslationTone.entries.forEach { tone ->
                            val selected = tone == selectedTone
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectTone(tone) },
                                label = { Text(tone.persianLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EarbudCyanContainer,
                                    selectedLabelColor = EarbudCyan
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    stepNumber: String,
    title: String,
    description: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
