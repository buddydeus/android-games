#!/usr/bin/env python3
"""Generate the package-owned Dou Shou Qi board and piece texture family."""

from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "package" / "assets"
BOARD_SIZE = 1400
PIECE_SIZE = 512
FONT = Path("/System/Library/Fonts/Supplemental/Songti.ttc")

ANIMALS = {
    "elephant": "象",
    "lion": "狮",
    "tiger": "虎",
    "leopard": "豹",
    "wolf": "狼",
    "dog": "狗",
    "cat": "猫",
    "rat": "鼠",
}

SIDE_COLORS = {
    "green": ((14, 90, 58), (5, 54, 36)),
    "red": ((198, 58, 32), (133, 34, 20)),
}


def rounded_mask(size: int, radius: int, inset: int = 0) -> Image.Image:
    mask = Image.new("L", (size, size))
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        (inset, inset, size - inset - 1, size - inset - 1),
        radius=radius,
        fill=255,
    )
    return mask


def vertical_gradient(
    size: tuple[int, int],
    top: tuple[int, int, int],
    bottom: tuple[int, int, int],
) -> Image.Image:
    image = Image.new("RGBA", size)
    pixels = image.load()
    for y in range(size[1]):
        amount = y / max(1, size[1] - 1)
        color = tuple(round(a + (b - a) * amount) for a, b in zip(top, bottom))
        for x in range(size[0]):
            pixels[x, y] = (*color, 255)
    return image


def add_bamboo_grain(image: Image.Image, mask: Image.Image) -> None:
    randomizer = random.Random(20260727)
    grain = Image.new("RGBA", image.size)
    draw = ImageDraw.Draw(grain)
    for _ in range(720):
        y = randomizer.randrange(55, BOARD_SIZE - 55)
        x = randomizer.randrange(45, BOARD_SIZE - 100)
        length = randomizer.randrange(18, 130)
        alpha = randomizer.randrange(5, 17)
        tone = (103, 61, 16, alpha) if randomizer.random() < 0.65 else (255, 244, 180, alpha)
        draw.line((x, y, min(x + length, BOARD_SIZE - 45), y), fill=tone, width=1)
    grain.putalpha(Image.composite(grain.getchannel("A"), Image.new("L", image.size), mask))
    image.alpha_composite(grain)


def regular_polygon(
    center_x: float,
    center_y: float,
    radius: float,
    count: int,
    angle_offset_degrees: float,
) -> list[tuple[int, int]]:
    return [
        (
            round(center_x + math.cos(math.radians(angle_offset_degrees + index * 360 / count)) * radius),
            round(center_y + math.sin(math.radians(angle_offset_degrees + index * 360 / count)) * radius),
        )
        for index in range(count)
    ]


def draw_trap_emblem(
    image: Image.Image,
    bounds: tuple[int, int, int, int],
) -> None:
    x0, y0, x1, y1 = bounds
    center_x = (x0 + x1) / 2
    center_y = (y0 + y1) / 2
    short_edge = min(x1 - x0, y1 - y0)
    rope_color = (90, 58, 18, 255)

    wash = Image.new("RGBA", image.size)
    wash_draw = ImageDraw.Draw(wash)
    wash_draw.polygon(
        regular_polygon(center_x, center_y, short_edge * 0.38, 8, 22.5),
        fill=(213, 155, 60, 62),
    )
    image.alpha_composite(wash)

    draw = ImageDraw.Draw(image)
    outer = regular_polygon(center_x, center_y, short_edge * 0.34, 8, 22.5)
    inner = regular_polygon(center_x, center_y, short_edge * 0.28, 8, 22.5)
    draw.line((*outer, outer[0]), fill=rope_color, width=4, joint="curve")
    draw.line((*inner, inner[0]), fill=rope_color, width=3, joint="curve")

    for index in range(8):
        angle = math.radians(index * 45)
        start_radius = short_edge * 0.055
        end_radius = short_edge * 0.265
        draw.line(
            (
                round(center_x + math.cos(angle) * start_radius),
                round(center_y + math.sin(angle) * start_radius),
                round(center_x + math.cos(angle) * end_radius),
                round(center_y + math.sin(angle) * end_radius),
            ),
            fill=rope_color,
            width=3,
        )

    hook_tip_radius = short_edge * 0.13
    hook_base_radius = short_edge * 0.20
    hook_half_width = short_edge * 0.045
    for index in range(4):
        angle = math.radians(index * 90)
        tangent_x = -math.sin(angle)
        tangent_y = math.cos(angle)
        tip = (
            round(center_x + math.cos(angle) * hook_tip_radius),
            round(center_y + math.sin(angle) * hook_tip_radius),
        )
        for direction in (-1, 1):
            base = (
                round(
                    center_x +
                    math.cos(angle) * hook_base_radius +
                    tangent_x * hook_half_width * direction
                ),
                round(
                    center_y +
                    math.sin(angle) * hook_base_radius +
                    tangent_y * hook_half_width * direction
                ),
            )
            draw.line((*base, *tip), fill=rope_color, width=3)

    knot_radius = round(short_edge * 0.035)
    draw.ellipse(
        (
            round(center_x) - knot_radius,
            round(center_y) - knot_radius,
            round(center_x) + knot_radius,
            round(center_y) + knot_radius,
        ),
        fill=rope_color,
    )


