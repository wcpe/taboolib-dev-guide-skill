// Replace com.example.plugin with your project's actual package
package com.example.plugin.nms

import taboolib.module.nms.nmsProxy
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.remap.require
import taboolib.common.util.unsafeLazy
import org.bukkit.entity.Player

/**
 * nmsProxy 跨版本代理示例
 *
 * 功能：
 * - 跨版本发送 ActionBar 消息
 * - 跨版本发送 Title 消息
 * - require() 字节码级类存在检查
 * - unsafeLazy 安全延迟初始化
 * - MinecraftVersion 版本差异处理
 *
 * 支持版本：1.8 ~ 1.21+
 */

// =============================================================================
// Type Aliases：避免冗长的完全限定类名
// =============================================================================

// 1.20+ (Mojang Mapping / Universal Remap)
typealias CraftPlayer_120R4 = org.bukkit.craftbukkit.v1_20_R4.entity.CraftPlayer
typealias ServerPlayer = net.minecraft.server.level.ServerPlayer
typealias ChatComponent = net.minecraft.network.chat.Component
typealias ActionBarPacket = net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
typealias ServerConnection = net.minecraft.server.network.ServerGamePacketListenerImpl
typealias TitlePacket = net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
typealias SubtitlePacket = net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
typealias TimesPacket = net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket

// 1.12 （传统 obfuscated names）
typealias CraftPlayer_112R1 = org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer

// =============================================================================
// 抽象代理类 1：跨版本 ActionBar
// =============================================================================

abstract class NMSCustomActionBar {

    /**
     * 发送 ActionBar 消息
     *
     * @param player  目标玩家
     * @param message 消息内容（支持 § 颜色代码）
     */
    abstract fun sendActionBar(player: Player, message: String)

    /**
     * 使用 require() 在字节码层面检查类是否存在
     * 零运行时开销 —— 比 Class.forName() + try-catch 更高效安全
     *
     * @return true 如果当前版本支持 ActionBar
     */
    fun isSupported(): Boolean {
        return require(ChatComponent::class.java) && require(ActionBarPacket::class.java)
    }

    companion object {
        /**
         * unsafeLazy：延迟初始化代理实例
         * - 在无法找到 Impl 或初始化失败时返回 null，不会崩溃
         * - 首次访问时触发 nmsProxy 实例化（确保 Minecraft 已初始化）
         */
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomActionBar>() }
    }
}

// =============================================================================
// Impl 类 1：ActionBar 实现（多版本分支）
// =============================================================================

class NMSCustomActionBarImpl : NMSCustomActionBar() {

    override fun sendActionBar(player: Player, message: String) {
        when {
            // 1.8 ~ 1.12：使用 obfuscated NMS 包名
            MinecraftVersion.isLower(MinecraftVersion.V1_13) -> {
                val craftPlayer = player as CraftPlayer_112R1
                val chatComponent = net.minecraft.server.v1_12_R1.ChatComponentText(message)
                val packet = net.minecraft.server.v1_12_R1.PacketPlayOutChat(
                    chatComponent,
                    net.minecraft.server.v1_12_R1.ChatMessageType.GAME_INFO
                )
                craftPlayer.handle.playerConnection.sendPacket(packet)
            }

            // 1.17+：使用 Universal Remap / Mojang Mapping 类名
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
                val craftPlayer = player as CraftPlayer_120R4
                val component = ChatComponent.literal(message)
                val packet = ActionBarPacket(component)
                craftPlayer.handle.connection.send(packet)
            }
        }
    }
}

// =============================================================================
// 抽象代理类 2：跨版本 Title
// =============================================================================

abstract class NMSCustomTitle {

    /**
     * 发送 Title（大标题）
     *
     * @param player   目标玩家
     * @param title    标题文本
     * @param subtitle 副标题文本
     * @param fadeIn   淡入时间（ticks）
     * @param stay     显示时间（ticks）
     * @param fadeOut  淡出时间（ticks）
     */
    abstract fun sendTitle(
        player: Player,
        title: String,
        subtitle: String = "",
        fadeIn: Int = 10,
        stay: Int = 70,
        fadeOut: Int = 20
    )

    fun isSupported(): Boolean {
        return require(TitlePacket::class.java)
                && require(SubtitlePacket::class.java)
                && require(TimesPacket::class.java)
    }

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSCustomTitle>() }
    }
}

// =============================================================================
// Impl 类 2：Title 实现
// =============================================================================

class NMSCustomTitleImpl : NMSCustomTitle() {

