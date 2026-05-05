# POL-776 — Tasks

1. Replace `docs/api/index.html` placeholder with a chrome shell that
   loads `api.css` and the new `api.js` `init()` entry point. Mirror
   the structural pattern from `docs/sdk/index.html`.
2. Create `docs/api/api.css` with `.api-*` styles for variant cards,
   request preview, snippet panel + copy button, response panel, and
   footer.
3. Create `docs/api/api.js` exporting `init()`. Implement:
   - Device-ID accessor (localStorage key `polst-embed-device-id`).
   - Variant config table (6 variants) with method, path template,
     required identifier kind, body shape (vote only).
   - `buildSnippet()` that materialises path + apiOrigin + device-id
     into a copy-pasteable `fetch()` string.
   - Card renderer: method badge, URL, request headers + body preview,
     copy snippet panel, "Run" button, response slot.
   - `runVariant()` async: build request, time it, surface
     status/headers/body. POL-768 fallback note when vote 400s with
     idempotency-key body.
   - Disabled / greyed-out variant card when `target.kind` doesn't
     match.
   - Footer with Swagger UI link for the active env.
4. Write `.local-testing-passed` marker after `node --check api.js` and
   structural review pass.
5. `git add -A && git commit -m "POL-776: …"`. Capture SHA. Emit
   committed progress event.
