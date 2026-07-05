package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.lexer.Lexer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 解析器综合测试：从旧版 MiniApp 样本到边界用例。
 *
 * @author JustNothing1021
 */
public class ParserComprehensiveTest {

    // ── 辅助 ──

    private static Parser parse(String source) {
        Parser p = new Parser(source, "<test>");
        if (p.hasErrors()) {
            fail("Parse errors: " + p.getErrors());
        }
        return p;
    }

    private static ASTNode parseExpr(String src) {
        return parse(src).parseExpression();
    }

    // ══════════════════════════════════════════
    //  MiniApp 完整源码解析
    // ══════════════════════════════════════════

    private static final String MINIAPP = """
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
                }
            });

            player.put("addItem", (item) -> {
                player.get("inventory").add(item);
            });

            player.put("showStatus", () -> {
                auto weapon = player.get("equippedWeapon");
                auto armor = player.get("equippedArmor");
                if (weapon != null) {
                }
                if (armor != null) {
                }
            });

            player;
        };

        auto createEnemy = (name, hp, attack, defense, gold, exp) -> {
            auto enemy = HashMap.new();
            enemy.put("name", name);
            enemy.put("hp", hp);
            enemy.put("maxHp", hp);
            enemy.put("attack", attack);
            enemy.put("defense", defense);
            enemy.put("gold", gold);
            enemy.put("exp", exp);
            enemy;
        };

        auto createItem = (name, type, value, price) -> {
            auto item = HashMap.new();
            item.put("name", name);
            item.put("type", type);
            item.put("value", value);
            item.put("price", price);
            item;
        };

        auto enemies = ArrayList.new();
        enemies.add(createEnemy("Goblin", 30, 10, 3, 15, 20));
        enemies.add(createEnemy("Wolf", 40, 12, 2, 20, 25));
        enemies.add(createEnemy("Orc", 60, 18, 8, 35, 40));
        enemies.add(createEnemy("Dark Knight", 80, 25, 15, 60, 60));
        enemies.add(createEnemy("Dragon", 150, 35, 20, 200, 150));

        auto shopItems = ArrayList.new();
        shopItems.add(createItem("Health Potion", "potion", 50, 30));
        shopItems.add(createItem("Iron Sword", "weapon", 10, 100));
        shopItems.add(createItem("Steel Sword", "weapon", 20, 250));

        auto calculateDamage = (attacker, defender) -> {
            auto baseDamage = attacker.get("attack");
            auto defense = defender.get("defense");
            auto damage = baseDamage - defense / 2;
            auto variance = random.nextInt(5) - 2;
            damage = damage + variance;
            if (damage < 1) damage = 1;
            damage;
        };

        auto battle = (player, enemy) -> {
            auto playerHp = player.get("hp");
            auto enemyHp = enemy.get("hp");
            while (playerHp > 0 && enemyHp > 0) {
                auto choice = scanner.nextInt();
                if (choice == 1) {
                    auto damage = calculateDamage(player, enemy);
                    enemyHp = enemyHp - damage;
                    if (enemyHp < 0) enemyHp = 0;
                    if (enemyHp > 0) {
                        auto enemyDamage = calculateDamage(enemy, player);
                        playerHp = playerHp - enemyDamage;
                        if (playerHp < 0) playerHp = 0;
                    }
                } else if (choice == 2) {
                    auto inventory = player.get("inventory");
                    auto hasPotion = false;
                    auto i = 0;
                    while (i < inventory.size()) {
                        auto item = inventory.get(i);
                        if (item.get("type").equals("potion")) {
                            hasPotion = true;
                            auto healAmount = player.get("heal").invoke(item.get("value"));
                            inventory.remove(i);
                            break;
                        }
                        i = i + 1;
                    }
                    if (!hasPotion) {
                        auto dummy = 0;
                    }
                    auto enemyDamage = calculateDamage(enemy, player);
                    playerHp = playerHp - enemyDamage;
                } else if (choice == 3) {
                    if (random.nextInt(100) < 30) {
                        return false;
                    } else {
                        auto enemyDamage = calculateDamage(enemy, player);
                        playerHp = playerHp - enemyDamage;
                    }
                }
            }
            player.put("hp", playerHp);
            if (playerHp > 0) {
                player.put("gold", player.get("gold") + enemy.get("gold"));
                player.get("addExp").invoke(enemy.get("exp"));
                true;
            } else {
                false;
            }
        };

        auto showShop = (player) -> {
            auto i = 0;
            while (i < shopItems.size()) {
                auto item = shopItems.get(i);
                i = i + 1;
            }
            auto choice = scanner.nextInt();
            if (choice > 0 && choice <= shopItems.size()) {
                auto item = shopItems.get(choice - 1);
                if (player.get("gold") >= item.get("price")) {
                    player.put("gold", player.get("gold") - item.get("price"));
                    player.get("addItem").invoke(item);
                }
            }
        };

        auto showInventory = (player) -> {
            auto inventory = player.get("inventory");
            if (inventory.size() == 0) {
                return;
            }
            auto i = 0;
            while (i < inventory.size()) {
                auto item = inventory.get(i);
                i = i + 1;
            }
            auto choice = scanner.nextInt();
            if (choice > 0 && choice <= inventory.size()) {
                auto item = inventory.get(choice - 1);
                auto type = item.get("type");
                if (type.equals("weapon")) {
                    player.put("equippedWeapon", item);
                    player.put("attack", player.get("attack") + item.get("value"));
                    inventory.remove(choice - 1);
                } else if (type.equals("armor")) {
                    player.put("equippedArmor", item);
                    player.put("defense", player.get("defense") + item.get("value"));
                    inventory.remove(choice - 1);
                } else if (type.equals("potion")) {
                    auto healAmount = player.get("heal").invoke(item.get("value"));
                    inventory.remove(choice - 1);
                }
            }
        };

        auto explore = (player) -> {
            auto encounter = random.nextInt(100);
            if (encounter < 50) {
                auto enemyIndex = random.nextInt(enemies.size());
                auto enemy = enemies.get(enemyIndex);
                battle(player, enemy);
            } else if (encounter < 70) {
                auto goldFound = random.nextInt(30) + 10;
                player.put("gold", player.get("gold") + goldFound);
            } else if (encounter < 85) {
                auto potion = createItem("Health Potion", "potion", 50, 30);
                player.get("addItem").invoke(potion);
            }
        };

        auto playerName = scanner.next();
        auto player = createPlayer(playerName);
        auto playing = true;
        while (playing && player.get("hp") > 0) {
            auto choice = scanner.nextInt();
            if (choice == 1) {
                explore(player);
            } else if (choice == 2) {
                showShop(player);
            } else if (choice == 3) {
                showInventory(player);
            } else if (choice == 4) {
                player.get("showStatus").invoke();
            } else if (choice == 5) {
                if (player.get("gold") >= 10) {
                    player.put("gold", player.get("gold") - 10);
                    player.put("hp", player.get("maxHp"));
                }
            } else if (choice == 0) {
                playing = false;
            }
        }
        if (player.get("hp") <= 0) {
            auto finalLevel = player.get("level");
            auto finalGold = player.get("gold");
        }
        """;

