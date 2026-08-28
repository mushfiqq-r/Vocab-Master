package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.WordDetailBottomSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.TutorScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.VocabMasterTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VocabMasterTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val selectedWord by viewModel.selectedWordDetail.collectAsState()
                val stats by viewModel.userStats.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = "App Logo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "VocabMaster",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            actions = {
                                // Streak Badge in Top Bar
                                Surface(
                                    color = AmberAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${stats?.currentStreak ?: 1}d",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.Study,
                                onClick = { viewModel.setTab(ScreenTab.Study) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Style,
                                        contentDescription = "Study Flashcards"
                                    )
                                },
                                label = { Text("Study") },
                                modifier = Modifier.testTag("nav_study")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.Library,
                                onClick = { viewModel.setTab(ScreenTab.Library) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CollectionsBookmark,
                                        contentDescription = "Word Library"
                                    )
                                },
                                label = { Text("Library") },
                                modifier = Modifier.testTag("nav_library")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.Tutor,
                                onClick = { viewModel.setTab(ScreenTab.Tutor) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "AI Word Tutor"
                                    )
                                },
                                label = { Text("Tutor") },
                                modifier = Modifier.testTag("nav_tutor")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.Progress,
                                onClick = { viewModel.setTab(ScreenTab.Progress) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Leaderboard,
                                        contentDescription = "Progress & Stats"
                                    )
                                },
                                label = { Text("Progress") },
                                modifier = Modifier.testTag("nav_progress")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "TabTransition"
                        ) { tab ->
                            when (tab) {
                                ScreenTab.Study -> StudyScreen(
                                    viewModel = viewModel,
                                    onViewDetail = { viewModel.selectWordDetail(it) }
                                )
                                ScreenTab.Library -> LibraryScreen(
                                    viewModel = viewModel,
                                    onWordClick = { viewModel.selectWordDetail(it) }
                                )
                                ScreenTab.Tutor -> TutorScreen(
                                    viewModel = viewModel,
                                    onViewDetail = { viewModel.selectWordDetail(it) }
                                )
                                ScreenTab.Progress -> ProgressScreen(
                                    viewModel = viewModel,
                                    onWordClick = { viewModel.selectWordDetail(it) }
                                )
                            }
                        }

                        // Word Detail BottomSheet Modal
                        WordDetailBottomSheet(
                            word = selectedWord,
                            onDismiss = { viewModel.selectWordDetail(null) }
                        )
                    }
                }
            }
        }
    }
}
