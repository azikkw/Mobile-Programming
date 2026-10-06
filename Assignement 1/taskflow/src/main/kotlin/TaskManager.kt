import kotlinx.coroutines.delay

class TaskManager(private val notifiers: List<Notifier>) {

    private val tasks = mutableListOf<Task>()
    private var nextId = 1

    fun add(
        title: String,
        priority: Priority,
        hours: Int,
        assignee: String,
        vararg tags: String
    ): Task {
        val task = Task(nextId++, title, priority, hours, assignee, tags.toSet())
        tasks.add(task)
        return task
    }

    fun allTasks(): List<Task> = tasks.toList()

    fun complete(id: Int): TaskResult {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return TaskResult.NotFound(id)
        if (tasks[index].done) return TaskResult.Failure("Task #$id is already completed")
        val updated = tasks[index].copy(done = true)   // data class copy()
        tasks[index] = updated
        return TaskResult.Success(updated)
    }

    fun delete(id: Int, by: User): TaskResult {
        if (!by.canDelete()) return TaskResult.Failure("${by.name} has no permission to delete tasks")
        val task = tasks.find { it.id == id } ?: return TaskResult.NotFound(id)
        tasks.remove(task)
        return TaskResult.Success(task)
    }

    // Higher-order function: takes a lambda as a parameter
    fun filterTasks(predicate: (Task) -> Boolean): List<Task> = tasks.filter(predicate)

    // Set
    fun allTags(): Set<String> = tasks.flatMap { it.tags }.toSet()

    // Map: assignee -> total estimated hours (groupBy + mapValues)
    fun hoursByAssignee(): Map<String, Int> =
        tasks.groupBy { it.assignee }
            .mapValues { (_, list) -> list.sumOf { it.estimateHours } }

    // map + filter + reduce
    fun pendingHours(): Int =
        tasks.filter { !it.done }
            .map { it.estimateHours }
            .reduceOrNull { acc, h -> acc + h } ?: 0

    // ---------- Suspend functions ----------
    suspend fun sendReminder(task: Task) {
        delay(300)  // simulates a slow network call
        notifiers.forEach { it.notify("Reminder: '${task.title}' is assigned to ${task.assignee}") }
    }

    suspend fun syncWithServer(): Int {
        delay(500)
        return tasks.size
    }
}
