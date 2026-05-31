# Card 14: 单元测试与集成测试

## When to Use

Load this card when:
- 用户提到 "测试", "单元测试", "集成测试", "test", "junit", "mock"
- 功能开发完成后需要编写测试
- 需要在测试中注入 TabooLib 启动环境或 IOC 容器
- 需要验证 Service/Repository 层逻辑的正确性
- 需要测试 @Component Bean 的依赖注入、生命周期回调
- 需要测试 AOP 切面、条件装配、作用域等高级 IOC 特性
- 需要 Mock TabooLib 平台服务（调度器 submit/submitAsync、日志 info/warning）
- 需要 Mock Bukkit API（Player、World、Event）
- 需要测试 @Config 配置类、命令解析、@SubscribeEvent 监听器

## 测试规范（Test Conventions）

仅作用于测试源集（src/test/、src/*Test/）。基于 JUnit 5。

### 1. 命名规范（强制）

#### 1.1 方法名必须使用英文

测试方法名使用 **英文 lowerCamelCase**，语义清晰、可 grep。

**禁止**：
- 中文方法名
- 拼音方法名
- 数字编号（`test1` / `test_场景_1`）
- Kotlin 反引号包裹的中文方法名（`fun `中文方法名`()`）
- Kotlin 反引号包裹的英文自然语言方法名（`fun `some description`()`）

#### 1.2 必须使用 @DisplayName 描述中文语义

每个 `@Test` / `@ParameterizedTest` 方法**必须**配一个 `@DisplayName`，内容为**简体中文**，描述"被测行为 + 期望结果"。

**禁止**：
- 用英文 @DisplayName
- 省略 @DisplayName 只留方法名

#### 正确示例

```kotlin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlayerServiceTest {

    @Test
    @DisplayName("getOrCreate 在玩家不存在时返回新实例")
    fun getOrCreateReturnsNewWhenNotFound() {
        // ...
    }

    @Test
    @DisplayName("@PreDestroy 在 shutdown 时被调用")
    fun preDestroyCalledOnShutdown() {
        // ...
    }
}
```

#### ❌ 错误示例

```kotlin
// ❌ 反引号中文方法名
@Test
fun `创建订单成功`() { }

// ❌ 反引号英文自然语言
@Test
fun `calculate tax correctly`() { }

// ❌ 数字编号
@Test
fun test1() { }

// ❌ 缺少 @DisplayName
@Test
fun getOrCreateReturnsNew() { }
```

### 2. 测试类命名

| 被测类 | 测试类 |
|--------|--------|
| `PlayerService` | `PlayerServiceTest` |
| `PlayerRepository` | `PlayerRepositoryTest` |
| `CoinCalculator` | `CoinCalculatorTest` |

### 3. 断言风格

- 使用 JUnit 5 Assertions（`assertEquals`、`assertNotNull`、`assertTrue`）
- 断言消息使用中文
- 每个测试方法只验证一个行为

## Core Idea

TabooLib 不是普通依赖，它有完整的启动流程（IsolatedClassLoader → PrimitiveLoader → 生命周期推进）。测试需要根据被测代码的依赖深度选择合适的测试层级：

| 层级 | 依赖深度 | 适用场景 | 是否需要服务器 |
|------|---------|---------|--------------|
| Tier 0: 纯单元测试 | 无 TabooLib 依赖 | 工具类、纯逻辑 | ❌ |
| Tier 1: IOC 容器测试 | 仅 IOC 容器 | @Component Bean 的注入、生命周期 | ❌ |
| Tier 2: TabooLib 引导测试 | 完整生命周期 | 需要平台服务的集成测试 | ❌ |
| Tier 3: MockBukkit 集成测试 | 模拟 Bukkit | 需要 Bukkit API 的端到端测试 | ❌（Mock） |

**关键原则：能用低层级测试就不用高层级。** 只有被测代码真正需要 TabooLib 运行时才引入引导测试。

## 测试层级决策流程

```
被测代码是否依赖 TabooLib API？
├── 否 → Tier 0: 纯单元测试（JUnit 5 + 断言）
└── 是
    ├── 仅依赖 IOC 注入（@Component/@Resource）？
    │   └── Tier 1: IocTestContext 容器测试
    ├── 依赖 TabooLib 生命周期/平台函数？
    │   └── Tier 2: @TabooLibIocTest 引导测试
    └── 依赖 Bukkit API（Player/World/Event）？
        └── Tier 3: MockBukkit 集成测试
```

## Tier 0: 纯单元测试

**适用场景**: 工具方法、纯业务逻辑、数据转换、算法

不引入任何 TabooLib 依赖，直接用 JUnit 5 测试：

```kotlin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoinCalculatorTest {

    @Test
    @DisplayName("applyTax 正确扣除税率")
    fun applyTaxDeductsCorrectly() {
        val result = CoinCalculator.applyTax(100, 0.1)
        assertEquals(90, result)
    }
}
```

**build.gradle.kts**:
```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.withType<Test> { useJUnitPlatform() }
```

## Tier 1: IOC 容器测试（IocTestContext）

**适用场景**: 测试 @Component Bean 的依赖注入、@PostConstruct/@PreDestroy 生命周期、AOP 切面、条件装配、作用域

`IocTestContext` 是一个**不依赖 TabooLib 运行时和 BeanContainer 单例**的轻量级 IOC 容器，可在纯 JUnit 环境中独立运行。

**依赖**: `top.wcpe.taboolib.ioc:taboolib-ioc-test`

### Gradle 配置

```kotlin
dependencies {
    // IOC 测试依赖
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-test:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-core:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-api:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-annotation:{version}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}
```

### 基本用法

```kotlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import top.wcpe.taboolib.ioc.IocTestContext
import top.wcpe.taboolib.ioc.annotation.Component
import top.wcpe.taboolib.ioc.annotation.Inject
import top.wcpe.taboolib.ioc.annotation.PostConstruct
import top.wcpe.taboolib.ioc.annotation.PreDestroy

class PlayerServiceTest {

    private lateinit var ctx: IocTestContext

    @BeforeEach
    fun setUp() {
        ctx = IocTestContext()
    }

    @AfterEach
    fun tearDown() {
        ctx.lifecycleManager.shutdown()
    }

    @Test
    @DisplayName("Service 依赖注入后 status 为 initialized")
    fun serviceGetsDependencyInjected() {
        ctx.register(PlayerRepository::class.java)
        ctx.register(PlayerService::class.java)
        ctx.initialize()

        val service = ctx.getBean(PlayerService::class.java)
        assertNotNull(service)
        assertEquals("initialized", service!!.status)
    }

    @Test
    @DisplayName("@PreDestroy 在 shutdown 时被调用")
    fun preDestroyCalledOnShutdown() {
        ctx.register(ResourceHolder::class.java)
        ctx.initialize()

        assertFalse(ResourceHolder.destroyed)
        ctx.lifecycleManager.shutdown()
        assertTrue(ResourceHolder.destroyed)
    }
}

@Component
class PlayerRepository {
    fun findByName(name: String): PlayerData? = null
}

@Component
class PlayerService {
    @Inject
    lateinit var repository: PlayerRepository

    var status: String = "created"

    @PostConstruct
    fun init() {
        status = "initialized"
    }
}

@Component
class ResourceHolder {
    companion object { var destroyed = false }

    @PreDestroy
    fun cleanup() { destroyed = true }
}
```

### IocTestContext API 参考

| 方法 | 说明 |
|------|------|
| `register(Class<*>)` | 扫描并注册一个 @Component 类 |
| `registerWithCondition(Class<*>)` | 带条件评估的注册（返回 true/false） |
| `registerBean(name, instance)` | 手动注册一个 Bean 实例（Mock 对象） |
| `registerScope(name, BeanScope)` | 注册自定义作用域 |
| `addBeanPostProcessor(processor)` | 手动注册 BeanPostProcessor |
| `initialize()` | 初始化容器（预创建 eager singleton） |
| `invokePostEnable()` | 执行所有 @PostEnable 方法 |
| `getBean(Class<T>)` | 按类型获取 Bean |
| `getBeansOfType(Class<T>)` | 获取某类型的所有 Bean |
| `containsBean(name)` | 检查 Bean 是否存在 |
| `getSingleton(name)` | 获取已创建的 singleton 实例 |
| `lifecycleManager.shutdown()` | 触发 @PreDestroy 并关闭容器 |
| `advisorRegistry` | 访问 AOP Advisor 注册表 |
| `registry` | 访问底层 BeanRegistry |

### 用 IocTestContext 注入 Mock 对象

```kotlin
@Test
@DisplayName("通过 registerBean 注入 Mock 替代真实实现")
fun mockExternalDependency() {
    val ctx = IocTestContext()
    ctx.registerBean("emailService", mockk<EmailService>(relaxed = true))
    ctx.register(NotificationService::class.java)
    ctx.initialize()

    val service = ctx.getBean(NotificationService::class.java)!!
    service.notify("test")

    verify { (ctx.getSingleton("emailService") as EmailService).send(any()) }
}
```

### 测试 AOP 切面

```kotlin
@Test
@DisplayName("@Aspect 切面拦截目标方法调用")
fun aspectInterceptsMethodCall() {
    val ctx = IocTestContext()
    ctx.register(LogAspect::class.java)    // @Aspect 类要先注册
    ctx.register(TargetService::class.java)
    ctx.initialize()

    val service = ctx.getBean(TargetService::class.java)!!
    service.doWork()

    assertEquals(1, LogAspect.interceptCount)
}
```

### 测试作用域

```kotlin
import top.wcpe.taboolib.ioc.scope.ThreadBeanScope
import top.wcpe.taboolib.ioc.bean.BeanScopes

@Test
@DisplayName("ThreadScope 在不同线程返回不同实例")
fun threadScopeCreatesPerThreadInstance() {
    val ctx = IocTestContext()
    ctx.registerScope(BeanScopes.THREAD, ThreadBeanScope())
    ctx.register(ThreadScopedService::class.java)
    ctx.initialize()

    val main = ctx.getBean(ThreadScopedService::class.java)
    var other: ThreadScopedService? = null
    Thread { other = ctx.getBean(ThreadScopedService::class.java) }.apply {
        start(); join()
    }
    assertNotSame(main, other)
}
```

## Tier 2: TabooLib 引导测试（@TabooLibIocTest）

**适用场景**: 需要完整 TabooLib 生命周期（CONST → ACTIVE），测试 @Awake 方法、平台函数、与 TabooLib 运行时的集成

`@TabooLibIocTest` 是类似 SpringBootTest 的注解，自动引导 TabooLib 生命周期和 IOC 容器。

### @TabooLibIocTest 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `components` | `KClass<*>[]` | `[]` | 要注册的 @Component 类 |
| `targetLifeCycle` | `LifeCycle` | `ACTIVE` | 生命周期推进到哪个阶段 |
| `invokePostEnable` | `Boolean` | `true` | 是否调用 @PostEnable |
| `observable` | `Boolean` | `false` | 启用调试日志 |
| `enablePrimitiveBootstrap` | `Boolean` | `false` | 是否启用 IsolatedClassLoader 引导 |

### 用法

```kotlin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import top.wcpe.taboolib.ioc.test.TabooLibIocTest
import top.wcpe.taboolib.ioc.test.IocAutowired
import top.wcpe.taboolib.ioc.test.TabooLibIocTestContext
import taboolib.common.LifeCycle

@TabooLibIocTest(
    PlayerService::class,
    PlayerRepository::class,
    targetLifeCycle = LifeCycle.ACTIVE,
    observable = true
)
class PlayerServiceIntegrationTest {

    @IocAutowired
    lateinit var service: PlayerService

    @IocAutowired
    lateinit var testContext: TabooLibIocTestContext

    @Test
    @DisplayName("从 IOC 容器注入的 Service 不为 null")
    fun serviceInjectedFromContainer() {
        assertNotNull(service)
    }

    @Test
    @DisplayName("通过 testContext 可访问其他 Bean")
    fun testContextProvidesBeanAccess() {
        val repo = testContext.getBean(PlayerRepository::class.java)
        assertNotNull(repo)
    }
}
```

### 引导过程详解

`@TabooLibIocTest` 内部执行以下步骤：

1. **beforeAll**: 创建 `TabooLibIocTestContext`，注册 components，推进生命周期
2. **postProcessTestInstance**: 扫描测试类 `@IocAutowired` 字段，从容器注入
3. **afterAll**: 调用 `shutdown()`（触发 @PreDestroy，推进 DISABLE）

生命周期推进顺序：
```
register components → CONST → INIT → LOAD → ENABLE (initialize IOC) → ACTIVE
                                                    ↑
                                          IOC 在 ENABLE 前完成初始化
```

### @IocAutowired 注入规则

| 字段类型 | 注入内容 |
|---------|---------|
| `TabooLibIocTestContext` | 注入测试上下文本身 |
| 其他任意类型 | 按 IOC 容器中的 Bean 类型注入 |

### 注意事项

- `@TabooLibIocTest` 使用 JUnit 5 Extension，每个测试类引导一次（`beforeAll`/`afterAll`）
- `enablePrimitiveBootstrap = true` 会初始化 IsolatedClassLoader，首次较慢但全局只执行一次
- 如果测试不需要完整 TabooLib 生命周期，优先使用 Tier 1 的 `IocTestContext`

## Tier 3: MockBukkit 集成测试

**适用场景**: 需要调用 Bukkit API（Player、World、Server），验证插件与 Bukkit 的交互

### Gradle 配置

```kotlin
dependencies {
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.93.2")
    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // MockBukkit-v1.20 需要 Java 21
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}
```

### 用法

```kotlin
import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MyPluginTest {

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    @DisplayName("MockBukkit 创建的 Player 名称正确")
    fun mockPlayerHasCorrectName() {
        val server = MockBukkit.getMock()
        val player = server.addPlayer("testPlayer")

        assertEquals("testPlayer", player.name)
    }

    @Test
    @DisplayName("IOC 容器与 MockBukkit 组合测试")
    fun iocCombinedWithMockBukkit() {
        val ctx = IocTestContext()
        ctx.register(MyService::class.java)
        ctx.initialize()
        val service = ctx.getBean(MyService::class.java)!!
        // MockBukkit 提供 Player 对象，IOC 提供 Service
        ctx.lifecycleManager.shutdown()
    }
}
```

## 附加章节: TabooLib IOC 注入到单元测试

### 方案一: IocTestContext（推荐，适用于大多数场景）

```kotlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OrderServiceTest {

    private lateinit var ctx: IocTestContext

    @BeforeEach
    fun setUp() {
        ctx = IocTestContext()
        ctx.register(OrderRepository::class.java)
        ctx.register(OrderService::class.java)
        ctx.initialize()
    }

    @AfterEach
    fun tearDown() {
        ctx.lifecycleManager.shutdown()
    }

    @Test
    @DisplayName("createOrder 正常创建订单")
    fun createOrderSucceeds() {
        val service = ctx.getBean(OrderService::class.java)!!
        val order = service.create("player1", "item1", 10)
        assertNotNull(order)
    }
}
```

### 方案二: 手动注入（无 IOC 框架依赖时）

适用于使用 TabooLib 核心 `database-ioc`（`taboolib.expansion.ioc`）的场景，该模块无独立测试上下文：

```kotlin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlayerServiceTest {

    @Test
    @DisplayName("手动注入依赖测试 getOrCreate 逻辑")
    fun pureLogicTestWithoutContainer() {
        val repository = PlayerRepository()
        val service = PlayerService()
        service.repository = repository

        val result = service.getOrCreate(UUID.randomUUID())
        assertNotNull(result)
    }
}
```

### 方案三: @TabooLibIocTest（需要完整 TabooLib 环境）

```kotlin
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@TabooLibIocTest(
    OrderService::class,
    OrderRepository::class,
    targetLifeCycle = LifeCycle.ENABLE,
    invokePostEnable = false
)
class OrderServiceFullTest {

    @IocAutowired
    lateinit var orderService: OrderService

    @Test
    @DisplayName("Bean 注入成功且可正常调用")
    fun beanInjectedAndFunctional() {
        assertNotNull(orderService)
    }
}
```

## 数据库测试

### 内存 SQLite 测试 Repository

```kotlin
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlayerRepositoryTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var repository: PlayerRepository

    @BeforeEach
    fun setUp() {
        dataSource = HikariDataSource().apply {
            jdbcUrl = "jdbc:sqlite::memory:"
            driverClassName = "org.sqlite.JDBC"
        }
        repository = PlayerRepository(dataSource)
        repository.createTableIfNotExists()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    @DisplayName("save 后可通过名称查询到玩家数据")
    fun saveAndRetrieveByName() {
        repository.save(PlayerData(UUID.randomUUID(), "test", 100))
        val loaded = repository.findByName("test")
        assertNotNull(loaded)
        assertEquals(100, loaded!!.coins)
    }
}
```

### Gradle 数据库测试依赖

```kotlin
dependencies {
    testImplementation("com.zaxxer:HikariCP:4.0.3")
    testImplementation("org.xerial:sqlite-jdbc:3.42.0.0")
}
```

## 平台服务 Mock（调度器与日志）

当被测代码调用 TabooLib 平台函数（`submit()`、`info()`、`warning()` 等）时，**无需修改 TabooLib 源码**，直接在测试中通过 `PlatformFactory.registerService<T>()` 注入 Mock 实现即可。

### 注入机制

`PlatformFactory.serviceMap` 是 `public ConcurrentHashMap`，`registerService<T>()` 是 `public inline fun`，测试中直接调用即生效：

```kotlin
// 一行注入
PlatformFactory.registerService<PlatformExecutor>(myMockExecutor)
PlatformFactory.registerService<PlatformIO>(myMockIO)
```

**不需要**修改 `build.gradle.kts`、不需要改 TabooLib 源码、不需要额外注解。

### 调度器 Mock

接口定义（`taboolib.common.platform.service.PlatformExecutor`）：

```kotlin
interface PlatformExecutor {
    fun submit(runnable: PlatformRunnable): PlatformTask  // 调度入口
    fun start()
}
```

**约束**：`executorService` 是 `unsafeLazy`（一次求值永久缓存），Mock 注册**必须在首次 `submit()` 调用前**。在 `@BeforeEach` 中注册即可。

#### 同步执行器（推荐，覆盖大多数场景）

```kotlin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformExecutor

class SchedulerAwareServiceTest {

    @BeforeEach
    fun setUp() {
        PlatformFactory.registerService<PlatformExecutor>(SyncTestExecutor())
    }

    @AfterEach
    fun tearDown() {
        PlatformFactory.serviceMap.remove(PlatformExecutor::class.java.name)
    }
}

/** 同步执行器：submit() 在当前线程立即同步执行，忽略 async/delay/period */
class SyncTestExecutor : PlatformExecutor {

