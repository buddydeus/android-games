# Design Brief — Android Games 亲子对弈 Logo

**Slug:** `android-games-family-versus-logo`
**User brief (verbatim summary):** “设计一个该游戏的logo，突出双人对战，亲子游戏概念”
**Stack:** Android launcher icon + Kotlin / Jetpack Compose 品牌图形
**Iteration:** 2026-07-18T11:42:10+08:00
**Status:** 用户已选定准稿并要求完全还原；`logo.svg` 与 Android launcher icon 已按同一 1254×1254 像素母版实现。
**Approved source SHA-256:** `7610f3f7206b10b2be58f399e0891fd34ef48408692fbb0ebe13a8cfca4a94f5`

> **Approved artwork override:** 用户提供的准稿图像是最终视觉权威。下方早期概念探索用于说明设计意图；任何与准稿的比例、梯形棋桌、色彩过渡或柔和阴影冲突的早期规则，均以准稿和根目录 `logo.svg` 为准。

## Base System（Step 1 — ui-ux-pro-max）

| Dimension | Content |
| --- | --- |
| Product / industry | Card & Board Game；家庭与朋友共用 Android Pad 的离线棋类游戏中心。 |
| Asset structure | 用户准稿 → 自包含根目录 `logo.svg` → Android legacy density icons → Android 8+ adaptive icon。Logo 是游戏中心母品牌，不替代五子棋、象棋、黑白棋各自的游戏徽记。 |
| Color tokens | 准稿像素聚类主色：背景 `#F9F5EB`、家长墨青约 `#18505E`、孩子枫金约 `#CF954F`、棋桌深墨约 `#13242A`、棋路瓷白约 `#FCFBF5`。 |
| Typography | 主图形不含文字。可选横向锁定组合仅使用 Android 系统中文无衬线 / Noto Sans SC 回退，标题 700；不在 launcher icon 中放汉字或英文缩写。 |
| Interaction | Logo 自身静态；作为可点击入口时沿用宿主控件 80–150ms 按压反馈，图形不变形、不位移。48dp 下主线宽至少 2.5dp，内部负形至少 3dp。 |
| Anti-patterns (avoid) | 不使用 emoji、游戏手柄、爱心、房屋、写实家庭人物、电竞闪电或 “VS” 字样；不使用棋盘照片、复杂网格、玻璃、霓虹、重 3D、纹理和细小文字；不能只靠颜色表达双方。 |

## Concept exploration

| Direction | Core idea | Trade-off |
| --- | --- | --- |
| **A. 对席棋桌（用户选定）** | 两个有明确尺度差的抽象玩家从左右面向中央；中央为宽梯形圆角棋桌、一个棋盘交叉点与两枚对局棋子。 | “两人、对战、亲子”三层语义最直接；准稿的大留白保证缩小时仍保持整体轮廓。 |
| B. 双弧对局 | 两道相向的圆弧围住中央落点，以弧长差暗示亲子。 | 轮廓最简、最像品牌章，但容易被误读为同步、链环或太极。 |
| C. 同檐开局 | 开放屋檐下，两个人物围桌落子。 | 家庭感最强，但容易滑向亲子教育或家庭服务品牌。 |

**Decision:** 采用用户提供的 A 方向准稿，不再对比例、棋桌轮廓、渐变或阴影做风格性重绘。

## Revised Direction（Step 2 — frontend-design）

### Subject grounding

- **Concrete subject:** 一位大人与一位孩子面对面坐在同一张棋桌两侧，同时把等大的棋子落向中央。
- **Audience:** 共用 Android Pad 的亲子、家人和朋友；标志不能把产品收窄成幼儿教育，也不能像电竞平台。
- **Single job:** 在 launcher 与首页标题处，一眼识别这是“适合两个人一起玩的经典棋类游戏中心”。
- **Subject language:** 圆棋子、交叉落点、面对面坐席与瓷面承载；不用完整棋盘、特定棋种或通用家庭图标。

