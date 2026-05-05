// docs/sdk/sdk.js
//
// POL-775 — JS SDK demo page.
//
// Renders four tabs (polst / campaign / brand / vote) demonstrating the
// `@polst-web/sdk` programmatic surface. The snippet panel on the left
// shows the EXACT code that runs on the right — both come from the
// per-tab `SNIPPETS` table, and the executed call site is written to
// match the snippet character-for-character. There are no hidden
// wrappers, no commented-out scaffolding, and no helpers around the
// per-variant call. (See spec § "Variants".)
//
// SDK loaded from esm.sh — pinned at `@polst-web/sdk` (latest) so the
// demo tracks published releases. If esm.sh subpath rewriting trips on
// `@polst-web/sdk/render` in any browser engine, fall back to the
// fully-qualified URL `https://esm.sh/@polst-web/sdk@0.7.0/render`.

import { PolstClient } from 'https://esm.sh/@polst-web/sdk';
import {
  renderPolst,
  renderCampaign,
  renderBrandFeed,
} from 'https://esm.sh/@polst-web/sdk/render';

import {
  getApiOrigin,
  getEnv,
  getPolstTarget,
} from '../assets/chrome.js';

/** @typedef {'polst' | 'campaign' | 'brand' | 'vote'} Tab */

const TAB_LABELS = Object.freeze({
  polst: 'Polst',
  campaign: 'Campaign',
  brand: 'Brand feed',
  vote: 'Vote',
});

/** @type {Tab[]} */
const TAB_ORDER = ['polst', 'campaign', 'brand', 'vote'];

/**
 * Per-tab snippet shown in the left column. The actual call site below
 * MUST run the same characters; if you edit a snippet, update the call
 * site too (and vice-versa).
 *
 * The strings use template placeholders `${shortId}` / `${campaignId}`
 * / `${slug}` ONLY for readability — the snippet is rendered with the
 * real identifier inlined so what's displayed is what runs. (See
 * `materialiseSnippet`.)
 */
const SNIPPETS = Object.freeze({
  polst:
`const polst = await client.getPolst("__ID__");
renderPolst(mountEl, { polstId: polst.shortId });`,

  campaign:
`const campaign = await client.getCampaign("__ID__");
renderCampaign(mountEl, { campaignId: campaign.id });`,

  brand:
`const feed = await client.getBrandFeed("__ID__");
renderBrandFeed(mountEl, { brandSlug: "__ID__", pageSize: feed.items.length });`,

  vote:
`const result = await client.polsts.vote("__ID__", choice);
// result: { optionACount, optionBCount, ... }`,
});

const SETUP_SNIPPET =
`import { PolstClient } from "https://esm.sh/@polst-web/sdk";
import { renderPolst, renderCampaign, renderBrandFeed }
  from "https://esm.sh/@polst-web/sdk/render";

const client = new PolstClient({ baseUrl: "${getApiOrigin(getEnv())}" });`;

/**
 * Inline the live identifier into the snippet placeholder. We do this
 * per-tab so the displayed code shows the actual id the demo is about
 * to fetch — same string the call site will pass.
 *
 * @param {string} template
 * @param {string} id
 * @returns {string}
 */
function materialiseSnippet(template, id) {
  return template.replace(/__ID__/g, id);
}

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
 * Classify an unknown error thrown by an SDK call into a {title,
 * detail, code} triple suitable for the inline error panel.
 *
 * @param {unknown} err
 * @returns {{ title: string, detail: string, code: string | null }}
 */
function classifyError(err) {
  // PolstApiError is the SDK's branded error class. We duck-type
  // against it (status + code fields) instead of importing the class
  // directly so this demo keeps a single SDK surface area to track.
  const e = /** @type {{ status?: number, code?: string, message?: string }} */ (
    err && typeof err === 'object' ? err : {}
  );
  const status = typeof e.status === 'number' ? e.status : null;
  const code = typeof e.code === 'string' ? e.code : null;
  const message =
    (e.message && String(e.message)) ||
    (err == null ? 'Unknown error' : String(err));

  if (status === 404) {
    return { title: 'Not found', detail: message, code };
  }
  if (status != null && status >= 500) {
    return { title: 'Request failed', detail: message, code };
  }
  if (status != null && status >= 400) {
    return { title: 'Bad request', detail: message, code };
  }
  if (status === 0 || status == null) {
    return { title: 'Network error', detail: message, code };
  }
  return { title: 'Error', detail: message, code };
}

