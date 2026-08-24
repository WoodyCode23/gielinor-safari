package com.osrsgo.overlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Renders both combatants' models in the scene during a battle and smashes
 * them into each other on every resolved turn, Pokemon-style. Models sit on
 * two tiles near where the battle started; ClientTick interpolates the lunge.
 */
@Singleton
public class BattleScene
{
    private static final long CLASH_MS = 450;
    private static final long DEATH_MS = 600;
    private static final int DEATH_SINK = 420;
    private static final int FACE_EAST = 1536;
    private static final int FACE_WEST = 512;

    private final Client client;
    private final SpawnModels spawnModels;
    private final com.osrsgo.anim.AnimationLibrary animLibrary;
    private final net.runelite.client.callback.ClientThread clientThread;
    private boolean attackAnimPlaying;

    private boolean active;
    private RuneLiteObject mine;
    private RuneLiteObject theirs;
    private int mineSpecies = -1;
    private int theirsSpecies = -1;
    private WorldPoint mineWp;
    private WorldPoint theirsWp;
    private long clashStart;
    // Faint transition: the outgoing model sinks into the ground before the
    // replacement appears
    private long mineSwapAt;
    private long theirsSwapAt;
    private int pendingMineSpecies = -1;
    private int pendingTheirsSpecies = -1;
    private int mineDeathZ;
    private int theirsDeathZ;
    // Victory celebration: the winner's mon hops in place before the scene ends
    private long celebrateUntil;
    private int celebrateBaseZ;
    private static final long CELEBRATE_MS = 1500;
    // FX state read by the overlay: send-out flashes and hitsplat values
    private long mineSwitchStart;
    private long theirsSwitchStart;
    private int hitOnMine = -1;
    private int hitOnTheirs = -1;

    @Inject
    public BattleScene(Client client, SpawnModels spawnModels,
        com.osrsgo.anim.AnimationLibrary animLibrary,
        net.runelite.client.callback.ClientThread clientThread)
    {
        this.client = client;
        this.spawnModels = spawnModels;
        this.animLibrary = animLibrary;
        this.clientThread = clientThread;
    }

    /** Game tick: keeps the two models existing and correct. Client thread only. */
    public void ensure(int mySpeciesId, int oppSpeciesId, WorldPoint playerWp)
    {
        if (playerWp == null)
        {
            return;
        }
        if (!active)
        {
            int[] offs = pickStage(playerWp);
            mineWp = new WorldPoint(playerWp.getX() + offs[0], playerWp.getY() + offs[1], playerWp.getPlane());
            theirsWp = new WorldPoint(playerWp.getX() + offs[2], playerWp.getY() + offs[3], playerWp.getPlane());
            active = true;
        }
        if (mine == null)
        {
            mine = respawn(null, mySpeciesId, mineWp, FACE_EAST);
            mineSpecies = mySpeciesId;
            mineSwapAt = 0;
        }
        else if (mineSpecies != mySpeciesId)
        {
            if (mineSwapAt == 0)
            {
                mineSwapAt = System.currentTimeMillis() + DEATH_MS;
                mineDeathZ = mine.getZ();
            }
            pendingMineSpecies = mySpeciesId;
        }
        if (theirs == null)
        {
            theirs = respawn(null, oppSpeciesId, theirsWp, FACE_WEST);
            theirsSpecies = oppSpeciesId;
            theirsSwapAt = 0;
        }
        else if (theirsSpecies != oppSpeciesId)
        {
            if (theirsSwapAt == 0)
            {
                theirsSwapAt = System.currentTimeMillis() + DEATH_MS;
                theirsDeathZ = theirs.getZ();
            }
            pendingTheirsSpecies = oppSpeciesId;
        }
    }

    // Candidate stages, mine west of theirs so the fixed facings stay right:
    // north pair first (the classic), then south, further out, and finally
    // flanking the trainer as a last resort
    private static final int[][] STAGE_OFFSETS = {
        {-1, 2, 2, 2},
        {-1, -2, 2, -2},
        {-1, 3, 2, 3},
        {-1, -3, 2, -3},
        {-2, 0, 2, 0},
    };

