<script lang="ts">
  import type { WidgetProps } from './widgetRegistry';

  let { data, onItemClick }: WidgetProps = $props();

  // Normalized item for display - we extract what we need from any input
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

  // Get items array from content (handles different response structures)
  function getItemsArray(content: any): any[] {
    if (!content) return [];
    // Response with { items: [...] }
    if (content.items && Array.isArray(content.items)) return content.items;
    // Direct array
    if (Array.isArray(content)) return content;
    return [];
  }

  // Detect what kind of item we're dealing with and normalize it
  function normalizeItem(item: any, index: number): DisplayItem {
    // String item (legacy format)
    if (typeof item === 'string') {
      return {
        id: `item-${index}`,
        title: item,
        subtitle: '',
        location: undefined,
        expandable: false,
        extraProps: {},
        original: item
      };
    }

    // Overlay suggestion (from cpg_llm_analyze) - has node + overlay
    if (item.node && item.overlay) {
      const node = item.node;
      return {
        id: node.id?.toString() || item.nodeId || `item-${index}`,
        title: node.name || 'Unknown',
        subtitle: node.type || '',
        badge: {
          text: getShortName(item.overlay),
          color: item.overlayType === 'Concept' ? 'green' : 'amber'
        },
        location: formatNodeLocation(node),
        expandable: !!(item.reasoning || item.securityImpact || node.code),
        extraProps: buildExtraProps({
          Reasoning: item.reasoning,
          'Security Impact': item.securityImpact,
          Code: node.code
        }),
        original: item
      };
    }

    // Applied overlay (from list_concepts_and_operations) - has nodeId + overlayClass
    if (item.nodeId && item.overlayClass) {
      return {
        id: item.nodeId,
        title: item.name || 'Unknown',
        subtitle: item.overlayClass,
        location: formatLocation(item),
        expandable: !!item.code,
        extraProps: buildExtraProps({ Code: item.code }),
        original: item
      };
    }

    // Node (from list_functions, list_records, etc.) - has id + type + name
    if (item.id && item.type) {
      return {
        id: item.id?.toString() || `item-${index}`,
        title: item.name || 'Unknown',
        subtitle: item.type,
        location: formatNodeLocation(item),
        expandable: !!item.code,
        extraProps: buildExtraProps({ Code: item.code }),
        original: item
      };
    }

    // Fallback - just show whatever we can
    return {
      id: item.id?.toString() || item.nodeId || `item-${index}`,
      title: item.name || item.title || JSON.stringify(item).slice(0, 50),
      subtitle: item.type || '',
      location: formatLocation(item),
      expandable: false,
      extraProps: {},
      original: item
    };
  }

  function buildExtraProps(props: Record<string, any>): Record<string, string> {
    const result: Record<string, string> = {};
    for (const [key, value] of Object.entries(props)) {
      if (value) result[key] = String(value);
    }
    return result;
  }

  function formatNodeLocation(item: any): string {
    if (!item) return '';
    if (!item.fileName && !item.startLine) return '';
    if (!item.fileName) return `Line ${item.startLine}`;
    return `${item.fileName}:${item.startLine}`;
  }

  function formatLocation(item: any): string {
    if (!item.fileName && !item.startLine) return '';
    if (!item.fileName) return `Line ${item.startLine}`;
    return `${item.fileName}:${item.startLine}`;
  }

  function getShortName(fullName: string): string {
    const parts = fullName.split('.');
    return parts[parts.length - 1];
  }

  function getBadgeClasses(color: string): string {
    switch (color) {
      case 'green': return 'bg-green-100 text-green-800';
      case 'amber': return 'bg-amber-100 text-amber-800';
      case 'blue': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  // Reactive: get normalized items
  let items = $derived(getItemsArray(data.content).map((item, i) => normalizeItem(item, i)));

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

  let toolDisplayName = $derived(data.toolName || 'Results');
</script>

<div class="item-list-widget">
  <div class="widget-header">
    <div class="tool-badge">
      <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
      </svg>
      <span class="tool-name">{toolDisplayName}</span>
    </div>
    <span class="results-count">{items.length} result{items.length !== 1 ? 's' : ''}</span>
  </div>

  <div class="results-items">
    {#if items.length === 0}
      <div class="empty-state">
        <p class="empty-text">No results</p>
      </div>
    {:else}
      {#each items as item (item.id)}
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
                  {#if item.subtitle && item.location}<span class="item-divider">·</span>{/if}
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
