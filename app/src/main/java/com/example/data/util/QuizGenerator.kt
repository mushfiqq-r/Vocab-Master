package com.example.data.util

import com.example.data.model.DistractorReason
import com.example.data.model.QuizOption
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizQuestionType
import com.example.data.model.QuizType
import com.example.data.model.WordEntity
import java.util.regex.Pattern

object QuizGenerator {

    fun generateQuiz(
        allWords: List<WordEntity>,
        bookFilter: String = "ALL",
        quizType: QuizType = QuizType.MIXED,
        questionCount: Int = 10
    ): List<QuizQuestion> {
        if (allWords.isEmpty()) return emptyList()

        val candidateWords = if (bookFilter == "ALL") {
            allWords
        } else {
            val filtered = allWords.filter { it.bookIds.contains(bookFilter) }
            if (filtered.size >= 4) filtered else allWords
        }

        val shuffledCandidates = candidateWords.shuffled()
        val count = minOf(questionCount, shuffledCandidates.size)
        val selectedWords = shuffledCandidates.take(count)

        val questions = mutableListOf<QuizQuestion>()

        selectedWords.forEachIndexed { index, targetWord ->
            val qType = when (quizType) {
                QuizType.DEFINITION -> QuizQuestionType.DEFINITION_MATCH
                QuizType.SENTENCE_BLANK -> QuizQuestionType.SENTENCE_COMPLETION
                QuizType.MIXED -> if (index % 2 == 0) QuizQuestionType.DEFINITION_MATCH else QuizQuestionType.SENTENCE_COMPLETION
            }

            val question = when (qType) {
                QuizQuestionType.DEFINITION_MATCH -> generateDefinitionQuestion(
                    index = index + 1,
                    targetWord = targetWord,
                    allPool = allWords
                )
                QuizQuestionType.SENTENCE_COMPLETION -> generateSentenceQuestion(
                    index = index + 1,
                    targetWord = targetWord,
                    allPool = allWords
                )
            }
            questions.add(question)
        }

        return questions
    }

    private fun generateDefinitionQuestion(
        index: Int,
        targetWord: WordEntity,
        allPool: List<WordEntity>
    ): QuizQuestion {
        val synonymShifts = SM2Algorithm.parseSynonymShifts(targetWord.synonymShiftsJson)
        val trapShift = synonymShifts.firstOrNull { it.isTrapWord }

        // Find distractor words from other words (prefer matching part of speech)
        val otherWords = allPool.filter { it.id != targetWord.id && it.primaryMeaning != targetWord.primaryMeaning }
        val matchingPosWords = otherWords.filter { it.partOfSpeech.equals(targetWord.partOfSpeech, ignoreCase = true) }
        val distractorPool = if (matchingPosWords.size >= 3) matchingPosWords else otherWords

        val distractorWords = distractorPool.shuffled().take(3)

        val correctOption = QuizOption(
            text = targetWord.primaryMeaning,
            secondaryText = targetWord.banglaMeaning,
            isCorrect = true,
            explanation = "✓ Correct! \"${targetWord.term}\" (${targetWord.partOfSpeech}) means: ${targetWord.primaryMeaning}"
        )

        val distractorOptions = mutableListOf<QuizOption>()
        val distractorReasons = mutableListOf<DistractorReason>()

        distractorWords.forEach { distractor ->
            val explanation = "✗ Incorrect: This is the definition of \"${distractor.term}\" (${distractor.partOfSpeech}). \"${targetWord.term}\" specifically denotes: ${targetWord.primaryMeaning}."
            distractorOptions.add(
                QuizOption(
                    text = distractor.primaryMeaning,
                    secondaryText = distractor.banglaMeaning,
                    isCorrect = false,
                    explanation = explanation
                )
            )
            distractorReasons.add(
                DistractorReason(
                    optionText = distractor.primaryMeaning,
                    reasonWhyWrong = "Corresponds to \"${distractor.term}\" (${distractor.banglaMeaning}). Differs because ${targetWord.term} implies ${targetWord.preciseMeaning.take(120)}..."
                )
            )
        }

        val allOptions = (listOf(correctOption) + distractorOptions).shuffled()
        val correctIndex = allOptions.indexOfFirst { it.isCorrect }

        val correctNuance = buildString {
            append("• Exact Definition: ${targetWord.primaryMeaning}\n")
            append("• Bangla Meaning: ${targetWord.banglaMeaning}\n")
            append("• Nuance & Register: ${targetWord.preciseMeaning}\n")
            if (trapShift != null) {
                append("• Contrast with Trap Word (\"${trapShift.word}\"): ${trapShift.explanation}")
            }
        }

        return QuizQuestion(
            id = index,
            type = QuizQuestionType.DEFINITION_MATCH,
            targetWord = targetWord,
            prompt = "Select the precise definition for \"${targetWord.term}\" (${targetWord.partOfSpeech}):",
            sentenceWithBlank = null,
            options = allOptions,
            correctIndex = correctIndex,
            correctNuanceExplanation = correctNuance,
            distractorReasons = distractorReasons,
            memoryHook = targetWord.memoryHook
        )
    }

