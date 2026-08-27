package com.osrsgo.anim;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Animations learned from watching real NPCs play. Idle and walk come
 * straight off any live NPC of the species; attack animations are voted in
 * from observed combat (3+ sightings of the same sequence) so a one-off
 * death or flinch can't win. Persisted locally so knowledge accumulates.
 */
@Slf4j
@Singleton
public class AnimationLibrary
{
    public static class Entry
    {
        public int idle = -1;
        public int walk = -1;
        public int attack = -1;
    }

    /** Persisted shape: species-keyed entries plus a universal name-keyed store. */
    public static class Store
    {
        public Map<Integer, Entry> species = new HashMap<>();
        public Map<String, Entry> names = new HashMap<>();
    }

    private final File file = com.osrsgo.storage.PluginFiles.file("anims.json");
    private final Gson gson;
    private final Map<Integer, Entry> bySpecies = new HashMap<>();
    private final Map<String, Entry> byName = new HashMap<>();
    private final Map<Integer, Map<Integer, Integer>> attackVotes = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> attackVotesByName = new HashMap<>();
    private boolean dirty;

    @Inject
    public AnimationLibrary(Gson gson)
    {
        this.gson = gson;
        load();
    }

    private Entry nameEntryForSpecies(int speciesId)
    {
        com.osrsgo.model.Species sp = com.osrsgo.data.SpeciesData.byId(speciesId);
        return sp != null ? byName.get(sp.getName().toLowerCase()) : null;
    }

    public synchronized Integer idleFor(int speciesId)
    {
        Entry e = bySpecies.get(speciesId);
        if (e != null && e.idle > -1)
        {
            return e.idle;
        }
        Entry n = nameEntryForSpecies(speciesId);
        return n != null && n.idle > -1 ? n.idle : null;
    }

    public synchronized Integer walkFor(int speciesId)
    {
        Entry e = bySpecies.get(speciesId);
        if (e != null && e.walk > -1)
        {
            return e.walk;
        }
        Entry n = nameEntryForSpecies(speciesId);
        return n != null && n.walk > -1 ? n.walk : null;
    }

    public synchronized Integer attackFor(int speciesId)
    {
        Entry e = bySpecies.get(speciesId);
        if (e != null && e.attack > -1)
        {
            return e.attack;
        }
        Entry n = nameEntryForSpecies(speciesId);
        return n != null && n.attack > -1 ? n.attack : null;
    }

    /** Universal learning: every named NPC teaches its poses, catalog or not. */
    public synchronized void learnPoseByName(String name, int idlePose, int walkAnim)
    {
        Entry e = byName.computeIfAbsent(name, k -> new Entry());
        if (idlePose > -1 && e.idle != idlePose)
        {
            e.idle = idlePose;
            dirty = true;
        }
        if (walkAnim > -1 && e.walk != walkAnim)
        {
            e.walk = walkAnim;
            dirty = true;
        }
    }

    public synchronized void voteAttackByName(String name, int animId)
    {
        if (animId <= -1)
        {
            return;
        }
        Map<Integer, Integer> votes = attackVotesByName.computeIfAbsent(name, k -> new HashMap<>());
        votes.merge(animId, 1, Integer::sum);
        int bestAnim = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> v : votes.entrySet())
        {
            if (v.getValue() > bestCount)
            {
                bestCount = v.getValue();
                bestAnim = v.getKey();
            }
        }
        if (bestCount >= 3)
        {
            Entry e = byName.computeIfAbsent(name, k -> new Entry());
            if (e.attack != bestAnim)
            {
                e.attack = bestAnim;
                dirty = true;
            }
        }
    }

    /** Poses read straight off a live NPC of this species. */
    public synchronized void learnPose(int speciesId, int idlePose, int walkAnim)
    {
        Entry e = bySpecies.computeIfAbsent(speciesId, k -> new Entry());
        if (idlePose > -1 && e.idle != idlePose)
        {
            e.idle = idlePose;
            dirty = true;
        }
        if (walkAnim > -1 && e.walk != walkAnim)
        {
            e.walk = walkAnim;
            dirty = true;
        }
    }

    /** A combat sequence animation seen on this species; majority wins at 3+ votes. */
    public synchronized void voteAttack(int speciesId, int animId)
    {
        if (animId <= -1)
        {
            return;
        }
        Map<Integer, Integer> votes = attackVotes.computeIfAbsent(speciesId, k -> new HashMap<>());
        votes.merge(animId, 1, Integer::sum);
        int bestAnim = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> v : votes.entrySet())
        {
            if (v.getValue() > bestCount)
            {
                bestCount = v.getValue();
                bestAnim = v.getKey();
            }
        }
        if (bestCount >= 3)
        {
            Entry e = bySpecies.computeIfAbsent(speciesId, k -> new Entry());
            if (e.attack != bestAnim)
            {
                e.attack = bestAnim;
                dirty = true;
            }
        }
    }

    public synchronized void saveIfDirty()
    {
        if (!dirty)
        {
            return;
        }
        try
        {
            Store store = new Store();
            store.species = bySpecies;
            store.names = byName;
            Files.write(file.toPath(), gson.toJson(store).getBytes(StandardCharsets.UTF_8));
            dirty = false;
        }
        catch (Exception e)
        {
            log.warn("could not save animation library", e);
        }
    }

    private void load()
    {
        if (!file.isFile())
        {
            return;
        }
        try
        {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (json.contains("\"names\""))
            {
                Store store = gson.fromJson(json, Store.class);
                if (store != null)
                {
                    if (store.species != null)
                    {
                        bySpecies.putAll(store.species);
                    }
                    if (store.names != null)
                    {
                        byName.putAll(store.names);
                    }
                }
            }
            else
            {
                // Pre-universal format: a bare species map
                Map<Integer, Entry> loaded = gson.fromJson(json,
                    new TypeToken<Map<Integer, Entry>>()
                    {
                    }.getType());
                if (loaded != null)
                {
                    bySpecies.putAll(loaded);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("could not load animation library", e);
        }
    }

    /** How much the library knows, for the curious. */
    public synchronized String summary()
    {
        return bySpecies.size() + " species + " + byName.size() + " npc names learned";
    }
}
