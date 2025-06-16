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

  const config = $derived(statusConfig[requirement.status as keyof typeof statusConfig] || statusConfig.UNDECIDED);
</script>

<div class="p-6 border rounded-lg {config.bgColor} border-gray-200">
  <div class="flex items-start justify-between">
    <div class="flex-1">
      <div class="flex items-center space-x-3 mb-2">
        <h1 class="text-2xl font-bold {config.textColor}">
          {requirement.name}
        </h1>
        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium {config.badgeColor}">
          {config.icon} {requirement.status}
        </span>
      </div>
      
      <p class="text-sm text-gray-600 mb-2">
        <strong>ID:</strong> {requirement.id}
      </p>
      
      <p class="text-sm text-gray-600 mb-4">
        <strong>Category:</strong> {requirement.categoryId}
      </p>
      
      {#if requirement.description}
        <div class="prose prose-sm max-w-none">
          <p class="{config.textColor}">
            {requirement.description}
          </p>
        </div>
      {/if}
    </div>
  </div>
</div>
