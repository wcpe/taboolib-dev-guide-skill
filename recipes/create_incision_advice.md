# Recipe: Create Incision Advice Patch

## Use When

- Need to intercept/modify Bukkit/Paper/NMS method at runtime
- Implementing hotfix without waiting for upstream patches
- Adding auditing/logging to specific internal method calls
- Replacing or surrounding specific method logic in third-party code

## Required Checks

- [ ] 确认目标类和方法签名（使用 `/incision list` 查看已注册补丁）
- [ ] 确认使用了 `install("module-incision")` 依赖
- [ ] 选择合适的 Advice 类型（参考决策树）
- [ ] 确认织入时机在 ENABLE 生命周期之后

## Implementation Steps

### Step 1: 理解目标方法 — 使用 list 查看

首先定位你要织入的目标方法。查看已注册的补丁和可用方法：

```
/incision list
```

输出示例（列出所有 Suture 及其状态）。也可以使用 `show` 查看单个补丁详情：

```
/incision show MyPatch#onTick
```

记录完整的方法描述符格式：`类全限定名#方法名(参数类型)返回类型`

### Step 2: 选择正确的 Advice 类型

根据需求使用以下决策树：

```
需要观察方法入口/出口？
  ├─ 入口（日志/监控/参数修改）          → @Lead
  └─ 出口（记录返回值/异常）             → @Trail

需要包围控制流（条件执行/计时/事务）？
  └─ 必须显式 proceed()/override()      → @Splice

需要拦截特定调用点的结果？
  ├─ 替换返回值                         → @Bypass + @Site
  └─ 在调用前后附加逻辑                  → @Graft + @Site

需要修改/过滤返回值？
  └─ 在返回前修改                        → @Trim

需要完全替换方法实现？
  └─ 紧急热修复                          → @Excise
```

### Step 3: 创建 @Surgeon 对象并配置优先级

**File**: `src/main/kotlin/<package>/incision/ExamplePatch.kt`

```kotlin
package com.example.plugin.incision

import taboolib.module.incision.*

@Surgeon
object ExamplePatch {

    // 补丁方法在此定义...

}
```

**约束**:
- 必须是 Kotlin `object`
- 补丁方法签名：`fun handler(theatre: Theatre): Unit`
- 补丁方法不能有除 `Theatre` 以外的参数

### Step 4: 编写 @Lead / @Trail 观察型 Advice

```kotlin
@Operation(priority = 0)
@Lead(
    scope = "method:net.minecraft.server.players.PlayerList#placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V"
)
fun onPlayerJoin(lead: Theatre) {
    // 1. 获取上下文
    val connection = lead.arg<Any>(0)   // 第一个参数
    val player = lead.arg<Any>(1)       // 第二个参数

    // 2. 织入逻辑（在原方法执行前）
    println("[Incision] Player joining: $player")

    // 3. 执行原方法
    lead.proceed()
}

@Operation(priority = 0)
@Trail(
    scope = "method:net.minecraft.server.players.PlayerList#placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V"
)
fun onPlayerJoinDone(trail: Theatre) {
    trail.proceed()
    println("[Incision] Player join complete")
}
```

> **⚠️ @Lead 和 @Trail 默认执行原方法**。如果不调用 `proceed()`，原方法仍然执行——`@Lead`/`@Trail` 不能阻止原方法执行。要阻止原方法请使用 `@Splice` 或 `@Excise`。

### Step 5: 编写 @Splice 包围型 Advice

`@Splice` 包围整个方法体。**必须显式调用 `proceed()` 或 `override()`**：

```kotlin
@Operation(priority = 10)
@Splice(
    scope = "method:net.minecraft.world.entity.Entity#hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
)
fun onEntityHurt(splice: Theatre) {
    val entity = splice.self
    val damageSource = splice.arg<Any>(0)
    val damage = splice.arg<Float>(1)

    // 条件执行：某些情况跳过原方法
    if (shouldCancelDamage(entity, damageSource)) {
        println("[Incision] Damage cancelled for $entity")
        splice.override(false)  // ← 返回 false，不执行原方法
        return
    }

    // 计时包装
    val start = System.nanoTime()
    splice.proceed()  // ← 必须调用！执行原方法
    val elapsed = System.nanoTime() - start

    println("[Incision] Entity#hurt took ${elapsed / 1_000_000}ms")
}
```

> **⚠️ 关键**: `@Splice` 如果不调用 `proceed()` 或 `override()`，原方法体**永远不会执行**！

### Step 6: 编写 @Bypass / @Graft 调用点拦截

`@Bypass` 和 `@Graft` 需要 `Site` 精确定位调用点，**Site 嵌入在注解内部**：

