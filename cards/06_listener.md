# Card 06: Event Listener System

## When to Use

Load this card when:
- User asks about "listener", "event", "@SubscribeEvent", "事件监听"
- Need to handle Bukkit/platform events
- Implementing event-driven features
- Troubleshooting listener not firing or event cancellation issues

## Core Idea

TabooLib provides `@SubscribeEvent` annotation for automatic event listener registration. Listeners support priority levels, cancellation handling, and cross-platform events. **Listeners are auto-registered** - no manual registration needed. **Event handlers run on main thread** - use async scheduler for IO operations.

## Recommended Pattern

### Basic Event Listener

**Package**: `taboolib.common.platform.event.SubscribeEvent`

```kotlin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerListener {
    
    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.sendMessage("Welcome!")
    }
    
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        // Cleanup player data
    }
}
```

### Event Priority

**Package**: `taboolib.common.platform.event.EventPriority`

```kotlin
enum class EventPriority {
    LOWEST,    // First to execute
    LOW,
    NORMAL,    // Default
    HIGH,
    HIGHEST,   // Last to execute
    MONITOR    // Read-only, should not modify event
}
```

**Usage**:
```kotlin
@SubscribeEvent(priority = EventPriority.HIGHEST)
fun onDamage(event: EntityDamageEvent) {
    // Executes after NORMAL priority listeners
}

@SubscribeEvent(priority = EventPriority.MONITOR)
fun onDamageMonitor(event: EntityDamageEvent) {
    // Read-only monitoring, don't modify event
    logDamageEvent(event)
}
```

### Ignore Cancelled Events

```kotlin
@SubscribeEvent(ignoreCancelled = true)
fun onBlockBreak(event: BlockBreakEvent) {
    // Only fires if event is NOT cancelled
    // Useful for features that should respect protection plugins
}

@SubscribeEvent(ignoreCancelled = false)  // Default
fun onBlockBreakMonitor(event: BlockBreakEvent) {
    // Fires even if event is cancelled
    // Useful for logging/statistics
}
```

### Event Cancellation

```kotlin
@SubscribeEvent(priority = EventPriority.HIGH)
fun onPlayerDamage(event: EntityDamageEvent) {
    if (event.entity is Player) {
        val player = event.entity as Player
        if (player.hasPermission("plugin.godmode")) {
            event.isCancelled = true  // ← Cancel event
        }
    }
}
```

### Platform-Specific Events

**Bukkit events**:
```kotlin
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.block.BlockBreakEvent

@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) {
    // Bukkit-specific event
}
```

**Cross-platform events** (TabooLib internal):
```kotlin
import taboolib.common.platform.event.ProxyEvent

class CustomEvent : ProxyEvent() {
    var data: String = ""
}

// Fire event
val event = CustomEvent()
event.data = "test"
event.call()  // Fire to all listeners

// Listen to event
@SubscribeEvent
fun onCustom(event: CustomEvent) {
    println("Custom event: ${event.data}")
}
```

### Async Event Handling

```kotlin
@SubscribeEvent
fun onPlayerJoin(event: PlayerJoinEvent) {
    val player = event.player
    
    // ⚠️ Event handler runs on main thread
    // Use async for IO operations
    submit(async = true) {
        val data = database.loadPlayerData(player.uniqueId)
        
        // Back to main thread for Bukkit API
        submit(async = false) {
            player.sendMessage("Loaded data: $data")
        }
    }
}
```

## Common Mistakes

### ❌ Mistake 1: Blocking IO in event handler
```kotlin
@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) {
    val data = database.query("SELECT * FROM players")  // ← Blocks main thread!
    event.player.sendMessage("Welcome!")
}
```
**Why wrong**: Event handlers run on main thread. Database/network IO blocks server.

**Fix**: Use async scheduler
```kotlin
@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) {
    val player = event.player
    submit(async = true) {
        val data = database.query("SELECT * FROM players")
        submit(async = false) {
            player.sendMessage("Welcome! Data: $data")
        }
    }
}
```

### ❌ Mistake 2: Missing @SubscribeEvent annotation
```kotlin
object PlayerListener {
    fun onJoin(event: PlayerJoinEvent) {  // ← Missing @SubscribeEvent!
        // Never called
    }
}
```
**Why wrong**: Listeners must have @SubscribeEvent to be registered.

**Fix**: Add annotation
```kotlin
@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) {
    // Now registered
}
```

### ❌ Mistake 3: Modifying event in MONITOR priority
```kotlin
@SubscribeEvent(priority = EventPriority.MONITOR)
fun onDamage(event: EntityDamageEvent) {
    event.damage = 0.0  // ← Should not modify in MONITOR!
}
```
**Why wrong**: MONITOR priority is for read-only observation, not modification.

**Fix**: Use appropriate priority
```kotlin
@SubscribeEvent(priority = EventPriority.HIGH)
fun onDamage(event: EntityDamageEvent) {
    event.damage = 0.0  // ← OK in HIGH priority
}
```

