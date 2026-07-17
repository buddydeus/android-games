# Android Games Home Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development for the presentation contract and superpowers:verification-before-completion before delivery. This checkout contains user-owned untracked design artifacts; do not create commits or a new worktree.

**Goal:** Replace the existing shelf-card home screen with the approved tactile three-button game selector while preserving package import and game launch behavior.

**Architecture:** Keep repository and plugin loading unchanged. Move responsive sizing, built-in game ordering, and Logo selection into pure Kotlin contracts in `HomeGamePresentation.kt`, render the three Logo marks with Compose vector primitives in a focused `HomeGameLogo.kt`, and let `GameCenterApp.kt` consume those contracts through `BoxWithConstraints`.

**Tech Stack:** Kotlin 2.4, Jetpack Compose BOM 2026.06.01, Material 3, JUnit 4.

## Global Constraints

- `designs/specs/android-games-home.md` is the visual SSOT.
- At `>= 960dp`, all three game buttons are exactly `264dp × 264dp` with `28dp` gaps and `112dp` Logo wells.
- At `< 600dp` or in portrait orientation, all three buttons are full width, exactly `112dp` high, with `16dp` gaps and `64dp` Logo wells.
- Buttons in the same viewport must share width, height, radius, border, shadow, padding, and elevation.
- Home uses only vector-drawn game marks: five stones, double-ring “象”, and three Othello discs.
- Game package shelf textures must not be loaded by the new buttons.
- The existing mineral texture may appear only on the page background at alpha `0.05–0.07`.
- Keep touch targets at least `48dp`, expose “打开<游戏名>” semantics, and provide pressed and focus feedback without layout movement.
- Keep existing import, plugin-load, empty, warning, success, and error flows functional.
- Do not change `game-api`, `DexGamePluginLoader`, `GamePackageRepository`, Gradle dependencies, or packaging tasks.
- Do not commit; the user requested implementation, not a git commit.

---

### Task 1: Lock the home presentation and responsive contract

**Files:**

