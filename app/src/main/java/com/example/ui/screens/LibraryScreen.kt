package com.example.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.data.model.WordWithReview
import com.example.ui.components.BadgeChip
import com.example.ui.components.highlightMatch
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onWordClick: (WordEntity) -> Unit
) {
    val wordsList by viewModel.filteredLibraryWords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBook by viewModel.selectedBookFilter.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val selectedTier by viewModel.selectedTierFilter.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        ttsEngine.language = Locale.US
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = { Text("Search words or meanings...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                        modifier = Modifier.testTag("library_search_clear_button")
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Book Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedBook == "ALL",
                onClick = { viewModel.setBookFilter("ALL") },
                label = { Text("All Books") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedBook == "gre_333",
                onClick = { viewModel.setBookFilter(if (selectedBook == "gre_333") "ALL" else "gre_333") },
                label = { Text("Barron's GRE 333") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedBook == "ws1",
                onClick = { viewModel.setBookFilter(if (selectedBook == "ws1") "ALL" else "ws1") },
                label = { Text("Word Smart 1") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedBook == "ws2",
                onClick = { viewModel.setBookFilter(if (selectedBook == "ws2") "ALL" else "ws2") },
                label = { Text("Word Smart 2") },
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status & Tier Filter Chips Row with Sort dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Filters
            FilterChip(
                selected = selectedStatus == "ALL",
                onClick = { viewModel.setStatusFilter("ALL") },
                label = { Text("All Status") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedStatus == "mastered",
                onClick = { viewModel.setStatusFilter(if (selectedStatus == "mastered") "ALL" else "mastered") },
                label = { Text("Mastered") },
                shape = RoundedCornerShape(8.dp),
                leadingIcon = if (selectedStatus == "mastered") {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null
            )
            FilterChip(
                selected = selectedStatus == "learning",
                onClick = { viewModel.setStatusFilter(if (selectedStatus == "learning") "ALL" else "learning") },
                label = { Text("Learning") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedStatus == "new",
                onClick = { viewModel.setStatusFilter(if (selectedStatus == "new") "ALL" else "new") },
                label = { Text("New") },
                shape = RoundedCornerShape(8.dp)
            )

            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

            // Tier Filters
            FilterChip(
                selected = selectedTier == 1,
                onClick = { viewModel.setTierFilter(if (selectedTier == 1) 0 else 1) },
                label = { Text("Core Tier 1") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedTier == 2,
                onClick = { viewModel.setTierFilter(if (selectedTier == 2) 0 else 2) },
                label = { Text("Moderate Tier 2") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedTier == 3,
                onClick = { viewModel.setTierFilter(if (selectedTier == 3) 0 else 3) },
                label = { Text("Advanced Tier 3") },
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Word count & Sort Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${wordsList.size} Word${if (wordsList.size == 1) "" else "s"} Listed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (selectedSortOrder) {
                                "Z_TO_A" -> "Sort: Z-A"
                                "DIFFICULTY" -> "Sort: Difficulty"
                                "MASTERY" -> "Sort: Mastery"
                                else -> "Sort: A-Z"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Alphabetical (A to Z)") },
                        onClick = {
                            viewModel.setSortOrder("A_TO_Z")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Reverse (Z to A)") },
                        onClick = {
                            viewModel.setSortOrder("Z_TO_A")
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Difficulty Tier (1 to 3)") },
                        onClick = {
                            viewModel.setSortOrder("DIFFICULTY")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mastery Level") },
                        onClick = {
                            viewModel.setSortOrder("MASTERY")
                            showSortMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (wordsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No words",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No words found matching your search filters",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try adjusting your search terms or clearing active book/status/tier filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            viewModel.setSearchQuery("")
                            viewModel.setBookFilter("ALL")
                            viewModel.setStatusFilter("ALL")
                            viewModel.setTierFilter(0)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset All Search Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_words_list")
            ) {
                items(wordsList, key = { it.word.id }) { item ->
                    WordListItemCard(
                        item = item,
                        searchQuery = searchQuery,
                        onPlayTts = { tts?.speak(item.word.term, TextToSpeech.QUEUE_FLUSH, null, null) },
                        onClick = { onWordClick(item.word) }
                    )
                }
            }
        }
    }
}

@Composable
fun WordListItemCard(
    item: WordWithReview,
    searchQuery: String = "",
    onPlayTts: () -> Unit = {},
    onClick: () -> Unit
) {
    val word = item.word
    val status = item.review?.status ?: "new"

    val statusColor = when (status) {
        "mastered" -> StatusMastered
        "learning" -> StatusLearning
        else -> StatusNew
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("word_item_${word.term.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = highlightMatch(word.term, searchQuery, MaterialTheme.colorScheme.primary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${word.partOfSpeech})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onPlayTts,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }

                    val bookBadge = when {
                        word.bookIds.contains("gre_333") -> "GRE 333"
                        word.bookIds.contains("ws1") -> "WS 1"
                        else -> "WS 2"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = bookBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Text(
                text = highlightMatch(word.banglaMeaning, searchQuery, MaterialTheme.colorScheme.secondary),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = highlightMatch(word.primaryMeaning, searchQuery, MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Nuance Preview Snippet Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Nuance: ${word.preciseMeaning.take(65)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "View details →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

