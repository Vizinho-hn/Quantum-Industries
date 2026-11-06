package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.HeatTreatment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Manually blown thermal furnace. Coal is fuel, not a source of FE. */
public final class BellowsBrazierBlockEntity extends BlockEntity {
    private static final int FUEL_PER_COAL = 1200, HEAT_PER_PUFF = 100, HEATING_TICKS = 60;
    private int fuel, heat, progress;
    private long lastPuff = -10;
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;

    public BellowsBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BELLOWS_BRAZIER_ENTITY.get(), pos, state);
    }
    public Component status() {
        return Component.translatable("message.quantumindustries.brazier.status", fuel, heat, progress);
    }
    public boolean insert(ItemStack stack) {
        if (!(level instanceof ServerLevel) || !output.isEmpty()) return false;
        if (stack.is(Items.COAL) && fuel <= FUEL_PER_COAL * 4) {
            fuel += FUEL_PER_COAL;
            stack.shrink(1);
            changed();
            return true;
        }
        if (!input.isEmpty() || !(stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT))) return false;
        input = stack.copyWithCount(1);
        stack.shrink(1);
        changed();
        return true;
    }
    public boolean puff() {
        if (!(level instanceof ServerLevel server) || fuel <= 0 || server.getGameTime() - lastPuff < 10) return false;
        lastPuff = server.getGameTime();
        heat = Math.min(HEAT_PER_PUFF * 3, heat + HEAT_PER_PUFF);
        changed();
        return true;
    }
    public ItemStack take(boolean includeInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        if (!output.isEmpty()) { result = output.copy(); output = ItemStack.EMPTY; }
        else if (includeInput && !input.isEmpty()) {
            result = input.copy(); input = ItemStack.EMPTY; progress = 0;
        }
        if (!result.isEmpty()) changed();
        return result;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (heat <= 0 || fuel <= 0) return;
        heat--;
        fuel--;
        if (!input.isEmpty() && output.isEmpty()) {
            progress++;
            if (progress >= HEATING_TICKS) {
                output = HeatTreatment.heat(input, server);
                input = ItemStack.EMPTY;
                progress = 0;
            }
        }
        if (server.getGameTime() % 10 == 0 || !output.isEmpty()) changed();
    }
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("fuel", fuel);
        out.putInt("heat", heat);
        out.putInt("progress", progress);
        out.putLong("last_puff", lastPuff);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        fuel = Math.max(0, in.getIntOr("fuel", 0));
        heat = Math.clamp(in.getIntOr("heat", 0), 0, HEAT_PER_PUFF*3);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, HEATING_TICKS);
        lastPuff = in.getLongOr("last_puff", -10);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos,state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) { Block.popResource(server,pos,input.copy()); input=ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server,pos,output.copy()); output=ItemStack.EMPTY; }
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
