# Phases

Phase 1 (gate): PLAN.md section 1. Hero renders, twelve sections present,
                content readable with JS off. FIRST: check whether the left
                column of .hero-grid carries the .reveal class before touching
                any CSS. .reveal is opacity:0 until an IntersectionObserver
                adds .in, so a capture taken before the observer fires looks
                exactly like this bug. Fix by inverting the animation so JS
                applies the hiding. Nothing else starts until the hero is
                visible in a fresh screenshot.
Phase 2 (1.5h): PLAN.md sections 2, 3, 4.
Phase 3 (1h):   PLAN.md section 6, plus section 5 gitignore and Pillow step.
Phase 4 (4h):   RUBRIC.md loop.
Phase 5 (0.5h): Final rubric pass, clean build, commit.
