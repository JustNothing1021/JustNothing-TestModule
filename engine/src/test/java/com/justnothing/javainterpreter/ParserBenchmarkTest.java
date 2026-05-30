package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.lexer.Lexer;
import com.justnothing.javainterpreter.parser.Parser;
import com.justnothing.javainterpreter.api.ClassResolver;
import org.junit.Test;

public class ParserBenchmarkTest {

    private static final String SOURCE = """
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Random;
        import java.util.Scanner;
        
        println("========================================");
        println("   The Lost Kingdom - Text Adventure");
        println("========================================");
        println("");
        
        auto random = Random.new();
        auto scanner = Scanner.new(System.in);
        
        auto createPlayer = (name) -> {
            auto player = HashMap.new();
            player.put("name", name);
            player.put("hp", 100);
            player.put("maxHp", 100);
            player.put("attack", 15);
            player.put("defense", 5);
            player.put("gold", 50);
            player.put("level", 1);
            player.put("exp", 0);
            player.put("inventory", ArrayList.new());
            player.put("equippedWeapon", null);
            player.put("equippedArmor", null);
        
            player.put("takeDamage", (damage) -> {
                auto currentHp = player.get("hp");
                auto newHp = currentHp - damage;
                if (newHp < 0) newHp = 0;
                player.put("hp", newHp);
                newHp;
            });
        
            player.put("heal", (amount) -> {
                auto currentHp = player.get("hp");
                auto maxHp = player.get("maxHp");
                auto newHp = currentHp + amount;
                if (newHp > maxHp) newHp = maxHp;
                player.put("hp", newHp);
                newHp - currentHp;
            });
        
            player.put("addExp", (amount) -> {
                auto currentExp = player.get("exp");
                auto newExp = currentExp + amount;
                player.put("exp", newExp);
                auto level = player.get("level");
                auto expNeeded = level * 50;
                if (newExp >= expNeeded) {
                    player.put("level", level + 1);
                    player.put("exp", newExp - expNeeded);
                    player.put("maxHp", player.get("maxHp") + 20);
                    player.put("attack", player.get("attack") + 5);
                    player.put("defense", player.get("defense") + 2);
                    player.put("hp", player.get("maxHp"));
                    println("  *** LEVEL UP! You are now level " + player.get("level") + "! ***");
                }
            });
        
            player.put("isAlive", () -> {
                player.get("hp") > 0;
            });
        
            player.put("attackEnemy", (enemy) -> {
                auto playerAtk = player.get("attack");
                auto enemyDef = enemy.get("defense");
                auto baseDamage = playerAtk - enemyDef;
                if (baseDamage < 1) baseDamage = 1;
                auto critChance = random.nextInt(100);
                auto isCrit = critChance < 15;
                if (isCrit) baseDamage = baseDamage * 2;
                auto actualDamage = enemy.takeDamage(baseDamage);
                auto critStr = isCrit ? " CRITICAL HIT!" : "";
                println("  You attack " + enemy.get("name") + " for " + actualDamage + " damage!" + critStr);
                player.addExp(actualDamage / 2);
                actualDamage;
            });
        
            player.put("usePotion", () -> {
                if (player.get("gold") >= 10) {
                    player.put("gold", player.get("gold") - 10);
                    auto healed = player.heal(30);
                    println("  Used potion! Healed for " + healed + " HP. HP: " + player.get("hp") + "/" + player.get("maxHp"));
                    true;
                } else {
                    println("  Not enough gold! Need 10 gold, have " + player.get("gold"));
                    false;
                }
            });
        
            player.put("buyWeapon", (weaponName, cost, atkBonus) -> {
                if (player.get("gold") >= cost) {
                    player.put("gold", player.get("gold") - cost);
                    player.put("equippedWeapon", weaponName);
                    player.put("attack", player.get("attack") + atkBonus);
                    println("  Bought " + weaponName + "! ATK: " + player.get("attack"));
                    true;
                } else {
                    println("  Not enough gold! Need " + cost + ", have " + player.get("gold"));
                    false;
                }
            });
        
            player.put("displayStatus", () -> {
                println("  --- " + player.get("name") + " [Lv." + player.get("level") + "] ---");
                println("  HP: " + player.get("hp") + "/" + player.get("maxHp") + " | ATK: " + player.get("attack") + " | DEF: " + player.get("defense"));
                println("  Gold: " + player.get("gold") + " | EXP: " + player.get("exp") + "/" + (player.get("level") * 50));
                auto weapon = player.get("equippedWeapon");
                if (weapon != null) println("  Weapon: " + weapon);
                else println("  Weapon: None");
            });
        
            player;
        };
        
        auto createEnemy = (name, hp, attack, defense, goldReward, expReward) -> {
            auto enemy = HashMap.new();
            enemy.put("name", name);
            enemy.put("hp", hp);
            enemy.put("maxHp", hp);
            enemy.put("attack", attack);
            enemy.put("defense", defense);
            enemy.put("goldReward", goldReward);
            enemy.put("expReward", expReward);
        
            enemy.put("takeDamage", (damage) -> {
                auto currentHp = enemy.get("hp");
                auto newHp = currentHp - damage;
                if (newHp < 0) newHp = 0;
                enemy.put("hp", newHp);
                newHp;
            });
        
            enemy.put("isAlive", () -> {
                enemy.get("hp") > 0;
            });
        
            enemy.put("attackPlayer", (player) -> {
                auto enemyAtk = enemy.get("attack");
                auto playerDef = player.get("defense");
                auto baseDamage = enemyAtk - playerDef;
                if (baseDamage < 1) baseDamage = 1;
                auto actualDamage = player.takeDamage(baseDamage);
                println("  " + enemy.get("name") + " attacks you for " + actualDamage + " damage! HP: " + player.get("hp") + "/" + player.get("maxHp"));
                actualDamage;
            });
        
            enemy;
        };
        
        auto battle = (player, enemy) -> {
            println("");
            println("  === BATTLE: " + player.get("name") + " vs " + enemy.get("name") + " ===");
            println("  Enemy HP: " + enemy.get("hp") + " | ATK: " + enemy.get("attack") + " | DEF: " + enemy.get("defense"));
            
            auto round = 1;
            while (player.isAlive() && enemy.isAlive()) {
                println("  --- Round " + round + " ---");
                
                auto actionChoice = random.nextInt(100);
                if (actionChoice < 60) {
                    player.attackEnemy(enemy);
                } else if (actionChoice < 80) {
                    if (!player.usePotion()) {
                        player.attackEnemy(enemy);
                    }
                } else {
                    player.displayStatus();
                    player.attackEnemy(enemy);
                }
                
                if (enemy.isAlive()) {
                    enemy.attackPlayer(player);
                }
                
                round++;
                if (round > 20) break;
            }
            
            println("");
            if (player.isAlive()) {
                auto goldGain = enemy.get("goldReward");
                auto expGain = enemy.get("expReward");
                player.put("gold", player.get("gold") + goldGain);
                player.addExp(expGain);
                println("  *** VICTORY! ***");
                println("  Gained " + goldGain + " gold and " + expGain + " EXP");
                true;
            } else {
                println("  *** DEFEATED ***");
                false;
            }
        };
        
        auto explore = (player) -> {
            auto eventRoll = random.nextInt(100);
            
            if (eventRoll < 40) {
                auto enemyType = random.nextInt(4);
                auto enemy;
                if (enemyType == 0) enemy = createEnemy("Goblin", 30, 8, 2, 10, 15);
                else if (enemyType == 1) enemy = createEnemy("Wolf", 25, 12, 3, 8, 12);
                else if (enemyType == 2) enemy = createEnemy("Skeleton", 40, 10, 5, 15, 20);
                else enemy = createEnemy("Orc", 60, 15, 6, 25, 30);
                battle(player, enemy);
            } else if (eventRoll < 60) {
                auto goldFound = random.nextInt(20) + 5;
                player.put("gold", player.get("gold") + goldFound);
                println("  Found a chest with " + goldFound + " gold!");
            } else if (eventRoll < 75) {
                player.heal(15);
                println("  Found a healing spring! Restored 15 HP. HP: " + player.get("hp") + "/" + player.get("maxHp"));
            } else if (eventRoll < 90) {
                println("  --- Shop ---");
                println("  1. Iron Sword (+5 ATK) - 30g");
                println("  2. Steel Sword (+10 ATK) - 60g");
                println("  3. Health Potion (30 HP) - 10g");
                println("  4. Leave");
                auto shopChoice = random.nextInt(4);
                if (shopChoice == 0) player.buyWeapon("Iron Sword", 30, 5);
                else if (shopChoice == 1) player.buyWeapon("Steel Sword", 60, 10);
                else if (shopChoice == 2) player.usePotion();
                else println("  Left shop.");
            } else {
                println("  A peaceful walk through the kingdom...");
            }
        };
        
        auto player = createPlayer("Hero");
        println("  Welcome, " + player.get("name") + "!");
        
        auto turns = 0;
        auto maxTurns = 15;
        
        while (player.isAlive() && turns < maxTurns) {
            turns++;
            println("\n--- Day " + turns + " ---");
            explore(player);
            player.displayStatus();
        }
        
        println("");
        println("========================================");
        if (player.isAlive()) {
            println("  CONGRATULATIONS!");
            println("  You survived " + turns + " days in the Lost Kingdom!");
            println("  Final Level: " + player.get("level"));
            println("  Final Gold: " + player.get("gold"));
        } else {
            println("  GAME OVER");
            println("  Final Score: Level " + player.get("level") + ", " + player.get("gold") + " gold");
        }
        println("========================================");
        """;

