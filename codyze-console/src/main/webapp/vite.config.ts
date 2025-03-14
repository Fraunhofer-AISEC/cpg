// src/main/web/vite.config.ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "../resources/static",
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
