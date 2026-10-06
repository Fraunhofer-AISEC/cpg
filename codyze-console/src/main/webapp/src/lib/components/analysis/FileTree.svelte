<script lang="ts">
  import { SvelteSet } from 'svelte/reactivity';
  import { goto } from '$app/navigation';
  import type { ComponentJSON, TranslationUnitJSON } from '$lib/types';
  import { CollapsiblePanel } from '$lib/components/ui';

  interface TreeNode {
    /** The displayed name; for compacted folders a chain like `src/main/kotlin` */
    name: string;
    type: 'folder' | 'file';
    /** The full relative path, which identifies the node */
    path: string;
    unit?: TranslationUnitJSON;
    children: TreeNode[];
  }

  /** A visible row of the tree */
  interface Row {
    node: TreeNode;
    depth: number;
  }

  interface Props {
    component: ComponentJSON;
    currentUnitId?: string;
    allComponents?: ComponentJSON[];
    onFileSelect?: (unit: TranslationUnitJSON) => void;
    fileHref?: (unit: TranslationUnitJSON) => string;
    componentHref?: (componentName: string) => string;
    /** Called when another component is selected; used instead of links if given */
    onComponentSelect?: (componentName: string) => void;
    collapsed?: boolean;
    width?: string;
    /** The units with concept suggestions, marked with a dot */
    conceptSuggestions?: Set<string>;
    /** The number of concepts and operations per unit ID, shown right-aligned */
    conceptCounts?: Map<string, number>;
    /** Renders the tree without its own collapsible panel, filling the parent (e.g. a sidebar) */
    embedded?: boolean;
  }

  let {
    component,
    currentUnitId,
    allComponents,
    onFileSelect,
    fileHref,
    componentHref,
    onComponentSelect,
    collapsed = $bindable(false),
    width = 'w-56',
    conceptSuggestions = new Set(),
    conceptCounts,
    embedded = false
  }: Props = $props();

  // Pixels per level of indentation, and the left padding of the first level
  const indent = 12;
  const padding = 6;

  function relativePath(unit: TranslationUnitJSON): string {
    let path = unit.path;
    if (path.startsWith('file:')) path = path.slice(5);
    const topLevel = component.topLevel;
    if (topLevel && path.startsWith(topLevel)) {
      path = path.slice(topLevel.length);
      if (path.startsWith('/')) path = path.slice(1);
    }
    return path || unit.name || 'root';
  }

  function sortNodes(nodes: TreeNode[]) {
    // Folders first, then alphabetically, like in most editors
    nodes.sort((a, b) =>
      a.type !== b.type ? (a.type === 'folder' ? -1 : 1) : a.name.localeCompare(b.name)
    );
    for (const node of nodes) sortNodes(node.children);
  }

  // Merges chains of folders that only contain a single folder into one node (`src/main/kotlin`)
  function compact(nodes: TreeNode[]): TreeNode[] {
    return nodes.map((node) => {
      let current = node;
      while (
        current.type === 'folder' &&
        current.children.length === 1 &&
        current.children[0].type === 'folder'
      ) {
        const child = current.children[0];
        current = { ...child, name: `${current.name}/${child.name}` };
      }
      return { ...current, children: compact(current.children) };
    });
  }

  function buildFileTree(units: TranslationUnitJSON[]): TreeNode[] {
    const root: TreeNode[] = [];
    for (const unit of units) {
      const parts = relativePath(unit)
        .split('/')
        .filter((p) => p.length > 0);
      let level = root;
      let path = '';
      parts.forEach((part, index) => {
        path += (path ? '/' : '') + part;
        const isFile = index === parts.length - 1;
        let node = level.find((n) => n.name === part && n.type === (isFile ? 'file' : 'folder'));
        if (!node) {
          node = {
            name: part,
            type: isFile ? 'file' : 'folder',
            path,
            unit: isFile ? unit : undefined,
            children: []
          };
          level.push(node);
        }
        level = node.children;
      });
    }
    sortNodes(root);
    return compact(root);
  }

  const fileTree = $derived(buildFileTree(component.translationUnits));

  // Folders are expanded by default, so only the collapsed ones are tracked
  const collapsedFolders = new SvelteSet<string>();

  function toggleFolder(path: string) {
    if (collapsedFolders.has(path)) collapsedFolders.delete(path);
    else collapsedFolders.add(path);
  }

  let filter = $state('');
  const filterText = $derived(filter.trim().toLowerCase());

  // Keeps the files whose path matches the filter, and the folders containing them
  function filterTree(nodes: TreeNode[], text: string): TreeNode[] {
    const result: TreeNode[] = [];
    for (const node of nodes) {
      if (node.type === 'file') {
        if (node.path.toLowerCase().includes(text)) result.push(node);
      } else {
        const children = filterTree(node.children, text);
        if (children.length > 0) result.push({ ...node, children });
      }
    }
    return result;
  }

  const visibleTree = $derived(filterText ? filterTree(fileTree, filterText) : fileTree);

  // The tree flattened into the visible rows. While filtering, all folders are expanded
  const rows = $derived.by(() => {
    const result: Row[] = [];
    const visit = (nodes: TreeNode[], depth: number) => {
      for (const node of nodes) {
        result.push({ node, depth });
        if (node.type === 'folder' && (filterText || !collapsedFolders.has(node.path))) {
          visit(node.children, depth + 1);
        }
      }
    };
    visit(visibleTree, 0);
    return result;
  });

  // Folders containing files with concept suggestions, so collapsed folders can show them too
  const foldersWithSuggestions = $derived.by(() => {
    const folders: string[] = [];
    const visit = (node: TreeNode): boolean => {
      if (node.type === 'file') return !!node.unit && conceptSuggestions.has(node.unit.id);
      let has = false;
      for (const child of node.children) has = visit(child) || has;
      if (has) folders.push(node.path);
      return has;
    };
    fileTree.forEach(visit);
    return new Set(folders);
  });

  function hasSuggestions(node: TreeNode): boolean {
    return node.type === 'file'
      ? !!node.unit && conceptSuggestions.has(node.unit.id)
      : foldersWithSuggestions.has(node.path);
  }

  // The number of concepts per folder path, summed over all files in it
  const folderConceptCounts = $derived.by(() => {
    const counts: Record<string, number> = {};
    if (!conceptCounts) return counts;
    const visit = (node: TreeNode): number => {
      if (node.type === 'file') return node.unit ? (conceptCounts.get(node.unit.id) ?? 0) : 0;
      let sum = 0;
      for (const child of node.children) sum += visit(child);
      counts[node.path] = sum;
      return sum;
    };
    fileTree.forEach(visit);
    return counts;
  });

  // Files always show their count; expanded folders do not, since their files show it
  function conceptCount(node: TreeNode): number {
    if (node.type === 'file') return node.unit ? (conceptCounts?.get(node.unit.id) ?? 0) : 0;
    const expanded = filterText || !collapsedFolders.has(node.path);
    return expanded ? 0 : (folderConceptCounts[node.path] ?? 0);
  }

  function selectComponent(name: string) {
    if (name === component.name) return;
    if (onComponentSelect) {
      onComponentSelect(name);
    } else if (componentHref) {
      // The href is built by the caller
      // eslint-disable-next-line svelte/no-navigation-without-resolve
      goto(componentHref(name));
    }
  }

  const rowClass =
    'relative flex h-[22px] w-full items-center gap-1 pr-2 text-left text-xs whitespace-nowrap';
