# Story Book Loot

Current truth as of 2026-05-28:

- The ten Witchery life stories from `C:\claude\witchery\stories` are available as vanilla `minecraft:written_book` loot entries.
- The book payloads live in `src/main/resources/data/witchery/loot_table/chests/village_story_books.json` and use `minecraft:set_book_cover` plus `minecraft:set_written_book_pages`, so no new item IDs are required.
- `witchery:village_story_books` is registered through NeoForge global loot modifiers and rolls at 18% chance for vanilla village chest loot tables, plus Witchery apothecary and bookshop chest tables.

QA note: verify in fresh village/apothecary/bookshop chests that books can appear, keep their title/author, and open with readable paged text.
