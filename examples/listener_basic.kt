// Replace com.example.plugin with your project's actual package
package com.example.plugin.listener

import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.submit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.entity.Player

/**
 * Basic event listener example
 *
 * Features:
 * - Auto-registration with @SubscribeEvent
 * - Event priority control
 * - Async event handling
 * - Event cancellation
 */
object PlayerListener {

    // Basic join handler
    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        event.joinMessage = "§a${player.name} joined the server"

        // Load data asynchronously
        submit(async = true) {
            // Simulate database load
            val data = loadPlayerData(player.uniqueId)

            // Back to main thread for Bukkit API
            submit(async = false) {
                player.sendMessage("§eWelcome back!")
                player.sendMessage("§aCoins: ${data.coins}")
            }
        }
    }

    // Quit handler with cleanup
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        event.quitMessage = "§c${player.name} left the server"

        // Save data asynchronously
        submit(async = true) {
            savePlayerData(player.uniqueId)
        }
    }

    // Block break with protection check
    @SubscribeEvent(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        // Only fires if event NOT cancelled by protection plugins
        // Give reward for breaking block
        // rewardService.giveBlockReward(player, block.type)

        player.sendMessage("§aYou broke ${block.type}")
    }

    // Damage handler with cancellation
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player

            // Cancel damage if player has god mode
            if (player.hasPermission("example.godmode")) {
                event.isCancelled = true
                player.sendMessage("§eGod mode protected you!")
            }
        }
    }

    // Monitor priority for logging (read-only)
    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onDamageMonitor(event: EntityDamageEvent) {
        // Log all damage events, even cancelled ones
        // Don't modify event in MONITOR priority
        if (event.entity is Player) {
            val player = event.entity as Player
            println("${player.name} took ${event.damage} damage (cancelled: ${event.isCancelled})")
        }
    }

    // Dummy data functions (replace with actual implementation)
    private fun loadPlayerData(uuid: java.util.UUID): PlayerData {
        return PlayerData(uuid, coins = 100)
    }

    private fun savePlayerData(uuid: java.util.UUID) {
        // Save to database
    }

    private data class PlayerData(val uuid: java.util.UUID, val coins: Int)
}
