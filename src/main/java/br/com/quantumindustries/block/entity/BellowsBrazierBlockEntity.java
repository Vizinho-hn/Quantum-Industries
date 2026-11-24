package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.logistics.MachineItemSlot;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.HeatTreatment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Thermal annealing brazier heated by a heat source placed directly underneath (fire, campfire, magma, lava, or lit furnace). */
public final class BellowsBrazierBlockEntity extends BlockEntity implements MachineStatusInspectable {
    private String automationOperator = "";
    public boolean automationAuthorized() {
        return ProcessingAuthorization.permitted(level, automationOperator, "thermal_annealing");
    }
    public boolean bindAutomation(net.minecraft.world.entity.player.Player player) {
        if (!ProcessingAuthorization.canBind(level, worldPosition, player, "thermal_annealing")) return false;
        if (!automationAuthorized()) {
            automationOperator = player.getUUID().toString();
            changed();
        }
        return automationAuthorized();
    }

    public static final int HEATING_TICKS = 60;
    private static final int MAX_HEAT = 300;
    private int heat, progress;
    private long lastPuff = -10;
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;

    private final ResourceHandler<ItemResource> itemPort = new CombinedResourceHandler<>(
            new MachineItemSlot(this, () -> input, stack -> input = stack,
                    stack -> automationAuthorized() && output.isEmpty() && HeatTreatment.canHeat(stack), 1, before -> changed()),
            new MachineItemSlot(this, () -> output, stack -> output = stack,
                    null, 1, before -> changed()));

    public ResourceHandler<ItemResource> itemPort() { return itemPort; }

