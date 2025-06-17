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
  const fulfillmentStats = $derived(
    data.result?.requirementCategories
      ? {
          total: data.result.requirementCategories.reduce(
            (acc, cat) => acc + cat.requirements.length,
            0
          ),
          fulfilled: data.result.requirementCategories.reduce(
            (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'FULFILLED').length,
            0
          ),
          notFulfilled: data.result.requirementCategories.reduce(
            (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'NOT_FULFILLED').length,
            0
          ),
          rejected: data.result.requirementCategories.reduce(
            (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'REJECTED').length,
            0
          ),
          undecided: data.result.requirementCategories.reduce(
            (acc, cat) => acc + cat.requirements.filter((r) => r.status === 'UNDECIDED').length,
            0
          ),
          notYetEvaluated: data.result.requirementCategories.reduce(
            (acc, cat) =>
              acc + cat.requirements.filter((r) => r.status === 'NOT_YET_EVALUATED').length,
            0
          )
        }
      : {
          total: 0,
          fulfilled: 0,
          notFulfilled: 0,
          rejected: 0,
          undecided: 0,
          notYetEvaluated: 0
        }
  );

  // Combined project and source code stats with additional metrics
  const combinedProjectStats = $derived(() => {
    const stats = [];
    
    // Project info
    if (data.project) {
      stats.push(
        { title: 'Project Name', value: data.project.name },
        { title: 'Created', value: new Date(data.project.projectCreatedAt).toLocaleString() }
      );
      
      if (data.project.lastAnalyzedAt) {
        stats.push({
          title: 'Last Analyzed',
          value: new Date(data.project.lastAnalyzedAt).toLocaleString()
        });
      }
    }
    
    // Source code info with additional calculated metrics
    if (data.result?.components) {
      const totalTUs = data.result.components.reduce(
        (acc, comp) => acc + comp.translationUnits.length,
        0
      );
      
      const avgTUsPerComponent = totalTUs > 0 ? 
        Math.round((totalTUs / data.result.components.length) * 10) / 10 : 0;
      
      const avgNodesPerTU = totalTUs > 0 ? 
        Math.round((data.result.totalNodes / totalTUs) * 10) / 10 : 0;
      
      // Find largest component
      const largestComponent = data.result.components.reduce((max, comp) => 
        comp.translationUnits.length > max.translationUnits.length ? comp : max
      );
      
      stats.push(
        { title: 'Components', value: data.result.components.length },
        { title: 'Translation Units', value: totalTUs },
        { title: 'Total Nodes', value: data.result.totalNodes.toLocaleString() },
        { title: 'Avg TUs per Component', value: avgTUsPerComponent },
        { title: 'Avg Nodes per TU', value: avgNodesPerTU },
        { 
          title: 'Largest Component', 
          value: `${largestComponent.name} (${largestComponent.translationUnits.length} files)` 
        }
      );
    }
    
    return stats;
  });
</script>

<PageHeader title="Dashboard" subtitle="Overview of your analysis project">
  {#snippet actions()}
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
        <StatsGrid stats={combinedProjectStats()} />
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
