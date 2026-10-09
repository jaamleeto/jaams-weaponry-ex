package net.jaams.weaponry_ex;

import net.jaams.weaponry_ex.diag.TierDiagnostics;
import net.jaams.weaponry_ex.data.TierHitEvents;
import net.jaams.weaponry_ex.loader.ExampleTiers;
import net.jaams.weaponry_ex.loader.TierLoader;
import net.jaams.weaponry_ex.recipe.SmithingTables;
import net.jaams.weaponry_ex.registry.ModRecipeSerializers;
import net.jaams.weaponry_ex.registry.TieredItems;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(JaamsWeaponryExMod.MODID)
public class JaamsWeaponryExMod {

    public static final String MODID = "jaams_weaponry_ex";

    public static final String ITEM_NAMESPACE = MODID;

    public static final String BASE_NAMESPACE = "jaams_weaponry";

    public static final String MOD_NAME = "Jaam's Weaponry Extended";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public JaamsWeaponryExMod(FMLJavaModLoadingContext context) {
        ExampleTiers.install();
        TierDiagnostics.set(TierLoader.load().reports());
        SmithingTables.collect(TierLoader.getTiers());
        ModRecipeSerializers.register(context.getModEventBus());
        TieredItems.register(context.getModEventBus());
        TierHitEvents.register();
    }
}
