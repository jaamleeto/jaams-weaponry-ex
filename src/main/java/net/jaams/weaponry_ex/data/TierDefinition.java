package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.Rarity;

public class TierDefinition {

    public String id;

    public String display_name;

    public transient boolean standalone;

    public transient String sourceWeapon;

    public String weapon;

    public Map<String, String> translations = new LinkedHashMap<>();

    public TooltipSection tooltip = new TooltipSection();

    public Map<String, Map<String, String>> tooltip_translations = new LinkedHashMap<>();

    public Map<String, Map<String, String>> name_overrides = new LinkedHashMap<>();

    public List<WeaponSpec> weapons() {
        List<WeaponSpec> selected = new ArrayList<>();
        if (standalone) {
            return pinSingleWeapon(selected);
        }
        boolean restricted = !tier.only_items.isEmpty() && !ItemFilters.isWildcarded(tier.only_items);
        String ranged = getRangedTiered();
        for (WeaponSpec spec : WeaponSpec.WEAPONS) {
            if (spec.itemOnly()) {
                continue;
            }
            if (restricted) {
                if (!ItemFilters.matchesName(tier.only_items, spec.name())) {
                    continue;
                }
            } else if (WeaponSpec.RANGED_WEAPONS.contains(spec.name()) && !ranged.equals(spec.name())) {
                continue;
            }
            if (ItemFilters.matchesName(tier.exclude_items, spec.name())) {
                continue;
            }
            selected.add(spec);
        }
        return selected;
    }

    public boolean hasMaterials() {
        return !standalone;
    }

    private List<WeaponSpec> pinSingleWeapon(List<WeaponSpec> selected) {
        WeaponSpec spec = sourceWeapon == null ? null : WeaponSpec.byName(sourceWeapon);
        if (spec == null || ItemFilters.matchesName(tier.exclude_items, spec.name())) {
            return selected;
        }
        selected.add(spec);
        return selected;
    }

    public void pinWeapon(WeaponSpec spec) {
        standalone = true;
        sourceWeapon = spec == null ? null : spec.name();
        if (spec != null) {
            tier.only_items = new ArrayList<>(List.of(spec.name()));
        }
    }

    public String overriddenName(WeaponSpec weapon, String code) {
        if (weapon == null || name_overrides == null || name_overrides.isEmpty()) {
            return null;
        }
        Map<String, String> names = name_overrides.get(weapon.registryName());
        if (names == null) {
            names = name_overrides.get(weapon.name());
        }
        if (names == null || names.isEmpty()) {
            return null;
        }
        String exact = names.get(code);
        if (exact != null && !exact.isBlank()) {
            return exact.trim();
        }
        String fallback = names.get("en_us");
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    public Set<String> nameOverrideCodes(WeaponSpec weapon) {
        if (weapon == null || name_overrides == null) {
            return Set.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        Map<String, String> names = name_overrides.get(weapon.registryName());
        if (names != null) {
            codes.addAll(names.keySet());
        }
        Map<String, String> alt = name_overrides.get(weapon.name());
        if (alt != null) {
            codes.addAll(alt.keySet());
        }
        return codes;
    }

    public TierProperties properties = new TierProperties();

    public TierRecipe recipe = new TierRecipe();

    public TierSection tier = new TierSection();

    public TagSection tags = new TagSection();

    public OnHit on_hit = new OnHit();

    public boolean enabled = true;

    public boolean example = false;

    public Rarity getRarity() {
        return properties.getRarity();
    }

    public int getColor() {
        return properties.getColor();
    }

    public TierRecipe.RecipeType getRecipeType() {
        return recipe.getRecipeType();
    }

    public TierSection.MaterialType getMaterialType() {
        return tier.getMaterialType();
    }

    public boolean hasBaseMaterial() {
        return recipe.hasBaseMaterial();
    }

    public boolean hasIngotMaterial() {
        return recipe.hasIngotMaterial();
    }

    public boolean hasBaseTier() {
        return tier.hasBaseTier();
    }

    public boolean hasSmithingTemplate() {
        return recipe.hasSmithingTemplate();
    }

    public String smithingTable() {
        return recipe.smithingTable();
    }

    public boolean hasCustomSmithingTable() {
        return recipe.hasCustomSmithingTable();
    }

    public boolean hasRepairItem() {
        return properties.hasRepairItem();
    }

    public String getRangedTiered() {
        return tier.getRangedTiered();
    }

    public String materialWord() {
        String source = hasIngotMaterial() ? recipe.ingot_material : recipe.base_material;
        if (source == null) {
            return id;
        }
        String value = source.trim();
        if (value.isEmpty() || value.startsWith("#")) {
            return id;
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(colon + 1);
        }
        value = value.trim().toLowerCase(Locale.ROOT);

        return value.isEmpty() || !value.matches("[a-z0-9_.-]+") ? id : value;
    }

    public boolean namedAfterMaterial() {
        return getMaterialType() != TierSection.MaterialType.INGOT;
    }

    public String stem() {
        return namedAfterMaterial() ? materialWord() : id;
    }

    public String heavyMaterialName() {
        String pinned = tier.pinnedHeavyName();
        return pinned != null ? pinned : getMaterialType().heavyItemName(stem());
    }

    public String doubleMaterialName() {
        String pinned = tier.pinnedDoubleName();
        return pinned != null ? pinned : getMaterialType().doubleItemName(stem());
    }

    public String naturalHeavyName() {
        return getMaterialType().heavyItemName(stem());
    }

    public String naturalDoubleName() {
        return getMaterialType().doubleItemName(stem());
    }

    public void pinMaterialNames(String heavy, String doubleItem) {
        tier.pinMaterialNames(heavy, doubleItem);
    }

    public boolean collisionFallback() {
        return tier.collisionFallback();
    }

    public Conditions conditions = new Conditions();
}
