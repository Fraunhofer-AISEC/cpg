import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [
    tailwindcss(),
    sveltekit({
      preprocess: vitePreprocess(),
      // `$lib` was removed in SvelteKit 3 in favor of `#lib` subpath imports;
      // keep it as an alias to avoid rewriting imports across the codebase.
      alias: { $lib: 'src/lib' },
      adapter: adapter({
        fallback: 'index.html',
        pages: '../resources/static'
      })
    })
  ],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
});
