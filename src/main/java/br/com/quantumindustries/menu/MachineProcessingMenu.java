package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.material.AlloyCatalog;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class MachineProcessingMenu extends AbstractContainerMenu {
    private final boolean crusher;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public MachineProcessingMenu(int id, Inventory inventory, BlockPos pos, boolean crusher) {
        super(crusher ? ModMenuTypes.JAW_CRUSHER_MENU.get() : ModMenuTypes.ELECTRIC_ALLOY_SMELTER_MENU.get(), id);
        this.crusher = crusher;
        Player player = inventory.player;
        access = ContainerLevelAccess.create(player.level(), pos);
        var entity = player.level().getBlockEntity(pos);
        var smelter = !player.level().isClientSide() && entity instanceof ElectricAlloySmelterBlockEntity machine ? machine : null;
        var jaw = !player.level().isClientSide() && entity instanceof br.com.quantumindustries.block.entity.JawCrusherBlockEntity machine ? machine : null;
        var items = jaw != null ? jaw.menuInventory() : smelter == null ? new SimpleContainer(crusher ? 2 : 3) : smelter.menuInventory();
        data = smelter == null && jaw == null ? new SimpleContainerData(5) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 4) return WorldResearchData.get((ServerLevel)player.level()).knows(player,
                        crusher ? "mineral_beneficiation" : "electric_alloy_smelting") ? 1 : 0;
                if (jaw != null) return switch (index) {
                    case 0 -> jaw.energy(); case 1 -> jaw.progress();
                    case 2 -> jaw.input().isEmpty() ? 0 : jaw.WORK_TICKS;
                    case 3 -> jaw.energy() >= jaw.FE_PER_TICK && jaw.statusState() == br.com.quantumindustries.block.entity.MachineStatusInspectable.State.WORKING ? 1 : 0;
                    default -> 0;
                };
                var recipe = AlloyCatalog.findMatch(smelter.inputA(), smelter.inputB(), 2);
                return switch (index) {
                    case 0 -> smelter.energy();
                    case 1 -> smelter.progress();
                    case 2 -> recipe == null ? 0 : recipe.workTicks();
                    case 3 -> smelter.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 5; }
        };
        for (int index = 0; index < inputCount(); index++) {
            final int inputSlot = index;
            addSlot(new Slot(items, index, crusher ? 139 : 112 + index * 54, 48) {
                @Override public int getMaxStackSize() { return crusher ? 64 : 16; }
                @Override public boolean mayPlace(ItemStack stack) {
                    if (!crusher) {
                        ItemStack other = getSlot(1 - inputSlot).getItem();
                        if (!other.isEmpty() && (other.is(stack.getItem()) || !AlloyCatalog.canPair(other, stack, 2))) return false;
                    }
                    return (crusher ? br.com.quantumindustries.material.CrushingCatalog.isValidInput(stack) : AlloyCatalog.isValidIngredient(stack, 2))
                            && (!(player.level() instanceof ServerLevel server)
                            || WorldResearchData.get(server).knows(player, crusher ? "mineral_beneficiation" : "electric_alloy_smelting"));
                }
            });
        }
        addSlot(new Slot(items, inputCount(), 263, 92) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, 9 + row * 9 + column, 83 + column * 18, 156 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 83 + column * 18, 214));
        addDataSlots(data);
    }

    public int energy() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int duration() { return data.get(2); }
    public boolean working() { return data.get(3) != 0; }
    public boolean permitted() { return data.get(4) != 0; }
    public boolean crusher() { return crusher; }
    public int inputCount() { return crusher ? 1 : 2; }
    public int capacity() { return crusher ? 480 : 1200; }
    public AlloyCatalog.Recipe recipe() { return crusher ? null : AlloyCatalog.findMatch(getSlot(0).getItem(), getSlot(1).getItem(), 2); }
    public ItemStack result() {
        return crusher ? br.com.quantumindustries.material.CrushingCatalog.resultFor(getSlot(0).getItem())
                : recipe() == null ? ItemStack.EMPTY : recipe().assemble();
    }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, crusher ? ModBlocks.JAW_CRUSHER.get() : ModBlocks.ELECTRIC_ALLOY_SMELTER.get()); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!crusher || button != 0 || !player.isAlive() || player.containerMenu != this || !stillValid(player)
                || !(player.level() instanceof ServerLevel server)
                || !WorldResearchData.get(server).knows(player, "mineral_beneficiation")
                || player.getInventory().countItem(br.com.quantumindustries.ModItems.HAND_CRANK.get()) == 0) return false;
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof br.com.quantumindustries.block.entity.JawCrusherBlockEntity jaw && jaw.manualStroke(), false);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var before = stack.copy();
        int machineSlots = inputCount() + 1;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean allowed = false;
            for (int slotIndex = 0; slotIndex < inputCount(); slotIndex++) allowed |= getSlot(slotIndex).mayPlace(stack);
            if (!allowed || !moveItemStackTo(stack, 0, inputCount(), false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return before;
    }
}
