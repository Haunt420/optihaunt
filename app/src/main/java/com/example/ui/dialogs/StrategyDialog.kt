package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreativityLevel
import com.example.data.model.ProjectSessionEntity
import com.example.ui.theme.OptiPurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyDialog(
    session: ProjectSessionEntity?,
    onSetCreativity: (CreativityLevel) -> Unit,
    onToggleMixPrompts: () -> Unit,
    onTogglePromoteUpvoted: () -> Unit,
    onToggleFreshOnly: () -> Unit,
    onTogglePrioritizeSteered: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val creativity = session?.let { CreativityLevel.fromLevel(it.creativity) } ?: CreativityLevel.BALANCED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("strategy_sheet")
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
                    Text("⚗️ Improvement Strategy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("How the AI evolves your prompts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_strategy_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))

            // 1. Creativity Level
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🎨 Creativity Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CreativityLevel.entries.forEach { level ->
                        FilterChip(
                            selected = creativity == level,
                            onClick = { onSetCreativity(level) },
                            label = { Text(level.title, fontSize = 12.sp) },
                            modifier = Modifier.testTag("creativity_${level.name.lowercase()}")
                        )
                    }
                }
                Text(
                    creativity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Mix in likes from earlier instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = session?.allowMix == true,
                    onCheckedChange = { onToggleMixPrompts() },
                    modifier = Modifier.testTag("mix_prompts_checkbox")
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("🔀 Mix in likes from earlier instructions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "The AI also learns from your likes under all earlier instructions, not just the current one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // 3. Promote upvoted prompt as instruction
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = session?.promoteUpvoted == true,
                    onCheckedChange = { onTogglePromoteUpvoted() },
                    modifier = Modifier.testTag("promote_upvoted_checkbox")
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("🎯 Sometimes use a top-liked prompt as instruction", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "About 3 batches in 5 are written from a liked prompt instead of your instruction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // 4. Fresh prompts only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = session?.freshOnly == true,
                    onCheckedChange = { onToggleFreshOnly() },
                    modifier = Modifier.testTag("fresh_only_checkbox")
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("⏳ Fresh prompts only", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "Each image uses a new prompt written for the current instruction — no stale or reused ones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // 5. Prioritize steered prompts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = session?.prioritizeSteered == true,
                    onCheckedChange = { onTogglePrioritizeSteered() },
                    modifier = Modifier.testTag("prioritize_steered_checkbox")
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text("🧲 Send steered prompts first", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "Sends steer-born prompts to image generation ahead of normal queue order.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