    override fun submit(runnable: PlatformExecutor.PlatformRunnable): PlatformExecutor.PlatformTask {
        val task = TestPlatformTask()
        runnable.executor.invoke(task)
        return task
    }

    override fun start() {}

    class TestPlatformTask : PlatformExecutor.PlatformTask {
        override fun cancel() {}
    }
}
```

#### 任务追踪执行器（验证调度行为）

```kotlin
import java.util.concurrent.CopyOnWriteArrayList

class TrackingTestExecutor : PlatformExecutor {

    val submittedTasks = CopyOnWriteArrayList<PlatformExecutor.PlatformRunnable>()

    override fun submit(runnable: PlatformExecutor.PlatformRunnable): PlatformExecutor.PlatformTask {
        submittedTasks.add(runnable)
        if (runnable.now) {
            runnable.executor.invoke(TestPlatformTask())
        }
        return TestPlatformTask()
    }

    override fun start() {}
}
```

测试调度行为：

```kotlin
@Test
@DisplayName("发放奖励时调用 submitAsync 提交异步任务")
fun grantRewardSubmitsAsyncTask() {
    val executor = TrackingTestExecutor()
    PlatformFactory.registerService<PlatformExecutor>(executor)

    val service = RewardService()
    service.grantReward(UUID.randomUUID(), "gold", 100)

    assertTrue(executor.submittedTasks.any { it.async })
}
```

### 日志 Mock

接口定义（`taboolib.common.platform.service.PlatformIO`）：提供 `info()`、`warning()`、`severe()` 等方法。

**日志比调度器更宽松**：未注入 Mock 时自动回退到 `PrimitiveIO`（`java.util.logging`），不会崩溃。注入 Mock 后可捕获日志输出验证。

```kotlin
import java.util.concurrent.CopyOnWriteArrayList

