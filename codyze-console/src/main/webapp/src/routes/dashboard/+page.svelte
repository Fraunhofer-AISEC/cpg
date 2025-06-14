<script lang="ts">
  import type { PageProps } from './$types';
  import NewAnalysis from '$lib/components/NewAnalysis.svelte';
  import DashboardSection from '$lib/components/DashboardSection.svelte';
  import StatsGrid from '$lib/components/StatsGrid.svelte';
  import ViolationsTable from '$lib/components/ViolationsTable.svelte';
  import { invalidate } from '$app/navigation';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  let loading = $state(false);

  // Calculate requirement stats with the $derived rune
  const fulfillmentStats = $derived(data.result?.requirementCategories ? {
    total: data.result.requirementCategories.reduce((acc, cat) => acc + cat.requirements.length, 0),
    fulfilled: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'FULFILLED').length, 0),
    violated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'VIOLATED').length, 0),
    notEvaluated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'NOT_EVALUATED').length, 0)
  } : null);

  // Project overview stats
  const projectStats = $derived(data.project ? [
    { title: 'Project Name', value: data.project.name },
    { title: 'Source Directory', value: data.project.sourceDir },
    { title: 'Created', value: new Date(data.project.projectCreatedAt).toLocaleString() },
    ...(data.project.lastAnalyzedAt ? [{ title: 'Last Analyzed', value: new Date(data.project.lastAnalyzedAt).toLocaleString() }] : [])
  ] : []);

  // Requirements summary stats
  const requirementStats = $derived(fulfillmentStats ? [
    { title: 'Total Requirements', value: fulfillmentStats.total },
    { 
      title: 'Fulfilled', 
      value: fulfillmentStats.fulfilled,
      subtitle: `(${Math.round(fulfillmentStats.fulfilled / fulfillmentStats.total * 100) || 0}%)`,
      variant: 'success' as const
    },
    { 
      title: 'Violated', 
      value: fulfillmentStats.violated,
      subtitle: `(${Math.round(fulfillmentStats.violated / fulfillmentStats.total * 100) || 0}%)`,
      variant: 'danger' as const
    },
    { 
      title: 'Not Evaluated', 
      value: fulfillmentStats.notEvaluated,
      subtitle: `(${Math.round(fulfillmentStats.notEvaluated / fulfillmentStats.total * 100) || 0}%)`
    }
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

  async function handleSubmit(
    sourceDir: string,
    includeDir?: string,
    topLevel?: string,
    conceptSummaries?: string
  ) {
    loading = true;
    try {
      const response = await fetch('/api/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ sourceDir, includeDir, topLevel, conceptSummaries })
      });

      if (!response.ok) {
        throw new Error('Network response was not ok');
      }

      await response.json();

      // Invalidate data to trigger reload of both project and result data
      await invalidate('/api/project');
      await invalidate('/api/result');

    } catch (error) {
      console.error('Error during analysis:', error);
    } finally {
      loading = false;
    }
  }
</script>

<div>
  <header class="mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
    <p class="mt-1 text-sm text-gray-600">Overview of your analysis project</p>
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
    {#if data.result?.requirementCategories && data.result.requirementCategories.length > 0 && fulfillmentStats}
      <DashboardSection title="Requirements Summary" actionText="View all" actionHref="/requirements">
        <StatsGrid stats={requirementStats} />
      </DashboardSection>

      <!-- Recent Requirements -->
      <DashboardSection title="Recent Violations">
        <ViolationsTable categories={data.result.requirementCategories} />
      </DashboardSection>
    {/if}

    <!-- Components Summary -->
    {#if data.result?.components && data.result.components.length > 0}
      <DashboardSection title="Source Code Summary" actionText="View source" actionHref="/source">
        <StatsGrid stats={sourceStats} columns={3} />
      </DashboardSection>
    {/if}

    <!-- Start New Analysis -->
    <DashboardSection title="Start New Analysis">
      <NewAnalysis submit={handleSubmit} {loading} />
    </DashboardSection>
  {/if}
</div>
