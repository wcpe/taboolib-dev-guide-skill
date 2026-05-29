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

## NMS 代理系统 (nmsProxy)

| 包路径 | 说明 |
|--------|------|
| `taboolib.module.nms` | NMS 核心（nmsProxy, nmsProxyClass, Packet, PacketImpl, PacketSendEvent, PacketReceiveEvent, MinecraftVersion, Mapping, PacketSender） |
| `taboolib.module.nms.remap` | 字节码级类存在性检查（require） |

> **Note**: `sendPacket` is an extension function on `Player` provided by `PacketSender` object in `taboolib.module.nms`, not a sub-package. `sendPacketBlocking` does not exist.

### nmsProxy 关键函数

| 函数 | 包路径 | 说明 |
|------|--------|------|
| `nmsProxy<T>(bind, parameter, parent)` | `taboolib.module.nms` | 返回转译后的 NMS 代理实例 |
| `nmsProxyClass<T>(bind, parent)` | `taboolib.module.nms` | 返回转译后的 NMS 代理 Class |
| `require(class)` | `taboolib.module.nms.remap` | 字节码级类存在检查，零运行时开销 |
| `unsafeLazy { }` | `taboolib.common.util` | 延迟初始化（避免类加载阶段错误） |

## Incision 字节码织入

| 包路径 | 说明 |
|--------|------|
| `taboolib.module.incision` | IncisionBootstrap（入口点） |
| `taboolib.module.incision.annotation` | 注解式 Advice 声明（@Surgeon, @Lead, @Trail, @Splice, @Bypass, @Excise, @Graft, @Trim） |
| `taboolib.module.incision.api` | 公共 API（Theatre, Resume, Suture, Anchor, Shift, Anatomy, VersionMatcher, IncisionAccessor） |
| `taboolib.module.incision.dsl` | DSL 入口（Scalpel, ScopedHandle） |
| `taboolib.module.incision.runtime` | 运行时调度（TheatreDispatcher, AdviceChain, SurgeryRegistry） |
| `taboolib.module.incision.loader` | 织入后端（Backend, InstrumentationBackend, JvmtiBackend, SurgeonScanner） |
| `taboolib.module.incision.weaver` | 字节码织入引擎（Scalpel.weave, SiteWeaver, FrameVerifier） |
| `taboolib.module.incision.diagnostic` | 诊断工具（Forensics, Trauma, Checkup, ConflictAnalyzer） |
| `taboolib.module.incision.lifecycle` | 生命周期管理（AutoHealHandler） |

### Incision 关键注解

| 注解 | 包路径 | 说明 |
|------|--------|------|
| `@Surgeon` | `taboolib.module.incision.annotation` | 标记 advice holder object |
| `@Lead` | 同上 | 方法入口 advice |
| `@Trail` | 同上 | 方法出口 advice |
| `@Splice` | 同上 | 环绕控制 advice（需显式 proceed/override） |
| `@Bypass` | 同上 | 调用点替换 |
| `@Graft` | 同上 | 锚点前后追加 |
| `@Trim` | 同上 | 值改写 |
| `@Excise` | 同上 | 整段方法覆写 |
| `@Operation` | 同上 | advice 元信息（id, priority, enabled） |
| `@Version` | 同上 | 版本门控 |
| `@KotlinTarget` | 同上 | Kotlin companion/@JvmStatic 双路径覆盖 |
| `@Site` | 同上 | 锚点规格 |
| `@SurgeryDesk` | 同上 | 标记 DSL patch holder object |

---

## References

- Source code: `taboolib/` (all subdirectories)
- Related maps: `annotation_map.md`, `common_class_map.md`
- Related cards: All cards reference these packages
