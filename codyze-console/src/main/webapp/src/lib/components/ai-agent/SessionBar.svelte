<script lang="ts">
  import type { Model } from '$lib/types';
  import { agentSession } from '$lib/stores/agentSession.svelte';

  interface Props {
    models?: Model[];
    selectedModel?: Model | null;
    onModelSelect?: (model: Model) => void;
  }

  let { models = [], selectedModel = null, onModelSelect }: Props = $props();

  let open = $state<'model' | 'tools' | null>(null);
  let filter = $state('');
  let container = $state<HTMLDivElement | undefined>();

  // Lists with more entries get a filter
  const filterFrom = 8;

  function toggle(popover: 'model' | 'tools') {
    open = open === popover ? null : popover;
    filter = '';
  }

  function selectModel(model: Model) {
    onModelSelect?.(model);
    open = null;
  }

  function scrollIntoViewWhenOpen(node: HTMLButtonElement) {
    node.scrollIntoView({ block: 'nearest' });
  }

  function handleWindowClick(event: MouseEvent) {
    if (open && container && !container.contains(event.target as Node)) open = null;
  }

  function handleWindowKeydown(event: KeyboardEvent) {
    if (open && event.key === 'Escape') {
      event.preventDefault();
      open = null;
    }
  }

  const query = $derived(filter.trim().toLowerCase());

  const modelsByProvider = $derived.by(() => {
    const grouped: [string, Model[]][] = [];
    for (const model of models) {
      if (query && !`${model.client} ${model.model}`.toLowerCase().includes(query)) continue;
      const section = grouped.find(([provider]) => provider === model.client);
      if (section) section[1].push(model);
      else grouped.push([model.client, [model]]);
    }
    return grouped;
  });

  const server = $derived(agentSession.mcpCapabilities);
  const skills = $derived(
    agentSession.skills.filter((s) => !query || s.name.toLowerCase().includes(query))
  );
  const toolsEntries = $derived((server ? 1 : 0) + agentSession.skills.length);
</script>

<svelte:window onclick={handleWindowClick} onkeydown={handleWindowKeydown} />

