<script lang="ts">
  import type { PageProps } from './$types';
  import PageHeader from '$lib/components/PageHeader.svelte';
  import QueryTreeExplorer from '$lib/components/QueryTreeExplorer.svelte';
  import Button from '$lib/components/Button.svelte';
  import { preloadQueryTree } from '$lib/stores/queryTreeStore';
  import { onMount } from 'svelte';

  let { data }: PageProps = $props();

  // Preload the root QueryTree into the cache when the page loads
  onMount(() => {
    if (data.requirement.queryTree) {
      preloadQueryTree(data.requirement.queryTree);
    }
  });

  // Status styling - using the same logic as QueryTree status
  const statusConfig = {
    FULFILLED: {
      bgColor: 'bg-green-50',
      textColor: 'text-green-700',
      badgeColor: 'bg-green-100 text-green-800',
      icon: '✓'
    },
    VIOLATED: {
      bgColor: 'bg-red-50',
      textColor: 'text-red-700',
      badgeColor: 'bg-red-100 text-red-800',
      icon: '✕'
    },
    REJECTED: {
      bgColor: 'bg-orange-50',
      textColor: 'text-orange-700',
      badgeColor: 'bg-orange-100 text-orange-800',
      icon: '⚠'
    },
    UNDECIDED: {
      bgColor: 'bg-yellow-50',
      textColor: 'text-yellow-700',
      badgeColor: 'bg-yellow-100 text-yellow-800',
      icon: '?'
    }
  };

  const config = statusConfig[data.requirement.status as keyof typeof statusConfig] || statusConfig.UNDECIDED;

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
  <div class="mb-8 p-6 border rounded-lg {config.bgColor} border-gray-200">
    <div class="flex items-start justify-between">
      <div class="flex-1">
        <div class="flex items-center space-x-3 mb-2">
          <h1 class="text-2xl font-bold {config.textColor}">
            {data.requirement.name}
          </h1>
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium {config.badgeColor}">
            {config.icon} {data.requirement.status}
          </span>
        </div>
        
        <p class="text-sm text-gray-600 mb-2">
          <strong>ID:</strong> {data.requirement.id}
        </p>
        
        <p class="text-sm text-gray-600 mb-4">
          <strong>Category:</strong> {data.requirement.categoryId}
        </p>
        
        {#if data.requirement.description}
          <div class="prose prose-sm max-w-none">
            <p class="{config.textColor}">
              {data.requirement.description}
            </p>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <!-- Query Tree Section -->
  {#if data.requirement.queryTree}
    <div class="mb-8">
      <h2 class="text-xl font-semibold mb-4 text-gray-900">
        Query Tree Analysis
      </h2>
      
      <div class="bg-white border rounded-lg p-4">
        <div class="mb-4 text-sm text-gray-600">
          <p>This tree shows how the requirement was evaluated, including all logical operations and their results.</p>
          <p class="mt-1">
            <span class="font-medium">✓ Green:</span> Fulfilled (true and accepted), 
            <span class="font-medium">✕ Red:</span> Violated (false and accepted), 
            <span class="font-medium">⚠ Orange:</span> Rejected results,
            <span class="font-medium">? Yellow:</span> Undecided results,
            <span class="font-medium">• Gray:</span> Non-boolean values
          </p>
          <p class="mt-1">
            <span class="font-medium">📍 Blue sections:</span> Show where in the code each query was executed from
          </p>
          <p class="mt-1">
            <span class="font-medium">🔷 Blue badges:</span> Show the QueryTree type - BinaryOperationResult (operations like AND, OR), UnaryOperationResult (operations like NOT), or QueryTree (single evaluations)
          </p>
        </div>
        
        <QueryTreeExplorer queryTree={data.requirement.queryTree} />
      </div>
    </div>
  {:else}
    <div class="mb-8 p-6 border rounded-lg bg-gray-50 border-gray-200">
      <div class="text-center text-gray-500">
        <p class="text-lg font-medium">No Query Tree Available</p>
        <p class="text-sm mt-1">This requirement has not been evaluated yet or the evaluation data is not available.</p>
      </div>
    </div>
  {/if}

  <!-- Additional Info -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- Evaluation Summary -->
    <div class="bg-white border rounded-lg p-6">
      <h3 class="text-lg font-semibold mb-4 text-gray-900">
        Evaluation Summary
      </h3>
      
      <dl class="space-y-3">
        <div class="flex justify-between">
          <dt class="text-sm font-medium text-gray-500">Status:</dt>
          <dd class="text-sm {config.textColor} font-medium">
            {config.icon} {data.requirement.status}
          </dd>
        </div>
        
        {#if data.requirement.queryTree}
          <div class="flex justify-between">
            <dt class="text-sm font-medium text-gray-500">Evaluation Result:</dt>
            <dd class="text-sm font-mono">
              <span class="{data.requirement.queryTree.value === 'true' ? 'text-green-600' : 'text-red-600'} font-semibold">
                {data.requirement.queryTree.value}
              </span>
            </dd>
          </div>
          
          <div class="flex justify-between">
            <dt class="text-sm font-medium text-gray-500">Confidence:</dt>
            <dd class="text-sm">
              {data.requirement.queryTree.confidence}
            </dd>
          </div>
          
          <div class="flex justify-between">
            <dt class="text-sm font-medium text-gray-500">Tree Depth:</dt>
            <dd class="text-sm">
              {data.requirement.queryTree.childrenIds && data.requirement.queryTree.childrenIds.length > 0 ? 'Has sub-evaluations' : 'Leaf evaluation'}
            </dd>
          </div>
        {/if}
      </dl>
    </div>

    <!-- Related Information -->
    <div class="bg-white border rounded-lg p-6">
      <h3 class="text-lg font-semibold mb-4 text-gray-900">
        Related Information
      </h3>
      
      <div class="space-y-4">
        <div>
          <h4 class="text-sm font-medium text-gray-700 mb-2">Category</h4>
          <p class="text-sm text-gray-600">
            This requirement belongs to the <code class="px-1 py-0.5 bg-gray-100 rounded text-xs">{data.requirement.categoryId}</code> category.
          </p>
        </div>
        
        {#if data.requirement.queryTree?.nodeId}
          <div>
            <h4 class="text-sm font-medium text-gray-700 mb-2">Associated Node</h4>
            <p class="text-sm text-gray-600 font-mono">
              {data.requirement.queryTree.nodeId}
            </p>
          </div>
        {/if}
        
        {#if data.requirement.queryTree?.callerInfo}
          <div>
            <h4 class="text-sm font-medium text-gray-700 mb-2">Query Source</h4>
            <div class="text-sm text-gray-600 space-y-1">
              <p class="font-mono">
                <span class="font-medium">Method:</span> {data.requirement.queryTree.callerInfo.className}.{data.requirement.queryTree.callerInfo.methodName}()
              </p>
              <p class="font-mono">
                <span class="font-medium">Location:</span> {data.requirement.queryTree.callerInfo.fileName}:{data.requirement.queryTree.callerInfo.lineNumber}
              </p>
            </div>
          </div>
        {/if}
        
        <div>
          <h4 class="text-sm font-medium text-gray-700 mb-2">Actions</h4>
          <Button variant="outline" size="sm" onclick={() => window.location.href = '/requirements'}>
            View All Requirements
          </Button>
        </div>
      </div>
    </div>
  </div>
</div>
