package com.osrsgo;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsgo.battle.BattleMon;
import com.osrsgo.battle.BattleSession;
import com.osrsgo.battle.MonSpec;
import com.osrsgo.battle.TurnState;
import com.osrsgo.data.GymData;
import com.osrsgo.gym.GymHolder;
import com.osrsgo.model.OwnedMon;
import com.osrsgo.model.Rarity;
import com.osrsgo.overlay.GoOverlay;
import com.osrsgo.party.BattleAcceptMsg;
import com.osrsgo.party.BattleChallengeMsg;
import com.osrsgo.party.BattleDeclineMsg;
import com.osrsgo.party.BattleEndMsg;
import com.osrsgo.party.BattleMoveMsg;
import com.osrsgo.party.BattleTurnResultMsg;
import com.osrsgo.spawn.CatchSequence;
import com.osrsgo.spawn.SpawnManager;
import com.osrsgo.spawn.WildSpawn;
import com.osrsgo.ui.Icons;
import com.osrsgo.storage.PlayerProfile;
import com.osrsgo.storage.ProfileStore;
import com.osrsgo.ui.OsrsGoPanel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.Notifier;

@Slf4j
@PluginDescriptor(
    name = "Gielinor Safari",
    description = "A wild-mon safari across Gielinor: catch them, beat gyms, battle friends over Party",
    tags = {"osrsgo", "pokemon", "catch", "battle", "party", "gym"}
)
public class OsrsGoPlugin extends Plugin
{
    private static final int NEARBY_RANGE = 32;
    private static final int GYM_RANGE = 10;
    private static final int PANEL_REFRESH_TICKS = 2;
    private static final int TILES_PER_BALL = 60;
    private static final int MAX_BALLS = 50;
    private static final int TILES_PER_BUDDY_XP = 10;
    private static final int BUDDY_XP_PER_GRANT = 2;
    private static final int MAX_STEP = 6;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private Notifier notifier;

    @Inject
    private PartyService partyService;

    @Inject
    private WSClient wsClient;

    @Inject
    private Gson gson;

    @Inject
    private OsrsGoConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private GoOverlay overlay;

    @Inject
    private SpawnManager spawnManager;

    @Inject
    private ProfileStore profileStore;

    @Inject
    private java.util.concurrent.ScheduledExecutorService executor;

    @Inject
    private com.osrsgo.ui.NpcImages npcImages;

    @Inject
    private com.osrsgo.overlay.SpawnModels spawnModels;

    @Inject
    private com.osrsgo.overlay.BattleScene battleScene;

    @Inject
    private com.osrsgo.anim.AnimationLibrary animLibrary;

    @Inject
    private com.osrsgo.harvest.NpcHarvest npcHarvest;

    // Species name (lowercase) -> speciesId, for learning animations from any
    // live NPC variant that shares the name
    private static final java.util.Map<String, Integer> SPECIES_BY_NAME = new java.util.HashMap<>();

    static
    {
        for (com.osrsgo.model.Species sp : com.osrsgo.data.SpeciesData.all())
        {
            SPECIES_BY_NAME.put(sp.getName().toLowerCase(), sp.getId());
        }
    }

    private PlayerProfile profile = new PlayerProfile();
    private OsrsGoPanel panel;
    private NavigationButton navButton;
    private final List<WorldMapPoint> gymMapPoints = new ArrayList<>();
    private final Random rng = new Random();
    private int tickCounter;

    private volatile WorldPoint playerLocation;
    private volatile List<WildSpawn> nearbySpawns = Collections.emptyList();
    private volatile List<SpawnManager.Candy> nearbyCandies = Collections.emptyList();
    // Per-spawn alert dedup: each key dings/notifies once, ever
    private final java.util.Set<String> shinyDinged = new java.util.HashSet<>();
    private final java.util.Set<String> rareNotified = new java.util.HashSet<>();
    private static final long TRIBUTE_MILLIS = 15 * 60 * 1000L;
    private long tributeBucket = -1;
    private volatile String myRsn;
    private int gymPollCounter;
    // ~15s: gym flips should feel near-live during a war
    private static final int GYM_POLL_TICKS = 25;
    // A real claim can never exceed the game's team cap; anything larger is a
    // malformed or hostile peer, not a legitimate claim
    private static final int MAX_CLAIM_TEAM = 5;
    // Accepted party breaks, per sender, so a peer cannot spam them
    private static final long BREAK_COOLDOWN_MS = 60_000L;
    private final java.util.Map<Long, Long> lastBreakFromMember = new java.util.HashMap<>();
    // Rival pressure is a per-rotation roll, but pollGyms runs every ~15s.
    // Without this gate the roll fires ~40 times a bucket and a 5% chance
    // becomes ~87%, which would make holding gyms nearly impossible.
    private long lastRivalRollBucket = -1;

    private BattleSession session;
    private List<OwnedMon> battleTeamRefs = Collections.emptyList();
    private volatile CatchSequence activeCatch;

    // Incoming challenge, waiting on accept/decline
    private String pendingBattleId;
    private long pendingChallengerId;
    private String pendingChallengerName;
    private String pendingChallengerTeamJson;

    // Outgoing challenge, waiting on the other side
    private String outgoingBattleId;
    private long outgoingTargetId;

