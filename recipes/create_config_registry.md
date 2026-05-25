# Recipe: Create Config Registry

## Use When

- Need to add configuration file management
- Implementing config reload functionality
- Setting up plugin settings

## Required Checks

- [ ] Verify config file location: `src/main/resources/`
- [ ] Check user's existing config pattern
- [ ] Confirm lifecycle timing (ENABLE or later)

## Implementation Steps

### Step 1: Create Config File

**File**: `src/main/resources/config.yml`
```yaml
# Plugin settings
server:
  name: "My Server"
  max-players: 100

features:
  economy: true
  pvp: false
```

### Step 2: Create Config Manager

**File**: `src/main/kotlin/<package>/config/ConfigManager.kt`
```kotlin
package com.example.plugin.config

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object ConfigManager {
    
    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration
    
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        config.onReload {
            info("Config reloaded!")
            loadSettings()
        }
        loadSettings()
    }
    
    private fun loadSettings() {
        val serverName = config.getString("server.name", "Default")
        info("Server name: $serverName")
    }
    
    fun reload() {
        config.reload()
    }
}
```

## Verification Steps

1. Build: `./gradlew build` → success
2. Start server → check logs for "Config reloaded!"
3. Modify config.yml → verify auto-reload (if enabled)
4. Call `ConfigManager.reload()` → verify manual reload works

## References

- Card: `04_config.md`
