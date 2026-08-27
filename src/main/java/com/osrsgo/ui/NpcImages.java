package com.osrsgo.ui;

import com.osrsgo.model.Species;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Real NPC portraits from the OSRS Wiki (chatheads preferred, renders as
 * fallback), fetched once and cached to disk. Rows fall back to the drawn
 * medallions until an image arrives; a version counter bumps on every new
 * image so the panel knows to rebuild. Images are CC BY-SA from
 * oldschool.runescape.wiki.
 */
@Slf4j
@Singleton
public class NpcImages
{
    private static final String BASE = "https://oldschool.runescape.wiki/w/Special:FilePath/";
    private static final int TARGET_HEIGHT = 28;
    private static final int MAX_WIDTH = 44;

    private final OkHttpClient httpClient;
    private final File cacheDir;
    private final Map<Integer, BufferedImage> images = new HashMap<>();
    private final Map<Integer, BufferedImage> darkened = new HashMap<>();
    private final Set<Integer> misses = new HashSet<>();
    private final Set<Integer> inFlight = new HashSet<>();
    private final AtomicInteger version = new AtomicInteger();
    private volatile Runnable onUpdate;

    // Species whose wiki file names don't follow any regular pattern
    private static final Map<String, String> OVERRIDES = new HashMap<>();

    static
    {
        OVERRIDES.put("Zulrah", "Zulrah_(serpentine).png");
        OVERRIDES.put("Zombie", "Zombie_(Level_13).png");
        OVERRIDES.put("Barbarian", "Barbarian_(Blue_Moon_Inn)_chathead.png");
        OVERRIDES.put("Grizzly Bear", "Grizzly_bear_(level_21).png");
        OVERRIDES.put("Giant Spider", "Giant_spider_(Level_27).png");
    }

    @Inject
    public NpcImages(OkHttpClient httpClient)
    {
        this.httpClient = httpClient;
        this.cacheDir = com.osrsgo.storage.PluginFiles.subDir("images");
    }

    public void setOnUpdate(Runnable onUpdate)
    {
        this.onUpdate = onUpdate;
    }

    public int getVersion()
    {
        return version.get();
    }

    /**
     * Returns the cached portrait, or null (kicking off a background fetch on
     * the first ask). Callers should fall back to the drawn medallion.
     */
    public synchronized BufferedImage cachedOrRequest(Species species)
    {
        int id = species.getId();
        BufferedImage cached = images.get(id);
        if (cached != null)
        {
            return cached;
        }
        if (misses.contains(id) || inFlight.contains(id))
        {
            return null;
        }
        File onDisk = new File(cacheDir, id + ".png");
        if (onDisk.isFile())
        {
            try
            {
                BufferedImage img = ImageIO.read(onDisk);
                if (img != null)
                {
                    images.put(id, img);
                    return img;
                }
            }
            catch (IOException e)
            {
                log.debug("bad cached image for {}", species.getName(), e);
            }
        }
        // ".miss2" is the current marker generation: renaming it once
        // invalidated every miss recorded by the buggy fetcher that gave up
        // on the first network failure. Old markers just get cleaned up.
        File legacyMiss = new File(cacheDir, id + ".miss");
        if (legacyMiss.isFile())
        {
            //noinspection ResultOfMethodCallIgnored
            legacyMiss.delete();
        }
        File missFile = new File(cacheDir, id + ".miss2");
        if (missFile.isFile())
        {
            if (System.currentTimeMillis() - missFile.lastModified() < 7L * 24 * 60 * 60 * 1000)
            {
                misses.add(id);
                return null;
            }
            // A week old: the wiki may have the file now, try again
            //noinspection ResultOfMethodCallIgnored
            missFile.delete();
        }
        inFlight.add(id);
        fetchNext(species, candidatesFor(species.getName()), 0);
        return null;
    }

    /** Darkened silhouette-ish variant for seen-but-uncaught Pokedex entries. */
    public synchronized BufferedImage darkenedCached(Species species)
    {
        BufferedImage base = cachedOrRequest(species);
        if (base == null)
        {
            return null;
        }
        return darkened.computeIfAbsent(species.getId(), id ->
        {
            BufferedImage dark = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dark.createGraphics();
            g.drawImage(base, 0, 0, null);
            g.setComposite(java.awt.AlphaComposite.SrcAtop);
            g.setColor(new java.awt.Color(20, 20, 25, 215));
            g.fillRect(0, 0, dark.getWidth(), dark.getHeight());
            g.dispose();
            return dark;
        });
    }

    private static List<String> candidatesFor(String name)
    {
        List<String> candidates = new ArrayList<>();
        String override = OVERRIDES.get(name);
        if (override != null)
        {
            candidates.add(override);
        }
        String exact = name.replace(' ', '_');
        String lowerRest = exact.length() > 1
            ? Character.toUpperCase(exact.charAt(0)) + exact.substring(1).toLowerCase()
            : exact;
        candidates.add(exact + "_chathead.png");
        if (!lowerRest.equals(exact))
        {
            candidates.add(lowerRest + "_chathead.png");
        }
        candidates.add(exact + ".png");
        if (!lowerRest.equals(exact))
        {
            candidates.add(lowerRest + ".png");
        }
        candidates.add(exact + "_(1).png");
        if (!lowerRest.equals(exact))
        {
            candidates.add(lowerRest + "_(1).png");
        }
        return candidates;
    }

