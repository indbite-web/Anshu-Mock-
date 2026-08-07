package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
data class GeneratedQuiz(
    @Json(name = "title") val title: String = "AI Generated Practice Test",
    @Json(name = "examName") val examName: String = "General Practice",
    @Json(name = "subject") val subject: String = "General",
    @Json(name = "sourceTopic") val sourceTopic: String = "Study Material",
    @Json(name = "difficulty") val difficulty: String = "Medium",
    @Json(name = "questions") val questions: List<McqQuestion>
)

data class TestConfig(
    val targetExam: String = "General Practice",
    val subject: String = "General",
    val topic: String = "General",
    val customInstruction: String = "",
    val questionCount: Int = 10,
    val difficulty: String = "Medium", // Easy, Medium, Hard, Very Hard
    val style: String = "Mixed", // Direct, Conceptual, Confusing Options, Statement Based, Match the Following, Exam Style, Mixed
    val language: String = "Hindi", // Hindi, English, Hindi + English
    val naturalPrompt: String = "",
    val strictSourceMode: Boolean = true,
    val timerModeMinutes: Int = 10, // 0 = No timer, >0 = Total time
    val negativeMarkingRatio: Float = 0.25f, // 0.0, 0.25, 0.33, 0.50
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
    val timeTakenSeconds: Long
)

@JsonClass(generateAdapter = true)
data class AppBackupData(
    @Json(name = "exportTimestamp") val exportTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "appVersion") val appVersion: String = "1.0",
    @Json(name = "questionBank") val questionBank: List<com.example.data.db.QuestionBankEntity> = emptyList(),
    @Json(name = "testRecords") val testRecords: List<com.example.data.db.TestRecordEntity> = emptyList(),
    @Json(name = "wrongQuestions") val wrongQuestions: List<com.example.data.db.WrongQuestionEntity> = emptyList(),
    @Json(name = "bookmarks") val bookmarks: List<com.example.data.db.BookmarkEntity> = emptyList(),
    @Json(name = "topicStats") val topicStats: List<com.example.data.db.TopicStatEntity> = emptyList()
)
