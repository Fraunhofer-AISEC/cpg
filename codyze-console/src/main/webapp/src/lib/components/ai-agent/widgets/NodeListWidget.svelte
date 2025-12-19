<script lang="ts">
    import type {NodeJSON} from '$lib/types';
    import type {WidgetProps} from './widgetRegistry';

    let {data, onItemClick}: WidgetProps = $props();

    // Extract nodes from content (can be array or object with nodes property)
    let items = $derived.by<NodeJSON[]>(() => {
        if (Array.isArray(data.content)) {
            return data.content;
        }
        if (data.content?.nodes && Array.isArray(data.content.nodes)) {
            return data.content.nodes;
        }
        return [];
    });

    function formatLocation(item: NodeJSON): string {
        if (!item.fileName && !item.startLine) return '';
        if (!item.fileName) return `Line ${item.startLine}`;
        return `${item.fileName}:${item.startLine}`;
    }

    function handleItemClick(item: NodeJSON) {
        if (onItemClick) {
            onItemClick(item);
        }
    }

    // Get tool display name
    let toolDisplayName = $derived(data.toolName || 'Results');
</script>

<div class="node-list-widget">
    <!-- Header with tool name and count -->
    <div class="widget-header">
        <div class="tool-badge">
            <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
            </svg>
            <span class="tool-name">{toolDisplayName}</span>
        </div>
        <span class="results-count">{items.length} result{items.length !== 1 ? 's' : ''}</span>
    </div>

    <!-- List items -->
    <div class="results-items">
        {#each items as item, idx}
            <button class="result-item" onclick={() => handleItemClick(item)} type="button">
                <!-- Content -->
                <div class="item-content">
                    <div class="item-title">{item.name}</div>
                    <div class="item-subtitle">
                        <span class="item-type">{item.type}</span>
                        {#if formatLocation(item)}
                            <span class="item-divider">·</span>
                            <span class="item-location">{formatLocation(item)}</span>
                        {/if}
                    </div>
                </div>

                <!-- Action arrow -->
                <div class="item-action">
                    <svg class="arrow-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                    </svg>
                </div>
            </button>
        {/each}
    </div>
</div>

<style>
    .node-list-widget {
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

    .icon {
        width: 1rem;
        height: 1rem;
        flex-shrink: 0;
    }

    .tool-name {
        font-size: 0.8125rem;
        font-weight: 600;
    }

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
        background: #f9fafb;
    }

    .result-item {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0.875rem 1rem;
        background: white;
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        cursor: pointer;
        transition: all 0.15s ease;
        text-align: left;
        width: 100%;
    }

    .result-item:hover {
        background: #f9fafb;
        border-color: #3b82f6;
        box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
        transform: translateY(-1px);
    }

    .result-item:active {
        transform: translateY(0);
    }

    .result-item:focus {
        outline: 2px solid #3b82f6;
        outline-offset: 2px;
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

    .item-type {
        color: #4b5563;
        font-weight: 500;
    }

    .item-divider {
        color: #d1d5db;
    }

    .item-location {
        font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
        font-size: 0.75rem;
        color: #9ca3af;
    }

    .item-action {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .arrow-icon {
        width: 1rem;
        height: 1rem;
        color: #9ca3af;
        transition: all 0.15s ease;
    }

    .result-item:hover .arrow-icon {
        color: #3b82f6;
        transform: translateX(2px);
    }

    /* Responsive adjustments */
    @media (max-width: 640px) {
        .result-item {
            padding: 0.75rem 0.875rem;
        }

        .item-title {
            font-size: 0.875rem;
        }

        .item-subtitle {
            font-size: 0.75rem;
        }
    }
</style>