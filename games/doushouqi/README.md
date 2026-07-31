# 斗兽棋

`doushouqi` 是游戏中心的第六个内置独立游戏包，版本为 `0.0.8`。规则、AI、会话、Compose 界面和包内资源都归本模块所有。

当前已完成标准 7×9 不可变棋盘与基础合法着法：

- 松绿方先行，双方各有象、狮、虎、豹、狼、狗、猫、鼠八枚棋子。
- 普通棋子每次正交移动一格；双方棋子都可进入、停留和离开己方兽穴，且不会因此获胜。
- 只有鼠可进入河道；狮虎可跨越完整河段，河内任一方的鼠都会阻挡跳跃。
- 同级或高阶可吃低阶；陆地鼠可吃象，象不能吃鼠。
- 鼠只能在同为陆地或同为河道时互吃，不能跨河岸边界吃子。
- 进入对手陷阱的棋子在停留期间按零级防守；离开陷阱攻击时恢复原兽阶。
- 每次合法着法都会生成独立新状态并更新回合、最后一步、静默步数和重复局面次数。
- 终局顺序固定为进入敌方兽穴、吃光对方、令下一方无合法着法、三次重复、连续 100 个半回合未吃子。
- 获胜条件优先于同一步触发的自动和棋。
- 单人机器人提供由玩家累计胜分驱动的 1–10 级强度，使用确定性的迭代加深 alpha-beta 搜索；递归节点通过原始数组棋盘 make/unmake 往返，不为每个后继复制完整公开状态。
- 每级集中定义深度、节点和时间预算；1–5 级才进行确定性弱化，6–10 级始终采用最佳搜索着法。
- 搜索优先处理立即入穴、吃光对手、鼠吃象和迫近兽穴等战术，并在预算耗尽时返回确定性的合法回退着法。
- 搜索在扩展节点前检查协程取消、节点和时间预算；取消时同样返回预先选定的合法回退着法，退出页面不会继续占用搜索线程。
- 增量 Zobrist 棋盘键、重复局面上下文和静默步数共同参与有界置换键，避免复用路径相关的错误和棋评分。
- 单人比分始终按玩家与机器人身份记录；玩家胜局后的下一轮交换执方，玩家负局恢复松绿先手，和棋保持当前执方与比分。
- 单人悔棋回到玩家行动前并一并撤销机器人响应；双人悔棋仅回退一步。终局比分、重复次数、静默步数和最后一步都随快照恢复。
- 单人模式按“玩家走棋 + 智能应答”汇总上一完整轮的吃子，最多显示两行 `绿方吃：<兽名>` / `红方吃：<兽名>`；智能应答完成或首步直接终局时发布，整轮无人吃子显示 `无吃子`。双人模式不等待双方合并，每次合法走棋都立即替换摘要：吃子显示该步捕获方与一枚棋子，普通走棋立即显示 `无吃子`。悔棋恢复对应快照，重新开始清空。
- 后手玩家的新轮由机器人自动开局，开局不进入悔棋历史；后台结果必须同时匹配 generation 与源局面键才可应用。
- 单人绛红方视角将 9×7 坐标双向旋转 180 度，双人模式和松绿方视角保持模型方向。
- Compose 界面使用包内 1400×1400 透明角竹木棋盘，以及松绿/朱砂双方共 16 张 512×512 透明浮雕棋子；资源由 `tools/generate_doushouqi_assets.py` 可再现生成。
- 运行时按包目录与版本缓存纹理，完整解码前先校验 `image/png` 和精确尺寸；任一棋盘或棋子资源缺失、损坏时只回退对应的 Compose 绘制。
- 透明 7×9 交互层继续负责选中框、合法点、吃子断环、读屏语义和共享亮蓝最后一步标记，因此贴图不改变规则、点击或悔棋行为。
- 宽屏沿用 28dp 外边距、34dp 间距与 300dp/320dp 侧栏；菜单侧栏将透明 Logo、标题和版本置于上区，三项操作置于下区。对局侧栏按比分、回合/结果、操作分为三段并使用两条分隔线。单人侧栏的当前回合和终局阵营只显示 `绿方` / `红方`，不显示松绿、朱砂或玩家执方；单人显示上一完整“玩家 + 智能”轮的最多两枚吃子，双人显示上一合法走棋的一枚吃子或 `无吃子`。窄于 900dp 时切换为棋盘在上、操作栏在下。
- 每格拥有包含阵营、兽名、行列与选择状态的读屏语义；机器人思考时显示 `智能思考中` 并禁用棋盘输入。
- `package/assets/icon.png` 是用户选定的象鼠 S 河冷瓷圆章，使用包内独立的 1024×1024 RGBA 透明 PNG；四角 alpha 固定为 0，菜单按包版本加载，失败时不影响游戏运行。
- `package/assets/board/doushouqi-board.png` 与 `package/assets/pieces/` 完整收录进独立游戏 zip；棋盘保持参考图的近方形竹木框、深蓝双河和上下兽穴，六个陷阱改用双层八角捕兽网纹章，以八条向心网线、四个内倒钩、中央锁结和浅赭印底明确表达特殊地形。
- 资源测试按固定六个陷阱坐标检查八角框、八向网线、中央锁结和印底色差，防止生成器退回含义不清的交叉线。

