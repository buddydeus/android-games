#!/usr/bin/env python3
"""Generate Xiangqi PNG assets from the approved ceramic master and board tokens."""

from __future__ import annotations

import argparse
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


BOARD_SIZE = (1600, 1500)
GRID_LEFT = 128
GRID_TOP = 110
GRID_RIGHT = 1472
GRID_BOTTOM = 1360
PIECE_SIZE = 1024
REQUIRED_GLYPHS = "帥俥傌炮相仕兵將車馬砲象士卒楚河漢界"
PIECE_MASTER = Path(__file__).resolve().parent / "source" / "ceramic-piece-master.png"

FONT_CANDIDATES = (
    Path("/System/Library/Fonts/Supplemental/Songti.ttc"),
    Path("/System/Library/AssetsV2/com_apple_MobileAsset_Font7/54a2ad3dac6cac875ad675d7d273dc425010a877.asset/AssetData/Kaiti.ttc"),
    Path("/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc"),
)

PIECES = (
    ("red-general.png", "帥", "red"),
    ("red-rook.png", "俥", "red"),
    ("red-horse.png", "傌", "red"),
    ("red-cannon.png", "炮", "red"),
    ("red-elephant.png", "相", "red"),
    ("red-advisor.png", "仕", "red"),
    ("red-soldier.png", "兵", "red"),
    ("black-general.png", "將", "black"),
    ("black-rook.png", "車", "black"),
    ("black-horse.png", "馬", "black"),
    ("black-cannon.png", "砲", "black"),
    ("black-elephant.png", "象", "black"),
    ("black-advisor.png", "士", "black"),
    ("black-soldier.png", "卒", "black"),
)


FontSpec = tuple[Path, int]


def supports_required_glyphs(font: ImageFont.FreeTypeFont) -> bool:
    return all(font.getmask(glyph).getbbox() is not None for glyph in REQUIRED_GLYPHS)


def resolve_font(explicit: Path | None) -> FontSpec:
    if explicit is not None:
        if not explicit.is_file():
            raise FileNotFoundError(f"Font does not exist: {explicit}")
        candidates = (explicit,)
    else:
        candidates = FONT_CANDIDATES
    for candidate in candidates:
        if candidate.is_file():
            for index in range(16):
                try:
                    font = ImageFont.truetype(str(candidate), 96, index=index)
                except OSError:
                    break
                if supports_required_glyphs(font):
                    return candidate, index
    raise FileNotFoundError(
        "No CJK serif font with all Xiangqi glyphs found; pass --font /path/to/font"
    )


def load_font(font_spec: FontSpec, size: int) -> ImageFont.FreeTypeFont:
    path, index = font_spec
    return ImageFont.truetype(str(path), size, index=index)


def soft_material(
    size: tuple[int, int],
    base: tuple[int, int, int],
    seed: int,
    variation: int = 7,
) -> Image.Image:
    width, height = size
    rng = random.Random(seed)
    small_width = max(24, width // 24)
    small_height = max(24, height // 24)
    noise_bytes = bytes(rng.randrange(96, 161) for _ in range(small_width * small_height))
    noise = Image.frombytes("L", (small_width, small_height), noise_bytes)
    noise = noise.resize(size, Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(12))
    texture = Image.new("RGB", size, base)
    pixels = texture.load()
    noise_pixels = noise.load()
    for y in range(height):
        vertical_tone = int(2 * (0.5 - y / max(1, height - 1)))
        for x in range(width):
            amount = (noise_pixels[x, y] - 128) / 64
            pixels[x, y] = tuple(
                max(0, min(255, int(channel + amount * variation + vertical_tone)))
                for channel in base
            )
    return texture


def paste_with_rounded_mask(
    canvas: Image.Image,
    texture: Image.Image,
    box: tuple[int, int, int, int],
    radius: int,
) -> None:
    left, top, right, bottom = box
    mask = Image.new("L", (right - left, bottom - top), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, mask.width - 1, mask.height - 1), radius=radius, fill=255)
    canvas.paste(texture.resize(mask.size, Image.Resampling.LANCZOS), (left, top), mask)


def centered_text(
    draw: ImageDraw.ImageDraw,
    center: tuple[float, float],
    text: str,
    font: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int, int],
    stroke_fill: tuple[int, int, int, int] | None = None,
    stroke_width: int = 0,
) -> None:
    bounds = draw.textbbox((0, 0), text, font=font, stroke_width=stroke_width)
    width = bounds[2] - bounds[0]
    height = bounds[3] - bounds[1]
    draw.text(
        (center[0] - width / 2 - bounds[0], center[1] - height / 2 - bounds[1]),
        text,
        font=font,
        fill=fill,
        stroke_fill=stroke_fill,
        stroke_width=stroke_width,
    )


