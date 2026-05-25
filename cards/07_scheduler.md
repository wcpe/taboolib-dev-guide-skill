# Card 07: Scheduler & Async Tasks

## When to Use

Load this card when:
- User asks about "scheduler", "async", "submit", "延迟", "定时任务"
- Need to run tasks asynchronously or with delay/period
- Implementing background jobs or periodic updates
- Troubleshooting thread safety or timing issues

## Core Idea

TabooLib provides `submit()` function for task scheduling with sync/async execution, delay, and period support. **Async tasks run on separate thread** - safe for IO but cannot use Bukkit API directly. **Sync tasks run on main thread** - safe for Bukkit API but should not block. **Scheduler is available from ACTIVE lifecycle onwards**.

## ⚠️ Critical Thread Safety Model

### Bukkit 是单主线程模型

所有代码为了高性能都应采用**双架构**：异步执行业务/IO → 结果提交主线程运行 Bukkit API。

### 🚫 主线程禁止操作

**命令、同步事件、GUI 回调默认都在主线程，以下操作会阻塞服务器**:

- ❌ 数据库查询/写入（SQL、ORM、EasyQuery、JDBC）
- ❌ Redis 读写
- ❌ HTTP / 网络请求
- ❌ 文件 IO（大文件读写）
- ❌ 阻塞等待：`Thread.sleep()`、`Future.get()`、`Future.join()`、`CountDownLatch.await()`
- ❌ 重计算：大量数据遍历、批量序列化、复杂聚合

### 🚫 异步线程禁止操作

**异步线程中不得直接修改 Bukkit 运行时对象**:

- ❌ `Player` 写操作（`sendMessage`、`teleport`、`kick`、`setHealth`）
- ❌ `Inventory` 写操作（`addItem`、`removeItem`、`setItem`、`clear`）
- ❌ `Equipment` 写操作（`setHelmet`、`setChestplate`、`setLeggings`、`setBoots`）
- ❌ `World` 操作（`spawn`、`setBlock`、`generateTree`、`createExplosion`）
- ❌ `Entity` 操作（`remove`、`setVelocity`、`setCustomName`）
- ❌ `Scoreboard` / `BossBar` / `Team` 操作
- ❌ `player.openInventory()` / `player.closeInventory()`

### 入口点线程规则

| 入口点 | 线程 | 规则 |
|--------|------|------|
| **命令** (`@CommandBody`) | 主线程 | 只做参数校验和权限校验，立即 `submitAsync` 委托 service |
| **同步事件** (`PlayerJoinEvent`) | 主线程 | 轻量处理后 `submitAsync` 委托 service |
| **异步事件** (`AsyncPlayerPreLoginEvent`) | 异步线程 | 需要 Bukkit API 时 `submit` 切回主线程 |
| **GUI 回调** (`InventoryClickEvent`) | 主线程 | 参数校验后 `submitAsync` 委托 service |
| **Placeholder 变量** (`onPlaceholderRequest`) | 主线程（高概率） | 禁止 DB/Redis/网络/文件 IO/阻塞等待，必须读取预计算快照或轻量缓存 |
| **插件 ENABLE** (`@Awake(LifeCycle.ENABLE)`) | 主线程 | 允许数据库初始化、连接校验、必要建表/迁移检查（启动准备例外） |
| **`submit { }`** | 主线程 | 仅用于 Bukkit API 操作 |
| **`submitAsync { }`** | 异步线程 | 用于 IO 和业务逻辑 |

### 推荐架构模式

```kotlin
// 命令入口（主线程）→ 异步业务 → 主线程 Bukkit API
execute<Player> { sender, context, _ ->
    val target = context[ARG_TARGET]
    submitAsync {
        // ✅ 异步执行业务逻辑和 IO
        val result = rewardService.grantReward(sender.name, target)
        
        submit {
            // ✅ 主线程执行 Bukkit API
            val player = Bukkit.getPlayerExact(sender.name) ?: return@submit
            player.sendMessage(if (result.success) "奖励发放成功" else "奖励发放失败")
        }
    }
}
```

### Inventory 特别约束

**所有 Inventory 写操作必须在主线程**。异步操作产生物品后必须 `submit { }` 切回主线程，并在主线程内检查 `player.isOnline` 后再操作背包。

