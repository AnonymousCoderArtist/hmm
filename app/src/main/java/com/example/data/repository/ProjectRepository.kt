package com.example.data.repository

import android.net.Uri
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.model.BadgeStyle
import com.example.data.model.HeaderFormatMode
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayFont
import com.example.data.model.PushUpRep
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getCurrentDraft(): ProjectEntity? = projectDao.getCurrentDraft()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun saveProject(
        id: Long = 0,
        config: OverlayConfig,
        reps: List<PushUpRep>,
        videoUri: Uri?,
        videoFilePath: String?,
        exportedPath: String? = null,
        exportedUri: Uri? = null,
        isExported: Boolean = false
    ): Long {
        val repsJson = serializeReps(reps)
        val title = "Day ${config.dayNumber} Push-up Reel"

        val entity = ProjectEntity(
            id = id,
            title = title,
            dayNumber = config.dayNumber,
            targetPushups = config.targetPushups,
            detectedRepsCount = reps.size,
            repsDataJson = repsJson,
            quoteText = config.quoteText,
            headerFormat = config.headerFormat.name,
            customHeaderText = config.customHeaderText,
            headerFont = config.headerFont.name,
            quoteFont = config.quoteFont.name,
            headerColorHex = config.headerColor,
            quoteColorHex = config.quoteColor,
            backgroundColorHex = config.backgroundColor,
            headerYPercent = config.headerYPercent,
            quoteYPercent = config.quoteYPercent,
            badgeXPercent = config.badgeXPercent,
            badgeYPercent = config.badgeYPercent,
            headerTextSizeSp = config.headerTextSizeSp,
            quoteTextSizeSp = config.quoteTextSizeSp,
            showBadgeCounter = config.showBadgeCounter,
            badgeStyle = config.badgeStyle.name,
            badgeColorHex = config.badgeColor,
            showProgressBar = config.showProgressBar,
            videoUriString = videoUri?.toString(),
            videoFilePath = videoFilePath,
            exportedVideoPath = exportedPath,
            exportedUriString = exportedUri?.toString(),
            isExported = isExported,
            isCurrentDraft = true,
            lastModified = System.currentTimeMillis()
        )

        val insertedId = projectDao.insertProject(entity)
        val finalId = if (id != 0L) id else insertedId
        projectDao.clearOtherDrafts(finalId)
        return finalId
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    fun deserializeReps(jsonStr: String): List<PushUpRep> {
        val list = mutableListOf<PushUpRep>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PushUpRep(
                        id = obj.optInt("id", i + 1),
                        timestampMs = obj.optLong("timestampMs", 0L),
                        depthPercent = obj.optDouble("depthPercent", 95.0).toFloat(),
                        durationMs = obj.optLong("durationMs", 1000L),
                        confidence = obj.optDouble("confidence", 0.95).toFloat()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeReps(reps: List<PushUpRep>): String {
        val arr = JSONArray()
        for (rep in reps) {
            val obj = JSONObject().apply {
                put("id", rep.id)
                put("timestampMs", rep.timestampMs)
                put("depthPercent", rep.depthPercent)
                put("durationMs", rep.durationMs)
                put("confidence", rep.confidence)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun toOverlayConfig(entity: ProjectEntity): OverlayConfig {
        return OverlayConfig(
            dayNumber = entity.dayNumber,
            targetPushups = entity.targetPushups,
            headerFormat = try { HeaderFormatMode.valueOf(entity.headerFormat) } catch (e: Exception) { HeaderFormatMode.DAY_EQUALS_COUNT },
            customHeaderText = entity.customHeaderText,
            quoteText = entity.quoteText,
            headerFont = try { OverlayFont.valueOf(entity.headerFont) } catch (e: Exception) { OverlayFont.IMPACT_CONDENSED },
            quoteFont = try { OverlayFont.valueOf(entity.quoteFont) } catch (e: Exception) { OverlayFont.CLEAN_SANS },
            headerColor = entity.headerColorHex,
            quoteColor = entity.quoteColorHex,
            backgroundColor = entity.backgroundColorHex,
            headerYPercent = entity.headerYPercent,
            quoteYPercent = entity.quoteYPercent,
            badgeXPercent = entity.badgeXPercent,
            badgeYPercent = entity.badgeYPercent,
            headerTextSizeSp = entity.headerTextSizeSp,
            quoteTextSizeSp = entity.quoteTextSizeSp,
            showBadgeCounter = entity.showBadgeCounter,
            badgeStyle = try { BadgeStyle.valueOf(entity.badgeStyle) } catch (e: Exception) { BadgeStyle.PILL_ACCENT },
            badgeColor = entity.badgeColorHex,
            showProgressBar = entity.showProgressBar
        )
    }
}
