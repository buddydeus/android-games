# Xiangqi Reference UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Xiangqi package UI so its emulator screenshot faithfully matches the supplied walnut-board, lacquer-piece, and mineral-panel reference.

**Architecture:** Keep all Xiangqi visuals in `games/xiangqi`. Compose Canvas renders exact board geometry, palace diagonals, river interruption, piece lighting, and touch targets; the existing package-local wood texture provides material detail without changing `game-api` or package loading.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, JUnit 4, package-local PNG texture

## Global Constraints

- Preserve the existing Xiangqi rules, robot, plugin manifest, and package boundary.
- Pieces must remain centered on the 9 x 10 line intersections.
- Vertical files must stop across the river; both palaces must use exact diagonal lines.
- Match the reference with black lacquer/gold black-side pieces and cinnabar/gold red-side pieces.
- Keep all controls at least 48dp and preserve the three required menu actions.

---

### Task 1: Geometry and Label Contracts

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`

- [ ] Add tests for the approximately 1.03:1 board frame and traditional piece labels.
- [ ] Run the Xiangqi tests and verify they fail for missing APIs.
- [ ] Add the sizing helper and display-label API, then rerun tests.

### Task 2: Reference-Accurate Board and Pieces

**Files:**
- Create: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Reuse: `games/xiangqi/package/assets/textures/xiangqi-shelf.png`

- [ ] Build the layered walnut frame and inset maple surface.
- [ ] Draw nine files, ten ranks, interrupted river files, two palaces, and river labels.
- [ ] Draw dimensional black/gold and red/gold lacquer pieces with selection halo.
- [ ] Preserve intersection-sized hit targets and coordinate semantics.
- [ ] Rebuild the mode menu as board preview plus material control panel.

### Task 3: Reference-Accurate Match Rail

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`

- [ ] Match the raised white rail, centered historical score, separators, current-turn hierarchy, and outlined exit button.
- [ ] Add the familiar exit glyph without adding dependencies.
- [ ] Preserve the narrow-layout stacked fallback.

### Task 4: Verification

**Files:**
- Inspect: `build/game-packages/xiangqi.zip`

- [ ] Run `./gradlew :games:xiangqi:testDebugUnitTest`.
- [ ] Run `npm run verify`.
- [ ] Confirm `xiangqi.zip` contains the plugin, manifest, and texture.
- [ ] Launch the Android Pad emulator, capture menu and match screenshots, perform a legal move, and compare board ratio, intersections, river, palace, pieces, rail, and clipping against the reference.
- [ ] Correct visible differences and repeat the full gate.
