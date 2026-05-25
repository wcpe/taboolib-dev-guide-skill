# Recipe: Fix IoC Injection Error

## Use When

- Error: "No bean found for type X"
- Error: "lateinit property X has not been initialized"
- Dependency injection not working

## Diagnostic Steps

### Step 1: Verify @Component Annotation

**Check**: Does the class have `@Component` annotation?

```kotlin
// WRONG - missing @Component
class MyService {
    fun doSomething() { }
}

// CORRECT
@Component
class MyService {
    fun doSomething() { }
}
```

**Fix**: Add `@Component` annotation to the class.

---

### Step 2: Verify Module Installation

**Check**: Is `database-ioc` module installed?

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("database-ioc")  // ← Must be present
}

taboolib {
    env {
        install("database-ioc")  // ← Must be present
    }
    relocate("taboolib", "${project.group}.taboolib")  // ← Required!
}
```

**Fix**: Add missing module and relocate configuration.

---

### Step 3: Check Circular Dependencies

**Check**: Do ServiceA and ServiceB depend on each other?

```kotlin
// WRONG - circular dependency
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

**Fix**: Refactor to remove cycle:
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

---

### Step 4: Check Initialization Timing

**Check**: Are you using dependency before `@PostConstruct`?

```kotlin
// WRONG - using dependency in init block
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    init {
        dependency.doSomething()  // ← TOO EARLY! Not injected yet
    }
}

// CORRECT - using dependency in @PostConstruct
@Component
class MyService {
    @Resource
    lateinit var dependency: OtherService
    
    @PostConstruct
    fun init() {
        dependency.doSomething()  // ← Safe: injected now
    }
}
```

**Fix**: Move initialization to `@PostConstruct`.

---

### Step 5: Verify Not Injecting Non-Bean

**Check**: Are you trying to inject a plain class?

```kotlin
// WRONG - plain data class is not a bean
data class PlayerData(val name: String)

@Component
class MyService {
    @Resource
    lateinit var playerData: PlayerData  // ← Will fail! Not a @Component
}

// CORRECT - inject service that provides data
data class PlayerData(val name: String)

@Component
class PlayerDataService {
    fun getData(name: String): PlayerData = PlayerData(name)
}

@Component
class MyService {
    @Resource
    lateinit var playerDataService: PlayerDataService  // ← Inject the service
}
```

**Fix**: Only inject `@Component` beans, not plain classes.

---

### Step 6: Check Package Scanning

**Check**: Is the class in a scanned package?

TabooLib scans:
- All classes with `@Inject` annotation
- All classes in plugin package and subpackages

**Fix**: Ensure class is in correct package or has `@Inject` annotation.

---

## Quick Fix Checklist

When encountering "No bean found" error:

- [ ] Add `@Component` to the class
- [ ] Verify `database-ioc` module installed
- [ ] Check `relocate` configured in build.gradle.kts
- [ ] Verify no circular dependencies
- [ ] Move dependency usage to `@PostConstruct`
- [ ] Confirm not injecting plain classes (only @Component beans)
- [ ] Rebuild project: `./gradlew clean build`
- [ ] Restart server

## Verification Steps

1. Build: `./gradlew clean build` → success
2. Start server → check logs for bean registration
3. Test injection → verify no "No bean found" errors
4. Test functionality → verify injected dependencies work

## Common Error Messages

**"No bean found for type com.example.MyService"**
- Cause: Missing `@Component` annotation
- Fix: Add `@Component` to MyService

**"lateinit property dependency has not been initialized"**
- Cause: Using dependency before `@PostConstruct`
- Fix: Move to `@PostConstruct`

**"Circular dependency detected"**
- Cause: ServiceA → ServiceB → ServiceA
- Fix: Refactor to remove cycle

## References

- Card: `05_ioc.md`
- Card: `10_troubleshooting.md`
- Related recipes: `create_ioc_service.md`
