package com.osrsgo.battle;

import com.osrsgo.data.MoveData;
import com.osrsgo.data.SpeciesData;
import com.osrsgo.model.Move;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Shared-HP party raid. The host owns the truth: it collects one move per
 * alive participant per turn, resolves everyone's hits on the boss, lets the
 * boss maul one random participant, and broadcasts a state snapshot. Guests
 * mirror snapshots. Each participant fights with a single mon; fainting
 * benches you for the rest of the raid.
 */
public class CoopRaid
{
    public static class Participant
    {
        public final long memberId;
        public final String name;
        public final BattleMon mon;
        public Integer pendingMove;
        public boolean alive = true;

        public Participant(long memberId, String name, BattleMon mon)
        {
            this.memberId = memberId;
            this.name = name;
            this.mon = mon;
        }
    }

    /** Wire-format snapshot the host broadcasts after each turn. */
    public static class State
    {
        public int turn;
        public int bossHp;
        public int winner;
        public List<String> names;
        public int[] hps;
        public List<String> log;
    }

    public final String raidKey;
    public final String gymId;
    public final int bossSpeciesId;
    public final int bossLevel;
    public final boolean host;
    public final long myMemberId;
    public final BattleMon boss;
    public final Map<Long, Participant> participants = new LinkedHashMap<>();
    public boolean started;
    public boolean finished;
    public boolean wonByUs;
    public int turn = 1;
    public int ticksThisTurn;
    public final List<String> fullLog = new ArrayList<>();

    private final Random rng = new Random();

    public CoopRaid(String raidKey, String gymId, int bossSpeciesId, int bossLevel, boolean host, long myMemberId)
    {
        this.raidKey = raidKey;
        this.gymId = gymId;
        this.bossSpeciesId = bossSpeciesId;
        this.bossLevel = bossLevel;
        this.host = host;
        this.myMemberId = myMemberId;
        this.boss = BattleMon.fromSpecies(bossSpeciesId, bossLevel);
    }

    public Participant me()
    {
        return participants.get(myMemberId);
    }

    /** Host: scales boss HP by the party size when the raid begins. */
    public void begin()
    {
        int scaled = boss.maxHp * Math.max(1, participants.size());
        boss.hp = scaled;
        started = true;
        fullLog.add("The raid begins! " + boss.name + " looms with " + scaled + " HP.");
    }

    /** Host: total scaled HP for rendering (max of the bar). */
    public int bossMaxHp()
    {
        return boss.maxHp * Math.max(1, Math.max(participants.size(), 1));
    }

    public boolean allMovesIn()
    {
        for (Participant p : participants.values())
        {
            if (p.alive && p.pendingMove == null)
            {
                return false;
            }
        }
        return true;
    }

    /** Host: resolves one turn; missing moves default to the first move. */
    public List<String> resolveTurn()
    {
        List<String> log = new ArrayList<>();
        for (Participant p : participants.values())
        {
            if (!p.alive || boss.hp <= 0)
            {
                continue;
            }
            int moveIdx = p.pendingMove != null ? p.pendingMove : 0;
            Move move = MoveData.byId(p.mon.moveIds.get(Math.max(0, Math.min(moveIdx, p.mon.moveIds.size() - 1))));
            if (move.isGuard())
            {
                p.mon.guarding = true;
                log.add(p.name + "'s " + p.mon.name + " braces.");
            }
            else if (rng.nextDouble() > move.getAccuracy())
            {
                log.add(p.name + "'s " + move.getName() + " missed!");
            }
            else
            {
                int dmg = BattleEngine.damage(p.mon, move, boss, rng);
                boss.hp = Math.max(0, boss.hp - dmg);
                log.add(p.name + "'s " + p.mon.name + " hits " + dmg + "!");
            }
            p.pendingMove = null;
        }

        if (boss.hp <= 0)
        {
            finished = true;
            wonByUs = true;
            log.add(boss.name + " goes down! Raid cleared!");
        }
        else
        {
            List<Participant> alive = new ArrayList<>();
            for (Participant p : participants.values())
            {
                if (p.alive)
                {
                    alive.add(p);
                }
            }
            if (!alive.isEmpty())
            {
                Participant target = alive.get(rng.nextInt(alive.size()));
                Move bossMove = MoveData.byId(boss.moveIds.get(rng.nextInt(boss.moveIds.size() - 1)));
                if (rng.nextDouble() <= bossMove.getAccuracy())
                {
                    int dmg = BattleEngine.damage(boss, bossMove, target.mon, rng);
                    target.mon.hp = Math.max(0, target.mon.hp - dmg);
                    log.add(boss.name + " savages " + target.name + "'s " + target.mon.name + " for " + dmg + "!");
                    if (target.mon.hp <= 0)
                    {
                        target.alive = false;
                        log.add(target.name + "'s " + target.mon.name + " fainted!");
                    }
                }
                else
                {
                    log.add(boss.name + "'s attack misses " + target.name + "!");
                }
            }
            boolean anyAlive = false;
            for (Participant p : participants.values())
            {
                anyAlive |= p.alive;
            }
            if (!anyAlive)
            {
                finished = true;
                wonByUs = false;
                log.add("The party has fallen. " + boss.name + " stands victorious.");
            }
        }
        for (Participant p : participants.values())
        {
            p.mon.guarding = false;
        }
        turn++;
        ticksThisTurn = 0;
        fullLog.addAll(log);
        return log;
    }

    /** Host: builds the snapshot for broadcast. */
    public State snapshot(List<String> turnLog)
    {
        State s = new State();
        s.turn = turn;
        s.bossHp = boss.hp;
        s.winner = finished ? (wonByUs ? 1 : 2) : 0;
        s.names = new ArrayList<>();
        s.hps = new int[participants.size()];
        int i = 0;
        for (Participant p : participants.values())
        {
            s.names.add(p.name);
            s.hps[i++] = p.mon.hp;
        }
        s.log = new ArrayList<>(turnLog);
        return s;
    }

    /** Names visible to guests before the raid begins (from lobby roster syncs). */
    public List<String> lobbyNames = new ArrayList<>();

    /** Guest: mirrors a host snapshot by participant name. Turn 0 = lobby roster only. */
    public List<String> applySnapshot(State s)
    {
        if (s.turn == 0)
        {
            if (s.names != null)
            {
                lobbyNames = new ArrayList<>(s.names);
            }
            return new ArrayList<>();
        }
        started = true;
        boss.hp = s.bossHp;
        turn = s.turn;
        if (s.names != null && s.hps != null)
        {
            int i = 0;
            for (Participant p : participants.values())
            {
                int idx = s.names.indexOf(p.name);
                if (idx >= 0 && idx < s.hps.length)
                {
                    p.mon.hp = s.hps[idx];
                    p.alive = p.mon.hp > 0;
                }
                i++;
            }
        }
        if (s.winner != 0)
        {
            finished = true;
            wonByUs = s.winner == 1;
        }
        Participant mine = me();
        if (mine != null)
        {
            mine.pendingMove = null;
        }
        List<String> log = s.log != null ? s.log : new ArrayList<>();
        fullLog.addAll(log);
        return log;
    }

    public String bossName()
    {
        return SpeciesData.byId(bossSpeciesId).getName();
    }

    public String fingerprint()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(raidKey).append(':').append(turn).append(':').append(boss.hp).append(':')
            .append(started).append(':').append(finished).append(':').append(participants.size());
        Participant mine = me();
        sb.append(':').append(mine != null ? mine.pendingMove : null)
            .append(':').append(fullLog.size()).append(':').append(lobbyNames.size());
        return sb.toString();
    }
}
