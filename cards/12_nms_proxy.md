# Card 12: nmsProxy 跨版本代理系统

## When to Use

Load this card when:
- User asks about "nmsProxy", "NMS代理", "NMS兼容", "跨版本", "版本适配", "MinecraftVersion"
- Need to call NMS (net.minecraft.server) APIs across Minecraft versions
- Implementing features requiring direct Minecraft server interaction (ActionBar, Title, Particle, Entity, Scoreboard, Sign editing)
- Troubleshooting `ClassNotFoundException`, `NoSuchMethodError` across versions
- Creating custom NMS proxy classes for in-house features

## Core Idea

TabooLib 的 nmsProxy 系统通过 **ASM 字节码翻译** 在运行时生成代理实例，实现跨 Minecraft 版本的 NMS 兼容。核心模式是 **抽象类 + Impl 实现**：你在抽象类中定义方法签名，TabooLib 在加载时根据当前服务器版本自动检测（`MinecraftVersion`）并生成对应实现类的实例。`require()` 方法在字节码层面对类存在性进行检查，零运行时开销。

**三大版本映射策略**：
- **1.8 ~ 1.16** 传统：Obfuscated names（`MinecraftServer` → `net.minecraft.server.v1_12_R1.MinecraftServer`）
- **1.17 ~ 1.20.4** Universal Remap：Spigot 映射（`net.minecraft.server.MinecraftServer`）
- **1.20.5+** Mojang Mapping：完整 Mojang 映射名（`net.minecraft.server.MinecraftServer`）

## ⚠️ 关键架构规则

### 🚫 nmsProxy 只能用于抽象类

**nmsProxy 不支持接口和普通类**：

```kotlin
// ❌ 错误：接口不可代理
interface NMSTitle {
    fun send(player: Player, message: String)
}
val title = nmsProxy<NMSTitle>()  // ← 编译失败！

// ✅ 正确：抽象类
abstract class NMSTitle {
    abstract fun send(player: Player, message: String)
}
val title = nmsProxy<NMSTitle>()  // ← 正常工作
```

### Impl 命名约定

**Impl 类必须遵循特定命名规则才能被自动发现**：
- 默认规则：`{AbstractClass}Impl`（如 `NMSTitleImpl`），匹配时使用**完全限定类名（FQCN）**
- 可以使用 `nmsProxy<T>(className: String)` 指定自定义 Impl 名称（也必须是 FQCN）
- **不要**在 Impl 类名中使用特殊字符，使用纯 ASCII 字母和数字

### 版本检测优先级

版本检测逻辑按以下顺序匹配（高优先级优先）：
1. 类名精确匹配（如 `NMSTitleImpl_V1_20_R4`）
2. `MinecraftVersion` 枚举范围匹配
3. 最接近当前版本的通用实现（fallback）

## Recommended Pattern

### 模块配置

**build.gradle.kts**：
```kotlin
dependencies {
    taboo("module-nms")  // ← NMS 代理模块
    compileOnly("ink.ptms.core:v12004:12004:mapped")     // 已映射 NMS 支持（可选）
    compileOnly("ink.ptms.core:v12004:12004:universal")  // 通用 NMS 支持（可选）
}

taboolib {
    env {
        install("module-nms")
    }
}
```

### 内置 NMS 代理

TabooLib 提供以下内置 NMS 代理，直接使用无需额外配置：

| 代理类 | 功能 | 典型方法 |
|--------|------|----------|
| `NMSMessage` | 跨版本聊天消息 | `sendRawActionBar()`, `sendTitle()` |
| `NMSParticle` | 粒子效果 | `sendParticle()` |
| `NMSEntity` | 实体操作 | `spawnEntity()`, `getLanguageKey()` |
| `NMSItemTag` | 物品 NBT 标签 | `getItemTag()`, `setItemTag()`, `getNMSCopy()`, `getBukkitCopy()` |
| `NMSSign` | 告示牌编辑 | `editSign()`, `openSign()` |
| `NMSScoreboard` | 计分板团队 | `setupScoreboard()`, `setDisplayName()`, `changeContent()`, `display()`, `updateTeam()` |
| `NMSTranslate` | 翻译文本 | `translate()` |

**使用示例**：
```kotlin
import taboolib.module.nms.NMSMessage

NMSMessage.instance.sendRawActionBar(player, "{\"text\":\"这是一条跨版本 ActionBar！\"}")
```

