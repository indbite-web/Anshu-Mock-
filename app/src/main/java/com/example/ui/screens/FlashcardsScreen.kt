package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FlashcardEntity
import com.example.data.viewmodel.MainViewModel
import com.example.model.GeneratedFlashcardItem
import com.example.model.GeneratedFlashcardSet

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
    val generatedSet by viewModel.generatedFlashcardSet.collectAsState()

    var subjectInput by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    var cardCountInput by remember { mutableIntStateOf(8) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Active review session state
    var activeReviewDeck by remember { mutableStateOf<List<FlashcardEntity>?>(null) }
    var reviewTitle by remember { mutableStateOf("") }

    val primaryExam by viewModel.primaryExam.collectAsState()

    LaunchedEffect(primaryExam) {
        if (subjectInput.isBlank() && primaryExam.isNotBlank()) {
            subjectInput = primaryExam
        }
    }

    // Group saved cards by (subject, topic)
    val groupedSavedCards = remember(savedCards) {
        savedCards.groupBy { "${it.subject}:::${it.topic}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Flashcards", fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    "Generate flashcards for active recall. Tap card during review to flip and check answer.",
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
                        Column {
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
                                    onSuccess = {
                                        Toast.makeText(context, "Flashcards ready!", Toast.LENGTH_SHORT).show()
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

                    // Render Preview of Generated Deck
                    generatedSet?.let { deck ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${deck.topic} (${deck.flashcards.size} Cards)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.saveFlashcards(
                                                    subject = subjectInput.ifBlank { "General" },
                                                    topic = topicInput,
                                                    cards = deck.flashcards,
                                                    onSaved = {
                                                        Toast.makeText(context, "Flashcard deck saved offline!", Toast.LENGTH_SHORT).show()
                                                        selectedTabIndex = 1
                                                    }
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("save_flashcards_deck_button")
                                        ) {
                                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save Deck")
                                        }
                                    }

                                    deck.flashcards.forEachIndexed { idx, cardItem ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("Card ${idx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Q: ${cardItem.frontText}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("A: ${cardItem.backText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Decks Tab
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                            IconButton(
                                                onClick = { viewModel.deleteFlashcardSet(subj, top) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete deck", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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
                                                activeReviewDeck = cards
                                                reviewTitle = "$top ($subj)"
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

    // Interactive Review Session Dialog
    activeReviewDeck?.let { deck ->
        FlashcardReviewDialog(
            title = reviewTitle,
            cards = deck,
            onDismiss = { activeReviewDeck = null },
            onUpdateMastery = { card, newState ->
                viewModel.updateFlashcardMastery(card, newState)
            }
        )
    }
}

@Composable
fun FlashcardReviewDialog(
    title: String,
    cards: List<FlashcardEntity>,
    onDismiss: () -> Unit,
    onUpdateMastery: (FlashcardEntity, String) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val currentCard = cards.getOrNull(currentIndex)

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "card_flip"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Card ${currentIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            if (currentCard != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12 * density
                            }
                            .clickable { isFlipped = !isFlipped }
                            .testTag("interactive_flashcard"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotation <= 90f) {
                                // Front Text
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("QUESTION / CONCEPT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        currentCard.frontText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("(Tap card to reveal answer)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            } else {
                                // Back Text (Inverted scale for back side text)
                                Column(
                                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("ANSWER / EXPLANATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        currentCard.backText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Mastery Buttons
                    Text("Mastery Assessment:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateMastery(currentCard, "New")
                                isFlipped = false
                                if (currentIndex < cards.size - 1) currentIndex++ else onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Again")
                        }

                        OutlinedButton(
                            onClick = {
                                onUpdateMastery(currentCard, "Learning")
                                isFlipped = false
                                if (currentIndex < cards.size - 1) currentIndex++ else onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Hard")
                        }

                        Button(
                            onClick = {
                                onUpdateMastery(currentCard, "Known")
                                isFlipped = false
                                if (currentIndex < cards.size - 1) currentIndex++ else onDismiss()
                            }
                        ) {
                            Text("Easy (Known)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close Session")
            }
        }
    )
}
