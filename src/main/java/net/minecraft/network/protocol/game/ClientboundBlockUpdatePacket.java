package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClientboundBlockUpdatePacket implements Packet<ClientGamePacketListener>
{
    private final BlockPos pos;
    private final BlockState blockState;

    public ClientboundBlockUpdatePacket(BlockPos pPos, BlockState pBlockState)
    {
        this.pos = pPos;
        this.blockState = pBlockState;
    }

    public ClientboundBlockUpdatePacket(BlockGetter pPos, BlockPos pBlockState)
    {
        this(pBlockState, pPos.getBlockState(pBlockState));
    }

    public ClientboundBlockUpdatePacket(FriendlyByteBuf pBuffer)
    {
        this.pos = pBuffer.readBlockPos();
        this.blockState = Block.BLOCK_STATE_REGISTRY.byId(pBuffer.readVarInt());
    }

    public void write(FriendlyByteBuf pBuffer)
    {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeVarInt(Block.getId(this.blockState));
    }

    public void handle(ClientGamePacketListener pHandler)
    {
        pHandler.handleBlockUpdate(this);
    }

    public BlockState getBlockState()
    {
        return this.blockState;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }
}
