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

    var subjectInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    var doubtInput by remember { mutableStateOf("") }
    var activeResponse by remember { mutableStateOf<GeneratedDoubtResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recentDoubts = remember { mutableStateListOf<DoubtHistoryItem>() }

    LaunchedEffect(primaryExam) {
        if (subjectInput.isBlank() && primaryExam.isNotBlank()) {
            subjectInput = primaryExam
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Doubt Solver", fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    onValueChange = { doubtInput = it },
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("Language: $preferredLanguage", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            errorMessage?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
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
                            onSuccess = { response ->
                                activeResponse = response
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
                        question = doubtInput,
                        response = resp,
                        onCopy = {
                            val fullText = "Q: $doubtInput\n\nAnswer: ${resp.directAnswer}\n\nSimple Explanation:\n${resp.simpleExplanation}\n\nDetailed Explanation:\n${resp.detailedExplanation}\n\nExam Point:\n${resp.examPoint}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Doubt Answer", fullText))
                            Toast.makeText(context, "Explanation copied!", Toast.LENGTH_SHORT).show()
                        },
                        onClear = { activeResponse = null }
                    )
                }
            }

            if (recentDoubts.isNotEmpty() && activeResponse == null) {
                item {
                    Text("Recent Doubts in Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(recentDoubts) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Q: ${item.question}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.response.directAnswer, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { activeResponse = item.response },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("View Full Answer")
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
    onCopy: () -> Unit,
    onClear: () -> Unit
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
                Text("AI Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row {
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
                        Text("🎯 DIRECT ANSWER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(response.directAnswer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Simple Explanation
            if (response.simpleExplanation.isNotBlank()) {
                Column {
                    Text("💡 Simple Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(response.simpleExplanation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Detailed Explanation
            if (response.detailedExplanation.isNotBlank()) {
                Column {
                    Text("📝 Step-by-Step Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(response.detailedExplanation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Exam Takeaway Point
            if (response.examPoint.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⭐ KEY EXAM TAKEAWAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(response.examPoint, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
