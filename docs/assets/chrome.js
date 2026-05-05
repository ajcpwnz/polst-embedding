// docs/assets/chrome.js
//
// Shared chrome — pure logic for the polst-embedding demo pages.
//
// Responsibilities:
//   - Parse `?polst=<url-or-id>` and `?env=<env>` from the location.
//   - Provide `getPolstTarget()` / `getEnv()` to consumers.
//   - Provide `getApiOrigin(env)` / `getAppOrigin(env)` for backends.
//
// No DOM access lives in this file. The mounting code lives in
// `chrome-render.js`. Vanilla ESM. No runtime deps.
//
// The accepted URL shapes and the short-id contract mirror what the
// polst app actually serves — see `apps/frontend/src/utils/polstUrl.ts`
// (canonical share URL is `/{category}/{slug}`; legacy fallback is
// `/p/<shortId-or-slug>`) and `apps/backend/src/utils/shortId.ts`
// (10-char `nanoid()`, URL-safe alphabet `A-Za-z0-9_-`).

/** @typedef {'canary' | 'staging' | 'prod'} Env */
/** @typedef {{ kind: 'polst' | 'brand' | 'campaign', id: string }} PolstTarget */

/** @type {ReadonlyArray<Env>} */
export const ENVS = Object.freeze(['canary', 'staging', 'prod']);

/** @type {Record<Env, { app: string, api: string }>} */
const ORIGINS = Object.freeze({
  canary: {
    app: 'https://canary.polst.app',
    api: 'https://canary-api.polst.app',
  },
  staging: {
    app: 'https://staging.polst.app',
    api: 'https://staging-api.polst.app',
  },
  prod: {
    app: 'https://polst.app',
    api: 'https://api.polst.app',
  },
});

// Backend mints `nanoid(10)` shortIds — URL-safe alphabet includes `_-`.
const RAW_ID_RE = /^[A-Za-z0-9_-]{10}$/;

// Top-level path segments that are NOT polst share URLs. A polst's
// canonical share URL is `/<category-slug>/<polst-slug>`, but other
// 2-segment routes (auth, user pages, etc.) must not be misparsed as
// polsts. Keep this list aligned with `apps/frontend/src/app/*` route
// dirs whose first segment is reserved.
const RESERVED_TOP_SEGMENTS = new Set([
  'api',
  'auth',
  'brand',
  'campaign',
  'embed',
  'explore',
  'following',
  'live',
  'location',
  'login',
  'logout',
  'nearby',
  'p',
  'register',
  'search',
  'sso',
  'trending',
  'u',
]);

/**
 * @param {string} hostname
 * @returns {Env | null}
 */
function envFromHostname(hostname) {
  if (!hostname) return null;
  const h = hostname.toLowerCase();
  if (h === 'canary.polst.app') return 'canary';
  if (h === 'staging.polst.app') return 'staging';
  if (h === 'polst.app' || h === 'www.polst.app') return 'prod';
  return null;
}

/**
 * Split a URL pathname into non-empty segments.
 *
 * @param {string} pathname
 * @returns {string[]}
 */
function pathSegments(pathname) {
  return pathname.split('/').filter(Boolean);
}

/**
 * Parse the raw `?polst=` value into a target plus an optionally
 * inferred env. Returns null when the input is unrecognized.
 *
 * Accepted shapes:
 *   - `<10-char nanoid>`                          → polst (no env)
 *   - `https://<env-host>/p/<id>`                  → polst (legacy)
 *   - `https://<env-host>/brand/<id>`              → brand
 *   - `https://<env-host>/campaign/<id>`           → campaign
 *   - `https://<env-host>/<category>/<polst-slug>` → polst (canonical)
 *
 * @param {string | null | undefined} raw
 * @returns {{ target: PolstTarget, inferredEnv: Env | null } | null}
 */
export function parsePolstParam(raw) {
  if (raw == null) return null;
  const value = String(raw).trim();
  if (value === '') return null;

  // Bare nanoid — treat as polst short id.
  if (RAW_ID_RE.test(value)) {
    return { target: { kind: 'polst', id: value }, inferredEnv: null };
  }

  let url;
  try {
    url = new URL(value);
  } catch {
    return null;
  }

  const inferredEnv = envFromHostname(url.hostname);
  const segments = pathSegments(url.pathname);
  if (segments.length === 0) return null;

  const [head, second] = segments;

  // Single-segment path is never a share URL.
  if (segments.length === 1) return null;

  // Reserved prefixes — explicit kinds.
  if (head === 'p' && second) {
    return { target: { kind: 'polst', id: decodeURIComponent(second) }, inferredEnv };
  }
  if (head === 'brand' && second) {
    return { target: { kind: 'brand', id: decodeURIComponent(second) }, inferredEnv };
  }
  if (head === 'campaign' && second) {
    return { target: { kind: 'campaign', id: decodeURIComponent(second) }, inferredEnv };
  }

  // Canonical polst share URL: `/<category>/<polst-slug>` — exactly two
  // segments where the first is not a reserved top-level route. The
  // backend's `polst.getById` resolves slugs natively, so we hand off
  // the polst-slug as the id.
  if (segments.length === 2 && !RESERVED_TOP_SEGMENTS.has(head) && second) {
    return {
      target: { kind: 'polst', id: decodeURIComponent(second) },
      inferredEnv,
    };
  }

  return null;
}

/**
 * @param {unknown} value
 * @returns {Env | null}
 */
function coerceEnv(value) {
  if (typeof value !== 'string') return null;
  const lc = value.toLowerCase();
  return ENVS.includes(/** @type {Env} */ (lc)) ? /** @type {Env} */ (lc) : null;
}

/**
 * @param {URLSearchParams | string | null | undefined} [searchInput]
 * @returns {URLSearchParams}
 */
function asSearchParams(searchInput) {
  if (searchInput instanceof URLSearchParams) return searchInput;
  if (typeof searchInput === 'string') return new URLSearchParams(searchInput);
  if (typeof window !== 'undefined' && window.location) {
    return new URLSearchParams(window.location.search);
  }
  return new URLSearchParams('');
}

/**
 * @param {URLSearchParams | string} [searchInput] - optional override
 *   used by tests; defaults to `window.location.search`.
 * @returns {PolstTarget | null}
 */
export function getPolstTarget(searchInput) {
  const params = asSearchParams(searchInput);
  const parsed = parsePolstParam(params.get('polst'));
  return parsed ? parsed.target : null;
}

/**
 * Resolve the active env. Precedence:
 *   1. explicit `?env=<env>`
 *   2. env inferred from `?polst=<url>` hostname
 *   3. default `'prod'`
 *
 * @param {URLSearchParams | string} [searchInput]
 * @returns {Env}
 */
export function getEnv(searchInput) {
  const params = asSearchParams(searchInput);
  const explicit = coerceEnv(params.get('env'));
  if (explicit) return explicit;
  const parsed = parsePolstParam(params.get('polst'));
  if (parsed && parsed.inferredEnv) return parsed.inferredEnv;
  return 'prod';
}

/**
 * @param {Env} env
 * @returns {string}
 */
export function getApiOrigin(env) {
  const e = coerceEnv(env) ?? 'prod';
  return ORIGINS[e].api;
}

/**
 * @param {Env} env
 * @returns {string}
 */
export function getAppOrigin(env) {
  const e = coerceEnv(env) ?? 'prod';
  return ORIGINS[e].app;
}
