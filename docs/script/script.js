// docs/script/script.js
//
// POL-774 — script-tag (Shadow DOM widget) embed demo page logic.
//
// Renders three tabs (polst / campaign / brand) with live customisation
// controls. Builds a `<div data-polst*>` marker element on the live
// preview side and an exact `<script>` + `<div>` snippet on the right.
// Vanilla ESM, no build step.
//
// Re-hydration model: the page sets `data-polst-auto-observe` on
// `<html>` (in index.html) so the widget's MutationObserver scans the
// document for new marker elements after the initial pass. On every
// control change `update()` removes the live marker and inserts a
// fresh one with the new `data-*` attributes — the observer picks it
// up and hydrates inside its own Shadow DOM. This avoids a full page
// reload while still exercising the public auto-hydration path the
// widget ships to integrators.

import {
  getApiOrigin,
  getAppOrigin,
  getEnv,
  getPolstTarget,
} from '../assets/chrome.js';

/** @typedef {'polst' | 'campaign' | 'brand'} Tab */
/** @typedef {'light' | 'dark' | 'auto'} Theme */
/** @typedef {'polsts' | 'campaigns' | 'mixed'} BrandMode */

/**
 * @typedef {object} State
 * @property {Tab}        tab
 * @property {Theme}      theme
 * @property {string|null} accent      6-char lowercase hex without `#`, or null
 * @property {boolean}    hideTitle
 * @property {boolean}    hideBrand
 * @property {boolean}    autoAdvance  campaign-only
 * @property {BrandMode}  mode         brand-only
 */

/** @type {State} */
const DEFAULT_STATE = Object.freeze({
  tab: 'polst',
  theme: 'auto',
  accent: null,
  hideTitle: false,
  hideBrand: false,
  autoAdvance: true,
  mode: 'polsts',
});

const TAB_LABELS = Object.freeze({
  polst: 'Polst',
  campaign: 'Campaign',
  brand: 'Brand listing',
});

/** Marker attribute name per tab — matches `auto-hydrate.ts` discovery. */
const MARKER_ATTR = Object.freeze({
  polst: 'data-polst',
  campaign: 'data-polst-campaign',
  brand: 'data-polst-brand',
});

const WIDGET_SRC =
  'https://unpkg.com/@polst-web/widget@latest/dist/widget.esm.js';

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
    for (const child of children) node.append(child);
  }
  return node;
}

/**
 * Build the ordered list of `data-*` attribute pairs the marker `<div>`
 * (and the rendered snippet) should carry for the current tab + state.
 *
 * Insertion order is fixed so the snippet is deterministic. Only
 * non-default values appear so the snippet stays minimal.
 *
 * @param {State} state
 * @param {{ kind: 'polst' | 'campaign' | 'brand', id: string }} target
 * @returns {Array<[string, string]>}
 */
export function buildAttrs(state, target) {
  /** @type {Array<[string, string]>} */
  const attrs = [];
  attrs.push([MARKER_ATTR[state.tab], target.id]);
  if (state.theme !== DEFAULT_STATE.theme) {
    attrs.push(['data-theme', state.theme]);
  }
  if (state.accent) {
    attrs.push(['data-accent', `#${state.accent}`]);
  }
  if (state.hideTitle) {
    attrs.push(['data-hide-title', '']);
  }
  if (state.hideBrand) {
    attrs.push(['data-hide-brand', '']);
  }
  if (
    state.tab === 'campaign' &&
    state.autoAdvance !== DEFAULT_STATE.autoAdvance
  ) {
    attrs.push(['data-auto-advance', state.autoAdvance ? 'true' : 'false']);
  }
  if (state.tab === 'brand' && state.mode !== DEFAULT_STATE.mode) {
    attrs.push(['data-mode', state.mode]);
  }
  return attrs;
}

/**
 * Build the copy-pasteable `<script>` + `<div>` block matching the
 * current state. Mirrors the form a real integrator would copy.
 *
 * @param {Array<[string, string]>} attrs
 * @returns {string}
 */
