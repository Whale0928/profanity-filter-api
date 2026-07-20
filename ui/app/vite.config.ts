import { resolve } from "node:path";

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
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