    override fun sendTitle(
        player: Player,
        title: String,
        subtitle: String,
        fadeIn: Int,
        stay: Int,
        fadeOut: Int
    ) {
        when {
            // 1.8 ~ 1.12
            MinecraftVersion.isLower(MinecraftVersion.V1_13) -> {
                val craftPlayer = player as CraftPlayer_112R1
                val connection = craftPlayer.handle.playerConnection

                // 设置显示时间
                val timesPacket = net.minecraft.server.v1_12_R1.PacketPlayOutTitle(
                    net.minecraft.server.v1_12_R1.PacketPlayOutTitle.EnumTitleAction.TIMES,
                    null, fadeIn, stay, fadeOut
                )
                connection.sendPacket(timesPacket)

                // 发送副标题
                if (subtitle.isNotEmpty()) {
                    val subtitlePacket = net.minecraft.server.v1_12_R1.PacketPlayOutTitle(
                        net.minecraft.server.v1_12_R1.PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                        net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer.a(
                            "{\"text\":\"$subtitle\"}"
                        )
                    )
                    connection.sendPacket(subtitlePacket)
                }

                // 发送标题
                val titlePacket = net.minecraft.server.v1_12_R1.PacketPlayOutTitle(
                    net.minecraft.server.v1_12_R1.PacketPlayOutTitle.EnumTitleAction.TITLE,
                    net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer.a(
                        "{\"text\":\"$title\"}"
                    )
                )
                connection.sendPacket(titlePacket)
            }

            // 1.17+ (Mojang Mapping)
            MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_17) -> {
                val craftPlayer = player as CraftPlayer_120R4
                val connection: ServerConnection = craftPlayer.handle.connection

                // 设置动画时间
                connection.send(TimesPacket(fadeIn, stay, fadeOut))

                // 发送副标题
                if (subtitle.isNotEmpty()) {
                    connection.send(SubtitlePacket(ChatComponent.literal(subtitle)))
                }

                // 发送标题
                connection.send(TitlePacket(ChatComponent.literal(title)))
            }
        }
    }
}

// =============================================================================
// 抽象代理类 3：NMS 实体工具
// =============================================================================

abstract class NMSEntityHelper {

    /**
     * 设置实体 AI 开关
     *
     * @param entity 目标实体
     * @param hasAI  true 启用 AI，false 禁用
     */
    abstract fun setEntityAI(entity: org.bukkit.entity.Entity, hasAI: Boolean)

    companion object {
        val INSTANCE by unsafeLazy { nmsProxy<NMSEntityHelper>() }
    }
}

// =============================================================================
// Impl 类 3：实体工具实现
// =============================================================================

class NMSEntityHelperImpl : NMSEntityHelper() {

    override fun setEntityAI(entity: org.bukkit.entity.Entity, hasAI: Boolean) {
        val craftEntity = entity as org.bukkit.craftbukkit.v1_20_R4.entity.CraftEntity
        val nmsEntity = craftEntity.handle as net.minecraft.world.entity.Mob
        nmsEntity.setNoAi(!hasAI)
    }
}

// =============================================================================
// 使用示例（可直接集成到命令或监听器）
// =============================================================================

/**
 * 在命令中使用 nmsProxy
 *
 * @CommandHeader(name = "nmsdemo")
 * object NMSDemoCommand {
 *
 *     @CommandBody
 *     val actionbar = subCommand {
 *         dynamic("message") {
 *             execute<ProxyPlayer> { player, context, _ ->
 *                 val message = context.argument(0)
 *                 val proxy = NMSCustomActionBar.INSTANCE
 *                 if (proxy != null && proxy.isSupported()) {
 *                     proxy.sendActionBar(player, message)
 *                 } else {
 *                     player.sendMessage("§c当前版本不支持 ActionBar")
 *                 }
 *             }
 *         }
 *     }
 *
 *     @CommandBody
 *     val title = subCommand {
 *         dynamic("title") {
 *             dynamic("subtitle", optional = true) {
 *                 execute<ProxyPlayer> { player, context, _ ->
 *                     val title = context.argument(0)
 *                     val subtitle = context.argumentOrNull(1) ?: ""
 *                     val proxy = NMSCustomTitle.INSTANCE
 *                     if (proxy != null && proxy.isSupported()) {
 *                         proxy.sendTitle(player, title, subtitle)
 *                     } else {
 *                         player.sendMessage("§c当前版本不支持 Title")
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     @CommandBody
 *     val main = mainCommand {
 *         execute<ProxyPlayer> { player, _, _ ->
 *             player.sendMessage("§e=== NMS 功能演示 ===")
 *             player.sendMessage("§a/nmsdemo actionbar <message> - 发送 ActionBar")
 *             player.sendMessage("§a/nmsdemo title <title> [subtitle] - 发送 Title")
 *         }
 *     }
 * }
 */

// =============================================================================
// 使用示例（在监听器中）
// =============================================================================

/**
 * 在监听器中使用 nmsProxy
 *
 * object JoinListener {
 *
 *     @SubscribeEvent
 *     fun onJoin(event: PlayerJoinEvent) {
 *         val player = event.player
 *
 *         // 发送欢迎 Title
 *         NMSCustomTitle.INSTANCE?.sendTitle(
 *             player,
 *             "§6欢迎你",
 *             "§e${player.name}",
 *             fadeIn = 20,
 *             stay = 60,
 *             fadeOut = 20
 *         )
 *
 *         // 发送 ActionBar 提示
 *         NMSCustomActionBar.INSTANCE?.sendActionBar(
 *             player,
 *             "§a欢迎回来！输入 /nmsdemo 查看功能"
 *         )
 *     }
 * }
 */