class CapturingTestIO : PlatformIO {

    val infoLogs = CopyOnWriteArrayList<String>()
    val warningLogs = CopyOnWriteArrayList<String>()

    override val pluginId: String get() = "test"
    override val pluginVersion: String get() = "1.0.0"
    override val isPrimaryThread: Boolean get() = true
    override fun <T> server(): T = throw UnsupportedOperationException()
    override fun getJarFile(): File = File(".")
    override fun getDataFolder(): File = File(".")
    override fun getPlatformData(): Map<String, Any> = emptyMap()

    override fun info(vararg message: Any?) {
        infoLogs.add(message.joinToString(" ") { it.toString() })
    }

    override fun severe(vararg message: Any?) {
        infoLogs.add(message.joinToString(" ") { it.toString() })
    }

    override fun warning(vararg message: Any?) {
        warningLogs.add(message.joinToString(" ") { it.toString() })
    }

    override fun releaseResourceFile(path: String, replace: Boolean, vararg param: Any?): File = File(".")
}
```

测试日志输出：

```kotlin
@BeforeEach
fun setUp() {
    testIO = CapturingTestIO()
    PlatformFactory.registerService<PlatformIO>(testIO)
}

@AfterEach
fun tearDown() {
    PlatformFactory.serviceMap.remove(PlatformIO::class.java.name)
}

