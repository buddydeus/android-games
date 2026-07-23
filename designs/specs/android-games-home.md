# Design Brief — Android Games 轻质感主界面

**Slug:** `android-games-home`
**User brief (verbatim summary):** “增加一点质感”，同时延续简单主界面、等尺寸游戏按钮、仅使用游戏特征 Logo 的要求；游戏选择区采用 4 列网格，尾行不足 4 个时不居中。
**Stack:** Kotlin、Jetpack Compose、Material 3（Android Pad，离线）
**Iteration:** 2026-07-16T00:00:00+08:00

## Base System（Step 1 — ui-ux-pro-max）

| Dimension | Content |
| --- | --- |
| Product / industry | Card & Board Game；家庭共享的离线 Android Pad 棋类选择器。 |
| Page structure | Android Top App Bar → “选择游戏” → 已安装游戏的同尺寸按钮。首页不增加说明区、推荐区或导航区。 |
| Color tokens | 通用检索仍偏暗色游戏大厅；补充检索选中高对比 `Soft UI Evolution`：浅色表面、可见边框、克制双层阴影、明确焦点。 |
| Typography | Android 系统中文无衬线 / Noto Sans SC 回退；游戏名 24sp、700；包内文本 Logo 按字符数自适应。 |
| Interaction | 触控目标至少 48dp；按下反馈 80–150ms 内出现；焦点环 3dp；按钮深度变化不能引起布局位移。 |
| Anti-patterns (avoid) | 不使用低对比完整新拟态、玻璃模糊、厚重 3D、木纹、棋盘图片、游戏截图、大小不同的卡片、霓虹、渐变光效、排行榜或状态徽章。 |

## Revised Direction（Step 2 — frontend-design）

### Subject grounding

- **Concrete subject:** 一组由游戏包提供徽记的实体启动键，而不是棋盘展柜。
- **Audience:** 共用 Android Pad 的家人与朋友；远距离也能看清按钮边界和游戏名。
- **Single page job:** 触摸一个同等重要的按钮进入游戏。
- **Subject language:** 棋子圆润、厚实、可触摸的感觉被转译成哑光瓷面和轻微内嵌，而不是复制棋盘材质。

### Memory point (signature)

**哑光瓷面棋键。** 已安装游戏使用完全同规格的按钮，像一组平静的硬件按键；每个按钮中央有同尺寸的浅内嵌圆槽，槽内展示游戏包清单 `icon` 指向的 Logo。

### Aesthetic risk

质感只由光线、边缘和阴影表达，不用不同按钮底色、不同尺寸或装饰图案。按钮保持近乎单色，要求细微层次足够清楚但不能滑向低对比新拟态。

### Detemplating changes

- **Palette:** 延续冷灰矿物背景与墨青操作色，按钮改为偏冷的瓷白，而非暖奶油色。
- **Surface:** 从纯平面白卡升级为 1dp 深边、1dp 顶部亮边、接触阴影和环境阴影组成的哑光瓷面。
- **Logo:** 每个游戏包提供自己的 Logo，壳层只负责统一的浅内嵌槽和图片或文本渲染。
- **Layout:** 所有按钮严格同宽同高，质感不能改变视觉权重；排列按成功启动频率动态调整。
- **Copy:** 仍只保留“游戏中心”“导入游戏包”“选择游戏”和清单提供的游戏名。

### Rejected defaults

- 拒绝暖奶油、陶土色和高反差衬线组合。
- 拒绝近黑背景、酸性色和游戏大厅光效。
- 拒绝报纸式分栏和装饰编号。
- 拒绝完整新拟态：不允许边界消失、文字灰度过低或靠阴影猜测可点击区域。
- 拒绝 Claymorphism：不使用糖果色、果冻形变和夸张圆角。

### De-templating critique

| Question | Resolution |
| --- | --- |
| 具体主题与受众是否明确？ | 是。家庭 Android Pad 上已安装离线游戏的同级启动键。 |
| 页面是否只有一个主要工作？ | 是。选择并打开游戏。 |
| 质感是否来自主题？ | 是。使用棋子般的圆润、厚实与内嵌触感，不复制棋盘。 |
| 首屏是否直接表达产品？ | 是。主体只有已安装游戏按钮。 |
| 字体是否适合中文 Android？ | 是。系统无衬线承担界面文字与包内文本 Logo。 |
| 结构是否存在无意义装饰？ | 否。没有元数据、编号、卡片标签或二级面板。 |
| 唯一记忆点是什么？ | 一组同规格的哑光瓷面棋键。 |
| 美学风险是否集中且可解释？ | 是。只用微弱光影表达质感，并用边框保证可访问性。 |
| 文案是否面向用户？ | 是。没有包、插件或加载器等实现词。 |
| 错误与空状态是否可恢复？ | 是。消息条说明原因；无游戏时邀请导入。 |

