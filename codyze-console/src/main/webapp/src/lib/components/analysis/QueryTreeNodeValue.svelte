<script lang="ts">
  import type { NodeJSON } from '$lib/types';
  import { getNodeLocation } from '$lib/types';

  interface Props {
    node: NodeJSON;
    baseUrl?: string;
    queryTreeId?: string;
  }

  let { node, baseUrl, queryTreeId }: Props = $props();
</script>

<div class="bg-opacity-50 rounded bg-white p-2">
  <div class="flex items-center justify-between">
    <span class="font-mono text-xs text-gray-600">{node.type}</span>
    <div class="flex items-center space-x-1">
      <span class="text-xs">📍</span>
      {#if getNodeLocation(node, baseUrl, queryTreeId)}
        <a
          href={getNodeLocation(node, baseUrl, queryTreeId)}
          class="cursor-pointer font-mono text-xs text-blue-600 hover:text-blue-800 hover:underline"
          title="Click to view in source code"
        >
          {#if node.fileName}
            {node.fileName}:{node.startLine}:{node.startColumn}
          {:else}
            {node.startLine}:{node.startColumn}
          {/if}
        </a>
      {:else}
        <span class="font-mono text-xs text-gray-500">
          {#if node.fileName}
            {node.fileName}:{node.startLine}:{node.startColumn}
          {:else}
            {node.startLine}:{node.startColumn}
          {/if}
        </span>
      {/if}
    </div>
  </div>
  <div class="mt-1 font-mono text-xs">
    <span class="font-medium text-gray-900">{node.name}</span>
  </div>
  {#if node.code}
    <div class="mt-1 rounded bg-gray-100 px-1 py-0.5 font-mono text-xs text-gray-700">
      {node.code.trim()}
    </div>
  {/if}
</div>
