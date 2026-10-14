<script lang="ts">
  import { marked } from 'marked';
  import type { Tokens } from 'marked';
  import type { NodeRefJSON } from '$lib/types';
  import { citationName, citationPattern } from '$lib/agentEvidence';

  interface Props {
    content: string;
    /** The cited nodes by ID, to label the citations `[[node:<id>]]` in the content */
    citations?: Map<string, NodeRefJSON>;
    /** Called when a citation is clicked; without it, citations are shown as plain text */
    onCite?: (nodeId: string) => void;
    /** Called when the mouse enters a citation, with its chip, and with `null` when it leaves it */
    onCiteHover?: (nodeId: string | null, chip: HTMLElement | null) => void;
  }

  let { content, citations, onCite, onCiteHover }: Props = $props();

  const escapeHtml = (text: string) => text.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

  // The icon of a citation: angle brackets, for a place in the code
  const codeIcon =
    '<svg class="node-cite-icon" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5.5 4.5 2 8l3.5 3.5M10.5 4.5 14 8l-3.5 3.5"/></svg>';

  // Replaces the citations of nodes with chips, which are labeled once the nodes are resolved
  function renderCitations(text: string): string {
    if (!onCite) return text;
    return text.replace(citationPattern, (_, id: string) => {
      const ref = citations?.get(id.toLowerCase());
      const name = ref ? citationName(ref) : `node …${id.slice(-6)}`;
      const where = ref && ref.startLine >= 1 ? `${ref.fileName ?? ''}:${ref.startLine}` : '';
      const title = ref ? `${ref.type} ${ref.code}` : 'Cited node';
      return (
        `<button type="button" class="node-cite" data-node-id="${id.toLowerCase()}" title="${escapeHtml(title)}">` +
        codeIcon +
        `<span class="node-cite-name">${escapeHtml(name)}</span>` +
        (where ? `<span class="node-cite-where">${escapeHtml(where)}</span>` : '') +
        `</button>`
      );
    });
  }

  // The citation under the mouse, so that leaving it is reported once
  let hoveredCite: HTMLElement | null = null;

  function handleMouseOver(event: MouseEvent) {
    const cite = (event.target as HTMLElement).closest<HTMLElement>('[data-node-id]');
    if (cite === hoveredCite) return;
    hoveredCite = cite;
    onCiteHover?.(cite?.dataset.nodeId ?? null, cite);
  }

  function handleMouseLeave() {
    if (!hoveredCite) return;
    hoveredCite = null;
    onCiteHover?.(null, null);
  }

  function handleClick(event: MouseEvent) {
    const cite = (event.target as HTMLElement).closest<HTMLElement>('[data-node-id]');
    if (cite?.dataset.nodeId) onCite?.(cite.dataset.nodeId);
  }

  const renderer = new marked.Renderer();
  renderer.code = function ({ text, lang }: Tokens.Code): string {
    const language = lang || 'plaintext';
    // The code is text, e.g. `#include <stdio.h>` must not become a tag
    return `<pre><code class="hljs language-${escapeHtml(language)}">${escapeHtml(text)}</code></pre>`;
  };

  marked.use({
    gfm: true, // GitHub Flavored Markdown
    breaks: true, // Convert \n to <br>
    renderer
  });

  let html = $derived(marked.parse(renderCitations(content)) as string);
</script>

<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions, a11y_mouse_events_have_key_events -->
<div
  class="prose prose-sm prose-gray max-w-none min-w-0"
  onclick={handleClick}
  onmouseover={handleMouseOver}
  onmouseleave={handleMouseLeave}
>
  {@html html}
</div>

<style>
  /* Long words (identifiers, paths, JSON) wrap instead of widening the column */
  :global(.prose) {
    color: rgb(17, 24, 39);
    overflow-wrap: anywhere;
  }

  :global(.prose p) {
    margin-bottom: 0.75rem;
    line-height: 1.6;
  }

  :global(.prose h1) {
    font-size: 1.5rem;
    font-weight: 700;
    margin-top: 1.5rem;
    margin-bottom: 0.75rem;
  }

  :global(.prose h2) {
    font-size: 1.25rem;
    font-weight: 600;
    margin-top: 1.25rem;
    margin-bottom: 0.5rem;
  }

  :global(.prose h3) {
    font-size: 1.125rem;
    font-weight: 600;
    margin-top: 1rem;
    margin-bottom: 0.5rem;
  }

  :global(.prose ul, .prose ol) {
    margin-top: 0.5rem;
    margin-bottom: 0.75rem;
    padding-left: 1.5rem;
  }

  :global(.prose li) {
    margin-bottom: 0.25rem;
  }

  :global(.prose code) {
    background-color: rgb(243, 244, 246);
    padding: 0.125rem 0.375rem;
    border-radius: 0.25rem;
    font-size: 0.875em;
    font-family: 'Noto Sans Mono', monospace;
    color: rgb(239, 68, 68);
  }

  :global(.prose pre) {
    background-color: rgb(31, 41, 55);
    color: rgb(229, 231, 235);
    padding: 1rem;
    border-radius: 0.5rem;
    overflow-x: auto;
    margin-top: 0.75rem;
    margin-bottom: 0.75rem;
  }

  :global(.prose pre code) {
    background-color: transparent;
    padding: 0;
    color: inherit;
    font-size: 0.875rem;
  }

  :global(.prose blockquote) {
    border-left: 4px solid rgb(209, 213, 219);
    padding-left: 1rem;
    margin: 0.75rem 0;
    color: rgb(75, 85, 99);
    font-style: italic;
  }

  :global(.prose a) {
    color: rgb(37, 99, 235);
    text-decoration: underline;
  }

  :global(.prose a:hover) {
    color: rgb(29, 78, 216);
  }

  /* A wide table scrolls by itself, not the column it is in */
  :global(.prose table) {
    display: block;
    max-width: 100%;
    overflow-x: auto;
    border-collapse: collapse;
    margin: 0.75rem 0;
    overflow-wrap: normal;
  }

  :global(.prose th, .prose td) {
    border: 1px solid rgb(209, 213, 219);
    padding: 0.5rem;
    text-align: left;
  }

  :global(.prose th) {
    background-color: rgb(243, 244, 246);
    font-weight: 600;
  }

  :global(.prose hr) {
    border: none;
    border-top: 1px solid rgb(209, 213, 219);
    margin: 1.5rem 0;
  }

  :global(.prose strong) {
    font-weight: 600;
  }

  :global(.prose em) {
    font-style: italic;
  }

  /* A cited node, see renderCitations */
  :global(.prose .node-cite) {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    max-width: 100%;
    margin: 0 0.125rem;
    padding: 0 0.5rem 0 0.375rem;
    height: 1.25rem;
    border-radius: 9999px;
    background-color: rgb(241, 245, 249);
    color: rgb(51, 65, 85);
    font-size: 0.6875rem;
    line-height: 1;
    white-space: nowrap;
    vertical-align: -0.2rem;
    cursor: pointer;
    transition: background-color 120ms;
  }

  :global(.prose .node-cite:hover) {
    background-color: rgb(226, 232, 240);
  }

  :global(.prose .node-cite-icon) {
    width: 0.75rem;
    height: 0.75rem;
    flex-shrink: 0;
    color: rgb(100, 116, 139);
  }

  :global(.prose .node-cite-name) {
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: 'Noto Sans Mono', monospace;
    font-weight: 500;
  }

  :global(.prose .node-cite-where) {
    flex-shrink: 0;
    color: rgb(148, 163, 184);
  }
</style>