    private fun generateSentenceQuestion(
        index: Int,
        targetWord: WordEntity,
        allPool: List<WordEntity>
    ): QuizQuestion {
        val originalSentence = targetWord.exampleSentence
        val sentenceWithBlank = createBlankInSentence(originalSentence, targetWord.term)

        val synonymShifts = SM2Algorithm.parseSynonymShifts(targetWord.synonymShiftsJson)
        val closeSynonym = synonymShifts.firstOrNull { !it.isTrapWord }
        val trapWord = synonymShifts.firstOrNull { it.isTrapWord }

        val otherWords = allPool.filter { it.id != targetWord.id && !it.term.equals(targetWord.term, ignoreCase = true) }
        val matchingPosWords = otherWords.filter { it.partOfSpeech.equals(targetWord.partOfSpeech, ignoreCase = true) }
        val distractorPool = if (matchingPosWords.size >= 3) matchingPosWords else otherWords

        val correctOption = QuizOption(
            text = targetWord.term,
            secondaryText = targetWord.partOfSpeech,
            isCorrect = true,
            explanation = "✓ Correct! \"${targetWord.term}\" (${targetWord.partOfSpeech}) fits the exact context: ${targetWord.primaryMeaning}."
        )

        val distractorOptions = mutableListOf<QuizOption>()
        val distractorReasons = mutableListOf<DistractorReason>()

        // Add close synonym distractor if present
        if (closeSynonym != null) {
            val reason = "While related, \"${closeSynonym.word}\" is suboptimal here: ${closeSynonym.explanation}"
            distractorOptions.add(
                QuizOption(
                    text = closeSynonym.word,
                    secondaryText = "Synonym Shift",
                    isCorrect = false,
                    explanation = "✗ Nuance Trap: \"${closeSynonym.word}\" ${closeSynonym.explanation}"
                )
            )
            distractorReasons.add(
                DistractorReason(
                    optionText = closeSynonym.word,
                    reasonWhyWrong = reason
                )
            )
        }

        // Add trap word distractor if present
        if (trapWord != null && distractorOptions.size < 3) {
            val reason = "\"${trapWord.word}\" is a trap word: ${trapWord.explanation}"
            distractorOptions.add(
                QuizOption(
                    text = trapWord.word,
                    secondaryText = "Trap / Antonym",
                    isCorrect = false,
                    explanation = "✗ Opposite / Trap: \"${trapWord.word}\" ${trapWord.explanation}"
                )
            )
            distractorReasons.add(
                DistractorReason(
                    optionText = trapWord.word,
                    reasonWhyWrong = reason
                )
            )
        }

        // Fill remaining distractors from random words in pool
        val remainingNeeded = 3 - distractorOptions.size
        val randomWords = distractorPool
            .filter { w -> distractorOptions.none { opt -> opt.text.equals(w.term, ignoreCase = true) } }
            .shuffled()
            .take(remainingNeeded)

        randomWords.forEach { rw ->
            val reason = "\"${rw.term}\" means \"${rw.primaryMeaning}\" (${rw.banglaMeaning}), which does not convey the required meaning in this sentence."
            distractorOptions.add(
                QuizOption(
                    text = rw.term,
                    secondaryText = rw.partOfSpeech,
                    isCorrect = false,
                    explanation = "✗ Incorrect: \"${rw.term}\" means ${rw.primaryMeaning}."
                )
            )
            distractorReasons.add(
                DistractorReason(
                    optionText = rw.term,
                    reasonWhyWrong = reason
                )
            )
        }

        val allOptions = (listOf(correctOption) + distractorOptions).shuffled()
        val correctIndex = allOptions.indexOfFirst { it.isCorrect }

        val correctNuance = buildString {
            append("• Target Word: \"${targetWord.term}\" (${targetWord.partOfSpeech})\n")
            append("• Meaning in Context: ${targetWord.primaryMeaning}\n")
            append("• Linguistic Nuance: ${targetWord.preciseMeaning}\n")
            append("• Complete Sentence: \"${targetWord.exampleSentence}\"")
        }

        return QuizQuestion(
            id = index,
            type = QuizQuestionType.SENTENCE_COMPLETION,
            targetWord = targetWord,
            prompt = "Complete the sentence with the most appropriate vocabulary word:",
            sentenceWithBlank = sentenceWithBlank,
            options = allOptions,
            correctIndex = correctIndex,
            correctNuanceExplanation = correctNuance,
            distractorReasons = distractorReasons,
            memoryHook = targetWord.memoryHook
        )
    }

    private fun createBlankInSentence(sentence: String, term: String): String {
        // Strip common suffixes or match case-insensitively
        val cleanTerm = term.trim()
        val regex = Pattern.compile("(?i)\\b" + Pattern.quote(cleanTerm) + "(?:s|ed|ing|es|d|ly)?\\b")
        val matcher = regex.matcher(sentence)
        return if (matcher.find()) {
            matcher.replaceAll("________")
        } else {
            // Fallback case-insensitive replace
            val idx = sentence.indexOf(cleanTerm, ignoreCase = true)
            if (idx != -1) {
                sentence.substring(0, idx) + "________" + sentence.substring(idx + cleanTerm.length)
            } else {
                "$sentence (Fill in: ________)"
            }
        }
    }
}
