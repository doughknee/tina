"""The stripped default surface: what a fresh install sees, in order. Same tokens as design/ux-audit."""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent / "ux-audit"))
from build import CSS, SVG, page, nav, bar  # noqa: E402

HERE = pathlib.Path(__file__).resolve().parent

EXTRA = """
    .q { position: absolute; left: 0; right: 0; bottom: 92px; padding: 0 16px; }
    .callout { margin: 12px 16px 0 16px; padding: 14px 16px; border-radius: 16px; background: #dae2ff; color: #001a41; font-size: 14px; line-height: 20px; display: flex; gap: 12px; align-items: flex-start; }
    .callout b { display: block; font-weight: 500; margin-bottom: 2px; }
    .callout .x { margin-left: auto; color: #475d92; }
    .setrow { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; }
    .setrow .t { font-size: 16px; } .setrow .s { font-size: 13px; color: #44464f; margin-top: 2px; }
    .sw { width: 52px; height: 32px; border-radius: 16px; background: #e8e7ef; border: 2px solid #74777f; box-sizing: border-box; position: relative; flex: none; }
    .sw::after { content: ""; position: absolute; left: 6px; top: 6px; width: 16px; height: 16px; border-radius: 8px; background: #74777f; }
    .sw.on { background: #475d92; border-color: #475d92; } .sw.on::after { left: 24px; top: 4px; width: 20px; height: 20px; background: #fff; }
    .dots { display: flex; gap: 6px; justify-content: center; margin-top: 14px; }
    .dots div { width: 6px; height: 6px; border-radius: 3px; background: #c4c6d0; } .dots .on { background: #475d92; }
"""


def item(title, meta):
    return (
        '<div class="row" style="padding:14px 16px"><div style="display:flex;gap:16px;align-items:center">'
        '<div style="width:20px;height:20px;border:2px solid #44464f;border-radius:4px"></div>'
        f'<div><div class="title">{title}</div><div class="meta">{meta}</div></div></div><div class="iconbtn">{SVG["chev"]}</div></div>'
    )


def plan_top(title, sub):
    return f'<div class="top"><div><div class="h1">{title}</div><div class="sub">{sub}</div></div><div class="icons">{SVG["cal"]}{SVG["search"]}{SVG["gear"]}</div></div>'


# 1 · first run: a question and a field, nothing else
first = page(
    '<div class="q"><div style="font-size:26px;line-height:32px;color:#1a1b20;margin:0 8px 18px 8px">What do you need to do this week?</div>'
    f'<div style="height:56px;border-radius:28px;background:#e8e7ef;display:flex;align-items:center;padding:0 12px 0 20px;gap:10px"><div style="flex:1;font-size:17px;color:#74777f">Type it the way you’d say it</div>{SVG["mic"]}</div>'
    '<div class="dots"><div class="on"></div><div></div><div></div></div></div>',
    EXTRA,
)

# 2 · typing: the chips are the demo
typing = page(
    '<div class="q"><div style="font-size:26px;line-height:32px;color:#1a1b20;margin:0 8px 18px 8px">What do you need to do this week?</div>'
    '<div class="chips" style="margin:0 0 12px 4px"><div class="chip tonal">Task</div><div class="chip">Tomorrow ✕</div><div class="chip">2 PM ✕</div></div>'
    f'<div style="height:56px;border-radius:28px;background:#e8e7ef;display:flex;align-items:center;padding:0 8px 0 20px;gap:10px"><div style="flex:1;font-size:17px">call the vet tomorrow at 2</div><div style="width:40px;height:40px;border-radius:20px;background:#475d92;color:#fff;display:flex;align-items:center;justify-content:center">{SVG["send"]}</div></div></div>',
    EXTRA,
)

# 3 · landed on Plan, on the day it went to; one callout, no strip, no range control
landed_plan = page(
    plan_top("Tomorrow", "Sunday, September 6")
    + f'<div class="callout"><div><b>Tomorrow at 2 PM</b>Peggy read the day and time from your words. Tap the item to change either.</div><div class="x">{SVG["close"]}</div></div>'
    + '<div class="section">Afternoon</div><div class="card" style="gap:0;padding:0">' + item("call the vet", "2 PM") + "</div>"
    + bar() + nav("Plan", badge=0),
    EXTRA,
)

