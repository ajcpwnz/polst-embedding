// docs/assets/chrome-render.js
//
// DOM-mounting layer for the shared chrome. Demo pages call
// `renderChrome()` from a small inline module on DOMContentLoaded.
// `renderEmptyState()` is exposed so each per-mode demo can render the
// paste-prompt placeholder while no polst is selected.

import {
  ENVS,
  getEnv,
  getPolstTarget,
  installCopyHandlers,
  installHealthBanner,
  installPrismHighlighting,
} from './chrome.js';

const SOURCE_BASE =
  'https://github.com/ajcpwnz/polst-embedding/blob/master/';

/**
 * @param {string} tag
 * @param {Record<string, string>} [attrs]
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
    for (const child of children) {
      node.append(child);
    }
  }
  return node;
}

/**
 * Update one query parameter on the current location and reload. If
 * `value` is empty / null the param is removed.
 *
 * @param {string} key
 * @param {string | null} value
 */
function setParamAndReload(key, value) {
  const url = new URL(window.location.href);
  if (value == null || value === '') {
    url.searchParams.delete(key);
  } else {
    url.searchParams.set(key, value);
  }
  // assign() preserves history so back-button works.
  window.location.assign(url.toString());
}

/**
 * Mount the shared top-bar into `containerEl`. Replaces any existing
 * children. Idempotent — calling it twice replaces the bar.
 *
 * @param {HTMLElement} containerEl
 * @param {{ sourcePath?: string }} [opts]
 *   - sourcePath: repo-relative path the "View source" link should
 *     point at, e.g. `docs/iframe/index.html`.
 */
export function renderChrome(containerEl, opts) {
  if (!containerEl) return;
  const sourcePath = opts && opts.sourcePath;

  containerEl.innerHTML = '';
  containerEl.classList.add('polst-chrome');

  const back = el(
    'a',
    { class: 'polst-chrome__back', href: '../' },
    ['← All modes'],
  );

  const divider1 = el('span', {
    class: 'polst-chrome__divider',
    'aria-hidden': 'true',
  });

  // Env switcher.
  const envField = el('span', { class: 'polst-chrome__field' });
  const envLabelId = 'polst-chrome-env-label';
  envField.append(
    el('label', { id: envLabelId, for: 'polst-chrome-env' }, ['env']),
  );
  const select = /** @type {HTMLSelectElement} */ (
    el('select', {
      id: 'polst-chrome-env',
      class: 'polst-chrome__select',
      'aria-labelledby': envLabelId,
    })
  );
  const currentEnv = getEnv();
  for (const env of ENVS) {
    const option = /** @type {HTMLOptionElement} */ (
      el('option', { value: env }, [env])
    );
    if (env === currentEnv) option.selected = true;
    select.append(option);
  }
  select.addEventListener('change', () => {
    setParamAndReload('env', select.value);
  });
  envField.append(select);

  const divider2 = el('span', {
    class: 'polst-chrome__divider',
    'aria-hidden': 'true',
  });

  // Polst-link form.
  const form = /** @type {HTMLFormElement} */ (
    el('form', { class: 'polst-chrome__form' })
  );
  const input = /** @type {HTMLInputElement} */ (
    el('input', {
      type: 'text',
      class: 'polst-chrome__input',
      name: 'polst',
      placeholder: 'Polst, brand, or campaign URL — or a 10-char short id',
      'aria-label': 'Polst, brand, or campaign URL or short id',
      autocomplete: 'off',
      spellcheck: 'false',
    })
  );
  // Pre-fill from current ?polst=.
  const initialPolst = new URLSearchParams(window.location.search).get(
    'polst',
  );
  if (initialPolst) input.value = initialPolst;
  form.append(input);
  form.append(
    el('button', { type: 'submit', class: 'polst-chrome__submit' }, [
      'Go',
    ]),
  );
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    setParamAndReload('polst', input.value.trim() || null);
  });

  const fragment = [back, divider1, envField, divider2, form];

  if (sourcePath) {
    const sourceUrl = SOURCE_BASE + String(sourcePath).replace(/^\/+/, '');
    fragment.push(
      el(
        'a',
        {
          class: 'polst-chrome__source',
          href: sourceUrl,
          target: '_blank',
          rel: 'noopener noreferrer',
        },
        ['View source'],
      ),
    );
  }

  for (const node of fragment) containerEl.append(node);
}

/**
 * Render the paste-prompt empty state inside `containerEl`. Replaces
 * existing children.
 *
 * @param {HTMLElement} containerEl
 */
export function renderEmptyState(containerEl) {
  if (!containerEl) return;
  containerEl.innerHTML = '';
  const wrap = el('div', { class: 'polst-empty' });
  const p = el('p');
  p.append(
    'Paste a Polst link above to see this embed mode in action. ' +
      "Don’t have one? Create one at ",
  );
  p.append(
    el(
      'a',
      {
        href: 'https://polst.app',
        target: '_blank',
        rel: 'noopener noreferrer',
      },
      ['polst.app'],
    ),
  );
  p.append('.');
  wrap.append(p);
  containerEl.append(wrap);
}

/**
 * Locate or inject the health-banner mount point. Returns the
 * `<div id="chrome-health">` element, creating one and inserting it
 * directly above the chrome shell (or prepending to `<body>`) if it
 * doesn't already exist.
 *
 * @param {HTMLElement | null} chromeEl
 * @returns {HTMLElement | null}
 */
function ensureHealthMount(chromeEl) {
  let healthEl = document.getElementById('chrome-health');
  if (healthEl) return healthEl;
  if (!document.body) return null;
  healthEl = document.createElement('div');
  healthEl.id = 'chrome-health';
  if (chromeEl && chromeEl.parentNode) {
    chromeEl.parentNode.insertBefore(healthEl, chromeEl);
  } else {
    document.body.insertBefore(healthEl, document.body.firstChild);
  }
  return healthEl;
}

/**
 * Convenience: wire DOMContentLoaded → renderChrome + (when no polst)
 * renderEmptyState. Each per-mode page can call this directly to
 * minimize boilerplate.
 *
 * Also injects a health banner above the chrome shell unless
 * `opts.skipHealthBanner` is set, runs Prism syntax highlighting
 * unless `opts.skipPrism` is set, and wires any
 * `<button data-copy-target>` buttons unless `opts.skipCopyHandlers`
 * is set. Per-page HTML doesn't need to change — `bootstrap()` creates
 * `<div id="chrome-health">` on demand.
 *
 * @param {{
 *   sourcePath: string,
 *   skipHealthBanner?: boolean,
 *   skipPrism?: boolean,
 *   skipCopyHandlers?: boolean,
 * }} opts
 */
export function bootstrap(opts) {
  const run = () => {
    const chromeEl = document.getElementById('chrome');
    const demoEl = document.getElementById('demo');
    if (!opts || !opts.skipHealthBanner) {
      const healthEl = ensureHealthMount(chromeEl);
      if (healthEl) installHealthBanner(healthEl);
    }
    if (chromeEl) renderChrome(chromeEl, opts);
    if (demoEl && !getPolstTarget()) renderEmptyState(demoEl);
    if (!opts || !opts.skipPrism) installPrismHighlighting();
    if (!opts || !opts.skipCopyHandlers) installCopyHandlers();
  };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run, { once: true });
  } else {
    run();
  }
}
