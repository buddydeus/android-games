# 黑白棋

黑白棋是游戏中心内置的离线游戏包，提供标准 8x8 棋盘、单人机器人对战和本地双人对战。规则、会话状态、界面和素材都归本模块所有，可单独构建并更新游戏包。

[返回项目说明](../../README.md)

## 包信息

| 字段 | 值 |
| --- | --- |
| `gameId` | `othello` |
| 显示名称 | 黑白棋 |
| 当前版本 | `0.0.6` (`versionCode = 6`) |
| 入口类 | `com.buddygames.othello.OthelloPlugin` |
| 最低外壳 API | `1` |
| 屏幕方向 | 横屏 |
| 构建产物 | `build/game-packages/othello.zip` |

版本信息同时维护在 `OthelloPlugin.manifest` 和 `package/manifest.json`，开始界面显示游戏包清单中的 `versionName`。

## 当前玩法

- 使用标准 8x8 初始布局，黑方先手。
- 合法落子必须在至少一个方向夹住对方棋子，并翻转全部被夹住的棋子。
- 当前方没有合法落点时自动跳过；双方都没有合法落点时结束对局，以棋子数量判定胜负或平局。
- 开始界面统一提供 `单人模式`、`双人对战` 和 `退出游戏`。
- 每次有效落子后，以半透明亮蓝色四角框标记新放置棋子的位置。
- 单人模式显示合法落点提示；双人模式隐藏提示点。
- 横屏宽布局使用象棋参考几何：`28dp` 页面外边距、`34dp` 棋盘/侧栏间距、`300dp × 94%` 对局侧栏和 `320dp × 88%` 菜单侧栏；棋盘仍保持标准正方形。

## 单人机器人

机器人从当前合法走法中按以下顺序选择：

1. 优先占据四个角。
2. 优先选择本回合翻转棋子更多的落点。
3. 使用行列坐标保持相同局面的选择结果稳定。

玩家默认执黑先手。玩家获胜后重新开始会交换执方，机器人执黑并完成开局行动；玩家失败后恢复执黑先手；平局不改变当前执方。

## 悔棋、跳过与比分

- 单人模式一次悔棋恢复到玩家上一手之前，同时撤销期间的机器人应答和自动跳过流程。
- 双人模式一次悔棋回退一步。
- 快照同时恢复棋盘、回合、比分和最后落点。
- 胜负终局隐藏悔棋；平局终局仍保留悔棋。
- 平局不增加任一方比分。

## 素材与代码

| 路径 | 作用 |
| --- | --- |
| `package/assets/icon.png` | 游戏中心读取的 1024x1024 圆形安全区 Logo |
| `package/assets/textures/othello-shelf.png` | 游戏包自带的黑白棋陈列纹理 |
| `src/main/.../OthelloRules.kt` | 合法落子、八方向翻转、计数和机器人选择 |
| `src/main/.../OthelloSession.kt` | 跳过流程、执方交换、悔棋快照和新局状态 |
| `src/main/.../OthelloVisuals.kt` | 开始界面、棋盘、提示点、右栏和状态覆盖层 |
| `src/main/.../OthelloPlugin.kt` | `GamePlugin` 入口及对局编排 |
| `src/test/.../OthelloRulesTest.kt` | 规则、机器人、重开、悔棋和视觉常量回归测试 |

## 构建与测试

在仓库根目录执行：

```bash
./gradlew :games:othello:testDebugUnitTest
npm run build:game:othello
npm run verify
```

`npm run build:game:othello` 会生成包含 `manifest.json`、`plugin.apk` 和 `assets/` 的独立 zip。

## 更新要求

修改黑白棋规则、机器人、界面或包内素材时，需要同步递增插件清单与 `package/manifest.json` 中的 `versionCode`、`versionName`，更新对应测试和本文档。仅修改文档不递增游戏版本。通用游戏包契约见 [游戏插件开发说明](../../docs/agents/game-plugins.md)。
