package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.viewmodel.MainViewModel
import com.example.model.GeneratedDoubtResponse

data class DoubtHistoryItem(
    val question: String,
    val response: GeneratedDoubtResponse,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDoubtScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val isSolving by viewModel.isSolvingDoubt.collectAsState()
    val doubtStatus by viewModel.doubtStatus.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val primaryExam by viewModel.primaryExam.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var subjectInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    var doubtInput by remember { mutableStateOf("") }
    var activeResponse by remember { mutableStateOf<GeneratedDoubtResponse?>(null) }
    var activeQuestion by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recentDoubts = remember { mutableStateListOf<DoubtHistoryItem>() }

    LaunchedEffect(preferredLanguage) {
        if (preferredLanguage.isNotBlank()) {
            selectedLanguage = preferredLanguage
        }
    }

    LaunchedEffect(primaryExam) {
        if (subjectInput.isBlank() && primaryExam.isNotBlank()) {
            subjectInput = primaryExam
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.doubt_title), fontWeight = FontWeight.Bold)
                        Text(
                            "Instant clear explanations for any concept",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("doubt_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Ask AI / Solve", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Recent Doubts (${recentDoubts.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTabIndex == 0) {
                // Tab 0: Ask AI / Solve Form & Response
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    "Ask any question or doubt regarding your subject or exam. AI will break it down into simple, direct, and exam-focused explanations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = subjectInput,
                                onValueChange = { subjectInput = it },
                                label = { Text("Subject (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                label = { Text("Topic (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = doubtInput,
                            onValueChange = {
                                doubtInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Ask your Question / Doubt") },
                            placeholder = { Text("e.g. What is the difference between speed and velocity?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .testTag("doubt_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Language",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("English", "Hindi", "Hinglish").forEach { lang ->
                                    val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(lang, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("doubt_lang_chip_$lang"),
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Error",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                    if (doubtInput.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                errorMessage = null
                                                viewModel.solveDoubt(
                                                    subject = subjectInput,
                                                    topic = topicInput,
                                                    doubt = doubtInput,
                                                    language = selectedLanguage,
                                                    onSuccess = { response ->
                                                        activeResponse = response
                                                        activeQuestion = doubtInput
                                                        recentDoubts.add(0, DoubtHistoryItem(question = doubtInput, response = response))
                                                        Toast.makeText(context, "Explanation generated!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { e -> errorMessage = e }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (doubtInput.isBlank()) {
                                    errorMessage = "Please enter your question or doubt."
                                    return@Button
                                }
                                errorMessage = null
                                viewModel.solveDoubt(
                                    subject = subjectInput,
                                    topic = topicInput,
                                    doubt = doubtInput,
                                    language = selectedLanguage,
                                    onSuccess = { response ->
                                        activeResponse = response
                                        activeQuestion = doubtInput
                                        recentDoubts.add(0, DoubtHistoryItem(question = doubtInput, response = response))
                                        Toast.makeText(context, "Explanation generated!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> errorMessage = err }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("ask_ai_doubt_button"),
                            enabled = !isSolving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSolving) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (doubtStatus.isNotBlank()) doubtStatus else "Solving Doubt...")
                            } else {
                                Icon(Icons.Default.Psychology, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask AI Solver", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    activeResponse?.let { resp ->
                        item {
                            DoubtResponseCard(
                                question = if (activeQuestion.isNotBlank()) activeQuestion else doubtInput,
                                response = resp,
                                subject = subjectInput,
                                topic = topicInput,
                                language = selectedLanguage,
                                onCopy = {
                                    val q = if (activeQuestion.isNotBlank()) activeQuestion else doubtInput
                                    val fullText = "Q: $q\n\nAnswer: ${resp.directAnswer}\n\nSimple Explanation:\n${resp.simpleExplanation}\n\nDetailed Explanation:\n${resp.detailedExplanation}\n\nExam Point:\n${resp.examPoint}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Doubt Answer", fullText))
                                    Toast.makeText(context, "Explanation copied!", Toast.LENGTH_SHORT).show()
                                },
                                onDownloadPdf = {
                                    val q = if (activeQuestion.isNotBlank()) activeQuestion else doubtInput
                                    com.example.util.PdfExporter.exportDoubtPdf(
                                        context = context,
                                        question = q,
                                        response = resp,
                                        subject = subjectInput.ifBlank { "General" },
                                        topic = topicInput.ifBlank { "Doubt" },
                                        language = selectedLanguage
                                    )
                                },
                                onClear = { activeResponse = null }
                            )
                        }
                    }
                }
            } else {
                // Tab 1: Recent Doubts & History
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recentDoubts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No doubts asked in this session yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Ask any question in the 'Ask AI / Solve' tab to see explanations here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { selectedTabIndex = 0 }) {
                                        Text("Ask a Question")
                                    }
                                }
                            }
                        }
                    } else {
                        items(recentDoubts) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Q: ${item.question}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        item.response.directAnswer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                com.example.util.PdfExporter.exportDoubtPdf(
                                                    context = context,
                                                    question = item.question,
                                                    response = item.response,
                                                    subject = subjectInput.ifBlank { "General" },
                                                    topic = topicInput.ifBlank { "Doubt" },
                                                    language = selectedLanguage
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                                        }

                                        Button(
                                            onClick = {
                                                activeResponse = item.response
                                                activeQuestion = item.question
                                                selectedTabIndex = 0
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("View Answer", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoubtResponseCard(
    question: String,
    response: GeneratedDoubtResponse,
    subject: String = "",
    topic: String = "",
    language: String = "English",
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onDownloadPdf: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (question.isNotBlank()) {
                        Text("Q: $question", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Row {
                    if (onDownloadPdf != null) {
                        IconButton(onClick = onDownloadPdf, modifier = Modifier.testTag("download_doubt_pdf_button")) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Answer")
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Answer")
                    }
                }
            }

            // Direct Answer
            if (response.directAnswer.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DIRECT ANSWER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(response.directAnswer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Simple Explanation
            if (response.simpleExplanation.isNotBlank()) {
                Column {
                    Text("Simple Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(response.simpleExplanation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Detailed Explanation
            if (response.detailedExplanation.isNotBlank()) {
                Column {
                    Text("Step-by-Step Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(response.detailedExplanation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Exam Takeaway Point
            if (response.examPoint.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("KEY EXAM TAKEAWAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(response.examPoint, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (onDownloadPdf != null) {
                OutlinedButton(
                    onClick = onDownloadPdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Solution PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
