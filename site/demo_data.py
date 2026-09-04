"""Seeds a device with the data the marketing screenshots show.

`python site/demo_data.py [--serial emulator-5554] [--package com.peggy.app.dev]`

Writes straight into the app's database over adb (needs a rooted emulator), then leaves the
app stopped so the next launch reads it fresh. Everything is relative to "today", so the
screenshots never show a stale date. Re-run it any time the screenshots need retaking.
"""
import argparse
import datetime as dt
import pathlib
import subprocess
import sys
import time
import uuid

HERE = pathlib.Path(__file__).resolve().parent
DAY = 24 * 3600 * 1000

COLUMNS = (
    "uuid,title,body,type,createdAt,updatedAt,tags,color,pinned,dueDate,dueTime,completed,"
    "completedAt,priority,reminderOffsetMinutes,sortOrder,startAt,endAt,allDay,recurrence,"
    "snoozedUntil,deletedAt"
)


def esc(text):
    return text.replace("'", "''")


class Seed:
    def __init__(self):
        self.now = int(time.time() * 1000)
        self.today = dt.date.today()
        self.epoch_day = (self.today - dt.date(1970, 1, 1)).days
        self.rows = []

    def ago(self, days=0, hours=0):
        return self.now - days * DAY - hours * 3600 * 1000

    def at(self, days, hour, minute=0):
        moment = dt.datetime.combine(self.today + dt.timedelta(days=days), dt.time(hour, minute))
        return int(moment.timestamp() * 1000)

    def add(self, **f):
        f.setdefault("uuid", uuid.uuid4().hex)
        values = []
        for column in COLUMNS.split(","):
            v = f.get(column)
            if v is None:
                values.append("NULL")
            elif isinstance(v, bool):
                values.append("1" if v else "0")
            elif isinstance(v, str):
                values.append(f"'{esc(v)}'")
            else:
                values.append(str(v))
        self.rows.append("(" + ",".join(values) + ")")

    def task(self, title, *, due=None, time_of_day=None, tags="", done=False, priority="NONE",
             repeat=None, inbox=False, age_days=0, snoozed=None):
        stamp = self.ago(days=age_days)
        self.add(
            title=title, type="INBOX" if inbox else "TASK", createdAt=stamp, updatedAt=stamp,
            tags=tags, pinned=False, dueDate=due, dueTime=time_of_day, completed=done,
            completedAt=self.now if done else None, priority=priority, sortOrder=stamp,
            allDay=False, recurrence=repeat, snoozedUntil=snoozed,
        )

    def event(self, title, start, minutes=60, *, tags="", repeat=None, remind=30):
        self.add(
            title=title, type="EVENT", createdAt=self.now, updatedAt=self.now, tags=tags,
            pinned=False, completed=False, priority="NONE", reminderOffsetMinutes=remind,
            sortOrder=self.now, startAt=start, endAt=start + minutes * 60_000, allDay=False,
            recurrence=repeat,
        )

    def note(self, title, body=None, *, tags="", pinned=False, age_days=0, age_hours=0, color=None):
        stamp = self.ago(days=age_days, hours=age_hours)
        self.add(
            title=title, body=body, type="NOTE", createdAt=stamp, updatedAt=stamp, tags=tags,
            color=color, pinned=pinned, completed=False, priority="NONE", sortOrder=stamp,
            allDay=False,
        )

    def sql(self):
        return (
            "DELETE FROM items;\nDELETE FROM occurrence_completions;\n"
            f"INSERT INTO items ({COLUMNS}) VALUES\n" + ",\n".join(self.rows) + ";\n"
        )


