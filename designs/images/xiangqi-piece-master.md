# Xiangqi Ceramic Piece Master

**Generated asset:** `games/xiangqi/tools/source/ceramic-piece-master.png`
**Visual reference:** `designs/previews/xiangqi-ui-menu.png`
**Spec SSOT:** `designs/specs/xiangqi-ui.md`

## Role

This transparent blank piece is the single material source for all fourteen Xiangqi piece PNGs. The local generator resizes it and adds exact Song-style traditional glyphs; image generation is never used for piece text.

## Generation Prompt

Create exactly one blank Chinese chess piece matching the reference image's tactile ceramic style. Use a circular warm ivory glazed porcelain disc viewed perfectly straight-on from above, with a slim double brass-gold rim, rounded raised edge, subtle glaze variation, bright highlight on the upper-left bevel, soft occlusion on the lower-right bevel, and a restrained soft cast shadow below and to the right. Keep the center face completely blank with no glyph or symbol. Center the piece with generous even padding on a perfectly flat solid `#00FF00` chroma-key background. No text, board, grid, extra object, watermark, wood, or stone.

The built-in image generation path produced the chroma-key source. The standard `remove_chroma_key.py` helper converted it to the committed transparent PNG while preserving the partially transparent shadow.
