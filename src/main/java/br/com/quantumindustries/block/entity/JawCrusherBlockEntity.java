package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.logistics.MachineItemSlot;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import br.com.quantumindustries.block.JawCrusherBlock;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.KineticNetworkManager;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import br.com.quantumindustries.material.CrushingCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Crushes raw ore, grain and coal by crank strokes or work delivered at the flywheel. */
public final class JawCrusherBlockEntity extends RotationalBlockEntity implements MachineStatusInspectable {
    public static final int WORK_TICKS = 20;
    private static final int STROKE_TICKS = 8;
    private static final int SHAFT_LOAD = 6;
    private static final double WORK_PER_PROGRESS = 1.35;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private double workRemainder;
    private boolean shaftWorking;
    private String automationOperator = "";
    public boolean automationAuthorized() {
        return ProcessingAuthorization.permitted(level, automationOperator, "mineral_beneficiation");
    }
    public boolean bindAutomation(net.minecraft.world.entity.player.Player player) {
        if (!ProcessingAuthorization.canBind(level, worldPosition, player, "mineral_beneficiation")) return false;
        if (!automationAuthorized()) {
            automationOperator = player.getUUID().toString();
            changed();
        }
        return automationAuthorized();
    }

    private long lastStroke = -STROKE_TICKS;

    private final MachineItemSlot inputPort = new MachineItemSlot(this, () -> input, stack -> input = stack,
            stack -> automationAuthorized() && CrushingCatalog.isValidInput(stack), 64, before -> changed());
    private final MachineItemSlot outputPort = new MachineItemSlot(this, () -> output, stack -> output = stack,
            null, 64, before -> changed());
    private final ResourceHandler<ItemResource> itemPort = new CombinedResourceHandler<>(inputPort, outputPort);

    public ResourceHandler<ItemResource> itemPort() { return itemPort; }

    public ResourceHandler<ItemResource> itemPort(BlockState state, Direction face) {
        if (!state.getValue(JawCrusherBlock.REFINED)) return itemPort;
        if (JawCrusherBlock.isInputPort(state, face)) return inputPort;
        return face != null && JawCrusherBlock.isOutputPort(state, face) ? outputPort : null;
    }

