// Replace com.example.plugin with your project's actual package
package com.example.plugin.scheduler

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitChain
import taboolib.common.platform.PlatformTask
import org.bukkit.entity.Player

/**
 * Scheduler example with async tasks
 *
 * Features:
 * - Periodic tasks with delay
 * - Async/sync task switching
 * - Task cancellation
 * - Sequential async operations with submitChain
 */
object SchedulerExample {

    private var autoSaveTask: PlatformTask? = null

    // Start tasks when scheduler ready (ACTIVE lifecycle)
    @Awake(LifeCycle.ACTIVE)
    fun startTasks() {
        // Periodic auto-save (every 5 minutes = 6000 ticks)
        autoSaveTask = submit(delay = 6000, period = 6000, async = true) {
            println("Auto-saving data...")
            // Simulate database save
            Thread.sleep(100)

            // Back to main thread for confirmation
            submit(async = false) {
                println("Auto-save completed!")
            }
        }
    }

    // Stop tasks on plugin disable
    @Awake(LifeCycle.DISABLE)
    fun stopTasks() {
        autoSaveTask?.cancel()
        println("Cancelled auto-save task")
    }

    // Declarative periodic task (every second on main thread)
    @Schedule(period = 20, async = false)
    fun updateScoreboard() {
        // Runs every second on main thread
        // Update scoreboard for all online players
        // onlinePlayers.forEach { updatePlayerScoreboard(it) }
    }

    // Example: Load player data on join
    fun loadPlayerDataAsync(player: Player) {
        submit(async = true) {
            // Step 1: Load from database (async thread)
            println("Loading data for ${player.name}...")
            val data = loadFromDatabase(player.uniqueId)

            // Step 2: Apply to player (main thread for Bukkit API)
            submit(async = false) {
                player.sendMessage("§aWelcome back!")
                player.sendMessage("§eCoins: ${data.coins}")
                // player.inventory.addItem(data.items)
            }
        }
    }

    // Example: Sequential async operations with submitChain
    fun processTransaction(player: Player, amount: Int) {
        submitChain {
            // Step 1: Async database query
            println("Checking balance...")
            val balance = getBalance(player.uniqueId)

            // Step 2: Main thread validation
            sync {
                if (balance < amount) {
                    player.sendMessage("§cInsufficient balance!")
                    return@submitChain  // Exit chain early
                }
                player.sendMessage("§eProcessing transaction...")
            }

            // Step 3: Async database update
            async {
                println("Deducting $amount from balance...")
                deductBalance(player.uniqueId, amount)
            }

            // Step 4: Main thread confirmation
            sync {
                player.sendMessage("§aTransaction successful!")
                player.sendMessage("§eNew balance: ${balance - amount}")
            }
        }
    }

    // Example: Delayed one-time task
    fun delayedAction(player: Player) {
        // Execute after 3 seconds (60 ticks)
        submit(delay = 60) {
            player.sendMessage("§eThis message appears 3 seconds later!")
        }
    }

    // Dummy data functions (replace with actual implementation)
    private fun loadFromDatabase(uuid: java.util.UUID): PlayerData {
        Thread.sleep(50)  // Simulate IO
        return PlayerData(uuid, coins = 100)
    }

    private fun getBalance(uuid: java.util.UUID): Int {
        Thread.sleep(50)  // Simulate IO
        return 100
    }

    private fun deductBalance(uuid: java.util.UUID, amount: Int) {
        Thread.sleep(50)  // Simulate IO
    }

    private data class PlayerData(val uuid: java.util.UUID, val coins: Int)
}
