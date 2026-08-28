package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val term: String,
    val partOfSpeech: String,
    val banglaMeaning: String,
    val primaryMeaning: String,
    val exampleSentence: String,
    val preciseMeaning: String,
    val synonymShiftsJson: String, // Stored as JSON list of 4 items (3 close synonyms + 1 trap word)
    val memoryHook: String,
    val difficultyTier: Int, // 1: common, 2: moderate, 3: advanced
    val bookIds: String // Comma-separated: "gre_333,ws1,ws2"
)

@Entity(tableName = "review_states")
data class ReviewStateEntity(
    @PrimaryKey
    val wordId: Long,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewedDate: Long = 0L,
    val status: String = "new" // "new", "learning", "mastered"
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val xpTotal: Int = 0,
    val level: Int = 1,
    val dailyGoal: Int = 15,
    val lastActiveDate: Long = 0L
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey
    val dateString: String, // "yyyy-MM-dd"
    val wordsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val xpEarned: Int = 0,
    val goalMet: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class StreakMilestone(
    val daysRequired: Int,
    val title: String,
    val badgeName: String,
    val bonusXp: Int,
    val iconDescription: String
)

data class SynonymShiftItem(
    val word: String,
    val explanation: String,
    val isTrapWord: Boolean = false
)

data class WordWithReview(
    val word: WordEntity,
    val review: ReviewStateEntity?
)
