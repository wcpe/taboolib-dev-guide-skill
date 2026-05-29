---
name: taboolib-dev-guide
description: LLM-first progressive query development handbook for TabooLib Minecraft plugin development. Use when creating or modifying TabooLib-based Bukkit/Paper plugins involving commands, config, IOC, listeners, scheduler, database, i18n, or troubleshooting lifecycle/injection errors. Guides task routing, prevents API hallucination, enables minimal compilable code generation matching user's project style.
---

# TabooLib Development Guide

## Purpose

This skill is an **LLM-first progressive query development handbook** for TabooLib Minecraft plugin development. It helps LLM:

1. **Route tasks correctly** - Map user keywords to relevant knowledge cards, recipes, and examples
2. **Prevent API hallucination** - Enforce "check project first, query MCP second, never guess" discipline
3. **Generate minimal code** - Produce compilable code matching user's existing project style
4. **Provide quick reference** - Short, scannable cards/recipes/maps optimized for LLM consumption

This skill complements existing TabooLib skills:
- `taboolib-bukkit-plugin-architecture-standards` - Architecture enforcement and code review
- `taboolib-corelib-easyquery-persistence-standards` - Database persistence standards
- `taboolib-corebridge-packet-standards` - CoreBridge packet integration

## Architecture Alignment

This skill follows the architecture standards defined in `taboolib-bukkit-plugin-architecture-standards`. Key alignment points:

### Plugin Main Class
- ✅ Must be Kotlin `object`, not `class`
- ✅ Never extend `JavaPlugin` or `Plugin()` directly
- ✅ Use `@Awake(LifeCycle.ENABLE/DISABLE)` instead of `onEnable()/onDisable()`
- ✅ Use `bukkitPlugin` property, not `BukkitPlugin.getInstance()`

### Command System
- ✅ 命令中禁止业务逻辑处理（主线程阻塞风险）
- ✅ 命令只能做参数解析、参数校验、权限校验
- ✅ 必须立即 `submitAsync` 委托到 service 层
- ✅ WCPE TabooLib 6.2.4-SNAPSHOT specific rules enforced
- ✅ Permission message rules for Paper servers

### Thread Safety Model
- ✅ 主线程禁止 DB/Redis/HTTP/文件 IO/阻塞等待
- ✅ 异步线程禁止 Bukkit API 写操作
- ✅ Placeholder/HUD 热路径禁止同步回源
- ✅ ENABLE 生命周期启动准备例外（数据库初始化）
- ✅ 入口点线程规则表（命令/事件/GUI/Placeholder）

### Chest Menu Standards
- ✅ Display-only menus must use `virtualize()` or `hidePlayerInventory()`
- ✅ All materials must use `XMaterial`, not `Material`
- ✅ Functional buttons must have lore/description
- ✅ Storable menus must lock decoration slots and validate items

### Layered Architecture
- ✅ Command/Listener → Service → Repository → Entity
- ✅ Service owns business logic, commands delegate immediately
- ✅ Repository only owns persistence, no business rules

**When to escalate to architecture skill**: If task involves architecture review, layer violations, transaction design, concurrency control, or test coverage requirements, continue with `$taboolib-bukkit-plugin-architecture-standards`.

## Version Baseline

