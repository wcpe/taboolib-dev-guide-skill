# Recipe: Create Scheduler Task

## Use When

- Need to run periodic background tasks
- Implementing auto-save or cleanup jobs
- Scheduling delayed actions

## Required Checks

- [ ] Verify task should run in ACTIVE lifecycle (not ENABLE)
- [ ] Determine if task should be async (IO) or sync (Bukkit API)
- [ ] Check if periodic task needs cancellation on disable

## Implementation Steps

### Step 1: Create Scheduler Manager

**File**: `src/main/kotlin/<package>/scheduler/SchedulerManager.kt`
```kotlin
package com.example.plugin.scheduler

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.submit
import taboolib.common.platform.PlatformTask

object SchedulerManager {
    
    private var autoSaveTask: PlatformTask? = null
    
    // Start tasks when scheduler ready
    @Awake(LifeCycle.ACTIVE)
    fun startTasks() {
        // Periodic auto-save (every 5 minutes)
        autoSaveTask = submit(delay = 6000, period = 6000, async = true) {
            database.saveAll()
            submit(async = false) {
                console.sendMessage("§aAuto-saved!")
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
}
```

### Step 2: Async Task with Callback

```kotlin
fun loadPlayerDataAsync(player: Player) {
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
```

### Step 3: Sequential Async Operations

```kotlin
import taboolib.common.platform.function.submitChain

fun processTransaction(player: Player, amount: Int) {
    submitChain {
        // Step 1: Async database query
        val balance = database.getBalance(player.uniqueId)
        
        // Step 2: Main thread validation
        sync {
            if (balance < amount) {
                player.sendMessage("§cInsufficient balance!")
                return@submitChain  // Exit chain
            }
        }
        
        // Step 3: Async database update
        async {
            database.deductBalance(player.uniqueId, amount)
        }
        
        // Step 4: Main thread confirmation
        sync {
            player.sendMessage("§aPurchase successful!")
        }
    }
}
```

## Verification Steps

1. Build: `./gradlew build` → success
2. Start server → check logs for task execution
3. Wait for periodic task → verify it executes at correct interval
4. Stop server → verify tasks cancelled cleanly

## References

- Card: `07_scheduler.md`
- Related cards: `02_lifecycle.md` (ACTIVE timing)
