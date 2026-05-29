# Recipe: Create NMS Proxy

## Use When

- Need to create a custom NMS proxy for cross-version Minecraft API access
- Implementing features not covered by built-in proxies (NMSMessage, NMSEntity, etc.)
- Requiring direct NMS calls (packets, entities, world operations)
- Supporting multiple Minecraft versions (1.12 ~ 1.21+) in a single plugin

## Required Checks

Before creating NMS proxy:

- [ ] Read user's existing NMS proxy files to match naming/style
- [ ] Verify `module-nms` installed in `build.gradle.kts`
- [ ] Check if built-in proxy already covers the feature (NMSMessage, NMSParticle, etc.)
- [ ] Confirm user's target Minecraft version range
- [ ] Check for existing `MinecraftVersion` usage to understand version detection patterns

## Implementation Steps

### Step 1: Verify Module Setup

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("module-nms")  // ← Required for nmsProxy
    
    // NMS 核心依赖（根据需要选择）
    compileOnly("ink.ptms.core:v12004:12004:mapped")     // 已映射 NMS 类
    compileOnly("ink.ptms.core:v12004:12004:universal")  // 通用 NMS 支持
}

taboolib {
    env {
        install("module-nms")
    }
}
```

**验证**：
```bash
grep -r "module-nms" build.gradle.kts
```

### Step 2: Define Abstract Class

**文件位置**: `src/main/kotlin/<package>/nms/NMSCustomFeature.kt`

```kotlin
package com.example.plugin.nms

import taboolib.module.nms.nmsProxy
import taboolib.common.util.unsafeLazy
import org.bukkit.entity.Player

/**
 * 自定义 NMS 代理：跨版本 [功能描述]
 *
 * 使用 unsafeLazy 确保在无法加载代理时不崩溃
 */
abstract class NMSCustomFeature {

    /** 核心方法：描述功能 */
    abstract fun doFeature(player: Player, param: String)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomFeature>() }
    }
}
```

**关键规则**：
- 必须是 `abstract class`，不能是 `interface`
- 所有 NMS 操作方法声明为 `abstract`
- Companion object 使用 `unsafeLazy` + `nmsProxy<T>()`
- `unsafeLazy` 在初始化失败时返回 `null`（不会崩溃）

### Step 3: Create Impl Class

**文件位置**: `src/main/kotlin/<package>/nms/NMSCustomFeatureImpl.kt`

**命名约定**: `{AbstractClassName}Impl`（自动发现），或自定义名（需要显式传递）

```kotlin
package com.example.plugin.nms

import taboolib.module.nms.MinecraftVersion
import org.bukkit.entity.Player

/**
 * {@link NMSCustomFeature} 的 1.20+ 实现
 *
 * 使用 Mojang Mapping 类名
 */
class NMSCustomFeatureImpl : NMSCustomFeature() {

    override fun doFeature(player: Player, param: String) {
        val craftPlayer = player as org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
        val serverPlayer = craftPlayer.handle

        // NMS 操作...
    }
}
```

**如果目标多个版本**，创建多个 Impl：

```kotlin
// 1.12 实现
class NMSCustomFeatureImpl_V1_12 : NMSCustomFeature() {
    override fun doFeature(player: Player, param: String) {
        val craftPlayer = player as org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
        val entityPlayer = craftPlayer.handle
        // 1.12 特定 NMS 调用...
    }
}

// 1.20+ 通用实现（作为默认 fallback）
class NMSCustomFeatureImpl : NMSCustomFeature() {
    override fun doFeature(player: Player, param: String) {
        val craftPlayer = player as org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
        val serverPlayer = craftPlayer.handle
        // 1.20+ NMS 调用...
    }
}
```

**Impl 发现规则**：
1. TabooLib 扫描所有 `{Abstract}Impl` 命名的类
2. 每个 Impl 的构造函数会被检查：如果构造函数中引用了不存在的类（通过 `require()` 检查），则该 Impl 被跳过
3. 如果所有 Impl 都被跳过 → `unsafeLazy` 返回 `null`

### Step 4: Handle Version Differences

**在单个 Impl 中使用 `MinecraftVersion` 条件分支**：

```kotlin
import taboolib.module.nms.MinecraftVersion

class NMSCustomFeatureImpl : NMSCustomFeature() {

