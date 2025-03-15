<script lang="ts">
  import { getColorForNodeType } from '$lib/colors';
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';

  interface Props {
    node: FlattenedNode;
    codeLines: string[];
    highlightedNode: NodeJSON | null;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
  }

  let {
    node,
    codeLines,
    highlightedNode = $bindable(),
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft
  }: Props = $props();

  function calculateWidth(node: NodeJSON, codeLines: string[]): number {
    if (node.startLine === node.endLine) {
      return (node.endColumn - node.startColumn) * charWidth;
    } else {
      return (
        codeLines
          .slice(node.startLine - 1, node.endLine - 1)
          .reduce((maxWidth, line) => Math.max(maxWidth, line.length), 0) * charWidth
      );
    }
  }
</script>

<div
  role="presentation"
  class="absolute cursor-pointer transition-all duration-200"
  style:top={`${(node.startLine - 1) * lineHeight + offsetTop}px`}
  style:left={`${node.startColumn * charWidth + offsetLeft}px`}
  style:height={`${(node.endLine - node.startLine + 1) * lineHeight}px`}
  style:width={`${calculateWidth(node, codeLines)}px`}
  style:background-color={getColorForNodeType(node.type)}
  style:border={highlightedNode?.id === node.id ? '2px solid black' : '1px solid transparent'}
  style:opacity={highlightedNode?.id === node.id ? 0.6 : 0.3}
  style:minWidth={'4px'}
  style:minHeight={'4px'}
  style:z-index={node.depth}
  onmouseenter={() => (highlightedNode = node)}
  onmouseleave={() => (highlightedNode = null)}
></div>
