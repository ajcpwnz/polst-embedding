// docs/iframe/iframe.js
//
// POL-773 — iframe embed demo page logic.
//
// Renders three tabs (polst / campaign / brand) with live customisation
// controls. Builds the embed iframe `src` and the matching snippet HTML
// from a single state object. Vanilla ESM, no build step.
//
// Param defaults follow `apps/frontend/src/app/embed/embedConfig.ts`
// (POL-604). Only non-default params appear in the URL / snippet so the
// snippet is clean and copy-pasteable.

import {
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
 * Build the iframe `src` URL for the current tab + state.
 *
 * The visitor's polst-link only resolves to a single (kind, id) pair.
 * We always use that id regardless of which tab is active — so a
 * polst-link viewed under the campaign tab will 404 inside the embed,
 * which is the correct feedback. We do not silently substitute ids.
 *
 * @param {State} state
 * @param {string} appOrigin
 * @param {{ kind: 'polst' | 'campaign' | 'brand', id: string }} target
 * @returns {string}
 */
export function buildSrc(state, appOrigin, target) {
  const params = new URLSearchParams();
  if (state.theme !== DEFAULT_STATE.theme) params.set('theme', state.theme);
  if (state.accent) params.set('accent', state.accent);
  if (state.hideTitle) params.set('hideTitle', '1');
  if (state.hideBrand) params.set('hideBrand', '1');
  if (state.tab === 'campaign' && state.autoAdvance !== DEFAULT_STATE.autoAdvance) {
    params.set('autoAdvance', state.autoAdvance ? '1' : '0');
  }
  if (state.tab === 'brand' && state.mode !== DEFAULT_STATE.mode) {
    params.set('mode', state.mode);
  }
  const path = `/embed/${state.tab}/${encodeURIComponent(target.id)}`;
  const qs = params.toString();
  return `${appOrigin}${path}${qs ? `?${qs}` : ''}`;
}

/**
 * Build the copy-pasteable `<iframe ...></iframe>` snippet for the given src.
 *
 * @param {string} src
 * @returns {string}
 */
export function buildSnippet(src) {
  // Reasonable defaults for an embedded polst iframe. Sandbox is
  // permissive enough for the embed to function and emit postMessage
  // events; consumers can tighten further if their host pages require.
  const lines = [
    '<iframe',
    `  src="${src}"`,
    '  width="100%"',
    '  height="640"',
    '  loading="lazy"',
    '  referrerpolicy="strict-origin-when-cross-origin"',
    '  allow="clipboard-write"',
    '  style="border: 0; max-width: 100%;"',
    '></iframe>',
  ];
  return lines.join('\n');
}

/**
 * @param {string} idValue
 * @param {string} labelText
 * @param {boolean} initial
 * @param {(next: boolean) => void} onChange
 * @returns {HTMLElement}
 */
function toggleControl(idValue, labelText, initial, onChange) {
  const wrap = el('label', { class: 'iframe-toggle', for: idValue });
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
  const wrap = el('fieldset', { class: 'iframe-radio' });
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
  const wrap = el('div', { class: 'iframe-accent' });
  const label = el('label', { for: idValue }, ['accent']);
  const input = /** @type {HTMLInputElement} */ (
    el('input', { type: 'color', id: idValue })
  );
  // Color inputs need a 7-char #rrggbb. Default to a neutral when unset.
  input.value = initial ? `#${initial}` : '#2962ff';
  input.addEventListener('input', () => {
    onChange(input.value.replace(/^#/, '').toLowerCase());
  });
  const clear = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'iframe-accent__clear' }, ['clear'])
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
  const wrap = el('div', { class: 'iframe-select' });
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
 * Mount the iframe demo into `rootEl`. Replaces existing children.
 *
 * @param {HTMLElement} rootEl
 * @param {{ kind: 'polst' | 'brand' | 'campaign', id: string }} target
 * @param {string} appOrigin
 */
export function mountIframeDemo(rootEl, target, appOrigin) {
  rootEl.innerHTML = '';
  rootEl.classList.add('iframe-demo');

  /** @type {State} */
  const state = { ...DEFAULT_STATE, tab: target.kind };

  // ---------- Tabs ----------
  const tabsRow = el('div', { class: 'iframe-tabs', role: 'tablist' });
  /** @type {Record<Tab, HTMLButtonElement>} */
  const tabButtons = /** @type {any} */ ({});
  /** @type {Tab[]} */
  const tabOrder = ['polst', 'campaign', 'brand'];
  for (const tab of tabOrder) {
    const btn = /** @type {HTMLButtonElement} */ (
      el('button', {
        type: 'button',
        class: 'iframe-tab',
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
  const grid = el('div', { class: 'iframe-grid' });

  const previewCol = el('div', { class: 'iframe-preview' });
  const iframe = /** @type {HTMLIFrameElement} */ (
    el('iframe', {
      class: 'iframe-preview__frame',
      title: 'Polst embed preview',
      loading: 'lazy',
      referrerpolicy: 'strict-origin-when-cross-origin',
      allow: 'clipboard-write',
    })
  );
  previewCol.append(iframe);

  const sideCol = el('div', { class: 'iframe-side' });

  // ---------- Controls (per tab) ----------
  // Each tab gets its own controls panel. Switching tabs hides the
  // others but keeps DOM mounted so state-bound inputs survive.
  /** @type {Record<Tab, HTMLElement>} */
  const controlPanels = /** @type {any} */ ({});

  // Each tab needs its own DOM (different ids on the radio groups etc.).
  // Build them per tab for simplicity.
  for (const tab of tabOrder) {
    const panel = el('div', { class: 'iframe-controls', 'data-tab': tab });
    const heading = el('h2', { class: 'iframe-controls__heading' }, [
      `${TAB_LABELS[tab]} controls`,
    ]);
    panel.append(heading);

    // Common controls.
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
   * Tab-scoped factory so the radio/input ids are unique across panels.
   * @param {Tab} tab
   * @returns {HTMLElement}
   */
  function commonControlsForTab(tab) {
    const wrap = el('div', { class: 'iframe-controls__common' });
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
      // Theme radios.
      const themeRadios = panel.querySelectorAll(
        `input[type="radio"][name="${tab}-theme"]`,
      );
      themeRadios.forEach((r) => {
        const radio = /** @type {HTMLInputElement} */ (r);
        radio.checked = radio.value === state.theme;
      });
      // Accent color input.
      const accentInput = /** @type {HTMLInputElement | null} */ (
        panel.querySelector(`input[type="color"]#${tab}-accent`)
      );
      if (accentInput) {
        accentInput.value = state.accent ? `#${state.accent}` : '#2962ff';
      }
      // Toggles.
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
  const snippetPanel = el('div', { class: 'iframe-snippet' });
  const snippetHeader = el('div', { class: 'iframe-snippet__header' });
  snippetHeader.append(el('h2', null, ['Snippet']));
  const copyBtn = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'iframe-snippet__copy' }, ['Copy'])
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
      // Silent. POL-POLISH-COPY introduces the shared helper with
      // proper fallback (textarea + execCommand). For now we just
      // surface no UX change on failure.
    }
  });
  snippetHeader.append(copyBtn);

  const pre = el('pre', { class: 'iframe-snippet__pre' });
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
      tabButtons[tab].classList.toggle('iframe-tab--active', isActive);
      tabButtons[tab].setAttribute('aria-selected', String(isActive));
      controlPanels[tab].hidden = !isActive;
    }
  }

  function update() {
    const src = buildSrc(state, appOrigin, target);
    iframe.src = src;
    code.textContent = buildSnippet(src);
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
  const appOrigin = getAppOrigin(getEnv());
  mountIframeDemo(demoEl, target, appOrigin);
}
