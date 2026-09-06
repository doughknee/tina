"""Shell proposal: Plan (with the Sort row) · Find (search + Ask) · Ideas. Bar does Plan/Idea only."""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent / "ux-audit"))
from build import SVG, page, plan_top  # noqa: E402

HERE = pathlib.Path(__file__).resolve().parent

EXTRA = """
    .need { margin: 12px 16px 0 16px; padding: 12px 8px 12px 16px; border-radius: 14px; background: #dae2ff; color: #001a41; display: flex; align-items: center; gap: 12px; font-size: 15px; font-weight: 500; }
    .need .n { width: 28px; height: 28px; border-radius: 14px; background: #475d92; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; }
    .need .go { margin-left: auto; color: #475d92; display: flex; align-items: center; }
    .field { margin: 8px 16px 0 16px; height: 56px; border-radius: 28px; background: #e8e7ef; display: flex; align-items: center; padding: 0 16px 0 20px; gap: 12px; font-size: 17px; }
    .field .ph { color: #74777f; flex: 1; }
    .res { display: flex; align-items: center; gap: 14px; padding: 12px 16px; }
    .res .ic { color: #44464f; width: 24px; display: flex; justify-content: center; }
    .res .t { font-size: 16px; } .res .m { font-size: 13px; color: #44464f; }
    .res mark { background: #dae2ff; color: inherit; border-radius: 3px; padding: 0 1px; }
    .askrow { margin: 16px 16px 0 16px; padding: 14px 16px; border-radius: 16px; background: #f4f3fa; display: flex; align-items: center; gap: 12px; }
    .askrow .t { font-size: 15px; font-weight: 500; } .askrow .m { font-size: 13px; color: #44464f; }
    .askrow .pro { margin-left: auto; font-size: 11px; font-weight: 600; letter-spacing: .4px; color: #475d92; border: 1px solid #475d92; border-radius: 6px; padding: 2px 6px; }
    .bub-me { align-self: flex-end; max-width: 80%; background: #475d92; color: #fff; padding: 10px 14px; border-radius: 18px 18px 4px 18px; font-size: 16px; line-height: 22px; }
    .bub-ai { align-self: flex-start; max-width: 88%; background: #eeedf4; padding: 12px 14px; border-radius: 18px 18px 18px 4px; font-size: 15px; line-height: 22px; }
    .bub-ai ul { margin: 6px 0 0 0; padding-left: 18px; }
    .conv { position: absolute; left: 16px; right: 16px; top: 100px; bottom: 164px; display: flex; flex-direction: column; justify-content: flex-end; gap: 10px; }
"""

SVG["sparkle"] = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"></path></svg>'
SVG["back"] = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 6l-6 6 6 6"></path></svg>'
SVG["tag"] = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12V4h8l9 9-8 8zM7 8h.01"></path></svg>'


def nav3(selected):
    items = [("Plan", "cal"), ("Find", "search"), ("Ideas", "lines")]
    out = ['<div class="nav">']
    for name, icon in items:
        cls = ' class="sel"' if name == selected else ""
        out.append(f'<div{cls}><div class="ind">{SVG[icon]}</div><span>{name}</span></div>')
    out.append("</div>")
    return "".join(out)


def bar():
    return f'<div class="bar"><div class="pill">{SVG["pen"]} Plan</div><div class="hint">What’s on your mind?</div>{SVG["mic"]}</div>'


def item(title, meta):
    return (
        '<div class="row" style="padding:14px 16px"><div style="display:flex;gap:16px;align-items:center">'
        '<div style="width:20px;height:20px;border:2px solid #44464f;border-radius:4px"></div>'
        f'<div><div class="title">{title}</div><div class="meta">{meta}</div></div></div><div class="iconbtn">{SVG["chev"]}</div></div>'
    )


# 1 · Plan as it is, plus the one row that replaces the Sort tab
plan = page(
    plan_top().replace(SVG["search"], "")  # search moved to the Find tab
    + f'<div class="need"><div class="n">3</div>need a day<div class="go">Decide {SVG["chev"]}</div></div>'
    + '<div class="section">Afternoon</div><div class="card" style="gap:0;padding:0">' + item("call the vet", "2 PM") + "</div>"
    + '<div class="section">Anytime</div><div class="card" style="gap:0;padding:0">' + item("pick up parcel", "") + "</div>"
    + bar() + nav3("Plan"),
    EXTRA,
)

