package com.osrsgo.battle;

import java.util.List;

/**
 * Snapshot of the battle after a resolved turn, sent host to guest as JSON.
 * Winner: 0 = ongoing, 1 = host, 2 = guest.
 */
public class TurnState
{
    public int hostActive;
    public int guestActive;
    public int[] hostHp;
    public int[] guestHp;
    public List<String> log;
    public int winner;
}