# 4 · landed on Sort: the rail is the explanation
landed_sort = page(
    f'<div class="top"><div class="h1">Sort</div><div class="icons">{SVG["gear"]}</div></div>'
    + f'<div class="callout"><div><b>No date, so it waits here</b>One tap decides. Nothing on Sort is lost, it just hasn’t been given a day.</div><div class="x">{SVG["close"]}</div></div>'
    + '<div class="section">New <span>1</span></div>'
    + '<div class="card"><div class="row"><div><div class="title">buy a new lamp</div><div class="meta">captured just now</div></div><div class="iconbtn">' + SVG["chev"] + '</div></div>'
    + '<div class="row"><div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">Someday</div></div>'
    + f'<div class="chips" style="gap:4px"><div class="iconbtn">{SVG["check"]}</div><div class="iconbtn">{SVG["trash"]}</div></div></div></div>'
    + bar() + nav("Sort", badge=1),
    EXTRA,
)

# 5 · a week in: the everyday default. A list, a bar, three tabs.
everyday = page(
    plan_top("Today", "Saturday, September 12")
    + '<div class="section">Morning</div><div class="card" style="gap:0;padding:0">' + item("gym", "7 AM · every Mon, Wed, Sat") + "</div>"
    + '<div class="section">Afternoon</div><div class="card" style="gap:0;padding:0">' + item("call the vet", "2 PM") + '<div style="height:1px;background:#e0e0ea;margin:0 16px"></div>' + item("pick up parcel", "") + "</div>"
    + '<div class="section">Anytime</div><div class="card" style="gap:0;padding:0">' + item("renew passport", "") + "</div>"
    + bar() + nav("Plan"),
    EXTRA,
)

# 6 · Settings › On screen: everything that used to be on by default
def setrow(t, s, on=False):
    return f'<div class="setrow"><div><div class="t">{t}</div><div class="s">{s}</div></div><div class="sw{" on" if on else ""}"></div></div>'

settings = page(
    f'<div class="top" style="justify-content:flex-start;gap:16px"><div style="transform:rotate(180deg)">{SVG["chev"]}</div><div class="h1" style="font-size:22px">On screen</div></div>'
    '<div class="sub" style="padding:4px 16px 8px 16px">Peggy starts with a list, a bar and three tabs. Turn on what you want to see.</div>'
    '<div class="card" style="gap:0;padding:4px 0">'
    + setrow("Calendar strip", "A week or month above the list, with Week, Month and All views")
    + setrow("Suggestions", "Recent captures and starters when the bar is empty")
    + setrow("Sort card on Plan", "“3 to sort” above today’s list. The tab badge shows it anyway")
    + setrow("Ask", "Turns the bar into a question for Peggy Pro. Needs AI on")
    + setrow("Keyboard on open", "Focus the bar every time the app opens", on=True)
    + "</div>",
    EXTRA,
)

# ---- Direction B · Peggy is a thread ---------------------------------------------------
THREAD_CSS = """
    .thread { position: absolute; left: 0; right: 0; top: 96px; bottom: 160px; padding: 0 16px; display: flex; flex-direction: column; justify-content: flex-end; gap: 10px; }
    .me { align-self: flex-end; max-width: 78%; background: #475d92; color: #fff; padding: 10px 14px; border-radius: 18px 18px 4px 18px; font-size: 16px; line-height: 22px; }
    .peg { align-self: flex-start; max-width: 84%; background: #eeedf4; color: #1a1b20; padding: 10px 14px; border-radius: 18px 18px 18px 4px; font-size: 15px; line-height: 21px; }
    .peg .chips { margin-top: 8px; flex-wrap: wrap; gap: 6px; }
    .peg .chip { height: 30px; line-height: 28px; font-size: 13px; background: #f9f9ff; }
    .day { align-self: center; font-size: 12px; color: #74777f; margin: 6px 0; }
    .pin { margin: 0 16px; padding: 12px 14px; border-radius: 16px; background: #f4f3fa; position: relative; z-index: 1; }
    .pin .h { font-size: 13px; font-weight: 500; color: #44464f; display: flex; justify-content: space-between; }
    .pin .l { font-size: 15px; margin-top: 6px; display: flex; gap: 10px; align-items: center; }
    .pin .l span { color: #44464f; font-size: 13px; }
"""

