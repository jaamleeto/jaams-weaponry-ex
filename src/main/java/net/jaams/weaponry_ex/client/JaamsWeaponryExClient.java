package net.jaams.weaponry_ex.client;

import net.jaams.weaponry_ex.JaamsWeaponryExMod;
import net.jaams.weaponry_ex.pack.PackFactory;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JaamsWeaponryExMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class JaamsWeaponryExClient {

    private JaamsWeaponryExClient() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(consumer -> accept(consumer, PackFactory.clientPack()));
        } else if (event.getPackType() == PackType.SERVER_DATA) {
            event.addRepositorySource(consumer -> accept(consumer, PackFactory.serverPack()));
        }
    }

    private static void accept(java.util.function.Consumer<Pack> consumer, Pack pack) {
        if (pack != null) {
            consumer.accept(pack);
        }
    }
}
