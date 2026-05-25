# Card 11: Chest Menu Standards

## When to Use

Load this card when:
- User asks about "menu", "GUI", "chest menu", "箱子菜单", "界面"
- Need to create inventory-based UI
- Implementing shop, settings, confirmation dialogs
- Troubleshooting item extraction exploits or click handling

## Core Idea

TabooLib provides chest-based menu system through `buildMenu<Chest>` and `openMenu<Chest>`. **Display-only menus MUST call `virtualize()` or `hidePlayerInventory()` to prevent item extraction/placement exploits**. All item materials MUST use `XMaterial` for cross-version compatibility. Functional buttons MUST have lore/description.

## ⚠️ Critical Security Rules

### 🚫 Display-Only Menus Must Use Virtual Mode

**展示型菜单（商店、信息、设置、确认、管理面板）必须调用 `virtualize()` 或 `hidePlayerInventory()` 防止物品提取/放置漏洞**。

**正确模式**:
```kotlin
// ✅ 使用 virtualize() - 推荐
player.openMenu<Chest>("商店") {
    virtualize()  // ← 防止物品提取/放置
    
    rows(3)
    
    set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!)) {
        // 点击处理
    }
}

// ✅ 使用 hidePlayerInventory() - 替代方案
player.openMenu<Chest>("商店") {
    hidePlayerInventory()  // ← 隐藏玩家背包
    
    rows(3)
    
    set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!)) {
        // 点击处理
    }
}
```

**错误模式**:
```kotlin
// ❌ 没有 virtualize() - 玩家可以提取/放置物品！
player.openMenu<Chest>("商店") {
    rows(3)
    
    set(10, ItemStack(Material.DIAMOND)) {  // ← 漏洞：玩家可以拿走钻石
        // 点击处理
    }
}
```

### 🚫 必须使用 XMaterial，禁止直接使用 Material

**所有菜单中的物品材质必须使用 `XMaterial`，禁止直接使用 `org.bukkit.Material`（不跨版本安全）**。

**正确模式**:
```kotlin
import taboolib.library.xseries.XMaterial

// ✅ 使用 XMaterial
val item = ItemStack(XMaterial.DIAMOND.parseMaterial()!!)
val wool = ItemStack(XMaterial.WHITE_WOOL.parseMaterial()!!)
val glass = ItemStack(XMaterial.GLASS_PANE.parseMaterial()!!)
```

**错误模式**:
```kotlin
import org.bukkit.Material

// ❌ 直接使用 Material - 跨版本不安全
val item = ItemStack(Material.DIAMOND)
val wool = ItemStack(Material.WHITE_WOOL)  // ← 1.12 没有这个枚举值
```

### 🚫 功能按钮必须有 Lore/Description

**所有功能按钮（非装饰）必须有 lore/description。装饰物品（玻璃板、填充物）可以使用空名称 `"§r"` 和无 lore**。

**正确模式**:
```kotlin
// ✅ 功能按钮有 lore
set(10, buildItem(XMaterial.DIAMOND) {
    name = "§a购买钻石"
    lore += "§7价格: §e100 金币"
    lore += "§7点击购买"
}) {
    // 点击处理
}

// ✅ 装饰物品可以无 lore
set(0, buildItem(XMaterial.GLASS_PANE) {
    name = "§r"  // 空名称
}) {
    isCancelled = true  // 禁止交互
}
```

**错误模式**:
```kotlin
// ❌ 功能按钮没有 lore - 玩家不知道这是什么
set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!)) {
    // 点击处理
}
```

### 🚫 Display-Only Menus Must Retain Hand-Lock Protection

**展示型菜单必须保持手持槽/热键/拖拽等交互保护**。

**正确模式**:
```kotlin
player.openMenu<Chest>("商店") {
    virtualize()
    handLocked(true)  // ✅ 默认值，保持手持槽锁定
    
    rows(3)
}
```

**错误模式**:
```kotlin
player.openMenu<Chest>("商店") {
    virtualize()
    handLocked(false)  // ❌ 除非确实需要，否则不要禁用
    
    rows(3)
}
```

## Recommended Pattern

### Basic Display-Only Menu

```kotlin
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

fun openShopMenu(player: Player) {
    player.openMenu<Chest>("商店") {
        virtualize()  // ← 防止物品提取
        
        rows(3)
        
        // 功能按钮
        set(10, buildItem(XMaterial.DIAMOND) {
            name = "§a购买钻石"
            lore += "§7价格: §e100 金币"
            lore += "§7点击购买"
        }) {
            isCancelled = true
            player.sendMessage("购买钻石")
            // 委托到 service
            submitAsync {
                shopService.buyItem(player.name, "diamond")
            }
        }
        
        // 装饰物品
        set(0, buildItem(XMaterial.GLASS_PANE) {
            name = "§r"
        }) {
            isCancelled = true
        }
    }
}
```

### Storable Menu (Player Can Place Items)

**可存储菜单（`StorableChest`）必须锁定所有装饰槽位，使用 `rule { }` + `checkSlot` 进行物品校验，并处理所有点击类型**。

