# Card 13: Incision 运行时字节码织入

## When to Use

Load this card when:
- User asks about "incision", "字节码", "bytecode", "weaving", "切面", "AOP", "@Surgeon", "Scalpel"
- Need to patch/modify Bukkit/Paper/NMS/Minecraft source code at runtime
- Implementing hotfix for server bugs without waiting for upstream patches
- Intercepting specific method calls in third-party libraries
- Need controllable, rollbackable bytecode manipulation

## Core Idea

Incision 是 TabooLib 的**运行时字节码织入框架**，允许在 Bukkit/Paper/NMS 目标方法执行时注入自定义逻辑。它不是动态代理、不是编译期 Mixin、不是通用 AOP 框架——它是**外科手术式的、可控可回滚的字节码补丁系统**。通过 `@Surgeon` 注解定义补丁，在 `CONST` 阶段扫描目标类，在 `ENABLE` 阶段应用织入。

## ⚠️ Incision 是什么 / 不是什么

### ✅ Incision 是

| 特性 | 说明 |
|------|------|
| **运行时字节码织入** | 在类加载后修改字节码，无需重新编译目标类 |
| **可控的补丁系统** | 通过 Suture 生命周期管理：启用→暂停→恢复→关闭 |
| **可诊断的** | 内置 Forensics/Trauma/Checkup 诊断工具 |
| **可回滚的** | `heal()` 可恢复到原始字节码 |
| **手术式精确织入** | 通过 Anchor + Site 精确定位织入点 |

### ❌ Incision 不是

| 不是 | 应该用什么 |
|------|-----------|
| **动态代理** → | Java Proxy / CGLIB |
| **编译期 Mixin** → | Mixin / Sponge Mixin |
| **通用 AOP 框架** → | Spring AOP / AspectJ |
| **方法替换**（整个类） → | 用 Mixin，Incision 擅长粒度的精确补丁 |
| **热重载框架** → | JRebel / DCEVM |

## 两种入口模式

### 注解模式 (@Surgeon) — 推荐用于长期维护的补丁

```kotlin
@Surgeon
object MyPatch {
    @Operation(priority = 10)
    @Lead(scope = "method:net.minecraft.server.level.WorldServer#tick()V")
    fun onTick(lead: Theatre) {
        val self = lead.self  // WorldServer 实例
        lead.proceed()        // 继续执行原方法
    }
}
```

### DSL 模式 (Scalpel) — 用于临时/作用域补丁

```kotlin
val scalpel = Scalpel().apply {
    operation("temp_patch", 0) {
        lead {
            anchor("net.minecraft.server.level.WorldServer#tick()V")
            handler { theatre ->
                theatre.proceed()
            }
        }
    }
}
scalpel.enable()
```

> **选择指南**: 长期维护 → `@Surgeon`；一次性测试/调试 → `Scalpel`；需要在运行时动态创建/销毁 → `Scalpel`

## 7 种 Advice 类型

| Advice | 织入位置 | 是否替换原逻辑 | 典型用途 | 关键约束 |
|--------|---------|--------------|---------|---------|
| **@Lead** | 方法入口（头部） | ❌ 不替换 | 日志记录、参数校验、性能监控 | 默认执行原方法，可通过 `override()` 阻止 |
| **@Trail** | 方法出口（返回/异常） | ❌ 不替换 | 返回值日志、异常捕获、结果统计 | 通过 `proceed()` 获取原方法执行结果 |
| **@Splice** | 方法体包围 | ⚠️ 必须显式 `proceed()` 或 `override()` | 条件执行、性能计时、事务控制 | **必须**调用 `proceed()` 或 `override()`，否则原方法不执行 |
| **@Graft** | 指定调用点（调用方） | ❌ 不替换 | 拦截特定 API 调用、Mock 外部依赖 | 需要 `Site` 精确定位调用点（嵌入注解内） |
| **@Bypass** | 指定调用点（替换调用） | ✅ 替换该调用的结果 | 替换方法返回值、短路优化 | 必须设置 `Site` 定位调用点（嵌入注解内）；原调用不会执行 |
| **@Trim** | 方法返回前 | ⚠️ 可修改返回值 | 数据脱敏、返回值过滤/转换 | `method` 参数指定目标方法描述符；Kind: ARG, RETURN, VAR |
| **@Excise** | 整个方法替换 | ✅ 完全替换 | 彻底重写方法逻辑、紧急热修复 | 原方法体完全不执行；必须使用 `override()` 提供新逻辑 |

