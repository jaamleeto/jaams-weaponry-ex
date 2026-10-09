package net.jaams.weaponry_ex.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record WeaponSpec(
        String name,
        Kind kind,
        String tag,
        float damageBonus,
        float attackModifier,
        float attackSpeed,
        int usesOffset,
        int enchantOffset
) {

    public enum Kind {
        SWORD, AXE, PICKAXE, SIMPLE, BLADED_GUN, GAUNTLET
    }

    private static final String BLADED_SUFFIX = "_bladed";

    public String label() {
        StringBuilder out = new StringBuilder();
        for (String part : registryName().split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public String registryName() {
        return name.endsWith(BLADED_SUFFIX)
                ? name.substring(0, name.length() - BLADED_SUFFIX.length())
                : name;
    }

    private static final Set<String> ITEM_ONLY = Set.of("gauntlet");

    public boolean itemOnly() {
        return ITEM_ONLY.contains(name);
    }

    public static final List<WeaponSpec> WEAPONS = List.of(
            new WeaponSpec("gauntlet", Kind.GAUNTLET, "gauntlets", 1.0F, 0, -2.4F, 0, 0),
            new WeaponSpec("dagger", Kind.SWORD, "daggers", 0.0F, 3, -1.8F, 0, 0),
            new WeaponSpec("dagger_reverse", Kind.SWORD, "reverse_daggers", -0.5F, 3, -1.4F, 0, 0),
            new WeaponSpec("knuckle", Kind.SWORD, "knuckles", 0.5F, 3, -1.6F, 0, 0),
            new WeaponSpec("cleaver", Kind.SWORD, "cleavers", 3.0F, 3, -3.2F, 0, 0),
            new WeaponSpec("ring", Kind.SWORD, "rings", 1.0F, 3, -1.8F, 0, 0),
            new WeaponSpec("kama", Kind.SWORD, "kamas", 1.5F, 3, -2.2F, 0, 0),
            new WeaponSpec("claw", Kind.SWORD, "claws", 1.0F, 3, -2.0F, 0, 0),
            new WeaponSpec("spear", Kind.SWORD, "spears", 2.0F, 3, -2.6F, 0, 0),
            new WeaponSpec("machete", Kind.SWORD, "machetes", 2.5F, 3, -2.5F, 100, 0),
            new WeaponSpec("katar", Kind.SWORD, "katars", 3.0F, 3, -2.6F, 100, 0),
            new WeaponSpec("sickle", Kind.SWORD, "sickles", 2.0F, 3, -2.2F, 100, 0),
            new WeaponSpec("butterfly_sword", Kind.SWORD, "butterfly_swords", 4.0F, 3, -2.6F, 100, 0),
            new WeaponSpec("hook_sword", Kind.SWORD, "hook_swords", 4.5F, 3, -2.6F, 100, 0),
            new WeaponSpec("katana", Kind.SWORD, "katanas", 3.5F, 3, -2.2F, 0, 0),
            new WeaponSpec("saw_cleaver", Kind.SWORD, "saw_cleavers", 5.5F, 3, -2.6F, 100, 0),
            new WeaponSpec("saw_cleaver_unfolded", Kind.SWORD, "saw_cleavers_unfolded", 4.5F, 3, -2.8F, 100, 0),
            new WeaponSpec("scythe", Kind.SWORD, "scythes", 6.0F, 3, -3.1F, 100, 0),
            new WeaponSpec("twinblade", Kind.SWORD, "twinblades", 2.0F, 3, -2.2F, 100, 0),
            new WeaponSpec("battle_axe", Kind.AXE, "battle_axes", 10.0F, 1.0F, -3.3F, 100, 0),
            new WeaponSpec("greatsword", Kind.SWORD, "greatswords", 7.0F, 3, -3.2F, 200, 0),
            new WeaponSpec("longsword", Kind.SWORD, "longswords", 5.0F, 3, -2.9F, 200, 0),
            new WeaponSpec("broadsword", Kind.SWORD, "broadswords", 5.0F, 3, -2.8F, 220, 0),
            new WeaponSpec("zweihander", Kind.SWORD, "zweihanders", 5.0F, 3, -3.1F, 220, 0),
            new WeaponSpec("buster_sword", Kind.SWORD, "buster_swords", 9.0F, 3, -3.4F, 300, 0),
            new WeaponSpec("hammer", Kind.PICKAXE, "hammers", 7.0F, 3, -3.4F, 300, 0),
            new WeaponSpec("greathammer", Kind.PICKAXE, "greathammers", 11.0F, 3, -3.6F, 400, 0),
            new WeaponSpec("pistol", Kind.SIMPLE, "pistols", 0.0F, 0, 0.0F, 0, 8),
            new WeaponSpec("scattergun", Kind.SIMPLE, "scatterguns", 2.0F, 0, 0.0F, 0, 8),
            new WeaponSpec("shotgun", Kind.SIMPLE, "shotguns", 5.0F, 0, 0.0F, 50, 8),
            new WeaponSpec("shotgun_bladed", Kind.BLADED_GUN, "shotguns", 6.0F, 0, 0.0F, 100, 8),
            new WeaponSpec("slingshot", Kind.SIMPLE, "slingshots", 1.5F, 0, 0.0F, 0, 8)
    );

    public static final List<String> TAB_ORDER = List.of(
            "dagger", "gauntlet", "knuckle", "cleaver", "ring", "kama", "claw", "machete", "katar",
            "hammer", "battle_axe", "sickle", "longsword", "zweihander", "greatsword",
            "broadsword", "buster_sword", "katana", "butterfly_sword", "hook_sword",
            "scythe", "greathammer", "spear", "twinblade", "saw_cleaver", "pistol",
            "scattergun", "shotgun", "shotgun_bladed", "slingshot");

    public static final List<String> RANGED_WEAPONS = List.of(
            "pistol", "scattergun", "shotgun", "shotgun_bladed", "slingshot");

    public static final Map<String, List<String>> EXTRA_TAGS = Map.of(
            "shotgun_bladed", List.of("misc/bladed_shotgun"));

    public static final Map<String, String> PRESET_ALIASES = Map.of(
            "shotgun_bladed", "shotgun_axe");

    public String preset() {
        return PRESET_ALIASES.getOrDefault(name, name);
    }

    private static final Map<String, WeaponSpec> BY_NAME = WEAPONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(WeaponSpec::name, w -> w));

    public static WeaponSpec byName(String name) {
        return BY_NAME.get(name);
    }

    public static final List<String> MELEE_WEAPONS = WEAPONS.stream()
            .filter(w -> !RANGED_WEAPONS.contains(w.name()))
            .map(WeaponSpec::name)
            .toList();
}