# 2 · Sort, now a page you are sent to, not a tab you live on
sort_page = page(
    f'<div class="top" style="justify-content:flex-start;gap:8px"><div class="iconbtn">{SVG["back"]}</div><div class="h1">Need a day</div></div>'
    + '<div class="section" style="padding-top:8px">New <span>2</span></div>'
    + ''.join(
        '<div class="card"><div class="row"><div><div class="title">' + t + '</div><div class="meta">captured ' + m + '</div></div><div class="iconbtn">' + SVG["chev"] + '</div></div>'
        '<div class="row"><div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">Someday</div></div>'
        f'<div class="chips" style="gap:4px"><div class="iconbtn">{SVG["check"]}</div><div class="iconbtn">{SVG["trash"]}</div></div></div></div>'
        for t, m in [("buy a new lamp", "just now"), ("renew passport", "2 h ago")]
    )
    + '<div class="section">Someday <span>1</span></div>'
    + '<div class="card"><div class="row"><div><div class="title">learn to sail</div><div class="meta">untouched 14 days</div></div><div class="iconbtn">' + SVG["chev"] + '</div></div>'
    '<div class="row"><div class="chips"><div class="chip">Today</div><div class="chip">Tomorrow</div><div class="chip">This week</div></div>'
    f'<div class="chips" style="gap:4px"><div class="iconbtn">{SVG["check"]}</div><div class="iconbtn">{SVG["trash"]}</div></div></div></div>'
    + bar(),
    EXTRA,
)

# 3 · Find, empty: one field for both jobs
find_empty = page(
    f'<div class="top"><div class="h1">Find</div><div class="icons">{SVG["gear"]}</div></div>'
    + f'<div class="field">{SVG["search"]}<div class="ph">Search, or ask Peggy</div></div>'
    + '<div class="section">Tags</div>'
    + f'<div class="chips" style="padding:8px 16px 0 16px;flex-wrap:wrap"><div class="chip">#home</div><div class="chip">#work</div><div class="chip">#kitchen</div><div class="chip">#car</div></div>'
    + '<div class="section">Recent</div>'
    + f'<div class="res"><div class="ic">{SVG["search"]}</div><div><div class="t">passport</div></div></div>'
    + f'<div class="res"><div class="ic">{SVG["sparkle"]}</div><div><div class="t">what’s left this week?</div><div class="m">asked yesterday</div></div></div>'
    + bar() + nav3("Find"),
    EXTRA,
)

# 4 · Find, typing: results as you type, Ask as the last row
find_typing = page(
    f'<div class="top"><div class="h1">Find</div><div class="icons">{SVG["gear"]}</div></div>'
    + f'<div class="field">{SVG["search"]}<div style="flex:1">vet</div>{SVG["close"]}</div>'
    + '<div class="card" style="gap:0;padding:4px 0;margin-top:16px">'
    + f'<div class="res"><div class="ic"><div style="width:18px;height:18px;border:2px solid #44464f;border-radius:4px"></div></div><div><div class="t">call the <mark>vet</mark></div><div class="m">Tomorrow · 2 PM</div></div></div>'
    + f'<div class="res"><div class="ic">{SVG["lines"]}</div><div><div class="t">Dog stuff</div><div class="m">…ask the <mark>vet</mark> about the flea treatment…</div></div></div>'
    + f'<div class="res"><div class="ic">{SVG["check"]}</div><div><div class="t"><mark>Vet</mark> bill</div><div class="m">Done · Aug 21</div></div></div>'
    + "</div>"
    + f'<div class="askrow">{SVG["sparkle"]}<div><div class="t">Ask Peggy about “vet”</div><div class="m">Uses your plans and ideas to answer</div></div><div class="pro">PRO</div></div>'
    + bar() + nav3("Find"),
    EXTRA,
)

