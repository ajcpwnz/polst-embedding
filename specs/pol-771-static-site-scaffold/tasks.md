# POL-771 — Tasks

Sequential. No parallelism warranted; the whole ticket is <30 min mechanical.

- [ ] **T1** — Create `docs/index.html` with seven mode tiles (iframe, script,
      sdk, api, ios, android, rn). Inline CSS; system fonts; light/dark;
      `prefers-color-scheme`.
- [ ] **T2** — Create `docs/<mode>/index.html` placeholder for each of the
      seven modes. Each page: minimal header, "Coming soon — DEMO-<XXX>"
      one-liner, back-link to `../`.
- [ ] **T3** — Create `docs/assets/.gitkeep` so FOUND-2 (POL-772) has a
      committed dir to populate.
- [ ] **T4** — Create `.github/workflows/pages-smoke.yml`. Trigger on push to
      master. Steps: checkout, sleep 30, `curl -fsSI` the deploy URL.
- [ ] **T5** — Edit root `README.md` to mention the demo URL (one short
      section; full rewrite is POLISH-README).
- [ ] **T6** — Stage only intended paths (`docs/`, `.github/`, `README.md`,
      `specs/pol-771-static-site-scaffold/`); commit with message
      `POL-771: static site scaffold + Pages smoke workflow`.
