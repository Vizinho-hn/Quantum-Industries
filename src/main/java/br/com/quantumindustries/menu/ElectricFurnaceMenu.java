package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity;
import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity.OperatingState;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ElectricFurnaceMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 12;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ElectricFurnaceMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), id);
        Player player = inventory.player;
        access = ContainerLevelAccess.create(player.level(), pos);
        ElectricFurnaceBlockEntity furnace = player.level() instanceof ServerLevel server
                && server.getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity entity ? entity : null;
        var items = furnace == null ? new SimpleContainer(ElectricFurnaceBlockEntity.SLOT_COUNT) : furnace.menuInventory();
        data = furnace == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                var thermal = furnace.thermalBody();
                return switch (index) {
                    case 0 -> furnace.energy();
                    case 1 -> furnace.progress();
                    case 2 -> ElectricFurnaceBlockEntity.WORK_TICKS;
                    case 3 -> thermal.temperatureC();
                    case 4 -> furnace.requiredTemperatureC();
                    case 5 -> thermal.maxOperatingTempC();
                    case 6 -> displayInt(thermal.thermalEnergyTu());
                    case 7 -> displayInt(thermal.lastHeatingRateTu());
                    case 8 -> displayInt(thermal.lastCoolingRateTu());
                    case 9 -> furnace.stateCode();
                    case 10 -> furnace.permitted() ? 1 : 0;
                    case 11 -> furnace.isHeating() ? 1 : 0;
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };

        addSlot(new Slot(items, ElectricFurnaceBlockEntity.INPUT_SLOT, 112, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return player.level().isClientSide() || furnace != null && furnace.canLoad(player, stack);
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                super.onTake(taker, stack);
                if (furnace != null) furnace.onInputTaken();
            }
        });
        addSlot(new Slot(items, ElectricFurnaceBlockEntity.OUTPUT_SLOT, 214, 56) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 83 + column * 18, 156 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 83 + column * 18, 214));
        }
        addDataSlots(data);
    }

    private static int displayInt(long value) {
        return Math.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public int energy() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int duration() { return data.get(2); }
    public int temperatureC() { return data.get(3); }
    public int requiredTempC() { return data.get(4); }
    public int maxTempC() { return data.get(5); }
    public int thermalEnergyTu() { return data.get(6); }
    public int heatingRateTu() { return data.get(7); }
    public int coolingRateTu() { return data.get(8); }
    public int stateCode() { return data.get(9); }
    public boolean permitted() { return data.get(10) != 0; }
    public boolean isHeating() { return data.get(11) != 0; }
    public OperatingState operatingState() { return OperatingState.fromCode(stateCode()); }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ELECTRIC_FURNACE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = ElectricFurnaceBlockEntity.SLOT_COUNT;
        int inputSlot = ElectricFurnaceBlockEntity.INPUT_SLOT;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!getSlot(inputSlot).mayPlace(stack) || !moveItemStackTo(stack, inputSlot, inputSlot + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
