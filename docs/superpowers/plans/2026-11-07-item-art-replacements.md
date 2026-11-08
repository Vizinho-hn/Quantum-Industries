# Item Art Replacements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace seven existing Quantum Industries item icons with the user's supplied artwork.

**Architecture:** Convert the seven supplied JPEGs into centered, transparent 32×32 PNGs and overwrite only their corresponding existing item textures. Remove the flat background from each source while preserving its silhouette; do not change item registrations, models, recipes, names, or translations.

**Tech Stack:** Python 3 and the already available Pillow library for image processing; Minecraft resource-pack PNG textures.

**Spec:** `docs/superpowers/specs/2026-11-07-item-art-replacements-design.md`

## Global Constraints

- Replace existing textures only; do not register new items or change gameplay, recipes, names, models, or translations.
- Convert each supplied JPEG into a 32×32 transparent PNG suitable for Minecraft item rendering.
- Remove the flat background, crop and center the item silhouette, and preserve the recognizable artwork and outline.
- Keep all generated textures under `src/main/resources/assets/quantumindustries/textures/item/`.

## Review Focus

- Each supplied art must map to the approved item texture; validate the seven source/destination pairs in a conversion manifest or explicit mapping.
- Background colors differ across sources; verify the image border becomes transparent and the item silhouette remains opaque for each output.
- Centering or aspect-ratio mistakes could clip thin or diagonal art; verify all non-transparent bounds remain inside each 32×32 canvas.
- A failed conversion must not leave a partial or corrupt texture set; convert to temporary outputs and replace destination files only after all seven outputs pass validation.
- This is a texture-only change; verify only the seven approved PNGs changed and item identifiers/models remain untouched.

---

### Task 1: Replace the seven existing item textures

**Files:**
- Modify: `src/main/resources/assets/quantumindustries/textures/item/heated_copper_ingot.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/flour.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/energy_inspection_goggles.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/manual_hammer.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/iron_sheet.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/copper_sheet.png`
- Modify: `src/main/resources/assets/quantumindustries/textures/item/empirical_field_notes.png`

**Interfaces:**
- Consumes: the seven confirmed JPEG sources under `/home/guilherme/Downloads/`.
- Produces: seven valid 32×32 RGBA PNG textures at the paths above.

**Approved source mapping:**

| Source | Destination |
|---|---|
| `/home/guilherme/Downloads/Gemini_Generated_Image.jpeg` | `heated_copper_ingot.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (1).jpeg` | `flour.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (2).jpeg` | `energy_inspection_goggles.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (3).jpeg` | `manual_hammer.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (4).jpeg` | `iron_sheet.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (5).jpeg` | `copper_sheet.png` |
| `/home/guilherme/Downloads/Gemini_Generated_Image (6).jpeg` | `empirical_field_notes.png` |

- [ ] Confirm all seven input images and destination textures exist; record the original destination files before conversion so failed processing can leave them unchanged.
- [ ] Convert all seven images using edge-connected background removal, tight silhouette cropping, aspect-preserving placement on a transparent square canvas, and nearest-neighbor scaling to 32×32. Write outputs to a temporary directory, not over the existing textures.
- [ ] Validate the complete temporary output set: exactly seven files; each is a decodable 32×32 RGBA PNG with transparent canvas pixels, non-empty opaque artwork, and non-transparent bounds within the canvas. Check each image's silhouette visually.
- [ ] Only after every conversion passes, copy the seven outputs over their mapped destination textures.
- [ ] Verify the final files still meet the image checks, and confirm the only modified paths for this task are the seven approved textures; do not change item definitions, models, recipes, names, or translations.
- [ ] Run `git diff --check` and inspect the texture diff visually. Do not commit or push.
