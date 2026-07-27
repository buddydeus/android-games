# Doushouqi Logo And Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the approved transparent elephant-and-rat Doushouqi Logo and align the menu and active-game rails with the existing game family, using simplified `红方` / `绿方` current-turn copy.

**Architecture:** Keep the Logo package-owned and convert the approved RGBA preview into the package’s exact `1024 × 1024` asset. Keep all UI ownership in `DoushouqiScreen.kt`, but extract the user-visible rail copy into pure internal helpers so JVM unit tests can protect identity, turn, thinking, and result wording without Compose instrumentation.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Java ImageIO JVM tests, Pillow asset conversion, Gradle Android unit tests.

## Global Constraints

- Touch only the `games/doushouqi/` game implementation plus synchronized project documentation and design artifacts.
- Increment Doushouqi from `versionCode = 3`, `versionName = 0.0.3` to `versionCode = 4`, `versionName = 0.0.4`; keep code and JSON manifests aligned.
- Keep the shell version unchanged.
- Production Logo must be `1024 × 1024` RGBA PNG with all four corner alpha values equal to `0`.
- Preserve the approved `象`, `鼠`, S-shaped river, porcelain rim, composition, and transparent exterior.
- Menu rail remains `320dp` at `88%` content height; active-game rail remains `300dp` at `94%`.
- Current-turn copy is exactly `当前回合：绿方` or `当前回合：红方`.
- Result copy keeps the existing full `松绿方` / `朱砂方` identity and reason.
- Preserve all rules, AI, session, board, piece, trap, score, undo, restart, rotation, and latest-move behavior.
- Update `README.md`, `AGENTS.md`, and `games/doushouqi/README.md` with every repository change.

---

### Task 1: Protect The Transparent Logo And Release Version

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiAssetsTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/package/assets/icon.png`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`

**Interfaces:**
- Consumes: `designs/previews/android-game-logo-doushouqi.png`, a `1254 × 1254` approved RGBA preview.
- Produces: package `assets/icon.png`, exact `1024 × 1024` RGBA; `DOUSHOUQI_VERSION_CODE = 4`; `DOUSHOUQI_VERSION_NAME = "0.0.4"`.

- [ ] **Step 1: Tighten the failing Logo asset contract**

Replace the permissive icon assertions with:

```kotlin
assertEquals(1024, image.width)
assertEquals(1024, image.height)
assertEquals(0, image.getRGB(0, 0) ushr 24)
assertEquals(0, image.getRGB(1023, 0) ushr 24)
assertEquals(0, image.getRGB(0, 1023) ushr 24)
assertEquals(0, image.getRGB(1023, 1023) ushr 24)
assertTrue("transparent medallion coverage", alphaCoverage(image) in 0.60f..0.82f)
assertTrue("Logo must retain pine green", hasOpaqueColorNear(image, 0x0E, 0x5A, 0x3A, 70))
assertTrue("Logo must retain cinnabar", hasOpaqueColorNear(image, 0xC6, 0x3A, 0x20, 70))
assertTrue("Logo must retain blue river", hasOpaqueColorNear(image, 0x07, 0x5D, 0x86, 70))
```

Add `hasOpaqueColorNear(image, red, green, blue, tolerance)` that samples every fourth pixel, requires alpha above `220`, and accepts a summed RGB distance no greater than `tolerance`.

- [ ] **Step 2: Update the manifest test to the target release**

```kotlin
assertEquals(4, manifest.versionCode)
assertEquals("0.0.4", manifest.versionName)
```

- [ ] **Step 3: Run RED verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiAssetsTest.packageIconIsReadableCircularSafePng \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest.packageIdentityMatchesReferenceArtworkRelease
```

Expected: the existing opaque-background Logo fails the four alpha assertions and the manifest fails with `3` / `0.0.3`.

- [ ] **Step 4: Create the package Logo without cropping**

Resize the approved RGBA preview with Pillow Lanczos:

```python
from PIL import Image

