package net.jaams.weaponry_ex.loader;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExampleTiers {

    private static final String EXAMPLE_TIER_FILE = "example_tier.json";

    private static final String EXAMPLE_ITEM_FILE = "example_item.json";

    private ExampleTiers() {
    }

    public static void install() {
        try {
            Path dir = TierLoader.configCandidates().get(0);
            Files.createDirectories(dir);
            write(dir.resolve(EXAMPLE_TIER_FILE), exampleTierJson(),
                    "an example tier definition with every option; it is disabled, so set "
                            + "\"enabled\" to true to use it");
            Path itemDir = TierLoader.configCandidates("jaams/weaponry_ex/item").get(0);
            Files.createDirectories(itemDir);
            write(itemDir.resolve(EXAMPLE_ITEM_FILE), exampleItemJson(),
                    "an example item definition with every option; it is disabled, so set "
                            + "\"enabled\" to true to use it");
        } catch (Exception | LinkageError e) {
            JaamsWeaponryExMod.LOGGER.warn(
                    "Could not create the tier config folder '{}'; tiers can still be shipped "
                            + "inside a mod jar.", e.toString());
        }
    }

    private static void write(Path file, String json, String what) throws Exception {
        if (Files.exists(file)) {
            return;
        }
        Files.writeString(file, json, StandardCharsets.UTF_8);
        JaamsWeaponryExMod.LOGGER.debug("Wrote {} to {}.", what, file);
    }

    private static String exampleItemJson() {
        return """
                {
                    "id": "example_item",
                    "display_name": "Example Prism Edge",

                    "weapon": "zweihander",

                    "conditions": {
                        "mods": [
                            { "modid": "minecraft", "required": true }
                        ]
                    },

                    "translations": {
                        "en_us": "Example Prism Edge",
                        "es_es": "Ejemplo Filo de Prisma",
                        "ru_ru": "Пример Призматический Клинок"
                    },

                    "properties": {
                        "color": "#B47BFF",
                        "durability": 1800,
                        "speed": 7.5,
                        "attack_damage_bonus": 2.0,
                        "harvest_level": 3,
                        "enchantability": 16,
                        "fire_resistant": true,
                        "repair_item": "minecraft:amethyst_shard",
                        "rarity": "epic"
                    },

                    "recipe": {
                        "recipe_type": "smithing",
                        "base_material": "minecraft:amethyst_shard",
                        "smithing_template": "minecraft:netherite_upgrade_smithing_template",
                        "smithing_table": "minecraft:smithing_table",
                        "ingot_material": "minecraft:iron_ingot"
                    },

                    "tier": {
                        "base_tier": "diamond",
                        "material_type": "ingot",
                        "tab_insert_after": "jaams_weaponry:double_netherite_ingot",
                        "base_tags": true,

                        "only_items": [
                            "zweihander"
                        ],
                        "exclude_items": [
                            "zweihander"
                        ]
                    },

                    "tags": {
                        "tier": [
                            "jaams_weaponry:trait/decapitation"
                        ],
                        "items": {
                            "zweihander": [
                                "jaams_weaponry:trait/heavy_handed",
                                "minecraft:swords"
                            ]
                        }
                    },

                    "tooltip": {
                        "lines": [
                            "Forged out of %material%.",
                            "&7Item &f%id%",
                            "jaams_weaponry_ex.tooltip.example.prism"
                        ],
                        "exclude_items": [
                            "zweihander"
                        ]
                    },

                    "on_hit": {
                        "fire": {
                            "enabled": true,
                            "chance": 1.0,
                            "ticks": 100,
                            "show_tooltip": true,
                            "only_items": [
                                "zweihander"
                            ],
                            "exclude_items": [
                                "zweihander"
                            ]
                        },
                        "status": [
                            {
                                "enabled": true,
                                "chance": 1.0,
                                "effect": "minecraft:poison",
                                "ticks": 80,
                                "amplifier": 0,
                                "particles": true,
                                "icon": true,
                                "show_tooltip": true
                            }
                        ],
                        "lightning": {
                            "enabled": true,
                            "chance": 0.25,
                            "show_tooltip": true
                        },
                        "explosion": {
                            "enabled": false,
                            "power": 3.0,
                            "breaks_blocks": false,
                            "chance": 1.0,
                            "show_tooltip": false
                        },
                        "lifesteal": {
                            "enabled": true,
                            "amount": 2.0,
                            "chance": 0.5,
                            "show_tooltip": true
                        },
                        "bonus_damage": {
                            "enabled": true,
                            "amount": 25.0,
                            "mode": "percent_max",
                            "chance": 1.0,
                            "show_tooltip": true,
                            "targets": [
                                "minecraft:skeleton",
                                "#minecraft:skeletons"
                            ]
                        },
                        "knockback": {
                            "enabled": true,
                            "power": 0.8,
                            "chance": 1.0,
                            "show_tooltip": true,
                            "at_max_strength": true,
                            "below_max_strength": false
                        },
                        "launch": {
                            "enabled": false,
                            "power": 1.2,
                            "chance": 1.0,
                            "show_tooltip": false
                        },
                        "summon": {
                            "enabled": false,
                            "entity": "minecraft:zombie",
                            "count": 2,
                            "spread": 2.0,
                            "chance": 1.0,
                            "show_tooltip": false
                        },
                        "chain": {
                            "enabled": true,
                            "amount": 2.0,
                            "radius": 5.0,
                            "max_targets": 3,
                            "chance": 0.4,
                            "show_tooltip": true,
                            "targets": [
                                "#minecraft:skeletons",
                                "#minecraft:zombies"
                            ]
                        },
                        "spread": {
                            "enabled": true,
                            "effect": "minecraft:poison",
                            "ticks": 80,
                            "amplifier": 0,
                            "particles": true,
                            "icon": true,
                            "radius": 4.0,
                            "max_targets": 6,
                            "chance": 0.35,
                            "show_tooltip": true,
                            "targets": [
                                "*"
                            ]
                        },
                        "particle": {
                            "enabled": true,
                            "particle": "minecraft:electric_spark",
                            "count": 16,
                            "radius": 0.6,
                            "speed": 0.05,
                            "chance": 0.5,
                            "show_tooltip": false
                        }
                    },

                    "enabled": false,
                    "example": true
                }
                """;
    }

    private static String exampleTierJson() {
        return """
                {
                    "id": "example_tier",
                    "display_name": "Example",

                    "conditions": {
                        "mods": [
                            { "modid": "minecraft", "required": true }
                        ],
                        "items": [
                            { "item": "minecraft:diamond" }
                        ],
                        "tags": [
                            { "tag": "forge:gems/diamond" }
                        ],
                        "all": [
                            { "mods": [{ "modid": "minecraft" }] }
                        ],
                        "any": [
                            { "mods": [{ "modid": "epicfight" }] },
                            { "mods": [{ "modid": "jei" }] }
                        ]
                    },

                    "translations": {
                        "en_us": "Example",
                        "es_es": "Ejemplo",
                        "ru_ru": "Пример"
                    },

                    "name_overrides": {
                        "dagger": {
                            "en_us": "Example Dagger",
                            "es_es": "Daga de Ejemplo"
                        }
                    },

                    "properties": {
                        "color": "#B4B4DC",
                        "durability": 1800,
                        "speed": 7.5,
                        "attack_damage_bonus": 2.0,
                        "harvest_level": 3,
                        "enchantability": 16,
                        "fire_resistant": true,
                        "repair_item": "minecraft:iron_ingot",
                        "rarity": "rare"
                    },

                    "recipe": {
                        "recipe_type": "smithing",
                        "base_material": "minecraft:iron_ingot",
                        "smithing_template": "minecraft:netherite_upgrade_smithing_template",
                        "smithing_table": "minecraft:smithing_table",
                        "ingot_material": "minecraft:iron_ingot"
                    },

                    "tier": {
                        "base_tier": "diamond",
                        "material_type": "ingot",
                        "tab_insert_after": "jaams_weaponry:double_diamond_ingot",
                        "base_tags": true,
                        "only_items": [
                            "*"
                        ],
                        "exclude_items": [
                            "ring",
                            "pistol"
                        ]
                    },

                    "tags": {
                        "tier": [
                            "jaams_weaponry:trait/heavy_handed"
                        ],
                        "materials": [
                            "jaams_weaponry:ingredient/example"
                        ],
                        "items": {
                            "zweihander": [
                                "jaams_weaponry:trait/decapitation",
                                "minecraft:swords"
                            ]
                        },
                        "tier_tag": {
                            "enabled": true,
                            "exclude_items": [
                                "ring"
                            ],
                            "include_items": [
                                "minecraft:netherite_sword"
                            ]
                        }
                    },

                    "tooltip": {
                        "lines": [
                            "Forged out of %material%.",
                            "&7Tier &f%tier% &7(&8%id%&7)",
                            "jaams_weaponry_ex.tooltip.example.heat"
                        ],
                        "exclude_items": [
                            "buster_sword"
                        ]
                    },

                    "on_hit": {
                        "fire": {
                            "enabled": true,
                            "chance": 1.0,
                            "ticks": 100,
                            "show_tooltip": true,
                            "only_items": [
                                "greatsword",
                                "zweihander",
                                "broadsword",
                                "katana"
                            ]
                        },
                        "status": [
                            {
                                "enabled": true,
                                "chance": 1.0,
                                "effect": "minecraft:poison",
                                "ticks": 80,
                                "amplifier": 0,
                                "particles": true,
                                "icon": true,
                                "show_tooltip": true,
                                "exclude_items": [
                                    "dagger",
                                    "dagger_reverse"
                                ]
                            }
                        ],
                        "lightning": {
                            "enabled": true,
                            "chance": 0.25,
                            "show_tooltip": true
                        },
                        "explosion": {
                            "enabled": false,
                            "power": 3.0,
                            "breaks_blocks": false,
                            "chance": 1.0
                        },
                        "lifesteal": {
                            "enabled": true,
                            "amount": 2.0,
                            "chance": 0.5,
                            "show_tooltip": true
                        },
                        "bonus_damage": {
                            "enabled": true,
                            "amount": 3.0,
                            "mode": "flat",
                            "chance": 1.0,
                            "show_tooltip": true,
                            "targets": [
                                "minecraft:skeleton",
                                "#minecraft:skeletons"
                            ]
                        },
                        "knockback": {
                            "enabled": true,
                            "power": 0.8,
                            "chance": 1.0,
                            "show_tooltip": true,
                            "at_max_strength": true,
                            "below_max_strength": false
                        },
                        "launch": {
                            "enabled": false,
                            "power": 1.2,
                            "chance": 1.0,
                            "show_tooltip": false
                        },
                        "summon": {
                            "enabled": false,
                            "entity": "minecraft:zombie",
                            "count": 2,
                            "spread": 2.0,
                            "chance": 1.0,
                            "show_tooltip": false
                        },
                        "chain": {
                            "enabled": true,
                            "amount": 2.0,
                            "radius": 5.0,
                            "max_targets": 3,
                            "chance": 0.4,
                            "show_tooltip": true,
                            "targets": [
                                "#minecraft:skeletons",
                                "#minecraft:zombies"
                            ]
                        },
                        "spread": {
                            "enabled": true,
                            "effect": "minecraft:poison",
                            "ticks": 80,
                            "amplifier": 0,
                            "particles": true,
                            "icon": true,
                            "radius": 4.0,
                            "max_targets": 6,
                            "chance": 0.35,
                            "show_tooltip": true,
                            "targets": [
                                "*"
                            ]
                        },
                        "particle": {
                            "enabled": true,
                            "particle": "minecraft:electric_spark",
                            "count": 16,
                            "radius": 0.6,
                            "speed": 0.05,
                            "chance": 0.5,
                            "show_tooltip": false
                        }
                    },

                    "enabled": false,
                    "example": true
                }
                """;
    }
}
