<script lang="ts">
  import type { RequirementJSON } from '$lib/types';

  interface Props {
    requirement: RequirementJSON;
  }

  let { requirement }: Props = $props();

  // Status styling
  const statusConfig = {
    FULFILLED: {
      bgColor: 'bg-green-50',
      textColor: 'text-green-700',
      badgeColor: 'bg-green-100 text-green-800',
      icon: '✓'
    },
    NOT_FULFILLED: {
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

  const config = $derived(
    statusConfig[requirement.status as keyof typeof statusConfig] || statusConfig.UNDECIDED
  );

  function navigateToDetail() {
    window.location.href = `/requirements/${requirement.id}`;
  }
</script>

<button
  type="button"
  onclick={navigateToDetail}
  class="w-full rounded-lg border border-gray-200 text-left {config.bgColor} cursor-pointer p-4 transition-shadow hover:shadow-md"
>
  <div class="flex items-start justify-between">
    <div class="flex-1">
      <div class="flex items-center gap-2">
        <span
          class="flex h-6 w-6 items-center justify-center rounded-full text-sm font-medium {config.badgeColor}"
        >
          {config.icon}
        </span>
        <h3 class="font-medium {config.textColor}">{requirement.name}</h3>
        {#if requirement.queryTree}
          <span class="rounded-full bg-blue-100 px-2 py-1 text-xs text-blue-700">
            Has Query Tree
          </span>
        {/if}
      </div>
      <p class="mt-2 text-sm text-gray-600">{requirement.description}</p>
    </div>
    <div class="ml-4 flex items-center space-x-2">
      <span class="inline-flex rounded-full px-2 py-1 text-xs font-semibold {config.badgeColor}">
        {requirement.status.replace('_', ' ')}
      </span>
      <svg class="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </div>
  </div>
</button>
