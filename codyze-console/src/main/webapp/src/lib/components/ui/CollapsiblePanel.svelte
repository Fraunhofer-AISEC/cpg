<script lang="ts">
  import type { Snippet } from 'svelte';

  interface Props {
    title: string;
    collapsed?: boolean;
    side?: 'left' | 'right';
    width?: string;
    children: Snippet;
  }

  let { title, collapsed = $bindable(false), side = 'right', width = 'w-96', children }: Props = $props();

  const isLeft = $derived(side === 'left');
</script>

{#if collapsed}
  <button
    onclick={() => (collapsed = false)}
    class="group flex h-full w-8 shrink-0 flex-col items-center justify-start gap-2 bg-white pt-4 text-gray-400 transition-all hover:bg-blue-50 hover:text-blue-600
      {isLeft ? 'border-r border-gray-200 rounded-l-xl' : 'border-l border-gray-200 rounded-r-xl'}"
    aria-label="Show {title} panel"
  >
    <svg
      class="w-3 h-3 transition-transform {isLeft ? 'group-hover:translate-x-0.5' : 'group-hover:-translate-x-0.5'}"
      fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5"
    >
      <path stroke-linecap="round" stroke-linejoin="round" d="{isLeft ? 'M9 5l7 7-7 7' : 'M15 19l-7-7 7-7'}" />
    </svg>
    <span class="text-[10px] font-semibold tracking-widest uppercase group-hover:text-blue-500" style="writing-mode: vertical-rl;">{title}</span>
  </button>
{:else}
  <div class="flex h-full {width} shrink-0 flex-col bg-white overflow-hidden
    {isLeft ? 'border-r border-gray-200 rounded-l-xl' : 'border-l border-gray-200 rounded-r-xl'}">
    <button
      onclick={() => (collapsed = true)}
      class="group flex w-full shrink-0 items-center justify-between border-b border-gray-200 px-3 py-2 text-left transition-colors hover:bg-gray-50"
      aria-label="Collapse {title} panel"
    >
      {#if isLeft}
        <div class="flex items-center gap-2">
          <div class="h-3.5 w-0.5 rounded-full bg-blue-500"></div>
          <span class="text-[11px] font-semibold uppercase tracking-widest text-gray-500 group-hover:text-gray-700">{title}</span>
        </div>
        <svg class="h-3.5 w-3.5 text-gray-300 transition-transform group-hover:text-gray-500 group-hover:-translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
      {:else}
        <svg class="h-3.5 w-3.5 text-gray-300 transition-transform group-hover:text-gray-500 group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
        </svg>
        <div class="flex items-center gap-2">
          <span class="text-[11px] font-semibold uppercase tracking-widest text-gray-500 group-hover:text-gray-700">{title}</span>
          <div class="h-3.5 w-0.5 rounded-full bg-blue-500"></div>
        </div>
      {/if}
    </button>
    <div class="flex-1 overflow-auto">
      {@render children()}
    </div>
  </div>
{/if}