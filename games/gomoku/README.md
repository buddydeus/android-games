# 五子棋

五子棋是游戏中心内置的离线游戏包，提供 15x15 交叉点棋盘、单人机器人对战和本地双人对战。规则、会话状态、界面和素材都归本模块所有，可单独构建并更新游戏包。

[返回项目说明](../../README.md)

## 包信息

| 字段 | 值 |
| --- | --- |
| `gameId` | `gomoku` |
| 显示名称 | 五子棋 |
| 当前版本 | `0.0.6` (`versionCode = 6`) |
| 入口类 | `com.buddygames.gomoku.GomokuPlugin` |
| 最低外壳 API | `1` |
| 屏幕方向 | 横屏 |
| 构建产物 | `build/game-packages/gomoku.zip` |

版本信息同时维护在 `GomokuPlugin.manifest` 和 `package/manifest.json`，开始界面显示游戏包清单中的 `versionName`。

## 当前玩法

- 15x15 棋盘，黑方先手，棋子落在网格交叉点。
- 横向、纵向或斜向连续五子即获胜；棋盘填满且无人获胜时为平局。
- 开始界面统一提供 `单人模式`、`双人对战` 和 `退出游戏`。
- 对局右栏显示比分、回合状态、悔棋，以及终局后的重新开始操作。
- 横屏宽布局使用象棋参考几何：`28dp` 页面外边距、`34dp` 棋盘/侧栏间距、`300dp × 94%` 对局侧栏和 `320dp × 88%` 菜单侧栏；棋盘仍保持正方形与原有 15 路交叉点绘制。
- 每次有效落子后，以半透明亮蓝色四角框标记最后落点；机器人应答后标记替换为机器人落点。

## 单人机器人

机器人按固定优先级选择落点：

1. 立即完成自己的五连。
2. 拦截玩家下一步成五，包括死四形成的唯一胜点。
3. 拦截落子后会产生至少两个立即胜点的连续或间隔活三威胁。
4. 按邻近棋子数量、中心距离、行列坐标进行确定性位置选择。

玩家默认执黑先手。玩家获胜后重新开始会交换执方，机器人执黑并自动落下首子；玩家失败后恢复执黑先手；平局不改变当前执方。

## 悔棋与比分

- 单人模式在玩家合法落子前保存快照，一次悔棋回退玩家落子及随后的机器人应答。
- 双人模式每次悔棋回退一步。
- 快照同时恢复棋盘、回合、比分、终局状态和最后落点。
- 产生胜者后隐藏悔棋；平局仍可重新开始。

## 素材与代码

| 路径 | 作用 |
| --- | --- |
| `package/assets/icon.png` | 游戏中心读取的 1024x1024 圆形安全区 Logo |
| `package/assets/textures/gomoku-shelf.png` | 游戏包自带的五子棋陈列纹理 |
| `src/main/.../GomokuRules.kt` | 棋盘状态、胜负判断和机器人优先级 |
| `src/main/.../GomokuSession.kt` | 比分、执方交换、悔棋快照和新局状态 |
| `src/main/.../GomokuVisuals.kt` | 开始界面、棋盘、右栏和状态覆盖层 |
| `src/main/.../GomokuPlugin.kt` | `GamePlugin` 入口及对局编排 |
| `src/test/.../GomokuRulesTest.kt` | 规则、机器人、重开、悔棋和视觉常量回归测试 |

## 构建与测试

在仓库根目录执行：

```bash
./gradlew :games:gomoku:testDebugUnitTest
npm run build:game:gomoku
npm run verify
```

`npm run build:game:gomoku` 会生成包含 `manifest.json`、`plugin.apk` 和 `assets/` 的独立 zip。

## 更新要求

修改五子棋规则、机器人、界面或包内素材时，需要同步递增插件清单与 `package/manifest.json` 中的 `versionCode`、`versionName`，更新对应测试和本文档。仅修改文档不递增游戏版本。通用游戏包契约见 [游戏插件开发说明](../../docs/agents/game-plugins.md)。
