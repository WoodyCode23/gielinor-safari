# Local Gym Control (No Network)

Status: approved design, not yet implemented
Date: 2026-08-13

## Goal

Remove every call to external infrastructure from the plugin so the
Plugin Hub submission has zero third-party network surface, while
keeping gym territory control as a real feature rather than deleting
it.

Gym ownership becomes local state. Territory is contested two ways:
by deterministic rival NPC trainers when playing alone, and by other
players when in a RuneLite party. The player-facing server returns
later (see Forward Path) without a redesign.

## Non-goals

- Global or cross-player gym state outside a party. That is Phase 2.
- Replacing PvP battles, trading, or co-op raids. Those already run
  over RuneLite's own Party service and are untouched.
- Changing the deterministic spawn engine, breeding, raids, or the
  GielDex.

Party features are IN scope. RuneLite's Party service is RuneLite's
own infrastructure, not ours, so using it costs nothing against the
zero-external-calls goal.

## Core design: a rival is a GymHolder

`GymHolder` already carries `holderRsn`, `holderTeam`, `heldSince`,
`defenseWins`, and `holderFaction`. A rival trainer is simply a holder
whose name is not a player.

Consequence: the gym rows, the "Take over" button, the two-phase
"beat their defenders, then beat the leader" battle, the break-hold
path, and the claim path all keep working unchanged. This is a swap of
where holder state comes from, not a redesign of the feature.

## Rival generation

New `data/RivalData.java`, built on the same pattern as `RaidData`:
seeded from the spawn rotation bucket so results are deterministic and
reproducible. Determinism also keeps the party phase cheap, because
every client derives the same rivals from the same bucket.

Each rival has a themed name, a faction, and a team.

Rivals are always assigned a faction opposing the player's own, so the
existing "do not attack a gym held by an ally" rule never blocks a
player from reclaiming their own gyms.

## Pressure: how often rivals take a gym

Rivals replace the human rival as the brake on tribute when playing
alone. Pressure scales with how much the player currently holds, so a
small empire is comfortable and a full sweep is a genuine fight to
sustain.

Starting formula, to be tuned in playtest:

    seizureChancePerRotation = min(0.5, 0.05 * gymsHeldByPlayer)

- 1 gym held: 5 percent per 10-minute rotation
- 5 gyms held: 25 percent
- 10 gyms held: 50 percent

This makes the monarch bonus (holding all ten) an achievement instead
of a default state, which is what keeps tribute honest without
nerfing the tribute rates themselves.

## Strength: how hard a rival is to beat

Rival team strength uses the gym's existing difficulty ladder as a
floor, scaled by trainer level, and the scaling stops at trainer
level 90.

    effectiveLevel = min(trainerLevel, 90)
    rivalMonLevel  = gymSlotLevel + round(effectiveLevel * 0.5)

where `gymSlotLevel` is the level already defined for that slot in
`GymData.Gym.team`, so Lumbridge rivals stay low and Champions' Guild
rivals stay brutal. Capped at 99.

The cap is deliberate: past level 90 the player's team outpaces rivals
and should simply destroy them. That is the intended power-fantasy
payoff for reaching endgame.

## Endgame: dominance past 90

Strength alone is not enough, because trivial fights at the same
seizure rate would read as repetitive clicking rather than power. So
pressure eases as well:

    if trainerLevel >= 90: seizureChance *= 0.2

At endgame a maxed trainer genuinely holds their empire and tribute
flows freely. Tribute effectively uncaps at 90+, which is earned.

## Party grouping: players as the threat

The organising idea: rivals exist to simulate contested territory when
the player is alone. In a party, real players do that job, so rivals
step back and the payout rises. Only one pressure system is meaningful
at a time.

Party members are competitors, not allies. They can take each other's
gyms. The tribute multiplier is a RISK PREMIUM, not a teamwork bonus:
a bigger party pays more precisely because more people can strip your
empire.

This is also why party size cannot be farmed by stuffing a party with
idle accounts. Those accounts are threats, not multipliers.

