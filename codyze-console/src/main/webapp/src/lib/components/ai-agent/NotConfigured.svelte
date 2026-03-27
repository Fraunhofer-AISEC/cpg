<script>
</script>

<div class="flex flex-1 items-center justify-center p-8">
  <div class="max-w-lg w-full rounded-xl border border-amber-200 bg-amber-50 p-8">
    <div class="mb-4 flex items-center gap-3">
      <svg class="h-6 w-6 text-amber-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
      </svg>
      <h2 class="text-lg font-semibold text-amber-800">Agent features not configured</h2>
    </div>

    <p class="mb-6 text-sm text-amber-700">
      The MCP module is enabled, but no configuration file was found. Copy
      <code class="rounded bg-amber-100 px-1 py-0.5 font-mono text-xs">application.conf.example</code>
      to
      <code class="rounded bg-amber-100 px-1 py-0.5 font-mono text-xs">application.conf</code>
      and configure the following, then restart the server:
    </p>

    <div class="space-y-4">
      <div class="rounded-lg border border-amber-200 bg-white p-4">
        <h3 class="mb-2 text-sm font-semibold text-gray-800">LLM Provider</h3>
        <p class="mb-2 text-xs text-gray-500">Choose one of the supported providers and set the model and base URL:</p>
        <ul class="space-y-1 text-xs text-gray-600">
          <li><span class="font-mono font-medium">ollama, vLLM, or mlx</span> — OpenAI-compatible endpoints, e.g. <span class="font-mono">http://localhost:11434</span></li>
          <li><span class="font-mono font-medium">gemini</span> — requires <span class="font-mono">GEMINI_API_KEY</span> env variable</li>
        </ul>
      </div>

      <div class="rounded-lg border border-amber-200 bg-white p-4">
        <h3 class="mb-2 text-sm font-semibold text-gray-800">MCP Server</h3>
        <p class="text-xs text-gray-600">
          Set <span class="font-mono font-medium">mcp.serverUrl</span> — default is
          <span class="font-mono">http://localhost:8081/mcp</span>.
        </p>
      </div>
    </div>
  </div>
</div>