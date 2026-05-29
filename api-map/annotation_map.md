# TabooLib Annotation Map

**⚠️ WARNING**: TabooLib APIs may change across versions. Always check user's project existing imports first before using these annotations.

## Lifecycle Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Awake` | `taboolib.common.platform` | `value: LifeCycle = CONST` | Mark method to execute at specific lifecycle stage |
| `@Inject` | `taboolib.common` | None | Mark class for ClassVisitor scanning |

**Verification**: `grep -r "@Awake" src/` to find existing usage in project.

---

## Command Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@CommandHeader` | `taboolib.common.platform.command` | `name`, `aliases`, `description`, `usage`, `permission`, `permissionMessage`, `permissionDefault`, `newParser` | Define main command structure |
| `@CommandBody` | `taboolib.common.platform.command` | `aliases`, `optional`, `permission`, `permissionDefault`, `hidden`, `description` | Define subcommand or argument |

**Verification**: `grep -r "@CommandHeader" src/` to find existing command definitions.

---

## Event Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@SubscribeEvent` | `taboolib.common.platform.event` | `priority: EventPriority = NORMAL`, `ignoreCancelled: Boolean = false`, `bind: String = ""` | Register event listener |

**Verification**: `grep -r "@SubscribeEvent" src/` to find existing listeners.

---

## Configuration Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Config` | `taboolib.module.configuration` | `value: String = "config.yml"`, `target: String = ""`, `migrate: Boolean = false`, `autoReload: Boolean = false`, `concurrent: Boolean = true` | Auto-load configuration file |

**Verification**: `grep -r "@Config" src/` to find existing config fields.

---

## IoC Annotations (database-ioc module)

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Component` | `taboolib.expansion.ioc.annotation` | `scope: BeanScope = SINGLETON` | Mark class as IoC bean |
| `@Service` | `taboolib.expansion.ioc.annotation` | `scope: BeanScope = SINGLETON` | Alias for @Component (service layer) |
| `@Repository` | `taboolib.expansion.ioc.annotation` | `scope: BeanScope = SINGLETON` | Alias for @Component (repository layer) |
| `@Controller` | `taboolib.expansion.ioc.annotation` | `scope: BeanScope = SINGLETON` | Alias for @Component (controller layer) |
| `@Resource` | `taboolib.expansion.ioc.annotation` | None | Field-level dependency injection |
| `@PostConstruct` | `taboolib.expansion.ioc.annotation` | None | Callback after bean initialization |
| `@PreDestroy` | `taboolib.expansion.ioc.annotation` | None | Callback before bean destruction (SINGLETON/PLAYER only) |

**Verification**: `grep -r "@Component" src/` to find existing beans.

**⚠️ CRITICAL**: IoC requires `database-ioc` module and `relocate` configuration in build.gradle.kts.

---

## Scheduler Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Schedule` | `taboolib.common.platform` | `delay: Long = 0`, `period: Long = 0`, `async: Boolean = false` | Declarative periodic task |

**Verification**: `grep -r "@Schedule" src/` to find existing scheduled tasks.

---

## Platform Annotations

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@PlatformSide` | `taboolib.common.platform` | `value: Array<Platform>` | Restrict class to specific platforms |
| `@PlatformService` | `taboolib.common.platform` | None | Mark interface as cross-platform service |

**Verification**: Check if user's plugin is multi-platform or Bukkit-only.

---

## Database Annotations (EasyQuery)

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Table` | `com.easy.query.core.annotation` | `value: String` | Define table name |
| `@EntityProxy` | `com.easy.query.core.annotation` | None | Enable entity proxy generation |
| `@Column` | `com.easy.query.core.annotation` | `primaryKey`, `comment`, `dbType`, etc. | Define column properties |

**Verification**: `grep -r "@Table" src/` to find existing entities.

**Note**: For detailed database annotations, use `$taboolib-corelib-easyquery-persistence-standards` skill.

---

## Incision Annotations (module/incision)