@Test
@DisplayName("发送失败时记录 warning 日志")
fun logWarningOnSendFailure() {
    val service = NotificationService()
    service.sendNotification("player1", "test")

    assertTrue(testIO.warningLogs.any { it.contains("player1") })
}
```

### @AfterEach 清理

跨测试不会互相污染的前提是清理 `serviceMap`：

```kotlin
@AfterEach
fun tearDown() {
    PlatformFactory.serviceMap.remove(PlatformExecutor::class.java.name)
    PlatformFactory.serviceMap.remove(PlatformIO::class.java.name)
    // 按需清理其他替换的服务
}
```

### 可替换性总结

| 平台服务 | 替换方式 | 未注入时行为 |
|---------|---------|------------|
| `PlatformExecutor` | `registerService<PlatformExecutor>(mock)` | 崩溃（`submit()` 内部调用 `getService()`，找不到抛异常） |
| `PlatformIO` | `registerService<PlatformIO>(mock)` | 自动回退 JUL，不会崩溃 |
| `PlatformCommand` | `registerService<PlatformCommand>(mock)` | 崩溃 |
| `PlatformListener` | `registerService<PlatformListener>(mock)` | 崩溃 |
| `PlatformAdapter` | `registerService<PlatformAdapter>(mock)` | 崩溃 |

**不需要修改 TabooLib 源码**即可完成全部替换，所有 `PlatformFactory` 的 `registerService<T>()` 都是 `public` API。

## Common Mistakes

### ❌ Mistake 1: 忘记调用 initialize()

```kotlin
val ctx = IocTestContext()
ctx.register(MyService::class.java)
// 缺少 ctx.initialize()！
val service = ctx.getBean(MyService::class.java)  // ← 返回 null！
```

**Fix**: 注册完所有 Bean 后调用 `initialize()`
```kotlin
val ctx = IocTestContext()
ctx.register(MyService::class.java)
ctx.initialize()  // ← 必须
val service = ctx.getBean(MyService::class.java)
```

### ❌ Mistake 2: 测试间共享状态

```kotlin
companion object {
    val ctx = IocTestContext()  // ← 所有测试共享！
}
```

**Fix**: 每个测试方法创建独立上下文
```kotlin
@BeforeEach
fun setUp() {
    ctx = IocTestContext()  // ← 每个测试独立
    ctx.register(MyService::class.java)
    ctx.initialize()
}

