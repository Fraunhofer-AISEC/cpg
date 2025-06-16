<script lang="ts">
  import type { PageProps } from './$types';
  import { DashboardSection, StatsGrid } from '$lib/components/dashboard';
  import { RequirementsChart } from '$lib/components/requirements';
  import { ViolationsTable } from '$lib/components/analysis';
  import { PageHeader } from '$lib/components/navigation';
  import { LoadingSpinner, EmptyState } from '$lib/components/ui';

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

<div class="space-y-6">
  {#if !data.project && !data.result}
    <LoadingSpinner message="Loading dashboard data..." />
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
      <EmptyState
        icon="chart"
        title="No projects yet"
        description="Get started by creating your first analysis project."
        actionText="Create New Project"
        actionHref="/new-analysis"
      />
    {/if}
  {/if}
</div>
