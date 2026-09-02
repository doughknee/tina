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


def check_mark(draw, x, y, scale, colour=WHITE, line=MUTED):
    """The launcher glyph: a check with a baseline under it."""
    w = int(28 * scale)
    draw.line([(x, y + 60 * scale), (x + 55 * scale, y + 115 * scale), (x + 165 * scale, y)], fill=colour, width=w, joint="curve")
    for px, py in [(x, y + 60 * scale), (x + 55 * scale, y + 115 * scale), (x + 165 * scale, y)]:
        draw.ellipse([px - w / 2, py - w / 2, px + w / 2, py + w / 2], fill=colour)
    draw.rounded_rectangle([x + 10 * scale, y + 150 * scale, x + 155 * scale, y + 172 * scale], radius=int(11 * scale), fill=line)


def feature_graphic():
    im = Image.new("RGB", (1024, 500), BRAND)
    d = ImageDraw.Draw(im)
    check_mark(d, 108, 200, 1.0)
    d.text((362, 150), "Peggy", font=font(96), fill=WHITE)
    d.text((362, 282), "Capture in two seconds.", font=font(40), fill=WHITE)
    d.text((362, 334), "Plan, sort, remember. All on your device.", font=font(30), fill=MUTED)
    im.save(HERE / "feature-graphic-1024x500.png")


def play_icon():
    im = Image.new("RGB", (512, 512), BRAND)
    d = ImageDraw.Draw(im)
    check_mark(d, 140, 170, 1.4)
    im.save(HERE / "play-icon-512.png")


if __name__ == "__main__":
    feature_graphic()
    play_icon()
    print("ok")
