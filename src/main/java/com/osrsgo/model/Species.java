package com.osrsgo.model;

public class Species
{
    private final int id;
    private final String name;
    private final MonType type;
    private final Rarity rarity;
    private final int baseHp;
    private final int baseAtk;
    private final int baseDef;
    private final int baseSpd;

    public Species(int id, String name, MonType type, Rarity rarity, int baseHp, int baseAtk, int baseDef, int baseSpd)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.rarity = rarity;
        this.baseHp = baseHp;
        this.baseAtk = baseAtk;
        this.baseDef = baseDef;
        this.baseSpd = baseSpd;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public MonType getType()
    {
        return type;
    }

    public Rarity getRarity()
    {
        return rarity;
    }

    public int getBaseHp()
    {
        return baseHp;
    }

    public int getBaseAtk()
    {
        return baseAtk;
    }

    public int getBaseDef()
    {
        return baseDef;
    }

    public int getBaseSpd()
    {
        return baseSpd;
    }
}
