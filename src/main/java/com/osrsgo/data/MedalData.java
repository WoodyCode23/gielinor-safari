package com.osrsgo.data;

import com.osrsgo.storage.PlayerProfile;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * PGo-style medals with bronze/silver/gold tiers, computed live from the
 * lifetime stats. Earned tiers are announced once and remembered in the
 * profile.
 */
public final class MedalData
{
    public static class Medal
    {
        public final String id;
        public final String name;
        public final String desc;
        public final long bronze;
        public final long silver;
        public final long gold;
        private final ToLongFunction<PlayerProfile> value;

        Medal(String id, String name, String desc, long bronze, long silver, long gold,
            ToLongFunction<PlayerProfile> value)
        {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.bronze = bronze;
            this.silver = silver;
            this.gold = gold;
            this.value = value;
        }

        public long valueOf(PlayerProfile profile)
        {
            return value.applyAsLong(profile);
        }

        /** 0 none, 1 bronze, 2 silver, 3 gold. */
        public int tierOf(PlayerProfile profile)
        {
            long v = valueOf(profile);
            if (v >= gold)
            {
                return 3;
            }
            if (v >= silver)
            {
                return 2;
            }
            if (v >= bronze)
            {
                return 1;
            }
            return 0;
        }

        public long nextGoal(PlayerProfile profile)
        {
            switch (tierOf(profile))
            {
                case 0: return bronze;
                case 1: return silver;
                case 2: return gold;
                default: return gold;
            }
        }
    }

    public static final List<Medal> ALL = Arrays.asList(
        new Medal("collector", "Collector", "Catch wild mons", 10, 50, 200, p -> p.stats.catches),
        new Medal("shinyhunter", "Shiny Hunter", "Catch shiny mons", 1, 5, 15, p -> p.stats.shinyCatches),
        new Medal("walker", "Gielinor Walker", "Walk tiles across the world", 5_000, 25_000, 100_000,
            p -> p.tilesWalked),
        new Medal("conqueror", "Gym Conqueror", "Capture gyms", 1, 10, 50, p -> p.stats.gymsCaptured),
        new Medal("battler", "Gym Battler", "Win gym battles", 5, 25, 100, p -> p.stats.gymWins),
        new Medal("duelist", "Duelist", "Win PvP battles against other trainers", 1, 10, 50, p -> p.stats.pvpWins),
        new Medal("breeder", "Breeder", "Hatch eggs from Gertrude's den", 1, 10, 30, p -> p.stats.eggsHatched),
        new Medal("scientist", "Evolver", "Evolve mons with essence", 1, 10, 30, p -> p.stats.evolutions),
        new Medal("raider", "Raider", "Beat raid bosses", 1, 10, 30, p -> p.stats.raidWins),
        new Medal("trader", "Trader", "Complete trades with other trainers", 1, 10, 50, p -> p.stats.trades),
        new Medal("researcher", "Researcher", "Finish daily research tasks", 3, 30, 100,
            p -> p.stats.researchCompleted),
        new Medal("registrar", "Registrar", "Register species in the GielDex", 15, 60, SpeciesData.all().size(),
            p -> p.caughtSpecies.size()));

    public static String tierName(int tier)
    {
        switch (tier)
        {
            case 1: return "Bronze";
            case 2: return "Silver";
            case 3: return "Gold";
            default: return "";
        }
    }

    private MedalData()
    {
    }
}
