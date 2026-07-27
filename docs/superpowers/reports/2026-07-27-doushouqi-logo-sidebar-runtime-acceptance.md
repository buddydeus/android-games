# 斗兽棋 Logo 与侧栏运行验收

**日期：** 2026-07-27

**环境：** Android 36，AVD `android_games_mvp_pad`，`emulator-5554`，2560×1800 横屏

**版本：** 斗兽棋 `0.0.4`

## 结论

通过。斗兽棋使用用户选定的象鼠 S 河图案作为透明背景包内 Logo；菜单侧栏和对局侧栏均采用现有游戏家族结构；当前回合按要求简化为 `红方` / `绿方`，玩家执方及终局文案仍保留松绿/朱砂阵营语义。

## 资产验收

- `games/doushouqi/package/assets/icon.png` 为 `1024 × 1024` RGBA PNG。
- 四角 alpha 均为 `0`，非透明像素覆盖率为 `0.694301`，无方形白底。
- 自动化测试覆盖松绿、朱砂、矿物蓝三组主色及圆形安全区。
- 打包产物 `build/game-packages/doushouqi.zip` 包含 `assets/icon.png`。

## 菜单侧栏

模拟器语义树确认：

- Logo 描述为 `斗兽棋图标`；
- 标题为 `斗兽棋`；
- 版本为 `版本 0.0.4`；
- 操作为 `单人模式`、`双人对战`、`退出游戏`。

截图：`build/runtime-acceptance/doushouqi-logo-sidebar-menu.png`

## 对局侧栏

单人模式初始局语义树确认：

- 比分标题为 `玩家 : 智能`；
- 智能等级为 `智能等级 1`；
- 回合为 `当前回合：绿方`；
- 玩家身份为 `玩家执松绿`；
- 操作为 `悔棋`、`返回菜单`。

侧栏按比分、回合/结果、操作三段组织，并以两条分隔线稳定分区；棋盘、棋子、河道、兽穴和捕兽网陷阱纹章保持完整。

截图：`build/runtime-acceptance/doushouqi-logo-sidebar-game.png`

## 自动化验证

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
unzip -l build/game-packages/doushouqi.zip | rg 'assets/icon.png'
```

以上命令均以退出码 `0` 完成；`npm run verify` 完成 178 个 Gradle task，并校验六个内置游戏包及 Debug APK。
