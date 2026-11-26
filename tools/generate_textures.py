#!/usr/bin/env python3
"""Generates block/item textures for Simple Byte Storage.

Static sources live in tools/textures_src (front/icon art). This script
  * draws the shared casing textures (machine_casing/top/bottom, drive_side),
  * turns the source fronts into animated strips (+ .mcmeta) with a gentle "breathing" glow,
  * draws the animated cable atlas and the pattern item icon.
Run:  python3 tools/generate_textures.py   (needs Pillow)
"""
import json
import os
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "textures_src")
OUT = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "simple_bytestorage", "textures")
BLOCK = os.path.join(OUT, "block")
ITEM = os.path.join(OUT, "item")


def c(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


EDGE, SHADOW, PANEL, PANEL_LT, BEVEL, RIVET, SLOT = (c(0x14161B), c(0x23272F), c(0x2E333B), c(0x353B45),
                                                     c(0x5A6475), c(0x8693A8), c(0x0E1014))
CYAN, CYAN_DIM, ORANGE = c(0x00D8F0), c(0x0A8C9C), c(0xFF8C00)
BLACK, DK, ST, LT, HL = c(0x14171C), c(0x23282F), c(0x3A424D), c(0x5A6372), c(0x7C879A)


def new(fill=PANEL):
    return Image.new("RGBA", (16, 16), fill)


def frame(im, rivets=True):
    px = im.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = EDGE
    for i in range(1, 15):
        px[i, 1] = BEVEL
        px[1, i] = BEVEL
        px[i, 14] = SHADOW
        px[14, i] = SHADOW
    px[1, 1] = RIVET
    if rivets:
        for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            px[x, y] = RIVET
            px[x + 1 if x < 8 else x - 1, y] = SHADOW
    return im


def gradient(im, top=PANEL_LT, bottom=PANEL):
    px = im.load()
    for y in range(2, 14):
        t = (y - 2) / 11.0
        col = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3)) + (255,)
        for x in range(2, 14):
            px[x, y] = col
    return im


def casing():
    im = gradient(new())
    frame(im)
    px = im.load()
    for x in range(2, 14):
        px[x, 8] = SLOT
        px[x, 9] = BEVEL
    for x in range(5, 11):  # маленький индикатор на шве
        px[x, 8] = SLOT
    px[7, 8] = CYAN_DIM
    px[8, 8] = CYAN_DIM
    return im


def top():
    im = gradient(new(), PANEL_LT, PANEL)
    frame(im, rivets=False)
    px = im.load()
    for row in range(4):
        y = 4 + row * 2 + (row // 2)
        for x in range(3, 13):
            px[x, y] = SLOT
            px[x, y + 1] = BEVEL if x % 2 == 0 else ST
    px[3, 3] = RIVET
    px[12, 3] = RIVET
    px[3, 12] = RIVET
    px[12, 12] = RIVET
    return im


def bottom():
    im = gradient(new(c(0x20242B)), c(0x262B33), c(0x1B1E24))
    frame(im, rivets=False)
    px = im.load()
    for (x0, y0) in ((2, 2), (11, 2), (2, 11), (11, 11)):
        for dx in range(3):
            for dy in range(3):
                px[x0 + dx, y0 + dy] = SLOT if dx + dy < 4 else EDGE
        px[x0, y0] = SHADOW
    return im


def drive_side():
    im = gradient(new())
    frame(im)
    px = im.load()
    for x in (4, 6, 8, 10, 12 - 0):
        if x > 11:
            continue
        for y in range(3, 13):
            px[x, y] = SLOT
            px[x + 1, y] = BEVEL if y % 4 else ST
    for y in range(3, 13):
        px[3, y] = SHADOW
    px[12, 3] = ORANGE
    px[12, 4] = c(0xC86400)
    return im


def scale(col, f):
    return tuple(max(0, min(255, int(col[i] * f))) for i in range(3)) + (col[3],)


def is_accent(p):
    return p[3] > 0 and (max(p[:3]) - min(p[:3])) > 90 and max(p[:3]) > 120


def is_orange(p):
    return p[0] > 150 and p[0] > p[2] + 80


def animate(im, factors=(1.0, 0.82, 0.62, 0.82)):
    """Анимация: холодные акценты «дышат», оранжевые индикаторы мигают в противофазе."""
    frames = []
    for k, f in enumerate(factors):
        fr = im.copy()
        px = fr.load()
        for y in range(16):
            for x in range(16):
                p = px[x, y]
                if is_accent(p):
                    g = (2.0 - f) if is_orange(p) else f
                    px[x, y] = scale(p, min(1.15, g))
        frames.append(fr)
    return frames


def save_strip(frames, path, frametime=10, interpolate=True):
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for i, fr in enumerate(frames):
        strip.paste(fr, (0, i * 16))
    strip.save(path)
    with open(path + ".mcmeta", "w") as f:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f)


