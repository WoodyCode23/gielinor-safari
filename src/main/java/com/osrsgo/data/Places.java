package com.osrsgo.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/** Fixed world locations: bank Heal Centers and Gertrude's breeding den. */
public final class Places
{
    public static final int HEAL_RANGE = 12;
    public static final int BREED_RANGE = 10;

    /** Gertrude's house, west of Varrock. The lady loves her animals. */
    public static final WorldPoint GERTRUDES_DEN = new WorldPoint(3151, 3412, 0);

    private static final List<WorldPoint> BANKS = Collections.unmodifiableList(Arrays.asList(
        new WorldPoint(3208, 3220, 0),  // Lumbridge castle
        new WorldPoint(3164, 3487, 0),  // Grand Exchange
        new WorldPoint(3185, 3436, 0),  // Varrock west
        new WorldPoint(3253, 3420, 0),  // Varrock east
        new WorldPoint(3094, 3491, 0),  // Edgeville
        new WorldPoint(2946, 3368, 0),  // Falador west
        new WorldPoint(3013, 3355, 0),  // Falador east
        new WorldPoint(3092, 3243, 0),  // Draynor
        new WorldPoint(3269, 3167, 0),  // Al Kharid
        new WorldPoint(2808, 3441, 0),  // Catherby
        new WorldPoint(2725, 3491, 0),  // Seers' Village
        new WorldPoint(2615, 3332, 0),  // Ardougne north
        new WorldPoint(2655, 3283, 0),  // Ardougne south
        new WorldPoint(2612, 3092, 0),  // Yanille
        new WorldPoint(3688, 3467, 0),  // Canifis
        new WorldPoint(2933, 3282, 0)   // Crafting Guild
    ));

    public static final int STORE_RANGE = 12;

    private static final List<WorldPoint> GENERAL_STORES = Collections.unmodifiableList(Arrays.asList(
        new WorldPoint(3211, 3246, 0),  // Lumbridge
        new WorldPoint(3217, 3414, 0),  // Varrock
        new WorldPoint(2955, 3388, 0),  // Falador
        new WorldPoint(3078, 3509, 0),  // Edgeville
        new WorldPoint(3315, 3180, 0),  // Al Kharid
        new WorldPoint(2946, 3216, 0),  // Rimmington
        new WorldPoint(2779, 3442, 0),  // Catherby
        new WorldPoint(1541, 3833, 0)   // Lovakengj (Zeah)
    ));

    public static boolean nearGeneralStore(WorldPoint player)
    {
        if (player == null)
        {
            return false;
        }
        for (WorldPoint store : GENERAL_STORES)
        {
            if (flatDistance(player, store) <= STORE_RANGE)
            {
                return true;
            }
        }
        return false;
    }

    /** Plane-agnostic Chebyshev distance (Lumbridge's bank is upstairs). */
    private static int flatDistance(WorldPoint a, WorldPoint b)
    {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    public static boolean nearBank(WorldPoint player)
    {
        if (player == null)
        {
            return false;
        }
        for (WorldPoint bank : BANKS)
        {
            if (flatDistance(player, bank) <= HEAL_RANGE)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean nearBreedingDen(WorldPoint player)
    {
        return player != null && flatDistance(player, GERTRUDES_DEN) <= BREED_RANGE;
    }

    private Places()
    {
    }
}