### 自定义 NMS 代理

**基础结构**：
```kotlin
package com.example.plugin.nms

import taboolib.module.nms.nmsProxy
import taboolib.module.nms.nmsProxyClass
import taboolib.module.nms.MinecraftVersion
import taboolib.common.util.unsafeLazy
import org.bukkit.entity.Player

/**
 * 自定义 NMS 代理：跨版本发送 ActionBar
 */
abstract class NMSCustomActionBar {

    /** 发送 ActionBar 消息 */
    abstract fun sendActionBar(player: Player, message: String)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomActionBar>() }
    }
}

// Impl - 1.20 版本（Mojang Mapping）
class NMSCustomActionBarImpl : NMSCustomActionBar() {
    override fun sendActionBar(player: Player, message: String) {
        val craftPlayer = player as org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
        val packet = net.minecraft.server.level.ClientboundSetActionBarTextPacket(
            net.minecraft.network.chat.Component.literal(message)
        )
        craftPlayer.handle.connection.send(packet)
    }
}
```

### 版本差异处理

**使用 `MinecraftVersion` 枚举**：
```kotlin
import taboolib.module.nms.MinecraftVersion

abstract class NMSFeature {
    abstract fun doSomething(player: Player)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSFeature>() }
    }
}

// 1.12 实现（旧版 NMS 包名）
class NMSFeatureImpl_V1_12 : NMSFeature() {
    override fun doSomething(player: Player) {
        if (MinecraftVersion.isLower(MinecraftVersion.V1_13)) {
            // 仅 1.12 及以下的逻辑
        }
    }
}

// 1.17+ 实现（新 NMS 包名）
class NMSFeatureImpl : NMSFeature() {
    override fun doSomething(player: Player) {
        // 适用于 1.17 及以上
    }
}
```

**使用 `MinecraftVersion.isHigher/isLower` 运行时判断**：
```kotlin
override fun send(player: Player, message: String) {
    when {
        MinecraftVersion.isLower(MinecraftVersion.V1_12) -> {
            // 1.8 - 1.11 的实现
        }
        MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_12) &&
        MinecraftVersion.isLower(MinecraftVersion.V1_13) -> {
            // 1.12 特定实现
        }
        MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
            // 1.17+ 通用实现
        }
    }
}
```

### require() 字节码级类检查

**`require()` 在字节码层面检查类存在性，运行时零开销**：

```kotlin
import taboolib.module.nms.remap.require

abstract class NMSWithFeature {
    
    /**
     * 检查某些类是否存在，构建时自动排除不可用分支
     */
    fun canUseFeature(): Boolean {
        return require(
            net.minecraft.server.level.ClientboundSetActionBarTextPacket::class.java
        )
    }

    abstract fun sendFeature(player: Player)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSWithFeature>() }
    }
}
```

**为什么用 `require()` 而不是 `Class.forName()`**：
- `require()` 是编译时字节码解析，不触发类加载
- `Class.forName()` 会实际加载类，可能触发 `ClassNotFoundException` 并导致连锁错误
- `require()` 在 ASM 字节码生成阶段就完成了检查

### unsafeLazy 延迟初始化

```kotlin
import taboolib.common.util.unsafeLazy

abstract class MyProxy {
    companion object {
        // ✅ 推荐：unsafeLazy 捕获初始化异常，返回 null 而非崩溃
        val INSTANCE by unsafeLazy { nmsProxy<MyProxy>() }
        
        // ❌ 不推荐：lazy 在代理初始化失败时直接抛出异常
        val BAD by lazy { nmsProxy<MyProxy>() }
    }
}

// 使用时检查 null
fun useProxy(player: Player) {
    MyProxy.INSTANCE?.send(player) ?: run {
        player.sendMessage("当前版本暂不支持此功能")
    }
}
```

### typealias 简化类型

```kotlin
// 避免冗长的完全限定类名
typealias ChatComponent = net.minecraft.network.chat.Component
typealias ActionBarPacket = net.minecraft.server.level.ClientboundSetActionBarTextPacket
typealias CraftPlayer = org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
typealias PacketPlayOutChat = net.minecraft.network.protocol.game.ClientboundSystemChatPacket
```

## Common Mistakes

