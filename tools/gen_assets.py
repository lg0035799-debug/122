#!/usr/bin/env python3
"""플레이스홀더 텍스처 + 리소스팩(font json, pack.mcmeta) 생성기. 표준 라이브러리만 사용.
직접 그린 PNG로 같은 파일명을 덮어쓰면 됩니다 (크기 규칙은 README 참고)."""
import json, math, os, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "resourcepack")
TEX = os.path.join(ROOT, "assets", "murim", "textures", "font")
FONT = os.path.join(ROOT, "assets", "murim", "font")
SKILL = os.path.join(TEX, "skill")
for d in (TEX, FONT, SKILL):
    os.makedirs(d, exist_ok=True)

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

# ---------- 슬롯 (18x18) ----------
def slot(border, fill, inner):
    img = blank(18, 18)
    rect(img, 0, 0, 17, 17, border)
    rect(img, 1, 1, 16, 16, inner)
    rect(img, 2, 2, 15, 15, fill)
    return img

write_png(f"{TEX}/slot_normal.png",   slot((210, 210, 210, 255), (20, 20, 20, 160), (70, 70, 70, 200)))
write_png(f"{TEX}/slot_cooldown.png", slot((230, 70, 70, 255),   (110, 0, 0, 190),  (160, 30, 30, 220)))
write_png(f"{TEX}/slot_empty.png",    slot((120, 120, 120, 210), (0, 0, 0, 90),     (40, 40, 40, 120)))

# ---------- 숫자 시트 (11칸 x 8px: 0~9 와 '.') ----------
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
    ".": ["#", "", "", "", "", "", ""],
}
sheet = blank(88, 8)
for i, ch in enumerate("0123456789."):
    pat = DIGITS[ch] if ch != "." else ["", "", "", "", "", "", "#"]
    for y, row in enumerate(pat):
        for x, c in enumerate(row):
            if c == "#":
                put(sheet, i * 8 + x, y, (255, 255, 255, 255))
write_png(f"{TEX}/digits.png", sheet)

# ---------- 아이콘 (16x16) ----------
def icon(bg, fg, shape):
    img = blank(16, 16)
    rect(img, 0, 0, 15, 15, (15, 15, 20, 255))
    rect(img, 1, 1, 14, 14, bg)
    if shape == "slash":
        for t in range(12):
            put(img, 2 + t, 13 - t, fg); put(img, 3 + t, 13 - t, fg)
    elif shape == "wave":
        for x in range(2, 14):
            y = 8 + round(3 * math.sin(x / 1.8)); put(img, x, y, fg); put(img, x, y + 1, fg)
    elif shape == "whirl":
        for y in range(16):
            for x in range(16):
                if 3.6 <= math.hypot(x - 7.5, y - 7.5) <= 5.6: put(img, x, y, fg)
    elif shape == "fist":
        rect(img, 4, 5, 11, 11, fg); rect(img, 4, 8, 11, 8, bg)
    elif shape == "burst":
        for t in range(2, 14):
            put(img, t, 8, fg); put(img, 8, t, fg); put(img, t, t, fg); put(img, t, 15 - t, fg)
    else:
        for y in range(16):
            for x in range(16):
                if abs(x - 7.5) + abs(y - 7.5) <= 5: put(img, x, y, fg)
    return img

ICONS = [  # (이름, 코드포인트, 배경, 전경, 모양)
    ("default",             0xE2FF, (60, 60, 60, 255),  (180, 180, 180, 255), "diamond"),
    ("skill_cheonma_slash", 0xE300, (50, 20, 60, 255),  (230, 200, 255, 255), "slash"),
    ("skill_cheonma_wave",  0xE301, (30, 30, 80, 255),  (160, 200, 255, 255), "wave"),
    ("skill_cheonma_whirl", 0xE302, (70, 20, 30, 255),  (255, 170, 170, 255), "whirl"),
    ("skill_fist_strike",   0xE303, (70, 50, 20, 255),  (255, 220, 150, 255), "fist"),
    ("skill_fist_burst",    0xE304, (80, 40, 10, 255),  (255, 200, 80, 255),  "burst"),
]
for name, cp, bg, fg, shape in ICONS:
    write_png(f"{SKILL}/{name}.png", icon(bg, fg, shape))

# ---------- font json ----------
adv = {}
for b in (1, 2, 4, 8, 16, 32, 64, 128):
    adv[chr(0xF800 + b)] = -b
    adv[chr(0xF900 + b)] = b

SLOT_ASCENT = -23      # 스킬바 세로 위치: 값이 작을수록(더 음수) 아래로 내려감
hud = {"providers": [
    {"type": "space", "advances": adv},
    {"type": "bitmap", "file": "murim:font/slot_normal.png",   "ascent": SLOT_ASCENT, "height": 18, "chars": ["\uE000"]},
    {"type": "bitmap", "file": "murim:font/slot_cooldown.png", "ascent": SLOT_ASCENT, "height": 18, "chars": ["\uE001"]},
    {"type": "bitmap", "file": "murim:font/slot_empty.png",    "ascent": SLOT_ASCENT, "height": 18, "chars": ["\uE002"]},
    # 쿨타임 숫자 (슬롯 가운데)
    {"type": "bitmap", "file": "murim:font/digits.png", "ascent": SLOT_ASCENT - 5, "height": 8,
     "chars": ["".join(chr(0xE200 + i) for i in range(11))]},
    # 키 번호 라벨 (슬롯 왼쪽 위)
    {"type": "bitmap", "file": "murim:font/digits.png", "ascent": SLOT_ASCENT - 2, "height": 8,
     "chars": ["".join(chr(0xE210 + i) for i in range(11))]},
]}
with open(f"{FONT}/hud.json", "w", encoding="utf-8") as f:
    json.dump(hud, f, indent=2, ensure_ascii=True)

icon_json = {"providers": [
    {"type": "bitmap", "file": f"murim:font/skill/{n}.png", "ascent": SLOT_ASCENT - 1, "height": 16, "chars": [chr(cp)]}
    for n, cp, *_ in ICONS]}
with open(f"{FONT}/icon.json", "w", encoding="utf-8") as f:
    json.dump(icon_json, f, indent=2, ensure_ascii=True)

with open(f"{ROOT}/pack.mcmeta", "w", encoding="utf-8") as f:
    json.dump({"pack": {"pack_format": 46, "description": "Murim Skills HUD"}}, f, indent=2)

print("generated:", ROOT)
