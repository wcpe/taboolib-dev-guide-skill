// Replace com.example.plugin with your project's actual package
package com.example.plugin.service

import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import taboolib.expansion.ioc.annotation.PostConstruct
import taboolib.expansion.ioc.annotation.PreDestroy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * IoC service example with dependency injection
 *
 * Features:
 * - @Component for bean registration
 * - @Resource for dependency injection
 * - @PostConstruct for initialization
 * - @PreDestroy for cleanup
 */

// Service layer
@Component
class PlayerService {

    @Resource
    lateinit var repository: PlayerRepository

    @PostConstruct
    fun init() {
        println("PlayerService initialized")
    }

    @PreDestroy
    fun cleanup() {
        println("PlayerService cleanup")
    }

    fun getPlayerData(uuid: UUID): PlayerData {
        return repository.findByUuid(uuid) ?: createDefault(uuid)
    }

    fun updateCoins(uuid: UUID, amount: Int) {
        val data = getPlayerData(uuid)
        data.coins += amount
        repository.save(data)
    }

    private fun createDefault(uuid: UUID): PlayerData {
        val data = PlayerData(uuid)
        repository.save(data)
        return data
    }
}

// Repository layer
@Component
class PlayerRepository {

    private val cache = ConcurrentHashMap<UUID, PlayerData>()

    fun findByUuid(uuid: UUID): PlayerData? {
        return cache[uuid]
    }

    fun save(data: PlayerData) {
        cache[data.uuid] = data
    }

    fun delete(uuid: UUID) {
        cache.remove(uuid)
    }

    fun findAll(): Collection<PlayerData> {
        return cache.values
    }
}

// Data class (not a bean - no @Component)
data class PlayerData(
    val uuid: UUID,
    var coins: Int = 0,
    var level: Int = 1,
    var lastLogin: Long = System.currentTimeMillis()
)

/**
 * Usage in command:
 *
 * @CommandHeader(name = "balance")
 * object BalanceCommand {
 *
 *     @Resource
 *     lateinit var playerService: PlayerService
 *
 *     @CommandBody
 *     val main = mainCommand {
 *         execute<ProxyPlayer> { player, _, _ ->
 *             val data = playerService.getPlayerData(player.uniqueId)
 *             player.sendMessage("Balance: ${data.coins}")
 *         }
 *     }
 * }
 */
