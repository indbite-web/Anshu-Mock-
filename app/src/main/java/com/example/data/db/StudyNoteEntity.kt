package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "study_notes")
@JsonClass(generateAdapter = true)
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topic: String,
    val title: String,
    val summary: String,
    val importantConceptsJson: String = "[]",
    val keyDefinitionsJson: String = "[]",
    val examPointsJson: String = "[]",
    val examplesJson: String = "[]",
    val quickRevisionJson: String = "[]",
    val customInstructions: String = "",
    val language: String = "English",
    val createdAt: Long = System.currentTimeMillis()
)
