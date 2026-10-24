"""Generate Trinity's item/block textures.

Natural materials (ores, raw uranium, powders, ingot) are hue/saturation recolours of the matching
vanilla textures, read straight out of the NeoForge client-resources jar that ModDevGradle downloads
(run any gradle task once first). Machined parts are hand-drawn 16x16 pixel art defined below.

    python tools/gen_textures.py [contact_sheet.png]
"""
import colorsys
import glob
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
OUT = os.path.join(REPO, "src", "main", "resources", "assets", "trinity", "textures")
JAR = glob.glob(os.path.join(REPO, "build", "moddev", "artifacts", "*client-extra*.jar"))[0]


def vanilla(path):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{path}"))).convert("RGBA")


def recolor(img, hue, sat_mul=1.0, sat_add=0.0, val_mul=1.0, only_saturated=None):
    """Move every pixel to `hue`, rescaling saturation/value. `only_saturated` limits it to pixels
    whose saturation exceeds that threshold (used to touch only the gems in an ore texture)."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if only_saturated is not None and s <= only_saturated:
                continue
            s = min(1.0, s * sat_mul + sat_add)
            v = min(1.0, v * val_mul)
            r, g, b = colorsys.hsv_to_rgb(hue, s, v)
            px[x, y] = (round(r * 255), round(g * 255), round(b * 255), a)
    return out


def sprite(rows, palette):
    assert len(rows) <= 16, f"{len(rows)} rows"
    rows = rows + ["." * 16] * (16 - len(rows))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, f"row {y} is {len(row)} wide: {row!r}"
        for x, ch in enumerate(row):
            if ch != ".":
                c = palette[ch]
                px[x, y] = tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
    return img


K = "#23272b"  # shared outline
STEEL = {"k": K, "w": "#dfe5e8", "s": "#a3acb1", "g": "#a3acb1", "d": "#6c757a"}

SPRITES = {
    "uranium_hexafluoride_canister": ([
        "................",
        "......kkkk......",
        "......kddk......",
        ".....kkkkkk.....",
        "....kwggggdk....",
        "....kwggggdk....",
        "....kyyyyyyk....",
        "....koyoyoyk....",
        "....kyoyoyok....",
        "....kyyyyyyk....",
        "....kwggggdk....",
        "....kwggggdk....",
        "....kwggggdk....",
        "....kwggggdk....",
        ".....kkkkkk.....",
    ], {**STEEL, "y": "#e3c21f", "o": "#3b3412"}),
    "gas_centrifuge": ([
        "................",
        "......kkkk......",
        ".....kwssdk.....",
        "....kkkkkkkk....",
        "....kwbsssdk....",
        "....kwbsssdk....",
        "....kwbsssdk....",
        "....kkkkkkkk....",
        "....kwbsssdk....",
        "....kwbsssdk....",
        "....kwbsssdk....",
        "....kkkkkkkk....",
        "...kmmmmmmmmk...",
        "..kmmrmmmmgmmk..",
        "..kkkkkkkkkkkk..",
    ], {**STEEL, "b": "#c6dbe6", "m": "#4b5258", "r": "#e8382c", "g": "#5df03a"}),
    "neutron_initiator": ([
        "................",
        "................",
        "................",
        "....o..o..o.....",
        ".....kkkkk......",
        "....kyywyyk.....",
        "...okywwyyko....",
        "....kyyyyyk.....",
        "...okyyyydko....",
        "....kyyyddk.....",
        ".....kkkkk......",
        "....o..o..o.....",
    ], {"k": "#5a3d07", "y": "#f2c832", "w": "#fff6b0", "d": "#b98a10", "o": "#fbe27a"}),
    "fissile_core": ([
        "................",
        "................",
        ".....kkkkkk.....",
        "...kkwwyyyykk...",
        "..kywwyyyyyydk..",
        "..kywyyyyyyydk..",
        ".kyyyyyyyyyyddk.",
        ".kGGGGGGGGGGGGk.",
        ".kggGGggGGggGGk.",
        ".kyyyyyyyyyyddk.",
        "..kyyyyyyyyddk..",
        "..kyyyyyyyyddk..",
        "...kkyyyyddkk...",
        ".....kkkkkk.....",
    ], {"k": "#3d2b05", "y": "#e6b631", "w": "#fff1a8", "d": "#a87a12",
        "G": "#9dff4f", "g": "#4fd12a"}),
    "explosive_lens": ([
        "................",
        "................",
        "......kkkk......",
        "....kkTTTTkk....",
        "...kTTttttTTk...",
        "..kTtttccctttk..",
        "..kTttcmmmcttk..",
        ".kTttcmmmmmcttk.",
        ".kTttcmmmmmcttk.",
        "..kTttcmmmcttk..",
        "..ktttccccctdk..",
        "...kttttttttk...",
        "....kkttttkk....",
        "......kkkk......",
    ], {"k": "#3a2815", "T": "#ead2a0", "t": "#c9a46a", "d": "#8f6d3d",
        "c": "#a8402c", "m": "#6b2116"}),
    "implosion_assembly": ([
        "................",
        "................",
        ".....kkkkkk.....",
        "...kkTTkkTTkk...",
        "..kTTTkttkTTTk..",
        "..kTTkttttkTTk..",
        ".kkkkttGGttkkkk.",
        ".kTTktGggGtkTTk.",
        ".kTTktGggGtkTTk.",
        ".kkkkttGGttkkkk.",
        "..kTTkttttkTTk..",
        "..kTTTkttkTTTk..",
        "...kkTTkkTTkk...",
        ".....kkkkkk.....",
    ], {"k": "#2e2418", "T": "#d9bd86", "t": "#b0905a", "G": "#9dff4f", "g": "#e6ffb8"}),
    "detonator_circuit": ([
        "................",
        "................",
        "................",
        "..kkkkkkkkkkkk..",
        "..kppppppppppk..",
        "..kpoooooppRpk..",
        "..kpopBBBBpppk..",
        "..kpopBBBBpopk..",
        "..kpooBBBBoopk..",
        "..kpppBBBBpppk..",
        "..kpRpppoooopk..",
        "..kppppppppppk..",
        "..kkkkkkkkkkkk..",
    ], {"k": "#0f2a17", "p": "#2f8a47", "o": "#f0c64a", "B": "#1b1d20", "R": "#ff3b30"}),
    "firing_set": ([
        "................",
        "................",
        "................",
        "..kkkkkkkkkkkk..",
        "..kssssssssssk..",
        "..ksRRsssbbssk..",
        "..ksRRsssbbssk..",
        "..kssssssssssk..",
        "..kyoyoyoyoyok..",
        "..koyoyoyoyoyk..",
        "..kssssssssssk..",
        "..kssggggssssk..",
        "..kkkkkkkkkkkk..",
    ], {"k": K, "s": "#59626a", "R": "#e8382c", "b": "#16181a", "y": "#f2c21b",
        "o": "#1d1d1d", "g": "#5df03a"}),
    "casing_plate": ([
        "................",
        "................",
        "..kkkkkkkkkkkk..",
        "..kwwwwwwwwwwk..",
        "..kwrssssssrdk..",
        "..kwssssssssdk..",
        "..kwssssssssdk..",
        "..kwssssssssdk..",
        "..kwssssssssdk..",
        "..kwssssssssdk..",
        "..kwssssssssdk..",
        "..kwrssssssrdk..",
        "..kddddddddddk..",
        "..kkkkkkkkkkkk..",
    ], {**STEEL, "s": "#8e979c", "r": "#3e4549"}),
    "tail_fin": ([
        "................",
        "................",
        "..k.............",
        "..kk............",
        "..kwk...........",
        "..kwsk..........",
        "..kwssk.........",
        "..kwsssk........",
        "..kwssssk.......",
        "..kwsssssk......",
        "..kwssssssk.....",
        "..kwsssssssk....",
        "..kwssssssssk...",
        ".kkkkkkkkkkkkk..",
        ".kmmmmmmmmmmmk..",
        ".kkkkkkkkkkkkk..",
    ], {**STEEL, "m": "#4b5258"}),
    "bomb_housing": ([
        "................",
        "................",
        ".....kkkkkk.....",
        "...kkHHhhhhhkk..",
        "..kHHhhhhhhhhdk.",
        ".kHhhhhhhhhhhddk",
        ".kHhohhhhhhohddk",
        ".kHhhhhhhhhhhddk",
        ".kkkkkkkkkkkkkkk",
        ".kHhhhhhhhhhhddk",
        ".kHhohhhhhhohddk",
        ".kHhhhhhhhhhhddk",
        "..kHhhhhhhhhhdk.",
        "...kkhhhhhhhkk..",
        ".....kkkkkk.....",
    ], {"k": "#2b2814", "H": "#ddc977", "h": "#b9a44c", "d": "#867337", "o": "#4a4020"}),
}


def build():
    tex = {}
    # Ores: keep the stone, swing the emerald gems from green to a hot uranium yellow-green.
    tex["block/uranium_ore"] = recolor(vanilla("block/emerald_ore.png"), 0.22, 1.0, 0.05, 1.08, 0.2)
    tex["block/deepslate_uranium_ore"] = recolor(vanilla("block/deepslate_emerald_ore.png"),
                                                 0.22, 1.0, 0.05, 1.08, 0.2)
    tex["item/raw_uranium"] = recolor(vanilla("item/raw_iron.png"), 0.19, 1.4, 0.15, 0.85)
    tex["item/yellowcake"] = recolor(vanilla("item/glowstone_dust.png"), 0.13, 0.9, 0.05, 0.95)
    tex["item/low_enriched_uranium"] = recolor(vanilla("item/redstone.png"), 0.26, 0.55, 0.0, 1.0)
    tex["item/highly_enriched_uranium"] = recolor(vanilla("item/redstone.png"), 0.28, 0.95, 0.0, 1.3)
    tex["item/enriched_uranium_ingot"] = recolor(vanilla("item/iron_ingot.png"), 0.30, 1.0, 0.32, 0.95)
    for name, (rows, palette) in SPRITES.items():
        tex[f"item/{name}"] = sprite(rows, palette)
    return tex


def contact_sheet(tex, path, scale=8):
    cell = 16 * scale + 12
    cols = 6
    rows = (len(tex) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (48, 52, 58, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(tex.items()):
        x, y = (i % cols) * cell + 6, (i // cols) * (cell + 14) + 6
        sheet.paste(img.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y), img.resize(
            (16 * scale, 16 * scale), Image.NEAREST))
        d.text((x, y + 16 * scale + 2), name.split("/")[1][:22], fill=(230, 230, 230, 255))
    sheet.save(path)


if __name__ == "__main__":
    textures = build()
    for name, img in textures.items():
        path = os.path.join(OUT, *name.split("/")) + ".png"
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    print(f"wrote {len(textures)} textures")
    if len(sys.argv) > 1:
        contact_sheet(textures, sys.argv[1])
        print(f"contact sheet -> {sys.argv[1]}")
