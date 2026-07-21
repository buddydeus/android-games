# Task 3 Report: Deployment, Battles, And Terminal Rules

## Scope

Implemented only the approved Task 3 Junqi deployment, battle adjudication, commander reveal, and terminal-rule foundation.

Production files:

- `games/junqi/src/main/java/com/buddygames/junqi/JunqiDeployment.kt`
- `games/junqi/src/main/java/com/buddygames/junqi/JunqiModel.kt`
- `games/junqi/src/main/java/com/buddygames/junqi/JunqiRules.kt`
- `AGENTS.md`

Tests:

- `games/junqi/src/test/java/com/buddygames/junqi/JunqiDeploymentTest.kt`
- `games/junqi/src/test/java/com/buddygames/junqi/JunqiBattleTest.kt`
- `games/junqi/src/test/java/com/buddygames/junqi/JunqiTerminalTest.kt`

## RED Evidence

Before adding any Task 3 production API, ran:

```sh
./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiDeploymentTest' --tests '*JunqiBattleTest' --tests '*JunqiTerminalTest'
```

Result: expected `:games:junqi:compileDebugUnitTestKotlin FAILED`. The compiler reported unresolved `JunqiDeployment`, `JunqiBattleOutcome`, `JunqiResult`, `JunqiRules.battleOutcome`, `JunqiRules.applyMove`, `JunqiRules.terminalResult`, and the new immutable-state properties.

The fixtures were then checked against the existing Task 2 movement graph. Battle fixtures were kept off occupied camps so GREEN would exercise adjudication rather than violate camp protection.

## Exact Semantics

### Deployment

- Each side deploys exactly the approved 25-piece inventory on all 25 non-camp positions in its six-row deployment region.
- Piece IDs and positions must be unique, and every piece must belong to the requested side.
- The flag must occupy one of that side's two headquarters.
- Mines must remain in rows `0..1` for Blue or `10..11` for Red.
- Bombs may not occupy Blue row `5` or Red row `6`.
- `default(side)` is deterministic.
- `random(side, seed)` uses only the supplied seed and deterministically places constrained pieces before shuffling the remaining ranks and positions.
- `swapIfLegal` swaps two occupied positions only when the complete resulting deployment remains legal. Missing or illegal swaps return the original list instance unchanged.

### Battles And State

- Ordinary ranks are Commander `9`, Army Commander `8`, Division Commander `7`, Brigade Commander `6`, Regiment `5`, Battalion `4`, Company `3`, Platoon `2`, and Engineer `1`.
- Higher ordinary rank survives; equal ranks are both removed.
- A bomb on either side of a collision removes both pieces, including bomb-versus-mine and bomb-versus-flag.
- An attacking engineer survives a mine; every other ordinary rank is removed while the mine survives.
- Every movable non-bomb rank captures a flag. A bomb that removes a flag also counts as flag capture.
- `JunqiRules.applyMove` rejects finished games and moves absent from the current side's legal move list.
- Surviving attackers move to the destination with `hasMoved = true`; collision outcome is retained as `ATTACKER_WINS`, `DEFENDER_WINS`, or `BOTH_REMOVED`.
- Removal of an attacking or defending commander permanently adds that commander's side to the immutable `revealedFlags` set.
- A collision resets `quietHalfMoves` to zero. A non-collision increments it by one.

### Terminal Precedence

After a legal move, `currentSide` changes to the opponent and results are resolved in this order:

1. Capturing the defending flag immediately wins for the attacker, including bomb-versus-flag.
2. Completing quiet half-move `31` makes the mover lose (`SL[31]`).
3. If the next side has no legal move, that side loses; if neither side has a legal move, the result is a draw.

`JunqiRules.terminalResult` applies the same mobility rule to an already-constructed non-terminal state.

## GREEN Evidence

Focused Task 3 suite:

```sh
./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiDeploymentTest' --tests '*JunqiBattleTest' --tests '*JunqiTerminalTest'
```

Result: `BUILD SUCCESSFUL`.

Complete Junqi suite:

```sh
./gradlew :games:junqi:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL`; 43 tests passed with 0 skipped, failures, or errors: 9 deployment, 8 battle, 10 terminal, 5 board, 10 movement, and 1 manifest test.

Repository gate:

```sh
npm run verify
git diff --check
```

Result: `BUILD SUCCESSFUL`; 157 Gradle tasks completed with no test/package verification failure, and the diff had no whitespace errors.

## Commit

- `26e896996796ea0bbf1ef8e33c1d1aad8995c782 feat: adjudicate junqi deployment and battles`

## Review

No P0-P3 findings after checking the implementation and tests line by line against the Task 3 brief and approved rule spec. Multi-agent review was unavailable because this workspace does not expose the review-agent tools, so the review gate was completed directly from the final diff and verification evidence.

## Concerns

No Task 3 correctness concern remains. Hidden-information observations, knowledge updates, AI, session flow, UI, and Junqi package integration are intentionally deferred to the later approved tasks. The repository package gate still validates the currently integrated four built-in game zips; it compiles and runs Junqi tests but does not package Junqi yet.
