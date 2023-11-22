package net.minecraft.client.resources;

import java.util.stream.Stream;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class TextureAtlasHolder extends SimplePreparableReloadListener<TextureAtlas.Preparations> implements AutoCloseable
{
    private final TextureAtlas textureAtlas;
    private final String prefix;

    public TextureAtlasHolder(TextureManager pTextureManager, ResourceLocation pLocation, String pPrefix)
    {
        this.prefix = pPrefix;
        this.textureAtlas = new TextureAtlas(pLocation);
        pTextureManager.register(this.textureAtlas.location(), this.textureAtlas);
    }

    protected abstract Stream<ResourceLocation> getResourcesToLoad();

    protected TextureAtlasSprite getSprite(ResourceLocation pLocation)
    {
        return this.textureAtlas.getSprite(this.resolveLocation(pLocation));
    }

    private ResourceLocation resolveLocation(ResourceLocation p_118907_)
    {
        return new ResourceLocation(p_118907_.getNamespace(), this.prefix + "/" + p_118907_.getPath());
    }

    protected TextureAtlas.Preparations prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler)
    {
        pProfiler.startTick();
        pProfiler.push("stitching");
        TextureAtlas.Preparations textureatlas$preparations = this.textureAtlas.prepareToStitch(pResourceManager, this.getResourcesToLoad().map(this::resolveLocation), pProfiler, 0);
        pProfiler.pop();
        pProfiler.endTick();
        return textureatlas$preparations;
    }

    protected void apply(TextureAtlas.Preparations pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler)
    {
        pProfiler.startTick();
        pProfiler.push("upload");
        this.textureAtlas.reload(pObject);
        pProfiler.pop();
        pProfiler.endTick();
    }

    public void close()
    {
        this.textureAtlas.clearTextureData();
    }
}
