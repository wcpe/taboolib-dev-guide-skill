# Recipe: 搭建 TabooLib 插件测试环境

## 适用场景

- 新建 TabooLib 插件项目需要添加测试支持
- 现有项目需要补充单元测试和集成测试
- 需要在 CI 中运行自动化测试

## 前置条件

- TabooLib 6.2.0+
- 使用 `taboolib-ioc`（`top.wcpe.taboolib.ioc`）IOC 框架
- JUnit 5

## 步骤

### 1. 添加测试依赖

在 `build.gradle.kts` 的 `dependencies` 块中添加：

```kotlin
dependencies {
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    // taboolib-ioc 测试模块（按需选择）
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-test:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-core:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-api:{version}")
    testImplementation("top.wcpe.taboolib.ioc:taboolib-ioc-annotation:{version}")

    // MockBukkit（仅 Tier 3 集成测试需要）
    // testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.93.2")
    // testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // 数据库测试（仅 Repository 层需要）
    // testImplementation("com.zaxxer:HikariCP:4.0.3")
    // testImplementation("org.xerial:sqlite-jdbc:3.42.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    workingDir = layout.buildDirectory.dir("test-run").get().asFile.also { it.mkdirs() }
}
```

### 2. 创建测试基类（可选）

```kotlin
package com.example.plugin.test

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import top.wcpe.taboolib.ioc.IocTestContext

abstract class AbstractIocTest {

    protected lateinit var ctx: IocTestContext

    @BeforeEach
    open fun setUp() {
        ctx = IocTestContext()
    }

    @AfterEach
    open fun tearDown() {
        ctx.lifecycleManager.shutdown()
    }

    protected fun registerAndInit(vararg classes: Class<*>) {
        classes.forEach { ctx.register(it) }
        ctx.initialize()
    }
}
```

### 3. 编写 Service 层测试模板

```kotlin
package com.example.plugin.service

import com.example.plugin.test.AbstractIocTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlayerServiceTest : AbstractIocTest() {

    @Test
    @DisplayName("getOrCreate 在玩家不存在时返回新实例")
    fun getOrCreateReturnsNewWhenNotFound() {
        registerAndInit(
            PlayerRepository::class.java,
            PlayerService::class.java
        )
        val service = ctx.getBean(PlayerService::class.java)!!

        val result = service.getOrCreate(UUID.randomUUID())

        assertNotNull(result)
    }

    @Test
    @DisplayName("getOrCreate 在玩家已存在时返回同一实例")
    fun getOrCreateReturnsExistingWhenFound() {
        registerAndInit(
            PlayerRepository::class.java,
            PlayerService::class.java
        )
        val service = ctx.getBean(PlayerService::class.java)!!
        val uuid = UUID.randomUUID()

        service.getOrCreate(uuid)
        val result = service.getOrCreate(uuid)

        assertNotNull(result)
    }
}
```

### 4. 编写 Repository 层数据库测试模板

```kotlin
package com.example.plugin.repository

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

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
    @DisplayName("save 后可通过 UUID 查询到玩家数据")
    fun saveAndFindByUuid() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid, "testPlayer", 100)

        repository.save(data)
        val loaded = repository.findByUuid(uuid)

        assertNotNull(loaded)
        assertEquals("testPlayer", loaded!!.name)
        assertEquals(100, loaded.coins)
    }

    @Test
    @DisplayName("查询不存在的 UUID 返回 null")
    fun findReturnsNullForNonExistentUuid() {
        val result = repository.findByUuid(UUID.randomUUID())
        assertNull(result)
    }
}
```

### 5. 编写 MockBukkit 集成测试模板

```kotlin
package com.example.plugin.integration

import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import top.wcpe.taboolib.ioc.IocTestContext

class MyPluginIntegrationTest {

    private var ctx: IocTestContext? = null

    @BeforeEach
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterEach
    fun tearDown() {
        ctx?.lifecycleManager?.shutdown()
        ctx = null
        MockBukkit.unmock()
    }

    @Test
    @DisplayName("命令处理器与 MockPlayer 交互正常")
    fun commandHandlerWorksWithMockPlayer() {
        val server = MockBukkit.getMock()
        val player = server.addPlayer("testPlayer")

        val testCtx = IocTestContext()
        testCtx.register(PlayerService::class.java)
        testCtx.register(PlayerRepository::class.java)
        testCtx.initialize()
        ctx = testCtx

        val service = testCtx.getBean(PlayerService::class.java)!!
        val result = service.getOrCreate(player.uniqueId)

        assertNotNull(result)
    }
}
```

### 6. 运行测试

```bash
# 运行全部测试
./gradlew test

# 运行指定测试类
./gradlew test --tests "com.example.plugin.service.PlayerServiceTest"

# 运行指定测试方法
./gradlew test --tests "*.PlayerServiceTest.getOrCreateReturnsNewWhenNotFound"
```

## 验证

- [ ] `./gradlew test` 全部通过
- [ ] `./gradlew build` 构建成功（测试是构建的一部分）
- [ ] 测试输出中无 TabooLib 初始化异常
- [ ] MockBukkit 测试在 `@AfterEach` 正确清理

## Troubleshooting

**问题: `NoClassDefFoundError: taboolib/common/TabooLib`**
- 原因: Tier 1 测试不应依赖 TabooLib 运行时
- 修复: 使用 `IocTestContext` 而非 `@TabooLibIocTest`

**问题: `MockBukkit` 测试报 `java.lang.UnsupportedOperationException`**
- 原因: MockBukkit 未 mock 或版本不匹配
- 修复: 确认 `@BeforeEach` 调用 `MockBukkit.mock()`，确认 MockBukkit 版本匹配测试的 Minecraft 版本

**问题: `BeanInstantiationException` 在 IocTestContext**
- 原因: 缺少依赖 Bean 的注册
- 修复: 确认所有被注入的类型都已 `ctx.register()`
