package com.osrsgo.model;

import java.awt.Color;

public enum Rarity
{
    // Weights are per-species within the pool. At ~30 loaded spawns this puts
    // a legendary in view roughly once per 30+ rotations instead of most
    // rotations (the wild-Graardor incident).
    COMMON("Common", 0.80, 550, 1, 15, new Color(200, 200, 200)),
    UNCOMMON("Uncommon", 0.60, 280, 10, 30, new Color(110, 220, 110)),
    RARE("Rare", 0.40, 110, 25, 50, new Color(90, 160, 255)),
    EPIC("Epic", 0.25, 25, 40, 70, new Color(200, 110, 255)),
    // Red, so legendaries never read as shiny (shiny keeps the gold)
    LEGENDARY("Legendary", 0.12, 1, 60, 90, new Color(235, 65, 60));

    private final String display;
    private final double baseCatchChance;
    private final int spawnWeight;
    private final int minLevel;
    private final int maxLevel;
    private final Color color;

    Rarity(String display, double baseCatchChance, int spawnWeight, int minLevel, int maxLevel, Color color)
    {
        this.display = display;
        this.baseCatchChance = baseCatchChance;
        this.spawnWeight = spawnWeight;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.color = color;
    }

    public String getDisplay()
    {
        return display;
    }

    public double getBaseCatchChance()
    {
        return baseCatchChance;
    }

    public int getSpawnWeight()
    {
        return spawnWeight;
    }

    public int getMinLevel()
    {
        return minLevel;
    }

    public int getMaxLevel()
    {
        return maxLevel;
    }

    public Color getColor()
    {
        return color;
    }
}