# 5 · Find, asking: the conversation lives here, not in the capture bar
find_ask = page(
    f'<div class="top" style="justify-content:flex-start;gap:8px"><div class="iconbtn">{SVG["back"]}</div><div><div class="h1" style="font-size:22px">Ask Peggy</div><div class="sub">Peggy Pro · 388 asks left this month</div></div></div>'
    + '<div class="conv">'
    '<div class="bub-me">what does this week look like?</div>'
    '<div class="bub-ai">Light until Thursday. Tomorrow you call the vet at 2 PM. Thursday is the dentist at 3 PM. Two things still need a day: <b>buy a new lamp</b> and <b>renew passport</b>, and the passport one has been waiting two days.'
    '<ul><li>Put the passport on Wednesday morning?</li><li>Lamp can wait for the weekend.</li></ul>'
    '<div class="chips" style="margin-top:10px"><div class="chip">Do that</div><div class="chip">Show Wednesday</div></div></div>'
    '</div>'
    + f'<div class="bar" style="background:#e8e7ef"><div class="hint" style="padding-left:12px">Ask a follow-up</div>{SVG["mic"]}</div>',
    EXTRA,
)

# ---- v2 · the Peggy tab, and every overlay given a page ----------------------------------
V2_CSS = """
    .procard { margin: 16px 16px 0 16px; padding: 16px; border-radius: 20px; background: #475d92; color: #fff; }
    .procard .h { font-size: 18px; font-weight: 500; } .procard .b { font-size: 14px; line-height: 20px; opacity: .9; margin-top: 4px; }
    .procard .cta { margin-top: 12px; display: inline-block; background: #fff; color: #475d92; border-radius: 20px; padding: 10px 18px; font-weight: 500; font-size: 14px; }
    .procard .fine { font-size: 12px; opacity: .8; margin-top: 8px; }
    .ex { margin: 6px 16px 0 16px; padding: 12px 14px; border-radius: 12px; background: #f4f3fa; font-size: 15px; display: flex; gap: 12px; align-items: center; color: #1a1b20; }
    .ex .s { color: #475d92; display: flex; }
    .newrow { margin: 12px 16px 0 16px; padding: 10px 8px 10px 14px; border-radius: 12px; background: #eeedf4; display: flex; align-items: center; gap: 10px; font-size: 14px; color: #44464f; }
    .newrow b { color: #1a1b20; font-weight: 500; } .newrow .go { margin-left: auto; color: #475d92; display: flex; }
    .kv { display: flex; justify-content: space-between; padding: 14px 16px; font-size: 15px; } .kv span:last-child { color: #44464f; }
    .imp { margin: 12px 16px 0 16px; padding: 14px 16px; border-radius: 16px; background: #dae2ff; color: #001a41; }
    .imp .t { font-weight: 500; font-size: 15px; display: flex; gap: 8px; align-items: center; } .imp .m { font-size: 14px; line-height: 20px; margin-top: 6px; }
    .plan { margin: 12px 16px 0 16px; padding: 16px; border-radius: 16px; border: 1px solid #c4c6d0; display: flex; justify-content: space-between; align-items: center; }
    .plan.sel { border: 2px solid #475d92; background: #f4f3fa; }
    .plan .n { font-size: 16px; font-weight: 500; } .plan .d { font-size: 13px; color: #44464f; margin-top: 2px; } .plan .p { font-size: 18px; font-weight: 500; }
"""

def nav_peggy(selected):
    items = [("Plan", "cal"), ("Peggy", "sparkle"), ("Ideas", "lines")]
    out = ['<div class="nav">']
    for name, icon in items:
        cls = ' class="sel"' if name == selected else ""
        out.append(f'<div{cls}><div class="ind">{SVG[icon]}</div><span>{name}</span></div>')
    out.append("</div>")
    return "".join(out)

def peggy_top(sub):
    return f'<div class="top"><div><div class="h1">Peggy</div><div class="sub">{sub}</div></div><div class="icons">{SVG["gear"]}</div></div>'

