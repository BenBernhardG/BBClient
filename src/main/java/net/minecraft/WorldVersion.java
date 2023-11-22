package net.minecraft;

import com.mojang.bridge.game.GameVersion;
import net.minecraft.world.level.storage.DataVersion;

public interface WorldVersion extends GameVersion
{
    @Deprecated

default int getWorldVersion()
    {
        return this.getDataVersion().getVersion();
    }

    @Deprecated

default String getSeriesId()
    {
        return this.getDataVersion().getSeries();
    }

    DataVersion getDataVersion();
}