export function buildSnippet(attrs) {
  const scriptLine =
    `<script src="${WIDGET_SRC}"\n        type="module" async></script>`;

  // Render the marker element. Boolean-style attrs (empty string value)
  // print as bare attribute names — same as how an author would type
  // them by hand and the same way the widget's parser interprets them.
  const divLines = ['<div'];
  attrs.forEach(([name, value], idx) => {
    const indent = idx === 0 ? '     ' : '     ';
    if (value === '') {
      divLines.push(`${indent}${name}`);
    } else {
      divLines.push(`${indent}${name}="${value}"`);
    }
  });
  // Close on the same line as the last attr to keep snippet compact.
  divLines[divLines.length - 1] = `${divLines[divLines.length - 1]}></div>`;
  const divBlock = divLines.join('\n');

  return `${scriptLine}\n${divBlock}`;
}

/**
 * @param {string} idValue
 * @param {string} labelText
 * @param {boolean} initial
 * @param {(next: boolean) => void} onChange
 * @returns {HTMLElement}
 */
function toggleControl(idValue, labelText, initial, onChange) {
  const wrap = el('label', { class: 'script-toggle', for: idValue });
  const input = /** @type {HTMLInputElement} */ (
    el('input', { type: 'checkbox', id: idValue })
  );
  input.checked = initial;
  input.addEventListener('change', () => onChange(input.checked));
  wrap.append(input, el('span', null, [labelText]));
  return wrap;
}

/**
 * @param {string} groupName
 * @param {ReadonlyArray<{ value: string, label: string }>} options
 * @param {string} initial
 * @param {(next: string) => void} onChange
 * @returns {HTMLElement}
 */
function radioGroup(groupName, options, initial, onChange) {
  const wrap = el('fieldset', { class: 'script-radio' });
  wrap.append(el('legend', null, [groupName]));
  for (const opt of options) {
    const id = `${groupName}-${opt.value}`;
    const label = el('label', { for: id });
    const input = /** @type {HTMLInputElement} */ (
      el('input', { type: 'radio', id, name: groupName, value: opt.value })
    );
    if (opt.value === initial) input.checked = true;
    input.addEventListener('change', () => {
      if (input.checked) onChange(opt.value);
    });
    label.append(input, el('span', null, [opt.label]));
    wrap.append(label);
  }
  return wrap;
}

/**
 * @param {string} idValue
 * @param {string|null} initial
 * @param {(next: string|null) => void} onChange
 * @returns {HTMLElement}
 */
function accentControl(idValue, initial, onChange) {
  const wrap = el('div', { class: 'script-accent' });
  const label = el('label', { for: idValue }, ['accent']);
  const input = /** @type {HTMLInputElement} */ (
    el('input', { type: 'color', id: idValue })
  );
  input.value = initial ? `#${initial}` : '#2962ff';
  input.addEventListener('input', () => {
    onChange(input.value.replace(/^#/, '').toLowerCase());
  });
  const clear = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'script-accent__clear' }, ['clear'])
  );
  clear.addEventListener('click', () => {
    onChange(null);
  });
  wrap.append(label, input, clear);
  return wrap;
}

/**
 * @param {string} idValue
 * @param {string} labelText
 * @param {ReadonlyArray<{ value: string, label: string }>} options
 * @param {string} initial
 * @param {(next: string) => void} onChange
 * @returns {HTMLElement}
 */
function selectControl(idValue, labelText, options, initial, onChange) {
  const wrap = el('div', { class: 'script-select' });
  wrap.append(el('label', { for: idValue }, [labelText]));
  const select = /** @type {HTMLSelectElement} */ (
    el('select', { id: idValue })
  );
  for (const opt of options) {
    const option = /** @type {HTMLOptionElement} */ (
      el('option', { value: opt.value }, [opt.label])
    );
    if (opt.value === initial) option.selected = true;
    select.append(option);
  }
  select.addEventListener('change', () => onChange(select.value));
  wrap.append(select);
  return wrap;
}

/**
 * Wait (briefly) for `window.Polst` to land, then call
 * `configure({ origins })`. The bundle is loaded with `type="module"
 * async` so its parse may finish after this page module runs. We poll
 * for up to ~1s before giving up — the widget's auto-hydration would
 * still work against the build-time origins, just not the env we want.
 *
 * @param {{ apiOrigin: string, appOrigin: string }} origins
 * @returns {Promise<void>}
 */
