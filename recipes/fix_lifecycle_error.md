# Recipe: Fix Lifecycle Error

## Use When

- Error: "Config not loaded" or config values are null
- Error: "Scheduler not available"
- Features not working due to timing issues
- Resources accessed too early or too late

## Diagnostic Steps

### Step 1: Config Not Loaded

**Symptom**: Config values are null or default

**Check**: Are you accessing config in CONST lifecycle?

```kotlin
// WRONG - accessing config too early
@Awake(LifeCycle.CONST)
fun loadConfig() {
    val value = config["key"]  // ← Config not loaded yet!
}

// CORRECT - accessing config in ENABLE
@Awake(LifeCycle.ENABLE)
fun loadConfig() {
    val value = config["key"]  // ← Safe: config loaded
}
```

**Fix**: Move config access to ENABLE or later.

---

### Step 2: Scheduler Not Available

**Symptom**: Tasks don't execute or throw errors

**Check**: Are you scheduling tasks in ENABLE lifecycle?

```kotlin
// WRONG - scheduler not fully initialized
@Awake(LifeCycle.ENABLE)
fun startTasks() {
    submit(period = 20) {  // ← May fail, scheduler not ready
        // Task code
    }
}

// CORRECT - scheduler ready in ACTIVE
@Awake(LifeCycle.ACTIVE)
fun startTasks() {
    submit(period = 20) {  // ← Safe: scheduler ready
        // Task code
    }
}
```

**Fix**: Move scheduler tasks to ACTIVE lifecycle.

---

### Step 3: Plugin Instance Not Available

**Symptom**: Plugin instance is null

**Check**: Are you accessing plugin instance in CONST?

```kotlin
// WRONG - plugin not instantiated yet
@Awake(LifeCycle.CONST)
fun init() {
    val plugin = bukkitPlugin  // ← Plugin not instantiated yet!
}

// CORRECT - plugin available from INIT onwards
@Awake(LifeCycle.INIT)
fun init() {
    val plugin = bukkitPlugin  // ← Safe: plugin instantiated
}
```

**Fix**: Move plugin instance access to INIT or later.

---

### Step 4: Database Not Connected

**Symptom**: Database queries fail

**Check**: Are you querying database before connection established?

```kotlin
// WRONG - database not connected yet
@Awake(LifeCycle.CONST)
fun loadData() {
    database.query()  // ← Database not connected!
}

// CORRECT - database available in ENABLE
@Awake(LifeCycle.ENABLE)
fun loadData() {
    database.query()  // ← Safe: database connected
}
```

**Fix**: Move database operations to ENABLE or later.

---

### Step 5: Execution Order Undefined

**Symptom**: Features initialize in wrong order

**Check**: Do you have multiple `@Awake` methods in same lifecycle?

```kotlin
// WRONG - order undefined
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

// CORRECT - use single method for guaranteed order
object MyInitializer {
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
}
```

**Fix**: Use single `@Awake` method if order matters.

---

### Step 6: Missing Cleanup

**Symptom**: Resources not released on plugin disable

**Check**: Do you have DISABLE lifecycle handler?

```kotlin
// WRONG - no cleanup
@Awake(LifeCycle.ENABLE)
fun startConnection() {
    connection = openConnection()
    // No cleanup! ← Resource leak
}

// CORRECT - cleanup in DISABLE
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

**Fix**: Add DISABLE handler for cleanup.

---

## Lifecycle Stage Reference

| Stage | Plugin | Config | Database | Scheduler | Bukkit API |
|-------|--------|--------|----------|-----------|------------|
| CONST | ❌ | ❌ | ❌ | ❌ | ❌ |
| INIT | ✅ | ❌ | ❌ | ❌ | ⚠️ |
| LOAD | ✅ | ⚠️ | ❌ | ❌ | ⚠️ |
| ENABLE | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| ACTIVE | ✅ | ✅ | ✅ | ✅ | ✅ |
| DISABLE | ✅ | ✅ | ⚠️ | ❌ | ⚠️ |

## Quick Fix Checklist

When encountering lifecycle errors:

- [ ] Config access → Move to ENABLE or later
- [ ] Scheduler tasks → Move to ACTIVE
- [ ] Plugin instance → Move to INIT or later
- [ ] Database operations → Move to ENABLE or later
- [ ] Multiple @Awake in same stage → Combine into single method
- [ ] Resources opened in ENABLE → Add DISABLE cleanup
- [ ] Rebuild and restart server

## Verification Steps

1. Check server logs for lifecycle messages
2. Verify resources initialize in correct order
3. Test feature functionality
4. Test plugin reload (DISABLE → ENABLE cycle)
5. Verify no resource leaks after disable

## Common Error Messages

**"Config not loaded"**
- Cause: Accessing config in CONST or early INIT
- Fix: Move to ENABLE lifecycle

**"Scheduler not available"**
- Cause: Scheduling tasks in ENABLE
- Fix: Move to ACTIVE lifecycle

**"Plugin instance null"**
- Cause: Accessing plugin instance in CONST
- Fix: Move to INIT or later

## References

- Card: `02_lifecycle.md`
- Card: `10_troubleshooting.md`
- Related cards: `04_config.md`, `07_scheduler.md`