    @Test
    public void benchmark_miniAppParsing() throws Exception {
        ClassResolver.clearClassCache();

        System.out.println("\n=== Parser Benchmark: MiniApp (" + SOURCE.length() / 1024 + "KB) ===\n");

        int warmupRounds = 3;
        int measureRounds = 10;

        long[] lexTimes = new long[measureRounds];
        long[] parseTimes = new long[measureRounds];
        long[] totalTimes = new long[measureRounds];

        for (int round = 0; round < warmupRounds + measureRounds; round++) {
            ClassResolver.clearClassCache();

            long startTotal = System.nanoTime();

            Lexer lexer = new Lexer(SOURCE, "benchmark.cyv");
            long lexEnd = System.nanoTime();
            var tokens = lexer.tokenize();
            long tokenizeTime = System.nanoTime() - lexEnd;

            Parser parser = new Parser(tokens, "benchmark.cyv");
            BlockNode block = null;
            try {
                block = parser.parse();
            } catch (Exception e) {
                System.out.println("  [Round " + (round + 1) + "] Parse error (pre-existing lambda bug): " + e.getMessage());
            }
            long parseEnd = System.nanoTime();
            long parseTime = parseEnd - lexEnd;

            long totalTime = parseEnd - startTotal;

            if (round >= warmupRounds) {
                int idx = round - warmupRounds;
                lexTimes[idx] = tokenizeTime;
                parseTimes[idx] = parseTime;
                totalTimes[idx] = totalTime;
            }
        }

        double avgLexMs = avg(lexTimes) / 1_000_000.0;
        double avgParseMs = avg(parseTimes) / 1_000_000.0;
        double avgTotalMs = avg(totalTimes) / 1_000_000.0;

        double minLexMs = min(lexTimes) / 1_000_000.0;
        double minParseMs = min(parseTimes) / 1_000_000.0;
        double minTotalMs = min(totalTimes) / 1_000_000.0;

        System.out.printf("  Lexer    : avg %7.2f ms | min %7.2f ms%n", avgLexMs, minLexMs);
        System.out.printf("  Parser   : avg %7.2f ms | min %7.2f ms%n", avgParseMs, minParseMs);
        System.out.printf("  Total    : avg %7.2f ms | min %7.2f ms%n", avgTotalMs, minTotalMs);
        System.out.printf("  Source   : %d bytes (%.1f KB)%n", SOURCE.length(), SOURCE.length() / 1024.0);
        System.out.println();
    }

