package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptEntity
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiManualBlue
import com.example.ui.theme.OptiPinkAccent
import com.example.ui.theme.OptiPurplePrimary
import com.example.ui.theme.OptiSteerAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsDialog(
    prompts: List<PromptEntity>,
    currentVersion: Int,
    onDrawNext: (String) -> Unit,
    onAddPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf("current") } // "current" or "all"
    var newPromptInput by remember { mutableStateOf("") }

    val filteredPrompts = remember(prompts, searchQuery, scopeFilter, currentVersion) {
        prompts
            .filter { p ->
                if (scopeFilter == "current") p.version == currentVersion else true
            }
            .filter { p ->
                if (searchQuery.isBlank()) true else p.text.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<PromptEntity> { it.score }
                    .thenByDescending { it.votes }
                    .thenByDescending { it.createdAt }
            )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("prompts_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("📜 Prompts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Best-liked first · every prompt in session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_prompts_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search and Scope Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search prompts…", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prompts_search_input")
                )

                FilterChip(
                    selected = scopeFilter == "current",
                    onClick = { scopeFilter = "current" },
                    label = { Text("This instruction", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = scopeFilter == "all",
                    onClick = { scopeFilter = "all" },
                    label = { Text("All", fontSize = 11.sp) }
                )
            }

            // Add Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = newPromptInput,
                    onValueChange = { newPromptInput = it },
                    placeholder = { Text("Add prompt to front of queue…", fontSize = 12.sp) },
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prompts_add_input")
                )
                Button(
                    onClick = {
                        if (newPromptInput.isNotBlank()) {
                            onAddPrompt(newPromptInput)
                            newPromptInput = ""
                        }
                    },
                    enabled = newPromptInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OptiManualBlue)
                ) {
                    Text("Add")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = DividerDefaults.color.copy(alpha = 0.3f))

            // Prompts List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPrompts, key = { it.id }) { prompt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Score / Fresh badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (prompt.used == 0) OptiCyanAccent.copy(alpha = 0.2f) else OptiPinkAccent.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (prompt.used == 0) "Fresh" else "${(prompt.score * 100).toInt()}% (${prompt.votes}/${prompt.used})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (prompt.used == 0) OptiCyanAccent else OptiPinkAccent
                                        )
                                    }

                                    // Origin tag
                                    when (prompt.origin) {
                                        "manual" -> Text("✏️ yours", fontSize = 10.sp, color = OptiManualBlue)
                                        "promoted" -> Text("🎯 promoted", fontSize = 10.sp, color = OptiPurplePrimary)
                                        "creative" -> Text("🎨 creative", fontSize = 10.sp, color = OptiCyanAccent)
                                    }

                                    if (prompt.influenceText != null) {
                                        Text("🧲 ${prompt.influenceText}", fontSize = 10.sp, color = OptiSteerAmber, maxLines = 1)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // ✏️ Draw Next Button
                                    OutlinedButton(
                                        onClick = {
                                            onDrawNext(prompt.text)
                                            Toast.makeText(context, "Queued to front", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Text("✏️ Draw next", fontSize = 10.sp)
                                    }

                                    // 📋 Copy Button
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Prompt", prompt.text)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied prompt", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(prompt.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
