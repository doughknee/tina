"""Composes Play Store phone screenshots (1080x1920, 9:16) from raw emulator captures.

Raw captures live in docs/assets/screenshots/raw/<name>.png (1080x2400). Run
`python docs/assets/screenshots.py`; output lands in docs/assets/screenshots/.
"""
import pathlib
from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
RAW = HERE / "screenshots" / "raw"
OUT = HERE / "screenshots"
BRAND = (0x4F, 0x5F, 0xD6)
WHITE = (255, 255, 255)
MUTED = (205, 210, 245)
W, H = 1080, 1920

# order matters: the first two decide the install
SHOTS = [
    ("capture", "Type it. Peggy files it.", "“Dentist next Tuesday 10am” is an event. No forms."),
    ("plan_day", "Your day, in order", "Morning, afternoon, evening, and what has no time yet."),
    ("sort", "Empty the inbox with a swipe", "Today, tomorrow, this week, or someday. Undo everywhere."),
    ("plan_week", "Habits are one line", "Repeats roll up, so a daily gym is one row, not five."),
    ("settings", "Yours, on your device", "No account, no cloud. Export everything any time."),
]


def font(size):
    for name in ("segoeuib.ttf", "arialbd.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def rounded(im, radius):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius=radius, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def compose(name, title, subtitle, index):
    raw = Image.open(RAW / f"{name}.png").convert("RGB")
    canvas = Image.new("RGB", (W, H), BRAND)
    d = ImageDraw.Draw(canvas)
    d.text((W / 2, 150), title, font=font(66), fill=WHITE, anchor="mm")
    d.text((W / 2, 235), subtitle, font=font(34), fill=MUTED, anchor="mm")
    phone_h = 1540
    phone_w = int(raw.width * phone_h / raw.height)
    phone = rounded(raw.resize((phone_w, phone_h), Image.LANCZOS), 48)
    x = (W - phone_w) // 2
    y = 320
    shadow = Image.new("RGBA", (phone_w + 60, phone_h + 60), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([30, 30, phone_w + 30, phone_h + 30], radius=48, fill=(0, 0, 0, 70))
    canvas.paste(shadow, (x - 30, y - 18), shadow)
    canvas.paste(phone, (x, y), phone)
    canvas.save(OUT / f"{index:02d}-{name}.png")


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    for i, (name, title, subtitle) in enumerate(SHOTS, start=1):
        compose(name, title, subtitle, i)
    print("ok")
