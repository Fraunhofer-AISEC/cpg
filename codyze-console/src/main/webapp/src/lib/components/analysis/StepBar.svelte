<script lang="ts">
  import { tick } from 'svelte';

  /**
   * A bar above the code with the steps of a path, e.g. `① source → ② a → ③ sink`, and buttons
   * to step through it. Each step is clickable.
   */
  interface Props {
    /** What the steps are, e.g. "Path" */
    title: string;
    /** The steps, numbered by their position unless they have their own number */
    steps: { label: string; title: string; number?: number }[];
    /** The index of the current step, or -1 if the current location is not on the path */
    index: number;
    onSelect: (index: number) => void;
    onClose: () => void;
    /** The tooltip of the close button */
    closeTitle?: string;
    /** Round number badges (e.g. for a dataflow path) or square ones (e.g. for the agent) */
    shape?: 'round' | 'square';
    /** Switches to another sequence of steps that shares the bar, e.g. from the path to the agent */
    other?: { label: string; onSwitch: () => void };
  }

  let {
    title,
    steps,
    index,
    onSelect,
    onClose,
    closeTitle = 'Close',
    shape = 'round',
    other
  }: Props = $props();

  let stepElements: HTMLButtonElement[] = $state([]);

  // Keep the current step in view when stepping through a long path
  $effect(() => {
    const element = stepElements[index];
    if (element) tick().then(() => element.scrollIntoView({ block: 'nearest', inline: 'nearest' }));
  });

  const previous = $derived(index < 0 ? steps.length - 1 : index - 1);
  const next = $derived(index < 0 ? 0 : index + 1);
</script>

<div
  class="flex h-8 shrink-0 items-center gap-2 border-b border-gray-200 bg-gray-50 px-3 text-xs"
  role="toolbar"
  aria-label={title}
>
  <span class="shrink-0 font-semibold text-gray-500">{title}</span>
  {#if other}
    <button
      type="button"
      class="shrink-0 rounded px-1 text-[11px] text-gray-400 hover:bg-gray-200 hover:text-gray-800"
      title="Show the steps of the {other.label.toLowerCase()} instead"
      onclick={other.onSwitch}
    >
      ⇄ {other.label}
    </button>
  {/if}
  <ol class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto whitespace-nowrap">
    {#each steps as step, i (i)}
      {#if i > 0}
        <li class="shrink-0 text-gray-300" aria-hidden="true">→</li>
      {/if}
      <li class="shrink-0">
        <button
          type="button"
          bind:this={stepElements[i]}
          class="flex items-center gap-1 rounded border px-1.5 py-px font-mono text-[11px] {i ===
          index
            ? 'border-blue-500 bg-white text-gray-900'
            : 'border-transparent text-gray-600 hover:border-gray-300 hover:bg-white'}"
          title={step.title}
          aria-current={i === index ? 'step' : undefined}
          onclick={() => onSelect(i)}
        >
          <span
            class="flex h-3.5 min-w-3.5 items-center justify-center {shape === 'round'
              ? 'rounded-full'
              : 'rounded-sm'} bg-slate-700 px-0.5 font-sans text-[9px] font-semibold text-white"
            >{step.number ?? i + 1}</span
          >
          <span class="max-w-40 truncate">{step.label}</span>
        </button>
      </li>
    {/each}
  </ol>
  <div class="flex shrink-0 items-center text-gray-500">
    <button
      type="button"
      class="rounded px-1 leading-5 hover:bg-gray-200 hover:text-gray-900 disabled:text-gray-300 disabled:hover:bg-transparent"
      disabled={previous < 0}
      onclick={() => onSelect(previous)}
      aria-label="Previous step"
      title="Previous step"
    >
      ◀
    </button>
    <span class="min-w-8 text-center tabular-nums"
      >{index < 0 ? '–' : index + 1}/{steps.length}</span
    >
    <button
      type="button"
      class="rounded px-1 leading-5 hover:bg-gray-200 hover:text-gray-900 disabled:text-gray-300 disabled:hover:bg-transparent"
      disabled={next >= steps.length}
      onclick={() => onSelect(next)}
      aria-label="Next step"
      title="Next step"
    >
      ▶
    </button>
    <button
      type="button"
      class="ml-1 rounded px-1 leading-5 hover:bg-gray-200 hover:text-gray-900"
      onclick={onClose}
      aria-label={closeTitle}
      title={closeTitle}
    >
      ×
    </button>
  </div>
</div>
