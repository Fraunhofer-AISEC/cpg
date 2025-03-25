<script lang="ts">
  import { invalidate } from '$app/navigation';
  import type { ConceptGroup } from '$lib/concepts';
  import type { FlattenedNode } from '$lib/flatten';

  interface Props {
    showDialog: boolean;
    node: FlattenedNode;
    conceptGroups: ConceptGroup[];
  }

  let { showDialog = $bindable(), node, conceptGroups }: Props = $props();

  let selectedConcept = $state('');
  let dfgToConcept = $state(false);
  let dfgFromConcept = $state(false);

  async function handleSubmit() {
    if (!selectedConcept) {
      alert('Please select a concept');
      return;
    }

    try {
      const response = await fetch('/api/concept', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
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
      invalidate(
        `/api/component/${node.componentName}/translation-unit/${node.unitId}/overlay-nodes`
      );
    } catch (error) {
      console.error('Error adding concept:', error);
      alert('Failed to add concept: ' + error.message);
    }
  }
</script>

<div
  role="presentation"
  class="fixed inset-0 z-50 flex items-center justify-center"
  onclick={() => (showDialog = false)}
  onkeydown={(e) => e.key === 'Escape' && (showDialog = false)}
>
  <div
    role="presentation"
    class="z-60 max-h-[80vh] overflow-auto rounded bg-white p-4 shadow-lg"
    onclick={(e) => e.stopPropagation()}
  >
    <h3 class="mb-4 text-lg font-bold">Add Concept</h3>
    To node: {node.name}
    <div class="mb-4 rounded border p-2">
      {#each conceptGroups as { path, concepts }}
        <div class="my-1 border-l-2 border-gray-200 pl-4">
          {#if path}
            <div class="text-sm font-medium text-gray-600">{path}</div>
          {/if}
          {#each concepts as concept}
            <label class="flex cursor-pointer items-center py-1 hover:bg-gray-50">
              <span class="mr-2 text-gray-400">└─</span>
              <input
                type="radio"
                name="concept"
                value={concept.fullName}
                bind:group={selectedConcept}
                class="mr-2"
              />
              <span>{concept.name}</span>
            </label>
          {/each}
        </div>
      {/each}
    </div>
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
