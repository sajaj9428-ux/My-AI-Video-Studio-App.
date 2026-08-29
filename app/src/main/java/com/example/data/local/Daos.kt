package com.example.data.local

import androidx.room.*
import com.example.data.model.FavoriteItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SceneEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameProject(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    fun getScenesForProject(projectId: Long): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    suspend fun getScenesForProjectSync(projectId: Long): List<SceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<SceneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity): Long

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: Long)

    @Query("DELETE FROM scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: Long)

    @Query("DELETE FROM scenes")
    suspend fun clearAllScenes()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteItemEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY createdAt DESC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteItemEntity): Long

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
