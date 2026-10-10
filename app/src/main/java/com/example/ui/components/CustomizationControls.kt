package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.BACKGROUND_COLOR_PRESETS
import com.example.data.model.BadgePosition
import com.example.data.model.BadgeStyle
import com.example.data.model.ColorPreset
import com.example.data.model.HeaderFormatMode
import com.example.data.model.OVERLAY_COLOR_PRESETS
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayFont
import com.example.ui.theme.AthleticGold
import com.example.ui.theme.AthleticOrange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverlayTextControls(
    config: OverlayConfig,
    onConfigChange: ((OverlayConfig) -> OverlayConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Day Number & Target Push-up count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.dayNumber.toString(),
                onValueChange = { str ->
                    val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                    onConfigChange { it.copy(dayNumber = num) }
                },
                label = { Text("Day Number") },
                modifier = Modifier.weight(1f).testTag("day_number_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = config.targetPushups.toString(),
                onValueChange = { str ->
                    val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                    onConfigChange { it.copy(targetPushups = num) }
                },
                label = { Text("Push-ups Target") },
                modifier = Modifier.weight(1f).testTag("target_pushups_input"),
                singleLine = true
            )
        }

        // Header Title Format
        Text(
            text = "Header Format",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderFormatMode.values().forEach { mode ->
                FilterChip(
                    selected = config.headerFormat == mode,
                    onClick = { onConfigChange { it.copy(headerFormat = mode) } },
                    label = { Text(mode.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticOrange,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("header_format_${mode.name}")
                )
            }
        }

        if (config.headerFormat == HeaderFormatMode.CUSTOM) {
            OutlinedTextField(
                value = config.customHeaderText,
                onValueChange = { text -> onConfigChange { it.copy(customHeaderText = text) } },
                label = { Text("Custom Title") },
                modifier = Modifier.fillMaxWidth().testTag("custom_header_input"),
                singleLine = true
            )
        }

        // Header Font Family
        Text(
            text = "Header Typography",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayFont.values().forEach { font ->
                FilterChip(
                    selected = config.headerFont == font,
                    onClick = { onConfigChange { it.copy(headerFont = font) } },
                    label = { Text(font.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Header Color Presets (Strictly NO blue or purple)
        Text(
            text = "Header Text Color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        ColorPalettePicker(
            selectedColorHex = config.headerColor,
            onColorSelected = { hex -> onConfigChange { it.copy(headerColor = hex) } }
        )

        // Font Size Slider
        Text(
            text = "Font Size: ${config.headerTextSizeSp} sp",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.headerTextSizeSp.toFloat(),
            onValueChange = { onConfigChange { cfg -> cfg.copy(headerTextSizeSp = it.toInt()) } },
            valueRange = 24f..56f,
            colors = SliderDefaults.colors(thumbColor = AthleticOrange, activeTrackColor = AthleticOrange),
            modifier = Modifier.fillMaxWidth().testTag("header_font_size_slider")
        )

        // Header Vertical Position Y-Offset Slider
        Text(
            text = "Vertical Position: ${(config.headerYPercent * 100).toInt()}% from Top",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.headerYPercent,
            onValueChange = { onConfigChange { cfg -> cfg.copy(headerYPercent = it) } },
            valueRange = 0.05f..0.38f,
            colors = SliderDefaults.colors(thumbColor = AthleticGold, activeTrackColor = AthleticGold),
            modifier = Modifier.fillMaxWidth().testTag("header_y_slider")
        )

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ALL CAPS BOLD", fontWeight = FontWeight.SemiBold)
            Switch(
                checked = config.allCapsHeader,
                onCheckedChange = { onConfigChange { cfg -> cfg.copy(allCapsHeader = it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = AthleticOrange, checkedTrackColor = AthleticGold)
            )
        }
    }
}

@Composable
fun QuoteAndStyleControls(
    config: OverlayConfig,
    onConfigChange: ((OverlayConfig) -> OverlayConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetQuotes = listOf(
        "Posting everyday until i forget her",
        "Discipline over motivation",
        "Day by day, rep by rep",
        "Prove them wrong",
        "Consistency is king",
        "Stronger than yesterday"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bottom Quote / Subtitle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = config.quoteText,
            onValueChange = { text -> onConfigChange { it.copy(quoteText = text) } },
            label = { Text("Reel Quote Text") },
            modifier = Modifier.fillMaxWidth().testTag("quote_text_input")
        )

        // Quick Preset Quotes Chips
        Text(
            text = "Preset Quotes",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetQuotes.forEach { quote ->
                AssistChip(
                    onClick = { onConfigChange { it.copy(quoteText = quote) } },
                    label = { Text(quote, fontSize = 12.sp) }
                )
            }
        }

        // Quote Typography
        Text(
            text = "Quote Font",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayFont.values().forEach { font ->
                FilterChip(
                    selected = config.quoteFont == font,
                    onClick = { onConfigChange { it.copy(quoteFont = font) } },
                    label = { Text(font.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AthleticGold,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Quote Text Color
        Text(
            text = "Quote Color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        ColorPalettePicker(
            selectedColorHex = config.quoteColor,
            onColorSelected = { hex -> onConfigChange { it.copy(quoteColor = hex) } }
        )

        // Quote Size & Position
        Text(
            text = "Quote Size: ${config.quoteTextSizeSp} sp",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.quoteTextSizeSp.toFloat(),
            onValueChange = { onConfigChange { cfg -> cfg.copy(quoteTextSizeSp = it.toInt()) } },
            valueRange = 16f..36f,
            colors = SliderDefaults.colors(thumbColor = AthleticOrange, activeTrackColor = AthleticOrange)
        )

        Text(
            text = "Vertical Position: ${(config.quoteYPercent * 100).toInt()}% from Top",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.quoteYPercent,
            onValueChange = { onConfigChange { cfg -> cfg.copy(quoteYPercent = it) } },
            valueRange = 0.60f..0.92f,
            colors = SliderDefaults.colors(thumbColor = AthleticGold, activeTrackColor = AthleticGold)
        )

        // Reel Background Color
        Text(
            text = "Reel Background (Letterboxing)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BACKGROUND_COLOR_PRESETS.forEach { preset ->
                val isSelected = config.backgroundColor == preset.colorHex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(preset.colorHex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) AthleticOrange else Color(0xFF555555),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onConfigChange { it.copy(backgroundColor = preset.colorHex) } }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = if (preset.colorHex == 0xFFF5F5F7) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun CounterBadgeControls(
    config: OverlayConfig,
    onConfigChange: ((OverlayConfig) -> OverlayConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle live counter badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Counter Badge", fontWeight = FontWeight.Bold)
                Text(
                    "Floating live rep counter badge on Reel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.showBadgeCounter,
                onCheckedChange = { onConfigChange { cfg -> cfg.copy(showBadgeCounter = it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = AthleticOrange, checkedTrackColor = AthleticGold)
            )
        }

        if (config.showBadgeCounter) {
            Text("Badge Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeStyle.values().forEach { style ->
                    FilterChip(
                        selected = config.badgeStyle == style,
                        onClick = { onConfigChange { it.copy(badgeStyle = style) } },
                        label = { Text(style.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AthleticOrange,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Text("Counter Badge Placement (Draggable)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "💡 Touch & drag the counter badge directly on the Reel preview above, or use the fine-tuning sliders below.",
                    fontSize = 12.sp,
                    color = AthleticGold
                )
            }

            Text("Horizontal Position (X): ${(config.badgeXPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.badgeXPercent,
                onValueChange = { onConfigChange { cfg -> cfg.copy(badgeXPercent = it) } },
                valueRange = 0.10f..0.90f,
                colors = SliderDefaults.colors(thumbColor = AthleticOrange, activeTrackColor = AthleticOrange),
                modifier = Modifier.fillMaxWidth().testTag("badge_x_slider")
            )

            Text("Vertical Position (Y): ${(config.badgeYPercent * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = config.badgeYPercent,
                onValueChange = { onConfigChange { cfg -> cfg.copy(badgeYPercent = it) } },
                valueRange = 0.05f..0.92f,
                colors = SliderDefaults.colors(thumbColor = AthleticGold, activeTrackColor = AthleticGold),
                modifier = Modifier.fillMaxWidth().testTag("badge_y_slider")
            )

            Text("Quick Presets", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onConfigChange { it.copy(badgeXPercent = 0.78f, badgeYPercent = 0.10f) } },
                    label = { Text("Top Right") }
                )
                AssistChip(
                    onClick = { onConfigChange { it.copy(badgeXPercent = 0.22f, badgeYPercent = 0.10f) } },
                    label = { Text("Top Left") }
                )
                AssistChip(
                    onClick = { onConfigChange { it.copy(badgeXPercent = 0.50f, badgeYPercent = 0.38f) } },
                    label = { Text("Over Video") }
                )
                AssistChip(
                    onClick = { onConfigChange { it.copy(badgeXPercent = 0.78f, badgeYPercent = 0.88f) } },
                    label = { Text("Bottom Right") }
                )
            }

            Text("Badge Accent Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ColorPalettePicker(
                selectedColorHex = config.badgeColor,
                onColorSelected = { hex -> onConfigChange { it.copy(badgeColor = hex) } }
            )
        }

        // Progress bar toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Bottom Progress Bar", fontWeight = FontWeight.Bold)
                Text(
                    "Dynamic bar filling up towards target reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = config.showProgressBar,
                onCheckedChange = { onConfigChange { cfg -> cfg.copy(showProgressBar = it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = AthleticOrange, checkedTrackColor = AthleticGold)
            )
        }
    }
}

@Composable
fun ColorPalettePicker(
    selectedColorHex: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OVERLAY_COLOR_PRESETS.forEach { preset ->
            val isSelected = selectedColorHex == preset.colorHex
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(preset.colorHex))
                    .border(
                        width = if (isSelected) 3.dp else 1.5.dp,
                        color = if (isSelected) AthleticOrange else Color(0x66888888),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(preset.colorHex) }
            )
        }
    }
}
