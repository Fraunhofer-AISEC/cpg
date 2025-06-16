<script lang="ts">
  import type { PageProps } from './$types';
  import { PageHeader } from '$lib/components/navigation';
  import { Button } from '$lib/components/ui';
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

  // Preload the root QueryTree into the cache when the page loads
  onMount(() => {
    if (data.requirement.queryTree) {
      preloadQueryTree(data.requirement.queryTree);
    }
  });

  function goBack() {
    history.back();
  }
</script>

<svelte:head>
  <title>Requirement: {data.requirement.name} - CPG Console</title>
</svelte:head>

<div class="mb-6 flex items-center justify-between">
  <PageHeader title="Requirement Details" subtitle="Detailed analysis of requirement evaluation" />
  <Button variant="secondary" onclick={goBack}>← Back to Requirements</Button>
</div>

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
