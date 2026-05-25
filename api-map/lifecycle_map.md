# TabooLib Lifecycle Map

**⚠️ WARNING**: Always verify lifecycle behavior against user's TabooLib version.

## Lifecycle Stages

**Package**: `taboolib.common.LifeCycle`

```kotlin
enum class LifeCycle {
    NONE,       // Not started
    CONST,      // Static initialization
    INIT,       // Plugin instantiation
    LOAD,       // Plugin load phase
    ENABLE,     // Plugin enable phase
    ACTIVE,     // Server fully started
    DISABLE     // Plugin unload phase
}
```

---

## Stage Timing & Capabilities

| Stage | Timing | Plugin Instance | Config | Database | Scheduler | Bukkit API |
|-------|--------|----------------|--------|----------|-----------|------------|
| **NONE** | Not started | ❌ | ❌ | ❌ | ❌ | ❌ |
| **CONST** | Before plugin class instantiation | ❌ | ❌ | ❌ | ❌ | ❌ |
| **INIT** | Plugin main class constructor | ✅ | ❌ | ❌ | ❌ | ⚠️ Limited |
| **LOAD** | Plugin onLoad() | ✅ | ⚠️ Partial | ❌ | ❌ | ⚠️ Limited |
| **ENABLE** | Plugin onEnable() | ✅ | ✅ | ✅ | ⚠️ Unsafe | ✅ |
| **ACTIVE** | After scheduler starts | ✅ | ✅ | ✅ | ✅ | ✅ |
| **DISABLE** | Plugin onDisable() | ✅ | ✅ | ⚠️ Closing | ❌ | ⚠️ Limited |

**Legend**:
- ✅ Safe to use
- ⚠️ Limited or unsafe
- ❌ Not available

---

## Stage Details

### CONST (Static Initialization)
**Timing**: Before plugin class instantiation, during static block execution

**Available**:
- Static initialization
- Class loading
- Early registration

**NOT Available**:
- Plugin instance
- Config files
- Database
- Scheduler
- Bukkit API

**Use Cases**:
- Early framework initialization
- Static data preparation
- Class registration

**Example**:
```kotlin
@Awake(LifeCycle.CONST)
fun onConst() {
    println("Early initialization")
    // No plugin instance, config, or Bukkit API available
}
```

---

### INIT (Plugin Instantiation)
**Timing**: Plugin main class constructor execution

**Available**:
- Plugin instance (`pluginId`, `pluginVersion`)
- Limited Bukkit API

**NOT Available**:
- Config files (not loaded yet)
- Database (not connected yet)
- Scheduler (not started yet)

**Use Cases**:
- Plugin instance setup
- Early service registration

**Example**:
```kotlin
@Awake(LifeCycle.INIT)
fun onInit() {
    val id = pluginId  // ← Available
    println("Plugin $id initializing")
}
```

---

### LOAD (Plugin Load Phase)
**Timing**: During Bukkit's onLoad() phase

**Available**:
- Plugin instance
- Partial config (may not be fully loaded)
- Limited Bukkit API

**NOT Available**:
- Database (not connected yet)
- Scheduler (not started yet)
- Most Bukkit API (server not fully started)

**Use Cases**:
- Early world/plugin registration
- Pre-enable setup

**Example**:
```kotlin
@Awake(LifeCycle.LOAD)
fun onLoad() {
    println("Plugin loading")
}
```

---

### ENABLE (Plugin Enable Phase)
**Timing**: During Bukkit's onEnable() phase

**Available**:
- Plugin instance
- Config files (fully loaded)
- Database (can connect)
- Bukkit API (safe to use)

**NOT Available / Unsafe**:
- Scheduler (not fully initialized - use ACTIVE instead)

**Use Cases**:
- Load configuration
- Connect database
- Register commands/listeners
- Initialize services

**Example**:
```kotlin
@Awake(LifeCycle.ENABLE)
fun onEnable() {
    config.reload()
    database.connect()
    println("Plugin enabled")
}
```

