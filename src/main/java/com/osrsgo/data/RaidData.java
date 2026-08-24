package com.osrsgo.data;

import com.osrsgo.model.Rarity;
import com.osrsgo.model.Species;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Raid bosses: every spawn rotation, two gyms host a Legendary raid boss.
 * Seeded from the bucket so every client sees the same raids. Beating one
 * grants big XP and catch attempts at the boss.
 */
public final class RaidData
{
    public static class Raid
    {
        public final String gymId;
        public final int speciesId;
        public final int level;

        Raid(String gymId, int speciesId, int level)
        {
            this.gymId = gymId;
            this.speciesId = speciesId;
            this.level = level;
        }
    }

    public static List<Raid> raidsFor(long bucket)
    {
        Random rng = new Random(bucket * 0xC2B2AE3D27D4EB4FL + 777);
        List<GymData.Gym> gyms = new ArrayList<>(GymData.all());
        List<Species> legendaries = new ArrayList<>();
        for (Species s : SpeciesData.all())
        {
            if (s.getRarity() == Rarity.LEGENDARY)
            {
                legendaries.add(s);
            }
        }
        List<Raid> raids = new ArrayList<>();
        for (int i = 0; i < 2 && !gyms.isEmpty(); i++)
        {
            GymData.Gym gym = gyms.remove(rng.nextInt(gyms.size()));
            Species boss = legendaries.get(rng.nextInt(legendaries.size()));
            raids.add(new Raid(gym.id, boss.getId(), 70 + rng.nextInt(16)));
        }
        return raids;
    }

    public static Raid raidAt(long bucket, String gymId)
    {
        for (Raid raid : raidsFor(bucket))
        {
            if (raid.gymId.equals(gymId))
            {
                return raid;
            }
        }
        return null;
    }

    private RaidData()
    {
    }
}
