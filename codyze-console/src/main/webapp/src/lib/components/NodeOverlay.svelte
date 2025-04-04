<script lang="ts">
  import { getColorForNodeType } from '$lib/colors';
  import type { ConceptGroup } from '$lib/concepts';
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';
  import type { MouseEventHandler } from 'svelte/elements';

  interface Props {
    node: FlattenedNode;
    codeLines: string[];
    highlightedNode: NodeJSON | null;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    onNodeClick: (node: FlattenedNode) => void;
  }

  let {
    node,
    codeLines,
    highlightedNode = $bindable(),
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    onNodeClick
  }: Props = $props();

  /**
   * Calculate the width of the {@link node} in rem units.
   *
   * There are two cases to consider:
   * - The node spans multiple lines. In this case, the width is the length of the longest line
   *   that the node spans.
   * - The node spans a single line. In this case, the width is the difference between the end
   *   and start columns.
   *
   * @param node the node to calculate the width for
   * @param codeLines the lines of code that the node spans
   */
  function computeWidth(node: NodeJSON, codeLines: string[]): number {
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

  let top = $derived(`${(node.startLine - 1) * lineHeight + offsetTop}rem`);
  let left = $derived(`${node.startColumn * charWidth + offsetLeft}rem`);
  let height = $derived(`${(node.endLine - node.startLine + 1) * lineHeight}rem`);
  let width = $derived(`${computeWidth(node, codeLines)}rem`);
</script>

<div
  role="presentation"
  class="absolute cursor-pointer transition-all duration-200"
  style:top
  style:left
  style:height
  style:width
  style:background-color={getColorForNodeType(node.type)}
  style:border={highlightedNode?.id === node.id
    ? '0.125em solid black'
    : '0.0625em solid transparent'}
  style:opacity={highlightedNode?.id === node.id ? 0.6 : 0.3}
  style:min-width="0.25em"
  style:min-height="0.25em"
  style:z-index={node.depth}
  onmouseenter={() => (highlightedNode = node)}
  onmouseleave={() => (highlightedNode = null)}
  onclick={() => onNodeClick(node)}
></div>
