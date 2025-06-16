<script lang="ts">
  import type { RequirementJSON } from '$lib/types';
  import Button from '../ui/Button.svelte';
  import { getShortCallerInfo } from '$lib/utils/display';

  interface Props {
    requirement: RequirementJSON;
  }

  let { requirement }: Props = $props();

  function navigateToRequirements() {
    window.location.href = '/requirements';
  }
</script>

<div class="rounded-lg border bg-white p-6">
  <h3 class="mb-4 text-lg font-semibold text-gray-900">Related Information</h3>

  <div class="space-y-4">
    <div>
      <h4 class="mb-2 text-sm font-medium text-gray-700">Category</h4>
      <p class="text-sm text-gray-600">
        This requirement belongs to the <code class="rounded bg-gray-100 px-1 py-0.5 text-xs"
          >{requirement.categoryId}</code
        > category.
      </p>
    </div>

    {#if requirement.queryTree?.nodeId}
      <div>
        <h4 class="mb-2 text-sm font-medium text-gray-700">Associated Node</h4>
        <p class="font-mono text-sm text-gray-600">
          {requirement.queryTree.nodeId}
        </p>
      </div>
    {/if}

    {#if requirement.queryTree?.callerInfo}
      <div>
        <h4 class="mb-2 text-sm font-medium text-gray-700">Query Source</h4>
        <div class="space-y-1 text-sm text-gray-600">
          <p class="font-mono">
            <span class="font-medium">Method:</span>
            {getShortCallerInfo(
              requirement.queryTree.callerInfo.className,
              requirement.queryTree.callerInfo.methodName
            )}
          </p>
          <p class="font-mono">
            <span class="font-medium">Location:</span>
            {requirement.queryTree.callerInfo.fileName}:{requirement.queryTree.callerInfo
              .lineNumber}
          </p>
        </div>
      </div>
    {/if}

    <div>
      <h4 class="mb-2 text-sm font-medium text-gray-700">Actions</h4>
      <Button variant="outline" size="sm" onclick={navigateToRequirements}>
        View All Requirements
      </Button>
    </div>
  </div>
</div>
