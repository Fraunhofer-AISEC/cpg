<script lang="ts">
  import type { PageProps } from './$types';
  import { DashboardSection, StatsGrid, CategorySection } from '$lib/components/dashboard';
  import { PageHeader } from '$lib/components/navigation';
  import { LoadingSpinner, EmptyState } from '$lib/components/ui';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  // Calculate overall requirement stats
  const overallStats = $derived(
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
          ),
          categories: data.result.requirementCategories.length
        }
      : null
  );

  // Stats for the summary grid
  const summaryStats = $derived(
    overallStats
      ? [
          { title: 'Total Requirements', value: overallStats.total },
          { title: 'Categories', value: overallStats.categories },
          {
            title: 'Fulfilled',
            value: overallStats.fulfilled,
            subtitle: `(${Math.round((overallStats.fulfilled / overallStats.total) * 100) || 0}%)`,
            variant: 'success' as const
          },
          {
            title: 'Not Fulfilled',
            value: overallStats.notFulfilled,
            subtitle: `(${Math.round((overallStats.notFulfilled / overallStats.total) * 100) || 0}%)`,
            variant: 'danger' as const
          }
        ]
      : []
  );
</script>

<PageHeader
  title="Requirements Analysis"
  subtitle="View and manage project requirements and their fulfillment status."
/>

<div class="space-y-6">
  {#if !data.result}
    <LoadingSpinner message="Loading requirements..." />
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
    <EmptyState
      icon="document"
      title="No requirements found"
      description="This project doesn't have any requirements defined or they haven't been evaluated yet."
    />
  {/if}
</div>
