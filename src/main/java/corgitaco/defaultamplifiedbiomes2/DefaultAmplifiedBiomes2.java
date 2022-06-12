package corgitaco.defaultamplifiedbiomes2;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
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
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;

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

    public static void dabBiomes(DynamicRegistries.Impl registries) {
        MutableRegistry<Biome> biomeRegistry = registries.registryOrThrow(Registry.BIOME_REGISTRY);
        DABConfig config = DABConfig.getConfig();
        Map<RegistryKey<Biome>, RegistryKey<Biome>> newIDEntryMap = config.getNewIDEntryMap();

        Map<RegistryKey<Biome>, DABConfig.Entry> entries = config.entryMap();


        config.entryMap().forEach((biomeRegistryKey, entry) -> {
            if (!biomeRegistry.containsKey(biomeRegistryKey.location())) {
                throw new IllegalArgumentException("This biome cannot be amplified because it does not exist in the registry!");
            }
        });


        for (Map.Entry<RegistryKey<Biome>, Biome> registryKeyBiomeEntry : new HashSet<>(biomeRegistry.entrySet())) {
            RegistryKey<Biome> biomeRegistryKey = registryKeyBiomeEntry.getKey();
            Biome biome = registryKeyBiomeEntry.getValue();
            if (entries.containsKey(biomeRegistryKey)) {
                DataResult<INBT> result = Biome.DIRECT_CODEC.encodeStart(NBTDynamicOps.INSTANCE, biome);
                CompoundNBT compoundNBT = (CompoundNBT) result.get().orThrow();

                CompoundNBT copy = compoundNBT.copy();

                for (String allkey : copy.getAllKeys()) {
                    INBT inbt = compoundNBT.get(allkey);
                    if (inbt instanceof StringNBT) {
                        String string = compoundNBT.getString(allkey);
                        compoundNBT.remove(allkey);
                        compoundNBT.putString(allkey, string.replace("\"", ""));
                    }
                }
                compoundNBT.remove("scale");
                compoundNBT.putFloat("scale", entries.get(biomeRegistryKey).getNewScale());
                compoundNBT.remove("depth");
                compoundNBT.putFloat("depth", entries.get(biomeRegistryKey).getNewDepth());
                RegistryKey<Biome> newBiomeRegistryKey = newIDEntryMap.get(biomeRegistryKey);

                compoundNBT.remove("forge:registry_name");
                compoundNBT.putString("forge:registry_name", newBiomeRegistryKey.location().toString());

                DataResult<Pair<Biome, INBT>> dataResult = Biome.DIRECT_CODEC.decode(NBTDynamicOps.INSTANCE, compoundNBT);
                biomeRegistry.registerOrOverride(OptionalInt.empty(), newBiomeRegistryKey, dataResult.result().orElseThrow(() -> {
                    return new RuntimeException();
                }).getFirst(), Lifecycle.experimental());
            }
        }
    }
}
