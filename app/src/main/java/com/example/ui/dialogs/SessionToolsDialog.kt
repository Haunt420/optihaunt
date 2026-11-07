package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectSessionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionToolsDialog(
    currentSession: ProjectSessionEntity?,
    allSessions: List<ProjectSessionEntity>,
    onSpinOffTrimmed: (String, Int) -> Unit,
    onClearSteerHistory: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onCreateNewSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val totalVersions = currentSession?.currentVersion ?: 1
    var keepVersionsSlider by remember { mutableFloatStateOf(totalVersions.toFloat()) }
    var newTrimmedName by remember { mutableStateOf("${currentSession?.name ?: "Session"} (Trimmed)") }
    var newSessionName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("session_tools_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("🧰 Session Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Spin off trimmed copies, steer history, and sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_tools_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))

            // 1. Spin off trimmed copy
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("↪ Spin off a trimmed copy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Saves a new session with only your latest instructions and what they made. This session isn't changed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Keep latest ${keepVersionsSlider.toInt()} of $totalVersions instructions", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = keepVersionsSlider,
                        onValueChange = { keepVersionsSlider = it },
                        valueRange = 1f..maxOf(1f, totalVersions.toFloat()),
                        steps = maxOf(0, totalVersions - 2)
                    )

                    OutlinedTextField(
                        value = newTrimmedName,
                        onValueChange = { newTrimmedName = it },
                        label = { Text("New session name") },
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSpinOffTrimmed(newTrimmedName, keepVersionsSlider.toInt())
                            Toast.makeText(context, "Trimmed copy created", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_trimmed_button")
                    ) {
                        Text("↪ Create Trimmed Copy")
                    }
                }
            }

            // 2. Steer history
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🧹 Steer History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Forgets the steer text behind past steered images so they stop showing it. Irreversible.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            onClearSteerHistory()
                            Toast.makeText(context, "Steer history cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_steer_history_button")
                    ) {
                        Text("🧹 Clear Steer History")
                    }
                }
            }

            // 3. Switch Sessions / Create New
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📂 All Sessions (${allSessions.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newSessionName,
                            onValueChange = { newSessionName = it },
                            placeholder = { Text("New session name…", fontSize = 12.sp) },
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newSessionName.isNotBlank()) {
                                    onCreateNewSession(newSessionName)
                                    newSessionName = ""
                                    onDismiss()
                                }
                            },
                            enabled = newSessionName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("New")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allSessions.forEach { sess ->
                            val isCurrent = sess.id == currentSession?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!isCurrent) {
                                            onSwitchSession(sess.id)
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    sess.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isCurrent) {
                                    Text("Active ✓", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
