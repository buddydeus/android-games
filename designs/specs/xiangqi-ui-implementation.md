# Xiangqi UI Implementation Handoff

**SSOT:** `designs/specs/xiangqi-ui.md`

## Delivery Sequence

1. Generate and validate the 1440 x 1600 complete board texture.
2. Generate and validate all 14 1024 x 1024 transparent piece textures as one matched family.
3. Lock texture filenames, dimensions, alpha corners, visual coverage, and glyph mapping in tests.
4. Update the Compose backdrop, menu rail, game rail, selection treatment, and texture scale from the SSOT tokens.
5. Verify red-side and black-side board orientation at 800 x 600 and a larger tablet viewport.

## Asset Acceptance

- The board is orthographic, bright, complete, and registered to the existing grid fractions.
- Every piece is a separate transparent PNG and shows exactly one correct Chinese glyph.
- Red and Black piece families share identical shape, size, lighting, and crop.
- Piece edges remain distinguishable over every board area.
- Latest-move and selection overlays remain outside the visible piece boundary.
- No generated watermark, background, accidental extra glyph, or mixed visual style remains.

## Engineering Guardrails

- Decode assets once per installed package version.
- Keep transparent-margin trimming in memory and preserve source files unchanged.
- Keep board and piece fallbacks available for missing or invalid package assets.
- Preserve 48 dp controls, Compose semantics, high-contrast status labels, and stable board geometry.
- Increment only the Xiangqi package version when implementation begins.

## Explicit Non-goals

- No changes to Xiangqi rules or AI.
- No game-center shell update.
- No online asset loading.
- No animated or 3D board.
- No shared asset dependency outside the Xiangqi package.