</script>

{#snippet rowContent(row: Row, selected: boolean)}
  <!-- Indent guides, one per ancestor level -->
  {#each [...Array(row.depth).keys()] as level (level)}
    <span
      class="pointer-events-none absolute top-0 bottom-0 w-px bg-gray-200"
      style:left="{padding + level * indent + 7}px"
    ></span>
  {/each}
  {#if selected}
    <span class="absolute top-0 bottom-0 left-0 w-0.5 bg-blue-500"></span>
  {/if}
  <!-- Fixed-width chevron column, so that files and folders are aligned -->
  <span
    class="flex w-3.5 shrink-0 items-center justify-center text-gray-400"
    style:margin-left="{padding + row.depth * indent}px"
  >
    {#if row.node.type === 'folder'}
      <svg
        class="h-3 w-3 transition-transform {filterText || !collapsedFolders.has(row.node.path)
          ? 'rotate-90'
          : ''}"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        stroke-width="2.5"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
      </svg>
    {/if}
  </span>
  <span class="min-w-0 flex-1 truncate" title={row.node.path}>{row.node.name}</span>
  <!-- Decorations, right-aligned -->
  {#if conceptCount(row.node) > 0}
    {@const count = conceptCount(row.node)}
    <span
      class="shrink-0 font-mono text-[10px] text-purple-600/80 tabular-nums"
      title="{count} concepts and operations"
    >
      ◆ {count}
    </span>
  {/if}
  {#if hasSuggestions(row.node)}
    <span class="h-1.5 w-1.5 shrink-0 rounded-full bg-purple-500" title="Has concept suggestions"
    ></span>
  {/if}
{/snippet}

{#snippet tree()}
  <div class="flex h-full min-h-0 flex-col bg-white">
    <!-- Header: component and filter -->
    <div class="shrink-0 space-y-1.5 border-b border-gray-200 px-2 py-2">
      {#if allComponents && allComponents.length > 1}
        <select
          class="w-full rounded border-gray-300 py-0.5 pr-7 pl-1.5 text-xs font-semibold text-gray-700"
          value={component.name}
          onchange={(e) => selectComponent(e.currentTarget.value)}
          aria-label="Component"
          title="Component"
        >
          {#each allComponents as comp (comp.name)}
            <option value={comp.name}>{comp.name}</option>
          {/each}
        </select>
      {:else}
        <p class="truncate px-0.5 text-xs font-semibold text-gray-700" title={component.topLevel}>
          {component.name}
        </p>
      {/if}
      <input
        type="search"
        class="w-full rounded border-gray-300 px-1.5 py-0.5 text-xs placeholder-gray-400"
        placeholder="Filter files"
        aria-label="Filter files"
        bind:value={filter}
        onkeydown={(e) => {
          if (e.key === 'Escape') filter = '';
        }}
      />
    </div>

    <nav class="min-h-0 flex-1 overflow-y-auto py-1 text-gray-700">
      {#each rows as row (row.node.path + ':' + row.node.type)}
        {@const selected = row.node.type === 'file' && currentUnitId === row.node.unit?.id}
        {#if row.node.type === 'folder'}
          <button
            type="button"
            class="{rowClass} hover:bg-gray-100"
            onclick={() => toggleFolder(row.node.path)}
            aria-expanded={!!filterText || !collapsedFolders.has(row.node.path)}
          >
            {@render rowContent(row, false)}
          </button>
        {:else if fileHref && row.node.unit}
          <!-- The href is built by the caller -->
          <!-- eslint-disable svelte/no-navigation-without-resolve -->
          <a
            href={fileHref(row.node.unit)}
            class="{rowClass} {selected ? 'bg-gray-200/70 text-gray-900' : 'hover:bg-gray-100'}"
            aria-current={selected ? 'page' : undefined}
          >
            {@render rowContent(row, selected)}
          </a>
          <!-- eslint-enable svelte/no-navigation-without-resolve -->
        {:else}
          <button
            type="button"
            class="{rowClass} {selected ? 'bg-gray-200/70 text-gray-900' : 'hover:bg-gray-100'}"
            onclick={() => row.node.unit && onFileSelect?.(row.node.unit)}
            aria-current={selected ? 'page' : undefined}
          >
            {@render rowContent(row, selected)}
          </button>
        {/if}
      {:else}
        <p class="px-3 py-2 text-xs text-gray-400">
          {filterText ? 'No matching files' : 'No files'}
        </p>
      {/each}
    </nav>
  </div>
{/snippet}

{#if embedded}
  {@render tree()}
{:else}
  <CollapsiblePanel title="Files" side="left" {width} bind:collapsed>
    {@render tree()}
  </CollapsiblePanel>
{/if}
