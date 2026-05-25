# Card 08: Database Integration

## When to Use

Load this card when:
- User asks about "database", "EasyQuery", "repository", "DAO", "数据库"
- Need to persist player data or plugin state
- Implementing data storage layer
- Troubleshooting database connection or query issues

## Core Idea

TabooLib integrates with EasyQuery ORM via CoreLib for database persistence. Use entity classes with annotations, repository pattern for queries, and Code First schema initialization. **For detailed entity modeling, repository patterns, and persistence standards, use `$taboolib-corelib-easyquery-persistence-standards` skill**.

## Quick Reference

### Module Setup

**build.gradle.kts**:
```kotlin
dependencies {
    taboo("database-ioc")  // IoC + EasyQuery support
}
```

### Basic Entity

```kotlin
import com.easy.query.core.annotation.*

@Table("player_data")
@EntityProxy
data class PlayerDataEntity(
    @Column(primaryKey = true, comment = "Player UUID")
    var uuid: String = "",
    
    @Column(comment = "Player coins", dbType = "INT")
    var coins: Int = 0,
    
    @Column(comment = "Last login", dbType = "BIGINT")
    var lastLogin: Long = 0
) : ProxyEntityAvailable<PlayerDataEntity, PlayerDataEntityProxy>
```

### Basic Repository

```kotlin
@Component
class PlayerDataRepository {
    
    @Resource
    lateinit var easyQuery: EasyQuery
    
    fun findByUuid(uuid: String): PlayerDataEntity? {
        return easyQuery.queryable<PlayerDataEntity>()
            .where { it.uuid eq uuid }
            .firstOrNull()
    }
    
    fun save(entity: PlayerDataEntity) {
        easyQuery.insertable(entity).executeRows()
    }
    
    fun update(entity: PlayerDataEntity) {
        easyQuery.updatable(entity).executeRows()
    }
}
```

## Important Notes

- **Entity rules**: Must have `@Table`, `@EntityProxy`, `@Column` with `comment` and `dbType`
- **Schema initialization**: Use EasyQuery Code First, never raw SQL
- **Thread safety**: Database queries must run on async thread
- **Transaction management**: See architecture skill for transaction patterns

## References

- **Detailed guidance**: Use `$taboolib-corelib-easyquery-persistence-standards` skill
- Related cards: `05_ioc.md` (repository injection), `07_scheduler.md` (async queries)
- Related recipes: `create_database_module.md`
