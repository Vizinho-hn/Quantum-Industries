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
    private String operator = "", processId = "", automationOperator = "";
    private long lastInputTransfer = Long.MIN_VALUE / 2, lastOutputTransfer = Long.MIN_VALUE / 2;
    private int clientDuration, clientState, clientInputTransport, clientOutputTransport;
    private int lastState = -1, lastInputState = -1, lastOutputState = -1;
    public enum ProcessingState { WAITING, CUTTING, NO_POWER, OUTPUT_FULL, RESEARCH, INVALID }
    private int progress;
    public MechanicalSawmillBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(), pos, state); }
    @Override public Direction.Axis axle() { return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis(); }
    public ItemStack input() { return input.copy(); }
    public ItemStack output() { return output.copy(); }
    public int progress() { return progress; }

    /** Legacy machines bind on the first authorized interaction; new placements bind immediately. */
    public boolean automationBound() { return !automationOperator.isEmpty(); }
    public boolean bindAutomation(Player player) {
        if (!authorized(player)) return false;
        if (automationOperator.isEmpty()) { automationOperator = player.getUUID().toString(); changed(); }
        return true;
    }
    public boolean canInsertAutomated(ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty() || automationOperator.isEmpty()) return false;
        var holder = MechanicalProcesses.find(server, stack);
        return holder != null && WorldResearchData.get(server).knows(automationOperator, ResearchIds.of(holder.value().research()))
                && (input.isEmpty() || ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < input.getMaxStackSize());
    }
    public boolean insertAutomated(ItemStack stack) {
        if (!canInsertAutomated(stack)) return false;
        if (input.isEmpty()) {
            input = stack.copyWithCount(1); operator = automationOperator; progress = 0; processId = "";
        } else input.grow(1);
        stack.shrink(1); lastInputTransfer = level.getGameTime(); changed(); return true;
    }
    public ItemStack extractOutput(int maximum) {
        if (!(level instanceof ServerLevel) || maximum <= 0 || output.isEmpty()) return ItemStack.EMPTY;
        var taken = output.split(Math.min(maximum, output.getCount()));
        if (output.isEmpty()) output = ItemStack.EMPTY;
        lastOutputTransfer = level.getGameTime(); changed(); return taken;
    }
    public int processingDuration() {
        if (!(level instanceof ServerLevel server)) return clientDuration;
        var holder = input.isEmpty() ? null : MechanicalProcesses.find(server, input);
        return holder == null ? 0 : holder.value().ticks();
    }
    public ProcessingState processingState() {
        if (!(level instanceof ServerLevel server)) return ProcessingState.values()[Math.clamp(clientState, 0, ProcessingState.values().length - 1)];
        if (input.isEmpty()) return ProcessingState.WAITING;
        var holder = MechanicalProcesses.find(server, input);
        if (holder == null) return ProcessingState.INVALID;
        if (!WorldResearchData.get(server).knows(operator, ResearchIds.of(holder.value().research()))) return ProcessingState.RESEARCH;
        var result = holder.value().result();
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result) || output.getCount() + result.getCount() > output.getMaxStackSize())) return ProcessingState.OUTPUT_FULL;
        return rotating() && isSpinning() ? ProcessingState.CUTTING : ProcessingState.NO_POWER;
    }
    public int inputTransportState() { return level instanceof ServerLevel ? transportState(true) : clientInputTransport; }
    public int outputTransportState() { return level instanceof ServerLevel ? transportState(false) : clientOutputTransport; }
    private int transportState(boolean incoming) {
        for (Direction side : Direction.values()) {
            var pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof PneumaticTubeBlockEntity tube)) continue;
            var mode = tube.connectionMode(side.getOpposite());
            boolean connected = incoming ? mode.allowsOutput() : mode == br.com.quantumindustries.logistics.ConnectionMode.PULL;
            if (!connected) continue;
            long last = incoming ? lastInputTransfer : lastOutputTransfer;
            if (level.getGameTime() - last <= 20) return 2;
            if (!incoming && !output.isEmpty() && !tube.packet().isEmpty()) return 3;
            return 1;
        }
        return 0;
    }
    private void syncInspection() {
        int state = processingState().ordinal(), in = inputTransportState(), out = outputTransportState();
        if (state != lastState || in != lastInputState || out != lastOutputState) {
            lastState = state; lastInputState = in; lastOutputState = out; changed();
        }
    }

    public boolean insert(Player player, ItemStack held) {
        if (!authorized(player) || held.isEmpty()) return false;
        var recipe = MechanicalProcesses.find((ServerLevel) level, held);
        if (recipe == null || !WorldResearchData.get((ServerLevel) level).knows(player, recipe.value().research())) return false;
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, held) || input.getCount() >= input.getMaxStackSize())) return false;
        bindAutomation(player);
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

    @Override protected boolean isCutting() { return rotating() && !input.isEmpty(); }

    @Override public double kineticInertia() { return 2.5; }
    @Override public double kineticDrag() { return .20; }
    @Override public double kineticLoadTorque() { return requestedLoad() * .75; }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        var net = br.com.quantumindustries.mechanical.KineticNetworkManager.snapshot(server, worldPosition);
        var recipe = availableProcess();
        applyKineticSnapshot(net);
        if (recipe == null || !net.powered() || Math.abs(net.angularVelocity()) <= 1e-3) { syncInspection(); return; }
        var holder = MechanicalProcesses.find(server, input);
        String current = holder.id().identifier().toString();
        if (!current.equals(processId)) { processId = current; progress = 0; }
        progress++;
        if (progress >= recipe.ticks()) {
            var result = recipe.result();
            input.shrink(1);
            if (output.isEmpty()) output = result; else output.grow(result.getCount());
            progress = 0;
            if (input.isEmpty()) { operator = processId = ""; }
            changed();
        } else {
            setChanged();
            if (server.getGameTime() % 10 == 0) changed();
        }
        syncInspection();
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
        out.putString("automation_operator", automationOperator);
        out.putString("process", processId);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = Math.clamp(in.getIntOr("progress", 0), 0, 12000);
        operator = in.getStringOr("operator", "");
        automationOperator = in.getStringOr("automation_operator", operator);
        clientDuration = in.getIntOr("inspection_duration", 0);
        clientState = in.getIntOr("inspection_state", 0);
        clientInputTransport = in.getIntOr("inspection_input", 0);
        clientOutputTransport = in.getIntOr("inspection_output", 0);
        processId = in.getStringOr("process", "");
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putInt("inspection_duration", processingDuration()); tag.putInt("inspection_state", processingState().ordinal());
        tag.putInt("inspection_input", inputTransportState()); tag.putInt("inspection_output", outputTransportState());
        return tag;
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
