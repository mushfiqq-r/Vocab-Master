package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.data.util.SM2Algorithm
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(
    viewModel: MainViewModel,
    onViewDetail: (WordEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allWordsWithReviews by viewModel.wordsWithReviews.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBookFilter by remember { mutableStateOf("ALL") } // "ALL", "gre_333", "ws1", "ws2"
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS ready
            }
        }
        ttsEngine.language = Locale.US
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    // Filter words according to search query and selected book
    val filteredWords = remember(allWordsWithReviews, searchQuery, selectedBookFilter) {
        allWordsWithReviews.map { it.word }.filter { w ->
            val matchesBook = when (selectedBookFilter) {
                "gre_333" -> w.bookIds.contains("gre_333")
                "ws1" -> w.bookIds.contains("ws1")
                "ws2" -> w.bookIds.contains("ws2")
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    w.term.contains(searchQuery, ignoreCase = true) ||
                    w.banglaMeaning.contains(searchQuery, ignoreCase = true) ||
                    w.preciseMeaning.contains(searchQuery, ignoreCase = true)
            matchesBook && matchesQuery
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { if (filteredWords.isEmpty()) 1 else filteredWords.size }
    )

    val chipListState = rememberLazyListState()

    // Scroll chip row to keep active item in view
    LaunchedEffect(pagerState.currentPage) {
        if (filteredWords.isNotEmpty() && pagerState.currentPage < filteredWords.size) {
            chipListState.animateScrollToItem(pagerState.currentPage.coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("tutor_screen_container")
    ) {
        // Tutor Banner Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Word Tutor",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Expert Vocabulary Tutor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Swipe left/right to browse words with Bangla gloss & nuance shifts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Book Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedBookFilter == "ALL",
                onClick = {
                    selectedBookFilter = "ALL"
                    coroutineScope.launch { pagerState.scrollToPage(0) }
                },
                label = { Text("All Books") }
            )
            FilterChip(
                selected = selectedBookFilter == "gre_333",
                onClick = {
                    selectedBookFilter = "gre_333"
                    coroutineScope.launch { pagerState.scrollToPage(0) }
                },
                label = { Text("Barron's GRE 333") }
            )
            FilterChip(
                selected = selectedBookFilter == "ws1",
                onClick = {
                    selectedBookFilter = "ws1"
                    coroutineScope.launch { pagerState.scrollToPage(0) }
                },
                label = { Text("Word Smart 1") }
            )
            FilterChip(
                selected = selectedBookFilter == "ws2",
                onClick = {
                    selectedBookFilter = "ws2"
                    coroutineScope.launch { pagerState.scrollToPage(0) }
                },
                label = { Text("Word Smart 2") }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Word Carousel Chips
        if (filteredWords.isNotEmpty()) {
            LazyRow(
                state = chipListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filteredWords) { idx, word ->
                    val isSelected = pagerState.currentPage == idx
                    SuggestionChip(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(idx)
                            }
                        },
                        label = {
                            Text(
                                text = word.term,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Swipe Navigation Controls (Prev, Page Indicator, Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("tutor_prev_button")
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous")
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Prev", style = MaterialTheme.typography.labelSmall)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Word ${pagerState.currentPage + 1} of ${filteredWords.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "← Swipe Left / Right →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }

                FilledTonalButton(
                    onClick = {
                        if (pagerState.currentPage < filteredWords.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < filteredWords.size - 1,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("tutor_next_button")
                ) {
                    Text("Next", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No words matching criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Horizontal Pager for Left-Right Swiping between Words
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tutor_word_pager")
            ) { page ->
                val word = filteredWords.getOrNull(page)
                if (word != null) {
                    TutorWordDetailCard(
                        word = word,
                        tts = tts,
                        onViewDetail = { onViewDetail(word) }
                    )
                }
            }
        }
    }
}

@Composable
fun TutorWordDetailCard(
    word: WordEntity,
    tts: TextToSpeech?,
    onViewDetail: () -> Unit
) {
    val synonymShifts = remember(word.synonymShiftsJson) {
        SM2Algorithm.parseSynonymShifts(word.synonymShiftsJson)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("tutor_breakdown_card_${word.term}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Part 1: [Word] (part of speech) — followed by its precise Bangla meaning in parentheses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = word.term,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { tts?.speak(word.term, TextToSpeech.QUEUE_FLUSH, null, null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak word",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "(${word.partOfSpeech}) — (${word.banglaMeaning})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (word.bookIds.contains("gre_333")) "GRE 333" else "Word Smart",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Part 2: Sentence
            Text(
                text = "Sentence:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“${word.exampleSentence}”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Part 3: Precise meaning
            Text(
                text = "Precise meaning:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = word.preciseMeaning,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Part 4: How synonyms shift the meaning (Exactly 4 bullet points)
            Text(
                text = "How synonyms shift the meaning:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            synonymShifts.forEachIndexed { idx, shift ->
                val isTrap = shift.isTrapWord || idx == 3
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTrap) StatusDanger.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.dp,
                            if (isTrap) StatusDanger.copy(alpha = 0.35f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTrap) StatusDanger.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTrap) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Trap Word",
                                    tint = StatusDanger,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = shift.word,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTrap) StatusDanger else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = shift.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Memory Retention Anchor Hook
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AmberAccent.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Memory Hook",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = word.memoryHook,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inspect Detail Action
            OutlinedButton(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Word Study Card")
            }
        }
    }
}
