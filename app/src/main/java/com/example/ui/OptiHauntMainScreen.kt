package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Resolution
import com.example.ui.components.ComposerDock
import com.example.ui.components.ImageTileCard
import com.example.ui.components.PromptQueueSidebar
import com.example.ui.components.TopBar
import com.example.ui.dialogs.CacheDialog
import com.example.ui.dialogs.GenerationTreeDialog
import com.example.ui.dialogs.HelpDialog
import com.example.ui.dialogs.InfluencesDialog
import com.example.ui.dialogs.PickedDialog
import com.example.ui.dialogs.PromptsDialog
import com.example.ui.dialogs.SavedDialog
import com.example.ui.dialogs.SessionToolsDialog
import com.example.ui.dialogs.StrategyDialog
import com.example.ui.dialogs.ThemeSelectionDialog
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiPinkAccent
import com.example.ui.theme.OptiPurplePrimary
import kotlinx.coroutines.launch

@Composable
fun OptiHauntMainScreen(viewModel: OptiHauntViewModel) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val feedImages by viewModel.feedImages.collectAsStateWithLifecycle()
    val allPrompts by viewModel.allPrompts.collectAsStateWithLifecycle()
    val activeSteers by viewModel.activeSteers.collectAsStateWithLifecycle()
    val pendingMarks by viewModel.pendingMarks.collectAsStateWithLifecycle()
    val instructionVersions by viewModel.instructionVersions.collectAsStateWithLifecycle()
    val savedImages by viewModel.savedImages.collectAsStateWithLifecycle()
    val cachedImages by viewModel.cachedImages.collectAsStateWithLifecycle()
    val pickedImages by viewModel.pickedImages.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val editingPromptId by viewModel.editingPromptId.collectAsStateWithLifecycle()
    val aiSuggestionText by viewModel.aiSuggestionText.collectAsStateWithLifecycle()
    val aiSuggestionState by viewModel.aiSuggestionState.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    var showSidebar by remember { mutableStateOf(false) }
    var showPromptsDialog by remember { mutableStateOf(false) }
    var showInfluencesDialog by remember { mutableStateOf(false) }
    var showStrategyDialog by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }
    var showTreeDialog by remember { mutableStateOf(false) }
    var showPickedDialog by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val upcomingPrompts = remember(allPrompts, session?.currentVersion) {
        allPrompts.filter { it.used == 0 && !it.stale && it.version == (session?.currentVersion ?: 1) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(
                session = session,
                onOpenPrompts = { showPromptsDialog = true },
                onOpenSaved = { showSavedDialog = true },
                onOpenCache = { showCacheDialog = true },
                onOpenPicked = { showPickedDialog = true },
                onOpenTree = { showTreeDialog = true },
                onOpenStrategy = { showStrategyDialog = true },
                onOpenTools = { showToolsDialog = true },
                onOpenThemeSelector = { showThemeDialog = true },
                onOpenHelp = { showHelpDialog = true },
                onToggleSidebar = { showSidebar = !showSidebar },
                onToggleMasterPause = { viewModel.toggleMasterPause() },
                onTogglePausePrompts = { viewModel.togglePausePrompts() },
                onTogglePauseImages = { viewModel.togglePauseImages() },
                onSelectResolution = { viewModel.setResolution(it) }
            )
        },
        bottomBar = {
            ComposerDock(
                session = session,
                pendingMarksCount = pendingMarks.size,
                onStart = { viewModel.startInstruction(it) },
                onDraftChange = { viewModel.updatePendingDraft(it) },
                onApply = { viewModel.applyInstructionChange(it) },
                onDiscard = { viewModel.discardPendingDraft() },
                onOpenInfluences = { showInfluencesDialog = true }
            )
        },
        floatingActionButton = {
            if (feedImages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OptiPinkAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Jump to newest", modifier = Modifier.size(16.dp))
                    },
                    text = { Text("↓ Newest", fontSize = 12.sp) },
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 76.dp)
                        .testTag("jump_newest_button")
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 700.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Main Feed Column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    if (feedImages.isEmpty()) {
                        // Intro empty placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = OptiPurplePrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "Image Prompt Optimizer",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "1. Type what you want to see in the box at the bottom, then press Start ▶.\n\n" +
                                                "2. An AI writes a batch of image prompts from it. They line up in the queue, where you can edit or delete them before they're drawn.\n\n" +
                                                "3. Tap the images you like. Liked prompts guide the next batches, so the stream drifts toward your taste.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )

                                    Button(
                                        onClick = { showHelpDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = OptiCyanAccent.copy(alpha = 0.2f)),
                                        modifier = Modifier.testTag("intro_help_button")
                                    ) {
                                        Text("📖 Help — how it all works", color = OptiCyanAccent)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("image_feed_list")
                        ) {
                            items(feedImages, key = { it.id }) { image ->
                                val isMarkedPositive = pendingMarks.any { it.promptId == image.promptId && !it.isAnti }
                                val isMarkedAnti = pendingMarks.any { it.promptId == image.promptId && it.isAnti }

                                ImageTileCard(
                                    image = image,
                                    isMarkedPositive = isMarkedPositive,
                                    isMarkedAnti = isMarkedAnti,
                                    onToggleUpvote = { viewModel.toggleUpvote(image.id) },
                                    onToggleSave = { viewModel.toggleSaveImage(image.id) },
                                    onTogglePositiveInfluence = {
                                        viewModel.toggleMarkInfluence(image, isAnti = false)
                                    },
                                    onToggleAntiInfluence = {
                                        viewModel.toggleMarkInfluence(image, isAnti = true)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                // Wide screen permanent side queue or slide-over drawer on mobile
                if (isWideScreen || showSidebar) {
                    AnimatedVisibility(
                        visible = isWideScreen || showSidebar,
                        enter = slideInHorizontally(initialOffsetX = { it }),
                        exit = slideOutHorizontally(targetOffsetX = { it })
                    ) {
                        PromptQueueSidebar(
                            statusMessage = statusMessage,
                            upcomingPrompts = upcomingPrompts,
                            activeSteers = activeSteers,
                            batchRule = session?.batchRule.orEmpty(),
                            promptPrefix = session?.promptPrefix.orEmpty(),
                            promptSuffix = session?.promptSuffix.orEmpty(),
                            editingPromptId = editingPromptId,
                            onAddPrompt = { viewModel.addManualPrompt(it) },
                            onAddSteer = { viewModel.addThemeSteer(it) },
                            onRemoveSteer = { viewModel.removeThemeSteer(it) },
                            onUpdateRules = { rule, pref, suff ->
                                viewModel.updateRulesAndFixes(rule, pref, suff)
                            },
                            onStartEditingPrompt = {
                                viewModel.scheduler.setEditingPrompt(it)
                            },
                            onDoneEditingPrompt = { id, newText ->
                                viewModel.updatePromptText(id, newText)
                            },
                            onDeletePrompt = { viewModel.deletePrompt(it) },
                            onClearUpcomingQueue = { viewModel.clearUpcomingQueue() },
                            onClearNonSteeredQueue = { viewModel.clearNonSteeredQueue() },
                            onCloseSidebar = { showSidebar = false }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showPromptsDialog) {
        PromptsDialog(
            prompts = allPrompts,
            currentVersion = session?.currentVersion ?: 1,
            onDrawNext = { viewModel.drawPromptNext(it) },
            onAddPrompt = { viewModel.addManualPrompt(it) },
            onDismiss = { showPromptsDialog = false }
        )
    }

    if (showInfluencesDialog) {
        InfluencesDialog(
            pendingMarks = pendingMarks,
            userNote = session?.pendingWhy,
            aiSuggestionText = aiSuggestionText,
            aiSuggestionState = aiSuggestionState,
            onNoteChange = { viewModel.updatePendingWhy(it) },
            onRequestAiHelp = { viewModel.requestAiHelp(it) },
            onUseAsDraft = {
                viewModel.useAiSuggestionAsDraft()
                showInfluencesDialog = false
            },
            onApplySuggestion = {
                viewModel.applyAiSuggestion()
                showInfluencesDialog = false
            },
            onDismiss = { showInfluencesDialog = false }
        )
    }

    if (showStrategyDialog) {
        StrategyDialog(
            session = session,
            onSetCreativity = { viewModel.setCreativity(it) },
            onToggleMixPrompts = { viewModel.toggleMixPrompts() },
            onTogglePromoteUpvoted = { viewModel.togglePromoteUpvoted() },
            onToggleFreshOnly = { viewModel.toggleFreshOnly() },
            onTogglePrioritizeSteered = { viewModel.togglePrioritizeSteered() },
            onDismiss = { showStrategyDialog = false }
        )
    }

    if (showToolsDialog) {
        SessionToolsDialog(
            currentSession = session,
            allSessions = allSessions,
            onSpinOffTrimmed = { name, count ->
                viewModel.spinOffTrimmedSession(name, count)
            },
            onClearSteerHistory = { viewModel.clearSteerHistory() },
            onSwitchSession = { viewModel.switchSession(it) },
            onCreateNewSession = { viewModel.createNewSession(it) },
            onDismiss = { showToolsDialog = false }
        )
    }

    if (showTreeDialog) {
        GenerationTreeDialog(
            versions = instructionVersions,
            prompts = allPrompts,
            images = feedImages,
            marks = pendingMarks,
            onDismiss = { showTreeDialog = false }
        )
    }

    if (showPickedDialog) {
        PickedDialog(
            images = pickedImages,
            onToggleUpvote = { viewModel.toggleUpvote(it) },
            onToggleSave = { viewModel.toggleSaveImage(it) },
            onDismiss = { showPickedDialog = false }
        )
    }

    if (showSavedDialog) {
        SavedDialog(
            images = savedImages,
            onToggleSave = { viewModel.toggleSaveImage(it) },
            onDismiss = { showSavedDialog = false }
        )
    }

    if (showCacheDialog) {
        CacheDialog(
            images = cachedImages,
            onDismiss = { showCacheDialog = false }
        )
    }

    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onSelectTheme = { viewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}
