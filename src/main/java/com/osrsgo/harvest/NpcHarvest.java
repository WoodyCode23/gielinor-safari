package com.osrsgo.harvest;

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
import net.runelite.client.RuneLite;

/**
 * The field journal: every named, renderable NPC encountered gets recorded
 * (name, a verified id, combat level), building toward a complete GielDex.
 * Uncatalogued sightings feed the species list as new batches are added.
 */
@Slf4j
@Singleton
public class NpcHarvest
{
    public static class Seen
    {
        public String name;
        public int npcId;
        public int combat;
        public int count;
    }

    private final File file = new File(RuneLite.RUNELITE_DIR, "osrsgo-npcdex.json");
    private final Gson gson;
    private final Map<String, Seen> byName = new HashMap<>();
    private boolean dirty;

    @Inject
    public NpcHarvest(Gson gson)
    {
        this.gson = gson;
        load();
    }

    /** Records a sighting; keeps the highest-combat variant's id per name. */
    public synchronized void record(String displayName, int npcId, int combat)
    {
        String key = displayName.toLowerCase();
        Seen seen = byName.get(key);
        if (seen == null)
        {
            seen = new Seen();
            seen.name = displayName;
            seen.npcId = npcId;
            seen.combat = combat;
            byName.put(key, seen);
            dirty = true;
        }
        else if (combat > seen.combat)
        {
            seen.npcId = npcId;
            seen.combat = combat;
            dirty = true;
        }
        seen.count++;
    }

    public synchronized int size()
    {
        return byName.size();
    }

    public synchronized java.util.List<Seen> all()
    {
        return new java.util.ArrayList<>(byName.values());
    }

    public synchronized void saveIfDirty()
    {
        if (!dirty)
        {
            return;
        }
        try
        {
            Files.write(file.toPath(), gson.toJson(byName).getBytes(StandardCharsets.UTF_8));
            dirty = false;
        }
        catch (Exception e)
        {
            log.warn("could not save npc harvest", e);
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
            Map<String, Seen> loaded = gson.fromJson(
                new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8),
                new TypeToken<Map<String, Seen>>()
                {
                }.getType());
            if (loaded != null)
            {
                byName.putAll(loaded);
            }
        }
        catch (Exception e)
        {
            log.warn("could not load npc harvest", e);
        }
    }
}
