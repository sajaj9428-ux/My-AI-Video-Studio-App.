package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.FavoriteItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SceneEntity
import com.example.data.model.UserSettingsEntity
import com.example.data.remote.AiGenerationService
import kotlinx.coroutines.flow.Flow

class StudioRepository(
    private val database: AppDatabase,
    private val aiService: AiGenerationService = AiGenerationService()
) {
    private val projectDao = database.projectDao()
    private val sceneDao = database.sceneDao()
    private val favoriteDao = database.favoriteDao()
    private val settingsDao = database.settingsDao()

    // Projects
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val recentProjects: Flow<List<ProjectEntity>> = projectDao.getRecentProjects()
    val favoriteProjects: Flow<List<ProjectEntity>> = projectDao.getFavoriteProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)
    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?> = projectDao.getProjectByIdFlow(id)

    suspend fun createProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: Long) {
        sceneDao.deleteScenesForProject(id)
        projectDao.deleteProjectById(id)
    }

    suspend fun duplicateProject(id: Long): Long {
        val original = projectDao.getProjectById(id) ?: return -1
        val duplicated = original.copy(
            id = 0,
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newProjectId = projectDao.insertProject(duplicated)
        val originalScenes = sceneDao.getScenesForProjectSync(id)
        val duplicatedScenes = originalScenes.map { it.copy(id = 0, projectId = newProjectId) }
        sceneDao.insertScenes(duplicatedScenes)
        return newProjectId
    }

    suspend fun renameProject(id: Long, newName: String) {
        projectDao.renameProject(id, newName)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        projectDao.updateFavoriteStatus(id, isFavorite)
    }

    // Scenes
    fun getScenesForProject(projectId: Long): Flow<List<SceneEntity>> = sceneDao.getScenesForProject(projectId)
    suspend fun getScenesForProjectSync(projectId: Long): List<SceneEntity> = sceneDao.getScenesForProjectSync(projectId)

    suspend fun saveScenes(scenes: List<SceneEntity>) {
        sceneDao.insertScenes(scenes)
    }

    suspend fun updateScene(scene: SceneEntity) {
        sceneDao.updateScene(scene)
    }

    suspend fun deleteScene(sceneId: Long) {
        sceneDao.deleteSceneById(sceneId)
    }

    // Favorites
    val allFavorites: Flow<List<FavoriteItemEntity>> = favoriteDao.getAllFavorites()
    fun getFavoritesByType(type: String): Flow<List<FavoriteItemEntity>> = favoriteDao.getFavoritesByType(type)

    suspend fun addFavorite(favorite: FavoriteItemEntity): Long = favoriteDao.insertFavorite(favorite)
    suspend fun removeFavorite(id: Long) = favoriteDao.deleteFavoriteById(id)

    // Settings
    val settingsFlow: Flow<UserSettingsEntity?> = settingsDao.getSettings()
    suspend fun getSettings(): UserSettingsEntity {
        return settingsDao.getSettingsSync() ?: UserSettingsEntity()
    }
    suspend fun saveSettings(settings: UserSettingsEntity) {
        settingsDao.saveSettings(settings)
    }

    suspend fun clearAllData() {
        projectDao.clearAllProjects()
        sceneDao.clearAllScenes()
        favoriteDao.clearAllFavorites()
    }

    // AI Generation Calls
    suspend fun generateStory(
        idea: String,
        characters: String,
        location: String,
        mood: String,
        ending: String,
        specialInstructions: String,
        language: String,
        style: String,
        videoType: String,
        targetDuration: String,
        actionType: String = "GENERATE",
        currentStory: String = ""
    ): String {
        return aiService.generateStory(
            idea, characters, location, mood, ending, specialInstructions,
            language, style, videoType, targetDuration, actionType, currentStory
        )
    }

    suspend fun generateScenesFromStory(
        project: ProjectEntity,
        storyText: String,
        numScenes: Int
    ): List<SceneEntity> {
        val scenes = aiService.generateScenes(project, storyText, numScenes)
        sceneDao.deleteScenesForProject(project.id)
        sceneDao.insertScenes(scenes)
        return scenes
    }

    suspend fun generateImagePromptForScene(
        project: ProjectEntity,
        scene: SceneEntity,
        characterRef: String = ""
    ): String {
        val prompt = aiService.generateImagePrompt(project, scene, characterRef)
        val updated = scene.copy(imagePrompt = prompt)
        sceneDao.updateScene(updated)
        return prompt
    }

    suspend fun generateVideoPromptForScene(
        project: ProjectEntity,
        scene: SceneEntity,
        characterRef: String = ""
    ): Pair<String, String> {
        val (textPrompt, jsonPrompt) = aiService.generateVideoPrompt(project, scene, characterRef)
        val updated = scene.copy(videoPrompt = textPrompt, videoPromptJson = jsonPrompt)
        sceneDao.updateScene(updated)
        return Pair(textPrompt, jsonPrompt)
    }

    suspend fun generateVoiceScript(
        project: ProjectEntity,
        scenes: List<SceneEntity>,
        voiceStyle: String,
        language: String
    ): String {
        return aiService.generateVoiceScript(project, scenes, voiceStyle, language)
    }

    suspend fun generateYouTubePackage(
        project: ProjectEntity,
        scenes: List<SceneEntity>,
        storyText: String
    ): Map<String, String> {
        val pkg = aiService.generateYouTubePackage(project, scenes, storyText)
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
        projectDao.updateProject(updatedProject)
        return pkg
    }
}
