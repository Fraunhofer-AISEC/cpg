<script lang="ts">
  import MessageInput from './MessageInput.svelte';
  import type { McpCapabilities } from '$lib/types';

  interface Props {
    onWelcomeMessage: (message: string) => void;
    mcpCapabilities?: McpCapabilities | null;
    onOpenMcp?: () => void;
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
  }

  let { onWelcomeMessage, mcpCapabilities, onOpenMcp, onPromptSelect }: Props = $props();

  let messageInput = $state('');

  const suggestedQuestions = [
    'List all functions and their parameters',
    'What classes are defined?',
    'Which function implements AES encryption?',
    'What functions are called by the main entry point?'
  ];

  function handleSendMessage() {
    if (messageInput.trim()) {
      onWelcomeMessage(messageInput);
      messageInput = '';
    }
  }
</script>

<div class="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center px-4 py-8">
  <!-- Welcome Header -->
  <div class="mb-8 text-center">
    <div class="mb-4 flex items-center justify-center gap-3">
      <h1 class="text-3xl font-bold text-gray-900 sm:text-4xl">
        <span
          class="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent"
          >CodAIze Agent</span
        >
        <div class="ml-3 inline-block align-middle">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-purple-600"
          >
            <svg class="h-5 w-5 text-white" viewBox="0 -960 960 960" fill="currentColor">
              <path
                d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z"
              />
            </svg>
          </div>
        </div>
      </h1>
    </div>
  </div>

  <!-- Suggestions -->
  <div class="mb-8 w-full max-w-4xl">
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
      {#each suggestedQuestions as question}
        <button
          type="button"
          class="group rounded-2xl border border-gray-200 bg-white p-4 text-left shadow-sm transition-all duration-200 hover:border-blue-400 hover:bg-gradient-to-br hover:from-blue-50 hover:to-purple-50 hover:shadow-md"
          onclick={() => onWelcomeMessage(question)}
        >
          <div class="flex items-start gap-3">
            <div
              class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blue-50 to-purple-50 transition-all group-hover:from-blue-100 group-hover:to-purple-100"
            >
              <svg class="h-4 w-4 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z" />
              </svg>
            </div>
            <span class="font-medium text-gray-800 transition-colors group-hover:text-gray-900"
              >{question}</span
            >
          </div>
        </button>
      {/each}
    </div>
  </div>

  <!-- Input Field -->
  <div class="w-full max-w-4xl">
    <MessageInput
      value={messageInput}
      onSend={handleSendMessage}
      onValueChange={(value) => messageInput = value}
      placeholder="Ask me anything about your codebase..."
      prompts={mcpCapabilities?.prompts}
      onPromptSelect={onPromptSelect}
    />
    {#if mcpCapabilities && onOpenMcp}
      <div class="mt-2 flex items-center">
        <button
          class="flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
          onclick={onOpenMcp}
          title="MCP Server"
        >
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.559.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.894.149c-.424.07-.764.383-.929.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.398.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.272-.806.108-1.204-.165-.397-.506-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {mcpCapabilities.serverName}
        </button>
      </div>
    {/if}
  </div>
</div>