source = Image.open("designs/previews/android-game-logo-doushouqi.png").convert("RGBA")
source.resize((1024, 1024), Image.Resampling.LANCZOS).save(
    "games/doushouqi/package/assets/icon.png",
)
```

- [ ] **Step 5: Update both manifests**

Set:

```kotlin
internal const val DOUSHOUQI_VERSION_CODE = 4
internal const val DOUSHOUQI_VERSION_NAME = "0.0.4"
```

and JSON:

```json
"versionCode": 4,
"versionName": "0.0.4"
```

- [ ] **Step 6: Run GREEN verification**

Run the same targeted Gradle command. Expected: both tests pass.

---

### Task 2: Protect The Simplified Rail Copy

**Files:**
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiPluginTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`

**Interfaces:**
- Produces: `doushouqiTurnSideLabel(side: DoushouqiSide): String`
- Produces: `doushouqiFullSideLabel(side: DoushouqiSide): String`
- Produces: `doushouqiTurnLine(side: DoushouqiSide): String`
- Produces: `doushouqiPlayerSideLine(side: DoushouqiSide): String`
- Consumes: existing `DoushouqiResult` for unchanged result reasons.

- [ ] **Step 1: Write failing rail-copy tests**

Add tests with literal expectations:

```kotlin
@Test
fun currentTurnUsesSimpleRedAndGreenSideNames() {
    assertEquals("绿方", doushouqiTurnSideLabel(DoushouqiSide.PINE_GREEN))
    assertEquals("红方", doushouqiTurnSideLabel(DoushouqiSide.VERMILION))
    assertEquals("当前回合：绿方", doushouqiTurnLine(DoushouqiSide.PINE_GREEN))
    assertEquals("当前回合：红方", doushouqiTurnLine(DoushouqiSide.VERMILION))
}

@Test
fun identityAndResultCopyKeepFullFactionMeaning() {
    assertEquals("玩家执松绿", doushouqiPlayerSideLine(DoushouqiSide.PINE_GREEN))
    assertEquals("玩家执朱砂", doushouqiPlayerSideLine(DoushouqiSide.VERMILION))
    assertEquals("松绿方", doushouqiFullSideLabel(DoushouqiSide.PINE_GREEN))
    assertEquals("朱砂方", doushouqiFullSideLabel(DoushouqiSide.VERMILION))
}
```

- [ ] **Step 2: Run RED verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiPluginTest
```

Expected: compilation fails because the new copy helpers do not exist.

- [ ] **Step 3: Implement the minimal pure copy helpers**

```kotlin
internal fun doushouqiTurnSideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "绿方" else "红方"

internal fun doushouqiFullSideLabel(side: DoushouqiSide): String =
    if (side == DoushouqiSide.PINE_GREEN) "松绿方" else "朱砂方"

internal fun doushouqiTurnLine(side: DoushouqiSide): String =
    "当前回合：${doushouqiTurnSideLabel(side)}"

internal fun doushouqiPlayerSideLine(side: DoushouqiSide): String =
    "玩家执${if (side == DoushouqiSide.PINE_GREEN) "松绿" else "朱砂"}"
```

Update result copy to call `doushouqiFullSideLabel`.

- [ ] **Step 4: Run GREEN verification**

Run the same `DoushouqiPluginTest` command. Expected: pass.

---

### Task 3: Implement The Family Menu And Active-Game Rails

**Files:**
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiVisuals.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiVisualsTest.kt`

**Interfaces:**
- Consumes: the pure copy helpers from Task 2.
- Produces: menu header/action grouping and active-game score/status/action grouping.
- Produces: `DOUSHOUQI_RAIL_CORNER_DP = 8f`, `DOUSHOUQI_RAIL_HORIZONTAL_PADDING_DP = 24f`, `DOUSHOUQI_RAIL_VERTICAL_PADDING_DP = 26f`, `DOUSHOUQI_ACTION_HEIGHT_DP = 54f`.

- [ ] **Step 1: Add failing family geometry assertions**

Extend `layoutAndMarkerTokensMatchGameFamily`:

```kotlin
assertEquals(8f, DOUSHOUQI_RAIL_CORNER_DP)
assertEquals(24f, DOUSHOUQI_RAIL_HORIZONTAL_PADDING_DP)
assertEquals(26f, DOUSHOUQI_RAIL_VERTICAL_PADDING_DP)
assertEquals(54f, DOUSHOUQI_ACTION_HEIGHT_DP)
```

