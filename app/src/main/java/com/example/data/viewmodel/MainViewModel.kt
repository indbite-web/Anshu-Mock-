package com.example.data.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.TestRecordEntity
import com.example.data.db.TopicStatEntity
import com.example.data.db.WrongQuestionEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.SupportedModel
import com.example.data.repository.ExamRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.model.GeneratedQuiz
import com.example.model.McqQuestion
import com.example.model.TestConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class QuizUiState {
    object Idle : QuizUiState()
    data class Generating(val statusMessage: String) : QuizUiState()
    data class Active(
        val quiz: GeneratedQuiz,
        val config: TestConfig,
        val modelUsed: String,
        val selectedModel: String = "",
        val wasFallback: Boolean = false
    ) : QuizUiState()
    data class Result(
        val record: TestRecordEntity,
        val quiz: GeneratedQuiz,
        val userAnswers: Map<Int, String>
    ) : QuizUiState()
    data class Error(val message: String) : QuizUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val examRepo = ExamRepository(application)
    val prefsRepo = UserPreferencesRepository(application)

    val userApiKey: StateFlow<String> = prefsRepo.userApiKey
    val onboardingCompleted: StateFlow<Boolean> = prefsRepo.onboardingCompleted
    val displayName: StateFlow<String> = prefsRepo.displayName
    val profileImageUri: StateFlow<String> = prefsRepo.profileImageUri
    val primaryExam: StateFlow<String> = prefsRepo.preferredExam
    val additionalExams: StateFlow<String> = prefsRepo.additionalExams
    val preferredLanguage: StateFlow<String> = prefsRepo.defaultLanguage
    val dailyGoalTarget: StateFlow<Int> = prefsRepo.dailyGoalTarget

    // Study Reminders State
    val studyRemindersEnabled: StateFlow<Boolean> = prefsRepo.studyRemindersEnabled
    val reminderIntervalHours: StateFlow<Int> = prefsRepo.reminderIntervalHours
    val quietHoursStartHour: StateFlow<Int> = prefsRepo.quietHoursStartHour
    val quietHoursStartMinute: StateFlow<Int> = prefsRepo.quietHoursStartMinute
    val quietHoursEndHour: StateFlow<Int> = prefsRepo.quietHoursEndHour
    val quietHoursEndMinute: StateFlow<Int> = prefsRepo.quietHoursEndMinute

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val completedTestsToday: StateFlow<Int> = examRepo.dao.getCompletedTestsCountSinceFlow(getStartOfDayTimestamp())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun toggleStudyReminders(enabled: Boolean) {
        prefsRepo.setStudyRemindersEnabled(enabled)
        if (enabled) {
            com.example.worker.StudyReminderScheduler.scheduleReminder(
                getApplication(),
                prefsRepo.getReminderIntervalHours()
            )
        } else {
            com.example.worker.StudyReminderScheduler.cancelReminder(getApplication())
        }
    }

    fun setReminderIntervalHours(hours: Int) {
        prefsRepo.setReminderIntervalHours(hours)
        if (prefsRepo.isStudyRemindersEnabled()) {
            com.example.worker.StudyReminderScheduler.scheduleReminder(getApplication(), hours)
        }
    }

    fun setQuietHours(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        prefsRepo.setQuietHours(startHour, startMin, endHour, endMin)
    }

    fun completeOnboarding(
        name: String,
        primaryExam: String,
        additionalExams: String,
        language: String,
        dailyTarget: Int
    ) {
        prefsRepo.setDisplayName(name)
        prefsRepo.setPreferredExam(if (primaryExam.isBlank()) "General Practice" else primaryExam)
        prefsRepo.setAdditionalExams(additionalExams)
        prefsRepo.setDefaultLanguage(if (language.isBlank()) "Hindi" else language)
        if (dailyTarget > 0) {
            prefsRepo.setDailyGoalTarget(dailyTarget)
        }
        prefsRepo.setOnboardingCompleted(true)
    }

    fun updateProfile(
        name: String,
        primaryExam: String,
        additionalExams: String,
        language: String,
        dailyTarget: Int
    ) {
        prefsRepo.setDisplayName(name)
        prefsRepo.setPreferredExam(if (primaryExam.isBlank()) "General Practice" else primaryExam)
        prefsRepo.setAdditionalExams(additionalExams)
        prefsRepo.setDefaultLanguage(if (language.isBlank()) "Hindi" else language)
        if (dailyTarget > 0) {
            prefsRepo.setDailyGoalTarget(dailyTarget)
        }
    }

    fun rerunOnboarding() {
        prefsRepo.setOnboardingCompleted(false)
    }

    fun resetProfile() {
        viewModelScope.launch {
            com.example.data.util.ProfileImageManager.deleteProfileImage(getApplication())
            prefsRepo.clearProfileImageUri()
            prefsRepo.resetProfile()
        }
    }

    fun saveProfileImage(sourceUri: Uri, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val savedPath = com.example.data.util.ProfileImageManager.saveUriToInternalStorage(
                getApplication(),
                sourceUri
            )
            if (savedPath != null) {
                prefsRepo.setProfileImageUri(savedPath)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun removeProfileImage(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val deleted = com.example.data.util.ProfileImageManager.deleteProfileImage(getApplication())
            prefsRepo.clearProfileImageUri()
            onResult(deleted)
        }
    }

    fun createCameraPictureUri(): Uri? {
        return com.example.data.util.ProfileImageManager.createCameraTempUri(getApplication())
    }

    // Flow State Observers
    val testHistory: StateFlow<List<TestRecordEntity>> = examRepo.testHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentStreak: StateFlow<Int> = testHistory.map { history ->
        calculateStreak(history)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val wrongQuestions: StateFlow<List<WrongQuestionEntity>> = examRepo.wrongQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookmarkedQuestions: StateFlow<List<BookmarkEntity>> = examRepo.bookmarkedQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topicStats: StateFlow<List<TopicStatEntity>> = examRepo.topicStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val questionBankItems: StateFlow<List<com.example.data.db.QuestionBankEntity>> = examRepo.questionBankItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveApiKey(key: String) {
        prefsRepo.saveUserApiKey(key)
    }

    fun removeApiKey() {
        prefsRepo.removeUserApiKey()
    }

    private fun calculateStreak(records: List<TestRecordEntity>): Int {
        if (records.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val testDateSet = records.mapTo(HashSet()) { sdf.format(Date(it.createdAt)) }
        if (testDateSet.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val hasToday = testDateSet.contains(todayStr)
        val hasYesterday = testDateSet.contains(yesterdayStr)

        if (!hasToday && !hasYesterday) {
            return 0
        }

        var streak = 0
        var checkCal = Calendar.getInstance()
        if (!hasToday && hasYesterday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val targetStr = sdf.format(checkCal.time)
            if (testDateSet.contains(targetStr)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    // Active Quiz Execution State
    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    // Test Creation Form State
    private val _selectedImages = MutableStateFlow<List<Bitmap>>(emptyList())
    val selectedImages: StateFlow<List<Bitmap>> = _selectedImages.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()

    private val _markedForReview = MutableStateFlow<Set<Int>>(emptySet())
    val markedForReview: StateFlow<Set<Int>> = _markedForReview.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Timer State
    private val _timeRemainingSeconds = MutableStateFlow(0L)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var testStartTimeSeconds = 0L

    // Connection Test State
    private val _connectionStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val connectionStatus: StateFlow<Pair<Boolean, String>?> = _connectionStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    fun addImagesFromUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            val newBitmaps = mutableListOf<Bitmap>()
            uris.forEach { uri ->
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            newBitmaps.add(scaleDown(bitmap, 1200f))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (newBitmaps.isNotEmpty()) {
                _selectedImages.value = _selectedImages.value + newBitmaps
            }
        }
    }

    fun removeImage(index: Int) {
        val list = _selectedImages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _selectedImages.value = list
        }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
    }

    private fun scaleDown(realImage: Bitmap, maxImageSize: Float): Bitmap {
        val ratio = Math.min(
            maxImageSize / realImage.width,
            maxImageSize / realImage.height
        )
        if (ratio >= 1.0) return realImage

        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)

        return Bitmap.createScaledBitmap(realImage, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private var lastTestConfig: TestConfig? = null

    fun retryLastTest() {
        val config = lastTestConfig
        if (config != null) {
            startNewTest(config)
        } else {
            _quizState.value = QuizUiState.Error("No previous test configuration found to retry.")
        }
    }

    fun startNewTest(config: TestConfig) {
        lastTestConfig = config
        viewModelScope.launch {
            _quizState.value = QuizUiState.Generating("Analyzing study material...")
            try {
                val apiKey = prefsRepo.getEffectiveApiKey()
                if (apiKey.isBlank()) {
                    _quizState.value = QuizUiState.Error("Gemini API key required. Please configure your API key in Settings → AI Configuration.")
                    return@launch
                }

                val b64List = withContext(Dispatchers.Default) {
                    _selectedImages.value.map { bitmapToBase64(it) }
                }
                val finalConfig = config.copy(imageBase64List = b64List)

                val result = examRepo.generateQuiz(
                    config = finalConfig,
                    preferredModelId = prefsRepo.selectedModel.value,
                    autoFallback = prefsRepo.autoFallbackEnabled.value,
                    apiKey = apiKey,
                    onStatusUpdate = { status ->
                        _quizState.value = QuizUiState.Generating(status)
                    }
                )

                _userAnswers.value = emptyMap()
                _markedForReview.value = emptySet()
                _currentQuestionIndex.value = 0

                // Setup Timer
                val timerMinutes = finalConfig.timerModeMinutes
                testStartTimeSeconds = System.currentTimeMillis() / 1000

                _quizState.value = QuizUiState.Active(
                    quiz = result.quiz,
                    config = finalConfig,
                    modelUsed = result.actualModelUsed,
                    selectedModel = result.selectedModel,
                    wasFallback = result.wasFallback
                )

                startTimer(timerMinutes)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Test generation error", e)
                val msg = e.localizedMessage ?: "Failed to generate practice test. Please try again."
                _quizState.value = QuizUiState.Error(msg)
            }
        }
    }

    private var testEndTimeMs: Long = 0L

    private fun startTimer(timerMinutes: Int) {
        timerJob?.cancel()
        if (timerMinutes <= 0) {
            _timeRemainingSeconds.value = 0L
            return
        }

        val durationMs = timerMinutes * 60 * 1000L
        testEndTimeMs = System.currentTimeMillis() + durationMs
        _timeRemainingSeconds.value = (timerMinutes * 60).toLong()

        timerJob = viewModelScope.launch {
            while (isActive && _quizState.value is QuizUiState.Active) {
                val now = System.currentTimeMillis()
                val remainingMs = testEndTimeMs - now
                val remainingSec = maxOf(0L, (remainingMs + 999) / 1000)

                _timeRemainingSeconds.value = remainingSec

                if (remainingMs <= 0) {
                    submitTest(fromTimer = true)
                    break
                }
                delay(500)
            }
        }
    }

    fun selectAnswer(questionId: Int, optionId: String) {
        val current = _userAnswers.value.toMutableMap()
        current[questionId] = optionId
        _userAnswers.value = current
    }

    fun clearAnswer(questionId: Int) {
        val current = _userAnswers.value.toMutableMap()
        current.remove(questionId)
        _userAnswers.value = current
    }

    fun toggleMarkForReview(questionId: Int) {
        val current = _markedForReview.value.toMutableSet()
        if (current.contains(questionId)) {
            current.remove(questionId)
        } else {
            current.add(questionId)
        }
        _markedForReview.value = current
    }

    fun setCurrentQuestionIndex(index: Int) {
        _currentQuestionIndex.value = index
    }

    fun submitTest(fromTimer: Boolean = false) {
        timerJob?.cancel()
        val activeState = _quizState.value as? QuizUiState.Active ?: return

        viewModelScope.launch {
            val nowSeconds = System.currentTimeMillis() / 1000
            val timeTaken = maxOf(1L, nowSeconds - testStartTimeSeconds)

            val modelRecordString = if (activeState.wasFallback) {
                "${activeState.modelUsed} (Fallback from ${activeState.selectedModel})"
            } else {
                activeState.modelUsed
            }

            val record = examRepo.saveTestResult(
                quiz = activeState.quiz,
                userAnswers = _userAnswers.value,
                timeTakenSeconds = timeTaken,
                modelUsed = modelRecordString,
                negativeMarkingRatio = activeState.config.negativeMarkingRatio,
                timerLimitMinutes = activeState.config.timerModeMinutes,
                autoSubmitted = fromTimer
            )

            _quizState.value = QuizUiState.Result(
                record = record,
                quiz = activeState.quiz,
                userAnswers = _userAnswers.value
            )
        }
    }

    fun reopenTestRecord(record: TestRecordEntity) {
        viewModelScope.launch {
            try {
                val moshi: Moshi = GeminiClient.moshi
                val adapter = moshi.adapter(GeneratedQuiz::class.java)
                val quiz = adapter.fromJson(record.questionsJson)
                if (quiz != null) {
                    _quizState.value = QuizUiState.Result(
                        record = record,
                        quiz = quiz,
                        userAnswers = emptyMap()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleBookmark(question: McqQuestion) {
        viewModelScope.launch {
            examRepo.toggleBookmark(question)
        }
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            val effectiveKey = prefsRepo.getEffectiveApiKey()
            val model = SupportedModel.fromModelId(prefsRepo.selectedModel.value)
            val result = GeminiClient.testConnection(
                apiKey = effectiveKey,
                model = model
            )
            _connectionStatus.value = result
            _isTestingConnection.value = false
        }
    }

    fun resetQuizState() {
        timerJob?.cancel()
        _quizState.value = QuizUiState.Idle
    }

    fun deleteTestRecord(id: Long) {
        viewModelScope.launch {
            examRepo.deleteTestRecord(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            examRepo.clearAllHistory()
        }
    }

    fun markWrongQuestionMastered(id: Long) {
        viewModelScope.launch {
            examRepo.markWrongQuestionCorrect(id)
        }
    }

    fun startQuestionBankTest(
        requestedCount: Int = 10,
        topicFilter: String? = null,
        subjectFilter: String? = null,
        examFilter: String? = null
    ) {
        viewModelScope.launch {
            _quizState.value = QuizUiState.Generating("Loading questions from Question Bank...")
            try {
                val quiz = examRepo.createTestFromQuestionBank(
                    requestedCount = requestedCount,
                    topicFilter = topicFilter,
                    subjectFilter = subjectFilter,
                    examFilter = examFilter
                )
                val config = TestConfig(
                    targetExam = quiz.examName,
                    subject = quiz.subject,
                    topic = quiz.sourceTopic,
                    questionCount = quiz.questions.size,
                    difficulty = quiz.difficulty,
                    timerModeMinutes = maxOf(5, quiz.questions.size / 2)
                )

                _userAnswers.value = emptyMap()
                _markedForReview.value = emptySet()
                _currentQuestionIndex.value = 0

                testStartTimeSeconds = System.currentTimeMillis() / 1000

                _quizState.value = QuizUiState.Active(
                    quiz = quiz,
                    config = config,
                    modelUsed = "Local Question Bank (Offline)"
                )

                startTimer(config.timerModeMinutes)
            } catch (e: Exception) {
                _quizState.value = QuizUiState.Error(e.localizedMessage ?: "Failed to start test from Question Bank")
            }
        }
    }

    fun startCustomComposedTest(subjectCounts: Map<String, Int>) {
        viewModelScope.launch {
            _quizState.value = QuizUiState.Generating("Building custom test from Question Bank...")
            try {
                val quiz = examRepo.createCustomComposedTestFromBank(subjectCounts)
                val config = TestConfig(
                    targetExam = quiz.examName,
                    subject = quiz.subject,
                    topic = quiz.sourceTopic,
                    questionCount = quiz.questions.size,
                    difficulty = quiz.difficulty,
                    timerModeMinutes = maxOf(5, quiz.questions.size / 2)
                )

                _userAnswers.value = emptyMap()
                _markedForReview.value = emptySet()
                _currentQuestionIndex.value = 0

                testStartTimeSeconds = System.currentTimeMillis() / 1000

                _quizState.value = QuizUiState.Active(
                    quiz = quiz,
                    config = config,
                    modelUsed = "Custom Question Bank Builder (Offline)"
                )

                startTimer(config.timerModeMinutes)
            } catch (e: Exception) {
                _quizState.value = QuizUiState.Error(e.localizedMessage ?: "Failed to build custom test")
            }
        }
    }

    fun exportBackup(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = examRepo.exportBackupJson()
                onResult(json)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun importBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                examRepo.importBackupJson(jsonString)
                onResult(true, "Backup restored successfully!")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to restore backup")
            }
        }
    }

    fun deleteQuestionBankItem(id: Long) {
        viewModelScope.launch {
            examRepo.dao.deleteQuestionBankItemById(id)
        }
    }

    fun clearQuestionBank() {
        viewModelScope.launch {
            examRepo.dao.deleteAllQuestionBankItems()
        }
    }
}
