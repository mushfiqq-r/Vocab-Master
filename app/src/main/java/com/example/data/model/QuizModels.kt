package com.example.data.model

enum class QuizType(val label: String, val description: String) {
    MIXED("Mixed Quiz", "Combination of definitions and sentence completions"),
    DEFINITION("Definition Match", "Identify the exact definition for target words"),
    SENTENCE_BLANK("Sentence Completion", "Fill in the blank with the appropriate word")
}

enum class QuizQuestionType {
    DEFINITION_MATCH,
    SENTENCE_COMPLETION
}

data class QuizOption(
    val text: String,
    val secondaryText: String? = null,
    val isCorrect: Boolean,
    val explanation: String
)

data class DistractorReason(
    val optionText: String,
    val reasonWhyWrong: String
)

data class QuizQuestion(
    val id: Int,
    val type: QuizQuestionType,
    val targetWord: WordEntity,
    val prompt: String,
    val sentenceWithBlank: String? = null,
    val options: List<QuizOption>,
    val correctIndex: Int,
    val correctNuanceExplanation: String,
    val distractorReasons: List<DistractorReason>,
    val memoryHook: String,
    var selectedOptionIndex: Int? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false
)

data class QuizSessionState(
    val isConfiguring: Boolean = true,
    val selectedBookFilter: String = "ALL", // "ALL", "gre_333", "ws1", "ws2"
    val selectedQuizType: QuizType = QuizType.MIXED,
    val questionCount: Int = 10,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val isFinished: Boolean = false,
    val score: Int = 0,
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val totalXpEarned: Int = 0,
    val missedQuestions: List<QuizQuestion> = emptyList()
)
