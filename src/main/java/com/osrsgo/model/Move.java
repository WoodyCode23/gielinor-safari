package com.osrsgo.model;

public class Move
{
    private final int id;
    private final String name;
    private final MonType type;
    private final int power;
    private final double accuracy;
    private final boolean guard;

    public Move(int id, String name, MonType type, int power, double accuracy, boolean guard)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
        this.guard = guard;
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

    public int getPower()
    {
        return power;
    }

    public double getAccuracy()
    {
        return accuracy;
    }

    public boolean isGuard()
    {
        return guard;
    }
}
