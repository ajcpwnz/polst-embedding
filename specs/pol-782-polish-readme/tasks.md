# POL-782 — Tasks

1. Replace `README.md` with the integrator-facing rewrite. Order:
   title → live demo call-out → "Pick your integration mode"
   subsections (iframe, script tag, JS SDK, REST API, iOS, Android,
   React Native) → Native SDKs delegation → How this site works →
   License → footer.
2. Per mode subsection: one-paragraph framing + `Live:` URL line +
   `Source:` repo path line + a single fenced 30-second starter snippet.
3. iOS / Android subsections delegate run instructions to
   `ios/README.md` and `android/README.md`. Do NOT duplicate the
   per-platform build commands.
4. Quality gates:
   - `wc -l README.md` returns ≤ 200.
   - `for f in $(grep -oE '\b(docs/[a-z]+/index\.html|ios/README\.md|android/README\.md|android/LICENSE)\b' README.md | sort -u); do test -e "$f" && echo OK: "$f" || echo MISSING: "$f"; done` reports OK for every line, no MISSING.
5. Write marker `touch specs/pol-782-polish-readme/.local-testing-passed`.
6. `git add -A && git commit -m "POL-782: rewrite README as integrator-facing entry point"`. Capture SHA. Emit committed progress event.
