# Item Art Replacements

## Intent

Replace seven existing Quantum Industries item icons with the user's supplied artwork because the current icons are visually unsatisfactory.

## Goals and constraints

- Replace existing textures only; do not register new items or change gameplay, recipes, names, models, or translations.
- Convert each supplied JPEG into a 32×32 transparent PNG suitable for Minecraft item rendering.
- Remove the flat background, crop and center the item silhouette, and preserve the recognizable artwork and outline.
- Keep all generated textures under `src/main/resources/assets/quantumindustries/textures/item/`.

## Approaches

- **Use the supplied artwork, clean and export at 32×32 (chosen):** preserves the user's intended designs and more detail than the existing 16×16 icons.
- **Export at 16×16:** matches current icon resolution but loses more of the supplied detail.
- **Redraw the icons:** offers tighter pixel-art consistency but replaces rather than uses the user's designs.

## Approved mapping

| Supplied image | Existing item texture |
|---|---|
| `Gemini_Generated_Image.jpeg` | `heated_copper_ingot.png` |
| `Gemini_Generated_Image (1).jpeg` | `flour.png` |
| `Gemini_Generated_Image (2).jpeg` | `energy_inspection_goggles.png` |
| `Gemini_Generated_Image (3).jpeg` | `manual_hammer.png` |
| `Gemini_Generated_Image (4).jpeg` | `iron_sheet.png` |
| `Gemini_Generated_Image (5).jpeg` | `copper_sheet.png` |
| `Gemini_Generated_Image (6).jpeg` | `empirical_field_notes.png` |

## Success criteria

- All seven replacement files are 32×32 RGBA PNGs with transparent backgrounds.
- Existing item IDs, models, recipes, and translations remain unchanged.
- The target image files render correctly in the resource pack and are reviewed visually before delivery.