- Modify: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`
- Modify: `app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt`

**Interfaces:**

- Produces: `HomeGameLogo`, `HomeGamePresentation`, `HomeGameLayoutMode`, `HomeGameLayout`, `homeGamePresentation(gameId)`, and `homeGameLayout(widthDp, heightDp)`.
- Consumed by: the Compose Logo renderer and `GameCenterHome`.

- [ ] **Step 1: Replace shelf-metadata tests with failing design-contract tests**

```kotlin
package com.buddygames.center.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGamePresentationTest {
    @Test
    fun builtInGamesUseDesignOrderAndVectorLogos() {
        assertEquals(HomeGameLogo.Gomoku, homeGamePresentation("gomoku").logo)
        assertEquals(0, homeGamePresentation("gomoku").order)
        assertEquals(HomeGameLogo.Xiangqi, homeGamePresentation("xiangqi").logo)
        assertEquals(1, homeGamePresentation("xiangqi").order)
        assertEquals(HomeGameLogo.Othello, homeGamePresentation("othello").logo)
        assertEquals(2, homeGamePresentation("othello").order)
        assertEquals(HomeGameLogo.Generic, homeGamePresentation("sudoku").logo)
        assertEquals(Int.MAX_VALUE, homeGamePresentation("sudoku").order)
    }

    @Test
    fun wideTabletUsesFixedEqualSquareButtons() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.SquareRow,
                horizontalPaddingDp = 32,
                gapDp = 28,
                buttonSizeDp = 264,
                buttonHeightDp = 264,
                logoSizeDp = 112
            ),
            homeGameLayout(widthDp = 1200f, heightDp = 800f)
        )
    }

    @Test
    fun mediumTabletUsesThreeEqualAdaptiveSquares() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.SquareRow,
                horizontalPaddingDp = 24,
                gapDp = 20,
                buttonSizeDp = 237,
                buttonHeightDp = 237,
                logoSizeDp = 96
            ),
            homeGameLayout(widthDp = 800f, heightDp = 600f)
        )
    }

    @Test
    fun compactWidthUsesEqualHorizontalButtons() {
        assertEquals(
            HomeGameLayout(
                mode = HomeGameLayoutMode.CompactColumn,
                horizontalPaddingDp = 24,
                gapDp = 16,
                buttonSizeDp = null,
                buttonHeightDp = 112,
                logoSizeDp = 64
            ),
            homeGameLayout(widthDp = 599f, heightDp = 800f)
        )
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.buddygames.center.ui.HomeGamePresentationTest
```

Expected: Kotlin test compilation fails because the new Logo/layout types and properties do not exist.

- [ ] **Step 3: Implement the minimal pure Kotlin contract**

```kotlin
package com.buddygames.center.ui

internal enum class HomeGameLogo {
    Gomoku,
    Xiangqi,
    Othello,
    Generic
}

internal data class HomeGamePresentation(
    val logo: HomeGameLogo,
    val order: Int
)

internal enum class HomeGameLayoutMode {
    SquareRow,
    CompactColumn
}

internal data class HomeGameLayout(
    val mode: HomeGameLayoutMode,
    val horizontalPaddingDp: Int,
    val gapDp: Int,
    val buttonSizeDp: Int?,
    val buttonHeightDp: Int,
    val logoSizeDp: Int
)

internal fun homeGamePresentation(gameId: String): HomeGamePresentation = when (gameId) {
    "gomoku" -> HomeGamePresentation(HomeGameLogo.Gomoku, 0)
    "xiangqi" -> HomeGamePresentation(HomeGameLogo.Xiangqi, 1)
    "othello" -> HomeGamePresentation(HomeGameLogo.Othello, 2)
    else -> HomeGamePresentation(HomeGameLogo.Generic, Int.MAX_VALUE)
}

internal fun homeGameLayout(widthDp: Float, heightDp: Float): HomeGameLayout {
    if (widthDp < 600 || heightDp > widthDp) {
        return HomeGameLayout(
            mode = HomeGameLayoutMode.CompactColumn,
            horizontalPaddingDp = 24,
            gapDp = 16,
            buttonSizeDp = null,
            buttonHeightDp = 112,
            logoSizeDp = 64
        )
    }

    val wide = widthDp >= 960
    val horizontalPadding = if (wide) 32 else 24
    val gap = if (wide) 28 else 20
    val buttonSize = if (wide) {
        264
    } else {
        ((widthDp - horizontalPadding * 2 - gap * 2) / 3f)
            .toInt()
            .coerceAtMost(240)
    }

    return HomeGameLayout(
        mode = HomeGameLayoutMode.SquareRow,
        horizontalPaddingDp = horizontalPadding,
        gapDp = gap,
        buttonSizeDp = buttonSize,
        buttonHeightDp = buttonSize,
        logoSizeDp = if (wide) 112 else 96
    )
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.buddygames.center.ui.HomeGamePresentationTest
```

Expected: all responsive, Logo, ordering, portrait, and exact-boundary contract tests pass.

---

### Task 2: Add Compose vector game marks

**Files:**

- Create: `app/src/main/java/com/buddygames/center/ui/HomeGameLogo.kt`
- Verify: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`

**Interfaces:**

- Consumes: `HomeGameLogo` from Task 1.
- Produces: `HomeGameLogoMark(logo, fallbackSymbol, modifier)` for the home button.

- [ ] **Step 1: Implement the shared Logo well and vector marks**

Create one internal composable entrypoint:

```kotlin
@Composable
internal fun HomeGameLogoMark(
    logo: HomeGameLogo,
    fallbackSymbol: String,
    modifier: Modifier = Modifier
)
```

Implementation requirements:

- The outer modifier is a circular `#E5ECE9` well with a `#AEBDB9` border and a subtle inner white rim.
- `Gomoku` draws exactly five circles on a short diagonal, alternating `#242A2C` and `#F5F2E9`; white stones have dark outlines.
- `Xiangqi` draws two `#A33B33` rings and centers the serif “象” character.
- `Othello` draws black, white, and split black/white overlapping discs; all discs have dark outlines.
- `Generic` renders the first visible character of `fallbackSymbol`.
- The drawing must use `Canvas`, shapes, and `Text`; no raster Logo assets.

- [ ] **Step 2: Compile the app**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: build succeeds with no Kotlin compiler errors.

- [ ] **Step 3: Re-run the presentation tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.buddygames.center.ui.HomeGamePresentationTest
```

Expected: all presentation and responsive contract tests remain green.

---

### Task 3: Replace the shelf-card home with the approved tactile selector

**Files:**

- Modify: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`
- Verify: `app/src/test/java/com/buddygames/center/ui/HomeGamePresentationTest.kt`

**Interfaces:**

- Consumes: `homeGamePresentation`, `homeGameLayout`, and `HomeGameLogoMark`.
- Preserves: `GameCenterApp`, import launcher, `onOpen`, plugin loading, message states, and `ShellGameContext`.

- [ ] **Step 1: Replace the home visual tokens**

Use these exact semantic values:

```kotlin
private val MineralCanvas = Color(0xFFDDE5E3)
private val ButtonTop = Color(0xFFFCFDFB)
private val ButtonBottom = Color(0xFFF0F4F1)
private val LogoWell = Color(0xFFE5ECE9)
private val PressedSurface = Color(0xFFD8E3E0)
private val Ink = Color(0xFF17282C)
private val MutedInk = Color(0xFF56686C)
private val PrimaryTeal = Color(0xFF185864)
private val Border = Color(0xFFAEBDB9)
private val FocusRing = Color(0xFF08758A)
```

Keep existing semantic success, warning, and error colors for message states.

- [ ] **Step 2: Simplify the top app bar**

- Remove the seal and installed-dot decoration.
- Keep only “游戏中心” and an outlined import action.
- Show “导入游戏包” in non-compact landscape mode; use “导入” for narrow or portrait compact mode.
- Keep the action at least `48dp` high with `contentDescription = "导入游戏包"`.

- [ ] **Step 3: Implement responsive equal-button layouts**

- Use a full-window `BoxWithConstraints` and `homeGameLayout(maxWidth.value, maxHeight.value)`; share the resulting compact mode with both the top bar and content.
- Sort packages by `homeGamePresentation(gameId).order`, then display name.
- `SquareRow`: centered row of equal `.size(buttonSizeDp.dp)` buttons.
- `CompactColumn`: full-width `.height(112.dp)` buttons with `16dp` gaps.
- Use the layout contract’s padding, gap, height, and Logo size without ad-hoc per-game values.

- [ ] **Step 4: Implement tactile `GameSelectionButton`**

Create:

```kotlin
@Composable
private fun GameSelectionButton(
    gamePackage: GamePackage,
    logoSizeDp: Int,
    horizontalContent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
```

Requirements:

- One semantic button with `contentDescription = "打开 ${displayName}"`.
- Resting scale `1f`; pressed scale `0.985f` only when `ValueAnimator.areAnimatorsEnabled()`.
- Pressed background `#D8E3E0`; resting background is a subtle vertical brush from `ButtonTop` to `ButtonBottom`.
- Resting elevation `12dp`, pressed elevation `2dp`; retain a visible `1dp` border.
- Focus uses a visible `3dp #08758A` border and never changes layout dimensions.
- Square layout stacks Logo above text; compact layout places Logo left of text.
- Game name uses system sans, `24sp`, bold.

- [ ] **Step 5: Preserve transient and empty states**

- Render `ImportMessageBar` below “选择游戏” when a message exists.
- Empty copy: `还没有游戏。导入本地游戏包开始。`
- Empty action calls the same `onImport`.
- Keep success/warning/error messages actionable and readable.

- [ ] **Step 6: Remove obsolete shelf behavior**

- Delete `InstalledInlay`, `GameTile`, `ShelfBoardSurface`, and `loadPackageTexture`.
- Remove package-board texture reads and obsolete imports.
- Keep `loadAppTexture` and set the background texture alpha to `0.06f`.

- [ ] **Step 7: Run app verification**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: app unit tests pass.

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: the debug APK compiles successfully.

---

### Task 4: Delivery and visual smoke test

**Files:**

- Inspect: `app/src/main/java/com/buddygames/center/ui/GameCenterApp.kt`
- Inspect: `app/src/main/java/com/buddygames/center/ui/HomeGameLogo.kt`
- Inspect: `app/src/main/java/com/buddygames/center/ui/HomeGamePresentation.kt`

- [ ] **Step 1: Run the full repository gate**

Run:

```bash
npm run verify
```

Expected: unit tests, all three game packages, and the debug APK pass.

- [ ] **Step 2: Launch the app for a real-screen smoke test**

Run:

```bash
DETACH_EMULATOR=1 npm start
```

Expected: AVD `android_games_mvp_pad` boots, the APK installs, and `com.buddygames.center/.MainActivity` launches.

- [ ] **Step 3: Capture and inspect the emulator**

Run:

```bash
adb exec-out screencap -p > /tmp/android-games-home.png
```

Inspect the screenshot against `designs/previews/android-games-home-desktop.png` for:

- three equal buttons;
- order 五子棋 → 象棋 → 黑白棋;
- Logo-only imagery;
- visible boundaries and restrained depth;
- no board textures inside buttons;
- correct top-bar and title copy.

- [ ] **Step 4: Run the delivery checklist**

- Contrast and button boundaries remain clear.
- Touch targets and gaps meet the spec.
- Focus and press states are implemented.
- No Emoji, board thumbnails, metadata, status badges, or decorative dot strip remain.
- Remove one accessory: the old “installed inlay” dot strip is intentionally omitted.

- [ ] **Step 5: Review the final diff**

Run:

```bash
git diff --check
git status --short
git diff -- app/src/main/java/com/buddygames/center/ui app/src/test/java/com/buddygames/center/ui
```

Expected: only the scoped home UI/model/tests plus user-owned design and plan artifacts are changed; no secrets or generated APKs are staged.
