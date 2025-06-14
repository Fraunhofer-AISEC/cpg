<script lang="ts">
  import type { PageProps } from './$types';
  import DashboardSection from '$lib/components/DashboardSection.svelte';
  import StatsGrid from '$lib/components/StatsGrid.svelte';
  import RequirementsChart from '$lib/components/RequirementsChart.svelte';
  import ViolationsTable from '$lib/components/ViolationsTable.svelte';
  import PageHeader from '$lib/components/PageHeader.svelte';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  // Calculate requirement stats with the $derived rune
  const fulfillmentStats = $derived(data.result?.requirementCategories ? {
    total: data.result.requirementCategories.reduce((acc, cat) => acc + cat.requirements.length, 0),
    fulfilled: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'FULFILLED').length, 0),
    violated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'VIOLATED').length, 0),
    rejected: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'REJECTED').length, 0),
    undecided: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'UNDECIDED').length, 0)
  } : {
    total: 0,
    fulfilled: 0,
    violated: 0,
    rejected: 0,
    undecided: 0
  });

  // Project overview stats
  const projectStats = $derived(data.project ? [
    { title: 'Project Name', value: data.project.name },
    { title: 'Source Directory', value: data.project.sourceDir },
    { title: 'Created', value: new Date(data.project.projectCreatedAt).toLocaleString() },
    ...(data.project.lastAnalyzedAt ? [{ title: 'Last Analyzed', value: new Date(data.project.lastAnalyzedAt).toLocaleString() }] : [])
  ] : []);

  // Source code summary stats
  const sourceStats = $derived(data.result?.components ? [
    { title: 'Components', value: data.result.components.length },
    { 
      title: 'Translation Units', 
      value: data.result.components.reduce((acc, comp) => acc + comp.translationUnits.length, 0)
    },
    { title: 'Total Nodes', value: data.result.totalNodes }
  ] : []);
</script>

<div>
  <header class="mb-6">
    <PageHeader 
      title="Dashboard" 
      subtitle="Overview of your analysis project"
    >
      {#snippet actions()}
        <a 
          href="/new-analysis" 
          class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          <svg class="-ml-1 mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          New Project
        </a>
      {/snippet}
    </PageHeader>
  </header>

  {#if !data.project && !data.result}
    <div class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
      <p class="ml-2 text-gray-700">Loading dashboard data...</p>
    </div>
  {:else}
    <!-- Project Overview -->
    {#if data.project}
      <DashboardSection title="Project Overview">
        <StatsGrid stats={projectStats} />
      </DashboardSection>
    {/if}

    <!-- Requirements Summary -->
    {#if data.result?.requirementCategories && data.result.requirementCategories.length > 0}
      <DashboardSection title="Requirements Summary" actionText="View all" actionHref="/requirements">
        <RequirementsChart 
          fulfilled={fulfillmentStats.fulfilled}
          violated={fulfillmentStats.violated}
          rejected={fulfillmentStats.rejected}
          undecided={fulfillmentStats.undecided}
        />
      </DashboardSection>

      <!-- Recent Requirements -->
      <DashboardSection title="Recent Violations">
        <ViolationsTable categories={data.result.requirementCategories} />
      </DashboardSection>
    {/if}

    <!-- Components Summary -->
    {#if data.result?.components && data.result.components.length > 0}
      <DashboardSection title="Source Code Summary" actionText="Browse components" actionHref="/source">
        <StatsGrid stats={sourceStats} columns={3} />
      </DashboardSection>
    {/if}

    <!-- Empty state for new users -->
    {#if !data.project && !data.result}
      <div class="rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 p-12 text-center">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        <h3 class="mt-4 text-lg font-medium text-gray-900">No projects yet</h3>
        <p class="mt-2 text-sm text-gray-500">Get started by creating your first analysis project.</p>
        <div class="mt-6">
          <a 
            href="/new-analysis" 
            class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            <svg class="-ml-1 mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Create New Project
          </a>
        </div>
      </div>
    {/if}
  {/if}
</div>
