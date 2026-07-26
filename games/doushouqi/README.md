# 斗兽棋

`doushouqi` 是游戏中心正在实现的第六个独立游戏包，初始版本为 `0.0.1`。规则、AI、会话、Compose 界面和包内资源都归本模块所有。

当前已完成标准 7×9 不可变棋盘与基础合法着法：

- 松绿方先行，双方各有象、狮、虎、豹、狼、狗、猫、鼠八枚棋子。
- 普通棋子每次正交移动一格，不能进入己方兽穴。
- 只有鼠可进入河道；狮虎可跨越完整河段，河内任一方的鼠都会阻挡跳跃。
- 同级或高阶可吃低阶；陆地鼠可吃象，象不能吃鼠。
- 鼠只能在同为陆地或同为河道时互吃，不能跨河岸边界吃子。
- 进入对手陷阱的棋子在停留期间按零级防守；离开陷阱攻击时恢复原兽阶。

终局、三次重复/100 静默步和棋、1–10 级机器人、单/双人会话与界面将在后续计划任务中完成。

## 命令

```bash
./gradlew :games:doushouqi:testDebugUnitTest
```

完整设计见：

- `docs/superpowers/specs/2026-07-26-doushouqi-game-design.md`
- `designs/specs/doushouqi-ui.md`
- `docs/superpowers/plans/2026-07-26-doushouqi-game.md`
