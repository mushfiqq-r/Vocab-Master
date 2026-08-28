package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.ui.components.BadgeChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProgressScreen(
    viewModel: MainViewModel,
    onWordClick: (WordEntity) -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    val allWords by viewModel.wordsWithReviews.collectAsState()
    val wordOfTheDay by viewModel.wordOfTheDay.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }

    val masteredCount = allWords.count { it.review?.status == "mastered" }
    val learningCount = allWords.count { it.review?.status == "learning" }
    val newCount = allWords.count { it.review?.status == "new" || it.review == null }
    val totalCount = allWords.size.coerceAtLeast(1)

    // Book breakdowns
    val greWords = allWords.filter { it.word.bookIds.contains("gre_333") }
    val ws1Words = allWords.filter { it.word.bookIds.contains("ws1") }
    val ws2Words = allWords.filter { it.word.bookIds.contains("ws2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Gamification Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Level ${stats?.level ?: 1} Scholar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${stats?.xpTotal ?: 0} Total XP Earned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        color = AmberAccent,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFF5D4037),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stats?.currentStreak ?: 1}d Streak",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XP to next level progress bar
                val currentXp = stats?.xpTotal ?: 0
                val progressInLevel = (currentXp % 100).toFloat() / 100f
                LinearProgressIndicator(
                    progress = { progressInLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AmberAccent,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${100 - (currentXp % 100)} XP to Level ${(stats?.level ?: 1) + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Word of the Day Spotlight
        if (wordOfTheDay != null) {
            val wotd = wordOfTheDay!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWordClick(wotd) }
                    .testTag("word_of_the_day_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Word of the day",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Word of the Day",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = "${wotd.term} (${wotd.partOfSpeech})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = wotd.banglaMeaning,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overall Memory Retention Stats
        Text(
            text = "Spaced Repetition Retention",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RetentionCard(
                modifier = Modifier.weight(1f),
                title = "Mastered",
                count = masteredCount,
                percent = (masteredCount * 100) / totalCount,
                color = StatusMastered
            )
            RetentionCard(
                modifier = Modifier.weight(1f),
                title = "Learning",
                count = learningCount,
                percent = (learningCount * 100) / totalCount,
                color = StatusLearning
            )
            RetentionCard(
                modifier = Modifier.weight(1f),
                title = "New",
                count = newCount,
                percent = (newCount * 100) / totalCount,
                color = StatusNew
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Book Curriculum Progress
        Text(
            text = "Curriculum Completion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        BookProgressItem(
            title = "Barron's GRE 333 High-Frequency",
            words = greWords,
            color = BookGreColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        BookProgressItem(
            title = "Word Smart 1 Core Vocabulary",
            words = ws1Words,
            color = BookWs1Color
        )
        Spacer(modifier = Modifier.height(8.dp))
        BookProgressItem(
            title = "Word Smart 2 Advanced Vocabulary",
            words = ws2Words,
            color = BookWs2Color
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showGoalDialog) {
        val currentGoal = stats?.dailyGoal ?: 15
        var tempGoal by remember { mutableStateOf(currentGoal.toString()) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Review Goal") },
            text = {
                OutlinedTextField(
                    value = tempGoal,
                    onValueChange = { tempGoal = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Words per day") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val g = tempGoal.toIntOrNull() ?: 15
                    viewModel.updateDailyGoal(g.coerceIn(5, 100))
                    showGoalDialog = false
                }) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RetentionCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    percent: Int,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BookProgressItem(
    title: String,
    words: List<com.example.data.model.WordWithReview>,
    color: Color
) {
    val total = words.size.coerceAtLeast(1)
    val mastered = words.count { it.review?.status == "mastered" }
    val progress = (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percentInt = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "$mastered / ${words.size} mastered ($percentInt%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
