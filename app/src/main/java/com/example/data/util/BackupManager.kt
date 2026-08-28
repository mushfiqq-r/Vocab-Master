package com.example.data.util

import android.content.Context
import android.net.Uri
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val timestamp: Long,
    val formattedDate: String,
    val wordCount: Int,
    val xpTotal: Int
)

data class BackupData(
    val appVersion: String,
    val exportTimestamp: Long,
    val stats: UserStatsEntity,
    val reviewStates: List<ReviewStateEntity>
)

object BackupManager {

    private const val BACKUP_DIR_NAME = "backups"
    private const val SCHEMA_VERSION = 1
    private const val APP_TAG = "VocabMaster_SM2"

    fun generateBackupJson(stats: UserStatsEntity?, reviews: List<ReviewStateEntity>): String {
        val root = JSONObject()
        val currentStats = stats ?: UserStatsEntity()
        val now = System.currentTimeMillis()

        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("appTag", APP_TAG)
        root.put("exportTimestamp", now)
        root.put("exportDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now)))
        root.put("totalReviewsTracked", reviews.size)

        // User Stats object
        val statsObj = JSONObject()
        statsObj.put("id", currentStats.id)
        statsObj.put("currentStreak", currentStats.currentStreak)
        statsObj.put("longestStreak", currentStats.longestStreak)
        statsObj.put("xpTotal", currentStats.xpTotal)
        statsObj.put("level", currentStats.level)
        statsObj.put("dailyGoal", currentStats.dailyGoal)
        statsObj.put("lastActiveDate", currentStats.lastActiveDate)
        root.put("userStats", statsObj)

        // Review States Array
        val reviewsArray = JSONArray()
        for (r in reviews) {
            val rObj = JSONObject()
            rObj.put("wordId", r.wordId)
            rObj.put("easeFactor", r.easeFactor)
            rObj.put("intervalDays", r.intervalDays)
            rObj.put("repetitions", r.repetitions)
            rObj.put("nextReviewDate", r.nextReviewDate)
            rObj.put("lastReviewedDate", r.lastReviewedDate)
            rObj.put("status", r.status)
            reviewsArray.put(rObj)
        }
        root.put("reviewStates", reviewsArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)
            val schemaVersion = root.optInt("schemaVersion", 1)
            val timestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            val statsObj = root.optJSONObject("userStats")
            val stats = if (statsObj != null) {
                UserStatsEntity(
                    id = 1,
                    currentStreak = statsObj.optInt("currentStreak", 0),
                    longestStreak = statsObj.optInt("longestStreak", 0),
                    xpTotal = statsObj.optInt("xpTotal", 0),
                    level = statsObj.optInt("level", 1),
                    dailyGoal = statsObj.optInt("dailyGoal", 15),
                    lastActiveDate = statsObj.optLong("lastActiveDate", 0L)
                )
            } else {
                UserStatsEntity()
            }

            val reviewsList = mutableListOf<ReviewStateEntity>()
            val reviewsArray = root.optJSONArray("reviewStates")
            if (reviewsArray != null) {
                for (i in 0 until reviewsArray.length()) {
                    val rObj = reviewsArray.getJSONObject(i)
                    val wordId = rObj.optLong("wordId", -1L)
                    if (wordId > 0) {
                        reviewsList.add(
                            ReviewStateEntity(
                                wordId = wordId,
                                easeFactor = rObj.optDouble("easeFactor", 2.5).coerceIn(1.30, 3.50),
                                intervalDays = rObj.optInt("intervalDays", 0),
                                repetitions = rObj.optInt("repetitions", 0),
                                nextReviewDate = rObj.optLong("nextReviewDate", System.currentTimeMillis()),
                                lastReviewedDate = rObj.optLong("lastReviewedDate", 0L),
                                status = rObj.optString("status", "new")
                            )
                        )
                    }
                }
            }

            Result.success(
                BackupData(
                    appVersion = "v$schemaVersion",
                    exportTimestamp = timestamp,
                    stats = stats,
                    reviewStates = reviewsList
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid or corrupted backup JSON file: ${e.message}", e))
        }
    }

    fun saveToLocalAppDirectory(context: Context, jsonString: String): Result<File> {
        return try {
            val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "vocab_backup_$timeStamp.json")
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listLocalBackups(context: Context): List<BackupMetadata> {
        val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!backupDir.exists()) return emptyList()

        val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".json") } ?: return emptyList()
        val list = mutableListOf<BackupMetadata>()

        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        for (file in files.sortedByDescending { it.lastModified() }) {
            try {
                val content = file.readText()
                val parsed = parseBackupJson(content).getOrNull()
                val wordCount = parsed?.reviewStates?.size ?: 0
                val xp = parsed?.stats?.xpTotal ?: 0
                val sizeKb = (file.length() / 1024).coerceAtLeast(1)

                list.add(
                    BackupMetadata(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileSizeFormatted = "${sizeKb} KB",
                        timestamp = file.lastModified(),
                        formattedDate = sdf.format(Date(file.lastModified())),
                        wordCount = wordCount,
                        xpTotal = xp
                    )
                )
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }
        return list
    }

    fun deleteLocalBackup(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    fun readJsonFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalStateException("Could not open file stream"))
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeJsonToUri(context: Context, uri: Uri, jsonContent: String): Result<Unit> {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("Could not open file output stream"))
            outputStream.write(jsonContent.toByteArray())
            outputStream.flush()
            outputStream.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
