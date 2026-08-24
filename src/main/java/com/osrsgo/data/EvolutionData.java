package com.osrsgo.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Evolution chains. The BASE species' rarity-tier essence pays the cost;
 * catches and releases of any same-tier mon feed the pool, which keeps common
 * spawns worth catching forever. No chain ends in a Legendary.
 */
public final class EvolutionData
{
    public static class Evolution
    {
        public final int toSpeciesId;
        public final int cost;

        Evolution(int toSpeciesId, int cost)
        {
            this.toSpeciesId = toSpeciesId;
            this.cost = cost;
        }
    }

    private static final Map<Integer, Evolution> CHAINS = new HashMap<>();

    static
    {
        chain(3, 24, 25);    // Goblin -> Hobgoblin
        chain(10, 53, 50);   // Skeleton -> Ankou
        chain(12, 29, 50);   // Ghost -> Banshee
        chain(11, 47, 60);   // Zombie -> Bloodveld
        chain(47, 62, 100);  // Bloodveld -> Nechryael
        chain(5, 40, 75);    // Imp -> Lesser Demon
        chain(40, 41, 75);   // Lesser Demon -> Greater Demon
        chain(41, 61, 150);  // Greater Demon -> Abyssal Demon
        chain(7, 34, 40);    // Scorpion -> King Scorpion
        chain(23, 43, 60);   // Guard -> White Knight
        chain(22, 42, 60);   // Highwayman -> Black Knight
        chain(20, 21, 30);   // Wizard -> Dark Wizard
        chain(25, 26, 40);   // Hill Giant -> Moss Giant
        chain(26, 27, 40);   // Moss Giant -> Ice Giant
        chain(27, 56, 60);   // Ice Giant -> Fire Giant
        chain(2, 33, 40);    // Cow -> Unicorn (moo)
        chain(8, 68, 100);   // Wolf -> Hellhound
        chain(44, 45, 80);   // Green Dragon -> Blue Dragon
        chain(45, 46, 80);   // Blue Dragon -> Red Dragon
        chain(46, 60, 120);  // Red Dragon -> Black Dragon
        chain(60, 70, 150);  // Black Dragon -> Brutal Black Dragon
        chain(28, 52, 60);   // Cave Crawler -> Basilisk
        chain(49, 64, 100);  // Dust Devil -> Smoke Devil
        chain(57, 65, 100);  // Jelly -> Cave Kraken
        chain(173, 102, 25); // Frog -> Giant Frog
        chain(180, 204, 100); // Terrorbird -> Warped Terrorbird
        chain(170, 15, 25);  // Man -> Barbarian (gains confidence)
        chain(313, 4, 20);   // Rat -> Giant Rat
        chain(314, 6, 20);   // Spider -> Giant Spider
        chain(312, 2, 20);   // Cow calf -> Cow (grows up)
        chain(101, 304, 30); // Bat -> Giant bat
        chain(308, 9, 25);   // Black bear -> Grizzly Bear
        chain(304, 318, 80); // Giant bat -> Dire bat
        chain(130, 317, 100); // Feral Vampyre -> Vyrewatch Sentinel
        chain(373, 369, 20); // Goat -> Billy Goat
        chain(384, 100, 15); // Duckling -> Duck
        chain(378, 33, 25);  // Unicorn Foal -> Unicorn
        chain(438, 308, 20); // Bear Cub -> Black bear
        chain(416, 412, 60); // Lizardman -> Lizardman brute
        chain(412, 69, 120); // Lizardman brute -> Lizardman Shaman
        chain(409, 195, 80); // Elf Archer -> Elf Warrior
    }

    private static void chain(int from, int to, int cost)
    {
        CHAINS.put(from, new Evolution(to, cost));
    }

    /** The evolution available from this species, or null if it is terminal. */
    public static Evolution of(int speciesId)
    {
        return CHAINS.get(speciesId);
    }

    /** Chain roots: species that evolve but are nobody's evolution target. */
    public static java.util.List<Integer> roots()
    {
        java.util.Set<Integer> targets = new java.util.HashSet<>();
        for (Evolution evo : CHAINS.values())
        {
            targets.add(evo.toSpeciesId);
        }
        java.util.List<Integer> roots = new java.util.ArrayList<>();
        for (Integer from : CHAINS.keySet())
        {
            if (!targets.contains(from))
            {
                roots.add(from);
            }
        }
        java.util.Collections.sort(roots);
        return roots;
    }

    private EvolutionData()
    {
    }
}
