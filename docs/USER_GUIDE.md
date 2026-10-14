# User guide

**English** · [Русский](USER_GUIDE.ru.md)

## Requirements

| Component | Version | Required |
|---|---|---|
| Minecraft | 1.20.1 | yes |
| Forge | 47.x | yes |
| [TaCZ] Timeless and Classics Zero | 1.1.8 or newer | yes |
| Cloth Config API (Forge) | 11.x | no: only needed for the in-game settings menu |

The mod is client-only: you do not need to install it on the server, and you can play with it on any server that has TaCZ.

## Installation

1. Copy `tacz_tactical_hud-1.20.1-0.1.0-alpha.jar` into the `mods/` folder.
2. Make sure TaCZ and, optionally, Cloth Config are there too.
3. Launch the game and pick up any TaCZ gun.

## Opening the settings

- **Mods → TaCZ Tactical HUD → Config** (the button is available when Cloth Config is installed);
- or bind a key: **Options → Controls → TaCZ Tactical HUD → "Open HUD settings"**.

Without Cloth Config, edit the settings in `config/tacz_tactical_hud-client.toml`. Changes from the menu apply immediately; it is better to edit the file by hand while the game is closed.

## Settings

### General (`[general]`)

| Menu option | Key in the file | Default | What it does |
|---|---|---|---|
| Hide health and armor | `HideHealthAndArmor` | yes | Hides vanilla hearts and armor while the panel is visible (the panel shows them itself) |
| Hide hotbar | `HideHotbar` | no | Hides the hotbar and the popup name of the selected item |
| Hide experience | `HideExperience` | no | Hides the experience bar and the level number |
| Hide food | `HideFood` | no | Hides the food bar |

Everything hidden comes back as soon as you put the gun away.

### "First person" (`[first_person]`) and "Third person" (`[third_person]`) profiles

Each camera view has its own set of options. In third person the panel sits by the character's hip by default, with the right edge receding into the distance. In first person the panel is to the right of the gun and it is the other way round: the left edge recedes towards the front sight and the right edge is closer to the screen.

| Option | Key | Range | 1st person | 3rd person | What it does |
|---|---|---|---|---|---|
| Enabled | `Enabled` | yes/no | yes | yes | Show the panel in this view; when off, the classic TaCZ HUD stays |
| GUI size | `GuiScale` | Auto, x1–x4 | Auto | Auto | Auto — size follows screen height; xN — as at GUI scale N |
| Horizontal position | `AnchorX` | 0–100% | 72% | 70% | Where the panel center is across the screen width |
| Vertical position | `AnchorY` | 0–100% | 62% | 62% | Where the panel center is along the screen height |
| Scale | `Scale` | 0.25–4.00x | 1.25x | 1.25x | Size multiplier |
| Yaw angle | `Yaw` | −60…60° | −18° | 18° | "+" pushes the right edge away, "−" the left edge |
| Tilt angle | `Tilt` | −45…45° | −6° | −6° | "−" pushes the top edge away |
| Perspective FOV | `Fov` | 10–110° | 40° | 40° | Lower is flatter, higher gives a stronger "fisheye" |
| Sway strength | `SwayStrength` | 0–3.00 | 1.00 | 1.00 | How much the panel lags behind camera rotation; 0 disables sway |
| Background opacity | `BackgroundOpacity` | 0–100% | 72% | 72% | The dark backing of the panel |

Every slider can be reset with the **Reset** button on its right.

**Tuning tips**

- Want a flat HUD without 3D? Set `Yaw` and `Tilt` to 0.
- The panel covers your character? Increase `AnchorX` or reduce `Scale`.
- On an ultrawide monitor `GuiScale = x2` or `x3` works better: the size stops depending on the window height.
- Feeling motion sick? Lower `SwayStrength` or set it to 0.

## Grenades and medkits: item tags

The counters count items from two tags:

| Tag | Default |
|---|---|
| `tacz_tactical_hud:grenades` | `splash_potion`, `lingering_potion`, `snowball`, `egg`, `fire_charge` |
| `tacz_tactical_hud:medkits` | `golden_apple`, `enchanted_golden_apple` |

You can add your own items (for example grenades or bandages from another mod) with a datapack:

`data/tacz_tactical_hud/tags/items/medkits.json`
```json
{
  "replace": false,
  "values": [
    { "id": "firstaid:bandage", "required": false }
  ]
}
```

`"required": false` keeps the tag from breaking if the mod is missing. `"replace": true` replaces the default list completely.

## FAQ

**There is no panel.**
Check in order:
1. you are holding a TaCZ gun, not ammo or an attachment;
2. the gun HUD is enabled in the TaCZ settings;
3. the profile for the current camera view has `Enabled = yes`;
4. you are not in spectator mode.

**The old TaCZ HUD shows up in first person.**
The "First person" profile is disabled — enable it.

**Why does the orange cell show "--"?**
This gun feeds directly from the inventory: it has no magazine, so there are no spare magazines either.

**Why does the reserve show ∞?**
You are in creative, the gun has an infinite reserve, or there is a creative ammo box in your inventory.

**Does it work with shaders, Shoulder Surfing and other HUD mods?**
The panel is drawn as a regular Forge overlay on top of everything, so shaders do not affect it. With Shoulder Surfing the third-person profile is used. Mods that also hide or move the vanilla HUD may conflict with the hiding toggles — in that case turn them off in one of the mods.

**Does it affect performance?**
Barely: one pass over the inventory per frame and a few dozen rectangles. Magazine capacity and reserve are recalculated at most once per tick.

## If something breaks

Attach to your bug report:
- `logs/latest.log` (or `crash-reports/…`);
- Minecraft, Forge, TaCZ and addon versions;
- your mod list;
- the `config/tacz_tactical_hud-client.toml` file;
- a screenshot and what you were doing at the time.
