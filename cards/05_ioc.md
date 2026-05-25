# Card 05: IoC Container & Dependency Injection

## When to Use

Load this card when:
- User asks about "IOC", "DI", "@Component", "@Inject", "@Resource", "Bean", "注入失败"
- Need to implement service layer with dependency injection
- Troubleshooting "No bean found" or injection errors
- Implementing testable, loosely-coupled architecture

## Core Idea

TabooLib provides Spring-like IoC container via `database-ioc` module. Use `@Component` (or `@Service`/`@Repository`) to mark beans, `@Resource` for field injection, `@PostConstruct` for post-initialization. **Only @Component beans can be injected** - plain data classes or objects without @Component will fail. **Prototype and custom-scoped beans skip @PreDestroy callbacks** - manual cleanup required.

## Recommended Pattern

### Module Setup

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("database-ioc")  // ← Required for IoC
}

taboolib {
    env {
        install("database-ioc")
    }
    relocate("taboolib", "${project.group}.taboolib")  // ← Required!
}
```

### Component Definition

**Package**: `taboolib.expansion.ioc.annotation`

```kotlin
import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import taboolib.expansion.ioc.annotation.PostConstruct
import taboolib.expansion.ioc.annotation.PreDestroy
import taboolib.expansion.ioc.bean.BeanScope

// Basic component (singleton by default)
@Component
class PlayerService {
    
    @Resource
    lateinit var dataRepository: PlayerDataRepository
    
    @PostConstruct
    fun init() {
        // Called after all dependencies injected
        println("PlayerService initialized")
    }
    
    @PreDestroy
    fun cleanup() {
        // Called during plugin disable (SINGLETON only!)
        println("PlayerService cleanup")
    }
    
    fun getPlayerData(uuid: UUID): PlayerData {
        return dataRepository.findByUuid(uuid)
    }
}

// Repository component
@Component
class PlayerDataRepository {
    fun findByUuid(uuid: UUID): PlayerData {
        // Database query
    }
}
```

### Bean Scopes

```kotlin
@Target(AnnotationTarget.CLASS)
annotation class Component(val scope: BeanScope = BeanScope.SINGLETON)

enum class BeanScope {
    SINGLETON,   // One instance per application (default)
    PROTOTYPE,   // New instance on each retrieval
    PLAYER       // One instance per player (bound to join/quit)
}
```

**Singleton** (default):
```kotlin
@Component  // scope = SINGLETON by default
class ConfigService {
    // One instance shared across application
}
```

**Prototype** (new instance each time):
```kotlin
@Component(scope = BeanScope.PROTOTYPE)
class TaskExecutor {
    // New instance on each BeanContainer.getBean() call
    // ⚠️ WARNING: @PreDestroy NOT called for prototype beans!
}
```

**Player-scoped** (one per player):
```kotlin
@Component(scope = BeanScope.PLAYER)
class PlayerSession {
    // One instance per player
    // Created on player join, destroyed on player quit
    // @PreDestroy called on player quit
}
```

### Dependency Injection Types

**Field Injection** (most common):
```kotlin
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
}
```

**Constructor Injection** (preferred for immutability):
```kotlin
@Component
class MyService(
    private val dependency: OtherService  // ← Auto-injected
) {
    // No @Resource needed for constructor params
}
```

**Method Injection** (rare):
```kotlin
@Component
class MyService {
    private lateinit var dependency: OtherService
    
    @Resource
    fun setDependency(dep: OtherService) {
        this.dependency = dep
    }
}
```

### Lifecycle Callbacks

```kotlin
@Component
class MyService {
    
    @Resource
    lateinit var dependency: OtherService
    
    @PostConstruct
    fun init() {
        // Called after all @Resource fields injected
        // Safe to use dependencies here
        dependency.initialize()
    }
    
    @PreDestroy
    fun cleanup() {
        // Called during plugin disable
        // ⚠️ Only for SINGLETON and PLAYER scopes!
        // PROTOTYPE beans skip this callback
        dependency.close()
    }
}
```

### Manual Bean Retrieval (Service Locator Anti-Pattern)

**⚠️ Avoid in business code** - use dependency injection instead:
```kotlin
import taboolib.expansion.ioc.bean.BeanContainer

// Get singleton bean
val service = BeanContainer.getBean<PlayerService>()

// Get prototype bean (new instance)
val executor = BeanContainer.getBean<TaskExecutor>()

// Get player-scoped bean
val session = BeanContainer.getBean<PlayerSession>(player.uniqueId)
```

## Common Mistakes

### ❌ Mistake 1: Injecting non-@Component classes
```kotlin
data class PlayerData(val name: String)  // ← Not a @Component!

@Component
class MyService {
    @Resource
    lateinit var playerData: PlayerData  // ← Injection will fail!
}
```
**Why wrong**: Only @Component beans can be injected. Plain data classes are not beans.

**Fix**: Inject service that provides data
```kotlin
data class PlayerData(val name: String)  // ← Plain data class

@Component
class PlayerDataService {  // ← This is the bean
    fun getData(name: String): PlayerData = PlayerData(name)
}

@Component
class MyService {
    @Resource
    lateinit var playerDataService: PlayerDataService  // ← Inject the service
}
```

### ❌ Mistake 2: Using BeanContainer in business code
```kotlin
@Component
class MyService {
    fun doSomething() {
        val other = BeanContainer.getBean<OtherService>()  // ← Service Locator anti-pattern!
        other.execute()
    }
}
```
**Why wrong**: Service Locator pattern hides dependencies and makes testing harder.

**Fix**: Use dependency injection
```kotlin
@Component
class MyService {
    @Resource
    lateinit var other: OtherService  // ← Explicit dependency
    
