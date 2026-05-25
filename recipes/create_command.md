# Recipe: Create Command

## Use When

- User wants to add a new command to the plugin
- Need to implement command with subcommands and arguments
- Implementing admin commands or player commands

## Required Checks

Before creating command:

- [ ] Read user's existing command files to match style
- [ ] Verify command name doesn't conflict with existing commands
- [ ] Check user's TabooLib version for API compatibility
- [ ] Confirm package structure (where to place command file)
- [ ] Determine if IoC service injection is needed

## Implementation Steps

### Step 1: Inspect Existing Commands

```bash
# Find existing command files
grep -r "@CommandHeader" src/
```

**Extract**:
- Package naming convention
- Import style (wildcard vs explicit)
- Service injection pattern (if using IoC)
- Command structure (simple vs complex)

### Step 2: Determine Command Structure

**Simple command** (no subcommands):
- Main command only: `/example`
- Use `mainCommand` block

**Complex command** (with subcommands):
- Multiple subcommands: `/example reload`, `/example help`
- Use `@CommandBody` fields with `subCommand` blocks

**With arguments**:
- Dynamic arguments: `dynamic("arg")`
- Player arguments: `player("target")`
- Typed arguments: `int("amount")`, `double("value")`

### Step 3: Create Command File

**File location**: `src/main/kotlin/<package>/command/<CommandName>Command.kt`

**Template**:
```kotlin
package com.example.plugin.command  // ← Use user's actual package

import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer

@CommandHeader(
    name = "example",
    aliases = ["ex"],
    description = "Example command",
    permission = "example.use",
    permissionDefault = PermissionDefault.OP
)
object ExampleCommand {
    
    // Subcommand: /example reload
    @CommandBody(permission = "example.admin")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            // Delegate to service
            configService.reload()
            sender.sendMessage("§aReloaded!")
        }
    }
    
    // Subcommand with arguments: /example give <player> <amount>
    @CommandBody
    val give = subCommand {
        player("target") {  // ← Auto player name completion
            int("amount") {
                execute<ProxyCommandSender> { sender, context, argument ->
                    val target = context.player(0)
                    val amount = context.argument(1).toInt()
                    
                    // Delegate to service
                    economyService.give(target.uniqueId, amount)
                    sender.sendMessage("§aGave $amount to ${target.name}")
                }
            }
        }
    }
    
    // Main command: /example (no subcommand)
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§e=== Example Commands ===")
            sender.sendMessage("§a/example reload - Reload config")
            sender.sendMessage("§a/example give <player> <amount> - Give coins")
        }
    }
}
```

### Step 4: Add Service Injection (if using IoC)

```kotlin
import taboolib.expansion.ioc.annotation.Component
import taboolib.expansion.ioc.annotation.Resource

@CommandHeader(name = "example")
object ExampleCommand {
    
    @Resource
    lateinit var configService: ConfigService
    
    @Resource
    lateinit var economyService: EconomyService
    
    // Command bodies...
}
```

### Step 5: Handle Async Operations (if needed)

```kotlin
import taboolib.common.platform.function.submit

@CommandBody
val reload = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        sender.sendMessage("§eReloading...")
        
        submit(async = true) {
            // Async IO operation
            database.reload()
            
            submit(async = false) {
                sender.sendMessage("§aReloaded!")
            }
        }
    }
}
```

## Code Generation Rules

1. **Match user's package structure**: Use their actual package, not `com.example`
2. **Match import style**: Wildcard if they use wildcard, explicit if they use explicit
3. **Match naming convention**: PascalCase for object name, camelCase for subcommands
4. **Delegate to services**: Don't put business logic in command handler
5. **Use async for IO**: Database/network operations must be async
6. **Provide tab completion**: Use `player()` or `dynamic()` with `suggestion`
7. **Handle permissions**: Add permission checks for admin commands
8. **Player-only commands**: Check if sender is player before casting

## Recommended File Structure

```
src/main/kotlin/<package>/
├── command/
│   ├── ExampleCommand.kt      # Main command
│   ├── AdminCommand.kt         # Admin commands
│   └── PlayerCommand.kt        # Player-only commands
├── service/
│   ├── ConfigService.kt        # Config management
│   └── EconomyService.kt       # Business logic
└── ExamplePlugin.kt            # Plugin main class
```

## Verification Steps

### Compile Check
```bash
./gradlew build
```
**Expected**: Build succeeds without errors

### Runtime Check
1. Start server with plugin
2. Check logs for command registration:
   ```
   [TabooLib] Registered command: example
   ```

### Functional Test
1. Execute command: `/example`
   - **Expected**: Shows help message
2. Test subcommand: `/example reload`
   - **Expected**: Executes reload, shows success message
3. Test tab completion: `/example give <TAB>`
   - **Expected**: Shows online player names
4. Test permissions: Execute as non-op player
   - **Expected**: Shows permission denied message

### Edge Case Test
1. Invalid arguments: `/example give invalid 100`
   - **Expected**: Shows usage or error message
2. Console execution (player-only command): Execute from console
   - **Expected**: Shows "player-only" message
3. Missing permission: Execute without permission
   - **Expected**: Shows permission message

## Common Issues

**Command not registered**:
- Check `@CommandHeader` annotation present
- Verify class is in scanned package
- Check logs for registration errors

**Tab completion not working**:
- Use `player()` or `dynamic()` with `suggestion`
- Don't use plain `dynamic()` without suggestion

**Permission denied for all**:
- Check `permissionDefault` value
- Verify permission node is correct

## Example Output

**File**: `src/main/kotlin/com/example/plugin/command/EconomyCommand.kt`

```kotlin
package com.example.plugin.command

import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.expansion.ioc.annotation.Resource

@CommandHeader(
    name = "economy",
    aliases = ["eco", "money"],
    description = "Economy commands",
    permission = "example.economy"
)
object EconomyCommand {
    
    @Resource
    lateinit var economyService: EconomyService
    
    @CommandBody
    val balance = subCommand {
        execute<ProxyPlayer> { player, _, _ ->
            val balance = economyService.getBalance(player.uniqueId)
            player.sendMessage("§aYour balance: §e$balance §acoins")
        }
    }
    
    @CommandBody(permission = "example.economy.admin")
    val give = subCommand {
        player("target") {
            int("amount") {
                execute<ProxyCommandSender> { sender, context, _ ->
                    val target = context.player(0)
                    val amount = context.argument(1).toInt()
                    
                    economyService.give(target.uniqueId, amount)
                    sender.sendMessage("§aGave $amount coins to ${target.name}")
                }
            }
        }
    }
    
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("§e=== Economy Commands ===")
            sender.sendMessage("§a/economy balance - Check balance")
            sender.sendMessage("§a/economy give <player> <amount> - Give coins")
        }
    }
}
```

## References

- Card: `03_command.md`
- Related recipes: `create_ioc_service.md` (service delegation)