    /** First stage whose two tiles are provably walkable (no bank booths), else the default. */
    private int[] pickStage(WorldPoint p)
    {
        for (int[] o : STAGE_OFFSETS)
        {
            WorldPoint a = new WorldPoint(p.getX() + o[0], p.getY() + o[1], p.getPlane());
            WorldPoint b = new WorldPoint(p.getX() + o[2], p.getY() + o[3], p.getPlane());
            if (Boolean.TRUE.equals(com.osrsgo.spawn.Coords.walkableInScene(client, a))
                && Boolean.TRUE.equals(com.osrsgo.spawn.Coords.walkableInScene(client, b)))
            {
                return o;
            }
        }
        return STAGE_OFFSETS[0];
    }

    private RuneLiteObject respawn(RuneLiteObject old, int speciesId, WorldPoint wp, int orientation)
    {
        if (old != null)
        {
            old.setActive(false);
        }
        Model model = spawnModels.modelForSpecies(speciesId);
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,wp);
        if (model == null || lp == null)
        {
            return null;
        }
        RuneLiteObject obj = client.createRuneLiteObject();
        obj.setModel(model);
        obj.setLocation(lp, wp.getPlane());
        obj.setOrientation(orientation);
        obj.setActive(true);
        applyIdle(obj, speciesId);
        return obj;
    }

    /**
     * loadAnimation is client-thread only and throws IllegalStateException
     * anywhere else. Battle turns are driven from Swing (the move buttons), so
     * every animation swap defers to the client thread; invoke() runs inline
     * when we are already on it. A throw here used to escape mid-turn and skip
     * everything after it, silently costing gym claims and boss throws.
     */
    private void setAnim(RuneLiteObject target, int animId)
    {
        clientThread.invoke(() ->
        {
            try
            {
                target.setAnimation(client.loadAnimation(animId));
                target.setShouldLoop(true);
            }
            catch (Exception e)
            {
                // A bad learned animation is cosmetic; never fatal
            }
        });
    }

    private void applyIdle(RuneLiteObject target, int forSpecies)
    {
        Integer idle = animLibrary.idleFor(forSpecies);
        if (target != null && idle != null)
        {
            setAnim(target, idle);
        }
    }

    private void applyAttack(RuneLiteObject target, int forSpecies)
    {
        Integer attack = animLibrary.attackFor(forSpecies);
        if (target != null && attack != null)
        {
            setAnim(target, attack);
            attackAnimPlaying = true;
        }
    }

    /** The winner's mon hops in place for a moment before the scene ends. */
    public void celebrate()
    {
        if (active && mine != null)
        {
            celebrateUntil = System.currentTimeMillis() + CELEBRATE_MS;
            celebrateBaseZ = mine.getZ();
        }
    }

    public boolean isCelebrating()
    {
        return active && System.currentTimeMillis() < celebrateUntil;
    }

    /** Kicks off one lunge; both models slam toward each other and bounce back. */
    public void clash()
    {
        clash(-1, -1);
    }

    /**
     * Lunge plus hitsplat values (-1 = no splat, 0 = miss/blocked). Callers
     * settle battles right after this, so it swallows its own failures rather
     * than unwinding a turn that has already been decided.
     */
    public void clash(int damageOnMine, int damageOnTheirs)
    {
        if (!active)
        {
            return;
        }
        try
        {
            clashStart = System.currentTimeMillis();
            hitOnMine = damageOnMine;
            hitOnTheirs = damageOnTheirs;
            applyAttack(mine, mineSpecies);
            applyAttack(theirs, theirsSpecies);
        }
        catch (Exception e)
        {
            // Purely visual; the turn stands regardless
        }
    }

    public boolean isActive()
    {
        return active;
    }

    public WorldPoint getMineWp()
    {
        return mineWp;
    }

    public WorldPoint getTheirsWp()
    {
        return theirsWp;
    }

    public long getClashStart()
    {
        return clashStart;
    }

    public long getMineSwitchStart()
    {
        return mineSwitchStart;
    }

    public long getTheirsSwitchStart()
    {
        return theirsSwitchStart;
    }

    public int getHitOnMine()
    {
        return hitOnMine;
    }

    public int getHitOnTheirs()
    {
        return hitOnTheirs;
    }

    /** Client tick (~50/s): faint sink-outs first, then the lunge. Client thread only. */
    public void onClientTick()
    {
        if (!active)
        {
            return;
        }
        long now = System.currentTimeMillis();
        boolean dying = false;
        if (mineSwapAt != 0 && mine != null)
        {
            dying = true;
            if (now >= mineSwapAt)
            {
                mine = respawn(mine, pendingMineSpecies, mineWp, FACE_EAST);
                mineSpecies = pendingMineSpecies;
                mineSwitchStart = now;
                mineSwapAt = 0;
            }
            else
            {
                double t = 1 - (mineSwapAt - now) / (double) DEATH_MS;
                mine.setZ(mineDeathZ + (int) (t * DEATH_SINK));
                mine.setOrientation((mine.getOrientation() + 40) & 2047);
            }
        }
        if (theirsSwapAt != 0 && theirs != null)
        {
            dying = true;
            if (now >= theirsSwapAt)
            {
                theirs = respawn(theirs, pendingTheirsSpecies, theirsWp, FACE_WEST);
                theirsSpecies = pendingTheirsSpecies;
                theirsSwitchStart = now;
                theirsSwapAt = 0;
            }
            else
            {
                double t = 1 - (theirsSwapAt - now) / (double) DEATH_MS;
                theirs.setZ(theirsDeathZ + (int) (t * DEATH_SINK));
                theirs.setOrientation((theirs.getOrientation() + 40) & 2047);
            }
        }
        if (isCelebrating() && mine != null)
        {
            // Three little victory hops
            double t = 1 - (celebrateUntil - now) / (double) CELEBRATE_MS;
            int hop = (int) (Math.abs(Math.sin(t * Math.PI * 3)) * 40);
            mine.setZ(celebrateBaseZ - hop);
        }
        if (dying || clashStart == 0 || mine == null || theirs == null || mineWp == null)
        {
            return;
        }
        LocalPoint mineBase = com.osrsgo.spawn.Coords.toLocal(client,mineWp);
        LocalPoint theirsBase = com.osrsgo.spawn.Coords.toLocal(client,theirsWp);
        if (mineBase == null || theirsBase == null)
        {
            return;
        }
        double t = (System.currentTimeMillis() - clashStart) / (double) CLASH_MS;
        if (t >= 1)
        {
            mine.setLocation(mineBase, mineWp.getPlane());
            theirs.setLocation(theirsBase, theirsWp.getPlane());
            clashStart = 0;
            if (attackAnimPlaying)
            {
                // Swing's over: settle back into the learned idles
                applyIdle(mine, mineSpecies);
                applyIdle(theirs, theirsSpecies);
                attackAnimPlaying = false;
            }
            return;
        }
        // Peak at the midpoint of the swing: 0.4 of the gap each, so they meet
        double swing = Math.sin(Math.PI * t) * 0.4;
        int dx = theirsBase.getX() - mineBase.getX();
        int dy = theirsBase.getY() - mineBase.getY();
        mine.setLocation(new LocalPoint(
            mineBase.getX() + (int) (dx * swing), mineBase.getY() + (int) (dy * swing)), mineWp.getPlane());
        theirs.setLocation(new LocalPoint(
            theirsBase.getX() - (int) (dx * swing), theirsBase.getY() - (int) (dy * swing)), theirsWp.getPlane());
    }

    /** After a loading screen the objects are gone; rebuild on next ensure. */
    public void invalidate()
    {
        mine = null;
        theirs = null;
        mineSpecies = -1;
        theirsSpecies = -1;
        mineSwapAt = 0;
        theirsSwapAt = 0;
    }

    /** Client thread only. */
    public void stop()
    {
        if (!active)
        {
            return;
        }
        if (mine != null)
        {
            mine.setActive(false);
        }
        if (theirs != null)
        {
            theirs.setActive(false);
        }
        mine = null;
        theirs = null;
        mineSpecies = -1;
        theirsSpecies = -1;
        mineWp = null;
        theirsWp = null;
        clashStart = 0;
        mineSwapAt = 0;
        theirsSwapAt = 0;
        celebrateUntil = 0;
        active = false;
    }
}