Starting formula, to be tuned in playtest:

    others = active party members other than you
    tributeMultiplier   = 1 + 0.25 * others   (capped at 2.0)
    rivalPressureFactor = 1 / (1 + others)

"Active" means present in `PartyService.getMembers()`, which only
lists members currently connected. A member who logs off stops
counting toward the multiplier on their next poll, which is correct:
they are no longer a threat, so they should no longer be paying you a
risk premium.

So a party of five (four others) doubles tribute and cuts rival
pressure to a fifth, replacing NPC pressure with human pressure.

### Rules the party layer needs

1. **Party members are always attackable**, regardless of faction.
   The existing same-faction ally rule would otherwise grey out the
   button between two Zamorak friends and kill the mechanic outright.
   The ally rule continues to apply to NPC rivals.

2. **Holdings freeze when you leave the party or log off.** They stay
   yours, are no longer contestable, and earn no multiplier. You are
   exposed exactly as long as you are being paid for it. This also
   avoids the desync where a party takes gyms from a player whose
   client is not around to hear it.

3. **Claims broadcast.** A new party message carries a claim; incoming
   messages update the receiver's local map. No host, no election, no
   authority. The existing party messaging pattern is already used in
   sixteen places.

## State and persistence

Gym state moves into `PlayerProfile` as a map of gym id to
`GymHolder`.

Given the 2026-08-10 profile wipe, this field gets the same treatment
as `tierEssence`:

- Null-guarded on load in `ProfileStore`, so an older profile without
  the field loads cleanly.
- A save-load round-trip assertion added to the sim harness, so a
  change to the gym map can never silently produce an empty profile.
- An explicit merge rule for `::goimport` and `::gorestore`: keep
  whichever side holds more gyms. Gson defaults must not decide this.

## Removed

- `gym/GymApiService.java` (308 lines of HTTP)
- `gym/Leaderboard.java` and `fetchLeaderboard`. A single-player "top
  holders" board is meaningless, and the leaderboard is already out of
  the stats tab.
- The `SERVER_URL` constant and all okhttp usage in the gym path.
- `GymOwnership.OFFLINE`. With no server there is no offline state.
- The `gymsim` server integration harness.

Tribute base rates are NOT changed. Rivals and party members are the
brake.

## UI changes

- The gym control config stays but reads as claiming and holding gyms
  against rival trainers and party members.
- Gym rows render both rivals and party members through the existing
  "Held by X" path.
- The gym tab shows the current tribute multiplier and party size, so
  the risk premium is visible rather than implied.
- The claim-skip diagnostics added on 2026-08-13 stay, minus the
  server-unreachable branch.
- `::gogym` stays and reports local state, party size, and multiplier.

## Testing

Extend `gradlew sim` to assert:

1. Rival generation is deterministic for a given bucket.
2. Seizure pressure rises with held count and drops past level 90.
3. Rival strength stops scaling at trainer level 90.
4. Tribute multiplier scales with party size and caps at 2.0.
5. Rival pressure falls as party size rises.
6. A same-faction party member's gym is attackable; a same-faction
   NPC rival's gym is not.
7. Holdings freeze (uncontestable, unmultiplied) on leaving a party.
8. A profile carrying gym state survives a save-load round trip.

## Forward path

`GymHolder` keeps its shape, so the remaining phase is not a redesign:

- Phase 2 (post-traction): the server returns as the source of the
  same map, extending contested territory beyond a single party to
  everyone.

The Hetzner box remains useful throughout, since it still hosts the
jar download for sideloaded testing.

## Risks

- Tribute rates were tuned against a single human rival. Both the
  seizure formula and the party multiplier are starting points and
  will need playtest tuning.
- Rival flavor (names, teams) needs to feel like trainers rather than
  filler, or the territory layer reads as busywork.
- A party that agrees not to attack each other still farms the
  multiplier. This needs real coordination between real people and
  pays only in balls and XP toward their own progression, so it is
  accepted rather than engineered against.
