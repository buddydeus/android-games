# Design Brief - 斗兽棋界面

**Slug:** `doushouqi-ui`
**User brief (verbatim summary):** “根据图片，完整还原实现棋盘和棋子，侧边栏与其他游戏形式保持一致。”
**Stack:** Android Kotlin + Jetpack Compose，离线动态游戏包，横屏 Android Pad
**Iteration:** 2026-07-27T12:00:00+08:00 — 陷阱辨识度修订
**Status:** 本文件是斗兽棋参考图还原的视觉 SSOT；`designs/references/doushouqi-board-reference.png` 是用户批准的像素级方向，预览、Compose 实现与包内资源必须遵循本文件。

## Base System

`ui-ux-pro-max` 将产品识别为触屏棋盘娱乐并推荐 3D/拟物游戏风格。用户参考图明确要求竹木棋具、深蓝河面、浮雕方棋子和柔和投影，因此保留克制的实体棋具材质；不采用 WebGL、视差、循环动画或会影响离线性能的动态 3D。

| Dimension | Content |
| --- | --- |
| Product / industry | Android Pad 离线斗兽棋；面向家庭、本地双人玩家与棋类爱好者 |
| Page structure | 左侧固定 7×9 棋盘主舞台，右侧固定状态与操作栏；菜单和对局共享同一横屏空间逻辑 |
| Color tokens | 参考图提取的竹木金、深河蓝、松绿、朱砂、米白字和冷灰背景 |
| Typography | 棋子使用包内纹理烘焙的楷书/毛笔单字；状态和按钮继续使用 Android 系统 CJK 字体，避免外部运行时依赖 |
| Interaction | 最小 48dp 触控目标、至少 8dp 间距、100ms 内按压反馈；机器人思考时禁用棋盘；状态不只依赖颜色 |
| Anti-patterns (avoid) | 不使用实时 3D、玻璃拟态、霓虹森林、动物照片、卡片堆叠、过小棋格、将截图整体裁成不可交互棋盘，或只靠红绿表达阵营 |

## Revised Direction

### Three approaches considered

1. **完整包内棋具纹理，采用。** 一张无棋子的近方形竹木棋盘 PNG 负责木纹、边框、河水、网格、陷阱和兽穴；松绿/朱砂各八张透明棋子 PNG 负责浮雕、投影和楷书兽字。Compose 只负责规则状态、点击、合法提示与最后一步标记。
2. **纯 Compose 拟物绘制。** 能保持规则准确和体积较小，但难以稳定还原参考图的细木纹、水纹、棋子倒角与柔和投影。
3. **直接裁切用户截图。** 像素最接近单一画面，但棋子、选中框和侧栏被烘焙在一起，无法支持真实移动、换边、悔棋和终局状态，因此拒绝。

### Detemplating answers

1. **Subject grounding** - 具体对象是同一台 Android Pad 上进行的标准 7×9 斗兽棋；受众是需要快速判断兽阶、河道、陷阱和兽穴的玩家；单屏任务是完成一次选择与走棋。
2. **Hero / opening** - 菜单的主角不是通用 Logo 卡片，而是一张无棋子的斗兽棋领地板；两条河道、六个陷阱和两个兽穴直接说明游戏是什么。
3. **Typography** - 棋子单字使用接近参考图的楷书/毛笔字形并烘焙进透明纹理；侧栏标题、比分和按钮沿用其他内置游戏的系统字体层级。
4. **Structure** - 右栏规则线只分隔比分、回合和操作三种真实信息层级；不使用装饰性编号、徽章或胶囊标签。
5. **Memory point (signature)** - 近方形竹木棋盘中的“双河峡谷”，搭配两色方形浮雕兽棋和大号米白楷书兽字。
6. **Aesthetic risk** - 接受参考图明显的拟物材质与投影，但把它们限制在棋盘和棋子资产中；侧栏保持平整、克制且与游戏家族一致。
7. **Detemplating changes** - 从首版纯 Compose 平涂升级为包内棋具纹理，棋盘约为 `1:1` 而不是严格按 `7:9` 单元格正方形展开；侧栏不复制参考图的专属排版。
8. **Rejected defaults** - 拒绝奶油色加陶土红生活方式模板、近黑加荧光单色、报纸栏目、通用深色电竞 HUD、森林照片和玻璃卡片。
9. **Trap legibility revision** - 用户指出参考图的交叉线含义不明确。六个陷阱统一改为“八角捕兽网纹章”：双层八角绳框、八条向心网线、四个向内倒钩、中央锁结与浅赭印底。拒绝继续使用单纯 `X`、通用警告三角或额外的 `陷阱`/`阱` 文字。

## Final Tokens

