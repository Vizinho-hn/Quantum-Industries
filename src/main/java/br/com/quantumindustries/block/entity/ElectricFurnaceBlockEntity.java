package br.com.quantumindustries.block.entity;

import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.PhysicsEnergyContract;
import br.com.quantumindustries.energy.TransferBudget;
import br.com.quantumindustries.research.ResearchIds;
import br.com.quantumindustries.research.WorldResearchData;
import br.com.quantumindustries.thermal.ThermalBody;
import br.com.quantumindustries.thermal.ThermalInspectable;
import br.com.quantumindustries.thermal.ThermalNetwork;
import br.com.quantumindustries.thermal.ThermalParticipant;
import br.com.quantumindustries.thermal.ThermalSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class ElectricFurnaceBlockEntity extends BlockEntity
        implements EnergyInspectable, ThermalInspectable, ThermalParticipant, MachineStatusInspectable {
    public static final int CAPACITY = 1600;
    public static final int MAX_RECEIVE = 64;
    public static final int HEATING_FE_PER_TICK = 8;
    public static final int REQUIRED_TEMP_C = 180;
    public static final int MAX_TEMP_C = 420;
    public static final int WORK_TICKS = 100;
    public static final int PROCESS_TU_PER_TICK = 4;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final String TITLE_KEY = "block.quantumindustries.electric_furnace";
    public static final String GUI_KEY = "gui.quantumindustries.electric_furnace.";

    private static final Identifier JOULE_HEATING = ResearchIds.of("joule_heating");
    private static final String THERMAL_PREFIX = "qts_";
    private static final int INITIAL_TEMP_C = 20;
    private static final int HEAT_CAPACITY = 16;
    private static final int CONDUCTANCE_MILLI = 120;
    private static final int INSULATION_PERMIL = 780;
    private static final int MIN_TEMP_C = 0;
    private static final int HEATER_CUTOFF_C = 360;
    private static final int INSPECTION_INTERVAL = 5;
    private static final String INSPECTION_STATE = "inspection_state";

    public enum OperatingState {
        IDLE("idle"), HEATING("heating"), PROCESSING("processing"), RESIDUAL("residual"),
        COOLING("cooling"), NO_ENERGY("no_energy"), OUTPUT_FULL("output_full"), LOCKED("locked");

        private static final OperatingState[] BY_CODE = values();
        private final String key;

        OperatingState(String key) { this.key = key; }

        public static OperatingState fromCode(int code) {
            return code >= 0 && code < BY_CODE.length ? BY_CODE[code] : IDLE;
        }

        public Component label() {
            return Component.translatable(GUI_KEY + "state." + key);
        }

        public boolean processing() { return this == PROCESSING || this == RESIDUAL; }
    }

    private final SimpleEnergyHandler battery = new SimpleEnergyHandler(CAPACITY, MAX_RECEIVE, HEATING_FE_PER_TICK);
    private final TransferBudget transferBudget = new TransferBudget(MAX_RECEIVE);
    private final ThermalBody thermal = new ThermalBody(INITIAL_TEMP_C, HEAT_CAPACITY, CONDUCTANCE_MILLI,
            INSULATION_PERMIL, MIN_TEMP_C, MAX_TEMP_C);

    private int progress;
    private String operator = "";
    private String activeRecipe = "";
    private boolean loading;
    private boolean heating;
    private boolean inspectionDirty;
    private boolean clientHasOutput;
    private OperatingState operatingState = OperatingState.IDLE;
    private final SimpleContainer inventory = new SimpleContainer(SLOT_COUNT) {
        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = super.removeItem(slot, amount);
            if (slot == INPUT_SLOT && !removed.isEmpty()) resetProgress();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!loading && slot == INPUT_SLOT && !ItemStack.isSameItemSameComponents(getItem(slot), stack)) {
                resetProgress();
            }
            super.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) markChanged();
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ELECTRIC_FURNACE_ENTITY.get(), pos, state);
    }

    public boolean bind(Player player) {
        if (!(level instanceof ServerLevel server) || player.level() != server
                || !operator.isEmpty() || !WorldResearchData.get(server).knows(player, JOULE_HEATING)) return false;
        operator = player.getUUID().toString();
        markChanged();
        return true;
    }

    public boolean permitted() {
        return level instanceof ServerLevel server && !operator.isEmpty()
                && WorldResearchData.get(server).knows(operator, JOULE_HEATING);
    }

    public boolean canInsert(ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty() || !permitted()) return false;
        ItemStack current = input();
        int limit = inventory.getMaxStackSize(stack);
        if (!current.isEmpty() && (!ItemStack.isSameItemSameComponents(current, stack)
                || current.getCount() >= limit)) return false;
        return recipeFor(server, new SingleRecipeInput(stack)) != null;
    }

    public boolean canLoad(Player player, ItemStack stack) {
        return level instanceof ServerLevel server && player.level() == server
                && !stack.isEmpty() && permitted() && WorldResearchData.get(server).knows(player, JOULE_HEATING)
                && recipeFor(server, new SingleRecipeInput(stack)) != null;
    }

    // ItemRouting.insertLoop expects the packet to retain only the unaccepted remainder.
    public boolean insert(ItemStack stack) {
        if (!canInsert(stack)) return false;
        inventory.setItem(INPUT_SLOT, stack.copyWithCount(input().getCount() + 1));
        stack.shrink(1);
        return true;
    }

    public ItemStack extractOutput(int maximum) {
        return level instanceof ServerLevel && maximum > 0
                ? inventory.removeItem(OUTPUT_SLOT, maximum) : ItemStack.EMPTY;
    }

    private static RecipeHolder<SmeltingRecipe> recipeFor(ServerLevel server, SingleRecipeInput input) {
        return input.isEmpty() ? null
                : server.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, server).orElse(null);
    }

    private boolean hasOutputRoom(ItemStack result) {
        ItemStack existing = output();
        return !result.isEmpty()
                && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, result))
                && result.getCount() <= inventory.getMaxStackSize(result) - existing.getCount();
    }

    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        thermal.beginTick(server.getGameTime());
        int ambient = ThermalNetwork.ambientAt(server, worldPosition);
        long previousHeat = thermal.thermalEnergyTu();
        int previousProgress = progress;
        int previousEnergy = energy();
        OperatingState previousState = operatingState;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input());
        RecipeHolder<SmeltingRecipe> recipe = recipeFor(server, recipeInput);
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(recipeInput);
        String recipeId = recipe == null ? "" : recipe.id().identifier().toString();
        boolean allowed = permitted();
        boolean room = hasOutputRoom(result);
        if (!allowed || result.isEmpty() || !activeRecipe.equals(recipeId)) resetProgress();

        heating = false;
        boolean processing = false;
        if (allowed && room) {
            if (energy() > 0 && thermal.temperatureC() < HEATER_CUTOFF_C) {
                int spentFe = extractHeatingEnergy();
                thermal.addHeat(PhysicsEnergyContract.jouleHeating(spentFe).usefulOutput());
                heating = spentFe > 0;
            }
            if (thermal.temperatureC() >= REQUIRED_TEMP_C && thermal.thermalEnergyTu() >= PROCESS_TU_PER_TICK) {
                thermal.consumeHeat(PROCESS_TU_PER_TICK);
                processing = true;
                activeRecipe = recipeId;
                if (++progress == WORK_TICKS) {
                    inventory.removeItem(INPUT_SLOT, 1);
                    inventory.setItem(OUTPUT_SLOT, result.copyWithCount(output().getCount() + result.getCount()));
                }
            }
        }

        long ambientLoss = thermal.stepAmbient(ambient, 0);
        ThermalNetwork.stepNeighbors(server, worldPosition, this);
        operatingState = resolveState(allowed, result, room, processing, ambientLoss);
        boolean lit = heating || processing;
        if (getBlockState().getValue(BlockStateProperties.LIT) != lit) {
            server.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
        if (previousHeat != thermal.thermalEnergyTu() || previousProgress != progress || previousEnergy != energy()) {
            markChanged();
        }
        if (previousState != operatingState || inspectionDirty && server.getGameTime() % INSPECTION_INTERVAL == 0) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            inspectionDirty = false;
        }
    }

    private OperatingState resolveState(boolean allowed, ItemStack result, boolean room, boolean processing,
                                        long ambientLoss) {
        if (!allowed) return OperatingState.LOCKED;
        if (!result.isEmpty() && !room) return OperatingState.OUTPUT_FULL;
        if (processing) return heating || energy() > 0 ? OperatingState.PROCESSING : OperatingState.RESIDUAL;
        if (heating) return OperatingState.HEATING;
        if (!result.isEmpty() && energy() == 0) return OperatingState.NO_ENERGY;
        return ambientLoss > 0 ? OperatingState.COOLING : OperatingState.IDLE;
    }

    private int extractHeatingEnergy() {
        try (var transaction = Transaction.openRoot()) {
            int spent = battery.extract(HEATING_FE_PER_TICK, transaction);
            transaction.commit();
            return spent;
        }
    }

    @Override
    public int receiveEnergy(int maximum) {
        if (!(level instanceof ServerLevel server) || maximum <= 0) return 0;
        long tick = server.getGameTime();
        int limit = Math.min(maximum, transferBudget.receiveRemaining(tick));
        if (limit == 0) return 0;
        try (var transaction = Transaction.openRoot()) {
            int received = battery.insert(limit, transaction);
            transaction.commit();
            transferBudget.recordReceive(tick, received);
            if (received > 0) markChanged();
            return received;
        }
    }

    public SimpleContainer menuInventory() { return inventory; }
    public ItemStack input() { return inventory.getItem(INPUT_SLOT); }
    public ItemStack output() { return inventory.getItem(OUTPUT_SLOT); }
    public int progress() { return progress; }
    public int requiredTemperatureC() { return REQUIRED_TEMP_C; }
    public boolean isHeating() { return heating; }
    public boolean isProcessing() { return operatingState.processing(); }
    public int stateCode() { return operatingState.ordinal(); }
    @Override public int energy() { return (int) battery.getAmountAsLong(); }
    @Override public int capacity() { return CAPACITY; }
    @Override public ThermalBody thermalBody() { return thermal; }
    @Override public boolean connectsThermal(Direction face) { return true; }
    @Override public int thermalContactConductanceMilli(Direction face) { return CONDUCTANCE_MILLI; }
    @Override public ThermalSnapshot thermalSnapshot() { return thermal.snapshot(REQUIRED_TEMP_C); }

    @Override public Component inspectTitle() { return Component.translatable(TITLE_KEY); }
    @Override public Component statusTitle() { return inspectTitle(); }
    @Override public Component inspectDetail() { return statusDetail(); }
    @Override public Component statusDetail() { return operatingState.label(); }

    @Override
    public State statusState() {
        return switch (operatingState) {
            case HEATING, PROCESSING, RESIDUAL -> State.WORKING;
            case NO_ENERGY, OUTPUT_FULL, LOCKED -> State.BLOCKED;
            case IDLE, COOLING -> hasStatusOutput() ? State.READY : State.IDLE;
        };
    }

    @Override
    public float statusProgress() {
        return statusState() == State.READY ? 1f : (float) progress / WORK_TICKS;
    }

    private boolean hasStatusOutput() {
        return level != null && level.isClientSide() ? clientHasOutput : !output().isEmpty();
    }

    private void markChanged() {
        setChanged();
        inspectionDirty = true;
    }

    public void onInputTaken() {
        if (progress == 0) return;
        resetProgress();
        markChanged();
    }

    private void resetProgress() {
        progress = 0;
        activeRecipe = "";
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("stored_fe", energy());
        out.putInt("progress", progress);
        out.putString("operator", operator);
        out.putString("recipe", activeRecipe);
        if (!input().isEmpty()) out.store("input", ItemStack.CODEC, input());
        if (!output().isEmpty()) out.store("output", ItemStack.CODEC, output());
        thermal.save(out, THERMAL_PREFIX);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        loading = true;
        try {
            inventory.setItem(INPUT_SLOT, in.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY));
            inventory.setItem(OUTPUT_SLOT, in.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        } finally {
            loading = false;
        }
        battery.set(Math.clamp(in.getIntOr("stored_fe", 0), 0, CAPACITY));
        progress = Math.clamp(in.getIntOr("progress", 0), 0, WORK_TICKS - 1);
        operator = in.getStringOr("operator", "");
        activeRecipe = in.getStringOr("recipe", "");
        int ambient = level instanceof ServerLevel server ? ThermalNetwork.ambientAt(server, worldPosition) : INITIAL_TEMP_C;
        thermal.load(in, THERMAL_PREFIX, ambient);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(INSPECTION_STATE, stateCode());
        tag.putInt("progress", progress);
        tag.putInt("stored_fe", energy());
        tag.putBoolean("heating", heating);
        tag.putBoolean("has_output", !output().isEmpty());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput in) {
        operatingState = OperatingState.fromCode(in.getIntOr(INSPECTION_STATE, 0));
        progress = in.getIntOr("progress", 0);
        battery.set(in.getIntOr("stored_fe", 0));
        heating = in.getBooleanOr("heating", false);
        clientHasOutput = in.getBooleanOr("has_output", false);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput in) {
        handleUpdateTag(in);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) Containers.dropContents(server, pos, inventory);
    }
}
