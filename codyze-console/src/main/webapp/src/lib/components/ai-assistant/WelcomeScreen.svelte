<script lang="ts">
  import MessageInput from './MessageInput.svelte';

  interface Props {
    onWelcomeMessage: (message: string) => void;
  }

  let { onWelcomeMessage }: Props = $props();

  let messageInput = $state('');

  // Suggested questions - project-wide
  const suggestedQuestions = [
    'List all functions in the project',
    'Find security vulnerabilities',
    'Analyze data flows from user inputs',
    'Show all external dependencies'
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
      <span class="text-3xl sm:text-4xl">👋</span>
      <h1 class="text-3xl font-bold text-gray-900 sm:text-4xl">
        Hi, I'm your <span
          class="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent"
          >CodAIze Assistant</span
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

    <p class="mx-auto mb-8 max-w-2xl text-lg leading-relaxed text-gray-600 sm:text-xl">
      I help you find security vulnerabilities, understand data flows, and analyze code dependencies
      using graph-based analysis.
    </p>
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
    />
  </div>
</div>
