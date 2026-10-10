package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CvAnalysisResult
import com.example.data.model.PoseMode
import com.example.data.model.PushUpRep
import com.example.data.model.Sensitivity
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange

@Composable
fun RepTimelineEditor(
    reps: List<PushUpRep>,
    cvResult: CvAnalysisResult?,
    isAnalyzing: Boolean,
    analysisProgress: Float,
    poseMode: PoseMode,
    sensitivity: Sensitivity,
    onPoseModeChanged: (PoseMode) -> Unit,
    onSensitivityChanged: (Sensitivity) -> Unit,
    onReAnalyzeClicked: () -> Unit,
    onAddRepClicked: () -> Unit,
    onRemoveRepClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CV Pose Mode Selection (Front, Side, Rapid)
        Text(
            text = "Computer Vision Pose Angle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PoseMode.values().forEach { mode ->
                FilterChip(
                    selected = poseMode == mode,
                    onClick = { onPoseModeChanged(mode) },
                    label = { Text(mode.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticOrange,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f).testTag("pose_mode_${mode.name}")
                )
            }
        }

        // Sensitivity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Sensitivity.values().forEach { sens ->
                FilterChip(
                    selected = sensitivity == sens,
                    onClick = { onSensitivityChanged(sens) },
                    label = { Text(sens.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticGold,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Re-analyze Button / Progress
        if (isAnalyzing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Analyzing frames with Computer Vision...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${(analysisProgress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AthleticOrange)
                }
                LinearProgressIndicator(
                    progress = { analysisProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AthleticOrange,
                    trackColor = Color(0x33888888)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReAnalyzeClicked,
                    modifier = Modifier.weight(1f).testTag("reanalyze_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Re-analyze", tint = AthleticOrange)
                    Spacer(Modifier.width(6.dp))
                    Text("Re-Run CV", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = onAddRepClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).testTag("add_rep_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rep")
                    Spacer(Modifier.width(6.dp))
                    Text("Add Rep at Playhead", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Analysis Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Detected Reps", value = "${reps.size}", icon = Icons.Default.FitnessCenter, tint = AthleticOrange)
                StatColumn(label = "Avg Tempo", value = "${(cvResult?.averageRepTimeMs ?: 1100L) / 1000f}s", icon = Icons.Default.Timer, tint = AthleticGold)
                StatColumn(label = "Consistency", value = "${cvResult?.consistencyScore ?: 94}%", icon = Icons.Default.CheckCircle, tint = AthleticGreen)
            }
        }

        // Reps list with delete action
        Text(
            text = "Detected Reps Timeline (${reps.size} total)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(reps, key = { it.id }) { rep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(AthleticOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rep.id}",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Push-up #${rep.id} • ${formatTimestamp(rep.timestampMs)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Depth: ${rep.depthPercent.toInt()}% • Duration: ${rep.durationMs}ms",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRemoveRepClicked(rep.id) },
                        modifier = Modifier.size(32.dp).testTag("delete_rep_${rep.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove rep",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTimestamp(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000)
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, millis)
}
