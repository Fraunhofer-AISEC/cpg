<script lang="ts">
  import type { PageProps } from './$types';
  import { DashboardSection, StatsGrid } from '$lib/components/dashboard';
  import { RequirementsChart } from '$lib/components/requirements';
  import { ViolationsTable } from '$lib/components/analysis';
  import { PageHeader } from '$lib/components/navigation';
  import { LoadingSpinner, EmptyState } from '$lib/components/ui';
  import { calculateFulfillmentStats, calculateCombinedProjectStats } from '$lib/utils/dashboardStats';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  // Calculate requirement stats with the $derived rune
  const fulfillmentStats = $derived(
    calculateFulfillmentStats(data.result?.requirementCategories)
  );

  // Combined project and source code stats with additional metrics
  const combinedProjectStats = $derived(
    calculateCombinedProjectStats(data.project ?? undefined, data.result ?? undefined)
  );
</script>

<PageHeader title="Dashboard" subtitle="Overview of your analysis project">
  {#snippet children()}
    <a
      href="/new-analysis"
      class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:outline-none"
    >
      <svg class="mr-2 -ml-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
    <!-- Combined Project & Source Code Overview -->
    {#if data.project || (data.result?.components && data.result.components.length > 0)}
      <DashboardSection 
        title="Project & Source Code Overview"
        actionText="Browse components"
        actionHref="/source"
      >
        <StatsGrid stats={combinedProjectStats} />
      </DashboardSection>
    {/if}

    <!-- Requirements Summary -->
    {#if data.result?.requirementCategories && data.result.requirementCategories.length > 0}
      <DashboardSection
        title="Requirements Summary"
        actionText="View all"
        actionHref="/requirements"
      >
        <RequirementsChart
          fulfilled={fulfillmentStats.fulfilled}
          notFulfilled={fulfillmentStats.notFulfilled}
          rejected={fulfillmentStats.rejected}
          undecided={fulfillmentStats.undecided}
          notYetEvaluated={fulfillmentStats.notYetEvaluated}
        />
      </DashboardSection>

      <!-- Recent Requirements -->
      <DashboardSection title="Recent Violations">
        <ViolationsTable categories={data.result.requirementCategories} />
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
