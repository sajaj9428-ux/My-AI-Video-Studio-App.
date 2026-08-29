package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudioApp(viewModel: StudioViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle Snackbar messages from ViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSnackbar()
            }
        }
    }

    // Android Hardware Back Handler
    BackHandler(enabled = uiState.currentScreen != ScreenDestination.HOME) {
        when (uiState.currentScreen) {
            ScreenDestination.WORKFLOW -> {
                val step = uiState.activeProject?.currentWorkflowStep ?: 0
                if (step > 0) {
                    viewModel.setWorkflowStep(step - 1)
                } else {
                    viewModel.navigateTo(ScreenDestination.PROJECTS)
                }
            }
            ScreenDestination.NEW_PROJECT -> viewModel.navigateTo(ScreenDestination.HOME)
            ScreenDestination.PROJECTS -> viewModel.navigateTo(ScreenDestination.HOME)
            ScreenDestination.FAVORITES -> viewModel.navigateTo(ScreenDestination.HOME)
            ScreenDestination.SETTINGS -> viewModel.navigateTo(ScreenDestination.HOME)
            ScreenDestination.HOME -> { /* Exit app */ }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        bottomBar = {
            // Show Bottom Bar on main navigation screens (or allow quick switching everywhere)
            StudioBottomNavigationBar(
                currentScreen = uiState.currentScreen,
                onNavigate = { destination -> viewModel.navigateTo(destination) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
                ScreenDestination.NEW_PROJECT -> NewProjectScreen(viewModel = viewModel)
                ScreenDestination.WORKFLOW -> WorkflowScreen(viewModel = viewModel)
                ScreenDestination.PROJECTS -> ProjectsScreen(viewModel = viewModel)
                ScreenDestination.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                ScreenDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val destination: ScreenDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun StudioBottomNavigationBar(
    currentScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit
) {
    val items = listOf(
        NavigationItem(ScreenDestination.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationItem(ScreenDestination.NEW_PROJECT, "New", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
        NavigationItem(ScreenDestination.PROJECTS, "Projects", Icons.Filled.Folder, Icons.Outlined.Folder),
        NavigationItem(ScreenDestination.FAVORITES, "Favorites", Icons.Filled.Star, Icons.Outlined.StarBorder),
        NavigationItem(ScreenDestination.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        modifier = Modifier.testTag("studio_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp
                    )
                },
                modifier = Modifier.testTag("nav_${item.label.lowercase()}")
            )
        }
    }
}
