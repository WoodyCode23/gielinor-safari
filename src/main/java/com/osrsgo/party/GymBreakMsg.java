package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * A held gym reverting to unclaimed, broadcast to the party. Sent when the
 * challenger beats the defenders but loses to the NPC leader, so the holder's
 * client releases the gym too instead of the two clients disagreeing on who
 * holds it until the next restart.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GymBreakMsg extends PartyMemberMessage
{
    String gymId;
}
