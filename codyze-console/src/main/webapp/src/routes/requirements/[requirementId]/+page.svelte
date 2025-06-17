<script lang="ts">
  import type { PageProps } from './$types';
  import { PageHeader, BackButton } from '$lib/components/navigation';
  import {
    RequirementStatusCard,
    QueryTreeSection,
    EvaluationSummaryCard,
    RequirementRelatedInfoCard
  } from '$lib/components/requirements';
  import { preloadQueryTree } from '$lib/stores/queryTreeStore';
  import { onMount } from 'svelte';
  import { page } from '$app/stores';

  let { data }: PageProps = $props();

  // Get target node from URL parameters
  const targetNodeId = $derived($page.url.searchParams.get('targetNodeId'));

  // Breadcrumb items
  const breadcrumbItems = $derived([
    { label: 'Requirements', href: '/requirements' },
    { label: data.requirement.categoryId, href: `/requirements?category=${encodeURIComponent(data.requirement.categoryId)}` },
    { label: data.requirement.name, href: `/requirements/${data.requirement.id}` }
  ]);

  // Preload the root QueryTree into the cache when the page loads
  onMount(() => {
    if (data.requirement.queryTree) {
      preloadQueryTree(data.requirement.queryTree);
    }
  });
</script>

<svelte:head>
  <title>Requirement: {data.requirement.name} - CPG Console</title>
</svelte:head>

<PageHeader 
  title="Requirement Details" 
  subtitle="Detailed analysis of requirement evaluation"
  breadcrumbItems={breadcrumbItems}
>
  <BackButton fallbackHref="/requirements" fallbackText="Back to Requirements" />
</PageHeader>

<div class="space-y-8">
  <!-- Requirement Info Card -->
  <RequirementStatusCard requirement={data.requirement} />

  <!-- Query Tree Section -->
  <QueryTreeSection
    queryTree={data.requirement.queryTree}
    requirementId={data.requirement.id}
    targetNodeId={targetNodeId || undefined}
  />

  <!-- Additional Info -->
  <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
    <!-- Evaluation Summary -->
    <EvaluationSummaryCard requirement={data.requirement} />

    <!-- Related Information -->
    <RequirementRelatedInfoCard requirement={data.requirement} />
  </div>
</div>