    @Provides
    OsrsGoConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OsrsGoConfig.class);
    }

    @Override
    protected void startUp()
    {
        // Gym control is local now: drop the stored host from the versions
        // that talked to a server, so no stale URL lingers in a user's config
        configManager.unsetConfiguration("osrsgo", "serverUrl");

        profile = profileStore.load();
        if (profileStore.wasTampered())
        {
            log.warn("Gielinor Safari: stored profile failed its integrity check and was reset");
        }
        log.info("Gielinor Safari started: {} species, {} gyms, {} mons in collection",
            com.osrsgo.data.SpeciesData.all().size(), GymData.all().size(), profile.mons.size());

        overlayManager.add(overlay);

        panel = new OsrsGoPanel(this);
        navButton = NavigationButton.builder()
            .tooltip("Gielinor Safari")
            .icon(Icons.pokeball())
            .priority(7)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);

        for (GymData.Gym gym : GymData.all())
        {
            WorldMapPoint point = new WorldMapPoint(gym.location, Icons.gymMark());
            point.setTooltip(gym.name);
            gymMapPoints.add(point);
            worldMapPointManager.add(point);
        }

        wsClient.registerMessage(BattleChallengeMsg.class);
        wsClient.registerMessage(BattleAcceptMsg.class);
        wsClient.registerMessage(BattleDeclineMsg.class);
        wsClient.registerMessage(BattleMoveMsg.class);
        wsClient.registerMessage(BattleTurnResultMsg.class);
        wsClient.registerMessage(BattleEndMsg.class);
        wsClient.registerMessage(com.osrsgo.party.TradeOfferMsg.class);
        wsClient.registerMessage(com.osrsgo.party.TradeUpdateMsg.class);
        wsClient.registerMessage(com.osrsgo.party.TradeCancelMsg.class);
        wsClient.registerMessage(com.osrsgo.party.GymClaimMsg.class);
        wsClient.registerMessage(com.osrsgo.party.GymBreakMsg.class);

        npcImages.setOnUpdate(this::refreshPanel);
        pollGyms();
        refreshPanel();
    }

    public com.osrsgo.ui.NpcImages getNpcImages()
    {
        return npcImages;
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        clientToolbar.removeNavigation(navButton);
        for (WorldMapPoint point : gymMapPoints)
        {
            worldMapPointManager.remove(point);
        }
        gymMapPoints.clear();

        wsClient.unregisterMessage(BattleChallengeMsg.class);
        wsClient.unregisterMessage(BattleAcceptMsg.class);
        wsClient.unregisterMessage(BattleDeclineMsg.class);
        wsClient.unregisterMessage(BattleMoveMsg.class);
        wsClient.unregisterMessage(BattleTurnResultMsg.class);
        wsClient.unregisterMessage(BattleEndMsg.class);
        wsClient.unregisterMessage(com.osrsgo.party.TradeOfferMsg.class);
        wsClient.unregisterMessage(com.osrsgo.party.TradeUpdateMsg.class);
        wsClient.unregisterMessage(com.osrsgo.party.TradeCancelMsg.class);
        wsClient.unregisterMessage(com.osrsgo.party.GymClaimMsg.class);
        wsClient.unregisterMessage(com.osrsgo.party.GymBreakMsg.class);

        clientThread.invoke(spawnModels::clear);
        animLibrary.saveIfDirty();
        npcHarvest.saveIfDirty();
        spawnManager.reset();
        session = null;
        profileStore.save(profile);
        profileDirty = false;
    }

    @Subscribe
    public void onClientTick(net.runelite.api.events.ClientTick tick)
    {
        battleScene.onClientTick();
        spawnModels.onClientTick(config.spawnModels() && config.idleMotion());
    }

    @Subscribe
    public void onAnimationChanged(net.runelite.api.events.AnimationChanged event)
    {
        if (!(event.getActor() instanceof net.runelite.api.NPC))
        {
            return;
        }
        net.runelite.api.NPC npc = (net.runelite.api.NPC) event.getActor();
        String npcName = npc.getName();
        if (npcName == null || npcName.isEmpty())
        {
            return;
        }
        String lower = npcName.toLowerCase();
        animLibrary.voteAttackByName(lower, npc.getAnimation());
        Integer sid = SPECIES_BY_NAME.get(lower);
        if (sid != null)
        {
            animLibrary.voteAttack(sid, npc.getAnimation());
        }
    }

    @Subscribe
    public void onGameStateChanged(net.runelite.api.events.GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING)
        {
            // Scene rebuild wipes RuneLiteObjects; recreate them next tick
            spawnModels.invalidate();
            battleScene.invalidate();
            pohPoolInScene = false;
        }
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            // Shared-trainer model: an alt (or another client) may have saved
            // since we loaded. Refresh on login when it's safe, and adopt the
            // loaded profile ONLY when it's provably newer AND not a husk: a
            // torn read (save race) yields a fresh/empty profile and must
            // never replace real data (the 2026-08-10 wipe).
            if (session == null && activeCatch == null && !profileDirty)
            {
                PlayerProfile candidate = profileStore.load();
                synchronized (this)
                {
                    boolean newer = candidate.saveCounter > profile.saveCounter;
                    boolean notShrunken = candidate.mons.size() >= profile.mons.size();
                    if (newer && notShrunken)
                    {
                        profile = candidate;
                    }
                }
                refreshPanel();
            }
        }
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            // StatChanged fires absolute values on login; re-baseline to avoid phantom deltas
            xpBaseline.clear();
        }
    }

    // ------------------------------------------------------------------ play-to-earn balls
    // OSRS TCG-style: real gameplay pays out too, so running isn't the only faucet

    private static final int XP_PER_BALL = 5000;
    private final java.util.Map<net.runelite.api.Skill, Integer> xpBaseline =
        new java.util.EnumMap<>(net.runelite.api.Skill.class);

    @Subscribe
    public synchronized void onStatChanged(net.runelite.api.events.StatChanged event)
    {
        Integer prev = xpBaseline.put(event.getSkill(), event.getXp());
        if (prev == null || event.getXp() <= prev)
        {
            return;
        }
        profile.xpBallProgress += event.getXp() - prev;
        int gained = 0;
        while (profile.xpBallProgress >= xpPerBall())
        {
            profile.xpBallProgress -= xpPerBall();
            if (profile.balls < maxBalls())
            {
                profile.balls++;
                profile.stats.ballsFound++;
                gained++;
            }
        }
        if (gained > 0)
        {
            chatFind("Gielinor Safari: your training earned " + gained + " Gielinor Ball"
                + (gained == 1 ? "" : "s") + "! (" + profile.balls + " in pouch)");
            saveSoon();
        }
    }

    @Subscribe
    public synchronized void onNpcLootReceived(net.runelite.client.events.NpcLootReceived event)
    {
        int combatLevel = event.getNpc().getCombatLevel();
        String name = event.getNpc().getName();
        // Boss throws first: some Epic bosses (Evil Chicken's low forms) sit
        // under the combat-40 ball floor and must not miss their roll
        tryBossKillThrow(event.getNpc());
        int killBonus = killBallBonus();
        if (combatLevel >= 250)
        {
            profile.greatBalls++;
            profile.balls = Math.min(maxBalls(), profile.balls + 2 + killBonus);
            profile.stats.ballsFound += 2 + killBonus;
            chatFind("Gielinor Safari: felling " + name + " (lvl " + combatLevel
                + ") earned a GREAT BALL and 2 Gielinor Balls!");
        }
        else if (combatLevel >= 100)
        {
            profile.balls = Math.min(maxBalls(), profile.balls + 3 + killBonus);
            profile.stats.ballsFound += 3 + killBonus;
            chatFind("Gielinor Safari: slaying " + name + " earned 3 Gielinor Balls!");
        }
        else if (combatLevel >= 40)
        {
            if (profile.balls < maxBalls())
            {
                profile.balls++;
                profile.stats.ballsFound++;
                chatFind("Gielinor Safari: slaying " + name + " earned a Gielinor Ball!");
            }
        }
        else
        {
            return;
        }
        saveSoon();
    }

    // ------------------------------------------------------------------ boss-kill catch throws

    private static final String BOSS_CATCH_KEY = "boss-catch";
    // KPH balancing: odds grow with time since YOUR last kill of that boss,
    // so speed-farmed bosses pay less per kill and slow ones pay more
    private final java.util.Map<Integer, Long> lastBossKillMs = new java.util.HashMap<>();
    // One kill can surface as both ActorDeath and NpcLootReceived; only the
    // first throw within the window counts
    private final java.util.Map<Integer, Long> lastBossThrowMs = new java.util.HashMap<>();

    /** Why a kill did or did not earn a throw. Log only, never shown in game. */
    private void bossLog(String name, String verdict)
    {
        log.debug("boss throw | {}: {}", name, verdict);
    }

    /**
     * Felling a real boss that exists in the GielDex hurls a Gielinor Ball at
     * the corpse, full animation included. Epic/Legendary species only. Odds
     * scale with kill pace; the Master Ball toggle guarantees it instead.
     */
    /**
     * Corpse-transforming bosses (the Maggot King) never die: the boss NPC
     * becomes a corpse NPC, loot goes straight to the inventory, and neither
     * ActorDeath nor a loot event ever fires. A freshly appearing "X Corpse"
     * near the player IS the kill signal (the arena is a solo instance), and
     * the corpse-suffix strip in the matcher maps it back to the boss.
     */
    @Subscribe
    public synchronized void onNpcSpawned(net.runelite.api.events.NpcSpawned event)
    {
        tryCorpseBossThrow(event.getNpc());
    }

    @Subscribe
    public synchronized void onNpcChanged(net.runelite.api.events.NpcChanged event)
    {
        tryCorpseBossThrow(event.getNpc());
    }

    private void tryCorpseBossThrow(net.runelite.api.NPC npc)
    {
        String name = npc.getName();
        if (name == null || !name.toLowerCase().endsWith(" corpse") || playerLocation == null)
        {
            return;
        }
        WorldPoint where = npc.getWorldLocation();
        net.runelite.api.coords.LocalPoint local = npc.getLocalLocation();
        if (local != null && client.isInInstancedRegion())
        {
            where = WorldPoint.fromLocalInstance(client, local);
        }
        if (where == null || playerLocation.distanceTo(where) > 30)
        {
            return;
        }
        tryBossKillThrow(npc);
    }

    /**
     * Chest-paid bosses (Xarpus and every other raid room) never fire a loot
     * event, so a boss you were personally fighting also rolls its throw the
     * moment it dies. The loot path stays for kills where the death slipped
     * by; the throw-window dedup keeps one kill at one throw.
     */
    @Subscribe
    public synchronized void onActorDeath(net.runelite.api.events.ActorDeath event)
    {
        if (!(event.getActor() instanceof net.runelite.api.NPC)
            || client.getLocalPlayer() == null)
        {
            return;
        }
        net.runelite.api.NPC npc = (net.runelite.api.NPC) event.getActor();
        boolean fightingIt = npc.getInteracting() == client.getLocalPlayer()
            || client.getLocalPlayer().getInteracting() == npc;
        if (!fightingIt)
        {
            return;
        }
        tryBossKillThrow(npc);
    }

    private void tryBossKillThrow(net.runelite.api.NPC npc)
    {
        if (npc.getName() == null)
        {
            return;
        }
        if (!config.bossCatch())
        {
            bossLog(npc.getName(), "skipped (Boss catch throws disabled in settings)");
            return;
        }
        if (activeCatch != null)
        {
            bossLog(npc.getName(), "skipped (another catch animation was mid-flight)");
            return;
        }
        // Some bosses hand out loot through a corpse NPC (Maggot King), so a
        // trailing "Corpse"/"corpse" still matches the living boss's name
        String name = npc.getName();
        String lower = name.toLowerCase();
        if (lower.endsWith(" corpse"))
        {
            name = name.substring(0, name.length() - " corpse".length());
        }
        com.osrsgo.model.Species match = null;
        boolean knownName = false;
        for (com.osrsgo.model.Species sp : com.osrsgo.data.SpeciesData.all())
        {
            if (sp.getName().equalsIgnoreCase(name))
            {
                knownName = true;
                if (sp.getRarity() == Rarity.EPIC || sp.getRarity() == Rarity.LEGENDARY)
                {
                    match = sp;
                    break;
                }
            }
        }
        if (match == null)
        {
            bossLog(npc.getName(), knownName
                ? "no throw (in the GielDex but not Epic/Legendary)"
                : "no throw (species not in the GielDex)");
            return;
        }
        net.runelite.api.coords.LocalPoint local = npc.getLocalLocation();
        WorldPoint corpse = local != null && client.isInInstancedRegion()
            ? WorldPoint.fromLocalInstance(client, local)
            : npc.getWorldLocation();
        throwAtBoss(match, corpse);
    }

    /**
     * Raid and Barrows rewards arrive from a chest, not an NPC kill, so no
     * NpcLootReceived ever fires for those bosses. The EVENT-typed loot record
     * carries the raid's name instead; map it to the boss you just felled.
     */
    @Subscribe
    public synchronized void onLootReceived(net.runelite.client.plugins.loottracker.LootReceived event)
    {
        if (event.getType() != net.runelite.http.api.loottracker.LootRecordType.EVENT
            || event.getName() == null)
        {
            return;
        }
        String name = event.getName();
        int speciesId;
        if (name.startsWith("Chambers of Xeric"))
        {
            speciesId = 344; // Great Olm
        }
        else if (name.startsWith("Theatre of Blood"))
        {
            speciesId = 350; // Verzik Vitur
        }
        else if (name.startsWith("Tombs of Amascut"))
        {
            // The fight ends on both Wardens; honor one at random
            speciesId = rng.nextBoolean() ? 354 : 355;
        }
        else if (name.startsWith("Barrows"))
        {
            int[] brothers = {219, 322, 323, 324, 325, 326};
            speciesId = brothers[rng.nextInt(brothers.length)];
        }
        else
        {
            return;
        }
        if (!config.bossCatch())
        {
            bossLog(name, "skipped (Boss catch throws disabled in settings)");
            return;
        }
        if (activeCatch != null)
        {
            bossLog(name, "skipped (another catch animation was mid-flight)");
            return;
        }
        // The chest is a fallback: if the boss's own death already rolled a
        // throw this raid, don't roll a second one from the reward chest
        Long recent = lastBossThrowMs.get(speciesId);
        if (recent != null && System.currentTimeMillis() - recent < 10 * 60_000)
        {
            bossLog(name, "skipped (its boss already threw this raid)");
            return;
        }
        throwAtBoss(com.osrsgo.data.SpeciesData.byId(speciesId), playerLocation);
    }

    private void throwAtBoss(com.osrsgo.model.Species match, WorldPoint corpse)
    {
        Long lastThrow = lastBossThrowMs.get(match.getId());
        if (lastThrow != null && System.currentTimeMillis() - lastThrow < 30_000)
        {
            bossLog(match.getName(), "skipped (already threw at this kill)");
            return;
        }
        if (corpse == null)
        {
            corpse = playerLocation;
        }
        if (corpse == null)
        {
            return;
        }
        lastBossThrowMs.put(match.getId(), System.currentTimeMillis());
        long now = System.currentTimeMillis();
        Long lastKill = lastBossKillMs.put(match.getId(), now);
        int tier;
        double chance;
        if (config.bossMasterBall() && profile.masterBalls > 0)
        {
            profile.masterBalls--;
            tier = 4;
            chance = 1.0;
        }
        else
        {
            if (profile.balls <= 0)
            {
                bossLog(match.getName(), "skipped (no plain Gielinor Balls to throw)");
                return;
            }
            profile.balls--;
            tier = 0;
            // Tuned to roughly one catch per 10 hours of continuous bossing,
            // regardless of the boss's kill speed
            double minutes = lastKill == null ? 10 : (now - lastKill) / 60000.0;
            chance = Math.min(0.025, minutes / 600.0);
        }
        profile.stats.ballsThrown++;
        boolean caught = rng.nextDouble() < chance;
        bossLog(match.getName(), "THREW (" + String.format("%.1f", chance * 100) + "% chance, "
            + (caught ? "CAUGHT!" : "broke free") + ")");
        OwnedMon pending = null;
        if (caught)
        {
            pending = new OwnedMon();
            pending.speciesId = match.getId();
            pending.level = 50;
            pending.ivHp = 5 + rng.nextInt(11);
            pending.ivAtk = 5 + rng.nextInt(11);
            pending.ivDef = 5 + rng.nextInt(11);
            pending.ivSpd = 5 + rng.nextInt(11);
            pending.caughtAt = System.currentTimeMillis();
            pending.caughtNear = "Boss kill";
        }
        profile.seenSpecies.add(match.getId());
        chatMessage("Gielinor Safari: your " + BALL_NAMES[Math.max(0, Math.min(4, tier))]
            + " Ball flies at the fallen " + match.getName()
            + (tier == 4 ? "..." : " (" + String.format("%.1f", chance * 100) + "% chance)..."));
        WildSpawn fake = new WildSpawn(BOSS_CATCH_KEY, match.getId(), 50,
            corpse, false, new int[0], new int[0]);
        activeCatch = new CatchSequence(fake,
            caught ? CatchSequence.Outcome.CAUGHT : CatchSequence.Outcome.BROKE_FREE,
            pending, tier);
        saveSoon();
    }

    private void finalizeBossKillThrow(CatchSequence seq)
    {
        String bossName = com.osrsgo.data.SpeciesData.byId(seq.spawn.speciesId).getName();
        if (seq.outcome == CatchSequence.Outcome.CAUGHT)
        {
            profile.mons.add(seq.pendingMon);
            profile.caughtSpecies.add(seq.spawn.speciesId);
            profile.stats.catches++;
            profile.addEssence(seq.spawn.speciesId,
                catchEssence(com.osrsgo.data.SpeciesData.byId(seq.spawn.speciesId).getRarity()));
            chatMessage("Gielinor Safari: gotcha! The fallen " + bossName + "'s spirit joins you at level 50!");
        }
        else
        {
            chatMessage("Gielinor Safari: the " + bossName + "'s spirit broke free and faded.");
        }
        saveSoon();
        refreshPanel();
    }

    @Subscribe
    public void onMenuOpened(net.runelite.api.events.MenuOpened event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        if (mouse == null)
        {
            return;
        }
        for (WildSpawn spawn : nearbySpawns)
        {
            net.runelite.api.coords.LocalPoint lp =
                com.osrsgo.spawn.Coords.toLocal(client, spawn.location);
            if (lp == null)
            {
                continue;
            }
            java.awt.Polygon poly = net.runelite.api.Perspective.getCanvasTilePoly(client, lp);
            if (poly == null || !poly.contains(mouse.getX(), mouse.getY()))
            {
                continue;
            }
            String target = net.runelite.client.util.ColorUtil.wrapWithColorTag(
                (spawn.shiny ? "Shiny " : "") + spawn.species().getName() + " (lvl " + spawn.level + ")",
                spawn.shiny ? new java.awt.Color(255, 215, 60) : spawn.species().getRarity().getColor());
            final String key = spawn.key;
            client.createMenuEntry(-1)
                .setOption("Examine")
                .setTarget(target)
                .setType(net.runelite.api.MenuAction.RUNELITE)
                .onClick(e -> chatMessage("Gielinor Safari: " + spawn.species().getRarity().getDisplay() + " "
                    + spawn.species().getType().getDisplay() + " mon. "
                    + Math.round(spawn.species().getRarity().getBaseCatchChance() * 100) + "% base catch chance."));
            if (profile.berries > 0)
            {
                client.createMenuEntry(-1)
                    .setOption("Berry throw")
                    .setTarget(target)
                    .setType(net.runelite.api.MenuAction.RUNELITE)
                    .onClick(e ->
                    {
                        String err = attemptCatch(key, true, selectedBallTier);
                        if (err != null)
                        {
                            chatMessage("Gielinor Safari: " + err);
                        }
                    });
            }
            client.createMenuEntry(-1)
                .setOption("Catch")
                .setTarget(target)
                .setType(net.runelite.api.MenuAction.RUNELITE)
                .onClick(e ->
                {
                    String err = attemptCatch(key, false, selectedBallTier);
                    if (err != null)
                    {
                        chatMessage("Gielinor Safari: " + err);
                    }
                });
            break;
        }
    }

    private long slowTickLogMs;
    private final long[] segMs = new long[7];

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // The catch pipeline finalizes OUTSIDE the main tick body: a failure
        // anywhere else must never starve it, because a wedged activeCatch
        // silently blocks every future boss throw ("mid-flight" skips)
        try
        {
            if (activeCatch != null && activeCatch.finished())
            {
                finalizeCatch();
            }
        }
        catch (Exception e)
        {
            log.warn("catch finalize failed; dropping the stuck catch", e);
            activeCatch = null;
        }
        long tickStart = System.nanoTime();
        try
        {
            runGameTick();
        }
        catch (Exception e)
        {
            // One bad tick (a rogue learned animation, a weird scene) must not
            // take the whole plugin loop down with it
            log.warn("Gielinor Safari game tick error", e);
        }
        long ms = (System.nanoTime() - tickStart) / 1_000_000L;
        if (ms > 8 && System.currentTimeMillis() - slowTickLogMs > 10_000)
        {
            slowTickLogMs = System.currentTimeMillis();
            log.warn("Gielinor Safari slow game tick: {}ms (pre={} walk={} save={} spawns={} models={} scene={} tail={})",
                ms, segMs[5], segMs[0], segMs[1], segMs[2], segMs[3], segMs[4], segMs[6]);
        }
    }

    private void runGameTick()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        long preT = System.nanoTime();
        ensureResearchDay();
        WorldPoint previous = playerLocation;
        // Template-space inside instances so raid/POH spawns line up
        playerLocation = com.osrsgo.spawn.Coords.playerLocation(client);
        if (client.getLocalPlayer().getName() != null)
        {
            myRsn = client.getLocalPlayer().getName();
        }
        segMs[5] = (System.nanoTime() - preT) / 1_000_000L;
        long segT = System.nanoTime();
        trackWalking(previous, playerLocation);
        autoHealAtBank();
        segMs[0] = (System.nanoTime() - segT) / 1_000_000L;

        if (++gymPollCounter >= GYM_POLL_TICKS)
        {
            gymPollCounter = 0;
            pollGyms();
        }

        // Learn idle/walk animations from EVERY named NPC: catalog species get
        // their entry, and everything else builds the universal name library
        // so future species arrive pre-animated
        if (tickCounter % 5 == 0)
        {
            for (net.runelite.api.NPC npc : client.getNpcs())
            {
                String npcName = npc.getName();
                if (npcName == null || npcName.isEmpty())
                {
                    continue;
                }
                String lower = npcName.toLowerCase();
                animLibrary.learnPoseByName(lower, npc.getIdlePoseAnimation(), npc.getWalkAnimation());
                Integer sid = SPECIES_BY_NAME.get(lower);
                if (sid != null)
                {
                    animLibrary.learnPose(sid, npc.getIdlePoseAnimation(), npc.getWalkAnimation());
                }
                // Field journal: record every renderable NPC toward a full dex
                net.runelite.api.NPCComposition comp = npc.getTransformedComposition();
                if (comp != null && comp.getModels() != null && comp.getModels().length > 0)
                {
                    npcHarvest.record(npcName, npc.getId(), npc.getCombatLevel());
                }
            }
        }
        if (tickCounter % 200 == 0)
        {
            animLibrary.saveIfDirty();
            npcHarvest.saveIfDirty();
        }

        segT = System.nanoTime();
        if (tickCounter % 10 == 0)
        {
            checkMedals();
            flushProfileIfDirty();
        }
        segMs[1] = (System.nanoTime() - segT) / 1_000_000L;

        segT = System.nanoTime();
        spawnManager.update(client);
        nearbySpawns = spawnManager.nearby(playerLocation, NEARBY_RANGE);
        nearbyCandies = spawnManager.nearbyCandies(playerLocation, NEARBY_RANGE);
        // Walk onto (or next to) a candy to scoop it
        for (SpawnManager.Candy candy : nearbyCandies)
        {
            if (playerLocation.distanceTo(candy.location) <= 1)
            {
                spawnManager.takeCandy(candy.key);
                profile.rareCandies++;
                profile.stats.candiesFound++;
                chatAlways("Gielinor Safari: you scoop up a RARE CANDY! (" + profile.rareCandies + " held)");
                client.playSoundEffect(net.runelite.api.SoundEffectID.TOWN_CRIER_BELL_DING);
                saveSoon();
            }
        }
        segMs[2] = (System.nanoTime() - segT) / 1_000_000L;
        segT = System.nanoTime();
        spawnModels.sync(nearbySpawns, config.spawnModels());
        segMs[3] = (System.nanoTime() - segT) / 1_000_000L;
        segT = System.nanoTime();

        if (config.spawnModels())
        {
            if (session != null && !session.finished)
            {
                battleScene.ensure(session.myMon().speciesId, session.oppMon().speciesId, playerLocation);
            }
            else if (raidCatchPlaying())
            {
                // Keep the weakened boss standing while the catch scene plays
                battleScene.ensure(raidSceneMySpecies, raidCatchSpecies, playerLocation);
            }
            else if (battleScene.isCelebrating())
            {
                // Let the victory hops finish before tearing the scene down
            }
            else
            {
                battleScene.stop();
            }
        }
        else
        {
            battleScene.stop();
        }
        segMs[4] = (System.nanoTime() - segT) / 1_000_000L;
        segT = System.nanoTime();

        boolean sawNew = false;
        for (WildSpawn spawn : nearbySpawns)
        {
            sawNew |= profile.seenSpecies.add(spawn.speciesId);
        }
        if (sawNew)
        {
            saveSoon();
        }

        // Tribute pays on its own fixed 15-min clock: spawn waves are staggered
        // per region now and must not drive the economy
        long tributeBucketNow = System.currentTimeMillis() / TRIBUTE_MILLIS;
        if (tributeBucket == -1)
        {
            tributeBucket = tributeBucketNow;
        }
        else if (tributeBucketNow != tributeBucket)
        {
            tributeBucket = tributeBucketNow;
            grantGymTribute();
        }
        if (profileStore.consumeConflict())
        {
            chatAlways("Gielinor Safari: another client is playing this Safari profile at the same time!"
                + " Progress from both will fight (last save wins). Play one client at a time.");
        }
        // Spawn keys are bucket-unique, so these dedup sets only need pruning
        if (shinyDinged.size() > 2000)
        {
            shinyDinged.clear();
        }
        if (rareNotified.size() > 2000)
        {
            rareNotified.clear();
        }
        for (WildSpawn spawn : nearbySpawns)
        {
            Rarity r = spawn.species().getRarity();
            if (config.notifyRare() && (r == Rarity.EPIC || r == Rarity.LEGENDARY)
                && rareNotified.add(spawn.key))
            {
                notifier.notify("Gielinor Safari: a wild " + spawn.species().getName()
                    + " (lvl " + spawn.level + ") appeared nearby!");
            }
            if (config.shinyDing() && spawn.shiny && shinyDinged.add(spawn.key))
            {
                client.playSoundEffect(net.runelite.api.SoundEffectID.TOWN_CRIER_BELL_DING);
                chatMessage("Gielinor Safari: a SHINY " + spawn.species().getName()
                    + " (lvl " + spawn.level + ") sparkles nearby!");
            }
        }

        if (++tickCounter % PANEL_REFRESH_TICKS == 0)
        {
            refreshPanel();
        }
        segMs[6] = (System.nanoTime() - segT) / 1_000_000L;
    }

    // ------------------------------------------------------------------ state reads (panel, overlay)

    public PlayerProfile getProfile()
    {
        return profile;
    }

    public WorldPoint getPlayerLocation()
    {
        return playerLocation;
    }

    public List<WildSpawn> getNearbySpawns()
    {
        return nearbySpawns;
    }

    public boolean hasBadge(String badge)
    {
        return profile.badges.contains(badge);
    }

    public String getMyRsn()
    {
        return myRsn;
    }

    /** Gym control state, local since 2026-08-13. Empty when the feature is off. */
    public java.util.Map<String, GymHolder> gymHolders()
    {
        return config.gymControl() ? profile.gyms : java.util.Collections.emptyMap();
    }

    public GymHolder getGymHolder(String gymId)
    {
        return gymHolders().get(gymId);
    }

    public enum GymOwnership
    {
        UNCLAIMED,
        MINE,
        ENEMY
    }

    public GymOwnership gymOwnership(String gymId)
    {
        GymHolder holder = getGymHolder(gymId);
        if (holder == null || !holder.isClaimed())
        {
            return GymOwnership.UNCLAIMED;
        }
        return holder.isHeldBy(myRsn) ? GymOwnership.MINE : GymOwnership.ENEMY;
    }

    /** Whether the gym control feature is switched on in settings. */
    public boolean gymControlOn()
    {
        return config.gymControl();
    }

    /** Was a server poll; now a local sweep of lapsed holds plus rival pressure. */
    private synchronized void pollGyms()
    {
        if (!config.gymControl())
        {
            return;
        }
        com.osrsgo.gym.LocalGyms.expireStale(profile.gyms);
        applyRivalPressure();
        refreshPanel();
    }

    /**
     * Once per rotation a rival may take one held gym. Which rival occupies it
     * is deterministic for the bucket; whether a seizure happens is a roll,
     * because each player's empire is their own.
     */
    private void applyRivalPressure()
    {
        long bucket = System.currentTimeMillis() / SpawnManager.BUCKET_MILLIS;
        if (bucket == lastRivalRollBucket)
        {
            return;
        }
        lastRivalRollBucket = bucket;
        int held = gymsHeldNow();
        if (held <= 0 || myRsn == null)
        {
            return;
        }
        double chance = com.osrsgo.data.RivalData.seizureChance(
            held, profile.trainerLevel(), getOtherPartyMembers().size());
        if (rng.nextDouble() >= chance)
        {
            return;
        }
        List<String> mine = new ArrayList<>();
        for (java.util.Map.Entry<String, GymHolder> e : profile.gyms.entrySet())
        {
            if (e.getValue() != null && e.getValue().isHeldBy(myRsn))
            {
                mine.add(e.getKey());
            }
        }
        if (mine.isEmpty())
        {
            return;
        }
        String gymId = mine.get(rng.nextInt(mine.size()));
        com.osrsgo.data.RivalData.Rival rival = com.osrsgo.data.RivalData.rivalFor(
            bucket, gymId, profile.trainerLevel(), profile.faction);
        com.osrsgo.gym.LocalGyms.claim(profile.gyms, gymId, rival.name, rival.team,
            null, rival.faction);
        GymData.Gym gym = GymData.byId(gymId);
        saveSoon();
        chatMessage("Gielinor Safari: " + rival.name + " has seized "
            + (gym != null ? gym.name : gymId) + "! Take it back.");
    }

    public long millisUntilRotation()
    {
        return spawnManager.millisUntilRotation();
    }

    /**
     * Holding gyms pays every 15 minutes: balls and xp per gym, plus empire
     * bonuses: a Great Ball per 3 gyms held, and holding ALL of them pays 2
     * Ultra Balls and +250 xp on top (plus the passive catch-chance bonus).
     */
    private synchronized void grantGymTribute()
    {
        int held = gymsHeldNow();
        if (held <= 0)
        {
            return;
        }
        double mult = tributeMultiplier();
        int payout = (int) Math.round(held * tributePerGym() * mult);
        int ballsGained = Math.min(payout, maxBalls() - profile.balls);
        profile.balls = Math.min(maxBalls(), profile.balls + payout);
        int xp = (int) Math.round(25 * held * mult);
        int greats = held / 3;
        profile.greatBalls += greats;
        boolean monarch = held >= com.osrsgo.data.GymData.all().size();
        if (monarch)
        {
            profile.ultraBalls += 2;
            xp += 250;
        }
        profile.trainerXp += xp;
        saveSoon();
        String bonus = (greats > 0 ? ", +" + greats + " Great" : "")
            + (monarch ? ", +2 Ultra" : "");
        chatMessage("Gielinor Safari: tribute from your " + held + " gym" + (held == 1 ? "" : "s") + ": +"
            + Math.max(0, ballsGained) + " balls" + bonus + ", +" + xp + " xp."
            + (mult > 1.0 ? " (party x" + String.format("%.2f", mult) + ")" : "")
            + (monarch ? " ALL GYMS HELD: you reign over Gielinor!" : ""));
    }

    /** +2% catch chance per held gym, capped at +10%. */
    public double gymCatchBonus()
    {
        return Math.min(0.10, gymsHeldNow() * 0.02);
    }

    /** Gyms currently held by this player, from local state. */
    public int gymsHeldNow()
    {
        return myRsn == null ? 0 : com.osrsgo.gym.LocalGyms.heldBy(gymHolders(), myRsn);
    }

    public boolean isNightNow()
    {
        return spawnManager.isNightNow();
    }

    public com.osrsgo.model.Species getSpotlightSpecies()
    {
        return com.osrsgo.data.SpeciesData.byId(spawnManager.currentSpotlightId());
    }

    /** Biome the player is standing in, for the Nearby tab header. */
    public com.osrsgo.data.BiomeData.Biome getCurrentBiome()
    {
        WorldPoint player = playerLocation;
        return player != null ? com.osrsgo.data.BiomeData.biomeAt(player.getX(), player.getY()) : null;
    }

    public BattleSession getSession()
    {
        return session;
    }

    public boolean isInParty()
    {
        return partyService.isInParty();
    }

    /** Party members other than the local player. */
    public List<PartyMember> getOtherPartyMembers()
    {
        List<PartyMember> others = new ArrayList<>();
        if (!partyService.isInParty() || partyService.getLocalMember() == null)
        {
            return others;
        }
        long self = partyService.getLocalMember().getMemberId();
        for (PartyMember member : partyService.getMembers())
        {
            if (member.getMemberId() != self)
            {
                others.add(member);
            }
        }
        return others;
    }

    public String getPendingChallengerName()
    {
        return pendingChallengerName;
    }

    public boolean hasOutgoingChallenge()
    {
        return outgoingBattleId != null;
    }

    // ------------------------------------------------------------------ trainer perks
    // All computed live from trainer level; leveling up IS the unlock.

    /** Ball pouch grows +10 per trainer level (base 40, cap 300). */
    public int maxBalls()
    {
        return Math.min(300, 40 + profile.trainerLevel() * 10);
    }

    /** Berry pouch grows +2 per level (base 18, cap 60). */
    public int maxBerries()
    {
        return Math.min(60, 18 + profile.trainerLevel() * 2);
    }

    /** Egg slots: 3, +1 at levels 5, 15, 50, 90. */
    public int eggCap()
    {
        int lvl = profile.trainerLevel();
        return 3 + (lvl >= 5 ? 1 : 0) + (lvl >= 15 ? 1 : 0) + (lvl >= 50 ? 1 : 0) + (lvl >= 90 ? 1 : 0);
    }

    /** Team size: 3, then 4 at level 10, 5 at level 30. */
    public int teamCap()
    {
        int lvl = profile.trainerLevel();
        return 3 + (lvl >= 10 ? 1 : 0) + (lvl >= 30 ? 1 : 0);
    }

    /** Ball find rate: starts at 100 tiles, improves 2 tiles per level, floors at 35. */
    public int tilesPerBall()
    {
        int tiles = Math.max(35, 100 - profile.trainerLevel() * 2);
        return SpawnManager.isCommunityHour() ? Math.max(18, tiles / 2) : tiles;
    }

    /** Extra balls on qualifying kills as the trainer grows (0 to +4). */
    public int killBallBonus()
    {
        return Math.min(4, profile.trainerLevel() / 25);
    }

    /** Mon battle XP multiplier: +1% per trainer level (2x at level 100). */
    public double monXpMultiplier()
    {
        return 1.0 + Math.min(100, profile.trainerLevel()) / 100.0;
    }

    /** Buddy XP per 10 tiles: 2, +1 per 25 levels (up to 6). */
    public int buddyXpPerGrant()
    {
        return 2 + Math.min(4, profile.trainerLevel() / 25);
    }

    /** Berry find rate: 150 tiles, 125 at level 20, 100 at level 75. */
    public int tilesPerBerry()
    {
        int lvl = profile.trainerLevel();
        return lvl >= 75 ? 100 : (lvl >= 20 ? 125 : 150);
    }

    /** Gym tribute balls per gym per rotation: 1, 2 at level 25, 3 at level 65. */
    public int tributePerGym()
    {
        int lvl = profile.trainerLevel();
        return 1 + (lvl >= 25 ? 1 : 0) + (lvl >= 65 ? 1 : 0);
    }

    public double tributeMultiplier()
    {
        return com.osrsgo.data.RivalData.tributeMultiplierFor(getOtherPartyMembers().size());
    }

    /** Berry throw bonus: +15%, +20% at level 35. */
    public double berryBonus()
    {
        return profile.trainerLevel() >= 35 ? 0.20 : 0.15;
    }

    /** Flawless egg odds: 3%, 5% at level 45, 8% at level 90. */
    public double flawlessChance()
    {
        int lvl = profile.trainerLevel();
        return lvl >= 90 ? 0.08 : (lvl >= 45 ? 0.05 : 0.03);
    }

    /** Egg shiny denominator: 1/32, 1/24 at level 50, 1/16 at level 85. */
    public int eggShinyDenominator()
    {
        int lvl = profile.trainerLevel();
        return lvl >= 85 ? 16 : (lvl >= 50 ? 24 : 32);
    }

    /** XP per earned ball: 5000 shrinking 25 per level, floors at 2500. */
    public int xpPerBall()
    {
        return Math.max(2500, 5000 - profile.trainerLevel() * 25);
    }

    /** Catch range: 5 tiles, growing to 6/7/8 at levels 40/65/90. */
    public int catchRange()
    {
        int lvl = profile.trainerLevel();
        return 5 + (lvl >= 40 ? 1 : 0) + (lvl >= 65 ? 1 : 0) + (lvl >= 90 ? 1 : 0);
    }

    /** Master Ball exchange cost: 25 Ultras, 20 at level 80. */
    public int masterCost()
    {
        return profile.trainerLevel() >= 80 ? 20 : 25;
    }

    /** Trainer catch bonus cap: +15%, raised to +25% at level 100 (Champion). */
    public double trainerCatchBonus()
    {
        double cap = profile.trainerLevel() >= 100 ? 0.25 : 0.15;
        return Math.min(cap, profile.trainerLevel() * 0.005);
    }

    /**
     * The walking economy: traveled tiles earn Gielinor Balls and level the
     * buddy. Jumps beyond MAX_STEP tiles (teleports, agility shortcuts) earn
     * nothing.
     */
    private synchronized void trackWalking(WorldPoint from, WorldPoint to)
    {
        if (from == null || to == null || from.getPlane() != to.getPlane())
        {
            return;
        }
        int delta = from.distanceTo(to);
        if (delta < 1 || delta > MAX_STEP)
        {
            return;
        }
        profile.tilesWalked += delta;

        profile.ballProgress += delta;
        boolean save = false;
        while (profile.ballProgress >= tilesPerBall())
        {
            profile.ballProgress -= tilesPerBall();
            if (profile.balls < maxBalls())
            {
                profile.balls++;
                profile.stats.ballsFound++;
                chatFind("Gielinor Safari: you found a Gielinor Ball while traveling! (" + profile.balls + " in pouch)");
            }
            save = true;
        }

        OwnedMon buddy = buddy();
        if (buddy != null)
        {
            profile.buddyProgress += delta;
            while (profile.buddyProgress >= TILES_PER_BUDDY_XP)
            {
                profile.buddyProgress -= TILES_PER_BUDDY_XP;
                int gained = buddy.gainXp(buddyXpPerGrant());
                if (gained > 0)
                {
                    chatMessage("Gielinor Safari: your buddy " + buddy.name() + " grew to level " + buddy.level + "!");
                    save = true;
                }
            }
        }

        researchEvent(com.osrsgo.data.ResearchData.Kind.WALK_TILES, delta);

        profile.berryProgress += delta;
        while (profile.berryProgress >= tilesPerBerry())
        {
            profile.berryProgress -= tilesPerBerry();
            if (profile.berries < maxBerries())
            {
                profile.berries++;
                profile.stats.berriesFound++;
                chatFind("Gielinor Safari: you found a juicy berry! (" + profile.berries + " held)");
            }
            save = true;
        }

        java.util.Iterator<com.osrsgo.model.Egg> it = profile.eggs.iterator();
        while (it.hasNext())
        {
            com.osrsgo.model.Egg egg = it.next();
            egg.tilesProgress += delta;
            if (egg.tilesProgress >= egg.tilesRequired)
            {
                it.remove();
                OwnedMon hatched = new OwnedMon();
                hatched.speciesId = egg.speciesId;
                hatched.level = 5;
                hatched.ivHp = egg.ivHp;
                hatched.ivAtk = egg.ivAtk;
                hatched.ivDef = egg.ivDef;
                hatched.ivSpd = egg.ivSpd;
                hatched.shiny = egg.shiny;
                hatched.caughtAt = System.currentTimeMillis();
                hatched.caughtNear = "Gertrude's egg";
                hatched.hatchedBy = myRsn;
                boolean newDexEntry = !profile.caughtSpecies.contains(egg.speciesId);
                profile.mons.add(hatched);
                profile.seenSpecies.add(egg.speciesId);
                profile.caughtSpecies.add(egg.speciesId);
                profile.stats.eggsHatched++;
                researchEvent(com.osrsgo.data.ResearchData.Kind.HATCH_EGGS, 1);
                String hatchName = (egg.shiny ? "SHINY " : "") + hatched.name();
                boolean flawless = egg.ivHp == 15 && egg.ivAtk == 15 && egg.ivDef == 15 && egg.ivSpd == 15;
                // Hatch xp scales with the walk you put in (400-3000 tiles),
                // doubled for shinies, +100 for a new GielDex entry
                int hatchXp = egg.tilesRequired / 10;
                if (egg.shiny)
                {
                    hatchXp *= 2;
                }
                if (newDexEntry)
                {
                    hatchXp += 100;
                }
                profile.trainerXp += hatchXp;
                if (config.eggDing())
                {
                    client.playSoundEffect(net.runelite.api.SoundEffectID.TOWN_CRIER_BELL_DONG);
                }
                chatAlways("Gielinor Safari: *** EGG HATCHED *** " + hatchName + " (lvl 5"
                    + (flawless ? ", FLAWLESS" : "") + ") joins you! +" + hatchXp + " trainer xp.");
                notifier.notify("Gielinor Safari: your egg hatched into a " + hatchName + "!");
                save = true;
            }
        }

        if (save)
        {
            saveSoon();
        }
    }

    public OwnedMon buddy()
    {
        Integer idx = profile.buddyIndex;
        if (idx == null || idx < 0 || idx >= profile.mons.size())
        {
            return null;
        }
        return profile.mons.get(idx);
    }

    public synchronized void renameMon(int monIndex, String nickname)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return;
        }
        OwnedMon mon = profile.mons.get(monIndex);
        if (nickname == null || nickname.trim().isEmpty())
        {
            mon.nickname = null;
        }
        else
        {
            String cleaned = nickname.trim().replaceAll("[^A-Za-z0-9 '\\-]", "");
            mon.nickname = cleaned.substring(0, Math.min(16, cleaned.length()));
        }
        saveSoon();
        refreshPanel();
    }

    public synchronized void setBuddy(Integer monIndex)
    {
        profile.buddyIndex = monIndex;
        profile.buddyProgress = 0;
        saveSoon();
        refreshPanel();
    }

    // ------------------------------------------------------------------ research

    private void ensureResearchDay()
    {
        boolean changed = false;
        long today = com.osrsgo.data.ResearchData.todayUtc();
        if (profile.researchDay != today)
        {
            profile.researchDay = today;
            profile.researchProgress = new int[3];
            profile.researchDone = new boolean[3];
            changed = true;
        }
        long week = com.osrsgo.data.ResearchData.weekUtc();
        if (profile.researchWeek != week)
        {
            profile.researchWeek = week;
            profile.weeklyProgress = new int[2];
            profile.weeklyDone = new boolean[2];
            changed = true;
        }
        long month = com.osrsgo.data.ResearchData.monthUtc();
        if (profile.researchMonth != month)
        {
            profile.researchMonth = month;
            profile.monthlyProgress = new int[2];
            profile.monthlyDone = new boolean[2];
            changed = true;
        }
        if (changed)
        {
            saveSoon();
            refreshPanel();
        }
    }

    public List<com.osrsgo.data.ResearchData.Task> researchTasks()
    {
        return com.osrsgo.data.ResearchData.tasksFor(com.osrsgo.data.ResearchData.todayUtc());
    }

    private com.osrsgo.model.MonType currentResearchType()
    {
        for (com.osrsgo.data.ResearchData.Task task : researchTasks())
        {
            if (task.kind == com.osrsgo.data.ResearchData.Kind.CATCH_TYPE)
            {
                return task.type;
            }
        }
        return null;
    }

    private synchronized void researchEvent(com.osrsgo.data.ResearchData.Kind kind, int amount)
    {
        if (amount <= 0)
        {
            return;
        }
        ensureResearchDay();
        applyResearchGroup(kind, amount, researchTasks(),
            profile.researchProgress, profile.researchDone, "research");
        applyResearchGroup(kind, amount, weeklyTasks(),
            profile.weeklyProgress, profile.weeklyDone, "weekly challenge");
        applyResearchGroup(kind, amount, monthlyTasks(),
            profile.monthlyProgress, profile.monthlyDone, "monthly challenge");
    }

    public List<com.osrsgo.data.ResearchData.Task> weeklyTasks()
    {
        return com.osrsgo.data.ResearchData.weeklyTasks(com.osrsgo.data.ResearchData.weekUtc());
    }

    public List<com.osrsgo.data.ResearchData.Task> monthlyTasks()
    {
        return com.osrsgo.data.ResearchData.monthlyTasks(com.osrsgo.data.ResearchData.monthUtc());
    }

    private void applyResearchGroup(com.osrsgo.data.ResearchData.Kind kind, int amount,
        List<com.osrsgo.data.ResearchData.Task> tasks, int[] progress, boolean[] done, String label)
    {
        for (int i = 0; i < tasks.size() && i < progress.length; i++)
        {
            com.osrsgo.data.ResearchData.Task task = tasks.get(i);
            if (task.kind != kind || done[i])
            {
                continue;
            }
            progress[i] += amount;
            if (progress[i] >= task.goal)
            {
                done[i] = true;
                profile.balls = Math.min(Math.max(maxBalls(), profile.balls), profile.balls + task.rewardBalls);
                profile.greatBalls += task.rewardGreatBalls;
                profile.ultraBalls += task.rewardUltraBalls;
                profile.trainerXp += task.rewardXp;
                profile.stats.researchCompleted++;
                chatMessage("Gielinor Safari " + label + " complete: " + task.describe()
                    + "! " + task.rewardText());
            }
            saveSoon();
        }
    }

    /** Announces newly earned medal tiers; earned set persists in the profile. */
    private synchronized void checkMedals()
    {
        boolean save = false;
        for (com.osrsgo.data.MedalData.Medal medal : com.osrsgo.data.MedalData.ALL)
        {
            int tier = medal.tierOf(profile);
            for (int t = 1; t <= tier; t++)
            {
                if (profile.earnedMedals.add(medal.id + ":" + t))
                {
                    String bonus;
                    switch (t)
                    {
                        case 2:
                            profile.ultraBalls += 2;
                            bonus = " +2 Ultra Balls!";
                            break;
                        case 3:
                            profile.masterBalls++;
                            bonus = " +1 MASTER BALL!";
                            break;
                        default:
                            profile.greatBalls += 2;
                            bonus = " +2 Great Balls!";
                            break;
                    }
                    chatMessage("Gielinor Safari: " + com.osrsgo.data.MedalData.tierName(t)
                        + " medal earned: " + medal.name + "!" + bonus);
                    save = true;
                }
            }
        }
        if (save)
        {
            saveSoon();
            refreshPanel();
        }
    }

    /** Catch essence scales with rarity: 3 for Common up to 7 for Legendary. */
    public int catchEssence(Rarity rarity)
    {
        return 3 + rarity.ordinal();
    }

    /** Release essence scales with rarity AND level: 5 + 3/tier + level/5. */
    public int releaseEssence(OwnedMon mon)
    {
        return 5 + mon.species().getRarity().ordinal() * 3 + mon.level / 5;
    }

    // ------------------------------------------------------------------ essence and evolution

    public synchronized String evolve(int monIndex)
    {
        return evolve(monIndex, -1);
    }

    /** expectedSpeciesId guards against stale panel rows evolving the wrong mon. */
    public synchronized String evolve(int monIndex, int expectedSpeciesId)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return "Unknown mon.";
        }
        OwnedMon mon = profile.mons.get(monIndex);
        if (expectedSpeciesId != -1 && mon.speciesId != expectedSpeciesId)
        {
            refreshPanel();
            return "The list changed under you; nothing was evolved. Try again.";
        }
        com.osrsgo.data.EvolutionData.Evolution evo = com.osrsgo.data.EvolutionData.of(mon.speciesId);
        if (evo == null)
        {
            return mon.name() + " cannot evolve.";
        }
        String fromName = mon.name();
        int oldHp = mon.maxHp();
        int oldAtk = mon.atk();
        if (!spendEssence(mon.speciesId, evo.cost))
        {
            return "Need " + evo.cost + " " + tierName(mon.speciesId) + " essence (have "
                + profile.essenceOf(mon.speciesId) + " + " + profile.shinyEssence
                + " shiny). Catch or release more " + tierName(mon.speciesId) + " mons!";
        }
        mon.speciesId = evo.toSpeciesId;
        // Evolution surge: each hidden number improves by 1-3 (capped 15)
        mon.ivHp = Math.min(15, mon.ivHp + 1 + rng.nextInt(3));
        mon.ivAtk = Math.min(15, mon.ivAtk + 1 + rng.nextInt(3));
        mon.ivDef = Math.min(15, mon.ivDef + 1 + rng.nextInt(3));
        mon.ivSpd = Math.min(15, mon.ivSpd + 1 + rng.nextInt(3));
        mon.healFull();
        profile.seenSpecies.add(evo.toSpeciesId);
        profile.caughtSpecies.add(evo.toSpeciesId);
        profile.trainerXp += 50;
        profile.stats.evolutions++;
        saveSoon();
        chatMessage("Gielinor Safari: your " + fromName + " evolved into " + mon.name()
            + "! HP " + oldHp + "->" + mon.maxHp() + ", Atk " + oldAtk + "->" + mon.atk()
            + ", and its hidden numbers surged.");
        refreshPanel();
        return null;
    }

    // ------------------------------------------------------------------ faction

    public synchronized String chooseFaction(String faction)
    {
        if (profile.faction != null)
        {
            return "You already serve " + profile.faction + ".";
        }
        if (!"SARADOMIN".equals(faction) && !"ZAMORAK".equals(faction) && !"GUTHIX".equals(faction))
        {
            return "Unknown god.";
        }
        profile.faction = faction;
        saveSoon();
        chatMessage("Gielinor Safari: you have pledged yourself to " + faction + "! Take gyms in their name.");
        refreshPanel();
        return null;
    }

    public String getFaction()
    {
        return profile.faction;
    }

    // ------------------------------------------------------------------ raids

    public List<com.osrsgo.data.RaidData.Raid> currentRaids()
    {
        return com.osrsgo.data.RaidData.raidsFor(System.currentTimeMillis() / SpawnManager.BUCKET_MILLIS);
    }

    /** One shot per raid boss per rotation: the key ties the attempt to this bucket's boss. */
    private String raidAttemptKey(String gymId)
    {
        return (System.currentTimeMillis() / SpawnManager.BUCKET_MILLIS) + ":" + gymId;
    }

    public boolean raidAlreadyAttempted(String gymId)
    {
        return profile.raidAttempts.contains(raidAttemptKey(gymId));
    }

    private void recordRaidAttempt(String gymId)
    {
        String prefix = (System.currentTimeMillis() / SpawnManager.BUCKET_MILLIS) + ":";
        profile.raidAttempts.removeIf(key -> !key.startsWith(prefix));
        profile.raidAttempts.add(raidAttemptKey(gymId));
        saveSoon();
    }

    public com.osrsgo.data.RaidData.Raid raidAtGym(String gymId)
    {
        return com.osrsgo.data.RaidData.raidAt(System.currentTimeMillis() / SpawnManager.BUCKET_MILLIS, gymId);
    }

    public synchronized String startRaidBattle(String gymIdArg)
    {
        GymData.Gym gym = GymData.byId(gymIdArg);
        com.osrsgo.data.RaidData.Raid raid = gym != null ? raidAtGym(gym.id) : null;
        if (raid == null)
        {
            return "No raid boss here right now.";
        }
        if (raidAlreadyAttempted(gym.id))
        {
            return "You already challenged this raid boss. A new one arrives next rotation.";
        }
        autoCloseFinishedBattle();
        drainRaidThrows();
        if (session != null)
        {
            return "Already in a battle.";
        }
        if (!inGymRange(gym))
        {
            return "Walk to the gym mark first (within 10 tiles).";
        }
        List<OwnedMon> team = battleReadyTeam();
        if (team.isEmpty())
        {
            return teamError();
        }
        List<BattleMon> mine = new ArrayList<>();
        for (OwnedMon m : team)
        {
            mine.add(BattleMon.fromOwned(m));
        }
        BattleMon boss = BattleMon.fromSpecies(raid.speciesId, raid.level);
        List<BattleMon> theirs = new ArrayList<>();
        theirs.add(boss);
        battleTeamRefs = team;
        session = new BattleSession(BattleSession.Mode.AI, "raid-" + gym.id,
            "RAID: " + boss.name, -1, null, mine, theirs);
        session.raidSpeciesId = raid.speciesId;
        session.fullLog.add("A raid-boss " + boss.name + " (lvl " + raid.level + ") towers over " + gym.name + "!");
        refreshPanel();
        return null;
    }

    /**
     * Shared spoils: every team member gets the base, and each KO a mon
     * scored pays a +50% bonus on top. Participation matters now.
     */
    private void grantBattleMonXp(int avgOppLevel)
    {
        int monXp = (int) ((30 + 5 * avgOppLevel) * monXpMultiplier());
        java.util.Map<Integer, Integer> kos = new java.util.HashMap<>();
        for (int scorer : session.koScorers)
        {
            kos.merge(scorer, 1, Integer::sum);
        }
        for (int i = 0; i < battleTeamRefs.size(); i++)
        {
            OwnedMon m = battleTeamRefs.get(i);
            int bonusKos = kos.getOrDefault(i, 0);
            int total = monXp + (monXp / 2) * bonusKos;
            int gained = m.gainXp(total);
            if (bonusKos > 0)
            {
                session.fullLog.add(m.name() + " scored " + bonusKos + " KO"
                    + (bonusKos == 1 ? "" : "s") + ": +" + ((monXp / 2) * bonusKos) + " bonus xp!");
            }
            if (gained > 0)
            {
                session.fullLog.add(m.name() + " grew to level " + m.level + "!");
            }
        }
    }

    private void finishRaid()
    {
        profile.stats.raidWins++;
        profile.trainerXp += 300;
        session.fullLog.add("Raid cleared! +300 trainer xp.");
        session.fullLog.add("(Raids don't claim the gym: hit its Claim/Take over button for that.)");
        // The team that felled the boss levels from it too
        grantBattleMonXp(session.oppTeam.isEmpty() ? 70 : session.oppTeam.get(0).level);
        rollRaidCatch(session.raidSpeciesId, session.fullLog);
    }

    /** Bonus catch throws after a raid win: 2 base, +1 per 20 trainer levels. */
    private int raidThrows()
    {
        return Math.min(7, 2 + profile.trainerLevel() / 20);
    }

    // Raid catch scene: after a raid win the throws play out as real ball
    // animations at the boss's tile, one sequence per throw
    private static final String RAID_CATCH_KEY = "raid-catch";
    private final java.util.ArrayDeque<com.osrsgo.spawn.CatchSequence.Outcome> raidThrowOutcomes =
        new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Integer> raidThrowTiers = new java.util.ArrayDeque<>();
    private int raidCatchSpecies = -1;
    private int raidSceneMySpecies = -1;
    private int raidThrowNum;
    private List<String> raidCatchLog;
    // Captured once when the throws are rolled; a later battle's scene must
    // never re-aim leftover throws at its own opponent
    private WorldPoint raidCatchTile;

    public boolean raidCatchPlaying()
    {
        return raidCatchSpecies != -1;
    }

    /** Raid throw odds per ball tier; Master Balls never fail. */
    private static double raidCatchChance(int tier)
    {
        switch (tier)
        {
            case 1: return 0.22;
            case 2: return 0.32;
            case 3: return 0.47;
            case 4: return 1.0;
            default: return 0.12;
        }
    }

    private static final String[] BALL_NAMES = {"Gielinor", "Great", "Super", "Ultra", "Master"};

    /** Consumes one ball of the tier if available; returns the tier actually used, or -1 when dry. */
    private int consumeRaidBall(int wantedTier)
    {
        switch (wantedTier)
        {
            case 1:
                if (profile.greatBalls > 0)
                {
                    profile.greatBalls--;
                    return 1;
                }
                break;
            case 2:
                if (profile.superBalls > 0)
                {
                    profile.superBalls--;
                    return 2;
                }
                break;
            case 3:
                if (profile.ultraBalls > 0)
                {
                    profile.ultraBalls--;
                    return 3;
                }
                break;
            case 4:
                if (profile.masterBalls > 0)
                {
                    profile.masterBalls--;
                    return 4;
                }
                break;
            default:
                break;
        }
        if (profile.balls > 0)
        {
            profile.balls--;
            return 0;
        }
        return -1;
    }

    private void rollRaidCatch(int speciesId, List<String> log)
    {
        String bossName = com.osrsgo.data.SpeciesData.byId(speciesId).getName();
        int maxThrows = raidThrows();
        int wantedTier = getSelectedBallTier();
        log.add("The weakened " + bossName + " gives you " + maxThrows
            + " throws (2 base, +1 per 20 trainer levels). Throwing "
            + BALL_NAMES[Math.max(0, Math.min(4, wantedTier))] + " Balls.");
        raidThrowOutcomes.clear();
        raidThrowTiers.clear();
        boolean caught = false;
        int attempts = 0;
        while (attempts < maxThrows && !caught)
        {
            int usedTier = consumeRaidBall(wantedTier);
            if (usedTier < 0)
            {
                break;
            }
            if (usedTier != wantedTier)
            {
                wantedTier = usedTier;
            }
            profile.stats.ballsThrown++;
            attempts++;
            caught = rng.nextDouble() < raidCatchChance(usedTier);
            raidThrowOutcomes.add(caught
                ? com.osrsgo.spawn.CatchSequence.Outcome.CAUGHT
                : com.osrsgo.spawn.CatchSequence.Outcome.BROKE_FREE);
            raidThrowTiers.add(usedTier);
        }
        if (attempts == 0)
        {
            profile.seenSpecies.add(speciesId);
            log.add("No balls to throw! The " + bossName + " wanders off.");
            return;
        }
        raidCatchSpecies = speciesId;
        raidCatchLog = log;
        raidThrowNum = 0;
        raidSceneMySpecies = session != null && !session.myTeam.isEmpty()
            ? session.myMon().speciesId
            : speciesId;
        raidCatchTile = battleScene.isActive() && battleScene.getTheirsWp() != null
            ? battleScene.getTheirsWp()
            : (playerLocation != null
                ? new WorldPoint(playerLocation.getX() + 2, playerLocation.getY() + 2, playerLocation.getPlane())
                : null);
        startNextRaidThrow();
    }

    private void startNextRaidThrow()
    {
        com.osrsgo.spawn.CatchSequence.Outcome outcome = raidThrowOutcomes.poll();
        Integer throwTier = raidThrowTiers.poll();
        if (outcome == null)
        {
            raidCatchSpecies = -1;
            raidCatchLog = null;
            return;
        }
        int ballTier = throwTier != null ? throwTier : 0;
        raidThrowNum++;
        OwnedMon pending = null;
        if (outcome == com.osrsgo.spawn.CatchSequence.Outcome.CAUGHT)
        {
            pending = new OwnedMon();
            pending.speciesId = raidCatchSpecies;
            pending.level = 50;
            pending.ivHp = 5 + rng.nextInt(11);
            pending.ivAtk = 5 + rng.nextInt(11);
            pending.ivDef = 5 + rng.nextInt(11);
            pending.ivSpd = 5 + rng.nextInt(11);
            pending.caughtAt = System.currentTimeMillis();
            pending.caughtNear = "Raid";
        }
        WorldPoint bossTile = raidCatchTile;
        if (bossTile == null)
        {
            // No scene to animate in; resolve this throw instantly
            finalizeRaidThrow(new com.osrsgo.spawn.CatchSequence(
                new WildSpawn(RAID_CATCH_KEY, raidCatchSpecies, 50,
                    new WorldPoint(0, 0, 0), false, new int[0], new int[0]),
                outcome, pending, ballTier));
            return;
        }
        WildSpawn fake = new WildSpawn(RAID_CATCH_KEY, raidCatchSpecies, 50,
            bossTile, false, new int[0], new int[0]);
        activeCatch = new com.osrsgo.spawn.CatchSequence(fake, outcome, pending, ballTier);
    }

    /**
     * Instantly resolves any raid throws still animating or queued. Called
     * before a new battle starts so leftover throws can't play out aimed at
     * the new battle's opponent (they once pelted Duke Horacio's goblins).
     */
    private void drainRaidThrows()
    {
        int guard = 0;
        while (activeCatch != null && RAID_CATCH_KEY.equals(activeCatch.spawn.key) && guard++ < 16)
        {
            com.osrsgo.spawn.CatchSequence seq = activeCatch;
            activeCatch = null;
            finalizeRaidThrow(seq);
        }
    }

    private void finalizeRaidThrow(com.osrsgo.spawn.CatchSequence seq)
    {
        String bossName = com.osrsgo.data.SpeciesData.byId(seq.spawn.speciesId).getName();
        if (seq.outcome == com.osrsgo.spawn.CatchSequence.Outcome.CAUGHT)
        {
            profile.mons.add(seq.pendingMon);
            profile.seenSpecies.add(seq.spawn.speciesId);
            profile.caughtSpecies.add(seq.spawn.speciesId);
            profile.stats.catches++;
            profile.addEssence(seq.spawn.speciesId,
                catchEssence(com.osrsgo.data.SpeciesData.byId(seq.spawn.speciesId).getRarity()));
            if (raidCatchLog != null)
            {
                raidCatchLog.add("Gotcha! The raid " + bossName + " joins you at level 50!");
            }
            chatMessage("Gielinor Safari: you caught the raid " + bossName + "!");
            raidThrowOutcomes.clear();
            raidThrowTiers.clear();
            raidCatchSpecies = -1;
            raidCatchLog = null;
            saveSoon();
        }
        else
        {
            if (raidCatchLog != null)
            {
                raidCatchLog.add("Throw " + raidThrowNum + ": the " + bossName + " broke free!");
            }
            if (!raidThrowOutcomes.isEmpty())
            {
                startNextRaidThrow();
            }
            else
            {
                profile.seenSpecies.add(seq.spawn.speciesId);
                if (raidCatchLog != null)
                {
                    raidCatchLog.add("The raid " + bossName + " escaped!");
                }
                chatMessage("Gielinor Safari: the raid " + bossName + " escaped!");
                raidCatchSpecies = -1;
                raidCatchLog = null;
                saveSoon();
            }
        }
        refreshPanel();
    }

    // ------------------------------------------------------------------ trading over party

    /**
     * A live two-sided trade window. Offers hold OwnedMon references (never
     * indices) so nothing else shifting the collection can swap in the wrong
     * mon at commit time. Any offer change withdraws both acceptances; the
     * trade commits only when both sides have accepted the same state.
     */
    public static class TradeSession
    {
        public String tradeId;
        public long partnerId;
        public String partnerName;
        public final List<OwnedMon> myOffer = new ArrayList<>();
        public List<OwnedMon> theirOffer = new ArrayList<>();
        public boolean myAccepted;
        public boolean theirAccepted;
        public boolean partnerOpened;
    }

    public static final int TRADE_OFFER_CAP = 8;
    private TradeSession trade;
    private String inTradeId;
    private long inTradeFromId = -1;
    private String inTradeFromName;

    public TradeSession tradeSession()
    {
        return trade;
    }

    public String getIncomingTradeFrom()
    {
        return inTradeId != null ? inTradeFromName : null;
    }

    /** Opens a trade window with a party member and invites them in. */
    public synchronized String startTrade(long targetId)
    {
        if (!partyService.isInParty() || partyService.getLocalMember() == null)
        {
            return "Join a RuneLite party first.";
        }
        if (trade != null)
        {
            return "You already have a trade open.";
        }
        trade = new TradeSession();
        trade.tradeId = localMemberId() + "-t-" + System.currentTimeMillis();
        trade.partnerId = targetId;
        trade.partnerName = partyMemberName(targetId);
        partyService.send(new com.osrsgo.party.TradeOfferMsg(targetId, trade.tradeId, localName(), ""));
        refreshPanel();
        return null;
    }

    /** Accepts a pending trade invite, opening this side's window. */
    public synchronized String acceptIncomingTrade()
    {
        if (inTradeId == null)
        {
            return "No trade invite pending.";
        }
        if (trade != null)
        {
            return "You already have a trade open.";
        }
        trade = new TradeSession();
        trade.tradeId = inTradeId;
        trade.partnerId = inTradeFromId;
        trade.partnerName = inTradeFromName;
        trade.partnerOpened = true;
        clearIncomingTrade();
        sendTradeUpdate();
        refreshPanel();
        return null;
    }

    public synchronized void declineIncomingTrade()
    {
        if (inTradeId != null && partyService.isInParty())
        {
            partyService.send(new com.osrsgo.party.TradeCancelMsg(inTradeId));
        }
        clearIncomingTrade();
        refreshPanel();
    }

    /** Adds or removes one of my mons from the offer; resets acceptances. */
    public synchronized String tradeToggleOffer(OwnedMon mon)
    {
        if (trade == null || mon == null)
        {
            return null;
        }
        boolean removed = trade.myOffer.remove(mon);
        if (!removed)
        {
            if (trade.myOffer.size() >= TRADE_OFFER_CAP)
            {
                return "Offer cap is " + TRADE_OFFER_CAP + " mons.";
            }
            if (!profile.mons.contains(mon))
            {
                return null;
            }
            trade.myOffer.add(mon);
        }
        trade.myAccepted = false;
        trade.theirAccepted = false;
        sendTradeUpdate();
        refreshPanel();
        return null;
    }

    /** Marks my side accepted; the trade commits once both sides accept. */
    public synchronized void tradeAccept()
    {
        if (trade == null || trade.myAccepted)
        {
            return;
        }
        trade.myAccepted = true;
        sendTradeUpdate();
        maybeCommitTrade();
        refreshPanel();
    }

    public synchronized void cancelTrade()
    {
        if (trade != null && partyService.isInParty())
        {
            partyService.send(new com.osrsgo.party.TradeCancelMsg(trade.tradeId));
        }
        trade = null;
        refreshPanel();
    }

    private void sendTradeUpdate()
    {
        if (trade == null || !partyService.isInParty())
        {
            return;
        }
        partyService.send(new com.osrsgo.party.TradeUpdateMsg(trade.tradeId,
            gson.toJson(trade.myOffer), trade.myAccepted));
    }

    private String partyMemberName(long memberId)
    {
        for (net.runelite.client.party.PartyMember member : partyService.getMembers())
        {
            if (member.getMemberId() == memberId)
            {
                return member.getDisplayName();
            }
        }
        return "partner";
    }

    private void maybeCommitTrade()
    {
        if (trade == null || !trade.myAccepted || !trade.theirAccepted)
        {
            return;
        }
        TradeSession done = trade;
        trade = null;
        // Everything I offered must still be mine; otherwise abort loudly
        for (OwnedMon mine : done.myOffer)
        {
            if (!profile.mons.contains(mine))
            {
                partyService.send(new com.osrsgo.party.TradeCancelMsg(done.tradeId));
                chatAlways("Gielinor Safari: trade aborted, an offered mon was no longer available.");
                refreshPanel();
                return;
            }
        }
        StringBuilder gave = new StringBuilder();
        for (OwnedMon mine : done.myOffer)
        {
            if (gave.length() > 0)
            {
                gave.append(", ");
            }
            gave.append(mine.shiny ? "SHINY " : "").append(mine.displayName()).append(" lvl ").append(mine.level);
            removeMonAt(profile.mons.indexOf(mine));
        }
        StringBuilder got = new StringBuilder();
        for (OwnedMon theirs : done.theirOffer)
        {
            if (got.length() > 0)
            {
                got.append(", ");
            }
            got.append(theirs.shiny ? "SHINY " : "").append(theirs.displayName()).append(" lvl ").append(theirs.level);
            profile.mons.add(theirs);
            profile.seenSpecies.add(theirs.speciesId);
            profile.caughtSpecies.add(theirs.speciesId);
            profile.stats.trades++;
        }
        saveSoon();
        chatMessage("Gielinor Safari: TRADE COMPLETE with " + done.partnerName + "! Gave: "
            + (gave.length() > 0 ? gave : "nothing") + ". Received: "
            + (got.length() > 0 ? got : "nothing") + ".");
        refreshPanel();
    }

    @Subscribe
    public synchronized void onTradeOfferMsg(com.osrsgo.party.TradeOfferMsg msg)
    {
        if (isEcho(msg.getMemberId()) || localMemberId() != msg.getTargetId())
        {
            return;
        }
        inTradeId = msg.getTradeId();
        inTradeFromId = msg.getMemberId();
        inTradeFromName = msg.getOffererName();
        notifier.notify("Gielinor Safari: " + msg.getOffererName() + " wants to trade!");
        chatMessage("Gielinor Safari: " + msg.getOffererName() + " wants to trade! Check the Battle tab.");
        refreshPanel();
    }

    @Subscribe
    public synchronized void onTradeUpdateMsg(com.osrsgo.party.TradeUpdateMsg msg)
    {
        if (isEcho(msg.getMemberId()) || trade == null || !trade.tradeId.equals(msg.getTradeId())
            || msg.getMemberId() != trade.partnerId)
        {
            return;
        }
        trade.partnerOpened = true;
        List<OwnedMon> offer = validatedTradeMons(msg.getOfferJson());
        if (offer == null)
        {
            cancelTrade();
            panelToast("Their offer failed validation; trade cancelled.");
            return;
        }
        if (!gson.toJson(offer).equals(gson.toJson(trade.theirOffer)))
        {
            // Their offer changed: my acceptance no longer covers it
            trade.myAccepted = false;
        }
        trade.theirOffer = offer;
        trade.theirAccepted = msg.isAccepted();
        maybeCommitTrade();
        refreshPanel();
    }

    @Subscribe
    public synchronized void onTradeCancelMsg(com.osrsgo.party.TradeCancelMsg msg)
    {
        if (isEcho(msg.getMemberId()))
        {
            return;
        }
        if (inTradeId != null && inTradeId.equals(msg.getTradeId()))
        {
            clearIncomingTrade();
            panelToast("Trade invite withdrawn.");
            refreshPanel();
        }
        if (trade != null && trade.tradeId.equals(msg.getTradeId()))
        {
            trade = null;
            panelToast("Trade cancelled.");
            chatMessage("Gielinor Safari: the trade was cancelled.");
            refreshPanel();
        }
    }

    private void clearIncomingTrade()
    {
        inTradeId = null;
        inTradeFromId = -1;
        inTradeFromName = null;
    }

    /** Validates a whole offered list; null when anything in it is bogus. */
    private List<OwnedMon> validatedTradeMons(String json)
    {
        try
        {
            if (json == null || json.isEmpty())
            {
                return new ArrayList<>();
            }
            OwnedMon[] raw = gson.fromJson(json, OwnedMon[].class);
            if (raw == null || raw.length > TRADE_OFFER_CAP)
            {
                return null;
            }
            List<OwnedMon> result = new ArrayList<>();
            for (OwnedMon mon : raw)
            {
                OwnedMon valid = validatedTradeMon(gson.toJson(mon));
                if (valid == null)
                {
                    return null;
                }
                result.add(valid);
            }
            return result;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private OwnedMon validatedTradeMon(String json)
    {
        try
        {
            OwnedMon mon = gson.fromJson(json, OwnedMon.class);
            if (mon == null || !com.osrsgo.data.SpeciesData.exists(mon.speciesId)
                || mon.level < 1 || mon.level > 99)
            {
                return null;
            }
            mon.ivHp = Math.max(0, Math.min(15, mon.ivHp));
            mon.ivAtk = Math.max(0, Math.min(15, mon.ivAtk));
            mon.ivDef = Math.max(0, Math.min(15, mon.ivDef));
            mon.ivSpd = Math.max(0, Math.min(15, mon.ivSpd));
            mon.xp = Math.max(0, mon.xp);
            if (mon.hp > 0)
            {
                mon.hp = Math.min(mon.hp, mon.maxHp());
            }
            if (mon.nickname != null)
            {
                String cleaned = mon.nickname.trim().replaceAll("[^A-Za-z0-9 '\\-]", "");
                mon.nickname = cleaned.isEmpty() ? null : cleaned.substring(0, Math.min(16, cleaned.length()));
            }
            return mon;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** Removes a mon and fixes every index-based reference (teams, buddy). */
    private void removeMonAt(int monIndex)
    {
        profile.mons.remove(monIndex);
        profile.teamIndices = fixIndexList(profile.teamIndices, monIndex);
        for (int slot = 0; slot < profile.savedTeams.size(); slot++)
        {
            profile.savedTeams.set(slot, fixIndexList(profile.savedTeams.get(slot), monIndex));
        }
        if (profile.buddyIndex != null)
        {
            if (profile.buddyIndex == monIndex)
            {
                profile.buddyIndex = null;
            }
            else if (profile.buddyIndex > monIndex)
            {
                profile.buddyIndex--;
            }
        }
    }

    private static List<Integer> fixIndexList(List<Integer> indices, int removed)
    {
        List<Integer> rebuilt = new ArrayList<>();
        for (Integer idx : indices)
        {
            if (idx == removed)
            {
                continue;
            }
            rebuilt.add(idx > removed ? idx - 1 : idx);
        }
        return rebuilt;
    }

    /** Swaps to another team loadout, stashing the current one in its slot. */
    public synchronized void switchTeamSlot(int slot)
    {
        if (slot < 0 || slot > 2 || slot == profile.activeTeamSlot)
        {
            return;
        }
        profile.savedTeams.set(profile.activeTeamSlot, new ArrayList<>(profile.teamIndices));
        List<Integer> loaded = new ArrayList<>();
        for (Integer idx : profile.savedTeams.get(slot))
        {
            if (idx != null && idx >= 0 && idx < profile.mons.size())
            {
                loaded.add(idx);
            }
        }
        profile.teamIndices = loaded;
        profile.activeTeamSlot = slot;
        saveSoon();
        refreshPanel();
    }

    // ------------------------------------------------------------------ heal centers and breeding

    // POH pools double as Heal Centers; tracked via object spawn events
    private static final java.util.Set<Integer> POH_POOL_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
        net.runelite.api.ObjectID.POOL_OF_RESTORATION,
        net.runelite.api.ObjectID.POOL_OF_REVITALISATION,
        net.runelite.api.ObjectID.POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.FANCY_POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.ORNATE_POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.POOL_OF_REFRESHMENT,
        net.runelite.api.ObjectID.FROZEN_POOL_OF_RESTORATION,
        net.runelite.api.ObjectID.FROZEN_POOL_OF_REVITALISATION,
        net.runelite.api.ObjectID.FROZEN_POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.FROZEN_FANCY_POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.FROZEN_ORNATE_POOL_OF_REJUVENATION,
        net.runelite.api.ObjectID.FIERY_ORNATE_POOL_OF_REJUVENATION));

    private volatile boolean pohPoolInScene;

    @Subscribe
    public void onGameObjectSpawned(net.runelite.api.events.GameObjectSpawned event)
    {
        if (POH_POOL_IDS.contains(event.getGameObject().getId()))
        {
            pohPoolInScene = true;
        }
    }

    public boolean isNearBank()
    {
        return com.osrsgo.data.Places.nearBank(playerLocation)
            || (pohPoolInScene && client.isInInstancedRegion());
    }

    public boolean isNearGeneralStore()
    {
        return com.osrsgo.data.Places.nearGeneralStore(playerLocation);
    }

    // Which ball the panel/right-click throws; panel keeps it in sync
    private volatile int selectedBallTier;

    public int getSelectedBallTier()
    {
        return selectedBallTier;
    }

    public void setSelectedBallTier(int tier)
    {
        selectedBallTier = Math.max(0, Math.min(4, tier));
    }

    /** Ball exchange, general stores only: 50 G to Great, 10 Great to Super, 10 Super to Master. */
    public synchronized String exchangeBalls(int targetTier)
    {
        if (!isNearGeneralStore())
        {
            return "Ball exchanges only happen at a general store.";
        }
        switch (targetTier)
        {
            case 1:
                if (profile.balls < 25)
                {
                    return "Need 25 Gielinor Balls (have " + profile.balls + ").";
                }
                profile.balls -= 25;
                profile.greatBalls++;
                break;
            case 2:
                if (profile.greatBalls < 25)
                {
                    return "Need 25 Great Balls (have " + profile.greatBalls + ").";
                }
                profile.greatBalls -= 25;
                profile.superBalls++;
                break;
            case 3:
                if (profile.superBalls < 25)
                {
                    return "Need 25 Super Balls (have " + profile.superBalls + ").";
                }
                profile.superBalls -= 25;
                profile.ultraBalls++;
                break;
            case 4:
                if (profile.ultraBalls < masterCost())
                {
                    return "Need " + masterCost() + " Ultra Balls (have " + profile.ultraBalls + ").";
                }
                profile.ultraBalls -= masterCost();
                profile.masterBalls++;
                break;
            default:
                return "Unknown exchange.";
        }
        saveSoon();
        String[] names = {"", "Great Ball", "Super Ball", "Ultra Ball", "Master Ball"};
        chatMessage("Gielinor Safari: the shopkeeper hands you a " + names[targetTier] + "!");
        refreshPanel();
        return null;
    }

    public boolean isNearBreedingDen()
    {
        return com.osrsgo.data.Places.nearBreedingDen(playerLocation);
    }

    /** Trainer level 8 perk: walking into bank (or POH pool) range heals everything, no click. */
    private static final int AUTO_HEAL_LEVEL = 8;

    private void autoHealAtBank()
    {
        if (profile.trainerLevel() < AUTO_HEAL_LEVEL || session != null || !isNearBank())
        {
            return;
        }
        boolean hurt = false;
        for (OwnedMon m : profile.mons)
        {
            if (m.currentHp() < m.maxHp())
            {
                hurt = true;
                break;
            }
        }
        if (!hurt)
        {
            return;
        }
        for (OwnedMon m : profile.mons)
        {
            m.healFull();
        }
        saveSoon();
        chatMessage("Gielinor Safari: the bank's calm restores your mons to full health. (Auto-heal perk)");
        refreshPanel();
    }

    public synchronized String healTeam()
    {
        if (session != null)
        {
            return "No healing mid-battle! Finish the fight first.";
        }
        if (!isNearBank())
        {
            return "Visit any major bank (or your POH pool) to heal your mons.";
        }
        for (OwnedMon m : profile.mons)
        {
            m.healFull();
        }
        saveSoon();
        chatMessage("Gielinor Safari: your mons are restored to full health!");
        refreshPanel();
        return null;
    }

    /** Trainer level 70 perk: Gertrude's den works from anywhere. */
    public boolean hasRemoteBreeding()
    {
        return profile.trainerLevel() >= 70;
    }

    /** Breeds two owned mons at Gertrude's den into a hidden egg. */
    public synchronized String breed(int parentA, int parentB)
    {
        if (!isNearBreedingDen() && !hasRemoteBreeding())
        {
            return "Visit Gertrude's house in west Varrock to breed (or reach trainer level 70).";
        }
        if (parentA == parentB || parentA < 0 || parentB < 0
            || parentA >= profile.mons.size() || parentB >= profile.mons.size())
        {
            return "Pick two different parents.";
        }
        if (profile.eggs.size() >= eggCap())
        {
            return "You already carry " + eggCap() + " eggs. Hatch some first!";
        }
        OwnedMon a = profile.mons.get(parentA);
        OwnedMon b = profile.mons.get(parentB);

        com.osrsgo.model.Egg egg = new com.osrsgo.model.Egg();
        OwnedMon primary = rng.nextBoolean() ? a : b;
        OwnedMon other = primary == a ? b : a;
        // Epics and Legendaries CAN pass down now, at super rare odds; a failed
        // gate falls back to the other parent, and if both fail the egg turns
        // out ordinary
        if (passesBreedGate(primary))
        {
            egg.speciesId = primary.speciesId;
        }
        else if (passesBreedGate(other))
        {
            egg.speciesId = other.speciesId;
        }
        else
        {
            List<com.osrsgo.model.Species> commons = new ArrayList<>();
            for (com.osrsgo.model.Species sp : com.osrsgo.data.SpeciesData.all())
            {
                if (sp.getRarity() == Rarity.COMMON)
                {
                    commons.add(sp);
                }
            }
            egg.speciesId = commons.get(rng.nextInt(commons.size())).getId();
        }
        egg.ivHp = inheritIv(a.ivHp, b.ivHp);
        egg.ivAtk = inheritIv(a.ivAtk, b.ivAtk);
        egg.ivDef = inheritIv(a.ivDef, b.ivDef);
        egg.ivSpd = inheritIv(a.ivSpd, b.ivSpd);
        // The endgame chase: a flawless egg hatches with perfect IVs
        if (rng.nextDouble() < flawlessChance())
        {
            egg.ivHp = 15;
            egg.ivAtk = 15;
            egg.ivDef = 15;
            egg.ivSpd = 15;
        }
        egg.shiny = rng.nextInt(eggShinyDenominator()) == 0;
        switch (com.osrsgo.data.SpeciesData.byId(egg.speciesId).getRarity())
        {
            case UNCOMMON:
                egg.tilesRequired = 700;
                break;
            case RARE:
                egg.tilesRequired = 1200;
                break;
            case EPIC:
                egg.tilesRequired = 2000;
                break;
            case LEGENDARY:
                egg.tilesRequired = 3000;
                break;
            default:
                egg.tilesRequired = 400;
                break;
        }
        egg.createdAt = System.currentTimeMillis();
        profile.eggs.add(egg);
        profile.stats.breeds++;
        saveSoon();
        chatMessage("Gielinor Safari: Gertrude tucks a mystery egg into your pack. Walk "
            + egg.tilesRequired + " tiles to hatch it!");
        refreshPanel();
        return null;
    }

    /** Common through Rare always pass down; Epic 25%, Legendary 8%. */
    private boolean passesBreedGate(OwnedMon parent)
    {
        switch (parent.species().getRarity())
        {
            case EPIC:
                return rng.nextDouble() < 0.25;
            case LEGENDARY:
                return rng.nextDouble() < 0.08;
            default:
                return true;
        }
    }

    /** Child IVs draw from the parents with a chance to improve: the point of breeding. */
    private int inheritIv(int ivA, int ivB)
    {
        int base = rng.nextBoolean() ? ivA : ivB;
        if (rng.nextDouble() < 0.25)
        {
            base += 1 + rng.nextInt(3);
        }
        return Math.min(15, base);
    }

    // ------------------------------------------------------------------ catching

    /**
     * Starts a catch attempt: the outcome is rolled now, the overlay animates
     * the ball throw, and the result is applied when the sequence finishes.
     * Returns an error message, or null if the ball is in the air.
     */
    public synchronized String attemptCatch(String spawnKey, boolean useBerry)
    {
        return attemptCatch(spawnKey, useBerry, 0);
    }

    /** Ball tiers: 0 Gielinor, 1 Great (+12%), 2 Super (+25%), 3 Master (guaranteed, Rare+ only). */
    public synchronized String attemptCatch(String spawnKey, boolean useBerry, int ballTier)
    {
        if (activeCatch != null)
        {
            return "A ball is already in the air!";
        }
        WildSpawn spawn = spawnManager.byKey(spawnKey);
        WorldPoint player = playerLocation;
        if (spawn == null)
        {
            return "That mon is gone.";
        }
        if (player == null || !spawnManager.inCatchRange(player, spawn, catchRange()))
        {
            return "Too far away. Get within " + catchRange() + " tiles.";
        }
        if (useBerry && profile.berries <= 0)
        {
            return "No berries left! Walk around to find more.";
        }
        Rarity spawnRarity = spawn.species().getRarity();
        switch (ballTier)
        {
            case 1:
                if (profile.greatBalls <= 0)
                {
                    return "No Great Balls. Exchange 25 Gielinor Balls at a general store.";
                }
                profile.greatBalls--;
                break;
            case 2:
                if (profile.superBalls <= 0)
                {
                    return "No Super Balls. Exchange 25 Great Balls at a general store.";
                }
                profile.superBalls--;
                break;
            case 3:
                if (profile.ultraBalls <= 0)
                {
                    return "No Ultra Balls. Exchange 25 Super Balls at a general store.";
                }
                profile.ultraBalls--;
                break;
            case 4:
                if (profile.masterBalls <= 0)
                {
                    return "No Master Balls. Gold medals grant them.";
                }
                if (spawnRarity != Rarity.RARE && spawnRarity != Rarity.EPIC && spawnRarity != Rarity.LEGENDARY)
                {
                    return "A " + spawn.species().getName() + " isn't worth a Master Ball!";
                }
                profile.masterBalls--;
                break;
            default:
                if (profile.balls <= 0)
                {
                    return "Out of Gielinor Balls! Walk around to find more.";
                }
                profile.balls--;
                break;
        }
        profile.stats.ballsThrown++;
        researchEvent(com.osrsgo.data.ResearchData.Kind.THROW_BALLS, 1);
        if (useBerry)
        {
            profile.berries--;
            profile.stats.berriesUsed++;
        }

        double tierBonus;
        switch (ballTier)
        {
            case 1: tierBonus = 0.10; break;
            case 2: tierBonus = 0.20; break;
            case 3: tierBonus = 0.35; break;
            default: tierBonus = 0; break;
        }
        double chance = ballTier == 4 ? 1.0
            : spawn.species().getRarity().getBaseCatchChance()
            + trainerCatchBonus()
            + (useBerry ? berryBonus() : 0)
            + gymCatchBonus()
            + tierBonus;
        CatchSequence.Outcome outcome;
        OwnedMon mon = null;
        if (rng.nextDouble() < chance)
        {
            outcome = CatchSequence.Outcome.CAUGHT;
            mon = new OwnedMon();
            mon.speciesId = spawn.speciesId;
            mon.level = spawn.level;
            mon.ivHp = rng.nextInt(16);
            mon.ivAtk = rng.nextInt(16);
            mon.ivDef = rng.nextInt(16);
            mon.ivSpd = rng.nextInt(16);
            mon.caughtAt = System.currentTimeMillis();
            mon.caughtNear = "(" + spawn.location.getX() + ", " + spawn.location.getY() + ")";
            mon.shiny = spawn.shiny;
        }
        else
        {
            outcome = rng.nextDouble() < 0.35 ? CatchSequence.Outcome.FLED : CatchSequence.Outcome.BROKE_FREE;
        }
        activeCatch = new CatchSequence(spawn, outcome, mon, ballTier);
        refreshPanel();
        return null;
    }

    /** Your real odds for this spawn with the selected ball, for display. */
    public double catchChanceFor(WildSpawn spawn)
    {
        int tier = getSelectedBallTier();
        if (tier == 4)
        {
            return 1.0;
        }
        double tierBonus = tier == 1 ? 0.10 : (tier == 2 ? 0.20 : (tier == 3 ? 0.35 : 0));
        return Math.min(0.99, spawn.species().getRarity().getBaseCatchChance()
            + trainerCatchBonus() + gymCatchBonus() + tierBonus);
    }

    public CatchSequence getActiveCatch()
    {
        return activeCatch;
    }

    private synchronized void finalizeCatch()
    {
        CatchSequence seq = activeCatch;
        if (seq != null && RAID_CATCH_KEY.equals(seq.spawn.key))
        {
            activeCatch = null;
            finalizeRaidThrow(seq);
            return;
        }
        if (seq != null && BOSS_CATCH_KEY.equals(seq.spawn.key))
        {
            activeCatch = null;
            finalizeBossKillThrow(seq);
            return;
        }
        if (seq == null)
        {
            return;
        }
        activeCatch = null;
        String name = seq.spawn.species().getName();
        switch (seq.outcome)
        {
            case CAUGHT:
                profile.mons.add(seq.pendingMon);
                boolean newSpecies = !profile.caughtSpecies.contains(seq.spawn.speciesId);
                int xp = catchXp(seq.spawn.species().getRarity());
                if (SpawnManager.isCommunityHour())
                {
                    xp *= 2;
                }
                if (seq.spawn.shiny)
                {
                    xp *= 4;
                }
                if (newSpecies)
                {
                    xp += 100;
                }
                profile.trainerXp += xp;
                profile.caughtSpecies.add(seq.spawn.speciesId);
                profile.stats.catches++;
                if (seq.spawn.shiny)
                {
                    profile.stats.shinyCatches++;
                }
                profile.addEssence(seq.spawn.speciesId, catchEssence(seq.spawn.species().getRarity()));
                researchEvent(com.osrsgo.data.ResearchData.Kind.CATCH_ANY, 1);
                researchEvent(com.osrsgo.data.ResearchData.Kind.CATCH_TYPE,
                    seq.spawn.species().getType() == currentResearchType() ? 1 : 0);
                if (seq.spawn.species().getRarity() != Rarity.COMMON)
                {
                    researchEvent(com.osrsgo.data.ResearchData.Kind.CATCH_UNCOMMON_PLUS, 1);
                }
                saveSoon();
                spawnManager.remove(seq.spawn);
                chatMessage("Gotcha! " + (seq.spawn.shiny ? "SHINY " : "") + name
                    + " (lvl " + seq.spawn.level + ") was caught! +" + xp + " trainer xp"
                    + (newSpecies ? " (new GielDex entry!)" : "") + ".");
                break;
            case FLED:
                spawnManager.remove(seq.spawn);
                profile.stats.flees++;
                chatMessage("Oh no! The wild " + name + " fled!");
                break;
            default:
                seq.spawn.failedAttempts++;
                profile.stats.breakFrees++;
                chatMessage("The wild " + name + " broke free! Try again.");
                break;
        }
        saveSoon();
        refreshPanel();
    }

    private static int catchXp(Rarity rarity)
    {
        switch (rarity)
        {
            case UNCOMMON: return 35;
            case RARE: return 60;
            case EPIC: return 100;
            case LEGENDARY: return 200;
            default: return 20;
        }
    }

    // ------------------------------------------------------------------ collection management

    public synchronized void toggleTeamMember(int monIndex)
    {
        Integer boxed = monIndex;
        if (profile.teamIndices.contains(boxed))
        {
            profile.teamIndices.remove(boxed);
        }
        else if (profile.teamIndices.size() < teamCap())
        {
            profile.teamIndices.add(boxed);
        }
        saveSoon();
        refreshPanel();
    }

    public java.util.Set<Integer> getHuntTargets()
    {
        return profile.huntTargets;
    }

    public synchronized void setHuntTargets(java.util.Set<Integer> targets)
    {
        profile.huntTargets.clear();
        profile.huntTargets.addAll(targets);
        saveSoon();
        refreshPanel();
    }

    /** Moves a team member up (-1) or down (+1) in battle order. */
    public synchronized void moveTeamMember(int monIndex, int delta)
    {
        int pos = profile.teamIndices.indexOf(monIndex);
        int newPos = pos + delta;
        if (pos < 0 || newPos < 0 || newPos >= profile.teamIndices.size())
        {
            return;
        }
        java.util.Collections.swap(profile.teamIndices, pos, newPos);
        saveSoon();
        refreshPanel();
    }

    public List<SpawnManager.Candy> getNearbyCandies()
    {
        return nearbyCandies;
    }

    /** Rare Candy: +1 level, any mon, no questions asked. */
    public synchronized String useRareCandy(int monIndex)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return "Pick a mon first.";
        }
        if (profile.rareCandies <= 0)
        {
            return "No Rare Candies. Purple sweets appear rarely in the world; walk onto one!";
        }
        OwnedMon mon = profile.mons.get(monIndex);
        if (mon.level >= 99)
        {
            return mon.name() + " is already level 99.";
        }
        profile.rareCandies--;
        mon.level++;
        saveSoon();
        chatMessage("Gielinor Safari: " + mon.name() + " grew to level " + mon.level + " (Rare Candy)!");
        refreshPanel();
        return null;
    }

    public static final int BERRIES_PER_CANDY = 50;

    /**
     * Berries pile up faster than they get thrown, so they buy Rare Candies
     * at a deliberately steep rate: a slow sink, not a shortcut to level 99.
     */
    public synchronized String exchangeBerriesForCandy()
    {
        if (!isNearGeneralStore())
        {
            return "Berry exchanges only happen at a general store.";
        }
        if (profile.berries < BERRIES_PER_CANDY)
        {
            return "Need " + BERRIES_PER_CANDY + " berries (have " + profile.berries + ").";
        }
        profile.berries -= BERRIES_PER_CANDY;
        profile.rareCandies++;
        saveSoon();
        chatMessage("Gielinor Safari: traded " + BERRIES_PER_CANDY + " berries for a Rare Candy! ("
            + profile.rareCandies + " held, " + profile.berries + " berries left)");
        refreshPanel();
        return null;
    }

    /**
     * Species with no evolution turn their essence into levels instead:
     * reaching level N costs N essence of that species.
     */
    public synchronized String levelWithEssence(int monIndex)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return "Pick a mon first.";
        }
        OwnedMon mon = profile.mons.get(monIndex);
        if (com.osrsgo.data.EvolutionData.of(mon.speciesId) != null)
        {
            return mon.species().getName() + " uses its essence to evolve instead.";
        }
        if (mon.level >= 99)
        {
            return mon.name() + " is already level 99.";
        }
        int cost = mon.level + 1;
        if (!spendEssence(mon.speciesId, cost))
        {
            return "Needs " + cost + " " + tierName(mon.speciesId) + " essence ("
                + profile.essenceOf(mon.speciesId) + " held + " + profile.shinyEssence + " shiny).";
        }
        mon.level++;
        saveSoon();
        chatMessage("Gielinor Safari: " + mon.name() + " grew to level " + mon.level
            + " (" + cost + " essence)!");
        refreshPanel();
        return null;
    }

    public synchronized void toggleFavorite(int monIndex)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return;
        }
        OwnedMon mon = profile.mons.get(monIndex);
        mon.favorite = !mon.favorite;
        saveSoon();
        refreshPanel();
    }

    /**
     * Releases a hand-picked set of mons in one pass. Protections are
     * re-checked here regardless of what the UI offered: favorites, team,
     * buddy, and shinies never bulk-release.
     */
    public synchronized int releaseMons(java.util.Collection<OwnedMon> toRelease)
    {
        int released = 0;
        int totalEssence = 0;
        for (OwnedMon mon : toRelease)
        {
            int idx = profile.mons.indexOf(mon);
            if (idx < 0)
            {
                continue;
            }
            boolean onTeam = profile.teamIndices.contains(idx);
            boolean isBuddy = profile.buddyIndex != null && profile.buddyIndex == idx;
            if (mon.favorite || onTeam || isBuddy || mon.shiny)
            {
                continue;
            }
            int ess = releaseEssence(mon);
            totalEssence += ess;
            profile.addEssence(mon.speciesId, ess);
            removeMonAt(idx);
            released++;
        }
        if (released > 0)
        {
            saveSoon();
            chatMessage("Gielinor Safari: released " + released + " mon" + (released == 1 ? "" : "s")
                + " for " + totalEssence + " total essence!");
        }
        refreshPanel();
        return released;
    }

    /** Same protections as releaseNonFavorites, limited to one species. */
    public synchronized int releaseAllOfSpecies(int speciesId)
    {
        int released = 0;
        int totalEssence = 0;
        for (int i = profile.mons.size() - 1; i >= 0; i--)
        {
            OwnedMon mon = profile.mons.get(i);
            boolean onTeam = profile.teamIndices.contains(i);
            boolean isBuddy = profile.buddyIndex != null && profile.buddyIndex == i;
            if (mon.speciesId != speciesId || mon.favorite || onTeam || isBuddy || mon.shiny)
            {
                // Shinies never bulk-release; let them go one at a time, on purpose
                continue;
            }
            totalEssence += releaseEssence(mon);
            profile.addEssence(mon.speciesId, releaseEssence(mon));
            removeMonAt(i);
            released++;
        }
        if (released > 0)
        {
            saveSoon();
            chatMessage("Gielinor Safari: released " + released + " "
                + com.osrsgo.data.SpeciesData.byId(speciesId).getName() + (released == 1 ? "" : "s")
                + " for " + totalEssence + " essence!");
        }
        refreshPanel();
        return released;
    }

    public synchronized void releaseMon(int monIndex)
    {
        if (monIndex < 0 || monIndex >= profile.mons.size())
        {
            return;
        }
        OwnedMon releasing = profile.mons.get(monIndex);
        int speciesId = releasing.speciesId;
        int essence = releaseEssence(releasing);
        boolean shiny = releasing.shiny;
        removeMonAt(monIndex);
        if (shiny)
        {
            profile.shinyEssence += essence;
            chatAlways("Gielinor Safari: released a SHINY! +" + essence
                + " SHINY essence (works for any species).");
        }
        else
        {
            profile.addEssence(speciesId, essence);
            chatMessage("Gielinor Safari: released. +" + essence + " "
                + tierName(speciesId) + " essence.");
        }
        saveSoon();
        refreshPanel();
    }

    /** The rarity-tier label an essence cost is paid in ("Rare", "Epic"...). */
    private String tierName(int speciesId)
    {
        return com.osrsgo.data.SpeciesData.byId(speciesId).getRarity().getDisplay();
    }

    /**
     * Spends an essence cost for a species: its rarity tier's pool first, then
     * SHINY essence covers any remainder. Returns false (spending nothing)
     * when the two together can't cover it.
     */
    private boolean spendEssence(int speciesId, int cost)
    {
        int have = profile.essenceOf(speciesId);
        int fromSpecies = Math.min(have, cost);
        int fromShiny = cost - fromSpecies;
        if (fromShiny > profile.shinyEssence)
        {
            return false;
        }
        if (fromSpecies > 0)
        {
            profile.addEssence(speciesId, -fromSpecies);
        }
        if (fromShiny > 0)
        {
            profile.shinyEssence -= fromShiny;
            chatMessage("Gielinor Safari: " + fromShiny + " SHINY essence made up the difference.");
        }
        return true;
    }

    /** A finished battle no longer blocks starting the next one. */
    private void autoCloseFinishedBattle()
    {
        if (session != null && session.finished)
        {
            session = null;
            battleTeamRefs = Collections.emptyList();
        }
    }

    /** Team members that can actually fight; fainted mons stay benched until healed. */
    private List<OwnedMon> battleReadyTeam()
    {
        List<OwnedMon> ready = new ArrayList<>();
        for (OwnedMon m : profile.team())
        {
            if (!m.isFainted())
            {
                ready.add(m);
            }
        }
        return ready;
    }

    private String teamError()
    {
        if (profile.team().isEmpty())
        {
            return "Pick a team first in the Dex tab.";
        }
        return "Your whole team has fainted! Heal at any bank.";
    }

    /** Writes battle damage back onto the owned mons after a battle ends. */
    private void syncTeamHp()
    {
        if (session == null)
        {
            return;
        }
        for (int i = 0; i < session.myTeam.size() && i < battleTeamRefs.size(); i++)
        {
            battleTeamRefs.get(i).hp = Math.max(0, session.myTeam.get(i).hp);
        }
    }

    /**
     * Merges another profile into the live one: mons append, sets union,
     * currencies and xp take the max. Used by the backup restore command.
     */
    private synchronized int mergeProfile(PlayerProfile other)
    {
        if (other == null)
        {
            return -1;
        }
        int added = 0;
        if (other.mons != null)
        {
            for (OwnedMon m : other.mons)
            {
                profile.mons.add(m);
                profile.seenSpecies.add(m.speciesId);
                profile.caughtSpecies.add(m.speciesId);
                added++;
            }
        }
        if (other.badges != null)
        {
            profile.badges.addAll(other.badges);
        }
        if (other.seenSpecies != null)
        {
            profile.seenSpecies.addAll(other.seenSpecies);
        }
        if (other.caughtSpecies != null)
        {
            profile.caughtSpecies.addAll(other.caughtSpecies);
        }
        if (other.earnedMedals != null)
        {
            profile.earnedMedals.addAll(other.earnedMedals);
        }
        profile.trainerXp = Math.max(profile.trainerXp, other.trainerXp);
        profile.balls = Math.max(profile.balls, other.balls);
        profile.greatBalls = Math.max(profile.greatBalls, other.greatBalls);
        profile.superBalls = Math.max(profile.superBalls, other.superBalls);
        profile.ultraBalls = Math.max(profile.ultraBalls, other.ultraBalls);
        profile.masterBalls = Math.max(profile.masterBalls, other.masterBalls);
        profile.berries = Math.max(profile.berries, other.berries);
        profile.rareCandies = Math.max(profile.rareCandies, other.rareCandies);
        profile.shinyEssence = Math.max(profile.shinyEssence, other.shinyEssence);
        profile.tilesWalked = Math.max(profile.tilesWalked, other.tilesWalked);
        // Fold the incoming profile's essence (either format) into its own tier
        // pools, then take the max per tier so re-imports can't inflate
        other.migrateEssence();
        profile.migrateEssence();
        // Keep whichever side holds more gyms; Gson defaults must not decide this
        if (other.gyms != null && !other.gyms.isEmpty())
        {
            boolean theirsBigger;
            if (myRsn == null)
            {
                // No RSN yet (an import performed at the login screen): heldBy
                // can't tell who owns what, so fall back to raw map size
                // rather than silently dropping the incoming gyms
                theirsBigger = other.gyms.size() > profile.gyms.size();
            }
            else
            {
                int mineHeld = com.osrsgo.gym.LocalGyms.heldBy(profile.gyms, myRsn);
                int theirsHeld = com.osrsgo.gym.LocalGyms.heldBy(other.gyms, myRsn);
                theirsBigger = theirsHeld > mineHeld;
            }
            if (theirsBigger)
            {
                profile.gyms = other.gyms;
            }
        }
        if (other.tierEssence != null)
        {
            for (java.util.Map.Entry<String, Integer> e : other.tierEssence.entrySet())
            {
                profile.tierEssence.merge(e.getKey(), e.getValue(), Math::max);
            }
        }
        saveSoon();
        refreshPanel();
        return added;
    }

    /**
     * Restores the automatic backup, which only ever grows: it is rewritten
     * only when the live profile has at least as many mons, so a wiped or
     * shrunken profile can never overwrite it. Merges rather than replaces,
     * so nothing caught since the backup is lost.
     */
    @Subscribe
    public void onCommandExecuted(net.runelite.api.events.CommandExecuted event)
    {
        if (!"gorestore".equalsIgnoreCase(event.getCommand()))
        {
            return;
        }
        int added = mergeProfile(profileStore.loadBackup());
        chatAlways(added < 0 ? "Gielinor Safari: no backup profile exists yet."
            : "Gielinor Safari: backup merged in (" + added + " mon"
                + (added == 1 ? "" : "s") + " added).");
    }

    // ------------------------------------------------------------------ gyms

    public boolean inGymRange(GymData.Gym gym)
    {
        WorldPoint player = playerLocation;
        return player != null
            && player.getPlane() == gym.location.getPlane()
            && player.distanceTo(gym.location) <= GYM_RANGE;
    }

    public synchronized String startGymBattle(String gymIdArg)
    {
        GymData.Gym gym = GymData.byId(gymIdArg);
        if (gym == null)
        {
            return "Unknown gym.";
        }
        autoCloseFinishedBattle();
        drainRaidThrows();
        if (session != null)
        {
            return "Already in a battle.";
        }
        if (!inGymRange(gym))
        {
            return "Walk to the gym mark first (within 10 tiles).";
        }
        List<OwnedMon> team = battleReadyTeam();
        if (team.isEmpty())
        {
            return teamError();
        }

        List<BattleMon> mine = new ArrayList<>();
        for (OwnedMon m : team)
        {
            mine.add(BattleMon.fromOwned(m));
        }

        GymHolder holder = getGymHolder(gym.id);
        boolean vsTrainer = holder != null && holder.isClaimed()
            && holder.holderTeam != null && !holder.holderTeam.isEmpty();
        if (vsTrainer && holder.isHeldBy(myRsn))
        {
            return "You already hold " + gym.name + ".";
        }
        if (vsTrainer && com.osrsgo.gym.LocalGyms.isAlly(profile.faction, holder,
            isPartyMemberName(holder.holderRsn)))
        {
            return gym.name + " is held by a fellow follower of " + profile.faction + ". Leave it be.";
        }

        // Enemy lineup: the holder's defender team first (if claimed), then the
        // NPC leader's team. Both must fall in one battle to take the gym.
        List<BattleMon> theirs = new ArrayList<>();
        int holderMons = 0;
        if (vsTrainer)
        {
            for (com.osrsgo.battle.MonSpec spec : holder.holderTeam)
            {
                if (com.osrsgo.battle.SpecValidator.valid(spec))
                {
                    theirs.add(BattleMon.fromSpec(spec));
                }
            }
            holderMons = theirs.size();
            if (holderMons == 0)
            {
                // Every defender spec failed validation; fall back to leader-only
                vsTrainer = false;
            }
        }
        for (int[] entry : gym.team)
        {
            theirs.add(BattleMon.fromSpecies(entry[0], entry[1]));
        }

        battleTeamRefs = team;
        session = new BattleSession(BattleSession.Mode.AI, "gym-" + gym.id, gym.leader, -1, gym.id, mine, theirs);
        if (holder != null)
        {
            session.gymExpectedUpdatedAt = holder.updatedAt;
        }
        if (vsTrainer)
        {
            session.gymHolderRsn = holder.holderRsn;
            session.gymHolderMonCount = holderMons;
            session.fullLog.add(holder.holderRsn + "'s " + holderMons
                + " defenders guard " + gym.name + ". Beat them, then " + gym.leader + ", to take it!");
            session.fullLog.add("Fall to " + gym.leader + " after breaking "
                + holder.holderRsn + "'s team and the gym goes unclaimed.");
        }
        else
        {
            session.fullLog.add(gym.leader + " accepts your challenge at " + gym.name + "!");
        }
        refreshPanel();
        return null;
    }

    // ------------------------------------------------------------------ pvp over party

    public synchronized String challenge(long targetId)
    {
        if (!partyService.isInParty() || partyService.getLocalMember() == null)
        {
            return "Join a RuneLite party first (Party plugin).";
        }
        autoCloseFinishedBattle();
        if (session != null)
        {
            return "Already in a battle.";
        }
        List<OwnedMon> team = battleReadyTeam();
        if (team.isEmpty())
        {
            return teamError();
        }
        long self = partyService.getLocalMember().getMemberId();
        outgoingBattleId = self + "-" + targetId + "-" + System.currentTimeMillis();
        outgoingTargetId = targetId;
        String teamJson = gson.toJson(specsOf(team));
        partyService.send(new BattleChallengeMsg(targetId, outgoingBattleId, localName(), teamJson));
        refreshPanel();
        return null;
    }

    public synchronized void acceptChallenge()
    {
        autoCloseFinishedBattle();
        if (pendingBattleId == null || session != null)
        {
            return;
        }
        List<OwnedMon> team = battleReadyTeam();
        if (team.isEmpty())
        {
            panelToast(teamError());
            return;
        }
        List<BattleMon> mine = new ArrayList<>();
        for (OwnedMon m : team)
        {
            mine.add(BattleMon.fromOwned(m));
        }
        List<BattleMon> theirs = specsToMons(pendingChallengerTeamJson);
        battleTeamRefs = team;
        session = new BattleSession(BattleSession.Mode.PVP_GUEST, pendingBattleId,
            pendingChallengerName, pendingChallengerId, null, mine, theirs);
        session.fullLog.add("Battle vs " + pendingChallengerName + " begins!");
        partyService.send(new BattleAcceptMsg(pendingBattleId, localName(), gson.toJson(specsOf(team))));
        clearPendingChallenge();
        refreshPanel();
    }

    public synchronized void declineChallenge()
    {
        if (pendingBattleId != null)
        {
            partyService.send(new BattleDeclineMsg(pendingBattleId));
            clearPendingChallenge();
            refreshPanel();
        }
    }

    /** A move button was clicked; index is into the active mon's moveset. */
    public synchronized void chooseMove(int moveIdx)
    {
        if (session == null || session.finished || session.myPendingMove != null)
        {
            return;
        }
        switch (session.mode)
        {
            case AI:
            {
                session.myPendingMove = moveIdx;
                session.resolveTurn(moveIdx, session.aiMove());
                // Cosmetics must never gate settlement: a throw out of the
                // scene once skipped finishBattle entirely, silently eating
                // gym claims, badges, and xp on won fights
                try
                {
                    battleScene.clash(session.myMon().lastHitTaken, session.oppMon().lastHitTaken);
                }
                catch (Exception e)
                {
                    log.warn("battle scene clash failed", e);
                }
                if (session.finished)
                {
                    finishBattle();
                }
                break;
            }
            case PVP_HOST:
            {
                session.myPendingMove = moveIdx;
                tryResolveHostTurn();
                break;
            }
            case PVP_GUEST:
            {
                session.myPendingMove = moveIdx;
                partyService.send(new BattleMoveMsg(session.battleId, session.turn, moveIdx));
                break;
            }
        }
        refreshPanel();
    }

    private void tryResolveHostTurn()
    {
        if (session == null || session.myPendingMove == null || session.oppPendingMove == null)
        {
            return;
        }
        int sentTurn = session.turn;
        List<String> log = session.resolveTurn(session.myPendingMove, session.oppPendingMove);
        try
        {
            battleScene.clash(session.myMon().lastHitTaken, session.oppMon().lastHitTaken);
        }
        catch (Exception e)
        {
            OsrsGoPlugin.log.warn("battle scene clash failed", e);
        }
        TurnState state = session.snapshot(log);
        partyService.send(new BattleTurnResultMsg(session.battleId, sentTurn, gson.toJson(state)));
        if (session.finished)
        {
            finishBattle();
        }
    }

    public synchronized void forfeit()
    {
        if (session == null)
        {
            return;
        }
        if (session.mode != BattleSession.Mode.AI && partyService.isInParty())
        {
            partyService.send(new BattleEndMsg(session.battleId, "forfeit"));
        }
        session.finished = true;
        session.wonByMe = false;
        session.fullLog.add("You forfeited the battle.");
        syncTeamHp();
        if (session.raidSpeciesId != null)
        {
            // Raid retreats carry no penalty beyond the damage taken
        }
        else if (session.gymId != null)
        {
            profile.stats.gymLosses++;
        }
        else
        {
            profile.stats.pvpLosses++;
        }
        saveSoon();
        refreshPanel();
    }

    public synchronized void closeBattle()
    {
        session = null;
        battleTeamRefs = Collections.emptyList();
        refreshPanel();
    }


    /** How a battle settled and what it triggered. Log only, never shown in game. */
    private void battleDiag(String line)
    {
        log.debug("battle end | {}", line);
    }

    private void finishBattle()
    {
        if (session == null)
        {
            return;
        }
        syncTeamHp();
        if (session.wonByMe)
        {
            battleScene.celebrate();
        }
        if (session.raidSpeciesId != null)
        {
            battleDiag(session.battleId + " " + (session.wonByMe ? "WON" : "LOST")
                + " [raid] -> throws roll; raids never claim gyms");
            // Win or lose, a finished raid fight burns this rotation's attempt
            if (session.battleId != null && session.battleId.startsWith("raid-"))
            {
                recordRaidAttempt(session.battleId.substring("raid-".length()));
            }
            if (session.wonByMe)
            {
                finishRaid();
            }
            saveSoon();
            refreshPanel();
            return;
        }
        battleDiag(session.battleId + " " + (session.wonByMe ? "WON" : "LOST")
            + (session.gymId != null
                ? (session.gymHolderRsn != null ? " [gym vs " + session.gymHolderRsn + "]" : " [gym, unclaimed]")
                : " [pvp]")
            + (session.wonByMe && session.gymId != null ? " -> claiming" : ""));
        if (session.gymId != null)
        {
            if (session.wonByMe)
            {
                profile.stats.gymWins++;
                researchEvent(com.osrsgo.data.ResearchData.Kind.WIN_GYM, 1);
            }
            else
            {
                profile.stats.gymLosses++;
            }
        }
        else if (session.wonByMe)
        {
            profile.stats.pvpWins++;
        }
        else
        {
            profile.stats.pvpLosses++;
        }
        if (session.wonByMe)
        {
            int oppLevels = 0;
            for (BattleMon m : session.oppTeam)
            {
                oppLevels += m.level;
            }
            int avgOpp = session.oppTeam.isEmpty() ? 1 : oppLevels / session.oppTeam.size();
            grantBattleMonXp(avgOpp);
            if (session.gymId != null)
            {
                GymData.Gym gym = GymData.byId(session.gymId);
                if (gym != null && !profile.badges.contains(gym.badge))
                {
                    profile.badges.add(gym.badge);
                    session.fullLog.add("You earned the " + gym.badge + "!");
                    chatMessage("Gielinor Safari: you earned the " + gym.badge + "!");
                }
                profile.trainerXp += 150;
                if (gym != null && config.gymControl())
                {
                    chatMessage("Gielinor Safari: gym win at " + gym.name + ", claiming it...");
                }
                claimGymAfterWin(gym);
            }
            else
            {
                profile.trainerXp += 100;
                chatMessage("Gielinor Safari: you won the battle against " + session.opponentName + "!");
            }
        }
        else if (session.gymHolderRsn != null && config.gymControl() && myRsn != null)
        {
            if (session.holderTeamDefeated())
            {
                breakGymAfterPartialWin();
            }
            else
            {
                com.osrsgo.gym.LocalGyms.recordDefense(profile.gyms, session.gymId);
                saveSoon();
            }
        }
        saveSoon();
    }

    /**
     * The challenger beat the holder's defender team but lost to the NPC
     * leader: the hold is broken and the gym reverts to unclaimed.
     */
    private void breakGymAfterPartialWin()
    {
        GymData.Gym gym = GymData.byId(session.gymId);
        if (gym == null)
        {
            return;
        }
        String holderRsn = session.gymHolderRsn;
        com.osrsgo.gym.LocalGyms.breakHold(profile.gyms, gym.id);
        saveSoon();
        chatMessage("Gielinor Safari: you crushed " + holderRsn + "'s defenders, but "
            + gym.leader + " stopped you. " + gym.name + " now stands unclaimed!");
        // Without this the holder's client keeps the gym and the two of you
        // disagree about it permanently
        broadcastGymBreak(gym.id);
        refreshPanel();
    }

    private void broadcastGymBreak(String gymId)
    {
        if (!partyService.isInParty())
        {
            return;
        }
        partyService.send(new com.osrsgo.party.GymBreakMsg(gymId));
    }

    private void broadcastGymClaim(String gymId)
    {
        GymHolder holder = profile.gyms.get(gymId);
        if (holder == null || !partyService.isInParty())
        {
            return;
        }
        partyService.send(new com.osrsgo.party.GymClaimMsg(gymId, holder.holderRsn,
            gson.toJson(holder.holderTeam), holder.holderFaction));
    }

    private void claimGymAfterWin(GymData.Gym gym)
    {
        if (gym == null || !config.gymControl() || myRsn == null || battleTeamRefs.isEmpty())
        {
            String why = gym == null ? "the gym was unknown"
                : !config.gymControl() ? "Gym control is OFF in settings"
                : myRsn == null ? "your RSN wasn't loaded yet" : "the battle team reference was empty";
            battleDiag("claim skipped: " + why);
            // Silent skips here cost hours of "I beat it and nothing happened"
            chatAlways("Gielinor Safari: your win did NOT claim "
                + (gym != null ? gym.name : "the gym") + " because " + why + ".");
            return;
        }
        com.osrsgo.gym.LocalGyms.claim(profile.gyms, gym.id, myRsn,
            specsOf(battleTeamRefs), client.getWorld(), profile.faction);
        profile.stats.gymsCaptured++;
        battleDiag("claim " + gym.id + " OK");
        saveSoon();
        chatMessage("Gielinor Safari: you now hold " + gym.name + "! Your team stands guard.");
        broadcastGymClaim(gym.id);
        refreshPanel();
    }

    @Subscribe
    public synchronized void onGymClaimMsg(com.osrsgo.party.GymClaimMsg msg)
    {
        if (isEcho(msg.getMemberId()) || !config.gymControl()
            || GymData.byId(msg.getGymId()) == null || msg.getHolderRsn() == null)
        {
            return;
        }
        // Bind the holder name to whoever actually sent this, or any party
        // member can claim (or hand away) any gym under any name, and
        // gymsHeldNow() feeds tribute, the catch bonus and the monarch bonus.
        // Both sides go through toJagexName first: an RSN carries a
        // non-breaking space where a space is displayed, so a raw comparison
        // silently rejects every player whose name has a space in it.
        net.runelite.client.party.PartyMember sender = partyService.getMemberById(msg.getMemberId());
        if (sender == null || !jagexName(msg.getHolderRsn()).equalsIgnoreCase(jagexName(sender.getDisplayName())))
        {
            return;
        }
        String faction = msg.getFaction();
        if (faction != null && !faction.equals("SARADOMIN") && !faction.equals("ZAMORAK")
            && !faction.equals("GUTHIX"))
        {
            // A malformed faction means a malformed sender: treat the whole
            // message as hostile rather than silently nulling the field
            return;
        }
        List<MonSpec> team = new ArrayList<>();
        try
        {
            MonSpec[] raw = gson.fromJson(msg.getTeamJson(), MonSpec[].class);
            if (raw != null)
            {
                for (MonSpec spec : raw)
                {
                    if (com.osrsgo.battle.SpecValidator.valid(spec))
                    {
                        team.add(spec);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("bad party gym claim", e);
            return;
        }
        if (team.isEmpty() || team.size() > MAX_CLAIM_TEAM)
        {
            log.warn("party gym claim for {} carried {} valid mons; ignoring",
                msg.getGymId(), team.size());
            return;
        }
        com.osrsgo.gym.LocalGyms.claim(profile.gyms, msg.getGymId(), msg.getHolderRsn(),
            team, null, faction);
        saveSoon();
        GymData.Gym gym = GymData.byId(msg.getGymId());
        chatMessage("Gielinor Safari: " + msg.getHolderRsn() + " took " + gym.name + "!");
        refreshPanel();
    }

    @Subscribe
    public synchronized void onGymBreakMsg(com.osrsgo.party.GymBreakMsg msg)
    {
        if (isEcho(msg.getMemberId()) || !config.gymControl() || GymData.byId(msg.getGymId()) == null)
        {
            return;
        }
        // A break costs the sender a real gym battle, so one per minute per
        // sender is generous. Without this any peer can loop breaks over all
        // ten gyms and hold the whole party at zero tribute forever.
        long now = System.currentTimeMillis();
        Long lastBreak = lastBreakFromMember.get(msg.getMemberId());
        if (lastBreak != null && now - lastBreak < BREAK_COOLDOWN_MS)
        {
            return;
        }
        lastBreakFromMember.put(msg.getMemberId(), now);
        GymHolder losing = profile.gyms.get(msg.getGymId());
        boolean wasMine = losing != null && myRsn != null && losing.isHeldBy(myRsn);
        com.osrsgo.gym.LocalGyms.breakHold(profile.gyms, msg.getGymId());
        if (wasMine)
        {
            // Losing tribute silently is how a griefer stays invisible
            GymData.Gym gym = GymData.byId(msg.getGymId());
            chatMessage("Gielinor Safari: your hold on " + gym.name + " was broken. It stands unclaimed.");
        }
        saveSoon();
        refreshPanel();
    }

    /**
     * RSNs and party display names spell the same player differently: the RSN
     * keeps the non-breaking space the game stores, the display name has been
     * normalised. Compare them only through here.
     */
    private static String jagexName(String name)
    {
        return name == null ? "" : net.runelite.client.util.Text.toJagexName(name);
    }

    /** True when this holder name belongs to someone currently in the party. */
    public boolean isPartyMemberName(String rsn)
    {
        if (rsn == null || !partyService.isInParty())
        {
            return false;
        }
        for (net.runelite.client.party.PartyMember member : partyService.getMembers())
        {
            if (jagexName(rsn).equalsIgnoreCase(jagexName(member.getDisplayName())))
            {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ party message handlers



    @Subscribe
    public synchronized void onBattleChallengeMsg(BattleChallengeMsg msg)
    {
        if (isEcho(msg.getMemberId()) || localMemberId() != msg.getTargetId())
        {
            return;
        }
        pendingBattleId = msg.getBattleId();
        pendingChallengerId = msg.getMemberId();
        pendingChallengerName = msg.getChallengerName();
        pendingChallengerTeamJson = msg.getTeamJson();
        notifier.notify("Gielinor Safari: " + msg.getChallengerName() + " challenged you to a battle!");
        refreshPanel();
    }

    @Subscribe
    public synchronized void onBattleAcceptMsg(BattleAcceptMsg msg)
    {
        if (isEcho(msg.getMemberId()) || outgoingBattleId == null || !outgoingBattleId.equals(msg.getBattleId()))
        {
            return;
        }
        List<OwnedMon> team = battleReadyTeam();
        if (team.isEmpty() || session != null)
        {
            return;
        }
        List<BattleMon> mine = new ArrayList<>();
        for (OwnedMon m : team)
        {
            mine.add(BattleMon.fromOwned(m));
        }
        List<BattleMon> theirs = specsToMons(msg.getTeamJson());
        battleTeamRefs = team;
        session = new BattleSession(BattleSession.Mode.PVP_HOST, outgoingBattleId,
            msg.getAccepterName(), msg.getMemberId(), null, mine, theirs);
        session.fullLog.add("Battle vs " + msg.getAccepterName() + " begins!");
        outgoingBattleId = null;
        refreshPanel();
    }

    @Subscribe
    public synchronized void onBattleDeclineMsg(BattleDeclineMsg msg)
    {
        if (isEcho(msg.getMemberId()))
        {
            return;
        }
        if (outgoingBattleId != null && outgoingBattleId.equals(msg.getBattleId()))
        {
            outgoingBattleId = null;
            panelToast("Your challenge was declined.");
            refreshPanel();
        }
    }

    @Subscribe
    public synchronized void onBattleMoveMsg(BattleMoveMsg msg)
    {
        if (isEcho(msg.getMemberId()) || session == null || session.mode != BattleSession.Mode.PVP_HOST
            || !session.battleId.equals(msg.getBattleId()) || msg.getTurn() != session.turn)
        {
            return;
        }
        session.oppPendingMove = msg.getMoveIndex();
        tryResolveHostTurn();
        refreshPanel();
    }

    @Subscribe
    public synchronized void onBattleTurnResultMsg(BattleTurnResultMsg msg)
    {
        if (isEcho(msg.getMemberId()) || session == null || session.mode != BattleSession.Mode.PVP_GUEST
            || !session.battleId.equals(msg.getBattleId()))
        {
            return;
        }
        try
        {
            TurnState state = gson.fromJson(msg.getStateJson(), TurnState.class);
            session.applySnapshot(state);
            battleScene.clash(session.myMon().lastHitTaken, session.oppMon().lastHitTaken);
            if (session.finished)
            {
                finishBattle();
            }
        }
        catch (Exception e)
        {
            log.warn("Bad turn state from host", e);
        }
        refreshPanel();
    }

    @Subscribe
    public synchronized void onBattleEndMsg(BattleEndMsg msg)
    {
        if (isEcho(msg.getMemberId()) || session == null || !session.battleId.equals(msg.getBattleId()))
        {
            return;
        }
        if (!session.finished)
        {
            session.finished = true;
            session.wonByMe = true;
            session.fullLog.add(session.opponentName + " forfeited. You win!");
            finishBattle();
        }
        refreshPanel();
    }

    @Subscribe
    public synchronized void onPartyChanged(PartyChanged event)
    {
        // Leaving the party invalidates any pvp state
        if (!partyService.isInParty())
        {
            if (session != null && session.mode != BattleSession.Mode.AI && !session.finished)
            {
                session.finished = true;
                session.wonByMe = false;
                session.fullLog.add("Party disbanded; battle abandoned.");
                syncTeamHp();
                saveSoon();
            }
            outgoingBattleId = null;
            clearPendingChallenge();
            trade = null;
            clearIncomingTrade();
            refreshPanel();
        }
    }

    // ------------------------------------------------------------------ helpers

    private List<MonSpec> specsOf(List<OwnedMon> team)
    {
        List<MonSpec> specs = new ArrayList<>();
        for (OwnedMon m : team)
        {
            specs.add(MonSpec.fromOwned(m));
        }
        return specs;
    }

    private List<BattleMon> specsToMons(String teamJson)
    {
        List<BattleMon> mons = new ArrayList<>();
        try
        {
            MonSpec[] specs = gson.fromJson(teamJson, MonSpec[].class);
            if (specs != null)
            {
                for (MonSpec s : specs)
                {
                    if (com.osrsgo.battle.SpecValidator.valid(s))
                    {
                        mons.add(BattleMon.fromSpec(s));
                    }
                    else
                    {
                        log.warn("Rejected impossible mon spec from opponent: species {} lvl {}",
                            s != null ? s.speciesId : -1, s != null ? s.level : -1);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Bad team json", e);
        }
        if (mons.isEmpty())
        {
            mons.add(BattleMon.fromSpecies(3, 5));
        }
        return mons;
    }

    private boolean isEcho(long memberId)
    {
        return localMemberId() == memberId;
    }

    private long localMemberId()
    {
        PartyMember local = partyService.getLocalMember();
        return local != null ? local.getMemberId() : -1;
    }

    private String localName()
    {
        PartyMember local = partyService.getLocalMember();
        return local != null && local.getDisplayName() != null ? local.getDisplayName() : "Trainer";
    }

    private void clearPendingChallenge()
    {
        pendingBattleId = null;
        pendingChallengerId = -1;
        pendingChallengerName = null;
        pendingChallengerTeamJson = null;
    }

    /** Milestone messages; muted by the 'Chat: milestones' toggle. */
    private void chatMessage(String message)
    {
        if (config.chatMilestones())
        {
            chatAlways(message);
        }
    }

    /** Travel-find messages; muted by the 'Chat: travel finds' toggle. */
    private void chatFind(String message)
    {
        if (config.chatFinds())
        {
            chatAlways(message);
        }
    }

    /** Warnings that must never be muted (lost claims, tampered profiles). */
    private void chatAlways(String message)
    {
        clientThread.invokeLater(() ->
        {
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            }
        });
    }

    private void panelToast(String message)
    {
        if (panel != null)
        {
            panel.showToast(message);
        }
    }

    private void refreshPanel()
    {
        if (panel != null)
        {
            panel.refreshLater();
        }
    }

    // ------------------------------------------------------------------ debounced persistence
    // Saving = full profile JSON + HMAC + a config write that broadcasts to
    // every plugin. Walking research once triggered that EVERY TICK; now dirty
    // state flushes at most every 10 ticks (~6s), plus on shutdown.

    private volatile boolean profileDirty;

    private void saveSoon()
    {
        profileDirty = true;
    }

    private void flushProfileIfDirty()
    {
        if (profileDirty)
        {
            profileDirty = false;
            // Serialization grows with the collection; keep it OFF the client
            // thread. The lock keeps mutators (all synchronized) consistent.
            executor.execute(() ->
            {
                synchronized (OsrsGoPlugin.this)
                {
                    profileStore.save(profile);
                }
            });
        }
    }

}
