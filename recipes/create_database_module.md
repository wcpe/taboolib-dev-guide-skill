# Recipe: Create Database Module

## Use When

- Need to add database persistence
- Implementing player data storage
- Setting up entity and repository layers

## Required Checks

- [ ] Verify `database-ioc` module installed
- [ ] Check if using EasyQuery (recommended) or other ORM
- [ ] Confirm database connection configuration

## Important Note

**For detailed database implementation**, use the specialized skill:
```
$taboolib-corelib-easyquery-persistence-standards
```

This recipe provides a minimal quick-start only.

## Quick Start Steps

### Step 1: Add Dependencies

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("database-ioc")
    // Add database driver
    compileOnly("mysql:mysql-connector-java:8.0.33")
}
```

### Step 2: Create Entity (Minimal)

**File**: `src/main/kotlin/<package>/entity/PlayerDataEntity.kt`
```kotlin
package com.example.plugin.entity

import com.easy.query.core.annotation.*

@Table("player_data")
@EntityProxy
data class PlayerDataEntity(
    @Column(primaryKey = true, comment = "Player UUID", dbType = "VARCHAR(36)")
    var uuid: String = "",
    
    @Column(comment = "Player coins", dbType = "INT")
    var coins: Int = 0,
    
    @Column(comment = "Last login timestamp", dbType = "BIGINT")
    var lastLogin: Long = 0
) : ProxyEntityAvailable<PlayerDataEntity, PlayerDataEntityProxy>
```

### Step 3: Create Repository (Minimal)

**File**: `src/main/kotlin/<package>/repository/PlayerDataRepository.kt`
```kotlin
package com.example.plugin.repository

import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import com.easy.query.api.EasyQuery

@Component
class PlayerDataRepository {
    
    @Resource
    lateinit var easyQuery: EasyQuery
    
    fun findByUuid(uuid: String): PlayerDataEntity? {
        return easyQuery.queryable<PlayerDataEntity>()
            .where { it.uuid eq uuid }
            .firstOrNull()
    }
    
    fun save(entity: PlayerDataEntity) {
        easyQuery.insertable(entity).executeRows()
    }
    
    fun update(entity: PlayerDataEntity) {
        easyQuery.updatable(entity).executeRows()
    }
}
```

### Step 4: Database Manager (Minimal)

**File**: `src/main/kotlin/<package>/database/DatabaseManager.kt`
```kotlin
package com.example.plugin.database

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.expansion.ioc.annotation.Component
import com.easy.query.api.EasyQuery

@Component
class DatabaseManager {
    
    lateinit var easyQuery: EasyQuery
    
    @Awake(LifeCycle.ENABLE)
    fun init() {
        // Initialize EasyQuery client
        // Register entities
        // Run Code First schema sync
    }
}
```

## Full Implementation

For complete implementation with:
- Proper entity annotations and validation
- Transaction management
- Connection pooling
- Schema migration
- Repository patterns
- Testing strategies

**Use the specialized skill**:
```
$taboolib-corelib-easyquery-persistence-standards
```

## Verification Steps

1. Build: `./gradlew build` → success
2. Start server → check database connection
3. Test CRUD operations
4. Verify schema created correctly

## References

- Card: `08_database.md`
- **Detailed guidance**: `$taboolib-corelib-easyquery-persistence-standards`
