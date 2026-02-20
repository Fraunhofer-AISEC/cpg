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

  function deriveWidgetTitle(items: DisplayItem[]): string {
    if (items.length === 0) return 'No Results';
    const types = new Set(items.map(i => i.subtitle).filter(Boolean));
    if (types.size === 1) {
      const type = [...types][0];
      return `${items.length} ${type}${items.length !== 1 ? 's' : ''}`;
    }
    return `${items.length} Result${items.length !== 1 ? 's' : ''}`;
  }

  let items = $derived(getItemsArray(data.content).map((item, i) => normalizeItem(item, i)));

  // Collapse/show-more
  const DEFAULT_VISIBLE = 10;
  let showAll = $state(false);
  let visibleItems = $derived(showAll ? items : items.slice(0, DEFAULT_VISIBLE));
  let hasMore = $derived(items.length > DEFAULT_VISIBLE);

  let widgetTitle = $derived(deriveWidgetTitle(items));

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

<div class="item-list-widget">
  <div class="widget-header">
    <div class="tool-badge">
      <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <span class="tool-name">{widgetTitle}</span>
    </div>
    <span class="results-count">{items.length} result{items.length !== 1 ? 's' : ''}</span>
  </div>

  <div class="results-items">
    {#if items.length === 0}
      <div class="empty-state">
        <p class="empty-text">No results</p>
      </div>
    {:else}
      {#each visibleItems as item (item.id)}
        <div class="result-item-container">
          <button
            class="result-item"
            onclick={() => item.expandable ? toggleExpand(item.id) : handleClick(item)}
            type="button"
          >
            {#if item.badge}
              <span class="item-badge {getBadgeClasses(item.badge.color)}">{item.badge.text}</span>
            {/if}

            <div class="item-content">
              <div class="item-title">{item.title}</div>
              {#if item.subtitle || item.location}
                <div class="item-subtitle">
                  {#if item.subtitle}<span class="item-type">{item.subtitle}</span>{/if}
                  {#if item.subtitle && item.location}<span class="item-divider">&middot;</span>{/if}
                  {#if item.location}<span class="item-location">{item.location}</span>{/if}
                </div>
              {/if}
            </div>

            <div class="item-action">
              {#if item.expandable}
                <svg class="expand-icon" class:expanded={expandedIds.has(item.id)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              {:else}
                <svg class="arrow-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              {/if}
            </div>
          </button>

          {#if item.expandable && expandedIds.has(item.id)}
            <div class="expanded-content">
              {#each Object.entries(item.extraProps) as [key, value]}
                <div class="detail-item">
                  <span class="detail-label">{key}:</span>
                  {#if key === 'Code'}
                    <pre class="detail-code">{value}</pre>
                  {:else}
                    <p class="detail-text">{value}</p>
                  {/if}
                </div>
              {/each}
              <button class="view-btn" onclick={() => handleClick(item)} type="button">View Details</button>
            </div>
          {/if}
        </div>
      {/each}

      {#if hasMore && !showAll}
        <button class="show-more-btn" onclick={() => showAll = true} type="button">
          Show all {items.length} results ({items.length - DEFAULT_VISIBLE} more)
        </button>
      {/if}
    {/if}
  </div>
</div>

<style>
  .item-list-widget {
    margin: 0.5rem 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 0.75rem;
    overflow: hidden;
  }

  .widget-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1rem;
    background: white;
    border-bottom: 1px solid #e5e7eb;
  }

  .tool-badge {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.25rem 0.75rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 0.5rem;
    color: white;
  }

  .icon { width: 1rem; height: 1rem; flex-shrink: 0; }
  .tool-name { font-size: 0.8125rem; font-weight: 600; }

  .results-count {
    font-size: 0.75rem;
    font-weight: 500;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .results-items {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.5rem;
  }

  .result-item-container {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .result-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.875rem 1rem;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: background 0.15s ease;
    text-align: left;
    width: 100%;
  }

  .result-item:hover { background: #f9fafb; }
  .result-item:focus { outline: 2px solid #3b82f6; outline-offset: -2px; }

  .item-badge {
    font-size: 0.6875rem;
    font-weight: 600;
    padding: 0.25rem 0.5rem;
    border-radius: 0.375rem;
    text-transform: uppercase;
    letter-spacing: 0.025em;
    flex-shrink: 0;
  }

  .item-content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .item-title {
    font-size: 0.9375rem;
    font-weight: 600;
    color: #111827;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-subtitle {
    font-size: 0.8125rem;
    color: #6b7280;
    display: flex;
    align-items: center;
    gap: 0.375rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .item-type { color: #4b5563; font-weight: 500; }
  .item-divider { color: #d1d5db; }
  .item-location {
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    font-size: 0.75rem;
    color: #9ca3af;
  }

  .item-action { flex-shrink: 0; display: flex; align-items: center; justify-content: center; }

  .arrow-icon {
    width: 1rem;
    height: 1rem;
    color: #9ca3af;
    transition: all 0.15s ease;
  }
  .result-item:hover .arrow-icon { color: #3b82f6; transform: translateX(2px); }

  .expand-icon {
    width: 1.25rem;
    height: 1.25rem;
    color: #9ca3af;
    transition: transform 0.2s;
  }
  .expand-icon.expanded { transform: rotate(180deg); }

  .expanded-content {
    padding: 0 1rem 1rem 1rem;
    border-top: 1px solid #e5e7eb;
    background: #fafafa;
  }

  .detail-item { margin-top: 0.75rem; }
  .detail-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .detail-text {
    font-size: 0.875rem;
    color: #374151;
    margin: 0.375rem 0 0 0;
    line-height: 1.5;
  }

  .detail-code {
    font-size: 0.8125rem;
    color: #374151;
    margin: 0.375rem 0 0 0;
    padding: 0.5rem;
    background: #f3f4f6;
    border-radius: 0.25rem;
    overflow-x: auto;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    white-space: pre-wrap;
    word-wrap: break-word;
  }

  .view-btn {
    margin-top: 0.75rem;
    padding: 0.5rem 1rem;
    background: white;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.8125rem;
    font-weight: 500;
    color: #374151;
    cursor: pointer;
    transition: all 0.15s;
  }
  .view-btn:hover { background: #f9fafb; border-color: #3b82f6; color: #3b82f6; }

  .show-more-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.625rem 1rem;
    background: white;
    border: 1px dashed #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.8125rem;
    font-weight: 500;
    color: #6b7280;
    cursor: pointer;
    transition: all 0.15s;
    width: 100%;
  }
  .show-more-btn:hover {
    background: #f9fafb;
    border-color: #3b82f6;
    color: #3b82f6;
  }

  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    color: #9ca3af;
  }
  .empty-text { font-size: 0.875rem; margin: 0; }

  @media (max-width: 640px) {
    .result-item { padding: 0.75rem 0.875rem; }
    .item-title { font-size: 0.875rem; }
    .item-subtitle { font-size: 0.75rem; }
  }
</style>
