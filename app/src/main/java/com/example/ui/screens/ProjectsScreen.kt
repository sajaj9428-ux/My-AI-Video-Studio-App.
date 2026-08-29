package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.ProjectEntity
import com.example.ui.components.SelectableChip
import com.example.ui.components.StudioCard
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val allProjects by viewModel.allProjects.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf(uiState.projectsSearchQuery) }
    var activeFilter by remember { mutableStateOf(uiState.projectsFilter) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filteredProjects = remember(allProjects, searchQuery, activeFilter) {
        allProjects.filter { project ->
            val matchesSearch = searchQuery.isBlank() ||
                    project.name.contains(searchQuery, ignoreCase = true) ||
                    project.storyIdea.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (activeFilter) {
                "Recent" -> true // Sorted by updatedAt descending
                "Favorites" -> project.isFavorite
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedByDescending { it.updatedAt }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📂 My Projects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allProjects.size} Total Projects Saved Locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(ScreenDestination.NEW_PROJECT) },
                    modifier = Modifier.testTag("projects_new_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.setProjectsSearchQuery(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("projects_search_bar"),
                placeholder = { Text("Search projects by title or story concept...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.setProjectsSearchQuery("")
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // Filter Chips (All, Recent, Favorites)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Recent", "Favorites").forEach { filter ->
                    SelectableChip(
                        selected = activeFilter == filter,
                        label = filter,
                        onClick = {
                            activeFilter = filter
                            viewModel.setProjectsFilter(filter)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "project_filter_$filter"
                    )
                }
            }
        }

        // Project Items List
        if (filteredProjects.isEmpty()) {
            item {
                StudioCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching projects found" else "No projects in this view",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a new project to start crafting your video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.navigateTo(ScreenDestination.NEW_PROJECT) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("➕ New Project")
                        }
                    }
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                ProjectItemCard(
                    project = project,
                    onOpen = { viewModel.openProject(project) },
                    onFavoriteToggle = { viewModel.toggleProjectFavorite(project.id, !project.isFavorite) },
                    onDuplicate = { viewModel.duplicateProject(project.id) },
                    onRename = {
                        projectToRename = project
                        renameText = project.name
                    },
                    onDelete = { projectToDelete = project }
                )
            }
        }
    }

    // Rename Dialog
    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToRename?.let { viewModel.renameProject(it.id, renameText) }
                        projectToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to permanently delete \"${projectToDelete?.name}\"?") },
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
private fun ProjectItemCard(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(project.updatedAt))
    }

    StudioCard(
        modifier = Modifier.testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name.ifBlank { "Untitled Project" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Updated: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (project.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Metadata badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text("${project.numScenes} Scenes", fontSize = 10.sp) },
                shape = RoundedCornerShape(6.dp)
            )
            AssistChip(
                onClick = {},
                label = { Text(project.language, fontSize = 10.sp) },
                shape = RoundedCornerShape(6.dp)
            )
            AssistChip(
                onClick = {},
                label = { Text(project.videoType, fontSize = 10.sp) },
                shape = RoundedCornerShape(6.dp)
            )
        }

        if (project.storyIdea.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = project.storyIdea,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons (Open, Duplicate, Rename, Delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .testTag("open_project_btn_${project.id}"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onDuplicate,
                modifier = Modifier.heightIn(min = 40.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(14.dp))
            }

            OutlinedButton(
                onClick = onRename,
                modifier = Modifier.heightIn(min = 40.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(14.dp))
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
