# TabooLib Development Guide - Card Index

## Quick Navigation

### Foundation
- **[01_project_setup.md](01_project_setup.md)** - Gradle setup, dependencies, TabooLib version detection
- **[02_lifecycle.md](02_lifecycle.md)** - @Awake, LifeCycle stages, plugin instance access

### Core Features
- **[03_command.md](03_command.md)** - @CommandHeader, @CommandBody, argument parsing, service delegation
- **[04_config.md](04_config.md)** - @Config, Configuration API, reload mechanism
- **[05_ioc.md](05_ioc.md)** - @Component, @Resource, dependency injection, bean lifecycle
- **[06_listener.md](06_listener.md)** - @SubscribeEvent, event priority, cancellation
- **[07_scheduler.md](07_scheduler.md)** - submit, submitAsync, delay, period, thread safety model

### Advanced Features
- **[08_database.md](08_database.md)** - EasyQuery integration, entity, repository (→ persistence skill)
- **[09_message_i18n.md](09_message_i18n.md)** - Lang system, placeholder, multi-language
- **[11_chest_menu.md](11_chest_menu.md)** - Chest menu, virtualize(), XMaterial, click exploit prevention

### Troubleshooting
- **[10_troubleshooting.md](10_troubleshooting.md)** - Common errors, diagnostic steps, solutions

## Card Structure

Each card follows this format:

1. **When to use** - Trigger conditions for loading this card
2. **Core idea** - 2-3 sentence explanation of the concept
3. **Recommended pattern** - Code structure and best practices
4. **Common mistakes** - Anti-patterns with explanations
5. **Minimal example** - 10-20 lines showing structure only
6. **Checklist** - Pre-flight checks before using this pattern

## Usage in LLM Workflow

1. **Task routing**: User mentions keyword → load relevant card(s)
2. **Pattern matching**: Read card to understand recommended approach
3. **Project inspection**: Check user's existing code style
4. **API verification**: Verify APIs exist in user's project or source code
5. **Code generation**: Generate minimal code following card pattern
6. **Verification**: Use checklist to validate generated code

## Version Notes

All cards are based on TabooLib source code analysis (2026-04-26):
- Source: `<TABOOLIB_SOURCE_DIR>` (placeholder, optional)
- Wiki: `<TABOOLIB_WIKI_DIR>` (placeholder, optional)

These placeholders are resolved from the user's project `AGENTS.md` / `CLAUDE.md` (see SKILL.md → Evidence Policy → Local Path Detection). When the user has not declared a path, skip these layers and rely on the cards directly.

**Critical**: Always verify APIs against user's actual TabooLib version before using.

## Cross-References

- For architecture enforcement → `$taboolib-bukkit-plugin-architecture-standards`
- For database persistence → `$taboolib-corelib-easyquery-persistence-standards`
- For CoreBridge packets → `$taboolib-corebridge-packet-standards`