**Default Versions** (fallback only - always check user's project first):
- Minecraft/Paper: 1.20.1
- Java: 17
- Kotlin: Read from user's `build.gradle.kts` (typically 1.9.x - 2.1.x)
- Gradle: 8.x
- TabooLib: **MUST read from user's project** - never assume version

**Critical Rule**: If user's project code conflicts with this skill's examples, **user's project wins**. Adapt to their conventions.

**nmsProxy tested versions**: 1.8.8, 1.12.2, 1.16.5, 1.17.1, 1.19.4, 1.20.4, 1.20.5+, 1.21+

## Evidence Policy

Follow strict evidence hierarchy:

1. **User's existing project code** (highest priority)
   - Read actual imports, package names, TabooLib version
   - Match existing code style and patterns
   - Never override user's working conventions

2. **TabooLib source code** (optional - only if path is configured)
   - Package: `<TABOOLIB_SOURCE_DIR>` (placeholder)
   - **Resolution rule**: Before using, check the user's project root for `AGENTS.md` or `CLAUDE.md` and look for a "本地资源路径" / "Local Resource Paths" section declaring the actual path
   - If a path is declared → use it to verify API existence, method signatures, parameters
   - If no path is declared → **skip this layer entirely**, do not ask the user, fall through to layer 4 (skill references)
   - Never invent or guess a local path

3. **TabooLib wiki** (optional - only if path is configured)
   - Location: `<TABOOLIB_WIKI_DIR>` (placeholder)
   - **Resolution rule**: Same as layer 2 — only consult when `AGENTS.md` / `CLAUDE.md` declares the path
   - If declared → cross-reference with source code when conflicts arise; source code wins over wiki claims
   - If not declared → skip silently and use skill cards instead

4. **This skill's references** (always available)
   - Cards in `cards/`, recipes in `recipes/`, examples in `examples/`, API maps in `api-map/`
   - Use as the working baseline when layers 2/3 are unavailable
   - Still verify against the user's project (layer 1)

5. **Web search** (last resort only)
   - Only when layers 1-4 cannot answer and the question is version-sensitive
   - Always mark as "unverified" and suggest user confirmation

### Local Path Detection (executed once per session)

Before invoking layer 2 or 3, run this check:

1. Look for `AGENTS.md` or `CLAUDE.md` in the user's project root
2. Search for a heading exactly named `TabooLib 本地资源路径`
3. Extract values for the keys `TabooLib 源码` and `TabooLib Wiki`
4. Cache the result for the rest of the session:
   - Found → enable layers 2 / 3 with the resolved paths
   - Not found → disable layers 2 / 3, work from skill cards only, do **not** prompt the user

Example declaration the user may put in `AGENTS.md`:

```markdown
## TabooLib 本地资源路径
- TabooLib 源码: D:\path\to\taboolib
- TabooLib Wiki: D:\path\to\taboowiki
```

## Task Routing Rules

When user mentions these keywords, load corresponding cards and recipes:

| Keywords | Load Cards | Load Recipes |
|----------|-----------|--------------|
| command, 命令, GM指令, debug指令 | `03_command.md` | `create_command.md` |
| config, 配置, reload, yaml | `04_config.md` | `create_config_registry.md` |
| IOC, Inject, Component, Bean, 注入失败 | `05_ioc.md` | `create_ioc_service.md`, `fix_ioc_injection_error.md` |
| listener, event, 事件监听 | `06_listener.md` | `create_listener.md` |
| scheduler, async, 延迟, 定时任务 | `07_scheduler.md` | `create_scheduler_task.md` |
| database, EasyQuery, repository, DAO, 数据库 | `08_database.md` | `create_database_module.md` |
| enable, disable, Awake, 生命周期 | `02_lifecycle.md` | `fix_lifecycle_error.md` |
| error, 报错, 启动失败, No bean found, ClassNotFound | `10_troubleshooting.md` | `fix_ioc_injection_error.md`, `fix_lifecycle_error.md` |
| message, i18n, 多语言, Lang | `09_message_i18n.md` | - |
| menu, GUI, chest, 箱子菜单, 界面, virtualize | `11_chest_menu.md` | - |
| 项目搭建, Gradle, 依赖 | `01_project_setup.md` | - |
| nmsProxy, NMS, 跨版本, ASM转译, NMS代理 | `12_nms_proxy.md` | `create_nms_proxy.md` |
| Incision, 字节码织入, bytecode, @Surgeon, @Lead, @Trail, @Splice, ASM Weaving | `13_incision.md` | `create_incision_advice.md` |

**Multi-keyword tasks**: Load all relevant cards, prioritize by task type (setup → lifecycle → implementation → troubleshooting).

## Workflow

### Step 1: Detect Task Type
Identify user's intent from keywords and context:
- **Creation task**: "create", "add", "implement", "新增"
- **Fix task**: "fix", "error", "报错", "修复"
- **Review task**: "check", "review", "审查"
- **Explanation task**: "how", "what", "explain", "如何"

### Step 2: Load Relevant Materials
Based on task routing rules:
1. Read relevant card(s) from `cards/`
2. Read relevant recipe(s) from `recipes/`
3. Check relevant example(s) from `examples/`
4. Reference relevant API map(s) from `api-map/`

### Step 3: Inspect User's Project
**MANDATORY before generating any code**:
1. Read user's `build.gradle.kts` for TabooLib version and Kotlin version
2. Find existing TabooLib imports to verify package paths
3. Identify project package name and module structure
4. Check existing code style (naming, formatting, patterns)

### Step 4: Verify APIs
Before using any TabooLib API:
1. Check if it exists in user's project (grep for imports)
2. If not found, check `api-map/` for package path
3. If still uncertain **and** local source path is configured (see Evidence Policy / Local Path Detection), grep the configured `<TABOOLIB_SOURCE_DIR>` for the API
4. If local path not configured → rely on `cards/` and `api-map/` content; mark borderline cases as "unverified - please confirm against your TabooLib version"
5. **Never fabricate APIs** - use conservative fallbacks or ask user

### Step 5: Generate Minimal Code
Follow these rules:
1. **Match user's style**: Use their package names, import patterns, formatting
2. **Minimal scope**: Only code directly addressing the requirement
3. **Conservative APIs**: Use only verified APIs from Steps 3-4
4. **Include verification**: Provide logs/commands to test the code
5. **Document assumptions**: Note any unverified APIs or version dependencies

### Step 6: Provide Verification Steps
Every code generation must include:
1. **Compilation check**: Expected build output
2. **Runtime check**: Expected logs or behavior
3. **Test method**: Commands or actions to verify functionality
4. **Troubleshooting**: Common issues and diagnostic steps

## Output Rules

### Code Generation Format
```markdown
## [Task Description]

### File: `path/to/File.kt`
**Purpose**: [What this code does]

**Code**:
```kotlin
// [Inline comments explaining TabooLib-specific parts]
package [user's actual package]

import [verified imports from user's project or api-map]

// [Minimal implementation]
```

**Integration**:
- [How to wire this into existing code]
- [Dependencies or prerequisites]

**Verification**:
1. Compile: `./gradlew build` → expect success
2. Runtime: Check logs for `[Expected log message]`
3. Test: Run `/command` → expect `[Expected behavior]`

**Troubleshooting**:
- If [error X], check [diagnostic Y]
```

### Fix/Troubleshooting Format
```markdown
## [Error Description]

**Symptom**: [What user sees]

**Possible Causes**:
1. [Most likely cause]
2. [Second likely cause]
3. [Less common cause]

**Diagnostic Steps**:
1. Check [file/log/config]
2. Verify [condition]
3. Test [hypothesis]

**Minimal Fix**:
[Smallest change to resolve issue]

**Verification**:
[How to confirm fix worked]
```

## Anti-Patterns (NEVER DO THIS)

### 1. API Fabrication
❌ **WRONG**: Inventing TabooLib APIs without verification
```kotlin
// This API doesn't exist!
@TabooLibService  // ← Fabricated annotation
class MyService
```

✅ **CORRECT**: Use verified APIs or mark as unverified
```kotlin
// Verified from user's project imports
@Component  // ← From taboolib.expansion.ioc.annotation
class MyService
```

### 2. Data Class as IOC Bean
❌ **WRONG**: Treating plain data classes as injectable beans
```kotlin
data class PlayerData(val name: String)  // ← Not a Bean!

@Component
class MyService {
    @Resource
    lateinit var playerData: PlayerData  // ← Injection will fail!
}
```

✅ **CORRECT**: Only inject actual @Component beans
```kotlin
data class PlayerData(val name: String)  // ← Plain data class

@Component
class PlayerDataService {  // ← This is the Bean
    fun getData(name: String): PlayerData = PlayerData(name)
}

@Component
class MyService {
    @Resource
    lateinit var playerDataService: PlayerDataService  // ← Inject the service
}
```

### 3. Main Thread IO
❌ **WRONG**: Database/network operations in command/listener
```kotlin
@CommandBody
val reload = subCommand {
    execute<ProxyCommandSender> { sender, _, _ ->
        database.query("SELECT * FROM players")  // ← Blocks main thread!
        sender.sendMessage("Done")
    }
}
```

✅ **CORRECT**: Use async scheduler for IO
```kotlin
@CommandBody
val reload = subCommand {
    execute<ProxyCommandSender> { sender, _, _ ->
        submit(async = true) {
            val result = database.query("SELECT * FROM players")
            submit(async = false) {  // ← Back to main thread for Bukkit API
                sender.sendMessage("Done: ${result.size} players")
            }
        }
    }
}
```

### 4. Business Logic in Command/Listener
❌ **WRONG**: Complex logic directly in command handler
```kotlin
@CommandBody
val transfer = subCommand {
    dynamic("from") {
        dynamic("to") {
            int("amount") {
                execute<ProxyCommandSender> { sender, ctx, _ ->
                    // 50 lines of business logic here ← Wrong layer!
                }
            }
        }
    }
}
```

✅ **CORRECT**: Delegate to service layer
```kotlin
@Component
class TransferService {
    fun transfer(from: String, to: String, amount: Int): Result<Unit> {
        // Business logic here
    }
}

@CommandBody
val transfer = subCommand {
    dynamic("from") {
        dynamic("to") {
            int("amount") {
                execute<ProxyCommandSender> { sender, ctx, _ ->
                    val result = transferService.transfer(
                        ctx.argument(0), ctx.argument(1), ctx.argument(2)
                    )
                    sender.sendMessage(result.message)
                }
            }
        }
    }
}
```

### 5. Premature Lifecycle Access
❌ **WRONG**: Accessing config/database before initialization
```kotlin
@Component
class MyService {
    init {
        val value = config["key"]  // ← Config not loaded yet!
    }
}
```

✅ **CORRECT**: Use @PostConstruct or @Awake(LifeCycle.ENABLE)
```kotlin
@Component
class MyService {
    @PostConstruct
    fun init() {
        val value = config["key"]  // ← Safe: called after dependencies injected
    }
}
```

### 6. Mixed Schedulers Without Reason
❌ **WRONG**: Using multiple scheduler APIs inconsistently
```kotlin
submit(async = true) { /* ... */ }
Bukkit.getScheduler().runTaskAsynchronously(plugin) { /* ... */ }
submitAsync { /* ... */ }
```

✅ **CORRECT**: Use TabooLib scheduler consistently
```kotlin
submit(async = true) { /* ... */ }
submit(async = true, delay = 20) { /* ... */ }
submitAsync { /* ... */ }  // ← Equivalent to submit(async = true)
```

### 7. Fake API Examples
❌ **WRONG**: Writing code that won't compile
```kotlin
// These methods don't exist in TabooLib!
TabooLib.getConfigManager().reload()
PlayerManager.getInstance().getPlayer(uuid)
```

✅ **CORRECT**: Use only verified APIs
```kotlin
// Verified from source code analysis
config.reload()  // ← Configuration.reload()
Bukkit.getPlayer(uuid)  // ← Standard Bukkit API
```

## Integration with Other Skills

When task involves these areas, continue with specialized skills:

- **Database persistence**: Use `$taboolib-corelib-easyquery-persistence-standards` for entity modeling, repository patterns, Code First schema
- **Architecture review**: Use `$taboolib-bukkit-plugin-architecture-standards` for layering, transactions, locking, testing
- **CoreBridge packets**: Use `$taboolib-corebridge-packet-standards` for packet handlers, registration, placeholder sync
- **Attribute hooks**: Use `$bukkit-attribute-plugin-hook-standards` for AttributeSource integration

## Quick Reference

### Essential Files
- `cards/00_index.md` - Card navigation index
- `api-map/annotation_map.md` - All TabooLib annotations with package paths
- `api-map/package_map.md` - Common package structure
- `tests/compile_checklist.md` - Pre-generation verification checklist
- `tests/troubleshooting_checklist.md` - Error diagnosis checklist

### Common Packages (Verify in User's Project)
```
taboolib.common.platform          - @Awake, LifeCycle
taboolib.common.platform.command  - @CommandHeader, @CommandBody
taboolib.common.platform.event    - @SubscribeEvent
taboolib.common.platform.function - submit, submitAsync
taboolib.module.configuration     - @Config, Configuration
taboolib.module.lang              - sendLang, asLangText
taboolib.expansion.ioc.annotation - @Component, @Resource, @PostConstruct
taboolib.module.nms                 - nmsProxy, nmsProxyClass, Packet, PacketImpl, PacketSendEvent, MinecraftVersion
taboolib.module.nms.remap           - require (bytecode-level class check)
taboolib.module.incision            - IncisionBootstrap entry point
taboolib.module.incision.annotation - @Surgeon, @Lead, @Trail, @Splice, @Bypass, @Graft, @Trim, @Excise, @Operation, @Version, @KotlinTarget
taboolib.module.incision.api        - Theatre, Resume, Suture, Anchor, Shift, Anatomy, IncisionAccessor
taboolib.module.incision.dsl        - Scalpel, ScopedHandle, SutureImpl
```

### Lifecycle Order
```
CONST → INIT → LOAD → ENABLE → ACTIVE → DISABLE
  ↑       ↑      ↑       ↑        ↑        ↑
Static  Plugin  Plugin  Plugin  Scheduler Plugin
blocks  class   load()  enable() started  disable()
```

### IOC Injection Types
```
Constructor > Field > Method
   ↑           ↑        ↑
Preferred  @Resource  @Inject
```

## Deliverables

When completing a task:

1. **For creation tasks**: Provide file path, code, integration steps, verification steps
2. **For fix tasks**: Provide symptom, causes, diagnostic steps, minimal fix, verification
3. **For review tasks**: Provide findings, risks, recommendations, references to architecture skills
4. **For explanation tasks**: Provide concept, usage pattern, example, common mistakes, checklist

Always include:
- Source of API information (user's project / source code / wiki / unverified)
- Version notes if API is version-sensitive
- Verification method (compile check / runtime log / test command)
- Troubleshooting hints for common issues

## Final Reminder

**Priority #1**: Reduce API hallucination
- Check user's project first
- Verify against source code second
- Never guess APIs third

**Priority #2**: Generate minimal, compilable code
- Match user's existing style
- Use only verified APIs
- Provide verification steps

**Priority #3**: Enable progressive learning
- Load only relevant cards/recipes
- Provide quick-reference maps
- Include troubleshooting checklists
