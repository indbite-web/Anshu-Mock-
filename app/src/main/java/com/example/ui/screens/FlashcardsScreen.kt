package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.FlashcardEntity
import com.example.data.viewmodel.MainViewModel
import com.example.model.GeneratedFlashcardSet

data class FlashcardDisplayItem(
    val frontText: String,
    val backText: String,
    val rawEntity: FlashcardEntity? = null
)

data class ReviewSessionData(
    val subject: String,
    val topic: String,
    val cards: List<FlashcardDisplayItem>,
    val isUnsaved: Boolean = false,
    val rawGeneratedSet: GeneratedFlashcardSet? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val savedCards by viewModel.savedFlashcards.collectAsState()
    val isGenerating by viewModel.isGeneratingFlashcards.collectAsState()
    val generationStatus by viewModel.flashcardsGenerationStatus.collectAsState()

    var selectedLanguage by remember { mutableStateOf("English") }
    var subjectInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    var cardCountInput by remember { mutableIntStateOf(8) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Active review session state
    var activeReviewSession by remember { mutableStateOf<ReviewSessionData?>(null) }

    val primaryExam by viewModel.primaryExam.collectAsState()
    val preferredLanguage by viewModel.preferredLanguage.collectAsState()

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

    // Group saved cards by (subject, topic)
    val groupedSavedCards = remember(savedCards) {
        savedCards.groupBy { "${it.subject}:::${it.topic}" }
    }

    // If an active review session is running, render the dedicated Flashcard Review Screen
    val currentSession = activeReviewSession
    if (currentSession != null) {
        FlashcardReviewScreen(
            session = currentSession,
            viewModel = viewModel,
            onNavigateBack = { activeReviewSession = null }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.flashcards_title), fontWeight = FontWeight.Bold)
                        Text(
                            "Smart active recall & spaced review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("flashcards_back_button")) {
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
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Generate", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Decks (${groupedSavedCards.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTabIndex == 0) {
                // Generate Tab - Clean input form only
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    "Generate flashcards for active recall. After generation, the deck automatically opens in review mode.",
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
                            label = { Text("Subject / Exam (e.g., Biology, UPSC, History)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flashcards_subject_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Topic / Chapter (e.g., Cell Division, Mughals)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flashcards_topic_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Number of Flashcards: $cardCountInput",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = cardCountInput.toFloat(),
                                onValueChange = { cardCountInput = it.toInt() },
                                valueRange = 4f..15f,
                                steps = 10,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                                            .testTag("flashcards_lang_chip_$lang"),
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
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(err, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
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
                                viewModel.generateFlashcards(
                                    subject = subjectInput.ifBlank { "General" },
                                    topic = topicInput,
                                    count = cardCountInput,
                                    language = selectedLanguage,
                                    onSuccess = { cardSet ->
                                        Toast.makeText(context, "Flashcards ready!", Toast.LENGTH_SHORT).show()
                                        // Immediately open dedicated Review Screen
                                        activeReviewSession = ReviewSessionData(
                                            subject = subjectInput.ifBlank { "General" },
                                            topic = topicInput,
                                            cards = cardSet.flashcards.map {
                                                FlashcardDisplayItem(frontText = it.frontText, backText = it.backText)
                                            },
                                            isUnsaved = true,
                                            rawGeneratedSet = cardSet
                                        )
                                    },
                                    onError = { err -> errorMessage = err }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_flashcards_button"),
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
                                Text(if (generationStatus.isNotBlank()) generationStatus else "Generating Flashcards...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Flashcard Deck", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Decks Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search flashcard decks or topics...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (groupedSavedCards.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No saved flashcard decks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Generate decks above to study offline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        val filteredKeys = groupedSavedCards.keys.filter { key ->
                            searchQuery.isBlank() || key.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredKeys, key = { it }) { key ->
                                val cards = groupedSavedCards[key] ?: emptyList()
                                val parts = key.split(":::")
                                val subj = parts.getOrNull(0) ?: "General"
                                val top = parts.getOrNull(1) ?: "Topic"

                                val knownCount = cards.count { it.masteryState == "Known" }
                                val learningCount = cards.count { it.masteryState == "Learning" }
                                val newCount = cards.count { it.masteryState == "New" }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                                                Text(subj, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        com.example.util.PdfExporter.exportFlashcardsPdf(
                                                            context = context,
                                                            subject = subj,
                                                            topic = top,
                                                            cards = cards
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteFlashcardSet(subj, top) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete deck", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(top, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${cards.size} Cards", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("•", style = MaterialTheme.typography.bodySmall)
                                            Text("Known: $knownCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            Text("•", style = MaterialTheme.typography.bodySmall)
                                            Text("Learning: $learningCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            Text("•", style = MaterialTheme.typography.bodySmall)
                                            Text("New: $newCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                activeReviewSession = ReviewSessionData(
                                                    subject = subj,
                                                    topic = top,
                                                    cards = cards.map { FlashcardDisplayItem(frontText = it.frontText, backText = it.backText, rawEntity = it) },
                                                    isUnsaved = false
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("start_flashcard_review_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Review Session")
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

/**
 * Dedicated Full Screen for Flashcard Review Mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardReviewScreen(
    session: ReviewSessionData,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isSavedOffline by remember { mutableStateOf(!session.isUnsaved) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Flashcards", fontWeight = FontWeight.Bold)
                        Text(
                            "Smart active recall & spaced review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("review_back_button")) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Deck Header Card (Topic, subject, PDF, Save)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            session.topic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                session.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("•", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${session.cards.size} Cards",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // PDF Export Button
                        OutlinedButton(
                            onClick = {
                                val entities = session.cards.map {
                                    FlashcardEntity(
                                        subject = session.subject,
                                        topic = session.topic,
                                        frontText = it.frontText,
                                        backText = it.backText
                                    )
                                }
                                com.example.util.PdfExporter.exportFlashcardsPdf(
                                    context = context,
                                    subject = session.subject,
                                    topic = session.topic,
                                    cards = entities
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        // Save Button
                        if (isSavedOffline) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    session.rawGeneratedSet?.let { rawSet ->
                                        viewModel.saveFlashcards(
                                            subject = session.subject,
                                            topic = session.topic,
                                            cards = rawSet.flashcards,
                                            onSaved = {
                                                isSavedOffline = true
                                                Toast.makeText(context, "Deck saved offline!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Single Interactive Flashcard Review Component
            InteractiveFlashcardReview(
                cards = session.cards,
                onUpdateMastery = { item, newState ->
                    item.rawEntity?.let { entity ->
                        viewModel.updateFlashcardMastery(entity, newState)
                    }
                }
            )
        }
    }
}

/**
 * Single Interactive 3D Flashcard Review Component
 */
@Composable
fun InteractiveFlashcardReview(
    cards: List<FlashcardDisplayItem>,
    onUpdateMastery: ((FlashcardDisplayItem, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Always reset card to FRONT when changing card index
    LaunchedEffect(currentIndex) {
        isFlipped = false
    }

    val currentCard = cards.getOrNull(currentIndex) ?: return

    // Smooth 3D Flip Rotation around Y axis
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "card_flip"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Container with side navigation arrows and drag gesture support
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Circular Previous Arrow
            IconButton(
                onClick = {
                    if (currentIndex > 0) {
                        currentIndex--
                        isFlipped = false
                    }
                },
                enabled = currentIndex > 0,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (currentIndex > 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Card",
                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            // Main 3D Flashcard
            Card(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 240.dp, max = 340.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 16 * density
                    }
                    .pointerInput(currentIndex) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (totalDrag < -70f && currentIndex < cards.size - 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentIndex++
                                    isFlipped = false
                                } else if (totalDrag > 70f && currentIndex > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentIndex--
                                    isFlipped = false
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isFlipped = !isFlipped
                    }
                    .testTag("interactive_flashcard"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isFlipped) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    } else {
                        Color(0xFFE8F8F0) // Subtle green-tinted back card
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (!isFlipped) {
                        // FRONT SIDE
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: FRONT badge & Card count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "FRONT",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Card ${currentIndex + 1} / ${cards.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Center Content: Icon & Question
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = currentCard.frontText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Bottom Hint
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Tap the card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        // BACK SIDE (Inverted rotationY = 180f to prevent text mirroring)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: BACK badge & Card count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFC8E6C9),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "BACK",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                Text(
                                    "Card ${currentIndex + 1} / ${cards.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            // Center Content: Lightbulb icon, "Answer:" heading & Answer text
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Answer:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentCard.backText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF1C3A27),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Bottom Hint
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF2E7D32).copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Tap the card to flip",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Right Circular Next Arrow
            IconButton(
                onClick = {
                    if (currentIndex < cards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                enabled = currentIndex < cards.size - 1,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (currentIndex < cards.size - 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Card",
                    tint = if (currentIndex < cards.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        // Progress Section ("Progress: 1 / 8" + Horizontal progress bar)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progress:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${currentIndex + 1} / ${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / cards.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        }

        // Rating Buttons Section (Again, Hard, Good, Easy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onUpdateMastery?.invoke(currentCard, "New")
                    if (currentIndex < cards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Again", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    onUpdateMastery?.invoke(currentCard, "Learning")
                    if (currentIndex < cards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Hard", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    onUpdateMastery?.invoke(currentCard, "Learning")
                    if (currentIndex < cards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Good", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onUpdateMastery?.invoke(currentCard, "Known")
                    if (currentIndex < cards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                modifier = Modifier.weight(1.1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Easy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Full-width Primary "Next Card →" Button
        Button(
            onClick = {
                if (currentIndex < cards.size - 1) {
                    currentIndex++
                    isFlipped = false
                }
            },
            enabled = currentIndex < cards.size - 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next Card →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
