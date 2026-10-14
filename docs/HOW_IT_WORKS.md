# How TaCZ Tactical HUD works

**English** · [Русский](HOW_IT_WORKS.ru.md)

## The idea

In *The Division* the gun HUD is not a flat strip in a screen corner but a holographic panel next to the agent. It is rotated in space and lags behind the camera during sharp moves. TaCZ Tactical HUD brings this approach to Minecraft: the panel is drawn with the regular GUI tools, but in a real 3D perspective.

## What the panel shows

```
 ┃┊  [■][■][■][■][■][■][■][■][■][■]     ← armor
 ┃┊  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫     ← health
 ┃┊ ┌─────────┬────┬───────┬───────┐
 ┃┊ │   31    │ 28 │  ▮▮▮  │ ═╦══  │     ← magazine | other guns | fire mode | gun
 ┃┊ │  128    │ 17 ├───────┼───────┤
 ┃┊ │         │▐05▌│ ● 02  │ ✚ 04  │     ← reserve | spare magazines | grenades | medkits
 ┗┛ └─────────┴────┴───────┴───────┘
 ↑ heat/reload bar and ammo ticks
```

| Element | What it shows | Behavior |
|---|---|---|
| **Armor segments** (10 white blocks) | The player's armor points: 1 block = 2 points, an odd value gives a half block | Empty blocks are translucent |
| **Health bar** (orange) | Health / max health | On damage a white "ghost" bar stays for 0.5 s, then drains. Below 30% the bar pulses red |
| **Big number** | Rounds in the magazine (+1 if a round is chambered and the bolt is not open) | Red at ≤25% of the magazine; blinks at 0 or when overheated. Briefly "kicks" on every shot |
| **Number below it** | Reserve ammo in the inventory or the gun's virtual reserve | `∞` means infinite reserve or creative; a red `0` means the reserve is empty; blank means the gun feeds directly from the inventory |
| **Two numbers in the column** | Rounds in the magazines of two other TaCZ guns in the hotbar | `--` if there is no gun |
| **Orange cell** | Spare magazines = reserve ÷ capacity, rounded up | `--` for guns that feed from the inventory |
| **Fire mode** | One bullet — semi, three — burst, three with a bar — auto | — |
| **Gun silhouette** | The HUD icon from the TaCZ gun pack | Fills orange while reloading, turns red when overheated |
| **Grenades / medkits** | Count of items from the `tacz_tactical_hud:grenades` / `medkits` tags across the whole inventory | 0 is shown dimmed |
| **Ticks on the left** | The magazine as ticks (up to 30; with a larger capacity one tick stands for several rounds) | Red when ammo is low |
| **Thin bar on the left** | Heat (orange → red), or reload progress if the gun has no heat | A red marker blinks at the bottom when ammo is low or the gun is overheated |

## When the panel is visible

The panel is shown when all of the following are true:

1. the TaCZ gun HUD is enabled (`GunHUDEnable` in the TaCZ settings);
2. a TaCZ gun is in the main hand and its data is loaded from the pack;
3. the player is not in spectator mode;
4. the profile for the current camera view is enabled (`first_person.Enabled` / `third_person.Enabled`).

While our panel is visible, the classic TaCZ HUD is hidden. If the profile for the current view is disabled, the classic TaCZ HUD comes back. When the gun is put away, the panel fades out smoothly.

## Rendering: where the 3D comes from

The Minecraft interface is drawn with an orthographic projection: everything is flat, there is no depth. While the panel is being drawn, the `HudPerspective` class:

1. flushes the accumulated GUI buffers (`GuiGraphics#flush`);
2. saves the projection and sets a perspective one (`Matrix4f#setPerspective`) with a configurable field of view;
3. resets the GUI view matrix, which pushes everything to the far end of the orthographic range;
4. clears the depth buffer, otherwise overlays drawn earlier would cover the panel;
5. places the camera at a distance where 1 unit = 1 GUI pixel in the plane of the panel's center, so coordinates inside the panel stay regular pixel coordinates;
6. rotates the panel by `Yaw` (around the vertical axis) and `Tilt` (around the horizontal axis) about its center;
7. flips the Y axis so that "down" stays positive, as in the GUI. The GUI's orthographic projection flips Y as well, so triangle winding and back-face culling do not change.

After drawing, everything is restored in a `finally` block, so if something fails the rest of the interface does not break.

Inside the panel, elements are spread in depth by fractions of a unit (background → bars → text and icons). The near clipping plane sits at 10 units, which gives the depth buffer enough precision for the layers not to flicker.

Icons (bullets, grenade, medkit) are drawn with rectangles, not textures. A downscaled texture at an angle shimmers, while geometry stays sharp. The font is Rajdhani (TTF with 8× oversampling), so digits stay crisp at any scale.

## Size and layouts

`HudLayout` turns the config profile into numbers for the current screen:

- **Auto**: `scale = Scale × gui_height ÷ 360`. The panel takes the same share of the screen height at any Minecraft GUI scale.
- **x1…x4**: `scale = Scale × N ÷ current_gui_scale`. The panel looks as it would at GUI scale N.

The first- and third-person layouts are computed every frame. When the view changes, the animator blends them over ~0.25 s: position, scale, angles, FOV, sway and opacity.

## Animations (`HudAnimator`)

All animations depend on real frame time, not on FPS. Frames that are too long (pause, minimized window) are clamped to 0.1 s so nothing jumps.

| Effect | How it works |
|---|---|
| Appear | The background unfolds from left to right, armor segments light up one by one, then the numbers appear (~0.45 s). Switching guns partly replays the animation |
| Disappear | The panel fades out over ~0.2 s after the gun is put away |
| Sway | The per-frame change of view direction accumulates into an offset with a soft limit (`tanh`, up to ±14 px) and decay. A second smoothing layer on top keeps mouse jitter away from the panel so it does not shake |
| Damage | A panel shake that decays over ~0.3 s, plus the "ghost" health bar |
| Shot | A short "kick" of the big number |
| Aiming | The panel becomes more transparent (up to −30%) and a bit smaller (−8%) so it does not get in the way of the sight |
| Reload | TaCZ only reports the remaining time of the current phase. The addon remembers the starting value of each phase and computes progress from it |

Animations change only the picture and never affect game state.

## Where the data comes from

Every frame `HudSnapshotFactory` builds an immutable `HudSnapshot`, and the painter works only with it:

- gun data — through the TaCZ API (`IGun`, `TimelessAPI`, `IGunOperator#getSynReloadState`, heat data);
- magazine capacity with attachments and the reserve — through `AmmoCache`. It refreshes at most once per tick (50 ms) and mirrors the TaCZ HUD logic, including one important detail: for guns that feed from the inventory, the counted ammo is written back into the gun. TaCZ does this from its own HUD, so while our HUD replaces it, `AmmoCache` does it instead;
- health, armor, inventory — from the local player.

## Hiding other elements (`HudElementHider`)

The addon does not change any TaCZ or Minecraft code — no mixins. It subscribes to the Forge `RenderGuiOverlayEvent.Pre` event and, while the panel is visible, cancels rendering of:

- the TaCZ overlay `tacz:tac_gun_hud_overlay` (the classic HUD — always);
- vanilla `player_health` and `armor_level` if `HideHealthAndArmor` is on;
- `hotbar` and `item_name` if `HideHotbar` is on;
- `experience_bar` (the level number is hidden with it) if `HideExperience` is on;
- `food_level` if `HideFood` is on.

## Client and server

The mod is fully client-side. On a dedicated server it loads but does nothing. The server does not need it, and you can join servers that do not have it (`displayTest = IGNORE_ALL_VERSION`).
