package com.osrsgo.spawn;

import com.osrsgo.model.Rarity;
import com.osrsgo.model.Species;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import com.osrsgo.data.SpeciesData;

/**
 * Deterministic wild spawns with no server: every client seeds the same RNG
 * from (regionId, 15-minute epoch bucket), so all players see identical spawns.
 * Spawns rotate when the bucket rolls over.
 */
public class SpawnManager
{
    public static final long BUCKET_MILLIS = 10 * 60 * 1000L;
    private static final int CATCH_RANGE = 5;
    // Fixed for everyone: density is part of the shared world, not a preference.
    // 4 felt crowded once real 3D models were standing on every spawn tile.
    private static final int SPAWNS_PER_REGION = 2;

    /** A Rare Candy on the ground: deterministic like spawns, scooped by walking onto it. */
    public static class Candy
    {
        public final String key;
        public WorldPoint location;
        public final int[] candX;
        public final int[] candY;
        public boolean placementVerified;

        Candy(String key, WorldPoint location, int[] candX, int[] candY)
        {
            this.key = key;
            this.location = location;
            this.candX = candX;
            this.candY = candY;
        }
    }

    // Roughly one candy per ~60 region-waves: genuinely rare, shared by all
    private static final int CANDY_ONE_IN = 60;

    private final Map<String, WildSpawn> spawns = new LinkedHashMap<>();
    private final Map<String, Candy> candies = new LinkedHashMap<>();
    private final Set<String> takenCandies = new HashSet<>();
    private final Set<String> removedKeys = new HashSet<>();
    private final Map<Integer, Long> regionBuckets = new HashMap<>();
    private int[] currentRegions = new int[0];
    private final List<Species> speciesPool = SpeciesData.all();

    /**
     * Each region's rotation clock is phase-shifted by a hash of its id, so
     * fresh spawns ripple in continuously instead of the whole world flipping
     * at once. Pure function of region id: every client agrees.
     */
    private static long regionOffset(int regionId)
    {
        return Math.floorMod(regionId * 2654435761L, BUCKET_MILLIS);
    }

    private static long bucketFor(int regionId, long now)
    {
        return (now + regionOffset(regionId)) / BUCKET_MILLIS;
    }

