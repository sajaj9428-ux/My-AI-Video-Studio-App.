package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "projects")
@JsonClass(generateAdapter = true)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val storyIdea: String = "",
    val language: String = "Hindi", // Hindi, English, Urdu
    val videoType: String = "YouTube Short", // YouTube Short, Instagram Reel, Normal Video
    val targetDuration: String = "60 seconds", // 30 seconds, 60 seconds, 90 seconds, Custom
    val numScenes: Int = 5,
    val style: String = "Cinematic", // Realistic, Cinematic, 3D Animation, Cartoon, Custom
    val characterRef: String = "",
    val storyText: String = "",
    val characters: String = "",
    val location: String = "",
    val mood: String = "Dramatic",
    val ending: String = "Inspiring Twist",
    val specialInstructions: String = "",
    val currentWorkflowStep: Int = 0, // 0: Story, 1: Scenes, 2: Image, 3: Video, 4: Voice, 5: YouTube
    val isFavorite: Boolean = false,
    val status: String = "Draft", // Draft, Completed, In Progress
    val youtubeTitle: String = "",
    val youtubeAltTitles: String = "",
    val youtubeDescription: String = "",
    val youtubeHashtags: String = "",
    val youtubeTags: String = "",
    val youtubeShortCaption: String = "",
    val youtubeThumbnailText: String = "",
    val youtubePinnedComment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scenes")
@JsonClass(generateAdapter = true)
data class SceneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val sceneNumber: Int = 1,
    val sceneTitle: String = "",
    val sceneDescription: String = "",
    val characters: String = "",
    val location: String = "",
    val characterActions: String = "",
    val emotion: String = "",
    val cameraShot: String = "",
    val lighting: String = "",
    val background: String = "",
    val dialogue: String = "",
    val voiceOver: String = "",
    val duration: String = "10s",
    // Generated prompts and scripts
    val imagePrompt: String = "",
    val videoPrompt: String = "",
    val videoPromptJson: String = "",
    val voiceScript: String = "",
    val pauseInstructions: String = "[Pause 1s]"
)

@Entity(tableName = "favorites")
@JsonClass(generateAdapter = true)
data class FavoriteItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val type: String, // STORY, IMAGE_PROMPT, VIDEO_PROMPT, VOICE_SCRIPT, YOUTUBE_PACKAGE
    val title: String,
    val subtitle: String = "",
    val content: String,
    val metaInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
@JsonClass(generateAdapter = true)
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val defaultLanguage: String = "Hindi",
    val defaultDuration: String = "60 seconds",
    val defaultNumScenes: Int = 5,
    val defaultStyle: String = "Cinematic",
    val defaultPromptStyle: String = "Detailed Cinematic",
    val isDarkMode: Boolean = true,
    val isAutoSave: Boolean = true
)
