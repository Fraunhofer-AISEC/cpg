<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';

  interface Props {
    title: string;
    nodes: FlattenedNode[];
    highlightedNode: NodeJSON | null;
    nodeClick: (node: NodeJSON) => void;
  }

  let { title, nodes, highlightedNode = $bindable(), nodeClick }: Props = $props();
</script>

<div class="rounded bg-white p-6 shadow-md">
  <h2 class="mb-4 text-lg font-semibold">
    {title} ({nodes.length})
  </h2>
  <div class="overflow-x-auto">
    <table class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50">
        <tr>
          <th
            class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
          >
            Type
          </th>
          <th
            class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
          >
            Name
          </th>
          <th
            class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
          >
            Location
          </th>
          <th
            class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
          >
            Code
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-200 bg-white">
        {#each nodes as node (node.id)}
          <tr
            class={`${
              highlightedNode?.id === node.id ? 'bg-gray-100' : ''
            } cursor-pointer hover:bg-gray-50`}
            onmouseenter={() => (highlightedNode = node)}
            onmouseleave={() => (highlightedNode = null)}
            onclick={() => nodeClick(node)}
          >
            <td
              class="px-6 py-4 text-sm font-medium whitespace-nowrap text-gray-900"
              style:padding-left={`${node.depth * 10}px`}
            >
              {node.type}
            </td>
            <td class="px-6 py-4 text-sm whitespace-nowrap text-gray-500">
              {node.name}
            </td>
            <td class="px-6 py-4 text-sm whitespace-nowrap text-gray-500">
              L{node.startLine}:C{node.startColumn} - L{node.endLine}:C
              {node.endColumn}
            </td>
            <td class="max-w-xs truncate px-6 py-4 text-sm whitespace-nowrap text-gray-500">
              {node.code}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>