### Memory point (signature)

**一大一小，等距落子。**

两个无性别、无五官的圆头玩家从左右向中央靠近；准稿以明显的一大一小建立亲子识别，以共享棋桌和相向落子建立双人对战关系。双方棋子在同一棋路上，强调亲子之间既是对手也是伙伴。

### Aesthetic risk

标志故意使用轻微不对称，而不是常见的镜像电竞徽章。亲子差异通过尺寸表达，公平感通过等大的棋子、对称落点和相同到桌距离重新平衡。风险集中在这一处，其他造型保持安静、圆润、扁平。

### Detemplating changes

- **Style:** 删除 ui-ux-pro-max 初始命中的玻璃拟态；最终准稿使用几何主体、低幅度明暗过渡和柔和环境阴影。为满足“完全还原”，`logo.svg` 直接内嵌获批 PNG 像素，不进行近似矢量重绘。
- **Palette:** 不采用通用科技蓝 + 活力橙。墨青来自当前首页操作色，枫木色来自实际棋盘材质，只作为较小玩家的有限暖色；瓷面与矿物灰继续承接现有主界面。
- **Structure:** 不使用盾牌、桂冠、手柄、棋盘格或文字缩写。只保留两个玩家、一个抽象棋桌交叉点与两枚棋子。
- **Family signal:** 不画大手牵小手、房屋、爱心或三口之家；使用克制的尺寸差与共同落点。
- **Versus signal:** 不用闪电、剑、火焰或 “VS”；使用左右对席与两枚等大对局棋子。
- **Texture:** 保留准稿中的轻微中心亮度、主体色阶和棋桌下方柔和阴影；不额外添加玻璃、噪点、金属或木纹。

### Rejected defaults

- 拒绝暖奶油 + 陶土 + 高反差衬线的生活方式品牌组合。
- 拒绝近黑底 + 酸性色 + 电竞徽章。
- 拒绝玻璃拟态、霓虹、强高光和厚重立体棋子；仅保留准稿已有的克制色阶。
- 拒绝通用“蓝色家长 + 橙色孩子”教育 App 造型。
- 拒绝报纸分栏、编号或任何与 Logo 无关的版式装饰。

### De-templating critique

| Question | Resolution |
| --- | --- |
| 具体主题与受众是否明确？ | 是。共享 Android Pad 的亲子、家人与朋友，使用一个离线经典棋类游戏中心。 |
| 单一任务是否明确？ | 是。识别“两个人一起下棋”的游戏中心母品牌。 |
| 色彩与造型是否来自主题？ | 是。墨青、枫木与瓷面来自现有棋具和主界面；圆棋子、交叉落点和对席来自真实对弈。 |
| 开场是否直接表达主题？ | 是。主图形本身就是两人围桌落子，不需要营销文案、统计或渐变背景。 |
| 字体是否模板化？ | 主图形无字体；可选锁定组合只用现有 Android 中文系统字，避免另造儿童字体。 |
| 结构是否含无意义编号或装饰？ | 否。每个形体都对应玩家、棋桌、交叉落点或棋子。 |
| 唯一记忆点是什么？ | “一大一小，等距落子”的不对称对席轮廓。 |
| 美学风险是否集中且可解释？ | 是。明显的人物比例差集中表达亲子，宽棋桌与共同棋路稳定整体关系。 |
| 颜色是否承担唯一语义？ | 否。全黑与反白版本仍由尺度、朝向和空间关系表达亲子与对战。 |
| 文案是否面向用户？ | 主图形无文案；横向组合只使用现有名称“游戏中心”，不添加口号。 |

## Final token table

| Token | Value | Role |
| --- | --- | --- |
| `brand.canvas` | `#F9F5EB` | 准稿主背景采样色 |
| `brand.porcelain` | `#FCFBF5` | 棋路线与空心棋子 |
| `brand.teal` | `#18505E` | 较大玩家与实心棋子 |
| `brand.ink` | `#13242A` | 梯形棋桌 |
| `brand.maple` | `#CF954F` | 较小玩家 |
| `brand.shadow` | `rgba(19, 36, 42, 0.14)` | 准稿已有的柔和环境阴影 |

