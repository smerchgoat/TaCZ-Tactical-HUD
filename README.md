<p align="center"><img src="docs/images/logo.png" width="128" alt="TaCZ Tactical HUD logo"></p>

# TaCZ Tactical HUD

**English** · [Русский](README.ru.md)

An angled, perspective 3D gun HUD for **[TaCZ] Timeless and Classics Zero** (Minecraft 1.20.1, Forge), inspired by the interface of *Tom Clancy's The Division*.

The panel floats next to your character, rotated in space, and shows everything you need in a fight: ammo, reserve, armor, health, fire mode, reload, overheat, grenades and medkits.

> Version: **0.1.0-alpha** · Minecraft **1.20.1** · Forge **47.x** · TaCZ **1.1.8+** · client only
>
> Not affiliated with or endorsed by Ubisoft or the TaCZ team.

![Third person](docs/images/third_person.jpg)

![First person](docs/images/first_person.jpg)

## Features

- **Real 3D**: the panel is drawn with a perspective projection, so one edge is closer and the other farther away, like an SHD agent's HUD.
- **Separate first- and third-person layouts**: position, scale, angles, perspective, sway and opacity are set per view. Pressing F5 smoothly morphs the panel from one layout to the other.
- **Live animation**: sway when turning the camera, shake on damage, a "ghost" health bar, a kick on every shot, a boot-up animation and pulsing warnings.
- **Everything in one place**: rounds in the magazine and chamber, reserve, spare magazines, ammo of other guns in the hotbar, fire mode, reload, overheat, armor, health, grenades, medkits.
- **Clean screen if you want it**: hide vanilla hearts, armor, hotbar, experience and food while the HUD is shown.
- **Fits any screen**: `Auto` (follows screen height) or fixed `x1`–`x4`.
- **In-game sliders** via Cloth Config (optional dependency).
- **Data-driven**: what counts as a grenade or a medkit is defined by item tags and can be extended with a datapack.

## Quick start

1. Install Forge 47.x for Minecraft 1.20.1.
2. Put the TaCZ jar and `tacz_tactical_hud-1.20.1-0.1.0-alpha.jar` into `mods/`.
3. Optionally add Cloth Config 11.x to get the settings menu.
4. Pick up a TaCZ gun. In third person (F5) the panel sits by the character's hip.

Settings: **Mods → TaCZ Tactical HUD → Config**, or a keybind in **Controls → TaCZ Tactical HUD** (unbound by default).

## Documentation

| File | Contents |
|---|---|
| [docs/HOW_IT_WORKS.md](docs/HOW_IT_WORKS.md) | What each element shows and how rendering, animation and data work |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | Installation, all settings, tags, FAQ, troubleshooting |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Building, running in an IDE, code layout, adding elements, releasing |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

## Building

```bash
# once: put the TaCZ jar into libs/ (see docs/DEVELOPMENT.md)
./gradlew build
# output: build/libs/tacz_tactical_hud-1.20.1-0.1.0-alpha.jar
```

## License

The code is licensed under **GPL-3.0** (see [LICENSE](LICENSE)) because part of the ammo counting logic is ported from TaCZ, which is GPL-3.0 as well. The Rajdhani font is licensed under the SIL Open Font License 1.1 (see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)).
