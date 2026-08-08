package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.MainViewModel
import com.example.data.viewmodel.QuizUiState
import com.example.model.McqQuestion

import androidx.compose.material.icons.filled.Quiz
import com.example.ui.components.AppEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    onNavigateHome: () -> Unit,
    onNavigateToResult: () -> Unit = {}
) {
    val quizState by viewModel.quizState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(quizState) {
        if (quizState is QuizUiState.Result) {
            onNavigateToResult()
        }
    }

    when (val currentState = quizState) {
        is QuizUiState.Generating -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Creating Practice Test", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateHome) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Generating Questions...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentState.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onNavigateHome) {
                            Text("Cancel")
                        }
                    }
                }
            }
            return
        }

        is QuizUiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Practice Test Error", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateHome) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Unable to Load Practice Test",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(onClick = onNavigateHome) {
                                    Text("Go Back")
                                }
                                Button(
                                    onClick = { viewModel.retryLastTest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        is QuizUiState.Result -> {
            Box(modifier = Modifier.fillMaxSize())
            return
        }

        is QuizUiState.Idle -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Practice Test", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateHome) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                AppEmptyState(
                    icon = Icons.Default.Quiz,
                    title = "No active test session",
                    description = "Start a new practice test from the dashboard.",
                    actionButtonText = "Go to Home",
                    onActionButtonClick = onNavigateHome,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            return
        }

        is QuizUiState.Active -> {
            val quiz = currentState.quiz
            val mcqQuestions = quiz.questions
            val writtenQuestions = quiz.writtenQuestions
            val totalMcqs = mcqQuestions.size
            val totalWritten = writtenQuestions.size
            val totalQuestions = totalMcqs + totalWritten

            if (totalQuestions == 0) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Practice Test", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateHome) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AppEmptyState(
                        icon = Icons.Default.Quiz,
                        title = "No questions found",
                        description = "No questions were found for this test session.",
                        actionButtonText = "Go Back to Home",
                        onActionButtonClick = onNavigateHome,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                return
            }

            val userAnswers by viewModel.userAnswers.collectAsState()
            val writtenAnswers by viewModel.writtenAnswers.collectAsState()
            val markedForReview by viewModel.markedForReview.collectAsState()
            val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
            val bookmarkedQuestions by viewModel.bookmarkedQuestions.collectAsState()
            val isEvaluating by viewModel.isEvaluating.collectAsState()
            val evaluationError by viewModel.evaluationError.collectAsState()

            var showPaletteSheet by remember { mutableStateOf(false) }
            var showSubmitConfirmation by remember { mutableStateOf(false) }

            val isCurrentMcq = currentQuestionIndex < totalMcqs
            val currentMcq = if (isCurrentMcq) mcqQuestions.getOrNull(currentQuestionIndex) else null
            val currentWritten = if (!isCurrentMcq) writtenQuestions.getOrNull(currentQuestionIndex - totalMcqs) else null

            val currentQuestionId = currentMcq?.id ?: currentWritten?.id ?: ""

            val answeredCount = userAnswers.size + writtenAnswers.filterValues { it.isNotBlank() }.size
            val unansweredCount = (totalQuestions - answeredCount).coerceAtLeast(0)
            val markedCount = markedForReview.size

            val isTimerEnabled = currentState.config.timerModeMinutes > 0

            val isCurrentBookmarked = remember(currentMcq, currentWritten, bookmarkedQuestions) {
                if (currentMcq != null) {
                    bookmarkedQuestions.any { it.questionText == currentMcq.question }
                } else false
            }

            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // ROW 1: [Back Arrow] Question X / Y [Timer] [Grid Icon]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { showSubmitConfirmation = true },
                                    modifier = Modifier.testTag("back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Exit Test",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Question ${currentQuestionIndex + 1} / $totalQuestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                 Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    QuizTimerBadge(
                                        timeRemainingFlow = viewModel.timeRemainingSeconds,
                                        isTimerEnabled = isTimerEnabled
                                    )

                                    IconButton(
                                        onClick = { showPaletteSheet = true },
                                        modifier = Modifier.testTag("palette_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Question Palette",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // ROW 2: Subject/Exam Title
                            val subjectTitle = remember(quiz) {
                                val topic = quiz.sourceTopic.ifEmpty { quiz.examName }
                                if (quiz.subject.isNotEmpty() && !topic.contains(quiz.subject, ignoreCase = true)) {
                                    "$topic • ${quiz.subject}"
                                } else {
                                    topic
                                }
                            }
                            Text(
                                text = subjectTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                            )

                            if (currentState.wasFallback) {
                                Surface(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Generated with ${currentState.modelUsed} • Fallback from ${currentState.selectedModel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // ROW 3: Progress Bar
                            val animatedProgress by animateFloatAsState(
                                targetValue = if (totalQuestions > 0) (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat() else 0f,
                                animationSpec = tween(durationMillis = 300),
                                label = "quiz_progress"
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .testTag("session_progress_bar"),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // ROW 4: Status Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(label = "Answered", count = answeredCount, color = Color(0xFF10B981))
                                StatusChip(label = "Unanswered", count = unansweredCount, color = Color(0xFF64748B))
                                StatusChip(label = "Review", count = markedCount, color = Color(0xFFA855F7))
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentQuestionIndex > 0) {
                                    IconButton(
                                        onClick = { viewModel.setCurrentQuestionIndex(currentQuestionIndex - 1) },
                                        modifier = Modifier.testTag("prev_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Previous Question",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                val activeQuestionId = currentMcq?.id ?: currentWritten?.id ?: 0
                                val isMarked = markedForReview.contains(activeQuestionId)
                                OutlinedButton(
                                    onClick = { if (activeQuestionId != 0) viewModel.toggleMarkForReview(activeQuestionId) },
                                    modifier = Modifier.testTag("mark_review_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMarked) Color(0xFFA855F7) else MaterialTheme.colorScheme.outline
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isMarked) Color(0xFFA855F7).copy(alpha = 0.12f) else Color.Transparent,
                                        contentColor = if (isMarked) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMarked) "Marked" else "Review",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        if (currentMcq != null) {
                                            viewModel.clearAnswer(currentMcq.id)
                                        }
                                        if (currentWritten != null) {
                                            viewModel.updateWrittenAnswer(currentWritten.id, "")
                                        }
                                    },
                                    modifier = Modifier.testTag("clear_button"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (currentQuestionIndex < totalQuestions - 1) {
                                Button(
                                    onClick = { viewModel.setCurrentQuestionIndex(currentQuestionIndex + 1) },
                                    modifier = Modifier.testTag("next_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Save & Next", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Button(
                                    onClick = { showSubmitConfirmation = true },
                                    modifier = Modifier.testTag("submit_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Save & Submit", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("quiz_screen_container"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isCurrentMcq && currentMcq != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("question_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Q${currentQuestionIndex + 1} • MCQ • ${currentMcq.difficulty}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.toggleBookmark(currentMcq) },
                                            modifier = Modifier.testTag("bookmark_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Bookmark Question",
                                                tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = currentMcq.question,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.testTag("question_text")
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = currentMcq.options,
                            key = { _, option -> "${currentMcq.id}_${option.id}" }
                        ) { _, option ->
                            val isSelected = userAnswers[currentMcq.id] == option.id

                            CbtOptionBox(
                                optionLetter = option.id,
                                optionText = option.text,
                                isSelected = isSelected,
                                onSelect = { viewModel.selectAnswer(currentMcq.id, option.id) },
                                modifier = Modifier.testTag("option_item_${option.id}")
                            )
                        }
                    } else if (currentWritten != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("written_question_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Q${currentQuestionIndex + 1} • WRITTEN • ${currentWritten.marks} Marks",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Marks: ${currentWritten.marks}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = currentWritten.question,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.testTag("written_question_text")
                                    )
                                }
                            }
                        }

                        item {
                            WrittenAnswerInput(
                                questionId = currentWritten.id,
                                initialAnswer = writtenAnswers[currentWritten.id] ?: "",
                                wordLimit = currentState.config.wordLimit.coerceAtLeast(100),
                                onAnswerChanged = { newAnswer ->
                                    viewModel.updateWrittenAnswer(currentWritten.id, newAnswer)
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Question Palette Modal Sheet
            if (showPaletteSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showPaletteSheet = false },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question Palette",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showPaletteSheet = false }) {
                                Icon(Icons.Default.Clear, contentDescription = "Close Palette")
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(
                                count = totalQuestions,
                                key = { index ->
                                    if (index < totalMcqs) "mcq_${mcqQuestions[index].id}"
                                    else "written_${writtenQuestions[index - totalMcqs].id}"
                                }
                            ) { index ->
                                val isMcq = index < totalMcqs
                                val qId = if (isMcq) mcqQuestions[index].id else writtenQuestions[index - totalMcqs].id
                                val isAnswered = if (isMcq) userAnswers.containsKey(qId) else writtenAnswers[qId]?.isNotBlank() == true
                                val isMarked = markedForReview.contains(qId)
                                val isCurrent = index == currentQuestionIndex

                                val bgColor = when {
                                    isMarked -> Color(0xFFA855F7)
                                    isAnswered -> Color(0xFF10B981)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isCurrent) 2.5.dp else 0.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.setCurrentQuestionIndex(index)
                                            showPaletteSheet = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnswered || isMarked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusChip(label = "Answered", count = answeredCount, color = Color(0xFF10B981))
                            StatusChip(label = "Unanswered", count = unansweredCount, color = Color(0xFF64748B))
                            StatusChip(label = "Review", count = markedCount, color = Color(0xFFA855F7))
                        }

                        Button(
                            onClick = {
                                showPaletteSheet = false
                                showSubmitConfirmation = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("palette_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Practice Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Submit Confirmation Dialog
            if (showSubmitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showSubmitConfirmation = false },
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Submit Practice Test?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Are you sure you want to finish and submit your practice test session?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Answered:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("$answeredCount / $totalQuestions", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Unanswered:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("$unansweredCount", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Marked for Review:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("$markedCount", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSubmitConfirmation = false
                                viewModel.submitTest()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Submit Test", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showSubmitConfirmation = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Continue Test")
                        }
                    }
                )
            }

            if (isEvaluating) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("Evaluating Written Answers", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.5.dp
                            )
                            Text(
                                text = "Gemini AI is grading your written answers, key points, and structure...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            if (evaluationError != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearEvaluationError() },
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("AI Evaluation Error", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = evaluationError ?: "Failed to evaluate written answers. Your answers are safe.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.clearEvaluationError()
                            viewModel.submitTest()
                        }) {
                            Text("Retry Evaluation")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearEvaluationError() }) {
                            Text("Edit Answers")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatusChip(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CbtOptionBox(
    optionLetter: String,
    optionText: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "opt_container"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(150),
        label = "opt_border"
    )
    val animatedBadgeBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150),
        label = "opt_badge"
    )
    val animatedBadgeTextColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "opt_badge_text"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, animatedBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(animatedBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLetter,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = animatedBadgeTextColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuizTimerBadge(
    timeRemainingFlow: StateFlow<Long>,
    isTimerEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isTimerEnabled) return
    val timeRemainingSeconds by timeRemainingFlow.collectAsState()

    val timerString = if (timeRemainingSeconds >= 3600) {
        val h = timeRemainingSeconds / 3600
        val m = (timeRemainingSeconds % 3600) / 60
        val s = timeRemainingSeconds % 60
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        val m = timeRemainingSeconds / 60
        val s = timeRemainingSeconds % 60
        "%02d:%02d".format(m, s)
    }

    val is1MinRemaining = timeRemainingSeconds in 1..60
    val is5MinRemaining = timeRemainingSeconds in 61..300

    val (timerBgColor, timerContentColor, timerBorder) = when {
        is1MinRemaining -> Triple(
            Color(0xFFEF4444).copy(alpha = 0.15f),
            Color(0xFFEF4444),
            BorderStroke(1.dp, Color(0xFFEF4444))
        )
        is5MinRemaining -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFD97706),
            BorderStroke(1.dp, Color(0xFFF59E0B))
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            null
        )
    }

    Surface(
        color = timerBgColor,
        contentColor = timerContentColor,
        shape = RoundedCornerShape(20.dp),
        border = timerBorder,
        modifier = modifier.testTag("quiz_timer")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = timerContentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = timerString,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = timerContentColor
            )
        }
    }
}

@Composable
fun WrittenAnswerInput(
    questionId: Int,
    initialAnswer: String,
    wordLimit: Int,
    onAnswerChanged: (String) -> Unit
) {
    var text by remember(questionId) { mutableStateOf(initialAnswer) }

    val wordCount = remember(text) {
        if (text.isBlank()) 0
        else text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Answer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onAnswerChanged(newText)
                },
                placeholder = { Text("Type your comprehensive answer here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .testTag("written_answer_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$wordCount / $wordLimit words",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (wordCount > wordLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
