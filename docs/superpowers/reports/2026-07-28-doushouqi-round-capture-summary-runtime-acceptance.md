# 斗兽棋完整回合吃子摘要运行验收

**日期：** 2026-07-28  
**结论：** 通过

## 验收范围

- 斗兽棋包版本 `0.0.6`；
- 单人模式初始空摘要与一方吃子摘要；
- 双人模式同一完整轮内双方各吃一枚的两行摘要；
- 旧标题 `最近一步吃子` 不再出现在运行侧栏；
- 完整轮发布、首步终局、机器人开局、悔棋、重开和失效请求边界的自动化回归。

## 自动化验证

运行：

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
unzip -p build/game-packages/doushouqi.zip manifest.json |
  rg '"version(Code|Name)": (6|"0.0.6")'
```

结果：

- 斗兽棋模块测试 `BUILD SUCCESSFUL`；
- 完整仓库验证 `BUILD SUCCESSFUL`，共 `178 actionable tasks`；
- `git diff --check` 无输出；
- 生成包清单包含 `"versionCode": 6` 与 `"versionName": "0.0.6"`。

会话测试覆盖：

- 单人玩家吃子在智能应答前不发布；
- 智能应答后一次性发布玩家与智能两步结果；
- 完整无吃子轮覆盖更早的非空摘要；
- 玩家首步终局立即发布；
- 智能自动开局不发布轮次；
- 双人绿方半轮保留旧摘要，红方走棋后发布完整轮；
- 双人绿方首步终局立即发布；
- 单双人悔棋恢复准确的 completed/pending 轮次边界；
- 重开清空，非法或失效请求不修改摘要。

## 运行环境

- AVD：`android_games_mvp_pad`
- 设备：`emulator-5554`
- Android API：36
- 分辨率：`2560 × 1800`
- 密度：`320 dpi`
- Activity：`com.buddygames.center/.MainActivity`

安装与启动：

```bash
npm start
```

构建、流式安装和 Activity 启动均成功。斗兽棋菜单显示 `版本 0.0.6`。

## 单人模式

初始语义树包含：

```text
玩家 : 智能
智能等级 1
当前回合：
无吃子
```

沿第 7 列推进绿鼠并吃掉红象，等待智能应答完成。最终棋盘语义确认：

```text
松绿方鼠，第3行第7列
绿方吃：象
```

侧栏没有 `最近一步吃子`、`玩家执…`、`松绿方吃…` 或 `朱砂方吃…`。

截图：

`build/runtime-acceptance/doushouqi-round-capture-summary.png`

## 双人模式

按以下完整轮序列走棋：

1. 绿鼠从第 7 行第 7 列逐轮向上推进；
2. 红猫在第 2、3 行第 6 列之间应答；
3. 最后一轮绿鼠从第 4 行第 7 列吃掉第 3 行第 7 列的红象；
4. 红猫从第 3 行第 6 列吃掉第 3 行第 7 列的绿鼠。

完整轮结束后的语义树包含：

```text
朱砂方猫，第3行第7列
绿方吃：象
红方吃：鼠
```

截图确认两行按绿方、红方顺序显示，分别使用绿、红阵营色，未挤压比分、
回合或操作区：

`build/runtime-acceptance/doushouqi-two-player-round-captures.png`

## 边界说明

Level 1 智能应答在运行设备上完成很快，人工截图无法稳定停留在思考中的
半轮。该瞬时边界由确定性 `DoushouqiSessionTest` 直接验证：玩家走棋后的
公开 completed 摘要仍为上一轮，只有匹配 generation 和源局面键的合法智能
应答才会发布 pending 摘要。

