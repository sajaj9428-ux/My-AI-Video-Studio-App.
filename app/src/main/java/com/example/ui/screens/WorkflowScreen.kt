package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.data.model.SceneEntity
import com.example.ui.components.*
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun WorkflowScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.activeProject

    if (project == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active project loaded")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.navigateTo(ScreenDestination.HOME) }) {
                    Text("Go to Home")
                }
            }
        }
        return
    }

    val currentStep = project.currentWorkflowStep
    val isGenerating = uiState.isGenerating
    val generatingTask = uiState.generatingTask
    val errorMessage = uiState.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Workflow Top Bar
        WorkflowTopBar(
            project = project,
            onBackClick = { viewModel.navigateTo(ScreenDestination.PROJECTS) },
            onSaveClick = { viewModel.saveActiveProject() },
            onFavoriteToggle = { viewModel.toggleProjectFavorite(project.id, !project.isFavorite) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Stepper Indicator (Story → Scenes → Images → Video → Voice → YouTube)
        WorkflowStepper(
            currentStep = currentStep,
            onStepSelected = { step -> viewModel.setWorkflowStep(step) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Loading Indicator Overlay / Bar
        if (isGenerating) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = generatingTask.ifBlank { "Processing with AI..." },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Error Banner
        if (errorMessage != null) {
            ErrorCard(
                message = errorMessage,
                onRetry = { uiState.lastFailedAction?.invoke() ?: viewModel.clearError() },
                onBack = { viewModel.clearError() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Active Step Content
        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                0 -> StoryMakerStep(viewModel = viewModel, project = project)
                1 -> SceneGeneratorStep(viewModel = viewModel, project = project, scenes = uiState.activeScenes)
                2 -> ImagePromptStep(viewModel = viewModel, project = project, scenes = uiState.activeScenes, selectedIndex = uiState.selectedSceneIndex)
                3 -> VideoPromptStep(viewModel = viewModel, project = project, scenes = uiState.activeScenes, selectedIndex = uiState.selectedSceneIndex, mode = uiState.activeVideoPromptMode)
                4 -> VoiceOverStep(viewModel = viewModel, project = project, scenes = uiState.activeScenes, activeStyle = uiState.activeVoiceStyle)
                5 -> YouTubePackageStep(viewModel = viewModel, project = project, scenes = uiState.activeScenes)
            }
        }
    }
}

@Composable
private fun WorkflowTopBar(
    project: ProjectEntity,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("workflow_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = project.name.ifBlank { "Untitled AI Project" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.language} • ${project.videoType} • ${project.style}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("project_fav_toggle")
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (project.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSaveClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("workflow_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Project",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 1: STORY MAKER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun StoryMakerStep(
    viewModel: StudioViewModel,
    project: ProjectEntity
) {
    var idea by remember(project.id) { mutableStateOf(project.storyIdea) }
    var characters by remember(project.id) { mutableStateOf(project.characters) }
    var location by remember(project.id) { mutableStateOf(project.location) }
    var mood by remember(project.id) { mutableStateOf(project.mood) }
    var ending by remember(project.id) { mutableStateOf(project.ending) }
    var instructions by remember(project.id) { mutableStateOf(project.specialInstructions) }
    var storyText by remember(project.id, project.storyText) { mutableStateOf(project.storyText) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudioCard {
                Text(
                    text = "📖 Story Setup & Creative Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = idea,
                    onValueChange = {
                        idea = it
                        viewModel.updateActiveProjectDetails { p -> p.copy(storyIdea = it) }
                    },
                    label = { Text("Story Idea / Concept") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_idea_field"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = characters,
                        onValueChange = {
                            characters = it
                            viewModel.updateActiveProjectDetails { p -> p.copy(characters = it) }
                        },
                        label = { Text("Characters") },
                        placeholder = { Text("e.g., Kabir & Maya") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("characters_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            viewModel.updateActiveProjectDetails { p -> p.copy(location = it) }
                        },
                        label = { Text("Location") },
                        placeholder = { Text("e.g., Old Varanasi Ghats") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = mood,
                        onValueChange = {
                            mood = it
                            viewModel.updateActiveProjectDetails { p -> p.copy(mood = it) }
                        },
                        label = { Text("Mood / Tone") },
                        placeholder = { Text("Dramatic / Thriller") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mood_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ending,
                        onValueChange = {
                            ending = it
                            viewModel.updateActiveProjectDetails { p -> p.copy(ending = it) }
                        },
                        label = { Text("Ending") },
                        placeholder = { Text("Twist / Inspiring") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ending_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = {
                        instructions = it
                        viewModel.updateActiveProjectDetails { p -> p.copy(specialInstructions = it) }
                    },
                    label = { Text("Special Instructions (Optional)") },
                    placeholder = { Text("e.g. Include a suspenseful cliffhanger hook") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instructions_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Story Action Buttons
        item {
            StudioCard {
                Text(
                    text = "AI Story Controls",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateOrModifyStory("GENERATE") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("story_generate_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("✨ Generate", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.generateOrModifyStory("REGENERATE") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("story_regenerate_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔄 Regenerate")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.generateOrModifyStory("IMPROVE") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("story_improve_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("💡 Improve", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateOrModifyStory("SHORTEN") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("story_shorten_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("✂️ Shorten", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.generateOrModifyStory("EXPAND") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("story_expand_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔍 Expand", fontSize = 12.sp)
                    }
                }
            }
        }

        // Editable Generated Story Area
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Generated Story Script",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CopyButton(
                            textToCopy = storyText,
                            onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                            variant = CopyButtonVariant.Outlined,
                            testTag = "copy_story_button"
                        )
                        IconButton(
                            onClick = {
                                viewModel.saveToFavorites(
                                    type = "STORY",
                                    title = project.name.ifBlank { "Story Script" },
                                    content = storyText,
                                    metaInfo = "${project.language} • ${project.style}"
                                )
                            },
                            modifier = Modifier.testTag("favorite_story_button")
                        ) {
                            Icon(imageVector = Icons.Default.StarBorder, contentDescription = "Favorite")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = storyText,
                    onValueChange = {
                        storyText = it
                        viewModel.updateActiveProjectDetails { p -> p.copy(storyText = it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_editor_textarea"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 8,
                    placeholder = { Text("Your generated or custom story will appear here. You can manually edit any part...") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Convert Story Into Scenes Button
                Button(
                    onClick = { viewModel.convertStoryToScenes() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("convert_story_to_scenes_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.MovieCreation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎬 Convert Story Into Scenes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 2: SCENE GENERATOR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SceneGeneratorStep(
    viewModel: StudioViewModel,
    project: ProjectEntity,
    scenes: List<SceneEntity>
) {
    var editingScene by remember { mutableStateOf<SceneEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Global Actions Bar
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎬 Project Scenes (${scenes.size}/${project.numScenes})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target Duration: ${project.targetDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.generateAllScenePrompts() },
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("generate_all_scene_prompts_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate All Prompts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Empty state
        if (scenes.isEmpty()) {
            item {
                StudioCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Scenes Generated Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Convert your story into structured cinematic scenes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.convertStoryToScenes() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Generate Scenes Now")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(scenes, key = { index, scene -> if (scene.id > 0) scene.id else index.toLong() }) { index, scene ->
                SceneItemCard(
                    scene = scene,
                    onEdit = { editingScene = scene },
                    onRegenerate = { viewModel.regenerateScene(index) },
                    onCopy = {
                        val text = """
                            Scene ${scene.sceneNumber}: ${scene.sceneTitle} (${scene.duration})
                            Description: ${scene.sceneDescription}
                            Characters: ${scene.characters}
                            Location: ${scene.location}
                            Action: ${scene.characterActions}
                            Emotion: ${scene.emotion}
                            Camera Shot: ${scene.cameraShot}
                            Lighting: ${scene.lighting}
                            Background: ${scene.background}
                            Dialogue: "${scene.dialogue}"
                            Voice-Over: "${scene.voiceOver}"
                        """.trimIndent()
                        viewModel.showSnackbar("Copied successfully ✓")
                    },
                    onGoToImagePrompt = {
                        viewModel.setSelectedSceneIndex(index)
                        viewModel.setWorkflowStep(2)
                    },
                    onGoToVideoPrompt = {
                        viewModel.setSelectedSceneIndex(index)
                        viewModel.setWorkflowStep(3)
                    },
                    onGoToVoiceOver = {
                        viewModel.setSelectedSceneIndex(index)
                        viewModel.setWorkflowStep(4)
                    }
                )
            }
        }
    }

    // Edit Scene Dialog
    if (editingScene != null) {
        EditSceneDialog(
            scene = editingScene!!,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                viewModel.updateScene(updated)
                editingScene = null
                viewModel.showSnackbar("Scene updated ✓")
            }
        )
    }
}

@Composable
private fun SceneItemCard(
    scene: SceneEntity,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    onGoToImagePrompt: () -> Unit,
    onGoToVideoPrompt: () -> Unit,
    onGoToVoiceOver: () -> Unit
) {
    StudioCard(
        modifier = Modifier.testTag("scene_card_${scene.sceneNumber}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${scene.sceneNumber}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = scene.sceneTitle.ifBlank { "Scene ${scene.sceneNumber}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Duration: ${scene.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Scene", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRegenerate, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate Scene", modifier = Modifier.size(18.dp))
                }
                val sceneFullText = """
                    Scene ${scene.sceneNumber}: ${scene.sceneTitle}
                    Description: ${scene.sceneDescription}
                    Characters: ${scene.characters}
                    Location: ${scene.location}
                    Action: ${scene.characterActions}
                    Emotion: ${scene.emotion}
                    Camera Shot: ${scene.cameraShot}
                    Lighting: ${scene.lighting}
                    Background: ${scene.background}
                    Dialogue: ${scene.dialogue}
                    Voice-Over: ${scene.voiceOver}
                """.trimIndent()
                CopyButton(
                    textToCopy = sceneFullText,
                    onCopied = onCopy,
                    variant = CopyButtonVariant.IconOnly,
                    testTag = "copy_scene_${scene.sceneNumber}"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (scene.sceneDescription.isNotBlank()) {
            Text(
                text = scene.sceneDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Details grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SceneDetailRow("🎭 Characters", scene.characters)
            SceneDetailRow("📍 Location", scene.location)
            SceneDetailRow("⚡ Action", scene.characterActions)
            SceneDetailRow("💭 Emotion", scene.emotion)
            SceneDetailRow("🎥 Camera Shot", scene.cameraShot)
            SceneDetailRow("💡 Lighting", scene.lighting)
            SceneDetailRow("🌄 Background", scene.background)
            if (scene.dialogue.isNotBlank()) {
                SceneDetailRow("💬 Dialogue", "\"${scene.dialogue}\"")
            }
            if (scene.voiceOver.isNotBlank()) {
                SceneDetailRow("🗣️ Voice-Over", "\"${scene.voiceOver}\"")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Navigation Buttons to subsequent steps for this specific scene
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onGoToImagePrompt,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .testTag("scene_to_image_${scene.sceneNumber}"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("🖼️ Image Prompt", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onGoToVideoPrompt,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .testTag("scene_to_video_${scene.sceneNumber}"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("🎥 Video Prompt", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onGoToVoiceOver,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .testTag("scene_to_voice_${scene.sceneNumber}"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("🗣️ Voice Over", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SceneDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EditSceneDialog(
    scene: SceneEntity,
    onDismiss: () -> Unit,
    onSave: (SceneEntity) -> Unit
) {
    var title by remember { mutableStateOf(scene.sceneTitle) }
    var description by remember { mutableStateOf(scene.sceneDescription) }
    var characters by remember { mutableStateOf(scene.characters) }
    var location by remember { mutableStateOf(scene.location) }
    var actions by remember { mutableStateOf(scene.characterActions) }
    var emotion by remember { mutableStateOf(scene.emotion) }
    var cameraShot by remember { mutableStateOf(scene.cameraShot) }
    var lighting by remember { mutableStateOf(scene.lighting) }
    var background by remember { mutableStateOf(scene.background) }
    var dialogue by remember { mutableStateOf(scene.dialogue) }
    var voiceOver by remember { mutableStateOf(scene.voiceOver) }
    var duration by remember { mutableStateOf(scene.duration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scene ${scene.sceneNumber}", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Scene Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Scene Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = characters,
                        onValueChange = { characters = it },
                        label = { Text("Characters") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = actions,
                        onValueChange = { actions = it },
                        label = { Text("Character Actions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = emotion,
                        onValueChange = { emotion = it },
                        label = { Text("Emotion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = cameraShot,
                        onValueChange = { cameraShot = it },
                        label = { Text("Camera Shot") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = lighting,
                        onValueChange = { lighting = it },
                        label = { Text("Lighting") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = background,
                        onValueChange = { background = it },
                        label = { Text("Background") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = dialogue,
                        onValueChange = { dialogue = it },
                        label = { Text("Dialogue") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = voiceOver,
                        onValueChange = { voiceOver = it },
                        label = { Text("Voice-Over") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (e.g. 10s)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        scene.copy(
                            sceneTitle = title,
                            sceneDescription = description,
                            characters = characters,
                            location = location,
                            characterActions = actions,
                            emotion = emotion,
                            cameraShot = cameraShot,
                            lighting = lighting,
                            background = background,
                            dialogue = dialogue,
                            voiceOver = voiceOver,
                            duration = duration
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 3: IMAGE PROMPT GENERATOR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ImagePromptStep(
    viewModel: StudioViewModel,
    project: ProjectEntity,
    scenes: List<SceneEntity>,
    selectedIndex: Int
) {
    val activeScene = scenes.getOrNull(selectedIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Character Reference Section (Reusable character definition)
        item {
            CharacterReferenceCard(
                characterRef = project.characterRef,
                onCharacterRefChange = {
                    viewModel.updateActiveProjectDetails { p -> p.copy(characterRef = it) }
                }
            )
        }

        // Scene Selector Tabs
        item {
            Text(
                text = "Select Scene to View / Generate Prompt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(scenes) { index, sc ->
                    SelectableChip(
                        selected = selectedIndex == index,
                        label = "Scene ${sc.sceneNumber}",
                        onClick = { viewModel.setSelectedSceneIndex(index) },
                        testTag = "image_scene_tab_${sc.sceneNumber}"
                    )
                }
            }
        }

        if (activeScene == null) {
            item {
                StudioCard {
                    Text("Please generate scenes in Step 2 before generating image prompts.")
                }
            }
        } else {
            // Scene Details Summary
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Scene ${activeScene.sceneNumber}: ${activeScene.sceneTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Style: ${project.style} • Characters: ${activeScene.characters}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateImagePromptForCurrentScene() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("generate_image_prompt_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Prompt", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateImagePromptForCurrentScene() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("regenerate_image_prompt_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Regenerate")
                        }
                    }
                }
            }

            // Image Prompt Display & Editing
            item {
                var promptText by remember(activeScene.id, activeScene.imagePrompt) {
                    mutableStateOf(activeScene.imagePrompt)
                }

                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detailed AI Image Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CopyButton(
                                textToCopy = promptText,
                                onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                                variant = CopyButtonVariant.Outlined,
                                label = "Copy Prompt",
                                testTag = "copy_image_prompt_button"
                            )

                            IconButton(
                                onClick = {
                                    viewModel.saveToFavorites(
                                        type = "IMAGE_PROMPT",
                                        title = "Scene ${activeScene.sceneNumber} - ${activeScene.sceneTitle}",
                                        content = promptText,
                                        metaInfo = "${project.name} • ${project.style}"
                                    )
                                },
                                modifier = Modifier.testTag("save_image_prompt_favorite")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StarBorder,
                                    contentDescription = "Save to Favorites",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = {
                            promptText = it
                            viewModel.updateScene(activeScene.copy(imagePrompt = it))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_textarea"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 8,
                        placeholder = {
                            Text("Tap 'Generate Prompt' to create a comprehensive cinematic prompt with camera, lighting, and character details.")
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tip box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ready to paste directly into Midjourney, FLUX, Imagen, or Ideogram.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 4: VIDEO PROMPT GENERATOR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun VideoPromptStep(
    viewModel: StudioViewModel,
    project: ProjectEntity,
    scenes: List<SceneEntity>,
    selectedIndex: Int,
    mode: String
) {
    val activeScene = scenes.getOrNull(selectedIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scene Selector Tabs
        item {
            Text(
                text = "Select Scene for AI Video Prompt (Veo / AI Video)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(scenes) { index, sc ->
                    SelectableChip(
                        selected = selectedIndex == index,
                        label = "Scene ${sc.sceneNumber}",
                        onClick = { viewModel.setSelectedSceneIndex(index) },
                        testTag = "video_scene_tab_${sc.sceneNumber}"
                    )
                }
            }
        }

        if (activeScene == null) {
            item {
                StudioCard {
                    Text("Please generate scenes in Step 2 before generating video prompts.")
                }
            }
        } else {
            // Mode Toggle (Text Prompt vs JSON Prompt)
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prompt Format",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SelectableChip(
                                selected = mode == "TEXT",
                                label = "Directorial Text",
                                onClick = { viewModel.setVideoPromptMode("TEXT") },
                                testTag = "mode_text_chip"
                            )
                            SelectableChip(
                                selected = mode == "JSON",
                                label = "{ } JSON Prompt",
                                onClick = { viewModel.setVideoPromptMode("JSON") },
                                testTag = "mode_json_chip"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateVideoPromptForCurrentScene() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("generate_video_prompt_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Video Prompt", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.generateVideoPromptForCurrentScene() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("regenerate_video_prompt_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Regenerate")
                        }
                    }
                }
            }

            // Video Prompt Display
            item {
                val currentContent = if (mode == "JSON") activeScene.videoPromptJson else activeScene.videoPrompt
                var textValue by remember(activeScene.id, mode, currentContent) {
                    mutableStateOf(currentContent)
                }

                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == "JSON") "Valid JSON Video Prompt" else "Cinematic Video Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CopyButton(
                                textToCopy = textValue,
                                onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                                variant = CopyButtonVariant.Outlined,
                                label = "Copy",
                                testTag = "copy_video_prompt_button"
                            )

                            IconButton(
                                onClick = {
                                    viewModel.saveToFavorites(
                                        type = "VIDEO_PROMPT",
                                        title = "Scene ${activeScene.sceneNumber} (${if (mode == "JSON") "JSON" else "Text"})",
                                        content = textValue,
                                        metaInfo = "${project.name} • ${project.style}"
                                    )
                                },
                                modifier = Modifier.testTag("save_video_prompt_favorite")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StarBorder,
                                    contentDescription = "Save to Favorites",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            if (mode == "JSON") {
                                viewModel.updateScene(activeScene.copy(videoPromptJson = it))
                            } else {
                                viewModel.updateScene(activeScene.copy(videoPrompt = it))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_textarea"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 10,
                        textStyle = if (mode == "JSON") {
                            LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        } else LocalTextStyle.current,
                        placeholder = {
                            Text("Tap 'Generate Video Prompt' to create AI video instructions for Veo, Runway, and Luma.")
                        }
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 5: VOICE OVER SCRIPT
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun VoiceOverStep(
    viewModel: StudioViewModel,
    project: ProjectEntity,
    scenes: List<SceneEntity>,
    activeStyle: String
) {
    val sampleVoiceScript = scenes.firstOrNull()?.voiceScript ?: ""
    var voiceScriptText by remember(scenes, sampleVoiceScript) {
        mutableStateOf(sampleVoiceScript)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Voice Style Selector
        item {
            StudioCard {
                Text(
                    text = "Voice Over Style & Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Language: ${project.language}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val voiceStyles = listOf("Emotional", "Storytelling", "Energetic", "Calm", "Cinematic")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    voiceStyles.forEach { vStyle ->
                        SelectableChip(
                            selected = activeStyle == vStyle,
                            label = vStyle,
                            onClick = { viewModel.setVoiceStyle(vStyle) },
                            modifier = Modifier.weight(1f),
                            testTag = "voice_style_chip_$vStyle"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateVoiceScript() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("generate_voice_script_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Voice Script", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.generateVoiceScript() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("regenerate_voice_script_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔄 Regenerate")
                    }
                }
            }
        }

        // Voice Script Output
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synchronized Voice Script",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CopyButton(
                            textToCopy = voiceScriptText,
                            onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                            variant = CopyButtonVariant.Outlined,
                            testTag = "copy_voice_script_button"
                        )

                        IconButton(
                            onClick = {
                                viewModel.saveToFavorites(
                                    type = "VOICE_SCRIPT",
                                    title = "Voice Script (${project.language})",
                                    content = voiceScriptText,
                                    metaInfo = "${project.name} • $activeStyle"
                                )
                            },
                            modifier = Modifier.testTag("save_voice_script_favorite")
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Save to Favorites",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = voiceScriptText,
                    onValueChange = { voiceScriptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_script_textarea"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 10,
                    placeholder = {
                        Text("Generated voice script with emotion tags and [Pause 1.5s] instructions will appear here.")
                    }
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STEP 6: YOUTUBE PACKAGE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun YouTubePackageStep(
    viewModel: StudioViewModel,
    project: ProjectEntity,
    scenes: List<SceneEntity>
) {
    var ytTitle by remember(project.id, project.youtubeTitle) { mutableStateOf(project.youtubeTitle) }
    var altTitles by remember(project.id, project.youtubeAltTitles) { mutableStateOf(project.youtubeAltTitles) }
    var description by remember(project.id, project.youtubeDescription) { mutableStateOf(project.youtubeDescription) }
    var hashtags by remember(project.id, project.youtubeHashtags) { mutableStateOf(project.youtubeHashtags) }
    var tags by remember(project.id, project.youtubeTags) { mutableStateOf(project.youtubeTags) }
    var shortCaption by remember(project.id, project.youtubeShortCaption) { mutableStateOf(project.youtubeShortCaption) }
    var thumbnailText by remember(project.id, project.youtubeThumbnailText) { mutableStateOf(project.youtubeThumbnailText) }
    var pinnedComment by remember(project.id, project.youtubePinnedComment) { mutableStateOf(project.youtubePinnedComment) }

    val fullPackageText = """
        === YOUTUBE VIDEO PACKAGE ===
        TITLE:
        $ytTitle
        
        ALTERNATIVE TITLES:
        $altTitles
        
        DESCRIPTION:
        $description
        
        HASHTAGS:
        $hashtags
        
        TAGS / KEYWORDS:
        $tags
        
        SHORT CAPTION:
        $shortCaption
        
        THUMBNAIL TEXT:
        $thumbnailText
        
        PINNED COMMENT:
        $pinnedComment
    """.trimIndent()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Actions Card
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📺 YouTube Shorts / Video Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Complete metadata optimized for high CTR & views",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateYouTubePackage() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("generate_youtube_package_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Package", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.showSnackbar("Copied successfully ✓")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("copy_everything_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        CopyButton(
                            textToCopy = fullPackageText,
                            onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                            variant = CopyButtonVariant.IconOnly,
                            testTag = "copy_all_inner"
                        )
                        Text("Copy Everything", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Title & Alternative Titles
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Main YouTube Title",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    CopyButton(
                        textToCopy = ytTitle,
                        onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                        variant = CopyButtonVariant.Outlined,
                        label = "Copy Title",
                        testTag = "copy_title_button"
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = ytTitle,
                    onValueChange = { ytTitle = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Alternative Titles (A/B Testing)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = altTitles,
                    onValueChange = { altTitles = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_alt_titles_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
        }

        // Description
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Description (with Timestamps)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    CopyButton(
                        textToCopy = description,
                        onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                        variant = CopyButtonVariant.Outlined,
                        label = "Copy Description",
                        testTag = "copy_description_button"
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 6
                )
            }
        }

        // Hashtags & Tags
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hashtags (#)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    CopyButton(
                        textToCopy = hashtags,
                        onCopied = { viewModel.showSnackbar("Copied successfully ✓") },
                        variant = CopyButtonVariant.Outlined,
                        label = "Copy Hashtags",
                        testTag = "copy_hashtags_button"
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_hashtags_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Keywords & Search Tags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_tags_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        // Short Caption, Thumbnail Text & Pinned Comment
        item {
            StudioCard {
                Text(
                    text = "Thumbnail, Caption & Engagement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = thumbnailText,
                    onValueChange = { thumbnailText = it },
                    label = { Text("Thumbnail Overlay Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_thumb_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shortCaption,
                    onValueChange = { shortCaption = it },
                    label = { Text("Short Caption (Reels/Shorts)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pinnedComment,
                    onValueChange = { pinnedComment = it },
                    label = { Text("Pinned Comment (Engagement Hook)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_pinned_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.saveActiveProject()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_entire_project_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💾 Save Complete Project", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
