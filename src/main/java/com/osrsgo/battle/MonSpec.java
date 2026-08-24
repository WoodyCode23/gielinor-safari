package com.osrsgo.battle;

import com.osrsgo.model.OwnedMon;

/**
 * Wire-format description of one battle-ready mon. Stats are sent explicitly
 * (not re-derived) so IVs and future stat tweaks survive the trip between
 * clients unchanged.
 */
public class MonSpec
{
    public int speciesId;
    public int level;
    public int maxHp;
    public int atk;
    public int def;
    public int spd;

    public static MonSpec fromOwned(OwnedMon m)
    {
        MonSpec s = new MonSpec();
        s.speciesId = m.speciesId;
        s.level = m.level;
        s.maxHp = m.maxHp();
        s.atk = m.atk();
        s.def = m.def();
        s.spd = m.spd();
        return s;
    }
}