### ❌ Mistake 4: Not checking ignoreCancelled
```kotlin
@SubscribeEvent  // ignoreCancelled = false (default)
fun onBlockBreak(event: BlockBreakEvent) {
    // Fires even if event cancelled by protection plugin
    giveReward(event.player)  // ← Gives reward even when break denied!
}
```
**Why wrong**: Feature executes even when event is cancelled by other plugins.

**Fix**: Set ignoreCancelled = true
```kotlin
@SubscribeEvent(ignoreCancelled = true)
fun onBlockBreak(event: BlockBreakEvent) {
    // Only fires if event NOT cancelled
    giveReward(event.player)  // ← Only rewards successful breaks
}
```

### ❌ Mistake 5: Complex business logic in listener
```kotlin
@SubscribeEvent
fun onPlayerDamage(event: EntityDamageEvent) {
    // 50 lines of business logic here ← Wrong layer!
    if (event.entity is Player) {
        val player = event.entity as Player
        // Complex calculations, database queries, etc.
    }
}
```
**Why wrong**: Listeners should delegate to service layer, not contain business logic.

**Fix**: Delegate to service
```kotlin
@Component
class CombatService {
    fun handleDamage(player: Player, damage: Double): Boolean {
        // Business logic here
        return shouldCancelDamage
    }
}

@SubscribeEvent
fun onPlayerDamage(event: EntityDamageEvent) {
    if (event.entity is Player) {
        val player = event.entity as Player
        if (combatService.handleDamage(player, event.damage)) {
            event.isCancelled = true
        }
    }
}
```

### ❌ Mistake 6: Wrong event type
```kotlin
@SubscribeEvent
fun onJoin(event: PlayerLoginEvent) {  // ← Wrong event!
    event.player.sendMessage("Welcome!")  // ← May fail, player not fully loaded
}
```
**Why wrong**: PlayerLoginEvent fires before player fully joins. Use PlayerJoinEvent.

**Fix**: Use correct event
```kotlin
@SubscribeEvent
fun onJoin(event: PlayerJoinEvent) {  // ← Correct event
    event.player.sendMessage("Welcome!")  // ← Safe
}
```

## Minimal Example

**Complete listener setup**:

```kotlin
package com.example.plugin.listener

import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.submit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.block.BlockBreakEvent

object PlayerListener {
    
    // Basic join handler
    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        event.joinMessage = "§a${player.name} joined the server"
        
        // Load data asynchronously
        submit(async = true) {
            val data = playerService.loadData(player.uniqueId)
            submit(async = false) {
                player.sendMessage("Welcome back! Coins: ${data.coins}")
            }
        }
    }
    
    // Quit handler with cleanup
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        event.quitMessage = "§c${player.name} left the server"
        
        // Save data asynchronously
        submit(async = true) {
            playerService.saveData(player.uniqueId)
        }
    }
    
    // Block break with protection check
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        // Only fires if not cancelled by protection plugins
        rewardService.giveBlockReward(player, block.type)
    }
}
```

## Checklist

Before creating listeners:

- [ ] Verify event type is correct (PlayerJoinEvent vs PlayerLoginEvent, etc.)
- [ ] Check if ignoreCancelled should be true (respect protection plugins)
- [ ] Determine appropriate priority (NORMAL for most, HIGH for overrides, MONITOR for logging)
- [ ] Confirm no blocking IO in event handler (use async if needed)
- [ ] Verify business logic is in service layer, not listener
- [ ] Check if event cancellation is needed

After creating listeners:

- [ ] Test listener fires on event trigger
- [ ] Verify priority order works correctly (if multiple listeners)
- [ ] Test ignoreCancelled behavior (if set to true)
- [ ] Confirm event cancellation works (if cancelling events)
- [ ] Test async operations complete correctly
- [ ] Verify no main thread blocking

## Version-Specific Notes

**TabooLib 6.2.0+**:
- Auto-registration for all @SubscribeEvent methods
- Cross-platform event support
- EventPriority enum matches Bukkit priorities

**TabooLib 6.1.x**:
- Similar API, check wiki for differences

## Troubleshooting

**Error: "Listener not firing"**
- Cause: Missing @SubscribeEvent annotation
- Fix: Add @SubscribeEvent to method

**Error: "Server lag on event"**
- Cause: Blocking IO in event handler
- Fix: Move IO to async scheduler

**Error: "Event fires when cancelled"**
- Cause: ignoreCancelled = false (default)
- Fix: Set ignoreCancelled = true

**Error: "Player not fully loaded"**
- Cause: Using PlayerLoginEvent instead of PlayerJoinEvent
- Fix: Use PlayerJoinEvent for post-join actions

## References

- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/event/`
- Related cards: `07_scheduler.md` (async event handling), `05_ioc.md` (service delegation)
- Related recipes: `create_listener.md`
