# Design Brief - 斗兽棋界面

**Slug:** `doushouqi-ui`
**User brief (verbatim summary):** “实现斗兽棋”，并指定通过 `ai-everything:designer` 进行设计。
**Stack:** Android Kotlin + Jetpack Compose，离线动态游戏包，横屏 Android Pad
**Iteration:** 2026-07-26T00:00:00+08:00
**Status:** 本文件是斗兽棋首版视觉 SSOT；预览、Compose 实现与包内资源必须遵循本文件。

## Base System

`ui-ux-pro-max` 将产品识别为触屏棋盘娱乐，首先推荐深色毛毡、金色强调和重 3D 拟物。该方向虽然有棋具触感，但性能、文字对比和小尺寸棋格辨识不适合离线 Android Pad，因此只保留其触控、可访问性和即时反馈约束。

| Dimension | Content |
| --- | --- |
| Product / industry | Android Pad 离线斗兽棋；面向家庭、本地双人玩家与棋类爱好者 |
| Page structure | 左侧固定 7×9 棋盘主舞台，右侧固定状态与操作栏；菜单和对局共享同一横屏空间逻辑 |
| Color tokens | 原始建议为森林绿 `#15803D`、金棕 `#D97706`、深蓝黑 `#0F172A`、白色文字 |
| Typography | 原始建议为 Noto Serif TC 标题、Noto Sans TC 正文；实现使用 Android 系统 CJK 衬线/无衬线字体，避免外部字体依赖 |
| Interaction | 最小 48dp 触控目标、至少 8dp 间距、100ms 内按压反馈；机器人思考时禁用棋盘；状态不只依赖颜色 |
| Anti-patterns (avoid) | 不使用复杂 3D、重阴影、全屏暗色毛毡、霓虹森林、动物照片、悬浮卡片堆叠、过小棋格或只靠红绿表达阵营 |

## Revised Direction

### Three approaches considered

1. **野兽领地沙盘，推荐。** 以两条深蓝河道切开竹木色棋盘，兽穴、陷阱与狮虎跃河线路构成清晰地形；朱砂与松绿棋子使用大号拓印式单字兽名，缩小后仍能识别。
2. **儿童动物绘本。** 用八种动物插画和明亮草地吸引家庭用户，但 16 枚棋子会产生过多小图，降低棋力关系和地形的读取速度，也偏离现有游戏家族。
3. **暗色丛林竞技场。** 深色叶影、发光河流和金属棋子更戏剧化，但在明亮平板环境中对比不稳定，且与军棋、象棋现有的明亮实体棋具方向不一致。

### Detemplating answers

1. **Subject grounding** - 具体对象是同一台 Android Pad 上进行的标准 7×9 斗兽棋；受众是需要快速判断兽阶、河道、陷阱和兽穴的玩家；单屏任务是完成一次选择与走棋。
2. **Hero / opening** - 菜单的主角不是通用 Logo 卡片，而是一张无棋子的斗兽棋领地板；两条河道、六个陷阱和两个兽穴直接说明游戏是什么。
3. **Typography** - 标题使用克制的系统衬线体，棋子使用极粗无衬线单字。棋子文字像棋具上的拓印，不采用可复用于任意 SaaS 的中性字体层级。
4. **Structure** - 右栏规则线只分隔比分、回合和操作三种真实信息层级；不使用装饰性编号、徽章或胶囊标签。
5. **Memory point (signature)** - 棋盘中央的“双河峡谷”：深蓝河面带有极简流向短纹，狮虎可跨越的长边以克制的金色跳跃刻线提示。
6. **Aesthetic risk** - 将河道做成整块高对比深蓝地形，而不是传统浅蓝网格。它占据显著面积，但能让鼠入河、狮虎跃河这套独特规则一眼可见。
7. **Detemplating changes** - 删除原始深色 3D、金色 CTA 和拟真动物材质，改为明亮竹木领地板、矿物蓝河道、拓印兽名和平整账簿式右栏。
8. **Rejected defaults** - 拒绝奶油色加陶土红生活方式模板、近黑加荧光单色、报纸栏目、通用深色电竞 HUD、森林照片和玻璃卡片。