# 6 · the Peggy tab for a free user: search works, asking is the pitch, no popup
peggy_free = page(
    peggy_top("Search your plans, or ask")
    + f'<div class="field">{SVG["search"]}<div class="ph">Ask or search</div></div>'
    + '<div class="section">Try asking</div>'
    + ''.join(f'<div class="ex"><div class="s">{SVG["sparkle"]}</div>{q}</div>' for q in ["What’s left this week?", "When am I free on Thursday?", "What have I been putting off?"])
    + '<div class="procard"><div class="h">Peggy Pro answers from your plans</div><div class="b">Ask in plain words, get an answer that knows your week. Plus colour themes, icons and calendar export.</div><div class="cta">Try free for 7 days</div><div class="fine">then $29.99 a year, or $3.99 a month · cancel any time</div></div>'
    + bar() + nav_peggy("Peggy"),
    EXTRA + V2_CSS,
)

# 7 · the Peggy tab for a Pro user, mid-ask: matches above, answer below, same field
peggy_pro = page(
    peggy_top("Peggy Pro · 388 asks left this month")
    + f'<div class="field">{SVG["search"]}<div style="flex:1">what’s left this week</div>{SVG["close"]}</div>'
    + '<div class="section">Matches</div><div class="card" style="gap:0;padding:4px 0">'
    + f'<div class="res"><div class="ic"><div style="width:18px;height:18px;border:2px solid #44464f;border-radius:4px"></div></div><div><div class="t">renew passport</div><div class="m">needs a day</div></div></div>'
    + "</div>"
    + '<div class="section">Answer</div>'
    + '<div class="bub-ai" style="margin:8px 16px 0 16px;max-width:none">Light until Thursday. Tomorrow you call the vet at 2 PM, Thursday is the dentist at 3 PM. Two things still need a day: <b>buy a new lamp</b> and <b>renew passport</b>.'
    '<div class="chips" style="margin-top:10px"><div class="chip">Passport → Wednesday</div><div class="chip">Follow up</div></div></div>'
    + bar() + nav_peggy("Peggy"),
    EXTRA + V2_CSS,
)

# 8 · item detail with Improve inline: the AI rewrite stops being a sheet
detail = page(
    f'<div class="top" style="justify-content:flex-start;gap:8px"><div class="iconbtn">{SVG["back"]}</div><div class="h1" style="font-size:22px">call the vet</div></div>'
    + '<div class="card" style="gap:0;padding:4px 0">'
    + '<div class="kv"><span>When</span><span>Tomorrow · 2 PM</span></div><div class="kv"><span>Reminder</span><span>5 minutes before</span></div><div class="kv"><span>Repeat</span><span>Never</span></div><div class="kv"><span>Tags</span><span>#dog</span></div>'
    + "</div>"
    + f'<div class="imp"><div class="t">{SVG["sparkle"]} Peggy suggests</div><div class="m">Make it an event, 2–2:30 PM, and add “ask about flea treatment” from your Dog stuff note.</div>'
    '<div class="chips" style="margin-top:10px"><div class="chip" style="background:#fff">Apply</div><div class="chip" style="background:#fff">Not this one</div></div></div>'
    + '<div class="section">Notes</div><div class="card"><div class="meta" style="color:#74777f">Add a note…</div></div>',
    EXTRA + V2_CSS,
)

# 9 · What's new as a row on Plan, not a popup
plan_new = page(
    plan_top().replace(SVG["search"], "")
    + f'<div class="newrow">{SVG["sparkle"]}<span><b>New in 1.9.5</b> · Sort moved onto Plan</span><div class="go">{SVG["chev"]}</div></div>'
    + f'<div class="need"><div class="n">3</div>need a day<div class="go">Decide {SVG["chev"]}</div></div>'
    + '<div class="section">Afternoon</div><div class="card" style="gap:0;padding:0">' + item("call the vet", "2 PM") + "</div>"
    + bar() + nav_peggy("Plan"),
    EXTRA + V2_CSS,
)

