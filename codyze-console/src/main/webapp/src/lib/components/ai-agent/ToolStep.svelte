<script lang="ts">
  import type { NodePathsJSON, NodeRefJSON, ToolResult } from '$lib/types';

  /**
   * A tool call of the agent as a numbered step of its evidence trail: the tool, a short summary,
   * and, expanded, the nodes it returned as rows like in the inspector. Clicking a row shows the
   * node in the code; the raw result is behind a toggle. Pointing at the step makes its nodes stand
   * out in the code, clicking its number steps through them.
   */
  interface Props {
    toolResult: ToolResult;
    step: number;
    /** The nodes the tool returned, resolved to their locations */
    nodes: NodeRefJSON[];
    /** The paths the tool returned, if it searched for paths */
    paths?: NodePathsJSON | null;
    /** The inspected node, which is highlighted among the rows */
    selectedId?: string | null;
    onSelect: (ref: NodeRefJSON) => void;
    /** Shows the step in the code and goes to its first node */
    onActivate: () => void;
    onHover: (hovered: boolean) => void;
  }

  let { toolResult, step, nodes, paths, selectedId, onSelect, onActivate, onHover }: Props =
    $props();

  let expanded = $state(false);
  let showAll = $state(false);
  let showRaw = $state(false);
  const preview = 8;

  const toolName = $derived(toolResult.toolName || 'tool');

  function extractSkillName(content: unknown): string | null {
    const text =
      typeof content === 'string'
        ? content
        : Array.isArray(content) && typeof content[0]?.text === 'string'
          ? content[0].text
          : null;
    return text?.match(/<skill_content\s+name="([^"]+)"/)?.[1] ?? null;
  }

  // What the step found, e.g. "3 nodes in 2 files", or the start of a text result
  const summary = $derived.by(() => {
    const content = toolResult.content;
    if (toolResult.isError) return 'error';
    // The name of an activated skill, or else the start of the result like for other tools
    const skill = toolName === 'activate_skill' ? extractSkillName(content) : null;
    if (skill) return skill;
    if (toolName === 'cpg_suggest_llm_concepts_and_operations' && typeof content?.name === 'string')
      return `suggests ${content.name}`;
    if (paths) {
      const count = `${paths.paths.length}${paths.truncated ? '+' : ''}`;
      return `${paths.kind} · ${count} ${paths.paths.length === 1 ? 'path' : 'paths'}`;
    }
    if (nodes.length > 0) {
      const files = new Set(nodes.map((n) => n.fileName).filter(Boolean)).size;
      const what = `${nodes.length} ${nodes.length === 1 ? 'node' : 'nodes'}`;
      return files > 1 ? `${what} in ${files} files` : what;
    }
    if (typeof content === 'string') {
      const text = content.trim();
      if (!text) return 'done';
      return text.length <= 60 ? text : text.slice(0, 57) + '…';
    }
    return 'done';
  });

  const raw = $derived(
    typeof toolResult.content === 'string'
      ? toolResult.content
      : JSON.stringify(toolResult.content, null, 2)
  );

  const shownNodes = $derived(showAll ? nodes : nodes.slice(0, preview));

  function location(ref: NodeRefJSON): string {
    return ref.startLine >= 1 ? `${ref.fileName ?? ''}:${ref.startLine}` : 'no location';
  }
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div class="min-w-0" onmouseenter={() => onHover(true)} onmouseleave={() => onHover(false)}>
  <div class="flex min-w-0 items-center gap-1.5">
    <button
      type="button"
      class="flex h-4 min-w-4 shrink-0 items-center justify-center rounded-sm px-0.5 text-[10px] leading-none font-semibold tabular-nums {nodes.length
        ? 'bg-slate-700 text-white hover:bg-slate-900'
        : 'bg-slate-200 text-slate-600'}"
      title={paths
        ? `Step ${step}: show its ${paths.paths.length === 1 ? 'path' : 'paths'} in the code`
        : nodes.length
          ? `Step ${step}: show its nodes in the code`
          : `Step ${step} returned no nodes of the code`}
      disabled={nodes.length === 0}
      onclick={onActivate}
    >
      {step}
    </button>
    <button
      type="button"
      class="flex min-w-0 flex-1 items-center gap-1.5 rounded px-1 py-1 text-left text-xs hover:bg-gray-50 {toolResult.isError
        ? 'text-red-600'
        : 'text-gray-700'}"
      aria-expanded={expanded}
      onclick={() => (expanded = !expanded)}
    >
      <span class="shrink-0 font-mono">{toolName}</span>
      <span class="min-w-0 truncate text-gray-400" title={summary}>{summary}</span>
      <svg
        class="ml-auto h-3 w-3 shrink-0 text-gray-300 transition-transform {expanded
          ? 'rotate-180'
          : ''}"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    </button>
  </div>

  {#if expanded}
    <div class="mt-0.5 mb-1 ml-[1.375rem] border-l border-gray-200 pl-2">
      {#each shownNodes as ref (ref.id)}
        <button
          type="button"
          class="flex w-full min-w-0 items-baseline gap-2 rounded px-1 py-0.5 text-left hover:bg-gray-50 {ref.id ===
          selectedId
            ? 'bg-blue-50'
            : ''}"
          title="{ref.type} {ref.code}. Click to show it in the code."
          onclick={() => onSelect(ref)}
        >
          <span class="min-w-0 flex-1 truncate font-mono text-[11px] text-gray-800"
            >{ref.code || ref.name}</span
          >
          <span class="shrink-0 font-mono text-[10px] text-gray-400">{location(ref)}</span>
        </button>
      {/each}
      <div class="flex flex-wrap items-center gap-2 px-1 pt-0.5 text-[11px]">
        {#if nodes.length > preview}
          <button
            type="button"
            class="text-gray-500 hover:text-gray-900"
            onclick={() => (showAll = !showAll)}
          >
            {showAll ? 'Show less' : `Show all ${nodes.length}`}
          </button>
        {/if}
        <button
          type="button"
          class="text-gray-400 hover:text-gray-700"
          aria-expanded={showRaw}
          onclick={() => (showRaw = !showRaw)}
        >
          raw {showRaw ? '▾' : '▸'}
        </button>
      </div>
      {#if showRaw}
        <pre
          class="m-0 mt-1 max-h-72 overflow-auto rounded bg-gray-50 p-2 font-mono text-[11px] leading-relaxed break-words whitespace-pre-wrap text-gray-600">{raw}</pre>
      {/if}
    </div>
  {/if}
</div>
