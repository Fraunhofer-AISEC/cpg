<script lang="ts" module>
  export function getItemsArray(content: any): any[] {
    if (!content) return [];
    if (content.items && Array.isArray(content.items)) return content.items;
    if (Array.isArray(content)) return content;
    // Single object with id or nodeId — wrap as one-element list
    if (typeof content === 'object' && ('id' in content || 'nodeId' in content)) return [content];
    return [];
  }

  export function isCodeItemContent(content: any): boolean {
    const items = getItemsArray(content);
    if (items.length === 0) return false;
    const first = items[0];
    if (typeof first === 'string') return true;
    if (typeof first === 'object' && first !== null) {
      if ('id' in first || 'nodeId' in first) return true;
    }
    return false;
  }
</script>

<script lang="ts">
  import type { ToolResultData } from './widgetRegistry';

  let { data, onItemClick }: { data: ToolResultData; onItemClick?: (item: any) => void } = $props();

  interface DisplayItem {
    id: string;
    title: string;
    subtitle: string;
    badge?: { text: string; color: string };
    location?: string;
    expandable: boolean;
    extraProps: Record<string, string>;
    original: any;
  }

  const EXTRA_PROP_FIELDS: Record<string, string> = {
    parameters: 'Parameters',
    returnType: 'Return Type',
    callees: 'Callees',
    methodNames: 'Methods',
    arguments: 'Arguments',
    code: 'Code',
    reasoning: 'Reasoning',
    securityImpact: 'Security Impact',
    fieldCount: 'Fields',
  };

  function buildDynamicExtraProps(item: any): Record<string, string> {
    const props: Record<string, string> = {};
    for (const [field, label] of Object.entries(EXTRA_PROP_FIELDS)) {
      if (item[field] !== undefined && item[field] !== null) {
        props[label] = Array.isArray(item[field]) ? item[field].join(', ') : String(item[field]);
      }
    }
    return props;
  }

  function isConceptClass(overlayClass: string): boolean {
    const lower = overlayClass.toLowerCase();
    return lower.includes('concept') || lower.includes('Concept');
  }

  function getShortName(fullName: string): string {
    const parts = fullName.split('.');
    return parts[parts.length - 1];
  }

  function formatLocation(item: any): string {
    if (!item.fileName && !item.startLine) return '';
    if (!item.fileName) return `Line ${item.startLine}`;
    return `${item.fileName}:${item.startLine}`;
  }

  function normalizeItem(item: any, index: number): DisplayItem {
    if (typeof item === 'string') {
      return {
        id: `item-${index}`,
        title: item,
        subtitle: '',
        expandable: false,
        extraProps: {},
        original: item
      };
    }

    const id = `${item.id?.toString() || item.nodeId || 'item'}-${index}`;
    const title = item.name || item.nodeId || 'Unknown';
    const subtitle = item.overlay ? getShortName(item.overlay) : (item.returnType || item.kind || item.overlayClass || item.type || '');
    const location = formatLocation(item);
    const extraProps = buildDynamicExtraProps(item);

    return {
      id,
      title,
      subtitle,
      location,
      badge: item.overlayType
        ? { text: item.overlayType, color: item.overlayType === 'Concept' ? 'green' : 'amber' }
        : item.overlayClass
          ? { text: item.overlayClass, color: isConceptClass(item.overlayClass) ? 'green' : 'amber' }
          : undefined,
      expandable: Object.keys(extraProps).length > 0,
      extraProps,
      original: item,
    };
  }

  function getBadgeClasses(color: string): string {
    switch (color) {
      case 'green': return 'bg-green-100 text-green-800';
      case 'amber': return 'bg-amber-100 text-amber-800';
      case 'blue': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  let items = $derived(getItemsArray(data.content).map((item, i) => normalizeItem(item, i)));

  // Collapse/show-more
  const DEFAULT_VISIBLE = 10;
  let showAll = $state(false);
  let visibleItems = $derived(showAll ? items : items.slice(0, DEFAULT_VISIBLE));
  let hasMore = $derived(items.length > DEFAULT_VISIBLE);

  // Expanded state
  let expandedIds = $state<Set<string>>(new Set());

  function toggleExpand(id: string) {
    if (expandedIds.has(id)) {
      expandedIds.delete(id);
    } else {
      expandedIds.add(id);
    }
    expandedIds = new Set(expandedIds);
  }

  function handleClick(item: DisplayItem) {
    if (onItemClick) {
      onItemClick(item.original);
    }
  }
</script>

<div class="flex flex-col gap-0.5">
  {#if items.length === 0}
    <p class="py-4 text-center text-sm text-gray-400">No results</p>
  {:else}
    {#each visibleItems as item (item.id)}
      <div class="overflow-hidden rounded-md border border-gray-200 bg-white">
        <button
          class="flex w-full items-center gap-3 px-3 py-2.5 text-left transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-inset"
          onclick={() => item.expandable ? toggleExpand(item.id) : handleClick(item)}
          type="button"
        >
          {#if item.badge}
            <span class="shrink-0 rounded px-1.5 py-0.5 text-[11px] font-semibold uppercase tracking-wide {getBadgeClasses(item.badge.color)}">{item.badge.text}</span>
          {/if}

          <div class="flex min-w-0 flex-1 flex-col gap-0.5">
            <div class="truncate font-mono text-sm font-semibold text-gray-900">{item.title}</div>
            {#if item.subtitle || item.location}
              <div class="flex items-center gap-1.5 truncate text-[13px] text-gray-500">
                {#if item.subtitle}<span class="font-medium text-gray-600">{item.subtitle}</span>{/if}
                {#if item.subtitle && item.location}<span class="text-gray-300">&middot;</span>{/if}
                {#if item.location}<span class="font-mono text-xs text-gray-400">{item.location}</span>{/if}
              </div>
            {/if}
          </div>

          <div class="flex shrink-0 items-center">
            {#if item.expandable}
              <svg class="h-4 w-4 text-gray-400 transition-transform duration-200 {expandedIds.has(item.id) ? 'rotate-180' : ''}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            {:else}
              <svg class="h-4 w-4 text-gray-300 transition-all hover:translate-x-0.5 hover:text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            {/if}
          </div>
        </button>

        {#if item.expandable && expandedIds.has(item.id)}
          <div class="border-t border-gray-100 bg-gray-50 px-3 pb-3">
            {#each Object.entries(item.extraProps) as [key, value]}
              <div class="mt-2.5">
                <span class="text-xs font-semibold uppercase tracking-wide text-gray-500">{key}:</span>
                {#if key === 'Code'}
                  <pre class="mt-1 overflow-x-auto whitespace-pre-wrap break-words rounded bg-gray-100 px-2 py-1.5 font-mono text-[13px] text-gray-700">{value}</pre>
                {:else}
                  <p class="mt-1 text-sm leading-relaxed text-gray-700">{value}</p>
                {/if}
              </div>
            {/each}
            <button
              class="mt-2.5 rounded-md border border-gray-200 bg-white px-3 py-1.5 text-[13px] font-medium text-gray-600 transition-colors hover:border-blue-300 hover:text-blue-600"
              onclick={() => handleClick(item)}
              type="button"
            >View Details</button>
          </div>
        {/if}
      </div>
    {/each}

    {#if hasMore && !showAll}
      <button
        class="w-full rounded-md py-2 text-[13px] font-medium text-gray-500 transition-colors hover:bg-gray-50 hover:text-blue-600"
        onclick={() => showAll = true}
        type="button"
      >
        Show all {items.length} results ({items.length - DEFAULT_VISIBLE} more)
      </button>
    {/if}
  {/if}
</div>
