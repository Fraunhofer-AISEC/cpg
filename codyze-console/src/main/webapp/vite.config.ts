import { sveltekit } from '@sveltejs/kit/vite';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [tailwindcss(), sveltekit()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 0,
        proxyTimeout: 0,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            // Ensure connection stays alive for SSE
            proxyReq.setHeader('Connection', 'keep-alive');
          });
        }
      }
    }
  }
});
