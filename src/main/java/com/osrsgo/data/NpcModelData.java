package com.osrsgo.data;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.NpcID;

/**
 * Species -> real OSRS NPC id, used to render actual NPC models on spawn
 * tiles. Constants come from the legacy NpcID class so a wrong name fails at
 * compile time instead of showing the wrong monster.
 */
public final class NpcModelData
{
    private static final Map<Integer, Integer> NPC_IDS = new HashMap<>();

    static
    {
        map(1, NpcID.CHICKEN);
        map(2, NpcID.COW);
        map(3, NpcID.GOBLIN);
        map(4, NpcID.GIANT_RAT);
        map(5, NpcID.IMP);
        map(6, NpcID.GIANT_SPIDER);
        map(7, NpcID.SCORPION);
        map(8, NpcID.WOLF);
        map(9, NpcID.GRIZZLY_BEAR);
        map(10, NpcID.SKELETON);
        map(11, NpcID.ZOMBIE);
        map(12, NpcID.GHOST);
        map(13, NpcID.ROCK_CRAB);
        map(14, NpcID.SEAGULL);
        map(15, NpcID.BARBARIAN);
        map(16, NpcID.DWARF);
        map(20, NpcID.WIZARD);
        map(21, NpcID.DARK_WIZARD);
        map(22, NpcID.HIGHWAYMAN);
        map(23, NpcID.GUARD);
        map(24, NpcID.HOBGOBLIN);
        map(25, NpcID.HILL_GIANT);
        map(26, NpcID.MOSS_GIANT);
        map(27, NpcID.ICE_GIANT);
        map(28, NpcID.CAVE_CRAWLER);
        map(29, NpcID.BANSHEE);
        map(30, NpcID.COCKATRICE);
        map(31, NpcID.OGRE);
        map(32, NpcID.CHAOS_DRUID);
        map(33, NpcID.UNICORN);
        map(34, NpcID.KING_SCORPION);
        map(40, NpcID.LESSER_DEMON);
        map(41, NpcID.GREATER_DEMON);
        map(42, NpcID.BLACK_KNIGHT);
        map(43, NpcID.WHITE_KNIGHT);
        map(44, NpcID.GREEN_DRAGON);
        map(45, NpcID.BLUE_DRAGON);
        map(46, NpcID.RED_DRAGON);
        map(47, NpcID.BLOODVELD);
        map(48, NpcID.ABERRANT_SPECTRE);
        map(49, NpcID.DUST_DEVIL);
        map(50, NpcID.KURASK);
        map(51, NpcID.GARGOYLE);
        map(52, NpcID.BASILISK);
        map(53, NpcID.ANKOU);
        map(54, NpcID.CYCLOPS);
        map(55, NpcID.WATERFIEND);
        map(56, NpcID.FIRE_GIANT);
        map(57, NpcID.JELLY);
        map(60, NpcID.BLACK_DRAGON);
        map(61, NpcID.ABYSSAL_DEMON);
        map(62, NpcID.NECHRYAEL);
        map(63, NpcID.DARK_BEAST);
        map(64, NpcID.SMOKE_DEVIL);
        map(65, NpcID.CAVE_KRAKEN);
        map(66, NpcID.DEMONIC_GORILLA);
        map(67, NpcID.SKELETAL_WYVERN);
        map(68, NpcID.HELLHOUND);
        map(69, NpcID.LIZARDMAN_SHAMAN);
        map(70, NpcID.BRUTAL_BLACK_DRAGON);
        map(80, NpcID.KING_BLACK_DRAGON);
        map(81, NpcID.KALPHITE_QUEEN);
        map(82, NpcID.ZULRAH);
        map(83, NpcID.VORKATH);
        map(84, NpcID.TZTOKJAD);
        map(85, NpcID.CORPOREAL_BEAST);
        map(86, NpcID.GENERAL_GRAARDOR);
        map(87, NpcID.KRIL_TSUTSAROTH);
        map(88, NpcID.KREEARRA);
        map(89, NpcID.COMMANDER_ZILYANA);
        map(90, NpcID.DAGANNOTH_REX);
        map(91, NpcID.DAGANNOTH_PRIME);
        map(92, NpcID.DAGANNOTH_SUPREME);
        map(93, NpcID.CERBERUS);
        map(94, NpcID.ALCHEMICAL_HYDRA);
        map(95, NpcID.THE_NIGHTMARE);
        map(96, NpcID.CHAOS_ELEMENTAL);
        map(100, NpcID.DUCK);
        map(101, NpcID.BAT);
        map(102, NpcID.GIANT_FROG);
        map(103, NpcID.MONKEY);
        map(104, NpcID.SNAKE);
        map(105, NpcID.PENGUIN);
        map(106, NpcID.RAM);
        map(107, NpcID.ROOSTER);
        map(110, NpcID.GHOUL);
        map(111, NpcID.PIRATE);
        map(112, NpcID.MUGGER);
        map(113, NpcID.ICE_WARRIOR);
        map(114, NpcID.ICEFIEND);
        map(115, NpcID.ROCKSLUG);
        map(116, NpcID.CRAWLING_HAND);
        map(117, NpcID.CAVE_BUG);
        map(120, NpcID.MOUNTAIN_TROLL);
        map(121, NpcID.DAGANNOTH);
        map(122, NpcID.TUROTH);
        map(123, NpcID.PYREFIEND);
        map(128, NpcID.INFERNAL_MAGE);
        map(129, NpcID.LOAR_SHADE);
        map(130, NpcID.FERAL_VAMPYRE);
        map(131, NpcID.WEREWOLF);
        map(132, NpcID.BRONZE_DRAGON);
        map(133, NpcID.IRON_DRAGON);
        map(134, NpcID.STEEL_DRAGON);
        map(140, NpcID.WYRM);
        map(141, NpcID.DRAKE);
        map(142, NpcID.MITHRIL_DRAGON);
        map(143, NpcID.ADAMANT_DRAGON);
        map(144, NpcID.RUNE_DRAGON);
        map(145, NpcID.BASILISK_KNIGHT);
        map(146, NpcID.HYDRA);
        map(150, NpcID.GIANT_MOLE);
        map(151, NpcID.CALLISTO);
        map(152, NpcID.VENENATIS);
        map(153, NpcID.VETION);
        map(154, NpcID.SCORPIA);
        map(155, NpcID.SARACHNIS);
        map(156, NpcID.SKOTIZO);
        map(157, NpcID.ZALCANO);
        map(158, NpcID.CRYSTALLINE_HUNLLEF);
        map(159, NpcID.NEX);
        map(160, NpcID.PHANTOM_MUSPAH);
        map(161, NpcID.DUKE_SUCELLUS);
        map(162, NpcID.VARDORVIS);
        map(163, NpcID.THE_LEVIATHAN);
        map(164, NpcID.THE_WHISPERER);
        map(165, NpcID.DUSK);
        map(166, NpcID.ABYSSAL_SIRE);
        map(167, NpcID.KRAKEN);
        map(168, NpcID.THERMONUCLEAR_SMOKE_DEVIL);
        map(170, NpcID.MAN);
        map(171, NpcID.WOMAN);
        map(172, NpcID.RABBIT);
        map(173, NpcID.FROG);
        map(174, NpcID.GNOME);
        map(175, NpcID.SHEEP);
        map(176, NpcID.JACKAL);
        map(177, NpcID.VULTURE_1268);
        map(178, NpcID.TORTOISE);
        map(179, NpcID.CHOMPY_BIRD);
        map(180, NpcID.TERRORBIRD);
        map(181, NpcID.BANDIT);
        map(182, NpcID.MONK_OF_ZAMORAK);
        map(183, NpcID.EARTH_WARRIOR);
        map(184, NpcID.CAVE_SLIME);
        map(185, NpcID.BIG_WOLF);
        map(186, NpcID.JOGRE);
        map(187, NpcID.OTHERWORLDLY_BEING);
        map(188, NpcID.SULPHUR_LIZARD);
        map(190, NpcID.MUMMY);
        map(191, NpcID.CAVE_HORROR);
        map(192, NpcID.SUQAH);
        map(193, NpcID.AVIANSIE);
        map(194, NpcID.SPIRITUAL_MAGE);
        map(195, NpcID.ELF_WARRIOR);
        map(196, NpcID.CRAZY_ARCHAEOLOGIST);
        map(197, NpcID.DEVIANT_SPECTRE);
        map(198, NpcID.CHAOS_FANATIC);
        map(200, NpcID.BLACK_DEMON);
        map(201, NpcID.ANCIENT_WYVERN);
        map(202, NpcID.LONGTAILED_WYVERN);
        map(203, NpcID.GORAK);
        map(204, NpcID.WARPED_TERRORBIRD);
        map(205, NpcID.DERANGED_ARCHAEOLOGIST);
        map(206, NpcID.DAWN);
        map(210, NpcID.OBOR);
        map(211, NpcID.BRYOPHYTA);
        map(212, NpcID.HESPORI);
        map(213, NpcID.TEMPOROSS_10572);
        map(214, NpcID.SCURRIUS);
        map(215, NpcID.ARAXXOR);
        map(216, NpcID.AMOXLIATL);
        map(217, NpcID.THE_HUEYCOATL);
        map(218, NpcID.ZEBAK);
        map(219, NpcID.DHAROK_THE_WRETCHED);
        // Raw ids, each verified to render in-world
        map(300, 11934);  // Hero
        map(301, 5971);   // Mounted terrorbird gnome
        map(302, 3297);   // Knight of Ardougne
        map(303, 3026);   // Pit Scorpion
        map(304, 2834);   // Giant bat
        map(305, 3260);   // Warrior
        map(306, 1211);   // Khazard Guard
        map(307, 5965);   // Khazard trooper
        map(308, 2839);   // Black bear
        map(309, 5220);   // Thief
        map(310, 5966);   // Gnome troop
        map(311, 4246);   // Monk
        map(312, 2801);   // Cow calf
        map(313, 2854);   // Rat
        map(314, 3019);   // Spider
        map(315, 15742);  // Maggot King
        map(316, 6739);   // Evil Chicken
        map(317, 15726);  // Vyrewatch Sentinel
        map(318, 15757);  // Dire bat
        map(319, 15760);  // Ancient feral vyre
        map(320, 16223);  // Sanguidae
        map(321, 15743);  // Ur-maggot larvae
        // Bosses. Ids taken from the pinned api gameval NpcID
        // class; the four newest (Brutus/Demonic Brutus/Mad Angel) are wiki ids
        map(322, 1672);   // Ahrim the Blighted
        map(323, 1675);   // Karil the Tainted
        map(324, 1674);   // Guthan the Infested
        map(325, 1676);   // Torag the Corrupted
        map(326, 1677);   // Verac the Defiled
        map(327, 11992);  // Artio (CALLISTO_SINGLES)
        map(328, 11993);  // Calvar'ion (VETION_SINGLE)
        map(329, 11998);  // Spindel (VENENATIS_SINGLES)
        map(330, 11246);  // Revenant maledictus (WILD_CAVE_SUPERIOR)
        map(331, 13016);  // Blood Moon
        map(332, 13017);  // Blue Moon
        map(333, 13018);  // Eclipse Moon
        map(334, 9416);   // Phosani's Nightmare
        map(335, 14176);  // Yama
        map(336, 14707);  // Doom of Mokhaiotl (DOM_BOSS)
        map(337, 12596);  // Branda the Fire Queen
        map(338, 14147);  // Eldric the Ice King
        map(339, 15626);  // Brutus
        map(340, 15628);  // Demonic Brutus
        map(341, 16309);  // Mad Angel
        map(342, 14860);  // Shellbane gryphon
        map(343, 14779);  // Gemstone Crab
        map(344, 7554);   // Great Olm (OLM_HEAD)
        map(345, 8360);   // The Maiden of Sugadinti
        map(346, 8359);   // Pestilent Bloat
        map(347, 8355);   // Nylocas Vasilias
        map(348, 8388);   // Sotetseg
        map(349, 8340);   // Xarpus
        map(350, 8374);   // Verzik Vitur (phase 3)
        map(351, 11778);  // Ba-Ba
        map(352, 11719);  // Kephri
        map(353, 11789);  // Akkha
        map(354, 11756);  // Tumeken's Warden
        map(355, 11753);  // Elidinis' Warden
        map(356, 7706);   // TzKal-Zuk
        map(357, 12821);  // Sol Heredit
        map(358, 9035);   // Corrupted Hunllef
        // Verified to render in-world
        map(359, 15767);  // Venator
        map(360, 713);    // Ice wolf
        map(361, 699);    // Ice troll
        map(362, 15759);  // Impaler deer
        map(363, 4139);   // Thrower troll
        map(364, 11933);  // Paladin
        map(365, 2999);   // Tortured soul
        map(366, 568);    // Zombie pirate
        map(367, 561);    // Sorebones
        map(368, 3233);   // Leech
        map(369, 1794);   // Billy Goat
        map(370, 4176);   // Outlaw
        map(371, 4277);   // Jail guard
        map(372, 107);    // White wolf
        map(373, 1792);   // Goat
        map(374, 4757);   // Tarik
        map(375, 4759);   // Radat
        map(376, 4758);   // Poltenip
        map(377, 5732);   // Market Guard
        map(378, 3910);   // Unicorn Foal
        map(379, 13004);  // Buffalo
        map(380, 3292);   // Al Kharid warrior
        map(381, 3275);   // Gardener
        map(382, 774);    // Skraeling
        map(383, 2992);   // Undead cow
        map(384, 2001);   // Duckling
        map(385, 2993);   // Undead chicken
        map(386, 15224);  // Albatross
        map(387, 2955);   // Saradomin wizard
        map(388, 15194);  // Bull shark
        map(389, 114);    // Guard dog
        map(390, 1559);   // Air wizard
        map(391, 1557);   // Water wizard
        map(392, 1558);   // Earth wizard
        map(393, 1556);   // Fire wizard
        map(394, 3114);   // Farmer
        map(395, 3971);   // Zombie rat
        map(396, 6600);   // Runite Golem
        map(397, 7312);   // Double agent
        map(398, 7310);   // Brassican Mage
        map(399, 6603);   // Rogue
        map(400, 3755);   // Vyrewatch
        map(401, 6588);   // Bandosian guard
        map(402, 7276);   // Mutated Bloodveld
        map(403, 7309);   // Ancient Wizard
        map(404, 8690);   // Ancient Fungi
        map(405, 3429);   // Iorwerth Warrior
        map(406, 1782);   // Scarab Swarm
        map(407, 6587);   // Armadylean guard
        map(408, 15206);  // Pygmy kraken
        map(409, 5295);   // Elf Archer
        map(410, 3428);   // Iorwerth Archer
        map(411, 11073);  // Sergeant
        map(412, 6918);   // Lizardman brute
        map(413, 1046);   // Jungle horror
        map(414, 15216);  // Butterfly ray
        map(415, 2954);   // Zamorak wizard
        map(416, 6916);   // Lizardman
        map(417, 6871);   // Soldier
        map(418, 6795);   // Pyrelord
        map(419, 1610);   // Battle mage
        map(420, 1039);   // Albino bat
        map(421, 6470);   // Animated spade
        map(422, 6469);   // Possessed pickaxe
        map(423, 291);    // Chaos dwarf
        map(424, 7269);   // Magic axe
        map(425, 4406);   // Colonel Radick
        map(426, 3258);   // Druid
        map(427, 5420);   // Watchman
        map(428, 1379);   // Camp dweller
        map(429, 4821);   // Mudskipper
        map(430, 299);    // Gunthor the brave
        map(431, 4405);   // Tower guard
        map(432, 11070);  // Lynx
        map(433, 4819);   // Crab
        map(434, 6081);   // Gnome guard
        map(435, 11071);  // Lynx Tamer
        map(436, 7802);   // Hoop Snake
        map(437, 3901);   // Fox
        map(438, 3909);   // Bear Cub
        map(439, 2648);   // Bark Blamish Snail
        map(440, 1041);   // Giant mosquito
        map(441, 10541);  // Bird
        map(442, 3281);   // Rusty
        map(443, 6087);   // Gnome woman
        map(444, 2910);   // Chinchompa
        map(445, 7555);   // Great Olm (Left claw)
        map(446, 7553);   // Great Olm (Right claw)
        map(447, 8384);   // Nylocas Athanatos
        map(448, 7407);   // Marble gargoyle
        map(449, 7405);   // King kurask
        map(450, 11283);   // Fumus
        map(451, 11285);   // Cruor
        map(452, 11284);   // Umbra
        map(453, 11286);   // Glacies
        map(454, 7406);   // Nuclear smoke devil
        map(455, 8345);   // Nylocas Ischyros
        map(456, 8346);   // Nylocas Toxobolos
        map(457, 8347);   // Nylocas Hagios
        map(458, 7402);   // Abhorrent spectre
        map(459, 6593);   // Lava dragon
        map(460, 15504);   // Magma strykewyrm
        map(461, 15770);   // Blood-starved venator
        map(462, 10397);   // Spiked Turoth
        map(463, 14520);   // Ancient Custodian
        map(464, 7679);   // TzHaar-Ket
        map(465, 7278);   // Greater Nechryael
        map(466, 7104);   // Keef
        map(467, 12464);   // Mutated Terrorbird
        map(468, 11293);   // Blood Reaver
        map(469, 14180);   // Judge of Yama
        map(470, 5275);   // Monkey Guard
        map(471, 7396);   // Malevolent Mage
        map(472, 11291);   // Spiritual ranger
        map(473, 11290);   // Spiritual warrior
        map(474, 5263);   // Padulah
        map(475, 11176);   // Araxyte
        map(476, 14704);   // Elder custodian stalker
        map(477, 2172);   // TzHaar-Xil
        map(478, 14179);   // Void Flare
        map(479, 12490);   // Warped Tortoise
        map(480, 14703);   // Mature custodian stalker
        map(481, 13706);   // Emissary Ascended
        map(482, 15500);   // Lava Strykewyrm
        map(483, 1890);   // Honour guard
        map(484, 8366);   // Nylocas Matomenos
        map(485, 2209);   // Saradomin priest
        map(486, 13799);   // Chilled jelly
        map(487, 15202);   // Narwhal
        map(488, 2240);   // Ork
        map(489, 13788);   // Frost Nagua
        map(490, 2160);   // TzHaar-Mej
        map(491, 2213);   // Knight of Saradomin
        map(492, 7394);   // Flaming pyrelord
        map(493, 14858);   // Gryphon
        map(494, 14702);   // Juvenile custodian stalker
        map(495, 14195);   // Lieutenant
        map(496, 15218);   // Stingray
        map(497, 3959);   // Tormented Warrior
        map(498, 3426);   // Dire Wolf
        map(499, 5274);   // Monkey Archer
        map(500, 11097);   // Head Guard
        map(501, 4373);   // City guard
        map(502, 4319);   // Skeleton Mage
        map(503, 7955);   // Baby black dragon
        map(504, 4381);   // Enclave guard
        map(505, 5829);   // Ice troll male
        map(506, 5830);   // Ice troll female
        map(507, 4362);   // Ogre chieftain
        map(508, 680);   // Giant skeleton
        map(509, 13742);   // Emissary Chosen
        map(510, 7684);   // TzHaar-Hur
        map(511, 1874);   // Ice troll runt
        map(512, 5350);   // Undead one
        map(513, 2453);   // Animated Black Armour
        map(514, 14031);   // Chasm Crawler
        map(515, 12976);   // Jaguar
        map(516, 3023);   // Poison spider
        map(517, 232);   // Jungle Wolf
        map(518, 4655);   // Bedabin Nomad Fighter
        map(519, 8367);   // Blood spawn
        map(520, 3694);   // Vampyre Juvinate
        map(521, 2062);   // Oomlie bird
        map(522, 3692);   // Vampyre Juvenile
        map(523, 3020);   // Jungle spider
        map(524, 866);   // Zogre
        map(525, 4652);   // Ugthanki
        map(526, 4096);   // Archer
        map(527, 1276);   // Loar Shadow
        map(528, 2908);   // Spined larupia
        map(529, 1293);   // Afflicted
        map(530, 14151);   // Ice elemental
        map(531, 3517);   // Renegade Knight
        map(532, 14150);   // Fire elemental
        map(533, 301);   // Black Heather
        map(534, 302);   // Donny the lad
        map(535, 303);   // Speedy Keith
        map(536, 3021);   // Deadly red spider
        map(537, 13735);   // Emissary Acolyte
        map(538, 3424);   // Grizzly bear cub
        map(539, 12977);   // Jaguar cub
        map(540, 4649);   // Desert Wolf
        map(541, 7799);   // Ammonite Crab
        map(542, 1409);   // Black Guard
        map(543, 2538);   // H.A.M. Guard
        map(544, 5816);   // Yak
        map(545, 4643);   // Shantay Guard
        map(546, 1027);   // Guard Bandit
        map(547, 2645);   // Blood Blamish Snail
        map(548, 15228);   // Tern
        map(549, 2647);   // Bruise Blamish Snail
        map(550, 7238);   // Forester
        map(551, 4095);   // Eadburg
        map(552, 3279);   // Cuffs
        map(553, 3685);   // Broddi
        map(554, 3280);   // Narf
        map(555, 3902);   // Bunny
        map(556, 3682);   // Thora
        map(557, 3687);   // Ragnvald
        map(558, 2911);   // Carnivorous chinchompa
        map(559, 3681);   // Rannveig
        map(560, 3683);   // Valgerd
        map(561, 3677);   // Thorhild
        map(562, 3674);   // Ragnar
        map(563, 3675);   // Einar
        map(564, 3676);   // Alrik
    }

    private static void map(int speciesId, int npcId)
    {
        NPC_IDS.put(speciesId, npcId);
    }

    /** The NPC id whose model represents this species, or -1 if unmapped. */
    public static int npcIdFor(int speciesId)
    {
        Integer id = NPC_IDS.get(speciesId);
        return id != null ? id : -1;
    }

    private NpcModelData()
    {
    }
}
