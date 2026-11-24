package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.AlloyCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Era II FE-powered induction alloy furnace: smelts both Tier 1 (Bronze, Brass) and Tier 2 (Invar, Electrum, Constantan, Steel) alloys. */
public final class ElectricAlloySmelterBlockEntity extends BlockEntity implements EnergyInspectable, MachineStatusInspectable {
    public static final int CAPACITY = 1200;
    public static final int MAX_RECEIVE = 64;
    public static final int ENERGY_PER_TICK = 4;

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, ENERGY_PER_TICK);
    private ItemStack inputA = ItemStack.EMPTY;
    private ItemStack inputB = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private String automationOperator = "";
    public boolean automationAuthorized() {
        return ProcessingAuthorization.permitted(level, automationOperator, "electric_alloy_smelting");
    }
    public boolean bindAutomation(net.minecraft.world.entity.player.Player player) {
        if (!ProcessingAuthorization.canBind(level, worldPosition, player, "electric_alloy_smelting")) return false;
        if (!automationAuthorized()) {
            automationOperator = player.getUUID().toString();
            changed();
        }
        return automationAuthorized();
    }


    public ElectricAlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(), pos, state);
    }

    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public Component inspectTitle() {
        return Component.translatable("block.quantumindustries.electric_alloy_smelter");
    }

    @Override public Component statusTitle() { return inspectTitle(); }
    @Override public Component statusDetail() { return inspectDetail(); }

    @Override public State statusState() {
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (progress > 0 && recipe != null) return State.WORKING;
        if (!output.isEmpty()) return State.READY;
        if (recipe != null || !inputA.isEmpty() || !inputB.isEmpty()) return State.BLOCKED;
        return State.IDLE;
    }

    @Override public float statusProgress() {
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (progress > 0 && recipe != null) return Math.min(1.0f, (float) progress / recipe.workTicks());
        return !output.isEmpty() ? 1.0f : 0.0f;
    }

    public ItemStack inputA() { return inputA; }
    public ItemStack inputB() { return inputB; }
    public int progress() { return progress; }

    /** The menu edits the same stacks persisted and used by automation. */
    public net.minecraft.world.Container menuInventory() {
        return new net.minecraft.world.SimpleContainer(3) {
            @Override public ItemStack getItem(int slot) {
                return switch (slot) { case 0 -> inputA; case 1 -> inputB; case 2 -> output; default -> ItemStack.EMPTY; };
            }
            @Override public boolean isEmpty() { return inputA.isEmpty() && inputB.isEmpty() && output.isEmpty(); }
            @Override public void setItem(int slot, ItemStack stack) {
                if (slot < 2 && !ItemStack.isSameItemSameComponents(getItem(slot), stack)) progress = 0;
                switch (slot) { case 0 -> inputA = stack; case 1 -> inputB = stack; case 2 -> output = stack; default -> throw new IndexOutOfBoundsException(slot); }
                changed();
            }
            @Override public ItemStack removeItem(int slot, int amount) {
                var taken = getItem(slot).split(amount);
                if (slot < 2 && getItem(slot).isEmpty()) progress = 0;
                if (!taken.isEmpty()) changed();
                return taken;
            }
            @Override public ItemStack removeItemNoUpdate(int slot) {
                var taken = getItem(slot);
                setItem(slot, ItemStack.EMPTY);
                return taken;
            }
            @Override public void clearContent() { for (int slot = 0; slot < 3; slot++) setItem(slot, ItemStack.EMPTY); }
            @Override public void setChanged() { changed(); }
        };
    }

    @Override public Component inspectDetail() {
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (progress > 0 && recipe != null) {
            return Component.translatable("gui.quantumindustries.smelter.progress", progress * 100 / recipe.workTicks());
        }
        if (!output.isEmpty()) {
            return Component.translatable("gui.quantumindustries.smelter.output_ready", output.getCount(), output.getHoverName());
        }
        if (recipe != null) {
            return Component.translatable("gui.quantumindustries.smelter.waiting_energy");
        }
        if (!inputA.isEmpty() || !inputB.isEmpty()) {
            return Component.translatable("gui.quantumindustries.smelter.waiting_pair");
        }
        return Component.translatable("gui.quantumindustries.smelter.idle");
    }

    @Override public int receiveEnergy(int maxReceive) {
        if (!(level instanceof ServerLevel) || maxReceive <= 0 || energy() >= CAPACITY) return 0;
        int inserted;
        try (var transaction = Transaction.openRoot()) {
            inserted = battery.insert(Math.min(MAX_RECEIVE, maxReceive), transaction);
            if (inserted > 0) transaction.commit();
        }
        if (inserted > 0) changed();
        return inserted;
    }

    public boolean canInsertIngredient(int slot, ItemStack stack) {
        if (stack.isEmpty() || !AlloyCatalog.isValidIngredient(stack, 2)) return false;
        ItemStack target = slot == 0 ? inputA : inputB;
        ItemStack other = slot == 0 ? inputB : inputA;
        if (!other.isEmpty() && other.is(stack.getItem())) return false;
        if (!other.isEmpty() && !AlloyCatalog.canPair(other, stack, 2)) return false;
        return target.isEmpty() || (ItemStack.isSameItemSameComponents(target, stack) && target.getCount() < 16);
    }

    public boolean canInsertIngredient(ItemStack stack) {
        return automationAuthorized() && (canInsertIngredient(0, stack) || canInsertIngredient(1, stack));
    }

    public boolean insertIngredient(ItemStack stack) {
        if (!automationAuthorized()) return false;
        if (!(level instanceof ServerLevel)) return false;
        for (int slot = 0; slot < 2; slot++) {
            if (!canInsertIngredient(slot, stack)) continue;
            ItemStack target = slot == 0 ? inputA : inputB;
            if (target.isEmpty()) {
                if (slot == 0) inputA = stack.copyWithCount(1);
                else inputB = stack.copyWithCount(1);
            } else target.grow(1);
            stack.shrink(1);
            changed();
            return true;
        }
        return false;
    }

    public ItemStack output() { return output; }

    public ItemStack extractOutput(int maximum) {
        if (!(level instanceof ServerLevel) || output.isEmpty() || maximum <= 0) return ItemStack.EMPTY;
        ItemStack result = output.split(Math.min(maximum, output.getCount()));
        if (output.isEmpty()) output = ItemStack.EMPTY;
        changed();
        return result;
    }

    public ItemStack extractResult(boolean allowInput) {
        if (!(level instanceof ServerLevel)) return ItemStack.EMPTY;
        if (!output.isEmpty()) {
            ItemStack taken = output.copy();
            output = ItemStack.EMPTY;
            changed();
            return taken;
        }
        if (allowInput) {
            if (!inputB.isEmpty()) {
                ItemStack taken = inputB.copy();
                inputB = ItemStack.EMPTY;
                progress = 0;
                changed();
                return taken;
            }
            if (!inputA.isEmpty()) {
                ItemStack taken = inputA.copy();
                inputA = ItemStack.EMPTY;
                progress = 0;
                changed();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean canProcess(AlloyCatalog.Recipe recipe) {
        if (!automationAuthorized()) return false;
        if (recipe == null) return false;
        ItemStack result = recipe.assemble();
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void setLit(ServerLevel server, boolean lit) {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != lit) {
            server.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (!automationAuthorized()) { setLit(server, false); return; }
        var recipe = AlloyCatalog.findMatch(inputA, inputB, 2);
        if (!canProcess(recipe)) {
            setLit(server, false);
            if (progress != 0) {
                progress = 0;
                changed();
            }
            return;
        }
        int prevTarget = progress * recipe.totalFe() / recipe.workTicks();
        int nextTarget = (progress + 1) * recipe.totalFe() / recipe.workTicks();
        int stepFe = Math.max(0, nextTarget - prevTarget);
        if (energy() < stepFe) {
            setLit(server, false);
            return;
        }
        if (stepFe > 0) {
            try (var transaction = Transaction.openRoot()) {
                if (battery.extract(stepFe, transaction) < stepFe) {
                    setLit(server, false);
                    return;
                }
                transaction.commit();
            }
        }
        setLit(server, true);
        progress++;
        if (progress % 6 == 0) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.65, worldPosition.getZ() + 0.5,
                    2, 0.14, 0.06, 0.14, 0.01);
        }
        if (progress % 20 == 0) {
            server.playSound(null, worldPosition, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 1.15f);
        }
        if (progress >= recipe.workTicks()) {
            progress = 0;
            ItemStack produced = recipe.assemble();
            recipe.consume(inputA, inputB);
            if (inputA.isEmpty()) inputA = ItemStack.EMPTY;
            if (inputB.isEmpty()) inputB = ItemStack.EMPTY;
            if (output.isEmpty()) output = produced;
            else output.grow(produced.getCount());
            setLit(server, canProcess(AlloyCatalog.findMatch(inputA, inputB, 2)) && energy() >= ENERGY_PER_TICK);
            server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.35f, 1.25f);
        }
        if (progress % 4 == 0 || progress == 0) changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("automation_operator", automationOperator);
        out.putInt("stored_fe", energy());
        out.putInt("progress", progress);
        if (!inputA.isEmpty()) out.store("input_a", ItemStack.CODEC, inputA);
        if (!inputB.isEmpty()) out.store("input_b", ItemStack.CODEC, inputB);
        if (!output.isEmpty()) out.store("output", ItemStack.CODEC, output);
    }

    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        automationOperator = in.getStringOr("automation_operator", "");
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, 200);
        inputA = in.read("input_a", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        inputB = in.read("input_b", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            if (!inputA.isEmpty()) { Block.popResource(server, pos, inputA.copy()); inputA = ItemStack.EMPTY; }
            if (!inputB.isEmpty()) { Block.popResource(server, pos, inputB.copy()); inputB = ItemStack.EMPTY; }
            if (!output.isEmpty()) { Block.popResource(server, pos, output.copy()); output = ItemStack.EMPTY; }
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
