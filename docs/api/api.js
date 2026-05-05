// docs/api/api.js
//
// POL-776 — REST API demo page.
//
// Vertical list of six variant cards, each demonstrating one
// `/api/rest/v1` endpoint. Each card has:
//   - a header row (method pill + full URL + Run button)
//   - a 2-column body: copy-paste-able `fetch()` snippet on the left,
//     live response panel on the right
//   - an optional inline hint (used for the POL-768 keyless-vote
//     fallback note when an env still requires the idempotency-key)
//
// "Snippet uses resolved hostname, not placeholder" is load-bearing —
// the rendered snippet inlines the active env's `apiOrigin` and the
// real `X-Device-Id` literally, so a copy-paste actually runs.
//
// No build step. Vanilla ESM. No third-party deps.

import {
  getApiOrigin,
  getEnv,
  getPolstTarget,
} from '../assets/chrome.js';

/** @typedef {'polst' | 'brand' | 'campaign'} TargetKind */
/** @typedef {{ kind: TargetKind, id: string }} PolstTarget */

const DEVICE_ID_KEY = 'polst-embed-device-id';

/**
 * Get-or-create the persistent X-Device-Id. Generated once on first
 * read via `crypto.randomUUID()` and cached in localStorage. Falls
 * back to an in-memory uuid if storage is unavailable.
 *
 * @returns {string}
 */
function getDeviceId() {
  try {
    const existing = window.localStorage.getItem(DEVICE_ID_KEY);
    if (existing) return existing;
    const fresh = crypto.randomUUID();
    window.localStorage.setItem(DEVICE_ID_KEY, fresh);
    return fresh;
  } catch {
    // Storage unavailable (private mode, denied, etc.). Stable for the
    // life of this module instance, which is good enough for the demo.
    if (!getDeviceId._fallback) {
      getDeviceId._fallback = crypto.randomUUID();
    }
    return getDeviceId._fallback;
  }
}

/**
 * Variant config table. Each entry describes one of the six demo
 * requests. `needs` constrains which polst-target kind the variant
 * accepts; if the visitor's link is the wrong kind, the card greys
 * out. `body` is POST-only.
 *
 * @typedef {{
 *   key: string,
 *   label: string,
 *   method: 'GET' | 'POST',
 *   pathTpl: string,
 *   needs: TargetKind,
 *   body?: object,
 * }} Variant
 *
 * @type {ReadonlyArray<Variant>}
 */
const VARIANTS = Object.freeze([
  {
    key: 'polst',
    label: 'Get a polst',
    method: 'GET',
    pathTpl: '/api/rest/v1/polsts/{id}',
    needs: 'polst',
  },
  {
    key: 'results',
    label: 'Get polst results',
    method: 'GET',
    pathTpl: '/api/rest/v1/polsts/{id}/results',
    needs: 'polst',
  },
  {
    key: 'vote',
    label: 'Cast a vote',
    method: 'POST',
    pathTpl: '/api/rest/v1/polsts/{id}/votes',
    needs: 'polst',
    body: { choice: 'A' },
  },
  {
    key: 'brand',
    label: 'Get a brand',
    method: 'GET',
    pathTpl: '/api/rest/v1/brands/{id}',
    needs: 'brand',
  },
  {
    key: 'feed',
    label: 'Get a brand feed',
    method: 'GET',
    pathTpl: '/api/rest/v1/brands/{id}/feed?limit=20',
    needs: 'brand',
  },
  {
    key: 'campaign',
    label: 'Get a campaign',
    method: 'GET',
    pathTpl: '/api/rest/v1/campaigns/{id}',
    needs: 'campaign',
  },
]);

const NEEDS_LABEL = Object.freeze({
  polst: 'polst',
  brand: 'brand',
  campaign: 'campaign',
});

/**
 * @param {string} tag
 * @param {Record<string, string> | null} [attrs]
 * @param {Array<Node | string>} [children]
 * @returns {HTMLElement}
 */