def generate_board() -> None:
    canvas = Image.new("RGBA", (BOARD_SIZE, BOARD_SIZE))
    mask = rounded_mask(BOARD_SIZE, 52, 18)
    wood = vertical_gradient(
        (BOARD_SIZE, BOARD_SIZE),
        (242, 201, 116),
        (218, 157, 63),
    )
    wood.putalpha(mask)
    canvas.alpha_composite(wood)
    add_bamboo_grain(canvas, mask)
    draw = ImageDraw.Draw(canvas)

    # Layered frame and inset bevel.
    draw.rounded_rectangle((20, 20, 1379, 1379), radius=50, outline=(149, 91, 20, 255), width=9)
    draw.rounded_rectangle((35, 35, 1364, 1364), radius=38, outline=(255, 220, 140, 190), width=5)
    draw.rounded_rectangle((48, 48, 1351, 1351), radius=24, outline=(89, 51, 9, 255), width=6)

    left, top, right, bottom = 64, 64, 1336, 1336
    cell_w = (right - left) / 7
    cell_h = (bottom - top) / 9

    # Rivers use a deep lacquer-blue stone/water texture.
    river = Image.new("RGBA", canvas.size)
    river_draw = ImageDraw.Draw(river)
    for row in range(3, 6):
        for column in (1, 2, 4, 5):
            x0 = round(left + column * cell_w)
            y0 = round(top + row * cell_h)
            x1 = round(left + (column + 1) * cell_w)
            y1 = round(top + (row + 1) * cell_h)
            river_draw.rectangle((x0 + 2, y0 + 2, x1 - 2, y1 - 2), fill=(7, 93, 134, 255))
    randomizer = random.Random(731)
    for _ in range(360):
        x = randomizer.randrange(left, right)
        y = randomizer.randrange(round(top + 3 * cell_h), round(top + 6 * cell_h))
        if (
            int((x - left) / cell_w) in (1, 2, 4, 5)
            and int((y - top) / cell_h) in (3, 4, 5)
        ):
            length = randomizer.randrange(12, 55)
            river_draw.arc(
                (x, y, x + length, y + randomizer.randrange(3, 11)),
                185,
                350,
                fill=(74, 151, 177, randomizer.randrange(35, 90)),
                width=2,
            )
    canvas.alpha_composite(river)
    draw = ImageDraw.Draw(canvas)

    grid_color = (90, 58, 18, 255)
    for column in range(8):
        x = round(left + column * cell_w)
        draw.line((x, top, x, bottom), fill=grid_color, width=4)
        for row in range(10):
            y = round(top + row * cell_h)
            draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=(105, 66, 17, 255))
    for row in range(10):
        y = round(top + row * cell_h)
        draw.line((left, y, right, y), fill=grid_color, width=4)

    traps = ((0, 2), (0, 4), (1, 3), (8, 2), (8, 4), (7, 3))
    for row, column in traps:
        x0 = round(left + column * cell_w)
        y0 = round(top + row * cell_h)
        x1 = round(x0 + cell_w)
        y1 = round(y0 + cell_h)
        draw_trap_emblem(canvas, (x0, y0, x1, y1))

    den_font = ImageFont.truetype(str(FONT), 48, index=0)
    for row in (0, 8):
        column = 3
        x0 = round(left + column * cell_w)
        y0 = round(top + row * cell_h)
        x1 = round(x0 + cell_w)
        y1 = round(y0 + cell_h)
        margin = 8
        draw.rectangle((x0 + margin, y0 + margin, x1 - margin, y1 - margin), outline=(121, 71, 17, 210), width=3)
        text = "兽穴"
        box = draw.textbbox((0, 0), text, font=den_font)
        draw.text(
            ((x0 + x1 - (box[2] - box[0])) / 2, (y0 + y1 - (box[3] - box[1])) / 2 - box[1]),
            text,
            font=den_font,
            fill=(91, 48, 10, 255),
        )

    output = ASSETS / "board" / "doushouqi-board.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output, optimize=True)


def generate_piece(side: str, name: str, glyph: str) -> None:
    shadow = Image.new("RGBA", (PIECE_SIZE, PIECE_SIZE))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle((63, 71, 459, 467), radius=65, fill=(45, 25, 8, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))

    tile = vertical_gradient((PIECE_SIZE, PIECE_SIZE), *SIDE_COLORS[side])
    tile.putalpha(rounded_mask(PIECE_SIZE, 62, 52))
    canvas = Image.new("RGBA", (PIECE_SIZE, PIECE_SIZE))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(tile)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((52, 52, 459, 459), radius=62, outline=(61, 34, 10, 210), width=9)
    draw.rounded_rectangle((64, 63, 447, 446), radius=52, outline=(255, 221, 146, 80), width=4)
    draw.arc((73, 71, 438, 410), 200, 335, fill=(255, 255, 218, 70), width=9)

    font = ImageFont.truetype(str(FONT), 246, index=0)
    box = draw.textbbox((0, 0), glyph, font=font, stroke_width=2)
    x = (PIECE_SIZE - (box[2] - box[0])) / 2 - box[0]
    y = (PIECE_SIZE - (box[3] - box[1])) / 2 - box[1] - 4
    draw.text(
        (x + 5, y + 8),
        glyph,
        font=font,
        fill=(47, 24, 8, 135),
        stroke_width=2,
        stroke_fill=(47, 24, 8, 90),
    )
    draw.text(
        (x, y),
        glyph,
        font=font,
        fill=(255, 243, 210, 255),
        stroke_width=1,
        stroke_fill=(255, 250, 224, 230),
    )
    output = ASSETS / "pieces" / f"{side}-{name}.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output, optimize=True)


def main() -> None:
    if not FONT.is_file():
        raise SystemExit(f"Required CJK font is unavailable: {FONT}")
    generate_board()
    for side in SIDE_COLORS:
        for name, glyph in ANIMALS.items():
            generate_piece(side, name, glyph)
    print(f"Generated board and {len(SIDE_COLORS) * len(ANIMALS)} pieces under {ASSETS}")


if __name__ == "__main__":
    main()
