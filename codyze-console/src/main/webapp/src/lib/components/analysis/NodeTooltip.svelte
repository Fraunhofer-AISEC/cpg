<script lang="ts">
  import { type NodeJSON } from '$lib/types';

  interface Props {
    node: NodeJSON;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
  }

  let { node, lineHeight, charWidth, offsetTop, offsetLeft }: Props = $props();

  let top = $derived(`${node.endLine * lineHeight + offsetTop}rem`);
  let left = $derived(`${node.startColumn * charWidth + offsetLeft}rem`);
</script>

<div
  class="absolute z-30 rounded border border-gray-300 bg-white p-2 text-xs shadow-md"
  style:top
  style:left
  style:max-width="18.75em"
>
  <p class="font-bold">{node.type}</p>
  <p>
    <strong>Name:</strong>
    {node.name}
  </p>
  <p>
    <strong>Location:</strong> L{node.startLine}:C{node.startColumn} - L
    {node.endLine}:C{node.endColumn}
  </p>
  <p>
    <strong>PrevDFG:</strong>
    {JSON.stringify(node.prevDFG)}
  </p>
  <p class="mt-1 truncate">
    <strong>Code:</strong>
    {node.code}
  </p>
</div>
