package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyActivityEntity
import com.example.data.model.StreakMilestone
import com.example.data.model.UserStatsEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LearningStreakTracker(
    viewModel: MainViewModel,
    onStartStudyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.userStats.collectAsState()
    val recentActivity by viewModel.recentDailyActivity.collectAsState()
    val currentStreak = stats?.currentStreak ?: 0
    val longestStreak = stats?.longestStreak ?: 0
    val dailyGoal = stats?.dailyGoal ?: 15

    // Get today's activity
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { sdf.format(Date()) }
    val todayActivity = remember(recentActivity) {
        recentActivity.firstOrNull { it.dateString == todayStr }
    }
    val wordsReviewedToday = todayActivity?.wordsReviewed ?: 0
    val isGoalMetToday = wordsReviewedToday >= dailyGoal

    // Find current & next milestone
    val milestones = viewModel.streakMilestones
    val nextMilestone = milestones.firstOrNull { it.daysRequired > currentStreak }
    val currentUnlockedMilestones = milestones.filter { it.daysRequired <= currentStreak }

    // Pulsing animation for active streak flame
    val infiniteTransition = rememberInfiniteTransition(label = "streak_flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    // Streak flame color gradient based on tier
    val flameGradient = when {
        currentStreak >= 30 -> listOf(Color(0xFFFF8008), Color(0xFFFFC837), Color(0xFFFFD700))
        currentStreak >= 14 -> listOf(Color(0xFFE52D27), Color(0xFFB31217), Color(0xFFFF5722))
        currentStreak >= 7 -> listOf(Color(0xFFFF9900), Color(0xFFFF5E3A), Color(0xFFFF8C00))
        else -> listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFB74D))
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("learning_streak_tracker_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Section: Flame Header & Streak Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated Flame Container
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        flameGradient[0].copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = flameGradient[0].copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(44.dp)
                                .scale(if (currentStreak > 0) flameScale else 1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Learning Streak Flame",
                                    tint = flameGradient[0],
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$currentStreak Day${if (currentStreak == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isGoalMetToday) StatusMastered.copy(alpha = 0.15f) else AmberAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isGoalMetToday) "ACTIVE TODAY" else "IN PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoalMetToday) StatusMastered else Color(0xFFD84315),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isGoalMetToday) "Awesome! Daily review goal completed" else "$wordsReviewedToday of $dailyGoal words reviewed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Details Button (opens interactive modal)
                FilledTonalIconButton(
                    onClick = { viewModel.openStreakDetailDialog() },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("streak_info_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Streak Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 7-Day Weekly Interactive Calendar Row
            WeeklyStreakCalendar(
                recentActivity = recentActivity,
                dailyGoal = dailyGoal
            )

            // Today's Goal Progress Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = if (isGoalMetToday) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isGoalMetToday) StatusMastered else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Daily SM-2 Goal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "$wordsReviewedToday / $dailyGoal words",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isGoalMetToday) StatusMastered else MaterialTheme.colorScheme.primary
                        )
                    }

                    val progress = (wordsReviewedToday.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isGoalMetToday) StatusMastered else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (!isGoalMetToday) {
                        val remaining = (dailyGoal - wordsReviewedToday).coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review $remaining more to keep streak alive",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Review Now →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onStartStudyClick() }
                            )
                        }
                    }
                }
            }

            // Next Milestone Teaser
            if (nextMilestone != null) {
                val daysLeft = nextMilestone.daysRequired - currentStreak
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Next: ${nextMilestone.title} (${nextMilestone.badgeName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$daysLeft day${if (daysLeft == 1) "" else "s"} away • +${nextMilestone.bonusXp} Bonus XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Longest streak badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Best",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${longestStreak}d",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyStreakCalendar(
    recentActivity: List<DailyActivityEntity>,
    dailyGoal: Int,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.US) }

    val days = remember(recentActivity) {
        val list = mutableListOf<CalendarDayInfo>()
        val activityMap = recentActivity.associateBy { it.dateString }

        // Last 7 days ending today
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = sdf.format(cal.time)
            val activity = activityMap[dateStr]
            val reviewed = activity?.wordsReviewed ?: 0
            val isToday = (i == 0)
            val isCompleted = activity?.goalMet == true || reviewed >= dailyGoal

            list.add(
                CalendarDayInfo(
                    dateString = dateStr,
                    dayInitial = dayNameFormat.format(cal.time).take(1),
                    dayNumber = dayNumFormat.format(cal.time),
                    isToday = isToday,
                    isCompleted = isCompleted,
                    wordsReviewed = reviewed
                )
            )
        }
        list
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = day.dayInitial,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Day Node Circle
                Surface(
                    shape = CircleShape,
                    color = when {
                        day.isCompleted -> StatusMastered
                        day.isToday -> MaterialTheme.colorScheme.primaryContainer
                        day.wordsReviewed > 0 -> StatusLearning.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = when {
                        day.isToday && !day.isCompleted -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        day.isCompleted -> BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                        else -> null
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            day.isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            day.isToday -> {
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            day.wordsReviewed > 0 -> {
                                Text(
                                    text = "${day.wordsReviewed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            else -> {
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDayInfo(
    val dateString: String,
    val dayInitial: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isCompleted: Boolean,
    val wordsReviewed: Int
)