@AfterEach
fun tearDown() {
    ctx.lifecycleManager.shutdown()
}
```

### ❌ Mistake 3: 用 @TabooLibIocTest 测试纯逻辑

```kotlin
@TabooLibIocTest(MyCalculator::class)  // ← 过重！
class CalculatorTest {
    @IocAutowired lateinit var calc: MyCalculator

    @Test
    @DisplayName("加法计算正确")
    fun add() { assertEquals(3, calc.add(1, 2)) }
}
```

**Fix**: 纯逻辑用 Tier 0 测试
```kotlin
class CalculatorTest {
    @Test
    @DisplayName("加法计算正确")
    fun addReturnsCorrectSum() {
        assertEquals(3, MyCalculator().add(1, 2))
    }
}
```

### ❌ Mistake 4: @Aspect 注册顺序错误

```kotlin
val ctx = IocTestContext()
ctx.register(TargetService::class.java)
ctx.register(LogAspect::class.java)  // ← Aspect 在 Target 之后注册
ctx.initialize()
```

**Fix**: Aspect 要先于被切面 Bean 注册
```kotlin
val ctx = IocTestContext()
ctx.register(LogAspect::class.java)      // ← 先注册
ctx.register(TargetService::class.java)  // ← 再注册
ctx.initialize()
```

### ❌ Mistake 5: shutdown 后继续使用容器

```kotlin
ctx.lifecycleManager.shutdown()
val bean = ctx.getBean(MyService::class.java)  // ← 行为未定义！
```

**Fix**: shutdown 后不再使用，或在 @AfterEach 中 shutdown

## 专项测试指南

### 配置系统测试（@Config）

TabooLib 的 `@Config` 注解将 YAML 文件映射为 Kotlin 对象。测试时直接构造或使用临时文件。

#### 方案一：直接构造 Config 对象（Tier 0，推荐）

假设 Config 类为普通 data class，直接 `new` 并传入测试值。

#### 方案二：临时 YAML 文件（需要 TabooLib 运行时）

```kotlin
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ConfigTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("配置加载后值正确")
    fun configLoadsCorrectly() {
        val yamlFile = tempDir.resolve("config.yml").toFile()
        yamlFile.writeText("""
            database:
              host: localhost
              port: 3306
            limits:
              maxPlayers: 100
        """.trimIndent())

        val config = YamlConfiguration.loadConfiguration(yamlFile)
        assertEquals("localhost", config.getString("database.host"))
        assertEquals(100, config.getInt("limits.maxPlayers"))
    }
}
```

#### 方案三：通过 BeanContainer/IocTestContext + @Value 注入配置

如果使用 `taboolib-ioc` 的 `@Value` / `@PropertySource`：

```kotlin
@Test
@DisplayName("@Value 注入系统属性到 Bean")
fun valueInjectsSystemProperty() {
    System.setProperty("app.timeout", "30")
    val ctx = IocTestContext()
    ctx.register(TimeoutConfig::class.java)
    ctx.initialize()

    val config = ctx.getBean(TimeoutConfig::class.java)!!
    assertEquals(30, config.timeout)

    System.clearProperty("app.timeout")
}
```

### 命令层测试

架构规范要求命令不做业务逻辑，只做参数解析和校验后 `submitAsync` 委托 Service。因此命令测试分为两层：

#### 参数解析测试（Tier 0）

直接测试 Command DSL 生成的解析器，无需 TabooLib 运行时：

```kotlin
@Test
@DisplayName("parseArguments 正确解析玩家名称参数")
fun parseArgumentsExtractsPlayerName() {
    val args = arrayOf("transfer", "player1", "player2", "100")
    val result = CommandParser.parse(args)

    assertEquals("player1", result.get("from"))
    assertEquals("player2", result.get("to"))
    assertEquals(100, result.getInt("amount"))
}
```

#### 权限校验测试（Tier 0）

```kotlin
@Test
@DisplayName("adminOnly 检查无权限时返回 false")
fun adminOnlyReturnsFalseWithoutPermission() {
    val sender = mock<CommandSender> {
        on { hasPermission("admin.transfer") } doReturn false
    }
    assertFalse(CommandValidator.checkPermission(sender, "admin.transfer"))
}
```

#### 命令 → Service 委托测试（Tier 1）

只测 Service 层，命令层已由架构规范保证正确性：

```kotlin
@Test
@DisplayName("转账逻辑正确扣减和增加余额")
fun transferDeductsSourceAndCreditsTarget() {
    val ctx = IocTestContext()
    ctx.register(AccountRepository::class.java)
    ctx.register(TransferService::class.java)
    ctx.initialize()

    val service = ctx.getBean(TransferService::class.java)!!
    val result = service.transfer("player1", "player2", 50)

    assertTrue(result.isSuccess)
    assertEquals(50, result.sourceBalance)
    assertEquals(150, result.targetBalance)
}
```

### 监听器测试（@SubscribeEvent）

需要 MockBukkit 提供事件系统。涉及 Bukkit Event 的测试属于 Tier 3。

```kotlin
import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlayerJoinListenerTest {

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    @DisplayName("玩家加入时触发 PlayerJoinEvent 监听")
    fun onPlayerJoinTriggersListener() {
        val server = MockBukkit.getMock()
        val ctx = IocTestContext()
        ctx.register(PlayerJoinHandler::class.java)
        ctx.initialize()

        val handler = ctx.getBean(PlayerJoinHandler::class.java)!!
        val player = server.addPlayer("newPlayer")

        // 触发加入事件
        server.pluginManager.callEvent(PlayerJoinEvent(player, "欢迎"))

        assertTrue(handler.wasTriggered)
        assertEquals("newPlayer", handler.lastJoinedPlayer)
    }

    @Test
    @DisplayName("监听器中调用 submitAsync 正确委托 Service")
    fun listenerSubmitsAsyncToService() {
        val executor = TrackingTestExecutor()
        PlatformFactory.registerService<PlatformExecutor>(executor)

        val server = MockBukkit.getMock()
        val ctx = IocTestContext()
        ctx.register(WelcomeService::class.java)
        ctx.register(PlayerJoinListener::class.java)
        ctx.initialize()

        val player = server.addPlayer("testPlayer")
        server.pluginManager.callEvent(PlayerJoinEvent(player, "welcome"))

        assertTrue(executor.submittedTasks.any { it.async })
    }
}
```

### 测试数据工厂

避免在每个测试中重复构造数据：

```kotlin
object TestDataFactory {

