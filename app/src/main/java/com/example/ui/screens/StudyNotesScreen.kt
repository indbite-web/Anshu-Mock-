package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.StudyNoteEntity
import com.example.data.viewmodel.MainViewModel
import com.example.model.GeneratedStudyNotes
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyNotesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val savedNotes by viewModel.savedStudyNotes.collectAsState()
    val isGenerating by viewModel.isGeneratingNotes.collectAsState()
    val generationStatus by viewModel.notesGenerationStatus.collectAsState()
    val generatedNotes by viewModel.generatedNotes.collectAsState()

    var selectedLanguage by remember { mutableStateOf("English") }
    var subjectInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    var customInstructionsInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeNoteForView by remember { mutableStateOf<StudyNoteEntity?>(null) }

    val primaryExam by viewModel.primaryExam.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()

    LaunchedEffect(preferredLanguage) {
        if (preferredLanguage.isNotBlank()) {
            selectedLanguage = preferredLanguage
        }
    }

    // Pre-fill default subject/exam if available
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
                        Text(stringResource(R.string.notes_title), fontWeight = FontWeight.Bold)
                        Text(
                            "Structured summaries & exam key points",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notes_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
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
                    text = { Text("Generate Notes", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Saved (${savedNotes.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTabIndex == 0) {
                // Generate Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Generate comprehensive, structured notes with key definitions, exam points, and quick revision bullet points.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("Subject / Exam (e.g., Physics, History, UPSC)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_subject_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Topic (e.g., Thermodynamics, Indian Constitution)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_topic_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customInstructionsInput,
                            onValueChange = { customInstructionsInput = it },
                            label = { Text("Custom Instructions (Optional)") },
                            placeholder = { Text("e.g. Focus on key dates, formulas, or short bullet points") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notes_custom_instructions_input"),
                            maxLines = 3,
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
                                            .testTag("notes_lang_chip_$lang"),
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (topicInput.isBlank()) {
                                    errorMessage = "Please enter a topic name."
                                    return@Button
                                }
                                errorMessage = null
                                viewModel.generateNotes(
                                    subject = subjectInput.ifBlank { "General" },
                                    topic = topicInput,
                                    customInstructions = customInstructionsInput,
                                    language = selectedLanguage,
                                    onSuccess = {
                                        Toast.makeText(context, "Study Notes Generated!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_notes_button"),
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (generationStatus.isNotBlank()) generationStatus else "Generating...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Study Notes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Render generated notes if present
                    generatedNotes?.let { notes ->
                        item {
                            StudyNoteContentView(
                                notes = notes,
                                onSave = {
                                    viewModel.saveStudyNote(
                                        subject = subjectInput.ifBlank { "General" },
                                        topic = topicInput,
                                        notes = notes,
                                        customInstructions = customInstructionsInput,
                                        language = selectedLanguage,
                                        onSaved = {
                                            Toast.makeText(context, "Saved to Offline Notes!", Toast.LENGTH_SHORT).show()
                                            selectedTabIndex = 1
                                        }
                                    )
                                },
                                onCopy = {
                                    val textToCopy = formatNotesToPlainText(notes)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Study Notes", textToCopy))
                                    Toast.makeText(context, "Notes copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onDownloadPdf = {
                                    com.example.util.PdfExporter.exportStudyNotesPdf(context, notes)
                                }
                            )
                        }
                    }
                }
            } else {
                // Saved Tab
                if (savedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No saved study notes yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Generated notes saved offline will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Text("Offline Available: All saved notes can be viewed without internet.", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        items(savedNotes, key = { it.id }) { note ->
                            SavedNoteCard(
                                note = note,
                                onClick = { activeNoteForView = note },
                                onDelete = { viewModel.deleteStudyNote(note.id) },
                                onCopy = {
                                    val moshi = Moshi.Builder().build()
                                    val listType = Types.newParameterizedType(List::class.java, String::class.java)
                                    val adapter = moshi.adapter<List<String>>(listType)
                                    val notesObj = GeneratedStudyNotes(
                                        title = note.title,
                                        summary = note.summary,
                                        importantConcepts = adapter.fromJson(note.importantConceptsJson) ?: emptyList(),
                                        keyDefinitions = adapter.fromJson(note.keyDefinitionsJson) ?: emptyList(),
                                        examPoints = adapter.fromJson(note.examPointsJson) ?: emptyList(),
                                        examples = adapter.fromJson(note.examplesJson) ?: emptyList(),
                                        quickRevision = adapter.fromJson(note.quickRevisionJson) ?: emptyList()
                                    )
                                    val text = formatNotesToPlainText(notesObj)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Saved Study Note", text))
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog for Saved Note
    activeNoteForView?.let { noteEntity ->
        val moshi = remember { Moshi.Builder().build() }
        val listType = remember { Types.newParameterizedType(List::class.java, String::class.java) }
        val adapter = remember { moshi.adapter<List<String>>(listType) }

        val parsedNotes = remember(noteEntity) {
            GeneratedStudyNotes(
                title = noteEntity.title,
                summary = noteEntity.summary,
                importantConcepts = adapter.fromJson(noteEntity.importantConceptsJson) ?: emptyList(),
                keyDefinitions = adapter.fromJson(noteEntity.keyDefinitionsJson) ?: emptyList(),
                examPoints = adapter.fromJson(noteEntity.examPointsJson) ?: emptyList(),
                examples = adapter.fromJson(noteEntity.examplesJson) ?: emptyList(),
                quickRevision = adapter.fromJson(noteEntity.quickRevisionJson) ?: emptyList()
            )
        }

        AlertDialog(
            onDismissRequest = { activeNoteForView = null },
            title = {
                Column {
                    Text(noteEntity.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("${noteEntity.subject} • ${noteEntity.topic}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            StudyNoteContentView(
                                notes = parsedNotes,
                                onSave = null,
                                onCopy = {
                                    val text = formatNotesToPlainText(parsedNotes)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Study Note", text))
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onDownloadPdf = {
                                    com.example.util.PdfExporter.exportStudyNotesPdf(context, parsedNotes)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeNoteForView = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SavedNoteCard(
    note: StudyNoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("saved_note_card_${note.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = note.subject,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyNoteContentView(
    notes: GeneratedStudyNotes,
    onSave: (() -> Unit)?,
    onCopy: () -> Unit,
    onDownloadPdf: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notes.title.ifBlank { "Study Notes" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    if (onDownloadPdf != null) {
                        IconButton(onClick = onDownloadPdf, modifier = Modifier.testTag("download_pdf_button")) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Notes")
                    }
                    if (onSave != null) {
                        IconButton(onClick = onSave, modifier = Modifier.testTag("save_notes_button")) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Notes", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Summary Box
            if (notes.summary.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "QUICK SUMMARY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = notes.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Important Concepts
            if (notes.importantConcepts.isNotEmpty()) {
                NoteSection(
                    title = "Important Concepts",
                    items = notes.importantConcepts,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }

            // Key Definitions
            if (notes.keyDefinitions.isNotEmpty()) {
                NoteSection(
                    title = "Key Definitions",
                    items = notes.keyDefinitions,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }

            // Exam Points
            if (notes.examPoints.isNotEmpty()) {
                NoteSection(
                    title = "High-Yield Exam Points",
                    items = notes.examPoints,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }

            // Examples
            if (notes.examples.isNotEmpty()) {
                NoteSection(
                    title = "Examples & Applications",
                    items = notes.examples,
                    accentColor = MaterialTheme.colorScheme.outline
                )
            }

            // Quick Revision
            if (notes.quickRevision.isNotEmpty()) {
                NoteSection(
                    title = "1-Minute Quick Revision",
                    items = notes.quickRevision,
                    accentColor = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onDownloadPdf != null) {
                    OutlinedButton(
                        onClick = onDownloadPdf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontWeight = FontWeight.Bold)
                    }
                }
                if (onSave != null) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Offline", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSection(
    title: String,
    items: List<String>,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        items.forEach { itemText ->
            Row(
                modifier = Modifier.padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("•", fontWeight = FontWeight.Bold, color = accentColor)
                Text(text = itemText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun formatNotesToPlainText(notes: GeneratedStudyNotes): String {
    val sb = StringBuilder()
    sb.appendLine(notes.title)
    sb.appendLine("=".repeat(notes.title.length.coerceAtLeast(10)))
    if (notes.summary.isNotBlank()) {
        sb.appendLine("\nSUMMARY:\n${notes.summary}")
    }
    if (notes.importantConcepts.isNotEmpty()) {
        sb.appendLine("\nIMPORTANT CONCEPTS:")
        notes.importantConcepts.forEach { sb.appendLine("• $it") }
    }
    if (notes.keyDefinitions.isNotEmpty()) {
        sb.appendLine("\nKEY DEFINITIONS:")
        notes.keyDefinitions.forEach { sb.appendLine("• $it") }
    }
    if (notes.examPoints.isNotEmpty()) {
        sb.appendLine("\nEXAM POINTS:")
        notes.examPoints.forEach { sb.appendLine("• $it") }
    }
    if (notes.examples.isNotEmpty()) {
        sb.appendLine("\nEXAMPLES:")
        notes.examples.forEach { sb.appendLine("• $it") }
    }
    if (notes.quickRevision.isNotEmpty()) {
        sb.appendLine("\nQUICK REVISION:")
        notes.quickRevision.forEach { sb.appendLine("• $it") }
    }
    return sb.toString()
}
