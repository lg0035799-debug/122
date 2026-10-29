#!/usr/bin/env python3
"""플레이스홀더 텍스처 + 리소스팩(font json, pack.mcmeta) 생성기. 표준 라이브러리만 사용.
직접 그린 PNG로 같은 파일명을 덮어쓰면 됩니다."""
import json, math, os, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resourcepack")
TEX = os.path.join(ROOT, "assets", "murim", "textures", "font")
FONT = os.path.join(ROOT, "assets", "murim", "font")
SKILL = os.path.join(TEX, "skill")
for d in (TEX, FONT, SKILL):
    os.makedirs(d, exist_ok=True)

SLOT = 24          # 슬롯 크기(px)
ICON = 20          # 아이콘 크기(px)
# 스킬바 세로 위치(클수록 위). 규칙: 모든 bitmap 글리프는 ascent <= height 여야 함 (어기면 폰트 전체 로드 실패)
SLOT_ASCENT = 16

def blank(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

def write_png(path, img):
    h, w = len(img), len(img[0])
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in img)
    def chunk(t, data):
        return struct.pack(">I", len(data)) + t + data + struct.pack(">I", zlib.crc32(t + data) & 0xFFFFFFFF)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)

def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= y < len(img) and 0 <= x < len(img[0]):
                img[y][x] = c

def put(img, x, y, c):
    if 0 <= y < len(img) and 0 <= x < len(img[0]):
        img[y][x] = c

# ---------- 슬롯 ----------
def slot(border, fill, inner):
    n = SLOT
    img = blank(n, n)
    rect(img, 0, 0, n - 1, n - 1, border)
    rect(img, 1, 1, n - 2, n - 2, inner)
    rect(img, 2, 2, n - 3, n - 3, fill)
    return img

write_png(f"{TEX}/slot_normal.png",   slot((210, 210, 210, 255), (20, 20, 20, 160), (70, 70, 70, 200)))
write_png(f"{TEX}/slot_cooldown.png", slot((230, 70, 70, 255),   (110, 0, 0, 190),  (160, 30, 30, 220)))
write_png(f"{TEX}/slot_empty.png",    slot((120, 120, 120, 210), (0, 0, 0, 90),     (40, 40, 40, 120)))