    fun playerData(
        uuid: UUID = UUID.randomUUID(),
        name: String = "testPlayer",
        coins: Int = 100,
        level: Int = 1
    ) = PlayerData(uuid, name, coins, level)

    fun order(
        id: String = "order-001",
        playerName: String = "testPlayer",
        itemId: String = "item_sword",
        amount: Int = 1
    ) = Order(id, playerName, itemId, amount, System.currentTimeMillis())

    fun mockPlayer(name: String = "testPlayer"): org.bukkit.entity.Player {
        MockBukkit.getMock()?.addPlayer(name)
            ?: throw IllegalStateException("MockBukkit 未初始化")
        return Bukkit.getPlayer(name)!!
    }
}

// 使用
@Test
@DisplayName("calculateTotal 正确计算总价")
fun calculateTotalReturnsCorrectSum() {
    val data = TestDataFactory.playerData(coins = 200)
    val result = PriceCalculator.calculateTotal(data, 3)
    assertEquals(600, result)
}
```

### 完整端到端测试示例

以下是将调度器 Mock、日志捕获、IOC 容器、测试数据工厂组合在一起的完整示例：

```kotlin
import org.junit.jupiter.api.*
import top.wcpe.taboolib.ioc.IocTestContext

class OrderServiceIntegrationTest {

