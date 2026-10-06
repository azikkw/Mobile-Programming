// ---------- Enum ----------
enum class Priority(val weight: Int) { LOW(1), MEDIUM(2), HIGH(3) }

// ---------- Data class ----------
data class Task(
    val id: Int,
    val title: String,
    val priority: Priority,
    val estimateHours: Int,
    val assignee: String,
    val tags: Set<String> = emptySet(),
    val done: Boolean = false
)

// ---------- Sealed class ----------
sealed class TaskResult {
    data class Success(val task: Task) : TaskResult()
    data class NotFound(val id: Int) : TaskResult()
    data class Failure(val reason: String) : TaskResult()
}

// ---------- Interface + implementations (polymorphism) ----------
interface Notifier {
    val channel: String
    fun notify(message: String)
}

class ConsoleNotifier : Notifier {
    override val channel = "Console"
    override fun notify(message: String) {
        println("  [$channel] $message")
    }
}

class EmailNotifier(private val address: String) : Notifier {
    override val channel = "Email"
    override fun notify(message: String) {
        println("  [$channel -> $address] $message")
    }
}

// ---------- Inheritance ----------
open class User(val name: String) {
    open val role: String = "Member"
    open fun canDelete(): Boolean = false
    override fun toString(): String = "$name ($role)"
}

class Manager(name: String) : User(name) {
    override val role: String = "Manager"
    override fun canDelete(): Boolean = true
}
