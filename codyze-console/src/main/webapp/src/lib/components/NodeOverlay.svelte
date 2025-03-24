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

  let showDialog = $state(false);
  let availableConcepts = $state<string[]>([]);
  let selectedConcept = $state('');
  let dfgToConcept = $state(false);
  let dfgFromConcept = $state(false);

  async function handleClick() {
      const response = await fetch('/api/concepts');
      const data = await response.json();
      availableConcepts = data.concepts;
      showDialog = true;
  }

  async function handleSubmit() {
      if (!selectedConcept) {
          alert('Please select a concept');
          return;
      }

      try {
          const response = await fetch('/api/concept', {
              method: 'POST',
              headers: {
                  'Content-Type': 'application/json',
              },
              body: JSON.stringify({
                  nodeId: node.id,
                  conceptName: selectedConcept,
                  addDFGToConcept: dfgToConcept,
                  addDFGFromConcept: dfgFromConcept
              })
          });

          if (!response.ok) {
              const errorText = await response.text();
              throw new Error(`Server returned ${response.status}: ${errorText}`);
          }

          showDialog = false;
          window.location.reload();
      } catch (error) {
          console.error('Error adding concept:', error);
          alert('Failed to add concept: ' + error.message);
      }
  }
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
  onclick={handleClick}
></div>

{#if showDialog}
    <div
            class="fixed inset-0 z-50 flex items-center justify-center"
            onclick={() => (showDialog = false)}
    >
        <div class="rounded bg-white p-4 shadow-lg" onclick={(e) => e.stopPropagation()}>
            <h3 class="mb-4 text-lg font-bold">Add Concept</h3>
            <select
                class="mb-4 w-full rounded border p-2"
                bind:value={selectedConcept}
            >
                <option value="">Select a concept...</option>
                {#each availableConcepts as concept}
                    <option value={concept}>{concept}</option>
                {/each}
            </select>
            <div class="mb-4 space-y-2">
                <label class="flex items-center">
                    <input type="checkbox" bind:checked={dfgToConcept} class="mr-2" />
                    Connect DFG from this node to the new concept
                </label>
                <label class="flex items-center">
                    <input type="checkbox" bind:checked={dfgFromConcept} class="mr-2" />
                    Connect DFG from the new concept to this node
                </label>
            </div>
            <div class="flex justify-end space-x-2">
                <button
                        class="rounded bg-gray-300 px-4 py-2 hover:bg-gray-400"
                        onclick={() => (showDialog = false)}
                >
                    Cancel
                </button>
                <button
                        class="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
                        onclick={handleSubmit}
                >
                    Add
                </button>
            </div>
        </div>
    </div>
{/if}