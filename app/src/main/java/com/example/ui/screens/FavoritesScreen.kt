package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FavoriteItemEntity
import com.example.ui.components.CopyButton
import com.example.ui.components.CopyButtonVariant
import com.example.ui.components.SelectableChip
import com.example.ui.components.StudioCard
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FavoritesScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.allFavorites.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(uiState.favoritesFilter) }

    val filteredFavorites = remember(favorites, selectedFilter) {
        if (selectedFilter == "All") favorites
        else {
            val typeKey = when (selectedFilter) {
                "Stories" -> "STORY"
                "Image Prompts" -> "IMAGE_PROMPT"
                "Video Prompts" -> "VIDEO_PROMPT"
                "Voice Scripts" -> "VOICE_SCRIPT"
                "YouTube Packages" -> "YOUTUBE_PACKAGE"
                else -> ""
            }
            favorites.filter { it.type == typeKey }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "⭐ Favorites & Saved Prompts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${favorites.size} Saved Items (Stories, Image Prompts & Video Scripts)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Filter Chips
        item {
            val categories = listOf("All", "Stories", "Image Prompts", "Video Prompts", "Voice Scripts", "YouTube Packages")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    SelectableChip(
                        selected = selectedFilter == cat,
                        label = cat,
                        onClick = {
                            selectedFilter = cat
                            viewModel.setFavoritesFilter(cat)
                        },
                        testTag = "fav_filter_$cat"
                    )
                }
            }
        }

        // Empty state
        if (filteredFavorites.isEmpty()) {
            item {
                StudioCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Saved Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the ⭐ star icon next to any generated story, image prompt, or voice script to bookmark it here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredFavorites, key = { it.id }) { fav ->
                FavoriteCardItem(
                    favorite = fav,
                    onDelete = { viewModel.deleteFavorite(fav.id) },
                    onCopied = { viewModel.showSnackbar("Copied successfully ✓") }
                )
            }
        }
    }
}

@Composable
private fun FavoriteCardItem(
    favorite: FavoriteItemEntity,
    onDelete: () -> Unit,
    onCopied: () -> Unit
) {
    val dateStr = remember(favorite.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(favorite.createdAt))
    }

    val typeLabel = when (favorite.type) {
        "STORY" -> "📖 Story"
        "IMAGE_PROMPT" -> "🖼️ Image Prompt"
        "VIDEO_PROMPT" -> "🎥 Video Prompt"
        "VOICE_SCRIPT" -> "🗣️ Voice Script"
        "YOUTUBE_PACKAGE" -> "📺 YouTube Pkg"
        else -> "⭐ Item"
    }

    StudioCard(
        modifier = Modifier.testTag("favorite_card_${favorite.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (favorite.metaInfo.isNotBlank()) {
                    Text(
                        text = favorite.metaInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Favorite",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content preview
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = favorite.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Copy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            CopyButton(
                textToCopy = favorite.content,
                onCopied = onCopied,
                variant = CopyButtonVariant.Filled,
                label = "Copy Item",
                testTag = "copy_fav_btn_${favorite.id}"
            )
        }
    }
}
