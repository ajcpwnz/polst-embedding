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

/** @typedef {'dev' | 'canary' | 'staging' | 'prod'} Env */
/** @typedef {{ kind: 'polst' | 'brand' | 'campaign', id: string }} PolstTarget */

/** @type {ReadonlyArray<Env>} */
export const ENVS = Object.freeze(['dev', 'canary', 'staging', 'prod']);

/** @type {Record<Env, { app: string, api: string }>} */
const ORIGINS = Object.freeze({
  dev: {
    app: 'http://localhost:3000',
    api: 'http://localhost:8000',
  },
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

const RAW_ID_RE = /^[A-Za-z0-9]{12}$/;
const PATH_RE = /^\/([pbc])\/([^/?#]+)\/?$/;

/**
 * @param {string} hostname
 * @returns {Env | null}
 */
function envFromHostname(hostname) {
  if (!hostname) return null;
  const h = hostname.toLowerCase();
  if (h === 'localhost' || h === '127.0.0.1' || h === '0.0.0.0') return 'dev';
  if (h === 'canary.polst.app') return 'canary';
  if (h === 'staging.polst.app') return 'staging';
  if (h === 'polst.app' || h === 'www.polst.app') return 'prod';
  return null;
}

/**
 * @param {string} kindChar - 'p' | 'b' | 'c'
 * @returns {PolstTarget['kind'] | null}
 */
function kindFromChar(kindChar) {
  if (kindChar === 'p') return 'polst';
  if (kindChar === 'b') return 'brand';
  if (kindChar === 'c') return 'campaign';
  return null;
}

/**
 * Parse the raw `?polst=` value into a target plus an optionally
 * inferred env. Returns null when the input is unrecognized.
 *
 * @param {string | null | undefined} raw
 * @returns {{ target: PolstTarget, inferredEnv: Env | null } | null}
 */
export function parsePolstParam(raw) {
  if (raw == null) return null;
  const value = String(raw).trim();
  if (value === '') return null;

  // Raw 12-char alphanumeric (no protocol) — treat as polst short id.
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
  const match = PATH_RE.exec(url.pathname);
  if (!match) return null;
  const kind = kindFromChar(match[1]);
  if (!kind) return null;
  const id = decodeURIComponent(match[2]);
  if (!id) return null;
  return { target: { kind, id }, inferredEnv };
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
