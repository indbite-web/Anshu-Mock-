package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.remote.SupportedModel
import com.example.data.security.SecureKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("anshu_exam_prefs", Context.MODE_PRIVATE)

    private val secureKeyStorage = SecureKeyStorage(context)

    private val _userApiKey = MutableStateFlow(secureKeyStorage.getApiKey())
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(
        prefs.getString("selected_model", SupportedModel.DEFAULT_MODEL.modelId)
            ?: SupportedModel.DEFAULT_MODEL.modelId
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    fun saveUserApiKey(key: String) {
        val trimmed = key.trim()
        secureKeyStorage.saveApiKey(trimmed)
        _userApiKey.value = trimmed
    }

    fun removeUserApiKey() {
        secureKeyStorage.removeApiKey()
        _userApiKey.value = ""
    }

    /**
     * Resolves the active Gemini API key.
     * 1. User-entered API key from secure storage takes highest priority.
     * 2. In local DEBUG builds only, fallback to BuildConfig.GEMINI_API_KEY if user key is absent.
     * 3. In RELEASE builds, never fallback to developer key; return "" if user key is absent.
     */
    fun getEffectiveApiKey(): String {
        val userKey = _userApiKey.value.trim()
        if (userKey.isNotBlank()) {
            return userKey
        }

        // Allow fallback ONLY in Debug builds for local development
        if (BuildConfig.DEBUG) {
            val debugKey = BuildConfig.GEMINI_API_KEY.trim()
            if (debugKey.isNotBlank() && debugKey != "MY_GEMINI_API_KEY") {
                return debugKey
            }
        }

        return ""
    }

    private val _autoFallbackEnabled = MutableStateFlow(
        prefs.getBoolean("auto_fallback", true)
    )
    val autoFallbackEnabled: StateFlow<Boolean> = _autoFallbackEnabled.asStateFlow()

    private val _defaultQuestions = MutableStateFlow(
        prefs.getInt("default_questions", 10)
    )
    val defaultQuestions: StateFlow<Int> = _defaultQuestions.asStateFlow()

    private val _defaultDifficulty = MutableStateFlow(
        prefs.getString("default_difficulty", "Medium") ?: "Medium"
    )
    val defaultDifficulty: StateFlow<String> = _defaultDifficulty.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        prefs.getString("app_language", "English") ?: "English"
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _defaultLanguage = MutableStateFlow(
        prefs.getString("default_language", "Hindi") ?: "Hindi"
    )
    val defaultLanguage: StateFlow<String> = _defaultLanguage.asStateFlow()

    private val _defaultNegativeMarking = MutableStateFlow(
        prefs.getFloat("default_negative_marking", 0.0f)
    )
    val defaultNegativeMarking: StateFlow<Float> = _defaultNegativeMarking.asStateFlow()

    private val _defaultTimerMinutes = MutableStateFlow(
        prefs.getInt("default_timer_minutes", 10)
    )
    val defaultTimerMinutes: StateFlow<Int> = _defaultTimerMinutes.asStateFlow()

    private val _strictSourceMode = MutableStateFlow(
        prefs.getBoolean("strict_source_mode", true)
    )
    val strictSourceMode: StateFlow<Boolean> = _strictSourceMode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean("onboarding_completed", false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _permissionsOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("permissions_onboarding_completed", false)
    )
    val permissionsOnboardingCompleted: StateFlow<Boolean> = _permissionsOnboardingCompleted.asStateFlow()

    private val _displayName = MutableStateFlow(
        prefs.getString("display_name", "") ?: ""
    )
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _profileImageUri = MutableStateFlow(
        prefs.getString("profile_image_uri", "") ?: ""
    )
    val profileImageUri: StateFlow<String> = _profileImageUri.asStateFlow()

    private val _additionalExams = MutableStateFlow(
        prefs.getString("additional_exams", "") ?: ""
    )
    val additionalExams: StateFlow<String> = _additionalExams.asStateFlow()

    private val _dailyGoalTarget = MutableStateFlow(
        prefs.getInt("daily_goal_target", 50)
    )
    val dailyGoalTarget: StateFlow<Int> = _dailyGoalTarget.asStateFlow()

    private val _preferredExam = MutableStateFlow(
        prefs.getString("preferred_exam", "General Practice") ?: "General Practice"
    )
    val preferredExam: StateFlow<String> = _preferredExam.asStateFlow()

    // Study Reminders Preferences
    private val _studyRemindersEnabled = MutableStateFlow(
        prefs.getBoolean("study_reminders_enabled", false)
    )
    val studyRemindersEnabled: StateFlow<Boolean> = _studyRemindersEnabled.asStateFlow()

    private val _reminderIntervalHours = MutableStateFlow(
        prefs.getInt("reminder_interval_hours", 2)
    )
    val reminderIntervalHours: StateFlow<Int> = _reminderIntervalHours.asStateFlow()

    private val _quietHoursStartHour = MutableStateFlow(
        prefs.getInt("quiet_hours_start_hour", 22)
    )
    val quietHoursStartHour: StateFlow<Int> = _quietHoursStartHour.asStateFlow()

    private val _quietHoursStartMinute = MutableStateFlow(
        prefs.getInt("quiet_hours_start_minute", 0)
    )
    val quietHoursStartMinute: StateFlow<Int> = _quietHoursStartMinute.asStateFlow()

    private val _quietHoursEndHour = MutableStateFlow(
        prefs.getInt("quiet_hours_end_hour", 8)
    )
    val quietHoursEndHour: StateFlow<Int> = _quietHoursEndHour.asStateFlow()

    private val _quietHoursEndMinute = MutableStateFlow(
        prefs.getInt("quiet_hours_end_minute", 0)
    )
    val quietHoursEndMinute: StateFlow<Int> = _quietHoursEndMinute.asStateFlow()

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setPermissionsOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("permissions_onboarding_completed", completed).apply()
        _permissionsOnboardingCompleted.value = completed
    }

    fun setDisplayName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString("display_name", trimmed).apply()
        _displayName.value = trimmed
    }

    fun setProfileImageUri(uri: String) {
        val trimmed = uri.trim()
        prefs.edit().putString("profile_image_uri", trimmed).apply()
        _profileImageUri.value = trimmed
    }

    fun clearProfileImageUri() {
        prefs.edit().remove("profile_image_uri").apply()
        _profileImageUri.value = ""
    }

    fun setAdditionalExams(exams: String) {
        val trimmed = exams.trim()
        prefs.edit().putString("additional_exams", trimmed).apply()
        _additionalExams.value = trimmed
    }

    fun resetProfile() {
        setDisplayName("")
        setPreferredExam("General Practice")
        setAdditionalExams("")
        setDefaultLanguage("Hindi")
        setDailyGoalTarget(50)
    }

    fun setSelectedModel(modelId: String) {
        prefs.edit().putString("selected_model", modelId).apply()
        _selectedModel.value = modelId
    }

    fun setAutoFallbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_fallback", enabled).apply()
        _autoFallbackEnabled.value = enabled
    }

    fun setDefaultQuestions(count: Int) {
        prefs.edit().putInt("default_questions", count).apply()
        _defaultQuestions.value = count
    }

    fun setDefaultDifficulty(difficulty: String) {
        prefs.edit().putString("default_difficulty", difficulty).apply()
        _defaultDifficulty.value = difficulty
    }

    fun setAppLanguage(language: String) {
        val lang = if (language.equals("हिंदी", ignoreCase = true) || language.equals("hi", ignoreCase = true)) "हिंदी" else "English"
        prefs.edit().putString("app_language", lang).apply()
        _appLanguage.value = lang
    }

    fun setDefaultLanguage(language: String) {
        prefs.edit().putString("default_language", language).apply()
        _defaultLanguage.value = language
    }

    fun setDefaultNegativeMarking(ratio: Float) {
        prefs.edit().putFloat("default_negative_marking", ratio).apply()
        _defaultNegativeMarking.value = ratio
    }

    fun setDefaultTimerMinutes(minutes: Int) {
        prefs.edit().putInt("default_timer_minutes", minutes).apply()
        _defaultTimerMinutes.value = minutes
    }

    fun setStrictSourceMode(enabled: Boolean) {
        prefs.edit().putBoolean("strict_source_mode", enabled).apply()
        _strictSourceMode.value = enabled
    }

    fun setDailyGoalTarget(goal: Int) {
        prefs.edit().putInt("daily_goal_target", goal).apply()
        _dailyGoalTarget.value = goal
    }

    fun setPreferredExam(exam: String) {
        prefs.edit().putString("preferred_exam", exam).apply()
        _preferredExam.value = exam
    }

    fun setStudyRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("study_reminders_enabled", enabled).apply()
        _studyRemindersEnabled.value = enabled
    }

    fun setReminderIntervalHours(hours: Int) {
        val validHours = if (hours < 1) 1 else hours
        prefs.edit().putInt("reminder_interval_hours", validHours).apply()
        _reminderIntervalHours.value = validHours
    }

    fun setQuietHours(startHour: Int, startMin: Int, endHour: Int, endMin: Int) {
        prefs.edit()
            .putInt("quiet_hours_start_hour", startHour)
            .putInt("quiet_hours_start_minute", startMin)
            .putInt("quiet_hours_end_hour", endHour)
            .putInt("quiet_hours_end_minute", endMin)
            .apply()
        _quietHoursStartHour.value = startHour
        _quietHoursStartMinute.value = startMin
        _quietHoursEndHour.value = endHour
        _quietHoursEndMinute.value = endMin
    }

    fun isStudyRemindersEnabled(): Boolean = prefs.getBoolean("study_reminders_enabled", false)
    fun getReminderIntervalHours(): Int = prefs.getInt("reminder_interval_hours", 2)
    fun getQuietHoursStartHour(): Int = prefs.getInt("quiet_hours_start_hour", 22)
    fun getQuietHoursStartMinute(): Int = prefs.getInt("quiet_hours_start_minute", 0)
    fun getQuietHoursEndHour(): Int = prefs.getInt("quiet_hours_end_hour", 8)
    fun getQuietHoursEndMinute(): Int = prefs.getInt("quiet_hours_end_minute", 0)
}
