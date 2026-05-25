# Card 02: Lifecycle Management

## When to Use

Load this card when:
- User asks about "@Awake", "生命周期", "enable", "disable"
- Need to initialize resources at specific plugin stages
- Troubleshooting "too early initialization" errors
- Need to access plugin instance or schedule tasks

## Core Idea

TabooLib uses a 7-stage lifecycle system (CONST → INIT → LOAD → ENABLE → ACTIVE → DISABLE) controlled by `@Awake` annotation. Each stage has specific timing and capabilities. **CONST runs before plugin instantiation, ENABLE is for initialization, ACTIVE is after scheduler starts, DISABLE is for cleanup**. Multiple `@Awake` methods in the same lifecycle have undefined execution order.

## ⚠️ Critical Plugin Main Class Rules

### 🚫 插件主类必须是 Kotlin `object`，不能是 `class`

**正确模式**:
```kotlin
import taboolib.common.platform.Plugin

object MyPlugin : Plugin()  // ✅ Kotlin object
```

**错误模式**:
```kotlin
// ❌ 不要使用 class
class MyPlugin : Plugin()

// ❌ 不要继承 JavaPlugin
class MyPlugin : JavaPlugin()

// ❌ 不要自己创建 JavaPlugin 子类
class MyPlugin : org.bukkit.plugin.java.JavaPlugin()
```

**Why**: TabooLib 内部管理 `BukkitPlugin` 作为唯一的 `JavaPlugin` 入口点。你的插件主类应该是 Kotlin `object` 继承 `Plugin()`，而不是自己创建 `JavaPlugin` 实例。

### 🚫 不要覆盖 `onEnable()` / `onDisable()`

**正确模式**:
```kotlin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin

object MyPlugin : Plugin() {
    
    @Awake(LifeCycle.ENABLE)
    fun enable() {  // ✅ 使用 @Awake
        // 初始化逻辑
    }
    
    @Awake(LifeCycle.DISABLE)
    fun disable() {  // ✅ 使用 @Awake
        // 清理逻辑
    }
}
```

**错误模式**:
```kotlin
object MyPlugin : Plugin() {
    
    override fun onEnable() {  // ❌ 不要覆盖 onEnable
        // TabooLib 不会调用这个方法
    }
    
    override fun onDisable() {  // ❌ 不要覆盖 onDisable
        // TabooLib 不会调用这个方法
    }
}
```

### 🚫 获取插件实例：使用 `bukkitPlugin`，不要用 `BukkitPlugin.getInstance()`

**正确模式**:
```kotlin
import taboolib.platform.util.bukkitPlugin

// ✅ 使用 TabooLib 提供的顶层属性
val plugin = bukkitPlugin

// 用于需要 Plugin 参数的 Bukkit API
Bukkit.getScheduler().runTask(bukkitPlugin, Runnable { })
```

**错误模式**:
```kotlin
// ❌ 不要直接调用 BukkitPlugin.getInstance()
val plugin = BukkitPlugin.getInstance()
```

**Why**: `bukkitPlugin` 是 TabooLib 提供的规范访问器，确保跨平台兼容性。

### ⚠️ ENABLE 生命周期启动准备例外

**重要例外**: 为了确保服务器启动完成后插件数据库、连接池、表结构、必要 schema/check/migration 正确就绪，`@Awake(LifeCycle.ENABLE)` 生命周期中的**数据库初始化、连通性检查、必要建表或迁移检查**，可以按启动阶段在主线程执行。

**允许的启动准备操作**:
```kotlin
@Awake(LifeCycle.ENABLE)
fun initDatabase() {
    // ✅ 启动阶段允许：数据库连接初始化
    database.connect()
    
    // ✅ 启动阶段允许：连通性检查
    if (!database.testConnection()) {
        error("Database connection failed!")
    }
    
    // ✅ 启动阶段允许：必要建表/迁移检查
    database.createTablesIfNotExists()
    database.runMigrations()
}
```

**⚠️ 这个例外仅限启动准备阶段，不得扩展到运行期热路径**。启动完成后，所有数据库操作必须在异步线程执行。

## Lifecycle Stages

**Package**: `taboolib.common.LifeCycle`

