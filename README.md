# Android Games

面向 Android Pad 的离线单机游戏中心。应用本体是稳定的加载外壳，游戏逻辑、界面和资源由可独立构建、导入和更新的 zip 游戏包提供。

MVP 当前包含：

- 五子棋
- 黑白棋
- 象棋

三款游戏均支持“单人模式”“双人对战”“退出游戏”，单人模式由各游戏包自行实现机器人对手。

游戏中心主程序独立维护版本，当前从 `versionCode = 1`、`versionName = 0.0.1` 开始，并在首页标题下显示。修改游戏中心的外壳功能、界面、资源或游戏包加载逻辑时，需要同步递增 [app/build.gradle.kts](app/build.gradle.kts) 中的主版本；仅更新单个游戏包时不调整游戏中心版本。

## 架构

项目采用 Kotlin、Jetpack Compose 和 Gradle 多模块结构：

| 路径 | 职责 |
| --- | --- |
| `app/` | 游戏中心外壳、游戏包安装与发现、插件加载和页面托管 |
| `game-api/` | 外壳与游戏包之间的稳定契约 |
| `games/gomoku/` | 五子棋规则、机器人、界面和包资源 |
| `games/othello/` | 黑白棋规则、机器人、界面和包资源 |
| `games/xiangqi/` | 象棋规则、机器人、界面和包资源 |
| `scripts/` | 本地调试启动脚本 |
| `docs/` | 架构设计、实施计划和游戏包开发说明 |

游戏包通过 `DexClassLoader` 加载。外壳不包含具体棋类规则，因此单个游戏的功能更新或问题修复可以通过替换对应游戏包完成。

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

安装流程会验证清单和 `plugin.apk`，拒绝低版本覆盖，并在 Android 14 及以上系统加载动态代码前将 `plugin.apk` 设为只读。当前 MVP 支持本地文件导入，不包含在线分发、自动更新和游戏包签名验证。

每款游戏独立维护版本，五子棋、黑白棋和象棋均从 `0.0.1` 开始，当前版本均为 `0.0.2`。游戏开始界面会显示该游戏包的 `versionName`。修改某款游戏的规则、机器人、界面或包内资源时，只递增该游戏的 `versionCode` 和语义化 `versionName`，并保持插件代码与 `package/manifest.json` 中的版本一致；仅修改游戏中心外壳不需要调整游戏版本。

游戏包契约、清单字段和新增游戏步骤见 [游戏插件开发说明](docs/agents/game-plugins.md)。

## 当前游戏能力

三款游戏均提供独立开始界面、单人模式、本地双人对战、对局状态、历史比分，以及右侧栏中的悔棋和重新开始操作。单人模式悔棋会回退玩家与机器人组成的一轮，双人模式悔棋回退一步；胜者产生后隐藏悔棋按钮，黑白棋和局仍保留悔棋。撤销终局时比分同步恢复，重新开始后清空本局悔棋历史。单人模式中，玩家获胜后重新开始会交换执方；玩家失败后重新开始固定恢复先手方；交换为后手方时由机器人自动完成首步。和局重新开始保持当前执方，双人模式仍由固定先手方开局。

- **五子棋**：15×15 棋盘，棋子落在交叉点；机器人依次处理立即取胜、拦截对手成五、拦截会形成双胜点的活三，再进行常规位置选择。死四、连续活三和间隔活三均有规则测试覆盖。
- **黑白棋**：支持合法落子、翻转、跳过无合法步回合和终局计分；双人模式不显示机器人辅助提示点。
- **象棋**：棋子落在棋盘交叉点，支持安全合法着法、将军、将死与胜负判断；单人 AI 优先直接胜利和强制将军，并评估对手下一步最强反击，避免为了吃小子而立即损失高价值棋子。当前回合按红方或黑方着色，将军时在右侧状态栏显示提示。

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

# 完整验证：测试、三个游戏包和 Debug APK
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
```

`pnpm run <script>` 可执行相同脚本；项目没有 npm 运行时依赖。

构建产物：

```text
app/build/outputs/apk/debug/app-debug.apk
build/game-packages/gomoku.zip
build/game-packages/othello.zip
build/game-packages/xiangqi.zip
```

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
```

修改单个游戏时，应先运行对应模块测试，再运行 `npm run verify` 完成集成验证。

## 文档

- [产品与架构设计](docs/superpowers/specs/2026-07-07-android-pad-game-center-design.md)
- [MVP 实施计划](docs/superpowers/plans/2026-07-08-android-pad-game-center-mvp.md)
- [游戏插件开发说明](docs/agents/game-plugins.md)
