---
title: Docs Website
description: How the Flaggi docs website is structured and how to edit or add to it.
---

## Local preview

```bash title="Docs terminal"
cd website
npm install
npm run dev
```

## Where things live

| Path                        | Purpose                              |
| --------------------------- | ------------------------------------ |
| `website/content/docs/`     | Starlight docs pages                 |
| `website/styles/custom.css` | Shared docs theme overrides          |
| `website/astro.config.mjs`  | Starlight sidebar and site config    |
| `website/pages/`            | Landing pages and other Astro routes |

## Adding a page

1. Create a new `.md` file under `website/content/docs/...`
2. Add frontmatter with a title and description
3. Link it in `website/astro.config.mjs` if you want it in the sidebar
4. Use Starlight features like code block titles, tables, and admonitions to make the page easier to scan

## Styling tips

- Use `title="..."` on fenced code blocks to give commands a terminal look
- Keep tables short and focused
- Put the main action near the top of the page
- Use blockquotes for tips, notes, and warnings to make them stand out. You can use `:::tip`, `:::note`, and `:::warning` for different styles. Here is an example of a tip:

```bash title="Example tip"
:::tip[Tip]
This is a helpful tip to keep in mind.
:::
```
