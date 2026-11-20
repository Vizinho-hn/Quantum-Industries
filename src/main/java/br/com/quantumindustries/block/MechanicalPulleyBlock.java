package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MechanicalPulleyBlock extends AxialTransmissionBlock {
    public MechanicalPulleyBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return simpleCodec(MechanicalPulleyBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MechanicalPulleyBlockEntity(pos, state); }

    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (level instanceof net.minecraft.server.level.ServerLevel && player.mayBuild() && level.mayInteract(player, pos)
                && level.getBlockEntity(pos) instanceof MechanicalPulleyBlockEntity pulley && pulley.inReach(player)) pulley.unlink(true);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