- [ ] **Step 2: Run RED verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiVisualsTest.layoutAndMarkerTokensMatchGameFamily
```

Expected: compilation fails because the new family geometry constants do not exist.

- [ ] **Step 3: Add the geometry constants**

Add the four constants to `DoushouqiVisuals.kt` with the exact values from the interface block.

- [ ] **Step 4: Rebuild the menu rail**

Use `Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween)` with:

- a centered header `Column` containing the `112dp` transparent package Logo, `斗兽棋`, and the package version;
- a full-width action `Column` with `10dp` spacing and exact order `单人模式`, `双人对战`, `退出游戏`;
- `54dp` buttons and visible focus border.

- [ ] **Step 5: Rebuild the active-game rail**

Use `Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween)` with:

1. Score group: `玩家 : 智能` or `松绿方 : 朱砂方`, `0 : 0`, then `智能等级 N` for single player.
2. `HorizontalDivider`.
3. Turn/result group: nonterminal `当前回合：绿方` / `当前回合：红方`, secondary player-side line, and optional `智能思考中`; terminal `对局结果` plus existing reason.
4. `HorizontalDivider`.
5. Full-width action group: conditional `重新开始`, conditional `悔棋`, then `返回菜单`.

Use the existing undo/restart visibility functions and do not change their behavior.

- [ ] **Step 6: Align the panel surface and buttons**

Apply the shared constants to both wide and compact rails. Use a light surface, subtle one-pixel muted border, restrained shadow, `8dp` corners, `24dp` horizontal padding, `26dp` vertical padding, and `54dp` buttons. Keep the menu rail `320dp`/`88%` and game rail `300dp`/`94%`.

- [ ] **Step 7: Run GREEN verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: all Doushouqi JVM tests pass.

---

### Task 4: Synchronize Documentation And Verify The Package

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `games/doushouqi/README.md`
- Modify: `docs/superpowers/specs/2026-07-27-doushouqi-logo-sidebar-design.md`
- Modify: `designs/specs/doushouqi-ui.md`
- Modify: `designs/images/doushouqi-ui-game-tablet.md`
- Modify: `designs/previews/doushouqi-ui-game-tablet.png`
- Create: `docs/superpowers/reports/2026-07-27-doushouqi-logo-sidebar-runtime-acceptance.md`

**Interfaces:**
- Consumes: target version `0.0.4`, transparent Logo contract, family rail geometry, simplified current-turn copy.
- Produces: synchronized human/agent documentation and runtime evidence.

- [ ] **Step 1: Update current-version documentation**

Change all Doushouqi current-version references from `0.0.3` to `0.0.4`. Document the transparent elephant-and-rat Logo, menu/action hierarchy, active-game three-zone rail, and `红方` / `绿方` turn copy. Keep full `松绿方` / `朱砂方` language for rules and results.

- [ ] **Step 2: Run full deterministic verification**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
npm run verify
git diff --check
unzip -l build/game-packages/doushouqi.zip | rg 'assets/icon.png'
```

Expected: all commands exit `0`; the package zip contains `assets/icon.png`.

- [ ] **Step 3: Run emulator acceptance**

Run:

```bash
npm start
```

Capture:

- menu showing the transparent Logo, `版本 0.0.4`, and the three family actions;
- active single-player game showing score, `智能等级 1`, two dividers, `当前回合：绿方` or `当前回合：红方`, player-side line, undo, and return action.

- [ ] **Step 4: Record acceptance evidence**

Write the exact emulator resolution, screenshot paths, observed copy, asset alpha result, and verification commands to `docs/superpowers/reports/2026-07-27-doushouqi-logo-sidebar-runtime-acceptance.md`.

- [ ] **Step 5: Create the scoped local commit**

Use:

```text
feat: align Doushouqi identity and rails

- Ship the transparent elephant-and-rat package Logo
- Align menu and active-game rails with the game family
- Simplify current-turn copy to red and green sides
```

Do not push.
