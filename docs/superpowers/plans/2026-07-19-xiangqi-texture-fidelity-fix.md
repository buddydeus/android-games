# Xiangqi Texture Fidelity Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match the approved Xiangqi menu preview with visibly glazed ceramic pieces and its thin, landscape porcelain board.

**Architecture:** Keep package-local runtime loading unchanged. Replace the procedural flat piece body with one approved transparent ceramic master plus exact local-font glyph compositing, then update board geometry and Compose registration together.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Python 3, Pillow, built-in image generation

## Global Constraints

- Change only the Xiangqi package and design/docs that describe it.
- Advance Xiangqi to `versionCode = 12` and `versionName = 0.0.12`; do not change the shell version.
- Preserve rules, AI, score, undo, rotation, coordinate mapping, and latest-move behavior.
- Use `designs/previews/xiangqi-ui-menu.png` as the visual target.

---

### Task 1: Lock texture fidelity

**Files:**
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiPieceAssetsTest.kt`
- Modify: `games/xiangqi/src/test/java/com/buddygames/xiangqi/XiangqiRulesTest.kt`

**Interfaces:**
- Consumes: current package PNGs and manifest.
- Produces: failing checks for board geometry, glaze range, soft shadow, and version `0.0.12`.

- [ ] Add assertions for a 1600 x 1500 board, grid bounds 128/90/1472/1410, a 1.0667 aspect ratio, and 0.90 piece texture scale.
- [ ] Add sampled ceramic-body luminance and partially transparent lower-right shadow checks.
- [ ] Run the focused tests and confirm the old flat assets fail.

### Task 2: Rebuild the piece family

**Files:**
- Create: `games/xiangqi/tools/source/ceramic-piece-master.png`
- Modify: `games/xiangqi/tools/generate_xiangqi_assets.py`
- Replace: `games/xiangqi/package/assets/pieces/*.png`

**Interfaces:**
- Consumes: one transparent blank ceramic master and a local CJK serif font covering every required glyph.
- Produces: fourteen matched 1024 x 1024 transparent PNGs.

- [ ] Generate and chroma-key one blank ceramic master from the approved preview material.
- [ ] Composite exact red and black traditional glyphs locally.
- [ ] Regenerate the contact sheet and verify every glyph, highlight, rim, shade, and shadow.

### Task 3: Match board and menu geometry

**Files:**
- Modify: `games/xiangqi/tools/generate_xiangqi_assets.py`
- Replace: `games/xiangqi/package/assets/board/xiangqi-board.png`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPieceTextures.kt`
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiVisuals.kt`

**Interfaces:**
- Consumes: the approved menu preview proportions.
- Produces: a thin landscape celadon frame, warm-white field, narrow river band, icon controls, and aligned touch registration.

- [ ] Generate the 1600 x 1500 complete board with transparent corners and restrained elevation shadow.
- [ ] Synchronize board constants and piece scale.
- [ ] Replace the floating menu card and mixed button treatments with the preview's flat rail and outlined icon buttons.

### Task 4: Version, documentation, and verification

**Files:**
- Modify: `games/xiangqi/src/main/java/com/buddygames/xiangqi/XiangqiPlugin.kt`
- Modify: `games/xiangqi/package/manifest.json`
- Modify: `designs/specs/xiangqi-ui.md`
- Modify: `designs/specs/xiangqi-ui-implementation.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: final package assets and UI.
- Produces: aligned version/docs plus build and runtime evidence.

- [ ] Set Xiangqi to `0.0.12` in plugin and manifest.
- [ ] Run `./gradlew :games:xiangqi:testDebugUnitTest`.
- [ ] Run `npm run verify`.
- [ ] Install on the Android emulator and compare menu/game screenshots against the approved preview.
- [ ] Stage only this correction and create one local commit without pushing.
