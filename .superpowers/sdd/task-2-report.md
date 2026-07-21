# Task 2 Report: Board Graph And Legal Movement

## Scope

Implemented the approved immutable Junqi board graph and legal movement foundation only.

Files changed for Task 2:

- `AGENTS.md`
- `games/junqi/src/main/java/com/buddygames/junqi/JunqiModel.kt`
- `games/junqi/src/main/java/com/buddygames/junqi/JunqiBoard.kt`
- `games/junqi/src/main/java/com/buddygames/junqi/JunqiRules.kt`
- `games/junqi/src/test/java/com/buddygames/junqi/JunqiBoardTest.kt`
- `games/junqi/src/test/java/com/buddygames/junqi/JunqiMovementTest.kt`

## RED Evidence

1. Before production code existed, ran:

   ```sh
   ./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiBoardTest' --tests '*JunqiMovementTest'
   ```

   Result: expected `:games:junqi:compileDebugUnitTestKotlin FAILED` with unresolved `JunqiPosition`, `JunqiBoard`, `JunqiRules`, and related model APIs.

2. During self-review, added a focused duplicate-move test and ran:

   ```sh
   ./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiMovementTest.moveListDoesNotDuplicateASharedRoadAndRailDestination'
   ```

   Result: expected test failure at `JunqiMovementTest.kt:34`, proving overlapping road and railway edges initially produced duplicate moves.

## GREEN Evidence

Ran:

```sh
./gradlew :games:junqi:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL`; 11 tests passed: 3 board-graph tests, 7 movement tests, and 1 existing manifest test. `git diff --check` also passed before commit.

## Commit

- `34ec51199479a7d00119eafde0ec9fd0a5664bcc feat: implement junqi board movement`

## Self-review

No P0-P3 findings. Reviewed the exact 60-node graph, symmetric adjacency, railway blocking, engineer-only turns, non-engineer straight scans, camp attack protection, headquarters and immobile-piece locks, immutable map/set exposure, legal-move uniqueness, and scoped file changes.

## Concerns

None. Deployment, battle adjudication, hidden-information state, and session behavior remain intentionally deferred to later approved tasks.

## Review Fix Evidence (2026-07-22)

### RED Evidence

Added mutation-rejection tests for `JunqiBoard.headquarters` and `JunqiBoard.camps`, then ran:

```sh
./gradlew :games:junqi:testDebugUnitTest --tests '*JunqiBoardTest'
```

Result: expected `BUILD FAILED`; both tests failed because a `MutableSet` cast could add a position to the public sets.

### Fix And Coverage

- `headquarters` and `camps` now return defensive `Collections.unmodifiableSet` views over copied positions.
- Engineer BFS tests now prove that neither a friendly occupied railway node nor a capturable enemy occupied railway node is traversed.
- `legalMovesUsesExplicitlyRequestedBlueSideRegardlessOfCurrentSide` documents and covers the API contract: `currentSide` is session state, while `legalMoves(state, side)` generates moves only for its explicit `side` argument.

### GREEN Evidence

Ran:

```sh
./gradlew :games:junqi:testDebugUnitTest
git diff --check
```

Result: `BUILD SUCCESSFUL`; all 16 Junqi unit tests passed and the diff had no whitespace errors.

### Remaining Concerns

None for these review findings. Later Junqi tasks still own deployment, battle adjudication, hidden-information state, and session behavior.