```kotlin
submitAsync {
    // ✅ 异步生成物品
    val items = generateRewardItems()
    
    submit {
        // ✅ 主线程检查在线状态
        val player = Bukkit.getPlayerExact(playerName) ?: return@submit
        if (!player.isOnline) return@submit
        
        // ✅ 主线程操作背包
        player.inventory.addItem(*items.toTypedArray())
    }
}
```

### Placeholder / HUD / Scoreboard 热路径规则

**`PlaceholderExpansion#onRequest(...)`、`onPlaceholderRequest(...)`、TabooLib placeholder provider 必须按高频、主线程高概率、延迟敏感热路径处理**。

**禁止操作**:
- ❌ 数据库、Redis、HTTP、文件 IO
- ❌ 阻塞等待（`Future.get/join`、`await`、`sleep`）
- ❌ 同步 cache miss 回源
- ❌ 全量排行重算或大规模聚合

**推荐模式**:
```kotlin
// ✅ 异步预计算 / 启动预热 / 后台刷新
@Awake(LifeCycle.ACTIVE)
fun startRefresh() {
    submit(period = 200, async = true) {  // 每 10 秒刷新
        val newSnapshot = database.calculateRanking()
        rankingSnapshot = newSnapshot  // 更新内存快照
    }
}

// ✅ Placeholder 入口仅负责格式化输出
override fun onRequest(player: OfflinePlayer, params: String): String? {
    return rankingSnapshot[player.uniqueId]?.toString() ?: "N/A"
}
```

**Cache miss 时应快速降级为 fallback / stale snapshot / `null`，必要时触发异步刷新；绝不能阻塞当前 placeholder 调用**。

### Service 层线程规则

- Service 不关心线程调度，线程切换由调用方负责
- Service 方法默认假设在异步线程被调用
- Service 内部禁止直接调用 Bukkit API（通过 adapter/bridge 隔离）

### ⚠️ 避免"异步套异步"

如果外层入口已经在异步线程中，后续 helper 不应再无条件包一层新的 `submitAsync { }`。

**错误模式**:
```kotlin
// 命令已经 submitAsync
submitAsync {
    // ❌ helper 又无条件 submitAsync - 无意义排队
    helperService.doSomething()
}

// helperService 内部
fun doSomething() {
    submitAsync {  // ❌ 重复异步边界
        database.query()
    }
}
```

**正确模式**:
```kotlin
// 命令 submitAsync 一次
submitAsync {
    // ✅ helper 直接执行，不再套异步
    helperService.doSomething()
}

// helperService 内部
fun doSomething() {
    // ✅ 假设调用方已在异步线程
    database.query()
}
```

**例外**: 只有需要独立任务边界、脱离当前生命周期、或显式隔离异常时，才允许再次调度。

## Recommended Pattern

### Basic Task Scheduling

**Package**: `taboolib.common.platform.function.submit`

```kotlin
import taboolib.common.platform.function.submit

// Sync task (main thread)
submit {
    player.sendMessage("Hello!")  // ← Safe: Bukkit API on main thread
}

// Async task (separate thread)
submit(async = true) {
    val data = database.query()  // ← Safe: IO on async thread
}

// Delayed task (20 ticks = 1 second)
submit(delay = 20) {
    player.sendMessage("1 second later")
}

// Periodic task (every 20 ticks)
submit(period = 20) {
    // Runs every second
    updateScoreboard()
}

// Delayed + periodic
submit(delay = 20, period = 20) {
    // Starts after 1 second, then repeats every second
}
```

### Async with Main Thread Callback

```kotlin
submit(async = true) {
    // Step 1: IO on async thread
    val data = database.loadPlayerData(uuid)
    
    // Step 2: Back to main thread for Bukkit API
    submit(async = false) {
        player.sendMessage("Loaded: $data")
        player.inventory.addItem(data.items)
    }
}
```

### Task Cancellation

```kotlin
// Get task handle
val task = submit(period = 20) {
    println("Periodic task")
}

// Cancel task later
task.cancel()
```

### @Schedule Annotation (Declarative)

**Package**: `taboolib.common.platform.Schedule`

