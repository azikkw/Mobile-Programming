# TaskFlow

A small Kotlin console application: a **task manager**. You can add tasks, assign them to users,
complete or delete them, group them by priority, calculate workload and send reminders
asynchronously. This is a plain Kotlin/JVM project (no Android).

## How to run

Requirements: JDK 17+ and Gradle (or use your IDE, e.g. IntelliJ IDEA).

```bash
gradle run
```

Or open the folder in IntelliJ IDEA, wait for Gradle sync, and run `main()` in `Main.kt`.
(If you have a Gradle wrapper, use `./gradlew run`.)

## Where the requirements are demonstrated

| Requirement | Where |
|---|---|
| Variables, data types | `Main.kt` (`val`/`var`, `String`, `Int`, `Double`, `Boolean`) |
| Conditions | `Main.kt` (`if`, `when`), `TaskManager.complete()` |
| Loops | `Main.kt` (`for`, `while`, `repeat`) |
| List, Set, Map | `TaskManager.kt`: `tasks` (List), `allTags()` (Set), `hoursByAssignee()` (Map) |
| map, filter, reduce | `TaskManager.pendingHours()` (`filter` + `map` + `reduceOrNull`), also `groupBy`, `flatMap`, `count` |
| Functions, higher-order functions, lambdas | `section(title, block)` and `filterTasks(predicate)`; lambda `isUrgent` in `Main.kt` |
| Classes and objects | `TaskManager`, `User`, `ConsoleNotifier`, ... |
| Inheritance | `Manager` extends `User` (`Models.kt`) |
| Interfaces and polymorphism | `Notifier` interface with `ConsoleNotifier` / `EmailNotifier`; `List<Notifier>` and `List<User>` |
| Data class | `Task` (uses `copy()` in `complete()`) |
| Sealed class | `TaskResult` (`Success`, `NotFound`, `Failure`) handled with `when` in `describe()` |
| Suspend function + coroutine | `sendReminder()` and `syncWithServer()` (`TaskManager.kt`), run with `runBlocking`, `launch`, `async` in `Main.kt` |