### ❌ Mistake 1: 使用接口代替抽象类
```kotlin
interface MyNMSProxy {  // ← 错误！nmsProxy 不支持接口
    fun doSomething()
}
val proxy = nmsProxy<MyNMSProxy>()  // ← 运行时异常
```
**Why wrong**: nmsProxy 依赖 ASM 字节码修改，只能对抽象类生成子类代理。接口没有构造函数，无法代理。

**Fix**: 改用抽象类
```kotlin
abstract class MyNMSProxy {  // ← 正确
    abstract fun doSomething()
}
val proxy = nmsProxy<MyNMSProxy>()  // ← 正常工作
```

### ❌ Mistake 2: Impl 类不使用命名约定
```kotlin
abstract class NMSTitle { ... }

class MyCustomTitleSender : NMSTitle() { ... }  // ← 不会被自动发现！
```
**Why wrong**: nmsProxy 扫描类路径时按 `{AbstractClass}Impl` 或 `nmsProxy<T>(className:)` 指定的名称查找实现类。

**Fix**: 遵循命名约定
```kotlin
abstract class NMSTitle { ... }

class NMSTitleImpl : NMSTitle() { ... }  // ← 会被自动发现
```
或者显式指定：
```kotlin
val proxy = nmsProxy<NMSTitle>("com.example.nms.MyCustomTitleSender")
```

### ❌ Mistake 3: 忘记 null 检查
```kotlin
abstract class MyProxy {
    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<MyProxy>() }
    }
}

// ❌ 直接调用，可能在旧版本 NPE
MyProxy.INSTANCE.sendActionBar(player, "Hello")
```
**Why wrong**: 在某些 Minecraft 版本下，Impl 类可能不存在或初始化失败，`unsafeLazy` 返回 `null`。

**Fix**: 添加 null 检查
```kotlin
val proxy = NMSCustomActionBar.INSTANCE
if (proxy != null) {
    proxy.sendActionBar(player, "Hello")
} else {
    player.sendMessage("当前版本不支持此特性")
}
```

### ❌ Mistake 4: 硬编码 NMS 版本包名
```kotlin
class NMSTitleImpl : NMSTitle() {
    override fun send(player: Player, title: String) {
        val packet = net.minecraft.server.v1_20_R4.PacketPlayOutTitle(  // ← 硬编码！
            net.minecraft.server.v1_20_R4.PacketPlayOutTitle.EnumTitleAction.TITLE,
            net.minecraft.server.v1_20_R4.IChatBaseComponent.ChatSerializer.a("{\"text\":\"$title\"}")
        )
        // ...
    }
}
```
**Why wrong**: 硬编码 v1_20_R4 包名在其他版本无法编译或运行。

**Fix**: 使用 `MinecraftVersion` 分支或创建多个 Impl
```kotlin
class NMSTitleImpl : NMSTitle() {
    override fun send(player: Player, title: String) {
        when {
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
                // 使用 mojang mapped 类
            }
            else -> {
                // 使用 obfuscated 类
            }
        }
    }
}
```

### ❌ Mistake 5: 滥用 nmsProxy 代替 Bukkit API
```kotlin
// ❌ Bukkit 已提供完全等效的功能
abstract class PlayerTeleporter {
    abstract fun teleport(player: Player, world: World, x: Double, y: Double, z: Double)
}
```
**Why wrong**: Bukkit API 已经提供了 `player.teleport(Location)`，不需要 NMS 代理。

**Fix**: 优先使用 Bukkit API
```kotlin
player.teleport(Location(world, x, y, z))  // ✅ 跨版本兼容，无需 NMS
```

### ❌ Mistake 6: 在非主线程调用 NMS
```kotlin
submit(async = true) {
    NMSMessage.instance.sendRawActionBar(player, "{\"text\":\"title\"}")  // ← 异步调用 NMS！
}
```
**Why wrong**: NMS 操作涉及数据包发送，必须在主线程执行。

**Fix**: 确保在主线程调用
```kotlin
submit(async = true) {
    val computedValue = heavyCalculation()
    submit(async = false) {
        NMSMessage.instance.sendRawActionBar(player, "{\"text\":\"计算结果: $computedValue\"}")  // ✅ 主线程
    }
}
```

## Minimal Example

**完整的跨版本 ActionBar NMS 代理**：

