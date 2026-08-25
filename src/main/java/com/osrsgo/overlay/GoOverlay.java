package com.osrsgo.overlay;

import com.osrsgo.OsrsGoConfig;
import com.osrsgo.data.GymData;
import com.osrsgo.model.Rarity;
import com.osrsgo.spawn.SpawnManager;
import com.osrsgo.spawn.WildSpawn;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws wild spawns (tile poly + name) and gym marks (diamond + name) in the
 * scene, plus dots for both on the minimap.
 */
public class GoOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;
    private static final Color GYM_OPEN = new Color(0, 220, 220);
    private static final Color GYM_MINE = new Color(255, 200, 0);
    private static final Color GYM_ENEMY = new Color(255, 90, 90);

    private final Client client;
    private final OsrsGoConfig config;
    private final SpawnManager spawnManager;
    private final com.osrsgo.OsrsGoPlugin plugin;
    private final BattleScene battleScene;
    private final java.awt.image.BufferedImage ballImage = com.osrsgo.ui.Icons.drawOrb(28);
    private final java.awt.image.BufferedImage[] tierBalls = {
        com.osrsgo.ui.Icons.tierBall(0, 28),
        com.osrsgo.ui.Icons.tierBall(1, 28),
        com.osrsgo.ui.Icons.tierBall(2, 28),
        com.osrsgo.ui.Icons.tierBall(3, 28),
        com.osrsgo.ui.Icons.tierBall(4, 28)
    };

    @Inject
    public GoOverlay(Client client, OsrsGoConfig config, SpawnManager spawnManager,
        com.osrsgo.OsrsGoPlugin plugin, BattleScene battleScene)
    {
        this.client = client;
        this.config = config;
        this.spawnManager = spawnManager;
        this.plugin = plugin;
        this.battleScene = battleScene;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public java.awt.Dimension render(Graphics2D graphics)
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }
        // Template-space location so spawn/candy marks also work in instances
        WorldPoint player = com.osrsgo.spawn.Coords.playerLocation(client);
        if (player == null)
        {
            return null;
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        if (config.showSpawns())
        {
            for (WildSpawn spawn : spawnManager.nearby(player, MAX_DRAW_DISTANCE))
            {
                renderSpawn(graphics, spawn);
            }
            for (com.osrsgo.spawn.SpawnManager.Candy candy : spawnManager.nearbyCandies(player, MAX_DRAW_DISTANCE))
            {
                renderCandy(graphics, candy);
            }
        }
        if (config.showGyms())
        {
            for (GymData.Gym gym : GymData.all())
            {
                if (player.distanceTo(gym.location) <= MAX_DRAW_DISTANCE * 2)
                {
                    renderGym(graphics, gym);
                }
            }
        }
        renderCatchSequence(graphics);
        renderBattleFx(graphics);
        return null;
    }

    /** HP bars, hitsplats, and send-out flashes over the two battle models. */
    private void renderBattleFx(Graphics2D graphics)
    {
        if (!battleScene.isActive())
        {
            return;
        }
        int myHp = -1;
        int myMax = 1;
        int oppHp = -1;
        int oppMax = 1;
        com.osrsgo.battle.BattleSession session = plugin.getSession();
        if (session != null)
        {
            myHp = session.myMon().hp;
            myMax = session.myMon().maxHp;
            oppHp = session.oppMon().hp;
            oppMax = session.oppMon().maxHp;
        }
        else
        {
            return;
        }
        renderBattleSide(graphics, battleScene.getMineWp(), myHp, myMax,
            battleScene.getHitOnMine(), battleScene.getMineSwitchStart());
        renderBattleSide(graphics, battleScene.getTheirsWp(), oppHp, oppMax,
            battleScene.getHitOnTheirs(), battleScene.getTheirsSwitchStart());
    }

    private void renderBattleSide(Graphics2D graphics, net.runelite.api.coords.WorldPoint wp,
        int hp, int maxHp, int hit, long switchStart)
    {
        if (wp == null)
        {
            return;
        }
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,wp);
        if (lp == null)
        {
            return;
        }
        Point anchor = Perspective.getCanvasTextLocation(client, graphics, lp, "", 230);
        if (anchor == null)
        {
            return;
        }
        long now = System.currentTimeMillis();

        // Floating HP bar
        if (hp >= 0)
        {
            int barW = 34;
            int barH = 5;
            int x = anchor.getX() - barW / 2;
            int y = anchor.getY() - 8;
            graphics.setColor(new Color(120, 30, 30));
            graphics.fillRect(x, y, barW, barH);
            graphics.setColor(new Color(70, 200, 70));
            graphics.fillRect(x, y, (int) (barW * Math.max(0, Math.min(1.0, hp / (double) maxHp))), barH);
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(1));
            graphics.drawRect(x, y, barW, barH);
        }

        // Hitsplat riding the clash timing
        long clashElapsed = now - battleScene.getClashStart();
        if (hit >= 0 && clashElapsed > 150 && clashElapsed < 1300)
        {
            int cx = anchor.getX();
            int cy = anchor.getY() + 22;
            graphics.setColor(hit == 0 ? new Color(60, 90, 200) : new Color(180, 30, 30));
            graphics.fillOval(cx - 11, cy - 11, 22, 22);
            graphics.setColor(Color.BLACK);
            graphics.drawOval(cx - 11, cy - 11, 22, 22);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            String text = String.valueOf(hit);
            java.awt.FontMetrics fm = graphics.getFontMetrics();
            graphics.drawString(text, cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2 - 2);
        }

        // Send-out flash: the Gielinor Ball bursts open where the new mon appears
        long switchElapsed = now - switchStart;
        if (switchStart > 0 && switchElapsed < 700)
        {
            double t = switchElapsed / 700.0;
            int cx = anchor.getX();
            int cy = anchor.getY() + 20;
            graphics.drawImage(ballImage, cx - 14, cy - 14, null);
            int r = (int) (8 + t * 34);
            graphics.setColor(new Color(255, 255, 255, (int) (200 * (1 - t))));
            graphics.setStroke(new BasicStroke(3));
            graphics.drawOval(cx - r, cy - r, r * 2, r * 2);
            graphics.setColor(new Color(255, 220, 60, (int) (160 * (1 - t))));
            graphics.drawOval(cx - r / 2, cy - r / 2, r, r);
        }
    }

    /** The Gielinor Ball throw: arc to the tile, three shakes, then the verdict. */
    private void renderCatchSequence(Graphics2D graphics)
    {
        com.osrsgo.spawn.CatchSequence seq = plugin.getActiveCatch();
        if (seq == null)
        {
            return;
        }
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,seq.spawn.location);
        if (lp == null)
        {
            return;
        }
        Point target = Perspective.getCanvasTextLocation(client, graphics, lp, "", 0);
        if (target == null)
        {
            return;
        }
        long elapsed = seq.elapsed();
        java.awt.image.BufferedImage ball =
            tierBalls[Math.max(0, Math.min(seq.ballTier, tierBalls.length - 1))];
        int tx = target.getX();
        int ty = target.getY();

        if (elapsed < com.osrsgo.spawn.CatchSequence.THROW_MS)
        {
            double t = elapsed / (double) com.osrsgo.spawn.CatchSequence.THROW_MS;
            int sx = client.getCanvasWidth() / 2;
            int sy = client.getCanvasHeight() - 40;
            int x = (int) (sx + (tx - sx) * t);
            int y = (int) (sy + (ty - sy) * t - Math.sin(Math.PI * t) * 140);
            drawBall(graphics, ball, x, y, t * 720);
            return;
        }

        long shakeElapsed = elapsed - com.osrsgo.spawn.CatchSequence.THROW_MS;
        if (shakeElapsed < com.osrsgo.spawn.CatchSequence.SHAKE_MS)
        {
            double t = shakeElapsed / (double) com.osrsgo.spawn.CatchSequence.SHAKE_MS;
            double angle = Math.sin(t * Math.PI * 6) * 24;
            drawBall(graphics, ball, tx, ty, angle);
            return;
        }

        long resultElapsed = elapsed - com.osrsgo.spawn.CatchSequence.THROW_MS
            - com.osrsgo.spawn.CatchSequence.SHAKE_MS;
        double t = Math.min(1.0, resultElapsed / (double) com.osrsgo.spawn.CatchSequence.RESULT_MS);
        String verdict;
        Color verdictColor;
        if (seq.outcome == com.osrsgo.spawn.CatchSequence.Outcome.CAUGHT)
        {
            drawBall(graphics, ball, tx, ty, 0);
            // Expanding capture ring
            int r = (int) (10 + t * 30);
            graphics.setColor(new Color(255, 220, 60, (int) (200 * (1 - t))));
            graphics.setStroke(new BasicStroke(3));
            graphics.drawOval(tx - r, ty - r, r * 2, r * 2);
            verdict = "Gotcha!";
            verdictColor = new Color(120, 255, 120);
        }
        else
        {
            // Ball pops open: halves fly apart
            int off = (int) (t * 18);
            graphics.drawImage(ball.getSubimage(0, 0, 28, 14), tx - 14, ty - 14 - off, null);
            graphics.drawImage(ball.getSubimage(0, 14, 28, 14), tx - 14, ty + off, null);
            verdict = seq.outcome == com.osrsgo.spawn.CatchSequence.Outcome.FLED ? "It fled!" : "Broke free!";
            verdictColor = new Color(255, 120, 100);
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        graphics.setColor(Color.BLACK);
        java.awt.FontMetrics fm = graphics.getFontMetrics();
        int textX = tx - fm.stringWidth(verdict) / 2;
        int textY = ty - 34 - (int) (t * 10);
        graphics.drawString(verdict, textX + 1, textY + 1);
        graphics.setColor(verdictColor);
        graphics.drawString(verdict, textX, textY);
    }

    private void drawBall(Graphics2D graphics, java.awt.image.BufferedImage ball, int x, int y, double angleDeg)
    {
        java.awt.geom.AffineTransform old = graphics.getTransform();
        graphics.translate(x, y);
        graphics.rotate(Math.toRadians(angleDeg));
        graphics.drawImage(ball, -ball.getWidth() / 2, -ball.getHeight() / 2, null);
        graphics.setTransform(old);
    }

    private static final Color SHINY_GOLD = new Color(255, 215, 60);
    private static final Color CANDY_PURPLE = new Color(190, 110, 250);
    private final java.awt.image.BufferedImage candyImage = com.osrsgo.ui.Icons.candy(22);

    /** A rare candy on the ground: purple tile, floating sweet, sparkles. */
    private void renderCandy(Graphics2D graphics, com.osrsgo.spawn.SpawnManager.Candy candy)
    {
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client, candy.location);
        if (lp == null)
        {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            OverlayUtil.renderPolygon(graphics, poly, CANDY_PURPLE);
        }
        Point text = Perspective.getCanvasTextLocation(client, graphics, lp, "RARE CANDY", 40);
        if (text != null)
        {
            OverlayUtil.renderTextLocation(graphics, text, "RARE CANDY", CANDY_PURPLE);
            graphics.drawImage(candyImage, text.getX() - 34, text.getY() - 16, null);
        }
        renderShinySparkles(graphics, lp, candy.key.hashCode());
        renderMinimapDot(graphics, lp, CANDY_PURPLE);
    }

    private void renderSpawn(Graphics2D graphics, WildSpawn spawn)
    {
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,spawn.location);
        if (lp == null)
        {
            return;
        }
        Rarity rarity = spawn.species().getRarity();
        Color color = spawn.shiny ? SHINY_GOLD : rarity.getColor();

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            OverlayUtil.renderPolygon(graphics, poly, color);
        }
        String label = (spawn.shiny ? "SHINY " : "") + spawn.species().getName() + " (lvl " + spawn.level + ")";
        Point text = Perspective.getCanvasTextLocation(client, graphics, lp, label, 40);
        if (text != null)
        {
            OverlayUtil.renderTextLocation(graphics, text, label, color);
            if (spawn.shiny)
            {
                drawStar(graphics, text.getX() - 12, text.getY() - 5, 5);
            }
        }
        if (spawn.shiny)
        {
            renderShinySparkles(graphics, lp, spawn.key.hashCode());
        }
        renderMinimapDot(graphics, lp, color);
    }

    /**
     * Twinkling sparkles orbiting a shiny's model. Time-driven so they
     * animate every frame; each spawn gets its own phase from its key.
     */
    private void renderShinySparkles(Graphics2D graphics, LocalPoint lp, int phaseSeed)
    {
        Point anchor = Perspective.getCanvasTextLocation(client, graphics, lp, "", 90);
        if (anchor == null)
        {
            return;
        }
        double t = System.currentTimeMillis() / 1000.0 + (phaseSeed & 0xFF) / 37.0;
        for (int i = 0; i < 3; i++)
        {
            double angle = t * 1.6 + i * (Math.PI * 2 / 3);
            int radius = 16 + (int) (Math.sin(t * 2.3 + i) * 5);
            int sx = anchor.getX() + (int) (Math.cos(angle) * radius);
            int sy = anchor.getY() + (int) (Math.sin(angle) * radius * 0.55);
            // Twinkle: each sparkle fades in and out on its own beat
            double twinkle = (Math.sin(t * 3.1 + i * 2.1) + 1) / 2;
            if (twinkle < 0.15)
            {
                continue;
            }
            int size = 3 + (int) (twinkle * 3);
            java.awt.Composite old = graphics.getComposite();
            graphics.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, (float) Math.min(1.0, 0.35 + twinkle * 0.65)));
            drawStar(graphics, sx, sy, size);
            graphics.setComposite(old);
        }
    }

    /** Small four-point sparkle used to mark shiny spawns. */
    private void drawStar(Graphics2D graphics, int cx, int cy, int r)
    {
        Polygon star = new Polygon(
            new int[]{cx, cx + r / 3, cx + r, cx + r / 3, cx, cx - r / 3, cx - r, cx - r / 3},
            new int[]{cy - r, cy - r / 3, cy, cy + r / 3, cy + r, cy + r / 3, cy, cy - r / 3},
            8);
        graphics.setColor(SHINY_GOLD);
        graphics.fillPolygon(star);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(1));
        graphics.drawPolygon(star);
    }

    private void renderGym(Graphics2D graphics, GymData.Gym gym)
    {
        LocalPoint lp = com.osrsgo.spawn.Coords.toLocal(client,gym.location);
        if (lp == null)
        {
            return;
        }
        Color color;
        String suffix = "";
        if (!config.gymControl())
        {
            // Gym control is off: no holder state applies, so this is the
            // gym's pre-feature form, colored purely by whether its badge is
            // already earned (same fallback the panel uses in this state)
            color = plugin.hasBadge(gym.badge) ? GYM_MINE : GYM_OPEN;
        }
        else
        {
            switch (plugin.gymOwnership(gym.id))
            {
                case MINE:
                    color = GYM_MINE;
                    suffix = " (yours)";
                    break;
                case ENEMY:
                    color = GYM_ENEMY;
                    com.osrsgo.gym.GymHolder holder = plugin.getGymHolder(gym.id);
                    suffix = holder != null ? " (" + holder.holderRsn + ")" : "";
                    break;
                case UNCLAIMED:
                default:
                    color = GYM_OPEN;
                    break;
            }
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly != null)
        {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawPolygon(poly);
            // Diamond mark centered on the tile
            java.awt.Rectangle b = poly.getBounds();
            int cx = b.x + b.width / 2;
            int cy = b.y + b.height / 2;
            int r = Math.max(6, b.width / 4);
            Polygon diamond = new Polygon(
                new int[]{cx, cx + r, cx, cx - r},
                new int[]{cy - r, cy, cy + r, cy},
                4);
            graphics.drawPolygon(diamond);
        }
        String label = gym.name + suffix;
        Point text = Perspective.getCanvasTextLocation(client, graphics, lp, label, 60);
        if (text != null)
        {
            OverlayUtil.renderTextLocation(graphics, text, label, color);
        }
        renderMinimapDot(graphics, lp, color);
    }

    private void renderMinimapDot(Graphics2D graphics, LocalPoint lp, Color color)
    {
        Point mini = Perspective.localToMinimap(client, lp);
        if (mini == null)
        {
            return;
        }
        graphics.setColor(color);
        graphics.fill(new Ellipse2D.Float(mini.getX() - 2, mini.getY() - 2, 4, 4));
        graphics.setColor(Color.BLACK);
        graphics.draw(new Ellipse2D.Float(mini.getX() - 2, mini.getY() - 2, 4, 4));
    }
}