```kotlin
import taboolib.common.platform.Schedule

object ScheduledTasks {
    
    @Schedule(period = 20, async = true)
    fun autoSave() {
        // Runs every second on async thread
        database.saveAll()
    }
    
    @Schedule(delay = 100, period = 100)
    fun updateScoreboard() {
        // Runs every 5 seconds on main thread
        onlinePlayers.forEach { updatePlayerScoreboard(it) }
    }
}
```

### submitChain (Sequential Async Tasks)

**Package**: `taboolib.common.platform.function.submitChain`

```kotlin
import taboolib.common.platform.function.submitChain

submitChain {
    // Step 1: Async by default
    val data = database.query()
    
    // Step 2: Switch to main thread
    sync {
        player.sendMessage("Data: $data")
    }
    
    // Step 3: Back to async
    async {
        database.save(data)
    }
    
    // Step 4: Final main thread action
    sync {
        player.sendMessage("Saved!")
    }
}
```

### Thread Safety Rules

**Main Thread (sync)**:
- ✅ Bukkit API (player, world, inventory, etc.)
- ✅ Fast operations (< 50ms)
- ❌ Database queries
- ❌ Network requests
- ❌ File IO
- ❌ Heavy computations

**Async Thread**:
- ✅ Database queries
- ✅ Network requests
- ✅ File IO
- ✅ Heavy computations
- ❌ Bukkit API (player, world, inventory, etc.)
- ⚠️ Thread-safe data structures only

## Common Mistakes

### ❌ Mistake 1: Bukkit API in async task
```kotlin
submit(async = true) {
    player.sendMessage("Hello!")  // ← CRASH! Bukkit API on async thread
    player.inventory.addItem(item)  // ← CRASH!
}
```
**Why wrong**: Bukkit API is not thread-safe. Async access causes crashes.

**Fix**: Switch to main thread for Bukkit API
```kotlin
submit(async = true) {
    val data = database.query()
    submit(async = false) {  // ← Back to main thread
        player.sendMessage("Hello!")
        player.inventory.addItem(item)
    }
}
```

### ❌ Mistake 2: Blocking IO on main thread
```kotlin
submit {  // async = false (default)
    val data = database.query("SELECT * FROM players")  // ← Blocks main thread!
    player.sendMessage("Done")
}
```
**Why wrong**: Database queries block main thread, causing server lag.

**Fix**: Use async for IO
```kotlin
submit(async = true) {
    val data = database.query("SELECT * FROM players")
    submit(async = false) {
        player.sendMessage("Done")
    }
}
```

### ❌ Mistake 3: Scheduling tasks in ENABLE lifecycle
```kotlin
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) {  // ← Scheduler not fully initialized!
        // May have timing issues
    }
}
```
**Why wrong**: Scheduler may not be fully initialized during ENABLE.

**Fix**: Use ACTIVE lifecycle
```kotlin
@Awake(LifeCycle.ACTIVE)
fun startTasks() {
    submit(period = 20) {  // ← Safe: scheduler ready
        // Runs reliably
    }
}
```

### ❌ Mistake 4: Not cancelling periodic tasks
```kotlin
@Awake(LifeCycle.ENABLE)
fun startTask() {
    submit(period = 20) {
        // Periodic task
    }
    // No cleanup! ← Task continues after plugin disable
}
```
**Why wrong**: Periodic tasks continue running after plugin disable, causing errors.

**Fix**: Cancel in DISABLE lifecycle
```kotlin
private var task: PlatformTask? = null

@Awake(LifeCycle.ACTIVE)
fun startTask() {
    task = submit(period = 20) {
        // Periodic task
    }
}

@Awake(LifeCycle.DISABLE)
fun stopTask() {
    task?.cancel()
}
```

### ❌ Mistake 5: Race conditions with shared state
```kotlin
var counter = 0  // ← Shared mutable state

submit(async = true) {
    counter++  // ← Race condition!
}

submit(async = true) {
    counter++  // ← Race condition!
}
```
**Why wrong**: Multiple async tasks modifying shared state causes race conditions.

**Fix**: Use thread-safe structures or synchronization
```kotlin
val counter = AtomicInteger(0)  // ← Thread-safe

submit(async = true) {
    counter.incrementAndGet()  // ← Safe
}

// OR use synchronized block
var counter = 0
submit(async = true) {
    synchronized(this) {
        counter++  // ← Safe
    }
}
```

