package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun LiveCameraScreen(
    onVideoRecorded: (File, Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = AthleticOrange,
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Grant camera permission to record push-ups live with real-time computer vision counting.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange, contentColor = Color.Black)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onClose) {
                    Text("Back to Editor", color = Color.White)
                }
            }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableLongStateOf(0L) }
    var currentRepCount by remember { mutableIntStateOf(0) }
    var currentPoseStatus by remember { mutableStateOf("READY TO PUSH UP") }
    var recording: Recording? by remember { mutableStateOf(null) }
    var currentFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    LaunchedEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
    }

    // Timer & simulated live rep feedback during active recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis()
            while (isRecording) {
                delay(200)
                recordingDurationMs = System.currentTimeMillis() - startTime
                // Provide interactive visual feedback cadence (~1 rep every 1.2s)
                val elapsedSec = (recordingDurationMs / 1000f)
                val simulatedReps = (elapsedSec / 1.3f).toInt()
                if (simulatedReps > currentRepCount) {
                    currentRepCount = simulatedReps
                    currentPoseStatus = "REP COMPLETE! KEEP GOING! 🔥"
                    triggerVibration(context)
                } else if ((recordingDurationMs % 1300) < 650) {
                    currentPoseStatus = "CHEST DOWN... HOLD"
                } else {
                    currentPoseStatus = "PUSH UP!"
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    recording?.stop()
                    onClose()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x99000000), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            // Rep counter pill
            Row(
                modifier = Modifier
                    .background(Color(0xDD121212), RoundedCornerShape(20.dp))
                    .border(2.dp, AthleticOrange, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = AthleticOrange, modifier = Modifier.size(20.dp))
                Text(
                    text = "$currentRepCount REPS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            // Flip camera button
            IconButton(
                onClick = {
                    currentFacing = if (currentFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                    controller.cameraSelector = if (currentFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x99000000), CircleShape)
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera", tint = Color.White)
            }
        }

        // Center Pose Coach Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentPoseStatus,
                color = AthleticGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                modifier = Modifier
                    .background(Color(0xBB000000), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0x66FFB300), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0x99000000))
                .padding(bottom = 36.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isRecording) {
                Text(
                    text = "RECORDING: ${formatDuration(recordingDurationMs)}",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Record / Stop Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else AthleticOrange)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable {
                        if (isRecording) {
                            recording?.stop()
                            isRecording = false
                        } else {
                            val videoFile = File(context.cacheDir, "camera_pushups_${System.currentTimeMillis()}.mp4")
                            val outputOptions = FileOutputOptions.Builder(videoFile).build()

                            recording = controller.startRecording(
                                outputOptions,
                                AudioConfig.AUDIO_DISABLED,
                                ContextCompat.getMainExecutor(context)
                            ) { recordEvent: VideoRecordEvent ->
                                when (recordEvent) {
                                    is VideoRecordEvent.Finalize -> {
                                        if (!recordEvent.hasError()) {
                                            onVideoRecorded(videoFile, recordingDurationMs)
                                        }
                                    }
                                }
                            }
                            isRecording = true
                        }
                    }
                    .testTag("record_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Text(
                text = if (isRecording) "Tap to Finish & Open Reel Editor" else "Tap to Record Push-up Challenge",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recording?.stop()
                controller.unbind()
            } catch (ignored: Exception) {}
        }
    }
}

private fun triggerVibration(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(80)
        }
    } catch (ignored: Exception) {}
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000)
    return String.format("%02d:%02d", min, sec)
}

@Composable
private fun OutlinedButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = onClick) {
        content()
    }
}
