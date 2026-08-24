package com.osrsgo.ui;

import com.osrsgo.model.Species;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** All plugin imagery is drawn in code so the jar ships no resources. */
public final class Icons
{
    private static final Map<Integer, BufferedImage> SPECIES_CACHE = new HashMap<>();
    private static BufferedImage pokeball;
    private static BufferedImage gymMark;

    public static synchronized BufferedImage pokeball()
    {
        if (pokeball == null)
        {
            pokeball = drawOrb(24);
        }
        return pokeball;
    }

    public static BufferedImage drawOrb(int size)
    {
        return drawBall(size, new Color(220, 60, 60), Color.WHITE);
    }

    /** Tier colors: 0 Gielinor red, 1 Great blue, 2 Super silver, 3 Ultra gold/black, 4 Master purple. */
    public static BufferedImage tierBall(int tier, int size)
    {
        switch (tier)
        {
            case 1: return drawBall(size, new Color(60, 110, 230), Color.WHITE);
            case 2: return drawBall(size, new Color(190, 195, 205), Color.WHITE);
            case 3: return drawBall(size, new Color(240, 190, 40), new Color(45, 45, 50));
            case 4: return drawBall(size, new Color(150, 70, 210), Color.WHITE);
            default: return drawBall(size, new Color(220, 60, 60), Color.WHITE);
        }
    }

    /**
     * An enchanted rune orb: single-color glossy sphere with an etched rune
     * diamond. Deliberately nothing like a certain other franchise's
     * red-and-white capture device.
     */
    private static BufferedImage drawBall(int size, Color base, Color sigil)
    {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int m = Math.max(1, size / 12);
        int d = size - 2 * m;
        g.setPaint(new java.awt.GradientPaint(m, m, base.brighter(), m, m + d, base.darker()));
        g.fillOval(m, m, d, d);
        g.setColor(new Color(0, 0, 0, 170));
        g.setStroke(new BasicStroke(Math.max(1f, size / 24f)));
        g.drawOval(m, m, d, d);
        int r = Math.max(2, d / 4);
        int cx = size / 2;
        int cy = size / 2;
        g.setColor(sigil);
        g.setStroke(new BasicStroke(Math.max(1f, size / 16f)));
        g.drawPolygon(
            new int[]{cx, cx + r, cx, cx - r},
            new int[]{cy - r, cy, cy + r, cy},
            4);
        g.drawLine(cx, cy - r, cx, cy + r);
        g.setColor(new Color(255, 255, 255, 110));
        g.fillOval(m + d / 5, m + d / 6, d / 4, d / 5);
        g.dispose();
        return img;
    }

    public static synchronized BufferedImage gymMark()
    {
        if (gymMark != null)
        {
            return gymMark;
        }
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 200, 200));
        int[] xs = {8, 15, 8, 1};
        int[] ys = {1, 8, 15, 8};
        g.fillPolygon(xs, ys, 4);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 4);
        g.setColor(Color.WHITE);
        g.fillOval(6, 6, 4, 4);
        g.dispose();
        gymMark = img;
        return img;
    }

    /** 24px medallion: rarity-colored disc, type-colored ring, species initials. */
    public static synchronized BufferedImage speciesIcon(Species species)
    {
        BufferedImage cached = SPECIES_CACHE.get(species.getId());
        if (cached != null)
        {
            return cached;
        }
        int size = 24;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color rarity = species.getRarity().getColor();
        g.setColor(new Color(rarity.getRed() / 3, rarity.getGreen() / 3, rarity.getBlue() / 3));
        g.fillOval(1, 1, size - 2, size - 2);
        g.setColor(species.getType().getColor());
        g.setStroke(new BasicStroke(2f));
        g.drawOval(2, 2, size - 4, size - 4);
        g.setColor(rarity);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        String initials = initialsOf(species.getName());
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        SPECIES_CACHE.put(species.getId(), img);
        return img;
    }

    private static BufferedImage candy;

    /** The purple sweet: wrapped-candy silhouette, unmistakable at a glance. */
    public static synchronized BufferedImage candy(int size)
    {
        if (candy != null && candy.getWidth() == size)
        {
            return candy;
        }
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = size / 2;
        int cy = size / 2;
        int r = (int) (size * 0.28);
        int w = (int) (size * 0.22);
        // Wrapper twists
        g.setColor(new Color(150, 70, 210));
        g.fillPolygon(new int[]{cx - r, cx - r - w, cx - r - w}, new int[]{cy, cy - w, cy + w}, 3);
        g.fillPolygon(new int[]{cx + r, cx + r + w, cx + r + w}, new int[]{cy, cy - w, cy + w}, 3);
        // Body
        g.setPaint(new java.awt.GradientPaint(cx, cy - r, new Color(190, 110, 250),
            cx, cy + r, new Color(120, 40, 180)));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(0, 0, 0, 150));
        g.setStroke(new BasicStroke(Math.max(1f, size / 20f)));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        // Gloss
        g.setColor(new Color(255, 255, 255, 130));
        g.fillOval(cx - r / 2, cy - r + r / 4, r / 2, r / 3);
        g.dispose();
        candy = img;
        return img;
    }

    private static BufferedImage berry;

    public static synchronized BufferedImage berry(int size)
    {
        if (berry != null && berry.getWidth() == size)
        {
            return berry;
        }
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int d = (int) (size * 0.62);
        g.setColor(new Color(200, 40, 70));
        g.fillOval(1, size - d - 1, d, d);
        g.fillOval(size - d - 1, size - d - 1, d, d);
        g.setColor(new Color(90, 170, 60));
        g.fillOval(size / 2 - 2, 1, 5, 4);
        g.dispose();
        berry = img;
        return img;
    }

    private static String initialsOf(String name)
    {
        String[] parts = name.split("[\\s'-]+");
        if (parts.length >= 2 && !parts[1].isEmpty())
        {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private Icons()
    {
    }
}