```
NONE     - Not started (unused)
CONST    - Static initialization (before plugin class instantiation)
INIT     - Plugin main class instantiation
LOAD     - Plugin load phase (onLoad)
ENABLE   - Plugin enable phase (onEnable)
ACTIVE   - Server fully started (scheduler available)
DISABLE  - Plugin unload phase (onDisable)
```

### Stage Capabilities

| Stage | Plugin Instance | Config | Database | Scheduler | Bukkit API |
|-------|----------------|--------|----------|-----------|------------|
| CONST | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No |
| INIT | ✅ Yes | ❌ No | ❌ No | ❌ No | ⚠️ Limited |
| LOAD | ✅ Yes | ⚠️ Partial | ❌ No | ❌ No | ⚠️ Limited |
| ENABLE | ✅ Yes | ✅ Yes | ✅ Yes | ⚠️ Unsafe | ✅ Yes |
| ACTIVE | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| DISABLE | ✅ Yes | ✅ Yes | ⚠️ Closing | ❌ No | ⚠️ Limited |

## Recommended Pattern

### @Awake Annotation

**Package**: `taboolib.common.platform.Awake`

```kotlin
@Retention(AnnotationRetention.RUNTIME)
annotation class Awake(val value: LifeCycle = LifeCycle.CONST)
```

### Basic Usage

```kotlin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info

object MyInitializer {
    
    @Awake(LifeCycle.CONST)
    fun onConst() {
        // Runs before plugin instantiation
        // No plugin instance, config, or Bukkit API available
        info("CONST stage")
    }
    
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        // Runs during plugin enable
        // Config loaded, database available, Bukkit API safe
        info("ENABLE stage")
    }
    
    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        // Runs after scheduler starts
        // Safe to schedule tasks
        info("ACTIVE stage")
    }
    
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        // Runs during plugin disable
        // Cleanup resources here
        info("DISABLE stage")
    }
}
```

### Plugin Instance Access

**Package**: `taboolib.common.platform.function.Plugin.kt`

```kotlin
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import taboolib.platform.util.bukkitPlugin

// Get plugin ID (available from INIT onwards)
val id = pluginId  // Returns plugin name from plugin.yml

// Get plugin version (available from INIT onwards)
val version = pluginVersion  // Returns version from plugin.yml

// Get Bukkit plugin instance (Bukkit platform only, available from INIT onwards)
val plugin = bukkitPlugin  // Returns JavaPlugin instance
```

### Execution Order Rules

**Within same lifecycle**:
```kotlin
object MyInitializer {
    @Awake(LifeCycle.ENABLE)
    fun first() { }  // ← Order undefined!
    
    @Awake(LifeCycle.ENABLE)
    fun second() { }  // ← May run before first()!
}
```

**Solution**: Use single method if order matters
```kotlin
object MyInitializer {
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        first()   // ← Guaranteed order
        second()
    }
    
    private fun first() { }
    private fun second() { }
}
```

## Common Mistakes

### ❌ Mistake 1: Accessing config in CONST stage
```kotlin
@Awake(LifeCycle.CONST)
fun loadConfig() {
    val value = config["key"]  // ← Config not loaded yet!
}
```
**Why wrong**: Config files are loaded during INIT/LOAD, not available in CONST.

**Fix**: Use ENABLE stage
```kotlin
@Awake(LifeCycle.ENABLE)
fun loadConfig() {
    val value = config["key"]  // ← Safe: config loaded
}
```

### ❌ Mistake 2: Scheduling tasks in ENABLE stage
```kotlin
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) {  // ← Scheduler not fully initialized!
        // May cause timing issues
    }
}
```
**Why wrong**: Scheduler may not be fully initialized during ENABLE.

**Fix**: Use ACTIVE stage
```kotlin
@Awake(LifeCycle.ACTIVE)
fun startTasks() {
    submit(period = 20) {  // ← Safe: scheduler ready
        // Runs reliably
    }
}
```

### ❌ Mistake 3: Assuming execution order
```kotlin
object MyInitializer {
    @Awake(LifeCycle.ENABLE)
    fun initDatabase() {
        database.connect()
    }
    
    @Awake(LifeCycle.ENABLE)
    fun loadData() {
        database.query()  // ← May run before initDatabase()!
    }
}
```
**Why wrong**: Multiple @Awake methods in same lifecycle have undefined order.

