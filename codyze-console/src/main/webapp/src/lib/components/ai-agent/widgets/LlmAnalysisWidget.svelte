<script lang="ts">
  import type { NodeJSON } from '$lib/types';
  import type { WidgetProps } from './widgetRegistry';

  interface OverlaySuggestion {
    nodeId: string;
    overlay: string;
    overlayType: 'Concept' | 'Operation';
    conceptNodeId?: string;
    reasoning?: string;
    securityImpact?: string;
  }

  interface LlmAnalysisResult {
    overlaySuggestions?: OverlaySuggestion[];
    nodes?: NodeJSON[];
    summary?: string;
  }

  let { data, onItemClick }: WidgetProps = $props();

  // Parse the content - could be JSON string or object
  let analysisResult = $derived.by<LlmAnalysisResult>(() => {
    try {
      if (typeof data.content === 'string') {
        return JSON.parse(data.content);
      }
      return data.content as LlmAnalysisResult;
    } catch {
      return { overlaySuggestions: [], nodes: [] };
    }
  });

  let suggestions = $derived(analysisResult?.overlaySuggestions || []);
  let nodes = $derived(analysisResult?.nodes || []);
  let summary = $derived(analysisResult?.summary || '');

  // Group suggestions by type
  let conceptSuggestions = $derived(
    suggestions.filter((s) => s.overlayType === 'Concept')
  );
  let operationSuggestions = $derived(
    suggestions.filter((s) => s.overlayType === 'Operation')
  );

  // Expandable state for suggestions
  let expandedSuggestions = $state<Set<string>>(new Set());

  function toggleSuggestion(nodeId: string) {
    if (expandedSuggestions.has(nodeId)) {
      expandedSuggestions.delete(nodeId);
    } else {
      expandedSuggestions.add(nodeId);
    }
    expandedSuggestions = new Set(expandedSuggestions);
  }

  function getOverlayShortName(fullName: string): string {
    const parts = fullName.split('.');
    return parts[parts.length - 1];
  }

  function handleNodeClick(nodeId: string) {
    const node = nodes.find((n) => n.id === nodeId);
    if (node && onItemClick) {
      onItemClick(node);
    }
  }
</script>

