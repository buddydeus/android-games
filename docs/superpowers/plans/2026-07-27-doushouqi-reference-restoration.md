# Doushouqi Reference Restoration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat Compose Dou Shou Qi board and pieces with package-owned textures that closely reproduce the user-approved bamboo-board reference while retaining the shared game-family rail and every existing rule/session behavior.

**Architecture:** Add one deterministic asset generator that produces a complete empty `1400×1400` board and sixteen transparent `512×512` piece textures. A version-keyed texture loader validates MIME type and exact dimensions before decode; Compose draws the board texture as the visual base, overlays independent piece textures and interaction feedback, and retains the existing semantic Compose fallback when any asset is invalid.

**Tech Stack:** Kotlin, Jetpack Compose, Android `BitmapFactory`, JUnit 4, Python Pillow asset generator, package-local PNG assets.

## Global Constraints

- Visual authority is `designs/specs/doushouqi-ui.md` and `designs/references/doushouqi-board-reference.png`.
- Preserve the exact 7-column × 9-row model, terrain coordinates, legal moves, rotation mapping, score, undo, restart, AI, and latest-move behavior.
- Keep the shared 28dp outer padding, 34dp board/rail gap, 300dp game rail, 320dp menu rail, and below-900dp stacked fallback.
- The game version becomes exactly `versionCode = 2`, `versionName = 0.0.2`; the shell version remains `0.0.5`.
- Board texture path is exactly `assets/board/doushouqi-board.png`, dimensions `1400×1400`.
- Piece texture paths are exactly `assets/pieces/{green|red}-{elephant|lion|tiger|leopard|wolf|dog|cat|rat}.png`, each `512×512` RGBA.
- Texture loading must validate `image/png` and exact bounds before full decode and must fall back safely.
- Every production behavior or resource contract receives a witnessed failing test before implementation.
- Every repository change keeps root `README.md`, root `AGENTS.md`, and `games/doushouqi/README.md` aligned.
- Run `./gradlew :games:doushouqi:testDebugUnitTest` and `npm run verify` before completion.
- Do not push.

---

### Task 1: Texture And Version Contracts

**Files:**
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiManifest.kt`
- Modify: `games/doushouqi/package/manifest.json`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiVisuals.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiManifestTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiAssetsTest.kt`
- Modify: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiVisualsTest.kt`

**Interfaces:**
- Produces `DoushouqiVisuals.BOARD_TEXTURE_PATH`, `DoushouqiVisuals.pieceTexturePath(piece)`, exact texture dimensions, and version `0.0.2`.
- Consumes existing `DoushouqiPiece`, `DoushouqiSide`, and `DoushouqiAnimal`.

- [ ] **Step 1: Write the failing version and texture-path tests**

Extend manifest tests to require code/JSON version `2` / `0.0.2`. Extend visual tests with:

```kotlin
assertEquals("board/doushouqi-board.png", DoushouqiVisuals.BOARD_TEXTURE_PATH)
assertEquals(1400, DoushouqiVisuals.BOARD_TEXTURE_WIDTH)
assertEquals(1400, DoushouqiVisuals.BOARD_TEXTURE_HEIGHT)
assertEquals(
    "pieces/green-elephant.png",
    DoushouqiVisuals.pieceTexturePath(
        DoushouqiPiece(DoushouqiSide.PINE_GREEN, DoushouqiAnimal.ELEPHANT),
    ),
)
assertEquals(
    "pieces/red-rat.png",
    DoushouqiVisuals.pieceTexturePath(
        DoushouqiPiece(DoushouqiSide.VERMILION, DoushouqiAnimal.RAT),
    ),
)
```

- [ ] **Step 2: Write the failing asset contract test**

Require one readable `1400×1400` RGBA board and all sixteen readable `512×512` RGBA piece PNGs. Assert transparent board corners, transparent piece corners, non-empty subject bounds, consistent same-side tile geometry, and distinct green/red center colors.

- [ ] **Step 3: Run RED**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest \
  --tests com.buddygames.doushouqi.DoushouqiManifestTest \
  --tests com.buddygames.doushouqi.DoushouqiVisualsTest \
  --tests com.buddygames.doushouqi.DoushouqiAssetsTest
```

