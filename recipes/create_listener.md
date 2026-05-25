# Recipe: Create Event Listener

## Use When

- Need to handle Bukkit/platform events
- Implementing event-driven features
- Reacting to player actions or server events

## Required Checks

- [ ] Verify correct event type (PlayerJoinEvent vs PlayerLoginEvent, etc.)
- [ ] Check if ignoreCancelled needed (respect protection plugins)
- [ ] Determine appropriate priority

## Implementation Steps

### Step 1: Create Listener File

**File**: `src/main/kotlin/<package>/listener/PlayerListener.kt`
```kotlin
package com.example.plugin.listener

import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.submit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerListener {
    
    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        event.joinMessage = "§a${player.name} joined"
        
        // Load data asynchronously
        submit(async = true) {
            val data = playerService.loadData(player.uniqueId)
            submit(async = false) {
                player.sendMessage("Welcome! Coins: ${data.coins}")
            }
        }
    }
    
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        event.quitMessage = "§c${player.name} left"
        
        // Save data asynchronously
        submit(async = true) {
            playerService.saveData(player.uniqueId)
        }
    }
}
```

### Step 2: Add Service Injection (if using IoC)

```kotlin
import taboolib.expansion.ioc.annotation.Resource

object PlayerListener {
    
    @Resource
    lateinit var playerService: PlayerService
    
    // Event handlers...
}
```

### Step 3: Handle Priority and Cancellation

```kotlin
// High priority (executes late, can override other plugins)
@SubscribeEvent(priority = EventPriority.HIGH)
fun onDamage(event: EntityDamageEvent) {
    if (event.entity is Player) {
        val player = event.entity as Player
        if (player.hasPermission("plugin.godmode")) {
            event.isCancelled = true
        }
    }
}

// Ignore cancelled events (respect protection plugins)
@SubscribeEvent(ignoreCancelled = true)
fun onBlockBreak(event: BlockBreakEvent) {
    // Only fires if event NOT cancelled
    rewardService.giveReward(event.player)
}
```

## Verification Steps

1. Build: `./gradlew build` → success
2. Trigger event (player join, block break, etc.)
3. Check logs or behavior for listener execution
4. Test with cancelled events (if using ignoreCancelled)

## References

- Card: `06_listener.md`
- Related cards: `07_scheduler.md` (async handling)
