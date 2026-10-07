<script lang="ts">
  import type { FindingsJSON, RequirementJSON, RequirementsCategoryJSON } from '$lib/types';
  import { queryTreeStatusConfigs, type QueryTreeStatus } from '$lib/types';
  import { SvelteSet } from 'svelte/reactivity';

  /**
   * The requirements of the project with their status and the places they were checked at. The
   * places that violate a requirement come first; clicking one shows it in the code.
   */
  interface Props {
    categories: RequirementsCategoryJSON[];
    findings: FindingsJSON[];
    /** The name of the file of a finding, e.g. its path in the component */
    fileOf: (finding: FindingsJSON) => string;
    onOpen: (finding: FindingsJSON) => void;
    /** Asks the agent why a requirement is violated, at a place if given */
    onAsk: (requirement: RequirementJSON, finding?: FindingsJSON) => void;
  }

  let { categories, findings, fileOf, onOpen, onAsk }: Props = $props();

  const requirements = $derived(categories.flatMap((c) => c.requirements));
  const violated = $derived(requirements.filter((r) => r.status === 'NOT_FULFILLED').length);

  // The findings of each requirement, the places that violate it first
  const findingsOf = $derived.by(() => {
    const byRule: Record<string, FindingsJSON[]> = {};
    for (const finding of findings) {
      if (finding.rule) (byRule[finding.rule] ??= []).push(finding);
    }
    for (const list of Object.values(byRule)) {
      list.sort(
        (a, b) => Number(isViolation(b)) - Number(isViolation(a)) || a.startLine - b.startLine
      );
    }
    return byRule;
  });

  function isViolation(finding: FindingsJSON) {
    return finding.kind.toLowerCase() === 'fail';
  }

  function statusOf(requirement: RequirementJSON) {
    return (
      queryTreeStatusConfigs[requirement.status as QueryTreeStatus] ??
      queryTreeStatusConfigs.NOT_YET_EVALUATED
    );
  }

  // Requirements that are not fulfilled are expanded at first
  const collapsed = new SvelteSet<string>();
  const expanded = new SvelteSet<string>();
  const isExpanded = (r: RequirementJSON) =>
    expanded.has(r.id) || (r.status !== 'FULFILLED' && !collapsed.has(r.id));

  function toggle(r: RequirementJSON) {
    if (isExpanded(r)) {
      expanded.delete(r.id);
      collapsed.add(r.id);
    } else {
      collapsed.delete(r.id);
      expanded.add(r.id);
    }
  }

  // The places that fulfil a requirement are only listed on request
  const passedShown = new SvelteSet<string>();
</script>