Expected: failures for version `0.0.1`, missing texture constants, and missing PNG assets.

- [ ] **Step 4: Implement the minimal constants and version alignment**

Set both manifests to `2` / `0.0.2`. Add the exact texture constants and deterministic lower-case side/animal path mapping without adding shell-owned game-ID branches.

- [ ] **Step 5: Run the manifest/visual tests**

Expected: manifest and path tests pass; asset tests still fail only because generated assets do not exist.

---

### Task 2: Deterministic Board And Piece Asset Generation

**Files:**
- Create: `games/doushouqi/tools/generate_doushouqi_assets.py`
- Create: `games/doushouqi/package/assets/board/doushouqi-board.png`
- Create: `games/doushouqi/package/assets/pieces/green-*.png`
- Create: `games/doushouqi/package/assets/pieces/red-*.png`
- Modify: `games/doushouqi/README.md`

**Interfaces:**
- Produces the exact files consumed by `DoushouqiVisuals`.
- Consumes only deterministic drawing primitives, a local CJK Kai font selected after glyph coverage validation, and the approved SSOT geometry.

- [ ] **Step 1: Implement the reproducible generator**

Use Pillow to draw:

- transparent `1400×1400` canvas;
- rounded bamboo frame, inner rim, deterministic fine wood grain;
- grid bounds `56..1344` in both axes, seven equal columns and nine equal rows;
- rivers at model rows `3..5`, columns `1,2,4,5`;
- crossed traps at `(0,2)`, `(0,4)`, `(1,3)`, `(8,2)`, `(8,4)`, `(7,3)`;
- `兽穴` at `(0,3)` and `(8,3)`;
- dark grid intersections and center trap pins;
- sixteen transparent rounded-square tiles with shared bevel, inner rim, top highlight, bottom shadow, and exact labels from `DoushouqiAnimal.label`.

Before rendering, verify the chosen font covers `象狮虎豹狼狗猫鼠兽穴`; fail with a clear message otherwise. Use a fixed random seed for grain.

- [ ] **Step 2: Generate assets**

Run:

```bash
python3 games/doushouqi/tools/generate_doushouqi_assets.py
```

Expected: one board and sixteen piece files under the exact package paths.

- [ ] **Step 3: Inspect the generated board and piece sheet**

Render a temporary contact sheet from the sixteen final PNGs, then visually verify:

- no cropped glyphs or shadows;
- matching green geometry and matching red geometry;
- reference-like bamboo, river, bevel, and cream glyph treatment;
- exact grid, river, trap, and den positions.

- [ ] **Step 4: Run asset GREEN**

Run `DoushouqiAssetsTest`; expect every dimension, alpha, coverage, geometry, and color assertion to pass.

---

### Task 3: Validated Texture Loading And Compose Rendering

**Files:**
- Create: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiTextures.kt`
- Create: `games/doushouqi/src/test/java/com/buddygames/doushouqi/DoushouqiTexturesTest.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiPlugin.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiScreen.kt`
- Modify: `games/doushouqi/src/main/java/com/buddygames/doushouqi/DoushouqiVisuals.kt`

**Interfaces:**
- Produces:

```kotlin
internal data class DoushouqiTextureSet(
    val board: ImageBitmap?,
    val pieces: Map<DoushouqiPiece, ImageBitmap>,
)

