<script lang="ts">
  import { marked } from 'marked';
  import type { Tokens } from 'marked';
  import type { NodeRefJSON } from '$lib/types';
  import { citationLabel, citationPattern } from '$lib/agentEvidence';

  interface Props {
    content: string;
    /** The cited nodes by ID, to label the citations `[[node:<id>]]` in the content */
    citations?: Map<string, NodeRefJSON>;
    /** Called when a citation is clicked; without it, citations are shown as plain text */
    onCite?: (nodeId: string) => void;
  }

  let { content, citations, onCite }: Props = $props();

  const escapeHtml = (text: string) => text.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

  // Replaces the citations of nodes with chips, which are labeled once the nodes are resolved
  function renderCitations(text: string): string {
    if (!onCite) return text;
    return text.replace(citationPattern, (_, id: string) => {
      const ref = citations?.get(id.toLowerCase());
      const label = ref ? citationLabel(ref) : `node …${id.slice(-6)}`;
      const title = ref ? `${ref.type} ${ref.code}` : 'Cited node';
      return `<button type="button" class="node-cite" data-node-id="${id.toLowerCase()}" title="${escapeHtml(title)}">${escapeHtml(label)}</button>`;
    });
  }

  function handleClick(event: MouseEvent) {
    const cite = (event.target as HTMLElement).closest<HTMLElement>('[data-node-id]');
    if (cite?.dataset.nodeId) onCite?.(cite.dataset.nodeId);
  }

  const renderer = new marked.Renderer();
  renderer.code = function ({ text, lang }: Tokens.Code): string {
    const language = lang || 'plaintext';
    return `<pre><code class="hljs language-${language}">${text}</code></pre>`;
  };

  marked.use({
    gfm: true, // GitHub Flavored Markdown
    breaks: true, // Convert \n to <br>
    renderer
  });

  let html = $derived(marked.parse(renderCitations(content)) as string);
</script>

<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
<div class="prose prose-sm prose-gray max-w-none" onclick={handleClick}>
  {@html html}
</div>

<style>
  :global(.prose) {
    color: rgb(17, 24, 39);
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

  :global(.prose table) {
    width: 100%;
    border-collapse: collapse;
    margin: 0.75rem 0;
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
    display: inline-block;
    margin: 0 0.125rem;
    padding: 0 0.375rem;
    border: 1px solid rgb(209, 213, 219);
    border-radius: 0.25rem;
    background-color: rgb(249, 250, 251);
    color: rgb(55, 65, 81);
    font-family: 'Noto Sans Mono', monospace;
    font-size: 0.75rem;
    line-height: 1.25rem;
    white-space: nowrap;
    vertical-align: baseline;
    cursor: pointer;
  }

  :global(.prose .node-cite:hover) {
    border-color: rgb(37, 99, 235);
    color: rgb(29, 78, 216);
  }
</style>
