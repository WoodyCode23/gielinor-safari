package com.osrsgo;

import com.osrsgo.battle.BattleEngine;
import com.osrsgo.battle.BattleMon;
import com.osrsgo.battle.BattleSession;
import com.osrsgo.data.GymData;
import com.osrsgo.model.OwnedMon;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline sanity check for the battle core: plays a full gym battle with AI
 * picking both sides' moves, and exercises mon leveling. Run: gradlew sim
 */
public class SimHarness
{
    public static void main(String[] args)
    {
        OwnedMon a = mon(42, 30);
        OwnedMon b = mon(45, 28);
        OwnedMon c = mon(20, 25);
        List<BattleMon> mine = new ArrayList<>();
        mine.add(BattleMon.fromOwned(a));
        mine.add(BattleMon.fromOwned(b));
        mine.add(BattleMon.fromOwned(c));

        GymData.Gym gym = GymData.byId("varrock");
        List<BattleMon> theirs = new ArrayList<>();
        for (int[] e : gym.team)
        {
            theirs.add(BattleMon.fromSpecies(e[0], e[1]));
        }

        BattleSession s = new BattleSession(BattleSession.Mode.AI, "sim", gym.leader, -1, gym.id, mine, theirs);
        int guard = 0;
        while (!s.finished && guard++ < 200)
        {
            int myMove = BattleEngine.pickAiMove(s.myMon(), s.oppMon(), new java.util.Random(guard));
            s.resolveTurn(myMove, s.aiMove());
        }
        for (String line : s.fullLog)
        {
            System.out.println(line);
        }
        System.out.println("--- finished=" + s.finished + " wonByMe=" + s.wonByMe + " turns=" + s.turn);
        if (!s.finished)
        {
            throw new IllegalStateException("Battle did not terminate in 200 turns");
        }

        com.osrsgo.data.BiomeData.Biome wildy = com.osrsgo.data.BiomeData.biomeAt(3100, 3700);
        com.osrsgo.data.BiomeData.Biome karamja = com.osrsgo.data.BiomeData.biomeAt(2800, 3000);
        com.osrsgo.data.BiomeData.Biome misthalin = com.osrsgo.data.BiomeData.biomeAt(3222, 3218);
        System.out.println("Biomes: (3100,3700)=" + wildy.name + " (2800,3000)=" + karamja.name
            + " (3222,3218)=" + misthalin.name);
        if (!"The Wilderness".equals(wildy.name) || !"Karamja".equals(karamja.name))
        {
            throw new IllegalStateException("biome lookup wrong");
        }
        // Jad (84) is Karamja-exclusive: never in Misthalin, boosted at the volcano
        if (com.osrsgo.data.BiomeData.weightMultiplier(84, misthalin) != 0
            || com.osrsgo.data.BiomeData.weightMultiplier(84, karamja) == 0)
        {
            throw new IllegalStateException("exclusive spawn weighting wrong");
        }
        // KBD (80) only in the Wilderness
        if (com.osrsgo.data.BiomeData.weightMultiplier(80, karamja) != 0
            || com.osrsgo.data.BiomeData.weightMultiplier(80, wildy) == 0)
        {
            throw new IllegalStateException("KBD exclusivity wrong");
        }

        com.osrsgo.data.BiomeData.DangerZone cox = com.osrsgo.data.BiomeData.dangerZoneAt(3230, 5200);
        com.osrsgo.data.BiomeData.DangerZone sewer = com.osrsgo.data.BiomeData.dangerZoneAt(3200, 9870);
        com.osrsgo.data.BiomeData.DangerZone lumby = com.osrsgo.data.BiomeData.dangerZoneAt(3222, 3218);
        System.out.println("Zones: cox=" + (cox != null ? cox.name : "none")
            + " sewer=" + (sewer != null ? sewer.name : "none")
            + " lumbridge=" + (lumby != null ? lumby.name : "none"));
        if (cox == null || !"Chambers of Xeric".equals(cox.name)
            || cox.rarityMultiplier(com.osrsgo.model.Rarity.LEGENDARY) != 8
            || sewer == null || sewer.rarityMultiplier(com.osrsgo.model.Rarity.RARE) != 2
            || lumby != null)
        {
            throw new IllegalStateException("danger zone lookup wrong");
        }

        com.osrsgo.battle.MonSpec legit = com.osrsgo.battle.MonSpec.fromOwned(a);
        com.osrsgo.battle.MonSpec god = com.osrsgo.battle.MonSpec.fromOwned(a);
        god.atk = 999;
        com.osrsgo.battle.MonSpec fakeSpecies = com.osrsgo.battle.MonSpec.fromOwned(a);
        fakeSpecies.speciesId = 555;
        if (!com.osrsgo.battle.SpecValidator.valid(legit)
            || com.osrsgo.battle.SpecValidator.valid(god)
            || com.osrsgo.battle.SpecValidator.valid(fakeSpecies))
        {
            throw new IllegalStateException("spec validation wrong");
        }
        System.out.println("SpecValidator: legit ok, god-mode and fake species rejected");

        int before = a.level;
        a.gainXp(800);
        System.out.println("Leveling: " + before + " -> " + a.level + " after 800 xp");
        if (a.level <= before)
        {
            throw new IllegalStateException("gainXp did not level up");
        }

        // Gym state must survive a Gson round trip, or a profile-format change
        // silently empties a player's empire (see the 2026-08-10 wipe)
        com.osrsgo.storage.PlayerProfile gp = new com.osrsgo.storage.PlayerProfile();
        com.osrsgo.gym.GymHolder gh = new com.osrsgo.gym.GymHolder();
        gh.gymId = "varrock";
        gh.holderRsn = "SimTrainer";
        gh.heldSince = java.time.Instant.now().toString();
        gh.updatedAt = gh.heldSince;
        gh.holderFaction = "ZAMORAK";
        gp.gyms.put("varrock", gh);
        com.google.gson.Gson simGson = new com.google.gson.Gson();
        com.osrsgo.storage.PlayerProfile gpBack =
            simGson.fromJson(simGson.toJson(gp), com.osrsgo.storage.PlayerProfile.class);
        if (gpBack.gyms == null || gpBack.gyms.size() != 1
            || !"SimTrainer".equals(gpBack.gyms.get("varrock").holderRsn))
        {
            throw new IllegalStateException("gym map did not survive a save-load round trip");
        }

        // A profile written before this field existed has no gyms key at all.
        // It must still load with a usable map: this is the guarantee the
        // 2026-08-10 wipe was caused by violating.
        com.osrsgo.storage.PlayerProfile legacy =
            simGson.fromJson("{\"trainerXp\":100}", com.osrsgo.storage.PlayerProfile.class);
        if (legacy.gyms == null)
        {
            throw new IllegalStateException("a profile with no gyms key must still load a usable map");
        }
        System.out.println("Gym persistence: round trip and legacy profile OK");

        // Rivals must be stable within a rotation, or a gym would change
        // hands every time the panel repaints
        com.osrsgo.data.RivalData.Rival r1 =
            com.osrsgo.data.RivalData.rivalFor(12345L, "varrock", 40, "ZAMORAK");
        com.osrsgo.data.RivalData.Rival r2 =
            com.osrsgo.data.RivalData.rivalFor(12345L, "varrock", 40, "ZAMORAK");
        if (!r1.name.equals(r2.name) || r1.team.size() != r2.team.size())
        {
            throw new IllegalStateException("rival generation is not deterministic");
        }
        if ("ZAMORAK".equals(r1.faction))
        {
            throw new IllegalStateException("rival must not share the player's faction");
        }
        // Strength scales with trainer level but stops at 90
        int at40 = com.osrsgo.data.RivalData.rivalFor(1L, "varrock", 40, "GUTHIX").team.get(0).level;
        int at90 = com.osrsgo.data.RivalData.rivalFor(1L, "varrock", 90, "GUTHIX").team.get(0).level;
        int at99 = com.osrsgo.data.RivalData.rivalFor(1L, "varrock", 99, "GUTHIX").team.get(0).level;
        if (at90 <= at40 || at99 != at90)
        {
            throw new IllegalStateException("rival strength must scale to 90 then stop");
        }
        System.out.println("Rivals: deterministic, opposing faction, strength capped at 90");

        // The clamp is load-bearing, not theoretical: Champions' Guild slots
        // are levels 58, 62 and 68, and a level-90 trainer adds 45 to each,
        // so every slot overflows 99 and must come back clamped
        List<com.osrsgo.battle.MonSpec> championsTeam =
            com.osrsgo.data.RivalData.rivalFor(7L, "champions", 90, "GUTHIX").team;
        if (championsTeam.isEmpty())
        {
            // An empty team would pass the loop below vacuously; a renamed
            // or removed gym id must fail loudly instead
            throw new IllegalStateException("champions gym produced no team; check the gym id");
        }
        for (com.osrsgo.battle.MonSpec spec : championsTeam)
        {
            if (spec.level != 99)
            {
                throw new IllegalStateException("rival level must clamp to 99, got " + spec.level);
            }
        }
        System.out.println("Rivals: levels clamp to 99 at the hardest gym");

        // Pressure is the brake on tribute: more held means more contested
        double p1 = com.osrsgo.data.RivalData.seizureChance(1, 40, 0);
        double p10 = com.osrsgo.data.RivalData.seizureChance(10, 40, 0);
        double p10at99 = com.osrsgo.data.RivalData.seizureChance(10, 99, 0);
        double p10party = com.osrsgo.data.RivalData.seizureChance(10, 40, 4);
        if (com.osrsgo.data.RivalData.seizureChance(0, 40, 0) != 0.0)
        {
            throw new IllegalStateException("holding nothing must never be seized");
        }
        if (p10 <= p1 || p10 > 0.5)
        {
            throw new IllegalStateException("pressure must rise with held count and cap at 0.5");
        }
        if (p10at99 >= p10)
        {
            throw new IllegalStateException("pressure must ease past level 90");
        }
        if (p10party >= p10)
        {
            throw new IllegalStateException("rivals must step back as party size grows");
        }
        // 20 gyms computes to 1.0 uncapped, so unlike 10 gyms this input
        // actually exercises the ceiling instead of coinciding with it
        if (com.osrsgo.data.RivalData.seizureChance(20, 40, 0) != 0.5)
        {
            throw new IllegalStateException("pressure must clamp to 0.5");
        }
        // A negative count is the only input that distinguishes the guard:
        // without it the arithmetic returns a negative chance
        if (com.osrsgo.data.RivalData.seizureChance(-3, 40, 0) != 0.0)
        {
            throw new IllegalStateException("negative holdings must yield no pressure");
        }
        System.out.println("Pressure: rises with holdings, clamps at 0.5, eases at 90+ and in a party");

        java.util.Map<String, com.osrsgo.gym.GymHolder> lg = new java.util.HashMap<>();
        com.osrsgo.gym.LocalGyms.claim(lg, "varrock", "Trainer", new java.util.ArrayList<>(), 301, "GUTHIX");
        if (com.osrsgo.gym.LocalGyms.heldBy(lg, "Trainer") != 1)
        {
            throw new IllegalStateException("claim did not register");
        }
        com.osrsgo.gym.LocalGyms.recordDefense(lg, "varrock");
        if (lg.get("varrock").defenseWins != 1)
        {
            throw new IllegalStateException("defense was not recorded");
        }
        com.osrsgo.gym.LocalGyms.breakHold(lg, "varrock");
        if (com.osrsgo.gym.LocalGyms.heldBy(lg, "Trainer") != 0)
        {
            throw new IllegalStateException("break did not release the gym");
        }
        // Same masking trap as expiry: a breakHold that cleared the holder in
        // place instead of removing it would still satisfy heldBy
        if (lg.containsKey("varrock"))
        {
            throw new IllegalStateException("breakHold must remove the entry, not just clear the holder");
        }
        // A hold older than the duration must lapse on the next sweep
        com.osrsgo.gym.LocalGyms.claim(lg, "falador", "Trainer", new java.util.ArrayList<>(), 301, "GUTHIX");
        lg.get("falador").heldSince = java.time.Instant.now()
            .minusMillis(com.osrsgo.gym.GymHolder.HOLD_DURATION_MS + 1000).toString();
        com.osrsgo.gym.LocalGyms.expireStale(lg);
        if (com.osrsgo.gym.LocalGyms.heldBy(lg, "Trainer") != 0)
        {
            throw new IllegalStateException("stale hold did not expire");
        }
        // heldBy alone cannot tell removal from masking, because isClaimed
        // already excludes an expired holder. Assert the entry is really gone.
        if (lg.containsKey("falador"))
        {
            throw new IllegalStateException("expireStale must remove the lapsed entry, not just mask it");
        }
        // A rsn-blind heldBy would count another player's hold as yours
        com.osrsgo.gym.LocalGyms.claim(lg, "varrock", "Trainer", new java.util.ArrayList<>(), 301, "GUTHIX");
        com.osrsgo.gym.LocalGyms.claim(lg, "draynor", "Rival", new java.util.ArrayList<>(), 301, "SARADOMIN");
        if (com.osrsgo.gym.LocalGyms.heldBy(lg, "Trainer") != 1)
        {
            throw new IllegalStateException("heldBy must count only the named holder");
        }
        if (com.osrsgo.gym.LocalGyms.heldBy(lg, "Rival") != 1)
        {
            throw new IllegalStateException("heldBy must count the other holder separately");
        }
        System.out.println("LocalGyms: claim, defense, break and expiry OK");
        // The multiplier is a risk premium: more people who can take your gyms
        if (com.osrsgo.data.RivalData.tributeMultiplierFor(0) != 1.0)
        {
            throw new IllegalStateException("solo tribute must be unmultiplied");
        }
        if (com.osrsgo.data.RivalData.tributeMultiplierFor(4) != 2.0)
        {
            throw new IllegalStateException("four others must double tribute");
        }
        if (com.osrsgo.data.RivalData.tributeMultiplierFor(20) != 2.0)
        {
            throw new IllegalStateException("multiplier must cap at 2.0");
        }
        if (com.osrsgo.data.RivalData.tributeMultiplierFor(-5) != 1.0)
        {
            throw new IllegalStateException("negative party count must clamp to 1.0");
        }
        System.out.println("Tribute multiplier: 1.0 solo, caps at 2.0, negative clamped");
        com.osrsgo.gym.GymHolder sameFaith = new com.osrsgo.gym.GymHolder();
        sameFaith.holderRsn = "Someone";
        sameFaith.holderFaction = "ZAMORAK";
        // A same-faction NPC rival is protected by the ally rule
        if (!com.osrsgo.gym.LocalGyms.isAlly("ZAMORAK", sameFaith, false))
        {
            throw new IllegalStateException("same-faction rival should be an ally");
        }
        // The same holder, but in your party, is always attackable
        if (com.osrsgo.gym.LocalGyms.isAlly("ZAMORAK", sameFaith, true))
        {
            throw new IllegalStateException("party members must always be attackable");
        }
        // Solo, nothing is contestable by players and tribute is unmultiplied
        if (com.osrsgo.data.RivalData.tributeMultiplierFor(0) != 1.0)
        {
            throw new IllegalStateException("holdings must be unmultiplied outside a party");
        }
        System.out.println("Ally rule: protects same-faction rivals, never party members");

        // Shape, not value: these hold for any sane coefficient, but catch a
        // mistyped one. Pinning the constants themselves would fail the suite
        // on the playtest tuning the spec explicitly anticipates.
        double m1 = com.osrsgo.data.RivalData.tributeMultiplierFor(1);
        double m2 = com.osrsgo.data.RivalData.tributeMultiplierFor(2);
        if (!(m1 > 1.0 && m2 > m1 && m2 < 2.0))
        {
            throw new IllegalStateException("tribute multiplier must rise through an interior point");
        }
        if (com.osrsgo.data.RivalData.seizureChance(2, 40, 0)
            != 2 * com.osrsgo.data.RivalData.seizureChance(1, 40, 0))
        {
            throw new IllegalStateException("seizure pressure must stay linear below the clamp");
        }
        System.out.println("Formula shape: interior, monotonic, linear below the clamp");
        // Collision flags are an ALLOWLIST: a tile is standable only when every
        // bit set on it is one we know is harmless. The old blocklist called
        // 0x40000000 walkable purely because RuneLite has no name for it, which
        // put monsters inside dungeon walls.
        if (!com.osrsgo.spawn.Coords.walkableFlags(0))
        {
            throw new IllegalStateException("plain floor must be walkable");
        }
        if (com.osrsgo.spawn.Coords.walkableFlags(0x40000000))
        {
            throw new IllegalStateException("0x40000000 is solid in game and must not be walkable");
        }
        if (com.osrsgo.spawn.Coords.walkableFlags(net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL)
            || com.osrsgo.spawn.Coords.walkableFlags(net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_OBJECT)
            || com.osrsgo.spawn.Coords.walkableFlags(net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FLOOR))
        {
            throw new IllegalStateException("known blocking flags must stay blocked");
        }
        // A wall on one edge, or a line-of-sight block, still leaves the tile
        // standable: those must NOT be treated as blocked or open floor beside
        // any wall would stop spawning
        if (!com.osrsgo.spawn.Coords.walkableFlags(net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_WEST)
            || !com.osrsgo.spawn.Coords.walkableFlags(net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL))
        {
            throw new IllegalStateException("edge walls and line-of-sight blocks are still standable");
        }
        // Any unknown bit at all is treated as solid, which is what makes this
        // robust to client flags nobody has named yet
        if (com.osrsgo.spawn.Coords.walkableFlags(1 << 29))
        {
            throw new IllegalStateException("unknown flag bits must default to blocked");
        }
        System.out.println("Collision: allowlist blocks unnamed bits, keeps edge walls standable");

        // A restore now REPLACES the profile, so any field that fails to
        // survive a save and load is not merely stale afterwards, it is gone.
        // Round trip a fully populated profile and check the parts a restore
        // is expected to bring back with it.
        com.osrsgo.storage.PlayerProfile full = new com.osrsgo.storage.PlayerProfile();
        full.mons.add(caught(42, 1000L, 1, 2, 3, 4));
        full.mons.add(caught(45, 2000L, 5, 6, 7, 8));
        full.teamIndices.add(1);
        full.badges.add("varrock");
        full.caughtSpecies.add(42);
        full.berries = 7;
        full.rareCandies = 3;
        full.trainerXp = 5000;
        full.stats.catches = 61;
        full.researchProgress[0] = 2;
        com.osrsgo.storage.PlayerProfile back =
            simGson.fromJson(simGson.toJson(full), com.osrsgo.storage.PlayerProfile.class);
        if (back.mons.size() != 2 || back.mons.get(1).ivSpd != 8
            || back.mons.get(0).caughtAt != 1000L)
        {
            throw new IllegalStateException("a restore must bring the collection back intact");
        }
        if (back.teamIndices.size() != 1 || back.badges.size() != 1
            || !back.caughtSpecies.contains(42))
        {
            throw new IllegalStateException("team, badges and dex must survive a restore");
        }
        if (back.berries != 7 || back.rareCandies != 3 || back.trainerXp != 5000
            || back.stats.catches != 61 || back.researchProgress[0] != 2)
        {
            throw new IllegalStateException("currencies, stats and research must survive a restore");
        }
        // A backup written by an older version has whole keys missing, which
        // Gson leaves null. Merging tolerated that; replacing does not, since
        // those nulls become the live profile, so the backup load path has to
        // normalise exactly as a normal load does.
        com.osrsgo.storage.PlayerProfile sparse =
            simGson.fromJson("{\"trainerXp\":100}", com.osrsgo.storage.PlayerProfile.class);
        sparse.mons = null;
        sparse.stats = null;
        sparse.eggs = null;
        sparse.savedTeams = null;
        sparse.researchProgress = null;
        sparse.activeTeamSlot = 47;
        com.osrsgo.storage.ProfileStore.normalize(sparse);
        if (sparse.mons == null || sparse.stats == null || sparse.eggs == null
            || sparse.gyms == null || sparse.badges == null || sparse.essence == null)
        {
            throw new IllegalStateException("a restored sparse profile must have no null collections");
        }
        if (sparse.savedTeams.size() != 3 || sparse.researchProgress.length != 3
            || sparse.activeTeamSlot != 0)
        {
            throw new IllegalStateException("a restored profile must have its sized fields rebuilt and clamped");
        }
        System.out.println("Restore: a replaced profile round trips, and a sparse backup normalises");

        System.out.println("SIM OK");
    }

    /** A mon with a full birth certificate, for the restore checks. */
    private static OwnedMon caught(int speciesId, long caughtAt, int hp, int atk, int def, int spd)
    {
        OwnedMon m = new OwnedMon();
        m.speciesId = speciesId;
        m.level = 5;
        m.caughtAt = caughtAt;
        m.ivHp = hp;
        m.ivAtk = atk;
        m.ivDef = def;
        m.ivSpd = spd;
        return m;
    }

    private static OwnedMon mon(int speciesId, int level)
    {
        OwnedMon m = new OwnedMon();
        m.speciesId = speciesId;
        m.level = level;
        m.ivHp = 8;
        m.ivAtk = 8;
        m.ivDef = 8;
        m.ivSpd = 8;
        return m;
    }
}
