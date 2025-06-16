<script lang="ts">
  import type { RequirementJSON } from '$lib/types';

  interface Props {
    requirement: RequirementJSON;
  }

  let { requirement }: Props = $props();

  // Status styling configuration
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

  const config = $derived(
    statusConfig[requirement.status as keyof typeof statusConfig] || statusConfig.UNDECIDED
  );
</script>

<div class="rounded-lg border p-6 {config.bgColor} border-gray-200">
  <div class="flex items-start justify-between">
    <div class="flex-1">
      <div class="mb-2 flex items-center space-x-3">
        <h1 class="text-2xl font-bold {config.textColor}">
          {requirement.name}
        </h1>
        <span
          class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium {config.badgeColor}"
        >
          {config.icon}
          {requirement.status}
        </span>
      </div>

      <p class="mb-2 text-sm text-gray-600">
        <strong>ID:</strong>
        {requirement.id}
      </p>

      <p class="mb-4 text-sm text-gray-600">
        <strong>Category:</strong>
        {requirement.categoryId}
      </p>

      {#if requirement.description}
        <div class="prose prose-sm max-w-none">
          <p class={config.textColor}>
            {requirement.description}
          </p>
        </div>
      {/if}
    </div>
  </div>
</div>
