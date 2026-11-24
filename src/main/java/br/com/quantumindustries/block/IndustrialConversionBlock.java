package br.com.quantumindustries.block;

import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public final class IndustrialConversionBlock extends BaseEntityBlock {
    public enum Kind {
        GENERATOR("electromagnetic_generator", "electromagnetic_induction"),
        MOTOR("electric_motor", "electromechanical_forming"),
        BOILER("steam_boiler", "fluid_hydrostatics"),
        STEAM_ENGINE("steam_engine", "fluid_hydrostatics"),
        THERMOELECTRIC("thermoelectric_generator", "electric_alloy_smelting");
        public final String id, research;
        Kind(String id, String research) { this.id=id; this.research=research; }
        public boolean steam() { return this == BOILER || this == STEAM_ENGINE; }
    }
    private final Kind kind;
    public IndustrialConversionBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    public Kind kind() { return kind; }
    public static Direction facing(BlockState state) { return state.getValue(BlockStateProperties.HORIZONTAL_FACING); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(() -> new IndustrialConversionBlock(Properties.of().strength(3).noOcclusion(), kind));
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case BOILER -> new SteamBoilerBlockEntity(pos,state);
            case THERMOELECTRIC -> new ThermoelectricGeneratorBlockEntity(pos,state);
            default -> new RotaryConversionBlockEntity(pos,state);
        };
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return switch (kind) {
            case BOILER -> createTickerHelper(type, ModBlocks.STEAM_BOILER_ENTITY.get(), (l,p,s,b)->b.tick());
            case THERMOELECTRIC -> createTickerHelper(type, ModBlocks.THERMOELECTRIC_GENERATOR_ENTITY.get(), (l,p,s,b)->b.tick());
            default -> createTickerHelper(type, ModBlocks.ROTARY_CONVERSION_ENTITY.get(), (l,p,s,b)->b.tick());
        };
    }
    private static boolean bind(BlockEntity be, Player player) {
        if (be instanceof RotaryConversionBlockEntity rotor) return rotor.bind(player);
        if (be instanceof SteamBoilerBlockEntity boiler) return boiler.bind(player);
        if (be instanceof ThermoelectricGeneratorBlockEntity thermal) return thermal.bind(player);
        return false;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (placer instanceof Player player) bind(level.getBlockEntity(pos),player);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind != Kind.BOILER) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer operator)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SteamBoilerBlockEntity boiler) || !boiler.bind(operator))
            return InteractionResult.SUCCESS;
        if (boiler.addFuel(stack)) return InteractionResult.SUCCESS;
        if (hit.getDirection() != facing(state) && FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection(),null))
            return InteractionResult.SUCCESS;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer operator) {
            var be=level.getBlockEntity(pos);
            if (bind(be,operator) && operator.isShiftKeyDown() && be instanceof RotaryConversionBlockEntity rotor)
                rotor.toggle(operator);
            if (be instanceof MachineStatusInspectable machine) operator.sendOverlayMessage(machine.statusDetail());
        }
        return InteractionResult.SUCCESS;
    }
}
