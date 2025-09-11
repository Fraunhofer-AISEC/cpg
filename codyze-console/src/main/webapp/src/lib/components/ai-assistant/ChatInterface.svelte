<script lang="ts">
  import type { ComponentJSON, TranslationUnitJSON, NodeJSON } from '$lib/types';
  import { CodeViewer, FileTree } from '$lib/components/analysis';

  interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
  }

  interface Props {
    selectedComponent: ComponentJSON;
    messages: ChatMessage[];
    currentMessage: string;
    isLoading: boolean;
    streamingContent: string;
    onSendMessage: () => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
  }

  let { selectedComponent, messages, currentMessage, isLoading, streamingContent, onSendMessage, onReset, onMessageChange }: Props = $props();

  let selectedUnit = $state<TranslationUnitJSON | null>(null);
  let astNodes = $state<NodeJSON[]>([]);
  let overlayNodes = $state<NodeJSON[]>([]);

  async function handleFileSelect(unit: TranslationUnitJSON) {
    selectedUnit = unit;
    try {
      const response = await fetch(`/api/unit/${unit.id}/nodes`);
      const nodeData = await response.json();
      astNodes = nodeData.astNodes || [];
      overlayNodes = nodeData.overlayNodes || [];
    } catch (error) {
      console.error('Failed to load nodes:', error);
      astNodes = [];
      overlayNodes = [];
    }
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      onSendMessage();
    }
  }
</script>

<div class="h-full flex">
  <!-- Chat Area -->
  <div class="w-2/5 flex flex-col border-r border-gray-200 bg-gray-50">
    <!-- Chat Header -->
    <div class="flex items-center justify-between p-4 border-b border-gray-200 bg-white">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
          <svg class="w-4 h-4 text-white" viewBox="0 -960 960 960" fill="currentColor">
            <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z"/>
          </svg>
        </div>
        <div>
          <h2 class="text-lg font-semibold text-gray-800">AI Assistant</h2>
          <p class="text-sm text-gray-500">Analyzing {selectedComponent.name}</p>
        </div>
      </div>
      <button
        onclick={onReset}
        class="px-3 py-1 text-sm text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
      >
        New Chat
      </button>
    </div>

    <!-- Messages -->
    <div class="flex-1 overflow-y-auto p-4 space-y-4">
      {#each messages as message}
        <div class="flex {message.role === 'user' ? 'justify-end' : 'justify-start'}">
          <div class="max-w-[80%] group">
            <div class="flex items-end gap-2 {message.role === 'user' ? 'flex-row-reverse' : ''}">
              <!-- Avatar -->
              <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 {message.role === 'user' 
                ? 'bg-blue-600' : 'bg-gradient-to-br from-blue-500 to-purple-600'}">
                {#if message.role === 'user'}
                  <svg class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"/>
                  </svg>
                {:else}
                  <svg class="w-4 h-4 text-white" viewBox="0 -960 960 960" fill="currentColor">
                    <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z"/>
                  </svg>
                {/if}
              </div>
              
              <!-- Message bubble -->
              <div class="flex flex-col {message.role === 'user' ? 'items-end' : 'items-start'}">
                <div class="px-4 py-3 rounded-2xl {message.role === 'user'
                  ? 'bg-blue-600 text-white rounded-br-md'
                  : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md'} shadow-sm">
                  <div class="whitespace-pre-wrap">{message.content}</div>
                </div>
                <!-- Timestamp -->
                <div class="text-xs text-gray-500 mt-1 px-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  {new Intl.DateTimeFormat('de-DE', {
                    hour: '2-digit',
                    minute: '2-digit',
                    day: '2-digit',
                    month: '2-digit'
                  }).format(message.timestamp)}
                </div>
              </div>
            </div>
          </div>
        </div>
      {/each}

      {#if isLoading || streamingContent}
        <div class="flex justify-start">
          <div class="max-w-[80%] group">
            <div class="flex items-end gap-2">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                <svg class="w-4 h-4 text-white" viewBox="0 -960 960 960" fill="currentColor">
                  <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z"/>
                </svg>
              </div>
              
              <!-- Streaming content or typing indicator -->
              <div class="px-4 py-3 rounded-2xl rounded-bl-md bg-white border border-gray-200 shadow-sm">
                {#if streamingContent}
                  <div class="whitespace-pre-wrap">
                    {streamingContent}<span class="animate-pulse">▊</span>
                  </div>
                {:else}
                  <div class="flex items-center space-x-1">
                    <div class="flex space-x-1">
                      <div class="w-2 h-2 bg-gray-400 rounded-full animate-pulse"></div>
                      <div class="w-2 h-2 bg-gray-400 rounded-full animate-pulse" style="animation-delay: 0.2s"></div>
                      <div class="w-2 h-2 bg-gray-400 rounded-full animate-pulse" style="animation-delay: 0.4s"></div>
                    </div>
                  </div>
                {/if}
              </div>
            </div>
          </div>
        </div>
      {/if}
    </div>

    <!-- Input Area -->
    <div class="p-4 border-t border-gray-200 bg-white">
      <div class="flex items-end space-x-2">
        <textarea
          class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 resize-none"
          placeholder="Ask me about this component..."
          rows="2"
          value={currentMessage}
          oninput={(e) => onMessageChange(e.target.value)}
          onkeydown={handleKeyDown}
        ></textarea>
        <button
          onclick={onSendMessage}
          disabled={!currentMessage.trim() || isLoading}
          class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          Send
        </button>
      </div>
    </div>
  </div>

  <!-- File Tree and Code Viewer -->
  <div class="w-3/5 flex h-[calc(100vh-180px)] overflow-hidden rounded-lg border border-gray-200 bg-white">
    <!-- File Tree -->
    <div class="w-72 overflow-auto border-r border-gray-200">
      <FileTree 
        component={selectedComponent}
        currentUnitId={selectedUnit?.id}
        onFileSelect={handleFileSelect}
      />
    </div>

    <!-- Code Viewer -->
    <div class="flex flex-1 overflow-hidden">
      {#if selectedUnit}
        <CodeViewer
          translationUnit={selectedUnit}
          {astNodes}
          {overlayNodes}
        />
      {:else}
        <div class="flex flex-1 items-center justify-center p-6">
          <div class="text-center">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1"
                d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
              />
            </svg>
            <h3 class="mt-2 text-sm font-medium text-gray-900">No file selected</h3>
            <p class="mt-1 text-xs text-gray-500">
              Select a translation unit from the sidebar to view its contents
            </p>
          </div>
        </div>
      {/if}
    </div>
  </div>
</div>
