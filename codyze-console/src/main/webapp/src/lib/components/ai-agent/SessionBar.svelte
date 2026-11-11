<script lang="ts">
  import type { ContextUsageJSON, Model } from '$lib/types';
  import { agentSession } from '$lib/stores/agentSession.svelte';

  interface Props {
    models?: Model[];
    selectedModel?: Model | null;
    onModelSelect?: (model: Model) => void;
    /** How full the context window of the model was in its latest request */
    contextUsage?: ContextUsageJSON | null;
  }

  let { models = [], selectedModel = null, onModelSelect, contextUsage = null }: Props = $props();

  let open = $state<'model' | 'tools' | 'context' | null>(null);
  let filter = $state('');
  let container = $state<HTMLDivElement | undefined>();

  // Lists with more entries get a filter
  const filterFrom = 8;

  function toggle(popover: 'model' | 'tools' | 'context') {
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

  // Escape closes the open popover and nothing else, e.g. not the graph next to the code. The
  // listeners on the window run in the order they were added, which changes when this bar is shown
  // again, so the key is kept from reaching them
  function handleKeydown(event: KeyboardEvent) {
    if (open && event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
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

  // The share of the context window at which the server summarizes the older steps of a run
  const COMPRESSION_SHARE = 1 / 3;

  // Tokens in short, e.g. "58k" or "4.2k"
  function tokens(n: number): string {
    if (n < 1000) return `${n}`;
    return `${(n / 1000).toFixed(n < 10_000 ? 1 : 0)}k`;
  }

  const contextShare = $derived(
    contextUsage ? Math.min(1, contextUsage.usedTokens / contextUsage.windowTokens) : 0
  );

  // The parts of the context in shades of one color, from the largest one down
  const contextShades = [
    'bg-slate-600',
    'bg-slate-500',
    'bg-slate-400',
    'bg-slate-300',
    'bg-slate-200'
  ];
  const contextParts = $derived(
    [...(contextUsage?.categories ?? [])].sort((a, b) => b.tokens - a.tokens)
  );
</script>

<svelte:window onclick={handleWindowClick} />

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

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class="relative flex min-w-0 items-center gap-0.5 text-[11px] text-gray-500"
  bind:this={container}
  onkeydown={handleKeydown}
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

  {#if contextUsage}
    <!-- How full the context window of the model is, with its parts on click -->
    <div class="ml-auto shrink-0">
      <button
        type="button"
        class="flex h-6 items-center gap-1 rounded px-1.5 tabular-nums hover:bg-gray-100 hover:text-gray-800 {open ===
        'context'
          ? 'bg-gray-100 text-gray-800'
          : ''}"
        onclick={() => toggle('context')}
        aria-haspopup="dialog"
        aria-expanded={open === 'context'}
        title="Context of the model: {contextUsage.usedTokens.toLocaleString()} of {contextUsage.windowTokens.toLocaleString()} tokens"
      >
        <svg class="h-3 w-3 shrink-0 -rotate-90" viewBox="0 0 16 16" aria-hidden="true">
          <circle
            cx="8"
            cy="8"
            r="6"
            fill="none"
            stroke="currentColor"
            stroke-opacity="0.25"
            stroke-width="2.5"
          />
          <circle
            cx="8"
            cy="8"
            r="6"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-dasharray="{contextShare * 37.7} 37.7"
          />
        </svg>
        {contextUsage.estimated ? '≈' : ''}{tokens(contextUsage.usedTokens)} / {tokens(
          contextUsage.windowTokens
        )}
      </button>

      {#if open === 'context'}
        <div
          class="absolute right-0 bottom-full z-20 mb-1 w-64 rounded-md border border-gray-200 bg-white p-2.5 shadow-lg"
          role="dialog"
          aria-label="Context of the model"
        >
          <p class="text-xs text-gray-800 tabular-nums">
            {contextUsage.estimated ? 'About ' : ''}{contextUsage.usedTokens.toLocaleString()} of
            {contextUsage.windowTokens.toLocaleString()} tokens ({Math.round(contextShare * 100)}%)
          </p>
          <!-- The parts of the context, with the share at which older steps are summarized -->
          <div class="relative mt-1.5 flex h-1.5 overflow-hidden rounded-full bg-gray-100">
            {#each contextParts as part, i (part.name)}
              <span
                class="h-full {contextShades[i] ?? 'bg-slate-200'}"
                style="width: {(part.tokens / contextUsage.windowTokens) * 100}%"
              ></span>
            {/each}
            <span
              class="absolute inset-y-0 w-px bg-gray-400"
              style="left: {COMPRESSION_SHARE * 100}%"
              aria-hidden="true"
            ></span>
          </div>
          <ul class="mt-2 space-y-0.5">
            {#each contextParts as part, i (part.name)}
              <li class="flex items-center gap-1.5 text-[11px] text-gray-600">
                <span class="h-2 w-2 shrink-0 rounded-sm {contextShades[i] ?? 'bg-slate-200'}"
                ></span>
                <span class="min-w-0 flex-1 truncate">{part.name}</span>
                <span class="shrink-0 text-gray-400 tabular-nums">{tokens(part.tokens)}</span>
              </li>
            {/each}
          </ul>
          <p class="mt-2 text-[11px] leading-snug text-gray-400">
            The split is estimated. Once a third of the window is used, older steps are summarized
            (the line on the bar).
          </p>
        </div>
      {/if}
    </div>
  {/if}
</div>
