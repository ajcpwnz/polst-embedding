# Specification Quality Checklist: POL-776 — REST API demo

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-05-05
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details beyond the public REST surface being
      demonstrated (paths under `/api/rest/v1`, `X-Device-Id` header,
      vote body shape)
- [x] Focused on user value (integrator with no SDK can copy a `fetch()`
      and run it)
- [x] Audience-appropriate
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous (each variant has a
      method + URL + expected behaviour)
- [x] Success criteria are measurable (6 variants execute, snippets
      paste-runnable, Swagger link points to right env)
- [x] Success criteria are technology-agnostic where applicable
- [x] All acceptance scenarios are defined (6 variants + empty state +
      env switch + Swagger link)
- [x] Edge cases identified (mismatched ID kind → grey-out; vote 400 if
      env hasn't received POL-768 yet — explicit fallback documented;
      response not JSON → render raw text; empty polst link → empty state)
- [x] Scope is clearly bounded (Out of scope: auth flows, other demo
      modes, backend changes, POL-780 shared logic)
- [x] Dependencies and assumptions identified (POL-768 in QA;
      POL-771/772 merged; OpenAPI source-of-truth path; Swagger UI mount
      assumption; `crypto.randomUUID()` baseline)

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows (run a request, see response;
      copy snippet, paste in console; click Swagger link, see docs)
- [x] Feature meets measurable outcomes defined in Acceptance
- [x] No implementation details leak beyond the public REST contract

## Notes

- The POL-768 fallback ("if env hasn't received the fix, vote 400s with a
  one-line note") is the trickiest part of this spec. The note is
  intended to be removed once POL-768 deploys everywhere — that removal
  is NOT a separate ticket; it's expected as part of normal rolling
  cleanup once the fix lands.
- Device-ID storage key (`polst-embed-device-id`) is namespaced
  intentionally so the SDK demo's storage doesn't collide with this
  page's storage — they're independent demos.
- "Snippet uses resolved hostname, not placeholder" is explicit because
  the obvious-but-wrong implementation would template `<apiOrigin>` into
  the snippet text, and a copy-paste would fail.
