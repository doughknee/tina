# BRIEF — the Peggy marketing site

The one-page reference for every judgement call on this site. If a change cannot
be justified against this file, it is decoration.

## What Peggy is

A personal capture, calendar, tasks and notes app for a single person, on one
device. You type a line the way you would say it — `lunch with sam tomorrow at
noon #work !!` — and a parser running on the phone turns the date, the time, the
repeat, the tag and the priority into chips you can drop before you save.

Everything lives in Room on the device. No account, no server, no sync, no
analytics. Optional AI parsing is bring-your-own-key and off by default; pointed
at a local Ollama it never leaves the house.

Android 12 and up. Free. Currently in closed testing, so the call to action is
the testing opt-in, not a store listing.

## Who it is for

One person with a full head and no patience for software that asks questions.
They have tried the big task apps and quit them — not because those apps lacked
features, but because capturing a thought cost six taps, a project, a due-date
picker and a decision about which list it belonged to.

They are not shopping for a productivity system. They want the thought out of
their head before they reach the kitchen. They care that it is private, and they
will notice if the page pretends otherwise.

## The three things, in ten seconds

A visitor who reads nothing but the hero and the first strip must leave knowing:

1. **You type one line and Peggy files it.** No forms, no pickers, no required
   fields. This is the product; everything else is consequence.
2. **It is fast enough to be a reflex.** Under two seconds from thought to saved,
   from anywhere — widget, tile, share sheet, long-press.
3. **It is yours.** On the device, no account, no server, nothing phones home.

If a section does not serve one of those three, it is below the fold or it is
cut.

## The one action

**Join the closed test on Google Play.** One button, above the fold, repeated at
the close. Everything else on the page is either evidence for that button or it
is in the way.

`PRIMARY_URL` / `PRIMARY_LABEL` at the top of `site/build.py` are the two
constants that change when Peggy reaches production.

## Voice

Peggy's own copy is plain and slightly dry, and the site matches it. Concrete
over abstract: "no date picker" beats "streamlined entry". Claims trace to
something in `CHANGELOG.md` or the code, or they come out. No adjectives doing
work a screenshot could do.
