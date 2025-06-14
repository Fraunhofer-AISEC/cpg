<script lang="ts">
  import type { PageProps } from './$types';
  import DashboardSection from '$lib/components/DashboardSection.svelte';
  import StatsGrid from '$lib/components/StatsGrid.svelte';
  import CategorySection from '$lib/components/CategorySection.svelte';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  // Calculate overall requirement stats
  const overallStats = $derived(data.result?.requirementCategories ? {
    total: data.result.requirementCategories.reduce((acc, cat) => acc + cat.requirements.length, 0),
    fulfilled: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'FULFILLED').length, 0),
    violated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'VIOLATED').length, 0),
    notEvaluated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'NOT_EVALUATED').length, 0),
    categories: data.result.requirementCategories.length
  } : null);

  // Stats for the summary grid
  const summaryStats = $derived(overallStats ? [
    { title: 'Total Requirements', value: overallStats.total },
    { title: 'Categories', value: overallStats.categories },
    { 
      title: 'Fulfilled', 
      value: overallStats.fulfilled,
      subtitle: `(${Math.round(overallStats.fulfilled / overallStats.total * 100) || 0}%)`,
      variant: 'success' as const
    },
    { 
      title: 'Violated', 
      value: overallStats.violated,
      subtitle: `(${Math.round(overallStats.violated / overallStats.total * 100) || 0}%)`,
      variant: 'danger' as const
    }
  ] : []);
</script>

<div>
  <header class="mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Requirements Analysis</h1>
    <p class="mt-1 text-sm text-gray-600">View and manage project requirements and their fulfillment status.</p>
  </header>

  {#if !data.result}
    <div class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
      <p class="ml-2 text-gray-700">Loading requirements...</p>
    </div>
  {:else if data.result.requirementCategories && data.result.requirementCategories.length > 0}
    <!-- Summary Statistics -->
    <DashboardSection title="Requirements Overview">
      <StatsGrid stats={summaryStats} />
    </DashboardSection>

    <!-- Requirements by Category -->
    <DashboardSection title="Requirements by Category">
      <div class="space-y-4">
        {#each data.result.requirementCategories as category (category.id)}
          <CategorySection {category} />
        {/each}
      </div>
    </DashboardSection>
  {:else}
    <DashboardSection title="No Requirements Found">
      <div class="text-center py-12">
        <div class="mx-auto mb-4 h-16 w-16 rounded-full bg-gray-100 p-3 text-gray-500">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-full w-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        </div>
        <h3 class="text-lg font-medium text-gray-900">No requirements found</h3>
        <p class="mt-2 text-sm text-gray-600">
          This project doesn't have any requirements defined or they haven't been evaluated yet.
        </p>
      </div>
    </DashboardSection>
  {/if}
</div>
