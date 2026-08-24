package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BattleChallengeMsg extends PartyMemberMessage
{
    long targetId;
    String battleId;
    String challengerName;
    String teamJson;
}
