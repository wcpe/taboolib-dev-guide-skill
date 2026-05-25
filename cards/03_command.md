# Card 03: Command System

## When to Use

Load this card when:
- User asks about "command", "命令", "@CommandHeader", "@CommandBody"
- Need to create player/console commands
- Implementing subcommands or argument parsing
- Troubleshooting command registration or tab completion issues

## Core Idea

TabooLib provides annotation-based command system using `@CommandHeader` for main command definition and `@CommandBody` for subcommand structure. Commands support dynamic arguments with tab completion, permission checks, and option parsing. **Tab completion only works for `dynamic()` and `player()` types, not for `option()` values**.

## ⚠️ Critical Architecture Rules

### 🚫 命令中禁止业务逻辑处理

**命令处理器运行在主线程，任何耗时或复杂逻辑都会阻塞服务器。**

**命令中禁止的操作**:
- ❌ 数据库查询/写入（SQL、ORM、EasyQuery、JDBC）
- ❌ Redis 读写
- ❌ HTTP/网络请求
- ❌ 文件 IO（大文件读写）
- ❌ 阻塞等待（`Thread.sleep()`、`Future.get()`、`CountDownLatch.await()`）
- ❌ 重计算（大量数据遍历、批量序列化、复杂聚合）
- ❌ 业务逻辑处理（50+ 行业务代码）

**命令只能做**:
- ✅ 参数解析和校验
- ✅ 权限校验
- ✅ 立即委托到 service 层（通过 `submitAsync`）
- ✅ 轻量的消息发送

**正确模式**:
```kotlin
@CommandBody
val transfer = subCommand {
    dynamic("target") {
        int("amount") {
            execute<ProxyPlayer> { sender, context, _ ->
                val target = context.argument(0)
                val amount = context.argument(1).toInt()
                
                // ✅ 立即异步委托到 service
                submitAsync {
                    val result = transferService.transfer(sender.name, target, amount)
                    submit {
                        sender.sendMessage(if (result.success) "转账成功" else "转账失败")
                    }
                }
            }
        }
    }
}
```

**错误模式**:
```kotlin
@CommandBody
val transfer = subCommand {
    execute<ProxyPlayer> { sender, context, _ ->
        // ❌ 主线程直接查询数据库 - 阻塞服务器！
        val balance = repository.getBalance(sender.name)
        if (balance < amount) {
            sender.sendMessage("余额不足")
            return@execute
        }
        // ❌ 主线程直接写入数据库 - 阻塞服务器！
        repository.deduct(sender.name, amount)
        repository.add(target, amount)
        sender.sendMessage("转账成功")
    }
}
```

### Permission Message Rules

**For Paper servers (high version)**: Root commands should declare explicit plain-text `permissionMessage` on `@CommandHeader`:
```kotlin
@CommandHeader(
    name = "example",
    permissionMessage = "你没有权限执行此命令"  // ✅ Plain text, no color codes
)
```

**Why**: Paper's Adventure text path can trigger `LegacyFormattingDetected` warnings if you rely on TabooLib's default fallback.

**禁止**: `permissionMessage` must not contain legacy color codes (`§c`, `&c`).

### Version-Specific API Rules

**TabooLib < 6.3.0 (except WCPE version)**:
- Main command must use `createHelper()`:
  ```kotlin
  @CommandBody
  val main = mainCommand {
      createHelper()  // ✅ Use createHelper() for TabooLib < 6.3.0
  }
  ```

**TabooLib >= 6.3.0**:
- Main command must use `createDescriptionHelper()`:
  ```kotlin
  @CommandBody
  val main = mainCommand {
      createDescriptionHelper()  // ✅ Use createDescriptionHelper() for TabooLib >= 6.3.0
  }
  ```

**WCPE TabooLib 6.2.4-SNAPSHOT**:

If your project uses:
```kotlin
repoTabooLib = "https://maven.wcpe.top/repository/maven-public/"
taboolib = "6.2.4-wcpe-SNAPSHOT"
```

