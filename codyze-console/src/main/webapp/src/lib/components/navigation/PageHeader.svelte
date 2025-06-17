<script lang="ts">
  import type { Snippet } from 'svelte';
  import { Button } from '$lib/components/ui';
  import Breadcrumb from './Breadcrumb.svelte';

  interface BreadcrumbItem {
    label: string;
    href: string;
  }

  interface Props {
    title: string;
    subtitle?: string;
    breadcrumbText?: string;
    breadcrumbHref?: string;
    breadcrumbItems?: BreadcrumbItem[];
    children?: Snippet;
  }

  let { title, subtitle, breadcrumbText, breadcrumbHref, breadcrumbItems, children }: Props = $props();

  function handleBreadcrumbClick() {
    if (breadcrumbHref) {
      window.location.href = breadcrumbHref;
    }
  }
</script>

<header class="mb-6 pb-4 border-b border-gray-200">
  {#if breadcrumbText && breadcrumbHref}
    <div class="mb-4">
      <Button variant="secondary" onclick={handleBreadcrumbClick}>
        ← {breadcrumbText}
      </Button>
    </div>
  {/if}
  
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{title}</h1>
      {#if subtitle}
        <p class="mt-1 text-sm text-gray-600">{subtitle}</p>
      {/if}
    </div>
    
    {#if children}
      <div>
        {@render children()}
      </div>
    {/if}
  </div>
  
  {#if breadcrumbItems && breadcrumbItems.length > 0}
    <div class="mt-3">
      <Breadcrumb items={breadcrumbItems} />
    </div>
  {/if}
</header>