```kotlin
@Operation(priority = 10)
@Bypass(
    method = "net.minecraft.server.level.ServerLevel#tick()V",
    site = Site(
        anchor = Anchor.INVOKE,
        target = "net.minecraft.world.level.chunk.LevelChunk#getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        ordinal = 0   // 第一次调用
    )
)
fun bypassBlockState(bypass: Theatre) {
    // 替换 getBlockState 调用的返回值
    val pos = bypass.arg<Any>(0)
    val cached = blockStateCache[pos]
    if (cached != null) {
        bypass.override(cached)  // ← 短路返回缓存值
    } else {
        bypass.proceed()         // ← 正常执行原调用
    }
}

@Operation(priority = 0)
@Graft(
    method = "net.minecraft.server.level.ServerLevel#tick()V",
    site = Site(
        anchor = Anchor.INVOKE,
        target = "net.minecraft.world.entity.Entity#tick()V",
        ordinal = 0
    )
)
fun onEntityTickGraft(graft: Theatre) {
    println("[Incision] Before entity tick")
    graft.proceed()
    println("[Incision] After entity tick")
}
```

**Site 参数说明**:

| 参数 | 用途 | 示例 |
|------|------|------|
| `anchor` | 信息点类型 | `INVOKE`, `FIELD_GET`, `NEW` 等 |
| `target` | 目标方法/字段描述符 | `"ClassName#method(args)ret"` |
| `ordinal` | 第 N 次匹配（默认 `-1` = 全部；`0` = 第一次，`1` = 第二次） |
| `shift` | 相对偏移方向 | `Shift.BEFORE` = 前一条指令，`Shift.AFTER` = 后一条 |
| `offset` | 字节级偏移量 | 高级用法，通常 `0` |

### Step 7: 访问私有字段/方法

Incision 提供 `field()`、`fieldSet()`、`staticField()`、`method()` lambda 工厂访问私有成员：

```kotlin
@Operation(priority = 0)
@Lead(
    scope = "method:net.minecraft.world.entity.LivingEntity#tick()V"
)
fun onLivingTick(lead: Theatre) {
    val entity = lead.self as? LivingEntity ?: return

    // 实例私有字段 — field<T>("fieldName") 仅返回 getter
    val getHealth = field<Float>("health")       // getter: (Any) -> Float

    // fieldSet<T>("fieldName") 仅返回 setter
    val setHealth = fieldSet<Float>("health")    // setter: (Any, Float) -> Unit

    val currentHealth = getHealth(entity)
    if (currentHealth <= 0) {
        setHealth(entity, 1f)  // 防止死亡
    }

    // 静态私有字段 — staticField<T>(ownerClass, "fieldName") Class 参数在前
    val getServer = staticField<MinecraftServer>(MinecraftServer::class.java, "server")
    val server = getServer(null)

    // 私有方法 — method<ReturnType>("methodName", *paramTypes)
    val invokeDropAll = method<Unit>(
        "dropAllDeathLoot",
        DamageSource::class.java
    )
    invokeDropAll(entity, lastDamageSource)

    lead.proceed()
}
```

> **⚠️ 后端依赖**: JVMTI（自动加载 jar 内 native 库）或 Unsafe 后备。无需手动配置 `-agentpath`。

### Step 8: 版本门控与 @KotlinTarget

**@Version 门控**（限制补丁生效的 Minecraft 版本范围，**方法级别**）：

```kotlin
@Surgeon
object V120Patch {
    @Operation(priority = 0)
    @Version(start = "1.20.1", end = "1.20.4")
    @Lead(
        scope = "method:net.minecraft.server.level.WorldServer#tick()V",
        remap = true  // ← 启用 NMS 名称自动重映射
    )
    fun onTick(lead: Theatre) {
        lead.proceed()
    }
}
```

**@KotlinTarget 双路径覆盖**（处理 Kotlin companion + @JvmStatic，**必须设置参数**）：

```kotlin
@Surgeon
object KotlinUtilsPatch {
    @Operation(priority = 0)
    @Lead(
        scope = "method:com.example.util.PluginUtils#getVersion()Ljava/lang/String;"
    )
    @KotlinTarget(
        companionInstance = true,    // ← 覆盖 companion 内部路径
        jvmStaticBridge = true       // ← 覆盖 @JvmStatic 桥接路径
    )
    fun patchVersion(lead: Theatre) {
        lead.proceed()
    }
}
```

### Step 9: Suture 生命周期管理

在 `ENABLE` 阶段管理补丁状态：

