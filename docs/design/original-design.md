# OSRS Go: Design

Pokemon Go inside Old School RuneScape, delivered as a sideloaded RuneLite plugin. Built autonomously per Ryan's remote instruction ("build it all"); design decisions below were made solo and can be revisited.

## Core loop

1. **Wild spawns.** Monsters ("mons", all real OSRS NPCs) spawn on world tiles. Spawns are *deterministic*: seeded by `(regionId, 15-minute time bucket)`, so every player running the plugin sees the same spawns in the same places at the same time, with **no server at all**. Spawns rotate every 15 minutes.
2. **Catching.** Walk within 5 tiles of a spawn, hit Catch in the side panel. Catch chance scales with rarity (Common 80% down to Legendary 12%) plus a trainer-level bonus. Failed catches can make the mon flee. Caught mons roll IVs (0-15 per stat) and join your collection.
3. **Collection ("Dex").** Side panel lists owned mons with level/stats/type. Pick a team of up to 3. Trainer XP from catches and wins; badges from gyms.
4. **Gyms.** 10 fixed landmark locations (Lumbridge, Varrock, Falador, ... Champions' Guild as the finale) marked with special overlay marks in the scene, on the minimap, and on the world map. Walk within 10 tiles to battle the gym leader's themed AI team. Winning earns a badge.
5. **PvP battles.** Over RuneLite Party only (no custom networking). Challenge any party member, they accept, turn-based battle.

## Type system

Combat triangle as Pokemon types: **Melee beats Ranged beats Magic beats Melee** (1.5x / 0.65x). Every species is one type.

## Battle engine

Turn-based, teams of up to 3, auto-switch on faint. Each mon has 3-4 moves (type-tiered move pool: basic 55 power / mid 70 / heavy 90 / legendary signature 110, plus Guard which halves incoming damage that turn). Stats derive from species base stats + level + IVs, damage formula is Pokemon-style with the triangle multiplier. Faster mon moves first.

- **AI battles (gyms):** run entirely locally; AI picks the highest-expected-damage move with 20% randomness.
- **PvP battles:** host-authoritative. Challenger is host; both sides pick moves each turn, the guest's choice travels over party messages, the host resolves the turn with the same engine and broadcasts the turn result. Five message types (`Challenge`, `Accept`, `Move`, `TurnResult`, `End`), all extending `PartyMemberMessage`, complex payloads carried as JSON strings to keep Gson serialization trivial.

## Data

~60 species across 5 rarities (Common barnyard stuff up to Legendary bosses like Vorkath, Jad, Zulrah, GWD generals). All hardcoded in `SpeciesData`; gyms in `GymData`; moves in `MoveData`. No external assets: the nav icon and map marks are drawn in code.

## Persistence

Single JSON profile (owned mons, team, trainer XP, badges) via RuneLite `ConfigManager` under group `osrsgo`. Global, not per-RS-account. Mons fully heal between battles; battle/spawn state is in-memory only.

## Components

| Unit | Responsibility |
|---|---|
| `OsrsGoPlugin` | Lifecycle, event wiring, catch/battle orchestration, party message routing |
| `spawn/SpawnManager` | Deterministic spawn table for loaded regions + current bucket |
| `battle/BattleEngine` + `BattleSession` | Turn resolution; session state for AI and PvP |
| `party/*Msg` | Party protocol messages |
| `overlay/GoOverlay` | Scene tile polys + labels for spawns, gym marks, minimap dots |
| `ui/OsrsGoPanel` | Tabs: Nearby / Dex / Gyms / Battle |
| `storage/ProfileStore` | Load/save profile JSON |
| `data/*` | Species, moves, gyms catalogs |

## Gym control (v1.1, added same day)

Pokemon Go-style gym ownership, backed by the standalone **osrs-go-server** repo (`C:\dev\osrs-go-server`, Fastify + SQLite). Deliberately separate from the clan platform per Ryan's instruction: no shared code, DB, or deploy.

- Server holds per-gym state: holder RSN, defender team (MonSpec JSON), held-since, defense count. Identity is honor-system RSN.
- Plugin polls `GET /gyms` every ~2 minutes plus on startup. Claimed gyms are battled against the holder's snapshotted team (AI-controlled) instead of the NPC leader; winning auto-claims with your battle team via `POST /gyms/:id/claim`. Losing reports a defense.
- Optimistic concurrency: claims carry `expectedUpdatedAt`; a 409 means the gym changed hands mid-battle (chat message, refetch). Absent field is normalized to null server-side because Gson clients can drop null fields.
- Overlay: gold = yours, red = enemy-held (label shows holder RSN), cyan = unclaimed; falls back to badge coloring when the server is unreachable or gym control is off. Panel shows holder + defense count; your own gyms can't be battled.
- Config: `gymControl` toggle + `serverUrl` (default `http://localhost:5150`). The plugin degrades cleanly to v1 NPC-leader gyms when offline.

## v1.2 additions (same day)

- **Catch animation**: outcomes are pre-rolled but applied only after the overlay animates a Gielinor Ball arcing onto the spawn tile, wobbling through three shakes, then bursting a capture ring ("Gotcha!") or popping open ("Broke free!" / "It fled!"). `CatchSequence` carries the pending outcome; the game tick finalizes it after ~3.1s.
- **Biome spawns**: `BiomeData` maps world-coordinate rectangles to themed areas. Themed species get 6x spawn weight; biome exclusives (KBD + Chaos Elemental in the Wilderness, Kalphite Queen in the desert, The Nightmare in Morytania, Jad on Karamja, Vorkath + Dagannoth kings in Fremennik lands, Hydra in Kourend) never spawn elsewhere. The Nearby tab names the player's current area with a flavor hint.
- **Stat cards**: Dex rows expand via an Info toggle into XP bar, per-stat IV breakdown with quality tags, type-colored moveset with power/accuracy, and catch location. All rows carry code-drawn species medallions (rarity disc, type ring, initials).

## Known v1 limitations (accepted)

- ~~Spawn tiles are random within a region: a few land on walls/water~~ Fixed: spawns pick the first collision-map-walkable tile from a fixed 10-candidate roll. Tiles outside the loaded scene can't be verified and fall back to the raw roll.
- Host-authoritative PvP trusts the challenger's client.
- No evolutions, no items/pokeballs, no spectating, no timeout handling mid-battle (Forfeit button covers disconnects).