### Advice 选择决策树

```
观察方法入口/出口 → @Lead / @Trail
需要包围控制流(条件执行/计时) → @Splice
拦截特定调用点返回结果 → @Bypass + @Site
在调用点前后附加逻辑 → @Graft + @Site
修改/过滤返回值 → @Trim
完全替换方法实现 → @Excise
```

## Mixin 对照表

熟悉 Mixin 的开发者可以直接映射：

| Mixin 注解 | Incision 等价 | 差异说明 |
|-----------|--------------|---------|
| `@Inject(at = @At("HEAD"))` | `@Lead` | 均注入方法头，Incision 支持 `override()` 阻止原逻辑 |
| `@Inject(at = @At("RETURN"))` | `@Trail` | 均可获取返回值 |
| `@Inject(at = @At("INVOKE"))` | `@Graft` + `@Site` | Incision 通过 `@Site(anchor = Anchor.INVOKE)` 定位 |
| `@Redirect` | `@Bypass` | 均替换方法调用，Incision 需要 `@Site` 精确定位 |
| `@Overwrite` | `@Excise` | 均完全替换方法体 |
| `@ModifyArg` | `@Lead` + `proceed(newArgs)` | Incision 通过 `proceed()` 传参修改 |
| `@ModifyVariable` / `@ModifyConstant` | 无直接等价 | Incision 不直接支持局部变量修改，需用 `@Splice` 包围替代 |
| `@WrapOperation` | `@Splice` | 包围控制流，Incision 须显式 `proceed()` 或 `override()` |

## 注解模式详解

### @Surgeon

标记一个 Kotlin `object` 为外科医生（补丁容器）。Incision 在 `CONST` 阶段扫描所有 `@Surgeon` 对象。

```kotlin
@Surgeon(priority = 0)  // 默认优先级（所有方法的 fallback）
object MyPatch {
    // 补丁方法...
}
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `priority` | `Int` | `0` | 默认优先级，所有 @Operation 的 fallback 值 |

**约束**:
- 必须是 Kotlin `object`（单例）
- 补丁方法必须接收 `Theatre` 参数
- 补丁方法返回 `Unit`

### @Operation

配置补丁方法的元数据。

```kotlin
@Operation(
    priority = 10,              // 优先级：多个补丁同时生效时的执行顺序（Int）
    id = "custom_patch_id",     // 自定义 ID（默认使用类名#方法名）
    enabled = true              // 是否启用（默认为 true）
)
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `priority` | `Int` | `0` | 执行优先级：数值越大越先执行 |
| `id` | `String` | `"类名#方法名"` | 补丁唯一标识，用于 Suture 管理 |
| `enabled` | `Boolean` | `true` | 是否启用此补丁 |

### @Lead 示例

```kotlin
@Surgeon
object TickMonitor {
    @Operation(priority = 0)
    @Lead(scope = "method:net.minecraft.server.level.WorldServer#tick()V")
    fun onWorldTick(lead: Theatre) {
        val world = lead.self
        val startTime = System.nanoTime()
        lead.proceed()  // 执行原方法
        val elapsed = System.nanoTime() - startTime
        println("World tick took ${elapsed / 1_000_000}ms")
    }
}
```

### @Trail 示例

```kotlin
@Surgeon
object ReturnLogger {
    @Operation(priority = 0)
    @Trail(scope = "method:org.bukkit.craftbukkit.v1_20_R1.CraftServer#getOnlinePlayers()Ljava/util/Collection;")
    fun onGetPlayers(trail: Theatre) {
        trail.proceed()
        println("getOnlinePlayers completed")
    }
}
```

## Theatre 上下文对象

每个 Advice 方法都接收一个 `Theatre` 对象，提供对当前织入上下文的访问：

| 属性/方法 | 类型/签名 | 说明 |
|----------|----------|------|
| `self` | `val self: Any?` | 当前方法所属实例的属性（静态方法返回 null） |
| `args` | `val args: Array<Any?>` | 当前方法的参数数组属性 |
| `target` | `MethodCoordinate` | 方法坐标描述符，包含 owner/name/descriptor |
| `throwable` | `val throwable: Throwable?` | 异常属性（@Trail 中当方法抛异常时可用） |
| `arg(index)` | `<T> T` | 按索引获取参数：`theatre.arg<Player>(0)` |

