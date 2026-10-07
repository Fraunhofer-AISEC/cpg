<script lang="ts" module>
  /** An action of the command palette. */
  export interface PaletteCommand {
    id: string;
    label: string;
    /** E.g. `Layers` or `Navigation`, shown before the label */
    category: string;
    /** The keyboard shortcut, if there is one */
    shortcut?: string;
    /** Why the command cannot run right now, e.g. because no node is selected */
    disabledReason?: string;
    run: () => void;
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';
  import type { TranslationUnitJSON } from '$lib/types';
  import { fuzzyMatch } from '$lib/utils/fuzzy';

  /**
   * A quick open for files and a palette for commands in one dialog: the query searches the files,
   * and a query starting with `>` searches the commands.
   */
  interface Props {
    /** The query; set it to `''` to open the files or to `'>'` to open the commands, null closes */
    query: string | null;
    /** The files to open, with their path relative to the component */
    files: { unit: TranslationUnitJSON; path: string }[];
    /** The IDs of recently used files, most recent first, which are listed first */
    recentIds?: string[];
    commands: PaletteCommand[];
    onOpenFile: (unit: TranslationUnitJSON) => void;
  }

  let { query = $bindable(), files, recentIds = [], commands, onOpenFile }: Props = $props();

  const maxResults = 50;

  const isCommands = $derived(query?.startsWith('>') ?? false);
  const text = $derived((isCommands ? query?.slice(1) : query)?.trim() ?? '');

  interface Result {
    key: string;
    /** The parts of the main text, with the matched characters marked */
    title: { text: string; matched: boolean }[];
    detail: { text: string; matched: boolean }[];
    shortcut?: string;
    disabledReason?: string;
    run: () => void;
  }

  // Splits a text into parts with and without matched characters, for highlighting
  function highlight(value: string, indices: Set<number>, offset = 0) {
    const parts: { text: string; matched: boolean }[] = [];
    for (let i = 0; i < value.length; i++) {
      const matched = indices.has(i + offset);
      const last = parts.at(-1);
      if (last && last.matched === matched) last.text += value[i];
      else parts.push({ text: value[i], matched });
    }
    return parts;
  }

  const results = $derived.by((): Result[] => {
    if (query === null) return [];
    if (isCommands) {
      const scored = commands.flatMap((command) => {
        const label = `${command.category}: ${command.label}`;
        const match = fuzzyMatch(text, label);
        return match ? [{ command, label, match }] : [];
      });
      if (text) scored.sort((a, b) => b.match.score - a.match.score);
      return scored.slice(0, maxResults).map(({ command, label, match }) => ({
        key: command.id,
        title: highlight(label, new Set(match.indices)),
        detail: [],
        shortcut: command.shortcut,
        disabledReason: command.disabledReason,
        run: command.run
      }));
    }

    const recent = new Map(recentIds.map((id, i) => [id, i]));
    const scored = files.flatMap((file) => {
      const match = fuzzyMatch(text, file.path);
      if (!match) return [];
      const nameStart = file.path.length - file.unit.name.length;
      // Matches in the file name are better than matches in the folders
      const inName = match.indices.length > 0 && match.indices.every((i) => i >= nameStart);
      return [{ file, match, nameStart, score: match.score + (inName ? 10 : 0) }];
    });
    scored.sort((a, b) => {
      if (!text) {
        const ra = recent.get(a.file.unit.id) ?? Infinity;
        const rb = recent.get(b.file.unit.id) ?? Infinity;
        return ra - rb || a.file.path.localeCompare(b.file.path);
      }
      return b.score - a.score;
    });
    return scored.slice(0, maxResults).map(({ file, match, nameStart }) => {
      const indices = new Set(match.indices);
      return {
        key: file.unit.id,
        title: highlight(file.unit.name, indices, nameStart),
        detail: highlight(file.path.slice(0, nameStart).replace(/\/$/, ''), indices),
        run: () => onOpenFile(file.unit)
      };
    });
  });

  let active = $state(0);
  let input = $state<HTMLInputElement>();
  let list = $state<HTMLElement>();

  // Start at the top whenever the results change
  $effect(() => {
    void results;
    active = 0;
  });

  // Focus the input when the palette opens
  const isOpen = $derived(query !== null);
  $effect(() => {
    if (isOpen) tick().then(() => input?.focus());
  });

  function close() {
    query = null;
  }

  function choose(result: Result | undefined) {
    if (!result || result.disabledReason) return;
    close();
    result.run();
  }

  function move(delta: number) {
    if (results.length === 0) return;
    active = (active + delta + results.length) % results.length;
    tick().then(() =>
      list?.querySelector(`[data-index="${active}"]`)?.scrollIntoView({ block: 'nearest' })
    );
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      move(1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      move(-1);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      choose(results[active]);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      close();
    }
  }
</script>

{#if query !== null}
  <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
  <div class="fixed inset-0 z-50 flex justify-center bg-gray-900/10 pt-[12vh]" onclick={close}>
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div
      class="flex h-fit max-h-[60vh] w-[36rem] max-w-[calc(100vw-2rem)] flex-col overflow-hidden rounded-lg border border-gray-200 bg-white shadow-xl"
      onclick={(e) => e.stopPropagation()}
      role="dialog"
      tabindex="-1"
      aria-label={isCommands ? 'Command palette' : 'Open file'}
    >
      <input
        bind:this={input}
        bind:value={query}
        class="w-full border-0 border-b border-gray-200 px-3 py-2 text-sm focus:ring-0"
        placeholder={isCommands ? 'Run a command' : 'Open a file by name (type > for commands)'}
        onkeydown={handleKeydown}
        aria-label={isCommands ? 'Command' : 'File name'}
        role="combobox"
        aria-expanded="true"
        aria-controls="palette-results"
        aria-activedescendant={results[active] ? `palette-result-${active}` : undefined}
      />
      <ul id="palette-results" class="min-h-0 overflow-y-auto py-1" role="listbox" bind:this={list}>
        {#each results as result, i (result.key)}
          <li
            id="palette-result-{i}"
            data-index={i}
            role="option"
            aria-selected={i === active}
            aria-disabled={!!result.disabledReason}
            class="flex cursor-pointer items-baseline gap-2 px-3 py-1 text-xs {i === active
              ? 'bg-blue-50'
              : ''} {result.disabledReason ? 'text-gray-400' : 'text-gray-800'}"
            onmousemove={() => (active = i)}
            onclick={() => choose(result)}
            title={result.disabledReason}
          >
            <span class="shrink-0 {isCommands ? '' : 'font-mono'}">
              {#each result.title as part, j (j)}
                <span class={part.matched ? 'font-semibold text-blue-700' : ''}>{part.text}</span>
              {/each}
            </span>
            <span class="min-w-0 truncate font-mono text-[11px] text-gray-400">
              {#each result.detail as part, j (j)}
                <span class={part.matched ? 'font-semibold text-blue-700' : ''}>{part.text}</span>
              {/each}
              {#if result.disabledReason}
                <span class="font-sans">{result.disabledReason}</span>
              {/if}
            </span>
            {#if result.shortcut}
              <kbd
                class="ml-auto shrink-0 rounded border border-gray-200 bg-gray-50 px-1 font-sans text-[10px] text-gray-500"
                >{result.shortcut}</kbd
              >
            {/if}
          </li>
        {:else}
          <li class="px-3 py-2 text-xs text-gray-400">
            {isCommands ? 'No matching commands' : 'No matching files'}
          </li>
        {/each}
      </ul>
    </div>
  </div>
{/if}
