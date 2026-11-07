package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.InfluenceMarkEntity
import com.example.data.model.InstructionVersionEntity
import com.example.data.model.PromptEntity
import com.example.ui.theme.OptiAntiRed
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiPositiveGreen
import com.example.ui.theme.OptiPurplePrimary
import com.example.ui.theme.OptiSteerAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationTreeDialog(
    versions: List<InstructionVersionEntity>,
    prompts: List<PromptEntity>,
    images: List<GeneratedImageEntity>,
    marks: List<InfluenceMarkEntity>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("generation_tree_sheet")
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
                    Text("🌳 Prompt Generation Tree", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Causal lineage across instruction versions, steers & feedback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_tree_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (versions.isEmpty()) {
                    item {
                        Text("No instruction versions created yet. Start a run to begin lineage tree.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(versions, key = { it.id }) { ver ->
                        val verPrompts = prompts.filter { it.version == ver.versionNumber }
                        val verImages = images.filter { it.versionNumber == ver.versionNumber }
                        val verMarks = marks.filter { it.committedVersion == ver.versionNumber }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OptiPurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Version Node Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(OptiPurplePrimary, CircleShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                "v${ver.versionNumber}",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            ver.text,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2
                                        )
                                    }
                                }

                                if (!ver.whyNote.isNullOrBlank()) {
                                    Text("💭 Reason: ${ver.whyNote}", fontSize = 12.sp, color = OptiCyanAccent)
                                }

                                // Committed Marks that led to this version
                                if (verMarks.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Committed Influences (${verMarks.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            verMarks.forEach { m ->
                                                Text(
                                                    "${if (m.isAnti) "🚫" else "🔖"} ${m.text}",
                                                    fontSize = 11.sp,
                                                    color = if (m.isAnti) OptiAntiRed else OptiPositiveGreen,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Prompts generated under this version
                                Text(
                                    "Prompts (${verPrompts.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    verPrompts.take(6).forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (p.influenceText != null) {
                                                Text("🧲", fontSize = 10.sp)
                                            } else if (p.origin == "promoted") {
                                                Text("🎯", fontSize = 10.sp)
                                            } else if (p.origin == "manual") {
                                                Text("✏️", fontSize = 10.sp)
                                            } else {
                                                Text("•", fontSize = 10.sp)
                                            }

                                            Text(
                                                p.text,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (p.votes > 0) {
                                                Text("❤️ ${p.votes}", fontSize = 10.sp, color = Color(0xFFF472B6))
                                            }
                                        }
                                    }
                                    if (verPrompts.size > 6) {
                                        Text("+ ${verPrompts.size - 6} more prompts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Images stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        "${verImages.size} images generated · ${verImages.count { it.isUpvoted }} liked",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