def thread_top(pinned):
    return (f'<div class="top" style="padding-bottom:4px"><div class="h1">Peggy</div><div class="icons">{SVG["search"]}{SVG["gear"]}</div></div>'
            + (f'<div class="pin"><div class="h"><span>TODAY · Saturday 5</span><span style="color:#475d92">Open ›</span></div>{pinned}</div>' if pinned else ""))

def thread_bar():
    return f'<div class="bar" style="bottom:24px"><div class="hint" style="padding-left:12px">Message Peggy</div>{SVG["mic"]}</div>'

thread_first = page(
    thread_top("")
    + '<div class="thread" style="bottom:100px">'
    '<div class="day">Today</div>'
    '<div class="peg">Hi. Tell me one thing you need to do this week, the way you would say it.</div>'
    '<div class="me">call the vet tomorrow at 2</div>'
    '<div class="peg"><b>Tomorrow at 2 PM.</b> I read the day and time from your words. I will ring you five minutes before.'
    '<div class="chips"><div class="chip">Change time</div><div class="chip">No reminder</div></div></div>'
    '</div>' + thread_bar(),
    EXTRA + THREAD_CSS,
)

thread_sort = page(
    thread_top('<div class="l">☐ call the vet <span>2 PM</span></div>')
    + '<div class="thread" style="bottom:100px">'
    '<div class="me">buy a new lamp</div>'
    '<div class="peg">No day on this one. When?'
    '<div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">Someday</div><div class="chip">Drop</div></div></div>'
    '<div class="me">idea: split the bill app</div>'
    '<div class="peg">Filed under Ideas. Long ones can go on as far as you like.'
    '<div class="chips"><div class="chip">Open</div></div></div>'
    '</div>' + thread_bar(),
    EXTRA + THREAD_CSS,
)

thread_week = page(
    thread_top('<div class="l">☐ gym <span>7 AM</span></div><div class="l">☐ call the vet <span>2 PM</span></div><div class="l">☐ pick up parcel</div>')
    + '<div class="thread" style="bottom:100px">'
    '<div class="day">Yesterday</div>'
    '<div class="me">gym every mon wed sat 7am</div>'
    '<div class="peg"><b>Every Mon, Wed, Sat at 7 AM</b>, starting Monday.</div>'
    '<div class="day">Today</div>'
    '<div class="peg">Morning. Three things today, first at 7. <b>buy a new lamp</b> has waited four days.'
    '<div class="chips"><div class="chip">Today</div><div class="chip">Someday</div><div class="chip">Drop</div></div></div>'
    '<div class="me">pick up parcel</div>'
    '<div class="peg">Added to today, no time.</div>'
    '</div>' + thread_bar(),
    EXTRA + THREAD_CSS,
)

# ---- Direction C · one feed, no tabs -----------------------------------------------------
def feed_top():
    return f'<div class="top"><div><div class="h1">Peggy</div><div class="sub">Saturday, September 5</div></div><div class="icons">{SVG["search"]}{SVG["gear"]}</div></div>'

def sort_inline(title):
    return ('<div class="card"><div class="row"><div><div class="title">' + title + '</div><div class="meta">no day yet</div></div></div>'
            '<div class="row"><div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">Someday</div></div>'
            f'<div class="chips" style="gap:4px"><div class="iconbtn">{SVG["check"]}</div><div class="iconbtn">{SVG["trash"]}</div></div></div></div>')

def feed_bar():
    return f'<div class="bar" style="bottom:24px"><div class="pill">{SVG["pen"]} Plan</div><div class="hint">What’s on your mind?</div>{SVG["mic"]}</div>'

feed_first = page(
    feed_top()
    + f'<div class="callout"><div><b>Tomorrow at 2 PM</b>Peggy read the day and time from your words.</div><div class="x">{SVG["close"]}</div></div>'
    + '<div class="section">Tomorrow</div><div class="card" style="gap:0;padding:0">' + item("call the vet", "2 PM") + "</div>"
    + '<div class="section">Needs a day <span>1</span></div>' + sort_inline("buy a new lamp")
    + feed_bar(),
    EXTRA,
)