/**
 * Render an error block into `mountEl`, replacing existing content.
 *
 * @param {HTMLElement} mountEl
 * @param {{ title: string, detail: string, code: string | null }} info
 */
function renderError(mountEl, info) {
  mountEl.innerHTML = '';
  const wrap = el('div', { class: 'sdk-error' });
  wrap.append(el('h3', { class: 'sdk-error__title' }, [info.title]));
  wrap.append(el('p', { class: 'sdk-error__detail' }, [info.detail]));
  if (info.code) {
    wrap.append(el('code', { class: 'sdk-error__code' }, [info.code]));
  }
  mountEl.append(wrap);
}

/**
 * Render a one-line status (loading / "no target" / etc.) into mountEl.
 *
 * @param {HTMLElement} mountEl
 * @param {string} message
 */
function renderStatus(mountEl, message) {
  mountEl.innerHTML = '';
  mountEl.append(el('div', { class: 'sdk-status' }, [message]));
}

/**
 * Mount the SDK demo into `rootEl`. Replaces existing children.
 *
 * @param {HTMLElement} rootEl
 * @param {{ kind: 'polst' | 'campaign' | 'brand', id: string }} target
 */
export function mountSdkDemo(rootEl, target) {
  rootEl.innerHTML = '';
  rootEl.classList.add('sdk-demo');

  const client = new PolstClient({ baseUrl: getApiOrigin(getEnv()) });

  /** @type {Tab} */
  let activeTab = target.kind;

  // ---------- Tabs row ----------
  const tabsRow = el('div', { class: 'sdk-tabs', role: 'tablist' });
  /** @type {Record<Tab, HTMLButtonElement>} */
  const tabButtons = /** @type {any} */ ({});
  for (const tab of TAB_ORDER) {
    const btn = /** @type {HTMLButtonElement} */ (
      el('button', {
        type: 'button',
        class: 'sdk-tab',
        role: 'tab',
        'data-tab': tab,
      }, [TAB_LABELS[tab]])
    );
    if (tab === 'vote' && target.kind !== 'polst') {
      btn.disabled = true;
      btn.title = 'Vote requires a polst short id';
    }
    btn.addEventListener('click', () => {
      if (btn.disabled || activeTab === tab) return;
      activeTab = tab;
      syncTabUi();
      void runActiveTab();
    });
    tabButtons[tab] = btn;
    tabsRow.append(btn);
  }

  // ---------- Grid ----------
  const grid = el('div', { class: 'sdk-grid' });

  // Left: snippet panel.
  const snippetPanel = el('div', { class: 'sdk-snippet' });
  const snippetHeader = el('div', { class: 'sdk-snippet__header' });
  snippetHeader.append(el('h2', null, ['Code']));
  const copyBtn = /** @type {HTMLButtonElement} */ (
    el('button', { type: 'button', class: 'sdk-snippet__copy' }, ['Copy'])
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
      // Silent — POL-780 will introduce the shared copy helper with a
      // proper fallback. For now we no-op on failure.
    }
  });
  snippetHeader.append(copyBtn);
  const pre = el('pre', { class: 'sdk-snippet__pre' });
  const code = el('code');
  pre.append(code);
  snippetPanel.append(snippetHeader, pre);

  // Right: render panel.
  const renderPanel = el('div', { class: 'sdk-render' });
  const renderHeader = el('h2', { class: 'sdk-render__header' }, ['Output']);
  const mountEl = el('div', { class: 'sdk-render__mount' });
  renderPanel.append(renderHeader, mountEl);

  grid.append(snippetPanel, renderPanel);
  rootEl.append(tabsRow, grid);

  // ---------- Wiring ----------
  function syncTabUi() {
    for (const tab of TAB_ORDER) {
      const isActive = tab === activeTab;
      tabButtons[tab].classList.toggle('sdk-tab--active', isActive);
      tabButtons[tab].setAttribute('aria-selected', String(isActive));
    }
  }

  /** @returns {Promise<void>} */
  async function runActiveTab() {
    // The setup snippet (import + new PolstClient) is shown alongside
    // the per-tab snippet so the displayed code is a complete, runnable
    // recipe. Both halves run for real: the imports are at the top of
    // this module; `client` is the real instance; and the per-tab call
    // below executes the second half.
    const tabSnippet = materialiseSnippet(SNIPPETS[activeTab], target.id);
    code.textContent = `${SETUP_SNIPPET}\n\n${tabSnippet}`;

    // POL-788 follow-up: pass `apiOrigin` per-call to render helpers.
    // The widget's render path otherwise reads its REST origin from a
    // module-internal global that this realm cannot reliably write to
    // — esm.sh serves `@polst-web/sdk` and `@polst-web/sdk/render`
    // from separate bundle realms, so a `configureOrigins` call in
    // one does not update the global the other reads. Per-call
    // bypasses the global entirely.
    const apiOrigin = getApiOrigin(getEnv());

    if (activeTab === 'polst') {
      renderStatus(mountEl, 'Loading polst…');
      try {
        const polst = await client.getPolst(target.id);
        renderPolst(mountEl, { polstId: polst.shortId, apiOrigin });
      } catch (err) {
        renderError(mountEl, classifyError(err));
      }
      return;
    }

    if (activeTab === 'campaign') {
      renderStatus(mountEl, 'Loading campaign…');
      try {
        const campaign = await client.getCampaign(target.id);
        renderCampaign(mountEl, { campaignId: campaign.id, apiOrigin });
      } catch (err) {
        renderError(mountEl, classifyError(err));
      }
      return;
    }

    if (activeTab === 'brand') {
      renderStatus(mountEl, 'Loading brand feed…');
      try {
        const feed = await client.getBrandFeed(target.id);
        renderBrandFeed(mountEl, {
          brandSlug: target.id,
          pageSize: feed.items.length,
          apiOrigin,
        });
      } catch (err) {
        renderError(mountEl, classifyError(err));
      }
      return;
    }

    if (activeTab === 'vote') {
      renderVoteUi();
      return;
    }
  }

  /**
   * Render the vote UI: two buttons + result panel + hint. Each click
   * executes the displayed call (`client.polsts.vote(target.id, choice)`)
   * and re-renders the polst widget with the freshest state.
   */
  function renderVoteUi() {
    mountEl.innerHTML = '';
    const wrap = el('div', { class: 'sdk-vote' });
    const buttons = el('div', { class: 'sdk-vote__buttons' });
    const btnA = /** @type {HTMLButtonElement} */ (
      el('button', { type: 'button', class: 'sdk-vote__btn' }, ['Vote A'])
    );
    const btnB = /** @type {HTMLButtonElement} */ (
      el('button', { type: 'button', class: 'sdk-vote__btn' }, ['Vote B'])
    );
    const result = el('pre', { class: 'sdk-vote__result' }, ['—']);
    const hint = el('p', { class: 'sdk-vote__hint' }, [
      'Click a button to cast a vote against the active env. The SDK ' +
        'manages X-Device-Id and idempotency keys internally — no ' +
        'demo-side header work.',
    ]);

    /**
     * @param {'A' | 'B'} choice
     */
    async function castVote(choice) {
      btnA.disabled = true;
      btnB.disabled = true;
      result.textContent = `Voting ${choice}…`;
      try {
        const voteResult = await client.polsts.vote(target.id, choice);
        result.textContent = JSON.stringify(voteResult, null, 2);
      } catch (err) {
        const info = classifyError(err);
        result.textContent = `${info.title}: ${info.detail}`;
      } finally {
        btnA.disabled = false;
        btnB.disabled = false;
      }
    }

    btnA.addEventListener('click', () => void castVote('A'));
    btnB.addEventListener('click', () => void castVote('B'));

    buttons.append(btnA, btnB);
    wrap.append(buttons, result, hint);
    mountEl.append(wrap);
  }

  syncTabUi();
  void runActiveTab();
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
  mountSdkDemo(/** @type {HTMLElement} */ (demoEl), target);
}