    public BellowsBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BELLOWS_BRAZIER_ENTITY.get(), pos, state);
    }

    /** Returns 2 for intense heat (lava/fire), 1 for standard heat (magma/lit campfire/lit furnace), or 0 if unheated. */
    public static int heatSourceLevel(Level level, BlockPos pos) {
        if (level == null) return 0;
        return classifyHeatSource(level.getBlockState(pos.below()));
    }

    static int classifyHeatSource(BlockState below) {
        if (below.getFluidState().is(FluidTags.LAVA) || below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE)) {
            return 2;
        }
        if (below.is(Blocks.MAGMA_BLOCK)) return 1;
        if (below.is(BlockTags.CAMPFIRES) && below.getValueOrElse(BlockStateProperties.LIT, false)) return 1;
        if (below.getValueOrElse(BlockStateProperties.LIT, false) && (below.is(Blocks.FURNACE)
                || below.is(Blocks.BLAST_FURNACE) || below.is(Blocks.SMOKER))) {
            return 1;
        }
        return 0;
    }

    public boolean hasOutput() { return !output.isEmpty(); }
    public boolean hasInput() { return !input.isEmpty(); }
    public ItemStack input() { return input; }
    public ItemStack output() { return output; }
    public int progress() { return progress; }
    public boolean isLit() { return heat > 0 || heatSourceLevel(level, worldPosition) > 0; }

    public ItemStack extractOutput(int maximum) {
        if (!(level instanceof ServerLevel) || output.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        ItemStack result = output.split(Math.min(maximum, output.getCount()));
        if (output.isEmpty()) output = ItemStack.EMPTY;
        changed();
        return result;
    }

    public Component status() {
        return statusDetail();
    }

    @Override public Component statusTitle() {
        return Component.translatable("block.quantumindustries.bellows_brazier");
    }

    @Override public Component statusDetail() {
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.press.output_ready", output.getCount(), output.getHoverName());
        }
        if (progress > 0 && !input.isEmpty()) {
            return Component.translatable("message.quantumindustries.brazier.heating", progress * 100 / HEATING_TICKS);
        }
        int below = heatSourceLevel(level, worldPosition);
        if (below == 0 && heat <= 0) {
            return Component.translatable("message.quantumindustries.brazier.need_heat");
        }
        if (!input.isEmpty()) {
            return Component.translatable("message.quantumindustries.brazier.heating", progress * 100 / HEATING_TICKS);
        }
        return Component.translatable("message.quantumindustries.brazier.ready_idle");
    }

    @Override public State statusState() {
        if (!output.isEmpty()) return State.READY;
        if (!input.isEmpty()) return isLit() ? State.WORKING : State.BLOCKED;
        return isLit() ? State.IDLE : State.BLOCKED;
    }

    @Override public float statusProgress() {
        if (!output.isEmpty()) return 1.0f;
        return input.isEmpty() ? 0.0f : Math.min(1.0f, (float) progress / HEATING_TICKS);
    }

    public boolean insert(ItemStack stack) {
        if (!automationAuthorized()) return false;
        if (!(level instanceof ServerLevel) || !output.isEmpty()) return false;
        if (!input.isEmpty() || !HeatTreatment.canHeat(stack)) return false;
        input = stack.copyWithCount(1);
        stack.shrink(1);
        changed();
        return true;
    }

    /** Optional manual bellows blast when heated from below: accelerates current annealing by +20 ticks. */
    public boolean puff() {
        if (!automationAuthorized()) return false;
        if (!(level instanceof ServerLevel server) || !isLit() || server.getGameTime() - lastPuff < 10) return false;
        lastPuff = server.getGameTime();
        heat = Math.min(MAX_HEAT, Math.max(heat, 100) + 100);
        if (!input.isEmpty() && output.isEmpty()) {
            progress = Math.min(HEATING_TICKS - 1, progress + 20);
        }
        server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.78,
                worldPosition.getZ() + 0.5, 6, 0.14, 0.06, 0.14, 0.02);
        server.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25f, 1.3f);
        changed();
        return true;
    }

    public ItemStack take(boolean includeInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        if (!output.isEmpty()) {
            result = output.copy();
            output = ItemStack.EMPTY;
        } else if (includeInput && !input.isEmpty()) {
            result = input.copy();
            input = ItemStack.EMPTY;
            progress = 0;
        }
        if (!result.isEmpty()) changed();
        return result;
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        int belowHeat = heatSourceLevel(server, worldPosition);
        if (belowHeat > 0) {
            int target = belowHeat >= 2 ? 220 : 120;
            if (heat < target) heat = Math.min(target, heat + 4 * belowHeat);
        } else if (heat > 0) {
            heat = Math.max(0, heat - 2);
        }

        boolean shouldLit = belowHeat > 0 || heat > 0;
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != shouldLit) {
            server.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, shouldLit), Block.UPDATE_ALL);
        }

        if (!shouldLit) {
            if (progress > 0) {
                progress = Math.max(0, progress - 1);
                if (progress % 5 == 0) changed();
            }
            return;
        }

        if (automationAuthorized() && !input.isEmpty() && output.isEmpty()) {
            int step = belowHeat >= 2 || heat > 180 ? 2 : 1;
            progress += step;
            if (server.getGameTime() % 6 == 0) {
                server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.76,
                        worldPosition.getZ() + 0.5, 2, 0.1, 0.04, 0.1, 0.01);
            }
            if (progress >= HEATING_TICKS) {
                output = HeatTreatment.heat(input, server);
                input = ItemStack.EMPTY;
                progress = 0;
                server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.35f);
                server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.85,
                        worldPosition.getZ() + 0.5, 6, 0.12, 0.06, 0.12, 0.02);
                changed();
                return;
            }
            if (progress % 4 == 0) changed();
        } else if (server.getGameTime() % 20 == 0) {
            changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); out.putString("automation_operator", automationOperator);
        out.putInt("heat", heat);
        out.putInt("progress", progress);
        out.putLong("last_puff", lastPuff);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); automationOperator = in.getStringOr("automation_operator", "");
        heat = Math.clamp(in.getIntOr("heat", 0), 0, MAX_HEAT);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, HEATING_TICKS);
        lastPuff = in.getLongOr("last_puff", -10);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) { Block.popResource(server, pos, input.copy()); input = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
