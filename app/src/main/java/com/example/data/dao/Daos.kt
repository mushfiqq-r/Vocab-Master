package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY term ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE term LIKE '%' || :query || '%' OR banglaMeaning LIKE '%' || :query || '%' OR primaryMeaning LIKE '%' || :query || '%' ORDER BY term ASC")
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE bookIds LIKE '%' || :bookId || '%' ORDER BY term ASC")
    fun getWordsByBook(bookId: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_states")
    fun getAllReviewStates(): Flow<List<ReviewStateEntity>>

    @Query("SELECT * FROM review_states WHERE wordId = :wordId LIMIT 1")
    suspend fun getReviewStateForWord(wordId: Long): ReviewStateEntity?

    @Query("SELECT * FROM review_states WHERE nextReviewDate <= :timestamp OR status = 'new'")
    suspend fun getDueReviewStates(timestamp: Long): List<ReviewStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reviewState: ReviewStateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(reviewStates: List<ReviewStateEntity>)

    @Query("SELECT COUNT(*) FROM review_states WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsDirect(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)
}
