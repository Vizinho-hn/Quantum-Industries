package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import br.com.quantumindustries.recipe.MechanicalProcesses;
import br.com.quantumindustries.recipe.MechanicalProcessingRecipe;
import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MechanicalSawmillBlockEntity extends RotationalBlockEntity {
    private ItemStack input = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private String operator = "", processId = "";
    private int progress;
    public MechanicalSawmillBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis(); }
    public ItemStack input() { return input.copy(); }
    public ItemStack output() { return output.copy(); }
    public int progress() { return progress; }

    public boolean insert(Player player, ItemStack held) {
        if (!authorized(player) || held.isEmpty()) return false;
        var recipe = MechanicalProcesses.find((ServerLevel) level, held);
        if (recipe == null || !WorldResearchData.get((ServerLevel) level).knows(player, recipe.value().research())) return false;
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, held) || input.getCount() >= input.getMaxStackSize())) return false;
        if (input.isEmpty()) {
            input = held.copyWithCount(1);
            operator = player.getUUID().toString();
            progress = 0;
            processId = "";
        } else input.grow(1);
        held.shrink(1);
        changed();
        return true;
    }

    public ItemStack extract(Player player, boolean inputFirst) {
        if (!inReach(player)) return ItemStack.EMPTY;
        if (inputFirst && !input.isEmpty()) {
            var taken = input;
            input = ItemStack.EMPTY;
            progress = 0;
            operator = processId = "";
            setRotating(false);
            changed();
            return taken;
        }
        if (inputFirst) return ItemStack.EMPTY;
        if (output.isEmpty()) return ItemStack.EMPTY;
        var taken = output;
        output = ItemStack.EMPTY;
        changed();
        return taken;
    }

    private MechanicalProcessingRecipe availableProcess() {
        if (!(level instanceof ServerLevel server) || input.isEmpty()) return null;
        var holder = MechanicalProcesses.find(server, input);
        if (holder == null) return null;
        var recipe = holder.value();
        if (!WorldResearchData.get(server).knows(operator, ResearchIds.of(recipe.research()))) return null;
        var result = recipe.result();
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize())) return null;
        return recipe;
    }

    @Override public int requestedLoad() {
        var recipe = availableProcess();
        return recipe == null ? 0 : recipe.mechanicalLoad();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var recipe = availableProcess();
        boolean powered = recipe != null && br.com.quantumindustries.mechanical.MechanicalNetwork.state(server, worldPosition).powered();
        setRotating(powered);
        if (!powered) return;
        var holder = MechanicalProcesses.find(server, input);
        String current = holder.id().identifier().toString();
        if (!current.equals(processId)) { processId = current; progress = 0; }
        progress++;
        if (progress >= recipe.ticks()) {
            var result = recipe.result();
            input.shrink(1);
            if (output.isEmpty()) output = result; else output.grow(result.getCount());
            progress = 0;
            if (input.isEmpty()) { operator = processId = ""; setRotating(false); }
            changed();
        } else {
            setChanged();
            if (server.getGameTime() % 10 == 0) changed();
        }
    }

    public Component status() {
        return Component.translatable("gui.quantumindustries.sawmill.status", input.getCount(), output.getCount(), progress,
                rotating() ? Component.translatable("gui.quantumindustries.mechanical.running")
                        : Component.translatable("gui.quantumindustries.mechanical.stopped"));
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
        out.putInt("progress", progress);
        out.putString("operator", operator);
        out.putString("process", processId);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, 12000);
        operator = in.getStringOr("operator", "");
        processId = in.getStringOr("process", "");
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) { Block.popResource(server, pos, input.copy()); input = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
            progress = 0;
        }
    }
}
