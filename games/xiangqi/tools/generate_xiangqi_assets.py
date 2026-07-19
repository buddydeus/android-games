#!/usr/bin/env python3
"""Generate deterministic antique Xiangqi board and piece PNG assets."""

from __future__ import annotations

import argparse
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


BOARD_SIZE = (1440, 1600)
GRID_LEFT = 192
GRID_TOP = 190
GRID_RIGHT = 1248
GRID_BOTTOM = 1378
PIECE_SIZE = 1024

FONT_CANDIDATES = (
    Path("/System/Library/AssetsV2/com_apple_MobileAsset_Font7/54a2ad3dac6cac875ad675d7d273dc425010a877.asset/AssetData/Kaiti.ttc"),
    Path("/System/Library/Fonts/Supplemental/Songti.ttc"),
    Path("/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc"),
)

PIECES = (
    ("red-general.png", "帥", "red"),
    ("red-rook.png", "車", "red"),
    ("red-horse.png", "馬", "red"),
    ("red-cannon.png", "炮", "red"),
    ("red-elephant.png", "相", "red"),
    ("red-advisor.png", "仕", "red"),
    ("red-soldier.png", "兵", "red"),
    ("black-general.png", "將", "black"),
    ("black-rook.png", "車", "black"),
    ("black-horse.png", "馬", "black"),
    ("black-cannon.png", "炮", "black"),
    ("black-elephant.png", "象", "black"),
    ("black-advisor.png", "士", "black"),
    ("black-soldier.png", "卒", "black"),
)


def resolve_font(explicit: Path | None) -> Path:
    if explicit is not None:
        if not explicit.is_file():
            raise FileNotFoundError(f"Font does not exist: {explicit}")
        return explicit
    for candidate in FONT_CANDIDATES:
        if candidate.is_file():
            return candidate
    raise FileNotFoundError("No CJK serif font found; pass --font /path/to/font")