### ❌ Mistake 6: Nested async without reason
```kotlin
submit(async = true) {
    submit(async = true) {  // ← Unnecessary nesting
        database.query()
    }
}
```
**Why wrong**: Unnecessary nesting adds complexity without benefit.

**Fix**: Use single async block
```kotlin
submit(async = true) {
    database.query()  // ← Already on async thread
}
```

## Minimal Example

**Complete scheduler usage**:

```kotlin
package com.example.plugin

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.submit
import taboolib.common.platform.PlatformTask

object SchedulerExample {
    
    private var autoSaveTask: PlatformTask? = null
    
    // Start tasks when scheduler ready
    @Awake(LifeCycle.ACTIVE)
    fun startTasks() {
        // Periodic auto-save (every 5 minutes)
        autoSaveTask = submit(delay = 6000, period = 6000, async = true) {
            database.saveAll()
            submit(async = false) {
                console.sendMessage("Auto-saved!")
            }
        }
    }
    
    // Stop tasks on disable
    @Awake(LifeCycle.DISABLE)
    fun stopTasks() {
        autoSaveTask?.cancel()
    }
    
    // Declarative periodic task
    @Schedule(period = 20, async = false)
    fun updateScoreboard() {
        // Runs every second on main thread
        onlinePlayers.forEach { player ->
            updatePlayerScoreboard(player)
        }
    }
    
    // Example: Load player data on join
    fun loadPlayerData(player: Player) {
        submit(async = true) {
            // Step 1: Load from database (async)
            val data = database.loadPlayerData(player.uniqueId)
            
            // Step 2: Apply to player (main thread)
            submit(async = false) {
                player.sendMessage("Welcome back!")
                player.inventory.addItem(data.items)
            }
        }
    }
    
    // Example: Sequential async operations
    fun processPlayerAction(player: Player) {
        submitChain {
            // Step 1: Async database query
            val balance = database.getBalance(player.uniqueId)
            
            // Step 2: Main thread validation
            sync {
                if (balance < 100) {
                    player.sendMessage("Insufficient balance!")
                    return@submitChain  // Exit chain
                }
            }
            
            // Step 3: Async database update
            async {
                database.deductBalance(player.uniqueId, 100)
            }
            
            // Step 4: Main thread confirmation
            sync {
                player.sendMessage("Purchase successful!")
            }
        }
    }
}
```

## Checklist

Before using scheduler:

- [ ] Verify scheduler access happens in ACTIVE lifecycle or later
- [ ] Check if task should be async (IO/computation) or sync (Bukkit API)
- [ ] Confirm no Bukkit API in async tasks
- [ ] Verify no blocking IO in sync tasks
- [ ] Check if periodic tasks need cancellation in DISABLE
- [ ] Confirm thread-safe data structures for shared state
- [ ] Verify delay/period values are correct (20 ticks = 1 second)

After implementing scheduler:

- [ ] Test tasks execute at correct timing
- [ ] Verify async tasks don't crash with Bukkit API
- [ ] Confirm sync tasks don't cause server lag
- [ ] Test periodic tasks cancel on plugin disable
- [ ] Verify no race conditions with shared state
- [ ] Test submitChain sequences execute in order

## Version-Specific Notes

**TabooLib 6.2.0+**:
- `submit()` function for all scheduling
- `submitChain()` for sequential async operations
- `@Schedule` annotation for declarative tasks
- Folia support with automatic region-safe scheduling

**TabooLib 6.1.x**:
- Similar API, check wiki for differences

## Troubleshooting

**Error: "Scheduler not available"**
- Cause: Scheduling tasks in ENABLE or earlier
- Fix: Move to ACTIVE lifecycle

**Error: "ConcurrentModificationException"**
- Cause: Bukkit API access from async thread
- Fix: Switch to main thread with `submit(async = false)`

**Error: "Server lag"**
- Cause: Blocking IO on main thread
- Fix: Move IO to async thread

**Error: "Task continues after disable"**
- Cause: Not cancelling periodic tasks
- Fix: Cancel tasks in DISABLE lifecycle

**Error: "Race condition"**
- Cause: Shared mutable state in async tasks
- Fix: Use thread-safe structures or synchronization

## References

- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/function/Executor.kt`
- Related cards: `02_lifecycle.md` (ACTIVE timing), `06_listener.md` (async event handling)
- Related recipes: `create_scheduler_task.md`
