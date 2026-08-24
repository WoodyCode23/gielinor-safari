package com.osrsgo.spawn;

import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Spawn logic runs in real-map (template) coordinates everywhere, including
 * inside instances (raids, POH). Instanced scenes shift the client into a
 * private coordinate space; these helpers translate both directions so the
 * deterministic shared world keeps working there. Everyone in the same
 * instance shares the same template chunks, so they still see the same mons.
 */
public final class Coords
{
    /** The player's location in template space (plain world location outside instances). */
    public static WorldPoint playerLocation(Client client)
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }
        LocalPoint local = client.getLocalPlayer().getLocalLocation();
        if (client.isInInstancedRegion() && local != null)
        {
            return WorldPoint.fromLocalInstance(client, local);
        }
        return client.getLocalPlayer().getWorldLocation();
    }

    /**
     * Scene-local point for a template-space world point, instance-aware.
     * Inside instances only a same-plane match renders (no cross-floor
     * ghosts); null when the tile isn't in the current scene.
     */
    public static LocalPoint toLocal(Client client, WorldPoint wp)
    {
        if (wp == null)
        {
            return null;
        }
        if (client.isInInstancedRegion())
        {
            for (WorldPoint p : WorldPoint.toLocalInstance(client, wp))
            {
                if (p.getPlane() != client.getPlane())
                {
                    continue;
                }
                LocalPoint lp = LocalPoint.fromWorld(client, p);
                if (lp != null)
                {
                    return lp;
                }
            }
            return null;
        }
        return LocalPoint.fromWorld(client, wp);
    }

    public static final int BLOCKED_MASK = net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FLOOR
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;

    /**
     * Walkability of a template-space tile on the player's current plane.
     * Null when the tile or its collision data isn't in the scene.
     */
    public static Boolean walkableInScene(Client client, WorldPoint wp)
    {
        LocalPoint lp = toLocal(client, wp);
        net.runelite.api.CollisionData[] maps = client.getCollisionMaps();
        int plane = client.getPlane();
        if (lp == null || maps == null || plane < 0 || plane >= maps.length || maps[plane] == null)
        {
            return null;
        }
        int flags = maps[plane].getFlags()[lp.getSceneX()][lp.getSceneY()];
        return (flags & BLOCKED_MASK) == 0;
    }

    private Coords()
    {
    }
}
