package com.osrsgo.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * Fixed gym locations at F2P-reachable landmarks, roughly ascending in
 * difficulty. Team entries are {speciesId, level} pairs.
 */
public final class GymData
{
    public static class Gym
    {
        public final String id;
        public final String name;
        public final String leader;
        public final String badge;
        public final WorldPoint location;
        public final int[][] team;

        Gym(String id, String name, String leader, String badge, WorldPoint location, int[][] team)
        {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.badge = badge;
            this.location = location;
            this.team = team;
        }
    }

    private static final List<Gym> GYMS;

    static
    {
        List<Gym> g = new ArrayList<>();
        g.add(new Gym("lumbridge", "Lumbridge Gym", "Duke Horacio", "Lumbridge Badge",
            new WorldPoint(3222, 3218, 0),
            new int[][]{{3, 8}, {4, 10}, {2, 12}}));
        g.add(new Gym("draynor", "Draynor Gym", "The Wise Old Man", "Draynor Badge",
            new WorldPoint(3082, 3249, 0),
            new int[][]{{12, 14}, {11, 15}, {10, 18}}));
        g.add(new Gym("alkharid", "Al Kharid Gym", "Emir's Champion", "Desert Badge",
            new WorldPoint(3293, 3167, 0),
            new int[][]{{7, 16}, {34, 20}, {24, 24}}));
        g.add(new Gym("varrock", "Varrock Gym", "King Roald", "Varrock Badge",
            new WorldPoint(3213, 3424, 0),
            new int[][]{{23, 22}, {21, 25}, {42, 28}}));
        g.add(new Gym("falador", "Falador Gym", "Sir Amik Varze", "Falador Badge",
            new WorldPoint(2996, 3378, 0),
            new int[][]{{16, 24}, {43, 28}, {27, 32}}));
        g.add(new Gym("edgeville", "Edgeville Gym", "Mage of Zamorak", "Chaos Badge",
            new WorldPoint(3094, 3491, 0),
            new int[][]{{32, 30}, {40, 34}, {41, 38}}));
        g.add(new Gym("ardougne", "Ardougne Gym", "King Lathas", "Ardougne Badge",
            new WorldPoint(2662, 3305, 0),
            new int[][]{{22, 34}, {26, 38}, {51, 42}}));
        g.add(new Gym("catherby", "Catherby Gym", "Caleb Fitzharmon", "Tide Badge",
            new WorldPoint(2807, 3439, 0),
            new int[][]{{55, 38}, {57, 42}, {65, 48}}));
        g.add(new Gym("grandexchange", "Grand Exchange Gym", "Brugsen Bursen", "Exchange Badge",
            new WorldPoint(3164, 3487, 0),
            new int[][]{{68, 46}, {63, 50}, {61, 56}}));
        g.add(new Gym("champions", "Champions' Guild Gym", "The Champion of Champions", "Champion's Badge",
            new WorldPoint(3191, 3363, 0),
            new int[][]{{80, 58}, {84, 62}, {83, 68}}));
        GYMS = Collections.unmodifiableList(g);
    }

    public static List<Gym> all()
    {
        return GYMS;
    }

    public static Gym byId(String id)
    {
        for (Gym g : GYMS)
        {
            if (g.id.equals(id))
            {
                return g;
            }
        }
        return null;
    }

    private GymData()
    {
    }
}
