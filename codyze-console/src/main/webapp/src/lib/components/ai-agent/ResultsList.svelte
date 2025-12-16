<script lang="ts">
  import type { NodeJSON } from '$lib/types';

  interface Props {
    items: NodeJSON[];
    onItemClick?: (item: NodeJSON) => void;
  }

  let { items, onItemClick }: Props = $props();

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
</script>

<div class="results-list">
  <!-- Header with count -->
  <div class="results-header">
    <span class="results-count">{items.length} result{items.length !== 1 ? 's' : ''}</span>
  </div>

  <!-- List items -->
  <div class="results-items">
    {#each items as item, idx}
      <button
        class="result-item"
        onclick={() => handleItemClick(item)}
        type="button"
      >
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
          <svg
            class="arrow-icon"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 5l7 7-7 7"
            />
          </svg>
        </div>
      </button>
    {/each}
  </div>
</div>

<style>
  .results-list {
    margin: 0.5rem 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
  }

  .results-header {
    padding: 0.5rem 0.75rem;
    margin-bottom: 0.25rem;
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