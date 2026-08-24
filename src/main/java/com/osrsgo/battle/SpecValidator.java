package com.osrsgo.battle;

import com.osrsgo.data.SpeciesData;
import com.osrsgo.model.Species;

/**
 * Rejects mon specs whose stats are impossible for their species and level
 * (outside the IV 0..15 formula bounds). Applied to anything that arrives
 * from another client over the party channel. Keeps a hacked or malicious
 * client from fielding god-mode mons against others.
 */
public final class SpecValidator
{
    public static boolean valid(MonSpec spec)
    {
        if (spec == null || !SpeciesData.exists(spec.speciesId) || spec.level < 1 || spec.level > 99)
        {
            return false;
        }
        Species s = SpeciesData.byId(spec.speciesId);
        return inRange(spec.maxHp, s.getBaseHp(), spec.level, spec.level + 10)
            && inRange(spec.atk, s.getBaseAtk(), spec.level, 5)
            && inRange(spec.def, s.getBaseDef(), spec.level, 5)
            && inRange(spec.spd, s.getBaseSpd(), spec.level, 5);
    }

    private static boolean inRange(int value, int base, int level, int extra)
    {
        int min = ((base * 2) * level) / 100 + extra;
        int max = ((base * 2 + 15) * level) / 100 + extra;
        return value >= min && value <= max;
    }

    private SpecValidator()
    {
    }
}
