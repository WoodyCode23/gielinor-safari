package com.osrsgo.data;

import com.osrsgo.battle.MonSpec;
import com.osrsgo.model.OwnedMon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rival NPC trainers that contest gyms when the player is not in a party.
 * Seeded from the spawn rotation bucket exactly like RaidData, so a rival's
 * name is stable for the whole rotation and shared across clients. Its
 * faction and team are NOT shared: opposingFaction depends on the local
 * player's own faction, and team levels scale with the local trainer level,
 * so only the name is guaranteed to match between two clients looking at the
 * same gym.
 */
public final class RivalData
{
    /** Deliberately title-cased and unlike RSNs, so rivals never read as players. */
    private static final String[] NAMES = {
        "Wandering Zealot", "Varrock Duelist", "Al Kharid Nomad",
        "Fremennik Raider", "Ardougne Sellsword", "Karamja Trapper",
        "Wilderness Marauder", "Kandarin Ranger", "Asgarnian Squire",
        "Misthalin Adept", "Morytania Warden", "Gnome Tactician"
    };

    private static final String[] FACTIONS = {"SARADOMIN", "ZAMORAK", "GUTHIX"};

    /** Trainer levels past this stop making rivals harder. */
    public static final int STRENGTH_CAP_LEVEL = 90;

    public static class Rival
    {
        public final String name;
        public final String faction;
        public final List<MonSpec> team;

        Rival(String name, String faction, List<MonSpec> team)
        {
            this.name = name;
            this.faction = faction;
            this.team = team;
        }
    }

    /**
     * The rival occupying one gym this rotation. The gym's own slot levels set
     * the floor so the difficulty ladder survives, and the player's level
     * scales it up until the cap.
     */
    public static Rival rivalFor(long bucket, String gymId, int trainerLevel, String playerFaction)
    {
        Random rng = new Random(seed(bucket, gymId));
        String name = NAMES[rng.nextInt(NAMES.length)];
        String faction = opposingFaction(playerFaction, rng);
        GymData.Gym gym = GymData.byId(gymId);
        List<MonSpec> team = new ArrayList<>();
        if (gym != null)
        {
            int effective = Math.min(trainerLevel, STRENGTH_CAP_LEVEL);
            for (int[] slot : gym.team)
            {
                int level = Math.min(99, slot[1] + Math.round(effective * 0.5f));
                team.add(specFor(slot[0], level));
            }
        }
        return new Rival(name, faction, team);
    }

    /** Rivals never share the player's faction, so the ally rule never blocks a reclaim. */
    private static String opposingFaction(String playerFaction, Random rng)
    {
        List<String> others = new ArrayList<>();
        for (String f : FACTIONS)
        {
            if (!f.equals(playerFaction))
            {
                others.add(f);
            }
        }
        return others.get(rng.nextInt(others.size()));
    }

    /** Reuses OwnedMon's stat math so rival stats match player stats exactly. */
    private static MonSpec specFor(int speciesId, int level)
    {
        OwnedMon m = new OwnedMon();
        m.speciesId = speciesId;
        m.level = level;
        m.ivHp = 8;
        m.ivAtk = 8;
        m.ivDef = 8;
        m.ivSpd = 8;
        return MonSpec.fromOwned(m);
    }

    private static long seed(long bucket, String gymId)
    {
        return bucket * 0x9E3779B97F4A7C15L + gymId.hashCode() * 0x100000001B3L;
    }

    /**
     * Odds that a rival takes one of the player's gyms this rotation. Rivals
     * are the stand-in for a human rival, so pressure rises with how much is
     * held, eases once the player outclasses them at 90, and eases again in a
     * party because real players take over as the threat.
     */
    public static double seizureChance(int gymsHeldByPlayer, int trainerLevel, int partyOthers)
    {
        if (gymsHeldByPlayer <= 0)
        {
            return 0.0;
        }
        double chance = Math.min(0.5, 0.05 * gymsHeldByPlayer);
        if (trainerLevel >= STRENGTH_CAP_LEVEL)
        {
            chance *= 0.2;
        }
        return chance / (1 + Math.max(0, partyOthers));
    }

    /**
     * Tribute is multiplied by party size because every party member can take
     * your gyms. This is a risk premium, not a teamwork bonus, which is why
     * stuffing a party with idle accounts does not farm anything.
     */
    public static double tributeMultiplierFor(int partyOthers)
    {
        return Math.min(2.0, 1.0 + 0.25 * Math.max(0, partyOthers));
    }

    private RivalData()
    {
    }
}
