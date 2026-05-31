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
- **[12_nms_proxy.md](12_nms_proxy.md)** - nmsProxy cross-version NMS, ASM translation, Packet
- **[13_incision.md](13_incision.md)** - Bytecode weaving, @Surgeon, @Lead, @Trail, @Splice

### Testing
- **[14_testing.md](14_testing.md)** - 单元测试/集成测试层级、IocTestContext、@TabooLibIocTest、MockBukkit

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

## 14. 单元测试与集成测试
- **文件**: `14_testing.md`
- **核心 API**: `IocTestContext`, `@TabooLibIocTest`, `@IocAutowired`, `TabooLibIocTestContext`, `MockBukkit`
- **适用场景**: 功能开发完成后的自动化测试, IOC 注入测试, 生命周期回调测试, AOP 切面测试, 数据库测试
- **相关食谱**: `setup_testing.md`

## Cross-References

- For architecture enforcement → `$taboolib-bukkit-plugin-architecture-standards`
- For database persistence → `$taboolib-corelib-easyquery-persistence-standards`
- For CoreBridge packets → `$taboolib-corebridge-packet-standards`

## 12. NMS 代理系统 (nmsProxy)
- **文件**: `12_nms_proxy.md`
- **模块**: `module/bukkit-nms`
- **核心 API**: `nmsProxy<T>()`, `nmsProxyClass<T>()`, `unsafeLazy`, `MinecraftVersion`, `require()`
- **适用场景**: 跨版本 NMS 操作, 封装版本差异, 访问 Bukkit API 未暴露的底层功能
- **相关食谱**: `create_nms_proxy.md`

## 13. Incision 字节码织入
- **文件**: `13_incision.md`
- **模块**: `module/incision`
- **核心 API**: `@Surgeon`, `@Lead`, `@Trail`, `@Splice`, `@Bypass`, `@Excise`, `Scalpel`, `Theatre`, `Suture`
- **适用场景**: 运行时字节码织入, NMS 方法拦截, 方法入口/出口探针, 临时 patch, 私有字段/方法访问
- **相关食谱**: `create_incision_advice.md`
