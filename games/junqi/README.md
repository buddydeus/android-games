# 军棋

军棋模块是游戏中心中的二国暗棋包基础，当前实现 12x5 棋盘、合法布阵、完整碰子与终局规则、观察者受限知识，以及不读取敌方真实军衔的离线 1-10 级 AI。单人和双人会话界面、包内素材及内置包集成由后续任务完成。

[返回项目说明](../../README.md)

## 包信息

| 字段 | 值 |
| --- | --- |
| `gameId` | `junqi` |
| 显示名称 | 军棋 |
| 当前版本 | `0.0.2` (`versionCode = 2`) |
| 入口类 | `com.buddygames.junqi.JunqiPlugin` |
| 最低外壳 API | `1` |
| 屏幕方向 | 横屏 |

版本信息同时维护在 `JunqiManifest.kt` 和 `package/manifest.json`。军棋尚未加入当前四个内置游戏包的构建门禁。

## 当前规则

- 棋盘包含 60 个节点、行营、大本营、公路和铁路；工兵可以沿无阻挡铁路转弯，其他可移动棋子只能直行。
- 默认、种子随机和交换布阵均保持 25 枚唯一棋子、军旗、地雷和炸弹的位置约束。
- 碰子覆盖普通军阶、同归于尽、炸弹、地雷、工兵和军旗；司令移除后永久公开己方军旗。
- 终局优先级为夺旗、完成第 31 个连续无碰子半回合的一方判负、下一方无合法移动判负，以及双方均不可移动时和棋。

## 隐私与 AI

- 对手观察只包含不编码军衔的稳定 ID、位置、是否移动和公开约束；默认与随机布阵在相同位置使用相同的等级无关 ID。
- `JunqiAi.chooseMove` 只接收 `JunqiObservation`、`JunqiKnowledge` 和 `JunqiAiLevel`，不能接收 `JunqiState` 或原始敌方棋子。
- 确定化只分配当前存活身份，并与最新观察中的移动和明旗事实相交；全局采样不超过初始棋子容量。
- 明旗防守只有在一致性样本的结果局面中真正消除即时夺旗威胁时才获得战术优先级。
- 炸弹交换依据完整容量一致样本中的高阶占比，而不是局部候选集合的伪后验概率。
- 搜索从任何预处理开始前计算期限，在采样、合法走法、评估和 `applyMove` 前后检查时间；节点或时间任一预算耗尽时返回已完成结果。

## 构建与测试

在仓库根目录执行：

```bash
./gradlew :games:junqi:testDebugUnitTest
npm run verify
```

Task 4 重点测试位于 `JunqiDeploymentTest`、`JunqiObservationTest` 和 `JunqiAiTest`，覆盖部署身份保密、真实随机布阵公平性、捕获与明旗后的确定化、战术安全、全局容量估计及确定性时间预算。

## 更新要求

修改军棋规则、AI、界面或包内素材时，同步递增 `JunqiManifest.kt` 与 `package/manifest.json` 中的 `versionCode`、`versionName`，并更新对应测试、本文档和根 `AGENTS.md`。通用游戏包契约见 [游戏插件开发说明](../../docs/agents/game-plugins.md)，批准设计见 [军棋游戏设计](../../docs/superpowers/specs/2026-07-21-junqi-game-design.md)。
