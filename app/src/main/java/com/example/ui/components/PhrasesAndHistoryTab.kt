package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.model.ConversationChannel
import com.example.model.ConversationTurn
import com.example.model.PresetPhrasesRepository
import com.example.model.QuickPhrase
import com.example.model.TranslateLanguage
import com.example.ui.theme.EarbudCyan
import com.example.ui.theme.EarbudCyanContainer
import com.example.ui.theme.MidnightCardElevated
import com.example.ui.theme.MidnightSurfaceVariant
import com.example.ui.theme.SpeakerAmber
import com.example.ui.theme.SpeakerAmberContainer

@Composable
fun PhrasesAndHistoryTab(
    partnerLanguage: TranslateLanguage,
    conversationTurns: List<ConversationTurn>,
    onPlayQuickPhrase: (QuickPhrase, Boolean) -> Unit,
    onReplayTurn: (ConversationTurn) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("جملات آماده (${partnerLanguage.persianName} ⇄ فارسی)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SpeakerAmberContainer,
                    selectedLabelColor = SpeakerAmber
                ),
                modifier = Modifier.testTag("subtab_quick_phrases")
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("تاریخچه مکالمات (${conversationTurns.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EarbudCyanContainer,
                    selectedLabelColor = EarbudCyan
                ),
                modifier = Modifier.testTag("subtab_history")
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selectedSubTab == 1 && conversationTurns.isNotEmpty()) {
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "پاک کردن تاریخچه",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PresetPhrasesRepository.phrases, key = { it.id }) { phrase ->
                    val foreignText = phrase.translations[partnerLanguage].orEmpty()
                    val phonetic = phrase.phonetics[partnerLanguage].orEmpty()

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phrase_card_${phrase.id}")
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
                                Surface(
                                    color = EarbudCyanContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = phrase.categoryPersian,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EarbudCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "فارسی ⇄ ${partnerLanguage.persianName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SpeakerAmber
                                )
                            }

                            Text(
                                text = phrase.persianText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Rtl
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                color = Color(0xFF0A1324),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = foreignText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDirection = if (partnerLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                                        ),
                                        color = SpeakerAmber,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (phonetic.isNotBlank()) {
                                        Text(
                                            text = "تلفظ: $phonetic",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onPlayQuickPhrase(phrase, false) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SpeakerAmberContainer,
                                        contentColor = SpeakerAmber
                                    ),
                                    border = BorderStroke(1.dp, SpeakerAmber.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_phrase_speaker_${phrase.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SpeakerPhone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "پخش از اسپیکر (${partnerLanguage.shortBadge})",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onPlayQuickPhrase(phrase, true) },
                                    border = BorderStroke(1.dp, EarbudCyan.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("play_phrase_earbud_${phrase.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = EarbudCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "پخش در هدفون (FA)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EarbudCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (conversationTurns.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = EarbudCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "هنوز مکالمه‌ای ثبت نشده است",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "از زبانه «مترجم زنده» یا «جملات آماده» استفاده کنید تا ترجمه‌ها همراه با مسیر صوتی در اینجا ذخیره شوند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversationTurns, key = { it.id }) { turn ->
                        ConversationTurnCard(
                            turn = turn,
                            onReplay = { onReplayTurn(turn) },
                            onToggleBookmark = { onToggleBookmark(turn.id) },
                            onCopy = {
                                clipboard.setText(
                                    AnnotatedString("${turn.originalText}\n${turn.translatedText}")
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationTurnCard(
    turn: ConversationTurn,
    onReplay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToSpeaker = turn.channel == ConversationChannel.HOST_EARBUD_TO_SPEAKER
    val accent = if (isToSpeaker) SpeakerAmber else EarbudCyan
    val badgeBg = if (isToSpeaker) SpeakerAmberContainer else EarbudCyanContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCardElevated),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("turn_card_${turn.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isToSpeaker) Icons.Default.SpeakerPhone else Icons.Default.Headphones,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${turn.sourceLanguage.persianName} ⬅ ${turn.targetLanguage.persianName} • ${turn.routedToDescription}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onReplay,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "پخش مجدد ترجمه",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی متن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (turn.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "نشان کردن",
                            tint = if (turn.isBookmarked) SpeakerAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = turn.originalText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = if (turn.sourceLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = turn.translatedText,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDirection = if (turn.targetLanguage.isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            if (turn.phoneticGuide.isNotBlank()) {
                Text(
                    text = "تلفظ: ${turn.phoneticGuide}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.copy(alpha = 0.9f)
                )
            }

            if (!turn.culturalNote.isNullOrBlank()) {
                Text(
                    text = "💡 ${turn.culturalNote}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
