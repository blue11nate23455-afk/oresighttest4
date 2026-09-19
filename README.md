# OreSight

A client-side Fabric mod for **Minecraft 1.21.11**. It highlights chosen
blocks (ores, ancient debris, structures, containers, whatever you enable)
through terrain, while the terrain itself stays completely normal and
opaque -- not a traditional "invisible stone" X-ray mod. Single-player use.

Everything is here: full source, build config, and a GitHub Actions
workflow that compiles the `.jar` for you in the cloud if you don't have
Java/Gradle installed locally.

---

## Getting the .jar -- Option A: GitHub Actions (no Java/Gradle needed)

This repo already contains `.github/workflows/build.yml`, which builds the
mod automatically on GitHub's own servers (which have full internet access,
unlike a locked-down sandbox). You never install anything.

1. Go to https://github.com/new and create a new repository (any name,
   public or private).
2. On the new repo's page, click **"Add file" -> "Upload files"**, then
   drag this entire project folder (everything inside `oresight-mod/`,
   including the hidden `.github` folder) into the browser window.
   - If your OS hides the `.github` folder in the file picker, drag it
     into the browser window separately, or use `git` from a terminal
     instead (see the box below) -- either way it must end up committed.
   - GitHub's uploader preserves folder structure when you drag folders in.
3. Commit the files (the box at the bottom of the upload page).
4. Click the **"Actions"** tab at the top of the repo. A workflow run
   called "Build OreSight Mod" should already be running (pushing to
   `main` triggers it automatically). If it isn't listed yet, click
   **"build.yml"** in the left sidebar, then **"Run workflow"**.
5. Wait for the run to finish (a green checkmark, usually 2-4 minutes).
6. Open the finished run, scroll to the **"Artifacts"** section at the
   bottom, and download **`oresight-jar`**. It's a zip containing the
   actual mod file, `oresight-1.0.0.jar`.
7. Unzip it and drop `oresight-1.0.0.jar` into your `mods` folder (see
   Installation below).

<details>
<summary>If you'd rather use a terminal instead of drag-and-drop</summary>

```bash
cd oresight-mod
git init
git add .
git commit -m "Initial OreSight mod"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```
Then follow steps 4-7 above.
</details>

If the Actions build fails, open the failed step's log and paste the error
back to me -- see "If something doesn't build" at the bottom of this file
for the one file that's most likely to need a small tweak.

---

## Getting the .jar -- Option B: build locally

If you do have (or want) Java + Gradle installed:

1. Install a **JDK 21** (e.g. Temurin/Adoptium, or `openjdk-21-jdk`).
2. Install Gradle (https://gradle.org/install/), or use the official
   Fabric template generator at https://fabricmc.net/develop/template/
   (pick Minecraft 1.21.11, Fabric Loader 0.19.2) to get a project with a
   working `gradlew` wrapper already generated, then copy this project's
   `src/` folder (and `oresight.accesswidener`) on top of it.
3. From the project folder:
   ```bash
   gradle build
   ```
4. The finished jar appears at `build/libs/oresight-1.0.0.jar`.

Before building, double-check the version pins at the top of
`gradle.properties` -- Minecraft/Fabric ship new builds constantly, and the
comments there link to where to verify each number.

---

## Installation (once you have the .jar)

1. Install **Fabric Loader** for Minecraft 1.21.11 from
   https://fabricmc.net/use/installer/ (choose the vanilla launcher
   profile, or your launcher's Fabric option).
2. Install **Fabric API** for 1.21.11 from Modrinth or CurseForge, and
   place that `.jar` in your `mods` folder too -- OreSight depends on it.
3. Put `oresight-1.0.0.jar` in the same `mods` folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
4. Launch Minecraft using the **Fabric Loader 1.21.11** profile.
5. In-game, press **O** to toggle highlighting on/off (default key).
6. Open **Options -> Controls** to see/change the three OreSight keybinds
   under the "OreSight" category:
   - Toggle Block Highlighting (default: O)
   - Open OreSight Menu (unbound by default -- bind whatever you like)
   - Hold to Temporarily Hide Highlights (unbound by default)
7. Bind "Open OreSight Menu" to a key, then press it in-game to open the
   settings screen: General / Blocks / Appearance / Animation / Profiles
   tabs, all described in the feature list below.

---

## What's actually in here

- **Highlighting**: chosen blocks are drawn with an outline, filled glow,
  or both, visible through terrain, while the terrain around them stays
  fully normal. Two modes: "Through Terrain" (always visible in range) and
  "Exposed Blocks Only" (only blocks with at least one non-opaque
  neighbor -- i.e. not fully buried).
- **Blocks screen**: search box, scrollable list with a per-block ON/OFF
  toggle and a color swatch (click it to open an RGB picker), Enable
  All / Disable All, one-click category presets (All Ores, Valuable Ores,
  Common Ores, Ancient Debris, Structures, Containers), and a
  "modid:block_id" field to add any modded block by id.
- **Appearance**: highlight style (outline / solid / glow / outline+glow),
  brightness, fill opacity, outline opacity, outline thickness, glow
  intensity -- all sliders.
- **Animation**: optional pulse effect with adjustable speed.
- **Range & performance**: adjustable range slider with distance fade;
  block scanning is spread across ticks and limited to chunks near the
  player rather than scanning the whole loaded world every frame.
- **Profiles**: save/load named configurations (e.g. "Mining",
  "Structures") in addition to the single active config.
- **HUD**: optional small "Block Highlighting: ON/OFF" corner label, and
  an optional block-name readout when you look directly at a highlighted
  block.
- **Config persistence**: everything is saved to
  `config/oresight/config.json` (and `config/oresight/profiles/*.json`)
  and reloaded automatically next launch.
- **Purely visual**: no block/world modification, no auto-mining, no drop
  changes, no other gameplay effects. Client-side rendering only.

---

## If something doesn't build

Almost the entire mod uses ordinary, stable Fabric API calls. There is
exactly one file that reaches into Minecraft's internal rendering classes
to make highlights render through terrain, and it's the one place version
drift could realistically cause a compile error:

**`src/main/java/com/oresight/render/RenderLayers.java`** (and the matching
entries in `src/main/resources/oresight.accesswidener`).

Both files have a comment block explaining exactly what to check (Fabric
Loom's `genSources` Gradle task lets you view the real decompiled classes
for your exact build). If the Actions log or your local build points at
this file, paste me the error and I'll adjust it -- the fix is contained
to those two files and doesn't touch anything else in the mod.
