# International Chess UI Implementation Handoff

**SSOT:** `designs/specs/international-chess-ui.md`

## Asset map

| Side | Type | Package path |
| --- | --- | --- |
| White | King | `assets/pieces/white-king.png` |
| White | Queen | `assets/pieces/white-queen.png` |
| White | Rook | `assets/pieces/white-rook.png` |
| White | Bishop | `assets/pieces/white-bishop.png` |
| White | Knight | `assets/pieces/white-knight.png` |
| White | Pawn | `assets/pieces/white-pawn.png` |
| Black | King | `assets/pieces/black-king.png` |
| Black | Queen | `assets/pieces/black-queen.png` |
| Black | Rook | `assets/pieces/black-rook.png` |
| Black | Bishop | `assets/pieces/black-bishop.png` |
| Black | Knight | `assets/pieces/black-knight.png` |
| Black | Pawn | `assets/pieces/black-pawn.png` |

## Rendering contract

- Resolve assets relative to the installed Chess game package root.
- Decode each PNG once per package instance and cache by `ChessPiece`.
- Keep logical board geometry independent from image dimensions.
- Fit each bitmap inside 82% of a square with centered alignment.
- Preserve the existing 180-degree board coordinate mapping for a Black player; piece bitmaps stay upright.
- Draw state overlays above the square background and below or around the piece as specified by the SSOT.
- Missing or invalid piece assets must fall back to the current Unicode glyph so the game remains playable.

## Verification targets

- All 12 files are 1024 x 1024 RGBA PNG.
- Every corner pixel is transparent.
- Alpha coverage stays within a useful 25-70% range.
- No piece touches the image edge.
- White and Black pieces remain distinguishable on both board square colors.
- Package zip contains all 12 assets at the exact paths above.
- Emulator screenshots cover menu, active game, selected piece, legal capture, latest move, check, and Black-player rotation.
