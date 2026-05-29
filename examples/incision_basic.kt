// Replace com.example.plugin with your project's actual package
package com.example.plugin.incision

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.incision.*

/**
 * Incision 运行时字节码织入示例
 *
 * Features:
 * - @Lead / @Trail 观察型 Advice（入口/出口织入，scope DSL 格式）
 * - @Splice 包围型 Advice（带 proceed/override 控制流）
 * - @Bypass + Site 调用点拦截（Site 嵌入注解内）
 * - 私有字段访问（field() getter / fieldSet() setter / staticField()）
 * - Suture 生命周期管理（通过 Scalpel.find()）
 * - 诊断命令使用（list / show / heal / dump / plugins）
 */
@Surgeon(priority = 0)
object IncisionExample {

    // ============================================================
    // 示例 1: @Lead — 方法入口观察（日志/计时）
    // ============================================================
    // 在目标方法执行前注入代码。原方法照常执行。
    // 注意：不调用 proceed() 原方法也仍然会执行——@Lead 不能拦截执行。
    @Operation(priority = 0, id = "lead_world_tick")
    @Lead(
        scope = "method:net.minecraft.server.level.WorldServer#tick()V"
    )
    fun onWorldTickEntry(lead: Theatre) {
        // self 是属性，获取方法所属实例（WorldServer 对象）
        val world = lead.self

        // args 是属性 val args: Array<Any?>
        // WorldServer#tick() 无参数

        // proceed() 继续执行原方法体
        lead.proceed()
    }

    // ============================================================
    // 示例 2: @Trail — 方法出口观察（返回值/异常记录）
    // ============================================================
    // 在目标方法所有退出点（正常返回/异常抛出）注入代码。
    @Operation(priority = 0, id = "trail_world_tick")
    @Trail(
        scope = "method:net.minecraft.server.level.WorldServer#tick()V"
    )
    fun onWorldTickExit(trail: Theatre) {
        // proceed() 执行原方法逻辑（在 @Trail 中通常是必需的）
        trail.proceed()

        // throwable 是属性 val throwable: Throwable?，检查是否抛出了异常
        if (trail.throwable != null) {
            println("[Incision] WorldServer#tick threw exception: ${trail.throwable}")
        }
    }

    // ============================================================
    // 示例 3: @Splice — 包围控制流（条件执行/计时/短路）
    // ============================================================
    // @Splice 包围整个方法体。必须显式调用 proceed() 或 override()。
    // 如果不调用，原方法体永远不会执行！
    @Operation(priority = 10, id = "splice_entity_hurt")
    @Splice(
        // scope 格式: method:owner#methodName(args)returnType
        // Z = boolean 返回类型描述符
        scope = "method:net.minecraft.world.entity.Entity#hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
    )
    fun onEntityHurt(splice: Theatre) {
        val entity = splice.self
        val damageSource = splice.arg<Any>(0)    // 第一个参数：DamageSource
        val damageAmount = splice.arg<Float>(1)  // 第二个参数：Float

        // 条件短路示例：如果伤害量为 0，直接返回 false（不执行原方法）
        if (damageAmount <= 0f) {
            splice.override(false)  // ← override() 等同于 skip()，短路返回
            return
        }

        // 正常流程：执行原方法
        splice.proceed()
    }

    // ============================================================
    // 示例 4: @Bypass + Site — 调用点拦截（替换方法调用结果）
    // ============================================================
    // @Bypass 替换目标方法内部某个方法调用的返回值。
    // Site 嵌入在 @Bypass 注解内部，不是单独的 @Site 注解。
    @Operation(priority = 10, id = "bypass_block_state")
    @Bypass(
        method = "net.minecraft.server.level.ServerLevel#tick()V",
        site = Site(
            anchor = Anchor.INVOKE,
            target = "net.minecraft.world.level.chunk.LevelChunk#getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0  // 第一次匹配（默认 -1 = 全部匹配）
        )
    )
    fun bypassBlockStateCall(bypass: Theatre) {
        // 访问目标调用的参数
        val blockPos = bypass.arg<Any>(0)

        // 检查是否有缓存值
        val cached = blockStateCache[blockPos]
        if (cached != null) {
            bypass.override(cached)  // ← 短路返回缓存值，不执行原调用
        } else {
            bypass.proceed()         // ← 正常执行原调用
        }
    }

    // ============================================================
    // 示例 5: 私有字段访问（field() getter / fieldSet() setter / staticField()）
    // ============================================================
    // field<T>("fieldName") 仅返回 getter lambda
    // fieldSet<T>("fieldName") 仅返回 setter lambda
    // staticField<T>(ownerClass, "fieldName") Class 参数在前
    @Operation(priority = 0, id = "field_access")
    @Lead(
        scope = "method:net.minecraft.world.entity.LivingEntity#tick()V"
    )
    fun accessPrivateField(lead: Theatre) {
        val entity = lead.self

        // 创建 getter lambda：field<T>("fieldName")
        val getHealth = field<Float>("health")       // (Any) -> Float

        // 创建 setter lambda：fieldSet<T>("fieldName")
        val setHealth = fieldSet<Float>("health")    // (Any, Float) -> Unit

        // 读取私有字段
        val currentHealth = getHealth(entity)

        // 写入私有字段
        if (currentHealth <= 0f) {
            setHealth(entity, 1f)
        }

        // 静态字段访问 — staticField<T>(ownerClass, name)
        // val getServer = staticField<MinecraftServer>(MinecraftServer::class.java, "server")

        // 私有方法调用 — method<ReturnType>("methodName", *paramTypes)
        // val invokeDropAll = method<Unit>("dropAllDeathLoot", DamageSource::class.java)

        lead.proceed()
    }

    // 私有状态（演示 Bypass 缓存）
    private val blockStateCache = mutableMapOf<Any, Any?>()
}

// ============================================================
// Suture 生命周期管理
// ============================================================
// 在 ENABLE 阶段 Suture 已创建，可以进行管理操作
@Awake(LifeCycle.ENABLE)
fun managePatches() {
    // 获取指定 Operation 的 Suture — 通过 Scalpel.find()
    val suture = Scalpel.find("splice_entity_hurt")
    if (suture != null) {
        println("[Incision] Patch 'splice_entity_hurt' state: ${suture.state}")
    }

    // 暂停补丁（不触发但织入存在）
    // suture?.suspend()

    // 恢复补丁（回到 ARMED 状态）
    // suture?.resume()

    // 关闭补丁（等同于 heal，移除织入，恢复原始字节码）
    // suture?.heal()
}

@Awake(LifeCycle.DISABLE)
fun cleanupPatches() {
    // 遍历所有 Suture 进行关闭
    for (suture in Scalpel.sutures) {
        suture.heal()
    }
    println("[Incision] All patches healed")
}
