# Specification Quality Checklist: POL-774 — Script-tag widget demo

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-05-05
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details that aren't already public surface (the spec
      names the published widget package, the chrome API, and `data-*` attrs
      because those ARE the integrator-facing contract being demonstrated)
- [x] Focused on user value and business needs (integrator copy-paste flow)
- [x] Written for the audience that will read it (frontend devs who know the
      embedding stack — appropriate for a satellite repo demo page)
- [x] All mandatory sections completed (Goal, Scope, Behaviour, Acceptance,
      Out of scope, Dependencies, Assumptions)

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous (each Acceptance bullet is
      verifiable from the rendered page or a `node --check` pass)
- [x] Success criteria are measurable (live render, snippet copy, parse-clean)
- [x] Success criteria are technology-agnostic where the public surface
      allows (HTML/JS are inherent to a static demo site, not a tech choice)
- [x] All acceptance scenarios are defined (3 widget variants + empty state +
      env switcher + snippet copy)
- [x] Edge cases identified (no polst link → empty state; env switch →
      reload + reconfigure; missing `Polst.configure` window object before
      hydrate → handled by call-order requirement)
- [x] Scope is clearly bounded (Out of scope explicitly excludes widget-package
      changes, other demo modes, and POL-780 shared copy logic)
- [x] Dependencies and assumptions identified (POL-769 shipped, POL-771/772
      merged; assumptions about idempotent `configure` and Shadow DOM CSS
      isolation are explicit)

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows (paste polst link → see render;
      switch env → see new env's render; copy snippet → paste works)
- [x] Feature meets measurable outcomes defined in Acceptance
- [x] No implementation details leak into specification beyond what is the
      integrator-facing contract

## Notes

- This spec is intentionally prose-style (Goal/Scope/Behaviour/Acceptance) to
  match the existing repo convention (see `specs/pol-773-iframe-demo-page/spec.md`).
  The trc.specify template's User Story / Functional Requirements / Success
  Criteria headings would not add value for a single-purpose static demo page
  with one user journey.
- Forward-reference to POL-780 in "Out of scope" is intentional: it tells the
  next agent NOT to build a shared copy helper here.
