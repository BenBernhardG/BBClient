package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<ClientGamePacketListener>
{
    private final int killerId;
    private final int duration;

    public ClientboundPlayerCombatEndPacket(CombatTracker pBuffer)
    {
        this(pBuffer.getKillerId(), pBuffer.getCombatDuration());
    }

    public ClientboundPlayerCombatEndPacket(int pKillerId, int pDuration)
    {
        this.killerId = pKillerId;
        this.duration = pDuration;
    }

    public ClientboundPlayerCombatEndPacket(FriendlyByteBuf pBuffer)
    {
        this.duration = pBuffer.readVarInt();
        this.killerId = pBuffer.readInt();
    }

    public void write(FriendlyByteBuf pBuffer)
    {
        pBuffer.writeVarInt(this.duration);
        pBuffer.writeInt(this.killerId);
    }

    public void handle(ClientGamePacketListener pHandler)
    {
        pHandler.handlePlayerCombatEnd(this);
    }
}
