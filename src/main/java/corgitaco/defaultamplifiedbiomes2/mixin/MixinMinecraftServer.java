package corgitaco.defaultamplifiedbiomes2.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import corgitaco.defaultamplifiedbiomes2.DABConfig;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.StringNBT;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {


    @Inject(method = "<init>", at = @At("RETURN"))
    private void dab_dynamicallyAmplifyBiomes(Thread p_i232576_1_, DynamicRegistries.Impl registries, SaveFormat.LevelSave p_i232576_3_, IServerConfiguration p_i232576_4_, ResourcePackList p_i232576_5_, Proxy p_i232576_6_, DataFixer p_i232576_7_, DataPackRegistries p_i232576_8_, MinecraftSessionService p_i232576_9_, GameProfileRepository p_i232576_10_, PlayerProfileCache p_i232576_11_, IChunkStatusListenerFactory p_i232576_12_, CallbackInfo ci) {
        MutableRegistry<Biome> biomeRegistry = registries.registryOrThrow(Registry.BIOME_REGISTRY);
        DABConfig config = DABConfig.getConfig();
        Map<RegistryKey<Biome>, RegistryKey<Biome>> newIDEntryMap = config.getNewIDEntryMap();

        Map<RegistryKey<Biome>, DABConfig.Entry> entries = config.entryMap();
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

//        config.entryMap().forEach((biomeRegistryKey, entry) -> {
//            if(!biomeRegistry.containsKey(biomeRegistryKey.location())) {
//                throw new IllegalArgumentException("This biome cannot be amplified because it does not exist in the registry!");
//            }
//        });
    }


    @Inject(method = "halt", at = @At("HEAD"))
    private void removeEntries(boolean waitForServer, CallbackInfo ci) {
//        DABConfig config = DABConfig.getConfig();
//        config.entryMap().forEach((biomeRegistryKey, entry) -> {
//            BiomeManager.removeBiome(entry.getBiomeType(), new BiomeManager.BiomeEntry(config.getNewIDEntryMap().get(biomeRegistryKey), entry.getWeight()));
//        });
    }
}
