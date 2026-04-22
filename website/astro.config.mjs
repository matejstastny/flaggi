import { defineConfig } from "astro/config"
import starlight from "@astrojs/starlight"

export default defineConfig({
    site: "https://matejstastny.github.io",
    base: "/flaggi",
    srcDir: "./",
    integrations: [
        starlight({
            title: "Flaggi",
            favicon: "/favicon.png",
            logo: {
                src: "./assets/duck.png",
                alt: "Flaggi Duck",
                replacesTitle: true
            },
            customCss: ["./styles/custom.css"],
            head: [
                {
                    tag: "script",
                    content: `
                        if (!localStorage.getItem('starlight-theme')) {
                            localStorage.setItem('starlight-theme', 'dark');
                        }
                    `
                }
            ],
            social: [
                {
                    icon: "github",
                    label: "GitHub",
                    href: "https://github.com/matejstastny/flaggi"
                }
            ],
            sidebar: [
                {
                    label: "Getting Started",
                    items: [{ label: "Overview", slug: "getting-started/overview" }]
                }
            ]
        })
    ]
})
