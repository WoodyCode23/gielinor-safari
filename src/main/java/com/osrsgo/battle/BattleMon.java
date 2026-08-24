package com.osrsgo.battle;

import com.osrsgo.data.MoveData;
import com.osrsgo.data.SpeciesData;
import com.osrsgo.model.MonType;
import com.osrsgo.model.OwnedMon;
import com.osrsgo.model.Species;
import java.util.List;

/** Runtime battle state for one mon. */
public class BattleMon
{
    public final int speciesId;
    public final String name;
    public final MonType type;
    public final int level;
    public final int maxHp;
    public final int atk;
    public final int def;
    public final int spd;
    public final List<Integer> moveIds;
    public int hp;
    public boolean guarding;
    // Damage taken in the most recent turn (-1 none, 0 = miss/blocked), for hitsplats
    public int lastHitTaken = -1;
    // Repetition fatigue: consecutive uses of the same damaging move weaken it
    public int lastMoveId = -1;
    public int repeatCount;

    private BattleMon(int speciesId, int level, int maxHp, int atk, int def, int spd)
    {
        this(speciesId, level, maxHp, atk, def, spd, null);
    }

    private BattleMon(int speciesId, int level, int maxHp, int atk, int def, int spd, String nameOverride)
    {
        Species s = SpeciesData.byId(speciesId);
        this.speciesId = speciesId;
        this.name = nameOverride != null ? nameOverride : s.getName();
        this.type = s.getType();
        this.level = level;
        this.maxHp = maxHp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.moveIds = MoveData.movesetFor(s.getType(), s.getRarity());
        this.hp = maxHp;
    }

    public static BattleMon fromOwned(OwnedMon m)
    {
        BattleMon b = new BattleMon(m.speciesId, m.level, m.maxHp(), m.atk(), m.def(), m.spd(), m.displayName());
        b.hp = m.currentHp();
        return b;
    }

    public static BattleMon fromSpec(MonSpec s)
    {
        return new BattleMon(s.speciesId, s.level, s.maxHp, s.atk, s.def, s.spd);
    }

    /** Builds an NPC combatant (gym teams, wild mons) with average IVs. */
    public static BattleMon fromSpecies(int speciesId, int level)
    {
        Species s = SpeciesData.byId(speciesId);
        int maxHp = ((s.getBaseHp() * 2 + 8) * level) / 100 + level + 10;
        int atk = ((s.getBaseAtk() * 2 + 8) * level) / 100 + 5;
        int def = ((s.getBaseDef() * 2 + 8) * level) / 100 + 5;
        int spd = ((s.getBaseSpd() * 2 + 8) * level) / 100 + 5;
        return new BattleMon(speciesId, level, maxHp, atk, def, spd);
    }

    public boolean fainted()
    {
        return hp <= 0;
    }
}
