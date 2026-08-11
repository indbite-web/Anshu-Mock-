package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.remote.SupportedModel
import com.example.data.viewmodel.MainViewModel
import com.example.data.viewmodel.QuizUiState
import com.example.model.TestConfig
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateTestScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val context = LocalContext.current
    val selectedImages by viewModel.selectedImages.collectAsState()
    val quizState by viewModel.quizState.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()

    val defaultQuestions by viewModel.prefsRepo.defaultQuestions.collectAsState()
    val defaultDifficulty by viewModel.prefsRepo.defaultDifficulty.collectAsState()
    val defaultLanguage by viewModel.prefsRepo.defaultLanguage.collectAsState()
    val defaultNegativeMarking by viewModel.prefsRepo.defaultNegativeMarking.collectAsState()
    val defaultTimerMinutes by viewModel.prefsRepo.defaultTimerMinutes.collectAsState()
    val defaultStrictSource by viewModel.prefsRepo.strictSourceMode.collectAsState()
    val preferredExam by viewModel.primaryExam.collectAsState()
    val additionalExams by viewModel.additionalExams.collectAsState()
    val selectedModelId by viewModel.prefsRepo.selectedModel.collectAsState()

    var questionCount by remember(defaultQuestions) { mutableIntStateOf(defaultQuestions) }
    var selectedDifficulty by remember(defaultDifficulty) { mutableStateOf(defaultDifficulty) }
    var selectedStyle by remember { mutableStateOf("Mixed") }
    var selectedLanguage by remember(defaultLanguage) { mutableStateOf(defaultLanguage) }
    var strictSourceMode by remember(defaultStrictSource) { mutableStateOf(defaultStrictSource) }
    var timerMinutes by remember(defaultTimerMinutes) { mutableIntStateOf(defaultTimerMinutes) }
    var negativeMarkingRatio by remember { mutableFloatStateOf(0.0f) }

    var targetExam by remember(preferredExam) { mutableStateOf(if (preferredExam.isNotBlank()) preferredExam else "General Practice") }
    var customExamName by remember { mutableStateOf("") }
    var targetSubject by remember { mutableStateOf("General") }
    var customSubjectName by remember { mutableStateOf("") }
    var targetTopic by remember { mutableStateOf("") }
    var customInstruction by remember { mutableStateOf("") }

    var showModelBottomSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var customTimerInput by remember { mutableStateOf("") }
    var customTimerError by remember { mutableStateOf<String?>(null) }
    var showCustomNegativeDialog by remember { mutableStateOf(false) }
    var customNegativeInput by remember { mutableStateOf("") }
    var customNegativeError by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var selectedExamType by remember { mutableStateOf(com.example.model.ExamType.MCQ) }
    var marksPerQuestion by remember { mutableIntStateOf(5) }
    var answerLengthType by remember { mutableStateOf("Medium") }
    var wordLimit by remember { mutableIntStateOf(100) }
    var mcqQuestionCount by remember { mutableIntStateOf(5) }
    var writtenQuestionCount by remember { mutableIntStateOf(5) }

    // Written Question Sub-Configurations (Short, Medium, Long)
    var shortWrittenEnabled by remember { mutableStateOf(true) }
    var shortWrittenCount by remember { mutableIntStateOf(3) }
    var shortWrittenMarks by remember { mutableIntStateOf(2) }
    var shortWrittenWordLimit by remember { mutableIntStateOf(50) }

    var mediumWrittenEnabled by remember { mutableStateOf(false) }
    var mediumWrittenCount by remember { mutableIntStateOf(2) }
    var mediumWrittenMarks by remember { mutableIntStateOf(5) }
    var mediumWrittenWordLimit by remember { mutableIntStateOf(100) }

    var longWrittenEnabled by remember { mutableStateOf(false) }
    var longWrittenCount by remember { mutableIntStateOf(1) }
    var longWrittenMarks by remember { mutableIntStateOf(10) }
    var longWrittenWordLimit by remember { mutableIntStateOf(250) }

    val totalWrittenCount = remember(
        shortWrittenEnabled, shortWrittenCount,
        mediumWrittenEnabled, mediumWrittenCount,
        longWrittenEnabled, longWrittenCount
    ) {
        var sum = 0
        if (shortWrittenEnabled) sum += shortWrittenCount.coerceAtLeast(0)
        if (mediumWrittenEnabled) sum += mediumWrittenCount.coerceAtLeast(0)
        if (longWrittenEnabled) sum += longWrittenCount.coerceAtLeast(0)
        sum
    }

    val totalWrittenMarks = remember(
        shortWrittenEnabled, shortWrittenCount, shortWrittenMarks,
        mediumWrittenEnabled, mediumWrittenCount, mediumWrittenMarks,
        longWrittenEnabled, longWrittenCount, longWrittenMarks
    ) {
        var marks = 0
        if (shortWrittenEnabled) marks += (shortWrittenCount.coerceAtLeast(0) * shortWrittenMarks.coerceAtLeast(0))
        if (mediumWrittenEnabled) marks += (mediumWrittenCount.coerceAtLeast(0) * mediumWrittenMarks.coerceAtLeast(0))
        if (longWrittenEnabled) marks += (longWrittenCount.coerceAtLeast(0) * longWrittenMarks.coerceAtLeast(0))
        marks
    }

    val configValidationError: String? = remember(
        selectedExamType, questionCount, mcqQuestionCount,
        shortWrittenEnabled, shortWrittenCount, shortWrittenMarks, shortWrittenWordLimit,
        mediumWrittenEnabled, mediumWrittenCount, mediumWrittenMarks, mediumWrittenWordLimit,
        longWrittenEnabled, longWrittenCount, longWrittenMarks, longWrittenWordLimit,
        totalWrittenCount
    ) {
        when (selectedExamType) {
            com.example.model.ExamType.MCQ -> {
                if (questionCount < 1) "Please select at least 1 MCQ question." else null
            }
            com.example.model.ExamType.WRITTEN -> {
                if (!shortWrittenEnabled && !mediumWrittenEnabled && !longWrittenEnabled) {
                    "Please select at least one written question type (Short, Medium, or Long)."
                } else if (totalWrittenCount < 1) {
                    "Please set written question count to at least 1."
                } else null
            }
            com.example.model.ExamType.MIXED -> {
                if (mcqQuestionCount < 1) {
                    "Please select at least 1 MCQ question for Mixed test."
                } else if (!shortWrittenEnabled && !mediumWrittenEnabled && !longWrittenEnabled) {
                    "Please select at least one written question type (Short, Medium, or Long)."
                } else if (totalWrittenCount < 1) {
                    "Please set written question count to at least 1."
                } else null
            }
        }
    }

    // ONLY navigate to quiz when quizState turns Active and has questions!
    LaunchedEffect(quizState) {
        if (quizState is QuizUiState.Active) {
            val activeState = quizState as QuizUiState.Active
            if (activeState.quiz.questions.isNotEmpty() || activeState.quiz.writtenQuestions.isNotEmpty()) {
                val activity = context as? android.app.Activity
                com.example.util.AdManager.showInterstitialAdIfEligible(activity) {
                    onStartQuiz()
                }
            }
        }
    }

    fun createTempImageUri(): Uri? {
        return try {
            val tempFile = File.createTempFile("camera_photo_", ".jpg", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.addImagesFromUris(listOf(it)) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempImageUri()
            if (uri != null) {
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val uri = createTempImageUri()
            if (uri != null) {
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesFromUris(uris)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            Toast.makeText(context, "${uris.size} document(s) attached", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic prompt suggestions based on target exam
    val promptSuggestions = remember(targetExam) {
        val examStr = if (targetExam != "Custom Exam") targetExam else "exam"
        listOf(
            "$examStr level conceptual MCQs",
            "Focus on multi-statement verification questions",
            "Include assertion and reasoning style questions",
            "High difficulty numerical & analytical questions",
            "Focus on key formulas, dates, and core definitions"
        )
    }

    val currentModel = SupportedModel.fromModelId(selectedModelId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Practice Test", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Generation Error Card
                if (quizState is QuizUiState.Error) {
                    item {
                        val errMsg = (quizState as QuizUiState.Error).message
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Generation Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = errMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { viewModel.resetQuizState() }) {
                                        Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (errMsg.contains("API key", ignoreCase = true)) {
                                        Button(
                                            onClick = { showApiKeyDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Set Key")
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.retryLastTest() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Generation Loading Card Banner
                if (quizState is QuizUiState.Generating) {
                    item {
                        val status = (quizState as QuizUiState.Generating).statusMessage
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Creating your practice test...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Study Material Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Study Material",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedImages.isNotEmpty()) {
                                    Text(
                                        text = "${selectedImages.size} pages attached",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Compact Action Row: Camera, Gallery, PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchCamera() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Camera",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gallery",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { pdfLauncher.launch("application/pdf") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PDF",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Horizontal thumbnail row when images attached
                            if (selectedImages.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(
                                        items = selectedImages,
                                        key = { index, bitmap -> "img_${index}_${bitmap.hashCode()}" }
                                    ) { index, bitmap ->
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .clickable { viewModel.removeImage(index) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. AI Instructions (Optional)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "AI Instructions (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = customInstruction,
                                onValueChange = { customInstruction = it },
                                placeholder = { Text("e.g. Create difficult conceptual MCQs from this chapter with step-by-step solutions.", fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_instructions_input"),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )

                            // Dynamic suggestion chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                promptSuggestions.forEach { suggestion ->
                                    FilterChip(
                                        selected = customInstruction.contains(suggestion),
                                        onClick = {
                                            customInstruction = if (customInstruction.isBlank()) {
                                                suggestion
                                            } else {
                                                "$customInstruction. $suggestion"
                                            }
                                        },
                                        label = { Text(suggestion, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Source Control
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Use only uploaded material",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Strict source mode (no external facts)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = strictSourceMode,
                                    onCheckedChange = { checked ->
                                        if (checked && selectedImages.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "Please attach study material first to use strict source mode",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        strictSourceMode = checked
                                    },
                                    modifier = Modifier.testTag("strict_source_switch")
                                )
                            }
                        }
                    }
                }

                // 4. Target Exam & Subject
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Exam & Subject",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Target Exam Selection
                            val examOptions = remember(preferredExam, additionalExams) {
                                val list = mutableListOf("General Practice")
                                if (preferredExam.isNotBlank()) list.add(preferredExam)
                                if (additionalExams.isNotBlank()) {
                                    additionalExams.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                                        if (!list.contains(it)) list.add(it)
                                    }
                                }
                                list.add("Custom Exam")
                                list
                            }

                            Text("Target Exam", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                examOptions.forEach { exam ->
                                    val isSelected = targetExam == exam
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { targetExam = exam },
                                        label = { Text(exam, fontSize = 12.sp) },
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        )
                                    )
                                }
                            }

                            if (targetExam == "Custom Exam") {
                                OutlinedTextField(
                                    value = customExamName,
                                    onValueChange = { customExamName = it },
                                    label = { Text("Enter Exam Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Subject Selection
                            val subjectOptions = listOf("General", "Computer Science", "Quantitative Aptitude", "Reasoning", "English", "General Science", "History", "Custom Subject")
                            Text("Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subjectOptions.forEach { subj ->
                                    val isSelected = targetSubject == subj
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { targetSubject = subj },
                                        label = { Text(subj, fontSize = 12.sp) },
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        )
                                    )
                                }
                            }

                            if (targetSubject == "Custom Subject") {
                                OutlinedTextField(
                                    value = customSubjectName,
                                    onValueChange = { customSubjectName = it },
                                    label = { Text("Enter Subject Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Topic/Chapter Optional
                            OutlinedTextField(
                                value = targetTopic,
                                onValueChange = { targetTopic = it },
                                label = { Text("Topic / Chapter (Optional)") },
                                placeholder = { Text("e.g. Data Structures, Thermodynamics") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 4.5 Exam Type Selection
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Exam Type",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    com.example.model.ExamType.MCQ to "MCQ TEST",
                                    com.example.model.ExamType.WRITTEN to "WRITTEN TEST",
                                    com.example.model.ExamType.MIXED to "MIXED TEST"
                                ).forEach { (type, label) ->
                                    val isSelected = selectedExamType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedExamType = type },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("exam_type_chip_${type.name}"),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Test Configuration
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Test Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // --- MCQ SECTION (For MCQ or MIXED mode) ---
                            if (selectedExamType == com.example.model.ExamType.MCQ || selectedExamType == com.example.model.ExamType.MIXED) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "MCQ Question Count",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        val currentMcqVal = if (selectedExamType == com.example.model.ExamType.MIXED) mcqQuestionCount else questionCount
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (selectedExamType == com.example.model.ExamType.MIXED) {
                                                        if (mcqQuestionCount > 1) mcqQuestionCount -= 1
                                                    } else {
                                                        if (questionCount > 1) questionCount -= 1
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease MCQs")
                                            }
                                            Text(
                                                text = "$currentMcqVal",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (selectedExamType == com.example.model.ExamType.MIXED) {
                                                        if (mcqQuestionCount < 50) mcqQuestionCount += 1
                                                    } else {
                                                        if (questionCount < 50) questionCount += 1
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase MCQs")
                                            }
                                        }
                                    }

                                    // Responsive MCQ Preset Chips
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(3, 5, 10, 15, 20).forEach { preset ->
                                            val isSel = if (selectedExamType == com.example.model.ExamType.MIXED) mcqQuestionCount == preset else questionCount == preset
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    if (selectedExamType == com.example.model.ExamType.MIXED) mcqQuestionCount = preset else questionCount = preset
                                                },
                                                label = {
                                                    Text(
                                                        text = "$preset MCQs",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                },
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSel,
                                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                    borderWidth = 1.dp,
                                                    selectedBorderWidth = 1.5.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // --- WRITTEN SECTION (For WRITTEN or MIXED mode) ---
                            if (selectedExamType == com.example.model.ExamType.WRITTEN || selectedExamType == com.example.model.ExamType.MIXED) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Written Question Types (Multi-Selectable)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Enable the types of written questions you want and configure count & marks independently:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // 1. SHORT ANSWER CONFIG
                                    WrittenTypeConfigCard(
                                        title = "Short Answer (~50 words)",
                                        enabled = shortWrittenEnabled,
                                        onEnabledChange = { shortWrittenEnabled = it },
                                        count = shortWrittenCount,
                                        onCountChange = { shortWrittenCount = it },
                                        marks = shortWrittenMarks,
                                        onMarksChange = { shortWrittenMarks = it },
                                        wordLimit = shortWrittenWordLimit,
                                        onWordLimitChange = { shortWrittenWordLimit = it },
                                        countPresets = listOf(1, 2, 3, 5),
                                        marksPresets = listOf(1, 2, 3, 5)
                                    )

                                    // 2. MEDIUM ANSWER CONFIG
                                    WrittenTypeConfigCard(
                                        title = "Medium Answer (~100 words)",
                                        enabled = mediumWrittenEnabled,
                                        onEnabledChange = { mediumWrittenEnabled = it },
                                        count = mediumWrittenCount,
                                        onCountChange = { mediumWrittenCount = it },
                                        marks = mediumWrittenMarks,
                                        onMarksChange = { mediumWrittenMarks = it },
                                        wordLimit = mediumWrittenWordLimit,
                                        onWordLimitChange = { mediumWrittenWordLimit = it },
                                        countPresets = listOf(1, 2, 3, 5),
                                        marksPresets = listOf(3, 5, 8, 10)
                                    )

                                    // 3. LONG ANSWER CONFIG
                                    WrittenTypeConfigCard(
                                        title = "Long / Detailed (~250 words)",
                                        enabled = longWrittenEnabled,
                                        onEnabledChange = { longWrittenEnabled = it },
                                        count = longWrittenCount,
                                        onCountChange = { longWrittenCount = it },
                                        marks = longWrittenMarks,
                                        onMarksChange = { longWrittenMarks = it },
                                        wordLimit = longWrittenWordLimit,
                                        onWordLimitChange = { longWrittenWordLimit = it },
                                        countPresets = listOf(1, 2, 3),
                                        marksPresets = listOf(10, 15, 20)
                                    )

                                    // WRITTEN SUMMARY CARD
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Written Test Breakdown",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Total Written Qs: $totalWrittenCount | Total Marks: $totalWrittenMarks",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Difficulty Segmented Control - Responsive Layout
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Difficulty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Easy", "Medium", "Hard", "Very Hard").forEach { diff ->
                                        val isSelected = selectedDifficulty.equals(diff, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedDifficulty = diff },
                                            label = { Text(diff, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false) },
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp,
                                                selectedBorderWidth = 1.5.dp
                                            )
                                        )
                                    }
                                }
                            }

                            // Question Style
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Question Style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                val styleOptions = listOf("Mixed", "Conceptual", "Statement Based", "Match Following", "Direct")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    styleOptions.forEach { style ->
                                        val isSelected = selectedStyle == style
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedStyle = style },
                                            label = { Text(style, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp,
                                                selectedBorderWidth = 1.5.dp
                                            )
                                        )
                                    }
                                }
                            }

                            // Language
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Language", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Hindi", "English", "Bilingual").forEach { lang ->
                                        val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedLanguage = lang },
                                            label = { Text(lang, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp,
                                                selectedBorderWidth = 1.5.dp
                                            )
                                        )
                                    }
                                }
                            }

                            // Timer Configuration
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Timer",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                val isCustomTimer = timerMinutes > 0 && timerMinutes !in listOf(10, 20, 30, 60)

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf(
                                        0 to "No Timer",
                                        10 to "10 min",
                                        20 to "20 min",
                                        30 to "30 min",
                                        60 to "60 min"
                                    )
                                    presets.forEach { (mins, label) ->
                                        val isSelected = timerMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { timerMinutes = mins },
                                            label = { Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                            modifier = Modifier.testTag("timer_chip_$mins"),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp,
                                                selectedBorderWidth = 1.5.dp
                                            )
                                        )
                                    }

                                    val customText = if (isCustomTimer) "Custom (${timerMinutes}m)" else "Custom"
                                    FilterChip(
                                        selected = isCustomTimer,
                                        onClick = {
                                            customTimerInput = if (isCustomTimer) "$timerMinutes" else ""
                                            customTimerError = null
                                            showCustomTimerDialog = true
                                        },
                                        label = { Text(customText, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                        modifier = Modifier.testTag("timer_chip_custom"),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isCustomTimer,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        )
                                    )
                                }
                            }

                            // Negative Marking Configuration
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Negative Marking",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                val isCustomNegative = negativeMarkingRatio !in listOf(0.0f, 0.25f, 0.33f, 0.50f)

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val negativePresets = listOf(
                                        0.0f to "None",
                                        0.25f to "0.25",
                                        0.33f to "0.33",
                                        0.50f to "0.50"
                                    )
                                    negativePresets.forEach { (ratio, label) ->
                                        val isSelected = negativeMarkingRatio == ratio
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { negativeMarkingRatio = ratio },
                                            label = { Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                            modifier = Modifier.testTag("negative_chip_$label"),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                                borderWidth = 1.dp,
                                                selectedBorderWidth = 1.5.dp
                                            )
                                        )
                                    }

                                    val customText = if (isCustomNegative) "Custom ($negativeMarkingRatio)" else "Custom"
                                    FilterChip(
                                        selected = isCustomNegative,
                                        onClick = {
                                            customNegativeInput = if (isCustomNegative) "$negativeMarkingRatio" else ""
                                            customNegativeError = null
                                            showCustomNegativeDialog = true
                                        },
                                        label = { Text(customText, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                        modifier = Modifier.testTag("negative_chip_custom"),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isCustomNegative,
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 1.5.dp
                                        )
                                    )
                                }
                            }

                            // INLINE VALIDATION ERROR BANNER
                            if (configValidationError != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = "Validation Error",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = configValidationError,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. AI Model Row
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelBottomSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "AI Model",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${currentModel.displayName} (Free Tier)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Change Model",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 7. Generate CTA Button
                item {
                    val isGenerating = quizState is QuizUiState.Generating

                    Button(
                        onClick = {
                            if (userApiKey.isBlank()) {
                                showApiKeyDialog = true
                            } else {
                                val finalExam = if (targetExam == "Custom Exam") customExamName.ifBlank { "Custom Exam" } else targetExam
                                val finalSubject = if (targetSubject == "Custom Subject") customSubjectName.ifBlank { "General" } else targetSubject

                                val finalMcqCount = when (selectedExamType) {
                                    com.example.model.ExamType.MCQ -> questionCount
                                    com.example.model.ExamType.WRITTEN -> 0
                                    com.example.model.ExamType.MIXED -> mcqQuestionCount
                                }

                                val finalWrittenCount = when (selectedExamType) {
                                    com.example.model.ExamType.MCQ -> 0
                                    com.example.model.ExamType.WRITTEN -> totalWrittenCount
                                    com.example.model.ExamType.MIXED -> totalWrittenCount
                                }

                                val finalCount = finalMcqCount + finalWrittenCount

                                val config = TestConfig(
                                    targetExam = finalExam,
                                    subject = finalSubject,
                                    topic = targetTopic,
                                    questionCount = finalCount,
                                    difficulty = selectedDifficulty,
                                    style = selectedStyle,
                                    language = selectedLanguage,
                                    strictSourceMode = strictSourceMode,
                                    naturalPrompt = customInstruction,
                                    customInstruction = customInstruction,
                                    timerModeMinutes = timerMinutes,
                                    negativeMarkingRatio = negativeMarkingRatio,
                                    examType = selectedExamType,
                                    marksPerQuestion = marksPerQuestion,
                                    answerLengthType = answerLengthType,
                                    wordLimit = wordLimit,
                                    mcqQuestionCount = finalMcqCount,
                                    writtenQuestionCount = finalWrittenCount,
                                    shortWrittenConfig = com.example.model.WrittenTypeConfig(
                                        enabled = shortWrittenEnabled,
                                        count = shortWrittenCount,
                                        marksEach = shortWrittenMarks,
                                        wordLimit = shortWrittenWordLimit
                                    ),
                                    mediumWrittenConfig = com.example.model.WrittenTypeConfig(
                                        enabled = mediumWrittenEnabled,
                                        count = mediumWrittenCount,
                                        marksEach = mediumWrittenMarks,
                                        wordLimit = mediumWrittenWordLimit
                                    ),
                                    longWrittenConfig = com.example.model.WrittenTypeConfig(
                                        enabled = longWrittenEnabled,
                                        count = longWrittenCount,
                                        marksEach = longWrittenMarks,
                                        wordLimit = longWrittenWordLimit
                                    )
                                )

                                viewModel.startNewTest(config)
                            }
                        },
                        enabled = !isGenerating && configValidationError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_test_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Creating your test...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Practice Test", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showModelBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Gemini AI Model",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SupportedModel.FREE_MODEL_ALLOWLIST.forEach { model ->
                    val isSelected = model.modelId == selectedModelId

                    Surface(
                        onClick = {
                            viewModel.prefsRepo.setSelectedModel(model.modelId)
                            showModelBottomSheet = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Selected",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showApiKeyDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please add your free Gemini API key in Settings → AI Configuration to generate tests.")

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Don't know how to get an API key?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { com.example.util.Constants.openTutorialVideo(context) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "▶",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Watch Tutorial",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showApiKeyDialog = false; onNavigateBack() }) {
                    Text("Go to Settings")
                }
            }
        )
    }

    if (showCustomTimerDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimerDialog = false },
            title = {
                Text(
                    text = "Custom Test Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTimerInput,
                        onValueChange = {
                            customTimerInput = it
                            customTimerError = null
                        },
                        label = { Text("Enter minutes") },
                        placeholder = { Text("45") },
                        suffix = { Text("minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customTimerError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_timer_input")
                    )
                    if (customTimerError != null) {
                        Text(
                            text = customTimerError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputVal = customTimerInput.trim()
                        val valInt = inputVal.toIntOrNull()
                        if (valInt == null || valInt < 1 || valInt > 600) {
                            customTimerError = "Please enter a duration between 1 and 600 minutes."
                        } else {
                            timerMinutes = valInt
                            showCustomTimerDialog = false
                            customTimerError = null
                        }
                    },
                    modifier = Modifier.testTag("set_timer_button")
                ) {
                    Text("Set Timer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomTimerDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCustomNegativeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomNegativeDialog = false },
            title = {
                Text(
                    text = "Custom Negative Marking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customNegativeInput,
                        onValueChange = {
                            customNegativeInput = it
                            customNegativeError = null
                        },
                        label = { Text("Negative ratio (e.g. 0.20)") },
                        placeholder = { Text("0.20") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customNegativeError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_negative_input")
                    )
                    if (customNegativeError != null) {
                        Text(
                            text = customNegativeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputVal = customNegativeInput.trim()
                        val valFloat = inputVal.toFloatOrNull()
                        if (valFloat == null || valFloat < 0.0f || valFloat > 10.0f) {
                            customNegativeError = "Please enter a non-negative value (e.g. 0.20 or 0.50)."
                        } else {
                            negativeMarkingRatio = valFloat
                            showCustomNegativeDialog = false
                            customNegativeError = null
                        }
                    },
                    modifier = Modifier.testTag("set_negative_button")
                ) {
                    Text("Set Ratio")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomNegativeDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrittenTypeConfigCard(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    count: Int,
    onCountChange: (Int) -> Unit,
    marks: Int,
    onMarksChange: (Int) -> Unit,
    wordLimit: Int,
    onWordLimitChange: (Int) -> Unit,
    countPresets: List<Int>,
    marksPresets: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Question Count Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Count:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (count > 0) onCountChange(count - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$count Qs",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { if (count < 20) onCountChange(count + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        countPresets.forEach { p ->
                            FilterChip(
                                selected = count == p,
                                onClick = { onCountChange(p) },
                                label = { Text("$p Qs", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    // Marks Each Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Marks each:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (marks > 1) onMarksChange(marks - 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Marks")
                            }
                            Text(
                                text = "$marks Marks",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { if (marks < 100) onMarksChange(marks + 1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Marks")
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        marksPresets.forEach { m ->
                            FilterChip(
                                selected = marks == m,
                                onClick = { onMarksChange(m) },
                                label = { Text("$m Marks", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
