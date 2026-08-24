package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class RaidStartMsg extends PartyMemberMessage
{
    String raidKey;
    String gymId;
    int bossSpeciesId;
    int bossLevel;
    String hostName;
    String hostMonJson;
}