# 10 · the paywall as a page you are taken to, with a back arrow, never a sheet
paywall = page(
    f'<div class="top" style="justify-content:flex-start;gap:8px"><div class="iconbtn">{SVG["back"]}</div><div class="h1" style="font-size:22px">Peggy Pro</div></div>'
    + '<div class="sub" style="padding:0 16px 8px 16px;font-size:15px;line-height:22px">Ask Peggy about your week, 400 asks a month. Colour themes and app icons. Calendar export.</div>'
    + '<div class="plan sel"><div><div class="n">Yearly</div><div class="d">7 days free, then $29.99 a year</div></div><div class="p">$2.50<span style="font-size:12px;color:#44464f">/mo</span></div></div>'
    + '<div class="plan"><div><div class="n">Monthly</div><div class="d">Cancel any time</div></div><div class="p">$3.99</div></div>'
    + '<div class="plan"><div><div class="n">Lifetime</div><div class="d">Pay once</div></div><div class="p">$49.99</div></div>'
    + '<div style="padding:20px 16px 0 16px"><div class="filled" style="text-align:center;padding:14px;font-size:16px">Start free trial</div></div>'
    + '<div class="meta" style="text-align:center;padding-top:10px">Billed by Google Play · Restore purchase</div>',
    EXTRA + V2_CSS,
)

files = {
    "Main.dc.html": plan,
    "Sort.dc.html": sort_page,
    "FindEmpty.dc.html": find_empty,
    "FindTyping.dc.html": find_typing,
    "FindAsk.dc.html": find_ask,
    "PeggyFree.dc.html": peggy_free,
    "PeggyPro.dc.html": peggy_pro,
    "Detail.dc.html": detail,
    "PlanNew.dc.html": plan_new,
    "Paywall.dc.html": paywall,
}
titles = ["Plan · with the Need-a-day row", "Need a day · pushed from Plan", "Find · empty", "Find · typing", "Find · asking",
          "v2 · Peggy tab, free", "v2 · Peggy tab, Pro", "v2 · Detail with Improve inline", "v2 · What’s new as a row", "v2 · Pro as a page"]
for name, html in files.items():
    (HERE / name).write_text(html, encoding="utf-8")

W, H, G = 390, 844, 90
canvas = {
    "artboards": [{"file": f, "title": t, "x": (i % 5) * (W + G), "y": (i // 5) * (H + 220), "w": W, "h": H} for i, (f, t) in enumerate(zip(files, titles))],
    "annotations": [
        {"id": "shell", "x": 0, "y": -260, "w": 1000,
         "text": "v1 · SHELL: Plan · Find · Ideas, and the bar.\n\nPlan stays as it is. The Sort tab becomes one row at the top of Plan, “3 need a day”, that opens the Sort page (New, Snoozed, Someday; Overdue is already on Plan). Find is one field for two jobs: results as you type, and an Ask Peggy row that turns the query into a conversation. Search and Ask stop being overlays. The bar does Plan and Idea only; Ask leaves the pill. Ideas is unchanged.\n\nGone from the shell: the Sort tab, the Search sheet, the Ask sheet, Ask mode in the bar, the “N to sort” card (the row replaces it)."},
        {"id": "v2", "x": 0, "y": H + 20, "w": 1100,
         "text": "v2 · PRO ON THE NAV, NOTHING IN A POPUP\nThe middle tab is Peggy, not Find. Same field, two jobs: search is free and instant, asking is Pro. A free user sees example questions and the offer on the page itself; a Pro user gets matches and the answer under one field. Nothing opens as a sheet.\n\nWhere each overlay went:\n• Suggestions sheet → off by default; when on, a single chip row above the bar.\n• Ask sheet → the Peggy tab.\n• Search sheet → the Peggy tab (free half).\n• Improve sheet → a “Peggy suggests” block inside item detail.\n• Tag picker → tag chips inline in the editor.\n• Paywall sheet → a page with a back arrow, reached from the Peggy tab and Settings.\n• What’s new dialog → one row on Plan that opens a page.\n• Onboarding → the first-capture screen.\nThe bar keeps its chips row (it is the bar, not an overlay) and the Plan/Idea pill; the big toggle row goes."},
    ],
    "launch": {"view": "canvas"},
}
(HERE / "canvas.json").write_text(json.dumps(canvas, indent=2), encoding="utf-8")
print("wrote", len(files))