    @Test
    public void miniAppFullParse() {
        long start = System.nanoTime();
        List<ASTNode> result = parse(MINIAPP).parseProgram();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertNotNull("MiniApp parse result should not be null", result);
        assertFalse("MiniApp parse should produce declarations", result.isEmpty());
        System.out.println("MiniApp parsed: " + result.size() + " top-level decls in " + elapsed + " ms");
    }

    // ══════════════════════════════════════════
    //  for-each 变体
    // ══════════════════════════════════════════

    @Test public void forEachInt()     { parseStmt("for (int i : list) { }"); }
    @Test public void forEachVar()     { parseStmt("for (var x : items) { }"); }
    @Test public void forEachShort()   { parseStmt("for (x : list) { }"); }
    @Test public void forEachIdType()  { parseStmt("for (String s : strings) { }"); }

    @Test
    public void forEachLambdaBody() {
        parseStmt("for (int i : list) { auto x = i * 2; }");
    }

    // ══════════════════════════════════════════
    //  Lambda 边界
    // ══════════════════════════════════════════

    @Test public void lambdaNoParam()    { parseExpr("() -> 42"); }
    @Test public void lambdaOneParam()   { parseExpr("x -> x * 2"); }
    @Test public void lambdaMultiParam() { parseExpr("(a, b) -> a + b"); }
    @Test public void lambdaBlockBody()  { parseExpr("(x) -> { return x + 1; }"); }
    @Test public void lambdaFnCall()     { parseExpr("x -> foo(x)"); }