<div class="llm-analysis-widget">
  <!-- Header -->
  <div class="widget-header">
    <div class="tool-badge">
      <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"
        />
      </svg>
      <span class="tool-name">LLM Analysis</span>
    </div>
    <span class="results-count"
      >{suggestions.length} suggestion{suggestions.length !== 1 ? 's' : ''}</span
    >
  </div>

  <!-- Content -->
  <div class="widget-content">
    {#if summary}
      <div class="summary-section">
        <div class="section-title">Summary</div>
        <p class="summary-text">{summary}</p>
      </div>
    {/if}

    {#if suggestions.length > 0}
      <!-- Concepts Section -->
      {#if conceptSuggestions.length > 0}
        <div class="suggestions-section">
          <div class="section-title">
            <svg class="section-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
              />
            </svg>
            Concepts ({conceptSuggestions.length})
          </div>
          <div class="suggestions-list">
            {#each conceptSuggestions as suggestion}
              <div class="suggestion-item concept-item">
                <button
                  class="suggestion-header"
                  onclick={() => toggleSuggestion(suggestion.nodeId)}
                  type="button"
                >
                  <div class="suggestion-main">
                    <span class="overlay-badge concept-badge"
                      >{getOverlayShortName(suggestion.overlay)}</span
                    >
                    <span class="node-id">Node: {suggestion.nodeId}</span>
                  </div>
                  <svg
                    class="expand-icon"
                    class:expanded={expandedSuggestions.has(suggestion.nodeId)}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M19 9l-7 7-7-7"
                    />
                  </svg>
                </button>

                {#if expandedSuggestions.has(suggestion.nodeId)}
                  <div class="suggestion-details">
                    {#if suggestion.reasoning}
                      <div class="detail-item">
                        <span class="detail-label">Reasoning:</span>
                        <p class="detail-text">{suggestion.reasoning}</p>
                      </div>
                    {/if}
                    {#if suggestion.securityImpact}
                      <div class="detail-item">
                        <span class="detail-label">Security Impact:</span>
                        <p class="detail-text">{suggestion.securityImpact}</p>
                      </div>
                    {/if}
                    <button
                      class="view-node-btn"
                      onclick={() => handleNodeClick(suggestion.nodeId)}
                      type="button"
                    >
                      View Node
                    </button>
                  </div>
                {/if}
              </div>
            {/each}
          </div>
        </div>
      {/if}

      <!-- Operations Section -->
      {#if operationSuggestions.length > 0}
        <div class="suggestions-section">
          <div class="section-title">
            <svg class="section-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
            Operations ({operationSuggestions.length})
          </div>
          <div class="suggestions-list">
            {#each operationSuggestions as suggestion}
              <div class="suggestion-item operation-item">
                <button
                  class="suggestion-header"
                  onclick={() => toggleSuggestion(suggestion.nodeId)}
                  type="button"
                >
                  <div class="suggestion-main">
                    <span class="overlay-badge operation-badge"
                      >{getOverlayShortName(suggestion.overlay)}</span
                    >
                    <span class="node-id">Node: {suggestion.nodeId}</span>
                    {#if suggestion.conceptNodeId}
                      <span class="concept-ref">→ Concept: {suggestion.conceptNodeId}</span>
                    {/if}
                  </div>
                  <svg
                    class="expand-icon"
                    class:expanded={expandedSuggestions.has(suggestion.nodeId)}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M19 9l-7 7-7-7"
                    />
                  </svg>
                </button>

                {#if expandedSuggestions.has(suggestion.nodeId)}
                  <div class="suggestion-details">
                    {#if suggestion.reasoning}
                      <div class="detail-item">
                        <span class="detail-label">Reasoning:</span>
                        <p class="detail-text">{suggestion.reasoning}</p>
                      </div>
                    {/if}
                    {#if suggestion.securityImpact}
                      <div class="detail-item">
                        <span class="detail-label">Security Impact:</span>
                        <p class="detail-text">{suggestion.securityImpact}</p>
                      </div>
                    {/if}
                    <button
                      class="view-node-btn"
                      onclick={() => handleNodeClick(suggestion.nodeId)}
                      type="button"
                    >
                      View Node
                    </button>
                  </div>
                {/if}
              </div>
            {/each}
          </div>
        </div>
      {/if}

      <!-- Action Footer -->
      <div class="action-footer">
        <div class="info-note">
          <svg class="info-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          Review suggestions before applying. Use <code>cpg_apply_concepts</code> to apply.
        </div>
      </div>
    {:else}
      <div class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <p class="empty-text">No suggestions generated</p>
      </div>
    {/if}
  </div>
</div>

<style>
  .llm-analysis-widget {
    margin: 0.5rem 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 0.75rem;
    overflow: hidden;
  }

  .widget-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
  }

  .tool-badge {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .icon {
    width: 1.25rem;
    height: 1.25rem;
    flex-shrink: 0;
  }

  .tool-name {
    font-size: 0.9375rem;
    font-weight: 600;
  }

  .results-count {
    font-size: 0.75rem;
    font-weight: 500;
    background: rgba(255, 255, 255, 0.2);
    padding: 0.25rem 0.5rem;
    border-radius: 0.375rem;
  }

  .widget-content {
    padding: 1rem;
  }

  .summary-section {
    margin-bottom: 1.5rem;
    padding: 1rem;
    background: #eff6ff;
    border-left: 3px solid #3b82f6;
    border-radius: 0.5rem;
  }

  .section-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 0.75rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .section-icon {
    width: 1rem;
    height: 1rem;
  }

  .summary-text {
    font-size: 0.875rem;
    color: #374151;
    line-height: 1.5;
    margin: 0;
  }

  .suggestions-section {
    margin-bottom: 1.5rem;
  }

  .suggestions-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .suggestion-item {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .concept-item {
    border-left: 3px solid #10b981;
  }

  .operation-item {
    border-left: 3px solid #f59e0b;
  }

  .suggestion-header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.875rem 1rem;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: background 0.15s;
    text-align: left;
  }

  .suggestion-header:hover {
    background: rgba(0, 0, 0, 0.02);
  }

  .suggestion-main {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .overlay-badge {
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.25rem 0.625rem;
    border-radius: 0.375rem;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  }

  .concept-badge {
    background: #d1fae5;
    color: #065f46;
  }

  .operation-badge {
    background: #fef3c7;
    color: #92400e;
  }

  .node-id {
    font-size: 0.8125rem;
    color: #6b7280;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  }

  .concept-ref {
    font-size: 0.75rem;
    color: #9ca3af;
  }

  .expand-icon {
    width: 1.25rem;
    height: 1.25rem;
    color: #9ca3af;
    transition: transform 0.2s;
    flex-shrink: 0;
  }

  .expand-icon.expanded {
    transform: rotate(180deg);
  }

  .suggestion-details {
    padding: 0 1rem 1rem 1rem;
    border-top: 1px solid #e5e7eb;
    background: white;
  }

  .detail-item {
    margin-top: 0.75rem;
  }

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

  .view-node-btn {
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

  .view-node-btn:hover {
    background: #f9fafb;
    border-color: #3b82f6;
    color: #3b82f6;
  }

  .action-footer {
    margin-top: 1rem;
    padding-top: 1rem;
    border-top: 1px solid #e5e7eb;
  }

  .info-note {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem;
    background: #fef3c7;
    border-radius: 0.5rem;
    font-size: 0.8125rem;
    color: #78350f;
    line-height: 1.5;
  }

  .info-icon {
    width: 1rem;
    height: 1rem;
    flex-shrink: 0;
  }

  .info-note code {
    background: rgba(0, 0, 0, 0.1);
    padding: 0.125rem 0.375rem;
    border-radius: 0.25rem;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    font-size: 0.75rem;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 1rem;
    color: #9ca3af;
  }

  .empty-icon {
    width: 3rem;
    height: 3rem;
    margin-bottom: 0.75rem;
  }

  .empty-text {
    font-size: 0.875rem;
    margin: 0;
  }
</style>