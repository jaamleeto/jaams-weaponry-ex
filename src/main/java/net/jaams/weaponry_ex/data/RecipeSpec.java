package net.jaams.weaponry_ex.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecipeSpec(
        String weapon,
        String category,
        List<String> pattern,
        Map<Character, String> key,
        String smithingAddition
) {

    public static final String RAW = "@raw";
    public static final String DOUBLE = "@double";
    public static final String HEAVY = "@heavy";

    public static final String SHORT_STICK = "jaams_weaponry:short_stick";
    public static final String LONG_STICK = "jaams_weaponry:long_stick";
    public static final String STICK = "minecraft:stick";

    private static final String EQUIPMENT = "equipment";

    public static final List<RecipeSpec> RECIPES = List.of(
            new RecipeSpec("dagger", EQUIPMENT, List.of(" a", "b "),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("dagger_reverse", EQUIPMENT, List.of(" a", "b "),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("knuckle", EQUIPMENT, List.of(" a", "ab"),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("cleaver", EQUIPMENT, List.of(" a", "b "),
                    key("a", DOUBLE, "b", SHORT_STICK), RAW),
            new RecipeSpec("ring", EQUIPMENT, List.of("aa", "ab"),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("kama", EQUIPMENT, List.of("aa", " b"),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("claw", EQUIPMENT, List.of("a ", " a", "ab"),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("spear", EQUIPMENT, List.of("  a", " b ", "c  "),
                    key("a", DOUBLE, "b", LONG_STICK, "c", STICK), RAW),
            new RecipeSpec("machete", EQUIPMENT, List.of("  a", " a ", "b  "),
                    key("a", RAW, "b", SHORT_STICK), RAW),
            new RecipeSpec("katar", EQUIPMENT, List.of("ab", "bc"),
                    key("a", DOUBLE, "b", RAW, "c", SHORT_STICK), RAW),
            new RecipeSpec("sickle", EQUIPMENT, List.of("  a", " aa", "b  "),
                    key("a", RAW, "b", STICK), RAW),
            new RecipeSpec("butterfly_sword", EQUIPMENT, List.of("  a", "ab ", "c  "),
                    key("a", RAW, "b", DOUBLE, "c", STICK), DOUBLE),
            new RecipeSpec("hook_sword", EQUIPMENT, List.of("  a", "ba ", "c  "),
                    key("a", DOUBLE, "b", RAW, "c", LONG_STICK), DOUBLE),
            new RecipeSpec("katana", EQUIPMENT, List.of("  a", " a ", "b  "),
                    key("a", DOUBLE, "b", STICK), DOUBLE),
            new RecipeSpec("saw_cleaver", EQUIPMENT, List.of("  a", " bc", "dc "),
                    key("a", RAW, "b", LONG_STICK, "c", DOUBLE, "d", SHORT_STICK), DOUBLE),
            new RecipeSpec("saw_cleaver_unfolded", EQUIPMENT, List.of("  a", " bc", "dc "),
                    key("a", RAW, "b", LONG_STICK, "c", DOUBLE, "d", SHORT_STICK), DOUBLE),
            new RecipeSpec("scythe", EQUIPMENT, List.of("aab", " c ", "d  "),
                    key("a", DOUBLE, "b", RAW, "c", LONG_STICK, "d", STICK), DOUBLE),
            new RecipeSpec("twinblade", EQUIPMENT, List.of("  a", " b ", "a  "),
                    key("a", DOUBLE, "b", LONG_STICK), DOUBLE),
            new RecipeSpec("battle_axe", EQUIPMENT, List.of("aba", "aca", " d "),
                    key("a", RAW, "b", DOUBLE, "c", SHORT_STICK, "d", STICK), DOUBLE),
            new RecipeSpec("greatsword", EQUIPMENT, List.of("a", "b", "c"),
                    key("a", RAW, "b", HEAVY, "c", STICK), DOUBLE),
            new RecipeSpec("longsword", EQUIPMENT, List.of("a", "a", "b"),
                    key("a", DOUBLE, "b", LONG_STICK), DOUBLE),
            new RecipeSpec("broadsword", EQUIPMENT, List.of("a", "b", "c"),
                    key("a", DOUBLE, "b", HEAVY, "c", STICK), DOUBLE),
            new RecipeSpec("zweihander", EQUIPMENT, List.of(" a ", "bab", " c "),
                    key("a", DOUBLE, "b", RAW, "c", LONG_STICK), DOUBLE),
            new RecipeSpec("buster_sword", EQUIPMENT, List.of("a", "a", "b"),
                    key("a", HEAVY, "b", STICK), HEAVY),
            new RecipeSpec("hammer", EQUIPMENT, List.of("aaa", "aaa", " b "),
                    key("a", DOUBLE, "b", STICK), DOUBLE),
            new RecipeSpec("greathammer", EQUIPMENT, List.of("aba", "aba", " c "),
                    key("a", HEAVY, "b", DOUBLE, "c", LONG_STICK), HEAVY),
            new RecipeSpec("pistol", EQUIPMENT, List.of("abc", " de"),
                    key("a", DOUBLE, "b", "minecraft:fire_charge", "c", RAW,
                            "d", SHORT_STICK, "e", "minecraft:leather"), RAW),
            new RecipeSpec("scattergun", EQUIPMENT, List.of("ab ", "cdb", " ce"),
                    key("a", DOUBLE, "b", "minecraft:fire_charge", "c", SHORT_STICK,
                            "d", HEAVY, "e", "minecraft:leather"), RAW),
            new RecipeSpec("shotgun", EQUIPMENT, List.of("ab ", "cab", " de"),
                    key("a", DOUBLE, "b", "minecraft:fire_charge", "c", "minecraft:blaze_rod",
                            "d", SHORT_STICK, "e", "minecraft:leather"), DOUBLE),
            new RecipeSpec("shotgun_bladed", EQUIPMENT, List.of("ab ", "cab", "fde"),
                    key("a", DOUBLE, "b", "minecraft:fire_charge", "c", "minecraft:blaze_rod",
                            "d", SHORT_STICK, "e", "minecraft:leather", "f", DOUBLE), DOUBLE),
            new RecipeSpec("slingshot", EQUIPMENT, List.of("aba", " c ", " a "),
                    key("a", SHORT_STICK, "b", "minecraft:string", "c", RAW), RAW));

    private static final Map<String, RecipeSpec> BY_WEAPON = index();

    private static Map<Character, String> key(String... pairs) {
        LinkedHashMap<Character, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i].charAt(0), pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, RecipeSpec> index() {
        LinkedHashMap<String, RecipeSpec> map = new LinkedHashMap<>();
        for (RecipeSpec spec : RECIPES) {
            map.put(spec.weapon(), spec);
        }
        return Collections.unmodifiableMap(map);
    }

    public static RecipeSpec of(String weapon) {
        return BY_WEAPON.get(weapon);
    }
}
