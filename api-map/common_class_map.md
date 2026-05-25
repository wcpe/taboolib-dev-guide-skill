# TabooLib Common Class Map

**⚠️ WARNING**: Class signatures may change across TabooLib versions. Always verify against user's project first.

## Frequently Used Classes

### Configuration
**Package**: `taboolib.module.configuration`

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `Configuration` | Config interface | `getString()`, `getInt()`, `set()`, `saveToFile()`, `reload()` |
| `ConfigFile` | Config implementation | Extends `Configuration` |
| `ConfigSection` | Config section | `getKeys()`, `contains()`, `createSection()` |

**Verification**: `grep -r "Configuration" src/`

---

### Command System
**Package**: `taboolib.common.platform.command`

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `CommandContext` | Command execution context | `sender()`, `player()`, `argument()`, `args()`, `option()` |
| `CommandStructure` | Command metadata | Holds command info |
| `CommandBase` | Command tree root | `execute()`, `suggest()` |

**Common Functions**:
```kotlin
// Create subcommand
subCommand { }

// Create main command
mainCommand { }

// Dynamic argument with completion
dynamic("name") { }

// Player argument with auto-completion
player("target") { }

// Typed arguments
int("amount") { }
double("value") { }
boolean("flag") { }
```

**Verification**: `grep -r "CommandContext" src/`

---

### Scheduler
**Package**: `taboolib.common.platform.function`

| Function | Purpose | Parameters |
|----------|---------|------------|
| `submit()` | Schedule task | `async: Boolean = false`, `delay: Long = 0`, `period: Long = 0` |
| `submitAsync()` | Schedule async task | `delay: Long = 0`, `period: Long = 0` |
| `submitChain()` | Sequential async operations | Builder block |

**Usage**:
```kotlin
// Sync task
submit { }

// Async task
submit(async = true) { }

// Delayed task (20 ticks = 1 second)
submit(delay = 20) { }

// Periodic task
submit(period = 20) { }

// Sequential async
submitChain {
    async { }
    sync { }
}
```

**Verification**: `grep -r "submit" src/`

---

### Plugin Access
**Package**: `taboolib.common.platform.function`

| Property/Function | Purpose | Returns |
|-------------------|---------|---------|
| `pluginId` | Get plugin ID | `String` |
| `pluginVersion` | Get plugin version | `String` |
| `isPrimaryThread` | Check if main thread | `Boolean` |

**Bukkit-specific** (Package: `taboolib.platform.util`):
| Property | Purpose | Returns |
|----------|---------|---------|
| `bukkitPlugin` | Get Bukkit plugin instance | `JavaPlugin` |

**Usage**:
```kotlin
val id = pluginId
val version = pluginVersion
val plugin = bukkitPlugin  // Bukkit only
```

**Verification**: `grep -r "pluginId\|bukkitPlugin" src/`

---

### Event System
**Package**: `taboolib.common.platform.event`

| Enum | Values |
|------|--------|
| `EventPriority` | `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR` |

**Usage**:
```kotlin
@SubscribeEvent(priority = EventPriority.HIGH)
fun onEvent(event: SomeEvent) { }
```

---

### Proxy Types
**Package**: `taboolib.common.platform`

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `ProxyCommandSender` | Cross-platform sender | `sendMessage()`, `hasPermission()`, `name` |
| `ProxyPlayer` | Cross-platform player | Extends `ProxyCommandSender`, adds player methods |

**Usage**:
```kotlin
execute<ProxyCommandSender> { sender, _, _ ->
    sender.sendMessage("Hello")
}

execute<ProxyPlayer> { player, _, _ ->
    player.sendMessage("Hello player")
    player.kick("Reason")
}
```

---

### IoC Container
**Package**: `taboolib.expansion.ioc.bean`

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `BeanContainer` | Bean registry | `getBean<T>()`, `registerBean()` |
| `BeanScope` | Bean scope enum | `SINGLETON`, `PROTOTYPE`, `PLAYER` |

**⚠️ WARNING**: Avoid using `BeanContainer.getBean()` in business code - use dependency injection instead.

**Usage** (Service Locator anti-pattern - avoid):
```kotlin
val service = BeanContainer.getBean<MyService>()
```

**Preferred** (Dependency Injection):
```kotlin
@Component
class MyClass {
    @Resource
    lateinit var service: MyService
}
```

---

### Language System
**Package**: `taboolib.module.lang`

| Function | Purpose | Parameters |
|----------|---------|------------|
| `sendLang()` | Send localized message | `node: String`, `vararg args: Any` |
| `asLangText()` | Get localized text | `node: String`, `vararg args: Any` |
| `asLangTextOrNull()` | Get text or null | `node: String`, `vararg args: Any` |

**Usage**:
```kotlin
player.sendLang("message-key", arg1, arg2)
val text = player.asLangText("message-key", arg1)
```

**Verification**: `grep -r "sendLang" src/`

---

## Common Utility Functions

### Logging
**Package**: `taboolib.common.platform.function`

```kotlin
info("Info message")
warning("Warning message")
severe("Error message")
```

### Console Access
**Package**: `taboolib.common.platform.function`

```kotlin
val console = console()  // Get console sender
console.sendMessage("Message to console")
```

---

## Verification Workflow

Before using any class:

1. **Check user's imports**: `grep -r "ClassName" src/`
2. **Extract package path**: Look at import statements
3. **Verify method signatures**: Check actual usage in project
4. **Match user's style**: Use their patterns
5. **If uncertain**: Mark as "unverified - check project"

---

## Version-Specific Notes

**TabooLib 6.2.0+**:
- All classes listed above are available
- Some method signatures may have changed from 6.1.x

**TabooLib 6.1.x**:
- Some classes may have different packages
- Check wiki for migration guide

---

## Common Class Mistakes

### ❌ Wrong: Fabricating class names
```kotlin
val config = ConfigManager.getInstance()  // ← Doesn't exist!
```

### ✅ Correct: Use verified classes
```kotlin
@Config("config.yml")
lateinit var config: Configuration  // ← Verified from source
```

---

## References

- Source code: `taboolib/` (all modules)
- Related maps: `annotation_map.md`, `package_map.md`, `lifecycle_map.md`
- Related cards: All cards reference these classes