    public JawCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.JAW_CRUSHER_ENTITY.get(), pos, state);
    }

    public Component inspectDetail() {
        if (progress > 0 && !input.isEmpty()) {
            return Component.translatable("gui.quantumindustries.crusher.progress", progress * 100 / WORK_TICKS);
        }
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.press.output_ready", output.getCount(), output.getHoverName());
        }
        return Component.translatable("message.quantumindustries.jaw_crusher.status",
                progress, WORK_TICKS,
                input.isEmpty() ? Component.literal("-") : input.getHoverName(),
                output.isEmpty() ? 0 : output.getCount());
    }

    @Override public Component statusTitle() { return Component.translatable("block.quantumindustries.jaw_crusher"); }
    @Override public Component statusDetail() { return inspectDetail(); }
    @Override public State statusState() {
        if (working()) return State.WORKING;
        if (!output.isEmpty()) return State.READY;
        if (!input.isEmpty()) return State.BLOCKED;
        return State.IDLE;
    }
    @Override public float statusProgress() {
        if (progress > 0 && !input.isEmpty()) return Math.min(1.0f, (float) progress / WORK_TICKS);
        return !output.isEmpty() ? 1.0f : 0.0f;
    }

    public ItemStack input() { return input; }
    public ItemStack output() { return output; }
    public int progress() { return progress; }

    /** The menu edits the same stacks persisted and used by automation. */
    public net.minecraft.world.Container menuInventory() {
        return new net.minecraft.world.SimpleContainer(2) {
            @Override public ItemStack getItem(int slot) {
                return switch (slot) { case 0 -> input; case 1 -> output; default -> ItemStack.EMPTY; };
            }
            @Override public boolean isEmpty() { return input.isEmpty() && output.isEmpty(); }
            @Override public void setItem(int slot, ItemStack stack) {
                if (slot == 0 && !ItemStack.isSameItemSameComponents(getItem(slot), stack)) resetProgress();
                switch (slot) { case 0 -> input = stack; case 1 -> output = stack; default -> throw new IndexOutOfBoundsException(slot); }
                changed();
            }
            @Override public ItemStack removeItem(int slot, int amount) {
                var taken = getItem(slot).split(amount);
                if (slot == 0 && getItem(slot).isEmpty()) resetProgress();
                if (!taken.isEmpty()) changed();
                return taken;
            }
            @Override public ItemStack removeItemNoUpdate(int slot) {
                var taken = getItem(slot);
                setItem(slot, ItemStack.EMPTY);
                return taken;
            }
            @Override public void clearContent() { for (int slot = 0; slot < 2; slot++) setItem(slot, ItemStack.EMPTY); }
            @Override public void setChanged() { changed(); }
        };
    }

    public ItemStack extractOutput(int maximum) {
        if (!(level instanceof ServerLevel) || output.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        ItemStack result = output.split(Math.min(maximum, output.getCount()));
        if (output.isEmpty()) output = ItemStack.EMPTY;
        changed();
        return result;
    }

    private boolean canRun() {
        if (!automationAuthorized()) return false;
        ItemStack result = CrushingCatalog.resultFor(input);
        return !result.isEmpty() && (output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    public boolean insert(ItemStack stack) {
        if (!automationAuthorized()) return false;
        if (!(level instanceof ServerLevel) || !CrushingCatalog.isValidInput(stack)) return false;
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, stack)
                || input.getCount() >= input.getMaxStackSize())) return false;
        if (input.isEmpty()) input = stack.copyWithCount(1);
        else input.grow(1);
        stack.shrink(1);
        changed();
        return true;
    }

    public boolean manualStroke() {
        if (!automationAuthorized()) return false;
        if (!(level instanceof ServerLevel server) || !canRun() || server.getGameTime() - lastStroke < STROKE_TICKS) return false;
        var network = KineticNetworkManager.snapshot(server, worldPosition);
        if (network.powered() && network.nodeCount() > 1) return false;
        lastStroke = server.getGameTime();
        progress = Math.min(WORK_TICKS, progress + 7);
        server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.45f, 0.9f);
        finishIfReady(server);
        changed();
        return true;
    }

    public ItemStack take(boolean takeInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        if (!output.isEmpty()) {
            result = output.copy();
            output = ItemStack.EMPTY;
        } else if (takeInput && !input.isEmpty()) {
            result = input.copy();
            input = ItemStack.EMPTY;
            resetProgress();
        }
        if (!result.isEmpty()) changed();
        return result;
    }

    @Override public Direction.Axis axle() { return JawCrusherBlock.driveFace(getBlockState()).getAxis(); }
    @Override public boolean connectsMechanical(Direction face) { return face == JawCrusherBlock.driveFace(getBlockState()); }
    @Override public boolean allowsCrank() { return false; }
    @Override public int sourceCapacity() { return 0; }
    @Override public double kineticInertia() { return 2.5; }
    @Override public int requestedLoad() { return !isCranking() && canRun() ? SHAFT_LOAD : 0; }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if ((server.getGameTime() + worldPosition.asLong()) % 20 == 0
                && !JawCrusherBlock.assemblyIntact(server, worldPosition, getBlockState())) {
            server.removeBlock(worldPosition, false);
            return;
        }
        var network = KineticNetworkManager.snapshot(server, worldPosition);
        double work = requestedLoad() > 0 && network.powered()
                ? KineticNetworkManager.claimLoadWork(server, worldPosition) : 0;
        boolean wasWorking = shaftWorking;
        shaftWorking = work > 0;
        if (shaftWorking) {
            workRemainder += work;
            int earned = (int) (workRemainder / WORK_PER_PROGRESS);
            workRemainder -= earned * WORK_PER_PROGRESS;
            progress = Math.min(WORK_TICKS, progress + earned);
            setChanged();
            if (progress >= WORK_TICKS) {
                finishIfReady(server);
                server.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 1.05f);
                changed();
                return;
            }
        }
        if (wasWorking != shaftWorking || shaftWorking && server.getGameTime() % 5 == 0) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean working() { return isCranking() || shaftWorking; }

    public boolean isCranking() {
        if (level == null) return false;
        long age = level.getGameTime() - lastStroke;
        return age >= 0 && age < STROKE_TICKS;
    }

    public double visualTurns(double gameTime) {
        double stroke = Math.clamp((gameTime - lastStroke) / STROKE_TICKS, 0.0, 1.0);
        return stroke * stroke * (3.0 - 2.0 * stroke);
    }

    private void finishIfReady(ServerLevel server) {
        if (progress < WORK_TICKS || !canRun()) return;
        ItemStack result = CrushingCatalog.resultFor(input);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input.getItem()),
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.65, worldPosition.getZ() + 0.5,
                8, 0.1, 0.05, 0.1, 0.03);
        input.shrink(1);
        if (output.isEmpty()) output = result;
        else output.grow(result.getCount());
        resetProgress();
    }

    private void resetProgress() {
        progress = 0;
        workRemainder = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("automation_operator", automationOperator);
        out.putInt("progress", progress);
        out.putLong("last_stroke", lastStroke);
        out.putDouble("kinetic_work_remainder", workRemainder);
        if (!input.isEmpty()) out.store("input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        automationOperator = in.getStringOr("automation_operator", "");
        progress = Math.clamp(in.getIntOr("progress", 0), 0, WORK_TICKS);
        lastStroke = in.getLongOr("last_stroke", -STROKE_TICKS);
        double remainder = in.getDoubleOr("kinetic_work_remainder", 0);
        workRemainder = Double.isFinite(remainder) ? Math.clamp(remainder, 0, WORK_PER_PROGRESS) : 0;
        shaftWorking = in.getBooleanOr("shaft_working", false);
        input = in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!input.isEmpty()) {
                Block.popResource(server, pos, input.copy());
                input = ItemStack.EMPTY;
            }
            if (!output.isEmpty()) {
                Block.popResource(server, pos, output.copy());
                output = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("shaft_working", shaftWorking);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
