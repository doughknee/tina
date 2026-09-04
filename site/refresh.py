"""Retake every screenshot from the current code and rebuild the site, in one go.

    python site/refresh.py

Installs the dev build on the emulator, seeds the demo content, shoots both themes, rebuilds
the pages and runs the review probe. Stops at the first step that fails, so a broken step
never ships a wrong shot. Roughly ten minutes, none of it needing you.

Then look at the shots, and commit site/screenshots/ and site/peggy/.
"""
import argparse
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SITE = ROOT / "site"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--serial", default="emulator-5554")
    ap.add_argument("--only", nargs="*", help="shot names to retake instead of all of them")
    ap.add_argument("--theme", choices=("light", "dark", "both"), default="both")
    ap.add_argument("--skip-install", action="store_true",
                    help="the emulator already has the build you want")
    ap.add_argument("--skip-probe", action="store_true")
    args = ap.parse_args()

    gradlew = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    shoot = [sys.executable, SITE / "screenshots.py", "--serial", args.serial, "--theme", args.theme]
    if args.only:
        shoot += ["--only", *args.only]

    steps = [
        ("install the dev build", [gradlew, ":composeApp:installDev", "-q"]),
        ("seed the demo content", [sys.executable, SITE / "demo_data.py", "--serial", args.serial]),
        ("take the screenshots", shoot),
        ("build the site", [sys.executable, SITE / "build.py"]),
        ("run the review probe", [sys.executable, SITE / "review.py", "--probe"]),
    ]
    if args.skip_install:
        steps = steps[1:]
    if args.skip_probe:
        steps = [s for s in steps if not s[0].startswith("run the review")]

    # gradle picks the install target from ANDROID_SERIAL, the scripts from --serial
    env = {**os.environ, "ANDROID_SERIAL": args.serial}
    for i, (what, cmd) in enumerate(steps, 1):
        print(f"\n[{i}/{len(steps)}] {what}", flush=True)
        if subprocess.run([str(c) for c in cmd], cwd=ROOT, env=env).returncode:
            sys.exit(f"\nstopped: could not {what}")
    print("\ndone. Look at site/screenshots/ (and dark/), then commit site/screenshots/ and site/peggy/.")


if __name__ == "__main__":
    main()
