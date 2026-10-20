package com.example.taskflow.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.model.Manager
import com.example.taskflow.model.Priority
import com.example.taskflow.model.Task
import com.example.taskflow.model.TaskResult
import com.example.taskflow.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TaskFilter(val label: String) {
    ALL("All"), ACTIVE("Active"), DONE("Done"), HIGH("High")
}

data class TaskUiState(
    val tasks: List<Task>,
    val users: List<User>,
    val currentUser: User,
    val query: String = "",
    val filter: TaskFilter = TaskFilter.ALL,
    val message: String? = null,
    val isSending: Boolean = false
) {
    val visibleTasks: List<Task>
        get() = tasks
            .filter { task ->
                when (filter) {
                    TaskFilter.ALL -> true
                    TaskFilter.ACTIVE -> !task.done
                    TaskFilter.DONE -> task.done
                    TaskFilter.HIGH -> task.priority == Priority.HIGH
                }
            }
            .filter { task ->
                query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.tags.any { it.contains(query, ignoreCase = true) }
            }

    val doneCount: Int get() = tasks.count { it.done }

    val pendingHours: Int
        get() = tasks.filter { !it.done }
            .map { it.estimateHours }
            .reduceOrNull { acc, h -> acc + h } ?: 0
}

private fun TaskResult.toMessage(successPrefix: String): String = when (this) {
    is TaskResult.Success -> "$successPrefix: ${task.title}"
    is TaskResult.NotFound -> "Task #$id not found"
    is TaskResult.Failure -> reason
}

class TaskViewModel : ViewModel() {

    private val alice = User("Alice")
    private val bob = Manager("Bob")
    private var nextId = 6

    var uiState by mutableStateOf(
        TaskUiState(
            tasks = listOf(
                Task(1, "Design database", Priority.HIGH, 6, "Alice", setOf("backend", "design")),
                Task(2, "Write unit tests", Priority.MEDIUM, 4, "Alice", setOf("backend", "testing")),
                Task(3, "Build login screen", Priority.HIGH, 8, "Bob", setOf("frontend")),
                Task(4, "Update documentation", Priority.LOW, 2, "Bob", setOf("docs")),
                Task(5, "Fix payment bug", Priority.HIGH, 3, "Alice", setOf("backend", "bugfix"))
            ),
            users = listOf(alice, bob),
            currentUser = alice
        )
    )
        private set

    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query)
    }

    fun onFilterChange(filter: TaskFilter) {
        uiState = uiState.copy(filter = filter)
    }

    fun switchUser(user: User) {
        uiState = uiState.copy(currentUser = user, message = "Switched to ${user.name}")
    }

    fun messageShown() {
        uiState = uiState.copy(message = null)
    }

    fun addTask(title: String, hoursText: String, priority: Priority, tagsText: String): TaskResult {
        if (title.isBlank()) return fail("Title must not be empty")
        val hours = (if (hoursText.isBlank()) 1 else hoursText.trim().toIntOrNull())
            ?: return fail("Hours must be a number")
        if (hours <= 0) return fail("Hours must be positive")

        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val task = Task(nextId++, title.trim(), priority, hours, uiState.currentUser.name, tags)
        val result = TaskResult.Success(task)
        uiState = uiState.copy(tasks = uiState.tasks + task, message = result.toMessage("Added"))
        return result
    }

    fun toggleDone(id: Int) {
        uiState = uiState.copy(
            tasks = uiState.tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
        )
    }

    fun deleteTask(id: Int) {
        val user = uiState.currentUser
        val result: TaskResult = if (!user.canDelete()) {
            TaskResult.Failure("${user.name} has no permission to delete tasks")
        } else {
            val task = uiState.tasks.find { it.id == id }
            if (task == null) {
                TaskResult.NotFound(id)
            } else {
                uiState = uiState.copy(tasks = uiState.tasks.filter { it.id != id })
                TaskResult.Success(task)
            }
        }
        uiState = uiState.copy(message = result.toMessage("Deleted"))
    }

    fun remindPending() {
        if (uiState.isSending) return
        viewModelScope.launch {
            uiState = uiState.copy(isSending = true)
            val count = sendReminders(uiState.tasks.filter { !it.done })
            uiState = uiState.copy(
                isSending = false,
                message = if (count == 0) "Nothing to remind about" else "Reminders sent for $count task(s)"
            )
        }
    }

    private suspend fun sendReminders(tasks: List<Task>): Int {
        delay(1000)
        return tasks.size
    }

    private fun fail(reason: String): TaskResult.Failure {
        val result = TaskResult.Failure(reason)
        uiState = uiState.copy(message = result.toMessage(""))
        return result
    }
}