### Typography

| Role | Family | Size / weight | Use |
| --- | --- | --- | --- |
| Primary mark | None | — | Logo 不含文字 |
| Optional lockup | Android system sans / Noto Sans SC fallback | 26sp / 700 | 与现有“游戏中心”标题并排 |
| Utility caption | Android system sans | 12–14sp / 500 | 仅设计说明，不进入品牌图形 |

## Geometry and construction

- 准稿画布固定为 `1254 × 1254`；根目录 `logo.svg` 使用相同 viewBox，并内嵌与获批 PNG 字节一致的图像。
- 可见主体像素边界约为 `x=179–1073`、`y=280–949`；四周原始留白、背景亮度变化和阴影均属于准稿，不得自动裁切。
- 左右玩家由圆形头部和圆端 C 形肩臂构成；不画五官、手指、衣服或性别特征。
- 中央棋桌按准稿保留上窄下宽的圆角梯形轮廓、一条横线、一条竖线和一实一空两枚棋子。
- Android density icons 和 adaptive foreground 必须从 `logo.svg` 的完整画布等比缩放，不单独移动、裁切或重排主体。
- Launcher 必须检查圆形与圆角方形系统蒙版；准稿现有大留白作为 mask-safe 区。

## Variants

1. **Approved full color:** 根目录 `logo.svg`；完整保留准稿像素。
2. **Legacy launcher:** `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` 从同一完整画布缩放。
3. **Adaptive launcher:** `mipmap-anydpi-v26` 使用同一画布作为 foreground，背景为 `#F9F5EB`。
4. **Round launcher:** 与普通 launcher 使用相同母版，由系统圆形蒙版裁切。

## Layout concept

一个横向面对面的亲子对弈轮廓：人物略有尺度差，棋子与抵达中央的距离完全相同。

```text
+----------------------------------------------------------------+
|                                                                |
|     ●                                        ○                 |
|    ╭╯          ╭──────────────╮             ╰╮                |
|   ╭╯           │    ●  +  ○   │              ╰╮              |
|  ╰╯            ╰──────────────╯               ╰╯              |
|   较大玩家             等距落子               较小玩家          |
|                                                                |
+----------------------------------------------------------------+
```

## Copy tone

- **Register:** 温和、平等、直接，不幼稚，不竞技叫嚣。
- **Primary mark:** 无文字。
- **Optional lockup:** 仅使用“游戏中心”。
- **Prohibited copy:** 不添加“亲子乐园”“家庭教育”“VS”“Battle”或英文缩写。

## Preview index

| Preview | Spec doc | Description |
| --- | --- | --- |
| `designs/previews/android-games-family-versus-logo-master.png` | `designs/images/android-games-family-versus-logo-master.md` | 单一主标志，检查人物、棋桌和等距落子语义。 |
| `designs/previews/android-games-family-versus-logo-application.png` | `designs/images/android-games-family-versus-logo-application.md` | Launcher、首页标题旁与单色小尺寸的应用场景。 |

## Implementation notes（Step 4 handoff）

- **Primary asset:** 根目录 `logo.svg`，其内嵌 PNG SHA-256 必须等于获批源图。
- **Placement:** 本轮只更新 launcher app icon；不进入三个游戏按钮，不替换现有游戏 Logo。
- **Launcher resources:** 五档 legacy density icon、adaptive foreground/background、round icon。
- **Accessibility:** 首页标题组合保留完整文本“游戏中心”；装饰性 Logo 不重复朗读，单独使用时语义标签为“游戏中心”。
- **Non-goals:** 不修改 Compose 主界面、游戏包、`game-api`、加载器或 Gradle 任务。
