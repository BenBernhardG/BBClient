package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public abstract class WorldPreset
{
    public static final WorldPreset NORMAL = new WorldPreset("default")
    {
        protected ChunkGenerator generator(RegistryAccess p_194096_, long p_194097_)
        {
            return WorldGenSettings.makeDefaultOverworld(p_194096_, p_194097_);
        }
    };
    private static final WorldPreset FLAT = new WorldPreset("flat")
    {
        protected ChunkGenerator generator(RegistryAccess p_194099_, long p_194100_)
        {
            return new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(p_194099_.registryOrThrow(Registry.BIOME_REGISTRY)));
        }
    };
    public static final WorldPreset LARGE_BIOMES = new WorldPreset("large_biomes")
    {
        protected ChunkGenerator generator(RegistryAccess p_194102_, long p_194103_)
        {
            return WorldGenSettings.makeOverworld(p_194102_, p_194103_, NoiseGeneratorSettings.LARGE_BIOMES);
        }
    };
    public static final WorldPreset AMPLIFIED = new WorldPreset("amplified")
    {
        protected ChunkGenerator generator(RegistryAccess p_194105_, long p_194106_)
        {
            return WorldGenSettings.makeOverworld(p_194105_, p_194106_, NoiseGeneratorSettings.AMPLIFIED);
        }
    };
    private static final WorldPreset SINGLE_BIOME_SURFACE = new WorldPreset("single_biome_surface")
    {
        protected ChunkGenerator generator(RegistryAccess p_194108_, long p_194109_)
        {
            return WorldPreset.fixedBiomeGenerator(p_194108_, p_194109_, NoiseGeneratorSettings.OVERWORLD);
        }
    };
    private static final WorldPreset DEBUG = new WorldPreset("debug_all_block_states")
    {
        protected ChunkGenerator generator(RegistryAccess p_194111_, long p_194112_)
        {
            return new DebugLevelSource(p_194111_.registryOrThrow(Registry.BIOME_REGISTRY));
        }
    };
    protected static final List<WorldPreset> PRESETS = Lists.newArrayList(NORMAL, FLAT, LARGE_BIOMES, AMPLIFIED, SINGLE_BIOME_SURFACE, DEBUG);
    protected static final Map<Optional<WorldPreset>, WorldPreset.PresetEditor> EDITORS = ImmutableMap.of(Optional.of(FLAT), (p_194093_, p_194094_) ->
    {
        ChunkGenerator chunkgenerator = p_194094_.overworld();
        return new CreateFlatWorldScreen(p_194093_, (p_194080_) -> {
            p_194093_.worldGenSettingsComponent.updateSettings(new WorldGenSettings(p_194094_.seed(), p_194094_.generateFeatures(), p_194094_.generateBonusChest(), WorldGenSettings.withOverworld(p_194093_.worldGenSettingsComponent.registryHolder().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), p_194094_.dimensions(), new FlatLevelSource(p_194080_))));
        }, chunkgenerator instanceof FlatLevelSource ? ((FlatLevelSource)chunkgenerator).settings() : FlatLevelGeneratorSettings.getDefault(p_194093_.worldGenSettingsComponent.registryHolder().registryOrThrow(Registry.BIOME_REGISTRY)));
    }, Optional.of(SINGLE_BIOME_SURFACE), (p_194071_, p_194072_) ->
    {
        return new CreateBuffetWorldScreen(p_194071_, p_194071_.worldGenSettingsComponent.registryHolder(), (p_194076_) -> {
            p_194071_.worldGenSettingsComponent.updateSettings(fromBuffetSettings(p_194071_.worldGenSettingsComponent.registryHolder(), p_194072_, SINGLE_BIOME_SURFACE, p_194076_));
        }, parseBuffetSettings(p_194071_.worldGenSettingsComponent.registryHolder(), p_194072_));
    });
    private final Component description;

    static NoiseBasedChunkGenerator fixedBiomeGenerator(RegistryAccess p_194086_, long p_194087_, ResourceKey<NoiseGeneratorSettings> p_194088_)
    {
        return new NoiseBasedChunkGenerator(p_194086_.registryOrThrow(Registry.NOISE_REGISTRY), new FixedBiomeSource(p_194086_.registryOrThrow(Registry.BIOME_REGISTRY).getOrThrow(Biomes.PLAINS)), p_194087_, () ->
        {
            return p_194086_.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getOrThrow(p_194088_);
        });
    }

    WorldPreset(String pDescription)
    {
        this.description = new TranslatableComponent("generator." + pDescription);
    }

    private static WorldGenSettings fromBuffetSettings(RegistryAccess pRegistryAccess, WorldGenSettings pSettings, WorldPreset pPreset, Biome pBiome)
    {
        BiomeSource biomesource = new FixedBiomeSource(pBiome);
        Registry<DimensionType> registry = pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        Registry<NoiseGeneratorSettings> registry1 = pRegistryAccess.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        Supplier<NoiseGeneratorSettings> supplier = () ->
        {
            return registry1.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        };
        return new WorldGenSettings(pSettings.seed(), pSettings.generateFeatures(), pSettings.generateBonusChest(), WorldGenSettings.withOverworld(registry, pSettings.dimensions(), new NoiseBasedChunkGenerator(pRegistryAccess.registryOrThrow(Registry.NOISE_REGISTRY), biomesource, pSettings.seed(), supplier)));
    }

    private static Biome parseBuffetSettings(RegistryAccess pRegistryAccess, WorldGenSettings pSettings)
    {
        return pSettings.overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse(pRegistryAccess.registryOrThrow(Registry.BIOME_REGISTRY).getOrThrow(Biomes.PLAINS));
    }

    public static Optional<WorldPreset> of(WorldGenSettings pSettings)
    {
        ChunkGenerator chunkgenerator = pSettings.overworld();

        if (chunkgenerator instanceof FlatLevelSource)
        {
            return Optional.of(FLAT);
        }
        else
        {
            return chunkgenerator instanceof DebugLevelSource ? Optional.of(DEBUG) : Optional.empty();
        }
    }

    public Component description()
    {
        return this.description;
    }

    public WorldGenSettings create(RegistryAccess.RegistryHolder pRegistryHolder, long pSeed, boolean p_101544_, boolean pGenerateFeatures)
    {
        return new WorldGenSettings(pSeed, p_101544_, pGenerateFeatures, WorldGenSettings.withOverworld(pRegistryHolder.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(pRegistryHolder, pSeed), this.generator(pRegistryHolder, pSeed)));
    }

    protected abstract ChunkGenerator generator(RegistryAccess p_194083_, long p_194084_);

    public static boolean isVisibleByDefault(WorldPreset pPreset)
    {
        return pPreset != DEBUG;
    }

    public interface PresetEditor
    {
        Screen createEditScreen(CreateWorldScreen pCreateWorldScreen, WorldGenSettings pSettings);
    }
}
