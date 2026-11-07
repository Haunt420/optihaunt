package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptEntity
import com.example.data.model.ThemeSteerEntity
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiManualBlue
import com.example.ui.theme.OptiPurplePrimary
import com.example.ui.theme.OptiSteerAmber
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptQueueSidebar(
    statusMessage: String,
    upcomingPrompts: List<PromptEntity>,
    activeSteers: List<ThemeSteerEntity>,
    batchRule: String,
    promptPrefix: String,
    promptSuffix: String,
    editingPromptId: String?,
    onAddPrompt: (String) -> Unit,
    onAddSteer: (String) -> Unit,
    onRemoveSteer: (String) -> Unit,
    onUpdateRules: (String, String, String) -> Unit,
    onStartEditingPrompt: (String) -> Unit,
    onDoneEditingPrompt: (String, String) -> Unit,
    onDeletePrompt: (String) -> Unit,
    onClearUpcomingQueue: () -> Unit,
    onClearNonSteeredQueue: () -> Unit,
    onCloseSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var addPromptInput by remember { mutableStateOf("") }
    var steerInput by remember { mutableStateOf("") }
    var currentRule by remember(batchRule) { mutableStateOf(batchRule) }
    var currentPrefix by remember(promptPrefix) { mutableStateOf(promptPrefix) }
    var currentSuffix by remember(promptSuffix) { mutableStateOf(promptSuffix) }
    var editingText by remember { mutableStateOf("") }

    // Live countdown ticker
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(500)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxHeight()
            .width(340.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Prompt Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (editingPromptId != null) MaterialTheme.colorScheme.tertiary else OptiCyanAccent
                    )
                }
                IconButton(onClick = onCloseSidebar, modifier = Modifier.testTag("close_queue_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close Queue")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Add a prompt (manual)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✏️ Add a prompt (drawn first)", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = addPromptInput,
                                onValueChange = { addPromptInput = it },
                                placeholder = { Text("✏️ Add prompt or paste list…", fontSize = 12.sp) },
                                maxLines = 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inject_prompt_input"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OptiManualBlue)
                            )
                            Button(
                                onClick = {
                                    if (addPromptInput.isNotBlank()) {
                                        onAddPrompt(addPromptInput)
                                        addPromptInput = ""
                                    }
                                },
                                enabled = addPromptInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("inject_prompt_button")
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }

                // Section 2: Steer next batch
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🧲 Steer next batch", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = steerInput,
                                onValueChange = { steerInput = it },
                                placeholder = { Text("e.g. cyberpunk neon, noir…", fontSize = 12.sp) },
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("steer_input"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.tertiary)
                            )
                            Button(
                                onClick = {
                                    if (steerInput.isNotBlank()) {
                                        onAddSteer(steerInput)
                                        steerInput = ""
                                    }
                                },
                                enabled = steerInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                modifier = Modifier.testTag("steer_button")
                            ) {
                                Text("Steer", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Steer chips
                        if (activeSteers.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activeSteers.forEach { steer ->
                                    InputChip(
                                        selected = true,
                                        onClick = {},
                                        label = { Text("🧲 ${steer.text}", fontSize = 11.sp) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onRemoveSteer(steer.id) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "Remove steer")
                                            }
                                        },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Rules for every prompt
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Rules for every prompt", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        // Batch Rule
                        OutlinedTextField(
                            value = currentRule,
                            onValueChange = {
                                currentRule = it
                                onUpdateRules(it, currentPrefix, currentSuffix)
                            },
                            label = { Text("📏 Rule for every batch ({a|b} rolls)") },
                            placeholder = { Text("e.g. {natural lighting|cinematic}") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_rule_input")
                        )

                        // Prefix
                        OutlinedTextField(
                            value = currentPrefix,
                            onValueChange = {
                                currentPrefix = it
                                onUpdateRules(currentRule, it, currentSuffix)
                            },
                            label = { Text("Prefix (applied to all images)") },
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prefix_input")
                        )

                        // Suffix
                        OutlinedTextField(
                            value = currentSuffix,
                            onValueChange = {
                                currentSuffix = it
                                onUpdateRules(currentRule, currentPrefix, it)
                            },
                            label = { Text("Suffix (applied to all images)") },
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suffix_input")
                        )
                    }
                }

                // Section 4: Upcoming prompts list
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Upcoming (${upcomingPrompts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Text(
                                "Clear all",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { onClearUpcomingQueue() }
                                    .padding(horizontal = 4.dp)
                                    .testTag("clear_queue_button")
                            )
                        }
                    }
                }

                // Editing banner if active
                if (editingPromptId != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "✍️ Paused while editing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        onDoneEditingPrompt(editingPromptId, editingText)
                                    },
                                    modifier = Modifier.testTag("done_editing_button"),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Text("Done · Resume", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                items(upcomingPrompts, key = { it.id }) { prompt ->
                    val isEditingThis = editingPromptId == prompt.id
                    val remainingSec = maxOf(0, ((prompt.readyAt - currentTime) / 1000).toInt())

                    Surface(
                        color = if (isEditingThis) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isEditingThis) OptiPurplePrimary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!isEditingThis) {
                                    editingText = prompt.text
                                    onStartEditingPrompt(prompt.id)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Cooldown badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (remainingSec > 0) MaterialTheme.colorScheme.surfaceDim else OptiCyanAccent.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (remainingSec > 0) "⏳ ${remainingSec}s" else "Ready ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Origin tag
                                    when (prompt.origin) {
                                        "manual" -> Text("✏️ yours", fontSize = 10.sp, color = OptiManualBlue)
                                        "promoted" -> Text("🎯 promoted", fontSize = 10.sp, color = OptiPurplePrimary)
                                        "creative" -> Text("🎨 creative", fontSize = 10.sp, color = OptiCyanAccent)
                                    }

                                    if (prompt.influenced) {
                                        Text("🧲 steered", fontSize = 10.sp, color = OptiSteerAmber)
                                    }
                                }

                                IconButton(
                                    onClick = { onDeletePrompt(prompt.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Prompt", modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (isEditingThis) {
                                OutlinedTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { onDoneEditingPrompt(prompt.id, editingText) },
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Text("Save & Resume", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Text(
                                    prompt.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
