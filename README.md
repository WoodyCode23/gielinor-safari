# Gielinor Safari

Catch, battle, and breed wild mons across Old School RuneScape, as a RuneLite plugin.

Questions, bugs, or ideas? Join the community Discord: https://discord.gg/nphJz77pJk

- **Wild spawns**: OSRS NPCs spawn on world tiles in staggered 10-minute waves. Spawns are deterministic (seeded by region + time bucket), so every player with the plugin sees the same spawns.
- **Catch them**: walk within 5 tiles, hit Catch in the side panel. Rarer mons are harder to catch. Caught mons roll IVs.
- **Types**: the combat triangle. Melee beats Ranged beats Magic beats Melee.
- **Gyms**: 10 landmark gyms (Lumbridge through the Champions' Guild) marked with cyan diamonds in the scene, minimap, and world map. Beat the leader's themed team for a badge.
- **PvP**: battle other trainers over RuneLite Party. Challenger hosts; turns resolve host-side and sync via party messages. No custom networking.

## Build and deploy

```powershell
.\gradlew.bat jar -q
Copy-Item build\libs\osrs-go-plugin-1.0.0.jar "$env:USERPROFILE\.runelite\externalPlugins\" -Force
Copy-Item build\libs\osrs-go-plugin-1.0.0.jar "$env:USERPROFILE\.runelite\sideloaded-plugins\" -Force
```

Both folders matter: Kitsch's sideloader (the normal logged-in client) reads `externalPlugins`; the gradle `runClient` dev client reads `sideloaded-plugins`.

Launch through Kitsch's sideloader (normal logged-in play) or `.\gradlew.bat runClient` (dev client, login screen only).

## Verify logic offline

`.\gradlew.bat sim` plays a full AI-vs-AI gym battle and checks leveling.

Design docs: `docs/design/original-design.md` and `docs/design/local-gym-control.md`.

## Data and privacy

- **No third-party servers.** Your collection, stats, progress and gym holdings all live in your local RuneLite config and are never sent anywhere. Gyms are contested by rival NPC trainers when you play alone.
- **Party play** (battles, trades, and contesting each other's gyms) runs over RuneLite's own party system, so claims are exchanged only with the party you joined. Incoming claims are validated and bound to the sender before they are applied.
- **NPC portraits** are fetched once from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki) and cached locally. Wiki images are used under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/); RuneScape assets are the intellectual property of Jagex Ltd.
- License: BSD 2-Clause (see LICENSE).
