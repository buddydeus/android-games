#!/usr/bin/env python3
"""Generate deterministic package-owned Junqi visual assets."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


BOARD_SIZE = (1400, 1680)
SHELF_SIZE = (1400, 360)
ICON_SIZE = 1024
GRID_LEFT = 220
GRID_TOP = 180
GRID_COLUMN_STEP = 240
GRID_ROW_STEP = 120

ROAD = (83, 103, 98, 255)
RAIL = (33, 79, 69, 255)
CAMP = (220, 234, 227, 255)
HEADQUARTERS_FILL = (201, 183, 121, 255)
BOUNDARY = (32, 49, 46, 255)
FIELD = (247, 246, 240, 255)
RIM = (185, 204, 198, 255)

CAMPS = {
    (2, 1), (2, 3), (3, 2), (4, 1), (4, 3),
    (7, 1), (7, 3), (8, 2), (9, 1), (9, 3),
}
HEADQUARTERS = {(0, 1), (0, 3), (11, 1), (11, 3)}


def point(row: int, column: int) -> tuple[int, int]:
    return GRID_LEFT + column * GRID_COLUMN_STEP, GRID_TOP + row * GRID_ROW_STEP


def draw_link(draw: ImageDraw.ImageDraw, first: tuple[int, int], second: tuple[int, int], color: tuple[int, int, int, int], width: int) -> None:
    draw.line((point(*first), point(*second)), fill=color, width=width)


def road_links() -> list[tuple[tuple[int, int], tuple[int, int]]]:
    links = []
    for row in range(12):
        links.extend(((row, column), (row, column + 1)) for column in range(4))
    for column in range(5):
        links.extend([((0, column), (1, column)), ((10, column), (11, column))])
    for row in range(1, 11):
        links.extend(((row, column), (row + 1, column)) for column in (0, 2, 4))
    for row, column in CAMPS:
        links.extend(
            ((row, column), (row + row_offset, column + column_offset))
            for row_offset in (-1, 1)
            for column_offset in (-1, 1)
        )
    return links


def rail_links() -> list[tuple[tuple[int, int], tuple[int, int]]]:
    links = []
    for row in (1, 5, 6, 10):
        links.extend(((row, column), (row, column + 1)) for column in range(4))
    for column in (0, 4):
        links.extend(((row, column), (row + 1, column)) for row in range(1, 10))
    links.append(((5, 2), (6, 2)))
    return links


def draw_rail_ties(draw: ImageDraw.ImageDraw, first: tuple[int, int], second: tuple[int, int]) -> None:
    x1, y1 = point(*first)
    x2, y2 = point(*second)
    tie = (180, 199, 190, 255)
    if y1 == y2:
        for x in range(min(x1, x2) + 28, max(x1, x2), 48):
            draw.line((x, y1 - 12, x, y1 + 12), fill=tie, width=3)
    else:
        for y in range(min(y1, y2) + 24, max(y1, y2), 48):
            draw.line((x1 - 12, y, x1 + 12, y), fill=tie, width=3)


def generate_icon(output: Path) -> None:
    icon = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    shadow = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse((144, 154, 880, 890), fill=(32, 49, 46, 72))
    icon.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(22)))
    draw = ImageDraw.Draw(icon, "RGBA")
    draw.ellipse((116, 106, 908, 898), fill=(241, 246, 243, 255), outline=(133, 165, 156, 255), width=16)
    draw.ellipse((146, 136, 878, 868), outline=(201, 183, 121, 255), width=8)
    for offset in (-84, 0, 84):
        draw.line((260, 512 + offset, 764, 512 + offset), fill=RAIL, width=18)
        draw.line((512 + offset, 260, 512 + offset, 764), fill=RAIL, width=18)
    for offset in range(-216, 217, 54):
        draw.line((512 + offset, 410, 512 + offset, 614), fill=(184, 204, 196, 255), width=5)
        draw.line((410, 512 + offset, 614, 512 + offset), fill=(184, 204, 196, 255), width=5)
    draw.rounded_rectangle((215, 352, 472, 654), radius=34, fill=(184, 58, 50, 255), outline=(117, 44, 40, 255), width=10)
    draw.rounded_rectangle((552, 370, 809, 672), radius=34, fill=(49, 91, 131, 255), outline=(34, 62, 89, 255), width=10)
    for x, y, color in ((344, 502, (255, 226, 217, 255)), (680, 520, (218, 234, 248, 255))):
        draw.regular_polygon((x, y, 59), n_sides=5, rotation=90, fill=color)
    icon.save(output, "PNG", optimize=True)


def generate_board(output: Path) -> None:
    board = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))
    shadow = Image.new("RGBA", BOARD_SIZE, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((44, 38, 1372, 1652), radius=48, fill=(32, 49, 46, 66))
    board.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))
    draw = ImageDraw.Draw(board, "RGBA")
    draw.rounded_rectangle((28, 20, 1372, 1652), radius=48, fill=RIM, outline=(83, 103, 98, 255), width=7)
    draw.rounded_rectangle((58, 50, 1342, 1622), radius=32, fill=FIELD, outline=(224, 234, 229, 255), width=6)
    draw.rounded_rectangle((82, 74, 1318, 1598), radius=20, outline=(125, 153, 145, 255), width=4)

    for first, second in road_links():
        draw_link(draw, first, second, ROAD, 13)
    for first, second in rail_links():
        draw_link(draw, first, second, RAIL, 16)
        draw_rail_ties(draw, first, second)

    draw.line((112, 840, 1288, 840), fill=BOUNDARY, width=9)
    draw.line((700, 780, 700, 900), fill=BOUNDARY, width=10)

    for row, column in CAMPS:
        x, y = point(row, column)
        draw.regular_polygon((x, y, 54), n_sides=4, rotation=45, fill=CAMP, outline=(83, 125, 115, 255), width=5)
    for row, column in HEADQUARTERS:
        x, y = point(row, column)
        draw.rounded_rectangle((x - 57, y - 42, x + 57, y + 42), radius=14, fill=HEADQUARTERS_FILL, outline=(128, 109, 61, 255), width=5)
        draw.line((x - 31, y, x + 31, y), fill=(246, 240, 212, 255), width=3)

    draw.rectangle((0, 0, 9, 9), fill=(0, 0, 0, 0))
    draw.rectangle((1390, 0, 1399, 9), fill=(0, 0, 0, 0))
    draw.rectangle((0, 1670, 9, 1679), fill=(0, 0, 0, 0))
    draw.rectangle((1390, 1670, 1399, 1679), fill=(0, 0, 0, 0))
    board.save(output, "PNG", optimize=True)


def generate_shelf(output: Path) -> None:
    shelf = Image.new("RGBA", SHELF_SIZE, (233, 239, 236, 255))
    draw = ImageDraw.Draw(shelf, "RGBA")
    for y in range(0, SHELF_SIZE[1], 18):
        tone = 239 if (y // 18) % 2 == 0 else 232
        draw.rectangle((0, y, SHELF_SIZE[0], y + 17), fill=(tone, tone + 5, tone + 3, 255))
    draw.rounded_rectangle((34, 32, 1366, 328), radius=24, outline=(255, 255, 255, 185), width=5)
    draw.rounded_rectangle((52, 50, 1348, 310), radius=18, outline=(160, 184, 175, 170), width=4)
    for x in range(110, 1300, 180):
        draw.line((x, 76, x, 284), fill=(255, 255, 255, 96), width=3)
        draw.line((x + 7, 76, x + 7, 284), fill=(122, 153, 143, 62), width=3)
    shelf.save(output, "PNG", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output-root",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "package" / "assets",
    )
    args = parser.parse_args()
    root = args.output_root
    (root / "board").mkdir(parents=True, exist_ok=True)
    (root / "textures").mkdir(parents=True, exist_ok=True)
    generate_icon(root / "icon.png")
    generate_board(root / "board" / "junqi-board.png")
    generate_shelf(root / "textures" / "junqi-shelf.png")


if __name__ == "__main__":
    main()
