package com.osrsgo.data;

import com.osrsgo.model.MonType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Daily research: three tasks seeded from the UTC day, identical for every
 * player. Rewards are granted the moment a task completes.
 */
public final class ResearchData
{
    public enum Kind
    {
        CATCH_ANY,
        CATCH_TYPE,
        THROW_BALLS,
        WALK_TILES,
        WIN_GYM,
        CATCH_UNCOMMON_PLUS,
        HATCH_EGGS
    }

    public static class Task
    {
        public final Kind kind;
        public final MonType type;
        public final int goal;
        public final int rewardBalls;
        public final int rewardGreatBalls;
        public final int rewardUltraBalls;
        public final int rewardXp;

        Task(Kind kind, MonType type, int goal, int rewardBalls, int rewardGreat, int rewardUltra, int rewardXp)
        {
            this.kind = kind;
            this.type = type;
            this.goal = goal;
            this.rewardBalls = rewardBalls;
            this.rewardGreatBalls = rewardGreat;
            this.rewardUltraBalls = rewardUltra;
            this.rewardXp = rewardXp;
        }

        public String describe()
        {
            switch (kind)
            {
                case CATCH_ANY: return "Catch " + goal + " wild mons";
                case CATCH_TYPE: return "Catch " + goal + " " + type.getDisplay() + " mons";
                case THROW_BALLS: return "Throw " + goal + " balls";
                case WALK_TILES: return "Walk " + goal + " tiles";
                case WIN_GYM: return "Win " + goal + " gym battle" + (goal > 1 ? "s" : "");
                case HATCH_EGGS: return "Hatch " + goal + " egg" + (goal > 1 ? "s" : "");
                default: return "Catch " + goal + " Uncommon or rarer mon" + (goal > 1 ? "s" : "");
            }
        }

        public String rewardText()
        {
            StringBuilder sb = new StringBuilder();
            if (rewardBalls > 0)
            {
                sb.append("+").append(rewardBalls).append(" balls ");
            }
            if (rewardGreatBalls > 0)
            {
                sb.append("+").append(rewardGreatBalls).append(" Great ");
            }
            if (rewardUltraBalls > 0)
            {
                sb.append("+").append(rewardUltraBalls).append(" Ultra ");
            }
            sb.append("+").append(rewardXp).append(" xp");
            return sb.toString();
        }
    }

    public static long todayUtc()
    {
        return System.currentTimeMillis() / 86_400_000L;
    }

    /** Weeks start Monday (epoch day 0 was a Thursday). */
    public static long weekUtc()
    {
        return (todayUtc() + 3) / 7;
    }

    public static long monthUtc()
    {
        java.time.YearMonth ym = java.time.YearMonth.now(java.time.ZoneOffset.UTC);
        return ym.getYear() * 12L + ym.getMonthValue();
    }

    public static List<Task> tasksFor(long day)
    {
        Random rng = new Random(day * 0x9E3779B97F4A7C15L + 42);
        List<Task> pool = new ArrayList<>();
        MonType type = MonType.values()[rng.nextInt(MonType.values().length)];
        pool.add(new Task(Kind.CATCH_ANY, null, 3 + rng.nextInt(3), 5, 0, 0, 50));
        pool.add(new Task(Kind.CATCH_TYPE, type, 2 + rng.nextInt(2), 3, 0, 0, 150));
        pool.add(new Task(Kind.THROW_BALLS, null, 5 + rng.nextInt(4), 3, 0, 0, 50));
        pool.add(new Task(Kind.WALK_TILES, null, 500 + rng.nextInt(1000), 2, 0, 0, 100));
        pool.add(new Task(Kind.WIN_GYM, null, 1, 5, 0, 0, 150));
        pool.add(new Task(Kind.CATCH_UNCOMMON_PLUS, null, 1 + rng.nextInt(2), 5, 0, 0, 100));

        List<Task> picked = new ArrayList<>();
        while (picked.size() < 3 && !pool.isEmpty())
        {
            picked.add(pool.remove(rng.nextInt(pool.size())));
        }
        return picked;
    }

    /** Two bigger weekly challenges paying Great Balls. */
    public static List<Task> weeklyTasks(long week)
    {
        Random rng = new Random(week * 0xA24BAED4963EE407L + 7);
        List<Task> pool = new ArrayList<>();
        pool.add(new Task(Kind.CATCH_ANY, null, 20 + rng.nextInt(11), 0, 3, 0, 300));
        pool.add(new Task(Kind.WALK_TILES, null, 4000 + rng.nextInt(4001), 0, 3, 0, 300));
        pool.add(new Task(Kind.WIN_GYM, null, 3 + rng.nextInt(3), 0, 3, 0, 400));
        pool.add(new Task(Kind.THROW_BALLS, null, 30 + rng.nextInt(21), 0, 2, 0, 250));
        pool.add(new Task(Kind.CATCH_UNCOMMON_PLUS, null, 5 + rng.nextInt(4), 0, 3, 0, 350));

        List<Task> picked = new ArrayList<>();
        while (picked.size() < 2 && !pool.isEmpty())
        {
            picked.add(pool.remove(rng.nextInt(pool.size())));
        }
        return picked;
    }

    /** Two monthly epics paying Ultra Balls. */
    public static List<Task> monthlyTasks(long month)
    {
        Random rng = new Random(month * 0xD6E8FEB86659FD93L + 31);
        List<Task> pool = new ArrayList<>();
        pool.add(new Task(Kind.CATCH_ANY, null, 80 + rng.nextInt(41), 0, 0, 1, 1000));
        pool.add(new Task(Kind.WALK_TILES, null, 15000 + rng.nextInt(10001), 0, 0, 1, 1000));
        pool.add(new Task(Kind.WIN_GYM, null, 10 + rng.nextInt(6), 0, 0, 1, 1200));
        pool.add(new Task(Kind.HATCH_EGGS, null, 3 + rng.nextInt(3), 0, 0, 1, 1000));
        pool.add(new Task(Kind.CATCH_UNCOMMON_PLUS, null, 15 + rng.nextInt(11), 0, 0, 1, 1100));

        List<Task> picked = new ArrayList<>();
        while (picked.size() < 2 && !pool.isEmpty())
        {
            picked.add(pool.remove(rng.nextInt(pool.size())));
        }
        return picked;
    }

    private ResearchData()
    {
    }
}
