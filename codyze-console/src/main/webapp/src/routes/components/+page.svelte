<script lang="ts">
  import type { PageProps } from './$types';
  import PageHeader from '$lib/components/PageHeader.svelte';

  // Get data from the load function using props
  let { data }: PageProps = $props();
</script>

<div class="h-full">
  <PageHeader 
    title="Components" 
    subtitle="Browse project components and source files." 
  />

  {#if !data.components}
    <div class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
      <p class="ml-2 text-gray-700">Loading components...</p>
    </div>
  {:else if Array.isArray(data.components) && data.components.length > 0}
    <!-- Components grid -->
    <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {#each data.components as component}
        <a 
          href="/components/{component.name}" 
          class="group block rounded-lg border border-gray-200 bg-white p-6 hover:border-blue-300 hover:shadow-md transition-all"
        >
          <div class="flex items-start">
            <div class="flex-shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <div class="ml-4 flex-1 min-w-0">
              <h3 class="text-lg font-medium text-gray-900 group-hover:text-blue-600 transition-colors">
                {component.name}
              </h3>
              <p class="mt-1 text-sm text-gray-500 truncate" title={component.topLevel}>
                {component.topLevel}
              </p>
              <div class="mt-2 flex items-center text-sm text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {component.translationUnits?.length || 0} files
              </div>
            </div>
          </div>
        </a>
      {/each}
    </div>
  {:else}
    <div class="text-center py-12">
      <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900">No components found</h3>
      <p class="mt-1 text-sm text-gray-500">
        {#if data.components === undefined}
          Data is undefined - check if backend is running
        {:else if Array.isArray(data.components) && data.components.length === 0}
          Components array is empty - no components in the analysis result
        {:else}
          Unexpected data structure: {JSON.stringify(data.components)}
        {/if}
      </p>
    </div>
  {/if}
</div>
