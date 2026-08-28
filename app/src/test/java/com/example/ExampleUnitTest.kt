package com.example

import com.example.data.model.ReviewStateEntity
import com.example.data.seed.VocabSeedData
import com.example.data.util.SM2Algorithm
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSeedWordsCompleteness() {
        val words = VocabSeedData.getAllSeedWords()
        assertTrue("Seed word list should contain rich vocabulary", words.isNotEmpty())
        
        words.forEach { word ->
            assertFalse("Word term must not be blank", word.term.isBlank())
            assertFalse("Bangla gloss must not be blank", word.banglaMeaning.isBlank())
            assertFalse("Example sentence must not be blank", word.exampleSentence.isBlank())
            assertFalse("Precise meaning must not be blank", word.preciseMeaning.isBlank())
            assertTrue("Word must belong to at least one book", word.bookIds.isNotEmpty())
            
            val shifts = SM2Algorithm.parseSynonymShifts(word.synonymShiftsJson)
            assertEquals("Word ${word.term} must have exactly 4 synonym shift points", 4, shifts.size)
            assertTrue("Word ${word.term} must contain a trap word", shifts.any { it.isTrapWord })
        }
    }

    @Test
    fun testSM2AlgorithmProgression() {
        var review = ReviewStateEntity(wordId = 1L)

        // First review: Grade EASY (5)
        review = SM2Algorithm.calculateNextReview(review, SM2Algorithm.ReviewGrade.EASY)
        assertEquals(1, review.repetitions)
        assertEquals(1, review.intervalDays)
        assertEquals("learning", review.status)

        // Second review: Grade GOOD (4)
        review = SM2Algorithm.calculateNextReview(review, SM2Algorithm.ReviewGrade.GOOD)
        assertEquals(2, review.repetitions)
        assertEquals(6, review.intervalDays)

        // Third review: Grade EASY (5)
        review = SM2Algorithm.calculateNextReview(review, SM2Algorithm.ReviewGrade.EASY)
        assertEquals(3, review.repetitions)
        assertTrue("Interval should compound with ease factor", review.intervalDays > 6)

        // Fail review: Grade AGAIN (1)
        review = SM2Algorithm.calculateNextReview(review, SM2Algorithm.ReviewGrade.AGAIN)
        assertEquals(0, review.repetitions)
        assertEquals(1, review.intervalDays)
        assertEquals("learning", review.status)
    }
}
