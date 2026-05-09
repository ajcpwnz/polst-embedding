// docs/assets/chrome.js
//
// Shared chrome — logic + small mount helpers for the polst-embedding
// demo pages.
//
// Responsibilities:
//   - Parse `?polst=<url-or-id>` and `?env=<env>` from the location.
//   - Provide `getPolstTarget()` / `getEnv()` to consumers.
//   - Provide `getApiOrigin(env)` / `getAppOrigin(env)` for backends.
//   - Health banner helpers (`pingHealth`, `installHealthBanner`).
//
// The bulk of DOM mounting still lives in `chrome-render.js`. Small
// self-contained mount helpers (the "Health banner" section below,
// future copy handlers, etc.) live here so they can be wired in by
// `bootstrap()` without per-page changes. Vanilla ESM. No runtime deps.
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

// --------------------------------------------------------------------
// Health banner
// --------------------------------------------------------------------
//
// `pingHealth(apiOrigin)` GETs `<apiOrigin>/api/rest/v1/health`, treats
// any non-2xx, network error, or 5s-timeout as "down", and caches the
// result for 30s per `apiOrigin` to avoid spamming the endpoint on
// every demo interaction.
//
// `installHealthBanner(containerEl)` mounts the env / hostname / status
// row into `containerEl`, performs an immediate check, and schedules a
// 30s `setInterval`. Idempotent: a second mount on the same element
// clears the prior interval first.
//
// This section is the only DOM-touching code in this file. It's kept
// here (rather than in `chrome-render.js`) so the wiring point in
// `bootstrap()` only has to import a single helper, and so future
// `installX()` mount helpers (e.g. POL-780's copy handlers) compose
// cleanly without per-page HTML changes.

const HEALTH_CACHE_MS = 30_000;
const HEALTH_TIMEOUT_MS = 5_000;
const HEALTH_REFRESH_MS = 30_000;

/** @type {Map<string, { healthy: boolean, checkedAt: number }>} */
const _healthCache = new Map();

/**
 * Ping `<apiOrigin>/api/rest/v1/health`. Returns the cached result if
 * one is younger than 30s (per apiOrigin), otherwise issues a fresh
 * fetch with a 5s timeout. Any non-2xx response, network error, or
 * abort is reported as `healthy: false`.
 *
 * @param {string} apiOrigin
 * @param {{ force?: boolean }} [opts]
 * @returns {Promise<{ healthy: boolean, checkedAt: number }>}
 */
export async function pingHealth(apiOrigin, opts) {
  const force = !!(opts && opts.force);
  const now = Date.now();
  const cached = _healthCache.get(apiOrigin);
  if (cached && !force && now - cached.checkedAt < HEALTH_CACHE_MS) {
    return cached;
  }

  const controller =
    typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer =
    controller != null
      ? setTimeout(() => controller.abort(), HEALTH_TIMEOUT_MS)
      : null;

  let healthy = false;
  try {
    const res = await fetch(`${apiOrigin}/api/rest/v1/health`, {
      method: 'GET',
      cache: 'no-store',
      signal: controller ? controller.signal : undefined,
    });
    healthy = !!(res && res.ok);
  } catch {
    healthy = false;
  } finally {
    if (timer != null) clearTimeout(timer);
  }

  const result = { healthy, checkedAt: Date.now() };
  _healthCache.set(apiOrigin, result);
  return result;
}

/**
 * Mount the env health banner into `containerEl`. Replaces existing
 * children. Performs an immediate `pingHealth` and schedules a 30s
 * refresh. Returns a teardown function that clears the interval.
 *
 * Idempotent: if `containerEl` already has an interval registered from
 * a prior call, that interval is cleared before the new one is
 * scheduled. The container's existing children are wiped on every
 * mount so a re-mount with a different env produces a clean banner.
 *
 * @param {HTMLElement | null} containerEl
 * @param {{ env?: Env, apiOrigin?: string, hostname?: string }} [opts]
 *   - env: explicit env override (defaults to `getEnv()`).
 *   - apiOrigin: explicit api origin (defaults to `getApiOrigin(env)`).
 *   - hostname: explicit hostname (defaults to URL-parsed apiOrigin).
 * @returns {() => void} teardown
 */
export function installHealthBanner(containerEl, opts) {
  if (!containerEl || typeof document === 'undefined') {
    return () => {};
  }

  // Idempotent re-mount: clear any prior interval stored on the node.
  const prior = /** @type {any} */ (containerEl)._polstHealthTeardown;
  if (typeof prior === 'function') {
    try {
      prior();
    } catch {
      /* no-op */
    }
  }

  const env = (opts && opts.env) || getEnv();
  const apiOrigin = (opts && opts.apiOrigin) || getApiOrigin(env);
  let hostname = opts && opts.hostname;
  if (!hostname) {
    try {
      hostname = new URL(apiOrigin).hostname;
    } catch {
      hostname = apiOrigin;
    }
  }

  containerEl.innerHTML = '';
  containerEl.classList.add('chrome-health-banner');

  const envEl = document.createElement('span');
  envEl.className = 'chrome-health-banner__env';
  envEl.textContent = String(env).toUpperCase();

  const sep1 = document.createElement('span');
  sep1.className = 'chrome-health-banner__sep';
  sep1.setAttribute('aria-hidden', 'true');
  sep1.textContent = '·';

  const hostEl = document.createElement('span');
  hostEl.className = 'chrome-health-banner__host';
  hostEl.textContent = hostname;

  const sep2 = document.createElement('span');
  sep2.className = 'chrome-health-banner__sep';
  sep2.setAttribute('aria-hidden', 'true');
  sep2.textContent = '·';

  const statusEl = document.createElement('span');
  statusEl.className = 'chrome-health-banner__status';
  statusEl.setAttribute('aria-live', 'polite');

  const dotEl = document.createElement('span');
  dotEl.className = 'chrome-health-dot';
  dotEl.setAttribute('aria-hidden', 'true');

  const labelEl = document.createElement('span');
  labelEl.className = 'chrome-health-banner__label';
  labelEl.textContent = 'checking…';

  statusEl.append(dotEl, ' ', labelEl);
  containerEl.append(envEl, ' ', sep1, ' ', hostEl, ' ', sep2, ' ', statusEl);

  const applyResult = (result) => {
    if (result && result.healthy) {
      dotEl.classList.add('is-healthy');
      dotEl.classList.remove('is-down');
      labelEl.textContent = '● API healthy';
    } else {
      dotEl.classList.add('is-down');
      dotEl.classList.remove('is-healthy');
      labelEl.textContent = '✕ API down';
    }
  };

  const check = () => {
    pingHealth(apiOrigin)
      .then(applyResult)
      .catch(() => applyResult({ healthy: false, checkedAt: Date.now() }));
  };

  check();
  const intervalId = setInterval(check, HEALTH_REFRESH_MS);

  const teardown = () => {
    clearInterval(intervalId);
    /** @type {any} */ (containerEl)._polstHealthTeardown = undefined;
  };
  /** @type {any} */ (containerEl)._polstHealthTeardown = teardown;
  return teardown;
}
