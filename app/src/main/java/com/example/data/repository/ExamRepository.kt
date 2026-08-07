package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.db.BookmarkEntity
import com.example.data.db.QuestionBankEntity
import com.example.data.db.TestRecordEntity
import com.example.data.db.TopicStatEntity
import com.example.data.db.WrongQuestionEntity
import com.example.data.remote.Content
import com.example.data.remote.GeminiClient
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import com.example.data.remote.SupportedModel
import com.example.model.GeneratedQuiz
import com.example.model.McqOption
import com.example.model.McqQuestion
import com.example.model.TestConfig
import com.example.model.TestResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExamRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val dao = db.examDao()
    private val moshi: Moshi = GeminiClient.moshi

    val testHistory: Flow<List<TestRecordEntity>> = dao.getAllTestRecords()
    val wrongQuestions: Flow<List<WrongQuestionEntity>> = dao.getUnmasteredWrongQuestions()
    val bookmarkedQuestions: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    val topicStats: Flow<List<TopicStatEntity>> = dao.getAllTopicStats()
    val questionBankItems: Flow<List<QuestionBankEntity>> = dao.getAllQuestionBankItems()

data class GenerationResult(
    val quiz: GeneratedQuiz,
    val selectedModel: String,
    val actualModelUsed: String,
    val wasFallback: Boolean
)

    suspend fun generateQuiz(
        config: TestConfig,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String,
        onStatusUpdate: (String) -> Unit = {}
    ): GenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required. Please configure your API key in Settings → AI Configuration.")
        }

        // Build system prompt and contents
        val systemPrompt = """
            You are an expert examination paper setter and MCQ practice question creator.
            Your task is to generate high-quality Multiple Choice Questions (MCQs) in structured JSON format based on the user's provided study material.

            TARGET EXAM: ${config.targetExam}
            SUBJECT: ${config.subject}
            TOPIC: ${config.topic}

            CRITICAL RULES:
            ${if (config.strictSourceMode) "1. STRICT SOURCE MODE: Generate questions ONLY from information explicitly visible/written in the attached pages. DO NOT introduce unstated external facts or invent missing data." else "1. Base questions primarily on the provided study material, extending where relevant for full test coverage."}
            2. Language Requirement: ${config.language}. (If Hindi or Hindi + English, write questions in clear Devanagari Hindi or bilingual Hindi/English).
            3. Difficulty Level: ${config.difficulty}.
            4. Question Style: ${config.style}. (If Mixed Competitive, include multi-statement verification questions, match-the-following style, and assertion-reasoning style).
            5. Target Question Count: Exactly ${config.questionCount} questions.
            ${if (config.customInstruction.isNotBlank()) "6. Custom Instruction: '${config.customInstruction}'" else if (config.naturalPrompt.isNotBlank()) "6. Custom Instruction: '${config.naturalPrompt}'" else ""}
            7. Options Requirement: Every question MUST have exactly 4 options labeled "A", "B", "C", and "D". Ensure exactly ONE option is unambiguously correct.
            8. Explanations: Provide clear, concise, step-by-step explanations for the correct answer.
            9. Mathematical & Numerical Notation: If questions contain math, physics, formulas, or numbers, use readable standard mathematical notation (e.g. x², √x, ±, ÷, ×, ∫, θ, π, ½, etc.).

            JSON OUTPUT FORMAT (STRICT):
            {
              "title": "${config.targetExam} - ${config.subject} Practice Test",
              "examName": "${config.targetExam}",
              "subject": "${config.subject}",
              "sourceTopic": "${config.topic}",
              "difficulty": "${config.difficulty}",
              "questions": [
                {
                  "id": 1,
                  "question": "Question text here",
                  "options": [
                    {"id": "A", "text": "Option A text"},
                    {"id": "B", "text": "Option B text"},
                    {"id": "C", "text": "Option C text"},
                    {"id": "D", "text": "Option D text"}
                  ],
                  "correctAnswer": "A",
                  "explanation": "Detailed explanation here",
                  "subject": "${config.subject}",
                  "topic": "Specific Subtopic",
                  "difficulty": "${config.difficulty}"
                }
              ]
            }
        """.trimIndent()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = "Please generate ${config.questionCount} ${config.difficulty} MCQs based on the attached study material in JSON format."))

        // Attach Base64 images if present
        config.imageBase64List.forEach { b64 ->
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = b64)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        // Determine list of models to attempt strictly starting with user selected model
        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST

        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { fallback ->
                if (fallback != initialModel && !list.contains(fallback)) {
                    list.add(fallback)
                }
            }
            list
        } else {
            listOf(initialModel)
        }

        var lastFallbackError: String? = null

        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue

            onStatusUpdate("Generating test using ${model.displayName}...")

            val response = try {
                GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
            } catch (e: Exception) {
                // Connection or network level failure - affects all models, throw immediately
                android.util.Log.e("ExamRepository", "Network error calling ${model.displayName}", e)
                val formatted = formatGeminiException(e, model.displayName)
                throw IllegalStateException(formatted)
            }

            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (rawText.isBlank()) {
                    throw IllegalStateException("Gemini (${model.displayName}) returned an empty response. Please adjust your topic or prompt.")
                }

                val parsedQuiz = try {
                    parseAndValidateQuizJson(rawText, config)
                } catch (e: Exception) {
                    // Normal error: JSON parsing failure from model response - DO NOT FALLBACK
                    android.util.Log.e("ExamRepository", "JSON parse error from ${model.displayName}", e)
                    val detail = e.localizedMessage ?: "Failed to parse questions"
                    throw IllegalArgumentException("Failed to parse quiz response from ${model.displayName}: $detail")
                }

                if (parsedQuiz.questions.isNotEmpty()) {
                    saveQuestionsToBank(parsedQuiz)
                    val wasFallback = (model != initialModel)
                    return@withContext GenerationResult(
                        quiz = parsedQuiz,
                        selectedModel = initialModel.displayName,
                        actualModelUsed = model.displayName,
                        wasFallback = wasFallback
                    )
                } else {
                    throw IllegalArgumentException("Gemini (${model.displayName}) response did not contain any valid MCQs with 4 options.")
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                val formatted = formatGeminiApiHttpError(code, errBody, model.displayName)

                if (autoFallback && isFallbackEligibleError(code, errBody)) {
                    android.util.Log.w("ExamRepository", "${model.displayName} failed with fallback-eligible error ($code). Attempting next available model...")
                    lastFallbackError = formatted
                    continue
                } else {
                    // Normal error (e.g. 400 Bad Request, 401 Invalid Key) OR autoFallback disabled: fail immediately
                    throw IllegalStateException(formatted)
                }
            }
        }

        val finalMsg = lastFallbackError ?: "Couldn't generate practice test. Gemini free-tier models were unavailable."
        throw IllegalStateException(finalMsg)
    }

    private fun isFallbackEligibleError(code: Int, errBody: String): Boolean {
        val lower = errBody.lowercase()
        return when {
            code == 429 || lower.contains("resource_exhausted") || lower.contains("quota") || lower.contains("rate limit") -> true
            code == 404 || lower.contains("model_not_found") || lower.contains("not found") -> true
            code == 503 || code == 500 || code == 502 || code == 504 || lower.contains("service unavailable") || lower.contains("overloaded") -> true
            else -> false
        }
    }

    private fun formatGeminiApiHttpError(code: Int, errBody: String, modelName: String): String {
        return when {
            code in listOf(400, 401, 403) || errBody.contains("API_KEY_INVALID", ignoreCase = true) || errBody.contains("API key not valid", ignoreCase = true) ->
                "Invalid API key provided. Please verify your Gemini API key in Settings."
            code == 404 || errBody.contains("MODEL_NOT_FOUND", ignoreCase = true) ->
                "Selected AI model ($modelName) is currently unavailable."
            code == 429 || errBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || errBody.contains("quota", ignoreCase = true) ->
                "Quota or rate limit reached for Gemini free tier. Please wait a moment and try again."
            code >= 500 ->
                "Gemini server error ($code). Please try again in a few moments."
            else ->
                "Gemini request failed ($code): ${if (errBody.isNotBlank()) errBody.take(120) else "Unknown error"}"
        }
    }

    private fun formatGeminiException(e: Exception, modelName: String): String {
        return when (e) {
            is java.net.UnknownHostException, is java.io.IOException ->
                "Network unavailable. Please check your internet connection."
            is java.net.SocketTimeoutException ->
                "Request timed out while connecting to Gemini. Try reducing question count or attached images."
            else ->
                "Error generating test: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }

    private fun parseAndValidateQuizJson(rawText: String, config: TestConfig): GeneratedQuiz {
        val jsonString = extractJsonSubstring(rawText)
        if (jsonString.isBlank()) {
            throw IllegalArgumentException("Empty or invalid JSON returned from Gemini.")
        }

        // Try Moshi first
        try {
            val adapter = moshi.adapter(GeneratedQuiz::class.java)
            val quiz = adapter.fromJson(jsonString)
            if (quiz != null && quiz.questions.isNotEmpty()) {
                val validated = validateAndNormalizeQuestions(quiz.questions, config)
                if (validated.isNotEmpty()) {
                    return quiz.copy(questions = validated)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ExamRepository", "Moshi parsing failed, falling back to org.json parser", e)
        }

        // Fallback: org.json.JSONObject / JSONArray manual parsing
        val mcqList = mutableListOf<McqQuestion>()
        var quizTitle = "${config.targetExam} - ${config.subject} Practice Test"
        var examName = config.targetExam
        var subjectName = config.subject
        var sourceTopic = config.topic
        var diff = config.difficulty

        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val jsonArr = org.json.JSONArray(trimmed)
                parseJsonArrayToQuestions(jsonArr, mcqList, config)
            } else {
                val jsonObj = org.json.JSONObject(trimmed)
                quizTitle = jsonObj.optString("title", quizTitle)
                examName = jsonObj.optString("examName", examName)
                subjectName = jsonObj.optString("subject", subjectName)
                sourceTopic = jsonObj.optString("sourceTopic", jsonObj.optString("topic", sourceTopic))
                diff = jsonObj.optString("difficulty", diff)

                val questionsArr = jsonObj.optJSONArray("questions")
                if (questionsArr != null) {
                    parseJsonArrayToQuestions(questionsArr, mcqList, config)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Manual JSON parsing error", e)
            throw IllegalArgumentException("Failed to parse questions from Gemini response.")
        }

        val validatedQuestions = validateAndNormalizeQuestions(mcqList, config)
        if (validatedQuestions.isEmpty()) {
            throw IllegalArgumentException("Gemini response did not contain any valid MCQs with 4 options.")
        }

        return GeneratedQuiz(
            title = quizTitle,
            examName = examName,
            subject = subjectName,
            sourceTopic = sourceTopic,
            difficulty = diff,
            questions = validatedQuestions
        )
    }

    private fun extractJsonSubstring(raw: String): String {
        var text = raw.trim()
        if (text.contains("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```")
        } else if (text.contains("```")) {
            text = text.substringAfter("```").substringBeforeLast("```")
        }
        text = text.trim()

        val firstObj = text.indexOf('{')
        val firstArr = text.indexOf('[')
        val start = when {
            firstObj != -1 && firstArr != -1 -> minOf(firstObj, firstArr)
            firstObj != -1 -> firstObj
            firstArr != -1 -> firstArr
            else -> -1
        }

        val lastObj = text.lastIndexOf('}')
        val lastArr = text.lastIndexOf(']')
        val end = maxOf(lastObj, lastArr)

        return if (start != -1 && end != -1 && end > start) {
            text.substring(start, end + 1)
        } else {
            text
        }
    }

    private fun parseJsonArrayToQuestions(
        jsonArr: org.json.JSONArray,
        targetList: MutableList<McqQuestion>,
        config: TestConfig
    ) {
        for (i in 0 until jsonArr.length()) {
            try {
                val qObj = jsonArr.getJSONObject(i)
                val qText = qObj.optString("question", qObj.optString("questionText", ""))
                if (qText.isBlank()) continue

                val optionsList = mutableListOf<McqOption>()
                val optionsArr = qObj.optJSONArray("options")
                if (optionsArr != null) {
                    for (j in 0 until optionsArr.length()) {
                        val item = optionsArr.get(j)
                        if (item is org.json.JSONObject) {
                            val id = item.optString("id", listOf("A", "B", "C", "D").getOrElse(j) { "A" })
                            val text = item.optString("text", "")
                            optionsList.add(McqOption(id, text))
                        } else if (item is String) {
                            val id = listOf("A", "B", "C", "D").getOrElse(j) { "A" }
                            optionsList.add(McqOption(id, item))
                        }
                    }
                }

                val rawCorrect = qObj.opt("correctAnswer")?.toString() ?: "A"
                val explanation = qObj.optString("explanation", "Correct answer is $rawCorrect.")
                val subject = qObj.optString("subject", config.subject)
                val topic = qObj.optString("topic", config.topic)
                val difficulty = qObj.optString("difficulty", config.difficulty)

                targetList.add(
                    McqQuestion(
                        id = i + 1,
                        question = qText,
                        options = optionsList,
                        correctAnswer = rawCorrect,
                        explanation = explanation,
                        subject = subject,
                        topic = topic,
                        difficulty = difficulty
                    )
                )
            } catch (e: Exception) {
                // Skip malformed individual items
            }
        }
    }

    private fun validateAndNormalizeQuestions(
        rawQuestions: List<McqQuestion>,
        config: TestConfig
    ): List<McqQuestion> {
        val validList = mutableListOf<McqQuestion>()

        rawQuestions.forEachIndexed { index, q ->
            if (q.question.isBlank()) return@forEachIndexed

            // Ensure options are exactly 4 non-blank options labeled A, B, C, D
            var opts = q.options.filter { it.text.isNotBlank() }
            if (opts.size < 4) {
                // Pad with dummy options if needed to ensure 4 options
                val currentIds = opts.map { it.id }
                val needed = listOf("A", "B", "C", "D").filter { !currentIds.contains(it) }
                val paddedOpts = opts.toMutableList()
                needed.take(4 - opts.size).forEach { missingId ->
                    paddedOpts.add(McqOption(missingId, "None of the above"))
                }
                opts = paddedOpts
            } else if (opts.size > 4) {
                opts = opts.take(4)
            }

            // Standardize option IDs to A, B, C, D
            val normalizedOpts = opts.mapIndexed { idx, opt ->
                val stdId = when (idx) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    else -> "D"
                }
                McqOption(stdId, opt.text)
            }

            // Normalize correctAnswer to "A", "B", "C", or "D"
            val rawAns = q.correctAnswer.trim()
            val finalAns = when {
                rawAns.equals("A", ignoreCase = true) || rawAns == "0" -> "A"
                rawAns.equals("B", ignoreCase = true) || rawAns == "1" -> "B"
                rawAns.equals("C", ignoreCase = true) || rawAns == "2" -> "C"
                rawAns.equals("D", ignoreCase = true) || rawAns == "3" -> "D"
                else -> {
                    // Try matching option text
                    val foundIdx = normalizedOpts.indexOfFirst {
                        it.text.equals(rawAns, ignoreCase = true) || rawAns.contains(it.text, ignoreCase = true)
                    }
                    if (foundIdx in 0..3) {
                        listOf("A", "B", "C", "D")[foundIdx]
                    } else "A"
                }
            }

            val finalExplanation = if (q.explanation.isNotBlank()) {
                q.explanation
            } else {
                "The correct answer is Option $finalAns."
            }

            validList.add(
                McqQuestion(
                    id = index + 1,
                    question = q.question,
                    options = normalizedOpts,
                    correctAnswer = finalAns,
                    explanation = finalExplanation,
                    subject = if (q.subject.isNotBlank()) q.subject else config.subject,
                    topic = if (q.topic.isNotBlank()) q.topic else config.topic,
                    difficulty = if (q.difficulty.isNotBlank()) q.difficulty else config.difficulty
                )
            )
        }

        return validList
    }

    private fun cleanJson(raw: String): String {
        return extractJsonSubstring(raw)
    }

    suspend fun saveQuestionsToBank(quiz: GeneratedQuiz) = withContext(Dispatchers.IO) {
        val items = quiz.questions.map { q ->
            QuestionBankEntity(
                questionText = q.question,
                optionA = q.options.find { it.id == "A" }?.text ?: "",
                optionB = q.options.find { it.id == "B" }?.text ?: "",
                optionC = q.options.find { it.id == "C" }?.text ?: "",
                optionD = q.options.find { it.id == "D" }?.text ?: "",
                correctAnswer = q.correctAnswer,
                explanation = q.explanation,
                examName = quiz.examName.ifBlank { "General Practice" },
                subject = q.subject.ifBlank { quiz.subject.ifBlank { "General" } },
                topic = q.topic.ifBlank { quiz.sourceTopic.ifBlank { "General" } },
                difficulty = q.difficulty.ifBlank { quiz.difficulty.ifBlank { "Medium" } },
                testSourceRef = quiz.title
            )
        }
        dao.insertQuestionBankItems(items)
    }

    suspend fun createTestFromQuestionBank(
        requestedCount: Int,
        topicFilter: String? = null,
        subjectFilter: String? = null,
        examFilter: String? = null
    ): GeneratedQuiz = withContext(Dispatchers.IO) {
        val allItems = when {
            !topicFilter.isNullOrBlank() -> {
                val list = dao.getQuestionBankByTopic(topicFilter)
                if (list.isNotEmpty()) list else dao.getAllQuestionBankItemsList()
            }
            !subjectFilter.isNullOrBlank() -> {
                val list = dao.getQuestionBankBySubject(subjectFilter)
                if (list.isNotEmpty()) list else dao.getAllQuestionBankItemsList()
            }
            !examFilter.isNullOrBlank() -> {
                val list = dao.getQuestionBankByExam(examFilter)
                if (list.isNotEmpty()) list else dao.getAllQuestionBankItemsList()
            }
            else -> dao.getAllQuestionBankItemsList()
        }

        if (allItems.isEmpty()) {
            throw IllegalStateException("Question Bank is empty. Please generate an AI test or add study material first.")
        }

        val selected = allItems.shuffled().take(requestedCount)
        val mcqs = selected.mapIndexed { idx, item ->
            McqQuestion(
                id = idx + 1,
                question = item.questionText,
                options = listOf(
                    McqOption("A", item.optionA),
                    McqOption("B", item.optionB),
                    McqOption("C", item.optionC),
                    McqOption("D", item.optionD)
                ),
                correctAnswer = item.correctAnswer,
                explanation = item.explanation,
                subject = item.subject,
                topic = item.topic,
                difficulty = item.difficulty
            )
        }

        GeneratedQuiz(
            title = "Custom Practice Test",
            examName = examFilter ?: selected.firstOrNull()?.examName ?: "General Practice",
            subject = subjectFilter ?: selected.firstOrNull()?.subject ?: "General",
            sourceTopic = topicFilter ?: "Custom Question Bank Mix",
            difficulty = selected.firstOrNull()?.difficulty ?: "Mixed",
            questions = mcqs
        )
    }

    suspend fun createCustomComposedTestFromBank(
        subjectCounts: Map<String, Int>
    ): GeneratedQuiz = withContext(Dispatchers.IO) {
        val allItems = dao.getAllQuestionBankItemsList()
        if (allItems.isEmpty()) {
            throw IllegalStateException("Question Bank is empty. Please create an AI test first.")
        }

        val selectedList = mutableListOf<QuestionBankEntity>()
        subjectCounts.forEach { (subject, count) ->
            val matching = allItems.filter { it.subject.equals(subject, ignoreCase = true) }.shuffled().take(count)
            selectedList.addAll(matching)
        }

        // Fill remaining if needed
        val targetTotal = subjectCounts.values.sum()
        if (selectedList.size < targetTotal) {
            val remainingNeeded = targetTotal - selectedList.size
            val unselected = allItems.filter { !selectedList.contains(it) }.shuffled().take(remainingNeeded)
            selectedList.addAll(unselected)
        }

        val mcqs = selectedList.shuffled().mapIndexed { idx, item ->
            McqQuestion(
                id = idx + 1,
                question = item.questionText,
                options = listOf(
                    McqOption("A", item.optionA),
                    McqOption("B", item.optionB),
                    McqOption("C", item.optionC),
                    McqOption("D", item.optionD)
                ),
                correctAnswer = item.correctAnswer,
                explanation = item.explanation,
                subject = item.subject,
                topic = item.topic,
                difficulty = item.difficulty
            )
        }

        GeneratedQuiz(
            title = "Custom Composed Practice Test",
            examName = "Custom Test",
            subject = "Multi-Subject",
            sourceTopic = "Custom Selection",
            difficulty = "Mixed",
            questions = mcqs
        )
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val qb = dao.getAllQuestionBankItemsList()
        val records = dao.getAllTestRecordsList()
        val wrong = dao.getUnmasteredWrongQuestionsList()
        val bookmarks = dao.getAllBookmarksList()
        val stats = dao.getAllTopicStatsList()

        val backup = com.example.model.AppBackupData(
            exportTimestamp = System.currentTimeMillis(),
            appVersion = "1.0",
            questionBank = qb,
            testRecords = records,
            wrongQuestions = wrong,
            bookmarks = bookmarks,
            topicStats = stats
        )
        val adapter = moshi.adapter(com.example.model.AppBackupData::class.java)
        adapter.indent("  ").toJson(backup)
    }

    suspend fun importBackupJson(jsonString: String) = withContext(Dispatchers.IO) {
        val adapter = moshi.adapter(com.example.model.AppBackupData::class.java)
        val backup = adapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid backup JSON format")

        if (backup.questionBank.isNotEmpty()) {
            dao.insertQuestionBankItems(backup.questionBank)
        }
        backup.testRecords.forEach { dao.insertTestRecord(it) }
        backup.wrongQuestions.forEach { dao.insertWrongQuestion(it) }
        backup.bookmarks.forEach { dao.insertBookmark(it) }
        backup.topicStats.forEach { dao.insertOrUpdateTopicStat(it) }
    }

    suspend fun saveTestResult(
        quiz: GeneratedQuiz,
        userAnswers: Map<Int, String>,
        timeTakenSeconds: Long,
        modelUsed: String,
        negativeMarkingRatio: Float,
        timerLimitMinutes: Int = 0,
        autoSubmitted: Boolean = false
    ): TestRecordEntity = withContext(Dispatchers.IO) {
        var correct = 0
        var incorrect = 0
        var unattempted = 0

        quiz.questions.forEach { q ->
            val userAns = userAnswers[q.id]
            if (userAns.isNullOrBlank()) {
                unattempted++
            } else if (userAns.equals(q.correctAnswer, ignoreCase = true)) {
                correct++
            } else {
                incorrect++
                // Save to wrong questions table
                dao.insertWrongQuestion(
                    WrongQuestionEntity(
                        testId = 0, // set later or updated
                        questionText = q.question,
                        optionA = q.options.find { it.id == "A" }?.text ?: "",
                        optionB = q.options.find { it.id == "B" }?.text ?: "",
                        optionC = q.options.find { it.id == "C" }?.text ?: "",
                        optionD = q.options.find { it.id == "D" }?.text ?: "",
                        correctAnswer = q.correctAnswer,
                        userSelectedAnswer = userAns,
                        explanation = q.explanation,
                        topic = q.topic,
                        difficulty = q.difficulty
                    )
                )
            }
        }

        val totalQuestions = quiz.questions.size
        val maxScore = totalQuestions.toFloat()
        val rawScore = (correct * 1.0f) - (incorrect * negativeMarkingRatio)
        val finalScore = maxOf(0f, rawScore)
        val percentage = if (maxScore > 0) (finalScore / maxScore) * 100f else 0f
        val accuracy = if (correct + incorrect > 0) (correct.toFloat() / (correct + incorrect)) * 100f else 0f

        val quizAdapter = moshi.adapter(GeneratedQuiz::class.java)
        val questionsJson = quizAdapter.toJson(quiz)

        val record = TestRecordEntity(
            title = quiz.title,
            sourceTopic = quiz.sourceTopic,
            difficulty = quiz.difficulty,
            questionCount = totalQuestions,
            score = finalScore,
            maxScore = maxScore,
            correctCount = correct,
            incorrectCount = incorrect,
            unattemptedCount = unattempted,
            accuracyPercentage = accuracy,
            timeTakenSeconds = timeTakenSeconds,
            modelUsed = modelUsed,
            questionsJson = questionsJson,
            timerLimitMinutes = timerLimitMinutes,
            autoSubmitted = autoSubmitted
        )

        val newId = dao.insertTestRecord(record)

        // Update topic stats
        updateTopicStats(quiz.sourceTopic, correct, incorrect)

        record.copy(id = newId)
    }

    private suspend fun updateTopicStats(topic: String, correct: Int, incorrect: Int) {
        val existing = dao.getTopicStatByName(topic)
        val attempted = (existing?.totalAttempted ?: 0) + correct + incorrect
        val totalCorrect = (existing?.totalCorrect ?: 0) + correct
        val accuracy = if (attempted > 0) (totalCorrect.toFloat() / attempted) * 100f else 0f

        dao.insertOrUpdateTopicStat(
            TopicStatEntity(
                topicName = topic,
                totalAttempted = attempted,
                totalCorrect = totalCorrect,
                accuracyPercentage = accuracy
            )
        )
    }

    suspend fun toggleBookmark(question: McqQuestion) = withContext(Dispatchers.IO) {
        val isBookmarked = dao.isBookmarked(question.question)
        if (isBookmarked) {
            dao.deleteBookmarkByQuestionText(question.question)
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    questionText = question.question,
                    optionA = question.options.find { it.id == "A" }?.text ?: "",
                    optionB = question.options.find { it.id == "B" }?.text ?: "",
                    optionC = question.options.find { it.id == "C" }?.text ?: "",
                    optionD = question.options.find { it.id == "D" }?.text ?: "",
                    correctAnswer = question.correctAnswer,
                    explanation = question.explanation,
                    topic = question.topic,
                    difficulty = question.difficulty
                )
            )
        }
    }

    suspend fun isBookmarked(questionText: String): Boolean = withContext(Dispatchers.IO) {
        dao.isBookmarked(questionText)
    }

    suspend fun markWrongQuestionCorrect(questionId: Long) = withContext(Dispatchers.IO) {
        val unmastered = dao.getUnmasteredWrongQuestions()
        // Simple update to mark as mastered
        dao.deleteWrongQuestionById(questionId)
    }

    suspend fun deleteTestRecord(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTestRecordById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.deleteAllTestRecords()
    }
}