    fun doSomething() {
        other.execute()
    }
}
```

### ❌ Mistake 3: Expecting @PreDestroy for prototype beans
```kotlin
@Component(scope = BeanScope.PROTOTYPE)
class ResourceHolder {
    private val connection = openConnection()
    
    @PreDestroy
    fun cleanup() {
        connection.close()  // ← NEVER CALLED for prototype beans!
    }
}
```
**Why wrong**: Prototype beans skip @PreDestroy callbacks. Resources leak.

**Fix**: Manual cleanup or use singleton
```kotlin
@Component(scope = BeanScope.PROTOTYPE)
class ResourceHolder {
    private val connection = openConnection()
    
    fun close() {  // ← Manual cleanup method
        connection.close()
    }
}

// Caller must close manually
val holder = BeanContainer.getBean<ResourceHolder>()
try {
    holder.use()
} finally {
    holder.close()  // ← Manual cleanup
}
```

### ❌ Mistake 4: Missing relocate configuration
```kotlin
// build.gradle.kts
taboolib {
    env {
        install("database-ioc")
    }
    // Missing relocate! ← IoC may not work correctly
}
```
**Why wrong**: IoC container requires relocation to avoid conflicts.

**Fix**: Add relocate configuration
```kotlin
taboolib {
    env {
        install("database-ioc")
    }
    relocate("taboolib", "${project.group}.taboolib")  // ← Required
}
```

### ❌ Mistake 5: Using dependencies before @PostConstruct
```kotlin
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    init {
        dependency.initialize()  // ← dependency not injected yet!
    }
}
```
**Why wrong**: Dependencies are injected after constructor, before @PostConstruct.

**Fix**: Use @PostConstruct
```kotlin
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    @PostConstruct
    fun init() {
        dependency.initialize()  // ← Safe: dependencies injected
    }
}
```

### ❌ Mistake 6: Circular dependencies
```kotlin
@Component
class ServiceA {
    @Resource
    lateinit var serviceB: ServiceB  // ← Circular!
}

@Component
class ServiceB {
    @Resource
    lateinit var serviceA: ServiceA  // ← Circular!
}
```
**Why wrong**: Circular dependencies cause initialization deadlock.

**Fix**: Refactor to remove cycle
```kotlin
@Component
class ServiceA {
    @Resource
    lateinit var serviceC: ServiceC  // ← Shared dependency
}

@Component
class ServiceB {
    @Resource
    lateinit var serviceC: ServiceC  // ← Shared dependency
}

@Component
class ServiceC {
    // No circular dependencies
}
```

## Minimal Example

**Complete IoC setup**:

```kotlin
package com.example.plugin.service

import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import taboolib.expansion.ioc.annotation.PostConstruct
import taboolib.expansion.ioc.annotation.PreDestroy
import java.util.UUID

// Repository layer
@Component
class PlayerRepository {
    fun findByUuid(uuid: UUID): PlayerData? {
        // Database query
        return null
    }
    
    fun save(data: PlayerData) {
        // Database save
    }
}

// Service layer
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
    
    fun getOrCreate(uuid: UUID): PlayerData {
        return repository.findByUuid(uuid) ?: PlayerData(uuid).also {
            repository.save(it)
        }
    }
}

// Data class (not a bean)
data class PlayerData(
    val uuid: UUID,
    var coins: Int = 0
)

// Usage in command
@CommandBody
val balance = subCommand {
    execute<ProxyPlayer> { player, _, _ ->
        val data = playerService.getOrCreate(player.uniqueId)
        player.sendMessage("Balance: ${data.coins}")
    }
}
```

## Checklist

Before using IoC:

- [ ] Verify `database-ioc` module installed in build.gradle.kts
- [ ] Confirm `relocate` configured in taboolib block
- [ ] Check all injectable classes have @Component annotation
- [ ] Verify no circular dependencies exist
- [ ] Confirm prototype beans have manual cleanup if needed
- [ ] Check @PostConstruct used for initialization, not constructor

After implementing IoC:

- [ ] Test beans are created and injected correctly
- [ ] Verify @PostConstruct callbacks execute
- [ ] Test @PreDestroy callbacks execute on plugin disable (singleton only)
- [ ] Confirm no "No bean found" errors in logs
- [ ] Test player-scoped beans create/destroy on join/quit
- [ ] Verify prototype beans create new instances each time

## Version-Specific Notes

**TabooLib 6.2.0+ (database-ioc 1.1.0+)**:
- Requires Kotlin 2.1.0+
- Must use `taboo()` not `compileOnly()`
- Must configure `relocate()`
- Prototype beans skip @PreDestroy (breaking change)

**TabooLib 6.1.x**:
- Older IoC implementation, check wiki for differences

## Troubleshooting

**Error: "No bean found for type X"**
- Cause: Class missing @Component annotation
- Fix: Add @Component to the class

**Error: "lateinit property not initialized"**
- Cause: Using dependency before @PostConstruct
- Fix: Move initialization to @PostConstruct

**Error: "Circular dependency detected"**
- Cause: ServiceA → ServiceB → ServiceA
- Fix: Refactor to remove cycle, extract shared dependency

**Error: "Resource leak" (prototype beans)**
- Cause: @PreDestroy not called for prototype beans
- Fix: Add manual cleanup method, call explicitly

## References

- Source code: `taboolib/module/database/database-ioc/`
- Wiki: IoC container documentation
- Related cards: `02_lifecycle.md` (@PostConstruct timing), `08_database.md` (repository pattern)
- Related recipes: `create_ioc_service.md`, `fix_ioc_injection_error.md`