## Final Tokens

| Token | Hex | Role |
| --- | --- | --- |
| `canvas` | `#E7ECE8` | 应用背景，冷矿物灰绿 |
| `surface` | `#F5F6F1` | 右侧状态栏与按钮浅面 |
| `board` | `#D5B875` | 棋盘主体，低饱和竹木金 |
| `boardLight` | `#E7D29A` | 普通格与菜单空棋盘亮面 |
| `grid` | `#594A2F` | 棋盘网格、边线和地形文字 |
| `river` | `#315F78` | 双河峡谷主色 |
| `riverLine` | `#8DB4C3` | 河流短纹与跳跃提示的冷色部分 |
| `jumpLine` | `#D9B85F` | 狮虎跨河长边提示 |
| `den` | `#8E372E` | 兽穴中心与终局强调 |
| `trap` | `#B6813F` | 陷阱内框与符号 |
| `greenPiece` | `#25664E` | 松绿方棋子 |
| `redPiece` | `#A64332` | 朱砂方棋子 |
| `pieceText` | `#FFF9E8` | 棋子兽名与阵营辅助符号 |
| `ink` | `#292C25` | 主文字 |
| `muted` | `#686D63` | 次级文字和禁用状态 |
| `lastMove` | `#4FCBFF` at 72% | 所有游戏共享的最后移动亮蓝四角框 |
| `legalMove` | `#273E32` | 普通合法落点 |
| `captureRing` | `#F4EBD4` | 吃子目标的浅色内环 |

颜色不单独承担信息：阵营同时显示 `松绿方`、`朱砂方` 文本；河道、陷阱和兽穴同时依赖形状、位置与汉字标识。

## Typography

- Display: Android `FontFamily.Serif`，`斗兽棋` 48sp / 600，短标题 26sp / 600。
- Body: Android `FontFamily.SansSerif`，18sp / 500；次级信息 14sp / 400。
- Utility: Android `FontFamily.Monospace`，比分 38sp / 500、等级和版本 13-15sp。
- Piece label: Android `FontFamily.SansSerif` + `FontWeight.Black`，单字占棋子高度约 58%-64%，字距 0，无描边、无投影。
- 地形字 `兽穴`、`陷阱` 使用 11-13sp 粗体；即使有棋子覆盖，也通过底色和几何继续区分。

## Layout Concept

“一张从林地中切出的双河领地板，旁边放着一页安静的对局账簿。”棋盘负责规则辨识和视觉记忆，右栏只负责状态与操作。

### Main menu

```text
┌──────────────────────────────┬──────────────────┐
│  den · traps                 │   circular logo  │
│      blue river canyon       │      斗兽棋      │
│  empty 7×9 territory board   │   版本 0.0.1     │
│      blue river canyon       │──────────────────│
│  traps · den                 │   [单人模式]     │
│                              │   [双人对战]     │
│                              │   [退出游戏]     │
└──────────────────────────────┴──────────────────┘
```

- 宽布局沿用象棋家族几何：外边距 28dp、棋盘与右栏间距 34dp；菜单右栏固定 320dp 宽并占内容高度 88%。
- 空棋盘保持 7:9 比例并居中；河道和兽穴足以辨认游戏，不额外放置动物插画。
- 三个菜单按钮高 64dp，圆角 8dp；单人模式使用松绿实心，双人模式使用浅面描边，退出游戏使用朱砂文字描边。

### Game screen

```text
┌──────────────────────────────┬──────────────────┐
│  green den + traps           │ 玩家       智能 │
│  green animals               │   0   :   0      │
│      blue river canyon       │──────────────────│
│      blue river canyon       │ 松绿方回合       │
│  red animals                 │ 玩家执松绿 · 等级1│
│  red traps + den             │                  │
│                              │ [悔棋] [返回菜单]│
└──────────────────────────────┴──────────────────┘
```

