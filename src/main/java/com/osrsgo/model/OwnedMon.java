package com.osrsgo.model;

import com.osrsgo.data.SpeciesData;

/**
 * A caught mon in the player's collection. Persisted as JSON, so keep fields
 * simple and mutable (Gson-friendly).
 */
public class OwnedMon
{
    public int speciesId;
    public int level;
    public int xp;
    public int ivHp;
    public int ivAtk;
    public int ivDef;
    public int ivSpd;
    public long caughtAt;
    public String caughtNear;
    public boolean shiny;
    public boolean favorite;
    public String nickname;
    public String hatchedBy;

    /** True when this mon hatched from one of Gertrude's eggs. */
    public boolean isHatched()
    {
        return "Gertrude's egg".equals(caughtNear);
    }

    /** Nickname when set, species name otherwise. */
    public String displayName()
    {
        return nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : name();
    }
    // Persistent battle damage; -1 means full health (also the migration
    // default for profiles from before heal centers existed)
    public int hp = -1;

    public int currentHp()
    {
        return hp < 0 ? maxHp() : Math.min(hp, maxHp());
    }

    public boolean isFainted()
    {
        return currentHp() <= 0;
    }

    public void healFull()
    {
        hp = -1;
    }

    public Species species()
    {
        return SpeciesData.byId(speciesId);
    }

    public String name()
    {
        return species().getName();
    }

    public int xpForNextLevel()
    {
        return level * 25;
    }

    /** Grants xp, applying level-ups. Returns levels gained. */
    public int gainXp(int amount)
    {
        // Egg-hatched mons are prodigies: double xp from every source
        xp += isHatched() ? amount * 2 : amount;
        int gained = 0;
        while (level < 99 && xp >= xpForNextLevel())
        {
            xp -= xpForNextLevel();
            level++;
            gained++;
        }
        return gained;
    }

    public int maxHp()
    {
        Species s = species();
        return statOf(s.getBaseHp(), ivHp) + level + 10;
    }

    public int atk()
    {
        return statOf(species().getBaseAtk(), ivAtk) + 5;
    }

    public int def()
    {
        return statOf(species().getBaseDef(), ivDef) + 5;
    }

    public int spd()
    {
        return statOf(species().getBaseSpd(), ivSpd) + 5;
    }

    private int statOf(int base, int iv)
    {
        return ((base * 2 + iv) * level) / 100;
    }
}
