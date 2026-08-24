package com.osrsgo.model;

/**
 * A bred egg. The child's species, IVs, and shininess are rolled at breeding
 * time but hidden from the player until it hatches by walking.
 */
public class Egg
{
    public int speciesId;
    public int ivHp;
    public int ivAtk;
    public int ivDef;
    public int ivSpd;
    public boolean shiny;
    public int tilesRequired;
    public int tilesProgress;
    public long createdAt;
}