| Token | Hex | Role |
| --- | --- | --- |
| `canvas` | `#EDF3EF` | 应用背景，参考图的冷白灰绿 |
| `surface` | `#F7F8F3` | 其他游戏统一侧栏浅面 |
| `board` | `#E5B85D` | 棋盘主体竹木金 |
| `boardLight` | `#F0CB7B` | 木纹高光与普通格亮面 |
| `grid` | `#5A3A12` | 深棕网格、边线和兽穴文字 |
| `river` | `#075D86` | 深蓝双河 |
| `riverLine` | `#2A7898` | 细密水纹高光 |
| `jumpLine` | `#5A3A12` | 捕兽网向心网线、倒钩和中央锁结 |
| `den` | `#6A3E12` | 兽穴文字与角标 |
| `trap` | `#5A3A12` | 捕兽网双层八角绳框 |
| `trapWash` | `#D59B3C` at 18% | 陷阱格浅赭印章底纹 |
| `greenPiece` | `#0E5A3A` | 松绿方浮雕棋子 |
| `redPiece` | `#C63A20` | 朱砂方浮雕棋子 |
| `pieceText` | `#FFF3D2` | 米白楷书兽名 |
| `ink` | `#183D30` | 侧栏主文字 |
| `muted` | `#6B6D69` | 侧栏次级文字和禁用状态 |
| `lastMove` | `#4FCBFF` at 72% | 所有游戏共享的最后移动亮蓝四角框 |
| `legalMove` | `#273E32` | 普通合法落点 |
| `captureRing` | `#F4EBD4` | 吃子目标的浅色内环 |

颜色不单独承担信息：阵营同时显示 `松绿方`、`朱砂方` 文本；河道、陷阱和兽穴同时依赖形状、位置与汉字标识。

## Typography

- Display: Android `FontFamily.Serif`，`斗兽棋` 48sp / 600，短标题 26sp / 600。
- Body: Android `FontFamily.SansSerif`，18sp / 500；次级信息 14sp / 400。
- Utility: Android `FontFamily.Monospace`，比分 38sp / 500、等级和版本 13-15sp。
- Piece label: 资源生成时使用本机可再现的 CJK 楷书字体，单字占棋子本体高度约 62%，米白色，无描边；棋子整体带统一倒角、高光和下投影。
- 地形字只保留 `兽穴`；陷阱使用八角捕兽网纹章表达，不额外叠加 `陷阱` 或 `阱` 文案。

## Layout Concept

“一张从林地中切出的双河领地板，旁边放着一页安静的对局账簿。”棋盘负责规则辨识和视觉记忆，右栏只负责状态与操作。

### Main menu

```text
┌──────────────────────────────┬──────────────────┐
│  den · traps                 │   circular logo  │
│      blue river canyon       │      斗兽棋      │
│  empty 7×9 territory board   │   版本 0.0.3     │
│      blue river canyon       │──────────────────│
│  traps · den                 │   [单人模式]     │
│                              │   [双人对战]     │
│                              │   [退出游戏]     │
└──────────────────────────────┴──────────────────┘
```

- 宽布局沿用象棋家族几何：外边距 28dp、棋盘与右栏间距 34dp；菜单右栏固定 320dp 宽并占内容高度 88%。
- 空棋盘使用接近参考图的 `1:1` 外框比例并居中；内部仍是精确 7 列 × 9 行规则网格，单元格允许横向略宽。
- 每个陷阱纹章以单格短边为基准：外八角直径约 68%，内八角约 56%，八条网线汇聚到约 7% 的中央锁结；上下左右四条网线带短小向内倒钩。纹章与网格线保持至少 12% 短边间距。
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
- 棋子透明纹理画布占单格短边约 86%，其中方形棋子本体约占 76%；圆角、金色内边、高光和下投影与参考图一致。
- 空陷阱必须在整板缩略视图中与普通竹木格明显区分；棋子进入陷阱后允许覆盖中心网结，但外围浅赭印底和八角绳框仍应从棋子四周露出。
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
| `designs/previews/doushouqi-ui-game-tablet.png` | `designs/images/doushouqi-ui-game-tablet.md` | 用户批准参考图：近方形竹木棋盘、双河、浮雕兽棋和目标侧栏层级 |

## Implementation Notes

- Primary CTA labels: 菜单 `单人模式`；终局 `重新开始`。
- Components: `DoushouqiBoard`, `DoushouqiPiece`, `DoushouqiStatusRail`, `DoushouqiScoreLine`, `DoushouqiTurnLine`, `DoushouqiActionButton`。
- Package assets: `1024×1024` circular-safe `assets/icon.png`；`1400×1400` 完整透明角棋盘 `assets/board/doushouqi-board.png`；16 张 `512×512` 透明棋子 PNG 位于 `assets/pieces/{green|red}-{animal}.png`。
- Runtime: 纹理按包根目录和 manifest 版本缓存；先读 bounds 并要求 `image/png` 与精确尺寸，失败时回退到现有 Compose 绘制。
- Preserve: 标准 7×9 规则坐标、六个陷阱坐标、双向点击映射、统一菜单文案、比分/换边/悔棋/重开、共享最近一步标记、象棋家族外层几何、包内独立版本。
- Non-goals: 在线对战、联网素材、音效、动画河流、皮肤选择、棋谱导入导出、可配置计时。

## Delivery Guardrails

- 正常文字对比至少 4.5:1，大标题和大号棋子字至少 3:1。
- 48dp 最小触控区域；相邻按钮至少 8dp；棋格的可点击区域覆盖完整格。
- 不出现动物照片、表情符号、森林剪影堆叠、玻璃拟态、霓虹描边、悬浮卡片阵列、装饰性编号，或复制参考图中与其他游戏不一致的专属侧栏。
- 每屏只保留一个视觉主操作；棋盘地形始终比右栏装饰更醒目。
- 800×600dp 横屏下仍必须读清双方兽名、河道、兽穴、陷阱、比分和当前回合。
