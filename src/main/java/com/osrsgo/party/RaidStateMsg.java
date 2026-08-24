package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class RaidStateMsg extends PartyMemberMessage
{
    String raidKey;
    String stateJson;
}