function el(tag, attrs, children) {
  const node = document.createElement(tag);
  if (attrs) {
    for (const [k, v] of Object.entries(attrs)) {
      if (v == null) continue;
      node.setAttribute(k, v);
    }
  }
  if (children) {
    for (const child of children) node.append(child);
  }
  return node;
}

/**
 * Substitute `{id}` in a path template with the live identifier.
 *
 * @param {string} tpl
 * @param {string} id
 * @returns {string}
 */
function materialisePath(tpl, id) {
  return tpl.replace('{id}', encodeURIComponent(id));
}

/**
 * Build the copy-paste-able `fetch()` snippet for a variant. The
 * resulting string uses the active env's `apiOrigin` and (for vote)
 * the real persisted device id literally — no `<apiOrigin>` and no JS
 * template-literal expressions. Pasting the output into a browser
 * console must run successfully.
 *
 * @param {Variant} variant
 * @param {string} apiOrigin
 * @param {string} deviceId
 * @param {string} id
 * @returns {string}
 */
function buildSnippet(variant, apiOrigin, deviceId, id) {
  const url = apiOrigin + materialisePath(variant.pathTpl, id);
  if (variant.method === 'POST') {
    const bodyJson = JSON.stringify(variant.body ?? {});
    return (
      'const res = await fetch(\n' +
      `  ${JSON.stringify(url)},\n` +
      '  {\n' +
      '    method: "POST",\n' +
      '    headers: {\n' +
      '      "Accept": "application/json",\n' +
      '      "Content-Type": "application/json",\n' +
      `      "X-Device-Id": ${JSON.stringify(deviceId)},\n` +
      '    },\n' +
      `    body: ${JSON.stringify(bodyJson)},\n` +
      '  }\n' +
      ');\n' +
      'const data = await res.json();'
    );
  }
  return (
    'const res = await fetch(\n' +
    `  ${JSON.stringify(url)},\n` +
    '  { headers: { "Accept": "application/json" } }\n' +
    ');\n' +
    'const data = await res.json();'
  );
}

/**
 * Map an HTTP status to a status-badge class.
 *
 * @param {number | null} status
 * @returns {string}
 */
function statusBadgeClass(status) {
  if (status == null) return 'api-status__badge--err';
  if (status >= 200 && status < 300) return 'api-status__badge--2xx';
  if (status >= 400 && status < 500) return 'api-status__badge--4xx';
  if (status >= 500) return 'api-status__badge--5xx';
  return 'api-status__badge--err';
}

/**
 * Best-effort body decode: try JSON pretty-print, fall back to raw
 * text. Returns the rendered string and a boolean flag indicating
 * whether the response body matched the POL-768 idempotency-key
 * required pattern (so the caller can surface the keyless-vote hint).
 *
 * @param {Response} res
 * @returns {Promise<{ text: string, isJson: boolean, idempotencyKeyRequired: boolean }>}
 */
async function decodeBody(res) {
  const raw = await res.text();
  let pretty = raw;
  let isJson = false;
  try {
    const parsed = JSON.parse(raw);
    pretty = JSON.stringify(parsed, null, 2);
    isJson = true;
  } catch {
    // Leave as raw text.
  }
  // Match a body that complains about the missing idempotency-key.
  // The exact server message is "X-Polst-Idempotency-Key required" or
  // similar variants — match loosely.
  const idempotencyKeyRequired =
    res.status === 400 && /idempotency[-_ ]?key/i.test(raw);
  return { text: pretty, isJson, idempotencyKeyRequired };
}

/**
 * Render a "copy" button next to a `<pre>`. Wires clipboard and a
 * brief "Copied" affordance. Failure is silent (POL-780 will provide
 * the shared helper).
 *
 * @param {HTMLPreElement} preEl
 * @returns {HTMLButtonElement}
 */
function makeCopyButton(preEl) {
  const btn = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'api-panel__copy' }, ['Copy'])
  );
  btn.addEventListener('click', async () => {
    const text = preEl.textContent || '';
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        btn.textContent = 'Copied';
        window.setTimeout(() => {
          btn.textContent = 'Copy';
        }, 1200);
      }
    } catch {
      // no-op
    }
  });
  return btn;
}

