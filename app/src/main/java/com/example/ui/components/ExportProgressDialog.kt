package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExportState
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange

@Composable
fun ExportProgressDialog(
    exportState: ExportState,
    onCancelClicked: () -> Unit,
    onDismissClicked: () -> Unit
) {
    val context = LocalContext.current

    if (exportState is ExportState.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportState.InProgress) onDismissClicked()
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (exportState) {
                    is ExportState.InProgress -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0x33FF5722), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { exportState.progress },
                                color = AthleticOrange,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Text(
                            text = "Exporting 9:16 Instagram Reel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = exportState.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AthleticOrange,
                            trackColor = Color(0x33888888)
                        )

                        Text(
                            text = "${(exportState.progress * 100).toInt()}% • Frame ${exportState.currentFrame}/${exportState.totalFrames}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AthleticGold
                        )

                        OutlinedButton(
                            onClick = onCancelClicked,
                            modifier = Modifier.fillMaxWidth().testTag("cancel_export_button")
                        ) {
                            Text("Cancel Export")
                        }
                    }

                    is ExportState.Success -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0x2200E676), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Export complete",
                                tint = AthleticGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = "Reel Exported Successfully! 🎉",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Your vertical 9:16 push-up video with dynamic counter overlay is saved directly to your Gallery (Movies/PushUpReels).",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        // Share to Instagram / Apps Action
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, exportState.outputUri)
                                    putExtra(Intent.EXTRA_TEXT, "Day Push-up Challenge Reel! Keep pushing! 🔥")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Reel to Instagram"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AthleticOrange,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("share_instagram_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(Modifier.width(8.dp))
                            Text("Share to Instagram / Reel", fontWeight = FontWeight.Bold)
                        }

                        // Open in Video Player
                        OutlinedButton(
                            onClick = {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(exportState.outputUri, "video/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(viewIntent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("view_exported_button")
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "View Video")
                            Spacer(Modifier.width(8.dp))
                            Text("Play Exported Video")
                        }

                        Button(
                            onClick = onDismissClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Editor", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    is ExportState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(54.dp)
                        )

                        Text(
                            text = "Export Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = exportState.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onDismissClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }

                    is ExportState.Idle -> {}
                }
            }
        }
    )
}
