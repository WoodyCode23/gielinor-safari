package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class RaidJoinMsg extends PartyMemberMessage
{
    String raidKey;
    String joinerName;
    String monSpecJson;
}
