package com.example.data.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookmarkEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.StudyNoteEntity
import com.example.data.db.TestRecordEntity
import com.example.data.db.TopicStatEntity
import com.example.data.db.WrongQuestionEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.SupportedModel
import com.example.data.repository.ExamRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.model.GeneratedDoubtResponse
import com.example.model.GeneratedFlashcardItem
import com.example.model.GeneratedFlashcardSet
import com.example.model.GeneratedQuiz
import com.example.model.GeneratedStudyNotes
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
    val updateManager = com.example.data.update.UpdateManager.getInstance(application)
    val updateState: StateFlow<com.example.data.update.UpdateState> = updateManager.updateState

    init {
        try {
            // Automatically check for updates in background on app launch (throttled)
            updateManager.checkForUpdates(isManual = false)
            com.example.worker.UpdateScheduler.schedule(application)
        } catch (e: Throwable) {
            android.util.Log.e("MainViewModel", "Failed to perform initial update check or schedule background worker", e)
        }
    }

    fun checkForUpdatesManually() {
        updateManager.checkForUpdates(isManual = true)
    }

    val userApiKey: StateFlow<String> = prefsRepo.userApiKey
    val onboardingCompleted: StateFlow<Boolean> = prefsRepo.onboardingCompleted
    val permissionsOnboardingCompleted: StateFlow<Boolean> = prefsRepo.permissionsOnboardingCompleted

    fun setPermissionsOnboardingCompleted(completed: Boolean) {
        prefsRepo.setPermissionsOnboardingCompleted(completed)
    }
    val displayName: StateFlow<String> = prefsRepo.displayName
    val profileImageUri: StateFlow<String> = prefsRepo.profileImageUri
    val primaryExam: StateFlow<String> = prefsRepo.preferredExam
    val additionalExams: StateFlow<String> = prefsRepo.additionalExams
    val appLanguage: StateFlow<String> = prefsRepo.appLanguage
    val preferredLanguage: StateFlow<String> = prefsRepo.defaultLanguage

    fun setAppLanguage(language: String) {
        prefsRepo.setAppLanguage(language)
    }
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

    val questionsAttemptedToday: StateFlow<Int> = testHistory.map { history ->
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)

        val cal = Calendar.getInstance()
        history.sumOf { record ->
            cal.timeInMillis = record.createdAt
            if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                record.questionCount
            } else {
                0
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val overallAccuracy: StateFlow<Float> = testHistory.map { history ->
        val totalQuestionsSolved = history.sumOf { it.questionCount }
        val totalCorrect = history.sumOf { it.correctCount }
        if (totalQuestionsSolved > 0) (totalCorrect.toFloat() / totalQuestionsSolved) * 100f else 0f
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
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
        com.example.util.AdManager.onGenerationStart(getApplication())
        viewModelScope.launch {
            _quizState.value = QuizUiState.Generating("Analyzing study material...")
            try {
                val apiKey = prefsRepo.getEffectiveApiKey()
                if (apiKey.isBlank()) {
                    com.example.util.AdManager.onGenerationFailed()
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

                com.example.util.AdManager.onGenerationSuccess()

                _userAnswers.value = emptyMap()
                _writtenAnswers.value = emptyMap()
                _writtenEvaluations.value = emptyList()
                _evaluationError.value = null
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
                com.example.util.AdManager.onGenerationFailed()
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

    // Written Answers State
    private val _writtenAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val writtenAnswers: StateFlow<Map<Int, String>> = _writtenAnswers.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _evaluationError = MutableStateFlow<String?>(null)
    val evaluationError: StateFlow<String?> = _evaluationError.asStateFlow()

    private val _writtenEvaluations = MutableStateFlow<List<com.example.model.WrittenEvaluation>>(emptyList())
    val writtenEvaluations: StateFlow<List<com.example.model.WrittenEvaluation>> = _writtenEvaluations.asStateFlow()

    fun updateWrittenAnswer(questionId: Int, answer: String) {
        val current = _writtenAnswers.value.toMutableMap()
        current[questionId] = answer
        _writtenAnswers.value = current
    }

    fun clearEvaluationError() {
        _evaluationError.value = null
    }

    fun submitTest(fromTimer: Boolean = false) {
        timerJob?.cancel()
        val activeState = _quizState.value as? QuizUiState.Active ?: return

        viewModelScope.launch {
            val hasWrittenQuestions = activeState.quiz.writtenQuestions.isNotEmpty()

            var evals: List<com.example.model.WrittenEvaluation> = emptyList()

            if (hasWrittenQuestions) {
                _isEvaluating.value = true
                _evaluationError.value = null
                _quizState.value = QuizUiState.Generating("AI is evaluating your written answers...")

                val apiKey = prefsRepo.getEffectiveApiKey()
                if (apiKey.isBlank()) {
                    _isEvaluating.value = false
                    _evaluationError.value = "Gemini API key required for evaluation. Please configure key in Settings."
                    _quizState.value = activeState // restore active state so student doesn't lose answers!
                    return@launch
                }

                try {
                    evals = examRepo.evaluateWrittenTest(
                        questions = activeState.quiz.writtenQuestions,
                        userAnswers = _writtenAnswers.value,
                        preferredModelId = prefsRepo.selectedModel.value,
                        autoFallback = prefsRepo.autoFallbackEnabled.value,
                        apiKey = apiKey,
                        onStatusUpdate = { msg ->
                            _quizState.value = QuizUiState.Generating(msg)
                        }
                    )
                    _writtenEvaluations.value = evals
                    _isEvaluating.value = false
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Evaluation failed", e)
                    _isEvaluating.value = false
                    _evaluationError.value = e.localizedMessage ?: "AI Evaluation failed. Your answers are saved! Tap retry."
                    _quizState.value = activeState // restore active state so student doesn't lose answers!
                    return@launch
                }
            }

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
                writtenAnswers = _writtenAnswers.value,
                evaluations = evals,
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
                    val writtenAnswersMap = mutableMapOf<Int, String>()
                    if (record.writtenAnswersJson.isNotBlank()) {
                        try {
                            val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                            val mapAdapter = moshi.adapter<Map<String, String>>(type)
                            val strMap = mapAdapter.fromJson(record.writtenAnswersJson)
                            strMap?.forEach { (k, v) ->
                                k.toIntOrNull()?.let { id -> writtenAnswersMap[id] = v }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    _writtenAnswers.value = writtenAnswersMap

                    val evaluationsList = mutableListOf<com.example.model.WrittenEvaluation>()
                    if (record.evaluationsJson.isNotBlank()) {
                        try {
                            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.model.WrittenEvaluation::class.java)
                            val evalsAdapter = moshi.adapter<List<com.example.model.WrittenEvaluation>>(listType)
                            val parsedEvals = evalsAdapter.fromJson(record.evaluationsJson)
                            if (parsedEvals != null) evaluationsList.addAll(parsedEvals)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    _writtenEvaluations.value = evaluationsList

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

    // ==================================================
    // AI STUDY TOOLS (v1.2.0 Upgrade)
    // ==================================================

    val savedStudyNotes: StateFlow<List<StudyNoteEntity>> = examRepo.dao.getAllStudyNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedFlashcards: StateFlow<List<FlashcardEntity>> = examRepo.dao.getAllFlashcards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Study Notes ---
    private val _isGeneratingNotes = MutableStateFlow(false)
    val isGeneratingNotes: StateFlow<Boolean> = _isGeneratingNotes.asStateFlow()

    private val _notesGenerationStatus = MutableStateFlow("")
    val notesGenerationStatus: StateFlow<String> = _notesGenerationStatus.asStateFlow()

    private val _generatedNotes = MutableStateFlow<GeneratedStudyNotes?>(null)
    val generatedNotes: StateFlow<GeneratedStudyNotes?> = _generatedNotes.asStateFlow()

    fun generateNotes(
        subject: String,
        topic: String,
        customInstructions: String,
        language: String = "English",
        onSuccess: (GeneratedStudyNotes) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isGeneratingNotes.value) return
        _isGeneratingNotes.value = true
        _notesGenerationStatus.value = "Connecting to Gemini..."
        viewModelScope.launch {
            try {
                val apiKey = prefsRepo.userApiKey.value
                val modelId = prefsRepo.selectedModel.value
                val chosenLang = if (language.isNotBlank()) language else prefsRepo.defaultLanguage.value.ifBlank { "English" }
                val notes = examRepo.generateStudyNotes(
                    subject = subject,
                    topic = topic,
                    customInstructions = customInstructions,
                    language = chosenLang,
                    preferredModelId = modelId,
                    autoFallback = true,
                    apiKey = apiKey,
                    onStatusUpdate = { _notesGenerationStatus.value = it }
                )
                _generatedNotes.value = notes
                onSuccess(notes)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to generate study notes.")
            } finally {
                _isGeneratingNotes.value = false
            }
        }
    }

    fun saveStudyNote(
        subject: String,
        topic: String,
        notes: GeneratedStudyNotes,
        customInstructions: String,
        language: String = "English",
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val moshiAdapter = Moshi.Builder().build().adapter(List::class.java)
            val noteEntity = StudyNoteEntity(
                subject = subject,
                topic = topic,
                title = if (notes.title.isNotBlank()) notes.title else "$topic Study Notes",
                summary = notes.summary,
                importantConceptsJson = moshiAdapter.toJson(notes.importantConcepts),
                keyDefinitionsJson = moshiAdapter.toJson(notes.keyDefinitions),
                examPointsJson = moshiAdapter.toJson(notes.examPoints),
                examplesJson = moshiAdapter.toJson(notes.examples),
                quickRevisionJson = moshiAdapter.toJson(notes.quickRevision),
                customInstructions = customInstructions,
                language = if (language.isNotBlank()) language else prefsRepo.defaultLanguage.value.ifBlank { "English" }
            )
            examRepo.dao.insertStudyNote(noteEntity)
            onSaved()
        }
    }

    fun deleteStudyNote(id: Long) {
        viewModelScope.launch {
            examRepo.dao.deleteStudyNoteById(id)
        }
    }

    // --- Flashcards ---
    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards: StateFlow<Boolean> = _isGeneratingFlashcards.asStateFlow()

    private val _flashcardsGenerationStatus = MutableStateFlow("")
    val flashcardsGenerationStatus: StateFlow<String> = _flashcardsGenerationStatus.asStateFlow()

    private val _generatedFlashcardSet = MutableStateFlow<GeneratedFlashcardSet?>(null)
    val generatedFlashcardSet: StateFlow<GeneratedFlashcardSet?> = _generatedFlashcardSet.asStateFlow()

    fun generateFlashcards(
        subject: String,
        topic: String,
        count: Int,
        language: String = "English",
        onSuccess: (GeneratedFlashcardSet) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isGeneratingFlashcards.value) return
        _isGeneratingFlashcards.value = true
        _flashcardsGenerationStatus.value = "Preparing flashcards..."
        viewModelScope.launch {
            try {
                val apiKey = prefsRepo.userApiKey.value
                val modelId = prefsRepo.selectedModel.value
                val chosenLang = if (language.isNotBlank()) language else prefsRepo.defaultLanguage.value.ifBlank { "English" }
                val cardSet = examRepo.generateFlashcards(
                    subject = subject,
                    topic = topic,
                    count = count,
                    language = chosenLang,
                    preferredModelId = modelId,
                    autoFallback = true,
                    apiKey = apiKey,
                    onStatusUpdate = { _flashcardsGenerationStatus.value = it }
                )
                _generatedFlashcardSet.value = cardSet
                onSuccess(cardSet)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to generate flashcards.")
            } finally {
                _isGeneratingFlashcards.value = false
            }
        }
    }

    fun saveFlashcards(
        subject: String,
        topic: String,
        cards: List<GeneratedFlashcardItem>,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val entities = cards.map {
                FlashcardEntity(
                    subject = subject,
                    topic = topic,
                    frontText = it.frontText,
                    backText = it.backText,
                    masteryState = "New",
                    timesReviewed = 0
                )
            }
            examRepo.dao.insertFlashcards(entities)
            onSaved()
        }
    }

    fun updateFlashcardMastery(card: FlashcardEntity, newState: String) {
        viewModelScope.launch {
            val updated = card.copy(
                masteryState = newState,
                timesReviewed = card.timesReviewed + 1
            )
            examRepo.dao.updateFlashcard(updated)
        }
    }

    fun deleteFlashcard(id: Long) {
        viewModelScope.launch {
            examRepo.dao.deleteFlashcardById(id)
        }
    }

    fun deleteFlashcardSet(subject: String, topic: String) {
        viewModelScope.launch {
            examRepo.dao.deleteFlashcardsBySubjectAndTopic(subject, topic)
        }
    }

    // --- AI Doubt Solver ---
    private val _isSolvingDoubt = MutableStateFlow(false)
    val isSolvingDoubt: StateFlow<Boolean> = _isSolvingDoubt.asStateFlow()

    private val _doubtStatus = MutableStateFlow("")
    val doubtStatus: StateFlow<String> = _doubtStatus.asStateFlow()

    fun solveDoubt(
        subject: String,
        topic: String,
        doubt: String,
        language: String = "English",
        onSuccess: (GeneratedDoubtResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isSolvingDoubt.value) return
        _isSolvingDoubt.value = true
        _doubtStatus.value = "Consulting AI..."
        viewModelScope.launch {
            try {
                val apiKey = prefsRepo.userApiKey.value
                val modelId = prefsRepo.selectedModel.value
                val chosenLang = if (language.isNotBlank()) language else prefsRepo.defaultLanguage.value.ifBlank { "English" }
                val resp = examRepo.solveDoubt(
                    subject = subject,
                    topic = topic,
                    doubt = doubt,
                    language = chosenLang,
                    preferredModelId = modelId,
                    autoFallback = true,
                    apiKey = apiKey,
                    onStatusUpdate = { _doubtStatus.value = it }
                )
                onSuccess(resp)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to get doubt answer.")
            } finally {
                _isSolvingDoubt.value = false
            }
        }
    }

    fun explainQuestion(
        questionText: String,
        optionsText: String,
        correctAnswer: String,
        explanation: String,
        userQuery: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val apiKey = prefsRepo.userApiKey.value
                val modelId = prefsRepo.selectedModel.value
                val lang = prefsRepo.defaultLanguage.value
                val result = examRepo.explainQuestion(
                    questionText = questionText,
                    optionsText = optionsText,
                    correctAnswer = correctAnswer,
                    explanation = explanation,
                    userQuery = userQuery,
                    language = lang,
                    preferredModelId = modelId,
                    autoFallback = true,
                    apiKey = apiKey
                )
                onResult(result)
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Error explaining question.")
            }
        }
    }
}
