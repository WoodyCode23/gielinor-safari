package com.osrsgo.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Area-themed spawns. A biome boosts its themed species
 * (weight x6) and hosts exclusives that spawn nowhere else. Rectangles are
 * rough surface-level bounds in world coordinates; first match wins, so put
 * more specific areas before broader ones.
 */
public final class BiomeData
{
    public static class Biome
    {
        public final String name;
        public final String hint;
        private final int[][] rects;
        private final Set<Integer> boosted;
        private final Set<Integer> exclusive;

        Biome(String name, String hint, int[][] rects, int[] boosted, int[] exclusive)
        {
            this.name = name;
            this.hint = hint;
            this.rects = rects;
            this.boosted = toSet(boosted);
            this.exclusive = toSet(exclusive);
        }

        public boolean contains(int x, int y)
        {
            for (int[] r : rects)
            {
                if (x >= r[0] && x <= r[1] && y >= r[2] && y <= r[3])
                {
                    return true;
                }
            }
            return false;
        }

        public boolean boosts(int speciesId)
        {
            return boosted.contains(speciesId) || exclusive.contains(speciesId);
        }

        public boolean hosts(int speciesId)
        {
            return exclusive.contains(speciesId);
        }

        private static Set<Integer> toSet(int[] ids)
        {
            Set<Integer> set = new HashSet<>();
            for (int id : ids)
            {
                set.add(id);
            }
            return set;
        }
    }

    public static final int BOOST_MULTIPLIER = 6;

    private static final Set<Integer> ALL_EXCLUSIVES = new HashSet<>();
    private static final List<Biome> BIOMES;
    private static final Biome DEFAULT = new Biome(
        "Gielinor",
        "A bit of everything roams the open world.",
        new int[][]{},
        new int[]{},
        new int[]{});

    static
    {
        // Species ids from SpeciesData. Exclusives never spawn outside their biome.
        List<Biome> b = Arrays.asList(
            new Biome("The Wilderness",
                "Demons and dark knights prowl. The Chaos Elemental and King Black Dragon lair here.",
                new int[][]{{2944, 3392, 3525, 3968}},
                new int[]{10, 11, 21, 32, 40, 41, 42, 53, 68},
                new int[]{80, 96, 151, 152, 153, 154, 196, 198, 327, 328, 329, 330}),
            new Biome("Kharidian Desert",
                "Scorpions and dust devils thrive. The Kalphite Queen tunnels beneath the sands.",
                new int[][]{{3130, 3520, 2880, 3195}},
                new int[]{7, 34, 49, 10, 176, 177, 190},
                new int[]{81, 218, 351, 352, 353, 354, 355}),
            new Biome("Morytania",
                "The undead swarm. The Nightmare haunts these lands.",
                new int[][]{{3400, 3730, 3170, 3520}},
                new int[]{10, 11, 12, 29, 47, 48, 51, 62, 61, 110, 129, 130, 131, 316, 317, 318, 319, 320, 321},
                new int[]{95, 215, 219, 315, 322, 323, 324, 325, 326, 334, 345, 346, 347, 348, 349, 350}),
            new Biome("Karamja",
                "Tropical giants and fire. TzTok-Jad emerges from the volcano.",
                new int[][]{{2690, 3040, 2880, 3195}},
                new int[]{6, 26, 56, 57, 186},
                new int[]{84, 356}),
            new Biome("Fremennik Province",
                "Crabs and cave horrors of the north. Dagannoth kings and Vorkath rule here.",
                new int[][]{{2570, 2820, 3580, 3910}, {2300, 2570, 3750, 3910}},
                new int[]{13, 28, 50, 52, 15, 121, 192},
                new int[]{83, 90, 91, 92}),
            new Biome("Tirannwn and the West",
                "Elven lands. Dark beasts stalk the forests.",
                new int[][]{{2130, 2400, 3130, 3450}},
                new int[]{63, 33, 8, 195},
                new int[]{157, 158, 358}),
            new Biome("Great Kourend",
                "Lizardmen patrol the swamps. The Alchemical Hydra grows in the lab.",
                new int[][]{{1100, 1900, 3400, 3900}},
                new int[]{69, 65, 54, 188},
                new int[]{94, 155, 156, 212, 335, 344}),
            new Biome("God Wars Dungeon",
                "The generals' armies clash forever. Aviansie soar only here.",
                new int[][]{{2815, 2945, 5250, 5375}},
                new int[]{86, 87, 88, 89, 159},
                new int[]{193, 194}),
            new Biome("Asgarnia",
                "Knight country. White Knights and dwarves are common around Falador.",
                new int[][]{{2890, 3130, 3240, 3525}},
                new int[]{16, 23, 43, 27},
                new int[]{150}),
            new Biome("Misthalin",
                "Farmland and low-level wilds around Lumbridge and Varrock.",
                new int[][]{{3130, 3400, 3195, 3525}},
                new int[]{1, 2, 3, 4, 6, 23},
                new int[]{}));
        BIOMES = Collections.unmodifiableList(b);
        for (Biome biome : BIOMES)
        {
            ALL_EXCLUSIVES.addAll(biome.exclusive);
        }
    }

