package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class TradeOfferMsg extends PartyMemberMessage
{
    long targetId;
    String tradeId;
    String offererName;
    String monJson;
}