    // ══════════════════════════════════════════
    //  链式调用
    // ══════════════════════════════════════════

    @Test public void chainField()     { parseExpr("obj.field"); }
    @Test public void chainMethod()    { parseExpr("obj.method()"); }
    @Test public void chainDeep()      { parseExpr("a.b.c.d()"); }
    @Test public void chainMixed()     { parseExpr("map.get(\"key\").value"); }
    @Test public void chainSafeDeref() { parseExpr("obj?.field"); }

    // ══════════════════════════════════════════
    //  复杂表达式
    // ══════════════════════════════════════════

    @Test public void ternaryNested()  { parseExpr("a ? b ? c : d : e"); }
    @Test public void binaryChain()    { parseExpr("1 + 2 * 3 - 4 / 5"); }
    @Test public void parenNested()    { parseExpr("((a + b) * (c - d))"); }
    @Test public void prefixUnary()    { parseExpr("-!~+x"); }
    @Test public void newArray()       { parseExpr("new int[10]"); }
    @Test public void arrayLiteral()   { parseExpr("[1, 2, 3]"); }
    @Test public void instanceofExpr() { parseExpr("x instanceof String"); }
    @Test public void methodRef()      { parseExpr("System.out::println"); }

    // ══════════════════════════════════════════
    //  语句组合
    // ══════════════════════════════════════════

    @Test public void stmtExpr()       { parseStmt("x = 42;"); }
    @Test public void stmtIfElseIf()   { parseStmt("if (a) { } else if (b) { } else { }"); }
    @Test public void stmtWhile()      { parseStmt("while (x > 0) { x = x - 1; }"); }
    @Test public void stmtDoWhile()    { parseStmt("do { x = x - 1; } while (x > 0);"); }
    @Test public void stmtForClassic() { parseStmt("for (i = 0; i < 10; i = i + 1) { }"); }
    @Test public void stmtSwitch()     { parseStmt("switch (x) { case 1: break; default: }"); }
    @Test public void stmtReturn()     { parseStmt("return 42;"); }
    @Test public void stmtThrow()      { parseStmt("throw new Exception();"); }
    @Test public void stmtTryCatch()   { parseStmt("try { } catch (Exception e) { }"); }
    @Test public void stmtBlock()      { parseStmt("{ a; b; }"); }

    // ══════════════════════════════════════════
    //  声明
    // ══════════════════════════════════════════

    @Test public void declClass()      { parseProgram("class Foo { }"); }
    @Test public void declInterface()  { parseProgram("interface Bar { }"); }
    @Test public void declEnum()       { parseProgram("enum Color { RED, GREEN, BLUE }"); }
    @Test public void declImport()     { parseProgram("import java.util.List;"); }
    @Test public void declPackage()    { parseProgram("package com.test;"); }
    @Test public void declVarAuto()    { parseProgram("auto x = 42;"); }
    @Test public void declClassFull()  {
        parseProgram("""
            class Player {
                int hp;
                String name;
                void attack(Player target) { }
                Player(String name) { }
            }
            """);
    }

    // ══ helpers ══

    static void parseStmt(String src) {
        ASTNode n = parse(src).parseStatement();
        assertNotNull("stmt parse failed: " + src, n);
    }

    static void parseProgram(String src) {
        List<ASTNode> n = parse(src).parseProgram();
        assertNotNull("program parse failed: " + src, n);
        assertFalse("empty program: " + src, n.isEmpty());
    }
}
