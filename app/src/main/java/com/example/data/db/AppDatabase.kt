package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TestRecordEntity::class,
        WrongQuestionEntity::class,
        BookmarkEntity::class,
        TopicStatEntity::class,
        QuestionBankEntity::class,
        StudyNoteEntity::class,
        FlashcardEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `question_bank` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `questionText` TEXT NOT NULL,
                        `optionA` TEXT NOT NULL,
                        `optionB` TEXT NOT NULL,
                        `optionC` TEXT NOT NULL,
                        `optionD` TEXT NOT NULL,
                        `correctAnswer` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `testSourceRef` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `examName` TEXT NOT NULL DEFAULT 'General Practice'")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `subject` TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `masteryState` TEXT NOT NULL DEFAULT 'New'")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `timesAnswered` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `timesCorrect` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `test_records` ADD COLUMN `timerLimitMinutes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `test_records` ADD COLUMN `autoSubmitted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `test_records` ADD COLUMN `examType` TEXT NOT NULL DEFAULT 'MCQ'")
                db.execSQL("ALTER TABLE `test_records` ADD COLUMN `writtenAnswersJson` TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE `test_records` ADD COLUMN `evaluationsJson` TEXT NOT NULL DEFAULT '{}'")

                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `questionType` TEXT NOT NULL DEFAULT 'MCQ'")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `suggestedAnswer` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `keyPointsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `question_bank` ADD COLUMN `marks` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `importantConceptsJson` TEXT NOT NULL DEFAULT '[]',
                        `keyDefinitionsJson` TEXT NOT NULL DEFAULT '[]',
                        `examPointsJson` TEXT NOT NULL DEFAULT '[]',
                        `examplesJson` TEXT NOT NULL DEFAULT '[]',
                        `quickRevisionJson` TEXT NOT NULL DEFAULT '[]',
                        `customInstructions` TEXT NOT NULL DEFAULT '',
                        `language` TEXT NOT NULL DEFAULT 'English',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `topic` TEXT NOT NULL,
                        `frontText` TEXT NOT NULL,
                        `backText` TEXT NOT NULL,
                        `masteryState` TEXT NOT NULL DEFAULT 'New',
                        `timesReviewed` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `study_notes` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'English'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anshu_exam_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
