# Game Package Logos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the four text placeholders with a consistent family of circular PNG logos supplied by each game package.

**Architecture:** Each game owns `package/assets/icon.png` and declares it through `manifest.icon`. The shell remains package-agnostic and continues using its existing bounded bitmap loader.

**Tech Stack:** PNG assets, game package manifests, Kotlin/JUnit, Gradle zip packaging

## Global Constraints

- Source images are square `1024 x 1024` PNG files.
- Package path is exactly `assets/icon.png`.
- Bump all four game versions once; keep shell version at `0.0.3`.
- Do not add dependencies or game-ID presentation branches to the shell.

---

### Task 1: Lock Package Asset Contract

**Files:**
- Create: `app/src/test/java/com/buddygames/center/packages/GamePackageLogoAssetsTest.kt`
- Modify: four existing game version regression tests

- [ ] Write failing tests requiring each manifest to reference a readable square PNG and requiring the next game version.
- [ ] Run `./gradlew :app:testDebugUnitTest :games:gomoku:testDebugUnitTest :games:othello:testDebugUnitTest :games:xiangqi:testDebugUnitTest :games:chess:testDebugUnitTest`.
- [ ] Confirm failures report missing `icon.png` and old game versions.

### Task 2: Generate and Install Logos

**Files:**
- Create: `games/{gomoku,othello,xiangqi,chess}/package/assets/icon.png`
- Create: `designs/previews/android-game-logo-{gomoku,othello,xiangqi,chess}.png`
- Delete: `games/{gomoku,othello,xiangqi,chess}/package/assets/icon.txt`

- [ ] Generate each logo from its paired `designs/images/android-game-logo-*.md` prompt.
- [ ] Inspect each image at full size and as a contact sheet.
- [ ] Copy approved images to both preview and package paths.

### Task 3: Align Manifests, Versions, and Packaging

**Files:**
- Modify: four `*Plugin.kt` manifests
- Modify: four `package/manifest.json` files
- Modify: `build.gradle.kts`

- [ ] Point all package manifests to `assets/icon.png`.
- [ ] Bump Gomoku, Othello, and Chess to `0.0.5`; bump Xiangqi to `0.0.8`.
- [ ] Require `assets/icon.png` in `verifyGamePackages`.
- [ ] Run the targeted tests until they pass.

### Task 4: Document and Verify

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/agents/game-plugins.md`

- [ ] Document the packaged logo family and current game versions.
- [ ] Run `npm run verify`.
- [ ] Check staged assets and create one scoped local commit without including unrelated `desgins/`.
