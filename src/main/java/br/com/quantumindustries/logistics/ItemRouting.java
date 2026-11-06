package br.com.quantumindustries.logistics;

import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.block.entity.GravitationalSorterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Server-only inventory bridge. A packet is removed from its origin exactly once. */
public final class ItemRouting {
    private ItemRouting() {}

    public static ItemStack preview(ServerLevel level, BlockPos pos, int maximum) {
        if (!level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) {
            return press.output().isEmpty() ? ItemStack.EMPTY
                    : press.output().copyWithCount(Math.min(maximum, press.output().getCount()));
        }
        if (blockEntity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                // Extract the furnace product, not its fuel or unprocessed ingredients.
                if (blockEntity instanceof AbstractFurnaceBlockEntity && i != 2) continue;
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) return stack.copyWithCount(Math.min(maximum, stack.getCount()));
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extract(ServerLevel level, BlockPos pos, int maximum) {
        if (!level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) return press.extractOutput(maximum);
        if (blockEntity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (blockEntity instanceof AbstractFurnaceBlockEntity && i != 2) continue;
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    ItemStack result = container.removeItem(i, Math.min(maximum, stack.getCount()));
                    container.setChanged();
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Mutates the passed packet to the remainder. Machine outputs and vanilla
     * containers never generate copies when a destination rejects part of it.
     */
    public static boolean insert(ServerLevel level, BlockPos pos, ItemStack packet, Direction from) {
        if (packet.isEmpty() || !level.hasChunkAt(pos)) return false;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof PneumaticTubeBlockEntity tube) return tube.accept(packet, from);
        if (entity instanceof GravitationalSorterBlockEntity sorter) return sorter.accept(packet, from);
        if (entity instanceof ElectromechanicalPressBlockEntity press) {
            int before = packet.getCount();
            while (!packet.isEmpty() && press.insertIngot(packet)) {}
            return packet.getCount() != before;
        }
        if (entity instanceof Container container) {
            int before = packet.getCount();
            for (int i = 0; i < container.getContainerSize() && !packet.isEmpty(); i++) {
                if (!container.canPlaceItem(i, packet)) continue;
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot, packet)) continue;
                int room = Math.min(container.getMaxStackSize(), packet.getMaxStackSize()) - slot.getCount();
                int transfer = Math.min(room, packet.getCount());
                if (transfer <= 0) continue;
                if (slot.isEmpty()) container.setItem(i, packet.copyWithCount(transfer));
                else slot.grow(transfer);
                packet.shrink(transfer);
                container.setChanged();
            }
            return packet.getCount() < before;
        }
        return false;
    }
}
