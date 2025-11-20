<script lang="ts">
  import { marked } from 'marked';
  import type { Tokens } from 'marked';

  interface Props {
    content: string;
  }

  let { content }: Props = $props();

  // Configure marked options for better rendering
  marked.setOptions({
    gfm: true, // GitHub Flavored Markdown
    breaks: true // Convert \n to <br>
  });

  // Custom renderer for code blocks with syntax highlighting
  const renderer = new marked.Renderer();

  renderer.code = function({ text, lang }: Tokens.Code): string {
    const language = lang || 'plaintext';
    return `<pre><code class="hljs language-${language}">${text}</code></pre>`;
  };

  marked.use({ renderer });

  // Parse markdown content
  let html = $derived(marked.parse(content) as string);
</script>

<div class="prose prose-sm prose-gray max-w-none">
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
</style>