    private lateinit var ctx: IocTestContext
    private lateinit var executor: TrackingTestExecutor
    private lateinit var testIO: CapturingTestIO

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
        executor = TrackingTestExecutor()
        testIO = CapturingTestIO()
        PlatformFactory.registerService<PlatformExecutor>(executor)
        PlatformFactory.registerService<PlatformIO>(testIO)
        ctx = IocTestContext()
    }

    @AfterEach
    fun tearDown() {
        ctx.lifecycleManager.shutdown()
        MockBukkit.unmock()
        PlatformFactory.serviceMap.remove(PlatformExecutor::class.java.name)
        PlatformFactory.serviceMap.remove(PlatformIO::class.java.name)
    }

    @Test
    @DisplayName("创建订单后提交异步任务并写入日志")
    fun createOrderSubmitsAsyncAndLogs() {
        // Given: 注册 Bean
        ctx.register(OrderRepository::class.java)
        ctx.register(OrderService::class.java)
        ctx.register(InventoryService::class.java)
        ctx.initialize()

        val service = ctx.getBean(OrderService::class.java)!!
        val player = MockBukkit.getMock().addPlayer("buyer")

        // When: 执行业务
        val order = service.createOrder(player.uniqueId, "item_sword", 1)

        // Then: 验证结果
        assertNotNull(order)
        assertTrue(executor.submittedTasks.any { it.async },
            "应提交至少一个异步任务")
        assertTrue(testIO.infoLogs.any { it.contains("创建") },
            "应记录订单创建日志")
    }

    @Test
    @DisplayName("库存不足时创建订单失败且不提交任务")
    fun createOrderFailsWhenOutOfStock() {
        // Given: 库存为 0
        ctx.registerBean("inventoryService", mock<InventoryService> {
            on { checkStock(any(), any()) } doReturn false
        })
        ctx.register(OrderService::class.java)
        ctx.initialize()

        val service = ctx.getBean(OrderService::class.java)!!
        val player = TestDataFactory.mockPlayer("buyer")

        // When
        val order = service.createOrder(player.uniqueId, "rare_item", 10)

        // Then
        assertNull(order)
        assertTrue(testIO.warningLogs.any { it.contains("库存不足") },
            "应记录库存不足警告")
    }
}
```

### CI 环境配置

```kotlin
// build.gradle.kts
tasks.withType<Test> {
    useJUnitPlatform()
    // CI 友好输出
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // 避免 CI 内存溢出
    maxHeapSize = "512m"
    // 不依赖 workingDir
    workingDir = layout.buildDirectory.dir("test-run").get().asFile.also { it.mkdirs() }
}
```

### database-ioc 核心模块测试方案

TabooLib 核心的 `database-ioc`（`taboolib.expansion.ioc`）没有独立测试上下文。**推荐测试方案**：

1. **首选**：切换为 `taboolib-ioc`（`top.wcpe.taboolib.ioc`），获得完整测试基础设施
2. **次选**：手动实例化被测类，手动 set `@Resource` 字段（方案二）
3. **含 BeanContainer 的集成测试**：使用 `App.init()` 引导 TabooLib 应用模式，再通过 `BeanContainer.getBean<T>()` 获取 Bean

```kotlin
// 方案 3（仅适用于 database-ioc 核心模块）
@BeforeEach
fun setUp() {
    App.env()
        .skipKotlinRelocate(true)
        .skipSelfRelocate(true)
        .group("com.example")
        .scan("com.example.plugin")
    App.init()
}

@AfterEach
fun tearDown() {
    App.shutdown()
    BeanContainer.shutdown()
}

