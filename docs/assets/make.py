"""Regenerates the Play listing graphics. Run `python docs/assets/make.py`."""
import pathlib
from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
BRAND = (0x4F, 0x5F, 0xD6)
WHITE = (255, 255, 255)
MUTED = (200, 204, 240)


def font(size, bold=True):
    for name in (["segoeuib.ttf", "arialbd.ttf"] if bold else ["segoeui.ttf", "arial.ttf"]):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def pin_glyph(draw, x, y, scale, colour=WHITE, ink=BRAND):
    """The launcher glyph: a pin with the check inside, in the 108-unit icon space at (x, y)."""
    import math
    r, cx, cy, tip = 22 * scale, x + 54 * scale, y + 44 * scale, y + 84 * scale
    a = math.acos(r / (tip - cy))
    px, py = r * math.sin(a), r * math.cos(a)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=colour)
    draw.polygon([(cx - px, cy + py), (cx + px, cy + py), (cx, tip)], fill=colour)
    pts = [(x + 42 * scale, y + 45 * scale), (x + 50 * scale, y + 53 * scale), (x + 66 * scale, y + 37 * scale)]
    w = 8 * scale
    draw.line(pts, fill=ink, width=int(w), joint="curve")
    for qx, qy in pts:
        draw.ellipse([qx - w / 2, qy - w / 2, qx + w / 2, qy + w / 2], fill=ink)


def feature_graphic():
    im = Image.new("RGB", (1024, 500), BRAND)
    d = ImageDraw.Draw(im)
    pin_glyph(d, 40, 60, 3.5)
    d.text((345, 118), "Peggy", font=font(96), fill=WHITE)
    d.text((345, 250), "Capture in two seconds.", font=font(40), fill=WHITE)
    d.text((345, 302), "Plan, sort, remember. All on your device.", font=font(30), fill=MUTED)
    im.save(HERE / "feature-graphic-1024x500.png")


def play_icon():
    im = Image.new("RGB", (512, 512), BRAND)
    d = ImageDraw.Draw(im)
    pin_glyph(d, 0, 0, 512 / 108)
    im.save(HERE / "play-icon-512.png")


if __name__ == "__main__":
    feature_graphic()
    play_icon()
    print("ok")
