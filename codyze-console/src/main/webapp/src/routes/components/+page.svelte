<script lang="ts">
  import type { PageProps } from './$types';
  import { PageHeader } from '$lib/components/navigation';
  import { LoadingSpinner, EmptyState } from '$lib/components/ui';
  import { ComponentCard } from '$lib/components/dashboard';

  // Get data from the load function using props
  let { data }: PageProps = $props();

  // Compute empty state description
  const emptyStateDescription = $derived(() => {
    if (data.components === undefined) {
      return "Data is undefined - check if backend is running";
    } else if (Array.isArray(data.components) && data.components.length === 0) {
      return "Components array is empty - no components in the analysis result";
    } else {
      return "Unexpected data structure";
    }
  });
</script>

<div class="h-full">
  <PageHeader 
    title="Components" 
    subtitle="Browse project components and source files." 
  />

  {#if !data.components}
    <LoadingSpinner message="Loading components..." />
  {:else if Array.isArray(data.components) && data.components.length > 0}
    <!-- Components grid -->
    <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      {#each data.components as component}
        <ComponentCard {component} />
      {/each}
    </div>
  {:else}
    <EmptyState
      icon="component"
      title="No components found"
      description={emptyStateDescription()}
    />
  {/if}
</div>