@Test
@DisplayName("从 BeanContainer 获取 Service 并验证注入")
fun serviceInjectedFromCoreIoc() {
    val service = BeanContainer.getBean(PlayerService::class.java)
    assertNotNull(service)
}
```

## Checklist

### 命名规范（所有层级通用）

- [ ] 测试方法名使用英文 lowerCamelCase，可 grep
- [ ] 禁止反引号包裹的方法名（无论中英文）
- [ ] 每个 @Test / @ParameterizedTest 配备 @DisplayName（简体中文）
- [ ] @DisplayName 描述"被测行为 + 期望结果"
- [ ] 测试类名以 `Test` 结尾（如 `PlayerServiceTest`）

### 编写测试前

- [ ] 确定被测代码的依赖深度，选择合适的测试层级
- [ ] Tier 0-1 测试优先，仅在必要时使用 Tier 2-3
- [ ] 确认 build.gradle.kts 中已配置 JUnit 5 和测试依赖
- [ ] 如使用 IOC 测试，确认已添加 `taboolib-ioc-test` 依赖

### IOC 容器测试（Tier 1）

- [ ] 注册所有被测 Bean 及其依赖
- [ ] @Aspect 类先于被切面 Bean 注册
- [ ] 调用 `initialize()` 后再 `getBean()`
- [ ] 测试结束后调用 `lifecycleManager.shutdown()`
- [ ] 每个测试方法创建独立的 `IocTestContext`

### 引导测试（Tier 2）

- [ ] `@TabooLibIocTest` 的 `components` 列出所有需要的类
- [ ] `targetLifeCycle` 设为所需最低阶段
- [ ] 使用 `@IocAutowired` 注入被测 Bean
- [ ] 注入 `TabooLibIocTestContext` 可获取底层容器访问权

### MockBukkit 集成测试（Tier 3）

- [ ] `@BeforeEach` 调用 `MockBukkit.mock()`
- [ ] `@AfterEach` 调用 `MockBukkit.unmock()`
- [ ] MockBukkit-v1.20 需要 Java 21（源码和测试编译目标需设为 21）
- [ ] 不依赖 plugin.yml（测试 classpath 下没有）

### 平台服务 Mock

- [ ] 在 `@BeforeEach` 中注册 Mock，`@AfterEach` 中 remove
- [ ] 调度器 Mock 必须在首次 `submit()` 前注册
- [ ] 日志 Mock 可选（未注册时自动回退 JUL）

### 配置 / 命令 / 监听器测试

- [ ] 配置测试优先使用直接构造（Tier 0），避免 TabooLib 运行时
- [ ] 命令测试优先测试 Service 层，参数解析可独立单元测试
- [ ] 监听器测试需要 MockBukkit（Tier 3），测试异步委托模式

### CI 环境

- [ ] `testLogging` 输出 events，`exceptionFormat = FULL`
- [ ] `maxHeapSize` 限制在 CI 节点可用范围内
- [ ] `workingDir` 不依赖项目根目录下的运行时文件

## Gap Analysis: 当前限制与改进建议

### TabooLib 核心 `database-ioc` 模块限制

TabooLib 核心自带的 `database-ioc`（`taboolib.expansion.ioc`）是一个**较简单的 IOC 容器**，存在以下测试限制：

| 限制 | 影响 | 建议 |
|------|------|------|
| `BeanContainer` 是 Kotlin `object` 单例 | 测试间无法隔离，一次 JVM 只能初始化一次 | 考虑添加 `BeanContainer.reset()` 或引入 `IocTestContext` 等价物 |
| 无手动注册 Bean API | 无法注入 Mock 对象 | 添加 `registerBean(name, instance)` 方法 |
| 无独立测试上下文 | 无法在无 TabooLib 运行时下测试 | 参考 `taboolib-ioc` 项目的 `IocTestContext` 实现 |
| `shutdown()` 后 `initialized` 不会重置 | 无法重新初始化 | `shutdown()` 结尾重置 `initialized = false` |

### `taboolib-ioc` 项目已具备的测试能力

`taboolib-ioc`（`top.wcpe.taboolib.ioc`）项目**已提供完善的测试基础设施**：

| 能力 | 模块 | 说明 |
|------|------|------|
| `IocTestContext` | `taboolib-ioc-test` | 轻量级独立 IOC 容器 |
| `@TabooLibIocTest` | `taboolib-ioc-test` | 全生命周期引导测试 |
| `@IocAutowired` | `taboolib-ioc-test` | 测试字段自动注入 |
| `TabooLibIocTestContext` | `taboolib-ioc-test` | 完整 TabooLib + IOC 测试上下文 |
| MockBukkit 测试模板 | `test-v1_20` / `test-v1_12` | 多版本 MockBukkit 集成 |
| 全面的 IOC 功能测试 | `test-v1_20` | 注入/生命周期/作用域/AOP/条件装配 |

### 推荐改进

1. **TabooLib 核心 `database-ioc`**: 添加 `resetForTest()` 方法，在 `shutdown()` 后重置 `initialized = false`，使单例可在测试间复用
2. **TabooLib 核心 `database-ioc`**: 添加 `registerBean(name, instance)` 方法，支持手动注册（便于 Mock 注入）
3. **TabooLib 核心**: `platform-application` 模块的 `App.init()` / `App.shutdown()` 已可用于测试环境引导，但无 JUnit 5 集成，可考虑添加 `@TabooLibAppTest` 注解

## References

- `taboolib-ioc` 测试源码: `taboolib-ioc-test/` 模块
- `taboolib-ioc` 集成测试示例: `test-v1_20/src/test/kotlin/`
- TabooLib 应用平台引导: `platform/platform-application/App.java`
- 相关卡片: `05_ioc.md`（IOC 容器）、`02_lifecycle.md`（生命周期）
- 相关食谱: `setup_testing.md`
