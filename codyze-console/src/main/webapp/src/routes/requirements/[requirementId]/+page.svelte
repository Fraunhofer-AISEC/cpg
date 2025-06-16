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

  let { data }: PageProps = $props();

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

<div class="container mx-auto px-4 py-6">
  <!-- Header -->
  <div class="mb-6">
    <div class="flex items-center justify-between">
      <PageHeader 
        title="Requirement Details"
        subtitle="Detailed analysis of requirement evaluation"
      />
      <Button variant="secondary" onclick={goBack}>
        ← Back to Requirements
      </Button>
    </div>
  </div>

  <!-- Requirement Info Card -->
  <div class="mb-8">
    <RequirementStatusCard requirement={data.requirement} />
  </div>

  <!-- Query Tree Section -->
  <QueryTreeSection queryTree={data.requirement.queryTree} />

  <!-- Additional Info -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- Evaluation Summary -->
    <EvaluationSummaryCard requirement={data.requirement} />

    <!-- Related Information -->
    <RequirementRelatedInfoCard requirement={data.requirement} />
  </div>
</div>