### Resume 控制方法

| 方法 | 说明 | 适用 Advice |
|------|------|-----------|
| `proceed()` | 继续执行原方法逻辑（默认参数） | 所有 |
| `proceed(newArgs)` | 以新参数继续执行原方法 | @Lead, @Splice |
| `proceedResult(value)` | 返回指定值作为原方法结果（**不执行原方法体**） | @Splice, @Lead |
| `override(value)` | 跳过原方法，直接返回指定值（等同于 `skip()`） | @Excise, @Splice, @Lead |
| `skip(value)` | 跳过原方法执行，返回指定值（与 `override()` 行为相同） | @Splice, @Lead |

```kotlin
// proceed() — 正常继续
lead.proceed()

// proceed(newArgs) — 传递修改后的参数
lead.proceed(arrayOf(modifiedArg1, arg2))

// proceedResult(value) — 短路返回（不执行原方法）
splice.proceedResult("cached_value")

// override(value) — 短路返回（等同于 skip）
splice.override("new_result")

// skip(value) — 短路返回（等同于 override）
lead.skip("skipped_result")
```

> **⚠️ 关键区别**: `proceedResult()` 会触发 `@Trail` advice，而 `override()` 和 `skip()` 直接返回且不触发任何后续处理。

## Suture 生命周期管理

每个补丁（Operation）都有对应的 `Suture` 对象，代表一次织入操作的状态：

```
INACTIVE_UNRESOLVED → ARMED → TRIGGERED → SUSPENDED → HEALED
        ↑               ↑         ↑           ↑          ↑
  声明未成功解析    已准备    已激活触发   已挂起   已愈合(关闭)
```

### 5 种 Suture 状态

| 状态 | 说明 |
|------|------|
| `INACTIVE_UNRESOLVED` | 声明未成功解析（目标类/方法未找到） |
| `ARMED` | 已解析就绪，等待织入 |
| `TRIGGERED` | 已激活，织入生效中 |
| `SUSPENDED` | 已挂起，织入存在但不触发 |
| `HEALED` | 已愈合（关闭），织入已移除 |

### Suture 操作

```kotlin
// 获取 Suture — 通过 Scalpel 查找
val suture = Scalpel.find("补丁ID")
// 或通过完整 ID
val suture = Scalpel.find("com.example.MyPatch#methodName")

suture.heal()      // HEALED — 移除织入，恢复原始字节码
suture.suspend()   // SUSPENDED — 暂停补丁（不触发但织入存在）
suture.resume()    // ARMED — 从挂起恢复到就绪状态
suture.close()     // HEALED — 等同于 heal()，永久关闭，不可恢复
```

### 生命周期状态转换

```
INACTIVE_UNRESOLVED ──[解析成功]──▶ ARMED ──[enable]──▶ TRIGGERED ──[suspend]──▶ SUSPENDED
                                                                                      │
                                                                                  [resume]
                                                                                      │
                                                                                      ▼
                                                                                   ARMED
                                                                                      │
                                                                                  [enable]
                                                                                      │
TRIGGERED ──[heal/close]──▶ HEALED                                                    ▼
                                                                                 TRIGGERED
```

> **⚠️ 注意**: `heal()` 和 `close()` 行为完全相同，执行后 Suture 无法再恢复。如果需要可逆操作，使用 `suspend()`。

## 私有字段/方法访问

Incision 提供 `field()`、`fieldSet()`、`staticField()`、`method()` 四个 lambda 工厂函数，用于访问私有成员：

### field() / fieldSet() — 实例字段访问

```kotlin
// field<T>() 仅返回 getter
val getName = field<String>("name")                          // getter: (Any) -> String

// fieldSet<T>() 仅返回 setter
val setName = fieldSet<String>("name")                       // setter: (Any, String) -> Unit

val entity = theatre.self as? Entity ?: return
val name = getName(entity)     // 读取私有字段
setName(entity, "NewName")     // 写入私有字段
```

### staticField() — 静态字段访问

```kotlin
// staticField<T>(ownerClass, name) — Class 参数在前
val getServer = staticField<MinecraftServer>(MinecraftServer::class.java, "server")
val server = getServer(null)  // 静态字段传 null 或任意值
```

### method() — 私有方法调用

