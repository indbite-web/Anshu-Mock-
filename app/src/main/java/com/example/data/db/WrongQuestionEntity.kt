package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wrong_questions")
data class WrongQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,
    val userSelectedAnswer: String,
    val explanation: String,
    val topic: String,
    val difficulty: String,
    val timestamp: Long = System.currentTimeMillis(),
    val masteredTimes: Int = 0,
    val totalRetries: Int = 0,
    val isMastered: Boolean = false
)
