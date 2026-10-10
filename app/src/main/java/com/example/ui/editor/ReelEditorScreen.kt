package com.example.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import com.example.ui.components.InstallAppDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.camera.LiveCameraScreen
import com.example.ui.components.CounterBadgeControls
import com.example.ui.components.ExportProgressDialog
import com.example.ui.components.OverlayTextControls
import com.example.ui.components.QuoteAndStyleControls
import com.example.ui.components.ReelPreviewSurface
import com.example.ui.components.RepTimelineEditor
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelEditorScreen(
    viewModel: ReelEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayConfig by viewModel.overlayConfig.collectAsStateWithLifecycle()
    val videoMedia by viewModel.videoMedia.collectAsStateWithLifecycle()
    val detectedReps by viewModel.detectedReps.collectAsStateWithLifecycle()
    val cvResult by viewModel.cvAnalysisResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisProgress by viewModel.analysisProgress.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val poseMode by viewModel.poseMode.collectAsStateWithLifecycle()
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val isAutoSaved by viewModel.isAutoSaved.collectAsStateWithLifecycle()
    val showInstallHelpDialog by viewModel.showInstallHelpDialog.collectAsStateWithLifecycle()

    var showCameraRecording by remember { mutableStateOf(false) }

    // Visual media picker (zero storage permissions required)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoPicked(uri, "Selected Workout Video")
        }
    }

    if (showCameraRecording) {
        LiveCameraScreen(
            onVideoRecorded = { file, durationMs ->
                viewModel.onVideoRecorded(file, durationMs)
                showCameraRecording = false
            },
            onClose = { showCameraRecording = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PushUp Reel",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            // Auto-save badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isAutoSaved) AthleticGreen else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = if (isAutoSaved) "Saved" else "Saving...",
                                    fontSize = 10.sp,
                                    color = if (isAutoSaved) AthleticGreen else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "Made by Lokesh Artist Coder for Naren keep pushing!",
                            style = MaterialTheme.typography.labelSmall,
                            color = AthleticGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Recent Projects button
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.RECENT_PROJECTS) },
                        modifier = Modifier.testTag("projects_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Recent Projects",
                            tint = AthleticOrange
                        )
                    }

                    // Install App / Help button
                    IconButton(
                        onClick = { viewModel.setShowInstallHelpDialog(true) },
                        modifier = Modifier.testTag("install_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Install App on Phone",
                            tint = AthleticGold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
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
        bottomBar = {
            // Dedicated Export Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Day ${overlayConfig.dayNumber} • ${detectedReps.size} Push-ups",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "9:16 Vertical Reel Format",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.startExport() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AthleticOrange,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("export_reel_button")
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = "Export")
                        Spacer(Modifier.width(6.dp))
                        Text("Export Reel", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dedicated Attribution Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1810))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Made by Lokesh Artist Coder for Naren keep pushing! 💪🔥",
                        color = AthleticGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Quick Video Source Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("pick_video_button")
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "Pick Video", tint = AthleticOrange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pick Video", fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showCameraRecording = true },
                    modifier = Modifier.weight(1f).testTag("record_camera_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Record Camera", tint = AthleticGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Record Live", fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSampleWorkout(autoAnalyze = true) },
                    modifier = Modifier.weight(1f).testTag("load_sample_button")
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = "Sample", tint = AthleticGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Demo Reel", fontSize = 12.sp, maxLines = 1)
                }
            }

            // 9:16 Vertical Reel Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ReelPreviewSurface(
                    videoMedia = videoMedia,
                    overlayConfig = overlayConfig,
                    reps = detectedReps,
                    onCurrentTimeChanged = { pos -> viewModel.setPlaybackTime(pos) },
                    onOverlayConfigChanged = { transform -> viewModel.updateOverlayConfig(transform) },
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                )
            }

            // Tabs for customization
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AthleticOrange
            ) {
                EditorTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setSelectedTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name}")
                    )
                }
            }

            // Customization Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when (selectedTab) {
                    EditorTab.OVERLAY_TEXT -> {
                        OverlayTextControls(
                            config = overlayConfig,
                            onConfigChange = { transform -> viewModel.updateOverlayConfig(transform) }
                        )
                    }
                    EditorTab.QUOTE_STYLE -> {
                        QuoteAndStyleControls(
                            config = overlayConfig,
                            onConfigChange = { transform -> viewModel.updateOverlayConfig(transform) }
                        )
                    }
                    EditorTab.CV_COUNTER -> {
                        RepTimelineEditor(
                            reps = detectedReps,
                            cvResult = cvResult,
                            isAnalyzing = isAnalyzing,
                            analysisProgress = analysisProgress,
                            poseMode = poseMode,
                            sensitivity = sensitivity,
                            onPoseModeChanged = { mode -> viewModel.setPoseMode(mode) },
                            onSensitivityChanged = { sens -> viewModel.setSensitivity(sens) },
                            onReAnalyzeClicked = { viewModel.runCvAnalysis() },
                            onAddRepClicked = { viewModel.addRepAtCurrentTime() },
                            onRemoveRepClicked = { id -> viewModel.removeRep(id) }
                        )
                    }
                    EditorTab.EXPORT -> {
                        CounterBadgeControls(
                            config = overlayConfig,
                            onConfigChange = { transform -> viewModel.updateOverlayConfig(transform) }
                        )
                    }
                }
            }
        }
    }

    // Export progress / share modal
    ExportProgressDialog(
        exportState = exportState,
        onCancelClicked = { viewModel.cancelExport() },
        onDismissClicked = { viewModel.dismissExportDialog() }
    )

    // Install / Download APK Help Dialog
    if (showInstallHelpDialog) {
        InstallAppDialog(
            onDismiss = { viewModel.setShowInstallHelpDialog(false) }
        )
    }
}
