<script lang="ts">
  interface Props {
    title?: string;
    subtitle?: string;
    padding?: 'sm' | 'md' | 'lg';
    border?: boolean;
    shadow?: boolean;
    hover?: boolean;
  }

  let {
    title,
    subtitle,
    padding = 'md',
    border = true,
    shadow = false,
    hover = false
  }: Props = $props();

  const paddingClasses = {
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8'
  };
</script>

<div
  class="
  rounded-lg bg-white
  {border ? 'border border-gray-200' : ''}
  {shadow ? 'shadow-sm' : ''}
  {hover ? 'transition-shadow hover:shadow-md' : ''}
  {paddingClasses[padding]}
"
>
  {#if title || subtitle}
    <div class="mb-4">
      {#if title}
        <h3 class="text-lg font-medium text-gray-900">{title}</h3>
      {/if}
      {#if subtitle}
        <p class="mt-1 text-sm text-gray-500">{subtitle}</p>
      {/if}
    </div>
  {/if}

  <slot />
</div>
