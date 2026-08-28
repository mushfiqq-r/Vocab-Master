package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.data.model.WordWithReview
import com.example.ui.components.BadgeChip
import com.example.ui.components.BackupRestoreSection
import com.example.ui.components.LearningStreakTracker
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab
import kotlin.math.cos
import kotlin.math.sin

enum class ChartFilterCategory {
    ALL, MASTERED, LEARNING, NEW
}

@Composable
fun ProgressScreen(
    viewModel: MainViewModel,
    onWordClick: (WordEntity) -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    val allWords by viewModel.wordsWithReviews.collectAsState()
    val wordOfTheDay by viewModel.wordOfTheDay.collectAsState()
    val quizState by viewModel.quizState.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ChartFilterCategory.ALL) }

    val masteredList = remember(allWords) { allWords.filter { it.review?.status == "mastered" } }
    val learningList = remember(allWords) { allWords.filter { it.review?.status == "learning" } }
    val newList = remember(allWords) { allWords.filter { it.review?.status == "new" || it.review == null } }

    val masteredCount = masteredList.size
    val learningCount = learningList.size
    val newCount = newList.size
    val totalCount = allWords.size.coerceAtLeast(1)

    // Book breakdowns
    val greWords = remember(allWords) { allWords.filter { it.word.bookIds.contains("gre_333") } }
    val ws1Words = remember(allWords) { allWords.filter { it.word.bookIds.contains("ws1") } }
    val ws2Words = remember(allWords) { allWords.filter { it.word.bookIds.contains("ws2") } }

    // Tier breakdowns
    val tier1Words = remember(allWords) { allWords.filter { it.word.difficultyTier == 1 } }
    val tier2Words = remember(allWords) { allWords.filter { it.word.difficultyTier == 2 } }
    val tier3Words = remember(allWords) { allWords.filter { it.word.difficultyTier == 3 } }

    val activeDrilldownWords = when (selectedCategory) {
        ChartFilterCategory.ALL -> emptyList()
        ChartFilterCategory.MASTERED -> masteredList
        ChartFilterCategory.LEARNING -> learningList
        ChartFilterCategory.NEW -> newList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .testTag("progress_screen_container"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP GAMIFICATION & SCHOLAR STATUS HEADER
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${stats?.xpTotal ?: 0} Total XP Earned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Surface(
                        color = AmberAccent,
                        shape = CircleShape,
                        modifier = Modifier
                            .clickable { viewModel.openStreakDetailDialog() }
                            .testTag("progress_header_streak_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFF5D4037),
                                modifier = Modifier.size(20.dp)
                            )
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
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${100 - (currentXp % 100)} XP to Level ${(stats?.level ?: 1) + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Daily Goal: ${stats?.dailyGoal ?: 15} words",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.clickable { showGoalDialog = true }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. LEARNING STREAK TRACKER (GAMIFIED CALENDAR & MILESTONES)
        // -------------------------------------------------------------
        LearningStreakTracker(
            viewModel = viewModel,
            onStartStudyClick = { viewModel.setTab(ScreenTab.Study) }
        )

        // -------------------------------------------------------------
        // 2. D3 / RECHARTS STYLE INTERACTIVE DONUT CHART
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DonutLarge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Learning Progress Visualizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Interactive retention distribution",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (selectedCategory != ChartFilterCategory.ALL) {
                        TextButton(
                            onClick = { selectedCategory = ChartFilterCategory.ALL },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Reset Focus", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Interactive Donut Chart Visualizer Component
                D3StyleDonutVisualizer(
                    mastered = masteredCount,
                    learning = learningCount,
                    newWords = newCount,
                    total = totalCount,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { cat ->
                        selectedCategory = if (selectedCategory == cat) ChartFilterCategory.ALL else cat
                    }
                )

                // Interactive Filter Buttons / Legend Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InteractiveLegendPill(
                        modifier = Modifier.weight(1f),
                        title = "Mastered",
                        count = masteredCount,
                        percent = (masteredCount * 100) / totalCount,
                        color = StatusMastered,
                        isSelected = selectedCategory == ChartFilterCategory.MASTERED,
                        onClick = {
                            selectedCategory = if (selectedCategory == ChartFilterCategory.MASTERED) ChartFilterCategory.ALL else ChartFilterCategory.MASTERED
                        }
                    )

                    InteractiveLegendPill(
                        modifier = Modifier.weight(1f),
                        title = "Learning",
                        count = learningCount,
                        percent = (learningCount * 100) / totalCount,
                        color = StatusLearning,
                        isSelected = selectedCategory == ChartFilterCategory.LEARNING,
                        onClick = {
                            selectedCategory = if (selectedCategory == ChartFilterCategory.LEARNING) ChartFilterCategory.ALL else ChartFilterCategory.LEARNING
                        }
                    )

                    InteractiveLegendPill(
                        modifier = Modifier.weight(1f),
                        title = "New",
                        count = newCount,
                        percent = (newCount * 100) / totalCount,
                        color = StatusNew,
                        isSelected = selectedCategory == ChartFilterCategory.NEW,
                        onClick = {
                            selectedCategory = if (selectedCategory == ChartFilterCategory.NEW) ChartFilterCategory.ALL else ChartFilterCategory.NEW
                        }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 3. DRILL-DOWN EXPANDED WORD EXPLORER (When a category is active)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = selectedCategory != ChartFilterCategory.ALL,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() }} Words (${activeDrilldownWords.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedCategory) {
                                ChartFilterCategory.MASTERED -> StatusMastered
                                ChartFilterCategory.LEARNING -> StatusLearning
                                ChartFilterCategory.NEW -> StatusNew
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "Tap to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Display list of chips or cards
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeDrilldownWords.take(25)) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { onWordClick(item.word) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = item.word.term,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.word.banglaMeaning,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. STACKED RECHARTS-STYLE BAR CHART (CURRICULUM BREAKDOWN)
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Curriculum Stacked Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Stacked progress across Barron's & Word Smart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stacked Bar for Barron's GRE 333
                StackedBookProgressBar(
                    title = "Barron's GRE 333",
                    badgeColor = BookGreColor,
                    words = greWords
                )

                // Stacked Bar for Word Smart 1
                StackedBookProgressBar(
                    title = "Word Smart 1 Core",
                    badgeColor = BookWs1Color,
                    words = ws1Words
                )

                // Stacked Bar for Word Smart 2
                StackedBookProgressBar(
                    title = "Word Smart 2 Advanced",
                    badgeColor = BookWs2Color,
                    words = ws2Words
                )
            }
        }

        // -------------------------------------------------------------
        // 5. DIFFICULTY TIER MASTERY MATRIX (TIER 1, 2, 3)
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Difficulty Tier Mastery Matrix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Retention rates categorized by complexity tier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TierMetricCard(
                        modifier = Modifier.weight(1f),
                        tierName = "Tier 1: Foundation",
                        words = tier1Words,
                        color = Color(0xFF1976D2)
                    )
                    TierMetricCard(
                        modifier = Modifier.weight(1f),
                        tierName = "Tier 2: Advanced",
                        words = tier2Words,
                        color = Color(0xFF7B1FA2)
                    )
                    TierMetricCard(
                        modifier = Modifier.weight(1f),
                        tierName = "Tier 3: Nuanced",
                        words = tier3Words,
                        color = Color(0xFFC2185B)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 6. WORD OF THE DAY SPOTLIGHT
        // -------------------------------------------------------------
        if (wordOfTheDay != null) {
            val wotd = wordOfTheDay!!
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = CardDefaults.outlinedCardBorder(),
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
                            .background(AmberAccent.copy(alpha = 0.25f)),
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
                            text = "Daily Word Spotlight",
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
        }

        // -------------------------------------------------------------
        // 7. SM-2 DATABASE BACKUP & RESTORE SECTION
        // -------------------------------------------------------------
        BackupRestoreSection(viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Daily Goal Dialog
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

// -------------------------------------------------------------
// D3 / RECHARTS STYLE ANIMATED DONUT CHART COMPOSABLE
// -------------------------------------------------------------
@Composable
fun D3StyleDonutVisualizer(
    mastered: Int,
    learning: Int,
    newWords: Int,
    total: Int,
    selectedCategory: ChartFilterCategory,
    onSelectCategory: (ChartFilterCategory) -> Unit
) {
    val masteredRatio = (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val learningRatio = (learning.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val newRatio = (newWords.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(mastered, learning, newWords) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Calculate angle from center to detect clicked segment
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f

                        // Shift starting angle from -90 deg (top)
                        val normalizedAngle = (angle + 90f) % 360f

                        val masteredAngle = masteredRatio * 360f
                        val learningAngle = learningRatio * 360f

                        when {
                            normalizedAngle < masteredAngle -> onSelectCategory(ChartFilterCategory.MASTERED)
                            normalizedAngle < (masteredAngle + learningAngle) -> onSelectCategory(ChartFilterCategory.LEARNING)
                            else -> onSelectCategory(ChartFilterCategory.NEW)
                        }
                    }
                }
        ) {
            val strokeWidth = 26.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            var startAngle = -90f

            // Sweep angles scaled by animation
            val currentScale = animProgress.value
            val masteredSweep = masteredRatio * 360f * currentScale
            val learningSweep = learningRatio * 360f * currentScale
            val newSweep = newRatio * 360f * currentScale

            // Draw Mastered Arc
            if (masteredSweep > 0) {
                val alpha = if (selectedCategory == ChartFilterCategory.ALL || selectedCategory == ChartFilterCategory.MASTERED) 1f else 0.25f
                drawArc(
                    color = StatusMastered.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = masteredSweep - if (total > 1) 2f else 0f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += masteredRatio * 360f * currentScale
            }

            // Draw Learning Arc
            if (learningSweep > 0) {
                val alpha = if (selectedCategory == ChartFilterCategory.ALL || selectedCategory == ChartFilterCategory.LEARNING) 1f else 0.25f
                drawArc(
                    color = StatusLearning.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = learningSweep - if (total > 1) 2f else 0f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += learningRatio * 360f * currentScale
            }

            // Draw New Arc
            if (newSweep > 0) {
                val alpha = if (selectedCategory == ChartFilterCategory.ALL || selectedCategory == ChartFilterCategory.NEW) 1f else 0.25f
                drawArc(
                    color = StatusNew.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = newSweep - if (total > 1) 2f else 0f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content Displays Category Metrics
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val centerCount = when (selectedCategory) {
                ChartFilterCategory.ALL -> total
                ChartFilterCategory.MASTERED -> mastered
                ChartFilterCategory.LEARNING -> learning
                ChartFilterCategory.NEW -> newWords
            }
            val centerLabel = when (selectedCategory) {
                ChartFilterCategory.ALL -> "TOTAL WORDS"
                ChartFilterCategory.MASTERED -> "MASTERED"
                ChartFilterCategory.LEARNING -> "LEARNING"
                ChartFilterCategory.NEW -> "NEW"
            }
            val centerColor = when (selectedCategory) {
                ChartFilterCategory.ALL -> MaterialTheme.colorScheme.onSurface
                ChartFilterCategory.MASTERED -> StatusMastered
                ChartFilterCategory.LEARNING -> StatusLearning
                ChartFilterCategory.NEW -> StatusNew
            }

            Text(
                text = "$centerCount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = centerColor
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            if (selectedCategory != ChartFilterCategory.ALL) {
                val percent = (centerCount * 100) / total
                Text(
                    text = "$percent% of total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INTERACTIVE LEGEND PILL COMPONENT
// -------------------------------------------------------------
@Composable
fun InteractiveLegendPill(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    percent: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.18f) else color.copy(alpha = 0.08f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// STACKED HORIZONTAL BAR CHART COMPONENT (RECHARTS STYLE)
// -------------------------------------------------------------
@Composable
fun StackedBookProgressBar(
    title: String,
    badgeColor: Color,
    words: List<WordWithReview>
) {
    val total = words.size.coerceAtLeast(1)
    val mastered = words.count { it.review?.status == "mastered" }
    val learning = words.count { it.review?.status == "learning" }
    val newCount = words.count { it.review?.status == "new" || it.review == null }

    val masteredRatio = (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val learningRatio = (learning.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val newRatio = (newCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Text(
                text = "$mastered mastered • $total total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stacked Horizontal Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (masteredRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(masteredRatio.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(StatusMastered)
                )
            }
            if (learningRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(learningRatio.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(StatusLearning)
                )
            }
            if (newRatio > 0) {
                Box(
                    modifier = Modifier
                        .weight(newRatio.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(StatusNew.copy(alpha = 0.5f))
                )
            }
        }

        // Breakdown percentages sub-row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(masteredRatio * 100).toInt()}% Mastered",
                style = MaterialTheme.typography.labelSmall,
                color = StatusMastered,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(learningRatio * 100).toInt()}% Learning",
                style = MaterialTheme.typography.labelSmall,
                color = StatusLearning,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(newRatio * 100).toInt()}% Unexplored",
                style = MaterialTheme.typography.labelSmall,
                color = StatusNew,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// -------------------------------------------------------------
// DIFFICULTY TIER METRIC CARD
// -------------------------------------------------------------
@Composable
fun TierMetricCard(
    modifier: Modifier = Modifier,
    tierName: String,
    words: List<WordWithReview>,
    color: Color
) {
    val total = words.size.coerceAtLeast(1)
    val mastered = words.count { it.review?.status == "mastered" }
    val progress = (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tierName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )

            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            Text(
                text = "$mastered / $total words",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
