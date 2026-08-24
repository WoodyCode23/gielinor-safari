package com.osrsgo.spawn;

import com.osrsgo.model.OwnedMon;

/**
 * A catch attempt in flight: the outcome is rolled up front, but applied only
 * after the overlay finishes animating the ball throw and shakes.
 */
public class CatchSequence
{
    public enum Outcome
    {
        CAUGHT,
        BROKE_FREE,
        FLED
    }

    public static final long THROW_MS = 700;
    public static final long SHAKE_MS = 1500;
    public static final long RESULT_MS = 900;
    public static final long TOTAL_MS = THROW_MS + SHAKE_MS + RESULT_MS;

    public final WildSpawn spawn;
    public final Outcome outcome;
    public final OwnedMon pendingMon;
    public final int ballTier;
    public final long startMs;

    public CatchSequence(WildSpawn spawn, Outcome outcome, OwnedMon pendingMon, int ballTier)
    {
        this.spawn = spawn;
        this.outcome = outcome;
        this.pendingMon = pendingMon;
        this.ballTier = ballTier;
        this.startMs = System.currentTimeMillis();
    }

    public long elapsed()
    {
        return System.currentTimeMillis() - startMs;
    }

    public boolean finished()
    {
        return elapsed() >= TOTAL_MS;
    }
}
