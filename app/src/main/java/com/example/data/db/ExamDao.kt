package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    // Test Records
    @Query("SELECT * FROM test_records ORDER BY createdAt DESC")
    fun getAllTestRecords(): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records ORDER BY createdAt DESC")
    suspend fun getAllTestRecordsList(): List<TestRecordEntity>

    @Query("SELECT * FROM test_records WHERE id = :id")
    suspend fun getTestRecordById(id: Long): TestRecordEntity?

    @Query("SELECT COUNT(*) FROM test_records WHERE createdAt >= :startTime")
    suspend fun getCompletedTestsCountSince(startTime: Long): Int

    @Query("SELECT COUNT(*) FROM test_records WHERE createdAt >= :startTime")
    fun getCompletedTestsCountSinceFlow(startTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestRecord(record: TestRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestRecords(records: List<TestRecordEntity>)

    @Query("DELETE FROM test_records WHERE id = :id")
    suspend fun deleteTestRecordById(id: Long)

    @Query("DELETE FROM test_records")
    suspend fun deleteAllTestRecords()

    // Wrong Questions
    @Query("SELECT * FROM wrong_questions WHERE isMastered = 0 ORDER BY timestamp DESC")
    fun getUnmasteredWrongQuestions(): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions ORDER BY timestamp DESC")
    suspend fun getUnmasteredWrongQuestionsList(): List<WrongQuestionEntity>

    @Query("SELECT * FROM wrong_questions WHERE topic = :topic AND isMastered = 0")
    suspend fun getWrongQuestionsByTopic(topic: String): List<WrongQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrongQuestion(question: WrongQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWrongQuestions(questions: List<WrongQuestionEntity>)

    @Update
    suspend fun updateWrongQuestion(question: WrongQuestionEntity)

    @Query("DELETE FROM wrong_questions WHERE id = :id")
    suspend fun deleteWrongQuestionById(id: Long)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    suspend fun getAllBookmarksList(): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionText = :questionText LIMIT 1)")
    suspend fun isBookmarked(questionText: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE questionText = :questionText")
    suspend fun deleteBookmarkByQuestionText(questionText: String)

    // Topic Stats
    @Query("SELECT * FROM topic_stats ORDER BY accuracyPercentage ASC")
    fun getAllTopicStats(): Flow<List<TopicStatEntity>>

    @Query("SELECT * FROM topic_stats ORDER BY accuracyPercentage ASC")
    suspend fun getAllTopicStatsList(): List<TopicStatEntity>

    @Query("SELECT * FROM topic_stats WHERE topicName = :topicName")
    suspend fun getTopicStatByName(topicName: String): TopicStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTopicStat(stat: TopicStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopicStats(stats: List<TopicStatEntity>)

    // Question Bank
    @Query("SELECT * FROM question_bank ORDER BY createdAt DESC")
    fun getAllQuestionBankItems(): Flow<List<QuestionBankEntity>>

    @Query("SELECT * FROM question_bank ORDER BY createdAt DESC")
    suspend fun getAllQuestionBankItemsList(): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE topic = :topic ORDER BY createdAt DESC")
    suspend fun getQuestionBankByTopic(topic: String): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE subject = :subject ORDER BY createdAt DESC")
    suspend fun getQuestionBankBySubject(subject: String): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE examName = :examName ORDER BY createdAt DESC")
    suspend fun getQuestionBankByExam(examName: String): List<QuestionBankEntity>

    @Query("SELECT * FROM question_bank WHERE difficulty = :difficulty ORDER BY createdAt DESC")
    suspend fun getQuestionBankByDifficulty(difficulty: String): List<QuestionBankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionBankItem(item: QuestionBankEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionBankItems(items: List<QuestionBankEntity>)

    @Update
    suspend fun updateQuestionBankItem(item: QuestionBankEntity)

    @Query("DELETE FROM question_bank WHERE id = :id")
    suspend fun deleteQuestionBankItemById(id: Long)

    @Query("DELETE FROM question_bank")
    suspend fun deleteAllQuestionBankItems()

    @Query("SELECT COUNT(*) FROM question_bank")
    suspend fun getQuestionBankCount(): Int

    // Study Notes
    @Query("SELECT * FROM study_notes ORDER BY createdAt DESC")
    fun getAllStudyNotes(): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes ORDER BY createdAt DESC")
    suspend fun getAllStudyNotesList(): List<StudyNoteEntity>

    @Query("SELECT * FROM study_notes WHERE id = :id")
    suspend fun getStudyNoteById(id: Long): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNotes(notes: List<StudyNoteEntity>)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteStudyNoteById(id: Long)

    @Query("DELETE FROM study_notes")
    suspend fun deleteAllStudyNotes()

    // Flashcards
    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    suspend fun getAllFlashcardsList(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)

    @Query("DELETE FROM flashcards WHERE subject = :subject AND topic = :topic")
    suspend fun deleteFlashcardsBySubjectAndTopic(subject: String, topic: String)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()
}