    /**
     * High-danger areas (raids, dungeons) where rare spawns surge. Checked
     * independently of surface biomes since these live on instance/underground
     * coordinate bands.
     */
    public static class DangerZone
    {
        public final String name;
        public final String hint;
        private final int[][] rects;
        private final int rareMult;
        private final int epicMult;
        private final int legendaryMult;

        DangerZone(String name, String hint, int[][] rects, int rareMult, int epicMult, int legendaryMult)
        {
            this.name = name;
            this.hint = hint;
            this.rects = rects;
            this.rareMult = rareMult;
            this.epicMult = epicMult;
            this.legendaryMult = legendaryMult;
        }

        public boolean contains(int x, int y)
        {
            for (int[] r : rects)
            {
                if (x >= r[0] && x <= r[1] && y >= r[2] && y <= r[3])
                {
                    return true;
                }
            }
            return false;
        }

        public int rarityMultiplier(com.osrsgo.model.Rarity rarity)
        {
            switch (rarity)
            {
                case RARE: return rareMult;
                case EPIC: return epicMult;
                case LEGENDARY: return legendaryMult;
                default: return 1;
            }
        }
    }

    private static final List<DangerZone> DANGER_ZONES = Collections.unmodifiableList(Arrays.asList(
        new DangerZone("Chambers of Xeric",
            "Raid-grade danger: rare and legendary spawns surge.",
            new int[][]{{3100, 3370, 5150, 5310}}, 3, 5, 8),
        new DangerZone("Theatre of Blood",
            "Raid-grade danger: rare and legendary spawns surge.",
            new int[][]{{3140, 3330, 4230, 4470}}, 3, 5, 8),
        new DangerZone("Tombs of Amascut",
            "Raid-grade danger: rare and legendary spawns surge.",
            new int[][]{{3500, 3700, 5130, 5370}}, 3, 5, 8),
        new DangerZone("The Depths",
            "Underground: rarer mons lurk in the dark.",
            new int[][]{{1000, 4090, 8500, 10560}}, 2, 2, 2)));

    // Undead and night creatures that stir after dark (UTC 20:00-06:00)
    private static final Set<Integer> NIGHT_SPECIES = new HashSet<>(Arrays.asList(
        10, 11, 12, 29, 47, 48, 53, 61, 62, 68, 95, 101, 110, 129, 130, 131, 190, 191));
    public static final int NIGHT_MULTIPLIER = 3;

    /** Night is derived from the rotation bucket's UTC hour, so all clients agree. */
    public static boolean isNight(long bucketStartMillis)
    {
        int hourUtc = (int) ((bucketStartMillis / 3_600_000L) % 24);
        return hourUtc >= 20 || hourUtc < 6;
    }

    public static int nightMultiplier(int speciesId, boolean night)
    {
        return night && NIGHT_SPECIES.contains(speciesId) ? NIGHT_MULTIPLIER : 1;
    }

    public static DangerZone dangerZoneAt(int x, int y)
    {
        for (DangerZone zone : DANGER_ZONES)
        {
            if (zone.contains(x, y))
            {
                return zone;
            }
        }
        return null;
    }

    /** Where this species lives, for the GielDex: exclusive home, boosted home, or roams. */
    public static String homeOf(int speciesId)
    {
        for (Biome biome : BIOMES)
        {
            if (biome.hosts(speciesId))
            {
                return "Only in " + biome.name;
            }
        }
        for (Biome biome : BIOMES)
        {
            if (biome.boosts(speciesId))
            {
                return "Common in " + biome.name;
            }
        }
        return "Roams all of Gielinor";
    }

    public static Biome biomeAt(int x, int y)
    {
        for (Biome biome : BIOMES)
        {
            if (biome.contains(x, y))
            {
                return biome;
            }
        }
        return DEFAULT;
    }

    /** Weight multiplier for a species spawning at a tile: 0 = cannot spawn here. */
    public static int weightMultiplier(int speciesId, Biome biome)
    {
        if (biome.hosts(speciesId) || biome.boosts(speciesId))
        {
            return BOOST_MULTIPLIER;
        }
        if (ALL_EXCLUSIVES.contains(speciesId))
        {
            // Exclusive to some other biome
            return 0;
        }
        return 1;
    }

    private BiomeData()
    {
    }
}
