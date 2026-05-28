# Story Book Loot

Current truth as of 2026-05-28:

- The eleven Witchery life stories from `C:\claude\witchery\stories` are available as vanilla `minecraft:written_book` loot entries.
- The book payloads live in `src/main/resources/data/witchery/loot_table/chests/village_story_books.json` and use `minecraft:set_book_cover` plus `minecraft:set_written_book_pages`, so no new item IDs are required.
- `witchery:village_story_books` is registered through NeoForge global loot modifiers and rolls at 18% chance for vanilla village chest loot tables, plus Witchery apothecary and bookshop chest tables.

QA note: tester found a story book in a new village and confirmed it looked correct. Still useful to spot-check the new mandrake eyewitness book when it appears.
