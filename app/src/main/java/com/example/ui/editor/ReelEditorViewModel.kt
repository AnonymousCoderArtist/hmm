package com.example.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cv.PushUpCvDetector
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.model.CvAnalysisResult
import com.example.data.model.ExportState
import com.example.data.model.OverlayConfig
import com.example.data.model.PoseMode
import com.example.data.model.PushUpRep
import com.example.data.model.Sensitivity
import com.example.data.model.VideoMediaInfo
import com.example.data.repository.ProjectRepository
import com.example.export.SampleVideoGenerator
import com.example.export.VideoReelExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppScreen {
    EDITOR,
    RECENT_PROJECTS
}

enum class EditorTab(val title: String) {
    OVERLAY_TEXT("Header & Text"),
    QUOTE_STYLE("Quote & Fonts"),
    CV_COUNTER("CV Push-up Counter"),
    EXPORT("Export Reel")
}

class ReelEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val cvDetector = PushUpCvDetector(application)
    private val videoExporter = VideoReelExporter(application)
    private val projectRepository = ProjectRepository(AppDatabase.getDatabase(application).projectDao())

    val recentProjects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.EDITOR)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId: StateFlow<Long?> = _currentProjectId.asStateFlow()

    private val _isAutoSaved = MutableStateFlow(true)
    val isAutoSaved: StateFlow<Boolean> = _isAutoSaved.asStateFlow()

    private val _overlayConfig = MutableStateFlow(OverlayConfig())
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    private val _videoMedia = MutableStateFlow<VideoMediaInfo?>(null)
    val videoMedia: StateFlow<VideoMediaInfo?> = _videoMedia.asStateFlow()

    private val _detectedReps = MutableStateFlow<List<PushUpRep>>(emptyList())
    val detectedReps: StateFlow<List<PushUpRep>> = _detectedReps.asStateFlow()

    private val _cvAnalysisResult = MutableStateFlow<CvAnalysisResult?>(null)
    val cvAnalysisResult: StateFlow<CvAnalysisResult?> = _cvAnalysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentPlaybackTimeMs = MutableStateFlow(0L)
    val currentPlaybackTimeMs: StateFlow<Long> = _currentPlaybackTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _poseMode = MutableStateFlow(PoseMode.AUTO)
    val poseMode: StateFlow<PoseMode> = _poseMode.asStateFlow()

    private val _sensitivity = MutableStateFlow(Sensitivity.BALANCED)
    val sensitivity: StateFlow<Sensitivity> = _sensitivity.asStateFlow()

    private val _selectedTab = MutableStateFlow(EditorTab.OVERLAY_TEXT)
    val selectedTab: StateFlow<EditorTab> = _selectedTab.asStateFlow()

    private val _showInstallHelpDialog = MutableStateFlow(false)
    val showInstallHelpDialog: StateFlow<Boolean> = _showInstallHelpDialog.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        // Restore last active draft if available, otherwise initialize demo
        viewModelScope.launch {
            val draft = projectRepository.getCurrentDraft()
            if (draft != null) {
                restoreFromEntity(draft)
            } else {
                loadSampleWorkout(autoAnalyze = true)
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setShowInstallHelpDialog(show: Boolean) {
        _showInstallHelpDialog.value = show
    }

    fun setSelectedTab(tab: EditorTab) {
        _selectedTab.value = tab
    }

    fun setPlaybackTime(timeMs: Long) {
        _currentPlaybackTimeMs.value = timeMs
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setPoseMode(mode: PoseMode) {
        _poseMode.value = mode
        runCvAnalysis()
    }

    fun setSensitivity(sens: Sensitivity) {
        _sensitivity.value = sens
        runCvAnalysis()
    }

    fun updateOverlayConfig(transform: (OverlayConfig) -> OverlayConfig) {
        _overlayConfig.value = transform(_overlayConfig.value)
        triggerAutoSave()
    }

    fun loadSampleWorkout(autoAnalyze: Boolean = true) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val sampleFile = SampleVideoGenerator.getOrCreateSampleVideo(getApplication())
            val mediaInfo = VideoMediaInfo(
                uri = Uri.fromFile(sampleFile),
                filePath = sampleFile.absolutePath,
                isSampleDemo = true,
                durationMs = 10000L,
                width = 640,
                height = 360,
                displayName = "Sample Push-up Workout"
            )
            _videoMedia.value = mediaInfo

            if (autoAnalyze) {
                runCvAnalysisInternal(mediaInfo)
            } else {
                _isAnalyzing.value = false
            }
            triggerAutoSave()
        }
    }

    fun onVideoPicked(uri: Uri, displayName: String, durationMs: Long = 15000L) {
        viewModelScope.launch {
            val mediaInfo = VideoMediaInfo(
                uri = uri,
                filePath = null,
                isSampleDemo = false,
                durationMs = durationMs,
                width = 1280,
                height = 720,
                displayName = displayName
            )
            _videoMedia.value = mediaInfo
            runCvAnalysisInternal(mediaInfo)
            triggerAutoSave()
        }
    }

    fun onVideoRecorded(file: File, durationMs: Long) {
        viewModelScope.launch {
            val mediaInfo = VideoMediaInfo(
                uri = Uri.fromFile(file),
                filePath = file.absolutePath,
                isSampleDemo = false,
                durationMs = durationMs,
                width = 1280,
                height = 720,
                displayName = "Recorded Push-ups"
            )
            _videoMedia.value = mediaInfo
            runCvAnalysisInternal(mediaInfo)
            triggerAutoSave()
        }
    }

    fun runCvAnalysis() {
        val currentMedia = _videoMedia.value ?: return
        viewModelScope.launch {
            runCvAnalysisInternal(currentMedia)
        }
    }

    private suspend fun runCvAnalysisInternal(media: VideoMediaInfo) {
        _isAnalyzing.value = true
        _analysisProgress.value = 0f

        val result = cvDetector.analyzeVideo(
            videoUri = media.uri,
            videoFilePath = media.filePath,
            poseMode = _poseMode.value,
            sensitivity = _sensitivity.value,
            onProgress = { p -> _analysisProgress.value = p }
        )

        _cvAnalysisResult.value = result
        _detectedReps.value = result.detectedReps

        // If user hasn't explicitly set target, sync with detected reps
        if (result.detectedReps.isNotEmpty() && _overlayConfig.value.targetPushups <= 0) {
            _overlayConfig.value = _overlayConfig.value.copy(targetPushups = result.detectedReps.size)
        }

        _isAnalyzing.value = false
        triggerAutoSave()
    }

    fun addRepAtCurrentTime() {
        val timeMs = _currentPlaybackTimeMs.value
        val currentList = _detectedReps.value.toMutableList()
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        currentList.add(PushUpRep(id = newId, timestampMs = timeMs, depthPercent = 95f))
        currentList.sortBy { it.timestampMs }
        _detectedReps.value = currentList
        triggerAutoSave()
    }

    fun removeRep(id: Int) {
        _detectedReps.value = _detectedReps.value.filter { it.id != id }
        triggerAutoSave()
    }

    fun startExport(width: Int = 720, height: Int = 1280, fps: Int = 30) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress(0f, 0, 100, "Initializing H.264 video encoder...")

            try {
                val media = _videoMedia.value
                val (filePath, uri) = videoExporter.exportReel(
                    videoUri = media?.uri,
                    videoFilePath = media?.filePath,
                    overlayConfig = _overlayConfig.value,
                    reps = _detectedReps.value,
                    outputWidth = width,
                    outputHeight = height,
                    fps = fps,
                    onProgress = { p, curr, total ->
                        _exportState.value = ExportState.InProgress(
                            progress = p,
                            currentFrame = curr,
                            totalFrames = total,
                            message = "Rendering frame $curr / $total (${(p * 100).toInt()}%)"
                        )
                    }
                )

                _exportState.value = ExportState.Success(
                    outputPath = filePath,
                    outputUri = uri,
                    durationMs = media?.durationMs ?: 12000L
                )

                // Save export status in database
                val projId = _currentProjectId.value ?: 0L
                val savedId = projectRepository.saveProject(
                    id = projId,
                    config = _overlayConfig.value,
                    reps = _detectedReps.value,
                    videoUri = media?.uri,
                    videoFilePath = media?.filePath,
                    exportedPath = filePath,
                    exportedUri = uri,
                    isExported = true
                )
                _currentProjectId.value = savedId

            } catch (e: Exception) {
                e.printStackTrace()
                _exportState.value = ExportState.Error(e.message ?: "Failed to export video")
            }
        }
    }

    fun cancelExport() {
        videoExporter.cancel()
        _exportState.value = ExportState.Idle
    }

    fun dismissExportDialog() {
        _exportState.value = ExportState.Idle
    }

    // --- Auto-Save & Recent Projects Management ---

    private fun triggerAutoSave() {
        _isAutoSaved.value = false
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600) // Debounce rapid slider/drag updates
            val projId = _currentProjectId.value ?: 0L
            val media = _videoMedia.value
            val savedId = projectRepository.saveProject(
                id = projId,
                config = _overlayConfig.value,
                reps = _detectedReps.value,
                videoUri = media?.uri,
                videoFilePath = media?.filePath
            )
            _currentProjectId.value = savedId
            _isAutoSaved.value = true
        }
    }

    fun loadProject(id: Long) {
        viewModelScope.launch {
            val entity = projectRepository.getProjectById(id) ?: return@launch
            restoreFromEntity(entity)
            _currentScreen.value = AppScreen.EDITOR
        }
    }

    fun createNewProject() {
        viewModelScope.launch {
            val nextDay = (_overlayConfig.value.dayNumber + 1).coerceAtLeast(1)
            val newConfig = _overlayConfig.value.copy(
                dayNumber = nextDay,
                targetPushups = nextDay,
                customHeaderText = "DAY $nextDay = $nextDay PUSH UP"
            )
            _overlayConfig.value = newConfig
            _detectedReps.value = emptyList()
            _cvAnalysisResult.value = null
            _currentProjectId.value = null

            loadSampleWorkout(autoAnalyze = true)
            _currentScreen.value = AppScreen.EDITOR
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
            if (_currentProjectId.value == id) {
                _currentProjectId.value = null
            }
        }
    }

    private fun restoreFromEntity(entity: ProjectEntity) {
        _currentProjectId.value = entity.id
        _overlayConfig.value = projectRepository.toOverlayConfig(entity)
        _detectedReps.value = projectRepository.deserializeReps(entity.repsDataJson)

        val uri = entity.videoUriString?.let { Uri.parse(it) }
        val filePath = entity.videoFilePath

        if (filePath != null && File(filePath).exists()) {
            _videoMedia.value = VideoMediaInfo(
                uri = uri ?: Uri.fromFile(File(filePath)),
                filePath = filePath,
                isSampleDemo = filePath.contains("sample_pushup_workout"),
                durationMs = 12000L,
                displayName = entity.title
            )
        } else if (uri != null) {
            _videoMedia.value = VideoMediaInfo(
                uri = uri,
                filePath = null,
                isSampleDemo = false,
                durationMs = 12000L,
                displayName = entity.title
            )
        } else {
            loadSampleWorkout(autoAnalyze = false)
        }
        _isAutoSaved.value = true
    }
}
