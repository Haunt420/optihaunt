package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.GeneratedImageEntity
import com.example.data.model.Resolution
import com.example.ui.theme.OptiAntiRed
import com.example.ui.theme.OptiCyanAccent
import com.example.ui.theme.OptiManualBlue
import com.example.ui.theme.OptiPinkAccent
import com.example.ui.theme.OptiPositiveGreen
import com.example.ui.theme.OptiSteerAmber

@Composable
fun ImageTileCard(
    image: GeneratedImageEntity,
    isMarkedPositive: Boolean,
    isMarkedAnti: Boolean,
    onToggleUpvote: () -> Unit,
    onToggleSave: () -> Unit,
    onTogglePositiveInfluence: () -> Unit,
    onToggleAntiInfluence: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    val resolution = Resolution.fromId(image.resolution)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("image_card_${image.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(resolution.aspectRatio)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = image.resolvedText,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OptiCyanAccent, modifier = Modifier.size(36.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF201B2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎨 Image Preview", color = OptiCyanAccent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(resolution.aspectRatio)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            val extraColors = com.example.ui.theme.LocalExtraColors.current

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Version badge
                Surface(
                    color = (if (extraColors.isOled) extraColors.hunterGreen else Color.Black).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "v${image.versionNumber}",
                        color = if (extraColors.isOled) extraColors.chromeAccent else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Steer / Origin badge
                if (image.influenced) {
                    Surface(
                        color = extraColors.goldFleck.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "🧲 Steered",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Gradient Overlay for Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Group: Like & Save
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Upvote / Like Button
                        IconButton(
                            onClick = onToggleUpvote,
                            modifier = Modifier.testTag("like_button_${image.id}")
                        ) {
                            Icon(
                                imageVector = if (image.isUpvoted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (image.isUpvoted) "Unlike" else "Like",
                                tint = if (image.isUpvoted) (if (extraColors.isOled) extraColors.copperAccent else OptiPinkAccent) else Color.White
                            )
                        }

                        // Save Button
                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier.testTag("save_button_${image.id}")
                        ) {
                            Icon(
                                imageVector = if (image.isSaved) Icons.Filled.Collections else Icons.Outlined.Collections,
                                contentDescription = if (image.isSaved) "Unsave" else "Save Image",
                                tint = if (image.isSaved) (if (extraColors.isOled) extraColors.chromeAccent else OptiCyanAccent) else Color.White
                            )
                        }
                    }

                    // Right Group: 🔖 Positive mark, 🚫 Anti mark, ❓ Details
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 🔖 Positive Influence mark
                        IconButton(
                            onClick = onTogglePositiveInfluence,
                            modifier = Modifier.testTag("positive_mark_${image.id}")
                        ) {
                            Icon(
                                imageVector = if (isMarkedPositive) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Mark Positive Influence",
                                tint = if (isMarkedPositive) OptiPositiveGreen else Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // 🚫 Anti-Influence mark
                        IconButton(
                            onClick = onToggleAntiInfluence,
                            modifier = Modifier.testTag("anti_mark_${image.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Mark Anti-Influence",
                                tint = if (isMarkedAnti) OptiAntiRed else Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // ❓ Details Dialog
                        IconButton(
                            onClick = { showDetailsDialog = true },
                            modifier = Modifier.testTag("details_button_${image.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Details and Provenance",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Image Details & Lineage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Prompt Text:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(image.resolvedText, fontSize = 13.sp)

                    if (!image.influenceText.isNullOrBlank()) {
                        Text("🧲 Steer Input:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(image.influenceText, fontSize = 13.sp, color = OptiSteerAmber)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seed: ${image.seed}", fontSize = 12.sp)
                        Text("Version: v${image.versionNumber}", fontSize = 12.sp)
                    }

                    Text("Resolution: ${resolution.label} (${resolution.width}×${resolution.height})", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
