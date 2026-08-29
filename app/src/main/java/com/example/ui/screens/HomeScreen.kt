package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val recentProjects by viewModel.recentProjects.collectAsState()
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        item {
            HeaderBanner(
                onNewProjectClick = { viewModel.navigateTo(ScreenDestination.NEW_PROJECT) }
            )
        }

        // 10 Main Action Grid Buttons
        item {
            Text(
                text = "Quick Creation Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        item {
            QuickActionsGrid(
                onActionSelected = { actionIndex ->
                    when (actionIndex) {
                        0 -> viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        1 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 0)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        2 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 1)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        3 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 2)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        4 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 3)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        5 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 4)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        6 -> {
                            val active = viewModel.uiState.value.activeProject ?: recentProjects.firstOrNull()
                            if (active != null) viewModel.openProject(active, 5)
                            else viewModel.navigateTo(ScreenDestination.NEW_PROJECT)
                        }
                        7 -> viewModel.navigateTo(ScreenDestination.PROJECTS)
                        8 -> viewModel.navigateTo(ScreenDestination.FAVORITES)
                        9 -> viewModel.navigateTo(ScreenDestination.SETTINGS)
                    }
                }
            )
        }

        // Recent Projects Section Header
        item {
            SectionHeader(
                title = "Recent Projects",
                subtitle = "Pick up right where you left off",
                icon = Icons.Default.History,
                action = {
                    if (recentProjects.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.navigateTo(ScreenDestination.PROJECTS) },
                            modifier = Modifier.testTag("view_all_projects_button")
                        ) {
                            Text("View All", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }

        // Recent Projects List
        if (recentProjects.isEmpty()) {
            item {
                EmptyProjectsBanner(
                    onCreateClick = { viewModel.navigateTo(ScreenDestination.NEW_PROJECT) }
                )
            }
        } else {
            items(recentProjects, key = { it.id }) { project ->
                RecentProjectCard(
                    project = project,
                    onOpen = { viewModel.openProject(project) },
                    onEdit = { viewModel.openProject(project, 0) },
                    onDelete = { projectToDelete = project }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete \"${projectToDelete?.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { viewModel.deleteProject(it.id) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeaderBanner(
    onNewProjectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_header_banner"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My AI Video Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Create your next AI video in minutes with stories, scenes, prompt scripts & YouTube Shorts packages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNewProjectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("create_new_project_main_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "➕ Create New Project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String,
    val color: Color
)

@Composable
private fun QuickActionsGrid(
    onActionSelected: (Int) -> Unit
) {
    val actions = listOf(
        QuickActionItem("New Project", "Start new video", Icons.Default.AddCircle, "1", Color(0xFF8B5CF6)),
        QuickActionItem("Story Maker", "Write AI story", Icons.Default.MenuBook, "2", Color(0xFF06B6D4)),
        QuickActionItem("Scene Gen", "Break into scenes", Icons.Default.Movie, "3", Color(0xFFF59E0B)),
        QuickActionItem("Image Prompt", "Midjourney & Flux", Icons.Default.Image, "4", Color(0xFFEC4899)),
        QuickActionItem("Video Prompt", "Veo & Video Gen", Icons.Default.Videocam, "5", Color(0xFF10B981)),
        QuickActionItem("Voice Over", "Hindi/Eng scripts", Icons.Default.RecordVoiceOver, "6", Color(0xFF3B82F6)),
        QuickActionItem("YouTube Pkg", "Shorts SEO & tags", Icons.Default.Subscriptions, "7", Color(0xFFEF4444)),
        QuickActionItem("My Projects", "Project library", Icons.Default.Folder, "8", Color(0xFF6366F1)),
        QuickActionItem("Favorites", "Saved prompts", Icons.Default.Star, "9", Color(0xFFEAB308)),
        QuickActionItem("Settings", "Studio options", Icons.Default.Settings, "10", Color(0xFF64748B))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowIndex in actions.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item 1
                val item1 = actions[rowIndex]
                Box(modifier = Modifier.weight(1f)) {
                    QuickActionCard(
                        item = item1,
                        index = rowIndex,
                        onClick = { onActionSelected(rowIndex) }
                    )
                }

                // Item 2
                if (rowIndex + 1 < actions.size) {
                    val item2 = actions[rowIndex + 1]
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionCard(
                            item = item2,
                            index = rowIndex + 1,
                            onClick = { onActionSelected(rowIndex + 1) }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    item: QuickActionItem,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { onClick() }
            .testTag("quick_action_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun RecentProjectCard(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(project.updatedAt))
    }

    StudioCard(
        modifier = Modifier.testTag("recent_project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name.ifBlank { "Untitled AI Project" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Created: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AssistChip(
                onClick = {},
                label = { Text("${project.numScenes} Scenes", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        if (project.storyIdea.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = project.storyIdea,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons: Open, Edit, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .testTag("open_project_${project.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Open",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("edit_project_${project.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit")
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectsBanner(
    onCreateClick: () -> Unit
) {
    StudioCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Projects Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start creating your first AI video story & prompts!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreateClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create First Project")
            }
        }
    }
}
