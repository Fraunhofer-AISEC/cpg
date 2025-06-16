<script lang="ts">
  import type { RequirementJSON } from '$lib/types';
  import Button from '../ui/Button.svelte';

  interface Props {
    requirement: RequirementJSON;
  }

  let { requirement }: Props = $props();

  function navigateToRequirements() {
    window.location.href = '/requirements';
  }
</script>

<div class="bg-white border rounded-lg p-6">
  <h3 class="text-lg font-semibold mb-4 text-gray-900">
    Related Information
  </h3>
  
  <div class="space-y-4">
    <div>
      <h4 class="text-sm font-medium text-gray-700 mb-2">Category</h4>
      <p class="text-sm text-gray-600">
        This requirement belongs to the <code class="px-1 py-0.5 bg-gray-100 rounded text-xs">{requirement.categoryId}</code> category.
      </p>
    </div>
    
    {#if requirement.queryTree?.nodeId}
      <div>
        <h4 class="text-sm font-medium text-gray-700 mb-2">Associated Node</h4>
        <p class="text-sm text-gray-600 font-mono">
          {requirement.queryTree.nodeId}
        </p>
      </div>
    {/if}
    
    {#if requirement.queryTree?.callerInfo}
      <div>
        <h4 class="text-sm font-medium text-gray-700 mb-2">Query Source</h4>
        <div class="text-sm text-gray-600 space-y-1">
          <p class="font-mono">
            <span class="font-medium">Method:</span> {requirement.queryTree.callerInfo.className}.{requirement.queryTree.callerInfo.methodName}()
          </p>
          <p class="font-mono">
            <span class="font-medium">Location:</span> {requirement.queryTree.callerInfo.fileName}:{requirement.queryTree.callerInfo.lineNumber}
          </p>
        </div>
      </div>
    {/if}
    
    <div>
      <h4 class="text-sm font-medium text-gray-700 mb-2">Actions</h4>
      <Button variant="outline" size="sm" onclick={navigateToRequirements}>
        View All Requirements
      </Button>
    </div>
  </div>
</div>
