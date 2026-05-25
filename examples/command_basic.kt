// Replace com.example.plugin with your project's actual package
package com.example.plugin.command

import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer

/**
 * Basic command example with subcommands
 *
 * Commands:
 * - /example help - Show help message
 * - /example reload - Reload configuration
 * - /example give <player> <amount> - Give coins to player
 */
@CommandHeader(
    name = "example",
    aliases = ["ex", "exm"],
    description = "Example plugin commands",
    permission = "example.use",
    permissionDefault = PermissionDefault.OP
)
object ExampleCommand {

    // Subcommand: /example help
    @CommandBody
    val help = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("§e=== Example Commands ===")
            sender.sendMessage("§a/example help - Show this help")
            sender.sendMessage("§a/example reload - Reload config")
            sender.sendMessage("§a/example give <player> <amount> - Give coins")
        }
    }

    // Subcommand: /example reload (admin only)
    @CommandBody(permission = "example.admin")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // Delegate to config service
            // configService.reload()
            sender.sendMessage("§aConfiguration reloaded!")
        }
    }

    // Subcommand with arguments: /example give <player> <amount>
    @CommandBody
    val give = subCommand {
        // player() provides auto tab completion with online player names
        player("target") {
            // int() validates argument is integer
            int("amount") {
                execute<ProxyCommandSender> { sender, context, _ ->
                    val target = context.player(0)  // Get player argument
                    val amount = context.argument(1).toInt()  // Get int argument

                    // Delegate to service layer
                    // economyService.give(target.uniqueId, amount)

                    sender.sendMessage("§aGave $amount coins to ${target.name}")
                }
            }
        }
    }

    // Main command: /example (no subcommand)
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("§eUse /example help for command list")
        }
    }
}
