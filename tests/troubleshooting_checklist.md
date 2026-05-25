# Troubleshooting Checklist

**Purpose**: Systematic diagnostic steps for common TabooLib errors.

Use this checklist when user reports errors or issues with TabooLib plugin.

---

## Error Category Detection

### Step 1: Identify Error Type

**ClassNotFoundException / NoClassDefFoundError**:
- [ ] Go to Section A: Class Loading Errors

**"No bean found for type X"**:
- [ ] Go to Section B: IoC Injection Errors

**"Config not loaded" / Config values null**:
- [ ] Go to Section C: Configuration Errors

**"Scheduler not available" / Tasks not executing**:
- [ ] Go to Section D: Scheduler Errors

**Command not working / No tab completion**:
- [ ] Go to Section E: Command Errors

**Listener not firing**:
- [ ] Go to Section F: Event Listener Errors

**Thread errors / ConcurrentModificationException**:
- [ ] Go to Section G: Thread Safety Errors

**"lateinit property not initialized"**:
- [ ] Go to Section H: Initialization Errors

**Other errors**:
- [ ] Go to Section I: General Diagnostic Steps

---

## Section A: Class Loading Errors

**Symptom**: ClassNotFoundException, NoClassDefFoundError

### Diagnostic Steps

1. **Check module installation**:
```bash
# Check build.gradle.kts
grep "taboo(" build.gradle.kts
grep "install(" build.gradle.kts
```
- [ ] Verify required module installed with `taboo()`
- [ ] Verify module listed in `env { install() }`

2. **Check dependency type**:
```kotlin
// WRONG
compileOnly("taboolib:platform-bukkit:6.2.0")

// CORRECT
taboo("platform-bukkit")
```
- [ ] Verify using `taboo()` not `compileOnly()`

3. **Check relocate configuration** (for IoC):
```kotlin
taboolib {
    relocate("taboolib", "${project.group}.taboolib")
}
```
- [ ] Verify `relocate` configured if using IoC

4. **Rebuild project**:
```bash
./gradlew clean build
```
- [ ] Clean build succeeds
- [ ] JAR contains TabooLib classes

### Quick Fixes

- [ ] Add missing module: `taboo("module-name")`
- [ ] Change `compileOnly()` to `taboo()`
- [ ] Add `relocate` configuration
- [ ] Rebuild: `./gradlew clean build`

---

## Section B: IoC Injection Errors

**Symptom**: "No bean found for type X"

### Diagnostic Steps

1. **Check @Component annotation**:
```bash
grep -r "class.*MyService" src/
```
- [ ] Verify class has `@Component` annotation
- [ ] Verify not trying to inject plain data class

2. **Check module installation**:
```bash
grep "database-ioc" build.gradle.kts
```
- [ ] Verify `database-ioc` module installed
- [ ] Verify `relocate` configured

3. **Check circular dependencies**:
```bash
grep -r "@Resource" src/
```
- [ ] Verify no ServiceA → ServiceB → ServiceA cycles

4. **Check initialization timing**:
```kotlin
// WRONG
init {
    dependency.doSomething()  // Too early!
}

// CORRECT
@PostConstruct
fun init() {
    dependency.doSomething()  // Safe
}
```
- [ ] Verify dependencies used in `@PostConstruct`, not `init`

### Quick Fixes

- [ ] Add `@Component` to class
- [ ] Install `database-ioc` module
- [ ] Add `relocate` configuration
- [ ] Break circular dependencies
- [ ] Move initialization to `@PostConstruct`
- [ ] Rebuild: `./gradlew clean build`

---

## Section C: Configuration Errors

**Symptom**: Config not loaded, values are null

### Diagnostic Steps

1. **Check lifecycle timing**:
```bash
grep -r "@Awake.*CONST" src/
```
- [ ] Verify config access in ENABLE or later, not CONST

2. **Check file location**:
```bash
ls src/main/resources/config.yml
```
- [ ] Verify config file exists in resources folder

3. **Check @Config annotation**:
```bash
grep -r "@Config" src/
```
- [ ] Verify `@Config` annotation present
- [ ] Verify file path correct

4. **Check initialization**:
```kotlin
@Config("config.yml")
lateinit var config: Configuration
```
- [ ] Verify field is `lateinit var`, not `val`

### Quick Fixes

- [ ] Move config access to ENABLE lifecycle
- [ ] Place config file in `src/main/resources/`
- [ ] Add `@Config` annotation
- [ ] Change to `lateinit var`
- [ ] Restart server

---

## Section D: Scheduler Errors

**Symptom**: Tasks not executing, scheduler not available

### Diagnostic Steps

1. **Check lifecycle timing**:
```bash
grep -r "@Awake.*ENABLE" src/ | grep "submit"
```
- [ ] Verify tasks scheduled in ACTIVE, not ENABLE

2. **Check task syntax**:
```kotlin
// WRONG
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) { }  // Too early!
}

// CORRECT
@Awake(LifeCycle.ACTIVE)
fun startTasks() {
    submit(period = 20) { }  // Safe
}
```
- [ ] Verify using ACTIVE lifecycle

3. **Check task cancellation**:
```bash
grep -r "submit(period" src/
```
- [ ] Verify periodic tasks cancelled in DISABLE

### Quick Fixes

- [ ] Move to ACTIVE lifecycle
- [ ] Add task cancellation in DISABLE
- [ ] Restart server

---

## Section E: Command Errors

**Symptom**: Command not working, no tab completion

### Diagnostic Steps