def textured_wood(
    size: tuple[int, int],
    light: tuple[int, int, int],
    dark: tuple[int, int, int],
    seed: int,
    grain_strength: int = 28,
) -> Image.Image:
    width, height = size
    rng = random.Random(seed)
    small_width = max(24, width // 12)
    small_height = max(24, height // 12)
    noise_bytes = bytes(rng.randrange(80, 176) for _ in range(small_width * small_height))
    noise = Image.frombytes("L", (small_width, small_height), noise_bytes)
    noise = noise.resize(size, Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(8))
    texture = Image.new("RGB", size, light)
    pixels = texture.load()
    noise_pixels = noise.load()
    for y in range(height):
        vertical_tone = int(5 * (y / max(1, height - 1) - 0.5))
        for x in range(width):
            amount = (noise_pixels[x, y] - 128) / 128
            pixels[x, y] = tuple(
                max(0, min(255, int(light[index] + (dark[index] - light[index]) * amount * 0.42 + vertical_tone)))
                for index in range(3)
            )
    draw = ImageDraw.Draw(texture, "RGBA")
    for _ in range(max(12, height // 14)):
        y = rng.randrange(height)
        x = rng.randrange(-width // 5, width)
        length = rng.randrange(max(16, width // 18), max(24, width // 2))
        color = (65, 38, 18, rng.randrange(10, grain_strength))
        draw.arc((x, y - 4, x + length, y + 4), 180, 360, fill=color, width=rng.choice((1, 1, 2)))
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


def draw_registration_mark(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    left: bool,
    right: bool,
    color: tuple[int, int, int, int],
) -> None:
    gap = 14
    arm = 24
    offset = 10
    width = 4
    if left:
        draw.line((x - gap, y - offset, x - gap - arm, y - offset), fill=color, width=width)
        draw.line((x - gap, y - offset, x - gap, y - offset - arm), fill=color, width=width)
        draw.line((x - gap, y + offset, x - gap - arm, y + offset), fill=color, width=width)
        draw.line((x - gap, y + offset, x - gap, y + offset + arm), fill=color, width=width)
    if right:
        draw.line((x + gap, y - offset, x + gap + arm, y - offset), fill=color, width=width)
        draw.line((x + gap, y - offset, x + gap, y - offset - arm), fill=color, width=width)
        draw.line((x + gap, y + offset, x + gap + arm, y + offset), fill=color, width=width)
        draw.line((x + gap, y + offset, x + gap, y + offset + arm), fill=color, width=width)


def generate_board(output: Path, font_path: Path) -> None:
    width, height = BOARD_SIZE
    board = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))

    shadow = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((58, 58, width - 42, height - 34), 52, fill=(4, 7, 6, 145))
    shadow = shadow.filter(ImageFilter.GaussianBlur(24))
    board.alpha_composite(shadow)

    outer_box = (48, 36, width - 48, height - 52)
    outer = textured_wood(
        (outer_box[2] - outer_box[0], outer_box[3] - outer_box[1]),
        (112, 70, 38),
        (54, 29, 15),
        seed=41,
        grain_strength=36,
    )
    paste_with_rounded_mask(board, outer, outer_box, 46)

    draw = ImageDraw.Draw(board, "RGBA")
    draw.rounded_rectangle(outer_box, 46, outline=(34, 19, 11, 255), width=8)
    draw.rounded_rectangle((70, 58, width - 70, height - 74), 34, outline=(182, 128, 68, 180), width=5)
    draw.rounded_rectangle((92, 82, width - 92, height - 98), 22, outline=(38, 21, 12, 230), width=8)

    field_box = (116, 108, width - 116, height - 122)
    field = textured_wood(
        (field_box[2] - field_box[0], field_box[3] - field_box[1]),
        (194, 145, 86),
        (115, 72, 35),
        seed=73,
        grain_strength=24,
    )
    paste_with_rounded_mask(board, field, field_box, 15)
    draw.rounded_rectangle(field_box, 15, outline=(69, 39, 20, 255), width=6)
    draw.rounded_rectangle((130, 122, width - 130, height - 136), 11, outline=(222, 175, 108, 135), width=3)

    line_color = (54, 39, 24, 235)
    line_width = 4
    dx = (GRID_RIGHT - GRID_LEFT) // 8
    dy = (GRID_BOTTOM - GRID_TOP) // 9
    for row in range(10):
        y = GRID_TOP + row * dy
        draw.line((GRID_LEFT, y, GRID_RIGHT, y), fill=line_color, width=line_width)
    for col in range(9):
        x = GRID_LEFT + col * dx
        draw.line((x, GRID_TOP, x, GRID_TOP + 4 * dy), fill=line_color, width=line_width)
        draw.line((x, GRID_TOP + 5 * dy, x, GRID_BOTTOM), fill=line_color, width=line_width)

    draw.line((GRID_LEFT + 3 * dx, GRID_TOP, GRID_LEFT + 5 * dx, GRID_TOP + 2 * dy), fill=line_color, width=line_width)
    draw.line((GRID_LEFT + 5 * dx, GRID_TOP, GRID_LEFT + 3 * dx, GRID_TOP + 2 * dy), fill=line_color, width=line_width)
    draw.line((GRID_LEFT + 3 * dx, GRID_TOP + 7 * dy, GRID_LEFT + 5 * dx, GRID_BOTTOM), fill=line_color, width=line_width)
    draw.line((GRID_LEFT + 5 * dx, GRID_TOP + 7 * dy, GRID_LEFT + 3 * dx, GRID_BOTTOM), fill=line_color, width=line_width)

    for row in (2, 7):
        for col in (1, 7):
            draw_registration_mark(
                draw,
                GRID_LEFT + col * dx,
                GRID_TOP + row * dy,
                left=True,
                right=True,
                color=line_color,
            )
    for row in (3, 6):
        for col in (0, 2, 4, 6, 8):
            draw_registration_mark(
                draw,
                GRID_LEFT + col * dx,
                GRID_TOP + row * dy,
                left=col != 0,
                right=col != 8,
                color=line_color,
            )

    river_font = ImageFont.truetype(str(font_path), 68)
    river_y = GRID_TOP + int(4.5 * dy)
    centered_text(draw, (GRID_LEFT + 2.1 * dx, river_y), "楚河", river_font, (54, 37, 22, 235))
    centered_text(draw, (GRID_LEFT + 5.9 * dx, river_y), "漢界", river_font, (54, 37, 22, 235))

    seal_font = ImageFont.truetype(str(font_path), 24)
    centered_text(draw, (width / 2, height - 86), "對弈", seal_font, (139, 43, 36, 155))
    board.save(output, "PNG", optimize=True)


def generate_piece(output: Path, glyph: str, side: str, font_path: Path, seed: int) -> None:
    image = Image.new("RGBA", (PIECE_SIZE, PIECE_SIZE), (0, 0, 0, 0))
    shadow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.ellipse((128, 154, 916, 942), fill=(5, 8, 7, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(24))
    image.alpha_composite(shadow)

    disc_box = (108, 90, 916, 898)
    disc_size = (disc_box[2] - disc_box[0], disc_box[3] - disc_box[1])
    wood = textured_wood(disc_size, (207, 164, 105), (122, 77, 37), seed, grain_strength=32)
    mask = Image.new("L", disc_size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, disc_size[0] - 1, disc_size[1] - 1), fill=255)
    image.paste(wood, (disc_box[0], disc_box[1]), mask)

    draw = ImageDraw.Draw(image, "RGBA")
    center = (512, 494)
    ink = (152, 43, 35, 255) if side == "red" else (35, 37, 33, 255)
    ink_dark = (84, 27, 21, 230) if side == "red" else (12, 14, 13, 230)
    draw.ellipse(disc_box, outline=(66, 37, 18, 255), width=14)
    draw.ellipse((132, 114, 892, 874), outline=(239, 202, 142, 135), width=7)
    draw.ellipse((178, 160, 846, 828), outline=(74, 43, 22, 220), width=10)
    draw.ellipse((197, 179, 827, 809), outline=ink, width=6)
    draw.arc((224, 206, 800, 782), 204, 322, fill=(248, 218, 167, 120), width=10)
    draw.arc((236, 218, 788, 770), 22, 142, fill=(63, 36, 18, 85), width=8)

    rng = random.Random(seed + 901)
    for _ in range(16):
        y = rng.randrange(270, 720)
        x = rng.randrange(240, 610)
        draw.arc((x, y, x + rng.randrange(70, 230), y + rng.randrange(3, 11)), 180, 350, fill=(85, 48, 24, 42), width=2)

    glyph_font = ImageFont.truetype(str(font_path), 390)
    centered_text(
        draw,
        center,
        glyph,
        glyph_font,
        ink,
        stroke_fill=ink_dark,
        stroke_width=3,
    )
    image.save(output, "PNG", optimize=True)


def build_contact_sheet(piece_directory: Path, output: Path) -> None:
    thumb_size = 260
    sheet = Image.new("RGBA", (thumb_size * 7, thumb_size * 2), (24, 32, 31, 255))
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

    font_path = resolve_font(args.font)
    board_directory = args.output / "board"
    piece_directory = args.output / "pieces"
    board_directory.mkdir(parents=True, exist_ok=True)
    piece_directory.mkdir(parents=True, exist_ok=True)

    generate_board(board_directory / "xiangqi-board.png", font_path)
    for index, (file_name, glyph, side) in enumerate(PIECES):
        generate_piece(piece_directory / file_name, glyph, side, font_path, 100 + index)
    if args.contact_sheet is not None:
        build_contact_sheet(piece_directory, args.contact_sheet)


if __name__ == "__main__":
    main()
