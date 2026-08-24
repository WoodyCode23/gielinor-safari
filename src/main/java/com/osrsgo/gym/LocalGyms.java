package com.osrsgo.gym;

import com.osrsgo.battle.MonSpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Gym ownership operations against the player's local map. Kept as pure
 * functions over the map so the same state can be driven from anywhere: a
 * local claim, a rival trainer seizing a gym, or a party member's broadcast.
 */
public final class LocalGyms
{
    public static GymHolder claim(Map<String, GymHolder> gyms, String gymId, String rsn,
        List<MonSpec> team, Integer world, String faction)
    {
        String now = Instant.now().toString();
        GymHolder holder = new GymHolder();
        holder.gymId = gymId;
        holder.holderRsn = rsn;
        holder.holderTeam = team;
        holder.heldSince = now;
        holder.updatedAt = now;
        holder.defenseWins = 0;
        holder.claimedWorld = world;
        holder.holderFaction = faction;
        gyms.put(gymId, holder);
        return holder;
    }

    /** The challenger beat the defenders but lost to the leader: nobody holds it. */
    public static void breakHold(Map<String, GymHolder> gyms, String gymId)
    {
        gyms.remove(gymId);
    }

    public static void recordDefense(Map<String, GymHolder> gyms, String gymId)
    {
        GymHolder holder = gyms.get(gymId);
        if (holder != null)
        {
            holder.defenseWins++;
            holder.updatedAt = Instant.now().toString();
        }
    }

    /** Drops holds past their duration, the way the server swept them lazily. */
    public static void expireStale(Map<String, GymHolder> gyms)
    {
        gyms.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isExpired());
    }

    public static int heldBy(Map<String, GymHolder> gyms, String rsn)
    {
        int held = 0;
        for (GymHolder holder : gyms.values())
        {
            if (holder != null && holder.isHeldBy(rsn))
            {
                held++;
            }
        }
        return held;
    }

    /**
     * Whether a holder is protected from attack by the shared-faction rule.
     * Party members never are: two friends of one god must still be able to
     * contest a gym, or the party layer does nothing for them.
     */
    public static boolean isAlly(String playerFaction, GymHolder holder, boolean holderIsPartyMember)
    {
        return !holderIsPartyMember
            && playerFaction != null
            && holder != null
            && playerFaction.equals(holder.holderFaction);
    }

    private LocalGyms()
    {
    }
}
