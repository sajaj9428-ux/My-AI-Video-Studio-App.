package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FavoriteItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SceneEntity
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.StudioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    NEW_PROJECT,
    WORKFLOW,
    PROJECTS,
    FAVORITES,
    SETTINGS
}

data class StudioUiState(
    val currentScreen: ScreenDestination = ScreenDestination.HOME,
    val activeProject: ProjectEntity? = null,
    val activeScenes: List<SceneEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val generatingTask: String = "",
    val errorMessage: String? = null,
    val lastFailedAction: (() -> Unit)? = null,
    val snackbarMessage: String? = null,
    val projectsSearchQuery: String = "",
    val projectsFilter: String = "All", // All, Recent, Favorites
    val favoritesFilter: String = "All", // All, Stories, Image Prompts, Video Prompts, Voice Scripts, YouTube Packages
    val selectedSceneIndex: Int = 0,
    val activeVideoPromptMode: String = "TEXT", // TEXT, JSON
    val activeVoiceStyle: String = "Cinematic",
    val userSettings: UserSettingsEntity = UserSettingsEntity()
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = StudioRepository(database)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentProjects: StateFlow<List<ProjectEntity>> = repository.recentProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFavorites: StateFlow<List<FavoriteItemEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                if (settings != null) {
                    _uiState.update { it.copy(userSettings = settings) }
                } else {
                    val defaultSettings = UserSettingsEntity()
                    repository.saveSettings(defaultSettings)
                    _uiState.update { it.copy(userSettings = defaultSettings) }
                }
            }
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _uiState.update { it.copy(currentScreen = screen, errorMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, lastFailedAction = null) }
    }

    fun setProjectsSearchQuery(query: String) {
        _uiState.update { it.copy(projectsSearchQuery = query) }
    }

    fun setProjectsFilter(filter: String) {
        _uiState.update { it.copy(projectsFilter = filter) }
    }

    fun setFavoritesFilter(filter: String) {
        _uiState.update { it.copy(favoritesFilter = filter) }
    }

    fun setSelectedSceneIndex(index: Int) {
        _uiState.update { it.copy(selectedSceneIndex = index) }
    }

    fun setVideoPromptMode(mode: String) {
        _uiState.update { it.copy(activeVideoPromptMode = mode) }
    }

    fun setVoiceStyle(style: String) {
        _uiState.update { it.copy(activeVoiceStyle = style) }
    }

    fun setWorkflowStep(step: Int) {
        val currentProject = _uiState.value.activeProject ?: return
        val updated = currentProject.copy(currentWorkflowStep = step)
        _uiState.update { it.copy(activeProject = updated) }
        viewModelScope.launch {
            repository.updateProject(updated)
        }
    }

    // --- Project Operations ---

    fun startNewProject(
        name: String,
        storyIdea: String,
        language: String,
        videoType: String,
        targetDuration: String,
        numScenes: Int,
        style: String,
        autoGenerateStory: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = if (autoGenerateStory) "Generating AI Story..." else "Creating Project...",
                    errorMessage = null
                )
            }
            try {
                val projectName = name.ifBlank { "AI Video Project ${System.currentTimeMillis() % 1000}" }
                val initialProject = ProjectEntity(
                    name = projectName,
                    storyIdea = storyIdea,
                    language = language,
                    videoType = videoType,
                    targetDuration = targetDuration,
                    numScenes = numScenes,
                    style = style,
                    currentWorkflowStep = 0,
                    status = "In Progress"
                )

                val generatedStory = if (autoGenerateStory) {
                    repository.generateStory(
                        idea = storyIdea,
                        characters = "",
                        location = "",
                        mood = "Dramatic",
                        ending = "Inspiring Twist",
                        specialInstructions = "",
                        language = language,
                        style = style,
                        videoType = videoType,
                        targetDuration = targetDuration
                    )
                } else ""

                val projectWithStory = initialProject.copy(storyText = generatedStory)
                val projectId = repository.createProject(projectWithStory)
                val createdProject = projectWithStory.copy(id = projectId)

                _uiState.update {
                    it.copy(
                        activeProject = createdProject,
                        activeScenes = emptyList(),
                        currentScreen = ScreenDestination.WORKFLOW,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar(if (autoGenerateStory) "Story generated successfully ✓" else "Project created ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to initialize project",
                        lastFailedAction = {
                            startNewProject(name, storyIdea, language, videoType, targetDuration, numScenes, style, autoGenerateStory)
                        }
                    )
                }
            }
        }
    }

    fun openProject(project: ProjectEntity, targetWorkflowStep: Int? = null) {
        viewModelScope.launch {
            val scenes = repository.getScenesForProjectSync(project.id)
            val stepToOpen = targetWorkflowStep ?: project.currentWorkflowStep
            val updated = project.copy(currentWorkflowStep = stepToOpen)
            _uiState.update {
                it.copy(
                    activeProject = updated,
                    activeScenes = scenes,
                    currentScreen = ScreenDestination.WORKFLOW,
                    selectedSceneIndex = 0,
                    errorMessage = null
                )
            }
        }
    }

    fun saveActiveProject() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            repository.updateProject(project)
            showSnackbar("Project saved locally ✓")
        }
    }

    fun updateActiveProjectDetails(transform: (ProjectEntity) -> ProjectEntity) {
        val current = _uiState.value.activeProject ?: return
        val updated = transform(current)
        _uiState.update { it.copy(activeProject = updated) }
        if (_uiState.value.userSettings.isAutoSave) {
            viewModelScope.launch {
                repository.updateProject(updated)
            }
        }
    }

    fun updateScene(scene: SceneEntity) {
        val updatedList = _uiState.value.activeScenes.map { if (it.id == scene.id || (it.id == 0L && it.sceneNumber == scene.sceneNumber)) scene else it }
        _uiState.update { it.copy(activeScenes = updatedList) }
        viewModelScope.launch {
            repository.updateScene(scene)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_uiState.value.activeProject?.id == projectId) {
                _uiState.update { it.copy(activeProject = null, activeScenes = emptyList(), currentScreen = ScreenDestination.PROJECTS) }
            }
            showSnackbar("Project deleted ✓")
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(projectId)
            if (newId > 0) {
                showSnackbar("Project duplicated ✓")
            }
        }
    }

    fun renameProject(projectId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newName)
            if (_uiState.value.activeProject?.id == projectId) {
                _uiState.update { it.copy(activeProject = it.activeProject?.copy(name = newName)) }
            }
            showSnackbar("Project renamed ✓")
        }
    }

    fun toggleProjectFavorite(projectId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(projectId, isFavorite)
            if (_uiState.value.activeProject?.id == projectId) {
                _uiState.update { it.copy(activeProject = it.activeProject?.copy(isFavorite = isFavorite)) }
            }
            showSnackbar(if (isFavorite) "Added to favorites ⭐" else "Removed from favorites")
        }
    }

    // --- Story Operations ---

    fun generateOrModifyStory(actionType: String = "GENERATE") {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = when (actionType) {
                        "IMPROVE" -> "Improving Story..."
                        "SHORTEN" -> "Condensing Story..."
                        "EXPAND" -> "Expanding Story Beats..."
                        "REGENERATE" -> "Regenerating Story..."
                        else -> "Generating AI Story..."
                    },
                    errorMessage = null
                )
            }
            try {
                val result = repository.generateStory(
                    idea = project.storyIdea,
                    characters = project.characters,
                    location = project.location,
                    mood = project.mood,
                    ending = project.ending,
                    specialInstructions = project.specialInstructions,
                    language = project.language,
                    style = project.style,
                    videoType = project.videoType,
                    targetDuration = project.targetDuration,
                    actionType = actionType,
                    currentStory = project.storyText
                )
                val updatedProject = project.copy(storyText = result)
                _uiState.update {
                    it.copy(
                        activeProject = updatedProject,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                repository.updateProject(updatedProject)
                showSnackbar("Story updated successfully ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate story",
                        lastFailedAction = { generateOrModifyStory(actionType) }
                    )
                }
            }
        }
    }

    fun convertStoryToScenes() {
        val project = _uiState.value.activeProject ?: return
        if (project.storyText.isBlank()) {
            showSnackbar("Please generate or enter a story first")
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Converting Story into ${project.numScenes} Scenes...",
                    errorMessage = null
                )
            }
            try {
                val scenes = repository.generateScenesFromStory(
                    project = project,
                    storyText = project.storyText,
                    numScenes = project.numScenes
                )
                val updatedProject = project.copy(currentWorkflowStep = 1)
                _uiState.update {
                    it.copy(
                        activeProject = updatedProject,
                        activeScenes = scenes,
                        isGenerating = false,
                        generatingTask = "",
                        selectedSceneIndex = 0
                    )
                }
                repository.updateProject(updatedProject)
                showSnackbar("${scenes.size} Scenes generated successfully ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate scenes",
                        lastFailedAction = { convertStoryToScenes() }
                    )
                }
            }
        }
    }

    // --- Scene Operations ---

    fun regenerateScene(sceneIndex: Int) {
        val project = _uiState.value.activeProject ?: return
        val currentScenes = _uiState.value.activeScenes
        val targetScene = currentScenes.getOrNull(sceneIndex) ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Regenerating Scene ${targetScene.sceneNumber}...",
                    errorMessage = null
                )
            }
            try {
                // Re-run single scene generation
                val allUpdated = repository.generateScenesFromStory(project, project.storyText, project.numScenes)
                _uiState.update {
                    it.copy(
                        activeScenes = allUpdated,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("Scene regenerated ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to regenerate scene",
                        lastFailedAction = { regenerateScene(sceneIndex) }
                    )
                }
            }
        }
    }

    fun generateAllScenePrompts() {
        val project = _uiState.value.activeProject ?: return
        val currentScenes = _uiState.value.activeScenes
        if (currentScenes.isEmpty()) {
            showSnackbar("No scenes available. Generate scenes first.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Generating Image & Video Prompts for all ${currentScenes.size} scenes...",
                    errorMessage = null
                )
            }
            try {
                val updatedScenes = mutableListOf<SceneEntity>()
                for (scene in currentScenes) {
                    val imgPrompt = repository.generateImagePromptForScene(project, scene, project.characterRef)
                    val (vidPrompt, vidJson) = repository.generateVideoPromptForScene(project, scene, project.characterRef)
                    updatedScenes.add(
                        scene.copy(
                            imagePrompt = imgPrompt,
                            videoPrompt = vidPrompt,
                            videoPromptJson = vidJson
                        )
                    )
                }
                repository.saveScenes(updatedScenes)
                _uiState.update {
                    it.copy(
                        activeScenes = updatedScenes,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("All scene prompts generated successfully ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate all scene prompts",
                        lastFailedAction = { generateAllScenePrompts() }
                    )
                }
            }
        }
    }

    // --- Image Prompt Operations ---

    fun generateImagePromptForCurrentScene() {
        val project = _uiState.value.activeProject ?: return
        val scene = _uiState.value.activeScenes.getOrNull(_uiState.value.selectedSceneIndex) ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Creating Image Prompt for Scene ${scene.sceneNumber}...",
                    errorMessage = null
                )
            }
            try {
                val prompt = repository.generateImagePromptForScene(project, scene, project.characterRef)
                val updatedScene = scene.copy(imagePrompt = prompt)
                updateScene(updatedScene)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("Image prompt generated ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate image prompt",
                        lastFailedAction = { generateImagePromptForCurrentScene() }
                    )
                }
            }
        }
    }

    // --- Video Prompt Operations ---

    fun generateVideoPromptForCurrentScene() {
        val project = _uiState.value.activeProject ?: return
        val scene = _uiState.value.activeScenes.getOrNull(_uiState.value.selectedSceneIndex) ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Creating Video Prompt & JSON for Scene ${scene.sceneNumber}...",
                    errorMessage = null
                )
            }
            try {
                val (textPrompt, jsonPrompt) = repository.generateVideoPromptForScene(project, scene, project.characterRef)
                val updatedScene = scene.copy(videoPrompt = textPrompt, videoPromptJson = jsonPrompt)
                updateScene(updatedScene)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("Video prompt generated ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate video prompt",
                        lastFailedAction = { generateVideoPromptForCurrentScene() }
                    )
                }
            }
        }
    }

    // --- Voice Over Operations ---

    fun generateVoiceScript() {
        val project = _uiState.value.activeProject ?: return
        val scenes = _uiState.value.activeScenes
        val style = _uiState.value.activeVoiceStyle

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Generating Synchronized Voice-Over Script ($style)...",
                    errorMessage = null
                )
            }
            try {
                val script = repository.generateVoiceScript(project, scenes, style, project.language)
                val updatedScenes = scenes.map {
                    it.copy(voiceScript = script)
                }
                repository.saveScenes(updatedScenes)
                _uiState.update {
                    it.copy(
                        activeScenes = updatedScenes,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("Voice-over script generated ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate voice script",
                        lastFailedAction = { generateVoiceScript() }
                    )
                }
            }
        }
    }

    // --- YouTube Package Operations ---

    fun generateYouTubePackage() {
        val project = _uiState.value.activeProject ?: return
        val scenes = _uiState.value.activeScenes

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generatingTask = "Generating YouTube Shorts Package & SEO metadata...",
                    errorMessage = null
                )
            }
            try {
                val pkg = repository.generateYouTubePackage(project, scenes, project.storyText)
                val updatedProject = project.copy(
                    youtubeTitle = pkg["title"] ?: "",
                    youtubeAltTitles = pkg["altTitles"] ?: "",
                    youtubeDescription = pkg["description"] ?: "",
                    youtubeHashtags = pkg["hashtags"] ?: "",
                    youtubeTags = pkg["tags"] ?: "",
                    youtubeShortCaption = pkg["shortCaption"] ?: "",
                    youtubeThumbnailText = pkg["thumbnailText"] ?: "",
                    youtubePinnedComment = pkg["pinnedComment"] ?: ""
                )
                _uiState.update {
                    it.copy(
                        activeProject = updatedProject,
                        isGenerating = false,
                        generatingTask = ""
                    )
                }
                showSnackbar("YouTube package generated ✓")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate YouTube package",
                        lastFailedAction = { generateYouTubePackage() }
                    )
                }
            }
        }
    }

    // --- Favorites Operations ---

    fun saveToFavorites(
        type: String, // STORY, IMAGE_PROMPT, VIDEO_PROMPT, VOICE_SCRIPT, YOUTUBE_PACKAGE
        title: String,
        content: String,
        metaInfo: String = ""
    ) {
        val projectId = _uiState.value.activeProject?.id
        viewModelScope.launch {
            val fav = FavoriteItemEntity(
                projectId = projectId,
                type = type,
                title = title,
                subtitle = _uiState.value.activeProject?.name ?: "Personal Studio Item",
                content = content,
                metaInfo = metaInfo
            )
            repository.addFavorite(fav)
            showSnackbar("Saved to Favorites ⭐ ✓")
        }
    }

    fun deleteFavorite(id: Long) {
        viewModelScope.launch {
            repository.removeFavorite(id)
            showSnackbar("Removed from favorites ✓")
        }
    }

    // --- Settings Operations ---

    fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = _uiState.value.userSettings
        val updated = transform(current)
        _uiState.update { it.copy(userSettings = updated) }
        viewModelScope.launch {
            repository.saveSettings(updated)
            showSnackbar("Settings saved ✓")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.update {
                it.copy(
                    activeProject = null,
                    activeScenes = emptyList(),
                    currentScreen = ScreenDestination.HOME
                )
            }
            showSnackbar("All local data cleared ✓")
        }
    }
}