feed_week = page(
    feed_top()
    + '<div style="position:absolute;left:0;right:0;top:100px;bottom:96px;overflow:hidden">'
    + '<div class="section" style="padding-top:4px">Today</div><div class="card" style="gap:0;padding:0">' + item("gym", "7 AM · every Mon, Wed, Sat") + '<div style="height:1px;background:#e0e0ea;margin:0 16px"></div>' + item("call the vet", "2 PM") + '<div style="height:1px;background:#e0e0ea;margin:0 16px"></div>' + item("pick up parcel", "") + "</div>"
    + '<div class="section">Tomorrow</div><div class="card" style="gap:0;padding:0">' + item("dentist", "3 PM") + "</div>"
    + '<div class="section">Needs a day <span>2</span></div>' + sort_inline("buy a new lamp") + sort_inline("renew passport")
    + '<div class="section">Ideas <span>1</span></div><div class="card" style="gap:0;padding:0">' + item("split the bill app", "edited yesterday") + "</div></div>"
    + feed_bar(),
    EXTRA,
)

files = {
    "Main.dc.html": first,
    "Typing.dc.html": typing,
    "LandedPlan.dc.html": landed_plan,
    "LandedSort.dc.html": landed_sort,
    "Everyday.dc.html": everyday,
    "Settings.dc.html": settings,
    "ThreadFirst.dc.html": thread_first,
    "ThreadSort.dc.html": thread_sort,
    "ThreadWeek.dc.html": thread_week,
    "FeedFirst.dc.html": feed_first,
    "FeedWeek.dc.html": feed_week,
}
for name, html in files.items():
    (HERE / name).write_text(html, encoding="utf-8")

W, H, G, R = 390, 844, 90, 200
rows = [
    (["Main.dc.html", "Typing.dc.html", "LandedPlan.dc.html", "LandedSort.dc.html", "Everyday.dc.html", "Settings.dc.html"],
     ["A1 · First run", "A2 · Typing", "A3a · Dated → Plan", "A3b · Undated → Sort", "A4 · A week in", "A · Settings › On screen"]),
    (["ThreadFirst.dc.html", "ThreadSort.dc.html", "ThreadWeek.dc.html"],
     ["B1 · Thread, first message", "B2 · Sort and Ideas inline", "B3 · A week in"]),
    (["FeedFirst.dc.html", "FeedWeek.dc.html"],
     ["C1 · Feed, after two captures", "C2 · A week in"]),
]
artboards = []
for r, (fs, ts) in enumerate(rows):
    for i, (f, t) in enumerate(zip(fs, ts)):
        artboards.append({"file": f, "title": t, "x": i * (W + G), "y": r * (H + R), "w": W, "h": H})
canvas = {
    "artboards": artboards,
    "annotations": [
        {"id": "rule", "x": 0, "y": -220, "w": 900,
         "text": "A · STRIPPED (same app, less on it)\nThe default surface is the bar, today’s list and three tabs. Everything else is opt-in in Settings › On screen. First run is one question and a field; what they type decides which page they see first, and that page gets one callout."},
        {"id": "thread", "x": 3 * (W + G), "y": H + R, "w": 520,
         "text": "B · THREAD (Peggy is a conversation)\nHome is a message thread. You type, Peggy answers with what it did, and Sort happens as chips inside her reply. Today is a pinned card that opens the day view. Ideas are long messages she files.\n\nWhy: a message field cannot be mistaken for a calendar, and every reply is the explanation you gave Jensen by hand.\nCost: a new shell; a thread is poor at showing a week, so Plan survives as a second screen behind the pinned card."},
        {"id": "feed", "x": 2 * (W + G), "y": 2 * (H + R), "w": 520,
         "text": "C · ONE FEED (no tabs)\nOne scrolling page: Today, Tomorrow, then Needs a day with the Sort rail inline, then Ideas. The bar at the bottom, no navigation at all.\n\nWhy: the most literal “as simple as possible”, and every component already exists.\nCost: a busy user gets a long page; Sort and Ideas lose their own room."},
    ],
    "launch": {"view": "canvas"},
}
(HERE / "canvas.json").write_text(json.dumps(canvas, indent=2), encoding="utf-8")
print("wrote", len(files))
