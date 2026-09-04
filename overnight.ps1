#Requires -Version 5.1
param([int]$Hours = 8, [int]$MaxTurns = 80)

Set-Location $PSScriptRoot

Add-Type -Namespace Win32 -Name Power -MemberDefinition @'
[DllImport("kernel32.dll", SetLastError = true)]
public static extern uint SetThreadExecutionState(uint esFlags);
'@
$ES_CONTINUOUS = [uint32]"0x80000000"
$ES_SYSTEM_REQUIRED = [uint32]"0x00000001"
[Win32.Power]::SetThreadExecutionState($ES_CONTINUOUS -bor $ES_SYSTEM_REQUIRED) | Out-Null

$prompt = @'
Read STATUS.md, PLAN.md, and LOG.md if they exist.

FIRST SESSION ONLY: if BRIEF.md, RUBRIC.md and PHASES.md do not exist,
create them before doing anything else.
- BRIEF.md: what Peggy is, who it is for, the three things a visitor
  must understand in ten seconds, the one action to take.
- RUBRIC.md: scored 1-5 each. Hero communicates the product in under
  5 seconds. Type scale has real personality. Screenshots shown in
  context. Copy specific to Peggy, no filler. Responsive at
  390/820/1440. Lighthouse over 95. Motion purposeful, never blocks
  reading. Dark mode designed, not inverted.
- PHASES.md:
  Phase 1 (gate): PLAN.md section 1.
  Phase 2 (1.5h): PLAN.md sections 2, 3, 4.
  Phase 3 (1h):   PLAN.md section 6, plus section 5's .gitignore and
                  Pillow workflow step.
  Phase 4 (4h):   RUBRIC.md loop.
  Phase 5 (0.5h): Final rubric pass, clean build, commit.

PHASE 1 FIRST TASK: the hero renders blank in screenshots. Before
touching any CSS, check whether the left column of .hero-grid carries
the .reveal class. .reveal is opacity:0 until an IntersectionObserver
adds .in, so a screenshot taken before the observer fires shows
exactly this. If that is the cause, fix it by inverting the animation
so JS applies the hiding rather than removing it, which also resolves
PLAN.md item 1.3. Nothing else starts until the hero is visible in a
fresh screenshot.

Visual review, every session, in this order:
1. Serve from site/: python -m http.server 8899 --bind 127.0.0.1.
   Check the port is free first.
2. Try the Brave extension once, this session only: call
   tabs_context_mcp. If it returns tab context, use the
   claude-in-chrome tools against http://127.0.0.1:8899/peggy/.
3. If it errors for any reason, write "chrome: unavailable" to
   STATUS.md and use Playwright for the rest of the night. Do not
   retry, reconnect, or open Brave.
4. Playwright: headless Chromium, 390x844, 820x1180, 1440x900, both
   prefers-color-scheme values, networkidle plus 500ms before capture.

Never score a rubric item by reading CSS. Score what you saw.

Phases 1-3: work the next unchecked PLAN.md item for that phase,
verify with the command it names, check it off, commit.

Phases 4-5:
1. Score every RUBRIC.md line into STATUS.md with one-line reasons.
2. Fix only the lowest-scoring item in the current phase.
3. Rebuild, recapture, rescore. If the score did not improve, revert
   with git and note in STATUS.md what failed.
4. Commit naming the item and the score change.
5. Append one line to LOG.md: what changed, and the result.

Rules:
- Read LOG.md before deciding. Never retry an approach logged failed.
- Never rewrite a section scoring 4 or higher from scratch.
- Build must pass before any commit. Never leave a regression.
- Advance the phase only when every item for it scores 4 or higher.

Open questions you must NOT guess on. Take the safe option, note it
in STATUS.md under "For Doni to decide", and move on:
- Support email in the footer: leave it out.
- The Pro paragraph in the FAQ: cut the tier claim rather than
  publish an unconfirmed promise.

Repo-specific, absolute:
- Branch site/overnight only. Never commit or push main. No gh
  workflow, gh run, or any deploy command.
- Never touch the Play Console, the Play listing, or Linear.
- Never run site/demo_data.py. It wipes emulator-5554 and the eight
  screenshots are already correct. They are inputs tonight.
- Never run site/screenshots.py unless a rubric item requires a new
  shot, and never against the phone at 10.205.0.144.
- PLAN.md sections 7 and 8 are out of scope. Leave them unchecked.
- print() in build.py must stay ASCII.

If all phases are complete and every rubric item scores 5, create a
file named DONE at the repo root.
'@

$deadline = (Get-Date).AddHours($Hours)
$log = Join-Path $PSScriptRoot "overnight.log"
$i = 0

while ((Get-Date) -lt $deadline) {
    if (Test-Path (Join-Path $PSScriptRoot "DONE")) { break }
    $i++
    "=== iteration $i @ $(Get-Date -Format s) ===" | Tee-Object -FilePath $log -Append
    try {
        claude -p $prompt --dangerously-skip-permissions --max-turns $MaxTurns 2>&1 |
            Tee-Object -FilePath $log -Append
    } catch {
        "iteration $i failed: $_" | Tee-Object -FilePath $log -Append
    }
    git add -A 2>&1 | Out-Null
    git commit -m "overnight checkpoint $i" 2>&1 | Out-Null
    Start-Sleep -Seconds 60
}

[Win32.Power]::SetThreadExecutionState($ES_CONTINUOUS) | Out-Null
"done @ $(Get-Date -Format s)" | Tee-Object -FilePath $log -Append
