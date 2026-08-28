package com.example.data.util

import com.example.data.model.ReviewStateEntity
import com.example.data.model.SynonymShiftItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object SM2Algorithm {
    enum class ReviewGrade(val quality: Int, val label: String, val xp: Int) {
        AGAIN(quality = 1, label = "Again", xp = 5),
        HARD(quality = 3, label = "Hard", xp = 15),
        GOOD(quality = 4, label = "Good", xp = 25),
        EASY(quality = 5, label = "Easy", xp = 35)
    }

    /**
     * Exact SM-2 algorithm implementation according to master specification:
     * if quality < 3:
     *     repetitions = 0
     *     interval = 1 day
     * else:
     *     repetitions += 1
     *     if repetitions == 1: interval = 1 day
     *     elif repetitions == 2: interval = 6 days
     *     else: interval = round(interval * ease_factor)
     *
     * ease_factor = max(1.3, ease_factor + (0.1 - (5-quality) * (0.08 + (5-quality)*0.02)))
     * next_review_date = today + interval
     * status = "mastered" if repetitions >= 5 and interval >= 21 else
     *          "learning" if repetitions >= 1 else "new"
     */
    fun calculateNextReview(current: ReviewStateEntity, grade: ReviewGrade): ReviewStateEntity {
        val quality = grade.quality
        var repetitions = current.repetitions
        var interval = current.intervalDays
        var easeFactor = current.easeFactor

        if (quality < 3) {
            repetitions = 0
            interval = 1
        } else {
            repetitions += 1
            interval = when (repetitions) {
                1 -> 1
                2 -> 6
                else -> Math.round(interval * easeFactor).toInt().coerceAtLeast(1)
            }
        }

        val qualityFactor = (5 - quality).toDouble()
        easeFactor = maxOf(
            1.3,
            easeFactor + (0.1 - qualityFactor * (0.08 + qualityFactor * 0.02))
        )

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, interval)
        val nextReviewDate = cal.timeInMillis

        val status = when {
            repetitions >= 5 && interval >= 21 -> "mastered"
            repetitions >= 1 -> "learning"
            else -> "new"
        }

        return current.copy(
            easeFactor = easeFactor,
            intervalDays = interval,
            repetitions = repetitions,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = todayStart,
            status = status
        )
    }

    fun parseSynonymShifts(jsonString: String): List<SynonymShiftItem> {
        if (jsonString.isBlank()) return emptyList()
        val list = mutableListOf<SynonymShiftItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SynonymShiftItem(
                        word = obj.optString("word", ""),
                        explanation = obj.optString("explanation", ""),
                        isTrapWord = obj.optBoolean("isTrapWord", i == 3)
                    )
                )
            }
        } catch (_: Exception) {
            // fallback: parse line separated if any
            val lines = jsonString.split("\n").filter { it.isNotBlank() }
            lines.forEachIndexed { index, line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    list.add(SynonymShiftItem(parts[0].trim(), parts[1].trim(), index == 3))
                } else {
                    list.add(SynonymShiftItem(line.trim(), "", index == 3))
                }
            }
        }
        return list
    }

    fun synonymShiftsToJson(items: List<SynonymShiftItem>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("word", item.word)
            obj.put("explanation", item.explanation)
            obj.put("isTrapWord", item.isTrapWord)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