```kotlin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.incision.Scalpel

@Awake(LifeCycle.ENABLE)
fun initPatches() {
    // 获取 Suture
    val suture = Scalpel.find("ExamplePatch#onEntityHurt")

    // 检查状态
    if (suture != null) {
        println("Patch state: ${suture.state}")  // TRIGGERED

        // 暂停补丁
        suture.suspend()
        println("Patch suspended: ${suture.state}")  // SUSPENDED

        // 恢复补丁
        suture.resume()
        println("Patch resumed: ${suture.state}")  // ARMED
    }
}

@Awake(LifeCycle.DISABLE)
fun cleanupPatches() {
    // 遍历移除所有补丁（恢复原始字节码）
    for (suture in Scalpel.sutures) {
        suture.heal()
    }
    println("All patches healed")
}
```

**Suture 状态转换**:
```
INACTIVE_UNRESOLVED → ARMED → enable → TRIGGERED ⇄ suspend/resume → SUSPENDED
                                      ↓
                                   HEALED (heal/close，不可逆)
```

**操作对比**:

| 操作 | 效果 | 可逆？ |
|------|------|--------|
| `suspend()` | 暂停织入逻辑，原方法恢复执行 | ✅ `resume()` 恢复 |
| `heal()` | 移除织入，恢复原始字节码 | ❌ 需重新启用 |
| `close()` | 等同于 `heal()`，永久关闭 Suture | ❌ 不可恢复 |

### Step 10: 验证检查清单

1. **编译验证**:
   ```bash
   ./gradlew build
   ```
   预期：编译成功，无 Incision 相关错误。

2. **运行时验证 — 检查织入状态**:
    ```
    /incision list
    ```
    预期：列出所有 Suture，状态为正确状态。

3. **功能验证 — 触发目标方法**:
   - 触发目标行为（如玩家加入触发 `placeNewPlayer`）
   - 检查控制台日志确认补丁逻辑执行

4. **边界测试**:
   - `suspend()` → 触发目标方法 → 确认补丁暂停
   - `resume()` → 触发目标方法 → 确认补丁恢复
   - `heal()` → 触发目标方法 → 确认原始行为恢复

5. **异常测试**:
   - 补丁抛异常 → 确认不影响原方法（Incision 会捕获并记录）
   - 原方法抛异常 → 确认 `@Trail` 中 `theatre.throwable` 属性可用

## Complete Working Example

一个完整的补丁：给 Bukkit 的 `CraftServer#getOnlinePlayers` 添加日志计时：

**File**: `src/main/kotlin/<package>/incision/ServerPatch.kt`

```kotlin
package com.example.plugin.incision

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.incision.*

@Surgeon
object ServerPatch {

    @Operation(priority = 0, id = "log_get_players")
    @Lead(
        scope = "method:org.bukkit.craftbukkit.v1_20_R1.CraftServer#getOnlinePlayers()Ljava/util/Collection;"
    )
    fun beforeGetPlayers(lead: Theatre) {
        // getOnlinePlayers 无参数
        lead.proceed()
    }

    @Operation(priority = 0, id = "log_get_players_trail")
    @Trail(
        scope = "method:org.bukkit.craftbukkit.v1_20_R1.CraftServer#getOnlinePlayers()Ljava/util/Collection;"
    )
    fun afterGetPlayers(trail: Theatre) {
        trail.proceed()
        println("[Incision] getOnlinePlayers completed")
    }
}

// Suture 管理
@Awake(LifeCycle.ENABLE)
fun onEnable() {
    val count = Scalpel.sutures.size
    println("[ServerPatch] Patches armed: $count")
}

@Awake(LifeCycle.DISABLE)
fun onDisable() {
    for (suture in Scalpel.sutures) {
        suture.heal()
    }
}
```

## Troubleshooting During Creation

| 症状 | 可能原因 | 诊断方法 |
|------|---------|---------|
| 编译错误 "Unresolved reference: @Surgeon" | 未安装 `module-incision` | 检查 `build.gradle.kts` → `install("module-incision")` |
| 补丁不触发 | 方法描述符错误 / 类名错误 | 使用 `/incision list` 和 `/incision show <id>` 确认 |
| "Ambiguous method" | 重载方法未指定参数类型 | 添加完整参数描述符（含括号） |
| "No such method" | 目标类版本不匹配 | 确认 Minecraft 版本和 NMS 包名 |
| `@Splice` 导致原方法不执行 | 忘记调用 `proceed()` | 确保 handler 中调用了 `proceed()` 或 `override()` |
| JVMTI 降级警告 | native 库未打包 | 无需手动配置；检查 `module-incision` 安装 |
| Kotlin companion 补丁不完整 | 缺少 `@KotlinTarget` 参数 | 添加 `@KotlinTarget(companionInstance = true, jvmStaticBridge = true)` |

## References

- Card: `13_incision.md`
- Example: `examples/incision_basic.kt`
- Related cards: `02_lifecycle.md` (CONST/ENABLE timing)
