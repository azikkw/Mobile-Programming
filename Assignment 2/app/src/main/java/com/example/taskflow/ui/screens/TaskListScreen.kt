package com.example.taskflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.model.User
import com.example.taskflow.ui.components.FilterRow
import com.example.taskflow.ui.components.SearchField
import com.example.taskflow.ui.components.StatsCard
import com.example.taskflow.ui.components.TaskCard
import com.example.taskflow.ui.components.UserSwitcher
import com.example.taskflow.viewmodel.TaskFilter
import com.example.taskflow.viewmodel.TaskUiState

@Composable
fun TaskListScreen(
    state: TaskUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onUserSelect: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = state.visibleTasks

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "users") {
            UserSwitcher(state.users, state.currentUser, onUserSelect)
        }
        item(key = "stats") {
            StatsCard(state.tasks.size, state.doneCount, state.pendingHours)
        }
        item(key = "search") {
            SearchField(state.query, onQueryChange)
        }
        item(key = "filters") {
            FilterRow(state.filter, onFilterChange)
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks found")
                }
            }
        } else {
            items(visible, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    canDelete = state.currentUser.canDelete(),
                    canToggle = task.assignee == state.currentUser.name,
                    onToggle = { onToggle(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
        }
    }
}