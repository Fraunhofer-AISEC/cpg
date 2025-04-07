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

  let selectedConceptName = $state<string>('');
  const selectedConcept = $derived.by(
    () =>
      conceptGroups
        .flatMap((group) => group.concepts)
        .find((concept) => concept.info.conceptName === selectedConceptName)?.info
  );
  let constructorValues = $derived.by<string[]>(() => {
    if (selectedConcept != null) {
      return Array(selectedConcept.constructorInfo.length - 1).fill('');
    } else {
      return [];
    }
  });
  let dfgToConcept = $state(false);
  let dfgFromConcept = $state(false);

  async function handleSubmit() {
    if (!selectedConceptName) {
      alert('Please select a concept');
      return;
    }

    try {
      const response = await fetch('/api/concepts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nodeId: node.id,
          conceptName: selectedConceptName,
          addDFGToConcept: dfgToConcept,
          addDFGFromConcept: dfgFromConcept,
          constructorArgs: constructorValues.map((value, index) => ({
            argumentName: selectedConcept?.constructorInfo[index + 1].argumentName,
            argumentValue: value,
            argumentType: selectedConcept?.constructorInfo[index + 1].argumentType
          }))
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

<form
  role="presentation"
  class="fixed inset-0 z-50 flex items-center justify-center"
  onclick={() => (showDialog = false)}
  onkeydown={(e) => e.key === 'Escape' && (showDialog = false)}
  onsubmit={handleSubmit}
>
  <div
    role="presentation"
    class="z-60 max-h-[80vh] overflow-auto rounded bg-white p-4 shadow-lg"
    onclick={(e) => e.stopPropagation()}
  >
    <div class="mb-4">
      <h3 class="text-lg font-bold">Add Concept</h3>
      <div class="text-sm text-gray-500">
        Concept will be added to node: {node.name}
      </div>
    </div>
    <div class="mb-4 rounded border p-2">
      {#each conceptGroups as { path, concepts } (path)}
        <div class="my-1 border-l-2 border-gray-200 pl-4">
          {#if path}
            <div class="text-sm font-medium text-gray-600">{path}</div>
          {/if}
          {#each concepts as concept (concept.name)}
            <label class="flex cursor-pointer items-center py-1 hover:bg-gray-50">
              <span class="mr-2 text-gray-400">└─</span>
              <input
                type="radio"
                name="concept"
                value={concept.info.conceptName}
                bind:group={selectedConceptName}
                class="mr-2"
              />
              <span class="text-sm">{concept.name}</span>
            </label>
          {/each}
        </div>
      {/each}
    </div>
    <div class="mb-4 space-y-2 text-sm">
      <label class="flex items-center">
        <input type="checkbox" bind:checked={dfgToConcept} class="mr-2" />
        Connect DFG from this node to the new concept
      </label>
      <label class="flex items-center">
        <input type="checkbox" bind:checked={dfgFromConcept} class="mr-2" />
        Connect DFG from the new concept to this node
      </label>
    </div>
    {#if selectedConcept?.constructorInfo?.length ?? 0 > 1}
      <h4 class="mb-2 text-sm font-medium">Constructor Arguments</h4>
      <div class="-space-y-px">
        {#each selectedConcept?.constructorInfo?.slice(1) ?? [] as arg, idx (idx)}
          <div
            class="
            {idx == 0 ? 'rounded-t-md' : ''}
            {idx == (selectedConcept?.constructorInfo?.length ?? 0) - 2 ? 'rounded-b-md' : ''} 
              bg-white px-3 pt-2.5 pb-1.5 outline outline-1 -outline-offset-1 outline-gray-300 focus-within:relative focus-within:outline focus-within:outline-2 focus-within:-outline-offset-2 focus-within:outline-indigo-600"
          >
            <label for="arg{idx}" class="block text-xs font-medium text-gray-900"
              >{arg.argumentName}</label
            >
            <input
              type="text"
              name="arg{idx}"
              id="arg{idx}"
              bind:value={constructorValues[idx]}
              class="block w-full text-gray-900 placeholder:text-gray-400 focus:outline focus:outline-0 sm:text-sm/6"
              placeholder="Enter value for {arg.argumentName}"
              required
            />
          </div>
        {/each}
      </div>
    {/if}
    <div class="mt-4 flex justify-end space-x-2">
      <button
        class="rounded bg-gray-300 px-4 py-2 hover:bg-gray-400"
        onclick={() => (showDialog = false)}
      >
        Cancel
      </button>
      <button class="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700" type="submit">
        Add
      </button>
    </div>
  </div>
</form>
