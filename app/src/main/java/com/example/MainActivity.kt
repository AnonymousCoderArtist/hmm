package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.editor.AppScreen
import com.example.ui.editor.ReelEditorScreen
import com.example.ui.editor.ReelEditorViewModel
import com.example.ui.projects.RecentProjectsScreen
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.PushUpReelTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ReelEditorViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val recentProjects by viewModel.recentProjects.collectAsStateWithLifecycle()

            // System back button handler
            BackHandler(enabled = currentScreen == AppScreen.RECENT_PROJECTS) {
                viewModel.setScreen(AppScreen.EDITOR)
            }

            PushUpReelTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.EDITOR -> {
                            ReelEditorScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.RECENT_PROJECTS -> {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = "Recent Reels & Workouts",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(
                                                onClick = { viewModel.setScreen(AppScreen.EDITOR) },
                                                modifier = Modifier.testTag("back_to_editor_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back to Editor"
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = { viewModel.toggleTheme() },
                                                modifier = Modifier.testTag("theme_toggle_projects")
                                            ) {
                                                Icon(
                                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                    contentDescription = "Toggle Theme",
                                                    tint = AthleticGold
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                RecentProjectsScreen(
                                    projects = recentProjects,
                                    onOpenProject = { id -> viewModel.loadProject(id) },
                                    onCreateNewProject = { viewModel.createNewProject() },
                                    onDeleteProject = { id -> viewModel.deleteProject(id) },
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
