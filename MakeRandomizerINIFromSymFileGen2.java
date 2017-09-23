import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class MakeRandomizerINIFromSymFileGen2 {

    private static Map<String, Integer> romSymbols = new TreeMap<String, Integer>();
    private static byte[] rom;
    
    private static String pokecrystalPath = "/home/sam/pokecrystal-speedchoice/";

    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(new File(pokecrystalPath+"crystal-speedchoice.sym"),
                "UTF-8");
        rom = Functions.readFileFullyIntoBuffer(pokecrystalPath+"crystal-speedchoice.gbc");

        while (sc.hasNextLine()) {
            String ln = sc.nextLine().trim();
            if (ln.length() > 3 && ln.charAt(2) == ':') {
                int address = Integer.parseInt(ln.substring(3, 7), 16);
                if (address < 0x8000) {
                    int bank = Integer.parseInt(ln.substring(0, 2), 16);
                    int offset = (bank == 0) ? address : (bank - 1) * 0x4000 + address;
                    romSymbols.put(ln.substring(8), offset);
                }
            }
        }

        sc.close();

        System.out.println("[Crystal SpeedChoice v"+((rom[0x14C] & 0xFF)+1)+"]");
        String gameCode = new String(rom, 0x13F, 0x4, "US-ASCII");
        System.out.println("Game=" + gameCode);
        System.out.println("Version=" + (rom[0x14C] & 0xFF));
        System.out.println("NonJapanese=" + (rom[0x14A] & 0xFF));
        System.out.println("Type=Crystal");
        System.out.println("ExtraTableFile=gsc_english");

        printSymbol("PokemonNamesOffset", "PokemonNames");
        System.out.println("PokemonNamesLength=10");
        printSymbol("PokemonStatsOffset", "BaseData0");
        printSymbol("WildPokemonOffset", "JohtoGrassWildMons");
        printSymbol("FishingWildsOffset", "FishGroupShore_Old");
        printSymbol("HeadbuttWildsOffset", "TreeMons1");
        System.out.println("HeadbuttTableSize=13");
        printSymbol("BCCWildsOffset", "ContestMons");
        printSymbol("FleeingDataOffset", "FleeMons");
        printSymbol("MoveDataOffset", "MovesHMNerfs");
        printSymbol("MoveNamesOffset", "MoveNamesNerfedHMs");
        printSymbol("ItemNamesOffset", "ItemNames");
        printSymbol("PokemonMovesetsTableOffset", "EvosAttacksPointers");
        System.out.println("SupportsFourStartingMoves=1");

        // STARTERS

        printStarter(1, "Randomizer_StarterCyndaquilOffset");
        printStarter(2, "Randomizer_StarterTotodileOffset");
        printStarter(3, "Randomizer_StarterChikoritaOffset");

        printStarterHeldItems("Cyndaquil", "Totodile", "Chikorita");
        printStarterTextOffsets("Cyndaquil", "Totodile", "Chikorita");

        System.out.println("CanChangeStarterText=1");
        System.out.println("CanChangeTrainerText=1");

        System.out.println("TrainerClassAmount=0x43");
        printSymbol("TrainerDataTableOffset", "TrainerGroups");
        System.out
                .println("TrainerDataClassCounts=[1, 1, 1, 1, 1, 1, 1, 1, 15, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 5, 1, 14, 24, 19, 17, 1, 20, 21, 17, 15, 31, 5, 2, 3, 1, 19, 25, 21, 19, 13, 14, 6, 2, 22, 9, 1, 3, 8, 6, 9, 4, 12, 26, 22, 2, 12, 7, 3, 14, 6, 10, 6, 2, 1, 2, 5, 1]");
        printSymbol("TMMovesOffset", "TMHMMoves");
        printSymbol("TrainerClassNamesOffset", "TrainerClassNames");
        
        // Bytes available for trainer names
        int bytesForTrainerNames = 2691;
        int tOffset = 0x3BFFF;
        while(rom[tOffset--] == 0) {
            bytesForTrainerNames++;
        }
        System.out.println("MaxSumOfTrainerNameLengths="+bytesForTrainerNames);
        System.out.println("DoublesTrainerClasses=[60] // only twins");
        
        printSymbol("IntroSpriteOffset", "Randomizer_IntroSpriteOffset", 1);
        printSymbol("IntroCryOffset", "Randomizer_IntroCryOffset", 1);
        printSymbol("MapHeaders", "MapGroupPointers");
        printSymbol("LandmarkTableOffset", "Landmarks");
        System.out.println("LandmarkCount=96");
        printSymbol("TradeTableOffset", "NPCTrades");
        System.out.println("TradeTableSize=7");
        System.out.println("TradeNameLength=11");
        System.out.println("TradeOTLength=11");
        System.out.println("TradesUnused=[]");
        System.out.println("TextDelayFunctionOffset=0");

        // CATCHING TUTORIAL

        printCatchingTutorialOffsets();

        printSymbol("PicPointers", "PicPointers");
        printSymbol("PokemonPalettes", "PokemonPalettes");

        // MOVE TUTOR MOVES

        System.out.printf("MoveTutorMoves=[0x%X, 0x%X, 0x%X]", romSymbols.get("MoveTutorMove_Flamethrower"),
                romSymbols.get("MoveTutorMove_Thunderbolt"), romSymbols.get("MoveTutorMove_IceBeam"));
        System.out.println();

        printSymbol("MoveTutorMenuOffset", "Randomizer_MoveTutorMenuOffset");
        printSymbol("MoveTutorMenuNewSpace", "Randomizer_MoveTutorMenuNewSpace");
        
        // check value
        printSymbol("CheckValueOffset", "CheckValue");

        // STATIC POKEMON

        System.out.println("StaticPokemonSupport=1");
        System.out.println("GameCornerPokemonNameLength=11");

        handleStaticPokemon();

        // TM TEXT
        handleTMText();
    }

    private static void handleTMText() {
        printTMText(1, "That is\\n%m.\\e", "UnknownText_0x9d8da");
        printTMText(3, "TM03 is\\n%m.\\pIt's a terrifying\\nmove!\\e", "UnknownText_0x71db3");
        printTMText(5, "WROOOAR!\\nIT'S %m!\\e", "Text_RoarOutro");
        printTMText(6, "JANINE: You're so\\ntough! I have a \\lspecial gift!\\pIt's %m!\\e", "UnknownText_0x196002");
        printTMText(7, "MANAGER: TM07 is\\nmy %m.\\pIt's a powerful\\ntechnique!\\e", "UnknownText_0x1893f4");
        printTMText(8, "That happens to be\\n%m.\\pIf any rocks are\\nin your way, find\\lROCK SMASH!\\e",
                "UnknownText_0x19452c");
        printTMText(10, "Do you see it? It\\n is %m!\\e", "HiddenPowerGuyText2");
        printTMText(11, "It's %m.\\nUse it wisely.\\e", "UnknownText_0x5e821");
        printTMText(12, "It's %m.\\pUse it on\\nenemy [POKé]MON.\\e", "UnknownText_0x62df6");
        printTMText(13, "That there's\\n%m.\\pIt's a rare move.\\e", "UnknownText_0x9d1c7");
        printTMText(16, "That TM contains\\n%m.\\pIt demonstrates\\nthe harshness of\\lwinter.\\e",
                "UnknownText_0x199def");
        printTMText(
                19,
                "That was a\\ndelightful match.\\pI felt inspired.\\nPlease, I wish you\\lto have this TM.\\pIt's %m.\\pIt is a wonderful\\nmove!\\pPlease use it if\\nit pleases you…\\e",
                "UnknownText_0x72cb0");
        printTMText(23, "…That teaches\\n%m.\\e", "UnknownText_0x9c3a5");
        printTMText(24, "That contains\\n%m.\\pIf you don't want\\nit, you don't have\\lto take it.\\e",
                "ClairText_DescribeDragonbreathDragonDen");
        printTMText(24, "That contains\\n%m.\\pIf you don't want\\nit, you don't have\\lto take it.\\e",
                "BlackthornGymClairText_DescribeTM24");
        printTMText(29, "TM29 is\\n%m.\\pIt may be\\nuseful.\\e", "MrPsychicText2");
        printTMText(30, "It's %m.\\pUse it if it\\nappeals to you.\\e", "UnknownText_0x9a0ec");
        printTMText(
                31,
                "By using a TM, a\\n[POKé]MON will\\pinstantly learn a\\nnew move.\\pThink before you\\nact--a TM can be\\lused only once.\\pTM31 contains\\n%m.\\e",
                "UnknownText_0x68648");
        printTMText(37,
                "TM37 happens to be\\n%m.\\pIt's for advanced\\ntrainers only.\\pUse it if you\\ndare. Good luck!\\e",
                "SandstormHouseSandstormDescription");
        printTMText(42, "TM42 contains\\n%m…\\p…Zzz…\\e", "UnknownText_0x1a9d86");
        printTMText(45, "It's %m!\\pIsn't it just per-\\nfect for a cutie\\llike me?\\e", "UnknownText_0x54302");
        printTMText(49, "TM49 contains\\n%m.\\pIsn't it great?\\nI discovered it!\\e", "BugsyText_FuryCutterSpeech");
        printTMText(50, "TM50 is\\n%m.\\pOoooh…\\nIt's scary…\\pI don't want to\\nhave bad dreams.\\e",
                "Text_Route31DescribeNightmare");
    }

    private static void printTMText(int number, String text, String label) {
        System.out.printf("TMText[]=[%d,0x%X,%s]", number, romSymbols.get(label) + 1, text);
        System.out.println();
    }

    private static void handleStaticPokemon() {
        // Regular static Pokemon
        // Overworlds
        handleStaticPoke("Lapras");
        handleStaticPoke("Electrode1");
        handleStaticPoke("Electrode2");
        handleStaticPoke("Electrode3");
        handleStaticPoke("Lugia");
        handleStaticPoke("RedGyarados");
        handleStaticPoke("Sudowoodo");
        handleStaticPoke("Snorlax");
        handleStaticPoke("HoOh");
        handleStaticPoke("Suicune");
        // Rocket Base
        handleStaticPoke("Voltorb");
        handleStaticPoke("Geodude");
        handleStaticPoke("Koffing");
        // Gifts
        handleStaticPoke("Shuckle");
        handleStaticPoke("Tyrogue");
        handleStaticPoke("Togepi");
        handleStaticPoke("Kenya");
        handleStaticPoke("Eevee");
        handleStaticPoke("Dratini");
        // Roamers
        handleStaticPoke("Raikou");
        handleStaticPoke("Entei");

        // Odd Egg
        System.out.printf("StaticPokemonOddEggOffset=0x%X", romSymbols.get("Randomizer_OddEgg1"));
        System.out.println();
        System.out.printf("StaticPokemonOddEggDataSize=0x%X",
                romSymbols.get("Randomizer_OddEgg2") - romSymbols.get("Randomizer_OddEgg1"));
        System.out.println();

        // Game Corner
        handleGameCornerPoke("Abra");
        handleGameCornerPoke("Cubone");
        handleGameCornerPoke("Wobbuffet");
        handleGameCornerPoke("Pikachu");
        handleGameCornerPoke("Porygon");
        handleGameCornerPoke("Larvitar");
    }

    private static void handleGameCornerPoke(String pokeName) {
        String prefix = "Randomizer_GameCorner" + pokeName;
        System.out.printf("StaticPokemonGameCorner[]=[0x%X, 0x%X, 0x%X, 0x%X]",
                romSymbols.get(prefix + "Species1") + 1, romSymbols.get(prefix + "Species2") + 1,
                romSymbols.get(prefix + "Species3") + 1, romSymbols.get(prefix + "Name"));
        System.out.println();
    }

    private static void handleStaticPoke(String pokeName) {
        List<Integer> offsets = new ArrayList<Integer>();
        for (String key : romSymbols.keySet()) {
            if (key.startsWith("Randomizer_" + pokeName)) {
                offsets.add(romSymbols.get(key) + 1);
            }
        }
        if (offsets.size() == 1) {
            System.out.printf("StaticPokemon[]=0x%X", offsets.get(0));
        } else {
            System.out.print("StaticPokemon[]=[");
            for (int i = 0; i < offsets.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.printf("0x%X", offsets.get(i));
            }
            System.out.print("]");
        }
        System.out.println(" // " + pokeName);
    }

    private static void printCatchingTutorialOffsets() {
        System.out.printf("CatchingTutorialOffsets=[0x%X, 0x%X, 0x%X]",
                romSymbols.get("Randomizer_CatchingTutorialMonOffset1") + 1,
                romSymbols.get("Randomizer_CatchingTutorialMonOffset2") + 1,
                romSymbols.get("Randomizer_CatchingTutorialMonOffset3") + 1);
        System.out.println();
    }

    private static void printStarterHeldItems(String starter1, String starter2, String starter3) {
        System.out.printf("StarterHeldItems=[0x%X, 0x%X, 0x%X]",
                romSymbols.get("Randomizer_Starter" + starter1 + "Offset4") + 3,
                romSymbols.get("Randomizer_Starter" + starter2 + "Offset4") + 3,
                romSymbols.get("Randomizer_Starter" + starter3 + "Offset4") + 3);
        System.out.println();

    }
    
    private static void printStarterTextOffsets(String starter1, String starter2, String starter3) {
        System.out.printf("StarterTextOffsets=[0x%X, 0x%X, 0x%X]",
                romSymbols.get("Randomizer_Starter" + starter1 + "TextOffset") + 1,
                romSymbols.get("Randomizer_Starter" + starter2 + "TextOffset") + 1,
                romSymbols.get("Randomizer_Starter" + starter3 + "TextOffset") + 1);
        System.out.println();

    }

    private static void printStarter(int num, String mapKey) {
        int[] offsets = new int[4];
        for (int i = 1; i <= 4; i++) {
            offsets[i - 1] = romSymbols.get(mapKey + i) + 1;
        }
        System.out.printf("StarterOffsets%d=[0x%X, 0x%X, 0x%X, 0x%X]", num, offsets[0], offsets[1], offsets[2],
                offsets[3]);
        System.out.println();
    }

    private static void printSymbol(String key, String mapKey) {
        printSymbol(key, mapKey, 0);
    }

    private static void printSymbol(String key, String mapKey, int extraOffset) {
        System.out.printf("%s=0x%X", key, romSymbols.get(mapKey) + extraOffset);
        System.out.println();
    }

}
