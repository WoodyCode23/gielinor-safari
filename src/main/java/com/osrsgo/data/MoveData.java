package com.osrsgo.data;

import com.osrsgo.model.MonType;
import com.osrsgo.model.Move;
import com.osrsgo.model.Rarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MoveData
{
    private static final Map<Integer, Move> MOVES = new HashMap<>();

    // Melee
    public static final int SLASH = 1;
    public static final int CRUSH = 2;
    public static final int WHIRLWIND = 3;
    public static final int BERSERKER_RUSH = 4;
    // Ranged
    public static final int QUICK_SHOT = 11;
    public static final int LONGSHOT = 12;
    public static final int RAPID_FIRE = 13;
    public static final int DEADEYE_VOLLEY = 14;
    // Magic
    public static final int WIND_STRIKE = 21;
    public static final int FIRE_BLAST = 22;
    public static final int SURGE = 23;
    public static final int ANCIENT_BARRAGE = 24;
    // Universal
    public static final int GUARD = 99;

    static
    {
        add(new Move(SLASH, "Slash", MonType.MELEE, 55, 0.95, false));
        add(new Move(CRUSH, "Crush", MonType.MELEE, 70, 0.85, false));
        add(new Move(WHIRLWIND, "Whirlwind", MonType.MELEE, 90, 0.75, false));
        add(new Move(BERSERKER_RUSH, "Berserker Rush", MonType.MELEE, 110, 0.70, false));

        add(new Move(QUICK_SHOT, "Quick Shot", MonType.RANGED, 55, 0.95, false));
        add(new Move(LONGSHOT, "Longshot", MonType.RANGED, 70, 0.85, false));
        add(new Move(RAPID_FIRE, "Rapid Fire", MonType.RANGED, 90, 0.75, false));
        add(new Move(DEADEYE_VOLLEY, "Deadeye Volley", MonType.RANGED, 110, 0.70, false));

        add(new Move(WIND_STRIKE, "Wind Strike", MonType.MAGIC, 55, 0.95, false));
        add(new Move(FIRE_BLAST, "Fire Blast", MonType.MAGIC, 70, 0.85, false));
        add(new Move(SURGE, "Surge", MonType.MAGIC, 90, 0.75, false));
        add(new Move(ANCIENT_BARRAGE, "Ancient Barrage", MonType.MAGIC, 110, 0.70, false));

        add(new Move(GUARD, "Guard", MonType.MELEE, 0, 1.0, true));
    }

    private static void add(Move m)
    {
        MOVES.put(m.getId(), m);
    }

    public static Move byId(int id)
    {
        Move m = MOVES.get(id);
        return m != null ? m : MOVES.get(GUARD);
    }

    /** Moveset for a species: basic + mid + (heavy for Rare+, signature for Legendary) + Guard. */
    public static List<Integer> movesetFor(MonType type, Rarity rarity)
    {
        int basic;
        int mid;
        int heavy;
        int signature;
        switch (type)
        {
            case RANGED:
                basic = QUICK_SHOT;
                mid = LONGSHOT;
                heavy = RAPID_FIRE;
                signature = DEADEYE_VOLLEY;
                break;
            case MAGIC:
                basic = WIND_STRIKE;
                mid = FIRE_BLAST;
                heavy = SURGE;
                signature = ANCIENT_BARRAGE;
                break;
            default:
                basic = SLASH;
                mid = CRUSH;
                heavy = WHIRLWIND;
                signature = BERSERKER_RUSH;
                break;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(basic);
        ids.add(mid);
        if (rarity == Rarity.LEGENDARY)
        {
            ids.add(signature);
        }
        else if (rarity == Rarity.RARE || rarity == Rarity.EPIC)
        {
            ids.add(heavy);
        }
        ids.add(GUARD);
        return ids;
    }

    private MoveData()
    {
    }
}