**Additional requirements**:
1. Every `@CommandBody` must declare both `permission` and `description`
2. All `description` values must use `@` prefix for i18n keys (e.g. `"@command-desc-reload"`)
3. All `dynamic` comment arguments must use `private const val ARG_XXX = "@command-arg-xxx"` constants
4. Main command entry must use `createDescriptionHelper()` (WCPE version uses 6.3.0+ API)
5. Language file keys must use `-` separator with prefixes `command-desc-` or `command-arg-`

## Recommended Pattern

### Basic Command Structure

**Packages**:
- `taboolib.common.platform.command.CommandHeader`
- `taboolib.common.platform.command.CommandBody`
- `taboolib.common.platform.command.subCommand`
- `taboolib.common.platform.command.CommandContext`

```kotlin
import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer

@CommandHeader(
    name = "example",
    aliases = ["ex", "exm"],
    description = "Example command",
    usage = "/example <subcommand>",
    permission = "example.use",
    permissionMessage = "You don't have permission",
    permissionDefault = PermissionDefault.OP
)
object ExampleCommand {
    
    // Subcommand: /example reload
    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("Reloaded!")
        }
    }
    
    // Subcommand with argument: /example give <player> <item>
    @CommandBody
    val give = subCommand {
        dynamic("player") {  // ← Has tab completion
            dynamic("item") {  // ← Has tab completion
                execute<ProxyCommandSender> { sender, context, argument ->
                    val playerName = context.argument(0)  // First dynamic arg
                    val itemName = context.argument(1)    // Second dynamic arg
                    sender.sendMessage("Gave $itemName to $playerName")
                }
            }
        }
    }
    
    // Main command (no subcommand): /example
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("Example command help")
        }
    }
}
```

### Argument Types

**Dynamic Arguments** (with tab completion):
```kotlin
@CommandBody
val test = subCommand {
    dynamic("arg1") {  // ← Tab completion from suggestion
        suggestion<ProxyCommandSender> { sender, context ->
            listOf("option1", "option2", "option3")
        }
        execute<ProxyCommandSender> { sender, context, argument ->
            val value = context.argument(0)
            sender.sendMessage("You chose: $value")
        }
    }
}
```

**Player Arguments** (with player name completion):
```kotlin
@CommandBody
val kick = subCommand {
    player("target") {  // ← Auto tab completion with online player names
        execute<ProxyCommandSender> { sender, context, argument ->
            val player = context.player(0)  // Returns ProxyPlayer
            player.kick("Kicked by command")
        }
    }
}
```

**Integer Arguments** (with validation):
```kotlin
@CommandBody
val setLevel = subCommand {
    int("level") {  // ← Only accepts integers
        execute<ProxyCommandSender> { sender, context, argument ->
            val level = context.argument(0).toInt()
            sender.sendMessage("Level set to $level")
        }
    }
}
```

**Optional Arguments**:
```kotlin
@CommandBody
val teleport = subCommand {
    dynamic("player") {
        dynamic("world", optional = true) {  // ← Optional argument
            execute<ProxyCommandSender> { sender, context, argument ->
                val player = context.argument(0)
                val world = context.argumentOrNull(1) ?: "world"  // Default if not provided
                sender.sendMessage("Teleporting $player to $world")
            }
        }
    }
}
```

### Permission Checks

**Command-level permission**:
```kotlin
@CommandHeader(
    name = "admin",
    permission = "example.admin",  // ← Required for all subcommands
    permissionDefault = PermissionDefault.OP
)
object AdminCommand { }
```

**Subcommand-level permission**:
```kotlin
@CommandBody(permission = "example.reload")  // ← Additional permission
val reload = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        sender.sendMessage("Reloaded!")
    }
}
```

**Runtime permission check**:
```kotlin
execute<ProxyCommandSender> { sender, context, argument ->
    if (!sender.hasPermission("example.special")) {
        sender.sendMessage("No permission!")
        return@execute
    }
    // Execute command
}
```

### Context API

**Package**: `taboolib.common.platform.command.CommandContext`

