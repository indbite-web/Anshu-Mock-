package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "flashcards")
@JsonClass(generateAdapter = true)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topic: String,
    val frontText: String,
    val backText: String,
    val masteryState: String = "New", // New, Learning, Known
    val timesReviewed: Int = 0,
    val language: String = "English",
    val createdAt: Long = System.currentTimeMillis()
)
