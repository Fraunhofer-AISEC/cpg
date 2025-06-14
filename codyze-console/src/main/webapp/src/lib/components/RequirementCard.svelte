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

<div class="rounded-lg border border-gray-200 {config.bgColor} p-4">
  <div class="flex items-start justify-between">
    <div class="flex-1">
      <div class="flex items-center gap-2">
        <span class="flex h-6 w-6 items-center justify-center rounded-full text-sm font-medium {config.badgeColor}">
          {config.icon}
        </span>
        <h3 class="font-medium {config.textColor}">{requirement.name}</h3>
      </div>
      <p class="mt-2 text-sm text-gray-600">{requirement.description}</p>
    </div>
    <span class="ml-4 inline-flex rounded-full px-2 py-1 text-xs font-semibold {config.badgeColor}">
      {requirement.status.replace('_', ' ')}
    </span>
  </div>
</div>
