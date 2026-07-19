# International Chess Piece PNG Asset Prompts

**Spec SSOT:** `designs/specs/international-chess-ui.md`
**Final directory:** `games/chess/package/assets/pieces/`

## Common generation contract

Use case: stylized-concept
Asset type: Android board-game piece texture, one isolated chess piece per image
Scene/backdrop: perfectly flat solid #00FF00 chroma-key background for background removal
Style/medium: polished stylized 3D product render, modern Staunton tournament piece, physical but restrained
Composition/framing: square 1024 x 1024, one piece centered, slightly elevated front/top three-quarter camera, full base and top visible, 10% safe padding
Lighting/mood: soft daylight key from upper-left, subtle self-shadowing only, consistent across the complete set
White material: matte ivory porcelain #F2EEE2 with a fine graphite edge and very subtle cool reflected light
Black material: soft-black obsidian #202826 with readable dark-grey planes and a restrained cool rim light
Constraints: the background is one uniform #00FF00 color with no shadows, gradients, texture, reflections, floor plane, or lighting variation; crisp silhouette; no cast shadow; no contact shadow; no reflection; no text; no watermark; do not use #00FF00 in the piece
Avoid: wood grain, gold trim, jewels, heraldry, fantasy ornament, cartoon face, extreme perspective, cropped base, cropped top

## Asset prompts

| File | Subject-specific prompt |
| --- | --- |
| `white-king.png` | One White Staunton king, conventional restrained cross finial, tallest piece in the family, matte ivory porcelain material. |
| `white-queen.png` | One White Staunton queen, recognizable open crown with evenly spaced points, slightly shorter than the king, matte ivory porcelain material. |
| `white-rook.png` | One White Staunton rook, cylindrical tower with a clean crenellated top, broad stable base, matte ivory porcelain material. |
| `white-bishop.png` | One White Staunton bishop, pointed mitre with one clean diagonal slot, slender neck, matte ivory porcelain material. |
| `white-knight.png` | One White Staunton knight, calm carved horse head facing screen-right, strong readable mane plane, matte ivory porcelain material. |
| `white-pawn.png` | One White Staunton pawn, simple spherical head and compact balanced base, matte ivory porcelain material. |
| `black-king.png` | One Black Staunton king, conventional restrained cross finial, tallest piece in the family, soft-black obsidian material. |
| `black-queen.png` | One Black Staunton queen, recognizable open crown with evenly spaced points, slightly shorter than the king, soft-black obsidian material. |
| `black-rook.png` | One Black Staunton rook, cylindrical tower with a clean crenellated top, broad stable base, soft-black obsidian material. |
| `black-bishop.png` | One Black Staunton bishop, pointed mitre with one clean diagonal slot, slender neck, soft-black obsidian material. |
| `black-knight.png` | One Black Staunton knight, calm carved horse head facing screen-left, strong readable mane plane, soft-black obsidian material. |
| `black-pawn.png` | One Black Staunton pawn, simple spherical head and compact balanced base, soft-black obsidian material. |

## Post-processing

Run the bundled `remove_chroma_key.py` helper with border auto-key sampling, soft matte, despill, and one-pixel edge contraction if required. Validate RGBA mode, 1024 x 1024 dimensions, transparent corners, edge clearance, and useful alpha coverage before packaging.
