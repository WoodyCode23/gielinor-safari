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

    /**
     * Flags that leave a tile standable. A wall on one edge, or something
     * blocking line of sight across it, still lets a creature occupy the tile.
     */
    private static final int STANDABLE_MASK =
        net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_NORTH
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_EAST
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_SOUTH
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST
        | net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_WEST
        | net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH
        | net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST
        | net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH
        | net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST
        | net.runelite.api.CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;

    /**
     * Whether a creature can stand on a tile carrying these collision flags.
     *
     * This is an ALLOWLIST on purpose. It used to be a blocklist of the flags
     * RuneLite happens to name, which meant any bit the game client sets that
     * the API has no constant for read as open floor. Dungeon rock carries
     * 0x40000000, which RuneLite does not name, so monsters were placed inside
     * walls. Treating every unrecognised bit as solid fixes that bit and any
     * other the client may set.
     */
    public static boolean walkableFlags(int flags)
    {
        return (flags & ~STANDABLE_MASK) == 0;
    }

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
        return walkableFlags(flags);
    }

    private Coords()
    {
    }
}