def generate_board(output: Path, font_spec: FontSpec) -> None:
    width, height = BOARD_SIZE
    board = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))

    shadow = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (32, 34, width - 18, height - 12),
        radius=14,
        fill=(34, 54, 49, 72),
    )
    board.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(17)))

    outer_box = (24, 18, width - 24, height - 30)
    outer = soft_material(
        (outer_box[2] - outer_box[0], outer_box[3] - outer_box[1]),
        (169, 200, 190),
        seed=41,
        variation=7,
    )
    paste_with_rounded_mask(board, outer, outer_box, 11)

    draw = ImageDraw.Draw(board, "RGBA")
    draw.rounded_rectangle(outer_box, 11, outline=(95, 130, 119, 255), width=4)
    draw.rounded_rectangle(
        (33, 27, width - 33, height - 39),
        8,
        outline=(226, 239, 234, 210),
        width=2,
    )

    field_box = (52, 48, width - 52, height - 52)
    field = soft_material(
        (field_box[2] - field_box[0], field_box[3] - field_box[1]),
        (250, 247, 239),
        seed=73,
        variation=4,
    )
    paste_with_rounded_mask(board, field, field_box, 3)
    draw.rounded_rectangle(field_box, 3, outline=(102, 137, 127, 220), width=3)
    draw.line(
        (field_box[0] + 3, field_box[1] + 3, field_box[2] - 3, field_box[1] + 3),
        fill=(255, 255, 255, 205),
        width=3,
    )

    line_color = (39, 68, 63, 245)
    line_width = 2
    dx = (GRID_RIGHT - GRID_LEFT) / 8
    dy = (GRID_BOTTOM - GRID_TOP) / 9

    def x_at(col: int) -> int:
        return round(GRID_LEFT + col * dx)

    def y_at(row: int) -> int:
        return round(GRID_TOP + row * dy)

    river_top = GRID_TOP + 4 * dy
    river_bottom = GRID_TOP + 5 * dy
    draw.rectangle(
        (GRID_LEFT + 2, round(river_top) + 2, GRID_RIGHT - 2, round(river_bottom) - 2),
        fill=(226, 236, 231, 242),
    )
    for row in range(10):
        y = y_at(row)
        draw.line((GRID_LEFT, y, GRID_RIGHT, y), fill=line_color, width=line_width)
    for col in range(9):
        x = x_at(col)
        draw.line((x, GRID_TOP, x, y_at(4)), fill=line_color, width=line_width)
        draw.line((x, y_at(5), x, GRID_BOTTOM), fill=line_color, width=line_width)

    draw.line((x_at(3), y_at(0), x_at(5), y_at(2)), fill=line_color, width=line_width)
    draw.line((x_at(5), y_at(0), x_at(3), y_at(2)), fill=line_color, width=line_width)
    draw.line((x_at(3), y_at(7), x_at(5), y_at(9)), fill=line_color, width=line_width)
    draw.line((x_at(5), y_at(7), x_at(3), y_at(9)), fill=line_color, width=line_width)

    river_font = load_font(font_spec, 60)
    river_y = GRID_TOP + int(4.5 * dy)
    centered_text(
        draw,
        (GRID_LEFT + 1.35 * dx, river_y),
        "楚 河",
        river_font,
        (39, 68, 63, 245),
    )
    centered_text(
        draw,
        (GRID_LEFT + 6.65 * dx, river_y),
        "漢 界",
        river_font,
        (39, 68, 63, 245),
    )
    draw.rectangle((0, 0, 7, 7), fill=(0, 0, 0, 0))
    draw.rectangle((width - 8, 0, width - 1, 7), fill=(0, 0, 0, 0))
    draw.rectangle((0, height - 8, 7, height - 1), fill=(0, 0, 0, 0))
    draw.rectangle(
        (width - 8, height - 8, width - 1, height - 1),
        fill=(0, 0, 0, 0),
    )
    board.save(output, "PNG", optimize=True)


def generate_piece(output: Path, glyph: str, side: str, font_spec: FontSpec) -> None:
    if not PIECE_MASTER.is_file():
        raise FileNotFoundError(f"Missing approved ceramic piece master: {PIECE_MASTER}")
    image = Image.open(PIECE_MASTER).convert("RGBA")
    image = image.resize((PIECE_SIZE, PIECE_SIZE), Image.Resampling.LANCZOS)
    draw = ImageDraw.Draw(image, "RGBA")
    center = (512, 500)
    ink = (178, 48, 42, 255) if side == "red" else (28, 49, 64, 255)
    ink_shadow = (105, 24, 21, 95) if side == "red" else (10, 26, 35, 95)
    glyph_font = load_font(font_spec, 356)
    centered_text(
        draw,
        (center[0] + 3, center[1] + 4),
        glyph,
        glyph_font,
        ink_shadow,
        stroke_fill=ink_shadow,
        stroke_width=2,
    )
    centered_text(
        draw,
        center,
        glyph,
        glyph_font,
        ink,
        stroke_fill=ink,
        stroke_width=1,
    )
    image.save(output, "PNG", optimize=True)


def build_contact_sheet(piece_directory: Path, output: Path) -> None:
    thumb_size = 260
    sheet = Image.new("RGBA", (thumb_size * 7, thumb_size * 2), (234, 241, 239, 255))
    for index, (file_name, _, _) in enumerate(PIECES):
        piece = Image.open(piece_directory / file_name).convert("RGBA")
        piece.thumbnail((thumb_size, thumb_size), Image.Resampling.LANCZOS)
        x = (index % 7) * thumb_size
        y = (index // 7) * thumb_size
        sheet.alpha_composite(piece, (x, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, "PNG", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "package" / "assets",
    )
    parser.add_argument("--font", type=Path)
    parser.add_argument("--contact-sheet", type=Path)
    args = parser.parse_args()

    font_spec = resolve_font(args.font)
    board_directory = args.output / "board"
    piece_directory = args.output / "pieces"
    board_directory.mkdir(parents=True, exist_ok=True)
    piece_directory.mkdir(parents=True, exist_ok=True)

    generate_board(board_directory / "xiangqi-board.png", font_spec)
    for file_name, glyph, side in PIECES:
        generate_piece(piece_directory / file_name, glyph, side, font_spec)
    if args.contact_sheet is not None:
        build_contact_sheet(piece_directory, args.contact_sheet)


if __name__ == "__main__":
    main()