## Final token table

| Token | Value | Role |
| --- | --- | --- |
| `surface.canvas` | `#DDE5E3` | 浅冷灰矿物背景 |
| `surface.buttonTop` | `#FCFDFB` | 按钮上部瓷面色 |
| `surface.buttonBottom` | `#F0F4F1` | 按钮下部瓷面色 |
| `surface.logoWell` | `#E5ECE9` | Logo 浅内嵌槽 |
| `surface.pressed` | `#D8E3E0` | 按下状态 |
| `outline.default` | `#AEBDB9` | 1dp 清晰边框 |
| `rim.highlight` | `#FFFFFF` | 顶部与左侧细亮边 |
| `ink.primary` | `#17282C` | 标题、游戏名、深色 Logo |
| `ink.muted` | `#56686C` | 次要文案 |
| `action.primary` | `#185864` | 导入操作 |
| `focus.ring` | `#08758A` | 3dp 可见焦点环 |
| `piece.white` | `#F5F2E9` | 白棋圆片，配深色描边 |
| `piece.black` | `#242A2C` | 黑棋圆片 |

### Feedback tokens

| Token | Value | Role |
| --- | --- | --- |
| `feedback.success` / `feedback.successSoft` | `#2E715E` / `#E0EEE7` | 导入成功消息，文字对比度 4.82:1 |
| `feedback.warning` / `feedback.warningSoft` | `#98551F` / `#F4EADB` | 未选择文件消息，文字对比度 4.83:1 |
| `feedback.error` / `feedback.errorSoft` | `#A43B32` / `#F1DDDD` | 导入或加载失败消息，文字对比度 4.96:1 |

### Elevation tokens

| Token | Value | Use |
| --- | --- | --- |
| `shadow.contact` | `0 3dp 7dp rgba(47, 66, 67, 0.16)` | 按钮贴近背景的接触阴影 |
| `shadow.ambient` | `0 12dp 28dp rgba(47, 66, 67, 0.10)` | 柔和环境阴影 |
| `shadow.pressed` | `0 2dp 4dp rgba(47, 66, 67, 0.12)` | 按下时减弱深度 |
| `canvas.textureAlpha` | `0.05–0.07` | 仅背景使用现有矿物纹理，不能进入按钮 |

### Typography

| Role | Family | Size / weight | Use |
| --- | --- | --- | --- |
| App title | Android system sans / Noto Sans SC fallback | 26sp / 700 | “游戏中心” |
| Section title | Android system sans | 18sp / 600 | “选择游戏” |
| Game name | Android system sans | 24sp / 700 | 游戏包清单名称 |
| Utility action | Android system sans | 16sp / 600 | “导入游戏包” |
| Logo text | Android system sans | 16–54sp / 700 | 游戏包提供的文本 Logo |

## Surface and Logo construction

- 所有按钮在同一视图内必须同宽、同高、同圆角、同边框、同阴影。
- 按钮使用两档非常接近的瓷白色形成轻微纵向明度差；禁止彩色或发光渐变。
- 外层 1dp `outline.default`；顶部和左侧可增加 1dp、低透明度 `rim.highlight`。
- Logo 槽为 112dp 圆形、比按钮深一档；使用清晰边框与极浅内阴影感，不做真实凹洞。
- Logo 内容限制在圆槽内部，使用 `ContentScale.Fit` 保持完整，不裁切。
- Logo 和名称分别来自游戏包清单的 `icon` 与 `displayName`，壳层禁止根据 `gameId` 绘制、命名或排序。
- `icon` 支持 PNG、WebP、JPEG 和 UTF-8 文本；缺失或损坏时使用 `displayName` 首字符回退。
- 背景可低透明度复用 `textures/mineral-slate.png`；按钮、Logo 槽和文字上禁止纹理。

## Layout concept

一个简洁的离线游戏启动面板，已安装游戏按钮像同一套哑光瓷面硬件键；结构保持稳定，只由包元数据填充内容。

