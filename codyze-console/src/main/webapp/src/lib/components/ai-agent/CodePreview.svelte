<script lang="ts">
  import type { NodeJSON, TranslationUnitJSON } from '$lib/types';
  import { CodeViewer } from '$lib/components/analysis';

  interface Props {
    node: NodeJSON;
    translationUnit: TranslationUnitJSON;
    astNodes?: NodeJSON[];
    overlayNodes?: NodeJSON[];
    onClose: () => void;
  }

  let { node, translationUnit, astNodes = [], overlayNodes = [], onClose }: Props = $props();

  let fileName = $derived(node.fileName || translationUnit.name);
  let highlightLine = $derived(node.startLine);
</script>

<div class="code-preview">
  <!-- Header -->
  <div class="preview-header">
    <div class="header-content">
      <svg class="header-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
        />
      </svg>
      <div class="header-info">
        <div class="header-title">{node.name}</div>
        <div class="header-subtitle">
          {fileName}:{node.startLine}
        </div>
      </div>
    </div>
    <button onclick={onClose} class="close-button" type="button" aria-label="Close code preview">
      <svg class="close-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M6 18L18 6M6 6l12 12"
        />
      </svg>
    </button>
  </div>

  <!-- Code Content - Using existing CodeViewer -->
  <div class="preview-content">
    <CodeViewer
      {translationUnit}
      {astNodes}
      {overlayNodes}
      {highlightLine}
    />
  </div>

  <!-- Footer with metadata -->
  <div class="preview-footer">
    <div class="footer-item">
      <span class="footer-label">Type:</span>
      <span class="footer-value">{node.type}</span>
    </div>
    {#if node.startLine && node.endLine}
      <div class="footer-item">
        <span class="footer-label">Lines:</span>
        <span class="footer-value">{node.startLine}-{node.endLine}</span>
      </div>
    {/if}
  </div>
</div>

<style>
  .code-preview {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: white;
    border-left: 1px solid #e5e7eb;
  }

  .preview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    background: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
    flex-shrink: 0;
  }

  .header-content {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
    min-width: 0;
  }

  .header-icon {
    width: 1.25rem;
    height: 1.25rem;
    color: #6b7280;
    flex-shrink: 0;
  }

  .header-info {
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
    min-width: 0;
  }

  .header-title {
    font-size: 0.9375rem;
    font-weight: 600;
    color: #111827;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .header-subtitle {
    font-size: 0.75rem;
    color: #6b7280;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  }

  .close-button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2rem;
    height: 2rem;
    border-radius: 0.375rem;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: all 0.15s ease;
    flex-shrink: 0;
  }

  .close-button:hover {
    background: #e5e7eb;
  }

  .close-button:active {
    background: #d1d5db;
  }

  .close-icon {
    width: 1.125rem;
    height: 1.125rem;
    color: #6b7280;
  }

  .preview-content {
    flex: 1;
    overflow: auto;
    background: #ffffff;
  }

  .preview-footer {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    padding: 0.75rem 1.25rem;
    background: #f9fafb;
    border-top: 1px solid #e5e7eb;
    flex-shrink: 0;
  }

  .footer-item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.75rem;
  }

  .footer-label {
    color: #6b7280;
    font-weight: 500;
  }

  .footer-value {
    color: #111827;
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
    font-weight: 500;
  }

  /* Scrollbar styling */
  .preview-content::-webkit-scrollbar {
    width: 0.5rem;
    height: 0.5rem;
  }

  .preview-content::-webkit-scrollbar-track {
    background: #f3f4f6;
  }

  .preview-content::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 0.25rem;
  }

  .preview-content::-webkit-scrollbar-thumb:hover {
    background: #9ca3af;
  }
</style>
