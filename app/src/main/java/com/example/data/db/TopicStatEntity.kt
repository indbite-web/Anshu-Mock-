package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topic_stats")
data class TopicStatEntity(
    @PrimaryKey val topicName: String,
    val totalAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val accuracyPercentage: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
