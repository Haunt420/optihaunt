package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OptiAntiRed
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiManualBlue
import com.example.ui.theme.OptiPinkAccent
import com.example.ui.theme.OptiPositiveGreen
import com.example.ui.theme.OptiPurplePrimary
import com.example.ui.theme.OptiSteerAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("help_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("📖 OptiHaunt Help Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("How continuous AI prompt evolution works", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_help_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Core loop
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🌀 The Continuous Evolution Loop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "1. Enter what you want to see in the composer and tap Start ▶.\n" +
                                        "2. An AI writes a batch of prompts into the Upcoming Queue.\n" +
                                        "3. Prompts serve a 10s cooldown where you can inspect, edit, or delete them.\n" +
                                        "4. Generated images appear in your feed. Like the ones you love, mark influences, and steer batches!\n" +
                                        "5. Revise your instruction with AI Help to push the stream toward your artistic taste.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Feedback Types
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎯 Distinct Feedback Meanings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "OptiHaunt separates 4 distinct types of user feedback:",
                                fontSize = 12.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("❤️", fontSize = 14.sp)
                                    Column {
                                        Text("Image Upvote / Like", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = OptiPinkAccent)
                                        Text("Updates prompt score (votes / used). Guided prompt selection & optional instruction promotion.", fontSize = 11.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🔖", fontSize = 14.sp)
                                    Column {
                                        Text("Positive Influence Mark", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = OptiPositiveGreen)
                                        Text("Pending positive example for the NEXT instruction revision. Does NOT directly change prompt score.", fontSize = 11.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🚫", fontSize = 14.sp)
                                    Column {
                                        Text("Anti-Influence Mark", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = OptiAntiRed)
                                        Text("Pending negative example for the NEXT instruction revision. Tells AI what to avoid.", fontSize = 11.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🧲", fontSize = 14.sp)
                                    Column {
                                        Text("Theme Steer", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = OptiSteerAmber)
                                        Text("Queued theme consumed by the NEXT prompt-writing batch. Blends heavily into the new prompts.", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Queue rules & cooldown
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏳ Prompt Queue & Priority Rules", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "• 10-Second Cooldown: Allows you to read or edit upcoming prompts before image generation starts.\n" +
                                        "• Editing Pause: Clicking any prompt freezes all timers and pauses generation until you tap 'Done'.\n" +
                                        "• ✏️ Hand-written Prompts: Any prompt you type or inject manually gets drawn FIRST, ahead of AI prompts.\n" +
                                        "• 📏 Batch Rules: Supports {option a|option b} random rolls per brainstorm batch.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