/**
 * Render a single variant card.
 *
 * @param {Variant} variant
 * @param {PolstTarget} target
 * @param {string} apiOrigin
 * @returns {HTMLElement}
 */
function renderCard(variant, target, apiOrigin) {
  const matches = variant.needs === target.kind;
  const id = matches ? target.id : `<${NEEDS_LABEL[variant.needs]}-id>`;
  const fullUrl = apiOrigin + materialisePath(variant.pathTpl, id);
  const deviceId = getDeviceId();

  const card = el('section', {
    class: 'api-card' + (matches ? '' : ' api-card--disabled'),
    'data-variant': variant.key,
  });

  // ---------- Header ----------
  const header = el('div', { class: 'api-card__header' });
  const methodCls =
    variant.method === 'POST' ? 'api-card__method--post' : 'api-card__method--get';
  header.append(
    el('span', { class: `api-card__method ${methodCls}` }, [variant.method]),
  );
  header.append(el('span', { class: 'api-card__url' }, [fullUrl]));

  /** @type {HTMLButtonElement} */
  let runBtn;
  if (matches) {
    runBtn = /** @type {HTMLButtonElement} */ (
      el('button', { type: 'button', class: 'api-card__run' }, ['Run'])
    );
    header.append(runBtn);
  } else {
    header.append(
      el('span', { class: 'api-card__disabled-note' }, [
        `Paste a ${NEEDS_LABEL[variant.needs]} link to try this`,
      ]),
    );
  }
  card.append(header);

  // ---------- Body grid ----------
  const body = el('div', { class: 'api-card__body' });

  // Left: snippet panel.
  const snippetPanel = el('div', { class: 'api-panel' });
  const snippetHead = el('div', { class: 'api-panel__header' });
  snippetHead.append(el('h3', null, ['Snippet']));
  const snippetPre = /** @type {HTMLPreElement} */ (
    el('pre', { class: 'api-panel__pre' })
  );
  const snippetCode = el('code');
  snippetPre.append(snippetCode);
  snippetCode.textContent = buildSnippet(variant, apiOrigin, deviceId, id);
  snippetHead.append(makeCopyButton(snippetPre));
  snippetPanel.append(snippetHead, snippetPre);
  body.append(snippetPanel);

  // Right: response panel.
  const responsePanel = el('div', { class: 'api-panel' });
  const responseHead = el('div', { class: 'api-panel__header' });
  responseHead.append(el('h3', null, ['Response']));
  responsePanel.append(responseHead);
  const responseSlot = el('div', { class: 'api-panel__slot' });
  const placeholder = el('p', { class: 'api-panel__placeholder' }, [
    matches
      ? 'Hit Run to fire the request.'
      : `Provide a ${NEEDS_LABEL[variant.needs]} link to enable this variant.`,
  ]);
  responseSlot.append(placeholder);
  responsePanel.append(responseSlot);
  body.append(responsePanel);

  card.append(body);

  // Hint slot (POL-768 keyless-vote fallback note, etc.).
  const hintSlot = el('div', { class: 'api-card__hint-slot' });
  card.append(hintSlot);

  // ---------- Run wiring ----------
  if (matches) {
    runBtn.addEventListener('click', async () => {
      runBtn.disabled = true;
      runBtn.textContent = 'Running…';
      hintSlot.innerHTML = '';
      responseSlot.innerHTML = '';
      responseSlot.append(
        el('p', { class: 'api-panel__placeholder' }, ['Awaiting response…']),
      );

      const start = performance.now();
      /** @type {Response | null} */
      let res = null;
      /** @type {unknown} */
      let networkError = null;

      const init = {
        method: variant.method,
        headers: /** @type {Record<string, string>} */ ({
          Accept: 'application/json',
        }),
      };
      if (variant.method === 'POST') {
        init.headers['Content-Type'] = 'application/json';
        init.headers['X-Device-Id'] = deviceId;
        init.body = JSON.stringify(variant.body ?? {});
      }

      try {
        res = await fetch(fullUrl, init);
      } catch (err) {
        networkError = err;
      }

      const duration = Math.round(performance.now() - start);

      responseSlot.innerHTML = '';

      if (networkError) {
        const status = el('div', { class: 'api-status' });
        status.append(
          el('span', { class: 'api-status__badge api-status__badge--err' }, [
            'Network',
          ]),
        );
        status.append(
          el('span', { class: 'api-status__duration' }, [`${duration} ms`]),
        );
        responseSlot.append(status);
        const message =
          networkError instanceof Error
            ? networkError.message
            : String(networkError);
        const pre = el('pre', { class: 'api-panel__pre' });
        pre.append(el('code', null, [message]));
        responseSlot.append(pre);
      } else if (res) {
        const status = el('div', { class: 'api-status' });
        status.append(
          el(
            'span',
            { class: `api-status__badge ${statusBadgeClass(res.status)}` },
            [String(res.status)],
          ),
        );
        status.append(el('span', null, [res.statusText || '']));
        status.append(
          el('span', { class: 'api-status__duration' }, [`${duration} ms`]),
        );
        responseSlot.append(status);

        // Collect a small subset of useful response headers.
        const interesting = ['content-type', 'x-ratelimit-remaining'];
        const headerLines = [];
        for (const name of interesting) {
          const value = res.headers.get(name);
          if (value) headerLines.push(`${name}: ${value}`);
        }
        if (headerLines.length > 0) {
          const headerPre = el('pre', { class: 'api-panel__pre' });
          headerPre.append(el('code', null, [headerLines.join('\n')]));
          responseSlot.append(headerPre);
        }

        const decoded = await decodeBody(res);
        const bodyPre = /** @type {HTMLPreElement} */ (
          el('pre', { class: 'api-panel__pre' })
        );
        bodyPre.append(el('code', null, [decoded.text || '(empty body)']));
        responseSlot.append(bodyPre);

        if (
          variant.key === 'vote' &&
          decoded.idempotencyKeyRequired
        ) {
          const note = el(
            'p',
            { class: 'api-card__hint api-card__hint--warn' },
            [
              'This endpoint is being made keyless under POL-768 — your env may not have that fix yet.',
            ],
          );
          hintSlot.innerHTML = '';
          hintSlot.append(note);
        }
      }

      runBtn.disabled = false;
      runBtn.textContent = 'Run';
    });
  }

  return card;
}

