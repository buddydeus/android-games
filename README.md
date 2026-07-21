# Android Games

面向 Android Pad 的离线单机游戏中心。应用本体是稳定的加载外壳，游戏逻辑、界面和资源由可独立构建、导入和更新的 zip 游戏包提供。

当前内置：

- 五子棋
- 黑白棋
- 象棋
- 国际象棋

四款游戏均支持“单人模式”“双人对战”“退出游戏”，单人模式由各游戏包自行实现机器人对手。

游戏中心主程序独立维护版本，当前为 `versionCode = 3`、`versionName = 0.0.3`，并在首页标题下显示。修改游戏中心的外壳功能、界面、资源或游戏包加载逻辑时，需要同步递增 [app/build.gradle.kts](app/build.gradle.kts) 中的主版本；仅更新单个游戏包时不调整游戏中心版本。

## 架构

项目采用 Kotlin、Jetpack Compose 和 Gradle 多模块结构：

| 路径 | 职责 |
| --- | --- |
| `app/` | 游戏中心外壳、游戏包安装与发现、插件加载和页面托管 |
| `game-api/` | 外壳与游戏包之间的稳定契约 |
| `games/gomoku/` | 五子棋规则、机器人、界面和包资源 |
| `games/othello/` | 黑白棋规则、机器人、界面和包资源 |
| `games/xiangqi/` | 象棋规则、机器人、界面和包资源 |
| `games/chess/` | 国际象棋规则、1–10 级搜索 AI、界面和包资源 |
| `scripts/` | 本地调试启动脚本 |
| `docs/` | 架构设计、实施计划和游戏包开发说明 |

游戏包通过 `DexClassLoader` 加载。外壳不包含具体棋类规则，也不硬编码游戏名称、Logo 或排列序号：名称来自清单的 `displayName`，Logo 来自清单 `icon` 指向的包内资源，首页按各游戏成功启动次数从高到低排列。相同次数按名称和 `gameId` 保持稳定顺序。因此单个游戏的功能更新或问题修复可以通过替换对应游戏包完成，导入兼容 zip 后无需升级游戏中心。

## 游戏包

每个构建后的游戏包都是 zip 文件，顶层结构如下：

```text
<gameId>.zip
├── manifest.json
├── plugin.apk
└── assets/
```

导入后，外壳将游戏解压到应用内部目录：

```text
files/
└── Games/
    └── <gameId>/
        ├── manifest.json
        ├── plugin.apk
        └── assets/
```

安装流程会验证清单和 `plugin.apk`，拒绝低版本覆盖，并在 Android 14 及以上系统加载动态代码前将 `plugin.apk` 设为只读。`icon` 可指向包内 PNG、WebP、JPEG 或 UTF-8 文本文件；图标缺失或无法读取时，首页使用 `displayName` 的首字符作为回退。四款内置游戏均将独立的 1024×1024 圆形 PNG Logo 放在各自包内的 `assets/icon.png`，可随单个游戏包独立更新。当前 MVP 支持本地文件导入，不包含在线分发、自动更新和游戏包签名验证。

每款游戏独立维护版本并从 `0.0.1` 开始；当前五子棋和黑白棋为 `0.0.5`，国际象棋为 `0.0.9`，象棋为 `0.0.13`。游戏开始界面会显示该游戏包的 `versionName`。修改某款游戏的规则、机器人、界面或包内资源时，只递增该游戏的 `versionCode` 和语义化 `versionName`，并保持插件代码与 `package/manifest.json` 中的版本一致；仅修改游戏中心外壳不需要调整游戏版本。

游戏包契约、清单字段和新增游戏步骤见 [游戏插件开发说明](docs/agents/game-plugins.md)。

## 当前游戏能力

四款游戏均提供独立开始界面、单人模式、本地双人对战、对局状态、历史比分，以及右侧栏中的悔棋和重新开始操作。每次有效行动后，棋盘使用与棋子保持明显间隔的半透明亮蓝色四角框标记最后落点；标记限制在当前单元格内且不遮挡棋子中心。单人模式中机器人响应后标记机器人落点，交换为后手方的新局会标记机器人的自动首步。单人模式悔棋会回退玩家与机器人组成的一轮，双人模式悔棋回退一步，并同步恢复此前的最后落点；胜者产生后隐藏悔棋按钮，和局仍保留悔棋。撤销终局时比分同步恢复，重新开始后清空本局悔棋历史。单人模式中，玩家获胜后重新开始会交换执方；玩家失败后重新开始固定恢复先手方。和局重新开始保持当前执方，双人模式仍由固定先手方开局。

