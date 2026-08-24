package com.osrsgo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("osrsgo")
public interface OsrsGoConfig extends Config
{
    @ConfigItem(
        keyName = "showSpawns",
        name = "Show wild spawns",
        description = "Draw wild mon spawns in the scene and on the minimap",
        position = 1
    )
    default boolean showSpawns()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showGyms",
        name = "Show gym marks",
        description = "Draw gym marks in the scene, on the minimap, and on the world map",
        position = 2
    )
    default boolean showGyms()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyRare",
        name = "Notify on Epic/Legendary",
        description = "Desktop notification when an Epic or Legendary mon spawns nearby",
        position = 4
    )
    default boolean notifyRare()
    {
        return true;
    }

    @ConfigItem(
        keyName = "shinyDing",
        name = "Shiny ding",
        description = "Play a bell ding (and chat line) when a shiny sparkles into range",
        position = 5
    )
    default boolean shinyDing()
    {
        return true;
    }

    @ConfigItem(
        keyName = "eggDing",
        name = "Egg hatch ding",
        description = "Play a bell dong when an egg hatches",
        position = 6
    )
    default boolean eggDing()
    {
        return true;
    }

    @ConfigItem(
        keyName = "bossCatch",
        name = "Boss-kill catch throws",
        description = "Killing a real boss that lives in the GielDex auto-throws a Gielinor Ball at its corpse."
            + " Odds scale with your kill pace: slower bosses pay better per kill.",
        position = 7
    )
    default boolean bossCatch()
    {
        return true;
    }

    @ConfigItem(
        keyName = "idleMotion",
        name = "Spawn idle motion",
        description = "Wild mons gently bob in place so they read as alive",
        position = 10
    )
    default boolean idleMotion()
    {
        return true;
    }

    @ConfigItem(
        keyName = "bossMasterBall",
        name = "Master Ball boss kills",
        description = "Spend a Master Ball on boss kills for a GUARANTEED catch (off = never spend Masters automatically)",
        position = 8
    )
    default boolean bossMasterBall()
    {
        return false;
    }

    @ConfigItem(
        keyName = "spawnModels",
        name = "3D models on spawns",
        description = "Render the actual NPC model standing on each spawn tile (experimental)",
        position = 5
    )
    default boolean spawnModels()
    {
        return true;
    }

    @ConfigItem(
        keyName = "gymControl",
        name = "Gym control",
        description = "Claim and hold gyms against rival trainers, and against party members when you are in a party.",
        position = 5
    )
    default boolean gymControl()
    {
        return true;
    }

    @ConfigItem(
        keyName = "chatFinds",
        name = "Chat: travel finds",
        description = "Chat messages for Gielinor Balls and berries found while walking",
        position = 8
    )
    default boolean chatFinds()
    {
        return true;
    }

    @ConfigItem(
        keyName = "chatMilestones",
        name = "Chat: milestones",
        description = "Chat messages for catches, hatches, medals, research, gyms, raids, and trades",
        position = 9
    )
    default boolean chatMilestones()
    {
        return true;
    }

}
