package dev.feeddo.sdk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.feeddo.sdk.FeeddoInternal
import dev.feeddo.sdk.models.Task
import dev.feeddo.sdk.theme.FeeddoTheme
import dev.feeddo.sdk.ui.components.TaskCard
import dev.feeddo.sdk.ui.components.TaskDetailsSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeddoTasksScreen(
    theme: FeeddoTheme,
    onBack: () -> Unit,
    initialTaskId: String? = null,
    onNavigateToConversation: ((String) -> Unit)? = null,
    onNewTask: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTaskId by remember { mutableStateOf(initialTaskId) }

    // Filter & Sort
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Features, 2: Bugs
    var isSortByUpvotes by remember { mutableStateOf(true) }

    fun loadTasks() {
        coroutineScope.launch {
            try {
                isLoading = true
                FeeddoInternal.instance.ensureUserInitialized()
                val type = when (selectedTab) {
                    1 -> "feature"
                    2 -> "bug"
                    else -> null
                }
                val sortBy = if (isSortByUpvotes) "upvotes" else "time"
                val list = FeeddoInternal.instance.apiService.getTasks(
                    userId = FeeddoInternal.instance.cachedUserId,
                    type = type,
                    sortBy = sortBy,
                    sortOrder = "desc"
                )
                tasks = list
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab, isSortByUpvotes) {
        loadTasks()
    }

    if (selectedTaskId != null) {
        TaskDetailsSheet(
            taskId = selectedTaskId!!,
            theme = theme,
            onDismiss = {
                selectedTaskId = null
                loadTasks()
            },
            onNavigateToConversation = onNavigateToConversation
        )
    }

    val onPrimaryColor = if (theme.isDark) Color.Black else Color.White

    Scaffold(
        containerColor = theme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Community Board",
                        color = theme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.colors.iconColor
                        )
                    }
                },
                actions = {
                    // Sort toggle button
                    IconButton(onClick = { isSortByUpvotes = !isSortByUpvotes }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = if (isSortByUpvotes) theme.colors.primary else theme.colors.iconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.colors.appBarBackground
                )
            )
        },
        floatingActionButton = {
            if (onNewTask != null) {
                FloatingActionButton(
                    onClick = onNewTask,
                    containerColor = theme.colors.primary,
                    contentColor = onPrimaryColor,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Feedback",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs row (All, Features, Bugs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabLabels = listOf("All", "Features", "Bugs")
                tabLabels.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) theme.colors.primary
                                else theme.colors.cardBackground
                            )
                            .border(
                                1.dp,
                                if (isSelected) theme.colors.primary else theme.colors.border,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) onPrimaryColor else theme.colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = theme.colors.primary
                    )
                } else if (tasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = theme.colors.textSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No feedback items yet",
                            color = theme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Be the first to suggest a feature or report a bug!",
                            color = theme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                theme = theme,
                                onTap = { selectedTaskId = task.id },
                                onVote = { voteType ->
                                    coroutineScope.launch {
                                        try {
                                            FeeddoInternal.instance.apiService.voteTask(
                                                taskId = task.id,
                                                voteType = voteType,
                                                userId = FeeddoInternal.instance.cachedUserId
                                            )
                                            val isUpvoted = voteType == "up"
                                            val delta = if (isUpvoted) 1 else -1
                                            tasks = tasks.map {
                                                if (it.id == task.id) {
                                                    it.copy(
                                                        myVote = if (isUpvoted) "up" else null,
                                                        upvoteCount = it.upvoteCount + delta
                                                    )
                                                } else it
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
