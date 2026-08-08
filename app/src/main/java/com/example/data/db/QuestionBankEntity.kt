package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_bank")
data class QuestionBankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val explanation: String,
    val topic: String,
    val difficulty: String,
    val testSourceRef: String,
    val examName: String = "General Practice",
    val subject: String = "General",
    val masteryState: String = "New", // New, Learning, Weak, Improving, Mastered
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val questionType: String = "MCQ", // MCQ or WRITTEN
    val suggestedAnswer: String = "",
    val keyPointsJson: String = "[]",
    val marks: Int = 1
)

