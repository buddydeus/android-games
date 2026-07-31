# Doushouqi Universal Trap Capture Design

## Goal

Treat every occupied trap as a zero-rank defensive square regardless of trap ownership, so any enemy animal may capture a defender standing in any of the six traps.

This is an intentional local rule variant. It replaces only the ownership-qualified trap weakening rule in `2026-07-26-doushouqi-game-design.md`; all other movement, capture, terminal, session, UI, and package requirements remain authoritative.

## Rule Contract

- A defender occupying any Vermilion or Pine Green trap has effective defensive rank zero.
- Any enemy animal whose move otherwise legally reaches the occupied trap may capture that defender, regardless of either side's identity or normal ranks.
- The trap override includes an Elephant capturing an enemy Rat standing in a trap.
- Friendly occupancy remains non-capturable.
- All traps are land squares, so existing Rat land/river boundary restrictions remain unchanged.
- A trapped animal leaving the trap attacks with its normal rank; trap weakening applies to the defender at the destination, not the attacker at the source.
- The rule is identical in single-player and two-player modes and for both board orientations.

## Architecture

The immutable public rules and primitive AI search position must use the same ownership-independent trap predicate when evaluating a defender. Replace the attacker-owned-trap checks in both capture functions with a check for any trap at the destination. Keep the trap override before the Rat/Elephant special cases so every attacker truly can capture a trapped defender.

AI evaluation and ordering must describe the same risk model. Penalize every piece occupying any trap, not only a piece in the opponent's trap. Remove the existing move-order bonus for entering the mover's own trap because own traps are no longer safe or intrinsically favorable. Do not add a rule flag, new API, UI branch, dependency, or asset change.

## Testing

Use test-driven development:

1. Preserve coverage for a low-ranked attacker capturing a high-ranked defender in the attacker's trap.
2. Add the inverse case: a low-ranked attacker captures a high-ranked defender standing in the defender's own trap.
3. Prove an Elephant may capture an enemy Rat in either side's trap while the same land capture remains illegal outside a trap.
4. Preserve the test that a trapped attacker regains normal rank when leaving.
5. Add public/primitive move-list parity for ownership-independent trap captures.
6. Add deterministic AI tactical coverage for a winning capture of a high-ranked defender in its own trap.
7. Run the Doushouqi module tests and the repository-wide verification gate.

## Release And Documentation

This game-only rules and AI change increments only Doushouqi from `0.0.8` / version code `8` to `0.0.9` / version code `9`. Keep `DoushouqiManifest`, `package/manifest.json`, manifest tests, module and root README files, the baseline design, this increment design, and `AGENTS.md` aligned. The game-center shell version remains unchanged.

No game API, package loader, root packaging task, Android dependency, asset, or other game module changes are in scope.