    override fun doFeature(player: Player, param: String) {
        when {
            // 1.17 以下使用旧版 NMS 包名
            MinecraftVersion.isLower(MinecraftVersion.V1_12) -> {
                // 1.8 - 1.11 实现
                val craftPlayer = player as org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
                // NMS 调用...
            }
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_12) &&
            MinecraftVersion.isLower(MinecraftVersion.V1_13) -> {
                // 1.12 特定实现
                val craftPlayer = player as org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
                // NMS 调用...
            }
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_13) &&
            MinecraftVersion.isLower(MinecraftVersion.V1_17) -> {
                // 1.13 - 1.16 实现
                val craftPlayer = player as org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
                // NMS 调用...
            }
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
                // 1.17+ Universal Remap / Mojang Mapping
                val craftPlayer = player as org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
                // NMS 调用...
            }
        }
    }
}
```

**常用版本检测 API**：
```kotlin
MinecraftVersion.major             // 支持的版本索引（0=1.8 到 14=V26_1），用于 Impl 匹配
MinecraftVersion.versionId         // 版本 ID 数值（如 12004 表示 1.20.4）
MinecraftVersion.runningVersion    // 当前运行版本字符串（如 "1.20.4"）

// 版本比较（推荐使用 isHigher/isLower 系列方法）
MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17)
MinecraftVersion.isLower(MinecraftVersion.V1_12)
MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_20)

// majorLegacy 已弃用，返回 versionId（如 11200 表示 1.12），改用 isHigher/isLower
```

### Step 5: Use require() for Class Existence Checks

```kotlin
abstract class NMSCustomFeature {

    /**
     * 使用 require() 在字节码级别检查类是否存在
     * 如果类不存在，canUseFeature() 在编译后代码中被优化为直接返回 false
     * 零运行时开销 —— 比 Class.forName() 更高效安全
     */
    fun canUseFeature(): Boolean {
        return require(net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket::class.java)
                && require(net.minecraft.network.chat.Component::class.java)
    }

    abstract fun doFeature(player: Player, param: String)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomFeature>() }
    }
}
```

**`require()` vs `Class.forName()` 对比**：

| 方案 | 检查时机 | 开销 | 安全性 |
|------|---------|------|--------|
| `require()` | ASM 翻译时（编译后字节码） | 零运行时开销 | 不会触发类加载 |
| `Class.forName()` | 运行时 | 触发类加载、可能抛异常 | 可能导致连锁错误 |
| `try-catch` | 运行时 | 异常处理开销 | 可行但冗余 |

### Step 6: Use typealias for Cleaner Code

在代理类文件顶部或独立的 `TypeAliases.kt` 中定义类型别名：

```kotlin
// 避免 Impl 代码中冗长的完全限定类名
typealias CraftPlayer = org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
typealias ServerPlayer = net.minecraft.server.level.ServerPlayer
typealias ChatComponent = net.minecraft.network.chat.Component
typealias MutableComponent = net.minecraft.network.chat.MutableComponent
typealias PlayerConnection = net.minecraft.server.network.ServerGamePacketListenerImpl
typealias Packet = net.minecraft.network.protocol.Packet
```

**在 Impl 中使用**：
```kotlin
class NMSCustomFeatureImpl : NMSCustomFeature() {

    override fun doFeature(player: Player, param: String) {
        val craftPlayer = player as CraftPlayer
        val serverPlayer: ServerPlayer = craftPlayer.handle
        val connection: PlayerConnection = serverPlayer.connection

        val message: MutableComponent = ChatComponent.literal(param)
        val packet = ClientboundSetActionBarTextPacket(message)

        connection.send(packet)
    }
}
```

### Step 7: Integrate into Plugin Code

**在命令中使用**：
```kotlin
@CommandBody
val actionbar = subCommand {
    dynamic("message") {
        execute<ProxyPlayer> { player, context, _ ->
            val message = context.argument(0)

            // ✅ null 检查确保安全
            val proxy = NMSCustomActionBar.INSTANCE
            if (proxy != null) {
                proxy.sendActionBar(player, message)
            } else {
                player.sendMessage("§c当前版本不支持 ActionBar")
            }
        }
    }
}
```

**在监听器中使用**：
```kotlin
import taboolib.common.platform.event.SubscribeEvent
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoinListener {

    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        NMSCustomActionBar.INSTANCE?.sendActionBar(
            player,
            "§a欢迎回来，${player.name}！"
        )
    }
}
```

**在服务层中使用**：
```kotlin
@Component
class NotificationService {

    fun sendWelcomeMessage(player: Player) {
        NMSCustomActionBar.INSTANCE?.sendActionBar(player, "§e欢迎你！")
            ?: player.sendMessage("欢迎！")
    }
}
```

## Complete Working Example: Custom ActionBar Sender

### 文件 1: `NMSCustomActionBar.kt`（抽象类）

```kotlin
package com.example.plugin.nms

import taboolib.module.nms.nmsProxy
import taboolib.module.nms.remap.require
import taboolib.common.util.unsafeLazy
import org.bukkit.entity.Player

typealias CraftPlayer_120R4 = org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
typealias ServerPlayer = net.minecraft.server.level.ServerPlayer
typealias Component = net.minecraft.network.chat.Component
typealias ActionBarPacket = net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket

abstract class NMSCustomActionBar {

