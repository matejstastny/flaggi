import path from "node:path"
import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"

export default defineConfig({
    root: path.resolve(__dirname, "renderer"),
    base: "./",
    plugins: [react()],
    build: {
        outDir: "dist",
        emptyOutDir: true
    }
})