{#snippet filterInput(placeholder: string)}
  <div class="border-b border-gray-100 p-1.5">
    <!-- svelte-ignore a11y_autofocus -->
    <input
      type="search"
      class="h-7 w-full rounded-md border border-gray-200 bg-white px-2 text-xs text-gray-900 placeholder-gray-400"
      {placeholder}
      aria-label={placeholder}
      bind:value={filter}
      autofocus
    />
  </div>
{/snippet}

{#snippet sectionTitle(title: string)}
  <p class="px-2.5 pt-1.5 pb-0.5 text-[10px] font-semibold tracking-wider text-gray-400 uppercase">
    {title}
  </p>
{/snippet}

<div
  class="relative flex min-w-0 items-center gap-0.5 text-[11px] text-gray-500"
  bind:this={container}
>
  {#if models.length === 0}
    <span
      class="flex h-6 items-center gap-1 rounded px-1.5 text-amber-700"
      title="No LLM provider is available. Configure a client in application.conf (and set the matching API key env variable for providers that need one)."
    >
      <span class="h-1.5 w-1.5 rounded-full bg-amber-500"></span>
      No LLM provider configured
    </span>
  {:else}
    <div class="min-w-0">
      <button
        type="button"
        class="flex h-6 max-w-full min-w-0 items-center gap-1 rounded px-1.5 hover:bg-gray-100 hover:text-gray-800 {open ===
        'model'
          ? 'bg-gray-100 text-gray-800'
          : ''}"
        onclick={() => toggle('model')}
        aria-haspopup="listbox"
        aria-expanded={open === 'model'}
        title="Model: {selectedModel ? `${selectedModel.client} · ${selectedModel.model}` : 'none'}"
      >
        <span class="min-w-0 truncate font-mono text-gray-700"
          >{selectedModel?.model ?? 'No model'}</span
        >
        <svg
          class="h-3 w-3 shrink-0"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {#if open === 'model'}
        <div
          class="absolute bottom-full left-0 z-20 mb-1 w-72 overflow-hidden rounded-md border border-gray-200 bg-white shadow-lg"
        >
          {#if models.length >= filterFrom}
            {@render filterInput('Filter models')}
          {/if}
          <div class="max-h-80 overflow-y-auto pb-1" role="listbox" aria-label="Models">
            {#each modelsByProvider as [provider, providerModels] (provider)}
              {@render sectionTitle(provider)}
              {#each providerModels as model (model.model)}
                {@const isSelected =
                  selectedModel?.client === model.client && selectedModel?.model === model.model}
                <button
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  {@attach isSelected ? scrollIntoViewWhenOpen : () => {}}
                  class="flex w-full items-center justify-between gap-3 px-2.5 py-1 text-left hover:bg-gray-50 {isSelected
                    ? 'bg-blue-50'
                    : ''}"
                  onclick={() => selectModel(model)}
                >
                  <span
                    class="truncate font-mono text-xs {isSelected
                      ? 'font-semibold text-blue-700'
                      : 'text-gray-800'}">{model.model}</span
                  >
                  {#if isSelected}
                    <svg
                      class="h-3 w-3 shrink-0 text-blue-600"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2.5"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  {/if}
                </button>
              {/each}
            {:else}
              <p class="px-2.5 py-2 text-xs text-gray-400">No matching models</p>
            {/each}
          </div>
        </div>
      {/if}
    </div>
  {/if}

  <div class="shrink-0">
    <button
      type="button"
      class="flex h-6 items-center gap-1 rounded px-1.5 hover:bg-gray-100 hover:text-gray-800 {open ===
      'tools'
        ? 'bg-gray-100 text-gray-800'
        : ''}"
      onclick={() => toggle('tools')}
      aria-haspopup="dialog"
      aria-expanded={open === 'tools'}
      title={server
        ? 'What the agent can use: MCP servers and skills'
        : 'The MCP server is not available'}
    >
      Tools
      {#if !server}
        <!-- Only shown when something is wrong -->
        <span class="h-1.5 w-1.5 rounded-full bg-amber-500"></span>
      {/if}
    </button>

    {#if open === 'tools'}
      <div
        class="absolute bottom-full left-0 z-20 mb-1 w-72 overflow-hidden rounded-md border border-gray-200 bg-white shadow-lg"
        role="dialog"
        aria-label="Tools of the agent"
      >
        {#if toolsEntries >= filterFrom}
          {@render filterInput('Filter servers and skills')}
        {/if}
        <div class="max-h-80 overflow-y-auto pb-1">
          {@render sectionTitle('MCP servers')}
          {#if server}
            {#if !query || server.serverName.toLowerCase().includes(query)}
              <button
                type="button"
                class="flex w-full items-center gap-2 px-2.5 py-1 text-left hover:bg-gray-50"
                title="Show the tools, prompts and resources of the server"
                onclick={() => {
                  open = null;
                  agentSession.openMcpModal();
                }}
              >
                <span class="min-w-0 flex-1 truncate font-mono text-xs text-gray-800"
                  >{server.serverName}</span
                >
                <span class="shrink-0 text-[11px] text-gray-400 tabular-nums"
                  >{server.tools.length} tools</span
                >
              </button>
            {/if}
          {:else}
            <p class="flex items-center gap-2 px-2.5 py-1 text-xs text-amber-700">
              <span class="h-1.5 w-1.5 shrink-0 rounded-full bg-amber-500"></span>
              Not available
            </p>
          {/if}

          {@render sectionTitle('Skills')}
          {#each skills as skill (skill.name)}
            <button
              type="button"
              class="flex w-full px-2.5 py-1 text-left hover:bg-gray-50"
              title="Show the skill"
              onclick={() => {
                open = null;
                agentSession.openSkillsModal();
              }}
            >
              <span class="truncate font-mono text-xs text-gray-800">{skill.name}</span>
            </button>
          {:else}
            <p class="px-2.5 py-1 text-xs text-gray-400">
              {query ? 'No matching skills' : 'No skills loaded'}
            </p>
          {/each}
        </div>
      </div>
    {/if}
  </div>
</div>
