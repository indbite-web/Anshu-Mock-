package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_records")
data class TestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceTopic: String,
    val difficulty: String,
    val questionCount: Int,
    val score: Float,
    val maxScore: Float,
    val correctCount: Int,
    val incorrectCount: Int,
    val unattemptedCount: Int,
    val accuracyPercentage: Float,
    val timeTakenSeconds: Long,
    val modelUsed: String,
    val createdAt: Long = System.currentTimeMillis(),
    val questionsJson: String, // Full JSON representation for re-opening
    val timerLimitMinutes: Int = 0,
    val autoSubmitted: Boolean = false,
    val examType: String = "MCQ",
    val writtenAnswersJson: String = "{}",
    val evaluationsJson: String = "{}"
)
