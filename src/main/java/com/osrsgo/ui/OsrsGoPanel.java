package com.osrsgo.ui;

import com.osrsgo.OsrsGoPlugin;
import com.osrsgo.battle.BattleMon;
import com.osrsgo.battle.BattleSession;
import com.osrsgo.data.GymData;
import com.osrsgo.data.MoveData;
import com.osrsgo.model.Move;
import com.osrsgo.model.OwnedMon;
import com.osrsgo.model.Rarity;
import com.osrsgo.model.Species;
import com.osrsgo.spawn.WildSpawn;
import com.osrsgo.storage.PlayerProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.party.PartyMember;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class OsrsGoPanel extends PluginPanel
{
    private final OsrsGoPlugin plugin;

    private final JLabel titleLabel = new JLabel();
    private final JLabel trainerLabel = new JLabel();
    private final JLabel[] ballCountLabels = new JLabel[5];
    private final JLabel berryCountLabel = new JLabel();
    private final JLabel candyCountLabel = new JLabel();
    private final JLabel buddyLabel = new JLabel();
    private final JLabel rotationLabel = new JLabel();
    private static final String[] BALL_NAMES =
        {"Gielinor Ball", "Great Ball (+10%)", "Super Ball (+20%)", "Ultra Ball (+35%)", "Master Ball (100%, Rare+)"};
    private final JLabel toastLabel = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel nearbyContent = new JPanel();
    private final JPanel dexContent = new JPanel();
    private final JPanel gymContent = new JPanel();
    private final JPanel battleContent = new JPanel();
    private final JPanel statsContent = new JPanel();

    // Renders only when this is a non-empty https URL

    private volatile boolean refreshQueued;
    private String lastFingerprint = "";
    private Timer toastTimer;
    private final java.util.Set<Integer> expandedDex = new java.util.HashSet<>();
    private boolean pokedexMode;
    private static final Color SHINY_GOLD = new Color(255, 215, 60);

    public OsrsGoPanel(OsrsGoPlugin plugin)
    {
        // wrap=false: we run our own per-tab scroll panes, and PluginPanel's
        // outer scroll pane was swallowing the mouse wheel
        super(false);
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trainerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        trainerLabel.setFont(trainerLabel.getFont().deriveFont(12f));
        trainerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ballRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ballRow.setOpaque(false);
        ballRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        for (int tier = 0; tier < 5; tier++)
        {
            JLabel count = new JLabel("0");
            count.setIcon(new javax.swing.ImageIcon(Icons.tierBall(tier, 16)));
            count.setIconTextGap(3);
            count.setForeground(Color.WHITE);
            count.setFont(count.getFont().deriveFont(Font.BOLD, 12f));
            count.setToolTipText(BALL_NAMES[tier]);
            ballCountLabels[tier] = count;
            ballRow.add(count);
        }
        // Berry and candy live on their own row: seven items overflow the
        // panel width once ball counts get fat, and FlowLayout's wrap line
        // was being clipped invisible
        JPanel suppliesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        suppliesRow.setOpaque(false);
        suppliesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        suppliesRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        berryCountLabel.setIcon(new javax.swing.ImageIcon(Icons.berry(16)));
        berryCountLabel.setIconTextGap(3);
        berryCountLabel.setForeground(Color.WHITE);
        berryCountLabel.setFont(berryCountLabel.getFont().deriveFont(Font.BOLD, 12f));
        berryCountLabel.setToolTipText("Berries: +15% catch chance on a berry throw");
        suppliesRow.add(berryCountLabel);
        candyCountLabel.setIcon(new javax.swing.ImageIcon(Icons.candy(16)));
        candyCountLabel.setIconTextGap(3);
        candyCountLabel.setForeground(new Color(190, 110, 250));
        candyCountLabel.setFont(candyCountLabel.getFont().deriveFont(Font.BOLD, 12f));
        candyCountLabel.setToolTipText("Rare Candies: +1 level for any mon. Walk onto purple sweets in the world!");
        suppliesRow.add(candyCountLabel);

        buddyLabel.setForeground(new Color(140, 200, 255));
        buddyLabel.setFont(buddyLabel.getFont().deriveFont(11f));
        buddyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rotationLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        rotationLabel.setFont(rotationLabel.getFont().deriveFont(11f));
        rotationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        toastLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        toastLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(2));
        header.add(trainerLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(ballRow);
        header.add(Box.createVerticalStrut(2));
        header.add(suppliesRow);
        header.add(Box.createVerticalStrut(4));
        header.add(buddyLabel);
        header.add(rotationLabel);
        header.add(toastLabel);
        add(header, BorderLayout.NORTH);

        setupTab(nearbyContent, "Nearby");
        setupTab(dexContent, "Dex");
        setupTab(gymContent, "Gyms");
        setupTab(battleContent, "Party");
        setupTab(statsContent, "Stats");
        add(tabs, BorderLayout.CENTER);

        refresh();
    }

    private void setupTab(JPanel content, String name)
    {
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel wrapper = new ScrollableContent();
        wrapper.setLayout(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapper.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        tabs.addTab(name, scroll);
    }

    /**
     * Tracks the viewport width so rows lay out at panel width instead of
     * pushing buttons past the right edge (which also spawned a useless
     * horizontal scrollbar).
     */
    private static class ScrollableContent extends JPanel implements javax.swing.Scrollable
    {
        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
        {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }

    private volatile boolean popupOpen;

    /**
     * Rebuilds destroy components, which closes any open dropdown mid-click.
     * Combos wrapped here pause rebuilds while their popup is showing and
     * trigger a catch-up refresh when it closes.
     */
    private <T> javax.swing.JComboBox<T> guardPopup(javax.swing.JComboBox<T> combo)
    {
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e)
            {
                popupOpen = true;
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e)
            {
                popupOpen = false;
                refreshLater();
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e)
            {
                popupOpen = false;
            }
        });
        return combo;
    }

    private volatile boolean spinnerFocused;

    /**
     * The same guard as {@link #guardPopup} but for a spinner's text editor,
     * so a rebuild never lands on a half-typed number.
     *
     * Focus is tracked on the editor itself rather than by asking the client's
     * shared focus manager who currently holds focus. Reaching for that global
     * is not allowed in Plugin Hub plugins, and a listener on our own component
     * is the narrower thing to do anyway: it only ever sees this panel.
     */
    private javax.swing.JSpinner guardSpinner(javax.swing.JSpinner spinner)
    {
        java.awt.Component editor = spinner.getEditor();
        if (editor instanceof javax.swing.JSpinner.DefaultEditor)
        {
            editor = ((javax.swing.JSpinner.DefaultEditor) editor).getTextField();
        }
        editor.addFocusListener(new java.awt.event.FocusAdapter()
        {
            @Override
            public void focusGained(java.awt.event.FocusEvent e)
            {
                spinnerFocused = true;
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e)
            {
                spinnerFocused = false;
                refreshLater();
            }
        });
        return spinner;
    }

    /** Thread-safe refresh entry point; coalesces bursts into one EDT pass. */
    public void refreshLater()
    {
        if (refreshQueued)
        {
            return;
        }
        refreshQueued = true;
        SwingUtilities.invokeLater(() ->
        {
            refreshQueued = false;
            refresh();
        });
    }

    public void showToast(String message)
    {
        SwingUtilities.invokeLater(() ->
        {
            toastLabel.setText(message);
            if (toastTimer != null)
            {
                toastTimer.stop();
            }
            toastTimer = new Timer(5000, e -> toastLabel.setText(" "));
            toastTimer.setRepeats(false);
            toastTimer.start();
        });
    }

    private void refresh()
    {
        PlayerProfile profile = plugin.getProfile();
        String faction = profile.faction != null
            ? profile.faction.charAt(0) + profile.faction.substring(1).toLowerCase() : null;
        titleLabel.setText("Gielinor Safari" + (faction != null ? "  [" + faction + "]" : ""));
        trainerLabel.setText("Trainer lvl " + profile.trainerLevel()
            + "  (" + profile.xpIntoLevel() + "/" + profile.xpForNextLevel() + " xp)"
            + "  Badges " + profile.badges.size() + "/" + GymData.all().size());
        int[] counts = {profile.balls, profile.greatBalls, profile.superBalls,
            profile.ultraBalls, profile.masterBalls};
        for (int tier = 0; tier < 5; tier++)
        {
            ballCountLabels[tier].setText(String.valueOf(counts[tier]));
            ballCountLabels[tier].setToolTipText(BALL_NAMES[tier] + ": " + counts[tier]);
        }
        berryCountLabel.setText(String.valueOf(profile.berries));
        candyCountLabel.setText(String.valueOf(profile.rareCandies));
        OwnedMon buddy = plugin.buddy();
        buddyLabel.setText(buddy != null
            ? "Buddy: " + (buddy.shiny ? "SHINY " : "") + buddy.name() + " lvl " + buddy.level
            : "No buddy set (pick one in the Dex)");
        buddyLabel.setToolTipText("Your buddy gains XP as you walk together.");
        long millis = plugin.millisUntilRotation();
        rotationLabel.setText("Next spawn wave in " + (millis / 60000) + "m " + ((millis / 1000) % 60) + "s");

        if (popupOpen || spinnerFocused)
        {
            // Leave open dropdowns and half-typed spinners alone
            return;
        }
        String fingerprint = fingerprint(profile);
        if (fingerprint.equals(lastFingerprint))
        {
            return;
        }
        lastFingerprint = fingerprint;

        rebuildNearby();
        rebuildDex(profile);
        rebuildGyms(profile);
        rebuildBattle();
        rebuildStats(profile);
        applyTooltips(nearbyContent);
        applyTooltips(dexContent);
        applyTooltips(gymContent);
        applyTooltips(battleContent);
        applyTooltips(statsContent);
        updateTradeDialog();
        revalidate();
        repaint();
    }

    private String fingerprint(PlayerProfile profile)
    {
        StringBuilder sb = new StringBuilder();
        WorldPoint player = plugin.getPlayerLocation();
        for (WildSpawn s : plugin.getNearbySpawns())
        {
            sb.append(s.key).append(':');
            sb.append(player != null ? player.distanceTo(s.location) : -1).append(';');
        }
        sb.append('|').append(profile.mons.size()).append(':').append(profile.trainerXp)
            .append(':').append(profile.teamIndices).append(':').append(profile.badges.size())
            .append(':').append(pokedexMode).append(':').append(profile.seenSpecies.size())
            .append(':').append(profile.caughtSpecies.size())
            .append(':').append(profile.balls).append(':').append(profile.buddyIndex)
            .append(':').append(plugin.getSpotlightSpecies().getId());
        OwnedMon fpBuddy = plugin.buddy();
        if (fpBuddy != null)
        {
            sb.append(':').append(fpBuddy.level).append(',').append(fpBuddy.xp);
        }
        for (OwnedMon m : profile.mons)
        {
            sb.append(m.speciesId).append(',').append(m.level).append(',').append(m.currentHp())
                .append(',').append(m.favorite ? 'f' : '-').append(';');
        }
        sb.append('|').append(plugin.isNearBank()).append(':').append(plugin.isNearBreedingDen());
        sb.append('|').append(profile.berries).append(':').append(profile.greatBalls).append(':').append(profile.superBalls).append(':').append(profile.ultraBalls).append(':').append(profile.masterBalls).append(':').append(plugin.getSelectedBallTier()).append(':').append(plugin.isNearGeneralStore()).append(':').append(profile.faction)
            .append(':').append(tradeFingerprint()).append(':').append(plugin.getIncomingTradeFrom())
            .append(':').append(plugin.isNightNow())
            .append(':').append(plugin.getOtherPartyMembers().size());
        for (int i = 0; i < 3 && i < profile.researchProgress.length; i++)
        {
            sb.append(profile.researchProgress[i]).append(',')
                .append(profile.researchDone.length > i && profile.researchDone[i]).append(';');
        }
        for (int i = 0; i < 2; i++)
        {
            sb.append(profile.weeklyProgress[i]).append(',').append(profile.weeklyDone[i]).append(',')
                .append(profile.monthlyProgress[i]).append(',').append(profile.monthlyDone[i]).append(';');
        }
        sb.append('t').append(profile.activeTeamSlot).append("rc").append(profile.rareCandies);
        for (com.osrsgo.spawn.SpawnManager.Candy c : plugin.getNearbyCandies())
        {
            sb.append(c.key).append(',').append(player != null ? player.distanceTo(c.location) : -1).append(';');
        }
        for (com.osrsgo.data.RaidData.Raid raid : plugin.currentRaids())
        {
            sb.append(raid.gymId).append(',').append(raid.speciesId)
                .append(',').append(plugin.raidAlreadyAttempted(raid.gymId)).append(';');
        }
        java.util.Map<String, Integer> ess = profile.tierEssence;
        sb.append("e").append(ess.size()).append("se").append(profile.shinyEssence);
        for (Integer v : ess.values())
        {
            sb.append(',').append(v);
        }
        PlayerProfile.Stats st = profile.stats;
        sb.append('|').append(st.ballsThrown).append(':').append(st.catches).append(':')
            .append(st.gymWins).append(':').append(st.gymLosses).append(':')
            .append(st.pvpWins).append(':').append(st.pvpLosses).append(':')
            .append(st.gymsCaptured).append(':').append(st.eggsHatched)
            .append(':').append(profile.tilesWalked / 50).append(':').append(plugin.gymsHeldNow())
            .append(':').append(profile.earnedMedals.size());
        for (com.osrsgo.model.Egg egg : profile.eggs)
        {
            sb.append(egg.tilesRequired).append(',').append(egg.tilesProgress / 25).append(';');
        }
        sb.append('|');
        for (GymData.Gym g : gymsByDistance(player))
        {
            sb.append(g.id).append(plugin.inGymRange(g) ? '1' : '0');
            com.osrsgo.gym.GymHolder h = plugin.getGymHolder(g.id);
            if (h != null)
            {
                sb.append(h.holderRsn).append(',').append(h.defenseWins).append(',').append(h.updatedAt)
                    .append(',').append(holdMinutesLeft(h));
            }
            sb.append(';');
        }
        sb.append('|');
        BattleSession s = plugin.getSession();
        if (s != null)
        {
            sb.append(s.battleId).append(':').append(s.turn).append(':').append(s.fullLog.size())
                .append(':').append(s.myPendingMove).append(':').append(s.finished)
                .append(':').append(s.myMon().hp).append(':').append(s.oppMon().hp);
        }
        sb.append('|').append(plugin.getPendingChallengerName()).append(':').append(plugin.hasOutgoingChallenge())
            .append(':').append(plugin.getActiveCatch() != null)
            .append(':').append(plugin.getNpcImages().getVersion());
        com.osrsgo.data.BiomeData.Biome biome = plugin.getCurrentBiome();
        sb.append(':').append(biome != null ? biome.name : "");
        for (PartyMember m : plugin.getOtherPartyMembers())
        {
            sb.append(m.getMemberId()).append(',').append(m.getDisplayName()).append(';');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ Nearby

    private void rebuildNearby()
    {
        nearbyContent.removeAll();
        WorldPoint player = plugin.getPlayerLocation();
        List<WildSpawn> spawns = plugin.getNearbySpawns();
        if (player == null)
        {
            nearbyContent.add(hint("Log in to start hunting."));
            return;
        }
        com.osrsgo.data.BiomeData.Biome biome = plugin.getCurrentBiome();
        if (biome != null)
        {
            JPanel biomeCard = stackRow();
            JLabel biomeName = new JLabel("Area: " + biome.name);
            biomeName.setForeground(new Color(140, 200, 255));
            biomeName.setFont(biomeName.getFont().deriveFont(Font.BOLD));
            biomeCard.add(biomeName);
            JLabel biomeHint = new JLabel("<html><body style='width:160px'>" + biome.hint + "</body></html>");
            biomeHint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            biomeHint.setFont(biomeHint.getFont().deriveFont(11f));
            biomeCard.add(biomeHint);
            com.osrsgo.data.BiomeData.DangerZone zone =
                com.osrsgo.data.BiomeData.dangerZoneAt(player.getX(), player.getY());
            if (zone != null)
            {
                JLabel zoneLabel = new JLabel("<html><body style='width:160px'>" + zone.name
                    + ": " + zone.hint + "</body></html>");
                zoneLabel.setForeground(new Color(255, 150, 80));
                zoneLabel.setFont(zoneLabel.getFont().deriveFont(Font.BOLD, 11f));
                biomeCard.add(zoneLabel);
            }
            Species spotlight = plugin.getSpotlightSpecies();
            JLabel spotlightLabel = new JLabel("Spotlight: " + spotlight.getName() + " (5x spawns this rotation)");
            spotlightLabel.setIcon(iconFor(spotlight));
            spotlightLabel.setIconTextGap(6);
            spotlightLabel.setForeground(new Color(255, 220, 120));
            spotlightLabel.setFont(spotlightLabel.getFont().deriveFont(11f));
            biomeCard.add(Box.createVerticalStrut(3));
            biomeCard.add(spotlightLabel);
            if (com.osrsgo.spawn.SpawnManager.isCommunityHour())
            {
                JLabel event = new JLabel("COMMUNITY HOUR! 12x spotlight, 2x catch XP, 2x ball finds!");
                event.setForeground(new Color(255, 200, 40));
                event.setFont(event.getFont().deriveFont(Font.BOLD, 12f));
                event.setToolTipText("Every Saturday 20:00-21:00 UTC, worldwide");
                biomeCard.add(event);
            }
            if (plugin.isNightNow())
            {
                JLabel night = new JLabel("Night has fallen: the undead stir...");
                night.setForeground(new Color(190, 150, 255));
                night.setFont(night.getFont().deriveFont(Font.ITALIC, 11f));
                biomeCard.add(night);
            }
            nearbyContent.add(biomeCard);
            nearbyContent.add(Box.createVerticalStrut(4));

            addResearchCard();
        }
        addBallSelector(profile());
        addExchangeCard(profile());
        addNearbyFilters();
        java.util.Set<Integer> targets = plugin.getHuntTargets();
        java.util.List<WildSpawn> shown = new java.util.ArrayList<>();
        for (WildSpawn s : spawns)
        {
            // Shinies are never filtered out; missing one to a min-level box
            // would be a tragedy
            if (s.shiny
                || (s.level >= nearbyMinLevel
                    && s.species().getRarity().ordinal() >= nearbyMinRarity
                    && (!nearbyUncaughtOnly || !profile().caughtSpecies.contains(s.speciesId))
                    && (targets.isEmpty() || targets.contains(s.speciesId))))
            {
                shown.add(s);
            }
        }
        for (com.osrsgo.spawn.SpawnManager.Candy candy : plugin.getNearbyCandies())
        {
            JPanel candyRow = stackRow();
            JLabel candyLabel = new JLabel("RARE CANDY  " + player.distanceTo(candy.location)
                + " tiles " + directionOf(player, candy.location));
            candyLabel.setIcon(new javax.swing.ImageIcon(com.osrsgo.ui.Icons.candy(22)));
            candyLabel.setIconTextGap(6);
            candyLabel.setForeground(new Color(190, 110, 250));
            candyLabel.setFont(candyLabel.getFont().deriveFont(Font.BOLD));
            candyLabel.setToolTipText("A purple sweet! Walk onto its tile to scoop it up: +1 level for any mon.");
            candyRow.add(candyLabel);
            candyRow.add(hint("Walk onto it to scoop it up!"));
            nearbyContent.add(candyRow);
            nearbyContent.add(Box.createVerticalStrut(4));
        }
        int hidden = spawns.size() - shown.size();
        spawns = shown;
        if (spawns.isEmpty())
        {
            nearbyContent.add(hint(hidden > 0
                ? hidden + " spawn" + (hidden == 1 ? "" : "s") + " hidden by your filters."
                : "No wild mons within 32 tiles. Wander around, or wait for the next rotation."));
            return;
        }
        if (hidden > 0)
        {
            nearbyContent.add(hint(hidden + " hidden by filters."));
        }
        for (WildSpawn spawn : spawns)
        {
            nearbyContent.add(spawnRow(spawn, player));
            nearbyContent.add(Box.createVerticalStrut(4));
        }
        nearbyContent.add(hint("Walk within " + plugin.catchRange() + " tiles to catch. Higher rarity = harder catch."));
    }

    private PlayerProfile profile()
    {
        return plugin.getProfile();
    }

    private int nearbyMinLevel = 1;
    private int nearbyMinRarity;
    private boolean nearbyUncaughtOnly;

    private void addNearbyFilters()
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel lvlLabel = new JLabel("Min lvl");
        lvlLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        lvlLabel.setFont(lvlLabel.getFont().deriveFont(11f));
        javax.swing.JSpinner lvlSpin = guardSpinner(new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(nearbyMinLevel, 1, 99, 1)));
        lvlSpin.setPreferredSize(new Dimension(52, 22));
        lvlSpin.setToolTipText("Hide spawns below this level");
        lvlSpin.addChangeListener(e ->
        {
            nearbyMinLevel = (Integer) lvlSpin.getValue();
            lastFingerprint = "";
        });
        javax.swing.JComboBox<String> rarityBox = guardPopup(new javax.swing.JComboBox<>(
            new String[]{"Any rarity", "Uncommon+", "Rare+", "Epic+", "Legendary"}));
        rarityBox.setSelectedIndex(nearbyMinRarity);
        rarityBox.setToolTipText("Hide spawns below this rarity");
        rarityBox.addActionListener(e ->
        {
            nearbyMinRarity = rarityBox.getSelectedIndex();
            lastFingerprint = "";
            refresh();
        });
        row.add(lvlLabel);
        row.add(lvlSpin);
        row.add(rarityBox);
        nearbyContent.add(row);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        javax.swing.JCheckBox uncaughtBox = new javax.swing.JCheckBox("Uncaught", nearbyUncaughtOnly);
        uncaughtBox.setOpaque(false);
        uncaughtBox.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        uncaughtBox.setFont(uncaughtBox.getFont().deriveFont(11f));
        uncaughtBox.setToolTipText("Only show species you haven't caught yet");
        uncaughtBox.addActionListener(e ->
        {
            nearbyUncaughtOnly = uncaughtBox.isSelected();
            lastFingerprint = "";
            refresh();
        });
        int targetCount = plugin.getHuntTargets().size();
        JButton targetsBtn = new JButton(targetCount > 0 ? "Targets (" + targetCount + ")" : "Targets");
        targetsBtn.setFont(targetsBtn.getFont().deriveFont(11f));
        targetsBtn.setMargin(new java.awt.Insets(1, 6, 1, 6));
        targetsBtn.setToolTipText("Pick specific species from your GielDex to hunt; only they will show. Empty = show all.");
        targetsBtn.addActionListener(e -> openTargetPicker());
        row2.add(uncaughtBox);
        row2.add(targetsBtn);
        nearbyContent.add(row2);
        nearbyContent.add(Box.createVerticalStrut(4));
    }

    // Last successfully bred parents, for the "Same pair" shortcut
    private OwnedMon lastBredA;
    private OwnedMon lastBredB;

    /** Popup to pick hunt targets from every species you've seen or caught. */
    private void openTargetPicker()
    {
        java.util.Set<Integer> known = new java.util.TreeSet<>();
        known.addAll(profile().seenSpecies);
        known.addAll(profile().caughtSpecies);
        if (known.isEmpty())
        {
            showToast("Your GielDex is empty. See or catch some mons first!");
            return;
        }
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(ColorScheme.DARK_GRAY_COLOR);
        java.util.Map<Integer, javax.swing.JCheckBox> boxes = new java.util.LinkedHashMap<>();
        java.util.Set<Integer> current = plugin.getHuntTargets();
        for (int id : known)
        {
            Species sp = com.osrsgo.data.SpeciesData.byId(id);
            if (sp == null)
            {
                continue;
            }
            boolean caught = profile().caughtSpecies.contains(id);
            javax.swing.JCheckBox cb = new javax.swing.JCheckBox(
                sp.getName() + (caught ? "" : " (seen only)"), current.contains(id));
            cb.setOpaque(false);
            cb.setForeground(caught ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
            boxes.put(id, cb);
            list.add(cb);
        }
        JButton clearAll = new JButton("Clear all");
        clearAll.addActionListener(e -> boxes.values().forEach(cb -> cb.setSelected(false)));
        JScrollPane scroll = new JScrollPane(list,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(220, 320));
        JPanel dialogBody = new JPanel(new java.awt.BorderLayout(0, 6));
        dialogBody.add(clearAll, java.awt.BorderLayout.NORTH);
        dialogBody.add(scroll, java.awt.BorderLayout.CENTER);
        int ok = javax.swing.JOptionPane.showConfirmDialog(this, dialogBody, "Pick your targets",
            javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (ok == javax.swing.JOptionPane.OK_OPTION)
        {
            java.util.Set<Integer> sel = new java.util.LinkedHashSet<>();
            boxes.forEach((id, cb) ->
            {
                if (cb.isSelected())
                {
                    sel.add(id);
                }
            });
            plugin.setHuntTargets(sel);
            lastFingerprint = "";
            refresh();
        }
    }

    private void addBallSelector(PlayerProfile profile)
    {
        String[] options = {
            "Gielinor Ball (" + profile.balls + ")",
            "Great Ball (" + profile.greatBalls + ")  +10%",
            "Super Ball (" + profile.superBalls + ")  +20%",
            "Ultra Ball (" + profile.ultraBalls + ")  +35%",
            "Master Ball (" + profile.masterBalls + ")  100%, Rare+"
        };
        javax.swing.JComboBox<String> ballBox = guardPopup(new javax.swing.JComboBox<>(options));
        ballBox.setSelectedIndex(Math.min(plugin.getSelectedBallTier(), 4));
        ballBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        ballBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        ballBox.setToolTipText("Which ball Catch and right-click throws use");
        ballBox.addActionListener(e -> plugin.setSelectedBallTier(ballBox.getSelectedIndex()));
        nearbyContent.add(ballBox);
        nearbyContent.add(Box.createVerticalStrut(4));
    }

    private void addExchangeCard(PlayerProfile profile)
    {
        if (!plugin.isNearGeneralStore())
        {
            return;
        }
        JPanel card = stackRow();
        JLabel title = new JLabel("General Store: Exchange");
        title.setForeground(new Color(240, 200, 120));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        card.add(title);
        JButton great = new JButton("25 Gielinor -> 1 Great");
        great.setIcon(new javax.swing.ImageIcon(Icons.tierBall(1, 14)));
        great.setEnabled(profile.balls >= 25);
        great.addActionListener(e -> toastIfError(plugin.exchangeBalls(1)));
        JButton superB = new JButton("25 Great -> 1 Super");
        superB.setIcon(new javax.swing.ImageIcon(Icons.tierBall(2, 14)));
        superB.setEnabled(profile.greatBalls >= 25);
        superB.addActionListener(e -> toastIfError(plugin.exchangeBalls(2)));
        JButton ultra = new JButton("25 Super -> 1 Ultra");
        ultra.setIcon(new javax.swing.ImageIcon(Icons.tierBall(3, 14)));
        ultra.setEnabled(profile.superBalls >= 25);
        ultra.addActionListener(e -> toastIfError(plugin.exchangeBalls(3)));
        JButton master = new JButton("25 Ultra -> 1 Master");
        master.setIcon(new javax.swing.ImageIcon(Icons.tierBall(4, 14)));
        master.setEnabled(profile.ultraBalls >= 25);
        master.addActionListener(e -> toastIfError(plugin.exchangeBalls(4)));
        great.setAlignmentX(Component.LEFT_ALIGNMENT);
        superB.setAlignmentX(Component.LEFT_ALIGNMENT);
        ultra.setAlignmentX(Component.LEFT_ALIGNMENT);
        master.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton candy = new JButton(OsrsGoPlugin.BERRIES_PER_CANDY + " berries -> 1 Rare Candy");
        candy.setIcon(new javax.swing.ImageIcon(Icons.candy(14)));
        candy.setEnabled(profile.berries >= OsrsGoPlugin.BERRIES_PER_CANDY);
        candy.setToolTipText("Rare Candy: +1 level for any mon. You hold " + profile.berries + " berries.");
        candy.addActionListener(e -> toastIfError(plugin.exchangeBerriesForCandy()));
        great.setAlignmentX(Component.LEFT_ALIGNMENT);
        superB.setAlignmentX(Component.LEFT_ALIGNMENT);
        ultra.setAlignmentX(Component.LEFT_ALIGNMENT);
        master.setAlignmentX(Component.LEFT_ALIGNMENT);
        candy.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(great);
        card.add(Box.createVerticalStrut(2));
        card.add(superB);
        card.add(Box.createVerticalStrut(2));
        card.add(ultra);
        card.add(Box.createVerticalStrut(2));
        card.add(master);
        card.add(Box.createVerticalStrut(6));
        card.add(candy);
        nearbyContent.add(card);
        nearbyContent.add(Box.createVerticalStrut(4));
    }

    private void toastIfError(String err)
    {
        if (err != null)
        {
            showToast(err);
        }
    }

    private JPanel spawnRow(WildSpawn spawn, WorldPoint player)
    {
        Species species = spawn.species();
        Rarity rarity = species.getRarity();
        int dist = player.distanceTo(spawn.location);

        JPanel row = stackRow();
        JLabel name = new JLabel((spawn.shiny ? "SHINY " : "") + species.getName() + "  lvl " + spawn.level);
        name.setIcon(iconFor(species));
        name.setIconTextGap(6);
        name.setForeground(spawn.shiny ? SHINY_GOLD : rarity.getColor());
        int catchPct = (int) Math.round(plugin.catchChanceFor(spawn) * 100);
        name.setToolTipText(rarity.getDisplay() + " " + species.getType().getDisplay()
            + ", " + catchPct + "% catch chance with your selected ball (berries add more)"
            + (spawn.shiny ? ". SHINY: 4x catch XP!" : ""));
        JLabel info = new JLabel(rarity.getDisplay() + " " + species.getType().getDisplay()
            + "  " + dist + " tiles " + directionOf(player, spawn.location)
            + "  " + catchPct + "%");
        info.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        info.setFont(info.getFont().deriveFont(11f));

        boolean throwing = plugin.getActiveCatch() != null;
        int range = plugin.catchRange();
        JButton catchBtn = new JButton(throwing ? "Throwing..." : (dist <= range ? "Catch!" : "Too far"));
        catchBtn.setEnabled(dist <= range && !throwing);
        catchBtn.addActionListener(e ->
        {
            String result = plugin.attemptCatch(spawn.key, false, plugin.getSelectedBallTier());
            if (result != null)
            {
                showToast(result);
            }
            lastFingerprint = "";
            refresh();
        });
        JButton berryBtn = new JButton("Berry throw");
        berryBtn.setToolTipText("Feed a berry first: +15% catch chance. Costs 1 berry + 1 ball.");
        berryBtn.setEnabled(dist <= range && !throwing && plugin.getProfile().berries > 0);
        berryBtn.addActionListener(e ->
        {
            String result = plugin.attemptCatch(spawn.key, true, plugin.getSelectedBallTier());
            if (result != null)
            {
                showToast(result);
            }
            lastFingerprint = "";
            refresh();
        });

        row.add(name);
        row.add(info);
        row.add(buttonStrip(berryBtn, catchBtn));
        return row;
    }

    private static String directionOf(WorldPoint from, WorldPoint to)
    {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        if (Math.abs(dx) <= 2 && Math.abs(dy) <= 2)
        {
            return "(here)";
        }
        String ns = dy > 2 ? "N" : (dy < -2 ? "S" : "");
        String ew = dx > 2 ? "E" : (dx < -2 ? "W" : "");
        return "(" + ns + ew + ")";
    }

    private boolean researchCollapsed;

    private void addResearchCard()
    {
        PlayerProfile profile = plugin.getProfile();
        int doneCount = countDone(profile.researchDone) + countDone(profile.weeklyDone)
            + countDone(profile.monthlyDone);

        JPanel card = stackRow();
        JButton header = new JButton((researchCollapsed ? "[+] " : "[-] ")
            + "Research & Challenges  (" + doneCount + "/7 done)");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setForeground(new Color(150, 220, 150));
        header.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        header.setBorderPainted(false);
        header.setContentAreaFilled(false);
        header.setMargin(new java.awt.Insets(0, 0, 0, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.addActionListener(e ->
        {
            researchCollapsed = !researchCollapsed;
            lastFingerprint = "";
            refresh();
        });
        card.add(header);

        if (!researchCollapsed)
        {
            renderTaskGroup(card, "Today", plugin.researchTasks(),
                profile.researchProgress, profile.researchDone);
            renderTaskGroup(card, "This Week", plugin.weeklyTasks(),
                profile.weeklyProgress, profile.weeklyDone);
            renderTaskGroup(card, "This Month", plugin.monthlyTasks(),
                profile.monthlyProgress, profile.monthlyDone);
        }
        nearbyContent.add(card);
        nearbyContent.add(Box.createVerticalStrut(4));
    }

    private static int countDone(boolean[] done)
    {
        int n = 0;
        for (boolean d : done)
        {
            if (d)
            {
                n++;
            }
        }
        return n;
    }

    private void renderTaskGroup(JPanel card, String title, List<com.osrsgo.data.ResearchData.Task> tasks,
        int[] progressArr, boolean[] doneArr)
    {
        card.add(Box.createVerticalStrut(5));
        JLabel groupLabel = new JLabel(title);
        groupLabel.setForeground(new Color(140, 200, 255));
        groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD, 11f));
        groupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(groupLabel);
        for (int i = 0; i < tasks.size() && i < progressArr.length; i++)
        {
            com.osrsgo.data.ResearchData.Task task = tasks.get(i);
            boolean done = doneArr.length > i && doneArr[i];
            int progress = progressArr[i];
            JLabel line = new JLabel((done ? "[Done] " : "") + task.describe());
            line.setForeground(done ? new Color(120, 200, 120) : Color.WHITE);
            line.setFont(line.getFont().deriveFont(12f));
            line.setToolTipText(task.describe() + ". Reward: " + task.rewardText());
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(line);
            if (!done)
            {
                JProgressBar bar = new JProgressBar(0, task.goal);
                bar.setValue(Math.min(progress, task.goal));
                bar.setStringPainted(true);
                bar.setString(Math.min(progress, task.goal) + "/" + task.goal + "  (" + task.rewardText() + ")");
                bar.setFont(bar.getFont().deriveFont(10f));
                bar.setForeground(new Color(120, 200, 120));
                bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
                bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(bar);
            }
        }
    }

    // ------------------------------------------------------------------ Dex

    private void rebuildDex(PlayerProfile profile)
    {
        dexContent.removeAll();

        JButton collectionBtn = new JButton("Collection");
        collectionBtn.setEnabled(pokedexMode);
        collectionBtn.addActionListener(e ->
        {
            pokedexMode = false;
            lastFingerprint = "";
            refresh();
        });
        JButton pokedexBtn = new JButton("GielDex");
        pokedexBtn.setEnabled(!pokedexMode);
        pokedexBtn.addActionListener(e ->
        {
            pokedexMode = true;
            lastFingerprint = "";
            refresh();
        });
        JPanel modeStrip = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));
        modeStrip.setOpaque(false);
        modeStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        modeStrip.add(collectionBtn);
        modeStrip.add(pokedexBtn);
        dexContent.add(modeStrip);
        dexContent.add(Box.createVerticalStrut(4));

        // The buddy leads the whole tab, Pokemon-Go style
        if (!pokedexMode && profile.buddyIndex != null
            && profile.buddyIndex >= 0 && profile.buddyIndex < profile.mons.size())
        {
            dexContent.add(hint("Your buddy (gains XP as you walk):"));
            dexContent.add(monRow(profile, profile.buddyIndex));
            dexContent.add(Box.createVerticalStrut(6));
        }

        JButton storageBtn = new JButton("Open Storage");
        storageBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        storageBtn.setToolTipText("Your whole collection in one grid: browse, compare, and manage");
        storageBtn.addActionListener(e -> openStorageDialog());
        dexContent.add(storageBtn);
        dexContent.add(Box.createVerticalStrut(6));

        if (pokedexMode)
        {
            rebuildPokedex(profile);
            return;
        }

        addHealCard(profile);
        addEggCards(profile);
        addBreedingCard(profile);
        addEssencePouch(profile);

        if (profile.mons.isEmpty())
        {
            dexContent.add(hint("No mons yet. Catch some in the Nearby tab!"));
            return;
        }

        javax.swing.JComboBox<String> sortBox = guardPopup(new javax.swing.JComboBox<>(
            new String[]{"Newest", "Oldest", "Name", "Level", "Type", "Rarity", "Best IVs"}));
        sortBox.setSelectedItem(collectionSort);
        sortBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        sortBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sortBox.addActionListener(e ->
        {
            collectionSort = (String) sortBox.getSelectedItem();
            lastFingerprint = "";
            refresh();
        });
        dexContent.add(sortBox);
        dexContent.add(Box.createVerticalStrut(4));

        javax.swing.JComboBox<String> teamBox = guardPopup(new javax.swing.JComboBox<>(
            new String[]{"Team 1", "Team 2", "Team 3"}));
        teamBox.setSelectedIndex(profile.activeTeamSlot);
        teamBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        teamBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        teamBox.setToolTipText("Three switchable team loadouts");
        teamBox.addActionListener(e -> plugin.switchTeamSlot(teamBox.getSelectedIndex()));
        dexContent.add(teamBox);
        dexContent.add(Box.createVerticalStrut(4));
        dexContent.add(hint("Team " + (profile.activeTeamSlot + 1) + " (" + profile.teamIndices.size() + "/"
            + plugin.teamCap() + "): battles use these, in order."));
        for (int idx : sortedCollectionIndices(profile))
        {
            if (profile.buddyIndex != null && profile.buddyIndex == idx)
            {
                // Already showcased at the top of the tab
                continue;
            }
            dexContent.add(monRow(profile, idx));
            dexContent.add(Box.createVerticalStrut(4));
        }
        addCleanupSection(profile);
    }

    private boolean essencePouchCollapsed = true;

    /** The five rarity-tier essence pools (plus shiny) at a glance. */
    private void addEssencePouch(PlayerProfile profile)
    {
        profile.migrateEssence();
        List<java.util.Map.Entry<com.osrsgo.model.Rarity, Integer>> held = new java.util.ArrayList<>();
        for (com.osrsgo.model.Rarity tier : com.osrsgo.model.Rarity.values())
        {
            Integer amount = profile.tierEssence.get(tier.name());
            if (amount != null && amount > 0)
            {
                held.add(new java.util.AbstractMap.SimpleEntry<>(tier, amount));
            }
        }
        if (held.isEmpty() && profile.shinyEssence <= 0)
        {
            return;
        }

        JPanel card = stackRow();
        JButton header = new JButton((essencePouchCollapsed ? "[+] " : "[-] ")
            + "Essence pouch");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(new Color(180, 150, 255));
        header.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        header.setBorderPainted(false);
        header.setContentAreaFilled(false);
        header.setMargin(new java.awt.Insets(0, 0, 0, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.addActionListener(e ->
        {
            essencePouchCollapsed = !essencePouchCollapsed;
            lastFingerprint = "";
            refresh();
        });
        card.add(header);
        if (!essencePouchCollapsed)
        {
            if (profile.shinyEssence > 0)
            {
                JPanel shinyLine = new JPanel(new BorderLayout());
                shinyLine.setOpaque(false);
                shinyLine.setAlignmentX(Component.LEFT_ALIGNMENT);
                shinyLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                JLabel shinyName = new JLabel("SHINY essence");
                shinyName.setForeground(SHINY_GOLD);
                shinyName.setFont(shinyName.getFont().deriveFont(Font.BOLD, 11f));
                shinyName.setToolTipText("From releasing shinies. Substitutes for ANY tier's essence"
                    + " when evolving or leveling.");
                JLabel shinyCount = new JLabel(String.valueOf(profile.shinyEssence));
                shinyCount.setForeground(SHINY_GOLD);
                shinyCount.setFont(shinyCount.getFont().deriveFont(Font.BOLD, 12f));
                shinyLine.add(shinyName, BorderLayout.WEST);
                shinyLine.add(shinyCount, BorderLayout.EAST);
                card.add(shinyLine);
            }
            for (java.util.Map.Entry<com.osrsgo.model.Rarity, Integer> entry : held)
            {
                JPanel line = new JPanel(new BorderLayout());
                line.setOpaque(false);
                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                JLabel name = new JLabel(entry.getKey().getDisplay() + " essence");
                name.setForeground(entry.getKey().getColor());
                name.setFont(name.getFont().deriveFont(11f));
                name.setToolTipText("Fed by catching or releasing any " + entry.getKey().getDisplay()
                    + " mon; spent evolving or leveling any of them.");
                JLabel count = new JLabel(String.valueOf(entry.getValue()));
                count.setForeground(new Color(180, 150, 255));
                count.setFont(count.getFont().deriveFont(Font.BOLD, 12f));
                line.add(name, BorderLayout.WEST);
                line.add(count, BorderLayout.EAST);
                card.add(line);
            }
        }
        dexContent.add(card);
        dexContent.add(Box.createVerticalStrut(4));
    }

    /** One button into the checkbox mass-release picker. */
    private void addCleanupSection(PlayerProfile profile)
    {
        JPanel card = stackRow();
        JButton open = new JButton("Cleanup: mass release...");
        open.setAlignmentX(Component.LEFT_ALIGNMENT);
        open.setToolTipText("Open your collection with checkboxes and release several at once."
            + " Favorites, team, buddy, and shinies are always safe.");
        open.addActionListener(e -> openCleanupDialog());
        card.add(open);
        dexContent.add(card);
    }

    /**
     * The storage grid with checkboxes: tick mons, release them together.
     * Protected mons (favorite/team/buddy/shiny) show why they can't go.
     */
    private void openCleanupDialog()
    {
        PlayerProfile p = plugin.getProfile();
        if (p.mons.isEmpty())
        {
            showToast("Nothing to clean up!");
            return;
        }
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null,
            "Gielinor Safari - Cleanup", true);
        JPanel body = new JPanel(new BorderLayout(6, 6));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        java.util.Set<OwnedMon> selected = new java.util.LinkedHashSet<>();
        List<javax.swing.JCheckBox> boxes = new java.util.ArrayList<>();
        List<OwnedMon> boxMons = new java.util.ArrayList<>();
        JLabel countLabel = new JLabel("0 selected");
        countLabel.setForeground(Color.LIGHT_GRAY);
        Runnable updateCount = () ->
        {
            int essence = 0;
            for (OwnedMon m : selected)
            {
                essence += plugin.releaseEssence(m);
            }
            countLabel.setText(selected.size() + " selected  (+" + essence + " essence)");
        };

        // Species with more than one copy, for the duplicates-only filter
        java.util.Map<Integer, Integer> speciesCounts = new java.util.HashMap<>();
        for (OwnedMon m : p.mons)
        {
            speciesCounts.merge(m.speciesId, 1, Integer::sum);
        }
        javax.swing.JCheckBox dupesOnly = new javax.swing.JCheckBox("Only show duplicates");
        dupesOnly.setOpaque(false);
        dupesOnly.setForeground(Color.LIGHT_GRAY);
        dupesOnly.setToolTipText("Show only species you own more than once; keepers stay hidden.");

        JPanel grid = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
        grid.setOpaque(false);
        Runnable rebuildGrid = () ->
        {
            grid.removeAll();
            boxes.clear();
            boxMons.clear();
            for (int i = 0; i < p.mons.size(); i++)
            {
                OwnedMon mon = p.mons.get(i);
                if (dupesOnly.isSelected() && speciesCounts.getOrDefault(mon.speciesId, 0) < 2)
                {
                    continue;
                }
                Species species = mon.species();
                boolean onTeam = p.teamIndices.contains(i);
                boolean isBuddy = p.buddyIndex != null && p.buddyIndex == i;
                String protectedBy = mon.shiny ? "shiny" : mon.favorite ? "favorite"
                    : onTeam ? "on team" : isBuddy ? "buddy" : null;

                JPanel cell = new JPanel(new BorderLayout(4, 0));
                cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                cell.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                javax.swing.JCheckBox box = new javax.swing.JCheckBox();
                box.setOpaque(false);
                JLabel label = new JLabel("<html>" + (mon.shiny ? "SHINY " : "") + mon.displayName()
                    + "  lvl " + mon.level + "<br>IV " + ivTotal(mon) + "/60"
                    + (protectedBy != null ? "  [" + protectedBy + "]" : "") + "</html>");
                label.setIcon(iconFor(species));
                label.setIconTextGap(5);
                label.setFont(label.getFont().deriveFont(11f));
                if (protectedBy != null)
                {
                    box.setEnabled(false);
                    label.setForeground(new Color(120, 120, 130));
                    String reason = "Protected (" + protectedBy + "); cannot be bulk released.";
                    label.setToolTipText(reason);
                    cell.setToolTipText(reason);
                }
                else
                {
                    box.setSelected(selected.contains(mon));
                    label.setForeground(mon.shiny ? SHINY_GOLD : species.getRarity().getColor());
                    box.addActionListener(e ->
                    {
                        if (box.isSelected())
                        {
                            selected.add(mon);
                        }
                        else
                        {
                            selected.remove(mon);
                        }
                        updateCount.run();
                    });
                    // The whole cell is a click target, not just the little box
                    label.addMouseListener(new java.awt.event.MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e)
                        {
                            box.doClick();
                        }
                    });
                    boxes.add(box);
                    boxMons.add(mon);
                }
                cell.add(box, BorderLayout.WEST);
                cell.add(label, BorderLayout.CENTER);
                grid.add(cell);
            }
            grid.revalidate();
            grid.repaint();
        };
        dupesOnly.addActionListener(e -> rebuildGrid.run());
        rebuildGrid.run();

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(grid,
            javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(hint("Tick the mons to release. Favorites, team, buddy, and shinies are safe."),
            BorderLayout.CENTER);
        north.add(dupesOnly, BorderLayout.SOUTH);
        body.add(north, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setOpaque(false);
        south.add(countLabel, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton all = new JButton("Select all");
        all.addActionListener(e ->
        {
            for (int b = 0; b < boxes.size(); b++)
            {
                boxes.get(b).setSelected(true);
                selected.add(boxMons.get(b));
            }
            updateCount.run();
        });
        JButton none = new JButton("Clear");
        none.addActionListener(e ->
        {
            for (javax.swing.JCheckBox b : boxes)
            {
                b.setSelected(false);
            }
            selected.clear();
            updateCount.run();
        });
        JButton release = new JButton("Release selected");
        release.addActionListener(e ->
        {
            if (selected.isEmpty())
            {
                return;
            }
            int confirm = javax.swing.JOptionPane.showConfirmDialog(dialog,
                "Release " + selected.size() + " mon" + (selected.size() == 1 ? "" : "s")
                    + " for essence? This cannot be undone.",
                "Mass release", javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
            if (confirm == javax.swing.JOptionPane.YES_OPTION)
            {
                expandedDex.clear();
                plugin.releaseMons(selected);
                dialog.dispose();
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        btns.add(all);
        btns.add(none);
        btns.add(close);
        btns.add(release);
        south.add(btns, BorderLayout.EAST);
        body.add(south, BorderLayout.SOUTH);

        dialog.setContentPane(body);
        dialog.setSize(520, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String collectionSort = "Newest";

    /**
     * Display order for the collection: team members always pinned first (in
     * team-slot order), then the chosen sort. Rows keep their REAL list index
     * so every button still acts on the right mon.
     */
    private List<Integer> sortedCollectionIndices(PlayerProfile profile)
    {
        List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < profile.mons.size(); i++)
        {
            order.add(i);
        }
        java.util.Comparator<Integer> byMode;
        switch (collectionSort)
        {
            case "Oldest":
                byMode = java.util.Comparator.naturalOrder();
                break;
            case "Name":
                byMode = java.util.Comparator.comparing(i -> profile.mons.get(i).displayName());
                break;
            case "Level":
                byMode = java.util.Comparator.comparingInt((Integer i) -> profile.mons.get(i).level).reversed();
                break;
            case "Type":
                byMode = java.util.Comparator
                    .comparing((Integer i) -> profile.mons.get(i).species().getType())
                    .thenComparing(i -> profile.mons.get(i).displayName());
                break;
            case "Rarity":
                byMode = java.util.Comparator
                    .comparing((Integer i) -> profile.mons.get(i).species().getRarity())
                    .reversed()
                    .thenComparing(java.util.Comparator.comparingInt(
                        (Integer i) -> profile.mons.get(i).level).reversed());
                break;
            case "Best IVs":
                byMode = java.util.Comparator
                    .comparingInt((Integer i) -> -ivTotal(profile.mons.get(i)))
                    .thenComparing(java.util.Comparator.comparingInt(
                        (Integer i) -> profile.mons.get(i).level).reversed());
                break;
            default:
                byMode = java.util.Comparator.<Integer>naturalOrder().reversed();
                break;
        }
        order.sort(java.util.Comparator
            .comparingInt((Integer i) ->
            {
                int slot = profile.teamIndices.indexOf(i);
                return slot >= 0 ? slot : Integer.MAX_VALUE;
            })
            .thenComparing(byMode));
        return order;
    }

    private void addHealCard(PlayerProfile profile)
    {
        int hurt = 0;
        for (OwnedMon m : profile.mons)
        {
            if (m.currentHp() < m.maxHp())
            {
                hurt++;
            }
        }
        if (hurt == 0)
        {
            return;
        }
        boolean nearBank = plugin.isNearBank();
        JPanel card = stackRow();
        JLabel label = new JLabel(hurt + (hurt == 1 ? " mon needs" : " mons need") + " healing");
        label.setForeground(new Color(240, 140, 100));
        card.add(label);
        JButton healBtn = new JButton(nearBank ? "Heal team" : "Heal at a bank or POH pool");
        healBtn.setEnabled(nearBank);
        healBtn.setToolTipText("Banks and POH pools are Heal Centers: free, instant, full restore");
        healBtn.addActionListener(e ->
        {
            String err = plugin.healTeam();
            if (err != null)
            {
                showToast(err);
            }
        });
        card.add(buttonStrip(healBtn));
        dexContent.add(card);
        dexContent.add(Box.createVerticalStrut(4));
    }

    private void addEggCards(PlayerProfile profile)
    {
        for (com.osrsgo.model.Egg egg : profile.eggs)
        {
            boolean nearHatch = egg.tilesProgress >= egg.tilesRequired * 0.9;
            JPanel card = stackRow();
            if (nearHatch)
            {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SHINY_GOLD, 1),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            }
            JLabel label = new JLabel(nearHatch ? "Mystery Egg - it's twitching!" : "Mystery Egg");
            label.setForeground(nearHatch ? SHINY_GOLD : new Color(240, 220, 180));
            if (nearHatch)
            {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            card.add(label);
            JProgressBar bar = new JProgressBar(0, egg.tilesRequired);
            bar.setValue(Math.min(egg.tilesProgress, egg.tilesRequired));
            bar.setStringPainted(true);
            bar.setString(egg.tilesProgress + "/" + egg.tilesRequired + " tiles walked");
            bar.setFont(bar.getFont().deriveFont(10f));
            bar.setForeground(nearHatch ? SHINY_GOLD : new Color(240, 200, 120));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(bar);
            dexContent.add(card);
            dexContent.add(Box.createVerticalStrut(4));
        }
    }

    private void addBreedingCard(PlayerProfile profile)
    {
        boolean remote = !plugin.isNearBreedingDen() && plugin.hasRemoteBreeding();
        if ((!plugin.isNearBreedingDen() && !plugin.hasRemoteBreeding()) || profile.mons.size() < 2)
        {
            return;
        }
        JPanel card = stackRow();
        JLabel label = new JLabel(remote ? "Gertrude's Breeding Den (remote)" : "Gertrude's Breeding Den");
        label.setForeground(new Color(255, 170, 200));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        card.add(label);
        card.add(hint("Breeding is how the very best mons are made: children inherit"
            + " parent IVs with a chance to improve, generation after generation."));
        JButton openBtn = new JButton("Open Breeding Den");
        openBtn.addActionListener(e -> openBreedingDialog());
        card.add(buttonStrip(openBtn));
        dexContent.add(card);
        dexContent.add(Box.createVerticalStrut(4));
    }

    /** The breeding popup: parent pickers with IVs, a child forecast, and the ledger. */
    private void openBreedingDialog()
    {
        PlayerProfile profile = profile();
        javax.swing.JDialog dialog = new javax.swing.JDialog(
            SwingUtilities.getWindowAncestor(this), "Gertrude's Breeding Den");
        dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        // Parents sorted best-first by IV total; map combo rows back to mon indices
        java.util.List<Integer> order = new java.util.ArrayList<>();

        JPanel breedTab = new JPanel();
        breedTab.setLayout(new BoxLayout(breedTab, BoxLayout.Y_AXIS));
        breedTab.setBackground(ColorScheme.DARK_GRAY_COLOR);
        breedTab.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel pitch = new JLabel("<html><body style='width:290px'>"
            + "<b>How breeding works:</b><br>"
            + "1. Pick two parents. The egg hatches into one of them (coin flip).<br>"
            + "2. Every mon has 4 hidden power numbers (0 to 15). The baby copies each"
            + " number from mom or dad, and each one has a 1-in-4 chance to come out a"
            + " little HIGHER.<br>"
            + "3. So: breed your two best, keep the baby if it beats them, repeat."
            + " That ladder is how the very best mons in the game are made.<br>"
            + "Bonus: " + Math.round(plugin.flawlessChance() * 100) + " in 100 eggs hatch"
            + " FLAWLESS (all 15s), and 1 in " + plugin.eggShinyDenominator()
            + " hatches SHINY.</body></html>");
        pitch.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        pitch.setFont(pitch.getFont().deriveFont(11f));
        pitch.setAlignmentX(Component.LEFT_ALIGNMENT);
        breedTab.add(pitch);
        breedTab.add(Box.createVerticalStrut(10));

        javax.swing.JComboBox<String> parentA = new javax.swing.JComboBox<>();
        javax.swing.JComboBox<String> parentB = new javax.swing.JComboBox<>();

        // Species filter narrows both parent lists (finding pairs in a big box)
        java.util.TreeMap<String, Integer> ownedSpecies = new java.util.TreeMap<>();
        for (OwnedMon m : profile.mons)
        {
            ownedSpecies.put(m.species().getName(), m.speciesId);
        }
        javax.swing.JComboBox<String> speciesFilter = new javax.swing.JComboBox<>();
        speciesFilter.addItem("All species");
        for (String name : ownedSpecies.keySet())
        {
            speciesFilter.addItem(name);
        }
        Runnable rebuildParents = () ->
        {
            Integer wanted = speciesFilter.getSelectedIndex() <= 0
                ? null
                : ownedSpecies.get((String) speciesFilter.getSelectedItem());
            order.clear();
            for (int i = 0; i < profile.mons.size(); i++)
            {
                if (wanted == null || profile.mons.get(i).speciesId == wanted)
                {
                    order.add(i);
                }
            }
            order.sort((x, y) -> Integer.compare(ivTotal(profile.mons.get(y)), ivTotal(profile.mons.get(x))));
            parentA.removeAllItems();
            parentB.removeAllItems();
            for (int idx : order)
            {
                OwnedMon m = profile.mons.get(idx);
                String entry = m.name() + "  lvl " + m.level + "  ["
                    + m.species().getRarity().getDisplay() + "]  " + ivTotal(m) + "/60"
                    + (m.shiny ? " *" : "");
                parentA.addItem(entry);
                parentB.addItem(entry);
            }
            if (parentB.getItemCount() > 1)
            {
                parentB.setSelectedIndex(1);
            }
        };
        rebuildParents.run();
        JLabel detailA = breedDetailLabel();
        JLabel detailB = breedDetailLabel();
        JProgressBar powerA = powerBar();
        JProgressBar powerB = powerBar();
        JLabel forecast = new JLabel();
        forecast.setForeground(new Color(180, 220, 180));
        forecast.setFont(forecast.getFont().deriveFont(11f));
        forecast.setAlignmentX(Component.LEFT_ALIGNMENT);

        Runnable updatePreview = () ->
        {
            if (order.isEmpty() || parentA.getSelectedIndex() < 0 || parentB.getSelectedIndex() < 0)
            {
                return;
            }
            OwnedMon a = profile.mons.get(order.get(parentA.getSelectedIndex()));
            OwnedMon b = profile.mons.get(order.get(parentB.getSelectedIndex()));
            detailA.setIcon(iconFor(a.species()));
            detailA.setText(breedIvLine(a));
            powerA.setValue(ivTotal(a));
            powerA.setString("power " + ivTotal(a) + " / 60");
            detailB.setIcon(iconFor(b.species()));
            detailB.setText(breedIvLine(b));
            powerB.setValue(ivTotal(b));
            powerB.setString("power " + ivTotal(b) + " / 60");
            forecast.setText("<html><body style='width:280px'>" + breedForecast(a, b) + "</body></html>");
            dialog.pack();
        };
        parentA.addActionListener(e -> updatePreview.run());
        parentB.addActionListener(e -> updatePreview.run());
        speciesFilter.addActionListener(e ->
        {
            rebuildParents.run();
            updatePreview.run();
        });

        JButton bestPair = new JButton("Pick best pair");
        bestPair.setFont(bestPair.getFont().deriveFont(11f));
        bestPair.setToolTipText("Selects your two highest-IV mons");
        bestPair.addActionListener(e ->
        {
            parentA.setSelectedIndex(0);
            if (parentB.getItemCount() > 1)
            {
                parentB.setSelectedIndex(1);
            }
        });
        JButton samePair = new JButton("Same pair");
        samePair.setFont(samePair.getFont().deriveFont(11f));
        samePair.setToolTipText("Re-selects the two parents from your last breeding");
        samePair.setEnabled(lastBredA != null && lastBredB != null
            && profile.mons.contains(lastBredA) && profile.mons.contains(lastBredB));
        samePair.addActionListener(e ->
        {
            speciesFilter.setSelectedIndex(0);
            int idxA = order.indexOf(profile.mons.indexOf(lastBredA));
            int idxB = order.indexOf(profile.mons.indexOf(lastBredB));
            if (idxA >= 0 && idxB >= 0)
            {
                parentA.setSelectedIndex(idxA);
                parentB.setSelectedIndex(idxB);
            }
        });
        JPanel pairBtns = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));
        pairBtns.setOpaque(false);
        pairBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        pairBtns.setMaximumSize(new Dimension(240, 26));
        pairBtns.add(bestPair);
        pairBtns.add(samePair);

        breedTab.add(sectionHeader("1. Pick the parents"));
        breedTab.add(comboRow("Show", speciesFilter));
        breedTab.add(Box.createVerticalStrut(4));
        breedTab.add(comboRow("Mom", parentA));
        breedTab.add(detailA);
        breedTab.add(powerA);
        breedTab.add(Box.createVerticalStrut(6));
        breedTab.add(comboRow("Dad", parentB));
        breedTab.add(detailB);
        breedTab.add(powerB);
        breedTab.add(Box.createVerticalStrut(4));
        breedTab.add(pairBtns);
        breedTab.add(Box.createVerticalStrut(10));
        breedTab.add(sectionHeader("2. What the egg can be"));
        breedTab.add(forecast);
        breedTab.add(Box.createVerticalStrut(10));
        breedTab.add(sectionHeader("3. Make the egg"));

        JLabel status = new JLabel(" ");
        status.setFont(status.getFont().deriveFont(11f));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        final Runnable[] eggsRef = new Runnable[1];
        JButton breedBtn = new JButton();
        Runnable updateBreedBtn = () ->
        {
            int eggs = profile().eggs.size();
            breedBtn.setText("Breed  (" + eggs + "/" + plugin.eggCap() + " eggs)");
            breedBtn.setEnabled(eggs < plugin.eggCap());
        };
        updateBreedBtn.run();
        breedBtn.addActionListener(e ->
        {
            if (order.isEmpty() || parentA.getSelectedIndex() < 0 || parentB.getSelectedIndex() < 0)
            {
                return;
            }
            OwnedMon pickA = profile.mons.get(order.get(parentA.getSelectedIndex()));
            OwnedMon pickB = profile.mons.get(order.get(parentB.getSelectedIndex()));
            String err = plugin.breed(order.get(parentA.getSelectedIndex()),
                order.get(parentB.getSelectedIndex()));
            if (err == null)
            {
                lastBredA = pickA;
                lastBredB = pickB;
                samePair.setEnabled(true);
            }
            if (err != null)
            {
                status.setForeground(new Color(230, 120, 120));
                status.setText("<html><body style='width:280px'>" + err + "</body></html>");
            }
            else
            {
                status.setForeground(new Color(140, 220, 140));
                status.setText("Gertrude tucks a mystery egg into your pack. Walk to hatch it!");
            }
            updateBreedBtn.run();
            if (eggsRef[0] != null)
            {
                eggsRef[0].run();
            }
            dialog.pack();
        });
        breedBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        breedTab.add(breedBtn);
        breedTab.add(Box.createVerticalStrut(4));
        breedTab.add(status);

        JPanel eggTray = new JPanel();
        eggTray.setLayout(new BoxLayout(eggTray, BoxLayout.Y_AXIS));
        eggTray.setOpaque(false);
        eggTray.setAlignmentX(Component.LEFT_ALIGNMENT);
        Runnable refreshEggs = () ->
        {
            eggTray.removeAll();
            java.util.List<com.osrsgo.model.Egg> eggs = profile().eggs;
            JLabel trayHeader = new JLabel("Egg tray (" + eggs.size() + "/" + plugin.eggCap() + ")");
            trayHeader.setForeground(new Color(255, 170, 200));
            trayHeader.setFont(trayHeader.getFont().deriveFont(Font.BOLD, 12f));
            trayHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
            eggTray.add(trayHeader);
            for (com.osrsgo.model.Egg egg : eggs)
            {
                JProgressBar bar = new JProgressBar(0, egg.tilesRequired);
                bar.setValue((int) Math.min(egg.tilesProgress, egg.tilesRequired));
                bar.setStringPainted(true);
                bar.setString("Mystery egg: " + egg.tilesProgress + " / " + egg.tilesRequired + " tiles");
                bar.setFont(bar.getFont().deriveFont(10f));
                bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
                bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                eggTray.add(bar);
                eggTray.add(Box.createVerticalStrut(2));
            }
            eggTray.revalidate();
            eggTray.repaint();
        };
        eggsRef[0] = refreshEggs;
        refreshEggs.run();
        breedTab.add(Box.createVerticalStrut(8));
        breedTab.add(eggTray);
        updatePreview.run();

        JPanel ledgerTab = new JPanel();
        ledgerTab.setLayout(new BoxLayout(ledgerTab, BoxLayout.Y_AXIS));
        ledgerTab.setBackground(ColorScheme.DARK_GRAY_COLOR);
        ledgerTab.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        addLedger(ledgerTab, profile);
        JScrollPane ledgerScroll = new JScrollPane(ledgerTab,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ledgerScroll.setPreferredSize(new Dimension(330, 380));
        ledgerScroll.setBorder(null);
        ledgerScroll.getVerticalScrollBar().setUnitIncrement(12);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Breed", breedTab);
        tabs.addTab("Gertrude's Ledger", ledgerScroll);
        tabs.setToolTipTextAt(1, "Breeding rules and every evolution chain, revealed by your GielDex progress");
        dialog.setContentPane(tabs);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------ Storage (PC box)

    private javax.swing.JDialog storageDialog;
    private OwnedMon storageSelected;

    /** The PC box: every owned mon as an icon grid with a detail pane and full management. */
    private void openStorageDialog()
    {
        if (storageDialog != null && storageDialog.isDisplayable())
        {
            storageDialog.toFront();
            return;
        }
        javax.swing.JDialog dialog = new javax.swing.JDialog(
            SwingUtilities.getWindowAncestor(this), "Mon Storage");
        storageDialog = dialog;
        dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        JPanel grid = new JPanel(new java.awt.GridLayout(0, 5, 4, 4));
        grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridWrap.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        gridWrap.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(gridWrap,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(400, 430));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel detail = new JPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        detail.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detail.setPreferredSize(new Dimension(230, 430));

        javax.swing.JComboBox<String> sortBox = new javax.swing.JComboBox<>(
            new String[]{"Level", "IV total", "Name", "Species", "Newest"});
        javax.swing.JTextField search = new javax.swing.JTextField(7);
        search.setToolTipText("Filter by name or species");
        javax.swing.JCheckBox shinyOnly = new javax.swing.JCheckBox("Shiny");
        shinyOnly.setOpaque(false);
        shinyOnly.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        JLabel countLabel = new JLabel();
        countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        countLabel.setFont(countLabel.getFont().deriveFont(11f));

        final Runnable[] reloadRef = new Runnable[1];
        reloadRef[0] = () ->
        {
            PlayerProfile p = profile();
            String query = search.getText() != null ? search.getText().trim().toLowerCase() : "";
            java.util.List<Integer> shown = new java.util.ArrayList<>();
            for (int i = 0; i < p.mons.size(); i++)
            {
                OwnedMon m = p.mons.get(i);
                if (shinyOnly.isSelected() && !m.shiny)
                {
                    continue;
                }
                if (!query.isEmpty()
                    && !m.displayName().toLowerCase().contains(query)
                    && !m.species().getName().toLowerCase().contains(query))
                {
                    continue;
                }
                shown.add(i);
            }
            java.util.Comparator<Integer> cmp;
            switch (sortBox.getSelectedIndex())
            {
                case 1: cmp = java.util.Comparator.comparingInt(i -> -ivTotal(p.mons.get(i))); break;
                case 2: cmp = java.util.Comparator.comparing(i -> p.mons.get((Integer) i).displayName(),
                    String.CASE_INSENSITIVE_ORDER); break;
                case 3: cmp = java.util.Comparator.<Integer>comparingInt(i -> p.mons.get(i).speciesId)
                    .thenComparingInt(i -> -p.mons.get(i).level); break;
                case 4: cmp = java.util.Comparator.comparingLong(i -> -p.mons.get((Integer) i).caughtAt); break;
                default: cmp = java.util.Comparator.comparingInt(i -> -p.mons.get((Integer) i).level); break;
            }
            shown.sort(cmp);

            grid.removeAll();
            for (int idx : shown)
            {
                OwnedMon m = p.mons.get(idx);
                boolean onTeam = p.teamIndices.contains(idx);
                boolean isBuddy = p.buddyIndex != null && p.buddyIndex == idx;
                String marks = (onTeam ? "*" : "") + (isBuddy ? "B" : "") + (m.favorite ? "F" : "");
                JButton cell = new JButton(m.level + (marks.isEmpty() ? "" : " " + marks), iconFor(m.species()));
                cell.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
                cell.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                cell.setMargin(new java.awt.Insets(2, 2, 2, 2));
                cell.setFont(cell.getFont().deriveFont(10f));
                cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                cell.setForeground(m.shiny ? SHINY_GOLD : Color.WHITE);
                Color edge = m == storageSelected ? Color.WHITE
                    : (onTeam ? ColorScheme.BRAND_ORANGE
                        : (m.shiny ? SHINY_GOLD : m.species().getRarity().getColor()));
                cell.setBorder(BorderFactory.createLineBorder(edge, m == storageSelected ? 2 : 1));
                cell.setToolTipText((m.shiny ? "SHINY " : "") + m.displayName() + " lvl " + m.level
                    + " [" + m.species().getRarity().getDisplay() + "]"
                    + (onTeam ? " On team." : "") + (isBuddy ? " Buddy." : "")
                    + (m.favorite ? " Favorite." : ""));
                cell.addActionListener(e ->
                {
                    storageSelected = m;
                    reloadRef[0].run();
                });
                grid.add(cell);
            }
            countLabel.setText(shown.size() + " of " + p.mons.size() + " shown");
            rebuildStorageDetail(detail, reloadRef[0]);
            grid.revalidate();
            grid.repaint();
            detail.revalidate();
            detail.repaint();
        };

        sortBox.addActionListener(e -> reloadRef[0].run());
        shinyOnly.addActionListener(e -> reloadRef[0].run());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            public void insertUpdate(javax.swing.event.DocumentEvent e)
            {
                reloadRef[0].run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e)
            {
                reloadRef[0].run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e)
            {
                reloadRef[0].run();
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JLabel sortCap = new JLabel("Sort");
        sortCap.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        toolbar.add(sortCap);
        toolbar.add(sortBox);
        toolbar.add(search);
        toolbar.add(shinyOnly);
        toolbar.add(countLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(detail, BorderLayout.EAST);
        dialog.setContentPane(content);
        reloadRef[0].run();
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void rebuildStorageDetail(JPanel detail, Runnable reload)
    {
        detail.removeAll();
        PlayerProfile p = profile();
        int index = storageSelected != null ? p.mons.indexOf(storageSelected) : -1;
        if (index < 0)
        {
            JLabel none = new JLabel("Select a mon.");
            none.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            detail.add(none);
            return;
        }
        OwnedMon mon = storageSelected;
        Species species = mon.species();
        boolean onTeam = p.teamIndices.contains(index);
        boolean isBuddy = p.buddyIndex != null && p.buddyIndex == index;

        JLabel name = new JLabel("<html><body style='width:150px'>" + (mon.shiny ? "SHINY " : "")
            + mon.displayName() + "</body></html>", iconFor(species), javax.swing.SwingConstants.LEFT);
        name.setForeground(mon.shiny ? SHINY_GOLD : species.getRarity().getColor());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.add(name);
        JLabel meta = new JLabel(species.getRarity().getDisplay() + " " + species.getType().getDisplay()
            + "  lvl " + mon.level);
        meta.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        meta.setFont(meta.getFont().deriveFont(11f));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.add(meta);
        String badges = (onTeam ? "Team slot " + (p.teamIndices.indexOf(index) + 1) + ". " : "")
            + (isBuddy ? "Buddy. " : "")
            + (mon.favorite ? "Favorite. " : "") + (mon.isHatched() ? "Hatched from an egg. " : "");
        if (!badges.isEmpty())
        {
            JLabel badgeLabel = new JLabel("<html><body style='width:150px'>" + badges + "</body></html>");
            badgeLabel.setForeground(new Color(140, 200, 255));
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(11f));
            badgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(badgeLabel);
        }
        detail.add(Box.createVerticalStrut(6));
        String[] lines = {
            "HP  " + mon.currentHp() + "/" + mon.maxHp() + "  (IV " + mon.ivHp + ")",
            "Atk " + mon.atk() + "  (IV " + mon.ivAtk + ")",
            "Def " + mon.def() + "  (IV " + mon.ivDef + ")",
            "Spd " + mon.spd() + "  (IV " + mon.ivSpd + ")",
            "IV total " + ivTotal(mon) + "/60",
            species.getRarity().getDisplay() + " essence: " + p.essenceOf(mon.speciesId),
        };
        for (String line : lines)
        {
            JLabel l = new JLabel(line);
            l.setForeground(Color.WHITE);
            l.setFont(l.getFont().deriveFont(11f));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            detail.add(l);
        }
        detail.add(Box.createVerticalStrut(8));

        JPanel btns = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        JButton teamBtn = new JButton(onTeam ? "Bench" : "Team");
        teamBtn.addActionListener(e ->
        {
            plugin.toggleTeamMember(p.mons.indexOf(storageSelected));
            reload.run();
        });
        JButton buddyBtn = new JButton(isBuddy ? "Unbuddy" : "Buddy");
        buddyBtn.addActionListener(e ->
        {
            int i = p.mons.indexOf(storageSelected);
            plugin.setBuddy(isBuddy || i < 0 ? null : i);
            reload.run();
        });
        JButton favBtn = new JButton(mon.favorite ? "Unfav" : "Fav");
        favBtn.addActionListener(e ->
        {
            plugin.toggleFavorite(p.mons.indexOf(storageSelected));
            reload.run();
        });
        JButton renameBtn = new JButton("Rename");
        renameBtn.addActionListener(e ->
        {
            String input = javax.swing.JOptionPane.showInputDialog(detail, "Nickname for "
                + species.getName() + " (blank to clear):", mon.nickname != null ? mon.nickname : "");
            if (input != null)
            {
                plugin.renameMon(p.mons.indexOf(storageSelected), input);
                reload.run();
            }
        });
        btns.add(teamBtn);
        btns.add(buddyBtn);
        btns.add(favBtn);
        btns.add(renameBtn);
        if (onTeam)
        {
            JButton upBtn = new JButton("Order ^");
            upBtn.setToolTipText("Move earlier in battle order (first mon leads)");
            upBtn.addActionListener(e ->
            {
                plugin.moveTeamMember(p.mons.indexOf(storageSelected), -1);
                reload.run();
            });
            JButton downBtn = new JButton("Order v");
            downBtn.setToolTipText("Move later in battle order");
            downBtn.addActionListener(e ->
            {
                plugin.moveTeamMember(p.mons.indexOf(storageSelected), 1);
                reload.run();
            });
            btns.add(upBtn);
            btns.add(downBtn);
        }
        com.osrsgo.data.EvolutionData.Evolution evo = com.osrsgo.data.EvolutionData.of(mon.speciesId);
        if (evo != null)
        {
            Species target = com.osrsgo.data.SpeciesData.byId(evo.toSpeciesId);
            JButton evolveBtn = new JButton("Evolve (" + evo.cost + ")");
            evolveBtn.setToolTipText("Evolve to " + target.getName() + " for " + evo.cost
                + " " + species.getRarity().getDisplay() + " essence");
            evolveBtn.addActionListener(e ->
            {
                String err = plugin.evolve(p.mons.indexOf(storageSelected), mon.speciesId);
                if (err != null)
                {
                    javax.swing.JOptionPane.showMessageDialog(detail, err);
                }
                reload.run();
            });
            btns.add(evolveBtn);
        }
        else if (mon.level < 99)
        {
            int essCost = mon.level + 1;
            JButton essBtn = new JButton("Essence lvl (" + essCost + ")");
            essBtn.setToolTipText("No evolution, so essence buys levels: level "
                + (mon.level + 1) + " costs " + essCost + " essence");
            essBtn.addActionListener(e ->
            {
                String err = plugin.levelWithEssence(p.mons.indexOf(storageSelected));
                if (err != null)
                {
                    javax.swing.JOptionPane.showMessageDialog(detail, err);
                }
                reload.run();
            });
            btns.add(essBtn);
        }
        if (p.rareCandies > 0 && mon.level < 99)
        {
            JButton candyBtn = new JButton("Candy +1 (" + p.rareCandies + ")");
            candyBtn.setToolTipText("Rare Candy: instant level");
            candyBtn.addActionListener(e ->
            {
                String err = plugin.useRareCandy(p.mons.indexOf(storageSelected));
                if (err != null)
                {
                    javax.swing.JOptionPane.showMessageDialog(detail, err);
                }
                reload.run();
            });
            btns.add(candyBtn);
        }
        JButton releaseBtn = new JButton("Release");
        releaseBtn.addActionListener(e ->
        {
            int sameSpecies = 0;
            for (int i = 0; i < p.mons.size(); i++)
            {
                OwnedMon other = p.mons.get(i);
                boolean protectedMon = other.favorite || other.shiny || p.teamIndices.contains(i)
                    || (p.buddyIndex != null && p.buddyIndex == i);
                if (other.speciesId == mon.speciesId && !protectedMon)
                {
                    sameSpecies++;
                }
            }
            String[] options = sameSpecies > 1
                ? new String[]{"Release this one",
                    "Release all " + species.getName() + "s (" + sameSpecies + ")", "Cancel"}
                : new String[]{"Release this one", "Cancel"};
            int choice = javax.swing.JOptionPane.showOptionDialog(detail,
                "Release " + species.getName() + " lvl " + mon.level + "? Releases grant essence"
                    + " and cannot be undone.",
                "Release", javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE, null, options, options[options.length - 1]);
            if (choice == 0)
            {
                plugin.releaseMon(p.mons.indexOf(storageSelected));
                storageSelected = null;
            }
            else if (sameSpecies > 1 && choice == 1)
            {
                plugin.releaseAllOfSpecies(mon.speciesId);
                storageSelected = null;
            }
            reload.run();
        });
        btns.add(releaseBtn);
        detail.add(btns);
    }

    private JPanel comboRow(String caption, javax.swing.JComboBox<String> combo)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel cap = new JLabel(caption);
        cap.setForeground(Color.WHITE);
        cap.setFont(cap.getFont().deriveFont(Font.BOLD, 11f));
        row.add(cap, BorderLayout.WEST);
        row.add(combo, BorderLayout.CENTER);
        return row;
    }

    private JLabel breedDetailLabel()
    {
        JLabel l = new JLabel();
        l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        l.setFont(l.getFont().deriveFont(11f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static int ivTotal(OwnedMon m)
    {
        return m.ivHp + m.ivAtk + m.ivDef + m.ivSpd;
    }

    private String breedIvLine(OwnedMon m)
    {
        return "<html>" + m.displayName() + "  lvl " + m.level
            + "<br>hidden numbers: HP " + m.ivHp + " | Atk " + m.ivAtk
            + " | Def " + m.ivDef + " | Spd " + m.ivSpd + "</html>";
    }

    private JProgressBar powerBar()
    {
        JProgressBar bar = new JProgressBar(0, 60);
        bar.setStringPainted(true);
        bar.setFont(bar.getFont().deriveFont(10f));
        bar.setForeground(new Color(120, 200, 120));
        bar.setMaximumSize(new Dimension(220, 14));
        bar.setPreferredSize(new Dimension(220, 14));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setToolTipText("All 4 hidden power numbers added up. 60 is perfect.");
        return bar;
    }

    private JLabel sectionHeader(String text)
    {
        JLabel header = new JLabel(text);
        header.setForeground(new Color(255, 170, 200));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        return header;
    }

    /** What this pairing can produce, with the real odds spelled out. */
    private String breedForecast(OwnedMon a, OwnedMon b)
    {
        int bestHp = Math.min(15, Math.max(a.ivHp, b.ivHp) + 3);
        int bestAtk = Math.min(15, Math.max(a.ivAtk, b.ivAtk) + 3);
        int bestDef = Math.min(15, Math.max(a.ivDef, b.ivDef) + 3);
        int bestSpd = Math.min(15, Math.max(a.ivSpd, b.ivSpd) + 3);
        String nameA = a.species().getName();
        String nameB = b.species().getName();
        double gA = breedGateChance(a);
        double gB = breedGateChance(b);
        // Coin flip picks the primary; a failed gate falls back to the other
        // parent's gate; both failing hatches a random common
        double pA = 0.5 * gA + 0.5 * (1 - gB) * gA;
        double pB = 0.5 * gB + 0.5 * (1 - gA) * gB;
        double pCommon = (1 - gA) * (1 - gB);
        StringBuilder sb = new StringBuilder("<b>What's inside:</b> ");
        if (a.speciesId == b.speciesId)
        {
            sb.append(nameA).append(" ").append(oddsText(pA + pB));
        }
        else
        {
            sb.append(nameA).append(" ").append(oddsText(pA))
                .append(", ").append(nameB).append(" ").append(oddsText(pB));
        }
        if (pCommon > 0.0005)
        {
            sb.append(", random common ").append(oddsText(pCommon));
        }
        sb.append(".");
        sb.append("<br><b>Best hidden numbers</b> this baby could roll: HP ").append(bestHp)
            .append(", Atk ").append(bestAtk).append(", Def ").append(bestDef)
            .append(", Spd ").append(bestSpd)
            .append(". Each number: 25% (1 in 4) to improve by 1-3.");
        sb.append("<br><b>Special rolls:</b> FLAWLESS (all 15s) ")
            .append(oddsText(plugin.flawlessChance()))
            .append(", SHINY 1 in ").append(plugin.eggShinyDenominator()).append(".");
        return sb.toString();
    }

    /** Chance the species passes its rarity gate when picked as the primary parent. */
    private static double breedGateChance(OwnedMon m)
    {
        switch (m.species().getRarity())
        {
            case EPIC:
                return 0.25;
            case LEGENDARY:
                return 0.08;
            default:
                return 1.0;
        }
    }

    /** Exact odds as a friendly string: percentage, plus 1-in-x when it's rare. */
    private static String oddsText(double p)
    {
        if (p >= 0.9995)
        {
            return "100%";
        }
        if (p < 0.0005)
        {
            return "0%";
        }
        String pct = p >= 0.10
            ? Math.round(p * 100) + "%"
            : String.format("%.1f%%", p * 100);
        if (p < 0.10)
        {
            pct += " (1 in " + Math.round(1 / p) + ")";
        }
        return pct;
    }

    /**
     * Gertrude's Ledger: breeding rules plus every evolution chain, with each
     * link revealed only as far as the GielDex knows it.
     */
    private void addLedger(JPanel card, PlayerProfile profile)
    {
        card.add(hint("Eggs inherit a random parent's IV per stat (25% chance to improve)."
            + " 3% of eggs are FLAWLESS (perfect IVs). Shiny odds from eggs: 1 in 32."));
        card.add(hint("Species gates: Common/Uncommon/Rare always pass down. Epic passes 25%,"
            + " Legendary 8%; failures fall back to the other parent, or hatch a common."));
        card.add(hint("Walk to hatch: 400 / 700 / 1200 / 2000 / 3000 tiles by rarity."));
        card.add(Box.createVerticalStrut(4));
        JLabel chainsHeader = new JLabel("Evolution chains");
        chainsHeader.setForeground(Color.WHITE);
        chainsHeader.setFont(chainsHeader.getFont().deriveFont(Font.BOLD, 12f));
        chainsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(chainsHeader);

        for (Integer root : com.osrsgo.data.EvolutionData.roots())
        {
            StringBuilder line = new StringBuilder();
            boolean anyKnown = false;
            int current = root;
            line.append(ledgerName(profile, current));
            anyKnown |= profile.seenSpecies.contains(current);
            com.osrsgo.data.EvolutionData.Evolution evo;
            while ((evo = com.osrsgo.data.EvolutionData.of(current)) != null)
            {
                line.append(" -> ").append(ledgerName(profile, evo.toSpeciesId))
                    .append(" (").append(evo.cost).append(")");
                anyKnown |= profile.seenSpecies.contains(evo.toSpeciesId);
                current = evo.toSpeciesId;
            }
            JLabel chain = new JLabel("<html><body style='width:170px'>" + line + "</body></html>");
            chain.setForeground(anyKnown ? ColorScheme.LIGHT_GRAY_COLOR : new Color(110, 110, 110));
            chain.setFont(chain.getFont().deriveFont(11f));
            Species rootSpecies = com.osrsgo.data.SpeciesData.byId(root);
            if (profile.caughtSpecies.contains(root))
            {
                chain.setIcon(iconFor(rootSpecies));
                chain.setIconTextGap(5);
            }
            chain.setToolTipText("Essence costs in parentheses, paid in the base species' rarity tier."
                + " Catch or release any mon of that tier, then evolve from the mon's Info card.");
            chain.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(chain);
        }
        card.add(Box.createVerticalStrut(6));
    }

    private String ledgerName(PlayerProfile profile, int speciesId)
    {
        if (profile.caughtSpecies.contains(speciesId))
        {
            return com.osrsgo.data.SpeciesData.byId(speciesId).getName();
        }
        if (profile.seenSpecies.contains(speciesId))
        {
            return com.osrsgo.data.SpeciesData.byId(speciesId).getName() + "?";
        }
        return "???";
    }

    /** The GielDex: every species as a numbered entry with caught/seen/unknown state. */
    private void rebuildPokedex(PlayerProfile profile)
    {
        List<Species> all = com.osrsgo.data.SpeciesData.all();
        dexContent.add(hint("GielDex: " + profile.caughtSpecies.size() + "/" + all.size()
            + " caught, " + profile.seenSpecies.size() + " seen."));

        java.util.Set<Integer> shinyOwned = new java.util.HashSet<>();
        for (OwnedMon m : profile.mons)
        {
            if (m.shiny)
            {
                shinyOwned.add(m.speciesId);
            }
        }

        // Entry numbers follow catalog order regardless of the active sort
        java.util.Map<Integer, Integer> dexNumbers = new java.util.HashMap<>();
        int n = 0;
        for (Species species : all)
        {
            dexNumbers.put(species.getId(), ++n);
        }

        for (Species species : all)
        {
            boolean caught = profile.caughtSpecies.contains(species.getId());
            boolean seen = profile.seenSpecies.contains(species.getId());

            JPanel row = stackRow();
            // Owned entries pop with a green edge and brighter card; unknowns fade back
            if (caught)
            {
                row.setBackground(new Color(38, 48, 38));
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(90, 200, 90)),
                    BorderFactory.createEmptyBorder(4, 5, 4, 6)));
            }
            else if (!seen)
            {
                row.setBackground(new Color(28, 28, 30));
            }
            String number = String.format("#%03d  ", dexNumbers.get(species.getId()));
            if (caught)
            {
                JLabel name = new JLabel(number + species.getName()
                    + (shinyOwned.contains(species.getId()) ? "  SHINY" : ""));
                name.setToolTipText(name.getText());
                name.setIcon(iconFor(species));
                name.setIconTextGap(6);
                name.setForeground(shinyOwned.contains(species.getId()) ? SHINY_GOLD : species.getRarity().getColor());
                row.add(name);
                addDetailLine(row, species.getRarity().getDisplay() + " " + species.getType().getDisplay()
                    + "   HP " + species.getBaseHp() + " Atk " + species.getBaseAtk()
                    + " Def " + species.getBaseDef() + " Spd " + species.getBaseSpd());
                addDetailLine(row, com.osrsgo.data.BiomeData.homeOf(species.getId()));
            }
            else if (seen)
            {
                JLabel name = new JLabel(number + species.getName());
                java.awt.image.BufferedImage silhouette = plugin.getNpcImages().darkenedCached(species);
                if (silhouette != null)
                {
                    name.setIcon(new javax.swing.ImageIcon(silhouette));
                    name.setIconTextGap(6);
                }
                name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                row.add(name);
                addDetailLine(row, "Seen, not caught. " + com.osrsgo.data.BiomeData.homeOf(species.getId()));
            }
            else
            {
                JLabel name = new JLabel(number + "???");
                name.setForeground(new Color(110, 110, 110));
                row.add(name);
            }
            dexContent.add(row);
            dexContent.add(Box.createVerticalStrut(3));
        }
    }

    private JPanel monRow(PlayerProfile profile, int index)
    {
        OwnedMon mon = profile.mons.get(index);
        Species species = mon.species();
        boolean onTeam = profile.teamIndices.contains(index);

        JPanel row = stackRow();
        if (onTeam)
        {
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        }
        boolean isBuddy = profile.buddyIndex != null && profile.buddyIndex == index;
        boolean fainted = mon.isFainted();
        String shownName = mon.displayName()
            + (mon.nickname != null && !mon.nickname.isEmpty() ? " the " + species.getName() : "");
        JLabel name = new JLabel((onTeam ? "*" + (profile.teamIndices.indexOf(index) + 1) + " " : "")
            + (isBuddy ? "[Buddy] " : "")
            + (mon.favorite ? "[Fav] " : "") + (mon.isHatched() ? "[Egg] " : "") + (mon.shiny ? "SHINY " : "")
            + shownName + "  lvl " + mon.level
            + (fainted ? "  FAINTED" : "  (" + species.getType().getDisplay() + ")"));
        name.setToolTipText(name.getText());
        name.setIcon(iconFor(species));
        name.setIconTextGap(6);
        name.setForeground(fainted ? new Color(150, 90, 90)
            : (mon.shiny ? SHINY_GOLD : species.getRarity().getColor()));
        JLabel info = new JLabel("HP " + mon.currentHp() + "/" + mon.maxHp() + "  Atk " + mon.atk()
            + "  Def " + mon.def() + "  Spd " + mon.spd()
            + ("Best IVs".equals(collectionSort) ? "  IV " + ivTotal(mon) + "/60" : ""));
        info.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        info.setFont(info.getFont().deriveFont(11f));

        JButton favBtn = new JButton(mon.favorite ? "Unfav" : "Fav");
        favBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
        favBtn.setToolTipText("Favorites are protected from bulk release");
        favBtn.addActionListener(e -> plugin.toggleFavorite(index));

        JButton buddyBtn = new JButton(isBuddy ? "Unbuddy" : "Buddy");
        buddyBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
        buddyBtn.setToolTipText("Your buddy gains XP as you walk");
        buddyBtn.addActionListener(e -> plugin.setBuddy(isBuddy ? null : index));

        boolean expanded = expandedDex.contains(index);
        JButton infoBtn = new JButton(expanded ? "Less" : "Info");
        infoBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
        infoBtn.addActionListener(e ->
        {
            if (!expandedDex.remove(index))
            {
                expandedDex.add(index);
            }
            lastFingerprint = "";
            refresh();
        });

        JButton teamBtn = new JButton(onTeam ? "Bench" : "Team");
        teamBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
        teamBtn.addActionListener(e -> plugin.toggleTeamMember(index));

        row.add(name);
        row.add(info);
        if (expanded)
        {
            row.add(Box.createVerticalStrut(4));
            addStatCard(row, mon, species, index);
        }
        if (onTeam)
        {
            JButton upBtn = new JButton("^");
            upBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
            upBtn.setToolTipText("Move earlier in battle order (first mon leads)");
            upBtn.addActionListener(e -> plugin.moveTeamMember(index, -1));
            JButton downBtn = new JButton("v");
            downBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
            downBtn.setToolTipText("Move later in battle order");
            downBtn.addActionListener(e -> plugin.moveTeamMember(index, 1));
            row.add(buttonStrip(infoBtn, favBtn, buddyBtn, teamBtn, upBtn, downBtn));
        }
        else
        {
            row.add(buttonStrip(infoBtn, favBtn, buddyBtn, teamBtn));
        }
        return row;
    }

    /** The expanded per-mon stat card: XP bar, IV breakdown, moveset, origin, evolution. */
    private void addStatCard(JPanel row, OwnedMon mon, Species species, int index)
    {
        JProgressBar xpBar = new JProgressBar(0, mon.xpForNextLevel());
        xpBar.setValue(Math.min(mon.xp, mon.xpForNextLevel()));
        xpBar.setStringPainted(true);
        xpBar.setString("XP " + mon.xp + "/" + mon.xpForNextLevel());
        xpBar.setFont(xpBar.getFont().deriveFont(10f));
        xpBar.setForeground(new Color(90, 160, 255));
        xpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        xpBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(xpBar);
        row.add(Box.createVerticalStrut(4));

        addDetailLine(row, statLine("HP", mon.maxHp(), mon.ivHp));
        addDetailLine(row, statLine("Atk", mon.atk(), mon.ivAtk));
        addDetailLine(row, statLine("Def", mon.def(), mon.ivDef));
        addDetailLine(row, statLine("Spd", mon.spd(), mon.ivSpd));
        row.add(Box.createVerticalStrut(4));

        JLabel movesHeader = new JLabel("Moves");
        movesHeader.setForeground(Color.WHITE);
        movesHeader.setFont(movesHeader.getFont().deriveFont(Font.BOLD, 11f));
        movesHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(movesHeader);
        for (Integer moveId : MoveData.movesetFor(species.getType(), species.getRarity()))
        {
            Move move = MoveData.byId(moveId);
            String line = move.isGuard()
                ? move.getName() + "  (halves damage taken)"
                : move.getName() + "  " + move.getPower() + " pwr, "
                    + Math.round(move.getAccuracy() * 100) + "% acc";
            JLabel moveLabel = new JLabel(line);
            moveLabel.setForeground(move.getType().getColor());
            moveLabel.setFont(moveLabel.getFont().deriveFont(11f));
            moveLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(moveLabel);
        }
        row.add(Box.createVerticalStrut(4));
        addDetailLine(row, "Caught near " + mon.caughtNear);
        if (mon.isHatched())
        {
            addDetailLine(row, "Hatched by " + (mon.hatchedBy != null ? mon.hatchedBy : "an unknown trainer"));
        }

        JButton renameBtn = new JButton(mon.nickname != null ? "Rename" : "Nickname");
        renameBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
        renameBtn.addActionListener(e ->
        {
            String input = javax.swing.JOptionPane.showInputDialog(this,
                "Nickname for " + species.getName() + " (blank to clear):",
                mon.nickname != null ? mon.nickname : "");
            if (input != null)
            {
                plugin.renameMon(index, input);
            }
        });
        row.add(buttonStrip(renameBtn));

        PlayerProfile profile2 = plugin.getProfile();
        int sameSpecies = 0;
        for (int i = 0; i < profile2.mons.size(); i++)
        {
            OwnedMon other = profile2.mons.get(i);
            boolean protectedMon = other.favorite || other.shiny || profile2.teamIndices.contains(i)
                || (profile2.buddyIndex != null && profile2.buddyIndex == i);
            if (other.speciesId == mon.speciesId && !protectedMon)
            {
                sameSpecies++;
            }
        }
        final int speciesCount = sameSpecies;
        JButton releaseBtn = new JButton("Release");
        releaseBtn.setToolTipText("Release for essence (permanent), with an option to release"
            + " every non-favorite " + species.getName());
        releaseBtn.addActionListener(e ->
        {
            String[] options = speciesCount > 1
                ? new String[]{"Release this one",
                    "Release all " + species.getName() + "s (" + speciesCount + ")", "Cancel"}
                : new String[]{"Release this one", "Cancel"};
            int choice = javax.swing.JOptionPane.showOptionDialog(this,
                "Release " + species.getName() + " lvl " + mon.level + "? Releases grant essence"
                    + " and cannot be undone."
                    + (speciesCount > 1
                        ? "\n\"Release all\" takes every non-favorite " + species.getName()
                            + "; favorites, team, buddy, and shinies are safe."
                        : ""),
                "Release", javax.swing.JOptionPane.DEFAULT_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE, null, options, options[options.length - 1]);
            if (choice == 0)
            {
                expandedDex.clear();
                plugin.releaseMon(index);
            }
            else if (speciesCount > 1 && choice == 1)
            {
                expandedDex.clear();
                plugin.releaseAllOfSpecies(mon.speciesId);
            }
        });
        row.add(buttonStrip(releaseBtn));

        com.osrsgo.data.EvolutionData.Evolution evo = com.osrsgo.data.EvolutionData.of(mon.speciesId);
        int have = plugin.getProfile().essenceOf(mon.speciesId);
        addDetailLine(row, species.getRarity().getDisplay() + " essence: " + have
            + "  (from catching or releasing any " + species.getRarity().getDisplay() + " mon)");
        if (evo != null)
        {
            Species target = com.osrsgo.data.SpeciesData.byId(evo.toSpeciesId);
            int shinyEss = plugin.getProfile().shinyEssence;
            JButton evolveBtn = new JButton("Evolve to " + target.getName() + " (" + evo.cost + ")");
            evolveBtn.setEnabled(have + shinyEss >= evo.cost);
            evolveBtn.setToolTipText(have + shinyEss >= evo.cost
                ? "Consumes " + evo.cost + " " + species.getRarity().getDisplay() + " essence"
                    + (have < evo.cost ? " (shiny essence covers the gap)" : "") + ". Keeps level, IVs, and shiny."
                : "Needs " + evo.cost + " essence: you have " + have + " " + species.getRarity().getDisplay()
                    + " + " + shinyEss + " shiny");
            evolveBtn.addActionListener(e ->
            {
                String err = plugin.evolve(index, mon.speciesId);
                if (err != null)
                {
                    showToast(err);
                }
            });
            row.add(buttonStrip(evolveBtn));
        }
        else if (mon.level < 99)
        {
            // No evolution: essence buys levels instead (level N costs N)
            int cost = mon.level + 1;
            int shinyEss = plugin.getProfile().shinyEssence;
            JButton essLevelBtn = new JButton("Level up (" + cost + " essence)");
            essLevelBtn.setEnabled(have + shinyEss >= cost);
            essLevelBtn.setToolTipText(species.getName() + " doesn't evolve, so its essence powers"
                + " levels: reaching level " + (mon.level + 1) + " costs " + cost + " essence. You have "
                + have + " + " + shinyEss + " shiny.");
            essLevelBtn.addActionListener(e ->
            {
                String err = plugin.levelWithEssence(index);
                if (err != null)
                {
                    showToast(err);
                }
            });
            row.add(buttonStrip(essLevelBtn));
        }
        int candies = plugin.getProfile().rareCandies;
        if (candies > 0 && mon.level < 99)
        {
            JButton candyBtn = new JButton("Rare Candy +1 lvl (" + candies + " held)");
            candyBtn.setForeground(new Color(190, 110, 250));
            candyBtn.setToolTipText("Instantly grows " + mon.name() + " to level " + (mon.level + 1));
            candyBtn.addActionListener(e ->
            {
                String err = plugin.useRareCandy(index);
                if (err != null)
                {
                    showToast(err);
                }
            });
            row.add(buttonStrip(candyBtn));
        }
    }

    private String statLine(String label, int value, int iv)
    {
        String quality = iv >= 13 ? " (best)" : (iv >= 8 ? " (good)" : "");
        return label + " " + value + "   IV " + iv + "/15" + quality;
    }

    private void addDetailLine(JPanel row, String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(label.getFont().deriveFont(11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label);
    }

    // ------------------------------------------------------------------ Gyms

    private void rebuildGyms(PlayerProfile profile)
    {
        gymContent.removeAll();
        WorldPoint player = plugin.getPlayerLocation();
        if (profile.faction == null)
        {
            JPanel card = stackRow();
            JLabel title = new JLabel("Choose your god");
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            card.add(title);
            card.add(hint("Pledge once, forever. Gyms held by your god's followers are allies you cannot attack."));
            JButton sara = factionButton("Saradomin", "SARADOMIN", new Color(90, 160, 255));
            JButton zammy = factionButton("Zamorak", "ZAMORAK", new Color(255, 90, 90));
            JButton guthix = factionButton("Guthix", "GUTHIX", new Color(90, 220, 90));
            card.add(buttonStrip(sara, zammy, guthix));
            gymContent.add(card);
            gymContent.add(Box.createVerticalStrut(4));
        }
        boolean gymControlOn = plugin.gymControlOn();
        int held = plugin.gymsHeldNow();
        if (gymControlOn)
        {
            double mult = plugin.tributeMultiplier();
            int others = plugin.getOtherPartyMembers().size();
            String holdingHint;
            if (held == 0)
            {
                holdingHint = "Claim a gym and rival trainers will come for it.";
            }
            else if (others == 0)
            {
                holdingHint = "Holding " + held + (held == 1 ? " gym" : " gyms")
                    + ". Rival trainers will come for " + (held == 1 ? "it." : "them.");
            }
            else
            {
                holdingHint = "Holding " + held + (held == 1 ? " gym" : " gyms")
                    + ", party of " + (others + 1)
                    + ": tribute x" + String.format("%.2f", mult)
                    + " (party members can take your gyms)";
            }
            gymContent.add(hint(holdingHint));
        }
        for (GymData.Gym gym : gymsByDistance(player))
        {
            gymContent.add(gymRow(gym, profile, player));
            gymContent.add(Box.createVerticalStrut(4));
        }
        gymContent.add(hint(gymControlOn
            ? "Walk to a gym mark (cyan diamond) and battle the leader to earn its badge. "
                + "Holds expire 6 hours after a claim."
            : "Walk to a gym mark (cyan diamond) and battle the leader to earn its badge."));
    }

    /** Time until a hold auto-expires (6 hours from claim). Cleared locally, never on any server. */
    private String holdTimeLeft(com.osrsgo.gym.GymHolder holder)
    {
        Long mins = holdMinutesLeft(holder);
        if (mins == null)
        {
            return "";
        }
        if (mins <= 0)
        {
            return "(expiring...)";
        }
        return "(" + (mins >= 60 ? (mins / 60) + "h " : "") + (mins % 60) + "m left)";
    }

    private Long holdMinutesLeft(com.osrsgo.gym.GymHolder holder)
    {
        if (holder == null || holder.heldSince == null)
        {
            return null;
        }
        try
        {
            long held = java.time.Instant.parse(holder.heldSince).toEpochMilli();
            long expiresAt = held + 6L * 60 * 60 * 1000;
            return Math.max(0, (expiresAt - System.currentTimeMillis()) / 60000);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** Nearest gym first; falls back to the fixed order when the player location is unknown. */
    private java.util.List<GymData.Gym> gymsByDistance(WorldPoint player)
    {
        java.util.List<GymData.Gym> gyms = new java.util.ArrayList<>(GymData.all());
        if (player != null)
        {
            gyms.sort(java.util.Comparator.comparingInt(g -> player.distanceTo2D(g.location)));
        }
        return gyms;
    }

    private JButton factionButton(String label, String faction, Color color)
    {
        JButton btn = new JButton(label);
        btn.setForeground(color);
        btn.addActionListener(e ->
        {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "Pledge yourself to " + label + "? This choice is permanent.",
                "Choose your god", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm == javax.swing.JOptionPane.YES_OPTION)
            {
                String err = plugin.chooseFaction(faction);
                if (err != null)
                {
                    showToast(err);
                }
            }
        });
        return btn;
    }

    private JPanel gymRow(GymData.Gym gym, PlayerProfile profile, WorldPoint player)
    {
        boolean beaten = profile.badges.contains(gym.badge);
        boolean inRange = plugin.inGymRange(gym);
        boolean gymControlOn = plugin.gymControlOn();
        com.osrsgo.gym.GymHolder holder = gymControlOn ? plugin.getGymHolder(gym.id) : null;
        OsrsGoPlugin.GymOwnership ownership = gymControlOn
            ? plugin.gymOwnership(gym.id) : OsrsGoPlugin.GymOwnership.UNCLAIMED;

        JPanel row = stackRow();
        JLabel name = new JLabel((beaten ? "[Badge] " : "") + gym.name);
        name.setToolTipText(gym.name + " - leader " + gym.leader);
        if (gymControlOn && ownership == OsrsGoPlugin.GymOwnership.MINE)
        {
            name.setForeground(new Color(255, 200, 0));
        }
        else if (gymControlOn && ownership == OsrsGoPlugin.GymOwnership.ENEMY)
        {
            name.setForeground(new Color(255, 110, 110));
        }
        else
        {
            name.setForeground(beaten ? new Color(255, 200, 0) : Color.WHITE);
        }
        String distText = player != null && player.getPlane() == 0
            ? player.distanceTo(gym.location) + " tiles away"
            : "";
        boolean ally = gymControlOn && ownership == OsrsGoPlugin.GymOwnership.ENEMY
            && com.osrsgo.gym.LocalGyms.isAlly(plugin.getFaction(), holder,
                plugin.isPartyMemberName(holder == null ? null : holder.holderRsn));
        String holderText;
        if (!gymControlOn)
        {
            // Gym control is off: no holder state applies, so this is the
            // gym's pre-feature form, same as when the badge is simply unearned
            holderText = "Leader: " + gym.leader;
        }
        else if (ownership == OsrsGoPlugin.GymOwnership.MINE)
        {
            holderText = "Your gym!" + (holder.defenseWins > 0 ? " " + defenses(holder.defenseWins) : "")
                + " " + holdTimeLeft(holder);
        }
        else if (holder != null && holder.isClaimed())
        {
            holderText = "Held by " + holder.holderRsn
                + (holder.holderFaction != null && !holder.holderFaction.isEmpty()
                    ? " [" + holder.holderFaction.charAt(0)
                        + holder.holderFaction.substring(1).toLowerCase() + "]" : "")
                + (ally ? " (ally)" : "")
                + (holder.defenseWins > 0 ? " (" + defenses(holder.defenseWins) + ")" : "")
                + " " + holdTimeLeft(holder);
        }
        else
        {
            holderText = "Unclaimed! Leader: " + gym.leader;
        }
        JLabel info = new JLabel(holderText);
        info.setToolTipText(holderText);
        info.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        info.setFont(info.getFont().deriveFont(11f));
        JLabel dist = new JLabel(distText);
        dist.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        dist.setFont(dist.getFont().deriveFont(11f));

        String btnText;
        if (!gymControlOn)
        {
            btnText = beaten ? "Rematch" : "Battle";
        }
        else if (ownership == OsrsGoPlugin.GymOwnership.MINE)
        {
            btnText = "Yours";
        }
        else if (ownership == OsrsGoPlugin.GymOwnership.ENEMY)
        {
            btnText = ally ? "Allied" : "Take over";
        }
        else
        {
            // Beating the leader claims the open gym: say so
            btnText = "Claim gym";
        }
        JButton battleBtn = new JButton(btnText);
        boolean mineNow = gymControlOn && ownership == OsrsGoPlugin.GymOwnership.MINE;
        battleBtn.setEnabled(inRange && !mineNow && !ally);
        if (!inRange && !mineNow)
        {
            battleBtn.setToolTipText("Walk within 10 tiles of the gym mark to battle");
        }
        battleBtn.addActionListener(e ->
        {
            String err = plugin.startGymBattle(gym.id);
            if (err != null)
            {
                showToast(err);
            }
            else
            {
                tabs.setSelectedIndex(3);
            }
        });

        row.add(name);
        row.add(info);
        if (!distText.isEmpty())
        {
            row.add(dist);
        }

        com.osrsgo.data.RaidData.Raid raid = plugin.raidAtGym(gym.id);
        if (raid != null)
        {
            boolean challenged = plugin.raidAlreadyAttempted(gym.id);
            Species boss = com.osrsgo.data.SpeciesData.byId(raid.speciesId);
            JLabel raidLabel = new JLabel(challenged
                ? "[Done] RAID: " + boss.getName() + " lvl " + raid.level
                : "RAID: " + boss.getName() + " lvl " + raid.level + "!");
            raidLabel.setIcon(iconFor(boss));
            raidLabel.setIconTextGap(6);
            raidLabel.setForeground(challenged ? new Color(140, 140, 150) : new Color(220, 120, 255));
            raidLabel.setFont(raidLabel.getFont().deriveFont(Font.BOLD, 11f));
            raidLabel.setToolTipText(challenged
                ? "You already challenged this raid boss. A new one arrives next rotation."
                : "A raid boss towers over this gym!");
            row.add(raidLabel);
            JButton raidBtn = new JButton(challenged ? "Challenged" : "Fight raid boss");
            raidBtn.setEnabled(inRange && !challenged);
            raidBtn.setToolTipText(challenged
                ? "One attempt per rotation; a fresh boss arrives with the next wave"
                : "Beat it for xp and level-scaled catch throws. One attempt per rotation!"
                    + " Raids do NOT claim the gym; use the gym button for that.");
            raidBtn.addActionListener(e ->
            {
                String err = plugin.startRaidBattle(gym.id);
                if (err != null)
                {
                    showToast(err);
                }
                else
                {
                    tabs.setSelectedIndex(3);
                }
            });
            row.add(buttonStrip(raidBtn, battleBtn));
        }
        else
        {
            row.add(buttonStrip(battleBtn));
        }
        return row;
    }

    private static String defenses(int wins)
    {
        return wins == 1 ? "1 defense" : wins + " defenses";
    }

    // ------------------------------------------------------------------ Battle

    private void rebuildBattle()
    {
        battleContent.removeAll();
        BattleSession session = plugin.getSession();
        if (session == null)
        {
            rebuildLobby();
            return;
        }

        boolean holderPhase = session.gymHolderRsn != null
            && !session.currentOpponentName().equals(session.opponentName);
        battleContent.add(hint(session.mode == BattleSession.Mode.AI
            ? (holderPhase
                ? "Gym battle: fighting " + session.gymHolderRsn + "'s defenders (then " + session.opponentName + ")"
                : "Gym battle vs " + session.opponentName)
            : "PvP battle vs " + session.opponentName + " (turn " + session.turn + ")"));

        battleContent.add(monStatus(session.oppMon(), session.currentOpponentName(), false));
        battleContent.add(Box.createVerticalStrut(6));
        battleContent.add(monStatus(session.myMon(), "You", true));
        battleContent.add(Box.createVerticalStrut(6));

        if (!session.finished)
        {
            boolean waiting = session.myPendingMove != null;
            BattleMon mine = session.myMon();
            JPanel moves = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
            moves.setOpaque(false);
            moves.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (int i = 0; i < mine.moveIds.size(); i++)
            {
                Move move = MoveData.byId(mine.moveIds.get(i));
                boolean tired = !move.isGuard() && move.getId() == mine.lastMoveId;
                String label = move.isGuard()
                    ? move.getName()
                    : move.getName() + " (" + move.getPower() + ")" + (tired ? " ~" : "");
                JButton btn = new JButton(label);
                btn.setToolTipText(move.isGuard()
                    ? "Halve incoming damage this turn"
                    : move.getType().getDisplay() + ", power " + move.getPower()
                        + ", " + Math.round(move.getAccuracy() * 100) + "% accuracy"
                        + (tired ? ". Fatigued: repeating it again deals less damage!" : ""));
                btn.setEnabled(!waiting);
                final int idx = i;
                btn.addActionListener(e -> plugin.chooseMove(idx));
                moves.add(btn);
            }
            battleContent.add(moves);
            if (waiting)
            {
                battleContent.add(hint("Waiting for " + session.opponentName + "..."));
            }
            battleContent.add(Box.createVerticalStrut(6));
            JButton forfeit = new JButton("Forfeit");
            forfeit.addActionListener(e -> plugin.forfeit());
            forfeit.setAlignmentX(Component.LEFT_ALIGNMENT);
            battleContent.add(forfeit);
        }
        else
        {
            JLabel result = new JLabel(session.wonByMe ? "VICTORY!" : "DEFEAT");
            result.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            result.setForeground(session.wonByMe ? new Color(80, 220, 80) : ColorScheme.PROGRESS_ERROR_COLOR);
            result.setAlignmentX(Component.LEFT_ALIGNMENT);
            battleContent.add(result);
            JButton close = new JButton("Close battle");
            close.setAlignmentX(Component.LEFT_ALIGNMENT);
            close.addActionListener(e -> plugin.closeBattle());
            battleContent.add(close);
        }

        battleContent.add(Box.createVerticalStrut(6));
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        logArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        logArea.setFont(logArea.getFont().deriveFont(11f));
        List<String> log = session.fullLog;
        int from = Math.max(0, log.size() - 14);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < log.size(); i++)
        {
            sb.append(log.get(i)).append('\n');
        }
        logArea.setText(sb.toString());
        logArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        battleContent.add(logArea);
    }

    private void rebuildLobby()
    {
        addIncomingTradeCard();
        addOutgoingTradeCard();
        String pendingChallenger = plugin.getPendingChallengerName();
        if (pendingChallenger != null)
        {
            battleContent.add(hint(pendingChallenger + " challenges you to a battle!"));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            buttons.setOpaque(false);
            JButton accept = new JButton("Accept");
            accept.addActionListener(e -> plugin.acceptChallenge());
            JButton decline = new JButton("Decline");
            decline.addActionListener(e -> plugin.declineChallenge());
            buttons.add(accept);
            buttons.add(decline);
            battleContent.add(buttons);
            battleContent.add(Box.createVerticalStrut(8));
        }

        if (!plugin.isInParty())
        {
            battleContent.add(hint("Join a RuneLite party (Party plugin) to battle other trainers. Gym battles work anytime."));
            return;
        }
        List<PartyMember> others = plugin.getOtherPartyMembers();
        if (others.isEmpty())
        {
            battleContent.add(hint("You're in a party, but alone. Invite a friend!"));
            return;
        }
        if (plugin.hasOutgoingChallenge())
        {
            battleContent.add(hint("Challenge sent. Waiting for a response..."));
        }
        battleContent.add(hint("Party members:"));
        for (PartyMember member : others)
        {
            JPanel row = rowPanel();
            String display = member.getDisplayName() != null ? member.getDisplayName() : ("Member " + member.getMemberId());
            JLabel name = new JLabel(display);
            name.setForeground(Color.WHITE);
            JButton challengeBtn = new JButton("Challenge");
            challengeBtn.setEnabled(!plugin.hasOutgoingChallenge());
            final long id = member.getMemberId();
            challengeBtn.addActionListener(e ->
            {
                String err = plugin.challenge(id);
                if (err != null)
                {
                    showToast(err);
                }
            });
            JButton tradeBtn = new JButton("Trade");
            tradeBtn.setEnabled(plugin.tradeSession() == null);
            tradeBtn.addActionListener(e ->
            {
                String err = plugin.startTrade(id);
                if (err != null)
                {
                    showToast(err);
                }
                else
                {
                    openTradeDialog();
                }
            });
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            btns.setOpaque(false);
            btns.add(tradeBtn);
            btns.add(challengeBtn);
            row.add(name, BorderLayout.CENTER);
            row.add(btns, BorderLayout.EAST);
            battleContent.add(row);
            battleContent.add(Box.createVerticalStrut(4));
        }

    }

    private void addIncomingTradeCard()
    {
        String from = plugin.getIncomingTradeFrom();
        if (from == null)
        {
            return;
        }
        JPanel card = stackRow();
        JLabel title = new JLabel(from + " wants to trade!");
        title.setForeground(new Color(150, 220, 255));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        card.add(title);
        card.add(hint("Opens a trade window where you both place mons and accept."));
        JButton open = new JButton("Open trade");
        open.addActionListener(e ->
        {
            String err = plugin.acceptIncomingTrade();
            if (err != null)
            {
                showToast(err);
            }
            else
            {
                openTradeDialog();
            }
        });
        JButton decline = new JButton("Decline");
        decline.addActionListener(e -> plugin.declineIncomingTrade());
        card.add(buttonStrip(decline, open));
        battleContent.add(card);
        battleContent.add(Box.createVerticalStrut(4));
    }

    private void addOutgoingTradeCard()
    {
        OsrsGoPlugin.TradeSession trade = plugin.tradeSession();
        if (trade == null)
        {
            return;
        }
        JPanel card = stackRow();
        JLabel label = new JLabel("Trading with " + trade.partnerName + "...");
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        card.add(label);
        JButton open = new JButton("Open trade window");
        open.addActionListener(e -> openTradeDialog());
        JButton cancel = new JButton("Cancel trade");
        cancel.addActionListener(e -> plugin.cancelTrade());
        card.add(buttonStrip(cancel, open));
        battleContent.add(card);
        battleContent.add(Box.createVerticalStrut(4));
    }

    // ------------------------------------------------------------------ trade window

    private javax.swing.JDialog tradeDialog;
    private JPanel tradeDialogBody;

    /** Session state the fingerprint watches so the window rebuilds on changes. */
    private String tradeFingerprint()
    {
        OsrsGoPlugin.TradeSession t = plugin.tradeSession();
        if (t == null)
        {
            return "none";
        }
        StringBuilder sb = new StringBuilder("t").append(t.myAccepted).append(t.theirAccepted)
            .append(t.partnerOpened).append('m');
        for (OwnedMon m : t.myOffer)
        {
            sb.append(m.speciesId).append('.').append(m.level).append(',');
        }
        sb.append('o');
        for (OwnedMon m : t.theirOffer)
        {
            sb.append(m.speciesId).append('.').append(m.level).append(',');
        }
        return sb.toString();
    }

    private void openTradeDialog()
    {
        if (plugin.tradeSession() == null)
        {
            return;
        }
        if (tradeDialog != null)
        {
            tradeDialog.toFront();
            return;
        }
        tradeDialog = new javax.swing.JDialog((java.awt.Frame) null, "Gielinor Safari - Trade", false);
        tradeDialogBody = new JPanel(new BorderLayout(6, 6));
        tradeDialogBody.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tradeDialogBody.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tradeDialog.setContentPane(tradeDialogBody);
        tradeDialog.setSize(620, 560);
        tradeDialog.setLocationRelativeTo(this);
        tradeDialog.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                // Closing the window walks away from the trade, like OSRS
                tradeDialog = null;
                tradeDialogBody = null;
                if (plugin.tradeSession() != null)
                {
                    plugin.cancelTrade();
                }
            }
        });
        rebuildTradeDialog();
        tradeDialog.setVisible(true);
    }

    private void updateTradeDialog()
    {
        if (tradeDialog == null)
        {
            return;
        }
        if (plugin.tradeSession() == null)
        {
            javax.swing.JDialog dialog = tradeDialog;
            tradeDialog = null;
            tradeDialogBody = null;
            dialog.dispose();
            return;
        }
        rebuildTradeDialog();
    }

    private void rebuildTradeDialog()
    {
        OsrsGoPlugin.TradeSession t = plugin.tradeSession();
        if (t == null || tradeDialogBody == null)
        {
            return;
        }
        tradeDialogBody.removeAll();

        JLabel title = new JLabel("Trading with " + t.partnerName);
        title.setForeground(new Color(150, 220, 255));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        tradeDialogBody.add(title, BorderLayout.NORTH);

        // The two offers, side by side, stats visible on every row
        JPanel offers = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        offers.setOpaque(false);
        offers.add(tradeOfferColumn("Your offer (" + t.myOffer.size() + "/" + OsrsGoPlugin.TRADE_OFFER_CAP
            + ")  - click to remove", t.myOffer, true));
        offers.add(tradeOfferColumn(t.partnerName + "'s offer (" + t.theirOffer.size() + ")",
            t.theirOffer, false));

        // My collection below: click a mon to add it to the offer
        PlayerProfile profile = plugin.getProfile();
        JPanel grid = new JPanel(new java.awt.GridLayout(0, 3, 4, 4));
        grid.setOpaque(false);
        for (OwnedMon mon : profile.mons)
        {
            grid.add(tradeMonCell(mon, t.myOffer.contains(mon)));
        }
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(grid,
            javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
            "Your collection - click to offer",
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
            null, Color.LIGHT_GRAY));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        offers.setPreferredSize(new Dimension(10, 190));
        center.add(offers, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        tradeDialogBody.add(center, BorderLayout.CENTER);

        // Status + the two big buttons
        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setOpaque(false);
        String status;
        if (!t.partnerOpened)
        {
            status = "Waiting for " + t.partnerName + " to open the trade...";
        }
        else if (t.theirAccepted)
        {
            status = t.partnerName + " has ACCEPTED. " + (t.myAccepted ? "Completing..." : "Accept to complete.");
        }
        else if (t.myAccepted)
        {
            status = "You accepted. Waiting for " + t.partnerName + "...";
        }
        else
        {
            status = "Arrange your offers, then accept.";
        }
        JLabel statusLabel = new JLabel(status);
        statusLabel.setForeground(t.theirAccepted ? new Color(120, 220, 120) : Color.LIGHT_GRAY);
        south.add(statusLabel, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> plugin.cancelTrade());
        JButton accept = new JButton(t.myAccepted ? "Accepted" : "Accept trade");
        accept.setEnabled(!t.myAccepted && t.partnerOpened);
        accept.setToolTipText("Both sides must accept. Changing any offer resets acceptance.");
        accept.addActionListener(e -> plugin.tradeAccept());
        btns.add(cancel);
        btns.add(accept);
        south.add(btns, BorderLayout.EAST);
        tradeDialogBody.add(south, BorderLayout.SOUTH);

        tradeDialogBody.revalidate();
        tradeDialogBody.repaint();
    }

    private javax.swing.JComponent tradeOfferColumn(String heading, List<OwnedMon> mons, boolean mine)
    {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        for (OwnedMon mon : mons)
        {
            column.add(tradeOfferRow(mon, mine));
            column.add(Box.createVerticalStrut(3));
        }
        if (mons.isEmpty())
        {
            JLabel empty = new JLabel("(nothing yet)");
            empty.setForeground(new Color(120, 120, 130));
            column.add(empty);
        }
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(column,
            javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(mine ? new Color(90, 200, 90) : new Color(150, 150, 220)),
            heading, javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
            null, Color.LIGHT_GRAY));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JPanel tradeOfferRow(OwnedMon mon, boolean mine)
    {
        Species species = mon.species();
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel((mon.shiny ? "SHINY " : "") + mon.displayName() + "  lvl " + mon.level
            + "  (" + species.getType().getDisplay() + ")");
        name.setIcon(iconFor(species));
        name.setIconTextGap(5);
        name.setForeground(mon.shiny ? SHINY_GOLD : species.getRarity().getColor());
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(name);
        JLabel stats = new JLabel("HP " + mon.maxHp() + "  Atk " + mon.atk() + "  Def " + mon.def()
            + "  Spd " + mon.spd() + "  IV " + ivTotal(mon) + "/60");
        stats.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        stats.setFont(stats.getFont().deriveFont(11f));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(stats);
        if (mine)
        {
            row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            row.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    plugin.tradeToggleOffer(mon);
                }
            });
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return row;
    }

    private javax.swing.JComponent tradeMonCell(OwnedMon mon, boolean offered)
    {
        Species species = mon.species();
        JButton cell = new JButton("<html><center>" + (mon.shiny ? "SHINY " : "") + mon.displayName()
            + "<br>lvl " + mon.level + " - IV " + ivTotal(mon) + "/60</center></html>");
        cell.setIcon(iconFor(species));
        cell.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cell.setForeground(mon.shiny ? SHINY_GOLD : species.getRarity().getColor());
        cell.setToolTipText("HP " + mon.maxHp() + "  Atk " + mon.atk() + "  Def " + mon.def()
            + "  Spd " + mon.spd() + (offered ? "  -  click to withdraw" : "  -  click to offer"));
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(offered ? SHINY_GOLD : ColorScheme.DARKER_GRAY_COLOR, 2),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        cell.addActionListener(e ->
        {
            String err = plugin.tradeToggleOffer(mon);
            if (err != null)
            {
                showToast(err);
            }
        });
        return cell;
    }

    private JPanel monStatus(BattleMon mon, String owner, boolean mine)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name = new JLabel("<html><body style='width:170px'>" + owner + ": " + mon.name
            + "  lvl " + mon.level + "  (" + mon.type.getDisplay() + ")</body></html>");
        name.setIcon(iconFor(com.osrsgo.data.SpeciesData.byId(mon.speciesId)));
        name.setIconTextGap(6);
        name.setForeground(mine ? new Color(120, 220, 120) : new Color(240, 140, 100));
        JProgressBar hp = new JProgressBar(0, mon.maxHp);
        hp.setValue(Math.max(0, mon.hp));
        hp.setStringPainted(true);
        hp.setString(mon.hp + "/" + mon.maxHp + " HP");
        double frac = mon.maxHp > 0 ? (double) mon.hp / mon.maxHp : 0;
        hp.setForeground(frac > 0.5 ? new Color(80, 200, 80) : (frac > 0.2 ? new Color(230, 180, 40) : new Color(220, 60, 60)));
        hp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        panel.add(name);
        panel.add(hp);
        return panel;
    }

    // ------------------------------------------------------------------ Stats

    private final java.util.Set<String> collapsedStats = new java.util.HashSet<>(java.util.Arrays.asList(
        "Trainer", "Medals", "Catching", "Gyms", "Breeding", "More"));

    private boolean statOpen(String title)
    {
        return !collapsedStats.contains(title);
    }

    private void addStatHeader(String title)
    {
        boolean open = statOpen(title);
        JButton header = new JButton((open ? "[-] " : "[+] ") + title.toUpperCase());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setForeground(new Color(140, 200, 255));
        header.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        header.setBorderPainted(false);
        header.setContentAreaFilled(false);
        header.setMargin(new java.awt.Insets(8, 0, 2, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.addActionListener(e ->
        {
            if (!collapsedStats.remove(title))
            {
                collapsedStats.add(title);
            }
            lastFingerprint = "";
            refresh();
        });
        statsContent.add(header);
    }

    private void rebuildStats(PlayerProfile profile)
    {
        statsContent.removeAll();
        PlayerProfile.Stats s = profile.stats;

        addStatHeader("Trainer");
        if (statOpen("Trainer"))
        {
            statsContent.add(statLine2("Level", profile.trainerLevel() + "  (" + profile.trainerXp + " total xp)"));
            statsContent.add(statLine2("Badges", profile.badges.size() + "/" + com.osrsgo.data.GymData.all().size()));
            statsContent.add(statLine2("GielDex", profile.caughtSpecies.size() + " caught, "
                + profile.seenSpecies.size() + " seen"));
            statsContent.add(subHeader("Perks: every level"));
            statsContent.add(statLine2("Ball pouch +10/lvl", profile.balls + " / " + plugin.maxBalls()));
            statsContent.add(statLine2("Berry pouch +2/lvl", profile.berries + " / " + plugin.maxBerries()));
            statsContent.add(statLine2("Catch bonus +0.5%/lvl",
                "+" + Math.round(plugin.trainerCatchBonus() * 100) + "%"));
            statsContent.add(statLine2("Ball finds -2 tiles/lvl", "1 per " + plugin.tilesPerBall() + " tiles"));
            statsContent.add(statLine2("XP balls -25 xp/lvl", "1 per " + plugin.xpPerBall() + " xp"));
            statsContent.add(statLine2("Mon battle XP +1%/lvl",
                "x" + String.format("%.2f", plugin.monXpMultiplier())));
            statsContent.add(statLine2("Raid throws +1/20 lvls",
                String.valueOf(Math.min(7, 2 + profile.trainerLevel() / 20))));
            statsContent.add(subHeader("Perk milestones"));
            int lvl = profile.trainerLevel();
            statsContent.add(perkLine(lvl, 5, "4th egg slot"));
            statsContent.add(perkLine(lvl, 8, "Banks auto-heal your mons"));
            statsContent.add(perkLine(lvl, 10, "4th team member"));
            statsContent.add(perkLine(lvl, 15, "5th egg slot"));
            statsContent.add(perkLine(lvl, 20, "Berry finds every 125 tiles"));
            statsContent.add(perkLine(lvl, 25, "Gym tribute x2, kill rewards +1 ball"));
            statsContent.add(perkLine(lvl, 30, "5th team member"));
            statsContent.add(perkLine(lvl, 35, "Berry throws +20%"));
            statsContent.add(perkLine(lvl, 40, "Catch range 6 tiles"));
            statsContent.add(perkLine(lvl, 45, "Flawless eggs 5%"));
            statsContent.add(perkLine(lvl, 50, "6th egg slot, egg shinies 1/24, kill rewards +2 balls"));
            statsContent.add(perkLine(lvl, 65, "Gym tribute x3, catch range 7"));
            statsContent.add(perkLine(lvl, 70, "Remote breeding: the den comes to you"));
            statsContent.add(perkLine(lvl, 75, "Berry finds every 100 tiles, kill rewards +3 balls"));
            statsContent.add(perkLine(lvl, 80, "Master exchange: 20 Ultras"));
            statsContent.add(perkLine(lvl, 85, "Egg shinies 1/16"));
            statsContent.add(perkLine(lvl, 90, "7th egg slot, flawless eggs 8%, catch range 8"));
            statsContent.add(perkLine(lvl, 100, "CHAMPION: catch cap +25%, kill rewards +4 balls"));
        }

        addStatHeader("Medals");
        if (statOpen("Medals"))
        {
            addMedalCards(profile);
        }

        addStatHeader("Catching");
        if (statOpen("Catching"))
        {
            statsContent.add(statLine2("Tiles walked", String.format("%,d", profile.tilesWalked)));
            statsContent.add(statLine2("Balls found", String.format("%,d", s.ballsFound)));
            statsContent.add(statLine2("Balls thrown", String.format("%,d", s.ballsThrown)));
            String rate = s.ballsThrown > 0 ? Math.round(100.0 * s.catches / s.ballsThrown) + "%" : "-";
            statsContent.add(statLine2("Catches", String.format("%,d", s.catches) + "  (" + rate + ")"));
            statsContent.add(statLine2("Shinies caught", String.valueOf(s.shinyCatches)));
            statsContent.add(statLine2("Broke free / fled", s.breakFrees + " / " + s.flees));
            statsContent.add(statLine2("Rare Candies", profile.rareCandies + " held / " + s.candiesFound + " found"));
        }

        addStatHeader("Gyms");
        if (statOpen("Gyms"))
        {
            statsContent.add(statLine2("Holding right now", String.valueOf(plugin.gymsHeldNow())));
            statsContent.add(statLine2("Captured lifetime", String.valueOf(s.gymsCaptured)));
            statsContent.add(statLine2("Gym battles", s.gymWins + " W / " + s.gymLosses + " L"));
            statsContent.add(statLine2("PvP battles", s.pvpWins + " W / " + s.pvpLosses + " L"));
        }

        addStatHeader("Breeding");
        if (statOpen("Breeding"))
        {
            statsContent.add(statLine2("Eggs bred", String.valueOf(s.breeds)));
            statsContent.add(statLine2("Eggs hatched", String.valueOf(s.eggsHatched)));
        }

        addStatHeader("More");
        if (statOpen("More"))
        {
            statsContent.add(statLine2("Evolutions", String.valueOf(s.evolutions)));
            statsContent.add(statLine2("Berries found / used", s.berriesFound + " / " + s.berriesUsed));
            statsContent.add(statLine2("Trades completed", String.valueOf(s.trades)));
            statsContent.add(statLine2("Raid bosses beaten", String.valueOf(s.raidWins)));
            statsContent.add(statLine2("Research completed", String.valueOf(s.researchCompleted)));
            if (profile.faction != null)
            {
                statsContent.add(statLine2("Sworn to", profile.faction.charAt(0)
                    + profile.faction.substring(1).toLowerCase()));
            }
        }

        JButton cardBtn = new JButton("Export trainer card");
        cardBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardBtn.setToolTipText("Saves a shareable PNG and copies it to your clipboard");
        cardBtn.addActionListener(e -> exportTrainerCard());
        statsContent.add(Box.createVerticalStrut(6));
        statsContent.add(cardBtn);
    }

    private void addMedalCards(PlayerProfile profile)
    {
        for (com.osrsgo.data.MedalData.Medal medal : com.osrsgo.data.MedalData.ALL)
        {
            int tier = medal.tierOf(profile);
            long value = medal.valueOf(profile);
            Color tierColor;
            switch (tier)
            {
                case 1: tierColor = new Color(205, 127, 50); break;
                case 2: tierColor = new Color(192, 192, 192); break;
                case 3: tierColor = SHINY_GOLD; break;
                default: tierColor = new Color(110, 110, 110); break;
            }
            JPanel medalCard = stackRow();
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            JLabel name = new JLabel(medal.name);
            name.setForeground(Color.WHITE);
            name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
            JLabel tierLabel = new JLabel(tier > 0 ? com.osrsgo.data.MedalData.tierName(tier) : "Locked");
            tierLabel.setForeground(tierColor);
            tierLabel.setFont(tierLabel.getFont().deriveFont(Font.BOLD, 13f));
            top.add(name, BorderLayout.WEST);
            top.add(tierLabel, BorderLayout.EAST);
            medalCard.add(top);
            JLabel desc = new JLabel(medal.desc);
            desc.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            desc.setFont(desc.getFont().deriveFont(11f));
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            medalCard.add(desc);
            if (tier < 3)
            {
                long goal = medal.nextGoal(profile);
                JProgressBar bar = new JProgressBar(0, (int) Math.min(Integer.MAX_VALUE, goal));
                bar.setValue((int) Math.min(value, goal));
                bar.setStringPainted(true);
                bar.setString(value + " / " + goal + " for "
                    + com.osrsgo.data.MedalData.tierName(tier + 1));
                bar.setFont(bar.getFont().deriveFont(11f));
                bar.setForeground(tierColor);
                bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
                bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                medalCard.add(bar);
            }
            statsContent.add(medalCard);
            statsContent.add(Box.createVerticalStrut(3));
        }
    }

    private JPanel perkLine(int trainerLevel, int unlockLevel, String desc)
    {
        boolean unlocked = trainerLevel >= unlockLevel;
        Color color = unlocked ? new Color(120, 200, 120) : new Color(130, 130, 130);
        JPanel line = new JPanel(new BorderLayout(4, 0));
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        JLabel key = new JLabel("<html><body style='width:140px'>" + (unlocked ? "[x] " : "[ ] ") + desc + "</body></html>");
        key.setForeground(color);
        key.setFont(key.getFont().deriveFont(12f));
        JLabel val = new JLabel("lvl " + unlockLevel);
        val.setForeground(color);
        val.setFont(val.getFont().deriveFont(Font.BOLD, 12f));
        val.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        line.add(key, BorderLayout.CENTER);
        line.add(val, BorderLayout.EAST);
        return line;
    }

    /** Blanket hover text: any label or button without a tooltip gets its own text. */
    private void applyTooltips(java.awt.Container root)
    {
        for (java.awt.Component c : root.getComponents())
        {
            if (c instanceof JLabel)
            {
                JLabel label = (JLabel) c;
                if (label.getToolTipText() == null && label.getText() != null && !label.getText().trim().isEmpty())
                {
                    label.setToolTipText(label.getText());
                }
            }
            else if (c instanceof javax.swing.AbstractButton)
            {
                javax.swing.AbstractButton button = (javax.swing.AbstractButton) c;
                if (button.getToolTipText() == null && button.getText() != null && !button.getText().isEmpty())
                {
                    button.setToolTipText(button.getText());
                }
            }
            if (c instanceof java.awt.Container)
            {
                applyTooltips((java.awt.Container) c);
            }
        }
    }

    private void exportTrainerCard()
    {
        try
        {
            java.awt.image.BufferedImage card = TrainerCard.render(
                plugin.getProfile(), plugin.getMyRsn(), plugin.gymsHeldNow());
            java.io.File dir = com.osrsgo.storage.PluginFiles.subDir("cards");
            java.io.File out = new java.io.File(dir,
                "trainer-card-" + System.currentTimeMillis() + ".png");
            javax.imageio.ImageIO.write(card, "png", out);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.Transferable()
                {
                    @Override
                    public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors()
                    {
                        return new java.awt.datatransfer.DataFlavor[]{
                            java.awt.datatransfer.DataFlavor.imageFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor)
                    {
                        return java.awt.datatransfer.DataFlavor.imageFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(java.awt.datatransfer.DataFlavor flavor)
                    {
                        return card;
                    }
                }, null);
            showToast("Card copied to clipboard + saved!");
        }
        catch (Exception e)
        {
            showToast("Card export failed: " + e.getMessage());
        }
    }

    private JLabel subHeader(String title)
    {
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 1, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel statLine2(String label, String value)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        JLabel key = new JLabel(label);
        key.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        key.setFont(key.getFont().deriveFont(13f));
        JLabel val = new JLabel(value);
        val.setForeground(Color.WHITE);
        val.setFont(val.getFont().deriveFont(Font.BOLD, 13f));
        row.add(key, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------ shared bits

    /** Wiki portrait when cached, drawn medallion until then. */
    private javax.swing.ImageIcon iconFor(Species species)
    {
        java.awt.image.BufferedImage img = plugin.getNpcImages().cachedOrRequest(species);
        return new javax.swing.ImageIcon(img != null ? img : Icons.speciesIcon(species));
    }

    private JPanel rowPanel()
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** Vertical row card: labels stacked, then a right-aligned button strip. */
    private JPanel stackRow()
    {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JPanel buttonStrip(JButton... buttons)
    {
        // Rows of four: a FlowLayout wrap inside BoxLayout reports one-line
        // height and clips the overflow (this ate the team down-arrow)
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel row = null;
        for (int i = 0; i < buttons.length; i++)
        {
            if (i % 4 == 0)
            {
                row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
                row.setOpaque(false);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                stack.add(row);
            }
            row.add(buttons[i]);
        }
        return stack;
    }

    private JLabel hint(String text)
    {
        // The fixed body width makes the html label wrap instead of stretching
        // the row past the panel edge
        JLabel label = new JLabel("<html><body style='width:170px'>" + text + "</body></html>");
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(label.getFont().deriveFont(11f));
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