```kotlin
// 获取私有方法的调用 lambda
// method<ReturnType>(methodName, *paramTypes)
val invokeHandleDeath = method<Unit>("handleDeath", DamageSource::class.java)

val entity: LivingEntity = theatre.self as? LivingEntity ?: return
invokeHandleDeath(entity, damageSource)
```

### 后端技术

Incision 使用多种后端访问私有成员和执行字节码织入：

| 后端 | 技术 | 适用场景 |
|------|------|---------|
| **JVMTI Backend** | JVM TI native agent（自动加载 jar 内 native 库） | 生产环境，稳定可靠 |
| **Unsafe Fallback** | `sun.misc.Unsafe` | JVMTI 不可用时的后备方案 |
| **ClassLoaderHookBackend** | ClassLoader hook | 类加载时织入 |
| **PipelineBackend** | 管道式织入 | 链式处理 |

> **⚠️ JVMTI 后端自动从 jar 内加载 native 库**，无需手动添加 `-agentpath` JVM 参数。如果 JVMTI 不可用，Incision 自动降级到 Unsafe 方式。

## Anchor 类型

Anchor 定义织入点在目标方法中的精确位置：

| Anchor | 位置说明 | 配合 Advice | 示例 |
|--------|---------|------------|------|
| `HEAD` | 方法入口第一条指令 | @Lead | 方法开始时织入 |
| `TAIL` | 方法所有退出点 | @Trail, @Trim | 方法返回/异常时织入 |
| `RETURN` | 特定 return 指令 | @Trail, @Trim | 特定返回点织入 |
| `INVOKE` | 方法调用指令处 | @Graft, @Bypass | 拦截特定方法调用 |
| `FIELD_GET` | 字段读取指令 | @Graft | 拦截字段访问 |
| `FIELD_PUT` | 字段写入指令 | @Graft | 拦截字段赋值 |
| `NEW` | 对象创建指令 | @Graft | 拦截 `new` 操作 |
| `THROW` | 异常抛出指令 | @Trail | 拦截异常抛出点 |

## Site 参数

`Site` 用于 `@Graft` 和 `@Bypass`，精确定位调用点。**Site 嵌入在注解内部**（不是单独的 `@Site` 注解）：

| 参数 | 类型 | 说明 |
|------|------|------|
| `anchor` | `Anchor` | 定位类型（默认 `INVOKE`） |
| `target` | `String` | 目标描述符（方法/字段） |
| `shift` | `Shift` | 偏移方向枚举：`Shift.BEFORE` / `Shift.AFTER` |
| `ordinal` | `Int` | 第 N 次匹配（默认 `-1` = 所有匹配；`0` = 第一次；`1` = 第二次） |
| `offset` | `Int` | 字节码偏移量（高级用法，通常不需要设置） |

```kotlin
@Operation(priority = 0)
@Bypass(
    method = "net.minecraft.world.level.Level#tick()V",
    site = Site(
        anchor = Anchor.INVOKE,
        target = "net.minecraft.world.level.chunk.ChunkAccess#getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        ordinal = 0
    )
)
fun bypassBlockState(bypass: Theatre) {
    // 替换 ChunkAccess#getBlockState 调用的返回值
    bypass.override(cachedBlockState)
}
```

## 方法描述符格式

Incision 使用方法描述符来定位目标方法，**必须包含参数类型的括号**：

```
格式: owner#methodName(paramType1,paramType2)returnType
```

### 描述符示例

```kotlin
// 完整格式
"org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer#getHandle()Lnet/minecraft/server/level/ServerPlayer;"

// void 方法
"net.minecraft.server.level.WorldServer#tick()V"

// 带参数的完整格式
"net.minecraft.world.entity.Entity#hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
```

> **⚠️ 注意**: 当目标方法有重载时，**必须**指定完整的参数类型描述符，否则 Incision 无法确定织入哪个方法。不允许省略括号（如 `owner#method` 不带 `()` — 无法解析）。

## @Version 版本门控

当补丁针对特定 Minecraft 版本时，在 `@Operation` 方法上使用 `@Version` 限制生效版本：

```kotlin
@Surgeon
object V120Patch {
    @Operation(priority = 0)
    @Version(
        start = "1.20.1",    // 最低版本
        end = "1.20.4"       // 最高版本（可选，不写表示无上限）
    )
    @Lead(scope = "method:net.minecraft.server.level.WorldServer#tick()V")
    fun onTick(lead: Theatre) {
        lead.proceed()
    }
}
```

