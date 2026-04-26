<script lang="ts">
  import CodeItemList, { getItemsArray, isCodeItemContent } from './CodeItemList.svelte';
  import DfgFlowWidget from './DfgFlowWidget.svelte';
  import type { ToolResult } from '$lib/types';

  function extractSkillName(content: any): string | null {
    const text = typeof content === 'string'
      ? content
      : Array.isArray(content) && typeof content[0]?.text === 'string'
        ? content[0].text
        : null;
    if (!text) return null;
    const match = text.match(/<skill_content\s+name="([^"]+)"/);
    return match ? match[1] : null;
  }

  function getResultSummary(toolName: string, content: any, isError?: boolean): string {
    if (isError) return 'Error';
    if (toolName === 'activate_skill') {
      const name = extractSkillName(content);
      if (name) return name;
    }
    const items = getItemsArray(content);
    if (items.length > 0) {
      return `${items.length} result${items.length !== 1 ? 's' : ''}`;
    }
    if (typeof content === 'string') {
      const trimmed = content.trim();
      if (trimmed.length === 0) return 'Done';
      if (trimmed.length <= 60) return trimmed;
      return trimmed.slice(0, 57) + '...';
    }
    return 'Done';
  }

  let {
    toolResult,
    onItemClick,
  }: {
    toolResult: ToolResult;
    onItemClick?: (item: any) => void;
  } = $props();

  let expanded = $state(false);

  let toolName = $derived(toolResult.toolName || 'Tool');
  let summary = $derived(getResultSummary(toolName, toolResult.content, toolResult.isError));
  let isDfg = $derived(toolResult.toolName === 'cpg_dfg_backward');
  let isCodeItems = $derived(isCodeItemContent(toolResult.content));
  let hasExpandableContent = $derived(isDfg || isCodeItems || typeof toolResult.content === 'string' || toolResult.content != null);
</script>

<div class="my-1">
  <button
    class="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-[13px] transition-colors
      {toolResult.isError
        ? 'cursor-pointer text-red-500 hover:bg-red-50 hover:text-red-600'
        : 'cursor-pointer text-gray-500 hover:bg-gray-100 hover:text-gray-700'}
      {expanded ? 'bg-gray-50 text-gray-700' : ''}"
    onclick={() => expanded = !expanded}
    type="button"
  >
    <span class="font-mono font-medium">{toolName}</span>
    <span class="text-gray-400">{summary}</span>

    {#if hasExpandableContent}
      <svg
        class="h-3 w-3 shrink-0 text-gray-300 transition-transform duration-200 {expanded ? 'rotate-180' : ''}"
        fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
      </svg>
    {/if}
  </button>

  {#if expanded}
    <div class="ml-5 mt-1 border-l-2 pl-3 pb-1 {toolResult.isError ? 'border-red-200' : 'border-gray-200'}">
      {#if isDfg}
        <DfgFlowWidget content={toolResult.content} />
      {:else if isCodeItems}
        <CodeItemList data={{ toolName: toolResult.toolName, content: toolResult.content, isError: toolResult.isError }} {onItemClick} />
      {:else}
        <pre class="m-0 max-h-96 overflow-auto whitespace-pre-wrap break-words rounded bg-gray-50 p-2 font-mono text-xs leading-relaxed text-gray-600">{typeof toolResult.content === 'string' ? toolResult.content : JSON.stringify(toolResult.content, null, 2)}</pre>
      {/if}
    </div>
  {/if}
</div>