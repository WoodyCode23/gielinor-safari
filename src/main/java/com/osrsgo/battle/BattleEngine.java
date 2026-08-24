package com.osrsgo.battle;

import com.osrsgo.data.MoveData;
import com.osrsgo.model.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolves one battle turn. Pure logic: no client, no Swing, no party. The
 * same engine runs gym (AI) battles locally and PvP battles on the host.
 */
public final class BattleEngine
{
    /** Resolves a full turn between two active mons. Appends to and returns the log. */
    public static List<String> resolveTurn(BattleMon first, Move firstMove, BattleMon second, Move secondMove, Random rng)
    {
        List<String> log = new ArrayList<>();
        first.lastHitTaken = -1;
        second.lastHitTaken = -1;
        first.guarding = firstMove.isGuard();
        second.guarding = secondMove.isGuard();

        act(first, firstMove, second, rng, log);
        if (!second.fainted())
        {
            act(second, secondMove, first, rng, log);
        }
        first.guarding = false;
        second.guarding = false;
        return log;
    }

    private static void act(BattleMon attacker, Move move, BattleMon defender, Random rng, List<String> log)
    {
        if (attacker.fainted())
        {
            return;
        }
        if (move.isGuard())
        {
            // Guarding breaks the repetition streak, like any move change
            attacker.lastMoveId = move.getId();
            attacker.repeatCount = 0;
            log.add(attacker.name + " braces behind its guard.");
            return;
        }
        if (move.getId() == attacker.lastMoveId)
        {
            attacker.repeatCount++;
        }
        else
        {
            attacker.repeatCount = 0;
        }
        attacker.lastMoveId = move.getId();
        if (rng.nextDouble() > move.getAccuracy())
        {
            log.add(attacker.name + "'s " + move.getName() + " missed!");
            defender.lastHitTaken = 0;
            return;
        }
        int damage = damage(attacker, move, defender, rng);
        defender.hp = Math.max(0, defender.hp - damage);
        defender.lastHitTaken = damage;
        double mult = move.getType().multiplierAgainst(defender.type);
        String effect = mult > 1.0 ? " A devastating blow!" : (mult < 1.0 ? " It barely leaves a mark." : "");
        String fatigue = attacker.repeatCount > 0
            ? " Fatigue sets in (-" + Math.round((1 - fatigueFactor(attacker.repeatCount)) * 100) + "%)."
            : "";
        log.add(attacker.name + " used " + move.getName() + "! " + damage + " damage." + effect + fatigue);
        if (defender.fainted())
        {
            log.add(defender.name + " fainted!");
        }
    }

    /** Damage multiplier for the Nth consecutive use of the same move: 100/70/50/40% floor. */
    static double fatigueFactor(int repeatCount)
    {
        return Math.max(0.4, 1.0 - 0.3 * repeatCount);
    }

    static int damage(BattleMon attacker, Move move, BattleMon defender, Random rng)
    {
        double base = ((2.0 * attacker.level / 5.0 + 2.0) * move.getPower() * attacker.atk / Math.max(1, defender.def)) / 50.0 + 2.0;
        double mult = move.getType().multiplierAgainst(defender.type);
        double roll = 0.85 + rng.nextDouble() * 0.15;
        double dmg = base * mult * roll * fatigueFactor(attacker.repeatCount);
        if (defender.guarding)
        {
            dmg /= 2.0;
        }
        return Math.max(1, (int) Math.round(dmg));
    }

    /** Highest expected damage move (fatigue-aware), with a 20% chance to pick randomly instead. */
    public static int pickAiMove(BattleMon self, BattleMon target, Random rng)
    {
        List<Integer> ids = self.moveIds;
        if (rng.nextDouble() < 0.20)
        {
            return rng.nextInt(ids.size());
        }
        int bestIdx = 0;
        double bestScore = -1;
        for (int i = 0; i < ids.size(); i++)
        {
            Move m = MoveData.byId(ids.get(i));
            double score;
            if (m.isGuard())
            {
                score = 5;
            }
            else
            {
                score = m.getPower() * m.getAccuracy() * m.getType().multiplierAgainst(target.type);
                if (m.getId() == self.lastMoveId)
                {
                    score *= fatigueFactor(self.repeatCount + 1);
                }
            }
            if (score > bestScore)
            {
                bestScore = score;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private BattleEngine()
    {
    }
}
