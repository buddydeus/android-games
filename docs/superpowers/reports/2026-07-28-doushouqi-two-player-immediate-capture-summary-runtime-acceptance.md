# Doushouqi Two-Player Immediate Capture Summary Runtime Acceptance

**Date:** 2026-07-28
**Result:** Passed

## Environment

- AVD `android_games_mvp_pad`
- Android 36 `google_apis` x86_64 Pad emulator
- Physical size `2560 x 1800`, density `320`
- Game center `0.0.5`
- Packaged Doushouqi `versionCode = 7`, `versionName = 0.0.7`

## Build Gates

```text
./gradlew :games:doushouqi:testDebugUnitTest
BUILD SUCCESSFUL

npm run verify
BUILD SUCCESSFUL
178 actionable tasks: 8 executed, 170 up-to-date
```

`build/game-packages/doushouqi.zip` contains:

```json
"versionCode": 7,
"versionName": "0.0.7"
```

## Runtime Scenario

1. Launch Doushouqi and confirm the menu displays `版本 0.0.7`.
2. Start `双人对战`; the initial summary is `无吃子`.
3. Move the Green rat from row 7, column 7 to row 3, column 7 over three
   Green turns while Red moves its cat between row 2 and row 3, column 6.
4. Green captures the Red elephant on row 3, column 7.
5. Before Red responds, confirm the rail displays only `绿方吃：象`.
6. Move the Red cat from row 3, column 6 to row 2, column 6 without capturing.
7. Confirm the rail immediately replaces the capture with `无吃子`.

## Evidence

After Green captures the elephant, the accessibility projection reports:

```text
当前回合： 红方
绿方吃：象
```

截图：`build/runtime-acceptance/doushouqi-two-player-immediate-capture.png`

After Red's quiet move, the accessibility projection reports:

```text
当前回合： 绿方
无吃子
```

截图：`build/runtime-acceptance/doushouqi-two-player-quiet-clears-capture.png`

截图与对应的 `doushouqi-two-capture.xml`、
`doushouqi-two-quiet.xml` UI Automator 语义树都保存在本地
`build/runtime-acceptance/` 构建证据目录，不纳入版本控制。

## Verdict

Two-player Doushouqi now publishes every accepted move immediately. A capture
shows only that move's attacker and captured animal, and the next quiet move
immediately replaces it with `无吃子`; no Green-plus-Red merge or wait remains.
Single-player completed player-plus-robot aggregation is covered by the
unchanged session regression suite.
