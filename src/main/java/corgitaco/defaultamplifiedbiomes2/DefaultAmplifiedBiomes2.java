package corgitaco.defaultamplifiedbiomes2;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeMaker;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(DefaultAmplifiedBiomes2.MOD_ID)
@Mod.EventBusSubscriber(modid = DefaultAmplifiedBiomes2.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DefaultAmplifiedBiomes2 {
    public static final String MOD_ID = "defaultamplifiedbiomes2";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);

    public DefaultAmplifiedBiomes2() {
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        DABConfig config = DABConfig.getConfig();

        config.entryMap().forEach((biomeRegistryKey, entry) -> {
            RegistryKey<Biome> newKey = config.getNewIDEntryMap().get(biomeRegistryKey);
            Biome biome = BiomeMaker.theVoidBiome();
            biome.setRegistryName(newKey.location());
            BiomeManager.addBiome(entry.getBiomeType(), new BiomeManager.BiomeEntry(newKey, entry.getWeight()));
            event.getRegistry().register(biome);
        });
    }
}
