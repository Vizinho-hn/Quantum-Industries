package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.CopperCableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.InsideBlockEffectApplier;

/** Cable graph endpoints are traversed by the source, never manufacture FE. */
public final class CopperCableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);
    private final boolean insulated;

    public CopperCableBlock(Properties properties, boolean insulated) {
        super(properties);
        this.insulated = insulated;
    }
    public boolean insulated() { return insulated; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(() -> new CopperCableBlock(Properties.of().strength(0.8F).noOcclusion(), insulated));
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperCableBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.COPPER_CABLE_ENTITY.get(),
                (l, p, s, entity) -> entity.tick());
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects) {
        if (insulated || !(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)
                || !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                || server.getGameTime() % 20 != 0) return;
        if (server.getBlockEntity(pos) instanceof CopperCableBlockEntity cable && cable.flow() > 0) {
            living.hurtServer(server, server.damageSources().lightningBolt(), 1.0F);
        }
    }
}
