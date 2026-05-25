// Replace com.example.plugin with your project's actual package
package com.example.plugin.config

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * Config registry example with reload support
 *
 * Features:
 * - Auto-reload on file change
 * - Reload callback
 * - Type-safe value access
 */
object ConfigRegistry {

    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration

    @Config("messages.yml")
    lateinit var messages: Configuration

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        // Setup reload callback
        config.onReload {
            info("Config reloaded!")
            loadSettings()
        }

        // Initial load
        loadSettings()
    }

    private fun loadSettings() {
        val serverName = config.getString("server.name", "Default Server")
        val maxPlayers = config.getInt("server.max-players", 100)
        val enableEconomy = config.getBoolean("features.economy", true)

        info("Server: $serverName")
        info("Max Players: $maxPlayers")
        info("Economy: ${if (enableEconomy) "Enabled" else "Disabled"}")
    }

    // Get message from messages.yml
    fun getMessage(key: String): String {
        return messages.getString("messages.$key", "Message not found: $key")
    }

    // Update config value and save
    fun updateSetting(path: String, value: Any) {
        config[path] = value
        config.saveToFile()  // Must call explicitly to persist changes
    }

    // Manual reload
    fun reload() {
        config.reload()
        messages.reload()
    }
}

/**
 * Example config.yml structure:
 *
 * server:
 *   name: "My Server"
 *   max-players: 100
 *
 * features:
 *   economy: true
 *   pvp: false
 *
 * database:
 *   host: "localhost"
 *   port: 3306
 *   database: "minecraft"
 */