> **⚠️ 注意**: `@Version` 是**方法级别**注解（非类级别），每个补丁方法独立声明版本范围。

配合 NMS 版本重映射（`remap`）：

```kotlin
@Surgeon
object NMSPatch {
    @Operation(priority = 10)
    @Version(start = "1.20")
    @Lead(
        scope = "method:net.minecraft.server.level.WorldServer#tick()V",  // Mapped 名称
        remap = true    // ← 启用 NMS 名称重映射
    )
    fun onTick(lead: Theatre) { ... }
}
```

`remap = true` 让 Incision 根据 `mappings` 配置自动将 Mapped 名称转换为运行时混淆名称。

## @KotlinTarget 双路径覆盖

当需要同时覆盖 Kotlin `companion object` 方法和 `@JvmStatic` 生成的静态桥接方法时使用。**必须指定布尔参数**，裸注解无效：

```kotlin
@Surgeon
object KotlinPatch {
    @Operation(priority = 0)
    @Lead(
        scope = "method:com.example.TargetClass#staticMethod()V"
    )
    @KotlinTarget(
        companionInstance = true,    // 覆盖 companion 内部路径
        jvmStaticBridge = true       // 覆盖 @JvmStatic 桥接路径
    )
    fun patch(lead: Theatre) {
        lead.proceed()
    }
}
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `companionInstance` | `Boolean` | `false` | 是否覆盖 companion object 实例方法 |
| `jvmStaticBridge` | `Boolean` | `false` | 是否覆盖 @JvmStatic 静态桥接方法 |

**为什么需要**: Kotlin 的 `@JvmStatic` 会生成两套方法签名——一个在 companion 内部，一个在外部类作为静态桥接。不加 `@KotlinTarget` 只能覆盖其中一条路径。两个参数均需显式设为 `true` 才生效。

## 四种后端

| 后端 | 技术 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| **InstrumentationBackend** | `java.lang.instrument` + self-attach | 无需修改 JVM 参数 | Java 9+ 需要 `--add-opens`，部分环境限制 | 开发/测试环境 |
| **JvmtiBackend** | JVM TI native agent（自动加载） | 稳定、无限制、生产级 | 需要 native 库存在于 classpath | **生产环境推荐** |
| **ClassLoaderHookBackend** | ClassLoader hook | 类加载时拦截 | 仅对未加载类有效 | 早期织入 |
| **PipelineBackend** | 管道式织入 | 链式处理，可组合 | 性能开销较大 | 复杂织入流程 |

### 启用 JvmtiBackend

JVMTI 后端自动从 jar 内加载 native 库，**无需手动添加 `-agentpath` JVM 参数**。若未检测到 native 库，Incision 自动降级为 InstrumentationBackend。

## Incision 生命周期

```
CONST 阶段                     ENABLE 阶段
   │                               │
扫描所有 @Surgeon 和 Scalpel      应用字节码织入
解析方法描述符                     启用 Operation
构建 Advice 树                    生成 Suture
验证目标类存在                     启动诊断监控
   │                               │
   ▼                               ▼
 ARMING ────────────────────▶ ARMED ──▶ TRIGGERED
