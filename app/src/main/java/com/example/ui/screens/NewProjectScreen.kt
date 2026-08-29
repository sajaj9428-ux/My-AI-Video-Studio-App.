package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SelectableChip
import com.example.ui.components.StudioCard
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun NewProjectScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings = viewModel.uiState.collectAsState().value.userSettings

    var projectName by remember { mutableStateOf("") }
    var storyIdea by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(userSettings.defaultLanguage) }
    var videoType by remember { mutableStateOf("YouTube Short") }
    var targetDuration by remember { mutableStateOf(userSettings.defaultDuration) }
    var customDuration by remember { mutableStateOf("") }
    var numScenes by remember { mutableStateOf(userSettings.defaultNumScenes) }
    var isCustomScenes by remember { mutableStateOf(false) }
    var customScenesText by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(userSettings.defaultStyle) }
    var customStyle by remember { mutableStateOf("") }

    val isGenerating = viewModel.uiState.collectAsState().value.isGenerating
    val generatingTask = viewModel.uiState.collectAsState().value.generatingTask

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                    modifier = Modifier.testTag("back_to_home_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Create New Project",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure parameters & generate your AI story",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Project Name & Story Idea Card
        item {
            StudioCard {
                Text(
                    text = "Project Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g., The Lost Temple of Himalaya") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storyIdea,
                    onValueChange = { storyIdea = it },
                    label = { Text("Story Idea / Concept") },
                    placeholder = {
                        Text(
                            if (language == "Hindi") "उदा: एक गरीब लड़का जिसे जंगल में जादुई कैमरा मिलता है..."
                            else "e.g., An archaeologist discovers a futuristic portal in an ancient desert ruin..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_idea_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )
            }
        }

        // Language Selection
        item {
            StudioCard {
                Text(
                    text = "Language (भाषा)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Hindi", "English", "Urdu").forEach { lang ->
                        SelectableChip(
                            selected = language == lang,
                            label = lang,
                            onClick = { language = lang },
                            modifier = Modifier.weight(1f),
                            testTag = "lang_chip_$lang"
                        )
                    }
                }
            }
        }

        // Video Type Selection
        item {
            StudioCard {
                Text(
                    text = "Video Type / Format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val videoTypes = listOf(
                    "YouTube Short" to Icons.Default.Subscriptions,
                    "Instagram Reel" to Icons.Default.CameraAlt,
                    "Normal Video" to Icons.Default.Tv
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    videoTypes.forEach { (type, icon) ->
                        SelectableChip(
                            selected = videoType == type,
                            label = type,
                            onClick = { videoType = type },
                            modifier = Modifier.weight(1f),
                            icon = icon,
                            testTag = "video_type_chip_${type.replace(" ", "_")}"
                        )
                    }
                }
            }
        }

        // Target Duration
        item {
            StudioCard {
                Text(
                    text = "Target Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("30 seconds", "60 seconds", "90 seconds", "Custom").forEach { dur ->
                        val isSelected = targetDuration == dur
                        SelectableChip(
                            selected = isSelected,
                            label = if (dur == "Custom") "Custom" else dur.replace(" seconds", "s"),
                            onClick = { targetDuration = dur },
                            modifier = Modifier.weight(1f),
                            testTag = "duration_chip_${dur.replace(" ", "_")}"
                        )
                    }
                }

                AnimatedVisibility(visible = targetDuration == "Custom") {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = customDuration,
                            onValueChange = { customDuration = it },
                            label = { Text("Custom Duration (e.g. 45s, 2m)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_duration_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Number of Scenes
        item {
            StudioCard {
                Text(
                    text = "Number of Scenes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 5, 8, 10, 12).forEach { count ->
                        SelectableChip(
                            selected = !isCustomScenes && numScenes == count,
                            label = "$count",
                            onClick = {
                                isCustomScenes = false
                                numScenes = count
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "scenes_chip_$count"
                        )
                    }
                    SelectableChip(
                        selected = isCustomScenes,
                        label = "Custom",
                        onClick = { isCustomScenes = true },
                        modifier = Modifier.weight(1.3f),
                        testTag = "scenes_chip_custom"
                    )
                }

                AnimatedVisibility(visible = isCustomScenes) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = customScenesText,
                            onValueChange = {
                                customScenesText = it
                                it.toIntOrNull()?.let { num -> if (num in 1..30) numScenes = num }
                            },
                            label = { Text("Enter Number of Scenes (1 - 30)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_scenes_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Visual Style
        item {
            StudioCard {
                Text(
                    text = "Visual Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val styles = listOf("Realistic", "Cinematic", "3D Animation", "Cartoon", "Custom")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.take(3).forEach { s ->
                            SelectableChip(
                                selected = style == s,
                                label = s,
                                onClick = { style = s },
                                modifier = Modifier.weight(1f),
                                testTag = "style_chip_${s.replace(" ", "_")}"
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.drop(3).forEach { s ->
                            SelectableChip(
                                selected = style == s,
                                label = s,
                                onClick = { style = s },
                                modifier = Modifier.weight(1f),
                                testTag = "style_chip_${s.replace(" ", "_")}"
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = style == "Custom") {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = customStyle,
                            onValueChange = { customStyle = it },
                            label = { Text("Enter Custom Visual Style (e.g. Cyberpunk Anime)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_style_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Action Buttons: Generate Story / Save Project
        item {
            val effectiveDuration = if (targetDuration == "Custom" && customDuration.isNotBlank()) customDuration else targetDuration
            val effectiveStyle = if (style == "Custom" && customStyle.isNotBlank()) customStyle else style
            val effectiveScenes = if (isCustomScenes) (customScenesText.toIntOrNull() ?: numScenes) else numScenes

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.startNewProject(
                            name = projectName,
                            storyIdea = storyIdea,
                            language = language,
                            videoType = videoType,
                            targetDuration = effectiveDuration,
                            numScenes = effectiveScenes,
                            style = effectiveStyle,
                            autoGenerateStory = true
                        )
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("generate_story_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(generatingTask.ifBlank { "Generating..." }, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨ Generate Story", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.startNewProject(
                            name = projectName,
                            storyIdea = storyIdea,
                            language = language,
                            videoType = videoType,
                            targetDuration = effectiveDuration,
                            numScenes = effectiveScenes,
                            style = effectiveStyle,
                            autoGenerateStory = false
                        )
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_project_draft_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Project Draft & Open Editor", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
