package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class ExamType(val displayName: String) {
    MCQ("MCQ Test"),
    WRITTEN("Written Test"),
    MIXED("Mixed Test")
}

@JsonClass(generateAdapter = true)
data class McqOption(
    @Json(name = "id") val id: String, // "A", "B", "C", "D"
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class McqQuestion(
    @Json(name = "id") val id: Int,
    @Json(name = "question") val question: String,
    @Json(name = "options") val options: List<McqOption>,
    @Json(name = "correctAnswer") val correctAnswer: String, // "A", "B", "C", or "D"
    @Json(name = "explanation") val explanation: String = "",
    @Json(name = "subject") val subject: String = "General",
    @Json(name = "topic") val topic: String = "General",
    @Json(name = "difficulty") val difficulty: String = "Medium"
)

@JsonClass(generateAdapter = true)
data class WrittenQuestion(
    @Json(name = "id") val id: Int,
    @Json(name = "question") val question: String,
    @Json(name = "marks") val marks: Int = 5,
    @Json(name = "suggestedAnswer") val suggestedAnswer: String = "",
    @Json(name = "keyPoints") val keyPoints: List<String> = emptyList(),
    @Json(name = "topic") val topic: String = "General",
    @Json(name = "difficulty") val difficulty: String = "Medium",
    @Json(name = "subject") val subject: String = "General"
)

@JsonClass(generateAdapter = true)
data class WrittenEvaluation(
    @Json(name = "questionId") val questionId: Int,
    @Json(name = "marksObtained") val marksObtained: Float = 0f,
    @Json(name = "maxMarks") val maxMarks: Int = 5,
    @Json(name = "percentage") val percentage: Float = 0f,
    @Json(name = "feedback") val feedback: String = "",
    @Json(name = "correctKeyPoints") val correctKeyPoints: List<String> = emptyList(),
    @Json(name = "missingKeyPoints") val missingKeyPoints: List<String> = emptyList(),
    @Json(name = "suggestedImprovement") val suggestedImprovement: String = ""
)

@JsonClass(generateAdapter = true)
data class WrittenTestEvaluationContainer(
    @Json(name = "evaluations") val evaluations: List<WrittenEvaluation> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeneratedQuiz(
    @Json(name = "title") val title: String = "AI Generated Practice Test",
    @Json(name = "examName") val examName: String = "General Practice",
    @Json(name = "subject") val subject: String = "General",
    @Json(name = "sourceTopic") val sourceTopic: String = "Study Material",
    @Json(name = "difficulty") val difficulty: String = "Medium",
    @Json(name = "examType") val examType: String = "MCQ", // MCQ, WRITTEN, MIXED
    @Json(name = "questions") val questions: List<McqQuestion> = emptyList(),
    @Json(name = "writtenQuestions") val writtenQuestions: List<WrittenQuestion> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WrittenTypeConfig(
    val enabled: Boolean = true,
    val count: Int = 3,
    val marksEach: Int = 2,
    val wordLimit: Int = 50
)

data class TestConfig(
    val examType: ExamType = ExamType.MCQ,
    val targetExam: String = "General Practice",
    val subject: String = "General",
    val topic: String = "General",
    val customInstruction: String = "",
    val questionCount: Int = 10,
    val mcqQuestionCount: Int = 5,
    val writtenQuestionCount: Int = 3,
    val shortWrittenConfig: WrittenTypeConfig = WrittenTypeConfig(enabled = true, count = 3, marksEach = 2, wordLimit = 50),
    val mediumWrittenConfig: WrittenTypeConfig = WrittenTypeConfig(enabled = false, count = 2, marksEach = 5, wordLimit = 100),
    val longWrittenConfig: WrittenTypeConfig = WrittenTypeConfig(enabled = false, count = 1, marksEach = 10, wordLimit = 250),
    val difficulty: String = "Medium", // Easy, Medium, Hard, Mixed
    val style: String = "Mixed", // Direct, Conceptual, Confusing Options, Statement Based, Match the Following, Exam Style, Mixed
    val language: String = "Hindi", // Hindi, English, Hindi + English
    val naturalPrompt: String = "",
    val strictSourceMode: Boolean = true,
    val timerModeMinutes: Int = 10, // 0 = No timer, >0 = Total time
    val negativeMarkingRatio: Float = 0.0f, // 0.0 (None), 0.25, 0.33, 0.50
    val marksPerQuestion: Int = 5, // 2, 5, 10, Custom
    val answerLengthType: String = "Medium", // Short, Medium, Long, Custom
    val wordLimit: Int = 200,
    val imageBase64List: List<String> = emptyList()
)

data class TestResult(
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val rawScore: Float,
    val maxScore: Float,
    val percentage: Float,
    val accuracyPercentage: Float,
    val gradeLabel: String,
    val timeTakenSeconds: Long,
    val examType: ExamType = ExamType.MCQ,
    val mcqScore: Float = 0f,
    val mcqMaxScore: Float = 0f,
    val writtenScore: Float = 0f,
    val writtenMaxScore: Float = 0f
)

// --- Study Notes Models ---
@JsonClass(generateAdapter = true)
data class GeneratedStudyNotes(
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String = "",
    @Json(name = "importantConcepts") val importantConcepts: List<String> = emptyList(),
    @Json(name = "keyDefinitions") val keyDefinitions: List<String> = emptyList(),
    @Json(name = "examPoints") val examPoints: List<String> = emptyList(),
    @Json(name = "examples") val examples: List<String> = emptyList(),
    @Json(name = "quickRevision") val quickRevision: List<String> = emptyList()
)

// --- Flashcard Models ---
@JsonClass(generateAdapter = true)
data class GeneratedFlashcardItem(
    @Json(name = "frontText") val frontText: String = "",
    @Json(name = "backText") val backText: String = ""
)

@JsonClass(generateAdapter = true)
data class GeneratedFlashcardSet(
    @Json(name = "subject") val subject: String = "",
    @Json(name = "topic") val topic: String = "",
    @Json(name = "flashcards") val flashcards: List<GeneratedFlashcardItem> = emptyList()
)

// --- AI Doubt Models ---
@JsonClass(generateAdapter = true)
data class GeneratedDoubtResponse(
    @Json(name = "directAnswer") val directAnswer: String = "",
    @Json(name = "simpleExplanation") val simpleExplanation: String = "",
    @Json(name = "detailedExplanation") val detailedExplanation: String = "",
    @Json(name = "examPoint") val examPoint: String = ""
)

@JsonClass(generateAdapter = true)
data class AppBackupData(
    @Json(name = "exportTimestamp") val exportTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "appVersion") val appVersion: String = "1.2.0",
    @Json(name = "questionBank") val questionBank: List<com.example.data.db.QuestionBankEntity> = emptyList(),
    @Json(name = "testRecords") val testRecords: List<com.example.data.db.TestRecordEntity> = emptyList(),
    @Json(name = "wrongQuestions") val wrongQuestions: List<com.example.data.db.WrongQuestionEntity> = emptyList(),
    @Json(name = "bookmarks") val bookmarks: List<com.example.data.db.BookmarkEntity> = emptyList(),
    @Json(name = "topicStats") val topicStats: List<com.example.data.db.TopicStatEntity> = emptyList(),
    @Json(name = "studyNotes") val studyNotes: List<com.example.data.db.StudyNoteEntity> = emptyList(),
    @Json(name = "flashcards") val flashcards: List<com.example.data.db.FlashcardEntity> = emptyList()
)
