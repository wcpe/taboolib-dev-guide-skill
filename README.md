# TabooLib 开发指南

面向 LLM 的 TabooLib Minecraft 插件开发手册。当用户基于 TabooLib 开发 Bukkit / Paper 插件时，AI 加载本 skill 后会按 `SKILL.md` 的路由规则匹配关键字（命令、配置、IoC、监听器、调度器、生命周期、数据库、箱子菜单、排错等），加载对应的 cards / recipes / examples / api-map，生成最小可编译代码。

## 配置：本地源码与 Wiki 路径（可选）

本 skill 可以选择性地引用本地的 TabooLib 源码与 Wiki 做交叉验证。是否启用由用户在自己项目根目录的 `AGENTS.md`（或 `CLAUDE.md`）中声明：

- **声明了路径** → AI 会在生成代码前去对应目录查源码 / Wiki，校验 API 真实存在。
- **没有声明** → AI 直接使用 skill 内置的 cards / recipes / api-map 作为知识来源，不会向用户追问。

### 配置示例

在项目根目录的 `AGENTS.md` 中加入一节：

```markdown
## TabooLib 本地资源路径
- TabooLib 源码: D:\path\to\taboolib
- TabooLib Wiki: D:\path\to\taboowiki
```

要点：

- 标题必须是 **`TabooLib 本地资源路径`**，AI 通过这个标题定位配置块。
- 键名固定为 **`TabooLib 源码`** 和 **`TabooLib Wiki`**，AI 通过键名提取路径。
- 两项都是可选的，单独配一项也可以；未配置的那一项 AI 会自动跳过。

### 占位符说明

skill 文档里出现的 `<TABOOLIB_SOURCE_DIR>` / `<TABOOLIB_WIKI_DIR>` 是**字面占位符**，不会被任何机制自动替换。它们仅用于在文档中标注"此处指代用户配置的本地路径"，AI 读到时按上面的检测规则解析。
