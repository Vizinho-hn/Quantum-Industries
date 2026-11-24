package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity;
import br.com.quantumindustries.core.*;
import br.com.quantumindustries.recipe.MechanicalProcesses;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ElectricSawmillMenu extends AbstractContainerMenu {
  private final ContainerLevelAccess access;
  private final ContainerData data;

  public ElectricSawmillMenu(int id, Inventory inv, BlockPos pos) {
    super(ModMenuTypes.ELECTRIC_SAWMILL_MENU.get(), id);
    access = ContainerLevelAccess.create(inv.player.level(), pos);
    var be = inv.player.level().getBlockEntity(pos);
    var saw =
        be instanceof ElectricSawmillBlockEntity s && !inv.player.level().isClientSide() ? s : null;
    Container items = saw == null ? new SimpleContainer(8) : saw.inventory();
    data =
        saw == null
            ? new SimpleContainerData(6)
            : new ContainerData() {
              public int get(int i) {
                return switch (i) {
                  case 0 -> saw.energy();
                  case 1 -> saw.progress();
                  case 2 -> saw.activeSlot();
                  case 3 -> saw.working() ? 1 : 0;
                  case 4 -> saw.stateCode();
                  case 5 -> saw.transportFlags();
                  default -> 0;
                };
              }

              public void set(int i, int v) {}

              public int getCount() {
                return 6;
              }
            };
    for (int i = 0; i < 8; i++) {
      final int slot = i;
      addSlot(
          new Slot(items, i, (i < 4 ? 54 : 226) + (i % 2) * 20, 48 + (i % 4 / 2) * 20) {
            public boolean mayPlace(ItemStack stack) {
              if (slot >= 4) return false;
              if (!(inv.player.level() instanceof ServerLevel l)) return true;
              return WorldResearchData.get(l).knows(inv.player, "electric_wood_processing")
                  && MechanicalProcesses.find(l, stack) != null;
            }

            public int getMaxStackSize() {
              return 64;
            }
          });
    }
    for (int r = 0; r < 3; r++)
      for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 83 + c * 18, 156 + r * 18));
    for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 83 + c * 18, 214));
    addDataSlots(data);
  }

  public int stateCode() {
    return data.get(4);
  }

  public int transportFlags() {
    return data.get(5);
  }

  public int activeSlot() {
    return data.get(2);
  }

  public int energy() {
    return data.get(0);
  }

  public int progress() {
    return data.get(1);
  }

  public boolean working() {
    return data.get(3) != 0;
  }

  public boolean stillValid(Player p) {
    return stillValid(access, p, ModBlocks.ELECTRIC_SAWMILL.get());
  }

  public ItemStack quickMoveStack(Player p, int index) {
    if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    var stack = slot.getItem();
    var copy = stack.copy();
    if (index < 8) {
      if (!moveItemStackTo(stack, 8, slots.size(), true)) return ItemStack.EMPTY;
    } else {
      if (!getSlot(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 4, false))
        return ItemStack.EMPTY;
    }
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(p, stack);
    if (index < 4)
      access.execute(
          (level, pos) -> {
            if (level.getBlockEntity(pos) instanceof ElectricSawmillBlockEntity saw)
              saw.invalidateActive(index);
          });
    return copy;
  }
}