function configureWidgetOrigins(origins) {
  return new Promise((resolve) => {
    const start = Date.now();
    const TIMEOUT_MS = 1000;
    const tryOnce = () => {
      const polst = /** @type {any} */ (window).Polst;
      if (polst && typeof polst.configure === 'function') {
        try {
          polst.configure({ origins });
        } catch (err) {
          // Surface as console error but never throw into the host.
          console.error('[polst-demo] configure() threw:', err);
        }
        resolve();
        return;
      }
      if (Date.now() - start >= TIMEOUT_MS) {
        console.warn(
          '[polst-demo] window.Polst never appeared; rendering against build-time origins',
        );
        resolve();
        return;
      }
      window.setTimeout(tryOnce, 25);
    };
    tryOnce();
  });
}

/**
 * Mount the script-tag demo into `rootEl`. Replaces existing children.
 *
 * @param {HTMLElement} rootEl
 * @param {{ kind: 'polst' | 'brand' | 'campaign', id: string }} target
 */
export async function mountScriptDemo(rootEl, target) {
  rootEl.innerHTML = '';
  rootEl.classList.add('script-demo');

  // Configure origins BEFORE we ever insert a marker `<div>` so the
  // first hydration sees the env we mean to demo against.
  await configureWidgetOrigins({
    apiOrigin: getApiOrigin(getEnv()),
    appOrigin: getAppOrigin(getEnv()),
  });

  /** @type {State} */
  const state = { ...DEFAULT_STATE, tab: target.kind };

  // ---------- Tabs ----------
  const tabsRow = el('div', { class: 'script-tabs', role: 'tablist' });
  /** @type {Record<Tab, HTMLButtonElement>} */
  const tabButtons = /** @type {any} */ ({});
  /** @type {Tab[]} */
  const tabOrder = ['polst', 'campaign', 'brand'];
  for (const tab of tabOrder) {
    const btn = /** @type {HTMLButtonElement} */ (
      el('button', {
        type: 'button',
        class: 'script-tab',
        role: 'tab',
        'data-tab': tab,
      }, [TAB_LABELS[tab]])
    );
    btn.addEventListener('click', () => {
      if (state.tab === tab) return;
      state.tab = tab;
      syncTabUi();
      update();
    });
    tabButtons[tab] = btn;
    tabsRow.append(btn);
  }

  // ---------- Body grid ----------
  const grid = el('div', { class: 'script-grid' });

  const previewCol = el('div', { class: 'script-preview' });
  const mountSlot = el('div', { class: 'script-preview__mount' });
  previewCol.append(mountSlot);

  const sideCol = el('div', { class: 'script-side' });

  // ---------- Controls (per tab) ----------
  /** @type {Record<Tab, HTMLElement>} */
  const controlPanels = /** @type {any} */ ({});

  for (const tab of tabOrder) {
    const panel = el('div', { class: 'script-controls', 'data-tab': tab });
    const heading = el('h2', { class: 'script-controls__heading' }, [
      `${TAB_LABELS[tab]} controls`,
    ]);
    panel.append(heading);
    panel.append(commonControlsForTab(tab));

    if (tab === 'campaign') {
      panel.append(
        toggleControl('autoAdvance', 'auto advance', state.autoAdvance, (next) => {
          state.autoAdvance = next;
          update();
        }),
      );
    }
    if (tab === 'brand') {
      panel.append(
        selectControl(
          'mode',
          'mode',
          [
            { value: 'polsts', label: 'polsts' },
            { value: 'campaigns', label: 'campaigns' },
            { value: 'mixed', label: 'mixed' },
          ],
          state.mode,
          (next) => {
            state.mode = /** @type {BrandMode} */ (next);
            update();
          },
        ),
      );
    }

    controlPanels[tab] = panel;
    sideCol.append(panel);
  }

  /**
   * @param {Tab} tab
   * @returns {HTMLElement}
   */
  function commonControlsForTab(tab) {
    const wrap = el('div', { class: 'script-controls__common' });
    wrap.append(
      radioGroup(
        `${tab}-theme`,
        [
          { value: 'light', label: 'light' },
          { value: 'dark', label: 'dark' },
          { value: 'auto', label: 'auto' },
        ],
        state.theme,
        (next) => {
          state.theme = /** @type {Theme} */ (next);
          syncCommonControls();
          update();
        },
      ),
    );
    wrap.append(
      accentControl(`${tab}-accent`, state.accent, (next) => {
        state.accent = next;
        syncCommonControls();
        update();
      }),
    );
    wrap.append(
      toggleControl(`${tab}-hideTitle`, 'hide title', state.hideTitle, (next) => {
        state.hideTitle = next;
        syncCommonControls();
        update();
      }),
    );
    wrap.append(
      toggleControl(`${tab}-hideBrand`, 'hide brand', state.hideBrand, (next) => {
        state.hideBrand = next;
        syncCommonControls();
        update();
      }),
    );
    return wrap;
  }

  /**
   * Mirror common-control state across all three panels so switching
   * tabs preserves the visitor's last setting.
   */
  function syncCommonControls() {
    for (const tab of tabOrder) {
      const panel = controlPanels[tab];
      const themeRadios = panel.querySelectorAll(
        `input[type="radio"][name="${tab}-theme"]`,
      );
      themeRadios.forEach((r) => {
        const radio = /** @type {HTMLInputElement} */ (r);
        radio.checked = radio.value === state.theme;
      });
      const accentInput = /** @type {HTMLInputElement | null} */ (
        panel.querySelector(`input[type="color"]#${tab}-accent`)
      );
      if (accentInput) {
        accentInput.value = state.accent ? `#${state.accent}` : '#2962ff';
      }
      const hideTitle = /** @type {HTMLInputElement | null} */ (
        panel.querySelector(`#${tab}-hideTitle`)
      );
      if (hideTitle) hideTitle.checked = state.hideTitle;
      const hideBrand = /** @type {HTMLInputElement | null} */ (
        panel.querySelector(`#${tab}-hideBrand`)
      );
      if (hideBrand) hideBrand.checked = state.hideBrand;
    }
  }

  // ---------- Snippet panel ----------
  const snippetPanel = el('div', { class: 'script-snippet' });
  const snippetHeader = el('div', { class: 'script-snippet__header' });
  snippetHeader.append(el('h2', null, ['Snippet']));
  const copyBtn = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'script-snippet__copy' }, ['Copy'])
  );
  copyBtn.addEventListener('click', async () => {
    const text = code.textContent || '';
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        copyBtn.textContent = 'Copied';
        window.setTimeout(() => {
          copyBtn.textContent = 'Copy';
        }, 1200);
      }
    } catch {
      // Silent. POL-POLISH-COPY (POL-780) introduces the shared helper
      // with proper fallback. For now we no-op on failure.
    }
  });
  snippetHeader.append(copyBtn);

  const pre = el('pre', { class: 'script-snippet__pre' });
  const code = el('code');
  pre.append(code);
  snippetPanel.append(snippetHeader, pre);

  sideCol.append(snippetPanel);

  grid.append(previewCol, sideCol);
  rootEl.append(tabsRow, grid);

  // ---------- Wiring ----------
  function syncTabUi() {
    for (const tab of tabOrder) {
      const isActive = tab === state.tab;
      tabButtons[tab].classList.toggle('script-tab--active', isActive);
      tabButtons[tab].setAttribute('aria-selected', String(isActive));
      controlPanels[tab].hidden = !isActive;
    }
  }

  /**
   * Re-render the live marker. We always replace the element rather
   * than mutating attrs in place because the widget guards against
   * double-hydration via `data-polst-hydrated`; a fresh node is the
   * cleanest signal that this is a new hydration target.
   */
  function update() {
    const attrs = buildAttrs(state, target);
    const marker = document.createElement('div');
    for (const [name, value] of attrs) {
      marker.setAttribute(name, value);
    }
    mountSlot.innerHTML = '';
    mountSlot.append(marker);
    code.textContent = buildSnippet(attrs);
  }

  syncTabUi();
  update();
}

/**
 * Entry point invoked from `index.html` after `bootstrap()` mounts the
 * chrome bar. Renders the demo into `#demo` when a polst target is
 * present; otherwise leaves the empty state in place.
 */
export function init() {
  const demoEl = document.getElementById('demo');
  if (!demoEl) return;
  const target = getPolstTarget();
  if (!target) return;
  // mountScriptDemo is async (it awaits configureWidgetOrigins). We do
  // not block init() on its completion — the chrome is already up,
  // the empty state has already been cleared by bootstrap, and any
  // mount errors are logged by the awaited promise itself.
  void mountScriptDemo(/** @type {HTMLElement} */ (demoEl), target);
}
