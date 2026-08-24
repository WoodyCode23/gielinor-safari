package com.osrsgo.model;

import java.awt.Color;

/**
 * The combat triangle as an elemental type system:
 * Melee beats Ranged, Ranged beats Magic, Magic beats Melee.
 */
public enum MonType
{
    MELEE("Melee", new Color(220, 80, 60)),
    RANGED("Ranged", new Color(80, 180, 80)),
    MAGIC("Magic", new Color(90, 130, 240));

    private final String display;
    private final Color color;

    MonType(String display, Color color)
    {
        this.display = display;
        this.color = color;
    }

    public String getDisplay()
    {
        return display;
    }

    public Color getColor()
    {
        return color;
    }

    public boolean beats(MonType other)
    {
        return (this == MELEE && other == RANGED)
            || (this == RANGED && other == MAGIC)
            || (this == MAGIC && other == MELEE);
    }

    /** Damage multiplier of an attack of this type against a defender of the given type. */
    public double multiplierAgainst(MonType defender)
    {
        if (beats(defender))
        {
            return 1.5;
        }
        if (defender.beats(this))
        {
            return 0.65;
        }
        return 1.0;
    }
}
