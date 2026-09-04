"""Retakes every screenshot the marketing site uses, from a running device.

    python site/demo_data.py          # the data the shots show
    python site/screenshots.py        # drives the app and writes site/screenshots/*.png
    python site/build.py              # regenerates the pages

Needs a rooted emulator (or any device where the dev build is installed) on adb. The system
UI is put into demo mode first, so the status bar reads a clean 9:41 with a full battery
instead of whatever the machine happens to show.

Each entry in SHOTS is a name and the steps to reach it. Names must match the `file` fields
in screenshots.json; the build fails loudly if one is missing, so a rename can't slip through.
"""
import argparse
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE / "screenshots"


class Device:
    def __init__(self, adb, serial, package, theme="light"):
        self.adb = [adb, "-s", serial]
        self.package = package
        self.theme = theme

    def sh(self, *args, binary=False):
        r = subprocess.run(self.adb + ["shell", *args], capture_output=True)
        return r.stdout if binary else r.stdout.decode("utf-8", "replace")

    def demo_mode(self, on):
        """A clean status bar: fixed clock, full battery and signal, no notification icons."""
        def broadcast(*extras):
            self.sh("am", "broadcast", "-a", "com.android.systemui.demo", *extras)
        if not on:
            broadcast("-e", "command", "exit")
            return
        self.sh("settings", "put", "global", "sysui_demo_allowed", "1")
        broadcast("-e", "command", "enter")
        broadcast("-e", "command", "clock", "-e", "hhmm", "0941")
        broadcast("-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
        broadcast("-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4", "-e", "fully", "true")
        broadcast("-e", "command", "network", "-e", "mobile", "hide")
        broadcast("-e", "command", "notifications", "-e", "visible", "false")

    def restart(self):
        self.sh("am", "force-stop", self.package)
        self.sh("monkey", "-p", self.package, "-c", "android.intent.category.LAUNCHER", "1")
        time.sleep(6)

    def nodes(self, tries=4):
        """The accessibility dump, retried: it comes back empty while the screen is animating."""
        for _ in range(tries):
            self.sh("uiautomator", "dump", "/sdcard/ui.xml")
            raw = self.sh("cat", "/sdcard/ui.xml")
            start = raw.find("<?xml")
            if start >= 0:
                try:
                    return list(ET.fromstring(raw[start:]).iter("node"))
                except ET.ParseError:
                    pass
            time.sleep(1)
        raise SystemExit("the device stopped answering accessibility dumps")

    def find(self, text=None, desc=None, contains=None, tries=6):
        """Centre of the first node matching, retrying while the screen settles."""
        for _ in range(tries):
            for n in self.nodes():
                t, d = n.get("text", ""), n.get("content-desc", "")
                if (text is not None and t == text) or (desc is not None and d == desc) \
                        or (contains is not None and contains.lower() in t.lower()):
                    x1, y1, x2, y2 = [int(v) for v in n.get("bounds").replace("][", ",")
                                      .strip("[]").split(",")]
                    return (x1 + x2) // 2, (y1 + y2) // 2
            time.sleep(1)
        raise SystemExit(f"not found on screen: text={text!r} desc={desc!r} contains={contains!r}")

    def tap(self, x, y, settle=1.5):
        self.sh("input", "tap", str(x), str(y))
        time.sleep(settle)

    def tap_on(self, **kw):
        settle = kw.pop("settle", 1.5)
        self.tap(*self.find(**kw), settle=settle)

    def type(self, text, settle=1.0):
        self.sh("input", "text", text.replace(" ", "%s"))
        time.sleep(settle)

    def key(self, code, settle=1.0):
        self.sh("input", "keyevent", str(code))
        time.sleep(settle)

    def shot(self, name):
        # dark shots go in a subfolder so build.py can pair them with the light one by filename
        out = OUT if self.theme == "light" else OUT / self.theme
        out.mkdir(parents=True, exist_ok=True)
        data = self.sh("screencap", "-p", binary=True).replace(b"\r\n", b"\n")
        (out / f"{name}.png").write_bytes(data)
        print(f"  {self.theme}/{name}.png  ({len(data) // 1024} KB)")


# Each step runs against a freshly launched app, so one failure cannot poison the next shot.
def calendar(d, visible):
    """The calendar is a persisted toggle, so put it where this shot wants it and prove it."""
    d.find(text="Day")  # the agenda is composed; before this a dump can miss the day cells
    time.sleep(1)

    def showing():
        # only a day cell carries a full date in its description
        return any(", has items" in n.get("content-desc", "") or
                   n.get("content-desc", "").endswith(", Today")
                   for n in d.nodes())
    for _ in range(3):
        if showing() == visible:
            return
        d.tap_on(desc="Calendar view", settle=2.5)
    raise SystemExit(f"could not turn the calendar {'on' if visible else 'off'}")


def shot_capture(d):
    d.tap_on(text="Day")
    calendar(d, False)
    d.tap_on(contains="What’s on your mind")
    d.type("dentist thursday 3pm")
    time.sleep(2.5)
    d.shot("capture")


def shot_plan_day(d):
    d.tap_on(text="Day")
    calendar(d, False)
    d.shot("plan-day")


def shot_plan_week(d):
    d.tap_on(text="Week")
    calendar(d, False)
    d.shot("plan-week")


def shot_calendar(d):
    d.tap_on(text="Month")
    calendar(d, True)
    d.shot("calendar")


def shot_sort(d):
    d.tap_on(text="Sort")
    d.shot("sort")


def shot_ideas(d):
    d.tap_on(text="Ideas")
    d.shot("ideas")


def shot_editor(d):
    d.tap_on(text="Ideas")
    d.tap_on(text="Trip packing list", settle=2.5)
    # tapping into the body raises the formatting row, which is half of what this shot is for
    d.tap_on(contains="Ask Sam about", settle=2.5)
    d.shot("editor")


def shot_tag(d):
    d.tap_on(text="Ideas")
    x, y = d.find(contains="#kitchen")
    d.sh("input", "swipe", str(x), str(y), str(x), str(y), "1200")
    time.sleep(2.5)
    d.shot("tag")


SHOTS = [
    ("capture", shot_capture),
    ("plan-day", shot_plan_day),
    ("plan-week", shot_plan_week),
    ("calendar", shot_calendar),
    ("sort", shot_sort),
    ("ideas", shot_ideas),
    ("editor", shot_editor),
    ("tag", shot_tag),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--serial", default="emulator-5554")
    ap.add_argument("--package", default="com.peggy.app.dev")
    ap.add_argument("--adb", default=str(pathlib.Path.home() / "AppData/Local/Android/Sdk/platform-tools/adb.exe"))
    ap.add_argument("--only", nargs="*", help="names to retake; default is all of them")
    ap.add_argument("--theme", choices=("light", "dark", "both"), default="both",
                    help="light writes site/screenshots/, dark writes site/screenshots/dark/")
    args = ap.parse_args()

    themes = ("light", "dark") if args.theme == "both" else (args.theme,)
    for theme in themes:
        capture(args, theme)


def capture(args, theme):
    d = Device(args.adb, args.serial, args.package, theme)
    print(f"--- {theme}")
    d.demo_mode(True)
    d.sh("cmd", "uimode", "night", "yes" if theme == "dark" else "no")
    time.sleep(2)
    try:
        for name, step in SHOTS:
            if args.only and name not in args.only:
                continue
            print(f"{name} …")
            d.restart()
            step(d)
    finally:
        d.demo_mode(False)
        d.sh("cmd", "uimode", "night", "no")
    print(f"  -> {OUT if theme == 'light' else OUT / theme}")


if __name__ == "__main__":
    main()
