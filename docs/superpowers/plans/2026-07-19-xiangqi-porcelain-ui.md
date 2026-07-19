# Xiangqi Porcelain UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the antique Xiangqi presentation with the approved bright porcelain-and-celadon board, fourteen matched piece textures, and a quiet score-sheet UI.

**Architecture:** Keep the existing package-local texture loader and Compose board geometry. Extend deterministic asset tests first, then update the Pillow generator and regenerate every board/piece PNG as one set before applying the SSOT color and layout tokens to `XiangqiVisuals.kt`.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Python 3, Pillow, Android Gradle Plugin 9.2.1

## Global Constraints

- Follow `designs/specs/xiangqi-ui.md` as visual SSOT.
- Keep the complete board at 1440 x 1600 RGBA and preserve grid bounds 192/190/1248/1378.
- Keep all fourteen piece files at 1024 x 1024 RGBA with transparent corners.
- Keep rules, AI, score, undo, restart, rotation, coordinate mapping, and latest-move geometry unchanged.
- Increment only Xiangqi from `0.0.10` to `0.0.11`; do not increment the shell.
- Update `README.md` and `AGENTS.md`.

---

### Task 1: Lock the approved asset contract

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiPieceAssetsTest.kt`
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Consumes: existing `XIANGQI_*` texture constants and package PNG files.
- Produces: regression checks for porcelain palette samples, matched piece geometry, correct version, and existing alpha/dimension rules.

- [ ] **Step 1: Write failing palette and version tests**

Add checks that the board center is bright, the rim sample is celadon, every piece has matching visible bounds, and `XiangqiPlugin.manifest` reports version 11 / `0.0.11`.

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiPieceAssetsTest --tests com.buddygames.xiangqi.XiangqiRulesTest.gameVersionAndMainMenuLabelStayAligned
```

Expected: FAIL because the current board is dark wood and the package is `0.0.10`.

### Task 2: Generate the porcelain board and piece family

**Files:**
- Modify: `games/xiangqi/tools/generate_xiangqi_assets.py`
- Replace: `games/xiangqi/package/assets/board/xiangqi-board.png`
- Replace: `games/xiangqi/package/assets/pieces/*.png`

**Interfaces:**
- Consumes: `BOARD_SIZE`, grid bounds, `PIECES`, and a local CJK serif font.
- Produces: deterministic package assets accepted by `XiangqiPieceAssetsTest`.

- [ ] **Step 1: Replace antique wood generation**

Generate a transparent-corner board with `#F5F1E8` porcelain field, `#A9C7BE` celadon rim, `#263C38` grid, and `#D9E9E4` river band. Remove the decorative bottom seal and heavy cast shadow.

- [ ] **Step 2: Replace piece material generation**

Generate one centered circular matte-ivory disc per file with a restrained brass-tan edge, consistent 10-12% padding, and exact glyph mapping:

```text
Red: 帥 俥 傌 炮 相 仕 兵
Black: 將 車 馬 砲 象 士 卒
```

- [ ] **Step 3: Regenerate assets and contact sheet**

Run:

```bash
python3 games/xiangqi/tools/generate_xiangqi_assets.py \
  --contact-sheet build/visual/xiangqi-porcelain-pieces.png
```

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiPieceAssetsTest
```

Expected: PASS.

### Task 3: Apply the approved Compose presentation

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`

**Interfaces:**
- Consumes: `XiangqiTextureSet`, existing board geometry, session state, and SSOT tokens.
- Produces: bright menu/game layouts with unchanged callbacks and state semantics.

- [ ] **Step 1: Add failing visual-token assertions**

Expose package-local ARGB constants for canvas, rail, ink, celadon, cinnabar, and blue-black, then assert exact values from `XiangqiRulesTest`.

- [ ] **Step 2: Run the token test and verify RED**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiRulesTest.xiangqiPorcelainUiTokensStayAligned
```

Expected: FAIL until the new constants exist.

- [ ] **Step 3: Update layout and states**

Apply the SSOT colors, flatten the rail, reduce decorative shadows/rules, use an outlined `将军` notice, switch selection to celadon-blue, and preserve 54 dp controls plus compact stacked layout.

- [ ] **Step 4: Run the Xiangqi module tests**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest
```

Expected: PASS.

### Task 4: Version and documentation

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/package/manifest.json`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: completed package behavior.
- Produces: aligned package version and current project documentation.

- [ ] **Step 1: Set Xiangqi version**

Set `versionCode = 11` and `versionName = "0.0.11"` in plugin and manifest.

- [ ] **Step 2: Replace antique UI documentation**

Document the bright porcelain board, celadon river, fourteen independent PNGs, deterministic generator, and approved SSOT.

- [ ] **Step 3: Re-run version tests**

Run:

```bash
./gradlew :games:xiangqi:testDebugUnitTest --tests com.buddygames.xiangqi.XiangqiRulesTest.gameVersionAndMainMenuLabelStayAligned
```

Expected: PASS.

### Task 5: Delivery verification

**Files:**
- Verify only.

**Interfaces:**
- Consumes: complete Xiangqi package update.
- Produces: build, package, visual, and repository-level acceptance evidence.

- [ ] **Step 1: Run full repository verification**

Run:

```bash
npm run verify
```

Expected: all tests, four package zips, and debug APK build successfully.

- [ ] **Step 2: Launch the app**

Run:

```bash
npm start
```

Expected: emulator/device installs and launches `com.buddygames.center/.MainActivity`.

- [ ] **Step 3: Capture visual evidence**

Open Xiangqi and capture menu plus active-game screenshots. Verify a complete board, intersection placement, readable pieces, quiet right rail, no overlap at 800 x 600, and preserved Black-player rotation.

- [ ] **Step 4: Run delivery checks**

Confirm text contrast, 48 dp touch targets, visible state treatments, no emoji controls, no decorative animation, and remove one redundant decorative element.

- [ ] **Step 5: Commit**

Stage only the Xiangqi implementation, approved design/plan docs, README, and AGENTS changes. Commit with a scoped structured message and do not push.
