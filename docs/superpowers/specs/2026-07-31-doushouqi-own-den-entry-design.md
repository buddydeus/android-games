# Dou Shou Qi Own-Den Entry Design

## Goal

Allow either side's animals to enter, occupy, and leave their own den while preserving every other implemented Dou Shou Qi rule and terminal-result precedence.

This is an intentional local rule variant. It supersedes only the sentence “A piece may not enter its own den” in `2026-07-26-doushouqi-game-design.md`; all other requirements in that baseline remain authoritative.

## Rule Contract

- An empty own-den square is a legal destination for any animal that could otherwise move to that land square.
- A friendly piece occupying the own den still blocks entry under the normal friendly-occupancy rule.
- Enemy occupancy and capture on a den square follow the existing land capture rules.
- Entering, occupying, or leaving the mover's own den does not create a win or draw by itself.
- Entering the opponent's den continues to win immediately with `DoushouqiWinReason.DEN`.
- The behavior is identical in single-player and two-player modes and for Pine Green and Vermilion.

## Architecture

The immutable public rules and the primitive AI search position must expose the same legal moves. Remove the own-den destination rejection from both move generators; do not add a mode flag, new public API, dependency, or UI branch. Terminal adjudication remains opponent-den-specific and therefore requires no semantic change.

The session and Compose UI already consume legal moves from the authoritative rules, so they inherit the behavior without independent special cases. AI search must receive the same change so robot choices, mobility evaluation, no-legal-move adjudication, and public/search continuation parity remain consistent.

## Testing

Use test-driven development:

1. Change the existing public movement regression to require Pine Green to enter its own den while retaining the friendly-occupancy assertion.
2. Add the mirrored Vermilion own-den case so both orientations are protected.
3. Add primitive-search assertions for both own-den entries and confirm public/search move-list equivalence.
4. Preserve or add an enemy-den terminal assertion proving that only the opponent's den produces a `DEN` win.
5. Run the Dou Shou Qi module tests, then the repository-wide verification gate.

## Release And Documentation

This game-only rules change increments only the Dou Shou Qi package from `0.0.7` / version code `7` to `0.0.8` / version code `8`. Keep `DoushouqiManifest`, `package/manifest.json`, manifest tests, package asset identity tests, `games/doushouqi/README.md`, root `README.md`, and `AGENTS.md` aligned. The game-center shell version remains unchanged.

No game API, package loader, root packaging task, Android dependency, asset, or other game module changes are in scope.
