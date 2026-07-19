#!/usr/bin/env python3
"""Generate deterministic porcelain-and-celadon Xiangqi PNG assets."""

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
REQUIRED_GLYPHS = "帥俥傌炮相仕兵將車馬砲象士卒楚河漢界"

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


def radial_porcelain(
    size: tuple[int, int],
    center: tuple[int, int, int],
    edge: tuple[int, int, int],
) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, center)
    pixels = image.load()
    cx = (width - 1) / 2
    cy = (height - 1) / 2
    max_distance = (cx * cx + cy * cy) ** 0.5
    for y in range(height):
        for x in range(width):
            distance = (((x - cx) ** 2 + (y - cy) ** 2) ** 0.5) / max_distance
            amount = min(1.0, distance ** 1.35)
            pixels[x, y] = tuple(
                int(center[index] + (edge[index] - center[index]) * amount)
                for index in range(3)
            )
    return image


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


def generate_board(output: Path, font_spec: FontSpec) -> None:
    width, height = BOARD_SIZE
    board = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))

    outer_box = (48, 36, width - 48, height - 52)
    outer = soft_material(
        (outer_box[2] - outer_box[0], outer_box[3] - outer_box[1]),
        (169, 199, 190),
        seed=41,
        variation=5,
    )
    paste_with_rounded_mask(board, outer, outer_box, 46)

    draw = ImageDraw.Draw(board, "RGBA")
    draw.rounded_rectangle(outer_box, 46, outline=(110, 148, 137, 255), width=7)
    draw.rounded_rectangle((67, 55, width - 67, height - 71), 35, outline=(221, 235, 231, 190), width=5)
    draw.rounded_rectangle((92, 82, width - 92, height - 98), 22, outline=(92, 128, 118, 210), width=5)

    field_box = (116, 108, width - 116, height - 122)
    field = soft_material(
        (field_box[2] - field_box[0], field_box[3] - field_box[1]),
        (245, 241, 232),
        seed=73,
        variation=3,
    )
    paste_with_rounded_mask(board, field, field_box, 15)
    draw.rounded_rectangle(field_box, 15, outline=(110, 148, 137, 235), width=5)
    draw.rounded_rectangle((130, 122, width - 130, height - 136), 11, outline=(255, 255, 255, 170), width=3)

    line_color = (38, 60, 56, 238)
    line_width = 3
    dx = (GRID_RIGHT - GRID_LEFT) // 8
    dy = (GRID_BOTTOM - GRID_TOP) // 9
    river_top = GRID_TOP + 4 * dy
    river_bottom = GRID_TOP + 5 * dy
    draw.rectangle(
        (GRID_LEFT + 2, river_top + 2, GRID_RIGHT - 2, river_bottom - 2),
        fill=(217, 233, 228, 236),
    )
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

    river_font = load_font(font_spec, 70)
    river_y = GRID_TOP + int(4.5 * dy)
    centered_text(draw, (GRID_LEFT + 2.1 * dx, river_y), "楚河", river_font, (38, 60, 56, 238))
    centered_text(draw, (GRID_LEFT + 5.9 * dx, river_y), "漢界", river_font, (38, 60, 56, 238))
    board.save(output, "PNG", optimize=True)


def generate_piece(output: Path, glyph: str, side: str, font_spec: FontSpec, seed: int) -> None:
    image = Image.new("RGBA", (PIECE_SIZE, PIECE_SIZE), (0, 0, 0, 0))
    disc_box = (124, 124, 900, 900)
    disc_size = (disc_box[2] - disc_box[0], disc_box[3] - disc_box[1])
    porcelain = radial_porcelain(disc_size, (250, 244, 230), (226, 211, 181))
    material = Image.blend(
        porcelain,
        soft_material(disc_size, (243, 231, 206), seed, variation=3),
        0.30,
    )
    mask = Image.new("L", disc_size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, disc_size[0] - 1, disc_size[1] - 1), fill=255)
    image.paste(material, (disc_box[0], disc_box[1]), mask)

    draw = ImageDraw.Draw(image, "RGBA")
    center = (512, 512)
    ink = (184, 58, 50, 255) if side == "red" else (38, 60, 66, 255)
    ink_soft = (119, 42, 37, 205) if side == "red" else (25, 44, 49, 205)
    draw.ellipse(disc_box, outline=(184, 155, 109, 255), width=12)
    draw.ellipse((141, 141, 883, 883), outline=(255, 252, 242, 210), width=7)
    draw.ellipse((174, 174, 850, 850), outline=(184, 155, 109, 190), width=8)
    draw.ellipse((196, 196, 828, 828), outline=(*ink[:3], 195), width=5)
    draw.arc((164, 164, 860, 860), 205, 325, fill=(255, 255, 255, 150), width=8)
    draw.arc((178, 178, 846, 846), 25, 145, fill=(139, 116, 82, 70), width=6)

    glyph_font = load_font(font_spec, 390)
    centered_text(
        draw,
        center,
        glyph,
        glyph_font,
        ink,
        stroke_fill=ink_soft,
        stroke_width=2,
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
    for index, (file_name, glyph, side) in enumerate(PIECES):
        generate_piece(piece_directory / file_name, glyph, side, font_spec, 100 + index)
    if args.contact_sheet is not None:
        build_contact_sheet(piece_directory, args.contact_sheet)


if __name__ == "__main__":
    main()
