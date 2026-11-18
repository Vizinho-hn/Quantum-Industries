package br.com.quantumindustries.logistics;

import br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity;
import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.GravitationalSorterBlockEntity;
import br.com.quantumindustries.block.entity.JawCrusherBlockEntity;
import br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.block.entity.SettlingVatBlockEntity;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-only inventory bridge. A packet is removed from its origin exactly once. */
public final class ItemRouting {
    private ItemRouting() {}

    private static ItemStack previewStack(ItemStack stack, int maximum) {
        return stack.isEmpty() ? ItemStack.EMPTY
                : stack.copyWithCount(Math.min(maximum, stack.getCount()));
    }

    private static boolean insertLoop(ItemStack packet, Predicate<ItemStack> inserter) {
        int before = packet.getCount();
        while (!packet.isEmpty() && inserter.test(packet)) {}
        return packet.getCount() != before;
    }

    public static ItemStack preview(ServerLevel level, BlockPos pos, int maximum) {
        if (!level.hasChunkAt(pos) || maximum <= 0) return ItemStack.EMPTY;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) {
            return previewStack(press.output(), maximum);
        }
        if (blockEntity instanceof JawCrusherBlockEntity crusher) {
            return previewStack(crusher.output(), maximum);
        }
        if (blockEntity instanceof WireDrawingBenchBlockEntity wireBench) {
            return previewStack(wireBench.output(), maximum);
        }
        if (blockEntity instanceof ElectricAlloySmelterBlockEntity smelter) {
            return previewStack(smelter.output(), maximum);
        }
        if (blockEntity instanceof BellowsAlloyKilnBlockEntity kiln) {
            return previewStack(kiln.output(), maximum);
        }
        if (blockEntity instanceof BellowsBrazierBlockEntity brazier) {
            return previewStack(brazier.output(), maximum);
        }
        if (blockEntity instanceof ManualGrainMillBlockEntity mill) {
            return previewStack(mill.output(), maximum);
        }
        if (blockEntity instanceof SettlingVatBlockEntity vat) {
            return previewStack(vat.outputPreview(), maximum);
        }
        if (blockEntity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                // Extract the furnace product, not its fuel or unprocessed ingredients.
                if (blockEntity instanceof AbstractFurnaceBlockEntity && i != 2) continue;
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) return previewStack(stack, maximum);
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extract(ServerLevel level, BlockPos pos, int maximum) {
        if (!level.hasChunkAt(pos) || maximum <= 0) return ItemStack.EMPTY;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) return press.extractOutput(maximum);
        if (blockEntity instanceof JawCrusherBlockEntity crusher) return crusher.extractOutput(maximum);
        if (blockEntity instanceof WireDrawingBenchBlockEntity wireBench) return wireBench.extractOutput(maximum);
        if (blockEntity instanceof ElectricAlloySmelterBlockEntity smelter) return smelter.extractOutput(maximum);
        if (blockEntity instanceof BellowsAlloyKilnBlockEntity kiln) return kiln.extractOutput(maximum);
        if (blockEntity instanceof BellowsBrazierBlockEntity brazier) return brazier.extractOutput(maximum);
        if (blockEntity instanceof ManualGrainMillBlockEntity mill) return mill.extractOutput(maximum);
        if (blockEntity instanceof SettlingVatBlockEntity vat) return vat.extractOutput(maximum);
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
        if (entity instanceof ElectromechanicalPressBlockEntity press) return insertLoop(packet, press::insertIngot);
        if (entity instanceof JawCrusherBlockEntity crusher) return insertLoop(packet, crusher::insert);
        if (entity instanceof WireDrawingBenchBlockEntity wireBench) return insertLoop(packet, wireBench::insert);
        if (entity instanceof ElectricAlloySmelterBlockEntity smelter) return insertLoop(packet, smelter::insertIngredient);
        if (entity instanceof BellowsAlloyKilnBlockEntity kiln) return insertLoop(packet, kiln::insert);
        if (entity instanceof BellowsBrazierBlockEntity brazier) return insertLoop(packet, brazier::insert);
        if (entity instanceof ManualGrainMillBlockEntity mill) return insertLoop(packet, mill::insert);
        if (entity instanceof SettlingVatBlockEntity vat) return insertLoop(packet, vat::addIngredient);
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
