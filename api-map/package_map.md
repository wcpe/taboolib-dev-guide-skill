# TabooLib Package Map

**⚠️ WARNING**: Package paths may change across TabooLib versions. Always verify against user's project imports first.

## Core Platform Packages

| Package | Purpose | Key Classes/Functions |
|---------|---------|----------------------|
| `taboolib.common` | Core lifecycle system | `LifeCycle`, `Inject` |
| `taboolib.common.platform` | Platform abstraction | `Awake`, `Plugin`, `Schedule`, `PlatformSide` |
| `taboolib.common.platform.function` | Public API functions | `submit`, `submitAsync`, `submitChain`, `pluginId`, `pluginVersion` |
| `taboolib.common.platform.command` | Command system | `CommandHeader`, `CommandBody`, `CommandContext`, `subCommand`, `mainCommand` |
| `taboolib.common.platform.event` | Event system | `SubscribeEvent`, `EventPriority`, `ProxyEvent` |
| `taboolib.common.platform.service` | Platform services | `PlatformExecutor`, `PlatformIO`, `PlatformCommand` |

**Verification**: `grep -r "import taboolib.common" src/` to see actual imports in project.

---

## Module Packages

| Package | Module | Purpose | Key Classes |
|---------|--------|---------|-------------|
| `taboolib.module.configuration` | `basic-configuration` | Config system | `Config`, `Configuration`, `ConfigFile`, `ConfigSection` |
| `taboolib.module.lang` | `minecraft-i18n` | i18n system | `sendLang`, `asLangText`, `Language`, `LanguageFile` |
| `taboolib.module.chat` | `basic-chat` | Chat utilities | `ComponentText`, `TellrawJson` |
| `taboolib.module.nms` | `bukkit-nms` | NMS utilities | `MinecraftVersion`, `nmsProxy` |

**Verification**: Check user's `build.gradle.kts` for installed modules:
```kotlin
taboolib {
    env {
        install("module-configuration")
        install("module-lang")
        // etc.
    }
}
```

---

## IoC Container Packages (database-ioc module)

| Package | Purpose | Key Classes/Annotations |
|---------|---------|------------------------|
| `taboolib.expansion.ioc.annotation` | IoC annotations | `Component`, `Service`, `Repository`, `Resource`, `PostConstruct`, `PreDestroy` |
| `taboolib.expansion.ioc.bean` | Bean management | `BeanContainer`, `BeanScope` |
| `taboolib.expansion.ioc.scope` | Scope definitions | `SingletonScope`, `PrototypeScope`, `PlayerScope` |

**Verification**: `grep -r "import taboolib.expansion.ioc" src/`

**⚠️ CRITICAL**: Requires `database-ioc` module and `relocate` configuration.

---

## Platform-Specific Packages

| Package | Platform | Purpose |
|---------|----------|---------|
| `taboolib.platform` | All | Platform implementations |
| `taboolib.platform.util` | Bukkit | `bukkitPlugin`, Bukkit utilities |
| `taboolib.platform.type` | All | `BukkitPlayer`, `BukkitCommandSender` |

**Verification**: Check if user's plugin is multi-platform or Bukkit-only.

---

## Common Import Patterns

### Basic Plugin
```kotlin
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
```

### Command System
```kotlin
import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
```

### Event System
```kotlin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
```

### Config System
```kotlin
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
```

### IoC System
```kotlin
import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import taboolib.expansion.ioc.annotation.PostConstruct
```

### Scheduler System
```kotlin
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitChain
import taboolib.common.platform.Schedule
```

### i18n System
```kotlin
import taboolib.module.lang.sendLang
import taboolib.module.lang.asLangText
```

---

## Package Verification Workflow

Before using any package:

1. **Check user's existing imports**: `grep -r "import taboolib" src/`
2. **Extract common patterns**: Look for most frequently used packages
3. **Verify module installed**: Check `build.gradle.kts` for module
4. **Match user's style**: Use their import style (wildcard vs explicit)
5. **If uncertain**: Mark as "unverified - check project"

---

## Version-Specific Notes

**TabooLib 6.2.0+**:
- All packages listed above are available
- IoC packages require `database-ioc` module
- Some packages may have been reorganized from 6.1.x

**TabooLib 6.1.x**:
- Some package paths may differ
- Check wiki for migration guide

---

## Common Package Mistakes

### ❌ Wrong: Guessing package paths
```kotlin
import taboolib.api.command.*  // ← Fabricated package!
```

### ✅ Correct: Verify from user's project
```kotlin
// Step 1: grep -r "CommandHeader" src/
// Step 2: Extract actual package from results
import taboolib.common.platform.command.*  // ← Verified
```

---

## References

- Source code: `taboolib/` (all subdirectories)
- Related maps: `annotation_map.md`, `common_class_map.md`
- Related cards: All cards reference these packages
