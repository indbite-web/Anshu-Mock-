package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.MainViewModel
import com.example.data.viewmodel.QuizUiState
import com.example.model.McqQuestion

import androidx.compose.material.icons.filled.Analytics
import com.example.ui.components.AppEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onNavigateHome: () -> Unit,
    onNavigateWrongQuestions: () -> Unit = {},
    onNavigateToQuiz: () -> Unit = {}
) {
    val quizState by viewModel.quizState.collectAsState()
    val resultState = quizState as? QuizUiState.Result

    if (resultState == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Exam Result", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateHome) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            AppEmptyState(
                icon = Icons.Default.Analytics,
                title = "No test result available",
                description = "No detailed analysis could be loaded for this test.",
                actionButtonText = "Back to Home",
                onActionButtonClick = onNavigateHome,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val record = resultState.record
    val quiz = resultState.quiz
    val userAnswers = resultState.userAnswers
    val writtenAnswers by viewModel.writtenAnswers.collectAsState()
    val writtenEvaluationsState by viewModel.writtenEvaluations.collectAsState()

    // Animated Score & Accuracy
    val accuracy = record.accuracyPercentage
    val animatedAccuracy = remember { Animatable(0f) }
    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(accuracy, record.score) {
        animatedAccuracy.animateTo(
            targetValue = accuracy,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
        animatedScore.animateTo(
            targetValue = record.score.toFloat(),
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
    }

    var activeExplainQuestion by remember { mutableStateOf<McqQuestion?>(null) }
    var aiExplanationResult by remember { mutableStateOf<String?>(null) }
    var isGeneratingAiExplain by remember { mutableStateOf(false) }

    // Determine performance label
    val (gradeLabel, gradeColor) = when {
        accuracy >= 90f -> Pair("Excellent", Color(0xFF10B981))
        accuracy >= 75f -> Pair("Very Good", Color(0xFF3B82F6))
        accuracy >= 60f -> Pair("Good", Color(0xFFF59E0B))
        accuracy >= 40f -> Pair("Needs Improvement", Color(0xFFF97316))
        else -> Pair("Weak", Color(0xFFEF4444))
    }

    val timeFormatted = if (record.timeTakenSeconds >= 3600) {
        val h = record.timeTakenSeconds / 3600
        val m = (record.timeTakenSeconds % 3600) / 60
        val s = record.timeTakenSeconds % 60
        "${h}h ${m}m ${s}s"
    } else if (record.timeTakenSeconds >= 60) {
        val m = record.timeTakenSeconds / 60
        val s = record.timeTakenSeconds % 60
        "${m}m ${s}s"
    } else {
        "${record.timeTakenSeconds}s"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exam Result Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetQuizState()
                            onNavigateHome()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (record.autoSubmitted) {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Time's Up",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Time's Up!",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Your test has been submitted automatically.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = gradeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = gradeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "%.1f / %.0f".format(animatedScore.value, record.maxScore),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "%.1f%% Accuracy".format(animatedAccuracy.value),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time & Timer Limit info
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Time Taken", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(timeFormatted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }

                                if (record.timerLimitMinutes > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Time Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${record.timerLimitMinutes} min", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (record.autoSubmitted) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Submission", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Time Expired", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val isFallback = record.modelUsed.contains("(Fallback from ")
                        if (isFallback) {
                            val actualName = record.modelUsed.substringBefore(" (Fallback from").trim()
                            val fromName = record.modelUsed.substringAfter("Fallback from ").removeSuffix(")").trim()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Generated with $actualName",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Fallback from $fromName",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Generated with ${record.modelUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCardMini(label = "Correct", count = "${record.correctCount}", color = Color(0xFF10B981))
                            StatCardMini(label = "Incorrect", count = "${record.incorrectCount}", color = Color(0xFFEF4444))
                            StatCardMini(label = "Unattempted", count = "${record.unattemptedCount}", color = Color(0xFF64748B))
                        }
                    }
                }
            }

            // Answer Review Title
            item {
                Text(
                    text = "Detailed Answer Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Question Review Items (MCQs)
            itemsIndexed(
                items = quiz.questions,
                key = { index, question -> "${question.id}_$index" }
            ) { index, question ->
                val userAns = userAnswers[question.id]
                val isCorrect = userAns.equals(question.correctAnswer, ignoreCase = true)
                val isUnattempted = userAns.isNullOrBlank()

                ReviewQuestionCard(
                    index = index + 1,
                    question = question,
                    userAnswer = userAns,
                    isCorrect = isCorrect,
                    isUnattempted = isUnattempted,
                    onToggleBookmark = { viewModel.toggleBookmark(question) },
                    onAskAiExplain = { q ->
                        activeExplainQuestion = q
                        aiExplanationResult = null
                        isGeneratingAiExplain = true
                        viewModel.explainQuestion(
                            questionText = q.question,
                            optionsText = q.options.joinToString("\n") { "${it.id}: ${it.text}" },
                            correctAnswer = q.correctAnswer,
                            explanation = q.explanation,
                            userQuery = "Explain this question step-by-step.",
                            onResult = { res ->
                                aiExplanationResult = res
                                isGeneratingAiExplain = false
                            }
                        )
                    }
                )
            }

            // Written Question Review Items
            itemsIndexed(
                items = quiz.writtenQuestions,
                key = { index, question -> "written_${question.id}_$index" }
            ) { index, question ->
                val writtenAns = writtenAnswers[question.id] ?: ""
                val eval = writtenEvaluationsState.find { it.questionId == question.id }

                ReviewWrittenCard(
                    index = index + 1,
                    question = question,
                    userAnswer = writtenAns,
                    evaluation = eval
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.startQuestionBankTest(
                                requestedCount = record.questionCount,
                                topicFilter = quiz.sourceTopic
                            )
                            onNavigateToQuiz()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Take Similar Test",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Similar Test (From Bank)")
                    }

                    if (record.incorrectCount > 0) {
                        OutlinedButton(
                            onClick = {
                                onNavigateWrongQuestions()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Retry Wrong Questions",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Wrong Questions (${record.incorrectCount})")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetQuizState()
                            onNavigateHome()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Dashboard")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // AI Explanation Dialog
    activeExplainQuestion?.let { q ->
        AlertDialog(
            onDismissRequest = { activeExplainQuestion = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("AI Step-by-Step Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (isGeneratingAiExplain) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Gemini is breaking down this question...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("QUESTION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(q.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            item {
                                Text(
                                    aiExplanationResult ?: "No explanation returned.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeExplainQuestion = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatCardMini(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReviewQuestionCard(
    index: Int,
    question: McqQuestion,
    userAnswer: String?,
    isCorrect: Boolean,
    isUnattempted: Boolean,
    onToggleBookmark: () -> Unit,
    onAskAiExplain: (McqQuestion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when {
                            isCorrect -> Color(0xFF10B981)
                            isUnattempted -> Color(0xFF64748B)
                            else -> Color(0xFFEF4444)
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Q$index",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            isCorrect -> "Correct"
                            isUnattempted -> "Unattempted"
                            else -> "Incorrect"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isCorrect -> Color(0xFF10B981)
                            isUnattempted -> Color(0xFF64748B)
                            else -> Color(0xFFEF4444)
                        }
                    )
                }

                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Display Options
            question.options.forEach { option ->
                val isCorrectOption = option.id.equals(question.correctAnswer, ignoreCase = true)
                val isUserWrongOption = !isCorrect && option.id.equals(userAnswer, ignoreCase = true)

                val boxColor = when {
                    isCorrectOption -> Color(0xFF10B981).copy(alpha = 0.15f)
                    isUserWrongOption -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }

                val borderColor = when {
                    isCorrectOption -> Color(0xFF10B981)
                    isUserWrongOption -> Color(0xFFEF4444)
                    else -> Color.Transparent
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = boxColor,
                    border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${option.id}.",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isCorrectOption) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Correct",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isUserWrongOption) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Wrong Choice",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Explanation Section
            if (question.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAskAiExplain(question) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI to Explain", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ReviewWrittenCard(
    index: Int,
    question: com.example.model.WrittenQuestion,
    userAnswer: String?,
    evaluation: com.example.model.WrittenEvaluation?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "Written Q$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (evaluation != null) {
                    Surface(
                        color = if (evaluation.percentage >= 70f) Color(0xFF10B981).copy(alpha = 0.15f)
                                else if (evaluation.percentage >= 40f) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Score: %.1f / %d (%.0f%%)".format(evaluation.marksObtained, evaluation.maxMarks, evaluation.percentage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (evaluation.percentage >= 70f) Color(0xFF10B981)
                                    else if (evaluation.percentage >= 40f) Color(0xFFD97706)
                                    else Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Your Answer:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!userAnswer.isNullOrBlank()) userAnswer else "[No answer submitted]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!userAnswer.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (evaluation != null) {
                Spacer(modifier = Modifier.height(10.dp))

                if (evaluation.feedback.isNotBlank()) {
                    Text(
                        text = "AI Feedback: ${evaluation.feedback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (evaluation.correctKeyPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Key points covered: ${evaluation.correctKeyPoints.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (evaluation.missingKeyPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✗ Key points missed: ${evaluation.missingKeyPoints.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (evaluation.suggestedImprovement.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 Suggestion: ${evaluation.suggestedImprovement}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (question.suggestedAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Model Answer / Key Reference:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.suggestedAnswer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
