// Replace com.example.plugin with your project's actual package
package com.example.plugin.repository

import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource
import com.easy.query.api.EasyQuery
import com.easy.query.core.annotation.*

/**
 * Database repository example with EasyQuery
 *
 * NOTE: For detailed database implementation, use:
 * $taboolib-corelib-easyquery-persistence-standards
 *
 * This is a minimal example only.
 */

// Entity definition
@Table("player_data")
@EntityProxy
data class PlayerDataEntity(
    @Column(primaryKey = true, comment = "Player UUID", dbType = "VARCHAR(36)")
    var uuid: String = "",

    @Column(comment = "Player coins", dbType = "INT")
    var coins: Int = 0,

    @Column(comment = "Player level", dbType = "INT")
    var level: Int = 1,

    @Column(comment = "Last login timestamp", dbType = "BIGINT")
    var lastLogin: Long = 0
) : ProxyEntityAvailable<PlayerDataEntity, PlayerDataEntityProxy>

// Repository with EasyQuery
@Component
class PlayerDataRepository {

    @Resource
    lateinit var easyQuery: EasyQuery

    fun findByUuid(uuid: String): PlayerDataEntity? {
        return easyQuery.queryable<PlayerDataEntity>()
            .where { it.uuid eq uuid }
            .firstOrNull()
    }

    fun findAll(): List<PlayerDataEntity> {
        return easyQuery.queryable<PlayerDataEntity>()
            .toList()
    }

    fun findByCoinsGreaterThan(minCoins: Int): List<PlayerDataEntity> {
        return easyQuery.queryable<PlayerDataEntity>()
            .where { it.coins gt minCoins }
            .orderBy { it.coins.desc() }
            .toList()
    }

    fun save(entity: PlayerDataEntity) {
        easyQuery.insertable(entity).executeRows()
    }

    fun update(entity: PlayerDataEntity) {
        easyQuery.updatable(entity).executeRows()
    }

    fun delete(uuid: String) {
        easyQuery.deletable<PlayerDataEntity>()
            .where { it.uuid eq uuid }
            .executeRows()
    }

    fun updateCoins(uuid: String, coins: Int) {
        easyQuery.updatable<PlayerDataEntity>()
            .set { it.coins setTo coins }
            .where { it.uuid eq uuid }
            .executeRows()
    }
}

/**
 * Usage in service:
 *
 * @Component
 * class PlayerService {
 *
 *     @Resource
 *     lateinit var repository: PlayerDataRepository
 *
 *     fun getOrCreate(uuid: String): PlayerDataEntity {
 *         return repository.findByUuid(uuid) ?: PlayerDataEntity(uuid).also {
 *             repository.save(it)
 *         }
 *     }
 *
 *     fun addCoins(uuid: String, amount: Int) {
 *         val data = getOrCreate(uuid)
 *         data.coins += amount
 *         repository.update(data)
 *     }
 * }
 */