def cable_atlas():
    frames = []
    for k in range(4):
        im = Image.new("RGBA", (16, 16), ST)
        px = im.load()
        glow = [1.0, 0.85, 0.7, 0.85][k]
        cy = scale(CYAN, glow)
        # A: хаб 6x6 (0,0)
        for y in range(6):
            for x in range(6):
                r = min(x, y, 5 - x, 5 - y)
                px[x, y] = [BLACK, LT, cy][r]
        # B: провод 4x5 (6,0): светящийся импульс бежит по проводу
        for y in range(5):
            for x in range(4):
                v = DK if x in (0, 3) else LT
                if y == 0:
                    v = BLACK if x in (0, 3) else DK
                if y == 1 + k and x in (1, 2):
                    v = CYAN
                px[6 + x, y] = v
        for y in range(4):
            for x in range(4):
                px[10 + x, y] = ST
        # C: фланец 10x10 (0,6)
        for y in range(10):
            for x in range(10):
                r = min(x, y, 9 - x, 9 - y)
                v = [BLACK, cy, LT, LT, DK][r] if r < 5 else DK
                if r == 1 and x in (1, 8) and y in (1, 8):
                    v = CYAN_DIM
                px[x, 6 + y] = v
        frames.append(im)
    return frames


def pattern_item():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(1, 15):
        for x in range(2, 14):
            px[x, y] = EDGE if x in (2, 13) or y in (1, 14) else PANEL
    for x in range(3, 13):
        px[x, 2] = BEVEL
    for y in range(3, 14):
        px[3, y] = BEVEL
    # сетка 3x3 бирюзовых ячеек и стрелка к результату
    for gy in range(3):
        for gx in range(3):
            for dy in range(2):
                for dx in range(2):
                    px[4 + gx * 3 + dx, 4 + gy * 3 + dy] = CYAN if (gx + gy) % 2 == 0 else CYAN_DIM
    px[12, 8] = ORANGE
    px[12, 7] = ORANGE
    px[12, 9] = ORANGE
    px[11, 8] = c(0xC86400)
    # срез угла — «карточка»
    for (x, y) in ((13, 1), (12, 1), (13, 2)):
        px[x, y] = (0, 0, 0, 0)
    px[12, 2] = EDGE
    return im


def cable_item():
    it = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ip = it.load()
    pts = set()
    core = [(i, 15 - i) for i in range(2, 14)]
    for (x, y) in core:
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) <= 1 or dx == dy:
                    pts.add((x + dx, y + dy))

    def put(x, y, v):
        if 0 <= x < 16 and 0 <= y < 16:
            ip[x, y] = v

    for (x, y) in pts:
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and ip[x + dx, y + dy][3] == 0:
                    put(x + dx, y + dy, BLACK)
    for (x, y) in pts:
        put(x, y, LT if (x + y) % 2 == 0 else ST)
    for i, (x, y) in enumerate(core):
        put(x, y, CYAN if i % 3 == 1 else HL)
    for (cx, cy) in [(4, 11), (11, 4)]:
        for dx in range(-2, 2):
            for dy in range(-2, 2):
                put(cx + dx, cy + dy, BLACK if (dx in (-2, 1) or dy in (-2, 1)) else ST)
        put(cx - 1, cy - 1, CYAN)
        put(cx, cy, CYAN)
        put(cx - 1, cy, CYAN_DIM)
        put(cx, cy - 1, CYAN_DIM)
    return it


def main():
    casing().save(os.path.join(BLOCK, "machine_casing.png"))
    top().save(os.path.join(BLOCK, "machine_top.png"))
    bottom().save(os.path.join(BLOCK, "machine_bottom.png"))
    drive_side().save(os.path.join(BLOCK, "drive_side.png"))
    for name in sorted(os.listdir(SRC)):
        if not name.endswith(".png"):
            continue
        im = Image.open(os.path.join(SRC, name)).convert("RGBA")
        if name.startswith("controller_front_offline"):
            im.save(os.path.join(BLOCK, name))
            continue
        fast = name.startswith("controller_front_conflict")
        save_strip(animate(im, (1.0, 0.45, 1.0, 0.45) if fast else (1.0, 0.82, 0.62, 0.82)),
                   os.path.join(BLOCK, name), frametime=6 if fast else 10, interpolate=not fast)
    save_strip(cable_atlas(), os.path.join(BLOCK, "cable.png"), frametime=5, interpolate=False)
    pattern_item().save(os.path.join(ITEM, "pattern_item.png"))
    cable_item().save(os.path.join(ITEM, "cable.png"))


if __name__ == "__main__":
    main()
