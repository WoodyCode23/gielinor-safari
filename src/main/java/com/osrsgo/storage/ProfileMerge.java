package com.osrsgo.storage;

import com.osrsgo.model.OwnedMon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what a backup restore is actually missing.
 *
 * Restoring used to append the backup's mons wholesale, so running it on a
 * healthy profile doubled the collection every time. This computes a multiset
 * difference instead: a mon present on both sides is matched and skipped, and
 * only the shortfall is handed back. That makes a restore idempotent, tops up
 * a partially lost collection without duplicating what survived, and still
 * returns everything after a full wipe.
 */
public final class ProfileMerge
{
    /**
     * A mon's birth certificate: the fields fixed when it was acquired and
     * never touched afterwards. Level, xp, hp, nickname and favourite all
     * drift with play, so they cannot recognise the same mon across a live
     * profile and an older backup of it.
     */
    public static String monKey(OwnedMon m)
    {
        return m.speciesId + ":" + m.caughtAt + ":" + m.ivHp + ":" + m.ivAtk
            + ":" + m.ivDef + ":" + m.ivSpd + ":" + m.shiny;
    }

    /**
     * The mons in {@code backup} that {@code live} does not already account
     * for, in backup order. Counting rather than set membership matters: two
     * genuinely distinct mons can share a birth certificate (eggs hatching in
     * the same tick, or old profiles saved before caughtAt existed, which
     * deserialise with caughtAt 0), and both should survive a restore.
     */
    public static List<OwnedMon> missingFrom(List<OwnedMon> live, List<OwnedMon> backup)
    {
        List<OwnedMon> missing = new ArrayList<>();
        if (backup == null)
        {
            return missing;
        }
        Map<String, Integer> have = new HashMap<>();
        if (live != null)
        {
            for (OwnedMon m : live)
            {
                have.merge(monKey(m), 1, Integer::sum);
            }
        }
        for (OwnedMon m : backup)
        {
            String key = monKey(m);
            Integer count = have.get(key);
            if (count != null && count > 0)
            {
                have.put(key, count - 1);
                continue;
            }
            missing.add(m);
        }
        return missing;
    }

    private ProfileMerge()
    {
    }
}
