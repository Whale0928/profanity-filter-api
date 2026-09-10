import { resolve } from "node:path";

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

import { fontDisplayOptional, preloadShellFont } from "./vite-plugins";

export default defineConfig({
  plugins: [react(), fontDisplayOptional(), preloadShellFont()],
  build: {
    rollupOptions: {
      input: {
        landing: resolve(__dirname, "index.html"),
        docs: resolve(__dirname, "docs/index.html"),
      },
    },
  },
  server: {
    host: "0.0.0.0",
    allowedHosts: ["terminal.local"],
    port: 5173,
  },
});
