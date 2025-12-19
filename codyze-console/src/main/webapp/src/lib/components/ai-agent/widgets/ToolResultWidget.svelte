<script lang="ts">
  import {
    widgetRegistry,
    type WidgetComponent,
    type WidgetProps
  } from './widgetRegistry';
  import NodeListWidget from './NodeListWidget.svelte';
  import LlmAnalysisWidget from './LlmAnalysisWidget.svelte';

  let { data, onItemClick }: WidgetProps = $props();

  // Register widgets immediately (only once)
  let widgetsRegistered = false;
  if (!widgetsRegistered) {
    widgetsRegistered = true;

    // Register LlmAnalysisWidget for cpg_llm_analyze tool
    widgetRegistry.register(
      LlmAnalysisWidget,
      (data) => {
        // Check if tool name matches
        if (data.toolName === 'cpg_llm_analyze') return true;

        // Check if content has overlaySuggestions property
        if (data.content?.overlaySuggestions) return true;

        return false;
      }
    );

    // Register NodeListWidget for list_* tools
    widgetRegistry.register(
      NodeListWidget,
      (data) => {
        // Check if tool name starts with cpg_list
        if (data.toolName?.startsWith('cpg_list')) return true;

        // Check if content is an array of objects with node-like structure
        if (Array.isArray(data.content)) {
          const first = data.content[0];
          if (first && typeof first === 'object' && 'id' in first && 'type' in first) {
            return true;
          }
        }

        return false;
      }
    );
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
  {#if selectedWidget}
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
</style>