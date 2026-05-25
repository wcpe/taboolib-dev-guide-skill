# Card 01: Project Setup

## When to Use

Load this card when:
- User asks about "项目搭建", "Gradle配置", "TabooLib依赖"
- Starting a new TabooLib plugin project
- Troubleshooting dependency or version issues
- Need to detect user's TabooLib version

## Core Idea

TabooLib is a Gradle-based framework requiring specific dependency configuration. The plugin must declare TabooLib version, apply the TabooLib Gradle plugin, and configure module dependencies. **Always read the user's actual `build.gradle.kts` to detect their TabooLib version** - never assume a fixed version.

## Recommended Pattern

### Gradle Configuration Structure

**File: `build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"  // Check user's actual version
    id("io.izzel.taboolib") version "2.0.11"  // TabooLib Gradle plugin
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")  // NMS support (optional)
    compileOnly("ink.ptms.core:v12004:12004:universal")  // Universal NMS (optional)
    compileOnly(kotlin("stdlib"))
    
    // TabooLib modules - use taboo() not compileOnly()
    taboo("platform-bukkit")  // Required for Bukkit plugins
    taboo("module-configuration")  // Config system
    taboo("module-chat")  // Chat/message utilities
    taboo("module-lang")  // i18n system
    taboo("module-nms")  // NMS utilities (optional)
    taboo("expansion-command-helper")  // Command system
    taboo("database-ioc")  // IoC container (if using DI)
}

taboolib {
    version {
        taboolib = "6.2.0"  // Check user's actual version
    }
    
    env {
        install("platform-bukkit")
        install("module-configuration")
        install("module-chat")
        install("module-lang")
        install("expansion-command-helper")
        install("database-ioc")  // If using IoC
    }
    
    relocate("taboolib", "${project.group}.taboolib")  // Required for IoC
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"  // Match user's Java version
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}
```

### Plugin Main Class Structure

**File: `src/main/kotlin/com/example/plugin/ExamplePlugin.kt`**

```kotlin
package com.example.plugin

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

// Kotlin object, not class extending JavaPlugin
object ExamplePlugin : Plugin() {
    
    override fun onEnable() {
        info("Plugin enabled")
    }
    
    override fun onDisable() {
        info("Plugin disabled")
    }
}
```

### Plugin Metadata

**File: `src/main/resources/plugin.yml`**

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.plugin.ExamplePlugin
api-version: "1.20"
author: YourName
description: Example TabooLib plugin
```

## Detecting User's TabooLib Version

**Step 1**: Read `build.gradle.kts`
```kotlin
// Look for this line
taboolib {
    version {
        taboolib = "6.2.0"  // ← User's version
    }
}
```

**Step 2**: Check imports in existing code
```kotlin
// Grep for TabooLib imports
import taboolib.common.platform.*
import taboolib.module.configuration.*
```

**Step 3**: If version unclear, ask user or mark APIs as "unverified"

## Common Mistakes

### ❌ Mistake 1: Using `compileOnly()` for TabooLib modules
```kotlin
dependencies {
    compileOnly("taboolib:platform-bukkit:6.2.0")  // ← WRONG!
}
```
**Why wrong**: TabooLib modules must use `taboo()` to be properly relocated and included.

**Fix**: Use `taboo()` instead
```kotlin
dependencies {
    taboo("platform-bukkit")  // ← CORRECT
}
```

### ❌ Mistake 2: Missing `relocate` configuration
```kotlin
taboolib {
    version { taboolib = "6.2.0" }
    // Missing relocate! ← Will cause conflicts with other plugins
}
```
**Why wrong**: Without relocation, multiple plugins using TabooLib will conflict.

**Fix**: Add relocate configuration
```kotlin
taboolib {
    version { taboolib = "6.2.0" }
    relocate("taboolib", "${project.group}.taboolib")  // ← CORRECT
}
```

### ❌ Mistake 3: Plugin main class extends JavaPlugin
```kotlin
class ExamplePlugin : JavaPlugin() {  // ← WRONG for TabooLib
    override fun onEnable() { }
}
```
**Why wrong**: TabooLib uses its own Plugin base class for cross-platform support.

**Fix**: Use TabooLib's Plugin class
```kotlin
object ExamplePlugin : Plugin() {  // ← CORRECT
    override fun onEnable() { }
}
```

### ❌ Mistake 4: Assuming fixed TabooLib version
```kotlin
// In LLM-generated code
import taboolib.module.configuration.Config  // ← May not exist in user's version!
```
**Why wrong**: TabooLib APIs change across versions. User may have older/newer version.

**Fix**: Always check user's version first
```kotlin
// Step 1: Read user's build.gradle.kts
// Step 2: Verify import exists in their project
// Step 3: If uncertain, mark as "unverified - check project"
```

## Minimal Example

**Complete minimal plugin structure**:

```
my-plugin/
├── build.gradle.kts          # Gradle configuration
├── settings.gradle.kts       # Project settings
└── src/main/
    ├── kotlin/com/example/plugin/
    │   └── ExamplePlugin.kt  # Plugin main class
    └── resources/
        └── plugin.yml        # Plugin metadata
```

**`ExamplePlugin.kt`**:
```kotlin
package com.example.plugin

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object ExamplePlugin : Plugin() {
    override fun onEnable() {
        info("Plugin enabled")
    }
}
```

## Checklist

Before generating project setup code:

- [ ] Read user's `build.gradle.kts` to detect TabooLib version
- [ ] Read user's `build.gradle.kts` to detect Kotlin version
- [ ] Check if user already has TabooLib configuration
- [ ] Verify required modules for the task (config, command, ioc, etc.)
- [ ] Confirm Java version compatibility (TabooLib 6.2+ requires Java 17+)
- [ ] Check if `relocate` is configured (required for IoC)
- [ ] Verify plugin main class uses `Plugin` not `JavaPlugin`
- [ ] Confirm `plugin.yml` has correct main class path

After generating project setup code:

- [ ] Verify `./gradlew build` succeeds
- [ ] Check generated JAR includes TabooLib classes (if using `taboo()`)
- [ ] Confirm plugin loads on server without ClassNotFoundException
- [ ] Verify no conflicts with other TabooLib plugins (if relocate configured)

## Version-Specific Notes

**TabooLib 6.2.0+**:
- Requires Java 17+
- IoC container requires `database-ioc` module + `relocate` configuration
- New command system with option support (`newParser = true`)

**TabooLib 6.1.x**:
- Java 11+ supported
- Older IoC annotations may differ
- Check wiki for version-specific migration notes

**TabooLib 6.0.x and earlier**:
- Significantly different API surface
- Recommend upgrading to 6.2.x for better support
- Many examples in this skill may not apply

## References

- Source code: `taboolib/gradle-plugin/`
- Wiki: Project setup guide
- Related cards: `02_lifecycle.md` (plugin initialization)
