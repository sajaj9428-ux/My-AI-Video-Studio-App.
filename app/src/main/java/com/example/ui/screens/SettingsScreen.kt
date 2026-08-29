package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SelectableChip
import com.example.ui.components.StudioCard
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun SettingsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.userSettings
    val projects by viewModel.allProjects.collectAsState()
    val favorites by viewModel.allFavorites.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

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
                    text = "⚙️ Studio Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize defaults and manage local studio data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Default Language
        item {
            StudioCard {
                Text(
                    text = "Default Language (डिफ़ॉल्ट भाषा)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Hindi", "English", "Urdu").forEach { lang ->
                        SelectableChip(
                            selected = settings.defaultLanguage == lang,
                            label = lang,
                            onClick = {
                                viewModel.updateSettings { it.copy(defaultLanguage = lang) }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_lang_$lang"
                        )
                    }
                }
            }
        }

        // Default Video Duration
        item {
            StudioCard {
                Text(
                    text = "Default Target Duration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("30 seconds", "60 seconds", "90 seconds").forEach { dur ->
                        SelectableChip(
                            selected = settings.defaultDuration == dur,
                            label = dur,
                            onClick = {
                                viewModel.updateSettings { it.copy(defaultDuration = dur) }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_dur_${dur.replace(" ", "_")}"
                        )
                    }
                }
            }
        }

        // Default Number of Scenes
        item {
            StudioCard {
                Text(
                    text = "Default Number of Scenes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 5, 8, 10, 12).forEach { count ->
                        SelectableChip(
                            selected = settings.defaultNumScenes == count,
                            label = "$count Scenes",
                            onClick = {
                                viewModel.updateSettings { it.copy(defaultNumScenes = count) }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_scenes_$count"
                        )
                    }
                }
            }
        }

        // Default Visual Style
        item {
            StudioCard {
                Text(
                    text = "Default Visual Style",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val styles = listOf("Realistic", "Cinematic", "3D Animation", "Cartoon")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    styles.forEach { style ->
                        SelectableChip(
                            selected = settings.defaultStyle == style,
                            label = style,
                            onClick = {
                                viewModel.updateSettings { it.copy(defaultStyle = style) }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_style_${style.replace(" ", "_")}"
                        )
                    }
                }
            }
        }

        // Auto Save Switch
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Save Changes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatically save prompt edits directly to local device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isAutoSave,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(isAutoSave = checked) }
                        },
                        modifier = Modifier.testTag("settings_autosave_switch")
                    )
                }
            }
        }

        // Storage & Local Database Info
        item {
            StudioCard {
                Text(
                    text = "💾 Local Storage & Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Projects Saved:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${projects.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Favorites Bookmarked:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${favorites.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("clear_all_data_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Local Data", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // About Studio
        item {
            StudioCard {
                Text(
                    text = "🎬 My AI Video Studio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Personal offline-first mobile AI video creation suite. Designed for solo creators to architect AI stories, scenes, character-consistent image prompts, Veo video prompts, and YouTube Shorts packages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will remove all saved projects, scenes, and favorites from your device. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
