package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticGreen
import com.example.ui.theme.AthleticOrange

@Composable
fun InstallAppDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AthleticOrange, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().testTag("close_install_dialog")
            ) {
                Text("Got It, Let's Push!", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0x33FF5722), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AthleticOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Install App on Phone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Run on your real phone with camera & real videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📲 How to Download & Install the APK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AthleticGold
                        )

                        StepRow(
                            step = "1",
                            text = "In Google AI Studio, tap the top Settings / Export menu (three dots or download icon)."
                        )
                        StepRow(
                            step = "2",
                            text = "Select 'Export Project as ZIP' or download the generated APK."
                        )
                        StepRow(
                            step = "3",
                            text = "Transfer or download the APK directly onto your Android device."
                        )
                        StepRow(
                            step = "4",
                            text = "Tap the APK on your phone and choose 'Install'. Now Naren has the native standalone app!"
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🏋️ Real-Life Video Tips for Best Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AthleticGreen
                        )

                        TipRow(
                            icon = Icons.Default.Videocam,
                            title = "Any Camera Angle Supported",
                            desc = "Front Pose tracks chest depth & lateral elbow flare. Side Pose tracks torso flexion."
                        )
                        TipRow(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Stable Framing",
                            desc = "Prop your phone on the ground or against a water bottle 2-3 meters away."
                        )
                        TipRow(
                            icon = Icons.Default.FitnessCenter,
                            title = "Manual Override Timeline",
                            desc = "Use the CV Reps Timeline in the app to add or delete any rep if gym lighting causes shadows."
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun StepRow(step: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(AthleticOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = AthleticGreen, modifier = Modifier.size(18.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