    abstract fun sendActionBar(player: Player, message: String)

    fun isSupported(): Boolean = require(ActionBarPacket::class.java)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomActionBar>() }
    }
}
```

### 文件 2: `NMSCustomActionBarImpl.kt`（实现）

```kotlin
package com.example.plugin.nms

import taboolib.module.nms.MinecraftVersion
import org.bukkit.entity.Player

class NMSCustomActionBarImpl : NMSCustomActionBar() {

    override fun sendActionBar(player: Player, message: String) {
        when {
            MinecraftVersion.isLower(MinecraftVersion.V1_13) -> {
                // 1.8 - 1.12
                val craftPlayer = player as CraftPlayer_112R1
                val packet = net.minecraft.server.v1_12_R1.PacketPlayOutChat(
                    net.minecraft.server.v1_12_R1.ChatComponentText(message),
                    net.minecraft.server.v1_12_R1.ChatMessageType.GAME_INFO
                )
                craftPlayer.handle.playerConnection.sendPacket(packet)
            }
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
                // 1.17+ (Universal remap / Mojang mapping)
                val craftPlayer = player as CraftPlayer_120R4
                val component = Component.literal(message)
                val packet = ActionBarPacket(component)
                craftPlayer.handle.connection.send(packet)
            }
        }
    }
}

// 1.12 的类型别名
typealias CraftPlayer_112R1 = org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
```

### 文件 3: 使用示例（命令）

```kotlin
package com.example.plugin.command

import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyPlayer
import com.example.plugin.nms.NMSCustomActionBar

@CommandHeader(
    name = "actionbar",
    permission = "actionbar.use"
)
object ActionBarCommand {

    @CommandBody
    val send = subCommand {
        dynamic("message") {
            execute<ProxyPlayer> { player, context, _ ->
                val message = context.argument(0)

                val proxy = NMSCustomActionBar.INSTANCE
                if (proxy != null && proxy.isSupported()) {
                    proxy.sendActionBar(player, message)
                } else {
                    player.sendMessage("§c当前服务器版本不支持 ActionBar")
                }
            }
        }
    }

    @CommandBody
    val main = mainCommand {
        execute<ProxyPlayer> { player, _, _ ->
            player.sendMessage("§e/actionbar send <message> - 发送 ActionBar 消息")
        }
    }
}
```

## Verification Steps

### Compile Check
```bash
./gradlew build
```
**Expected**: 编译成功，无错误

### Runtime Check
1. 启动服务器，插件加载
2. 检查控制台日志，不应出现 `ClassNotFoundException` 或 NMS 相关错误

### Functional Test
1. 加入游戏，执行 `/actionbar send HelloWorld`
   - **Expected**: 看到 ActionBar 消息 "HelloWorld"
2. 使用彩色代码：`/actionbar send §c红色文字`
   - **Expected**: 看到红色 ActionBar 消息
3. 在不支持的环境测试（如纯 Bukkit）
   - **Expected**: 显示 "当前服务器版本不支持 ActionBar"

### Edge Case Test
1. 发送空消息：`/actionbar send ""`
   - **Expected**: 不崩溃（由 Impl 或调用方处理）
2. 快速重复发送
   - **Expected**: 无内存泄漏，性能稳定
3. 玩家退出后发送
   - **Expected**: 静默忽略或记录警告

### Version Matrix Test
如果条件允许，在以下版本测试：

| Minecraft 版本 | 预期行为 |
|---------------|---------|
| 1.12.2 | ActionBar 正常显示（旧版 NMS） |
| 1.16.5 | ActionBar 正常显示（obfuscated mapping） |
| 1.20.4 | ActionBar 正常显示（universal remap） |
| 1.21.x | ActionBar 正常显示（Mojang mapping） |

## Common Issues

**No Impl found, proxy returns null**:
- Check Impl class name matches `{Abstract}Impl` convention
- Verify Impl class is in the same package or scanned path
- Check if `require()` 检查的类确实存在于当前 NMS 依赖中

**ClassNotFoundException**:
- Confirm `compileOnly("ink.ptms.core:...")` 依赖正确配置
- 检查 NMS 依赖版本与目标 Minecraft 版本匹配
- 不同版本使用不同的 Impl 类

**NoSuchMethodError at runtime**:
- NMS 方法签名因版本而异
- 使用 `MinecraftVersion` 版本分支创建多个实现
- 检查 Mojang Mapping 变化（1.20.5+ 部分方法名改变）

**Thread safety**:
- NMS 操作（数据包发送）必须在主线程
- 异步计算结果通过 `submit(async = false)` 切回主线程再发送

## References

- Card: `12_nms_proxy.md`
- Example: `nms_proxy_basic.kt`
- Related cards: `03_command.md` (command integration), `06_listener.md` (event listener), `07_scheduler.md` (thread safety)
