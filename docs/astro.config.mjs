import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  site: "https://matejstastny.github.io",
  base: "/flaggi",
  integrations: [
    starlight({
      title: "Flaggi",
      favicon: "/favicon.png",
      logo: {
        src: "./src/assets/duck.png",
        alt: "Flaggi Duck",
      },
      customCss: ["./src/styles/custom.css"],
      social: [
        {
          icon: "github",
          label: "GitHub",
          href: "https://github.com/matejstastny/flaggi",
        },
      ],
      sidebar: [
        {
          label: "Getting Started",
          items: [
            { label: "Overview", slug: "introduction" },
            { label: "Setup & Running", slug: "getting-started" },
          ],
        },
        {
          label: "Architecture",
          items: [
            { label: "Project Structure", slug: "architecture/structure" },
            { label: "Networking", slug: "architecture/networking" },
            { label: "Protobuf Protocol", slug: "architecture/protocol" },
          ],
        },
        {
          label: "Client",
          items: [
            { label: "Overview", slug: "client/overview" },
            { label: "Game Renderer", slug: "client/game-ui" },
            { label: "Sprite System", slug: "client/sprites" },
            { label: "Game Manager", slug: "client/game-manager" },
          ],
        },
        {
          label: "Server",
          items: [
            { label: "Overview", slug: "server/overview" },
            { label: "Game Logic", slug: "server/game-logic" },
            { label: "Database", slug: "server/database" },
          ],
        },
        {
          label: "Shared",
          items: [
            { label: "Utilities", slug: "shared/utilities" },
          ],
        },
        {
          label: "Building & Distribution",
          items: [
            { label: "Dev Environment", slug: "building/dev-environment" },
            { label: "Packaging", slug: "building/packaging" },
          ],
        },
      ],
    }),
  ],
});
