package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Push-up Reel",
    val dayNumber: Int = 29,
    val targetPushups: Int = 29,
    val detectedRepsCount: Int = 0,
    val repsDataJson: String = "[]",
    val quoteText: String = "Posting everyday until i forget her",
    val headerFormat: String = "DAY_EQUALS_COUNT",
    val customHeaderText: String = "DAY 29 = 29 PUSH UP",
    val headerFont: String = "IMPACT_CONDENSED",
    val quoteFont: String = "CLEAN_SANS",
    val headerColorHex: Long = 0xFFFFFFFF,
    val quoteColorHex: Long = 0xFFFFFFFF,
    val backgroundColorHex: Long = 0xFF000000,
    val headerYPercent: Float = 0.16f,
    val quoteYPercent: Float = 0.67f,
    val badgeXPercent: Float = 0.78f,
    val badgeYPercent: Float = 0.10f,
    val headerTextSizeSp: Int = 38,
    val quoteTextSizeSp: Int = 22,
    val showBadgeCounter: Boolean = true,
    val badgeStyle: String = "PILL_ACCENT",
    val badgeColorHex: Long = 0xFFFF5722,
    val showProgressBar: Boolean = true,
    val videoUriString: String? = null,
    val videoFilePath: String? = null,
    val exportedVideoPath: String? = null,
    val exportedUriString: String? = null,
    val isExported: Boolean = false,
    val isCurrentDraft: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
)
