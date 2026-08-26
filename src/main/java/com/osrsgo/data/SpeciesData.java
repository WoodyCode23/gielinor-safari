package com.osrsgo.data;

import com.osrsgo.model.MonType;
import com.osrsgo.model.Rarity;
import com.osrsgo.model.Species;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The full mon catalog. Base stats are on a 1-100 scale, loosely tracking each
 * NPC's real OSRS combat level. Ids are stable; never reuse one.
 */
public final class SpeciesData
{
    private static final Map<Integer, Species> BY_ID = new LinkedHashMap<>();

    static
    {
        // Commons: barnyard and starter-area critters
        add(1, "Chicken", MonType.RANGED, Rarity.COMMON, 20, 20, 15, 40);
        add(2, "Cow", MonType.MELEE, Rarity.COMMON, 35, 22, 25, 15);
        add(3, "Goblin", MonType.MELEE, Rarity.COMMON, 28, 26, 20, 30);
        add(4, "Giant Rat", MonType.MELEE, Rarity.COMMON, 25, 24, 18, 38);
        add(5, "Imp", MonType.MAGIC, Rarity.COMMON, 22, 28, 15, 48);
        add(6, "Giant Spider", MonType.MELEE, Rarity.COMMON, 26, 27, 20, 42);
        add(7, "Scorpion", MonType.MELEE, Rarity.COMMON, 30, 30, 26, 32);
        add(8, "Wolf", MonType.MELEE, Rarity.COMMON, 32, 32, 22, 45);
        add(9, "Grizzly Bear", MonType.MELEE, Rarity.COMMON, 40, 34, 28, 22);
        add(10, "Skeleton", MonType.MELEE, Rarity.COMMON, 32, 30, 28, 28);
        add(11, "Zombie", MonType.MELEE, Rarity.COMMON, 36, 28, 30, 15);
        add(12, "Ghost", MonType.MAGIC, Rarity.COMMON, 30, 32, 24, 34);
        add(13, "Rock Crab", MonType.MELEE, Rarity.COMMON, 45, 24, 45, 8);
        add(14, "Seagull", MonType.RANGED, Rarity.COMMON, 18, 18, 12, 50);
        add(15, "Barbarian", MonType.MELEE, Rarity.COMMON, 34, 33, 26, 26);
        add(16, "Dwarf", MonType.MELEE, Rarity.COMMON, 33, 31, 30, 20);

        // Uncommons: guards, giants and low slayer
        add(20, "Wizard", MonType.MAGIC, Rarity.UNCOMMON, 35, 42, 25, 40);
        add(21, "Dark Wizard", MonType.MAGIC, Rarity.UNCOMMON, 36, 45, 26, 42);
        add(22, "Highwayman", MonType.MELEE, Rarity.UNCOMMON, 38, 40, 32, 44);
        add(23, "Guard", MonType.MELEE, Rarity.UNCOMMON, 42, 40, 38, 30);
        add(24, "Hobgoblin", MonType.MELEE, Rarity.UNCOMMON, 44, 44, 34, 36);
        add(25, "Hill Giant", MonType.MELEE, Rarity.UNCOMMON, 55, 48, 36, 15);
        add(26, "Moss Giant", MonType.MELEE, Rarity.UNCOMMON, 58, 50, 40, 15);
        add(27, "Ice Giant", MonType.MELEE, Rarity.UNCOMMON, 60, 52, 44, 15);
        add(28, "Cave Crawler", MonType.MELEE, Rarity.UNCOMMON, 40, 42, 40, 25);
        add(29, "Banshee", MonType.MAGIC, Rarity.UNCOMMON, 38, 48, 28, 46);
        add(30, "Cockatrice", MonType.MAGIC, Rarity.UNCOMMON, 40, 46, 32, 40);
        add(31, "Ogre", MonType.MELEE, Rarity.UNCOMMON, 62, 50, 40, 12);
        add(32, "Chaos Druid", MonType.MAGIC, Rarity.UNCOMMON, 40, 47, 30, 38);
        add(33, "Unicorn", MonType.MELEE, Rarity.UNCOMMON, 45, 38, 34, 55);
        add(34, "King Scorpion", MonType.MELEE, Rarity.UNCOMMON, 50, 48, 42, 30);

        // Rares: knights, dragons and mid slayer
        add(40, "Lesser Demon", MonType.MELEE, Rarity.RARE, 60, 58, 48, 35);
        add(41, "Greater Demon", MonType.MELEE, Rarity.RARE, 66, 62, 52, 35);
        add(42, "Black Knight", MonType.MELEE, Rarity.RARE, 58, 60, 55, 40);
        add(43, "White Knight", MonType.MELEE, Rarity.RARE, 58, 58, 58, 40);
        add(44, "Green Dragon", MonType.MAGIC, Rarity.RARE, 68, 62, 50, 30);
        add(45, "Blue Dragon", MonType.MAGIC, Rarity.RARE, 72, 65, 54, 30);
        add(46, "Red Dragon", MonType.MAGIC, Rarity.RARE, 76, 68, 58, 30);
        add(47, "Bloodveld", MonType.MELEE, Rarity.RARE, 64, 60, 44, 38);
        add(48, "Aberrant Spectre", MonType.MAGIC, Rarity.RARE, 60, 64, 42, 42);
        add(49, "Dust Devil", MonType.MAGIC, Rarity.RARE, 58, 66, 40, 55);
        add(50, "Kurask", MonType.MELEE, Rarity.RARE, 70, 60, 60, 20);
        add(51, "Gargoyle", MonType.MELEE, Rarity.RARE, 68, 62, 64, 25);
        add(52, "Basilisk", MonType.MELEE, Rarity.RARE, 62, 58, 52, 35);
        add(53, "Ankou", MonType.MELEE, Rarity.RARE, 60, 62, 50, 40);
        add(54, "Cyclops", MonType.MELEE, Rarity.RARE, 66, 60, 48, 25);
        add(55, "Waterfiend", MonType.MAGIC, Rarity.RARE, 62, 64, 50, 48);
        add(56, "Fire Giant", MonType.MELEE, Rarity.RARE, 70, 64, 50, 20);
        add(57, "Jelly", MonType.MELEE, Rarity.RARE, 64, 56, 58, 22);

        // Epics: high slayer and dangerous beasts
        add(60, "Black Dragon", MonType.MAGIC, Rarity.EPIC, 80, 75, 65, 32);
        add(61, "Abyssal Demon", MonType.MELEE, Rarity.EPIC, 74, 82, 60, 70);
        add(62, "Nechryael", MonType.MELEE, Rarity.EPIC, 76, 78, 62, 45);
        add(63, "Dark Beast", MonType.RANGED, Rarity.EPIC, 78, 76, 64, 40);
        add(64, "Smoke Devil", MonType.MAGIC, Rarity.EPIC, 72, 80, 55, 60);
        add(65, "Cave Kraken", MonType.MAGIC, Rarity.EPIC, 82, 78, 60, 20);
        add(66, "Demonic Gorilla", MonType.MELEE, Rarity.EPIC, 84, 82, 70, 50);
        add(67, "Skeletal Wyvern", MonType.RANGED, Rarity.EPIC, 82, 78, 68, 35);
        add(68, "Hellhound", MonType.MELEE, Rarity.EPIC, 70, 76, 56, 65);
        add(69, "Lizardman Shaman", MonType.RANGED, Rarity.EPIC, 80, 80, 62, 45);
        add(70, "Brutal Black Dragon", MonType.MAGIC, Rarity.EPIC, 88, 84, 72, 30);

        // Legendaries: bosses
        add(80, "King Black Dragon", MonType.MAGIC, Rarity.LEGENDARY, 92, 88, 78, 40);
        add(81, "Kalphite Queen", MonType.RANGED, Rarity.LEGENDARY, 94, 90, 85, 45);
        add(82, "Zulrah", MonType.MAGIC, Rarity.LEGENDARY, 88, 95, 70, 75);
        add(83, "Vorkath", MonType.MAGIC, Rarity.LEGENDARY, 96, 94, 82, 40);
        add(84, "TzTok-Jad", MonType.RANGED, Rarity.LEGENDARY, 95, 98, 80, 45);
        add(85, "Corporeal Beast", MonType.MAGIC, Rarity.LEGENDARY, 100, 90, 92, 20);
        add(86, "General Graardor", MonType.MELEE, Rarity.LEGENDARY, 96, 96, 84, 35);
        add(87, "K'ril Tsutsaroth", MonType.MELEE, Rarity.LEGENDARY, 92, 95, 78, 50);
        add(88, "Kree'arra", MonType.RANGED, Rarity.LEGENDARY, 90, 92, 76, 70);
        add(89, "Commander Zilyana", MonType.MAGIC, Rarity.LEGENDARY, 88, 94, 74, 85);
        add(90, "Dagannoth Rex", MonType.MELEE, Rarity.LEGENDARY, 90, 88, 88, 30);
        add(91, "Dagannoth Prime", MonType.MAGIC, Rarity.LEGENDARY, 88, 92, 74, 45);
        add(92, "Dagannoth Supreme", MonType.RANGED, Rarity.LEGENDARY, 88, 90, 76, 60);
        add(93, "Cerberus", MonType.MELEE, Rarity.LEGENDARY, 94, 94, 80, 55);
        add(94, "Alchemical Hydra", MonType.MAGIC, Rarity.LEGENDARY, 96, 96, 82, 45);
        add(95, "The Nightmare", MonType.MAGIC, Rarity.LEGENDARY, 98, 92, 86, 30);
        add(96, "Chaos Elemental", MonType.MAGIC, Rarity.LEGENDARY, 86, 90, 72, 65);

        // Expansion batch (2026-08-08): 54 more species
        add(100, "Duck", MonType.RANGED, Rarity.COMMON, 18, 16, 12, 45);
        add(101, "Bat", MonType.RANGED, Rarity.COMMON, 20, 22, 14, 55);
        add(102, "Giant Frog", MonType.MELEE, Rarity.COMMON, 34, 26, 24, 30);
        add(103, "Monkey", MonType.MELEE, Rarity.COMMON, 22, 20, 16, 52);
        add(104, "Snake", MonType.MELEE, Rarity.COMMON, 24, 26, 16, 40);
        add(105, "Penguin", MonType.MELEE, Rarity.COMMON, 22, 18, 20, 35);
        add(106, "Ram", MonType.MELEE, Rarity.COMMON, 28, 24, 22, 25);
        add(107, "Rooster", MonType.RANGED, Rarity.COMMON, 18, 20, 12, 48);
        add(110, "Ghoul", MonType.MELEE, Rarity.UNCOMMON, 44, 44, 32, 34);
        add(111, "Pirate", MonType.MELEE, Rarity.UNCOMMON, 40, 42, 30, 38);
        add(112, "Mugger", MonType.MELEE, Rarity.UNCOMMON, 36, 40, 26, 45);
        add(113, "Ice Warrior", MonType.MELEE, Rarity.UNCOMMON, 46, 44, 40, 26);
        add(114, "Icefiend", MonType.MAGIC, Rarity.UNCOMMON, 36, 44, 26, 44);
        add(115, "Rockslug", MonType.MELEE, Rarity.UNCOMMON, 42, 36, 44, 12);
        add(116, "Crawling Hand", MonType.MELEE, Rarity.UNCOMMON, 34, 38, 26, 42);
        add(117, "Cave Bug", MonType.MELEE, Rarity.UNCOMMON, 38, 34, 30, 30);
        add(120, "Mountain Troll", MonType.MELEE, Rarity.RARE, 68, 62, 52, 22);
        add(121, "Dagannoth", MonType.MELEE, Rarity.RARE, 62, 60, 50, 40);
        add(122, "Turoth", MonType.MELEE, Rarity.RARE, 64, 62, 50, 36);
        add(123, "Pyrefiend", MonType.MAGIC, Rarity.RARE, 56, 64, 42, 48);
        add(128, "Infernal Mage", MonType.MAGIC, Rarity.RARE, 58, 66, 40, 44);
        add(129, "Loar Shade", MonType.MAGIC, Rarity.RARE, 56, 60, 44, 40);
        add(130, "Feral Vampyre", MonType.MELEE, Rarity.RARE, 62, 64, 48, 50);
        add(131, "Werewolf", MonType.MELEE, Rarity.RARE, 66, 68, 46, 58);
        add(132, "Bronze Dragon", MonType.MAGIC, Rarity.RARE, 72, 66, 62, 25);
        add(133, "Iron Dragon", MonType.MAGIC, Rarity.RARE, 76, 70, 68, 25);
        add(134, "Steel Dragon", MonType.MAGIC, Rarity.RARE, 80, 74, 72, 25);
        add(140, "Wyrm", MonType.MAGIC, Rarity.EPIC, 74, 78, 58, 55);
        add(141, "Drake", MonType.MAGIC, Rarity.EPIC, 80, 82, 64, 45);
        add(142, "Mithril Dragon", MonType.MAGIC, Rarity.EPIC, 86, 82, 78, 28);
        add(143, "Adamant Dragon", MonType.MAGIC, Rarity.EPIC, 90, 86, 82, 28);
        add(144, "Rune Dragon", MonType.MAGIC, Rarity.EPIC, 92, 88, 86, 28);
        add(145, "Basilisk Knight", MonType.MELEE, Rarity.EPIC, 84, 84, 74, 42);
        add(146, "Hydra", MonType.MAGIC, Rarity.EPIC, 82, 84, 66, 40);
        add(150, "Giant Mole", MonType.MELEE, Rarity.LEGENDARY, 92, 84, 82, 50);
        add(151, "Callisto", MonType.MELEE, Rarity.LEGENDARY, 98, 96, 84, 45);
        add(152, "Venenatis", MonType.MAGIC, Rarity.LEGENDARY, 94, 94, 80, 55);
        add(153, "Vet'ion", MonType.MELEE, Rarity.LEGENDARY, 96, 95, 86, 40);
        add(154, "Scorpia", MonType.MELEE, Rarity.LEGENDARY, 90, 92, 78, 55);
        add(155, "Sarachnis", MonType.MELEE, Rarity.LEGENDARY, 90, 90, 76, 60);
        add(156, "Skotizo", MonType.MAGIC, Rarity.LEGENDARY, 96, 94, 84, 35);
        add(157, "Zalcano", MonType.MAGIC, Rarity.LEGENDARY, 94, 90, 88, 25);
        add(158, "Crystalline Hunllef", MonType.RANGED, Rarity.LEGENDARY, 94, 98, 82, 65);
        add(159, "Nex", MonType.MAGIC, Rarity.LEGENDARY, 100, 99, 90, 80);
        add(160, "Phantom Muspah", MonType.RANGED, Rarity.LEGENDARY, 94, 95, 80, 60);
        add(161, "Duke Sucellus", MonType.MELEE, Rarity.LEGENDARY, 98, 96, 90, 25);
        add(162, "Vardorvis", MonType.MELEE, Rarity.LEGENDARY, 95, 99, 82, 70);
        add(163, "The Leviathan", MonType.RANGED, Rarity.LEGENDARY, 98, 94, 88, 45);
        add(164, "The Whisperer", MonType.MAGIC, Rarity.LEGENDARY, 96, 97, 84, 55);
        add(165, "Dusk", MonType.MELEE, Rarity.LEGENDARY, 96, 94, 88, 45);
        add(166, "Abyssal Sire", MonType.MELEE, Rarity.LEGENDARY, 97, 93, 85, 35);
        add(167, "Kraken", MonType.MAGIC, Rarity.LEGENDARY, 95, 96, 78, 20);
        add(168, "Thermonuclear Smoke Devil", MonType.MAGIC, Rarity.LEGENDARY, 92, 95, 76, 60);

        // Expansion batch 3 (2026-08-09): 45 more species
        add(170, "Man", MonType.MELEE, Rarity.COMMON, 30, 25, 20, 30);
        add(171, "Woman", MonType.MELEE, Rarity.COMMON, 30, 25, 20, 32);
        add(172, "Rabbit", MonType.MELEE, Rarity.COMMON, 16, 14, 10, 58);
        add(173, "Frog", MonType.MELEE, Rarity.COMMON, 22, 18, 16, 36);
        add(174, "Gnome", MonType.RANGED, Rarity.COMMON, 24, 24, 18, 44);
        add(175, "Sheep", MonType.MELEE, Rarity.COMMON, 20, 12, 18, 20);
        add(176, "Jackal", MonType.MELEE, Rarity.COMMON, 24, 26, 16, 46);
        add(177, "Vulture", MonType.RANGED, Rarity.COMMON, 22, 24, 14, 50);
        add(178, "Tortoise", MonType.MELEE, Rarity.COMMON, 48, 20, 50, 6);
        add(179, "Chompy Bird", MonType.RANGED, Rarity.COMMON, 20, 22, 12, 46);
        add(180, "Terrorbird", MonType.MELEE, Rarity.UNCOMMON, 40, 38, 30, 48);
        add(181, "Bandit", MonType.MELEE, Rarity.UNCOMMON, 40, 42, 30, 40);
        add(182, "Monk of Zamorak", MonType.MAGIC, Rarity.UNCOMMON, 38, 45, 26, 36);
        add(183, "Earth Warrior", MonType.MELEE, Rarity.UNCOMMON, 46, 44, 38, 28);
        add(184, "Cave Slime", MonType.MELEE, Rarity.UNCOMMON, 38, 34, 36, 18);
        add(185, "Big Wolf", MonType.MELEE, Rarity.UNCOMMON, 44, 42, 30, 48);
        add(186, "Jogre", MonType.MELEE, Rarity.UNCOMMON, 56, 46, 38, 20);
        add(187, "Otherworldly Being", MonType.MAGIC, Rarity.UNCOMMON, 42, 46, 30, 38);
        add(188, "Sulphur Lizard", MonType.MELEE, Rarity.UNCOMMON, 40, 40, 34, 34);
        add(190, "Mummy", MonType.MELEE, Rarity.RARE, 62, 60, 48, 30);
        add(191, "Cave Horror", MonType.MELEE, Rarity.RARE, 60, 62, 46, 38);
        add(192, "Suqah", MonType.MAGIC, Rarity.RARE, 66, 62, 50, 32);
        add(193, "Aviansie", MonType.RANGED, Rarity.RARE, 62, 64, 46, 60);
        add(194, "Spiritual Mage", MonType.MAGIC, Rarity.RARE, 58, 68, 42, 46);
        add(195, "Elf Warrior", MonType.MELEE, Rarity.RARE, 62, 62, 52, 48);
        add(196, "Crazy Archaeologist", MonType.RANGED, Rarity.RARE, 64, 66, 48, 42);
        add(197, "Deviant Spectre", MonType.MAGIC, Rarity.RARE, 62, 66, 44, 40);
        add(198, "Chaos Fanatic", MonType.MAGIC, Rarity.RARE, 60, 68, 42, 44);
        add(200, "Black Demon", MonType.MELEE, Rarity.EPIC, 80, 78, 62, 45);
        add(201, "Ancient Wyvern", MonType.MAGIC, Rarity.EPIC, 84, 80, 70, 32);
        add(202, "Long-tailed Wyvern", MonType.RANGED, Rarity.EPIC, 78, 76, 62, 40);
        add(203, "Gorak", MonType.MELEE, Rarity.EPIC, 76, 80, 60, 50);
        add(204, "Warped Terrorbird", MonType.MELEE, Rarity.EPIC, 74, 76, 58, 62);
        add(205, "Deranged Archaeologist", MonType.RANGED, Rarity.EPIC, 78, 80, 60, 44);
        add(206, "Dawn", MonType.RANGED, Rarity.EPIC, 82, 80, 68, 48);
        add(210, "Obor", MonType.MELEE, Rarity.LEGENDARY, 92, 90, 78, 30);
        add(211, "Bryophyta", MonType.MAGIC, Rarity.LEGENDARY, 92, 88, 82, 25);
        add(212, "Hespori", MonType.MAGIC, Rarity.LEGENDARY, 90, 92, 76, 35);
        add(213, "Tempoross", MonType.MAGIC, Rarity.LEGENDARY, 96, 88, 84, 40);
        add(214, "Scurrius", MonType.MELEE, Rarity.LEGENDARY, 88, 86, 74, 65);
        add(215, "Araxxor", MonType.MELEE, Rarity.LEGENDARY, 96, 96, 84, 50);
        add(216, "Amoxliatl", MonType.MAGIC, Rarity.LEGENDARY, 92, 90, 80, 45);
        add(217, "The Hueycoatl", MonType.MAGIC, Rarity.LEGENDARY, 98, 92, 86, 35);
        add(218, "Zebak", MonType.RANGED, Rarity.LEGENDARY, 96, 94, 82, 45);
        add(219, "Dharok the Wretched", MonType.MELEE, Rarity.LEGENDARY, 94, 99, 80, 30);

        // Batch 4 (2026-08-10): first field-journal harvest, ids live-verified
        add(300, "Hero", MonType.MELEE, Rarity.RARE, 42, 39, 31, 59);
        add(301, "Mounted terrorbird gnome", MonType.MELEE, Rarity.UNCOMMON, 39, 36, 28, 39);
        add(302, "Knight of Ardougne", MonType.MELEE, Rarity.UNCOMMON, 38, 35, 28, 36);
        add(303, "Pit Scorpion", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 53);
        add(304, "Giant bat", MonType.RANGED, Rarity.UNCOMMON, 33, 30, 24, 52);
        add(305, "Warrior", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 49);
        add(306, "Khazard Guard", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 48);
        add(307, "Khazard trooper", MonType.MELEE, Rarity.COMMON, 31, 28, 22, 44);
        add(308, "Black bear", MonType.MELEE, Rarity.COMMON, 31, 28, 22, 44);
        add(309, "Thief", MonType.MELEE, Rarity.COMMON, 30, 27, 21, 41);
        add(310, "Gnome troop", MonType.MELEE, Rarity.COMMON, 23, 20, 16, 28);
        add(311, "Monk", MonType.MELEE, Rarity.COMMON, 23, 20, 16, 28);
        add(312, "Cow calf", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(313, "Rat", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);
        add(314, "Spider", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);

        // Batch 5 (2026-08-10): Morytania field-journal harvest
        add(315, "Maggot King", MonType.MELEE, Rarity.LEGENDARY, 97, 95, 82, 40);
        add(316, "Evil Chicken", MonType.MAGIC, Rarity.EPIC, 72, 78, 55, 60);
        add(317, "Vyrewatch Sentinel", MonType.MELEE, Rarity.EPIC, 78, 76, 62, 50);
        add(318, "Dire bat", MonType.RANGED, Rarity.EPIC, 72, 70, 55, 65);
        add(319, "Ancient feral vyre", MonType.MELEE, Rarity.EPIC, 74, 72, 58, 55);
        add(320, "Sanguidae", MonType.MAGIC, Rarity.EPIC, 70, 74, 52, 45);
        add(321, "Ur-maggot larvae", MonType.MELEE, Rarity.RARE, 58, 56, 46, 40);

        // Batch 6 (2026-08-10): full boss-roster sweep vs the wiki Boss page.
        // Names must match in-game NPC names exactly for boss-kill throws.
        add(322, "Ahrim the Blighted", MonType.MAGIC, Rarity.EPIC, 82, 86, 66, 35);
        add(323, "Karil the Tainted", MonType.RANGED, Rarity.EPIC, 80, 85, 62, 55);
        add(324, "Guthan the Infested", MonType.MELEE, Rarity.EPIC, 86, 82, 70, 35);
        add(325, "Torag the Corrupted", MonType.MELEE, Rarity.EPIC, 84, 80, 74, 30);
        add(326, "Verac the Defiled", MonType.MELEE, Rarity.EPIC, 83, 84, 68, 35);
        add(327, "Artio", MonType.MELEE, Rarity.EPIC, 84, 82, 70, 45);
        add(328, "Calvar'ion", MonType.MELEE, Rarity.EPIC, 82, 81, 72, 40);
        add(329, "Spindel", MonType.MAGIC, Rarity.EPIC, 80, 80, 66, 55);
        add(330, "Revenant maledictus", MonType.MAGIC, Rarity.LEGENDARY, 90, 88, 74, 60);
        add(331, "Blood Moon", MonType.MELEE, Rarity.EPIC, 84, 86, 68, 45);
        add(332, "Blue Moon", MonType.MAGIC, Rarity.EPIC, 84, 84, 70, 45);
        add(333, "Eclipse Moon", MonType.RANGED, Rarity.EPIC, 84, 85, 66, 50);
        add(334, "Phosani's Nightmare", MonType.MAGIC, Rarity.LEGENDARY, 99, 96, 88, 35);
        add(335, "Yama", MonType.MAGIC, Rarity.LEGENDARY, 98, 97, 88, 50);
        add(336, "Doom of Mokhaiotl", MonType.MAGIC, Rarity.LEGENDARY, 97, 96, 86, 45);
        add(337, "Branda the Fire Queen", MonType.MAGIC, Rarity.EPIC, 86, 84, 72, 40);
        add(338, "Eldric the Ice King", MonType.MELEE, Rarity.EPIC, 88, 82, 76, 35);
        add(339, "Brutus", MonType.MELEE, Rarity.EPIC, 68, 66, 58, 40);
        add(340, "Demonic Brutus", MonType.MELEE, Rarity.LEGENDARY, 99, 97, 90, 35);
        add(341, "Mad Angel", MonType.MAGIC, Rarity.LEGENDARY, 95, 94, 82, 55);
        add(342, "Shellbane gryphon", MonType.RANGED, Rarity.LEGENDARY, 92, 93, 78, 60);
        add(343, "Gemstone Crab", MonType.MELEE, Rarity.EPIC, 90, 55, 88, 15);
        add(344, "Great Olm", MonType.MAGIC, Rarity.LEGENDARY, 100, 96, 92, 25);
        add(345, "The Maiden of Sugadinti", MonType.MAGIC, Rarity.LEGENDARY, 96, 90, 84, 30);
        add(346, "Pestilent Bloat", MonType.MELEE, Rarity.LEGENDARY, 97, 92, 88, 20);
        add(347, "Nylocas Vasilias", MonType.MELEE, Rarity.LEGENDARY, 92, 94, 78, 60);
        add(348, "Sotetseg", MonType.MAGIC, Rarity.LEGENDARY, 95, 93, 84, 45);
        add(349, "Xarpus", MonType.RANGED, Rarity.LEGENDARY, 94, 90, 82, 35);
        add(350, "Verzik Vitur", MonType.MAGIC, Rarity.LEGENDARY, 100, 98, 90, 45);
        add(351, "Ba-Ba", MonType.MELEE, Rarity.LEGENDARY, 95, 93, 84, 35);
        add(352, "Kephri", MonType.RANGED, Rarity.LEGENDARY, 93, 91, 86, 45);
        add(353, "Akkha", MonType.MAGIC, Rarity.LEGENDARY, 94, 95, 82, 60);
        add(354, "Tumeken's Warden", MonType.MAGIC, Rarity.LEGENDARY, 99, 97, 92, 30);
        add(355, "Elidinis' Warden", MonType.MAGIC, Rarity.LEGENDARY, 98, 95, 92, 30);
        add(356, "TzKal-Zuk", MonType.MELEE, Rarity.LEGENDARY, 100, 99, 94, 20);
        add(357, "Sol Heredit", MonType.MELEE, Rarity.LEGENDARY, 99, 98, 90, 50);
        add(358, "Corrupted Hunllef", MonType.RANGED, Rarity.LEGENDARY, 95, 99, 84, 65);

        // Batch 7 (2026-08-11): field-journal harvest (mountains, deserts, seas)
        add(359, "Venator", MonType.MELEE, Rarity.LEGENDARY, 90, 88, 76, 50);
        add(360, "Ice wolf", MonType.MELEE, Rarity.EPIC, 72, 70, 58, 55);
        add(361, "Ice troll", MonType.MELEE, Rarity.EPIC, 74, 70, 62, 40);
        add(362, "Impaler deer", MonType.MELEE, Rarity.EPIC, 72, 68, 56, 50);
        add(363, "Thrower troll", MonType.RANGED, Rarity.RARE, 56, 54, 44, 50);
        add(364, "Paladin", MonType.MELEE, Rarity.RARE, 55, 52, 46, 45);
        add(365, "Tortured soul", MonType.MAGIC, Rarity.RARE, 50, 54, 38, 48);
        add(366, "Zombie pirate", MonType.MELEE, Rarity.RARE, 52, 48, 40, 38);
        add(367, "Sorebones", MonType.MAGIC, Rarity.RARE, 48, 52, 38, 44);
        add(368, "Leech", MonType.MELEE, Rarity.RARE, 46, 50, 34, 42);
        add(369, "Billy Goat", MonType.MELEE, Rarity.UNCOMMON, 40, 36, 30, 55);
        add(370, "Outlaw", MonType.MELEE, Rarity.UNCOMMON, 38, 36, 28, 50);
        add(371, "Jail guard", MonType.MELEE, Rarity.UNCOMMON, 38, 34, 32, 42);
        add(372, "White wolf", MonType.MELEE, Rarity.UNCOMMON, 36, 34, 26, 52);
        add(373, "Goat", MonType.MELEE, Rarity.UNCOMMON, 34, 30, 26, 46);
        add(374, "Tarik", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 44);
        add(375, "Radat", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 44);
        add(376, "Poltenip", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 44);
        add(377, "Market Guard", MonType.MELEE, Rarity.UNCOMMON, 34, 31, 26, 40);
        add(378, "Unicorn Foal", MonType.MELEE, Rarity.COMMON, 30, 26, 22, 48);
        add(379, "Buffalo", MonType.MELEE, Rarity.COMMON, 32, 26, 24, 28);
        add(380, "Al Kharid warrior", MonType.MELEE, Rarity.COMMON, 28, 26, 20, 36);
        add(381, "Gardener", MonType.MELEE, Rarity.COMMON, 24, 21, 16, 30);
        add(382, "Skraeling", MonType.MELEE, Rarity.COMMON, 22, 20, 15, 28);
        add(383, "Undead cow", MonType.MELEE, Rarity.COMMON, 26, 20, 20, 14);
        add(384, "Duckling", MonType.RANGED, Rarity.COMMON, 16, 14, 10, 44);
        add(385, "Undead chicken", MonType.RANGED, Rarity.COMMON, 18, 18, 12, 34);

        // Batch 8 (2026-08-11): field-journal harvest (coasts and wizard country)
        add(386, "Albatross", MonType.RANGED, Rarity.EPIC, 70, 68, 54, 58);
        add(387, "Saradomin wizard", MonType.MAGIC, Rarity.EPIC, 68, 72, 52, 50);
        add(388, "Bull shark", MonType.MELEE, Rarity.RARE, 52, 50, 40, 48);
        add(389, "Guard dog", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 40);
        add(390, "Air wizard", MonType.MAGIC, Rarity.COMMON, 28, 30, 18, 42);
        add(391, "Water wizard", MonType.MAGIC, Rarity.COMMON, 28, 30, 18, 40);
        add(392, "Earth wizard", MonType.MAGIC, Rarity.COMMON, 29, 29, 20, 36);
        add(393, "Fire wizard", MonType.MAGIC, Rarity.COMMON, 28, 31, 18, 38);
        add(394, "Farmer", MonType.MELEE, Rarity.COMMON, 25, 22, 17, 32);
        add(395, "Zombie rat", MonType.MELEE, Rarity.COMMON, 23, 20, 16, 28);

        // Batch 9 (2026-08-13): field-journal harvest (wilderness, Tirannwn,
        // Kourend, seas). Types corrected from the export's MELEE guesses.
        add(396, "Runite Golem", MonType.MELEE, Rarity.EPIC, 78, 74, 72, 30);
        add(397, "Double agent", MonType.MELEE, Rarity.EPIC, 70, 72, 56, 48);
        add(398, "Brassican Mage", MonType.MAGIC, Rarity.EPIC, 68, 76, 54, 50);
        add(399, "Rogue", MonType.MELEE, Rarity.EPIC, 66, 70, 52, 62);
        add(400, "Vyrewatch", MonType.MAGIC, Rarity.EPIC, 70, 72, 58, 56);
        add(401, "Bandosian guard", MonType.MELEE, Rarity.EPIC, 72, 70, 60, 40);
        add(402, "Mutated Bloodveld", MonType.MAGIC, Rarity.EPIC, 74, 72, 56, 42);
        add(403, "Ancient Wizard", MonType.MAGIC, Rarity.EPIC, 64, 74, 50, 46);
        add(404, "Ancient Fungi", MonType.MAGIC, Rarity.EPIC, 66, 68, 54, 34);
        add(405, "Iorwerth Warrior", MonType.MELEE, Rarity.EPIC, 68, 68, 56, 44);
        add(406, "Scarab Swarm", MonType.MELEE, Rarity.EPIC, 58, 62, 44, 66);
        add(407, "Armadylean guard", MonType.RANGED, Rarity.EPIC, 66, 68, 54, 52);
        add(408, "Pygmy kraken", MonType.MAGIC, Rarity.EPIC, 62, 66, 50, 48);
        add(409, "Elf Archer", MonType.RANGED, Rarity.EPIC, 60, 68, 48, 58);
        add(410, "Iorwerth Archer", MonType.RANGED, Rarity.EPIC, 60, 68, 48, 58);
        add(411, "Sergeant", MonType.MELEE, Rarity.RARE, 56, 54, 46, 40);
        add(412, "Lizardman brute", MonType.MELEE, Rarity.RARE, 56, 54, 44, 34);
        add(413, "Jungle horror", MonType.MELEE, Rarity.RARE, 54, 52, 42, 32);
        add(414, "Butterfly ray", MonType.MAGIC, Rarity.RARE, 46, 48, 38, 60);
        add(415, "Zamorak wizard", MonType.MAGIC, Rarity.RARE, 44, 56, 34, 50);
        add(416, "Lizardman", MonType.MELEE, Rarity.RARE, 48, 46, 38, 40);
        add(417, "Soldier", MonType.MELEE, Rarity.RARE, 48, 46, 40, 38);
        add(418, "Pyrelord", MonType.MAGIC, Rarity.RARE, 46, 52, 36, 44);
        add(419, "Battle mage", MonType.MAGIC, Rarity.RARE, 44, 54, 34, 46);
        add(420, "Albino bat", MonType.MELEE, Rarity.RARE, 38, 40, 30, 58);
        add(421, "Animated spade", MonType.MELEE, Rarity.RARE, 42, 44, 40, 30);
        add(422, "Possessed pickaxe", MonType.MELEE, Rarity.RARE, 42, 44, 40, 30);
        add(423, "Chaos dwarf", MonType.MELEE, Rarity.UNCOMMON, 40, 38, 32, 34);
        add(424, "Magic axe", MonType.MAGIC, Rarity.UNCOMMON, 36, 40, 28, 36);
        add(425, "Colonel Radick", MonType.MELEE, Rarity.UNCOMMON, 38, 36, 32, 30);
        add(426, "Druid", MonType.MAGIC, Rarity.UNCOMMON, 34, 40, 26, 40);
        add(427, "Watchman", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 28, 36);
        add(428, "Camp dweller", MonType.MELEE, Rarity.UNCOMMON, 34, 31, 26, 38);
        add(429, "Mudskipper", MonType.MELEE, Rarity.UNCOMMON, 32, 30, 26, 44);
        add(430, "Gunthor the brave", MonType.MELEE, Rarity.UNCOMMON, 36, 34, 28, 32);
        add(431, "Tower guard", MonType.RANGED, Rarity.UNCOMMON, 33, 34, 26, 40);
        add(432, "Lynx", MonType.MELEE, Rarity.UNCOMMON, 32, 32, 22, 52);
        add(433, "Crab", MonType.MELEE, Rarity.UNCOMMON, 34, 24, 40, 12);
        add(434, "Gnome guard", MonType.MELEE, Rarity.UNCOMMON, 30, 28, 24, 46);
        add(435, "Lynx Tamer", MonType.MELEE, Rarity.UNCOMMON, 31, 28, 24, 44);
        add(436, "Hoop Snake", MonType.MELEE, Rarity.COMMON, 28, 28, 20, 48);
        add(437, "Fox", MonType.MELEE, Rarity.COMMON, 28, 28, 20, 50);
        add(438, "Bear Cub", MonType.MELEE, Rarity.COMMON, 30, 26, 22, 34);
        add(439, "Bark Blamish Snail", MonType.MELEE, Rarity.COMMON, 30, 22, 32, 8);
        add(440, "Giant mosquito", MonType.MELEE, Rarity.COMMON, 22, 26, 16, 54);
        add(441, "Bird", MonType.RANGED, Rarity.COMMON, 18, 18, 12, 48);
        add(442, "Rusty", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(443, "Gnome woman", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 30);
        add(444, "Chinchompa", MonType.MELEE, Rarity.COMMON, 20, 22, 14, 52);
        add(445, "Great Olm (Left claw)", MonType.MELEE, Rarity.LEGENDARY, 100, 97, 77, 40);
        add(446, "Great Olm (Right claw)", MonType.MELEE, Rarity.LEGENDARY, 88, 85, 68, 49);
        add(447, "Nylocas Athanatos", MonType.MELEE, Rarity.LEGENDARY, 74, 71, 56, 25);
        add(448, "Marble gargoyle", MonType.MELEE, Rarity.LEGENDARY, 74, 71, 56, 59);
        add(449, "King kurask", MonType.MELEE, Rarity.LEGENDARY, 69, 66, 52, 40);
        add(450, "Fumus", MonType.MAGIC, Rarity.LEGENDARY, 68, 65, 52, 30);
        add(451, "Cruor", MonType.MAGIC, Rarity.LEGENDARY, 68, 65, 52, 30);
        add(452, "Umbra", MonType.MAGIC, Rarity.LEGENDARY, 68, 65, 52, 30);
        add(453, "Glacies", MonType.MAGIC, Rarity.LEGENDARY, 68, 65, 52, 30);
        add(454, "Nuclear smoke devil", MonType.MELEE, Rarity.LEGENDARY, 68, 65, 52, 25);
        add(455, "Nylocas Ischyros", MonType.MELEE, Rarity.LEGENDARY, 66, 63, 50, 40);
        add(456, "Nylocas Toxobolos", MonType.RANGED, Rarity.LEGENDARY, 66, 63, 50, 40);
        add(457, "Nylocas Hagios", MonType.MAGIC, Rarity.LEGENDARY, 66, 63, 50, 40);
        add(458, "Abhorrent spectre", MonType.MELEE, Rarity.LEGENDARY, 65, 62, 49, 33);
        add(459, "Lava dragon", MonType.MELEE, Rarity.LEGENDARY, 65, 62, 49, 32);
        add(460, "Magma strykewyrm", MonType.MELEE, Rarity.LEGENDARY, 65, 62, 49, 29);
        add(461, "Blood-starved venator", MonType.MELEE, Rarity.LEGENDARY, 65, 62, 49, 26);
        add(462, "Spiked Turoth", MonType.MELEE, Rarity.LEGENDARY, 64, 61, 48, 59);
        add(463, "Ancient Custodian", MonType.MELEE, Rarity.LEGENDARY, 64, 61, 48, 54);
        add(464, "TzHaar-Ket", MonType.MELEE, Rarity.LEGENDARY, 62, 59, 47, 36);
        add(465, "Greater Nechryael", MonType.MELEE, Rarity.LEGENDARY, 60, 57, 45, 50);
        add(466, "Keef", MonType.MELEE, Rarity.EPIC, 58, 55, 44, 28);
        add(467, "Mutated Terrorbird", MonType.MELEE, Rarity.EPIC, 58, 55, 44, 28);
        add(468, "Blood Reaver", MonType.MAGIC, Rarity.EPIC, 57, 54, 43, 59);
        add(469, "Judge of Yama", MonType.MELEE, Rarity.EPIC, 56, 53, 42, 53);
        add(470, "Monkey Guard", MonType.MELEE, Rarity.EPIC, 56, 53, 42, 52);
        add(471, "Malevolent Mage", MonType.MAGIC, Rarity.EPIC, 56, 53, 42, 47);
        add(472, "Spiritual ranger", MonType.RANGED, Rarity.EPIC, 55, 52, 41, 43);
        add(473, "Spiritual warrior", MonType.MELEE, Rarity.EPIC, 55, 52, 41, 43);
        add(474, "Padulah", MonType.MELEE, Rarity.EPIC, 54, 51, 40, 34);
        add(475, "Araxyte", MonType.MELEE, Rarity.EPIC, 54, 51, 40, 31);
        add(476, "Elder custodian stalker", MonType.MELEE, Rarity.EPIC, 53, 50, 40, 27);
        add(477, "TzHaar-Xil", MonType.RANGED, Rarity.EPIC, 52, 49, 39, 53);
        add(478, "Void Flare", MonType.MELEE, Rarity.EPIC, 52, 49, 39, 50);
        add(479, "Warped Tortoise", MonType.MELEE, Rarity.EPIC, 51, 48, 38, 41);
        add(480, "Mature custodian stalker", MonType.MELEE, Rarity.EPIC, 50, 47, 37, 37);
        add(481, "Emissary Ascended", MonType.MAGIC, Rarity.EPIC, 50, 47, 37, 37);
        add(482, "Lava Strykewyrm", MonType.MELEE, Rarity.EPIC, 50, 47, 37, 36);
        add(483, "Honour guard", MonType.MELEE, Rarity.EPIC, 50, 47, 37, 35);
        add(484, "Nylocas Matomenos", MonType.MELEE, Rarity.EPIC, 50, 47, 37, 35);
        add(485, "Saradomin priest", MonType.MAGIC, Rarity.EPIC, 49, 46, 36, 33);
        add(486, "Chilled jelly", MonType.MELEE, Rarity.EPIC, 49, 46, 36, 32);
        add(487, "Narwhal", MonType.MELEE, Rarity.EPIC, 49, 46, 36, 31);
        add(488, "Ork", MonType.MELEE, Rarity.EPIC, 49, 46, 36, 27);
        add(489, "Frost Nagua", MonType.MELEE, Rarity.EPIC, 48, 45, 36, 59);
        add(490, "TzHaar-Mej", MonType.MAGIC, Rarity.EPIC, 48, 45, 36, 58);
        add(491, "Knight of Saradomin", MonType.MELEE, Rarity.EPIC, 48, 45, 36, 58);
        add(492, "Flaming pyrelord", MonType.MELEE, Rarity.EPIC, 47, 44, 35, 52);
        add(493, "Gryphon", MonType.MELEE, Rarity.EPIC, 47, 44, 35, 50);
        add(494, "Juvenile custodian stalker", MonType.MELEE, Rarity.EPIC, 46, 43, 34, 48);
        add(495, "Lieutenant", MonType.MELEE, Rarity.EPIC, 46, 43, 34, 48);
        add(496, "Stingray", MonType.MELEE, Rarity.EPIC, 46, 43, 34, 47);
        add(497, "Tormented Warrior", MonType.MELEE, Rarity.EPIC, 46, 43, 34, 45);
        add(498, "Dire Wolf", MonType.MELEE, Rarity.RARE, 46, 43, 34, 43);
        add(499, "Monkey Archer", MonType.RANGED, Rarity.RARE, 45, 42, 33, 41);
        add(500, "Head Guard", MonType.MELEE, Rarity.RARE, 45, 42, 33, 39);
        add(501, "City guard", MonType.MELEE, Rarity.RARE, 45, 42, 33, 38);
        add(502, "Skeleton Mage", MonType.MAGIC, Rarity.RARE, 45, 42, 33, 38);
        add(503, "Baby black dragon", MonType.MELEE, Rarity.RARE, 45, 42, 33, 38);
        add(504, "Enclave guard", MonType.MELEE, Rarity.RARE, 45, 42, 33, 38);
        add(505, "Ice troll male", MonType.MELEE, Rarity.RARE, 45, 42, 33, 37);
        add(506, "Ice troll female", MonType.MELEE, Rarity.RARE, 45, 42, 33, 37);
        add(507, "Ogre chieftain", MonType.MELEE, Rarity.RARE, 45, 42, 33, 36);
        add(508, "Giant skeleton", MonType.MELEE, Rarity.RARE, 44, 41, 32, 35);
        add(509, "Emissary Chosen", MonType.MAGIC, Rarity.RARE, 44, 41, 32, 31);
        add(510, "TzHaar-Hur", MonType.MELEE, Rarity.RARE, 43, 40, 32, 29);
        add(511, "Ice troll runt", MonType.MELEE, Rarity.RARE, 43, 40, 32, 29);
        add(512, "Undead one", MonType.MELEE, Rarity.RARE, 43, 40, 32, 28);
        add(513, "Animated Black Armour", MonType.MELEE, Rarity.RARE, 42, 39, 31, 59);
        add(514, "Chasm Crawler", MonType.MELEE, Rarity.RARE, 42, 39, 31, 58);
        add(515, "Jaguar", MonType.MELEE, Rarity.RARE, 42, 39, 31, 57);
        add(516, "Poison spider", MonType.MELEE, Rarity.RARE, 42, 39, 31, 54);
        add(517, "Jungle Wolf", MonType.MELEE, Rarity.RARE, 42, 39, 31, 54);
        add(518, "Bedabin Nomad Fighter", MonType.MELEE, Rarity.RARE, 40, 37, 29, 46);
        add(519, "Blood spawn", MonType.MELEE, Rarity.RARE, 40, 37, 29, 45);
        add(520, "Vampyre Juvinate", MonType.MELEE, Rarity.RARE, 40, 37, 29, 44);
        add(521, "Oomlie bird", MonType.MELEE, Rarity.UNCOMMON, 38, 35, 28, 36);
        add(522, "Vampyre Juvenile", MonType.MELEE, Rarity.UNCOMMON, 38, 35, 28, 35);
        add(523, "Jungle spider", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 34);
        add(524, "Zogre", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 34);
        add(525, "Ugthanki", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 32);
        add(526, "Archer", MonType.RANGED, Rarity.UNCOMMON, 37, 34, 27, 32);
        add(527, "Loar Shadow", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 30);
        add(528, "Spined larupia", MonType.MELEE, Rarity.UNCOMMON, 37, 34, 27, 30);
        add(529, "Afflicted", MonType.MELEE, Rarity.UNCOMMON, 36, 33, 26, 27);
        add(530, "Ice elemental", MonType.MAGIC, Rarity.UNCOMMON, 36, 33, 26, 27);
        add(531, "Renegade Knight", MonType.MELEE, Rarity.UNCOMMON, 36, 33, 26, 27);
        add(532, "Fire elemental", MonType.MAGIC, Rarity.UNCOMMON, 36, 33, 26, 27);
        add(533, "Black Heather", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 25, 59);
        add(534, "Donny the lad", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 25, 59);
        add(535, "Speedy Keith", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 25, 59);
        add(536, "Deadly red spider", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 25, 59);
        add(537, "Emissary Acolyte", MonType.MAGIC, Rarity.UNCOMMON, 35, 32, 25, 59);
        add(538, "Grizzly bear cub", MonType.MELEE, Rarity.UNCOMMON, 35, 32, 25, 58);
        add(539, "Jaguar cub", MonType.MELEE, Rarity.UNCOMMON, 34, 31, 24, 56);
        add(540, "Desert Wolf", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 52);
        add(541, "Ammonite Crab", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 50);
        add(542, "Black Guard", MonType.MELEE, Rarity.UNCOMMON, 33, 30, 24, 50);
        add(543, "H.A.M. Guard", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 47);
        add(544, "Yak", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 47);
        add(545, "Shantay Guard", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 47);
        add(546, "Guard Bandit", MonType.MELEE, Rarity.UNCOMMON, 32, 29, 23, 47);
        add(547, "Blood Blamish Snail", MonType.MELEE, Rarity.UNCOMMON, 31, 28, 22, 45);
        add(548, "Tern", MonType.MELEE, Rarity.UNCOMMON, 31, 28, 22, 45);
        add(549, "Bruise Blamish Snail", MonType.MELEE, Rarity.UNCOMMON, 31, 28, 22, 45);
        add(550, "Forester", MonType.MELEE, Rarity.COMMON, 29, 26, 20, 40);
        add(551, "Eadburg", MonType.MELEE, Rarity.COMMON, 24, 21, 16, 29);
        add(552, "Cuffs", MonType.MELEE, Rarity.COMMON, 23, 20, 16, 28);
        add(553, "Broddi", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(554, "Narf", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(555, "Bunny", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(556, "Thora", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(557, "Ragnvald", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(558, "Carnivorous chinchompa", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(559, "Rannveig", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(560, "Valgerd", MonType.MELEE, Rarity.COMMON, 22, 19, 15, 27);
        add(561, "Thorhild", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);
        add(562, "Ragnar", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);
        add(563, "Einar", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);
        add(564, "Alrik", MonType.MELEE, Rarity.COMMON, 21, 18, 14, 26);
    }

    private static void add(int id, String name, MonType type, Rarity rarity, int hp, int atk, int def, int spd)
    {
        BY_ID.put(id, new Species(id, name, type, rarity, hp, atk, def, spd));
    }

    public static boolean exists(int id)
    {
        return BY_ID.containsKey(id);
    }

    public static Species byId(int id)
    {
        Species s = BY_ID.get(id);
        // Fall back to Goblin so a stale profile entry can never NPE the UI
        return s != null ? s : BY_ID.get(3);
    }

    public static List<Species> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private SpeciesData()
    {
    }
}
