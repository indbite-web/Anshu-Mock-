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
import com.example.model.WrittenQuestion
import com.example.model.WrittenEvaluation
import com.example.model.WrittenTestEvaluationContainer
import com.example.model.AppBackupData
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

        // Build system prompt based on exam type
        val systemPrompt = when (config.examType) {
            com.example.model.ExamType.MCQ -> """
                You are an expert examination paper setter and MCQ practice question creator.
                Your task is to generate high-quality Multiple Choice Questions (MCQs) in structured JSON format based on the user's provided study material.

                TARGET EXAM: ${config.targetExam}
                SUBJECT: ${config.subject}
                TOPIC: ${config.topic}

                CRITICAL RULES:
                ${if (config.strictSourceMode) "1. STRICT SOURCE MODE: Generate questions ONLY from information explicitly visible/written in the attached pages. DO NOT introduce unstated external facts or invent missing data." else "1. Base questions primarily on the provided study material, extending where relevant for full test coverage."}
                2. Language Requirement: ${config.language}. (If Hindi or Hindi + English, write questions in clear Devanagari Hindi or bilingual Hindi/English).
                3. Difficulty Level: ${config.difficulty}.
                4. Question Style: ${config.style}.
                5. Target Question Count: Exactly ${config.questionCount} questions.
                ${if (config.customInstruction.isNotBlank()) "6. Custom Instruction: '${config.customInstruction}'" else if (config.naturalPrompt.isNotBlank()) "6. Custom Instruction: '${config.naturalPrompt}'" else ""}
                7. Options Requirement: Every question MUST have exactly 4 options labeled "A", "B", "C", and "D". Ensure exactly ONE option is unambiguously correct.
                8. Explanations: Provide clear, concise, step-by-step explanations for the correct answer.
                9. Mathematical & Numerical Notation: Use readable standard math notation (x², √x, ±, ÷, ×).

                JSON OUTPUT FORMAT (STRICT):
                {
                  "title": "${config.targetExam} - ${config.subject} MCQ Test",
                  "examName": "${config.targetExam}",
                  "subject": "${config.subject}",
                  "sourceTopic": "${config.topic}",
                  "difficulty": "${config.difficulty}",
                  "examType": "MCQ",
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
                      "topic": "${config.topic}",
                      "difficulty": "${config.difficulty}"
                    }
                  ]
                }
            """.trimIndent()

            com.example.model.ExamType.WRITTEN -> {
                val writtenDesc = buildString {
                    val types = mutableListOf<String>()
                    if (config.shortWrittenConfig.enabled && config.shortWrittenConfig.count > 0) {
                        types.add("${config.shortWrittenConfig.count} Short Answer questions (${config.shortWrittenConfig.marksEach} marks each, max ~${config.shortWrittenConfig.wordLimit} words)")
                    }
                    if (config.mediumWrittenConfig.enabled && config.mediumWrittenConfig.count > 0) {
                        types.add("${config.mediumWrittenConfig.count} Medium Answer questions (${config.mediumWrittenConfig.marksEach} marks each, max ~${config.mediumWrittenConfig.wordLimit} words)")
                    }
                    if (config.longWrittenConfig.enabled && config.longWrittenConfig.count > 0) {
                        types.add("${config.longWrittenConfig.count} Long / Detailed questions (${config.longWrittenConfig.marksEach} marks each, max ~${config.longWrittenConfig.wordLimit} words)")
                    }
                    if (types.isEmpty()) {
                        append("${config.questionCount} written questions (${config.marksPerQuestion} marks each, ~${config.wordLimit} words)")
                    } else {
                        append(types.joinToString("; "))
                    }
                }

                """
                You are an expert examination paper setter and descriptive test designer.
                Your task is to generate high-quality Written / Short-Answer / Long-Answer questions in structured JSON format.

                TARGET EXAM: ${config.targetExam}
                SUBJECT: ${config.subject}
                TOPIC: ${config.topic}

                CRITICAL RULES:
                ${if (config.strictSourceMode) "1. STRICT SOURCE MODE: Generate questions ONLY from information explicitly visible in the study material." else "1. Base questions on the topic/subject material."}
                2. Language Requirement: ${config.language}.
                3. Difficulty Level: ${config.difficulty}.
                4. WRITTEN BREAKDOWN REQUIRED: Generate $writtenDesc.
                5. Total Target Question Count: Exactly ${config.questionCount} written questions with their specified marks set on each question item.
                ${if (config.customInstruction.isNotBlank()) "6. Custom Instruction: '${config.customInstruction}'" else if (config.naturalPrompt.isNotBlank()) "6. Custom Instruction: '${config.naturalPrompt}'" else ""}
                7. Key Points: List 3 to 6 essential bullet points expected in a full-mark answer.
                8. Suggested Answer: Provide a comprehensive, accurate model answer.

                JSON OUTPUT FORMAT (STRICT):
                {
                  "title": "${config.targetExam} - ${config.subject} Written Test",
                  "examName": "${config.targetExam}",
                  "subject": "${config.subject}",
                  "sourceTopic": "${config.topic}",
                  "difficulty": "${config.difficulty}",
                  "examType": "WRITTEN",
                  "writtenQuestions": [
                    {
                      "id": 1,
                      "question": "Descriptive question text here",
                      "marks": ${config.marksPerQuestion},
                      "suggestedAnswer": "Comprehensive model answer here...",
                      "keyPoints": ["Point 1", "Point 2", "Point 3"],
                      "topic": "${config.topic}",
                      "difficulty": "${config.difficulty}",
                      "subject": "${config.subject}"
                    }
                  ]
                }
            """.trimIndent()
            }

            com.example.model.ExamType.MIXED -> {
                val writtenDesc = buildString {
                    val types = mutableListOf<String>()
                    if (config.shortWrittenConfig.enabled && config.shortWrittenConfig.count > 0) {
                        types.add("${config.shortWrittenConfig.count} Short Answer questions (${config.shortWrittenConfig.marksEach} marks each, max ~${config.shortWrittenConfig.wordLimit} words)")
                    }
                    if (config.mediumWrittenConfig.enabled && config.mediumWrittenConfig.count > 0) {
                        types.add("${config.mediumWrittenConfig.count} Medium Answer questions (${config.mediumWrittenConfig.marksEach} marks each, max ~${config.mediumWrittenConfig.wordLimit} words)")
                    }
                    if (config.longWrittenConfig.enabled && config.longWrittenConfig.count > 0) {
                        types.add("${config.longWrittenConfig.count} Long / Detailed questions (${config.longWrittenConfig.marksEach} marks each, max ~${config.longWrittenConfig.wordLimit} words)")
                    }
                    if (types.isEmpty()) {
                        append("${config.writtenQuestionCount} written questions (${config.marksPerQuestion} marks each, ~${config.wordLimit} words)")
                    } else {
                        append(types.joinToString("; "))
                    }
                }

                """
                You are an expert examination paper setter creating a MIXED test containing BOTH Multiple Choice Questions (MCQs) AND Written Questions.

                TARGET EXAM: ${config.targetExam}
                SUBJECT: ${config.subject}
                TOPIC: ${config.topic}

                CRITICAL RULES:
                1. Generate EXACTLY ${config.mcqQuestionCount} MCQ questions AND ${config.writtenQuestionCount} Written questions.
                2. WRITTEN BREAKDOWN: For the written questions, generate $writtenDesc.
                3. Language Requirement: ${config.language}.
                4. Difficulty Level: ${config.difficulty}.
                5. For MCQs: 4 options ("A", "B", "C", "D"), exactly one correct, with explanation.
                6. For Written Questions: Include specified marks for each, model suggestedAnswer, and keyPoints.

                JSON OUTPUT FORMAT (STRICT):
                {
                  "title": "${config.targetExam} - ${config.subject} Mixed Test",
                  "examName": "${config.targetExam}",
                  "subject": "${config.subject}",
                  "sourceTopic": "${config.topic}",
                  "difficulty": "${config.difficulty}",
                  "examType": "MIXED",
                  "questions": [
                    {
                      "id": 1,
                      "question": "MCQ Question text...",
                      "options": [
                        {"id": "A", "text": "Option A"},
                        {"id": "B", "text": "Option B"},
                        {"id": "C", "text": "Option C"},
                        {"id": "D", "text": "Option D"}
                      ],
                      "correctAnswer": "A",
                      "explanation": "Explanation...",
                      "subject": "${config.subject}",
                      "topic": "${config.topic}",
                      "difficulty": "${config.difficulty}"
                    }
                  ],
                  "writtenQuestions": [
                    {
                      "id": 101,
                      "question": "Written Question text...",
                      "marks": ${config.marksPerQuestion},
                      "suggestedAnswer": "Model answer...",
                      "keyPoints": ["Point 1", "Point 2"],
                      "topic": "${config.topic}",
                      "difficulty": "${config.difficulty}",
                      "subject": "${config.subject}"
                    }
                  ]
                }
            """.trimIndent()
            }
        }

        val parts = mutableListOf<Part>()
        val requestDesc = when (config.examType) {
            com.example.model.ExamType.MCQ -> "Please generate ${config.questionCount} ${config.difficulty} MCQs in JSON format."
            com.example.model.ExamType.WRITTEN -> "Please generate ${config.questionCount} ${config.difficulty} written questions in JSON format."
            com.example.model.ExamType.MIXED -> "Please generate ${config.mcqQuestionCount} MCQs and ${config.writtenQuestionCount} written questions in JSON format."
        }
        parts.add(Part(text = requestDesc))

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
                    android.util.Log.e("ExamRepository", "JSON parse error from ${model.displayName}", e)
                    val detail = e.localizedMessage ?: "Failed to parse questions"
                    throw IllegalArgumentException("Failed to parse quiz response from ${model.displayName}: $detail")
                }

                val hasContent = (parsedQuiz.questions.isNotEmpty() || parsedQuiz.writtenQuestions.isNotEmpty())
                if (hasContent) {
                    saveQuestionsToBank(parsedQuiz)
                    val wasFallback = (model != initialModel)
                    return@withContext GenerationResult(
                        quiz = parsedQuiz,
                        selectedModel = initialModel.displayName,
                        actualModelUsed = model.displayName,
                        wasFallback = wasFallback
                    )
                } else {
                    throw IllegalArgumentException("Gemini (${model.displayName}) response did not contain valid questions.")
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
                    throw IllegalStateException(formatted)
                }
            }
        }

        val finalMsg = lastFallbackError ?: "Couldn't generate practice test. Gemini free-tier models were unavailable."
        throw IllegalStateException(finalMsg)
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
            if (quiz != null && (quiz.questions.isNotEmpty() || quiz.writtenQuestions.isNotEmpty())) {
                val validatedMcqs = validateAndNormalizeQuestions(quiz.questions, config)
                return quiz.copy(questions = validatedMcqs)
            }
        } catch (e: Exception) {
            android.util.Log.w("ExamRepository", "Moshi parsing failed, falling back to org.json parser", e)
        }

        // Fallback: org.json.JSONObject / JSONArray manual parsing
        val mcqList = mutableListOf<McqQuestion>()
        val writtenList = mutableListOf<WrittenQuestion>()
        var quizTitle = "${config.targetExam} - ${config.subject} Test"
        var examName = config.targetExam
        var subjectName = config.subject
        var sourceTopic = config.topic
        var diff = config.difficulty
        var examTypeStr = config.examType.name

        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val jsonArr = org.json.JSONArray(trimmed)
                if (config.examType == com.example.model.ExamType.WRITTEN) {
                    parseJsonArrayToWrittenQuestions(jsonArr, writtenList, config)
                } else {
                    parseJsonArrayToQuestions(jsonArr, mcqList, config)
                }
            } else {
                val jsonObj = org.json.JSONObject(trimmed)
                quizTitle = jsonObj.optString("title", quizTitle)
                examName = jsonObj.optString("examName", examName)
                subjectName = jsonObj.optString("subject", subjectName)
                sourceTopic = jsonObj.optString("sourceTopic", jsonObj.optString("topic", sourceTopic))
                diff = jsonObj.optString("difficulty", diff)
                examTypeStr = jsonObj.optString("examType", examTypeStr)

                val questionsArr = jsonObj.optJSONArray("questions")
                if (questionsArr != null) {
                    parseJsonArrayToQuestions(questionsArr, mcqList, config)
                }

                val writtenArr = jsonObj.optJSONArray("writtenQuestions")
                if (writtenArr != null) {
                    parseJsonArrayToWrittenQuestions(writtenArr, writtenList, config)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Manual JSON parsing error", e)
            throw IllegalArgumentException("Failed to parse questions from Gemini response.")
        }

        val validatedQuestions = validateAndNormalizeQuestions(mcqList, config)
        if (validatedQuestions.isEmpty() && writtenList.isEmpty()) {
            throw IllegalArgumentException("Gemini response did not contain any valid questions.")
        }

        val parsedType = when (examTypeStr.uppercase()) {
            "WRITTEN" -> com.example.model.ExamType.WRITTEN
            "MIXED" -> com.example.model.ExamType.MIXED
            else -> com.example.model.ExamType.MCQ
        }

        return GeneratedQuiz(
            title = quizTitle,
            examName = examName,
            subject = subjectName,
            sourceTopic = sourceTopic,
            difficulty = diff,
            examType = parsedType.name,
            questions = validatedQuestions,
            writtenQuestions = writtenList
        )
    }

    private fun parseJsonArrayToWrittenQuestions(
        jsonArr: org.json.JSONArray,
        targetList: MutableList<WrittenQuestion>,
        config: TestConfig
    ) {
        for (i in 0 until jsonArr.length()) {
            try {
                val qObj = jsonArr.getJSONObject(i)
                val qText = qObj.optString("question", qObj.optString("questionText", ""))
                if (qText.isBlank()) continue

                val marks = qObj.optInt("marks", config.marksPerQuestion)
                val suggestedAnswer = qObj.optString("suggestedAnswer", qObj.optString("modelAnswer", ""))
                val keyPointsList = mutableListOf<String>()
                val kpArr = qObj.optJSONArray("keyPoints")
                if (kpArr != null) {
                    for (j in 0 until kpArr.length()) {
                        val kp = kpArr.optString(j)
                        if (kp.isNotBlank()) keyPointsList.add(kp)
                    }
                }

                val subject = qObj.optString("subject", config.subject)
                val topic = qObj.optString("topic", config.topic)
                val difficulty = qObj.optString("difficulty", config.difficulty)

                targetList.add(
                    WrittenQuestion(
                        id = i + 1,
                        question = qText,
                        marks = marks,
                        suggestedAnswer = suggestedAnswer,
                        keyPoints = keyPointsList,
                        topic = topic,
                        difficulty = difficulty,
                        subject = subject
                    )
                )
            } catch (e: Exception) {
                // Skip malformed item
            }
        }
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
        val items = mutableListOf<QuestionBankEntity>()

        quiz.questions.forEach { q ->
            items.add(
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
                    testSourceRef = quiz.title,
                    questionType = "MCQ"
                )
            )
        }

        quiz.writtenQuestions.forEach { wq ->
            val keyPointsStr = try {
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
                moshi.adapter<List<String>>(listType).toJson(wq.keyPoints)
            } catch (e: Exception) { "[]" }

            items.add(
                QuestionBankEntity(
                    questionText = wq.question,
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctAnswer = "",
                    explanation = wq.suggestedAnswer,
                    examName = quiz.examName.ifBlank { "General Practice" },
                    subject = wq.subject.ifBlank { quiz.subject.ifBlank { "General" } },
                    topic = wq.topic.ifBlank { quiz.sourceTopic.ifBlank { "General" } },
                    difficulty = wq.difficulty.ifBlank { quiz.difficulty.ifBlank { "Medium" } },
                    testSourceRef = quiz.title,
                    questionType = "WRITTEN",
                    suggestedAnswer = wq.suggestedAnswer,
                    keyPointsJson = keyPointsStr,
                    marks = wq.marks
                )
            )
        }

        if (items.isNotEmpty()) {
            dao.insertQuestionBankItems(items)
        }
    }

    suspend fun evaluateWrittenTest(
        questions: List<WrittenQuestion>,
        userAnswers: Map<Int, String>,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String,
        onStatusUpdate: (String) -> Unit = {}
    ): List<WrittenEvaluation> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required for test evaluation. Please check Settings → AI Configuration.")
        }

        val questionsPayload = questions.map { q ->
            mapOf(
                "questionId" to q.id,
                "question" to q.question,
                "maxMarks" to q.marks,
                "suggestedAnswer" to q.suggestedAnswer,
                "keyPoints" to q.keyPoints,
                "studentAnswer" to (userAnswers[q.id]?.trim() ?: "")
            )
        }

        val systemPrompt = """
            You are an objective, expert examination paper evaluator.
            Evaluate the student's written answer for each question provided against the expected model answer and key points.

            CRITICAL RULES:
            1. Evaluate fairly. Award marks (marksObtained) strictly between 0 and maxMarks.
            2. If student answer is empty, blank, or completely irrelevant, award 0 marks.
            3. Calculate percentage = (marksObtained / maxMarks) * 100.
            4. Provide concise, constructive feedback (1-3 sentences) explaining the grade.
            5. Identify which key points were correctly covered (correctKeyPoints) and which were missing/incomplete (missingKeyPoints).
            6. Provide actionable suggestedImprovement.

            JSON OUTPUT FORMAT (STRICT):
            {
              "evaluations": [
                {
                  "questionId": 1,
                  "marksObtained": 4.0,
                  "maxMarks": 5,
                  "percentage": 80.0,
                  "feedback": "Good explanation of core concepts.",
                  "correctKeyPoints": ["Point A"],
                  "missingKeyPoints": ["Point B"],
                  "suggestedImprovement": "Explicitly define the initial condition."
                }
              ]
            }
        """.trimIndent()

        val payloadJson = try {
            val mapAdapter = moshi.adapter(Any::class.java)
            mapAdapter.toJson(mapOf("questionsToEvaluate" to questionsPayload))
        } catch (e: Exception) {
            "[]"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Evaluate these student answers in JSON format:\n$payloadJson")))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST
        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { if (it != initialModel && !list.contains(it)) list.add(it) }
            list
        } else {
            listOf(initialModel)
        }

        var lastError: String? = null

        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue
            onStatusUpdate("Evaluating test using AI (${model.displayName})...")

            try {
                val response = GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
                if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                    val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    val jsonStr = extractJsonSubstring(rawText)
                    if (jsonStr.isNotBlank()) {
                        try {
                            val containerAdapter = moshi.adapter(WrittenTestEvaluationContainer::class.java)
                            val container = containerAdapter.fromJson(jsonStr)
                            if (container != null && container.evaluations.isNotEmpty()) {
                                return@withContext container.evaluations
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ExamRepository", "Moshi evaluation parse failed, trying org.json fallback", e)
                        }

                        // Manual org.json fallback
                        val manualEvals = parseWrittenEvaluationsManually(jsonStr, questions)
                        if (manualEvals.isNotEmpty()) {
                            return@withContext manualEvals
                        }
                    }
                } else {
                    lastError = formatGeminiApiHttpError(response.code(), response.errorBody()?.string() ?: "", model.displayName)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExamRepository", "Evaluation call failed on ${model.displayName}", e)
                lastError = formatGeminiException(e, model.displayName)
            }
        }

        throw IllegalStateException(lastError ?: "AI Evaluation failed. Please try again.")
    }

    private fun parseWrittenEvaluationsManually(
        jsonStr: String,
        questions: List<WrittenQuestion>
    ): List<WrittenEvaluation> {
        val list = mutableListOf<WrittenEvaluation>()
        try {
            val jsonObj = org.json.JSONObject(jsonStr)
            val arr = jsonObj.optJSONArray("evaluations") ?: org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val qId = item.optInt("questionId", i + 1)
                val matchingQ = questions.find { it.id == qId } ?: questions.getOrNull(i)
                val maxM = matchingQ?.marks ?: item.optInt("maxMarks", 5)
                val marksObtained = item.optDouble("marksObtained", 0.0).toFloat()
                val pct = if (maxM > 0) (marksObtained / maxM.toFloat()) * 100f else 0f

                val correctKp = mutableListOf<String>()
                val cArr = item.optJSONArray("correctKeyPoints")
                if (cArr != null) {
                    for (j in 0 until cArr.length()) correctKp.add(cArr.optString(j))
                }

                val missingKp = mutableListOf<String>()
                val mArr = item.optJSONArray("missingKeyPoints")
                if (mArr != null) {
                    for (j in 0 until mArr.length()) missingKp.add(mArr.optString(j))
                }

                list.add(
                    WrittenEvaluation(
                        questionId = qId,
                        marksObtained = marksObtained,
                        maxMarks = maxM,
                        percentage = pct,
                        feedback = item.optString("feedback", "Evaluation completed."),
                        correctKeyPoints = correctKp,
                        missingKeyPoints = missingKp,
                        suggestedImprovement = item.optString("suggestedImprovement", "")
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Error manually parsing evaluations JSON", e)
        }
        return list
    }

    suspend fun saveTestResult(
        quiz: GeneratedQuiz,
        userAnswers: Map<Int, String>,
        writtenAnswers: Map<Int, String> = emptyMap(),
        evaluations: List<WrittenEvaluation> = emptyList(),
        timeTakenSeconds: Long,
        modelUsed: String,
        negativeMarkingRatio: Float,
        timerLimitMinutes: Int = 0,
        autoSubmitted: Boolean = false
    ): TestRecordEntity = withContext(Dispatchers.IO) {
        var mcqCorrect = 0
        var mcqIncorrect = 0
        var mcqUnattempted = 0

        quiz.questions.forEach { q ->
            val userAns = userAnswers[q.id]
            if (userAns.isNullOrBlank()) {
                mcqUnattempted++
            } else if (userAns.equals(q.correctAnswer, ignoreCase = true)) {
                mcqCorrect++
            } else {
                mcqIncorrect++
                dao.insertWrongQuestion(
                    WrongQuestionEntity(
                        testId = 0,
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

        var writtenTotalMarks = 0f
        var writtenObtainedMarks = 0f
        var writtenUnattempted = 0
        var writtenCorrect = 0
        var writtenIncorrect = 0

        quiz.writtenQuestions.forEach { wq ->
            writtenTotalMarks += wq.marks
            val ans = writtenAnswers[wq.id]?.trim() ?: ""
            if (ans.isBlank()) {
                writtenUnattempted++
            } else {
                val eval = evaluations.find { it.questionId == wq.id }
                if (eval != null) {
                    writtenObtainedMarks += eval.marksObtained
                    if (eval.percentage >= 70f) writtenCorrect++ else writtenIncorrect++
                } else {
                    writtenIncorrect++
                }
            }
        }

        val mcqMaxScore = quiz.questions.size.toFloat()
        val mcqRawScore = (mcqCorrect * 1.0f) - (mcqIncorrect * negativeMarkingRatio)
        val mcqFinalScore = maxOf(0f, mcqRawScore)

        val totalQuestions = quiz.questions.size + quiz.writtenQuestions.size
        val maxScore = mcqMaxScore + writtenTotalMarks
        val finalScore = mcqFinalScore + writtenObtainedMarks

        val totalCorrect = mcqCorrect + writtenCorrect
        val totalIncorrect = mcqIncorrect + writtenIncorrect
        val totalUnattempted = mcqUnattempted + writtenUnattempted

        val percentage = if (maxScore > 0) (finalScore / maxScore) * 100f else 0f
        val attemptedCount = totalQuestions - totalUnattempted
        val accuracy = if (attemptedCount > 0) (totalCorrect.toFloat() / attemptedCount) * 100f else 0f

        val quizAdapter = moshi.adapter(GeneratedQuiz::class.java)
        val questionsJson = quizAdapter.toJson(quiz)

        val writtenAnswersJsonStr = try {
            val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, String::class.java)
            moshi.adapter<Map<Int, String>>(mapType).toJson(writtenAnswers)
        } catch (e: Exception) { "{}" }

        val evaluationsJsonStr = try {
            val containerAdapter = moshi.adapter(WrittenTestEvaluationContainer::class.java)
            containerAdapter.toJson(WrittenTestEvaluationContainer(evaluations))
        } catch (e: Exception) { "{}" }

        val record = TestRecordEntity(
            title = quiz.title,
            sourceTopic = quiz.sourceTopic,
            difficulty = quiz.difficulty,
            questionCount = totalQuestions,
            score = finalScore,
            maxScore = maxScore,
            correctCount = totalCorrect,
            incorrectCount = totalIncorrect,
            unattemptedCount = totalUnattempted,
            accuracyPercentage = accuracy,
            timeTakenSeconds = timeTakenSeconds,
            modelUsed = modelUsed,
            questionsJson = questionsJson,
            timerLimitMinutes = timerLimitMinutes,
            autoSubmitted = autoSubmitted,
            examType = quiz.examType,
            writtenAnswersJson = writtenAnswersJsonStr,
            evaluationsJson = evaluationsJsonStr
        )

        val newId = dao.insertTestRecord(record)

        if (quiz.questions.isNotEmpty()) {
            updateTopicStats(quiz.sourceTopic, mcqCorrect, mcqIncorrect)
        }

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

    suspend fun createTestFromQuestionBank(
        requestedCount: Int = 10,
        topicFilter: String? = null,
        subjectFilter: String? = null,
        examFilter: String? = null
    ): GeneratedQuiz = withContext(Dispatchers.IO) {
        val allItems = dao.getAllQuestionBankItemsList()
        val filtered = allItems.filter { item ->
            val matchTopic = topicFilter.isNullOrBlank() || item.topic.equals(topicFilter, ignoreCase = true)
            val matchSubject = subjectFilter.isNullOrBlank() || item.subject.equals(subjectFilter, ignoreCase = true)
            val matchExam = examFilter.isNullOrBlank() || item.examName.equals(examFilter, ignoreCase = true)
            matchTopic && matchSubject && matchExam
        }

        if (filtered.isEmpty()) {
            throw IllegalStateException("No saved questions found matching filters in Question Bank.")
        }

        val selectedItems = filtered.shuffled().take(requestedCount)
        val mcqQuestions = selectedItems.mapIndexed { index, item ->
            McqQuestion(
                id = index + 1,
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
            title = "Practice Test from Question Bank",
            examName = selectedItems.firstOrNull()?.examName ?: "Question Bank Practice",
            subject = subjectFilter ?: selectedItems.firstOrNull()?.subject ?: "Mixed Subjects",
            sourceTopic = topicFilter ?: "Question Bank Collection",
            difficulty = selectedItems.firstOrNull()?.difficulty ?: "Medium",
            questions = mcqQuestions
        )
    }

    suspend fun createCustomComposedTestFromBank(subjectCounts: Map<String, Int>): GeneratedQuiz = withContext(Dispatchers.IO) {
        val allItems = dao.getAllQuestionBankItemsList()
        val mcqQuestions = mutableListOf<McqQuestion>()
        var currentIndex = 1

        subjectCounts.forEach { (subject, count) ->
            if (count > 0) {
                val subjectItems = allItems.filter { it.subject.equals(subject, ignoreCase = true) }.shuffled().take(count)
                subjectItems.forEach { item ->
                    mcqQuestions.add(
                        McqQuestion(
                            id = currentIndex++,
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
                    )
                }
            }
        }

        if (mcqQuestions.isEmpty()) {
            throw IllegalStateException("No questions available for the selected custom composition.")
        }

        GeneratedQuiz(
            title = "Custom Composed Test",
            examName = "Custom Question Bank Test",
            subject = "Multi-Subject",
            sourceTopic = "Custom Combination",
            difficulty = "Mixed",
            questions = mcqQuestions
        )
    }

    suspend fun generateStudyNotes(
        subject: String,
        topic: String,
        customInstructions: String,
        language: String,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String,
        onStatusUpdate: (String) -> Unit = {}
    ): com.example.model.GeneratedStudyNotes = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required. Please configure your API key in Settings → AI Configuration.")
        }

        val langInstruction = when (language.trim().lowercase()) {
            "hindi" -> "Generate the content in clear Hindi using Devanagari script."
            "hinglish" -> "Generate the content in natural Roman Hindi/Hinglish."
            else -> "Generate the content completely in English."
        }

        val systemPrompt = """
            You are an expert examination study guide creator and educational content designer.
            Your task is to generate structured study material for the given subject and topic.

            SUBJECT: $subject
            TOPIC: $topic
            LANGUAGE DIRECTIVE: $langInstruction
            ${if (customInstructions.isNotBlank()) "CUSTOM INSTRUCTIONS: $customInstructions" else ""}

            CRITICAL RULES:
            1. Language Requirement: $langInstruction
            2. Output MUST be a clean, study-friendly structure. Do NOT make response bloated.
            3. Response MUST be valid JSON strictly matching this schema:

            {
              "title": "$topic Study Notes",
              "summary": "Clear, concise 2-3 sentence overview of $topic...",
              "importantConcepts": [
                "Concept 1 with brief explanation",
                "Concept 2 with brief explanation"
              ],
              "keyDefinitions": [
                "Term 1: Concise definition",
                "Term 2: Concise definition"
              ],
              "examPoints": [
                "High-yield point 1 commonly asked in exams",
                "High-yield point 2 commonly asked in exams"
              ],
              "examples": [
                "Real-world example or practical illustration"
              ],
              "quickRevision": [
                "Key bullet point for 1-minute quick revision before exam"
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Please generate structured study notes for $topic ($subject) in JSON format. $langInstruction")))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.3f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST
        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { fallback -> if (fallback != initialModel && !list.contains(fallback)) list.add(fallback) }
            list
        } else listOf(initialModel)

        var lastError: String? = null
        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue
            onStatusUpdate("Generating study notes using ${model.displayName}...")
            val response = try {
                GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
            } catch (e: Exception) {
                lastError = formatGeminiException(e, model.displayName)
                continue
            }

            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val jsonString = extractJsonSubstring(rawText)
                if (jsonString.isNotBlank()) {
                    try {
                        val adapter = moshi.adapter(com.example.model.GeneratedStudyNotes::class.java)
                        val result = adapter.fromJson(jsonString)
                        if (result != null && result.title.isNotBlank()) return@withContext result
                    } catch (e: Exception) {
                        android.util.Log.w("ExamRepository", "Error parsing study notes JSON", e)
                    }
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                lastError = formatGeminiApiHttpError(code, errBody, model.displayName)
            }
        }
        throw IllegalStateException(lastError ?: "Failed to generate study notes.")
    }

    suspend fun generateFlashcards(
        subject: String,
        topic: String,
        count: Int = 8,
        language: String,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String,
        onStatusUpdate: (String) -> Unit = {}
    ): com.example.model.GeneratedFlashcardSet = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required.")
        }

        val langInstruction = when (language.trim().lowercase()) {
            "hindi" -> "Generate the content in clear Hindi using Devanagari script."
            "hinglish" -> "Generate the content in natural Roman Hindi/Hinglish."
            else -> "Generate the content completely in English."
        }

        val systemPrompt = """
            You are an expert educational flashcard creator.
            Generate $count high-yield study flashcards.

            SUBJECT: $subject
            TOPIC: $topic
            LANGUAGE DIRECTIVE: $langInstruction

            CRITICAL RULES:
            1. Language: $langInstruction.
            2. Front: Clear question, concept, or term.
            3. Back: Direct, accurate answer, definition, or explanation.
            4. JSON Output Schema (STRICT):

            {
              "subject": "$subject",
              "topic": "$topic",
              "flashcards": [
                {
                  "frontText": "Front question/concept text",
                  "backText": "Back answer/explanation text"
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Please generate $count flashcards for $topic ($subject) in JSON format. $langInstruction")))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.3f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST
        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { fallback -> if (fallback != initialModel && !list.contains(fallback)) list.add(fallback) }
            list
        } else listOf(initialModel)

        var lastError: String? = null
        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue
            onStatusUpdate("Generating flashcards using ${model.displayName}...")
            val response = try {
                GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
            } catch (e: Exception) {
                lastError = formatGeminiException(e, model.displayName)
                continue
            }

            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val jsonString = extractJsonSubstring(rawText)
                if (jsonString.isNotBlank()) {
                    try {
                        val adapter = moshi.adapter(com.example.model.GeneratedFlashcardSet::class.java)
                        val result = adapter.fromJson(jsonString)
                        if (result != null && result.flashcards.isNotEmpty()) return@withContext result
                    } catch (e: Exception) {
                        android.util.Log.w("ExamRepository", "Error parsing flashcards JSON", e)
                    }
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                lastError = formatGeminiApiHttpError(code, errBody, model.displayName)
            }
        }
        throw IllegalStateException(lastError ?: "Failed to generate flashcards.")
    }

    suspend fun solveDoubt(
        subject: String,
        topic: String,
        doubt: String,
        language: String,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String,
        onStatusUpdate: (String) -> Unit = {}
    ): com.example.model.GeneratedDoubtResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required.")
        }

        val langInstruction = when (language.trim().lowercase()) {
            "hindi" -> "Generate the content in clear Hindi using Devanagari script."
            "hinglish" -> "Generate the content in natural Roman Hindi/Hinglish."
            else -> "Generate the content completely in English."
        }

        val systemPrompt = """
            You are an educational AI Doubt Solver.
            Answer the student's question accurately, educationally, and concisely.

            ${if (subject.isNotBlank()) "SUBJECT: $subject" else ""}
            ${if (topic.isNotBlank()) "TOPIC: $topic" else ""}
            LANGUAGE DIRECTIVE: $langInstruction

            CRITICAL RULES:
            1. Language: $langInstruction.
            2. Be direct and educational.
            3. Return JSON format strictly matching:

            {
              "directAnswer": "Direct 1-2 sentence core answer",
              "simpleExplanation": "Simple, accessible explanation",
              "detailedExplanation": "Deeper step-by-step breakdown or context",
              "examPoint": "Key exam tip or takeaway related to this question"
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Doubt: $doubt. $langInstruction")))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST
        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { fallback -> if (fallback != initialModel && !list.contains(fallback)) list.add(fallback) }
            list
        } else listOf(initialModel)

        var lastError: String? = null
        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue
            onStatusUpdate("Solving doubt using ${model.displayName}...")
            val response = try {
                GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
            } catch (e: Exception) {
                lastError = formatGeminiException(e, model.displayName)
                continue
            }

            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val jsonString = extractJsonSubstring(rawText)
                if (jsonString.isNotBlank()) {
                    try {
                        val adapter = moshi.adapter(com.example.model.GeneratedDoubtResponse::class.java)
                        val result = adapter.fromJson(jsonString)
                        if (result != null) return@withContext result
                    } catch (e: Exception) {
                        android.util.Log.w("ExamRepository", "Error parsing doubt JSON", e)
                    }
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                lastError = formatGeminiApiHttpError(code, errBody, model.displayName)
            }
        }
        throw IllegalStateException(lastError ?: "Failed to solve doubt.")
    }

    suspend fun explainQuestion(
        questionText: String,
        optionsText: String,
        correctAnswer: String,
        explanation: String,
        userQuery: String,
        language: String,
        preferredModelId: String,
        autoFallback: Boolean,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key required.")
        }

        val promptText = """
            QUESTION:
            $questionText

            OPTIONS:
            $optionsText

            CORRECT ANSWER:
            $correctAnswer

            ${if (explanation.isNotBlank()) "EXISTING EXPLANATION:\n$explanation" else ""}

            USER INQUIRY:
            ${if (userQuery.isNotBlank()) userQuery else "Please explain step-by-step why the correct answer is $correctAnswer."}

            LANGUAGE: $language
        """.trimIndent()

        val systemPrompt = "You are an expert exam tutor explaining a practice question clearly and encouragingly to a student in $language."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptText)))),
            generationConfig = GenerationConfig(temperature = 0.3f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val initialModel = SupportedModel.fromModelId(preferredModelId)
        val allowlist = SupportedModel.FREE_MODEL_ALLOWLIST
        val modelsToTry = if (autoFallback) {
            val list = mutableListOf(initialModel)
            allowlist.forEach { fallback -> if (fallback != initialModel && !list.contains(fallback)) list.add(fallback) }
            list
        } else listOf(initialModel)

        for (model in modelsToTry) {
            if (!allowlist.contains(model)) continue
            val response = try {
                GeminiClient.apiService.generateContent(model.modelId, apiKey, request)
            } catch (e: Exception) {
                continue
            }

            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text
            }
        }
        return@withContext "Unable to generate explanation right now. Please check your connection and API key."
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val qBank = dao.getAllQuestionBankItemsList()
        val records = dao.getAllTestRecordsList()
        val wrongs = dao.getUnmasteredWrongQuestionsList()
        val bookmarks = dao.getAllBookmarksList()
        val stats = dao.getAllTopicStatsList()
        val notes = dao.getAllStudyNotesList()
        val flashcards = dao.getAllFlashcardsList()

        val backup = AppBackupData(
            exportTimestamp = System.currentTimeMillis(),
            appVersion = "1.2.0",
            questionBank = qBank,
            testRecords = records,
            wrongQuestions = wrongs,
            bookmarks = bookmarks,
            topicStats = stats,
            studyNotes = notes,
            flashcards = flashcards
        )

        moshi.adapter(AppBackupData::class.java).toJson(backup)
    }

    suspend fun importBackupJson(jsonString: String) = withContext(Dispatchers.IO) {
        val backup = moshi.adapter(AppBackupData::class.java).fromJson(jsonString)
            ?: throw IllegalArgumentException("Invalid backup JSON payload.")

        if (backup.questionBank.isNotEmpty()) dao.insertQuestionBankItems(backup.questionBank)
        if (backup.testRecords.isNotEmpty()) dao.insertTestRecords(backup.testRecords)
        if (backup.wrongQuestions.isNotEmpty()) dao.insertWrongQuestions(backup.wrongQuestions)
        if (backup.bookmarks.isNotEmpty()) dao.insertBookmarks(backup.bookmarks)
        if (backup.topicStats.isNotEmpty()) dao.insertTopicStats(backup.topicStats)
        if (backup.studyNotes.isNotEmpty()) dao.insertStudyNotes(backup.studyNotes)
        if (backup.flashcards.isNotEmpty()) dao.insertFlashcards(backup.flashcards)
    }

    private fun formatGeminiException(e: Exception, modelName: String): String {
        return "Network error calling $modelName: ${e.localizedMessage ?: "Connection failed"}"
    }

    private fun formatGeminiApiHttpError(code: Int, errorBody: String, modelName: String): String {
        return when {
            code in listOf(400, 401, 403) || errorBody.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Invalid API Key for $modelName. Please update your API key in Settings."
            code == 429 || errorBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || errorBody.contains("quota", ignoreCase = true) ->
                "Rate limit / Free-tier quota exceeded for $modelName."
            code == 404 || errorBody.contains("MODEL_NOT_FOUND", ignoreCase = true) ->
                "Model $modelName is currently unavailable."
            else ->
                "HTTP $code error from $modelName: $errorBody"
        }
    }

    private fun isFallbackEligibleError(code: Int, errorBody: String): Boolean {
        return code == 429 || code == 404 || code >= 500 ||
                errorBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                errorBody.contains("quota", ignoreCase = true) ||
                errorBody.contains("MODEL_NOT_FOUND", ignoreCase = true) ||
                errorBody.contains("UNAVAILABLE", ignoreCase = true)
    }
}
