<script lang="ts">
  type Status = 'FULFILLED' | 'NOT_FULFILLED' | 'REJECTED' | 'UNDECIDED' | 'NOT_YET_EVALUATED';

  interface Props {
    status: Status;
    size?: 'sm' | 'md' | 'lg';
    showIcon?: boolean;
  }

  let { status, size = 'md', showIcon = true }: Props = $props();

  // Status styling configuration
  const statusConfig = {
    FULFILLED: {
      badgeColor: 'bg-green-100 text-green-800',
      icon: '✓'
    },
    NOT_FULFILLED: {
      badgeColor: 'bg-red-100 text-red-800',
      icon: '✕'
    },
    REJECTED: {
      badgeColor: 'bg-orange-100 text-orange-800',
      icon: '⚠'
    },
    UNDECIDED: {
      badgeColor: 'bg-yellow-100 text-yellow-800',
      icon: '?'
    },
    NOT_YET_EVALUATED: {
      badgeColor: 'bg-gray-100 text-gray-800',
      icon: '⏳'
    }
  };

  const sizeConfig = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-0.5 text-xs',
    lg: 'px-3 py-1 text-sm'
  };

  const config = $derived(statusConfig[status] || statusConfig.UNDECIDED);
  const sizeClasses = $derived(sizeConfig[size]);
</script>

<span class="inline-flex items-center {sizeClasses} rounded-full font-medium {config.badgeColor}">
  {#if showIcon}
    {config.icon}
  {/if}
  {status}
</span>