<div class="flex h-full min-h-0 flex-col bg-[#fbfbfc]">
  <div
    class="flex h-9 shrink-0 items-center justify-between gap-2 border-b border-gray-100 pr-2.5 pl-3"
  >
    <span class="text-[11px] font-semibold tracking-wider text-gray-500">FINDINGS</span>
    {#if requirements.length > 0}
      <span class="text-[11px] text-gray-500">{violated} of {requirements.length} violated</span>
    {/if}
  </div>

  <div class="min-h-0 flex-1 overflow-y-auto py-1 text-[12.5px] text-gray-700">
    {#each categories as category (category.id)}
      {#if categories.length > 1}
        <p
          class="px-3 pt-2 pb-0.5 text-[10px] font-semibold tracking-wider text-gray-400 uppercase"
          title={category.description}
        >
          {category.name}
        </p>
      {/if}
      {#each category.requirements as requirement (requirement.id)}
        {@const status = statusOf(requirement)}
        {@const all = findingsOf[requirement.id] ?? []}
        {@const violations = all.filter(isViolation)}
        {@const passed = all.length - violations.length}
        {@const open = isExpanded(requirement)}
        <div class="group/req">
          <button
            type="button"
            class="flex h-[26px] w-full items-center gap-1.5 pr-2 pl-2 text-left hover:bg-gray-100"
            aria-expanded={open}
            title="{requirement.id}: {requirement.description}"
            onclick={() => toggle(requirement)}
          >
            <svg
              class="h-3 w-3 shrink-0 text-gray-400 transition-transform {open ? 'rotate-90' : ''}"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              stroke-width="2.5"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
            </svg>
            <span class="w-3 shrink-0 text-center font-semibold {status.textColor}"
              >{status.icon}</span
            >
            <span class="min-w-0 flex-1 truncate">{requirement.name}</span>
            {#if violations.length > 0}
              <span class="shrink-0 text-[10.5px] text-red-700 tabular-nums"
                >{violations.length}</span
              >
            {/if}
          </button>

          {#if open}
            <div class="pb-1">
              {#each violations as finding, i (i)}
                <div class="group/finding flex h-[22px] items-center pr-1.5 pl-7 hover:bg-gray-100">
                  <button
                    type="button"
                    class="flex min-w-0 flex-1 items-center gap-1.5 text-left"
                    title="Show it in the code"
                    onclick={() => onOpen(finding)}
                  >
                    <span class="h-1.5 w-1.5 shrink-0 rounded-full bg-amber-500"></span>
                    <span class="min-w-0 truncate font-mono text-[11.5px]"
                      >{fileOf(finding)}:{finding.startLine}</span
                    >
                  </button>
                  <button
                    type="button"
                    class="shrink-0 rounded px-1 text-[11px] text-gray-400 opacity-0 group-hover/finding:opacity-100 hover:bg-gray-200 hover:text-gray-800 focus:opacity-100"
                    title="Ask the agent why the requirement is violated here"
                    onclick={() => onAsk(requirement, finding)}
                  >
                    Ask
                  </button>
                </div>
              {/each}

              {#if passed > 0}
                {#if passedShown.has(requirement.id)}
                  {#each all.filter((f) => !isViolation(f)) as finding, i (i)}
                    <button
                      type="button"
                      class="flex h-[22px] w-full items-center gap-1.5 pr-2 pl-7 text-left text-gray-500 hover:bg-gray-100"
                      title="{finding.kind}: show it in the code"
                      onclick={() => onOpen(finding)}
                    >
                      <span class="h-1.5 w-1.5 shrink-0 rounded-full bg-gray-300"></span>
                      <span class="min-w-0 truncate font-mono text-[11.5px]"
                        >{fileOf(finding)}:{finding.startLine}</span
                      >
                    </button>
                  {/each}
                {:else}
                  <button
                    type="button"
                    class="flex h-[22px] w-full items-center pl-7 text-left text-[11px] text-gray-400 hover:text-gray-700"
                    onclick={() => passedShown.add(requirement.id)}
                  >
                    {passed} checked {passed === 1 ? 'place' : 'places'} without violation
                  </button>
                {/if}
              {/if}

              <div class="flex items-center gap-2 pt-0.5 pl-7 text-[11px]">
                <!-- The requirement page is a fixed route -->
                <!-- eslint-disable svelte/no-navigation-without-resolve -->
                <a
                  href="/requirements/{encodeURIComponent(requirement.id)}"
                  class="text-gray-400 hover:text-gray-800"
                  title="Open the requirement with its query tree">Query tree ↗</a
                >
                <!-- eslint-enable svelte/no-navigation-without-resolve -->
                {#if requirement.status !== 'FULFILLED'}
                  <button
                    type="button"
                    class="text-gray-400 hover:text-gray-800"
                    title="Ask the agent why the requirement is not fulfilled"
                    onclick={() => onAsk(requirement)}
                  >
                    Ask agent
                  </button>
                {/if}
              </div>
            </div>
          {/if}
        </div>
      {/each}
    {:else}
      <p class="px-3 py-2 text-xs text-gray-400">The project has no requirements.</p>
    {/each}
  </div>
</div>
