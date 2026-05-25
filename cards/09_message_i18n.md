# Card 09: Message & i18n System

## When to Use

Load this card when:
- User asks about "message", "i18n", "多语言", "Lang", "sendLang"
- Need to implement multi-language support
- Sending localized messages to players
- Troubleshooting message not found or placeholder issues

## Core Idea

TabooLib provides `Lang` system for multi-language support with flat YAML structure, placeholder replacement, and color code support. Messages are stored in `lang/` folder with language code files (zh_CN.yml, en_US.yml). **Use flat structure only** - nested keys not supported.

## Recommended Pattern

### Language File Structure

**File: `src/main/resources/lang/zh_CN.yml`**
```yaml
# Flat structure only - no nesting!
plugin-prefix: "§7[§6MyPlugin§7]"
command-reload-success: "§a配置重载成功！"
command-no-permission: "§c你没有权限执行此命令"
player-join-message: "§e欢迎 {0} 加入服务器！"
balance-display: "§a你的余额: §e{0} §a金币"
```

**File: `src/main/resources/lang/en_US.yml`**
```yaml
plugin-prefix: "§7[§6MyPlugin§7]"
command-reload-success: "§aConfig reloaded successfully!"
command-no-permission: "§cYou don't have permission"
player-join-message: "§eWelcome {0} to the server!"
balance-display: "§aYour balance: §e{0} §acoins"
```

### Sending Messages

**Package**: `taboolib.module.lang`

```kotlin
import taboolib.module.lang.sendLang
import taboolib.module.lang.asLangText

// Send message to player
player.sendLang("player-join-message", player.name)

// Get message as string
val message = player.asLangText("balance-display", balance)

// Send to console
console.sendLang("command-reload-success")
```

### Placeholder Replacement

```kotlin
// Single placeholder: {0}
player.sendLang("player-join-message", "Steve")
// Result: "Welcome Steve to the server!"

// Multiple placeholders: {0}, {1}, {2}
player.sendLang("transfer-success", "Steve", "Alex", 100)
// Result: "Transferred 100 coins from Steve to Alex"
```

### Color Codes

**Supported formats**:
- `§` codes: `§a`, `§c`, `§e`, etc.
- `&` codes: `&a`, `&c`, `&e`, etc. (auto-converted)
- Hex colors: `&#RRGGBB` (1.16+)

```yaml
colored-message: "§aGreen §cRed §eYellow"
hex-message: "&#FF5733This is orange text"
```

### Language Registration

```kotlin
import taboolib.module.lang.Language

@Awake(LifeCycle.ENABLE)
fun registerLanguages() {
    // Register supported languages
    Language.default = "zh_CN"  // Default language
}
```

## Common Mistakes

### ❌ Mistake 1: Using nested structure
```yaml
# WRONG - nested structure not supported!
messages:
  player:
    join: "Welcome!"
    quit: "Goodbye!"
```
**Why wrong**: Lang system only supports flat structure.

**Fix**: Use flat keys
```yaml
# CORRECT - flat structure
message-player-join: "Welcome!"
message-player-quit: "Goodbye!"
```

### ❌ Mistake 2: Wrong placeholder syntax
```kotlin
// WRONG placeholders
player.sendLang("message", "%player%")  // ← Not {0}
player.sendLang("message", "${player}")  // ← Not {0}
```
**Why wrong**: Lang system uses {0}, {1}, {2} syntax, not other formats.

**Fix**: Use correct syntax
```kotlin
player.sendLang("message", playerName)  // ← Replaces {0}
```

### ❌ Mistake 3: Missing language file
```kotlin
player.sendLang("missing-key")  // ← Key not in lang file
// Result: "missing-key" (raw key returned)
```
**Why wrong**: Missing keys return the key itself, not a user-friendly message.

**Fix**: Add key to language files
```yaml
missing-key: "This message was missing!"
```

## Minimal Example

**Complete i18n setup**:

```kotlin
// lang/zh_CN.yml
plugin-prefix: "§7[§6插件§7]"
command-help: "§a/example help - 显示帮助"
player-balance: "§a余额: §e{0} §a金币"

// lang/en_US.yml
plugin-prefix: "§7[§6Plugin§7]"
command-help: "§a/example help - Show help"
player-balance: "§aBalance: §e{0} §acoins"

// Code
import taboolib.module.lang.sendLang

@CommandBody
val balance = subCommand {
    execute<ProxyPlayer> { player, _, _ ->
        val balance = playerService.getBalance(player.uniqueId)
        player.sendLang("player-balance", balance)
    }
}
```

## Checklist

- [ ] Language files in `src/main/resources/lang/`
- [ ] Use flat structure (no nesting)
- [ ] All keys have translations in all language files
- [ ] Placeholders use {0}, {1}, {2} syntax
- [ ] Color codes use § or & prefix
- [ ] Default language set if needed

## References

- Source code: `taboolib/module/minecraft/minecraft-i18n/`
- Related cards: `03_command.md` (command messages)
