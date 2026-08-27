# Gielinor Safari

Catch, battle, and breed wild mons across Old School RuneScape, as a RuneLite plugin.

Questions, bugs, or ideas? Join the community Discord: https://discord.gg/nphJz77pJk

- **Wild spawns**: OSRS NPCs spawn on world tiles in staggered 10-minute waves. Spawns are deterministic (seeded by region + time bucket), so every player with the plugin sees the same spawns.
- **Catch them**: walk into range (5 tiles, growing to 8 as your trainer level rises) and hit Catch in the side panel. Rarer mons are harder to catch. Caught mons roll IVs.
- **440 species**, drawn from real OSRS NPCs and shown with their in-game models.
- **Types**: the combat triangle. Melee beats Ranged, Ranged beats Magic, Magic beats Melee.
- **Gyms**: 10 landmark gyms (Lumbridge through the Champions' Guild) marked with cyan diamonds in the scene, minimap, and world map. Beat the leader's themed team for a badge, then hold the gym against rival trainers.
- **Breeding**: pair two mons for an egg that hatches as you walk, inheriting and improving stats.
- **PvP**: battle other trainers over RuneLite Party. Challenger hosts; turns resolve host-side and sync via party messages. No custom networking.

## Data and privacy

- **No third-party servers.** Your collection, stats, progress and gym holdings all live in your local RuneLite config and are never sent anywhere. Gyms are contested by rival NPC trainers when you play alone.
- **Party play** (battles, trades, and contesting each other's gyms) runs over RuneLite's own party system. Multiplayer was originally designed around a server of my own, and moving it onto the party system is the better answer: there is no host holding your data, no account to create, and nothing of yours sitting on a machine you do not control. Claims are exchanged only with the party you joined, and each one is validated and bound to the sender before it is applied.
- **NPC portraits** are fetched once from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki) and cached locally. Wiki images are used under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/); RuneScape assets are the intellectual property of Jagex Ltd.
- License: BSD 2-Clause (see LICENSE).
