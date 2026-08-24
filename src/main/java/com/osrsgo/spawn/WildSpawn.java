package com.osrsgo.spawn;

import com.osrsgo.data.SpeciesData;
import com.osrsgo.model.Species;
import net.runelite.api.coords.WorldPoint;

public class WildSpawn
{
    public final String key;
    public final int speciesId;
    public final int level;
    public final boolean shiny;
    // Position can refine to a walkable candidate once collision data loads
    public WorldPoint location;
    public final int[] candX;
    public final int[] candY;
    public boolean placementVerified;
    public int failedAttempts;

    public WildSpawn(String key, int speciesId, int level, WorldPoint location, boolean shiny,
        int[] candX, int[] candY)
    {
        this.key = key;
        this.speciesId = speciesId;
        this.level = level;
        this.location = location;
        this.shiny = shiny;
        this.candX = candX;
        this.candY = candY;
    }

    public Species species()
    {
        return SpeciesData.byId(speciesId);
    }
}
