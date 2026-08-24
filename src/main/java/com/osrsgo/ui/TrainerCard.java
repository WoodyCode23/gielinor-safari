package com.osrsgo.ui;

import com.osrsgo.data.GymData;
import com.osrsgo.data.MedalData;
import com.osrsgo.data.SpeciesData;
import com.osrsgo.model.OwnedMon;
import com.osrsgo.storage.PlayerProfile;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Renders a shareable trainer-card PNG from the profile. */
public final class TrainerCard
{
    private static final int W = 460;
    private static final int H = 280;

    public static BufferedImage render(PlayerProfile profile, String rsn, int gymsHeldNow)
    {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(28, 28, 34), 0, H, new Color(16, 16, 20)));
        g.fillRect(0, 0, W, H);
        Color accent = factionColor(profile.faction);
        g.setColor(accent);
        g.fillRect(0, 0, W, 5);

        g.drawImage(Icons.drawOrb(34), 14, 16, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("Gielinor Safari Trainer Card", 58, 40);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(accent);
        g.drawString(rsn != null ? rsn : "Trainer", 16, 76);
        if (profile.faction != null)
        {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.drawString("Sworn to " + profile.faction.charAt(0)
                + profile.faction.substring(1).toLowerCase(), 16, 94);
        }

        int bronze = 0;
        int silver = 0;
        int gold = 0;
        for (MedalData.Medal medal : MedalData.ALL)
        {
            switch (medal.tierOf(profile))
            {
                case 1: bronze++; break;
                case 2: silver++; break;
                case 3: gold++; break;
                default: break;
            }
        }
        int shinies = 0;
        OwnedMon best = null;
        for (OwnedMon m : profile.mons)
        {
            if (m.shiny)
            {
                shinies++;
            }
            if (best == null || m.level > best.level)
            {
                best = m;
            }
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        int y = 122;
        y = line(g, "Trainer level", String.valueOf(profile.trainerLevel()), y);
        y = line(g, "GielDex", profile.caughtSpecies.size() + " / " + SpeciesData.all().size(), y);
        y = line(g, "Badges", profile.badges.size() + " / " + GymData.all().size(), y);
        y = line(g, "Gyms held", String.valueOf(gymsHeldNow), y);
        y = line(g, "Shinies", String.valueOf(shinies), y);
        y = line(g, "Tiles walked", String.format("%,d", profile.tilesWalked), y);
        line(g, "Medals", gold + " gold / " + silver + " silver / " + bronze + " bronze", y);

        if (best != null)
        {
            g.drawImage(Icons.speciesIcon(best.species()), 300, 118, 48, 48, null);
            g.setColor(best.shiny ? new Color(255, 215, 60) : Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("Partner", 300, 112);
            String bestName = (best.shiny ? "SHINY " : "") + best.displayName();
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.drawString(bestName.length() > 20 ? bestName.substring(0, 20) : bestName, 300, 184);
            g.drawString("lvl " + best.level, 300, 202);
        }

        g.setColor(new Color(120, 120, 130));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString("Gielinor Safari", 16, H - 12);
        // Every shared card doubles as an invitation
        String invite = "discord.gg/nphJz77pJk";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(140, 170, 220));
        g.drawString(invite, W - 16 - fm.stringWidth(invite), H - 12);
        g.dispose();
        return img;
    }

    private static int line(Graphics2D g, String key, String value, int y)
    {
        g.setColor(new Color(170, 170, 180));
        g.drawString(key, 16, y);
        g.setColor(Color.WHITE);
        g.drawString(value, 140, y);
        return y + 22;
    }

    private static Color factionColor(String faction)
    {
        if ("SARADOMIN".equals(faction))
        {
            return new Color(90, 160, 255);
        }
        if ("ZAMORAK".equals(faction))
        {
            return new Color(255, 90, 90);
        }
        if ("GUTHIX".equals(faction))
        {
            return new Color(90, 220, 90);
        }
        return new Color(220, 60, 60);
    }

    private TrainerCard()
    {
    }
}
