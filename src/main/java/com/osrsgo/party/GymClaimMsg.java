package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * One player claiming a gym, broadcast to the party. Receivers write it into
 * their own local map, so party members contest the same ten gyms without any
 * host or authority.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GymClaimMsg extends PartyMemberMessage
{
    String gymId;
    String holderRsn;
    String teamJson;
    String faction;
}
