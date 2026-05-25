# Card 10: Troubleshooting Common Errors

## When to Use

Load this card when:
- User reports "error", "报错", "启动失败", "不工作"
- Encountering ClassNotFoundException, NoClassDefFoundError
- "No bean found" injection errors
- Lifecycle timing issues
- Command or listener not working

## Common Error Patterns

### 1. ClassNotFoundException / NoClassDefFoundError

**Symptom**: Plugin fails to load with class not found errors

**Possible Causes**:
1. Missing TabooLib module in build.gradle.kts
2. Using `compileOnly()` instead of `taboo()`
3. Missing `relocate` configuration for IoC
4. Wrong TabooLib version

**Diagnostic Steps**:
```kotlin
// Check build.gradle.kts
dependencies {
    taboo("platform-bukkit")  // ← Must use taboo(), not compileOnly()
    taboo("database-ioc")     // ← Required for IoC
}

taboolib {
    relocate("taboolib", "${project.group}.taboolib")  // ← Required for IoC
}
```

**Fix**: Add missing modules, use `taboo()`, configure `relocate`

---

### 2. No Bean Found for Type X

**Symptom**: `No bean found for type com.example.MyService`

**Possible Causes**:
1. Missing `@Component` annotation on class
2. Class not scanned (wrong package or missing `@Inject`)
3. Circular dependency
4. IoC module not installed

**Diagnostic Steps**:
```kotlin
// Check 1: Class has @Component
@Component  // ← Must have this
class MyService { }

// Check 2: Class is in scanned package
// TabooLib scans all classes with @Inject or in plugin package

// Check 3: No circular dependencies
@Component
class ServiceA {
    @Resource
    lateinit var serviceB: ServiceB  // ← Check if ServiceB depends on ServiceA
}
```

**Fix**: Add `@Component`, verify package structure, break circular dependencies

---

### 3. Config Not Loaded / Null

**Symptom**: Config values are null or default

**Possible Causes**:
1. Accessing config in CONST lifecycle (too early)
2. Config file not in resources folder
3. Wrong file path in `@Config` annotation
4. Config not initialized before use

**Diagnostic Steps**:
```kotlin
// Check 1: Lifecycle timing
@Awake(LifeCycle.CONST)
fun loadConfig() {
    val value = config["key"]  // ← TOO EARLY! Config not loaded yet
}

// Fix: Use ENABLE or later
@Awake(LifeCycle.ENABLE)
fun loadConfig() {
    val value = config["key"]  // ← Safe
}

// Check 2: File location
// Must be in: src/main/resources/config.yml
```

**Fix**: Move config access to ENABLE lifecycle, verify file location

---

### 4. Command Not Registered

**Symptom**: Command doesn't work, no tab completion

**Possible Causes**:
1. Missing `@CommandHeader` or `@CommandBody` annotation
2. Class not scanned
3. Command name conflict with existing command
4. Wrong command syntax

**Diagnostic Steps**:
```kotlin
// Check 1: Annotations present
@CommandHeader(name = "example")  // ← Must have this
object ExampleCommand {
    
    @CommandBody  // ← Must have this
    val reload = subCommand { }
}

// Check 2: Check server logs for registration message
// [TabooLib] Registered command: example
```

**Fix**: Add annotations, check logs for conflicts

---

### 5. Listener Not Firing

**Symptom**: Event listener doesn't execute

**Possible Causes**:
1. Missing `@SubscribeEvent` annotation
2. Wrong event type
3. Event cancelled by other plugin (need `ignoreCancelled = false`)
4. Wrong method signature

**Diagnostic Steps**:
```kotlin
// Check 1: Annotation present
@SubscribeEvent  // ← Must have this
fun onJoin(event: PlayerJoinEvent) { }

// Check 2: Correct event type
// PlayerJoinEvent (after join) vs PlayerLoginEvent (before join)

// Check 3: Check if event cancelled
@SubscribeEvent(ignoreCancelled = false)  // ← Listen even if cancelled
fun onJoin(event: PlayerJoinEvent) {
    println("Cancelled: ${event.isCancelled}")
}
```

**Fix**: Add annotation, verify event type, check cancellation

---

### 6. Scheduler Not Available

**Symptom**: Tasks don't execute or throw errors

**Possible Causes**:
1. Scheduling tasks in ENABLE lifecycle (too early)
2. Scheduler not initialized yet
3. Task cancelled prematurely

**Diagnostic Steps**:
```kotlin
// Check: Lifecycle timing
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) { }  // ← May fail, scheduler not ready
}

// Fix: Use ACTIVE lifecycle
@Awake(LifeCycle.ACTIVE)
fun startTasks() {
    submit(period = 20) { }  // ← Safe, scheduler ready
}
```

**Fix**: Move to ACTIVE lifecycle

---

### 7. ConcurrentModificationException / Thread Errors

**Symptom**: Crashes with concurrent modification or thread errors

**Possible Causes**:
1. Bukkit API access from async thread
2. Modifying collection while iterating
3. Race condition with shared state

**Diagnostic Steps**:
```kotlin
// Check 1: Bukkit API in async
submit(async = true) {
    player.sendMessage("Hi")  // ← CRASH! Bukkit API on async thread
}

// Fix: Switch to main thread
submit(async = true) {
    val data = database.query()
    submit(async = false) {  // ← Back to main thread
        player.sendMessage("Hi")
    }
}

// Check 2: Collection modification
for (player in players) {
    players.remove(player)  // ← ConcurrentModificationException!
}

// Fix: Use iterator or copy
players.removeIf { condition }  // ← Safe
```

**Fix**: Use correct thread for Bukkit API, use safe collection operations

---

### 8. lateinit Property Not Initialized

**Symptom**: `lateinit property X has not been initialized`

**Possible Causes**:
1. Using dependency before `@PostConstruct`
2. Dependency injection failed
3. Accessing field before initialization

**Diagnostic Steps**:
```kotlin
// Check 1: Timing
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    init {
        dependency.doSomething()  // ← TOO EARLY! Not injected yet
    }
    
    @PostConstruct
    fun init() {
        dependency.doSomething()  // ← Safe, injected now
    }
}
```

**Fix**: Move to `@PostConstruct`, verify injection succeeded

---

## Diagnostic Checklist

When encountering errors:

- [ ] Check server logs for full stack trace
- [ ] Verify TabooLib version matches code expectations
- [ ] Confirm all required modules installed in build.gradle.kts
- [ ] Check lifecycle timing (CONST → INIT → LOAD → ENABLE → ACTIVE)
- [ ] Verify annotations present (@Component, @SubscribeEvent, @CommandHeader, etc.)
- [ ] Confirm thread safety (Bukkit API on main thread, IO on async)
- [ ] Check for circular dependencies in IoC
- [ ] Verify file locations (config, lang files in resources/)
- [ ] Test with minimal reproduction case

## Quick Fixes Reference

| Error | Quick Fix |
|-------|-----------|
| ClassNotFoundException | Add module to build.gradle.kts with `taboo()` |
| No bean found | Add `@Component` to class |
| Config null | Move access to ENABLE lifecycle |
| Command not working | Add `@CommandHeader` and `@CommandBody` |
| Listener not firing | Add `@SubscribeEvent` |
| Scheduler error | Move to ACTIVE lifecycle |
| Thread error | Use correct thread for Bukkit API |
| lateinit error | Move to `@PostConstruct` |

## References

- Related cards: All cards (lifecycle, IoC, commands, listeners, scheduler, config)
- Related recipes: `fix_ioc_injection_error.md`, `fix_lifecycle_error.md`
- Related tests: `troubleshooting_checklist.md`