    /**
     * Recomputes the spawn table when any region's wave or the loaded regions
     * change. Returns true if new spawns appeared (a wave rotated).
     */
    public boolean update(Client client)
    {
        if (client.getLocalPlayer() == null)
        {
            return false;
        }
        int[] regions = client.getMapRegions();
        if (regions == null)
        {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean regionsChanged = !Arrays.equals(regions, currentRegions);
        boolean anyRotated = false;
        for (int regionId : regions)
        {
            Long known = regionBuckets.get(regionId);
            if (known != null && known != bucketFor(regionId, now))
            {
                anyRotated = true;
                break;
            }
        }
        if (!regionsChanged && !anyRotated)
        {
            // Every tick: it's a few hundred array reads, and it caps how
            // long a mis-placed mon can be seen standing on water
            revalidatePlacements(client);
            return false;
        }
        currentRegions = regions.clone();
        rebuild(client, now);
        if (removedKeys.size() > 2000)
        {
            // Keys are bucket-unique so stale entries can never resurrect;
            // this only bounds memory over a long session
            removedKeys.clear();
        }
        return anyRotated;
    }

    // Candidate tiles rolled per spawn slot. The count is FIXED so every
    // client consumes the same random stream regardless of what its collision
    // data says; only the pick among candidates uses collision (static map
    // data, identical on every client that has the tile loaded). 20 keeps
    // cave regions (mostly wall) from despawning everything.
    private static final int TILE_CANDIDATES = 20;

    private void rebuild(Client client, long now)
    {
        // Scene crossings rebuild the table; spawns whose key survives keep
        // their object (and thus their settled position) so nothing teleports
        Map<String, WildSpawn> previous = new HashMap<>(spawns);
        Map<String, Candy> previousCandies = new HashMap<>(candies);
        spawns.clear();
        candies.clear();
        regionBuckets.clear();
        int perRegion = SPAWNS_PER_REGION;
        for (int regionId : currentRegions)
        {
            long bucket = bucketFor(regionId, now);
            regionBuckets.put(regionId, bucket);
            Random rng = new Random(seed(regionId, bucket));
            int baseX = (regionId >> 8) << 6;
            int baseY = (regionId & 0xFF) << 6;
            for (int i = 0; i < perRegion; i++)
            {
                int[] candX = new int[TILE_CANDIDATES];
                int[] candY = new int[TILE_CANDIDATES];
                for (int c = 0; c < TILE_CANDIDATES; c++)
                {
                    candX[c] = baseX + rng.nextInt(64);
                    candY[c] = baseY + rng.nextInt(64);
                }
                Species species = rollSpecies(rng, bucket, candX[0], candY[0]);
                Rarity r = species.getRarity();
                int level = r.getMinLevel() + rng.nextInt(r.getMaxLevel() - r.getMinLevel() + 1);
                // Same seeded stream, so every client agrees on which spawns shine
                boolean shiny = rng.nextInt(64) == 0;

                int x = candX[0];
                int y = candY[0];
                boolean verified = false;
                for (int c = 0; c < TILE_CANDIDATES; c++)
                {
                    if (Boolean.TRUE.equals(walkable(client, candX[c], candY[c])))
                    {
                        x = candX[c];
                        y = candY[c];
                        verified = true;
                        break;
                    }
                }

                String key = regionId + ":" + bucket + ":" + i;
                WildSpawn existing = previous.get(key);
                if (existing != null)
                {
                    spawns.put(key, existing);
                }
                else if (!removedKeys.contains(key))
                {
                    WildSpawn spawn = new WildSpawn(key, species.getId(), level,
                        new WorldPoint(x, y, 0), shiny, candX, candY);
                    spawn.placementVerified = verified;
                    spawns.put(key, spawn);
                }
            }

            // The rare candy roll rides the same seeded stream, so everyone
            // sees the same sweets on the same tiles
            if (rng.nextInt(CANDY_ONE_IN) == 0)
            {
                int[] cx = new int[TILE_CANDIDATES];
                int[] cy = new int[TILE_CANDIDATES];
                for (int c = 0; c < TILE_CANDIDATES; c++)
                {
                    cx[c] = baseX + rng.nextInt(64);
                    cy[c] = baseY + rng.nextInt(64);
                }
                String candyKey = "candy:" + regionId + ":" + bucket;
                Candy existing = previousCandies.get(candyKey);
                if (existing != null)
                {
                    candies.put(candyKey, existing);
                }
                else if (!takenCandies.contains(candyKey))
                {
                    int px = cx[0];
                    int py = cy[0];
                    boolean ok = false;
                    for (int c = 0; c < TILE_CANDIDATES; c++)
                    {
                        if (Boolean.TRUE.equals(walkable(client, cx[c], cy[c])))
                        {
                            px = cx[c];
                            py = cy[c];
                            ok = true;
                            break;
                        }
                    }
                    Candy candy = new Candy(candyKey, new WorldPoint(px, py, 0), cx, cy);
                    candy.placementVerified = ok;
                    candies.put(candyKey, candy);
                }
            }
        }
    }

    /** Candies on the player's plane within range, nearest first. */
    public List<Candy> nearbyCandies(WorldPoint player, int maxDistance)
    {
        List<Candy> result = new ArrayList<>();
        for (Candy c : candies.values())
        {
            if (player.getPlane() == c.location.getPlane() && player.distanceTo(c.location) <= maxDistance)
            {
                result.add(c);
            }
        }
        result.sort((a, b) -> Integer.compare(player.distanceTo(a.location), player.distanceTo(b.location)));
        return result;
    }

    /** Every live candy, for diagnostics. */
    public java.util.Collection<Candy> allCandies()
    {
        return candies.values();
    }

    /** Removes a picked-up candy for the rest of its rotation. */
    public void takeCandy(String key)
    {
        takenCandies.add(key);
        candies.remove(key);
        if (takenCandies.size() > 500)
        {
            takenCandies.clear();
        }
    }

    /**
     * Spawns placed before their tile's collision data was loaded (scene-edge
     * regions) may sit on water or walls. Once the data is available, hop them
     * to their first provably walkable candidate. Deterministic across clients
     * because the candidate list is part of the seeded roll.
     */
    private void revalidatePlacements(Client client)
    {
        java.util.Iterator<Candy> cit = candies.values().iterator();
        while (cit.hasNext())
        {
            Candy candy = cit.next();
            if (candy.placementVerified)
            {
                continue;
            }
            Boolean current = walkable(client, candy.location.getX(), candy.location.getY());
            if (current == null)
            {
                continue;
            }
            if (current)
            {
                candy.placementVerified = true;
                continue;
            }
            boolean moved = false;
            for (int c = 0; c < candy.candX.length; c++)
            {
                Boolean w = walkable(client, candy.candX[c], candy.candY[c]);
                if (Boolean.TRUE.equals(w))
                {
                    candy.location = new WorldPoint(candy.candX[c], candy.candY[c], 0);
                    candy.placementVerified = true;
                    moved = true;
                    break;
                }
            }
            if (!moved)
            {
                // Its tile is provably blocked (water, walls) and no candidate
                // rescues it: a sweet nobody can reach is just clutter
                cit.remove();
            }
        }

        java.util.Iterator<WildSpawn> it = spawns.values().iterator();
        while (it.hasNext())
        {
            WildSpawn spawn = it.next();
            if (spawn.placementVerified)
            {
                continue;
            }
            Boolean current = walkable(client, spawn.location.getX(), spawn.location.getY());
            if (current == null)
            {
                continue;
            }
            if (current)
            {
                spawn.placementVerified = true;
                continue;
            }
            boolean moved = false;
            for (int c = 0; c < spawn.candX.length; c++)
            {
                Boolean w = walkable(client, spawn.candX[c], spawn.candY[c]);
                if (Boolean.TRUE.equals(w))
                {
                    spawn.location = new WorldPoint(spawn.candX[c], spawn.candY[c], 0);
                    spawn.placementVerified = true;
                    moved = true;
                    break;
                }
            }
            if (!moved)
            {
                // The current tile is provably blocked (water, walls) and no
                // candidate rescues it. Sweep the whole region for a real
                // floor tile; the sweep skips unknown tiles safely, so an
                // off-scene candidate must not veto it (that veto left mons
                // standing on rivers).
                relocateOrDespawn(client, spawn, it);
            }
        }
    }

    /**
     * Deterministic last resort: scan the spawn's whole 64x64 region from a
     * seeded start point and take the first walkable tile. Collision is static
     * map data, so every client that reaches this point picks the same tile.
     * Despawns only when the entire region is provably blocked (open water).
     */
    private void relocateOrDespawn(Client client, WildSpawn spawn, java.util.Iterator<WildSpawn> it)
    {
        int regionId;
        try
        {
            regionId = Integer.parseInt(spawn.key.substring(0, spawn.key.indexOf(':')));
        }
        catch (Exception e)
        {
            return;
        }
        int baseX = (regionId >> 8) << 6;
        int baseY = (regionId & 0xFF) << 6;
        int start = ((spawn.candX[0] - baseX) & 63) * 64 + ((spawn.candY[0] - baseY) & 63);
        boolean anyUnknown = false;
        for (int step = 0; step < 4096; step++)
        {
            int idx = (start + step) & 4095;
            int x = baseX + (idx >> 6);
            int y = baseY + (idx & 63);
            Boolean w = walkable(client, x, y);
            if (w == null)
            {
                anyUnknown = true;
            }
            else if (w)
            {
                spawn.location = new WorldPoint(x, y, 0);
                spawn.placementVerified = true;
                return;
            }
        }
        if (!anyUnknown)
        {
            removedKeys.add(spawn.key);
            it.remove();
        }
    }

    /**
     * True/false when the tile's collision data is loaded in the current
     * scene, null when unknown (outside the scene or no collision map).
     */

    private static Boolean walkable(Client client, int worldX, int worldY)
    {
        net.runelite.api.CollisionData[] maps = client.getCollisionMaps();
        if (maps == null)
        {
            return null;
        }
        if (client.isInInstancedRegion())
        {
            // Spawn tiles are template coords; find where this instance placed
            // that chunk (if it did) and read collision there
            for (WorldPoint p : WorldPoint.toLocalInstance(client, new WorldPoint(worldX, worldY, 0)))
            {
                int sceneX = p.getX() - client.getBaseX();
                int sceneY = p.getY() - client.getBaseY();
                if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104
                    || p.getPlane() < 0 || p.getPlane() >= maps.length || maps[p.getPlane()] == null)
                {
                    continue;
                }
                int flags = maps[p.getPlane()].getFlags()[sceneX][sceneY];
                return Coords.walkableFlags(flags);
            }
            return null;
        }
        int sceneX = worldX - client.getBaseX();
        int sceneY = worldY - client.getBaseY();
        if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104)
        {
            return null;
        }
        if (maps[0] == null)
        {
            return null;
        }
        int flags = maps[0].getFlags()[sceneX][sceneY];
        return Coords.walkableFlags(flags);
    }

    private static long seed(int regionId, long bucket)
    {
        long h = 1469598103934665603L;
        h = (h ^ regionId) * 1099511628211L;
        h = (h ^ bucket) * 1099511628211L;
        return h;
    }

    private Species rollSpecies(Random rng, long bucket, int x, int y)
    {
        com.osrsgo.data.BiomeData.Biome biome = com.osrsgo.data.BiomeData.biomeAt(x, y);
        com.osrsgo.data.BiomeData.DangerZone zone = com.osrsgo.data.BiomeData.dangerZoneAt(x, y);
        // Both derive from the region's own bucket so clients that rebuild at
        // different wall times still roll identical spawns
        int spotlightId = spotlightSpeciesId(bucket);
        boolean night = com.osrsgo.data.BiomeData.isNight(bucket * BUCKET_MILLIS);
        int total = 0;
        for (Species s : speciesPool)
        {
            total += spawnWeight(s, biome, zone, spotlightId, night);
        }
        if (total <= 0)
        {
            return speciesPool.get(0);
        }
        int roll = rng.nextInt(total);
        for (Species s : speciesPool)
        {
            roll -= spawnWeight(s, biome, zone, spotlightId, night);
            if (roll < 0)
            {
                return s;
            }
        }
        return speciesPool.get(0);
    }

    public boolean isNightNow()
    {
        return com.osrsgo.data.BiomeData.isNight(System.currentTimeMillis());
    }

    private static final int SPOTLIGHT_MULTIPLIER = 5;
    private static final int COMMUNITY_SPOTLIGHT_MULTIPLIER = 12;

    /**
     * Community Hour: Saturdays 20:00-21:00 UTC, same for every player.
     * Spotlight surges, catches pay double XP, balls turn up twice as fast.
     */
    public static boolean isCommunityHour()
    {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        return now.getDayOfWeek() == java.time.DayOfWeek.SATURDAY && now.getHour() == 20;
    }

    /**
     * Every rotation spotlights one non-boss species worldwide with surged
     * spawns. Seeded from the bucket alone, so every client agrees.
     */
    public int spotlightSpeciesId(long bucket)
    {
        List<Species> eligible = new ArrayList<>();
        for (Species s : speciesPool)
        {
            if (s.getRarity() != Rarity.EPIC && s.getRarity() != Rarity.LEGENDARY)
            {
                eligible.add(s);
            }
        }
        Random rng = new Random(seed(999331, bucket));
        return eligible.get(rng.nextInt(eligible.size())).getId();
    }

    /** The current rotation's spotlight species id. */
    public int currentSpotlightId()
    {
        return spotlightSpeciesId(System.currentTimeMillis() / BUCKET_MILLIS);
    }

    private static int spawnWeight(Species s, com.osrsgo.data.BiomeData.Biome biome,
        com.osrsgo.data.BiomeData.DangerZone zone, int spotlightId, boolean night)
    {
        int weight = s.getRarity().getSpawnWeight()
            * com.osrsgo.data.BiomeData.weightMultiplier(s.getId(), biome)
            * com.osrsgo.data.BiomeData.nightMultiplier(s.getId(), night);
        if (zone != null)
        {
            weight *= zone.rarityMultiplier(s.getRarity());
        }
        if (s.getId() == spotlightId)
        {
            weight *= isCommunityHour() ? COMMUNITY_SPOTLIGHT_MULTIPLIER : SPOTLIGHT_MULTIPLIER;
        }
        return weight;
    }

    /** Spawns on the player's plane sorted by distance, nearest first. */
    public List<WildSpawn> nearby(WorldPoint player, int maxDistance)
    {
        List<WildSpawn> result = new ArrayList<>();
        for (WildSpawn s : spawns.values())
        {
            if (player.getPlane() == s.location.getPlane() && player.distanceTo(s.location) <= maxDistance)
            {
                result.add(s);
            }
        }
        result.sort((a, b) -> Integer.compare(player.distanceTo(a.location), player.distanceTo(b.location)));
        return result;
    }

    public boolean inCatchRange(WorldPoint player, WildSpawn spawn)
    {
        return inCatchRange(player, spawn, CATCH_RANGE);
    }

    public boolean inCatchRange(WorldPoint player, WildSpawn spawn, int range)
    {
        return player.getPlane() == spawn.location.getPlane()
            && player.distanceTo(spawn.location) <= range;
    }

    /** Removes a spawn for the rest of the current rotation (caught or fled). */
    public void remove(WildSpawn spawn)
    {
        removedKeys.add(spawn.key);
        spawns.remove(spawn.key);
    }

    public WildSpawn byKey(String key)
    {
        return spawns.get(key);
    }

    /** Millis until the soonest wave among the loaded regions rotates. */
    public long millisUntilRotation()
    {
        long now = System.currentTimeMillis();
        long soonest = BUCKET_MILLIS - (now % BUCKET_MILLIS);
        for (int regionId : currentRegions)
        {
            soonest = Math.min(soonest, BUCKET_MILLIS - ((now + regionOffset(regionId)) % BUCKET_MILLIS));
        }
        return soonest;
    }

    public void reset()
    {
        spawns.clear();
        candies.clear();
        takenCandies.clear();
        removedKeys.clear();
        regionBuckets.clear();
        currentRegions = new int[0];
    }
}
