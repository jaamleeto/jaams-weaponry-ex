package net.jaams.weaponry_ex.registry;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.data.TierDefinition;
import net.jaams.weaponry_ex.data.WeaponSpec;
import net.jaams.weaponry_ex.item.TieredAxeItem;
import net.jaams.weaponry_ex.item.TieredBladedGunItem;
import net.jaams.weaponry_ex.item.TieredGauntletItem;
import net.jaams.weaponry_ex.item.TieredMaterialItem;
import net.jaams.weaponry_ex.item.TieredPickaxeItem;
import net.jaams.weaponry_ex.item.TieredSimpleItem;
import net.jaams.weaponry_ex.item.TieredSwordItem;
import net.jaams.weaponry_ex.loader.TierLoader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TieredItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, JaamsWeaponryExMod.ITEM_NAMESPACE);

    public record TierEntry(
            TierDefinition definition,
            List<RegistryObject<Item>> weapons,
            List<WeaponSpec> specs,
            RegistryObject<Item> heavyIngot,
            RegistryObject<Item> doubleIngot,
            List<RegistryObject<Item>> allItems) {
    }

    public record ItemOwner(TierDefinition tier, WeaponSpec spec) {
    }

    private static final Map<String, TierEntry> TIERS = new LinkedHashMap<>();
    private static Map<Item, ItemOwner> OWNERS = Map.of();
    private static Map<String, TierEntry> BY_REGISTRY_NAME = Map.of();

    private TieredItems() {
    }

    public static void register(IEventBus bus) {
        build();
        ITEMS.register(bus);
        bus.addListener(TieredItems::bindOwners);
    }

    private static void build() {
        resolveMaterialNameCollisions();
        for (TierDefinition tier : TierLoader.getTiers()) {
            if (!tier.enabled) {
                continue;
            }
            Item.Properties ingotProps = props(tier);
            RegistryObject<Item> heavy = null;
            RegistryObject<Item> dbl = null;
            if (tier.hasMaterials()) {
                heavy = ITEMS.register(tier.heavyMaterialName(),
                        () -> new TieredMaterialItem(ingotProps, tier));
                dbl = ITEMS.register(tier.doubleMaterialName(),
                        () -> new TieredMaterialItem(ingotProps, tier));
            }

            List<RegistryObject<Item>> weapons = new ArrayList<>();
            List<WeaponSpec> specs = new ArrayList<>();

            for (WeaponSpec spec : tier.weapons()) {
                registerWeapon(tier, spec, heavy, dbl, weapons, specs);
            }

            List<RegistryObject<Item>> all = new ArrayList<>(weapons);
            if (heavy != null && dbl != null) {
                all.add(heavy);
                all.add(dbl);
            }
            TIERS.put(tier.id, new TierEntry(tier, List.copyOf(weapons), List.copyOf(specs), heavy, dbl,
                    Collections.unmodifiableList(all)));
        }
        int total = TIERS.values().stream().mapToInt(t -> t.allItems().size()).sum();
        BY_REGISTRY_NAME = Collections.unmodifiableMap(indexByRegistryName());
        if (TIERS.isEmpty()) {
            JaamsWeaponryExMod.LOGGER.debug("No tier is enabled, so no item is registered");
            return;
        }
        JaamsWeaponryExMod.LOGGER.info("Registered {} tier(s), {} items total", TIERS.size(), total);
    }

    private static Map<String, TierEntry> indexByRegistryName() {
        Map<String, TierEntry> index = new LinkedHashMap<>();
        for (TierEntry entry : TIERS.values()) {
            TierDefinition tier = entry.definition();
            for (WeaponSpec spec : entry.specs()) {
                index.put(tier.id + "_" + spec.registryName(), entry);
            }
            if (entry.heavyIngot() != null && entry.doubleIngot() != null) {
                index.put(tier.heavyMaterialName(), entry);
                index.put(tier.doubleMaterialName(), entry);
            }
        }
        return index;
    }

    private static void resolveMaterialNameCollisions() {
        Map<String, String> taken = new LinkedHashMap<>();
        for (TierDefinition tier : TierLoader.getTiers()) {
            if (!tier.enabled || !tier.hasMaterials()) {
                continue;
            }
            String heavy = claim(taken, tier.naturalHeavyName(), tier.id);
            String dbl = claim(taken, tier.naturalDoubleName(), tier.id);
            if (!heavy.equals(tier.naturalHeavyName()) || !dbl.equals(tier.naturalDoubleName())) {
                tier.pinMaterialNames(heavy, dbl);
                JaamsWeaponryExMod.LOGGER.warn("Tier '{}': its material '{}' is already used by "
                        + "tier '{}' for the same shape, so its own two items are named '{}' and "
                        + "'{}' instead. Two tiers can share a material; they just cannot share a "
                        + "name for it.", tier.id, tier.materialWord(), taken.get(heavy),
                        heavy, dbl);
            }
        }
    }

    private static String claim(Map<String, String> taken, String natural, String tierId) {
        if (taken.putIfAbsent(natural, tierId) == null) {
            return natural;
        }
        String alternative = natural.contains("_")
                ? natural.substring(0, natural.indexOf('_')) + "_" + tierId
                        + natural.substring(natural.indexOf('_'))
                : tierId + "_" + natural;
        taken.putIfAbsent(alternative, tierId);
        return alternative;
    }

    private static void registerWeapon(TierDefinition tier, WeaponSpec spec,
            RegistryObject<Item> heavy, RegistryObject<Item> dbl,
            List<RegistryObject<Item>> weapons, List<WeaponSpec> specs) {
        String name = tier.id + "_" + spec.registryName();
        int uses = tier.properties.durability + spec.usesOffset();
        Item.Properties props = props(tier);
        int color = tier.getColor();
        int enchant = tier.properties.enchantability + spec.enchantOffset();

        weapons.add(ITEMS.register(name, () -> {
            Ingredient repair = repairIngredient(tier, spec, heavy, dbl);
            return switch (spec.kind()) {
                case SWORD -> new TieredSwordItem(
                        makeTier(uses, tier.properties.speed, tier.properties.attack_damage_bonus + spec.damageBonus(),
                                tier.properties.harvest_level, tier.properties.enchantability, repair),
                        (int) spec.attackModifier(), spec.attackSpeed(), props, color, enchant, tier);
                case AXE -> new TieredAxeItem(
                        makeTier(uses, tier.properties.speed, tier.properties.attack_damage_bonus + spec.damageBonus(),
                                tier.properties.harvest_level, tier.properties.enchantability, repair),
                        spec.attackModifier(), spec.attackSpeed(), props, color, enchant, tier);
                case PICKAXE -> new TieredPickaxeItem(
                        makeTier(uses, tier.properties.speed, tier.properties.attack_damage_bonus + spec.damageBonus(),
                                tier.properties.harvest_level, tier.properties.enchantability, repair),
                        (int) spec.attackModifier(), spec.attackSpeed(), props, color, enchant, tier);
                case SIMPLE -> new TieredSimpleItem(props.durability(uses), color, enchant, tier);
                case BLADED_GUN -> new TieredBladedGunItem(props.durability(uses), color, enchant,
                        tier);
                case GAUNTLET -> new TieredGauntletItem(props.durability(uses),
                        tier.properties.attack_damage_bonus + spec.damageBonus(), color, enchant, tier);
            };
        }));
        specs.add(spec);
    }

    private static void bindOwners(RegisterEvent event) {
        if (!ForgeRegistries.ITEMS.equals(event.getForgeRegistry())) {
            return;
        }
        Map<Item, ItemOwner> owners = new LinkedHashMap<>();
        for (TierEntry entry : TIERS.values()) {
            List<RegistryObject<Item>> items = entry.weapons();
            List<WeaponSpec> specs = entry.specs();
            for (int i = 0; i < items.size() && i < specs.size(); i++) {
                owners.put(items.get(i).get(), new ItemOwner(entry.definition(), specs.get(i)));
            }

            if (entry.heavyIngot() != null && entry.doubleIngot() != null) {
                for (RegistryObject<Item> ingot : List.of(entry.heavyIngot(), entry.doubleIngot())) {
                    owners.put(ingot.get(), new ItemOwner(entry.definition(), null));
                }
            }
        }
        OWNERS = Collections.unmodifiableMap(owners);
    }

    public static ItemOwner owner(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemOwner owner = OWNERS.get(stack.getItem());
        if (owner != null) {
            return owner;
        }
        return byRegistryName(stack);
    }

    private static ItemOwner byRegistryName(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !JaamsWeaponryExMod.ITEM_NAMESPACE.equals(key.getNamespace())) {
            return null;
        }
        TierEntry entry = BY_REGISTRY_NAME.get(key.getPath());
        if (entry == null) {
            return null;
        }
        Item item = stack.getItem();
        for (RegistryObject<Item> candidate : entry.allItems()) {
            if (candidate.get() == item) {
                return new ItemOwner(entry.definition(), specOf(entry, item));
            }
        }
        return null;
    }

    private static WeaponSpec specOf(TierEntry entry, Item item) {
        List<RegistryObject<Item>> weapons = entry.weapons();
        List<WeaponSpec> specs = entry.specs();
        for (int i = 0; i < weapons.size() && i < specs.size(); i++) {
            if (weapons.get(i).get() == item) {
                return specs.get(i);
            }
        }
        return null;
    }

    private static Ingredient repairIngredient(TierDefinition tier, WeaponSpec spec,
            RegistryObject<Item> heavy, RegistryObject<Item> dbl) {
        if (tier.hasRepairItem()) {
            String value = tier.properties.repair_item.trim();
            if (value.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(value.substring(1));
                if (tag != null) {
                    return Ingredient.of(ItemTags.create(tag));
                }
            } else {
                ResourceLocation item = ResourceLocation.tryParse(value);
                if (item != null) {
                    Item repair = ForgeRegistries.ITEMS.getValue(item);
                    if (repair != null) {
                        return Ingredient.of(repair);
                    }
                }
            }
            JaamsWeaponryExMod.LOGGER.warn("Tier '{}': cannot resolve repair_item '{}', "
                    + "falling back to the tier's own ingot", tier.id, tier.properties.repair_item);
        }

        if (heavy == null || dbl == null) {
            return materialFallback(tier);
        }
        if (spec.usesOffset() >= 300) {
            return Ingredient.of(new ItemStack(heavy.get()));
        }
        if (spec.usesOffset() >= 100) {
            return Ingredient.of(new ItemStack(dbl.get()));
        }
        return Ingredient.of(new ItemStack(heavy.get()));
    }

    private static Ingredient materialFallback(TierDefinition tier) {
        String candidate = tier.hasIngotMaterial() ? tier.recipe.ingot_material : tier.recipe.base_material;
        if (candidate != null && !candidate.isBlank() && !candidate.trim().startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(candidate.trim());
            if (id != null) {
                Item resolved = ForgeRegistries.ITEMS.getValue(id);
                if (resolved != null) {
                    return Ingredient.of(resolved);
                }
            }
        }
        return Ingredient.of(Items.IRON_INGOT);
    }

    private static Item.Properties props(TierDefinition tier) {
        Item.Properties properties = tier.properties.fire_resistant ? new Item.Properties().fireResistant()
                : new Item.Properties();
        return properties.rarity(tier.getRarity());
    }

    private static Tier makeTier(int uses, float speed, float attackDamageBonus,
            int level, int enchantmentValue, Ingredient repairIngredient) {
        return new Tier() {
            @Override
            public int getUses() {
                return uses;
            }

            @Override
            public float getSpeed() {
                return speed;
            }

            @Override
            public float getAttackDamageBonus() {
                return attackDamageBonus;
            }

            @Override
            public int getLevel() {
                return level;
            }

            @Override
            public int getEnchantmentValue() {
                return enchantmentValue;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repairIngredient;
            }
        };
    }

    public static Map<String, TierEntry> getTierEntries() {
        return Collections.unmodifiableMap(TIERS);
    }

    public static List<TierEntry> entries() {
        return List.copyOf(TIERS.values());
    }
}