| Annotation | Package | Parameters | Usage |
|------------|---------|------------|-------|
| `@Surgeon` | `taboolib.module.incision.annotation` | `priority: Int = 0` | Mark advice holder object |
| `@Lead` | `taboolib.module.incision.annotation` | `scope: String`, `pattern: InsnPattern`, `where: String` | Method entry advice, executes before original body |
| `@Trail` | `taboolib.module.incision.annotation` | `scope: String`, `onThrow: Boolean = true`, `pattern: InsnPattern`, `where: String` | Method exit advice, executes after original body |
| `@Splice` | `taboolib.module.incision.annotation` | `scope: String`, `pattern: InsnPattern`, `where: String` | Surround advice, must explicitly call proceed() or override return |
| `@Bypass` | `taboolib.module.incision.annotation` | `method: String`, `site: Site`, `pattern: InsnPattern`, `where: String` | Call site replacement |
| `@Graft` | `taboolib.module.incision.annotation` | `method: String`, `site: Site`, `pattern: InsnPattern`, `where: String` | Append/prepend around anchor point |
| `@Trim` | `taboolib.module.incision.annotation` | `method: String`, `kind: Kind`, `index: Int = 0`, `site: Site`, `pattern: InsnPattern`, `where: String` | Return value rewriting (ARG/RETURN/VAR) |
| `@Excise` | `taboolib.module.incision.annotation` | `scope: String`, `pattern: InsnPattern`, `where: String` | Replace entire method body |
| `@Operation` | `taboolib.module.incision.annotation` | `id: String = ""`, `priority: Int = 0`, `enabled: Boolean = true` | Advice metadata |
| `@Version` | `taboolib.module.incision.annotation` | `start: String = ""`, `end: String = ""`, `matcher: String = ""` | Version gate |
| `@KotlinTarget` | `taboolib.module.incision.annotation` | `companionInstance: Boolean = false`, `jvmStaticBridge: Boolean = false` | Cover both companion and @JvmStatic paths for Kotlin targets |
| `@Site` | `taboolib.module.incision.annotation` | `anchor: Anchor`, `target: String = ""`, `shift: Shift = Shift.BEFORE`, `ordinal: Int = -1`, `offset: Int = 0` | Anchor specification |
| `@SurgeryDesk` | `taboolib.module.incision.annotation` | `priority: Int = 0` | Mark DSL patch holder object |
| `@InsnPattern` | `taboolib.module.incision.annotation` | `steps: Array<Step> = []` | Bytecode instruction pattern matching |
| `@Step` | `taboolib.module.incision.annotation` | `opcode: Op`, `owner: String`, `name: String`, `desc: String`, `cst: String`, `repeat: Int = 1` | Step for pattern-based advice |
| `@Scope` | `taboolib.module.incision.annotation` | (scope specification) | Scope specification for advice matching |

**Verification**: `grep -r "@Surgeon" src/` to find existing incisions.

**⚠️ CRITICAL**: Incision requires `module-incision` module and `-javaagent` or JVMTI agent for weaving.

---

## Version-Specific Notes

**TabooLib 6.2.0+**:
- All annotations listed above are available
- IoC annotations require `database-ioc` module
- `@Schedule` supports async parameter

**TabooLib 6.1.x**:
- Some IoC annotations may differ
- Check wiki for version-specific differences

---

## Usage Example

```kotlin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.command.*
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.Config
import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource

@Component
class MyService {
    
    @Config("config.yml")
    lateinit var config: Configuration
    
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        // Initialization
    }
}

@CommandHeader(name = "example")
object MyCommand {
    
    @Resource
    lateinit var myService: MyService
    
    @CommandBody
    val reload = subCommand { }
}

object MyListener {
    
    @SubscribeEvent
    fun onJoin(event: PlayerJoinEvent) { }
}
```

---

## Verification Workflow

Before using any annotation:

1. **Check user's project**: `grep -r "@AnnotationName" src/`
2. **Verify package path**: Check import statements in existing files
3. **Check TabooLib version**: Read `build.gradle.kts` for version
4. **Verify module installed**: Check `taboolib { env { install(...) } }`
5. **If uncertain**: Mark as "unverified - check project" and ask user

---

## References

- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/`
- Source code: `taboolib/module/database/database-ioc/src/main/kotlin/taboolib/expansion/ioc/`
- Related maps: `package_map.md`, `lifecycle_map.md`, `common_class_map.md`
