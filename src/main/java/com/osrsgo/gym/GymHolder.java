package com.osrsgo.gym;

import com.osrsgo.battle.MonSpec;
import java.util.List;

/** Local control state for one gym, held by whoever last claimed it. */
public class GymHolder
{
    public String gymId;
    public String holderRsn;
    public List<MonSpec> holderTeam;
    public String heldSince;
    public int defenseWins;
    public String updatedAt;
    public Integer claimedWorld;
    public String holderFaction;

    /** Holds lapse after 6h so a claim from an inactive session can't lock a gym forever. */
    public static final long HOLD_DURATION_MS = 6 * 60 * 60 * 1000L;

    public boolean isExpired()
    {
        if (heldSince == null)
        {
            return false;
        }
        try
        {
            long held = java.time.Instant.parse(heldSince).toEpochMilli();
            return System.currentTimeMillis() - held >= HOLD_DURATION_MS;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean isClaimed()
    {
        return holderRsn != null && !holderRsn.isEmpty() && !isExpired();
    }

    public boolean isHeldBy(String rsn)
    {
        return isClaimed() && rsn != null && holderRsn.equalsIgnoreCase(rsn);
    }
}
