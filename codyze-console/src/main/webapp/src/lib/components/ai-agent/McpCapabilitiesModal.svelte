<script lang="ts">
  import TabNavigation from '$lib/components/navigation/TabNavigation.svelte';
  import type { McpCapabilities } from '$lib/types';

  interface Props {
    capabilities: McpCapabilities;
    onClose: () => void;
  }

  let { capabilities, onClose }: Props = $props();

  let activeTab = $state('tools');

  const tabs = $derived([
    { id: 'tools', label: 'Tools', count: capabilities.tools.length },
    { id: 'prompts', label: 'Prompts', count: capabilities.prompts.length },
    { id: 'resources', label: 'Resources', count: capabilities.resources.length }
  ]);

  function getParamEntries(schema: any): { name: string; type: string; required: boolean }[] {
    if (!schema?.properties) return [];
    const required: string[] = schema.required ?? [];
    return Object.entries(schema.properties).map(([name, def]: [string, any]) => ({
      name,
      type: def?.type ?? 'any',
      required: required.includes(name)
    }));
  }

  function isLong(text: string | undefined): boolean {
    return !!text && text.length > 120;
  }

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === e.currentTarget) onClose();
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') onClose();
  }
</script>

<svelte:window onkeydown={handleKeydown} />

<div
  class="fixed inset-0 z-50 flex items-center justify-center bg-black/20 p-4 backdrop-blur-[2px]"
  onclick={handleBackdropClick}