def build():
    s = Seed()
    d = s.epoch_day

    # --- Plan: a believable day, with a habit and an evening event
    s.task("Water the plants", due=d, repeat="FREQ=DAILY")
    s.task("Get a quote for the worktop", due=d, time_of_day=10 * 60, tags="kitchen")
    s.task("Book the dentist", due=d, priority="HIGH")
    s.task("Reply to Ana about Saturday", due=d, done=True)
    s.event("Worktop fitter", s.at(0, 9), 90, tags="kitchen")
    s.event("Lunch with Sam", s.at(0, 13), 60)
    s.event("Yoga", s.at(0, 18, 30), 60, repeat="FREQ=WEEKLY;BYDAY=MO,WE,FR")
    s.task("Measure the alcove properly", due=d + 1, tags="kitchen")
    s.task("Renew the passport", due=d + 3, priority="MEDIUM")
    s.event("Dinner with Ana", s.at(2, 19, 30), 120)
    s.task("Send the invoice", due=d + 5)

    # --- Sort: one of each kind of decision
    s.task("Order the tap", inbox=True, age_days=0, tags="kitchen")
    s.task("Look into a bike rack", inbox=True, age_days=1)
    s.task("Chase the insurance renewal", due=d - 2, priority="HIGH")
    s.task("Call the landlord back", snoozed=s.at(0, 17))
    s.task("Learn to make sourdough", age_days=40)
    s.task("Fix the shed door", age_days=62)

    # --- Ideas: the three card shapes, a project tag, a checklist
    s.note(
        "Trip packing list",
        "<p>Ferry at 11 on Sunday, so everything in the bag Saturday night.</p>"
        "<ul><li>☑ Passport</li><li>☑ Chargers</li><li>☐ Rain jacket</li>"
        "<li>☐ The good headphones</li><li>☐ Adapter</li><li>☐ Sunscreen</li></ul>"
        "<p>Ask Sam about the roof box.</p>",
        tags="trip", pinned=True, age_days=2,
    )
    s.note(
        "Kitchen refresh",
        "<p>Budget 2.5k, in two passes. Pass one is anything that doesn’t need a tradesman. "
        "Measure before ordering — the alcove is not square.</p>",
        tags="kitchen", pinned=True, age_days=3,
    )
    s.note("Make it work, make it right, make it fast.", age_days=22)
    s.note(
        "Recipe: weeknight dal",
        "<p>Onion, garlic, ginger. Red lentils, cumin, coconut milk. Simmer 25 min, finish with lime.</p>",
        tags="recipes", age_days=4,
    )
    s.note(
        "Gift ideas for mum",
        "<p>A good kettle. The garden book she kept picking up. Ceramics class voucher.</p>",
        age_days=1, age_hours=2,
    )
    s.note(
        "The capture bar should stay put when I switch tabs. Half of what I lose, I lose because "
        "I had to go looking for the field.",
        tags="peggy", age_days=2, color=4294945296,
    )
    s.note("Guest wifi", "<p>casa-5G / sunflower-42</p>", age_days=86)
    s.note(
        "Books to read",
        "<ul><li>The Overstory</li><li>Piranesi</li><li>A Memory Called Empire</li>"
        "<li>Exhalation</li><li>Circe</li></ul>",
        age_days=10,
    )
    s.note("Tile shop on Bell St had the matte green ones in stock.", tags="kitchen", age_days=6)
    s.note("Alcove measurements", "<p>612 at the back, 618 at the front. Depth 590.</p>",
           tags="kitchen", age_days=0, age_hours=3)
    return s


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--serial", default="emulator-5554")
    ap.add_argument("--package", default="com.peggy.app.dev")
    ap.add_argument("--adb", default=str(pathlib.Path.home() / "AppData/Local/Android/Sdk/platform-tools/adb.exe"))
    args = ap.parse_args()

    sql_path = HERE / "demo_data.sql"
    sql_path.write_text(build().sql(), encoding="utf-8")

    adb = [args.adb, "-s", args.serial]
    db = f"/data/data/{args.package}/databases/tina.db"
    subprocess.run(adb + ["shell", "am", "force-stop", args.package], check=True)
    subprocess.run(adb + ["push", str(sql_path), "/data/local/tmp/demo.sql"], check=True,
                   stdout=subprocess.DEVNULL)
    r = subprocess.run(adb + ["shell", f"su 0 sqlite3 {db} < /data/local/tmp/demo.sql"],
                       capture_output=True, text=True)
    if r.returncode != 0 or r.stdout.strip():
        print(r.stdout, r.stderr, file=sys.stderr)
        sys.exit("seeding failed — is the device rooted and the app installed?")
    print(f"seeded {args.package} on {args.serial}")


if __name__ == "__main__":
    main()
