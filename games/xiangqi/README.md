# 象棋

象棋是游戏中心内置的离线游戏包，提供完整中国象棋规则、1-10 级本地搜索 AI、单人机器人对战和本地双人对战。规则、搜索、会话状态、界面与陶瓷素材全部归本模块所有。

[返回项目说明](../../README.md)

## 包信息

| 字段 | 值 |
| --- | --- |
| `gameId` | `xiangqi` |
| 显示名称 | 象棋 |
| 当前版本 | `0.0.13` (`versionCode = 13`) |
| 入口类 | `com.buddygames.xiangqi.XiangqiPlugin` |
| 最低外壳 API | `1` |
| 屏幕方向 | 横屏 |
| 构建产物 | `build/game-packages/xiangqi.zip` |

版本信息同时维护在 `XiangqiPlugin.manifest` 和 `package/manifest.json`，开始界面显示游戏包清单中的 `versionName`。

## 当前玩法

- 棋子落在 9x10 棋盘交叉点，覆盖车、马、象、士、将/帅、炮和兵/卒的合法走法。
- 合法走法会过滤导致己方将帅受攻击的移动，并处理将帅照面、将军、吃将与将死胜负。
- 当前回合在右栏按红方或黑方着色；出现将军时显示 `将军`。
- 单人玩家执黑时棋盘旋转 180 度，使黑方区域位于下方，棋子文字保持正向。
- 最后移动的目标交叉点使用半透明亮蓝色四角框标记。
- 和棋是独立终局状态，不改变比分、下一局执方或智能等级。

## 单人 AI

单人智能等级等于玩家累计胜局数加一，上限为 10 级。搜索在 Compose UI 线程之外执行，并按等级逐步增加深度、节点预算、思考时间、评估项和静态搜索范围。

核心实现包括：

- 迭代加深 Negamax 与 Alpha-Beta 剪枝。
- 原地 make/unmake 搜索局面和安全合法走法过滤。
- 置换表、缓存走法排序和有效深度统计。
- 低等级确定性弱化，高等级位置评估与有限静态搜索。
- 优先直接吃将、将死和强制将军，避免无意义或立即亏损的吃子。

玩家默认执红先手。玩家获胜后重新开始会交换执方；玩家失败后恢复执红先手；和棋保持当前执方。玩家成为黑方时，红方机器人先行，棋盘随后以黑方视角显示。

## 悔棋与比分

- 单人模式一次悔棋回退玩家移动及随后的机器人应答。
- 双人模式一次悔棋回退一步。
- 快照同时恢复棋盘、回合、比分、终局结果和最后移动。
- 产生胜者后隐藏悔棋；和棋保留悔棋。
- 单人比分按玩家与机器人身份记录，双人比分按红方与黑方记录。

## 棋盘与棋子素材

- `package/assets/board/xiangqi-board.png` 是完整的 1600x1500 RGBA 青瓷棋盘，包含网格、九宫线、楚河汉界和边框。
- `package/assets/pieces/` 包含红黑双方 14 张独立的 1024x1024 透明陶瓷棋子 PNG。
- 棋子使用同一个 `tools/source/ceramic-piece-master.png` 母版，保留双金边、釉面高光、立体暗部和柔和投影。
- 运行时棋子直径为单格的 80%，棋盘网格注册坐标为 `128/110/1472/1360`。
- 素材缺失或解码失败时，界面保留 Compose 回退绘制。

使用 Python 3 与 Pillow 从母版重新生成棋盘和棋子：

```bash
python3 games/xiangqi/tools/generate_xiangqi_assets.py
```

视觉规范见 [象棋 UI 设计](../../designs/specs/xiangqi-ui.md)。

## 代码结构

| 路径 | 作用 |
| --- | --- |
| `XiangqiRules.kt` | 棋盘状态、合法走法、将军与胜负判断 |
| `XiangqiAi.kt` | 1-10 级配置和 AI 入口 |
| `XiangqiSearchPosition.kt` | 原地 make/unmake 搜索局面 |
| `XiangqiSearchEngine.kt` | 迭代加深、剪枝、排序、置换表和静态搜索 |
| `XiangqiSession.kt` | 比分、执方、和棋、悔棋快照和新局状态 |
| `XiangqiPieceTextures.kt` | 包内棋盘与棋子贴图加载、裁边和注册常量 |
| `XiangqiVisuals.kt` | 开始界面、棋盘、右栏和状态覆盖层 |
| `XiangqiPlugin.kt` | `GamePlugin` 入口、异步机器人和对局编排 |

## 构建与测试

在仓库根目录执行：

```bash
./gradlew :games:xiangqi:testDebugUnitTest
npm run build:game:xiangqi
npm run verify
```

AI 相邻等级校准是显式启用的长测试，一次运行一组等级：

```bash
./gradlew :games:xiangqi:testDebugUnitTest \
  --tests com.buddygames.xiangqi.XiangqiAiCalibrationTest \
  -PxiangqiCalibration=true \
  -PxiangqiCalibrationPair=1
```

`xiangqiCalibrationPair` 支持 `1` 到 `9`，分别比较相邻等级。智能梯度与校准约束见 [象棋智能梯度设计](../../docs/superpowers/specs/2026-07-18-xiangqi-intelligence-gradient-design.md)。

## 更新要求

修改象棋规则、AI、界面或包内素材时，需要同步递增插件清单与 `package/manifest.json` 中的 `versionCode`、`versionName`，更新对应测试和本文档。素材变更必须通过生成器复现。仅修改文档不递增游戏版本。通用游戏包契约见 [游戏插件开发说明](../../docs/agents/game-plugins.md)。
