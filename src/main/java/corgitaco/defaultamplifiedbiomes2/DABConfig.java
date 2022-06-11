package corgitaco.defaultamplifiedbiomes2;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class DABConfig {


    private static final Supplier<Map<Biome.Category, BiomeManager.BiomeType>> TYPE_MAP = () -> {
        Map<Biome.Category, BiomeManager.BiomeType> categoryBiomeTypeMap = new IdentityHashMap<>();
        categoryBiomeTypeMap.put(Biome.Category.PLAINS, BiomeManager.BiomeType.WARM);
        categoryBiomeTypeMap.put(Biome.Category.TAIGA, BiomeManager.BiomeType.COOL);
        categoryBiomeTypeMap.put(Biome.Category.JUNGLE, BiomeManager.BiomeType.DESERT);
        categoryBiomeTypeMap.put(Biome.Category.MESA, BiomeManager.BiomeType.DESERT);
        categoryBiomeTypeMap.put(Biome.Category.SAVANNA, BiomeManager.BiomeType.DESERT);
        categoryBiomeTypeMap.put(Biome.Category.ICY, BiomeManager.BiomeType.ICY);
        categoryBiomeTypeMap.put(Biome.Category.FOREST, BiomeManager.BiomeType.WARM);
        categoryBiomeTypeMap.put(Biome.Category.DESERT, BiomeManager.BiomeType.DESERT);
        return categoryBiomeTypeMap;
    };

    private static DABConfig INSTANCE;


    private static final Supplier<DABConfig> DEFAULT = () -> new DABConfig(Util.make(new IdentityHashMap<>(), map -> {
        for (Map.Entry<RegistryKey<Biome>, Biome> registryKeyBiomeEntry : WorldGenRegistries.BIOME.entrySet()) {
            Biome biome = registryKeyBiomeEntry.getValue();
            RegistryKey<Biome> biomeRegistryKey = registryKeyBiomeEntry.getKey();
            Map<Biome.Category, BiomeManager.BiomeType> categoryBiomeTypeMap = TYPE_MAP.get();
            Biome.Category biomeCategory = biome.getBiomeCategory();
            if (categoryBiomeTypeMap.containsKey(biomeCategory)) {
                map.put(biomeRegistryKey, new Entry(2.5F, 1.5F, 2, categoryBiomeTypeMap.get(biomeCategory).name().toUpperCase(Locale.ROOT)));
            }
        }
    }));

    public static final Codec<DABConfig> CODEC = RecordCodecBuilder.create(builder -> builder.group(Codec.unboundedMap(codec(Registry.BIOME_REGISTRY), Entry.CODEC).fieldOf("amplifier").forGetter(dabConfig -> dabConfig.entryMap)).apply(builder, DABConfig::new));


    private final Map<RegistryKey<Biome>, Entry> entryMap = new IdentityHashMap<>();
    private final Map<RegistryKey<Biome>, RegistryKey<Biome>> newIDEntryMap = new IdentityHashMap<>();

    public DABConfig(Map<RegistryKey<Biome>, Entry> entries) {
        this.entryMap.putAll(entries);
        entries.forEach((biomeRegistryKey, entry) -> {
            ResourceLocation location = biomeRegistryKey.location();
            RegistryKey<Biome> newKey = RegistryKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(DefaultAmplifiedBiomes2.MOD_ID, location.getNamespace() + "/amplified_" + location.getPath()));
            newIDEntryMap.put(biomeRegistryKey, newKey);
        });
    }

    public Map<RegistryKey<Biome>, Entry> entryMap() {
        return entryMap;
    }

    public Map<RegistryKey<Biome>, RegistryKey<Biome>> getNewIDEntryMap() {
        return newIDEntryMap;
    }

    public static DABConfig getConfig() {
        return getConfig(false, false);
    }

    public static DABConfig getConfig(boolean serialize, boolean recreate) {
        if (INSTANCE == null || serialize || recreate) {
            INSTANCE = readConfig(recreate);
        }

        return INSTANCE;
    }

    private static DABConfig readConfig(boolean recreate) {
        final Path path = DefaultAmplifiedBiomes2.CONFIG_PATH.resolve("amplifier.json");

        if (!path.toFile().exists() || recreate) {


            JsonElement jsonElement = CODEC.encodeStart(JsonOps.INSTANCE, DEFAULT.get()).result().orElseThrow(RuntimeException::new);

            try {
                Files.createDirectories(path.getParent());
                Files.write(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonElement).getBytes());
            } catch (IOException e) {
                DefaultAmplifiedBiomes2.LOGGER.error(e.toString());
            }
        }
        DefaultAmplifiedBiomes2.LOGGER.info(String.format("\"%s\" was read.", path.toString()));

        try {
            DataResult<Pair<DABConfig, JsonElement>> decode = CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(new FileReader(path.toFile())));
            if (decode.error().isPresent()) {
                throw new IllegalArgumentException(String.format("Config loading failed for: %s\nReason: %s", path.toString(), decode.error().get().message()));
            }

            return decode.result().orElseThrow(RuntimeException::new).getFirst();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(String.format("Config loading failed for: %s\nReason: %s", path.toString(), e));
        }
    }


    public static <T> Codec<RegistryKey<T>> codec(RegistryKey<? extends Registry<T>> registryKey) {
        return ResourceLocation.CODEC.xmap((resourceLocation) -> RegistryKey.create(registryKey, resourceLocation), RegistryKey::location);
    }

    public static class Entry {

        public static Codec<Entry> CODEC = RecordCodecBuilder.create(builder ->
                builder.group(Codec.FLOAT.fieldOf("new_scale").forGetter(entry -> entry.newScale),
                        Codec.FLOAT.fieldOf("new_depth").forGetter(entry -> entry.newDepth),
                        Codec.INT.fieldOf("weight").forGetter(entry -> entry.weight),
                        Codec.STRING.fieldOf("biome_type").forGetter(entry -> entry.biomeType.toString().toUpperCase(Locale.ROOT))
                ).apply(builder, Entry::new)
        );

        private final float newScale;
        private final float newDepth;
        private final int weight;
        private final BiomeManager.BiomeType biomeType;

        public Entry(float newScale, float newDepth, int weight, String biomeType) {
            this.newScale = newScale;
            this.newDepth = newDepth;
            this.weight = weight;
            this.biomeType = BiomeManager.BiomeType.valueOf(biomeType.toUpperCase(Locale.ROOT));
        }

        public float getNewScale() {
            return newScale;
        }

        public float getNewDepth() {
            return newDepth;
        }

        public int getWeight() {
            return weight;
        }

        public BiomeManager.BiomeType getBiomeType() {
            return biomeType;
        }
    }
}