```

> **⚠️ 关键**: `@Surgeon` 的扫描发生在 `CONST` 阶段，但织入发生在 `ENABLE` 阶段。在 `CONST` 阶段不要尝试访问 Suture——它们还不存在。

## 诊断工具

### list — 列出所有补丁

列出所有已注册的 Suture：

```kotlin
// 命令行
/incision list
// 输出所有 Suture 及其状态
```

### show — 查看单个补丁详情

```kotlin
/incision show <id>
// 输出指定 Suture 的详细信息（状态、目标、织入点）
```

### heal — 关闭/恢复单个补丁

```kotlin
/incision heal <id>
// 关闭指定 Suture，恢复原始字节码
```

### dump — 导出字节码

```kotlin
/incision dump <class_name>
// 导出目标类的当前字节码
```

### plugins — 查看后端状态

```kotlin
/incision plugins
// 列出已加载的后端和状态
```

## 学习路径推荐

1. **先理解 Anchor 和 Site** — 这是定位的基础
2. **掌握方法描述符格式** — 否则找不到目标方法
3. **从 @Lead/@Trail 开始** — 最简单的观察型 Advice
4. **再学 @Splice** — 掌握 `proceed()`/`override()` 控制流
5. **进阶 @Graft/@Bypass** — 理解调用点拦截
6. **最后 @Excise** — 完全替换方法
7. **学习 Suture 管理** — 掌控补丁生命周期
8. **私有成员访问** — `field()`/`method()` 的使用

## Common Mistakes

### ❌ Mistake 1: @Splice 中忘记 proceed()
```kotlin
@Splice(scope = "method:com.example.TargetClass#targetMethod()V")
fun badSplice(splice: Theatre) {
    // 什么都没调用！原方法不会执行
    println("logging only")
}
```
**Why wrong**: `@Splice` 包围了方法体，必须显式调用 `proceed()` 或 `override()`，否则原方法永远不会执行。

**Fix**:
```kotlin
@Splice(scope = "method:com.example.TargetClass#targetMethod()V")
fun goodSplice(splice: Theatre) {
    println("before")
    splice.proceed()   // ← 必须显式调用
    println("after")
}
```

### ❌ Mistake 2: 方法描述符不匹配重载
```kotlin
// 目标类有多个重载，但描述符没写参数
@Lead(scope = "method:net.minecraft.world.entity.Entity#hurt()Z")
fun patch(lead: Theatre) { ... }
```
**Why wrong**: `Entity#hurt` 有多个重载，没有参数描述符 Incision 无法确定目标。

**Fix**:
```kotlin
@Lead(scope = "method:net.minecraft.world.entity.Entity#hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
fun patch(lead: Theatre) { ... }
```

### ❌ Mistake 3: 在 CONST 阶段访问 Suture
```kotlin
@Awake(LifeCycle.CONST)
fun earlyAccess() {
    val s = Scalpel.find("myPatch")  // ← Suture 还不存在!
    s.suspend()
}
```
**Why wrong**: Suture 在 ENABLE 阶段才创建。

**Fix**: 在 ENABLE 阶段操作 Suture：
```kotlin
@Awake(LifeCycle.ENABLE)
fun patchManagement() {
    val s = Scalpel.find("myPatch")
    s.suspend()
}
```

### ❌ Mistake 4: 混淆 proceedResult() 和 override()
```kotlin
// 想跳过原方法但还想触发 @Trail
splice.proceedResult("value")  // ✅ 会触发 @Trail

// 想完全跳过且不触发任何后续
splice.skip("value")          // ✅ 不触发 @Trail（等同于 override）

// 注意区别
splice.override("value")      // ⚠️ 等同于 skip，不触发后续
```

### ❌ Mistake 5: 未处理 NMS 版本差异
```kotlin
@Surgeon
object Patch {
    // 直接写了 NMS 混淆名，换版本就失效
    @Lead(scope = "method:net.minecraft.server.level.WorldServer#b()V")  // ← 混淆名!
    fun patch(lead: Theatre) { ... }
}
```
**Why wrong**: 不同 Minecraft 版本的混淆名不同。

**Fix**: 使用 Mapped 名 + `remap = true` + `@Version` 门控：
```kotlin
@Surgeon
object Patch {
    @Operation(priority = 0)
    @Version(start = "1.20", end = "1.20.4")
    @Lead(scope = "method:net.minecraft.server.level.WorldServer#tick()V", remap = true)
    fun patch(lead: Theatre) { ... }
}
```

### ❌ Mistake 6: 忘记 @KotlinTarget 参数导致部分调用未拦截
```kotlin
@Lead(scope = "method:com.example.Utils#doWork()V")
@KotlinTarget  // ← 裸注解无效！未设置参数
fun patch(lead: Theatre) { ... }
// 如果是 Kotlin companion + @JvmStatic，Java 端调用可能绕过补丁
```
**Fix**: 添加 `@KotlinTarget(companionInstance = true, jvmStaticBridge = true)` 覆盖双路径。

### ❌ Mistake 7: JVMTI 自动加载无需配置
```
[Incision] JVMTI native library loaded from jar
```
**说明**: JVMTI 后端自动从 jar 内加载 native 库，无需手动配置 `-agentpath`。

## Minimal Example

