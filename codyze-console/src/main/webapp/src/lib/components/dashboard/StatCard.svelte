<script lang="ts">
  interface Props {
    title: string;
    value: string | number;
    subtitle?: string;
    variant?: 'default' | 'success' | 'danger' | 'warning';
  }

  let { title, value, subtitle, variant = 'default' }: Props = $props();

  const variants = {
    default: 'bg-gray-50 border-gray-200',
    success: 'bg-green-50 border-gray-200',
    danger: 'bg-red-50 border-gray-200',
    warning: 'bg-yellow-50 border-gray-200'
  };

  const valueColors = {
    default: 'text-gray-900',
    success: 'text-green-600',
    danger: 'text-red-600',
    warning: 'text-yellow-600'
  };
</script>

<div class="rounded-md border p-4 {variants[variant]}">
  <h3 class="text-xs font-medium text-gray-500 uppercase">{title}</h3>
  <p class="mt-1 text-lg font-semibold {valueColors[variant]}">
    {value}
    {#if subtitle}
      <span class="ml-1 text-xs font-normal text-gray-500">
        {subtitle}
      </span>
    {/if}
  </p>
</div>