**Fix**: Use single method or explicit dependencies
```kotlin
object MyInitializer {
    @Awake(LifeCycle.ENABLE)
    fun initialize() {
        initDatabase()  // ← Guaranteed order
        loadData()
    }
    
    private fun initDatabase() { database.connect() }
    private fun loadData() { database.query() }
}
```

### ❌ Mistake 4: Not cleaning up in DISABLE
```kotlin
@Awake(LifeCycle.ENABLE)
fun startConnection() {
    connection = openConnection()
    // No cleanup! ← Resource leak
}
```
**Why wrong**: Resources opened during ENABLE should be closed during DISABLE.

**Fix**: Add DISABLE handler
```kotlin
private var connection: Connection? = null

@Awake(LifeCycle.ENABLE)
fun startConnection() {
    connection = openConnection()
}

@Awake(LifeCycle.DISABLE)
fun stopConnection() {
    connection?.close()  // ← Cleanup
    connection = null
}
```

### ❌ Mistake 5: Using @Awake with parameters
```kotlin
@Awake(LifeCycle.ENABLE)
fun onEnable(plugin: Plugin) {  // ← Parameters not supported!
    // Will fail to invoke
}
```
**Why wrong**: @Awake methods must be parameterless.

**Fix**: Remove parameters, use global access
```kotlin
@Awake(LifeCycle.ENABLE)
fun onEnable() {  // ← No parameters
    val plugin = bukkitPlugin  // Access via function
}
```

## Minimal Example

**Complete lifecycle management**:

```kotlin
package com.example.plugin

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit

object ExamplePlugin : Plugin() {
    
    // Early initialization (before plugin instance)
    @Awake(LifeCycle.CONST)
    fun onConst() {
        info("Stage 1: CONST - Early initialization")
    }
    
    // Plugin enabled (config available)
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        info("Stage 2: ENABLE - Loading config and database")
        // Load config, connect database
    }
    
    // Scheduler ready (safe to schedule tasks)
    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        info("Stage 3: ACTIVE - Starting scheduled tasks")
        submit(period = 20) {
            // Periodic task
        }
    }
    
    // Plugin disabled (cleanup)
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        info("Stage 4: DISABLE - Cleaning up resources")
        // Close connections, save data
    }
}
```

## Checklist

Before using @Awake:

- [ ] Verify which resources are available at target lifecycle stage
- [ ] Check if multiple @Awake methods need guaranteed order (use single method)
- [ ] Confirm method is parameterless
- [ ] Verify ENABLE stage resources have DISABLE cleanup
- [ ] Check if scheduler tasks should use ACTIVE instead of ENABLE
- [ ] Confirm config access happens in ENABLE or later
- [ ] Verify database operations happen in ENABLE or later

After adding @Awake methods:

- [ ] Check server logs for expected lifecycle messages
- [ ] Verify resources initialize in correct order
- [ ] Confirm scheduled tasks start reliably
- [ ] Test plugin reload (DISABLE → ENABLE cycle)
- [ ] Verify no resource leaks after disable

## Version-Specific Notes

**TabooLib 6.2.0+**:
- All 7 lifecycle stages supported
- @Awake execution order within same stage is undefined
- IoC @PostConstruct runs during ACTIVE stage (after ENABLE)

**TabooLib 6.1.x**:
- Similar lifecycle stages
- Check wiki for any timing differences

## Troubleshooting

**Error: "Config not loaded"**
- Cause: Accessing config in CONST or early INIT
- Fix: Move to ENABLE stage

**Error: "Scheduler not available"**
- Cause: Scheduling tasks in ENABLE
- Fix: Move to ACTIVE stage

**Error: "Plugin instance null"**
- Cause: Accessing plugin instance in CONST
- Fix: Move to INIT or later

**Error: "Method not invoked"**
- Cause: Method has parameters
- Fix: Remove parameters, make method parameterless

## References

- Source code: `taboolib/common/src/main/java/taboolib/common/LifeCycle.java`
- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/Awake.kt`
- Related cards: `05_ioc.md` (@PostConstruct timing), `07_scheduler.md` (ACTIVE stage)
