<script lang="ts">
    import { page } from '$app/state';
  </script>
  
  <div class="container mx-auto p-4">
    <div class="rounded bg-red-100 p-3 text-red-700">
      {page.error?.message}
    </div>
    <div class="mt-4">
      <a href="/" class="text-blue-600 hover:underline">Back to Dashboard</a>
    </div>
  </div>
  