/**
 * Render the footer with a live link to Swagger UI for the active env.
 *
 * @param {string} apiOrigin
 * @returns {HTMLElement}
 */
function renderFooter(apiOrigin) {
  const footer = el('footer', { class: 'api-footer' });
  const docsUrl = apiOrigin + '/api/rest/v1/docs';
  footer.append('Full API reference: ');
  footer.append(
    el(
      'a',
      { href: docsUrl, target: '_blank', rel: 'noopener noreferrer' },
      [docsUrl],
    ),
  );
  return footer;
}

/**
 * Mount the API demo into `rootEl`. Replaces existing children.
 *
 * @param {HTMLElement} rootEl
 * @param {PolstTarget} target
 */
export function mountApiDemo(rootEl, target) {
  rootEl.innerHTML = '';
  rootEl.classList.add('api-demo');

  const apiOrigin = getApiOrigin(getEnv());

  for (const variant of VARIANTS) {
    rootEl.append(renderCard(variant, target, apiOrigin));
  }

  rootEl.append(renderFooter(apiOrigin));
}

/**
 * Entry point invoked from `index.html` after `bootstrap()` mounts
 * the chrome bar. Renders the demo into `#demo` when a polst target
 * is present; otherwise leaves `bootstrap()`'s empty state in place.
 */
export function init() {
  const demoEl = document.getElementById('demo');
  if (!demoEl) return;
  const target = getPolstTarget();
  if (!target) return;
  mountApiDemo(/** @type {HTMLElement} */ (demoEl), target);
}