坐标按模型上方第 1 行、左侧第 1 列计数。朱砂方初始为：狮 `(1,1)`、虎 `(1,7)`、狗 `(2,2)`、猫 `(2,6)`、鼠 `(3,1)`、豹 `(3,3)`、狼 `(3,5)`、象 `(3,7)`；松绿方初始为：象 `(7,1)`、狼 `(7,3)`、豹 `(7,5)`、鼠 `(7,7)`、猫 `(8,2)`、狗 `(8,6)`、虎 `(9,1)`、狮 `(9,7)`。朱砂兽穴在 `(1,4)`，松绿兽穴在 `(9,4)`；两片河道位于第 4–6 行的第 2–3 列和第 5–6 列。

## 智能等级预算

等级由玩家累计胜分加一并限制在 `1..10`。下表是实现使用的精确预算；当前只完成确定性战术与预算回归测试，尚未完成长时间自对弈胜率校准，因此不宣称相邻等级具有统计显著的胜率梯度。

| 等级 | 最大深度 | 节点 | 毫秒 | 弱化候选 | 弱化概率 | 战术延伸 |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 1 | 1,000 | 60 | 6 | 60% | 0 |
| 2 | 2 | 4,000 | 90 | 5 | 45% | 0 |
| 3 | 3 | 12,000 | 140 | 4 | 30% | 0 |
| 4 | 4 | 35,000 | 220 | 3 | 20% | 1 |
| 5 | 5 | 80,000 | 350 | 2 | 10% | 1 |
| 6 | 6 | 180,000 | 550 | 1 | 0% | 1 |
| 7 | 7 | 400,000 | 850 | 1 | 0% | 2 |
| 8 | 8 | 800,000 | 1,200 | 1 | 0% | 2 |
| 9 | 9 | 1,500,000 | 1,800 | 1 | 0% | 3 |
| 10 | 10 | 2,500,000 | 2,600 | 1 | 0% | 3 |

## 命令

```bash
python3 games/doushouqi/tools/generate_doushouqi_assets.py
./gradlew :games:doushouqi:testDebugUnitTest
./gradlew packageDoushouqiGame
npm run verify
```

完整设计见：

- `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md`
- `designs/specs/doushouqi-ui.md`
- `docs/superpowers/plans/2026-07-26-doushouqi-game.md`
- `docs/superpowers/plans/2026-07-27-doushouqi-reference-restoration.md`
- `docs/superpowers/specs/2026-07-27-doushouqi-logo-sidebar-design.md`
- `docs/superpowers/plans/2026-07-27-doushouqi-logo-sidebar.md`
- `docs/superpowers/reports/2026-07-27-doushouqi-logo-sidebar-runtime-acceptance.md`
- `docs/superpowers/specs/2026-07-28-doushouqi-single-player-capture-summary-design.md`
- `docs/superpowers/plans/2026-07-28-doushouqi-single-player-capture-summary.md`
- `docs/superpowers/reports/2026-07-28-doushouqi-single-player-capture-summary-runtime-acceptance.md`
- `docs/superpowers/specs/2026-07-28-doushouqi-round-capture-summary-design.md`
- `docs/superpowers/plans/2026-07-28-doushouqi-round-capture-summary.md`
- `docs/superpowers/reports/2026-07-28-doushouqi-round-capture-summary-runtime-acceptance.md`
- `docs/superpowers/specs/2026-07-28-doushouqi-two-player-immediate-capture-summary-design.md`
- `docs/superpowers/plans/2026-07-28-doushouqi-two-player-immediate-capture-summary.md`
- `docs/superpowers/reports/2026-07-28-doushouqi-two-player-immediate-capture-summary-runtime-acceptance.md`