**⚠️ WARNING**: Don't schedule periodic tasks here - use ACTIVE instead.

---

### ACTIVE (Server Fully Started)
**Timing**: After scheduler fully initialized

**Available**:
- Everything from ENABLE
- Scheduler (fully safe)

**Use Cases**:
- Start periodic tasks
- Schedule delayed actions
- Background jobs

**Example**:
```kotlin
@Awake(LifeCycle.ACTIVE)
fun onActive() {
    submit(period = 20) {  // ← Safe: scheduler ready
        // Periodic task
    }
}
```

**✅ RECOMMENDED**: Use this for all scheduler tasks.

---

### DISABLE (Plugin Unload Phase)
**Timing**: During Bukkit's onDisable() phase

**Available**:
- Plugin instance
- Config files
- Limited Bukkit API

**NOT Available / Unsafe**:
- Database (may be closing)
- Scheduler (stopped)

**Use Cases**:
- Save data
- Close connections
- Cancel tasks
- Cleanup resources

**Example**:
```kotlin
@Awake(LifeCycle.DISABLE)
fun onDisable() {
    task?.cancel()
    database.close()
    println("Plugin disabled")
}
```

---

## Execution Order Rules

### Between Stages
Stages execute in order: CONST → INIT → LOAD → ENABLE → ACTIVE → DISABLE

### Within Same Stage
**⚠️ CRITICAL**: Multiple `@Awake` methods in the same lifecycle have **undefined execution order**.

**Wrong** (order not guaranteed):
```kotlin
@Awake(LifeCycle.ENABLE)
fun initDatabase() {
    database.connect()
}

@Awake(LifeCycle.ENABLE)
fun loadData() {
    database.query()  // ← May run before initDatabase()!
}
```

**Correct** (guaranteed order):
```kotlin
@Awake(LifeCycle.ENABLE)
fun initialize() {
    initDatabase()  // ← Guaranteed order
    loadData()
}

private fun initDatabase() {
    database.connect()
}

private fun loadData() {
    database.query()
}
```

---

## IoC Lifecycle Integration

**IoC Bean Lifecycle** (when using `database-ioc` module):

1. **ENABLE**: Beans scanned and registered
2. **ACTIVE**: Beans instantiated and dependencies injected
3. **ACTIVE**: `@PostConstruct` callbacks executed
4. **ACTIVE**: `@PostEnable` callbacks executed (if defined)
5. **DISABLE**: `@PreDestroy` callbacks executed (SINGLETON/PLAYER only)

**⚠️ CRITICAL**: Prototype beans skip `@PreDestroy` callbacks.

---

## Common Lifecycle Mistakes

### ❌ Mistake 1: Config in CONST
```kotlin
@Awake(LifeCycle.CONST)
fun loadConfig() {
    val value = config["key"]  // ← Config not loaded yet!
}
```
**Fix**: Use ENABLE or later.

### ❌ Mistake 2: Scheduler in ENABLE
```kotlin
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) { }  // ← Scheduler not fully ready!
}
```
**Fix**: Use ACTIVE.

### ❌ Mistake 3: Assuming execution order
```kotlin
@Awake(LifeCycle.ENABLE)
fun first() { }

@Awake(LifeCycle.ENABLE)
fun second() { }  // ← May run before first()!
```
**Fix**: Use single method.

---

## Quick Reference

**Need to...**
- Access plugin instance? → INIT or later
- Load config? → ENABLE or later
- Connect database? → ENABLE or later
- Schedule tasks? → ACTIVE
- Cleanup resources? → DISABLE

---

## Verification

Check user's existing lifecycle usage:
```bash
grep -r "@Awake" src/
```

Extract patterns and match their style.

---

## References

- Source code: `taboolib/common/src/main/java/taboolib/common/LifeCycle.java`
- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/Awake.kt`
- Related cards: `02_lifecycle.md`
- Related recipes: `fix_lifecycle_error.md`
