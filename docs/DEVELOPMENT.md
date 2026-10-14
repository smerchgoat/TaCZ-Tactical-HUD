# Development

**English** · [Русский](DEVELOPMENT.ru.md)

## Environment

| What | Version |
|---|---|
| JDK | 17 (64-bit) |
| Gradle | 7.5.1 via the wrapper (`gradlew` / `gradlew.bat`) |
| ForgeGradle | 5.1.x |
| Forge | 1.20.1-47.3.19 (for development; any 47.x works at runtime) |
| Mappings | Parchment 2023.08.20-1.20.1 on top of official Mojang mappings |
| IDE | IntelliJ IDEA: *File → Open* → select `build.gradle` → *Open as Project* |

Versions are kept in `gradle.properties`.

## TaCZ dependency

TaCZ is not published to a maven repository, so its jar goes into `libs/` (the jar is in `.gitignore` and is not committed).

1. Build TaCZ from source: run `./gradlew jarJar` in its folder. You get `build/libs/tacz-1.20.1-<version>-all.jar` — a reobfuscated jar with all nested libraries. The official TaCZ release jar works too.
2. Copy it into `libs/` of this project.
3. Check that `tacz_version` in `gradle.properties` matches the part of the file name after `tacz-1.20.1-`. For example, `libs/tacz-1.20.1-1.1.8-hotfix2-all.jar` corresponds to `tacz_version=1.1.8-hotfix2-all`.

`fg.deobf(...)` remaps the jar to development mappings, so TaCZ works both at compile time and in `runClient`. Its mixins apply thanks to the `mixin.env.remapRefMap` properties in `build.gradle`.

## Commands

```bash
./gradlew build                      # jar in build/libs/
./gradlew runClient                  # development client (run/ folder)
./gradlew runClient -PquickPlay="New World"   # join a world from run/saves right away
```

In IntelliJ, after importing, run the `genIntellijRuns` task (Gradle tab → `forgegradle runs`) and launch the `runClient` configuration. For debugging use the same configuration via Debug; HotSwap works for changes inside methods.

## Layout

```
src/main/java/com/tacticalhud/
├── TacticalHud.java              @Mod: registers the config, calls ClientSetup on the client
├── client/
│   ├── ClientSetup.java          overlay, settings keybind, config screen
│   └── hud/
│       ├── TacticalHudOverlay    IGuiOverlay: snapshot → animation → perspective → painting
│       ├── HudVisibility         single "should the panel be shown now" check
│       ├── HudElementHider       cancels the TaCZ HUD and the selected vanilla overlays
│       ├── HudSnapshot(Factory)  immutable per-frame data from TaCZ and the player
│       ├── AmmoCache             magazine capacity and reserve (TaCZ HUD logic)
│       ├── HudLayout             camera profile → screen numbers, profile blending
│       ├── HudAnimator           everything time-dependent
│       ├── HudPerspective        enabling and disabling the perspective projection
│       ├── HudPainter            panel layout and painting
│       ├── HudIcons              vector icons
│       └── HudPalette            colors
├── compat/cloth/ClothConfigScreen  Cloth Config menu (loaded only if the mod is present)
└── config/
    ├── HudConfig                 ForgeConfigSpec: general flags + two profiles
    ├── HudProfile                options of one camera view, ranges
    └── HudGuiScale               Auto / x1–x4
src/main/resources/
├── META-INF/mods.toml
├── assets/tacz_tactical_hud/font/hud.json (+ hud/rajdhani_medium.ttf)
├── assets/tacz_tactical_hud/lang/{en_us,ru_ru}.json
└── data/tacz_tactical_hud/tags/items/{grenades,medkits}.json
```

### Frame flow

```
TacticalHudOverlay.render
 ├─ HudLayout.of(first) / HudLayout.of(third)       config → screen numbers
 ├─ HudVisibility.shouldRender ? HudSnapshotFactory.capture : null
 │     └─ AmmoCache.refresh (≤ once per tick, syncs inventory-fed ammo)
 ├─ HudAnimator.update(snapshot, …)                 time, sway, transitions
 └─ draw: HudPerspective.begin → HudPainter.paint → HudPerspective.end (in finally)
```

## Code rules

- Client only: everything in `client/` and `compat/` is loaded only on `Dist.CLIENT`; `TacticalHud` and `config/` do not import `net.minecraft.client.*`.
- Cloth Config is an optional dependency. Its API is used only in `ClothConfigScreen`, which is called only after `ModList.isLoaded("cloth_config")`.
- No mixins: everything goes through Forge events and overlays.
- TaCZ internal classes (`GunDisplayInstance`, `GunData`, `AttachmentDataUtils`, etc.) are a likely breaking point when TaCZ updates. With a new TaCZ version, rebuild the addon against it first.
- Do not create textures or resources and do not touch game state while rendering. The only exception is the ammo sync in `AmmoCache`, which mirrors TaCZ behavior.
- New layout numbers go into well-named constants in `HudPainter`.

## Adding an element to the panel

1. Add a field to `HudSnapshot` and fill it in `HudSnapshotFactory.capture`.
2. If the element needs state over time (blinking, smoothing), add it to `HudAnimator`.
3. Paint it in `HudPainter`: rectangles via `fill`/`fineFill` inside `drawManaged`, text via `text(...)`, textures after the `drawManaged` block, as in `paintGunSlot`.
4. If the element is configurable, add an option to `HudConfig` or `HudProfile`, a slider to `ClothConfigScreen` and strings to both `lang` files.

## Releasing a version

1. Bump `mod_version` in `gradle.properties` and update `CHANGELOG.md`.
2. `./gradlew clean build`.
3. Test in a clean profile: Forge + TaCZ + the addon, then with and without Cloth Config.
4. Scenarios: first and third person, F5 back and forth, shooting down to zero, reloading, overheating, an inventory-fed gun, creative, changing the GUI scale, minimizing the window, joining a dedicated server without the addon.
5. Create a GitHub release and upload `build/libs/tacz_tactical_hud-1.20.1-<version>.jar` (not the `-sources` one) to it, then to Modrinth and CurseForge.
