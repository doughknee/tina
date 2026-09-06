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

files = {
    "Main.dc.html": plan,
    "Sort.dc.html": sort_page,
    "FindEmpty.dc.html": find_empty,
    "FindTyping.dc.html": find_typing,
    "FindAsk.dc.html": find_ask,
}
titles = ["Plan · with the Need-a-day row", "Need a day · pushed from Plan", "Find · empty", "Find · typing", "Find · asking"]
for name, html in files.items():
    (HERE / name).write_text(html, encoding="utf-8")

W, H, G = 390, 844, 90
canvas = {
    "artboards": [{"file": f, "title": t, "x": i * (W + G), "y": 0, "w": W, "h": H} for i, (f, t) in enumerate(zip(files, titles))],
    "annotations": [
        {"id": "shell", "x": 0, "y": -260, "w": 1000,
         "text": "SHELL: Plan · Find · Ideas, and the bar.\n\nPlan stays as it is. The Sort tab becomes one row at the top of Plan, “3 need a day”, that opens the Sort page (New, Snoozed, Someday; Overdue is already on Plan). Find is one field for two jobs: results as you type, and an Ask Peggy row that turns the query into a conversation. Search and Ask stop being overlays. The bar does Plan and Idea only; Ask leaves the pill. Ideas is unchanged.\n\nGone from the shell: the Sort tab, the Search sheet, the Ask sheet, Ask mode in the bar, the “N to sort” card (the row replaces it)."},
    ],
    "launch": {"view": "canvas"},
}
(HERE / "canvas.json").write_text(json.dumps(canvas, indent=2), encoding="utf-8")
print("wrote", len(files))
