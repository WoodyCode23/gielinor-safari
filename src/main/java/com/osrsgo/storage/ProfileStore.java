package com.osrsgo.storage;

import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class ProfileStore
{
    private static final String GROUP = "osrsgo";
    private static final String KEY = "profile";
    private static final String SIG_KEY = "profileSig";
    private static final String BACKUP_KEY = "profileBackup";

    private final ConfigManager configManager;
    private final Gson gson;
    private boolean tamperDetected;
    // The save counter we last wrote or loaded; a mismatch on save means
    // another client (an alt on the same RuneLite profile) is also playing
    private long lastKnownCounter = -1;
    private volatile boolean conflictDetected;

    /** True once when another writer has been detected; resets on read. */
    public boolean consumeConflict()
    {
        boolean c = conflictDetected;
        conflictDetected = false;
        return c;
    }

    private static final java.util.regex.Pattern COUNTER_PATTERN =
        java.util.regex.Pattern.compile("\"saveCounter\"\\s*:\\s*(\\d+)");

    private long storedCounter()
    {
        String json = configManager.getConfiguration(GROUP, KEY);
        if (json == null)
        {
            return -1;
        }
        java.util.regex.Matcher m = COUNTER_PATTERN.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    @Inject
    public ProfileStore(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    /** True when the last load found a profile whose signature didn't match. */
    public boolean wasTampered()
    {
        return tamperDetected;
    }

    public synchronized PlayerProfile load()
    {
        try
        {
            String json = configManager.getConfiguration(GROUP, KEY);
            if (json != null && !json.isEmpty())
            {
                String sig = configManager.getConfiguration(GROUP, SIG_KEY);
                // A mismatched signature is flagged but NEVER wipes: a save
                // race once cost a whole collection. Data survival outranks
                // the anti-cheat ceiling of a client-side game.
                if (sig != null && !sig.isEmpty() && !sign(json).equals(sig))
                {
                    log.warn("Gielinor Safari profile signature mismatch; accepting data anyway");
                    tamperDetected = true;
                }
                PlayerProfile profile = gson.fromJson(json, PlayerProfile.class);
                if (profile != null)
                {
                    lastKnownCounter = profile.saveCounter;
                    normalize(profile);
                    return profile;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load Gielinor Safari profile, starting fresh", e);
        }
        return new PlayerProfile();
    }

    /**
     * Fills in every collection a profile is expected to have and clamps the
     * fields that must sit in range. Profiles written by older versions are
     * missing keys entirely, and Gson leaves those null. Both load paths run
     * this: the restore command promotes a backup straight to the live
     * profile, so an unnormalised backup would put those nulls into play.
     */
    public static void normalize(PlayerProfile profile)
    {
        if (profile.mons == null)
        {
            profile.mons = new java.util.ArrayList<>();
        }
        if (profile.teamIndices == null)
        {
            profile.teamIndices = new java.util.ArrayList<>();
        }
        if (profile.badges == null)
        {
            profile.badges = new java.util.LinkedHashSet<>();
        }
        if (profile.seenSpecies == null)
        {
            profile.seenSpecies = new java.util.LinkedHashSet<>();
        }
        if (profile.caughtSpecies == null)
        {
            profile.caughtSpecies = new java.util.LinkedHashSet<>();
        }
        // Backfill the registry for mons caught before it existed
        if (profile.mons != null)
        {
            for (com.osrsgo.model.OwnedMon m : profile.mons)
            {
                profile.seenSpecies.add(m.speciesId);
                profile.caughtSpecies.add(m.speciesId);
            }
        }
        if (profile.eggs == null)
        {
            profile.eggs = new java.util.ArrayList<>();
        }
        if (profile.stats == null)
        {
            profile.stats = new PlayerProfile.Stats();
        }
        if (profile.essence == null)
        {
            profile.essence = new java.util.HashMap<>();
        }
        if (profile.tierEssence == null)
        {
            profile.tierEssence = new java.util.HashMap<>();
        }
        if (profile.gyms == null)
        {
            profile.gyms = new java.util.HashMap<>();
        }
        if (profile.researchProgress == null || profile.researchProgress.length != 3)
        {
            profile.researchProgress = new int[3];
        }
        if (profile.researchDone == null || profile.researchDone.length != 3)
        {
            profile.researchDone = new boolean[3];
        }
        if (profile.earnedMedals == null)
        {
            profile.earnedMedals = new java.util.LinkedHashSet<>();
        }
        if (profile.weeklyProgress == null || profile.weeklyProgress.length != 2)
        {
            profile.weeklyProgress = new int[2];
        }
        if (profile.weeklyDone == null || profile.weeklyDone.length != 2)
        {
            profile.weeklyDone = new boolean[2];
        }
        if (profile.monthlyProgress == null || profile.monthlyProgress.length != 2)
        {
            profile.monthlyProgress = new int[2];
        }
        if (profile.monthlyDone == null || profile.monthlyDone.length != 2)
        {
            profile.monthlyDone = new boolean[2];
        }
        if (profile.savedTeams == null)
        {
            profile.savedTeams = new java.util.ArrayList<>();
        }
        while (profile.savedTeams.size() < 3)
        {
            profile.savedTeams.add(new java.util.ArrayList<>());
        }
        if (profile.activeTeamSlot < 0 || profile.activeTeamSlot > 2)
        {
            profile.activeTeamSlot = 0;
        }
        // Starter ball grant, and migration for pre-economy profiles
        if (profile.balls <= 0 && profile.tilesWalked == 0)
        {
            profile.balls = 15;
        }
    }

    public synchronized void save(PlayerProfile profile)
    {
        long stored = storedCounter();
        if (lastKnownCounter >= 0 && stored >= 0 && stored != lastKnownCounter)
        {
            // Someone else wrote since we did; we still win (last writer),
            // but the player deserves to know their sessions are fighting
            conflictDetected = true;
        }
        profile.saveCounter = Math.max(stored, lastKnownCounter) + 1;
        lastKnownCounter = profile.saveCounter;
        String json = gson.toJson(profile);
        configManager.setConfiguration(GROUP, KEY, json);
        configManager.setConfiguration(GROUP, SIG_KEY, sign(json));
        maybeBackup(profile, json);
    }

    private static int monCount(String json)
    {
        if (json == null)
        {
            return -1;
        }
        // Must be a key unique to owned mons: GymHolder.holderTeam is a
        // List<MonSpec>, and MonSpec.speciesId serializes under the exact
        // same "speciesId" key, so gym defender teams would otherwise inflate
        // this count and let a backup with fewer real mons get overwritten
        int count = 0;
        int idx = 0;
        while ((idx = json.indexOf("\"caughtAt\"", idx)) != -1)
        {
            count++;
            idx += 10;
        }
        return count;
    }

    /**
     * A second copy that only ever grows: the backup is overwritten only when
     * the current profile has at least as many mons as the backup does, so a
     * wiped or shrunken profile can never destroy it. Also mirrored to a
     * plain file for hand recovery.
     */
    private void maybeBackup(PlayerProfile profile, String json)
    {
        try
        {
            String backup = configManager.getConfiguration(GROUP, BACKUP_KEY);
            if (monCount(json) >= monCount(backup))
            {
                configManager.setConfiguration(GROUP, BACKUP_KEY, json);
                java.io.File file = PluginFiles.file("profile-backup.json");
                java.nio.file.Files.write(file.toPath(),
                    json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        catch (Exception e)
        {
            log.warn("profile backup failed", e);
        }
    }

    /**
     * Writes the profile to osrsgo-profile-prerestore.json before a restore
     * replaces it. A restore is destructive by design, so this file is the
     * only route back if the backup turns out to be older or thinner than the
     * player expected. It is deliberately not read by anything: recovery is a
     * manual act, so a bad restore cannot be compounded by an automatic one.
     */
    public synchronized void snapshotBeforeRestore(PlayerProfile profile)
    {
        try
        {
            java.io.File file = PluginFiles.file("profile-prerestore.json");
            java.nio.file.Files.write(file.toPath(),
                gson.toJson(profile).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            log.warn("pre-restore snapshot failed", e);
        }
    }

    /** The backup profile, or null when none exists. */
    public synchronized PlayerProfile loadBackup()
    {
        try
        {
            String json = configManager.getConfiguration(GROUP, BACKUP_KEY);
            if (json != null && !json.isEmpty())
            {
                PlayerProfile backup = gson.fromJson(json, PlayerProfile.class);
                if (backup != null)
                {
                    // The restore command makes this the live profile outright,
                    // so it has to come back as complete as a normal load
                    normalize(backup);
                    return backup;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("could not load backup profile", e);
        }
        return null;
    }

    /**
     * HMAC over the profile JSON. Stops config-file editing; a determined
     * decompiler can recover the key, which is the accepted ceiling for a
     * client-side game. The key is assembled at runtime to avoid being a
     * single greppable string literal.
     */
    private String sign(String json)
    {
        try
        {
            byte[] key = keyMaterial();
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest)
            {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (Exception e)
        {
            log.warn("profile signing unavailable", e);
            return "";
        }
    }

    private static byte[] keyMaterial()
    {
        long[] parts = {0x4765727472756465L, 0x6C6F766573686572L, 0x6B697474656E7321L, 0x6F737273676F7631L};
        byte[] key = new byte[parts.length * 8];
        for (int i = 0; i < parts.length; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                key[i * 8 + j] = (byte) ((parts[i] >>> (56 - j * 8)) ^ (0x5A + i * 7 + j));
            }
        }
        return key;
    }
}
