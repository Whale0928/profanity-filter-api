import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const openApiYamlPath = resolve(__dirname, "../openapi.yaml");

function readOpenApiYaml() {
  return readFileSync(openApiYamlPath, "utf-8");
}

export default defineConfig({
  plugins: [
    react(),
    {
      name: "serve-openapi-yaml",
      configureServer(server) {
        server.middlewares.use("/openapi.yaml", (request, response, next) => {
          if (request.method !== "GET" && request.method !== "HEAD") {
            next();
            return;
          }

          response.setHeader("Content-Type", "text/yaml; charset=utf-8");
          if (request.method === "HEAD") {
            response.end();
            return;
          }
          response.end(readOpenApiYaml());
        });
      },
      closeBundle() {
        const distPath = resolve(__dirname, "dist");
        mkdirSync(distPath, { recursive: true });
        writeFileSync(resolve(distPath, "openapi.yaml"), readOpenApiYaml());
      },
    },
  ],
  server: {
    port: 5173,
  },
});
