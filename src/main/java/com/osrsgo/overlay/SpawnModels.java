package com.osrsgo.overlay;

import com.osrsgo.data.NpcModelData;
import com.osrsgo.spawn.WildSpawn;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

/**
 * Renders actual NPC models on spawn tiles via RuneLiteObjects. Everything
 * runs on the client thread from the game tick. Models are merged from the
 * NPC composition's model ids and cached per species; spawns without a
 * loadable model silently keep just the tile overlay.
 */
@Slf4j
@Singleton
public class SpawnModels
{
    private final Client client;
    private final com.osrsgo.anim.AnimationLibrary animLibrary;
    private final Map<String, RuneLiteObject> live = new HashMap<>();
    private final Map<String, Integer> baseZ = new HashMap<>();
    private final Map<Integer, Model> modelCache = new HashMap<>();
    private final Set<Integer> failedSpecies = new HashSet<>();

    @Inject
    public SpawnModels(Client client, com.osrsgo.anim.AnimationLibrary animLibrary)
    {
        this.client = client;
        this.animLibrary = animLibrary;
    }

    /**
     * Syncs scene objects to the current spawn set. Client thread only.
     * Model merging is expensive, so at most ONE new model is built per sync;
     * a rotation dumping many fresh species spreads its builds over ticks
     * instead of spiking one.
     */
    public void sync(List<WildSpawn> spawns, boolean enabled)
    {
        boolean builtOne = false;
        Set<String> wanted = new HashSet<>();
        if (enabled)
        {
            for (WildSpawn spawn : spawns)
            {
                if (spawn.location.getPlane() != client.getPlane())
                {
                    continue;
                }
                LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,spawn.location);
                if (lp == null)
                {
                    continue;
                }
                wanted.add(spawn.key);
                RuneLiteObject existing = live.get(spawn.key);
                if (existing != null)
                {
                    existing.setLocation(lp, spawn.location.getPlane());
                    // A pose learned after this object was created applies
                    // retroactively: bobbing keys are the unposed ones
                    if (baseZ.containsKey(spawn.key))
                    {
                        Integer learnt = animLibrary.idleFor(spawn.speciesId);
                        if (learnt != null)
                        {
                            try
                            {
                                existing.setAnimation(client.loadAnimation(learnt));
                                existing.setShouldLoop(true);
                                existing.setZ(baseZ.remove(spawn.key));
                            }
                            catch (Exception e)
                            {
                                // A rogue learned anim must never wedge the tick
                                baseZ.remove(spawn.key);
                            }
                        }
                    }
                    continue;
                }
                int modelKey = modelKey(spawn.speciesId, spawn.shiny);
                Model model = modelCache.get(modelKey);
                if (model == null)
                {
                    if (builtOne || failedSpecies.contains(modelKey))
                    {
                        // Not yet built; leave for a later tick
                        wanted.remove(spawn.key);
                        continue;
                    }
                    model = modelFor(spawn.speciesId, spawn.shiny);
                    builtOne = true;
                }
                if (model == null)
                {
                    wanted.remove(spawn.key);
                    continue;
                }
                RuneLiteObject obj = client.createRuneLiteObject();
                obj.setModel(model);
                obj.setLocation(lp, spawn.location.getPlane());
                obj.setActive(true);
                live.put(spawn.key, obj);
                Integer idle = animLibrary.idleFor(spawn.speciesId);
                boolean posed = false;
                if (idle != null)
                {
                    try
                    {
                        // A real learned idle animation beats the synthetic bob
                        obj.setAnimation(client.loadAnimation(idle));
                        obj.setShouldLoop(true);
                        posed = true;
                    }
                    catch (Exception e)
                    {
                        // Fall back to the bob rather than wedge the tick
                    }
                }
                if (!posed)
                {
                    baseZ.put(spawn.key, obj.getZ());
                }
            }
        }
        live.entrySet().removeIf(entry ->
        {
            if (!wanted.contains(entry.getKey()))
            {
                entry.getValue().setActive(false);
                baseZ.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /** Client tick: a gentle breathing bob, phase-shifted per spawn. Client thread only. */
    public void onClientTick(boolean enabled)
    {
        if (!enabled || live.isEmpty())
        {
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, RuneLiteObject> entry : live.entrySet())
        {
            Integer base = baseZ.get(entry.getKey());
            if (base == null)
            {
                continue;
            }
            double phase = (entry.getKey().hashCode() & 0xFFFF) / 65535.0 * Math.PI * 2;
            int bob = (int) ((Math.sin(now / 700.0 + phase) + 1) * 3);
            entry.getValue().setZ(base - bob);
        }
    }

    private static int modelKey(int speciesId, boolean shiny)
    {
        return speciesId | (shiny ? 1 << 20 : 0);
    }

    private Model modelFor(int speciesId)
    {
        return modelFor(speciesId, false);
    }

    private Model modelFor(int speciesId, boolean shiny)
    {
        int key = modelKey(speciesId, shiny);
        Model cached = modelCache.get(key);
        if (cached != null)
        {
            return cached;
        }
        if (failedSpecies.contains(key))
        {
            return null;
        }
        try
        {
            int npcId = NpcModelData.npcIdFor(speciesId);
            if (npcId < 0)
            {
                failedSpecies.add(key);
                return null;
            }
            NPCComposition comp = client.getNpcDefinition(npcId);
            if (comp == null || comp.getModels() == null || comp.getModels().length == 0)
            {
                failedSpecies.add(key);
                return null;
            }
            int[] ids = comp.getModels();
            ModelData[] parts = new ModelData[ids.length];
            for (int i = 0; i < ids.length; i++)
            {
                ModelData loaded = client.loadModelData(ids[i]);
                if (loaded == null)
                {
                    failedSpecies.add(key);
                    return null;
                }
                // loadModelData returns a shared cached instance; recoloring it
                // in place would bleed palette swaps into every other species
                // built from the same base model
                parts[i] = loaded.cloneColors();
            }
            ModelData merged = client.mergeModels(parts);
            // Palette swaps: chromatic dragons, wizard robes, etc. share one
            // base model and differ only by these recolor tables. Without them
            // every dragon renders red and every mage looks identical.
            short[] find = comp.getColorToReplace();
            short[] replaceWith = comp.getColorToReplaceWith();
            if (find != null && replaceWith != null)
            {
                for (int i = 0; i < find.length && i < replaceWith.length; i++)
                {
                    merged = merged.recolor(find[i], replaceWith[i]);
                }
            }
            // NPC definitions carry non-default scales (128 = 100%); without
            // them some species render comically over- or under-sized
            if (comp.getWidthScale() != 128 || comp.getHeightScale() != 128)
            {
                merged = merged.scale(comp.getWidthScale(), comp.getHeightScale(), comp.getWidthScale());
            }
            if (shiny)
            {
                merged = goldify(merged);
            }
            Model model = merged.light();
            modelCache.put(key, model);
            return model;
        }
        catch (Exception e)
        {
            log.warn("model build failed for species {} (shiny={})", speciesId, shiny, e);
            failedSpecies.add(key);
            return null;
        }
    }

    /**
     * The shiny variant: every face color hue-shifted to gold with boosted
     * saturation, luminance preserved so shading survives. Jagex HSL packs
     * hue in bits 10-15, saturation 7-9, luminance 0-6.
     */
    private static ModelData goldify(ModelData model)
    {
        // Write the face-color array directly: recolor() depends on exact
        // color matching and missed faces on some models. Every face golds,
        // no exceptions.
        ModelData cloned = model.cloneColors();
        short[] faceColors = cloned.getFaceColors();
        for (int i = 0; i < faceColors.length; i++)
        {
            int hsl = faceColors[i] & 0xFFFF;
            int sat = (hsl >> 7) & 0x7;
            int lum = hsl & 0x7F;
            // Lift near-black faces so dark armor visibly golds; keep the
            // original shading gradient everywhere brighter
            int goldLum = Math.min(127, Math.max(lum, 25 + lum / 2));
            faceColors[i] = (short) ((9 << 10) | (Math.min(7, sat + 3) << 7) | goldLum);
        }
        return cloned;
    }

    /**
     * Diagnostic sweep: tries to build one species' model from scratch and
     * says exactly where it fails. Clears any cached failure first so a
     * retry is honest. Client thread only.
     */
    public String diagnose(int speciesId, boolean shiny)
    {
        int key = modelKey(speciesId, shiny);
        failedSpecies.remove(key);
        modelCache.remove(key);
        int npcId = NpcModelData.npcIdFor(speciesId);
        if (npcId < 0)
        {
            return "no npc mapping";
        }
        NPCComposition comp;
        try
        {
            comp = client.getNpcDefinition(npcId);
        }
        catch (Exception e)
        {
            return "composition threw " + e.getClass().getSimpleName() + " (npc " + npcId + ")";
        }
        if (comp == null)
        {
            return "composition null (npc " + npcId + ")";
        }
        if (comp.getModels() == null || comp.getModels().length == 0)
        {
            return "composition has NO models (npc " + npcId + " '" + comp.getName() + "')";
        }
        for (int id : comp.getModels())
        {
            if (client.loadModelData(id) == null)
            {
                return "model part " + id + " failed to load (npc " + npcId + " '" + comp.getName() + "')";
            }
        }
        try
        {
            Model model = modelFor(speciesId, shiny);
            return model != null ? null : "build returned null (npc " + npcId + " '" + comp.getName() + "')";
        }
        catch (Exception e)
        {
            return "build threw " + e.getClass().getSimpleName() + ": " + e.getMessage()
                + " (npc " + npcId + " '" + comp.getName() + "')";
        }
    }

    /**
     * Scene reloads (loading screens, region transitions) destroy scene
     * objects; drop the stale handles so the next sync recreates everything.
     */
    public void invalidate()
    {
        live.clear();
        baseZ.clear();
    }

    /** Merged, lit model for a species; null when unavailable. */
    public Model modelForSpecies(int speciesId)
    {
        return modelFor(speciesId);
    }

    /** Shiny-aware variant for the buddy follower. */
    public Model modelForSpecies(int speciesId, boolean shiny)
    {
        return modelFor(speciesId, shiny);
    }

    /** Client thread only. */
    public void clear()
    {
        for (RuneLiteObject obj : live.values())
        {
            obj.setActive(false);
        }
        live.clear();
        modelCache.clear();
    }
}
