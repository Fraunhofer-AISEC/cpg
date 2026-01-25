<script lang="ts">
  import { widgetRegistry, type WidgetComponent, type WidgetProps } from './widgetRegistry';
  import ItemListWidget from './ItemListWidget.svelte';

  let { data, onItemClick }: WidgetProps = $props();

  // Register widgets immediately (only once)
  let widgetsRegistered = false;
  if (!widgetsRegistered) {
    widgetsRegistered = true;

    // Register ItemListWidget for all list-type tools and structured data
    widgetRegistry.register(ItemListWidget, (data) => {
      // Match by tool name pattern
      if (data.toolName?.startsWith('cpg_list')) return true;
      if (data.toolName === 'cpg_llm_analyze') return true;

      // Match by content structure - overlay suggestions response
      if (data.content?.items && Array.isArray(data.content.items)) {
        const first = data.content.items[0];
        if (first?.node && first?.overlay) return true;
      }

      // Match by content structure - array of structured objects
      if (Array.isArray(data.content) && data.content.length > 0) {
        const first = data.content[0];
        // Node-like objects (from list_functions, list_records, etc.)
        if (first && typeof first === 'object' && 'id' in first && 'type' in first) {
          return true;
        }
        // Applied overlay objects (from list_concepts_and_operations)
        if (first && typeof first === 'object' && 'nodeId' in first && 'overlayClass' in first) {
          return true;
        }
        // String array (from list_available_concepts/operations)
        if (typeof first === 'string') {
          return true;
        }
      }

      return false;
    });
  }

  // Compute selected widget reactively
  let selectedWidget: WidgetComponent | null = $derived.by(() => {
    return widgetRegistry.getWidget(data);
  });

  // Fallback rendering for when no widget matches
  function renderFallback(content: any): string {
    if (typeof content === 'string') return content;
    try {
      return JSON.stringify(content, null, 2);
    } catch {
      return String(content);
    }
  }
</script>

<div class="tool-result-widget">
  {#if data.isPending}
    <!-- Pending state: Tool is running -->
    <div class="pending-widget">
      <div class="pending-header">
        <div class="pending-spinner"></div>
        <span class="tool-name">{data.toolName}</span>
        <span class="pending-badge">Running</span>
      </div>
      <div class="pending-content">
        <span class="pending-text">Executing tool...</span>
      </div>
    </div>
  {:else if selectedWidget}
    <!-- Render the selected specialized widget -->
    {@const SelectedWidget = selectedWidget}
    <SelectedWidget {data} {onItemClick} />
  {:else}
    <!-- Fallback: Simple text/JSON rendering -->
    <div class="fallback-widget">
      <div class="fallback-header">
        {#if data.toolName}
          <span class="tool-name">{data.toolName}</span>
        {/if}
        {#if data.isError}
          <span class="error-badge">Error</span>
        {/if}
      </div>
      <pre class="fallback-content">{renderFallback(data.content)}</pre>
    </div>
  {/if}
</div>

<style>
  .tool-result-widget {
    width: 100%;
  }

  .fallback-widget {
    margin: 0.5rem 0;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .fallback-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1rem;
    background: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
  }

  .tool-name {
    font-size: 0.875rem;
    font-weight: 600;
    color: #374151;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  }

  .error-badge {
    padding: 0.25rem 0.5rem;
    background: #fee2e2;
    color: #991b1b;
    font-size: 0.75rem;
    font-weight: 600;
    border-radius: 0.25rem;
    text-transform: uppercase;
  }

  .fallback-content {
    padding: 1rem;
    margin: 0;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    font-size: 0.8125rem;
    line-height: 1.5;
    color: #374151;
    overflow-x: auto;
    white-space: pre-wrap;
    word-wrap: break-word;
  }

  /* Pending state styles */
  .pending-widget {
    margin: 0.5rem 0;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .pending-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1rem;
    background: #f0f9ff;
    border-bottom: 1px solid #e0f2fe;
  }

  .pending-spinner {
    width: 1rem;
    height: 1rem;
    border: 2px solid #bfdbfe;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .pending-badge {
    padding: 0.25rem 0.5rem;
    background: #dbeafe;
    color: #1d4ed8;
    font-size: 0.75rem;
    font-weight: 600;
    border-radius: 0.25rem;
    text-transform: uppercase;
  }

  .pending-content {
    padding: 1rem;
  }

  .pending-text {
    color: #6b7280;
    font-size: 0.875rem;
    font-style: italic;
  }
</style>
