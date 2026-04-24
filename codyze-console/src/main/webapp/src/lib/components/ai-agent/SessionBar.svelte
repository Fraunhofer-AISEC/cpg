<script lang="ts">
  import type { Model } from '$lib/types';
  import { agentSession } from '$lib/stores/agentSession.svelte';

  interface Props {
    models?: Model[];
    selectedModel?: Model | null;
    onModelSelect?: (model: Model) => void;
  }

  let {
    models = [],
    selectedModel = null,
    onModelSelect
  }: Props = $props();

  let menuOpen = $state(false);
  let menuContainer = $state<HTMLDivElement | undefined>();

  function selectModel(model: Model) {
    onModelSelect?.(model);
    menuOpen = false;
  }

  function handleDocumentClick(event: MouseEvent) {
    if (!menuOpen) return;
    if (menuContainer && !menuContainer.contains(event.target as Node)) {
      menuOpen = false;
    }
  }

  let modelsByProvider = $derived.by(() => {
    const grouped = new Map<string, Model[]>();
    for (const model of models) {
      const section = grouped.get(model.client);
      if (section) {
        section.push(model);
      } else {
        grouped.set(model.client, [model]);
      }
    }
    return [...grouped.entries()];
  });
</script>

<svelte:window onclick={handleDocumentClick} />

<div class="flex items-center gap-1.5">
  {#if models.length === 0}
    <div
      class="flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700"
      title="No LLM provider is available. Configure a client in application.conf (and set the matching API key env variable for providers that need one)."
    >
      <svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m0-6.75a9 9 0 100 18 9 9 0 000-18zm0 12.75h.008v.008H12v-.008z" />
      </svg>
      <span>No LLM provider configured</span>
    </div>
  {:else}
    <div class="relative" bind:this={menuContainer}>
      <button
        type="button"
        class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
        onclick={() => (menuOpen = !menuOpen)}
        aria-haspopup="listbox"
        aria-expanded={menuOpen}
        title="Model"
      >
        <span class="font-mono text-gray-700">{selectedModel?.model ?? 'No model'}</span>
        <svg class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {#if menuOpen}
        <div class="absolute bottom-full left-0 z-10 mb-2 w-72 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xl">
          <div class="border-b border-gray-100 px-3 py-2">
            <p class="text-xs font-medium text-gray-500">Available models</p>
          </div>

          <div class="max-h-80 overflow-y-auto py-1" role="listbox">
            {#each modelsByProvider as [provider, providerModels]}
              <div class="px-3 pt-2 pb-1">
                <p class="text-[10px] font-semibold tracking-wider text-gray-400 uppercase">{provider}</p>
              </div>
              {#each providerModels as model}
                {@const isSelected = selectedModel?.client === model.client && selectedModel?.model === model.model}
                <button
                  type="button"
                  class="flex w-full items-center justify-between gap-3 px-3 py-2 text-left transition-colors hover:bg-gray-50 {isSelected ? 'bg-blue-50' : ''}"
                  onclick={() => selectModel(model)}
                >
                  <span class="font-mono text-sm {isSelected ? 'font-semibold text-blue-700' : 'text-gray-800'}">{model.model}</span>
                  {#if isSelected}
                    <svg class="h-3.5 w-3.5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  {/if}
                </button>
              {/each}
            {/each}
          </div>
        </div>
      {/if}
    </div>
  {/if}

  {#if agentSession.mcpCapabilities}
    <button
      class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
      onclick={agentSession.openMcpModal}
      title="MCP Server"
    >
      <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
      {agentSession.mcpCapabilities.serverName}
    </button>
  {/if}

  {#if agentSession.skills.length > 0}
    <button
      class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
      onclick={agentSession.openSkillsModal}
      title="Skills"
    >
      <svg class="h-3 w-3 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
      {agentSession.skills.length} skill{agentSession.skills.length === 1 ? '' : 's'}
    </button>
  {/if}
</div>