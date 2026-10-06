import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Higher-order function: prints a header and runs the given block
fun section(title: String, block: () -> Unit) {
    println("\n=== $title ===")
    block()
}

// `when` over a sealed class: the compiler checks all cases
fun describe(result: TaskResult): String = when (result) {
    is TaskResult.Success -> "OK: '${result.task.title}' (done = ${result.task.done})"
    is TaskResult.NotFound -> "Error: task #${result.id} not found"
    is TaskResult.Failure -> "Error: ${result.reason}"
}

// Prints all the tasks in the `manager` (for loop + when)
fun describeTasks(tasks: List<Task>) {
    for (task in tasks) {
        val mark = if (task.done) "[x]" else "[ ]"
        val level = when (task.priority) {
            Priority.HIGH -> "!!!"
            Priority.MEDIUM -> "!!"
            Priority.LOW -> "!"
        }
        println("$mark #${task.id} ${task.title} ${task.tags} $level (${task.estimateHours}h, ${task.assignee})")
    }
}

fun main() {
    // Polymorphism: a list of the interface type
    val notifiers: List<Notifier> = listOf(ConsoleNotifier(), EmailNotifier("team@example.com"))
    val manager = TaskManager(notifiers)

    // Polymorphism: Manager is used as User
    val alice = User("Alice")
    val bob: User = Manager("Bob")
    val users = listOf(alice, bob)

    section("Users (inheritance)") {
        for (user in users) println("${user} can delete: ${user.canDelete()}")
    }

    // Variables and basic types
    val appName: String = "TaskFlow"
    var addedCount: Int = 0
    val urgentLimit: Double = 5.0
    println("Welcome to $appName!")

    manager.add("Design database", Priority.HIGH, 6, alice.name, "backend", "design")
    manager.add("Write unit tests", Priority.MEDIUM, 4, alice.name, "backend", "testing")
    manager.add("Build login screen", Priority.HIGH, 8, bob.name, "frontend", "implementation")
    manager.add("Update documentation", Priority.LOW, 2, bob.name, "docs")
    manager.add("Fix payment bug", Priority.HIGH, 3, alice.name, "backend", "bugfix")
    addedCount = manager.allTasks().size

    section("All tasks at the start") {
        describeTasks(manager.allTasks())
    }

    section("Collections: List, Set, Map") {
        // Lambda stored in a variable
        val isUrgent: (Task) -> Boolean = { it.priority == Priority.HIGH && !it.done }
        val urgent = manager.filterTasks(isUrgent)             // higher-order function
        println("Urgent tasks: ${urgent.map { it.title }}")     // List + map

        println("Unique tags (Set): ${manager.allTags()}")
        println("Hours per person (Map): ${manager.hoursByAssignee()}")

        val byPriority = manager.allTasks().groupBy { it.priority }
        for ((priority, list) in byPriority) println("$priority -> ${list.size} task(s)")

        val total = manager.pendingHours()                     // map + filter + reduce
        val average = total.toDouble() / addedCount
        println("Pending hours: $total, average per task: ${"%.1f".format(average)}")
        if (average > urgentLimit) println("Warning: workload is high!") else println("Workload is OK.")
    }

    section("Results with sealed class") {
        println(describe(manager.complete(1)))
        println(describe(manager.complete(3)))
        println(describe(manager.complete(1)))        // already done -> Failure
        println(describe(manager.delete(4, alice)))   // no permission -> Failure
        println(describe(manager.delete(4, bob)))     // Manager can delete -> Success
        println(describe(manager.complete(4)))        // NotFound after deleted
    }

    section("Progress (while + repeat)") {
        val all = manager.allTasks()
        val doneCount = all.count { it.done }
        var i = 0
        val bar = StringBuilder()
        while (i < all.size) {
            bar.append(if (all[i].done) "#" else "-")
            i++
        }
        println("Progress: [$bar] $doneCount/${all.size}")
        repeat(2) { n -> println("Report line ${n + 1}") }
    }

    section("Coroutines") {
        runBlocking {
            val pending = manager.filterTasks { !it.done }.take(2)
            // Two reminders run concurrently
            val jobs = pending.map { task -> launch { manager.sendReminder(task) } }
            jobs.joinAll()

            val synced = async { manager.syncWithServer() }
            println("Synced ${synced.await()} task(s) with server.")
        }
    }

    section("All tasks at the end") {
        describeTasks(manager.allTasks())
    }
}