```kotlin
import taboolib.module.ui.type.StorableChest

fun openStorageMenu(player: Player) {
    player.openMenu<StorableChest>("仓库") {
        rows(6)
        
        // 锁定装饰槽位
        slots(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8))
        
        // 物品校验规则
        rule { event ->
            // 只允许特定槽位
            if (event.slot !in 9..53) {
                event.isCancelled = true
                return@rule
            }
            
            // 校验物品类型
            val item = event.currentItem ?: return@rule
            if (item.type == XMaterial.BEDROCK.parseMaterial()) {
                event.isCancelled = true
                player.sendMessage("§c不能存放基岩")
            }
        }
        
        // 处理所有点击类型
        onClick { event ->
            when (event.clickEvent.click) {
                ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                    // 处理 Shift 点击
                }
                ClickType.NUMBER_KEY -> {
                    // 处理数字键
                }
                else -> {
                    // 处理其他点击
                }
            }
        }
    }
}
```

### Pagination Menu

```kotlin
fun openPagedMenu(player: Player, items: List<ItemStack>) {
    player.openMenu<Chest>("分页菜单") {
        virtualize()
        
        rows(6)
        
        // 设置分页槽位
        slots(listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25))
        
        // 设置分页元素
        elements { items }
        
        // 上一页按钮
        setPreviousPage(48) { _, hasPreviousPage ->
            buildItem(XMaterial.ARROW) {
                name = if (hasPreviousPage) "§a上一页" else "§7上一页"
            }
        }
        
        // 下一页按钮
        setNextPage(50) { _, hasNextPage ->
            buildItem(XMaterial.ARROW) {
                name = if (hasNextPage) "§a下一页" else "§7下一页"
            }
        }
        
        // 元素点击处理
        onGenerate { _, element, _, _ ->
            element
        }
        
        onClick { event ->
            isCancelled = true
            // 处理点击
        }
    }
}
```

## Common Mistakes

### ❌ Mistake 1: No virtualize() for display-only menu

```kotlin
// ❌ 玩家可以提取物品
player.openMenu<Chest>("商店") {
    rows(3)
    set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!))
}
```

**Fix**: Add `virtualize()`
```kotlin
// ✅ 防止物品提取
player.openMenu<Chest>("商店") {
    virtualize()
    rows(3)
    set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!))
}
```

### ❌ Mistake 2: Using Material instead of XMaterial

```kotlin
// ❌ 跨版本不安全
val item = ItemStack(Material.WHITE_WOOL)
```

**Fix**: Use XMaterial
```kotlin
// ✅ 跨版本安全
val item = ItemStack(XMaterial.WHITE_WOOL.parseMaterial()!!)
```

### ❌ Mistake 3: Functional button without lore

```kotlin
// ❌ 玩家不知道这是什么
set(10, ItemStack(XMaterial.DIAMOND.parseMaterial()!!)) {
    // 点击处理
}
```

**Fix**: Add lore
```kotlin
// ✅ 有说明
set(10, buildItem(XMaterial.DIAMOND) {
    name = "§a购买钻石"
    lore += "§7价格: §e100 金币"
}) {
    // 点击处理
}
```

### ❌ Mistake 4: Storable menu without slot validation

```kotlin
// ❌ 玩家可以在任何槽位放置任何物品
player.openMenu<StorableChest>("仓库") {
    rows(6)
}
```

**Fix**: Add slot validation
```kotlin
// ✅ 锁定装饰槽位，校验物品
player.openMenu<StorableChest>("仓库") {
    rows(6)
    slots(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8))
    
    rule { event ->
        if (event.slot !in 9..53) {
            event.isCancelled = true
        }
    }
}
```

## Verification Checklist

Before deploying menu code:

- [ ] Display-only menus use `virtualize()` or `hidePlayerInventory()`
- [ ] All materials use `XMaterial`, not `Material`
- [ ] All functional buttons have lore/description
- [ ] Decoration items use `"§r"` name and no lore
- [ ] Storable menus lock decoration slots
- [ ] Storable menus validate item types
- [ ] All click types handled (CLICK, DRAG, number-key, shift-click)
- [ ] `handLocked(true)` is default and not disabled unless needed
- [ ] Menu click handlers delegate to service layer (no business logic in menu)

## Quick Reference

### Menu Types

| Type | Use Case | Virtual Mode | Item Validation |
|------|----------|--------------|-----------------|
| `Chest` (display-only) | Shop, info, settings | ✅ Required | ❌ Not needed |
| `StorableChest` | Storage, backpack | ❌ Not used | ✅ Required |
| `Chest` (pagination) | Item list, ranking | ✅ Required | ❌ Not needed |

### XMaterial Common Items

```kotlin
XMaterial.DIAMOND.parseMaterial()
XMaterial.GLASS_PANE.parseMaterial()
XMaterial.WHITE_WOOL.parseMaterial()
XMaterial.ARROW.parseMaterial()
XMaterial.BARRIER.parseMaterial()
XMaterial.PLAYER_HEAD.parseMaterial()
```

## References

- Related cards: `03_command.md` (service delegation), `07_scheduler.md` (async execution)
- Related recipes: `create_command.md` (menu opening from commands)
- Architecture skill: `taboolib-bukkit-plugin-architecture-standards` (chest menu standards)
