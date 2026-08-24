package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BattleTurnResultMsg extends PartyMemberMessage
{
    String battleId;
    int turn;
    String stateJson;
}
