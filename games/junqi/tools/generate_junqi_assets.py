#!/usr/bin/env python3
"""Generate deterministic package-owned Junqi visual assets."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


BOARD_SIZE = (1400, 1680)
SHELF_SIZE = (1400, 360)
ICON_SIZE = 1024
GRID_LEFT = 220
GRID_TOP = 180
GRID_COLUMN_STEP = 240
GRID_ROW_STEP = 120

ROAD = (73, 66, 51, 255)
RAIL_DARK = (45, 42, 35, 255)
RAIL_LIGHT = (241, 229, 185, 255)
CAMP = (187, 194, 166, 255)
STATION = (233, 220, 170, 255)
HEADQUARTERS_LABEL = (180, 49, 43, 255)
BOUNDARY = (104, 116, 93, 255)
FIELD = (216, 201, 143, 255)
RIM = (104, 116, 93, 255)

FONT_CANDIDATES = (
    Path("/System/Library/Fonts/STHeiti Medium.ttc"),
    Path("/System/Library/Fonts/PingFang.ttc"),
    Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"),
    Path("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"),
)

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


def draw_rail(draw: ImageDraw.ImageDraw, first: tuple[int, int], second: tuple[int, int]) -> None:
    x1, y1 = point(*first)
    x2, y2 = point(*second)
    draw.line((x1, y1, x2, y2), fill=RAIL_DARK, width=24)
    if y1 == y2:
        for x in range(min(x1, x2) + 16, max(x1, x2) - 8, 48):
            draw.rectangle((x, y1 - 7, min(x + 27, max(x1, x2)), y1 + 7), fill=RAIL_LIGHT)
    else:
        for y in range(min(y1, y2) + 12, max(y1, y2) - 8, 40):
            draw.rectangle((x1 - 7, y, x1 + 7, min(y + 23, max(y1, y2))), fill=RAIL_LIGHT)


def load_board_font(size: int) -> ImageFont.FreeTypeFont:
    required = "兵站行营大本"
    for candidate in FONT_CANDIDATES:
        if not candidate.is_file():
            continue
        try:
            font = ImageFont.truetype(str(candidate), size=size, index=0)
        except OSError:
            continue
        if all(font.getmask(character).getbbox() is not None for character in required):
            return font
    raise RuntimeError("No installed bold CJK font can render the Junqi board labels")


def draw_centered_label(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    label: str,
    font: ImageFont.FreeTypeFont,
    fill: tuple[int, int, int, int],
) -> None:
    bounds = draw.textbbox((0, 0), label, font=font)
    width = bounds[2] - bounds[0]
    height = bounds[3] - bounds[1]
    draw.text(
        (center[0] - width / 2 - bounds[0], center[1] - height / 2 - bounds[1]),
        label,
        fill=fill,
        font=font,
    )


def generate_icon(output: Path) -> None:
    icon = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    shadow = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse((144, 154, 880, 890), fill=(32, 49, 46, 72))
    icon.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(22)))
    draw = ImageDraw.Draw(icon, "RGBA")
    draw.ellipse((116, 106, 908, 898), fill=(241, 246, 243, 255), outline=(133, 165, 156, 255), width=16)
    draw.ellipse((146, 136, 878, 868), outline=(201, 183, 121, 255), width=8)
    for offset in (-84, 0, 84):
        draw.line((260, 512 + offset, 764, 512 + offset), fill=(33, 79, 69, 255), width=18)
        draw.line((512 + offset, 260, 512 + offset, 764), fill=(33, 79, 69, 255), width=18)
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
    draw.rounded_rectangle((28, 20, 1372, 1652), radius=42, fill=RIM, outline=(73, 83, 66, 255), width=7)
    draw.rounded_rectangle((58, 50, 1342, 1622), radius=24, fill=FIELD, outline=(164, 151, 99, 255), width=6)
    for y in range(72, 1600, 14):
        line_color = (220, 205, 149, 255) if (y // 14) % 2 == 0 else (212, 197, 139, 255)
        draw.line((80, y, 1320, y), fill=line_color, width=1)
    draw.rounded_rectangle((82, 74, 1318, 1598), radius=14, outline=(73, 66, 51, 255), width=4)

    draw.line((112, 840, 1288, 840), fill=BOUNDARY, width=9)

    for first, second in road_links():
        draw_link(draw, first, second, ROAD, 7)
    for first, second in rail_links():
        draw_rail(draw, first, second)

    label_font = load_board_font(30)
    for row in range(12):
        for column in range(5):
            if (row, column) in CAMPS or (row, column) in HEADQUARTERS:
                continue
            x, y = point(row, column)
            draw.rounded_rectangle(
                (x - 88, y - 36, x + 88, y + 36),
                radius=5,
                fill=STATION,
                outline=ROAD,
                width=4,
            )
            draw_centered_label(draw, (x, y), "兵站", label_font, ROAD)

    for row, column in CAMPS:
        x, y = point(row, column)
        draw.ellipse((x - 74, y - 40, x + 74, y + 40), fill=CAMP, outline=BOUNDARY, width=5)
        draw_centered_label(draw, (x, y), "行营", label_font, ROAD)
    for row, column in HEADQUARTERS:
        x, y = point(row, column)
        draw.rounded_rectangle(
            (x - 88, y - 36, x + 88, y + 36),
            radius=5,
            fill=STATION,
            outline=HEADQUARTERS_LABEL,
            width=5,
        )
        draw_centered_label(draw, (x, y), "大本营", label_font, HEADQUARTERS_LABEL)

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