```text
LANDSCAPE TABLET · 16:10
+------------------------------------------------------------------------------+
| 游戏中心                                                   [导入游戏包]       |
+------------------------------------------------------------------------------+
|                            选择游戏                                          |
|                                                                              |
| ╭──────────────╮ ╭──────────────╮ ╭──────────────╮ ╭──────────────╮         |
| │  (包 Logo)   │ │  (包 Logo)   │ │  (包 Logo)   │ │  (包 Logo)   │         |
| │  displayName │ │  displayName │ │  displayName │ │  displayName │         |
| ╰──────────────╯ ╰──────────────╯ ╰──────────────╯ ╰──────────────╯         |
|    equal square     equal square     equal square     equal square            |
| ╭──────────────╮                                                             |
| │  (包 Logo)   │       incomplete row starts at column one                    |
| │  displayName │                                                             |
| ╰──────────────╯                                                             |
+------------------------------------------------------------------------------+

PORTRAIT / COMPACT
+--------------------------------------+
| 游戏中心                    [导入]   |
| 选择游戏                             |
| ╭──────────────────────────────────╮ |
| │ [包 Logo]         displayName   │ |  112dp
| ╰──────────────────────────────────╯ |
| ╭──────────────────────────────────╮ |
| │ [包 Logo]         displayName   │ |  112dp
| ╰──────────────────────────────────╯ |
| ╭──────────────────────────────────╮ |
| │ [包 Logo]         displayName   │ |  112dp
| ╰──────────────────────────────────╯ |
| ╭──────────────────────────────────╮ |
| │ [包 Logo]         displayName   │ |  112dp
| ╰──────────────────────────────────╯ |
+--------------------------------------+
```

### Size and responsive rules

- `>= 1108dp` 横屏：固定四列，按钮均为 `240dp × 240dp`，间距 `28dp`。
- `600–1107dp` 横屏：固定四列，按钮等分可用宽度，统一高宽比 1:1，最大 240dp。
- 横屏尾行不足四个按钮时，按钮从第一列开始占位，剩余列保持为空，不得将尾行按钮居中。
- `< 600dp` 或竖屏：按钮均为全宽 `112dp` 高；Logo 槽统一为 64dp。
- 不允许某个按钮单独加深阴影、改变底色、抬高位置或放大 Logo。
- 圆角统一 20dp；按钮间距不少于 16dp。

## Interaction and motion

- 整个按钮为单一语义目标：“打开 {displayName}”。
- 每次插件成功加载后，该游戏的本地使用次数加一；返回首页时按次数降序显示，同次数按 `displayName`、`gameId` 排序。
- 按下时统一缩放至 `0.985`，瓷面变为 `surface.pressed`，环境阴影消失，仅保留 `shadow.pressed`；120–150ms。
- 松开时 160–200ms 恢复；不使用弹性过冲。
- 焦点使用 3dp `focus.ring`，不能只靠阴影表达。
- 开启减少动态效果时取消缩放，只切换表面色、边框和阴影。

## Copy tone

- **Register:** 简短、安静、直接。
- **Visible vocabulary:** 游戏中心、选择游戏、导入游戏包，以及游戏包提供的 `displayName`。
- **Empty state:** “还没有游戏。导入本地游戏包开始。”
- **Error state:** “导入失败：请选择有效的 .zip 游戏包后重试。”

## Preview index

| Preview | Spec doc | Description |
| --- | --- | --- |
| `designs/previews/android-games-home-desktop.png` | `designs/images/android-games-home-desktop.md` | 原三游戏横屏质感参考；当前实现为固定四列、尾行从第一列开始的同规格方形按钮。 |
| `designs/previews/android-games-home-mobile.png` | `designs/images/android-games-home-mobile.md` | 原三游戏竖屏质感参考；当前紧凑实现为单列同规格横向按钮。 |

## Implementation notes（Step 4 handoff）

- **Primary actions:** 已安装游戏的同级按钮。
- **Secondary action:** `导入游戏包`。
- **Components:** `HomeTopBar` → `GameSelectionGrid` → `GameSelectionButton` → `GameLogoWell` → `GameLogo` → `ImportMessageBar`。
- **Asset rule:** 首页不加载各游戏的棋盘纹理；仅加载清单 `icon` 指向的包内 Logo。现有矿物纹理仅可低透明度用于页面背景。
- **Non-goals:** 不改 `game-api`、加载器或包仓库；不新增依赖；不增加游戏状态、排行或联网信息。