# ---------- 숫자 ----------
DIGITS = {
    "0": [".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."],
    "1": ["..#..", ".##..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "2": [".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"],
    "3": ["####.", "....#", "....#", ".###.", "....#", "....#", "####."],
    "4": ["...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."],
    "5": ["#####", "#....", "####.", "....#", "....#", "#...#", ".###."],
    "6": [".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."],
    "7": ["#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."],
    "8": [".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."],
    "9": [".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."],
    ".": ["", "", "", "", "", "", "#"],
}
WHITE = (255, 255, 255, 255)

def digit_sheet(cell_w, cell_h, scale, top):
    sheet = blank(cell_w * 11, cell_h)
    for i, ch in enumerate("0123456789."):
        for y, row in enumerate(DIGITS[ch]):
            for x, c in enumerate(row):
                if c == "#":
                    rect(sheet, i * cell_w + x * scale, top + y * scale,
                         i * cell_w + x * scale + scale - 1, top + y * scale + scale - 1, WHITE)
    return sheet

# 쿨타임 숫자: 2배 크기 (글자 10x14, 칸 16x16). 키 번호 라벨: 1배 (글자 5x7, 칸 8x16)
write_png(f"{TEX}/digits_cd.png", digit_sheet(16, 16, 2, 1))
write_png(f"{TEX}/digits_label.png", digit_sheet(8, 16, 1, 0))

# ---------- 아이콘 (20x20) ----------
def icon(bg, fg, shape):
    n = ICON
    c = (n - 1) / 2
    img = blank(n, n)
    rect(img, 0, 0, n - 1, n - 1, (15, 15, 20, 255))
    rect(img, 1, 1, n - 2, n - 2, bg)
    if shape == "slash":
        for t in range(n - 7):
            for k in range(3): put(img, 3 + t + k, n - 4 - t, fg)
    elif shape == "wave":
        for x in range(3, n - 3):
            y = round(c + 4 * math.sin(x / 2.2)); put(img, x, y, fg); put(img, x, y + 1, fg)
    elif shape == "whirl":
        for y in range(n):
            for x in range(n):
                if 4.6 <= math.hypot(x - c, y - c) <= 7.0: put(img, x, y, fg)
    elif shape == "fist":
        rect(img, 5, 6, n - 6, n - 6, fg); rect(img, 5, 10, n - 6, 10, bg); rect(img, 9, 6, 9, 10, bg)
    elif shape == "burst":
        for t in range(3, n - 3):
            for k in range(2):
                put(img, t, 9 + k, fg); put(img, 9 + k, t, fg); put(img, t, t + k, fg); put(img, t, n - 1 - t + k - 1, fg)
    else:
        for y in range(n):
            for x in range(n):
                if abs(x - c) + abs(y - c) <= 6: put(img, x, y, fg)
    return img

ICONS = [
    ("default",             0xE2FF, (60, 60, 60, 255),  (180, 180, 180, 255), "diamond"),
    ("skill_cheonma_slash", 0xE300, (50, 20, 60, 255),  (230, 200, 255, 255), "slash"),
    ("skill_cheonma_wave",  0xE301, (30, 30, 80, 255),  (160, 200, 255, 255), "wave"),
    ("skill_cheonma_whirl", 0xE302, (70, 20, 30, 255),  (255, 170, 170, 255), "whirl"),
    ("skill_fist_strike",   0xE303, (70, 50, 20, 255),  (255, 220, 150, 255), "fist"),
    ("skill_fist_burst",    0xE304, (80, 40, 10, 255),  (255, 200, 80, 255),  "burst"),
]
for name, cp, bg, fg, shape in ICONS:
    write_png(f"{SKILL}/{name}.png", icon(bg, fg, shape))

# ---------- 글리프 폭 검증 (Java 상수와 일치해야 함) ----------
def glyph_width(img, cell_w, idx):
    w = 0
    for row in img:
        for x in range(cell_w):
            if row[idx * cell_w + x][3] > 0: w = max(w, x + 1)
    return w
cd, lab = digit_sheet(16, 16, 2, 1), digit_sheet(8, 16, 1, 0)
assert all(glyph_width(cd, 16, i) == 10 for i in range(10)), "쿨타임 숫자 폭 != 10"
assert glyph_width(cd, 16, 10) == 2, "'.' 폭 != 2"
assert all(glyph_width(lab, 8, i) == 5 for i in range(10)), "라벨 숫자 폭 != 5"

# ---------- font json ----------
adv = {}
for b in (1, 2, 4, 8, 16, 32, 64, 128):
    adv[chr(0xF800 + b)] = -b
    adv[chr(0xF900 + b)] = b

hud = {"providers": [
    {"type": "space", "advances": adv},
    {"type": "bitmap", "file": "murim:font/slot_normal.png",   "ascent": SLOT_ASCENT, "height": SLOT, "chars": ["\uE000"]},
    {"type": "bitmap", "file": "murim:font/slot_cooldown.png", "ascent": SLOT_ASCENT, "height": SLOT, "chars": ["\uE001"]},
    {"type": "bitmap", "file": "murim:font/slot_empty.png",    "ascent": SLOT_ASCENT, "height": SLOT, "chars": ["\uE002"]},
    {"type": "bitmap", "file": "murim:font/digits_cd.png",    "ascent": SLOT_ASCENT - 4, "height": 16,
     "chars": ["".join(chr(0xE200 + i) for i in range(11))]},
    {"type": "bitmap", "file": "murim:font/digits_label.png", "ascent": SLOT_ASCENT - 3, "height": 16,
     "chars": ["".join(chr(0xE210 + i) for i in range(11))]},
]}
icon_json = {"providers": [
    {"type": "bitmap", "file": f"murim:font/skill/{n}.png", "ascent": SLOT_ASCENT - 2, "height": ICON, "chars": [chr(cp)]}
    for n, cp, *_ in ICONS]}

for font in (hud, icon_json):
    for pr in font["providers"]:
        if pr["type"] == "bitmap" and pr["ascent"] > pr["height"]:
            raise SystemExit(f"ascent({pr['ascent']}) > height({pr['height']}): {pr['file']}")

for name, data in (("hud", hud), ("icon", icon_json)):
    with open(f"{FONT}/{name}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=True)
with open(f"{ROOT}/pack.mcmeta", "w", encoding="utf-8") as f:
    json.dump({"pack": {"pack_format": 46, "description": "Murim Skills HUD"}}, f, indent=2)

print("generated:", ROOT)