- **五子棋**：15×15 棋盘，棋子落在交叉点；机器人依次处理立即取胜、拦截对手成五、拦截会形成双胜点的活三，再进行常规位置选择。死四、连续活三和间隔活三均有规则测试覆盖。
- **黑白棋**：支持合法落子、翻转、跳过无合法步回合和终局计分；双人模式不显示机器人辅助提示点。
- **象棋**：棋子落在棋盘交叉点，支持安全合法着法、将军、将死与胜负判断；单人 AI 优先直接胜利和强制将军，并评估对手下一步最强反击，避免为了吃小子而立即损失高价值棋子。单人玩家执黑时棋盘按 180° 视角映射，让黑方区域位于下方，同时保持棋子文字正向；当前回合按红方或黑方着色，将军时在右侧状态栏显示提示。和棋作为独立终局结果，不改变比分、下一回合执方或单人智能等级。界面采用明亮简洁的“晴光瓷局”设计：游戏包携带一张完整的 1600×1500 横向薄框青瓷棋盘 PNG，以及红黑双方 14 种独立的 1024×1024 透明陶瓷棋子 PNG；棋子保留双金边、釉面高光、立体暗部和柔和投影，按单格 `80%` 显示，并通过内收的上下网格边界为底线棋子保留清晰留白。运行时按与 PNG 完全一致的固定网格坐标加载，素材缺失时回退到 Compose 绘制。所有棋子由同一母版 `games/xiangqi/tools/source/ceramic-piece-master.png` 生成，再叠加准确的本地繁体字形。
- **国际象棋**：完整支持王车易位、吃过路兵、可选择后/车/象/马的升变、将军与将死，以及逼和、三次重复、五十回合和子力不足和棋。单人 AI 使用纯 Kotlin 的迭代加深 Negamax、Alpha-Beta 剪枝、置换表、走法排序和静态搜索，按玩家累计胜局映射到 1–10 级并在设备上离线运行；搜索会继承本局重复历史并保持将死优先于自动和棋。所有和棋都不改变比分、下一回合执方或单人智能等级。玩家每次落子与吃子均按机器人应答后的当前棋局重新解析合法走法。玩家执黑时棋盘旋转 180°，棋子保持正向。游戏包在 `assets/pieces/` 中携带黑白双方共 12 枚 1024×1024 透明 PNG 棋子贴图，菜单、棋盘和升变选择均从当前安装的游戏包加载并缓存这些贴图；单张素材缺失或损坏时自动回退到 Unicode 棋子。

## 环境要求

- JDK：满足 Android Gradle Plugin 9.2.1 的运行要求
- Android SDK：API 36
- Android SDK Build Tools：36.0.0
- Node.js：可选，仅用于调用根目录的 npm 脚本，不安装运行时依赖
- Android 模拟器：默认使用 `android_games_mvp_pad`

通过环境变量或 `local.properties` 配置 SDK：

```properties
sdk.dir=/path/to/Android/sdk
```

也可以设置：

```bash
export ANDROID_HOME=/path/to/Android/sdk
```

## 常用命令

所有命令均在项目根目录执行。

```bash
# 运行全部单元测试
npm run test

# 完整验证：测试、四个游戏包和 Debug APK
npm run verify

# 构建 Debug APK 和全部游戏包
npm run build

# 仅构建 Debug APK
npm run build:apk

# 构建全部游戏包
npm run build:game

# 分别构建单个游戏包
npm run build:game:gomoku
npm run build:game:othello
npm run build:game:xiangqi
npm run build:game:chess
```

`pnpm run <script>` 可执行相同脚本；项目没有 npm 运行时依赖。

构建产物：

```text
app/build/outputs/apk/debug/app-debug.apk
build/game-packages/gomoku.zip
build/game-packages/othello.zip
build/game-packages/xiangqi.zip
build/game-packages/chess.zip
```

## 连接 USB 设备

Android 设备开启 USB 调试并接入电脑后，先列出 ADB 当前发现的全部设备：

```bash
pnpm connect list
```

列表会保留 `device`、`unauthorized`、`offline` 等状态。使用完整 serial 精确选择并验证一个在线设备：

```bash
pnpm connect <serial-id>
```

USB 传输由 ADB 自动建立，因此该命令使用 serial 选择设备，不会调用面向网络地址的 `adb connect`。如果状态为 `unauthorized`，请解锁设备并接受 USB 调试 RSA 授权提示后重试。

## 启动调试

```bash
npm start
```

启动脚本会：

1. 检查 Android SDK 工具。
2. 使用已有在线设备，或启动默认模拟器。
3. 在系统镜像存在时自动创建缺失的 AVD。
4. 等待 Android 启动完成。
5. 构建并安装 Debug APK。
6. 启动 `com.buddygames.center/.MainActivity`。

可选环境变量：

| 变量 | 作用 |
| --- | --- |
| `ANDROID_GAMES_AVD` | 覆盖默认 AVD 名称 |
| `HEADLESS=1` | 无窗口启动模拟器 |
| `DETACH_EMULATOR=1` | 应用启动后立即返回终端 |

模拟器日志位于 `build/logs/emulator-<AVD_NAME>.log`。

若默认 AVD 不存在，先安装 Android 36 x86_64 系统镜像：

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "system-images;android-36;google_apis;x86_64"
```

## 模块测试

```bash
./gradlew :game-api:testDebugUnitTest
./gradlew :app:testDebugUnitTest
./gradlew :games:gomoku:testDebugUnitTest
./gradlew :games:othello:testDebugUnitTest
./gradlew :games:xiangqi:testDebugUnitTest
./gradlew :games:chess:testDebugUnitTest
```

修改单个游戏时，应先运行对应模块测试，再运行 `npm run verify` 完成集成验证。

## 文档

- [产品与架构设计](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md)
- [MVP 实施计划](docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md)
- [游戏插件开发说明](docs/agents/game-plugins.md)
- [国际象棋设计](docs/superpowers/specs/2026-07-18-international-chess-game-design.md)
- [国际象棋实施计划](docs/superpowers/plans/2026-07-18-international-chess-game.md)
