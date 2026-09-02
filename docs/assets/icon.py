"""Peggy icon candidates. `python docs/assets/icon.py` writes previews to docs/assets/icon-*.png
and prints the VectorDrawable path data for each, in the 108x108 adaptive-icon viewport.

The geometry is defined once (in 108-unit icon space) and used for both the preview
raster and the vector paths, so what you approve is what ships.
"""
import math
import pathlib
from PIL import Image, ImageDraw

HERE = pathlib.Path(__file__).resolve().parent
BRAND = (0x4F, 0x5F, 0xD6)
WHITE = (255, 255, 255)
SOFT = (200, 204, 240)
SS = 8  # supersampling for the preview


def rot(points, deg, cx=54, cy=54):
    r = math.radians(deg)
    out = []
    for x, y in points:
        dx, dy = x - cx, y - cy
        out.append((cx + dx * math.cos(r) - dy * math.sin(r), cy + dx * math.sin(r) + dy * math.cos(r)))
    return out


def path(points):
    return "M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in points) + " Z"


def circle_path(cx, cy, r):
    return f"M{cx - r:.1f},{cy:.1f} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"


# --- candidate A: thumbtack, tilted, the "peg it down" literal ------------------------------
def thumbtack():
    tilt = 38
    cap = rot([(43, 22), (65, 22), (65, 32), (43, 32)], tilt)           # narrow cap
    stem = rot([(50, 32), (58, 32), (58, 50), (50, 50)], tilt)          # stem
    collar = rot([(35, 50), (73, 50), (73, 58), (35, 58)], tilt)        # wide collar
    pin = rot([(52, 58), (56, 58), (54, 90)], tilt)                     # needle, tapering to a point
    return {"white": [cap, stem, collar], "soft": [pin]}


# --- candidate B: round pin with the check inside, brand continuity ------------------------
def pin_check():
    import math
    r, cx, cy, tip = 22.0, 54.0, 44.0, 84.0
    d = tip - cy
    a = math.acos(r / d)                                     # tangent angle from the vertical
    px, py = r * math.sin(a), r * math.cos(a)                # tangent points, symmetric about cx
    left, right = (cx - px, cy + py), (cx + px, cy + py)
    body = (f"M{left[0]:.1f},{left[1]:.1f} A{r},{r} 0 1,1 {right[0]:.1f},{right[1]:.1f} "
            f"L{cx:.1f},{tip:.1f} Z")
    check = [(42, 45), (50, 53), (66, 37)]
    return {"body_path": body, "check": check, "cx": cx, "cy": cy, "r": r, "left": left, "right": right, "tip": tip}


def preview_polys(polys_white, polys_soft, name, extra=None):
    size = 108 * SS
    im = Image.new("RGB", (size, size), BRAND)
    d = ImageDraw.Draw(im)
    for poly in polys_white:
        d.polygon([(x * SS, y * SS) for x, y in poly], fill=WHITE)
    for poly in polys_soft:
        d.polygon([(x * SS, y * SS) for x, y in poly], fill=SOFT)
    if extra:
        extra(d)
    # adaptive-icon mask preview: the 72-unit circle most launchers use
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([18 * SS, 18 * SS, 90 * SS, 90 * SS], fill=255)
    out = Image.new("RGB", (size, size), (245, 245, 250))
    out.paste(im, (0, 0), mask)
    out.resize((512, 512), Image.LANCZOS).save(HERE / f"icon-{name}.png")


def main():
    a = thumbtack()
    preview_polys(a["white"], a["soft"], "thumbtack")
    print("A thumbtack:")
    for poly in a["white"]:
        print("  white:", path(poly))
    for poly in a["soft"]:
        print("  soft:", path(poly))

    b = pin_check()

    def draw_b(d):
        cx, cy, r = b["cx"] * SS, b["cy"] * SS, b["r"] * SS
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=WHITE)
        d.polygon([(b["left"][0] * SS, b["left"][1] * SS), (b["right"][0] * SS, b["right"][1] * SS), (cx, b["tip"] * SS)], fill=WHITE)
        pts = [(x * SS, y * SS) for x, y in b["check"]]
        d.line(pts, fill=BRAND, width=8 * SS, joint="curve")
        for x, y in pts:
            d.ellipse([x - 4 * SS, y - 4 * SS, x + 4 * SS, y + 4 * SS], fill=BRAND)

    preview_polys([], [], "pin-check", draw_b)
    print("B pin+check:")
    print("  body:", b["body_path"])
    print("  check stroke:", "M" + " L".join(f"{x},{y}" for x, y in b["check"]))


if __name__ == "__main__":
    main()
