<script lang="ts">
  import type { Snippet } from 'svelte';

  interface Props {
    title: string;
    subtitle?: string;
    breadcrumbText?: string;
    breadcrumbHref?: string;
    actions?: Snippet;
  }

  let { title, subtitle, breadcrumbText, breadcrumbHref, actions }: Props = $props();
</script>

<header class="mb-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{title}</h1>
      {#if subtitle}
        <p class="mt-1 text-sm text-gray-600">{subtitle}</p>
      {/if}
    </div>
    {#if actions}
      <div>
        {@render actions()}
      </div>
    {/if}
  </div>
  {#if breadcrumbText && breadcrumbHref}
    <div class="mt-4">
      <a href={breadcrumbHref} class="text-blue-600 hover:underline">← {breadcrumbText}</a>
    </div>
  {/if}
</header>