internal fun loadDoushouqiTextures(assetsDir: File): DoushouqiTextureSet
```

- Consumes package `assetsDir`, manifest version for Compose cache identity, and exact SSOT contracts.

- [ ] **Step 1: Write loader RED tests**

Use a fake decoder boundary to assert:

- all seventeen exact files are requested;
- wrong MIME, wrong bounds, failed decode, or post-decode size mismatch returns `null` only for that texture;
- one broken piece does not discard the board or other pieces;
- the loader never accepts JPEG/WebP renamed to PNG.

- [ ] **Step 2: Run loader RED**

Run `DoushouqiTexturesTest`; expect missing loader types.

- [ ] **Step 3: Implement bounds-first loading**

Follow the Junqi loader pattern: read `BitmapFactory.Options.inJustDecodeBounds`, require `outMimeType == "image/png"` and exact dimensions, decode once, verify dimensions again, convert to `ImageBitmap`, and return null on any exception.

- [ ] **Step 4: Run loader GREEN**

Run `DoushouqiTexturesTest`; expect all validation/fallback cases to pass.

- [ ] **Step 5: Write board-layout RED assertions**

Update visuals tests to require:

```kotlin
assertEquals(1f, DOUSHOUQI_BOARD_ASPECT_RATIO)
assertEquals(0.86f, DOUSHOUQI_PIECE_TEXTURE_SCALE)
```

Keep the shared latest-move constants unchanged.

- [ ] **Step 6: Integrate textures into Compose**

Load/cache the texture set in both `MainScreen` and `GameScreen` using package root plus manifest version. Pass textures through menu/game/board:

- board texture fills the near-square board without rotation;
- click grid remains exactly 7×9 and transparent over the board;
- rotated view changes model/display mapping only;
- piece texture uses `ContentScale.Fit`, stays upright, and uses the mapped model piece;
- selected border, legal dot, capture ring, and latest-move marker remain Compose overlays;
- any missing board/piece uses the existing semantic Compose fallback;
- side rail keeps shared geometry, status hierarchy, labels, undo/restart/menu behavior, and 48dp targets.

- [ ] **Step 7: Run Compose/module GREEN**

Run:

```bash
./gradlew :games:doushouqi:testDebugUnitTest
```

Expected: compilation succeeds and all manifest, assets, texture, rules, AI, session, plugin, and visual tests pass.

---

### Task 4: Packaging, Runtime Acceptance, Documentation, And Commit

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `games/doushouqi/README.md`
- Modify: `designs/specs/doushouqi-ui.md`
- Create: `docs/superpowers/reports/2026-07-27-doushouqi-reference-runtime-acceptance.md`
- Modify: `docs/superpowers/plans/2026-07-27-doushouqi-reference-restoration.md` (checkboxes only)

**Interfaces:**
- Produces a verified `doushouqi.zip`, Debug APK inclusion, runtime screenshots/evidence, and scoped local commit.

- [ ] **Step 1: Verify package entries**

Run:

```bash
./gradlew packageDoushouqiGame
unzip -l build/game-packages/doushouqi.zip
```

Require manifest, plugin, icon, board texture, and all sixteen piece textures.

- [ ] **Step 2: Run the full repository gate**

Run:

```bash
npm run verify
```

Require exit zero for all unit tests, all six game packages, and Debug APK inclusion.

- [ ] **Step 3: Run emulator smoke and capture evidence**

Run `npm start`, open 斗兽棋, inspect the menu empty board, start single-player, move one legal piece, and capture screenshots. Confirm:

- board and pieces visually match the reference material and proportions;
- no piece/glyph is cropped;
- two rivers, six traps, and two dens align with click cells;
- side rail matches the other games' geometry and control style;
- legal/capture/selection/latest overlays remain visible;
- 800×600dp landscape remains operable.

Record exact device, build, actions, and screenshot paths in the runtime report. Do not claim device acceptance if this step is skipped.

- [ ] **Step 4: Complete the designer delivery pass**

Check AA contrast, 48dp controls, press/disabled feedback, 900dp breakpoint, screen-reader descriptions, and no decorative animation. Remove the redundant per-cell river line overlay when the valid board texture is present; retain it only in fallback rendering.

- [ ] **Step 5: Sync documentation**

Document version `0.0.2`, board/piece paths and dimensions, generator command, bounds-first fallback behavior, and runtime acceptance result in root/module docs and `AGENTS.md`.

- [ ] **Step 6: Inspect and commit**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Then create:

```text
feat: restore Doushouqi board artwork

- Add reference-matched package board and piece textures
- Preserve shared rail behavior with validated runtime fallbacks
```

Do not push.