>
  <!-- Modal -->
  <div class="flex w-full max-w-2xl flex-col rounded-2xl bg-white shadow-2xl" style="max-height: 80vh;">
    <!-- Header -->
    <div class="flex flex-shrink-0 items-center justify-between border-b border-gray-200 px-5 py-4">
      <div class="flex items-center gap-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 14.25h13.5m-13.5 0a3 3 0 01-3-3m3 3a3 3 0 100 6h13.5a3 3 0 100-6m-16.5-3a3 3 0 013-3h13.5a3 3 0 013 3m-19.5 0a4.5 4.5 0 01.9-2.7L5.737 5.1a3.375 3.375 0 012.7-1.35h7.126c1.062 0 2.062.5 2.7 1.35l2.587 3.45a4.5 4.5 0 01.9 2.7m0 0a3 3 0 01-3 3m0 3h.008v.008h-.008v-.008zm0-6h.008v.008h-.008v-.008zm-3 6h.008v.008h-.008v-.008zm0-6h.008v.008h-.008v-.008z" />
          </svg>
        </div>
        <div>
          <h2 class="text-sm font-semibold text-gray-900">{capabilities.serverName}</h2>
          {#if capabilities.serverVersion}
            <p class="text-xs text-gray-400">v{capabilities.serverVersion}</p>
          {/if}
        </div>
      </div>
      <button
        class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
        onclick={onClose}
        aria-label="Close"
      >
        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Tabs -->
    <div class="flex-shrink-0 px-5 pt-3">
      <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto px-5 py-3">
      {#if activeTab === 'tools'}
        {#if capabilities.tools.length === 0}
          <p class="py-4 text-center text-sm text-gray-400">No tools registered.</p>
        {:else}
          <ul class="space-y-2">
            {#each capabilities.tools as tool}
              {@const params = getParamEntries(tool.inputSchema)}
              <li class="rounded-xl border border-gray-200 bg-gray-50">
                <details class="group">
                  <summary class="flex cursor-pointer list-none items-start gap-3 px-4 py-3">
                    <!-- chevron -->
                    <svg
                      class="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-gray-400 transition-transform duration-150 group-open:rotate-90"
                      fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                    </svg>
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-2">
                        <span class="font-mono text-xs font-semibold text-gray-900">{tool.name}</span>
                        {#if params.length > 0}
                          <span class="rounded-full bg-gray-200 px-1.5 py-0.5 text-xs text-gray-500">
                            {params.length} param{params.length !== 1 ? 's' : ''}
                          </span>
                        {/if}
                      </div>
                      {#if tool.description}
                        {#if isLong(tool.description)}
                          <p class="mt-1 line-clamp-2 text-xs leading-snug text-gray-500 group-open:line-clamp-none">
                            {tool.description}
                          </p>
                        {:else}
                          <p class="mt-1 text-xs leading-snug text-gray-500">{tool.description}</p>
                        {/if}
                      {/if}
                    </div>
                  </summary>

                  {#if params.length > 0}
                    <div class="border-t border-gray-200 px-4 pb-3 pt-2.5">
                      <p class="mb-2 text-xs font-medium uppercase tracking-wide text-gray-400">Parameters</p>
                      <ul class="space-y-1.5">
                        {#each params as param}
                          <li class="flex items-center gap-2">
                            <span class="font-mono text-xs font-medium text-blue-700">{param.name}</span>
                            <span class="rounded bg-gray-200 px-1 py-0.5 font-mono text-xs text-gray-500">{param.type}</span>
                            {#if param.required}
                              <span class="rounded bg-orange-100 px-1.5 py-0.5 text-xs font-medium text-orange-700">required</span>
                            {/if}
                          </li>
                        {/each}
                      </ul>
                    </div>
                  {/if}
                </details>
              </li>
            {/each}
          </ul>
        {/if}

      {:else if activeTab === 'prompts'}
        {#if capabilities.prompts.length === 0}
          <p class="py-4 text-center text-sm text-gray-400">No prompts registered.</p>
        {:else}
          <ul class="space-y-2">
            {#each capabilities.prompts as prompt}
              <li class="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3">
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs font-semibold text-gray-900">{prompt.name}</span>
                  <span class="rounded-full bg-purple-100 px-1.5 py-0.5 text-xs font-medium text-purple-700">prompt</span>
                </div>
                {#if prompt.description}
                  <p class="mt-1 text-xs leading-snug text-gray-500">{prompt.description}</p>
                {/if}
                {#if prompt.arguments && prompt.arguments.length > 0}
                  <div class="mt-2.5">
                    <p class="mb-1.5 text-xs font-medium uppercase tracking-wide text-gray-400">Arguments</p>
                    <ul class="space-y-1">
                      {#each prompt.arguments as arg}
                        <li class="flex items-center gap-2">
                          <span class="font-mono text-xs font-medium text-blue-700">{arg.name}</span>
                          {#if arg.required}
                            <span class="rounded bg-orange-100 px-1.5 py-0.5 text-xs font-medium text-orange-700">required</span>
                          {:else}
                            <span class="rounded bg-gray-200 px-1.5 py-0.5 text-xs text-gray-500">optional</span>
                          {/if}
                          {#if arg.description}
                            <span class="truncate text-xs text-gray-400">{arg.description}</span>
                          {/if}
                        </li>
                      {/each}
                    </ul>
                  </div>
                {/if}
              </li>
            {/each}
          </ul>
        {/if}

      {:else if activeTab === 'resources'}
        {#if capabilities.resources.length === 0}
          <p class="py-4 text-center text-sm text-gray-400">No resources registered.</p>
        {:else}
          <ul class="space-y-2">
            {#each capabilities.resources as resource}
              <li class="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3">
                <span class="block font-mono text-xs font-semibold text-gray-900">{resource.uri}</span>
                {#if resource.name}
                  <span class="mt-0.5 block text-xs font-medium text-gray-700">{resource.name}</span>
                {/if}
                {#if resource.description}
                  <span class="mt-0.5 block text-xs text-gray-500">{resource.description}</span>
                {/if}
                {#if resource.mimeType}
                  <span class="mt-1.5 inline-block rounded bg-gray-200 px-1.5 py-0.5 font-mono text-xs text-gray-600">{resource.mimeType}</span>
                {/if}
              </li>
            {/each}
          </ul>
        {/if}
      {/if}
    </div>
  </div>
</div>