```kotlin
execute<ProxyCommandSender> { sender, context, argument ->
    // Get sender
    val sender = context.sender<ProxyCommandSender>()
    val player = context.sender<ProxyPlayer>()  // Throws if not player
    
    // Get arguments by index
    val arg0 = context.argument(0)  // First argument (String)
    val arg1 = context.argument(1)  // Second argument (String)
    val argOrNull = context.argumentOrNull(2)  // Returns null if not provided
    
    // Get player argument
    val targetPlayer = context.player(0)  // Returns ProxyPlayer
    
    // Get all remaining arguments
    val allArgs = context.args()  // Array<String> from current position
    
    // Check permission
    if (context.checkPermission("example.admin", true)) {  // true = send message if no permission
        // Has permission
    }
}
```

### New Parser (Options Support)

**Enable new parser**:
```kotlin
@CommandHeader(
    name = "example",
    newParser = true  // ← Enable option parsing
)
object ExampleCommand {
    
    @CommandBody
    val test = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            // Parse: /example test -v --debug arg1 arg2
            
            // Check if option exists
            if (context.hasOption("v")) {
                sender.sendMessage("Verbose mode")
            }
            
            // Get option value
            val debugLevel = context.option("debug", "d")  // Try --debug or -d
            
            // Get all options
            val options = context.options()  // Map<String, String>
            
            // Get positional arguments (non-option args)
            val args = context.args()  // ["arg1", "arg2"]
        }
    }
}
```

## Common Mistakes

### ❌ Mistake 1: Expecting tab completion for options
```kotlin
@CommandHeader(name = "test", newParser = true)
object TestCommand {
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            val mode = context.option("mode")  // ← No tab completion for option values!
        }
    }
}
```
**Why wrong**: Option values don't get tab completion, only `dynamic()` and `player()` nodes do.

**Fix**: Use `dynamic()` if tab completion needed
```kotlin
@CommandBody
val main = subCommand {
    dynamic("mode") {  // ← Has tab completion
        suggestion<ProxyCommandSender> { sender, context ->
            listOf("easy", "normal", "hard")
        }
        execute<ProxyCommandSender> { sender, context, argument ->
            val mode = context.argument(0)
        }
    }
}
```

### ❌ Mistake 2: Using `dynamic("player")` instead of `player()`
```kotlin
@CommandBody
val kick = subCommand {
    dynamic("player") {  // ← No auto player name completion!
        execute<ProxyCommandSender> { sender, context, argument ->
            val playerName = context.argument(0)  // Returns String, not ProxyPlayer
        }
    }
}
```
**Why wrong**: `dynamic()` doesn't provide player name completion or ProxyPlayer conversion.

**Fix**: Use `player()` helper
```kotlin
@CommandBody
val kick = subCommand {
    player("target") {  // ← Auto player name completion
        execute<ProxyCommandSender> { sender, context, argument ->
            val player = context.player(0)  // Returns ProxyPlayer
        }
    }
}
```

### ❌ Mistake 3: Business logic in command handler
```kotlin
@CommandBody
val transfer = subCommand {
    dynamic("from") {
        dynamic("to") {
            int("amount") {
                execute<ProxyCommandSender> { sender, context, argument ->
                    // 50 lines of business logic here ← Wrong layer!
                    val from = context.argument(0)
                    val to = context.argument(1)
                    val amount = context.argument(2).toInt()
                    
                    // Database queries, validation, calculations...
                    // All in command handler ← Anti-pattern!
                }
            }
        }
    }
}
```
**Why wrong**: Command handlers should delegate to service layer, not contain business logic.

**Fix**: Delegate to service
```kotlin
@Component
class TransferService {
    fun transfer(from: String, to: String, amount: Int): Result<String> {
        // Business logic here
    }
}

@CommandBody
val transfer = subCommand {
    dynamic("from") {
        dynamic("to") {
            int("amount") {
                execute<ProxyCommandSender> { sender, context, argument ->
                    val result = transferService.transfer(
                        context.argument(0),
                        context.argument(1),
                        context.argument(2).toInt()
                    )
                    sender.sendMessage(result.message)
                }
            }
        }
    }
}
```