```kotlin
package com.example.plugin.incision

import taboolib.module.incision.*
import taboolib.common.platform.Awake
import taboolib.common.LifeCycle

@Surgeon
object PlayerJoinPatch {

    @Operation(priority = 0)
    @Lead(scope = "method:net.minecraft.server.players.PlayerList#placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V")
    fun onPlayerJoin(lead: Theatre) {
        val connection = lead.arg<Any>(0)
        val player = lead.arg<Any>(1)
        println("[Incision] Player joining: $player")
        lead.proceed()
    }

    @Operation(priority = 0)
    @Trail(scope = "method:net.minecraft.server.players.PlayerList#placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V")
    fun onPlayerJoinDone(trail: Theatre) {
        trail.proceed()
        println("[Incision] Player join complete")
    }
}

// 在 ENABLE 阶段管理 Suture
@Awake(LifeCycle.ENABLE)
fun managePatch() {
    // 检查特定补丁状态
    Scalpel.find("PlayerJoinPatch#onPlayerJoin")?.let { suture ->
        println("Patch status: ${suture.state}")
    }
}

@Awake(LifeCycle.DISABLE)
fun cleanup() {
    // 遍历所有 Suture 进行关闭
    for (suture in Scalpel.sutures) {
        suture.heal()
    }
}
```

## Checklist

Before creating Incision patches:

- [ ] 确认目标方法签名（使用 `/incision list` 查看已注册补丁）
- [ ] 选择合适的 Advice 类型（参考决策树）
- [ ] 确认 `@Splice` 中显式调用了 `proceed()` 或 `override()`
- [ ] 重载方法指定了完整参数类型描述符（含括号）
- [ ] NMS 目标使用 Mapped 名 + `remap = true`
- [ ] 跨版本补丁在方法上添加了 `@Version(start = ..., end = ...)` 门控
- [ ] Kotlin companion/@JvmStatic 目标添加了 `@KotlinTarget(companionInstance = true, jvmStaticBridge = true)`
- [ ] 不在 CONST 阶段访问 Suture
- [ ] 设置合理的 `priority`（Int 值）避免冲突

After implementing patches:

- [ ] 编译通过：`./gradlew build`
- [ ] 启动服务器，检查 `[Incision]` 日志确认织入成功
- [ ] 使用 `/incision list` 查看所有 Suture 状态
- [ ] 使用 `/incision show <id>` 查看单个补丁详情
- [ ] 测试核心路径：触发目标方法验证补丁行为
- [ ] 测试边界情况：异常路径、参数边界值
- [ ] 测试 Suture 生命周期：`suspend()` → 验证补丁暂停 → `resume()` → 验证恢复
- [ ] 测试回滚：`heal()` 后验证原始行为恢复
- [ ] 有 `@Splice` 的补丁验证 `proceed()` 正确执行

## Version-Specific Notes

**TabooLib 6.2.0+**:
- Incision 模块需单独 `install("module-incision")`
- 支持 NMS `remap` 自动映射
- `@KotlinTarget` 需指定 `companionInstance` / `jvmStaticBridge` 布尔参数
- JVMTI 后端自动加载 native 库，无需配置 `-agentpath`

**TabooLib 6.1.x**:
- Incision API 可能不同，核实项目版本

## Troubleshooting

**Error: "No such method: xxx#yyy"**
- Cause: 方法描述符错误或目标类未加载
- Fix: 使用 `/incision list` 确认已注册的 Suture；检查 `scope` 描述符格式

**Error: "Ambiguous method: multiple matches"**
- Cause: 重载方法未指定参数类型
- Fix: 添加完整的参数描述符（含括号）

**Error: "@Splice must call proceed() or override()"**
- Cause: `@Splice` 中未调用 `proceed()` 或 `override()`
- Fix: 在 handler 中调用其中之一

**Error: "JVMTI native library not found"**
- Cause: native 库未随 jar 打包
- Fix: 检查 `module-incision` 是否正确安装；自动降级到 InstrumentationBackend

**Error: "Class not found in NMS mappings"**
- Cause: NMS 版本不匹配或 `remap` 配置错误
- Fix: 确认 Minecraft 版本和 mappings 配置

**Error: "@KotlinTarget requires companion object target"**
- Cause: 对非 companion 目标使用了 `@KotlinTarget`
- Fix: 移除 `@KotlinTarget` 或确认目标是 Kotlin companion object

## References

- Source code: `taboolib/module/incision/`
- Related cards: `02_lifecycle.md` (CONST/ENABLE timing), `05_ioc.md` (service injection into patches)
- Related recipes: `create_incision_advice.md`
- Related examples: `examples/incision_basic.kt`
