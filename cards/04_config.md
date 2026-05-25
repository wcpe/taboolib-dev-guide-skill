# Card 04: Configuration System

## When to Use

Load this card when:
- User asks about "config", "配置", "yaml", "reload"
- Need to load/save configuration files
- Implementing config reload functionality
- Troubleshooting config not loading or saving issues

## Core Idea

TabooLib provides `@Config` annotation for automatic file management and `Configuration` interface for programmatic access. Config files are loaded during INIT/LOAD lifecycle and support YAML, TOML, JSON, HOCON formats. **Changes must be explicitly saved with `saveToFile()`** - modifications are not auto-persisted.

## Recommended Pattern

### Basic Config Usage

**Package**: `taboolib.module.configuration`

```kotlin
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object ConfigManager {
    
    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration
    
    @Config("messages.yml")
    lateinit var messages: Configuration
    
    // Access values
    fun getValue(): String {
        return config.getString("path.to.value", "default")
    }
    
    // Modify and save
    fun setValue(value: String) {
        config["path.to.value"] = value
        config.saveToFile()  // ← Must call explicitly!
    }
    
    // Reload manually
    fun reload() {
        config.reload()
        messages.reload()
    }
}
```

### @Config Annotation Parameters

```kotlin
@Target(AnnotationTarget.FIELD)
annotation class Config(
    val value: String = "config.yml",      // File name
    val target: String = "",               // Target path for file release
    val migrate: Boolean = false,          // Enable migration
    val autoReload: Boolean = false,       // Auto-reload on file change
    val concurrent: Boolean = true         // Thread-safe access
)
```

### Configuration API

**Common Methods**:
```kotlin
// Get values
config.getString("path", "default")
config.getInt("path", 0)
config.getDouble("path", 0.0)
config.getBoolean("path", false)
config.getStringList("path")
config.getConfigurationSection("path")

// Set values
config["path"] = value
config.set("path", value)

// Check existence
config.contains("path")

// Get all keys
config.getKeys(deep = false)  // Top-level keys only
config.getKeys(deep = true)   // All nested keys

// Save and reload
config.saveToFile()
config.reload()
config.onReload { /* callback */ }
```

### Programmatic Config Creation

```kotlin
import taboolib.module.configuration.Configuration

// Create empty config
val config = Configuration.empty()

// Load from file
val config = Configuration.loadFromFile(file)

// Load from string
val yaml = """
    server:
      port: 25565
      name: "My Server"
""".trimIndent()
val config = Configuration.loadFromString(yaml)

// Save to file
config.saveToFile(File("config.yml"))
```

### Config Reload with Callback

```kotlin
@Config("config.yml", autoReload = true)
lateinit var config: Configuration

fun setupReloadCallback() {
    config.onReload {
        // Called when file changes (if autoReload = true)
        // or when reload() is called manually
        println("Config reloaded!")
        reloadDependentSystems()
    }
}
```

## Common Mistakes

### ❌ Mistake 1: Not saving after modification
```kotlin
config["value"] = "new value"
// Missing saveToFile()! ← Changes lost on reload
```
**Why wrong**: Configuration changes are in-memory only until saved.

**Fix**: Always call saveToFile()
```kotlin
config["value"] = "new value"
config.saveToFile()  // ← Persist changes
```

### ❌ Mistake 2: Accessing config in CONST lifecycle
```kotlin
@Awake(LifeCycle.CONST)
fun loadConfig() {
    val value = config["key"]  // ← Config not loaded yet!
}
```
**Why wrong**: Config files are loaded during INIT/LOAD, not available in CONST.

**Fix**: Use ENABLE or later
```kotlin
@Awake(LifeCycle.ENABLE)
fun loadConfig() {
    val value = config["key"]  // ← Safe
}
```

### ❌ Mistake 3: Expecting auto-reload without configuration
```kotlin
@Config("config.yml")  // ← autoReload = false (default)
lateinit var config: Configuration

// File changes on disk → not reloaded automatically
```
**Why wrong**: Auto-reload is disabled by default.

**Fix**: Enable autoReload or reload manually
```kotlin
@Config("config.yml", autoReload = true)  // ← Enable auto-reload
lateinit var config: Configuration

// OR reload manually
fun reloadConfig() {
    config.reload()
}
```

### ❌ Mistake 4: Using nested structure incorrectly
```kotlin
// config.yml
server:
  settings:
    port: 25565

// Code
val port = config.getInt("server.settings.port")  // ← Correct
val port = config.getInt("server/settings/port")  // ← WRONG! Use dots, not slashes
```
**Why wrong**: Path separator is dot (`.`), not slash (`/`).

**Fix**: Use dot notation
```kotlin
val port = config.getInt("server.settings.port")
```

### ❌ Mistake 5: Not handling missing values
```kotlin
val port = config.getInt("server.port")  // ← Returns 0 if missing, may cause issues
```
**Why wrong**: Missing values return defaults (0, false, null) which may not be appropriate.

**Fix**: Provide explicit defaults or check existence
```kotlin
val port = config.getInt("server.port", 25565)  // ← Explicit default

// OR check first
if (config.contains("server.port")) {
    val port = config.getInt("server.port")
} else {
    // Handle missing config
}
```

## Minimal Example

**Complete config management**:

```kotlin
package com.example.plugin

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object PluginConfig {
    
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
        info("Server: $serverName, Max Players: $maxPlayers")
    }
    
    fun getMessage(key: String): String {
        return messages.getString("messages.$key", "Message not found: $key")
    }
    
    fun updateSetting(path: String, value: Any) {
        config[path] = value
        config.saveToFile()
    }
    
    fun reload() {
        config.reload()
        messages.reload()
    }
}
```

**Example config.yml**:
```yaml
server:
  name: "My Server"
  max-players: 100
  
features:
  economy: true
  pvp: false
  
database:
  host: "localhost"
  port: 3306
  database: "minecraft"
```

## Checklist

Before using config:

- [ ] Verify config file exists in `src/main/resources/`
- [ ] Check if autoReload is needed (development vs production)
- [ ] Confirm config access happens in ENABLE or later lifecycle
- [ ] Verify all config paths have default values
- [ ] Check if concurrent access is needed (default: true)
- [ ] Determine if reload callback is needed

After implementing config:

- [ ] Test config loads on plugin enable
- [ ] Test default values work when keys missing
- [ ] Test modifications persist after saveToFile()
- [ ] Test reload() updates values correctly
- [ ] Test autoReload detects file changes (if enabled)
- [ ] Test nested paths work correctly

## Version-Specific Notes

**TabooLib 6.2.0+**:
- All formats supported: YAML, TOML, JSON, HOCON
- Auto-reload with file watcher
- Thread-safe by default (concurrent = true)

**TabooLib 6.1.x**:
- Similar API, check wiki for format support

## Troubleshooting

**Error: "Config not loaded"**
- Cause: Accessing config in CONST or before INIT
- Fix: Move to ENABLE lifecycle

**Error: "Changes not persisted"**
- Cause: Missing saveToFile() call
- Fix: Call saveToFile() after modifications

**Error: "File not found"**
- Cause: Config file not in resources folder
- Fix: Place file in `src/main/resources/`

**Error: "Auto-reload not working"**
- Cause: autoReload = false or file watcher issue
- Fix: Set autoReload = true, check file permissions

## References

- Source code: `taboolib/module/basic/basic-configuration/`
- Related cards: `02_lifecycle.md` (config loading timing)
- Related recipes: `create_config_registry.md`
