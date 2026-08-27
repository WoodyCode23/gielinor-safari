package com.osrsgo.storage;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Every file this plugin reads or writes lives under a single directory of
 * its own inside the RuneLite folder. Nothing here ever touches a path
 * outside {@link #dir()}.
 *
 * Earlier versions scattered six entries across the RuneLite folder itself,
 * each merely prefixed with the plugin name. {@link #migrateLegacyFiles()}
 * moves those into place once, so a player keeps the animations and NPC
 * records they built up rather than starting the collection over.
 */
@Slf4j
public final class PluginFiles
{
    private static final String DIR_NAME = "gielinor-safari";

    /** Old name, new name. Both loose files and directories. */
    private static final String[][] LEGACY = {
        {"osrsgo-anims.json", "anims.json"},
        {"osrsgo-npcdex.json", "npcdex.json"},
        {"osrsgo-profile-backup.json", "profile-backup.json"},
        {"osrsgo-profile-prerestore.json", "profile-prerestore.json"},
        {"osrsgo-images", "images"},
        {"osrsgo-cards", "cards"},
    };

    /** The plugin's own directory, created if it does not exist yet. */
    public static File dir()
    {
        File dir = new File(RuneLite.RUNELITE_DIR, DIR_NAME);
        if (!dir.isDirectory() && !dir.mkdirs())
        {
            log.warn("could not create the Gielinor Safari directory at {}", dir);
        }
        return dir;
    }

    /** A file inside the plugin directory. */
    public static File file(String name)
    {
        return new File(dir(), name);
    }

    /** A subdirectory inside the plugin directory, created if missing. */
    public static File subDir(String name)
    {
        File sub = new File(dir(), name);
        if (!sub.isDirectory() && !sub.mkdirs())
        {
            log.warn("could not create {}", sub);
        }
        return sub;
    }

    /**
     * Moves anything left in the old locations into the plugin directory.
     * Only ever moves onto a name that is still free, so a file already in
     * the new layout always wins and running this twice does nothing.
     */
    public static void migrateLegacyFiles()
    {
        for (String[] pair : LEGACY)
        {
            File old = new File(RuneLite.RUNELITE_DIR, pair[0]);
            if (!old.exists())
            {
                continue;
            }
            File moved = new File(dir(), pair[1]);
            if (moved.exists())
            {
                continue;
            }
            try
            {
                if (!old.renameTo(moved))
                {
                    log.debug("could not move {} into the plugin directory", pair[0]);
                }
            }
            catch (Exception e)
            {
                log.debug("could not move {} into the plugin directory", pair[0], e);
            }
        }
    }

    private PluginFiles()
    {
    }
}
