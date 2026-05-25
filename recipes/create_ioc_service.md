# Recipe: Create IoC Service

## Use When

- Need to implement service layer with dependency injection
- Creating testable, loosely-coupled components
- Implementing repository or business logic

## Required Checks

- [ ] Verify `database-ioc` module installed
- [ ] Confirm `relocate` configured in build.gradle.kts
- [ ] Check user's existing service pattern

## Implementation Steps

### Step 1: Verify Module Setup

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("database-ioc")
}

taboolib {
    env {
        install("database-ioc")
    }
    relocate("taboolib", "${project.group}.taboolib")  // Required!
}
```

### Step 2: Create Service

**File**: `src/main/kotlin/<package>/service/PlayerService.kt`
```kotlin
package com.example.plugin.service

import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import taboolib.expansion.ioc.annotation.PostConstruct
import taboolib.expansion.ioc.annotation.PreDestroy
import java.util.UUID

@Component
class PlayerService {
    
    @Resource
    lateinit var repository: PlayerRepository
    
    @PostConstruct
    fun init() {
        println("PlayerService initialized")
    }
    
    @PreDestroy
    fun cleanup() {
        println("PlayerService cleanup")
    }
    
    fun getPlayerData(uuid: UUID): PlayerData {
        return repository.findByUuid(uuid) ?: createDefault(uuid)
    }
    
    private fun createDefault(uuid: UUID): PlayerData {
        val data = PlayerData(uuid)
        repository.save(data)
        return data
    }
}

data class PlayerData(
    val uuid: UUID,
    var coins: Int = 0
)
```

### Step 3: Create Repository

**File**: `src/main/kotlin/<package>/repository/PlayerRepository.kt`
```kotlin
package com.example.plugin.repository

import taboolib.expansion.ioc.annotation.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class PlayerRepository {
    
    private val cache = ConcurrentHashMap<UUID, PlayerData>()
    
    fun findByUuid(uuid: UUID): PlayerData? {
        return cache[uuid]
    }
    
    fun save(data: PlayerData) {
        cache[data.uuid] = data
    }
}
```

### Step 4: Use in Command

```kotlin
@CommandHeader(name = "example")
object ExampleCommand {
    
    @Resource
    lateinit var playerService: PlayerService
    
    @CommandBody
    val balance = subCommand {
        execute<ProxyPlayer> { player, _, _ ->
            val data = playerService.getPlayerData(player.uniqueId)
            player.sendMessage("Balance: ${data.coins}")
        }
    }
}
```

## Verification Steps

1. Build: `./gradlew build` → success
2. Start server → check logs for "PlayerService initialized"
3. Execute command → verify service injection works
4. Stop server → check logs for "PlayerService cleanup"

## References

- Card: `05_ioc.md`
- Related recipes: `fix_ioc_injection_error.md`
