# LightShader (Minecraft 1.21.11, Fabric)

A small client-side shader engine built on vanilla's own post-effect pipeline. No Iris dependency,
no G-buffers, two fullscreen passes per frame.

## Build (GitHub only, nothing to install locally)
1. Create a new repo on GitHub and upload the *contents* of this folder (including `.github/`).
2. Open the **Actions** tab. The `Build` workflow runs on every push; you can also press **Run workflow**.
3. When it goes green, open the run and download the **lightshader-jar** artifact. Unzip it and drop
   the jar (not the `-sources` one) into your `mods` folder with Fabric API.
4. To publish a download page: **Releases > Draft a new release**, create a tag like `v0.1.0`, publish.
   The `Release` workflow builds and attaches the jar automatically.

If the build is red, open the failed step and read the error. Compile errors list the exact file and line.

Local alternative: `gradle build` (Gradle 9.6.0, Java 21) or `gradle runClient`.

## Controls (rebindable under Controls > LightShader)
| Key | Action |
|-----|--------|
| K | Cycle preset (Lite, Balanced, Cinematic, Vivid) |
| J | Shaders on/off |
| L | Settings screen (preset, auto quality, min FPS, reload) |
| ; | Reload shaders (same as F3+T, without the rest of the resource reload noise) |

Config lives in `config/lightshader.json`.

## What each pass does
`post/grade.fsh` is one uber shader. Each effect is skipped when its strength is 0.

1. Sharpen: 4 taps
2. Depth AO: 8 taps, noise-rotated ring, uses the main depth buffer
3. Bloom-lite: 16 bilinear taps, thresholded (no blur chain, so no extra render targets)
4. Distance haze: desaturates and cools far terrain, sky untouched
5. Grade: exposure with soft highlight roll-off, contrast, saturation, warmth
6. Vignette plus dither

`post/blit.fsh` copies the result back to the main target (a pass cannot read and write the same target).

## Auto quality
Optional. If FPS stays under the configured minimum for ~8 seconds it steps down one preset
(Vivid/Cinematic -> Balanced -> Lite -> off) and tells you on the action bar.

## Editing / adding presets (hot reload)
Presets are plain JSON in `assets/lightshader/post_effect/`. Change a value, rebuild (or edit the
jar/resource pack), press `;` in game. To add a preset: copy a JSON, add an entry to `Preset.java`,
add a lang key.

The uniform list order in each JSON must match the `LightShaderConfig` block in `grade.fsh` exactly.

## Tuning notes
* `Far` (default 512) should roughly match the game's far plane. AO radius falloff and haze depend on it.
  If haze starts too early/late or AO looks too strong at range, adjust `Far` in the preset JSON.
* Haze uses a neutral desaturate-and-cool tint, so it is deliberately subtle in the Nether/End.
* Spectating a creeper/spider/enderman uses vanilla's effect; LightShader steps aside and re-binds after.

## Compatibility
Works with Sodium (post effects are vanilla). Do not combine with an active Iris shader pack.

## Not included yet: waving foliage / water
Those need an override of vanilla `terrain.vsh/.fsh`, which must match 1.21.11's exact uniform blocks
and imports. Extract `assets/minecraft/shaders/core/terrain.*` from the 1.21.11 client jar and send
them over; I'll write the override against the real source instead of guessing.
