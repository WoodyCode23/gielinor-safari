package com.osrsgo.battle;

import com.osrsgo.data.MoveData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * State for one active battle, from either participant's perspective.
 * AI and PVP_HOST run the engine; PVP_GUEST only mirrors TurnState snapshots
 * broadcast by the host.
 */
public class BattleSession
{
    public enum Mode
    {
        AI,
        PVP_HOST,
        PVP_GUEST
    }

    public final Mode mode;
    public final String battleId;
    public final String opponentName;
    public final long opponentMemberId;
    public final String gymId;
    public final List<BattleMon> myTeam;
    public final List<BattleMon> oppTeam;
    public int myActive;
    public int oppActive;
    public int turn = 1;
    public boolean finished;
    public boolean wonByMe;
    public final List<String> fullLog = new ArrayList<>();

    public Integer myPendingMove;
    public Integer oppPendingMove;

    // Gym-control context: set when this AI battle is against another
    // trainer's defender team rather than the NPC leader.
    public String gymHolderRsn;
    public String gymExpectedUpdatedAt;
    // When battling a claimed gym, the enemy list is the holder's defender
    // team (indices 0..gymHolderMonCount-1) followed by the NPC leader's team.
    public int gymHolderMonCount;
    // Set when this AI battle is a raid boss fight
    public Integer raidSpeciesId;
    // Which of my team slots scored each enemy KO, for bonus xp
    public final List<Integer> koScorers = new ArrayList<>();

    private final Random rng = new Random();

    public BattleSession(Mode mode, String battleId, String opponentName, long opponentMemberId,
        String gymId, List<BattleMon> myTeam, List<BattleMon> oppTeam)
    {
        this.mode = mode;
        this.battleId = battleId;
        this.opponentName = opponentName;
        this.opponentMemberId = opponentMemberId;
        this.gymId = gymId;
        this.myTeam = myTeam;
        this.oppTeam = oppTeam;
    }

    public BattleMon myMon()
    {
        return myTeam.get(myActive);
    }

    public BattleMon oppMon()
    {
        return oppTeam.get(oppActive);
    }

    /** Whose mon is currently facing you: the gym holder during their phase, else the named opponent. */
    public String currentOpponentName()
    {
        return gymHolderRsn != null && oppActive < gymHolderMonCount ? gymHolderRsn : opponentName;
    }

    /** True once every mon of the holder's defender team has fainted. */
    public boolean holderTeamDefeated()
    {
        if (gymHolderMonCount <= 0)
        {
            return false;
        }
        for (int i = 0; i < gymHolderMonCount && i < oppTeam.size(); i++)
        {
            if (!oppTeam.get(i).fainted())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves a turn locally (AI and PVP_HOST modes). Move indices are into
     * each active mon's moveset. Returns the log lines for this turn.
     */
    public List<String> resolveTurn(int myMoveIdx, int oppMoveIdx)
    {
        BattleMon mine = myMon();
        BattleMon theirs = oppMon();
        com.osrsgo.model.Move myMove = MoveData.byId(mine.moveIds.get(clamp(myMoveIdx, mine.moveIds.size())));
        com.osrsgo.model.Move oppMove = MoveData.byId(theirs.moveIds.get(clamp(oppMoveIdx, theirs.moveIds.size())));

        boolean iGoFirst = mine.spd > theirs.spd || (mine.spd == theirs.spd && rng.nextBoolean());
        List<String> log = iGoFirst
            ? BattleEngine.resolveTurn(mine, myMove, theirs, oppMove, rng)
            : BattleEngine.resolveTurn(theirs, oppMove, mine, myMove, rng);

        handleFaints(log);
        turn++;
        myPendingMove = null;
        oppPendingMove = null;
        fullLog.addAll(log);
        return log;
    }

    private void handleFaints(List<String> log)
    {
        if (myMon().fainted())
        {
            int next = nextAlive(myTeam);
            if (next < 0)
            {
                finished = true;
                wonByMe = false;
                log.add("You are out of mons. Defeat!");
            }
            else
            {
                myActive = next;
                log.add("You send out " + myMon().name + "!");
            }
        }
        if (!finished && oppMon().fainted())
        {
            koScorers.add(myActive);
            boolean wasHolderPhase = gymHolderRsn != null && oppActive < gymHolderMonCount;
            int next = nextAlive(oppTeam);
            if (next < 0)
            {
                finished = true;
                wonByMe = true;
                log.add("Victory! " + opponentName + " is out of mons.");
            }
            else
            {
                oppActive = next;
                if (wasHolderPhase && next >= gymHolderMonCount)
                {
                    log.add(gymHolderRsn + "'s defenders are down! " + opponentName
                        + " steps up to defend the gym itself!");
                }
                log.add(currentOpponentName() + " sends out " + oppMon().name + "!");
            }
        }
    }

    private static int nextAlive(List<BattleMon> team)
    {
        for (int i = 0; i < team.size(); i++)
        {
            if (!team.get(i).fainted())
            {
                return i;
            }
        }
        return -1;
    }

    public int aiMove()
    {
        return BattleEngine.pickAiMove(oppMon(), myMon(), rng);
    }

    /** Host-side: package the post-turn state for the guest. Host is "me" here. */
    public TurnState snapshot(List<String> turnLog)
    {
        TurnState s = new TurnState();
        s.hostActive = myActive;
        s.guestActive = oppActive;
        s.hostHp = hpArray(myTeam);
        s.guestHp = hpArray(oppTeam);
        s.log = new ArrayList<>(turnLog);
        s.winner = finished ? (wonByMe ? 1 : 2) : 0;
        return s;
    }

    /** Guest-side: mirror a host TurnState. Guest's "my" team is the host's guest team. */
    public List<String> applySnapshot(TurnState s)
    {
        // Derive hitsplat values from the hp deltas of the previously active mons
        int prevMyHp = myMon().hp;
        int prevOppHp = oppMon().hp;
        BattleMon prevMine = myMon();
        BattleMon prevTheirs = oppMon();
        myActive = s.guestActive;
        oppActive = s.hostActive;
        applyHp(myTeam, s.guestHp);
        applyHp(oppTeam, s.hostHp);
        prevMine.lastHitTaken = Math.max(0, prevMyHp - prevMine.hp);
        prevTheirs.lastHitTaken = Math.max(0, prevOppHp - prevTheirs.hp);
        if (s.winner != 0)
        {
            finished = true;
            wonByMe = s.winner == 2;
        }
        turn++;
        myPendingMove = null;
        List<String> log = s.log != null ? s.log : new ArrayList<>();
        fullLog.addAll(log);
        return log;
    }

    private static int[] hpArray(List<BattleMon> team)
    {
        int[] hp = new int[team.size()];
        for (int i = 0; i < team.size(); i++)
        {
            hp[i] = team.get(i).hp;
        }
        return hp;
    }

    private static void applyHp(List<BattleMon> team, int[] hp)
    {
        if (hp == null)
        {
            return;
        }
        for (int i = 0; i < team.size() && i < hp.length; i++)
        {
            team.get(i).hp = hp[i];
        }
    }

    private static int clamp(int idx, int size)
    {
        return Math.max(0, Math.min(idx, size - 1));
    }
}