1. **Check annotations**:
```bash
grep -r "@CommandHeader" src/
grep -r "@CommandBody" src/
```
- [ ] Verify `@CommandHeader` present
- [ ] Verify `@CommandBody` present on subcommands

2. **Check registration logs**:
```
[TabooLib] Registered command: example
```
- [ ] Verify command registered in server logs

3. **Check tab completion**:
```kotlin
// No completion
dynamic("arg") { }

// Has completion
player("target") { }
dynamic("arg") {
    suggestion<ProxyCommandSender> { _, _ ->
        listOf("option1", "option2")
    }
}
```
- [ ] Verify using `player()` or `dynamic()` with `suggestion`

### Quick Fixes

- [ ] Add `@CommandHeader` annotation
- [ ] Add `@CommandBody` annotation
- [ ] Add `suggestion` block for tab completion
- [ ] Check logs for registration errors
- [ ] Restart server

---

## Section F: Event Listener Errors

**Symptom**: Listener not firing

### Diagnostic Steps

1. **Check @SubscribeEvent annotation**:
```bash
grep -r "fun.*Event" src/
```
- [ ] Verify `@SubscribeEvent` annotation present

2. **Check event type**:
```kotlin
// WRONG - fires before join
@SubscribeEvent
fun onLogin(event: PlayerLoginEvent) { }

// CORRECT - fires after join
@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) { }
```
- [ ] Verify using correct event type

3. **Check cancellation**:
```kotlin
@SubscribeEvent(ignoreCancelled = true)
fun onEvent(event: SomeEvent) {
    // Only fires if NOT cancelled
}
```
- [ ] Check if event cancelled by other plugin

### Quick Fixes

- [ ] Add `@SubscribeEvent` annotation
- [ ] Use correct event type
- [ ] Set `ignoreCancelled = false` to listen to cancelled events
- [ ] Restart server

---

## Section G: Thread Safety Errors

**Symptom**: ConcurrentModificationException, thread errors

### Diagnostic Steps

1. **Check Bukkit API in async**:
```bash
grep -r "submit(async = true)" src/ -A 5
```
- [ ] Verify no Bukkit API calls in async blocks

2. **Check collection modification**:
```kotlin
// WRONG
for (player in players) {
    players.remove(player)  // ConcurrentModificationException!
}

// CORRECT
players.removeIf { condition }
```
- [ ] Verify safe collection operations

3. **Check shared state**:
```kotlin
// WRONG
var counter = 0
submit(async = true) { counter++ }  // Race condition!

// CORRECT
val counter = AtomicInteger(0)
submit(async = true) { counter.incrementAndGet() }
```
- [ ] Verify thread-safe data structures

### Quick Fixes

- [ ] Move Bukkit API to main thread: `submit(async = false) { }`
- [ ] Use safe collection operations
- [ ] Use thread-safe data structures
- [ ] Add synchronization

---

## Section H: Initialization Errors

**Symptom**: "lateinit property X has not been initialized"

### Diagnostic Steps

1. **Check initialization timing**:
```kotlin
// WRONG
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    init {
        dependency.doSomething()  // Too early!
    }
}

// CORRECT
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    @PostConstruct
    fun init() {
        dependency.doSomething()  // Safe
    }
}
```
- [ ] Verify using `@PostConstruct`, not `init`

2. **Check injection success**:
```bash
grep -r "@Component" src/ | grep "MyService"
```
- [ ] Verify dependency has `@Component` annotation

### Quick Fixes

- [ ] Move to `@PostConstruct`
- [ ] Add `@Component` to dependency
- [ ] Verify IoC module installed

---

## Section I: General Diagnostic Steps

### Step 1: Collect Information

- [ ] Full error message and stack trace
- [ ] TabooLib version: `grep "taboolib =" build.gradle.kts`
- [ ] Kotlin version: `grep "kotlin" build.gradle.kts`
- [ ] Server version and platform (Bukkit/Paper/etc.)
- [ ] Relevant code snippets

### Step 2: Check Basics

- [ ] Plugin loads without errors
- [ ] All dependencies installed
- [ ] Build succeeds: `./gradlew build`
- [ ] No conflicting plugins

### Step 3: Isolate Issue

- [ ] Minimal reproduction case
- [ ] Test with clean server
- [ ] Test with minimal plugin

### Step 4: Verify Against Source

- [ ] Check TabooLib source code for API
- [ ] Check TabooLib wiki for documentation
- [ ] Check user's project for existing patterns

---

## Quick Reference: Common Fixes

| Error | Quick Fix |
|-------|-----------|
| ClassNotFoundException | Add module with `taboo()` |
| No bean found | Add `@Component` |
| Config null | Move to ENABLE lifecycle |
| Command not working | Add `@CommandHeader` and `@CommandBody` |
| Listener not firing | Add `@SubscribeEvent` |
| Scheduler error | Move to ACTIVE lifecycle |
| Thread error | Use correct thread for Bukkit API |
| lateinit error | Move to `@PostConstruct` |

---

## Output Template

After completing diagnostic steps:

```markdown
## Diagnostic Summary

**Error Type**: [error category]

**Root Cause**: [identified cause]

**Diagnostic Steps Completed**:
- [✅/❌] Step 1
- [✅/❌] Step 2
- [✅/❌] Step 3

**Recommended Fix**:
[Minimal fix to resolve issue]

**Verification Steps**:
1. [How to verify fix worked]
2. [Expected behavior after fix]

**Additional Notes**:
[Any version-specific or edge case notes]
```

---

## References

- Related cards: `10_troubleshooting.md`
- Related recipes: `fix_ioc_injection_error.md`, `fix_lifecycle_error.md`
- Related tests: `compile_checklist.md`