### ❌ Mistake 4: Blocking IO in command handler
```kotlin
@CommandBody
val reload = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        database.query("SELECT * FROM players")  // ← Blocks main thread!
        sender.sendMessage("Reloaded")
    }
}
```
**Why wrong**: Command handlers run on main thread. Database/network IO blocks server.

**Fix**: Use async scheduler
```kotlin
@CommandBody
val reload = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        submit(async = true) {
            val result = database.query("SELECT * FROM players")
            submit(async = false) {  // Back to main thread for messaging
                sender.sendMessage("Reloaded: ${result.size} players")
            }
        }
    }
}
```

### ❌ Mistake 5: Not handling player-only commands
```kotlin
@CommandBody
val heal = subCommand {
    execute<ProxyPlayer> { player, context, argument ->  // ← Requires ProxyPlayer
        player.health = player.maxHealth
    }
}
```
**Why wrong**: If console executes this, it will fail silently or throw exception.

**Fix**: Add explicit check or message
```kotlin
@CommandBody
val heal = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        if (sender !is ProxyPlayer) {
            sender.sendMessage("This command is player-only!")
            return@execute
        }
        sender.health = sender.maxHealth
    }
}
```

## Minimal Example

**Complete command with subcommands**:

```kotlin
package com.example.plugin.command

import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer

@CommandHeader(
    name = "example",
    aliases = ["ex"],
    description = "Example plugin commands",
    permission = "example.use"
)
object ExampleCommand {
    
    // /example help
    @CommandBody
    val help = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("=== Example Commands ===")
            sender.sendMessage("/example help - Show this help")
            sender.sendMessage("/example reload - Reload config")
        }
    }
    
    // /example reload
    @CommandBody(permission = "example.admin")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // Delegate to service
            configService.reload()
            sender.sendMessage("Config reloaded!")
        }
    }
    
    // /example (no subcommand)
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("Use /example help for commands")
        }
    }
}
```

## Checklist

Before creating commands:

- [ ] Verify command name doesn't conflict with existing commands
- [ ] Check if permissions are needed (admin commands, etc.)
- [ ] Determine if tab completion is needed (use `dynamic()` or `player()`)
- [ ] Identify if options are needed (enable `newParser = true`)
- [ ] Check if command is player-only or console-compatible
- [ ] Verify business logic is in service layer, not command handler
- [ ] Confirm no blocking IO in command handler (use async if needed)

After creating commands:

- [ ] Test command execution: `/command` works
- [ ] Test tab completion: Press TAB shows suggestions
- [ ] Test permissions: Non-permitted users see permission message
- [ ] Test player-only commands: Console gets appropriate message
- [ ] Test argument validation: Invalid args show usage
- [ ] Test subcommands: All subcommands work correctly
- [ ] Test main command: No-subcommand case handled

## Version-Specific Notes

**TabooLib 6.2.0+**:
- New parser with option support (`newParser = true`)
- `player()` helper for player arguments
- `int()`, `double()`, `boolean()` helpers for typed arguments

**TabooLib 6.1.x**:
- Basic command system without option parsing
- Manual argument parsing required

## Troubleshooting

**Error: "Command not registered"**
- Cause: Missing `@CommandHeader` or `@CommandBody`
- Fix: Verify annotations present and class is scanned

**Error: "No tab completion"**
- Cause: Using `option()` or plain `dynamic()` without `suggestion`
- Fix: Add `suggestion` block or use `player()` helper

**Error: "Permission denied" for all users**
- Cause: `permissionDefault = PermissionDefault.FALSE`
- Fix: Change to `OP` or `TRUE` or grant permission

**Error: "ClassCastException" when getting player**
- Cause: Console executing player-only command
- Fix: Add player check before casting

## References

- Source code: `taboolib/common-platform-api/src/main/kotlin/taboolib/common/platform/command/`
- Related cards: `05_ioc.md` (service delegation), `07_scheduler.md` (async execution)
- Related recipes: `create_command.md`