- 对局右栏固定 300dp 宽并占内容高度 94%；棋盘完整显示 7×9 格，不滚动、不裁切。
- 棋子占单格短边约 78%，使用轻微圆角矩形而非圆片；棋子边缘保留地形可见间隙。
- 选中棋子显示同阵营浅色内框；普通合法落点显示墨绿圆点；可吃目标显示象牙色断环。
- 最近移动目标使用共享亮蓝四角框，位于棋子外侧并保持中心空白。
- 人类执下方阵营；单人换边时棋盘旋转 180 度映射坐标，但兽名文字保持正向。
- 低于 900dp 可用宽度时，右栏变为棋盘下方全宽操作带，仍保持棋盘完整和 48dp 触控目标。

## Interaction And States

- 所有按钮最小 48dp，高优先级按钮 56-64dp；按钮间距至少 8dp。
- 棋子按下时在 100ms 内降低 5% 明度，不改变布局尺寸；选中态同时使用边框，不只依赖颜色。
- 页面切换只使用 160-220ms 淡入；不做河水循环动画、粒子或视差。系统减少动态效果时直接切换。
- 机器人思考时回合文字显示 `智能思考中`，棋盘触控禁用，最后一步标记保留。
- 胜负状态保留最终局面；右栏显示 `松绿方进入兽穴`、`朱砂方无棋可走` 等明确原因。
- 支持系统字体缩放；读屏顺序为局面摘要、当前回合、比分、可用操作。每枚棋子的语义包含阵营、兽名和坐标。

## Copy Tone

- Register: 简短、明确，像棋盘旁的裁判记录。
- Vocabulary: 使用 `松绿方回合`、`朱砂方进入兽穴`、`选择棋子`、`悔棋`、`重新开始`。
- Empty/error state: 说明原因和下一步，例如 `当前棋子没有合法走法，请选择另一枚棋子`。

## Preview Index

| Preview | Spec doc | Description |
| --- | --- | --- |
| `designs/previews/doushouqi-ui-menu-tablet.png` | `designs/images/doushouqi-ui-menu-tablet.md` | Android Pad 横屏菜单，空领地棋盘、双河峡谷、Logo、标题、版本与三项菜单 |
| `designs/previews/doushouqi-ui-game-tablet.png` | `designs/images/doushouqi-ui-game-tablet.md` | Android Pad 横屏单人对局，双方拓印兽棋、比分、等级、回合与操作 |

## Implementation Notes

- Primary CTA labels: 菜单 `单人模式`；终局 `重新开始`。
- Components: `DoushouqiBoard`, `DoushouqiPiece`, `DoushouqiStatusRail`, `DoushouqiScoreLine`, `DoushouqiTurnLine`, `DoushouqiActionButton`。
- Package assets: `1024×1024` circular-safe `assets/icon.png`；棋盘与棋子首版以 Compose 语义绘制，避免 16 张小图造成缩放和包体成本。
- Preserve: 标准 7×9 规则坐标、双向点击映射、统一菜单文案、比分/换边/悔棋/重开、共享最近一步标记、象棋家族外层几何、包内独立版本。
- Non-goals: 在线对战、联网素材、音效、动画河流、皮肤选择、棋谱导入导出、可配置计时。

## Delivery Guardrails

- 正常文字对比至少 4.5:1，大标题和大号棋子字至少 3:1。
- 48dp 最小触控区域；相邻按钮至少 8dp；棋格的可点击区域覆盖完整格。
- 不出现动物照片、表情符号、森林剪影堆叠、玻璃拟态、霓虹描边、悬浮卡片阵列或装饰性编号。
- 每屏只保留一个视觉主操作；棋盘地形始终比右栏装饰更醒目。
- 800×600dp 横屏下仍必须读清双方兽名、河道、兽穴、陷阱、比分和当前回合。
