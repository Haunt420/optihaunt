package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectSessionEntity
import com.example.data.model.Resolution
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiPurplePrimary

@Composable
fun TopBar(
    session: ProjectSessionEntity?,
    onOpenPrompts: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenPicked: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenStrategy: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenThemeSelector: () -> Unit,
    onOpenHelp: () -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleMasterPause: () -> Unit,
    onTogglePausePrompts: () -> Unit,
    onTogglePauseImages: () -> Unit,
    onSelectResolution: (Resolution) -> Unit,
    modifier: Modifier = Modifier
) {
    var pauseMenuExpanded by remember { mutableStateOf(false) }
    var resolutionMenuExpanded by remember { mutableStateOf(false) }

    val currentResolution = session?.let { Resolution.fromId(it.resolution) } ?: Resolution.SQUARE
    val isPaused = session?.isPaused == true
    val pausedPrompts = session?.pausedPrompts == true
    val pausedImages = session?.pausedImages == true

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Queue Drawer Toggle
            IconButton(
                onClick = onToggleSidebar,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toggle_queue_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ViewAgenda,
                    contentDescription = "Toggle Prompt Queue",
                    tint = OptiCyanAccent
                )
            }

            // Prompts modal button
            AssistChip(
                onClick = onOpenPrompts,
                label = { Text("📜 Prompts") },
                modifier = Modifier.testTag("show_prompts_button"),
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Saved button
            AssistChip(
                onClick = onOpenSaved,
                label = { Text("🖼️ Saved") },
                modifier = Modifier.testTag("show_saved_button")
            )

            // Picked button
            AssistChip(
                onClick = onOpenPicked,
                label = { Text("🧺 Picked") },
                modifier = Modifier.testTag("show_picked_button")
            )

            // Cache button
            AssistChip(
                onClick = onOpenCache,
                label = { Text("📷 Cache") },
                modifier = Modifier.testTag("show_cache_button")
            )

            // Generation Tree
            AssistChip(
                onClick = onOpenTree,
                label = { Text("🌳 Tree") },
                modifier = Modifier.testTag("show_tree_button")
            )

            // Resolution Selector
            Box {
                FilterChip(
                    selected = false,
                    onClick = { resolutionMenuExpanded = true },
                    label = { Text("${currentResolution.icon} ${currentResolution.label}") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Resolution Options")
                    },
                    modifier = Modifier.testTag("resolution_select_button")
                )
                DropdownMenu(
                    expanded = resolutionMenuExpanded,
                    onDismissRequest = { resolutionMenuExpanded = false }
                ) {
                    Resolution.entries.forEach { res ->
                        DropdownMenuItem(
                            text = { Text("${res.icon} ${res.label} (${res.width}×${res.height})") },
                            onClick = {
                                onSelectResolution(res)
                                resolutionMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Pause Split / Dropdown
            Box {
                FilterChip(
                    selected = isPaused,
                    onClick = onToggleMasterPause,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isPaused) "▶ Resume" else "⏸ Pause")
                            IconButton(
                                onClick = { pauseMenuExpanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Pause Options")
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.testTag("pause_button")
                )
                DropdownMenu(
                    expanded = pauseMenuExpanded,
                    onDismissRequest = { pauseMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (pausedPrompts) "✍️ Writing Prompts: Paused" else "✍️ Writing Prompts: Running")
                        },
                        onClick = {
                            onTogglePausePrompts()
                            pauseMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (pausedImages) "🖼️ Drawing Images: Paused" else "🖼️ Drawing Images: Running")
                        },
                        onClick = {
                            onTogglePauseImages()
                            pauseMenuExpanded = false
                        }
                    )
                }
            }

            // Strategy Button
            AssistChip(
                onClick = onOpenStrategy,
                label = { Text("⚗️ Strategy") },
                modifier = Modifier.testTag("strategy_button")
            )

            // Theme Selector Button
            AssistChip(
                onClick = onOpenThemeSelector,
                label = { Text("🎨 Theme") },
                modifier = Modifier.testTag("theme_selector_button")
            )

            // Session Tools Button
            AssistChip(
                onClick = onOpenTools,
                label = { Text("🧰 Tools") },
                modifier = Modifier.testTag("session_tools_button")
            )

            // Help Button
            IconButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Help Guide",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}