```kotlin
package com.example.plugin.nms

import taboolib.module.nms.nmsProxy
import taboolib.module.nms.MinecraftVersion
import taboolib.common.util.unsafeLazy
import org.bukkit.entity.Player

abstract class NMSCustomActionBar {

    abstract fun sendActionBar(player: Player, message: String)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomActionBar>() }
    }
}

class NMSCustomActionBarImpl : NMSCustomActionBar() {
    override fun sendActionBar(player: Player, message: String) {
        val craftPlayer = player as org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
        val chatComponent = net.minecraft.network.chat.Component.literal(message)
        val packet = net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(chatComponent)
        craftPlayer.handle.connection.send(packet)
    }
}

// 使用
fun sendActionBar(player: Player, msg: String) {
    NMSCustomActionBar.INSTANCE?.sendActionBar(player, msg)
}
```

## Checklist

Before creating NMS proxy:

- [ ] Verify `module-nms` installed in `build.gradle.kts`
- [ ] Confirm NMS dependency (mapped/universal) is configured
- [ ] Check if feature already exists in built-in NMS proxies (NMSMessage, NMSEntity, etc.)
- [ ] Confirm abstract class (not interface) is used
- [ ] Verify Impl class naming follows `{Abstract}Impl` convention
- [ ] Check `MinecraftVersion` version detection logic is correct
- [ ] Verify `require()` used for conditional class checks (not `Class.forName()`)
- [ ] Confirm `unsafeLazy` used for lazy initialization (not `lazy`)
- [ ] Verify all NMS calls are on main thread

After implementing NMS proxy:

- [ ] Test on target minimum Minecraft version → feature works or falls back gracefully
- [ ] Test on target maximum Minecraft version → no compile/runtime errors
- [ ] Test null-check path when proxy is unavailable
- [ ] Test edge case: player offline / null
- [ ] Verify no `ClassNotFoundException` in startup logs
- [ ] Verify no main thread blocking (if async computation needed)

## Version-Specific Notes

| Minecraft | NMS 包名格式 | 映射来源 | nmsProxy 行为 |
|-----------|-------------|---------|---------------|
| 1.8 - 1.16 | `net.minecraft.server.v{version}_R{revision}` | Obfuscated | 需要带版本包名的 Impl 类 |
| 1.17 - 1.20.4 | `net.minecraft.server` | Spigot mapped (universal) | 通用 Impl 无需版本包名 |
| 1.20.5+ | `net.minecraft.server` | Mojang Mapping | 使用 Mojang 映射名，部分类名变化 |

**TabooLib 6.2.0+**：
- `nmsProxy<T>(className:)` 支持自定义 Impl 类名
- `MinecraftVersion` 枚举包含完整版本信息
- `require()` API 稳定可用

**TabooLib 6.1.x**：
- nmsProxy 功能有限，建议升级到 6.2+

## Troubleshooting

**Error: "Unsupported class type for nmsProxy: interface"**
- Cause: nmsProxy 不能代理接口，只能代理抽象类
- Fix: 将 `interface` 改为 `abstract class`

**Error: "ClassNotFoundException: com.example.MyProxyImpl"**
- Cause: Impl 类命名不符合约定，或未在扫描路径中
- Fix: 确认 Impl 命名遵循 `{Abstract}Impl`，或在 `nmsProxy<T>("full.ClassName")` 指定

**Error: "NoSuchMethodError" at runtime**
- Cause: 方法签名在不同 NMS 版本间变化
- Fix: 使用 `MinecraftVersion` 版本分支，为不同版本提供不同实现

**Warning: "NMS proxy returned null"**
- Cause: 找不到适用于当前版本的 Impl 类
- Fix: 确认所有目标版本都有对应的 Impl，或提供 fallback 逻辑

**Error: "Cannot find symbol" when using NMS classes**
- Cause: 缺少 `compileOnly("ink.ptms.core:...")` 依赖
- Fix: 根据目标版本添加对应的 ptms.core 依赖

## References

- Source code: `taboolib/module/` → `module-nms`
- Package: `taboolib.module.nms` → `nmsProxy`, `nmsProxyClass`, `MinecraftVersion`
- Package: `taboolib.module.nms.remap` → `require`
- Package: `taboolib.common.util` → `unsafeLazy`
- Built-in proxies: `NMSMessage`, `NMSParticle`, `NMSEntity`, `NMSItemTag`, `NMSSign`, `NMSScoreboard`, `NMSTranslate`
- Related cards: `03_command.md` (command integration), `07_scheduler.md` (thread safety)
- Related recipes: `create_nms_proxy.md`
