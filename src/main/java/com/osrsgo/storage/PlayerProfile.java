package com.osrsgo.storage;

import com.osrsgo.model.OwnedMon;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlayerProfile
{
    public List<OwnedMon> mons = new ArrayList<>();
    public List<Integer> teamIndices = new ArrayList<>();
    // Three switchable team loadouts; teamIndices is always the ACTIVE one
    public int activeTeamSlot;
    public List<List<Integer>> savedTeams = new ArrayList<>();
    public Set<String> badges = new LinkedHashSet<>();
    public Set<Integer> seenSpecies = new LinkedHashSet<>();
    public Set<Integer> caughtSpecies = new LinkedHashSet<>();
    public int trainerXp;
    // Field default doubles as the starter grant: Gson leaves it when a stored
    // profile predates the ball economy
    public int balls = 15;
    public int greatBalls;
    public int superBalls;
    public int ultraBalls;
    public int masterBalls;
    public long tilesWalked;
    public int ballProgress;
    public int xpBallProgress;
    public Integer buddyIndex;
    public int buddyProgress;
    public List<com.osrsgo.model.Egg> eggs = new ArrayList<>();
    public Stats stats = new Stats();
    // Legacy per-species essence; folded into tierEssence on first touch
    public java.util.Map<Integer, Integer> essence = new java.util.HashMap<>();
    // Essence pools by rarity tier (COMMON..LEGENDARY): any mon of a tier
    // feeds the pool, any same-tier evolve/level spends from it
    public java.util.Map<String, Integer> tierEssence = new java.util.HashMap<>();
    // Gym control state, held locally. Keyed by gym id.
    public java.util.Map<String, com.osrsgo.gym.GymHolder> gyms = new java.util.HashMap<>();
    public String faction;
    public int berries;
    public int berryProgress;
    public long researchDay = -1;
    public int[] researchProgress = new int[3];
    public boolean[] researchDone = new boolean[3];
    public long researchWeek = -1;
    public int[] weeklyProgress = new int[2];
    public boolean[] weeklyDone = new boolean[2];
    public long researchMonth = -1;
    public int[] monthlyProgress = new int[2];
    public boolean[] monthlyDone = new boolean[2];
    public Set<String> earnedMedals = new LinkedHashSet<>();
    // Nearby-tab target filter: only these species show when non-empty
    public Set<Integer> huntTargets = new LinkedHashSet<>();
    // Raid bosses already challenged this rotation ("bucket:gymId"), pruned each rotation
    public Set<String> raidAttempts = new LinkedHashSet<>();
    // Purple sweets scooped off the ground; each levels a mon by one
    public int rareCandies;
    // Universal essence from releasing shinies; substitutes for any species essence
    public int shinyEssence;
    // Bumped on every save; lets a session notice another client writing
    public long saveCounter;

    public static String tierOf(int speciesId)
    {
        com.osrsgo.model.Species sp = com.osrsgo.data.SpeciesData.byId(speciesId);
        return sp != null ? sp.getRarity().name() : com.osrsgo.model.Rarity.COMMON.name();
    }

    /** Folds any legacy per-species essence into the tier pools. Idempotent. */
    public void migrateEssence()
    {
        if (essence.isEmpty())
        {
            return;
        }
        for (java.util.Map.Entry<Integer, Integer> e : essence.entrySet())
        {
            tierEssence.merge(tierOf(e.getKey()), e.getValue(), Integer::sum);
        }
        essence.clear();
    }

    public int essenceOf(int speciesId)
    {
        migrateEssence();
        Integer amount = tierEssence.get(tierOf(speciesId));
        return amount != null ? amount : 0;
    }

    public void addEssence(int speciesId, int amount)
    {
        migrateEssence();
        tierEssence.merge(tierOf(speciesId), amount, Integer::sum);
    }

    /** Lifetime counters, local-only. */
    public static class Stats
    {
        public int ballsFound;
        public int ballsThrown;
        public int catches;
        public int shinyCatches;
        public int breakFrees;
        public int flees;
        public int gymWins;
        public int gymLosses;
        public int gymsCaptured;
        public int pvpWins;
        public int pvpLosses;
        public int breeds;
        public int eggsHatched;
        public int evolutions;
        public int berriesFound;
        public int candiesFound;
        public int berriesUsed;
        public int trades;
        public int raidWins;
        public int researchCompleted;
    }

    public int trainerLevel()
    {
        return 1 + (int) Math.floor(Math.sqrt(trainerXp / 100.0));
    }

    public int xpIntoLevel()
    {
        int lvl = trainerLevel();
        return trainerXp - xpAtLevel(lvl);
    }

    public int xpForNextLevel()
    {
        int lvl = trainerLevel();
        return xpAtLevel(lvl + 1) - xpAtLevel(lvl);
    }

    private static int xpAtLevel(int level)
    {
        int n = level - 1;
        return n * n * 100;
    }

    public List<OwnedMon> team()
    {
        List<OwnedMon> team = new ArrayList<>();
        for (Integer idx : teamIndices)
        {
            if (idx != null && idx >= 0 && idx < mons.size())
            {
                team.add(mons.get(idx));
            }
        }
        return team;
    }
}