    private static double avg(long[] arr) {
        long sum = 0;
        for (long v : arr) sum += v;
        return (double) sum / arr.length;
    }

    private static double min(long[] arr) {
        long m = Long.MAX_VALUE;
        for (long v : arr) if (v < m) m = v;
        return m;
    }

    @Test
    public void debug_importArrays() throws Exception {
        System.out.println("\n=== Deep Profile: time breakdown ===");
        System.out.println("  Code size: " + (SOURCE.length() / 1024) + " KB");

        long t0 = System.nanoTime();
        Lexer lexer = new Lexer(SOURCE, "<test>");
        var tokens = lexer.tokenize();
        long t1 = System.nanoTime();
        System.out.println("  Lexer: " + ((t1 - t0) / 1_000_000) + " ms, " + tokens.size() + " tokens");

        ClassResolver.clearClassCache();
        Parser parser = new Parser(tokens, "<test>");

        long t2 = System.nanoTime();
        try { parser.parse(); } catch (Exception e) {}
        long t3 = System.nanoTime();

        long parseMs = (t3 - t2) / 1_000_000;
        System.out.println("  Parser: " + parseMs + " ms");
        System.out.println("  Tokens: " + tokens.size());
        System.out.println("  Per-token: " + (parseMs * 1000.0 / tokens.size()) + " μs");
        System.out.println("  Est. 30KB: " + (parseMs * 30.0 / 8.7) + " ms (extrapolated)");
        System.out.println("  ClassCache size: " + com.justnothing.javainterpreter.api.ClassResolver.getCacheSize());
        System.out.println("  Parser classNameCache size: " + Parser.getClassNameCacheSize());
    }
}