    private void fetchNext(Species species, List<String> candidates, int index)
    {
        if (index >= candidates.size())
        {
            // Plain names exhausted: many monsters only have variant-suffixed
            // files ("Zombie (Level 13).png"); ask the wiki what exists
            apiLookup(species, 0);
            return;
        }
        String file = candidates.get(index).replace("'", "%27");
        Request request = new Request.Builder().url(BASE + file + "?width=64").build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                // Network blip on one candidate must not doom the species
                log.debug("image fetch failed for {}: {}", species.getName(), e.getMessage());
                fetchNext(species, candidates, index + 1);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    if (!r.isSuccessful() || r.body() == null)
                    {
                        fetchNext(species, candidates, index + 1);
                        return;
                    }
                    BufferedImage raw = ImageIO.read(r.body().byteStream());
                    if (raw == null)
                    {
                        fetchNext(species, candidates, index + 1);
                        return;
                    }
                    store(species, scale(raw));
                }
                catch (Exception e)
                {
                    log.debug("image decode failed for {}", species.getName(), e);
                    fetchNext(species, candidates, index + 1);
                }
            }
        });
    }

    /**
     * Asks the MediaWiki API which files start with "<Name>_(" and picks the
     * most generic-looking one. Prefixes are case-sensitive past the first
     * letter, so both name casings get a try.
     */
    private void apiLookup(Species species, int prefixIndex)
    {
        String name = species.getName().replace(' ', '_');
        String lowerRest = name.length() > 1
            ? Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase()
            : name;
        List<String> prefixes = new ArrayList<>();
        prefixes.add(name + "_(");
        if (!lowerRest.equals(name))
        {
            prefixes.add(lowerRest + "_(");
        }
        if (prefixIndex >= prefixes.size())
        {
            recordMiss(species);
            return;
        }
        String url;
        try
        {
            url = "https://oldschool.runescape.wiki/api.php?action=query&format=json&list=allimages&ailimit=50&aiprefix="
                + java.net.URLEncoder.encode(prefixes.get(prefixIndex), "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            recordMiss(species);
            return;
        }
        httpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                apiLookup(species, prefixIndex + 1);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    String best = r.isSuccessful() && r.body() != null
                        ? pickBestFile(r.body().string())
                        : null;
                    if (best == null)
                    {
                        apiLookup(species, prefixIndex + 1);
                        return;
                    }
                    List<String> single = new ArrayList<>();
                    single.add(best);
                    fetchFinal(species, single);
                }
                catch (Exception e)
                {
                    apiLookup(species, prefixIndex + 1);
                }
            }
        });
    }

    /** Most generic variant: skip seasonal/historical files, prefer chatheads, then shortest. */
    private static String pickBestFile(String json)
    {
        com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
        if (!root.has("query") || !root.getAsJsonObject("query").has("allimages"))
        {
            return null;
        }
        String best = null;
        boolean bestChathead = false;
        for (com.google.gson.JsonElement el : root.getAsJsonObject("query").getAsJsonArray("allimages"))
        {
            String file = el.getAsJsonObject().get("name").getAsString();
            String lower = file.toLowerCase();
            if (!lower.endsWith(".png") || lower.contains("historical") || lower.contains("unused")
                || lower.contains("event") || lower.contains("halloween") || lower.contains("christmas")
                || lower.contains("easter"))
            {
                continue;
            }
            boolean chathead = lower.contains("chathead");
            boolean better = best == null
                || (chathead && !bestChathead)
                || (chathead == bestChathead && file.length() < best.length());
            if (better)
            {
                best = file;
                bestChathead = chathead;
            }
        }
        return best;
    }

    /** Last-chance fetch of an API-resolved file name; a miss here is final. */
    private void fetchFinal(Species species, List<String> candidates)
    {
        String file = candidates.get(0).replace("'", "%27").replace(' ', '_');
        Request request = new Request.Builder().url(BASE + file + "?width=64").build();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                recordMiss(species);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response r = response)
                {
                    BufferedImage raw = r.isSuccessful() && r.body() != null
                        ? ImageIO.read(r.body().byteStream())
                        : null;
                    if (raw == null)
                    {
                        recordMiss(species);
                        return;
                    }
                    store(species, scale(raw));
                }
                catch (Exception e)
                {
                    recordMiss(species);
                }
            }
        });
    }

    private static BufferedImage scale(BufferedImage raw)
    {
        double factor = TARGET_HEIGHT / (double) raw.getHeight();
        int w = Math.min(MAX_WIDTH, Math.max(8, (int) Math.round(raw.getWidth() * factor)));
        int h = TARGET_HEIGHT;
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(raw, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private synchronized void store(Species species, BufferedImage img)
    {
        images.put(species.getId(), img);
        inFlight.remove(species.getId());
        try
        {
            ImageIO.write(img, "png", new File(cacheDir, species.getId() + ".png"));
        }
        catch (IOException e)
        {
            log.debug("could not cache image for {}", species.getName(), e);
        }
        version.incrementAndGet();
        Runnable cb = onUpdate;
        if (cb != null)
        {
            cb.run();
        }
    }

    private synchronized void recordMiss(Species species)
    {
        misses.add(species.getId());
        inFlight.remove(species.getId());
        try
        {
            //noinspection ResultOfMethodCallIgnored
            new File(cacheDir, species.getId() + ".miss2").createNewFile();
        }
        catch (IOException ignored)
        {
        }
    }
}
