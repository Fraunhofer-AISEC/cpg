<script lang="ts">
    import type {ComponentJSON} from '$lib/types';

    interface Props {
        components: ComponentJSON[];
        onComponentSelect: (component: ComponentJSON) => void;
        onWelcomeMessage: (message: string) => void;
    }

    let {components, onComponentSelect, onWelcomeMessage}: Props = $props();

    let messageInput = $state('');

    // Suggested questions
    const suggestedQuestions = [
        "Show me all function calls in this component",
        "Where does sensitive data flow to?",
        "Show me all crypto function usage",
        "Trace data flows from sensitive inputs"
    ];

    function handleSendMessage() {
        if (messageInput.trim()) {
            onWelcomeMessage(messageInput);
            messageInput = '';
        }
    }
</script>

<div class="flex flex-col items-center justify-center min-h-[80vh] max-w-5xl mx-auto px-4">
    <!-- Welcome Header -->
    <div class="text-center mb-12">
        <div class="flex items-center justify-center gap-3 mb-6">
            <span class="text-4xl">👋</span>
            <h1 class="text-4xl font-bold text-gray-900">
                Hi, I'm your <span class="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">CodAIze Assistant</span>
                <div class="inline-block align-middle ml-3">
                    <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center">
                        <svg class="w-5 h-5 text-white" viewBox="0 -960 960 960" fill="currentColor">
                            <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z"/>
                        </svg>
                    </div>
                </div>
            </h1>
        </div>
        
        <p class="text-xl text-gray-600 max-w-2xl mx-auto leading-relaxed">
            I help you find security vulnerabilities, understand data flows, and analyze code dependencies using graph-based analysis.
        </p>
    </div>

    <!-- Component Cards -->
    {#if components.length > 0}
        <div class="w-full mb-8 flex justify-center">
            <div class="max-w-2xl w-full">
                <h2 class="text-xl font-semibold text-gray-800 mb-6 text-center">Choose a component to analyze</h2>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 justify-items-center">
                    {#each components as component}
                        <button
                            onclick={() => onComponentSelect(component)}
                            class="group p-5 bg-white rounded-xl border border-gray-200 hover:border-blue-300 hover:shadow-lg transition-all duration-300 text-left transform hover:-translate-y-1 w-full max-w-sm"
                            aria-label={`Select component ${component.name}`}
                        >
                            <div class="flex items-center mb-3">
                                <div class="w-10 h-10 bg-gradient-to-br from-blue-100 to-purple-100 rounded-lg flex items-center justify-center group-hover:from-blue-200 group-hover:to-purple-200 transition-all mr-3">
                                    <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                                        <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/>
                                    </svg>
                                </div>
                                <div class="flex-1">
                                    <h3 class="font-semibold text-gray-800 group-hover:text-blue-700 transition-colors">{component.name}</h3>
                                 </div>
                            </div>
                            <div class="flex items-center text-xs text-gray-500">
                                <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 24 24">
                                    <path d="M14,2H6A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2M18,20H6V4H13V9H18V20Z"/>
                                </svg>
                                {component.translationUnits?.length || 0} files
                            </div>
                        </button>
                    {/each}
                </div>
            </div>
        </div>
    {/if}

    <!-- Suggestions -->
    <div class="w-full max-w-3xl mb-8">
        <h3 class="text-lg font-medium text-gray-700 mb-4 text-center">Popular CPG analysis queries:</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            {#each suggestedQuestions as question}
                <button
                    type="button"
                    class="group p-4 text-left rounded-xl bg-gradient-to-r from-gray-50 to-blue-50 hover:from-blue-50 hover:to-purple-50 border border-gray-200 hover:border-blue-300 transition-all duration-300 hover:shadow-md"
                    onclick={() => onWelcomeMessage(question)}
                >
                    <div class="flex items-center">
                        <div class="w-6 h-6 bg-blue-100 group-hover:bg-blue-200 rounded-lg flex items-center justify-center mr-3 transition-colors">
                            <svg class="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
                            </svg>
                        </div>
                        <span class="text-sm font-medium text-gray-700 group-hover:text-blue-700 transition-colors">{question}</span>
                    </div>
                </button>
            {/each}
        </div>
    </div>

    <!-- Input Field -->
    <div class="w-full max-w-lg flex items-center">
        <input
            type="text"
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
            placeholder="Ask me anything about your code..."
            bind:value={messageInput}
            onkeydown={(e) => e.key === 'Enter' && handleSendMessage()}
        />
        <button
            class="ml-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all"
            onclick={handleSendMessage}
        >Send</button>
    </div>
</div>
