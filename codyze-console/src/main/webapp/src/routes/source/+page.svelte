<script lang="ts">
  import type { PageProps } from './$types';
  import type { ComponentJSON, TranslationUnitJSON } from '$lib/types';

  // Get data from the load function using props
  let { data }: PageProps = $props();

  // Use proper Svelte 5 runes for state
  let selectedComponent = $state<ComponentJSON | null>(null);
  let selectedUnit = $state<TranslationUnitJSON | null>(null);

  // Initialize selectedComponent when data changes
  $effect(() => {
    if (data.result?.components && data.result.components.length > 0) {
      selectedComponent = data.result.components[0];
    } else {
      selectedComponent = null;
    }
    selectedUnit = null;
  });

  function selectTranslationUnit(unit: TranslationUnitJSON) {
    selectedUnit = unit;
  }
</script>

<div class="h-full">
  <header class="mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Source Code</h1>
    <p class="mt-1 text-sm text-gray-600">Browse project components and source files.</p>
  </header>

  {#if !data.result}
    <div class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
      <p class="ml-2 text-gray-700">Loading source code...</p>
    </div>
  {:else if data.result?.components && data.result.components.length > 0}
    <div class="flex h-[calc(100vh-180px)] overflow-hidden rounded-lg border border-gray-200 bg-white">
      <!-- Component tree sidebar -->
      <div class="w-72 overflow-auto border-r border-gray-200">
        <nav class="p-4">
          <h2 class="mb-2 text-xs font-semibold uppercase text-gray-500">Components</h2>
          <ul class="space-y-1">
            {#each data.result.components as component}
              <li>
                <button
                  class="flex w-full items-center rounded-md px-3 py-2 text-left text-sm {selectedComponent?.name === component.name ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}"
                  onclick={() => selectedComponent = component}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
                  </svg>
                  {component.name}
                </button>

                {#if selectedComponent?.name === component.name}
                  <ul class="ml-6 mt-1 space-y-1">
                    {#each component.translationUnits as unit}
                      <li>
                        <button
                          class="flex w-full items-center rounded-md px-3 py-2 text-left text-xs {selectedUnit?.id === unit.id ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'}"
                          onclick={() => selectTranslationUnit(unit)}
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" class="mr-2 h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                          {unit.name}
                        </button>
                      </li>
                    {/each}
                  </ul>
                {/if}
              </li>
            {/each}
          </ul>
        </nav>
      </div>

      <!-- Source code viewer -->
      <div class="flex-1 overflow-auto">
        {#if selectedUnit}
          <div class="h-full">
            <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-2">
              <div class="text-sm text-gray-700">{selectedUnit.name}</div>
            </div>
            <pre class="h-full overflow-auto bg-white p-4 text-sm text-gray-800">
              <code>{selectedUnit.code}</code>
            </pre>
          </div>
        {:else}
          <div class="flex h-full items-center justify-center p-6">
            <div class="text-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
              </svg>
              <h3 class="mt-2 text-sm font-medium text-gray-900">No file selected</h3>
              <p class="mt-1 text-xs text-gray-500">Select a file from the sidebar to view its contents</p>
            </div>
          </div>
        {/if}
      </div>
    </div>
  {:else}
    <div class="rounded-lg border border-gray-200 bg-white p-8 text-center">
      <div class="mx-auto mb-4 h-16 w-16 rounded-full bg-gray-100 p-3 text-gray-500">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-full w-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
        </svg>
      </div>
      <h3 class="text-lg font-medium text-gray-900">No source code found</h3>
      <p class="mt-2 text-sm text-gray-600">
        There are no components or source files available for this project.
      </p>
    </div>
  {/if}
</div>
