package com.osrsgo.party;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * One side's current trade-window state: their full offer and whether they
 * have accepted. Any offer change implicitly withdraws both acceptances.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class TradeUpdateMsg extends PartyMemberMessage
{
    String tradeId;
    String offerJson;
    boolean accepted;
}
