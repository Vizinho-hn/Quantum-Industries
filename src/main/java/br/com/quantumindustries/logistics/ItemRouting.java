package br.com.quantumindustries.logistics;

import br.com.quantumindustries.block.entity.BellowsAlloyKilnBlockEntity;
import br.com.quantumindustries.block.entity.BellowsBrazierBlockEntity;
import br.com.quantumindustries.block.entity.ElectricAlloySmelterBlockEntity;
import br.com.quantumindustries.block.entity.ElectricFurnaceBlockEntity;
import br.com.quantumindustries.block.entity.ElectromechanicalPressBlockEntity;
import br.com.quantumindustries.block.entity.GravitationalSorterBlockEntity;
import br.com.quantumindustries.block.entity.JawCrusherBlockEntity;
import br.com.quantumindustries.block.entity.ManualGrainMillBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalConveyorBlockEntity;
import br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity;
import br.com.quantumindustries.block.entity.PneumaticTubeBlockEntity;
import br.com.quantumindustries.block.entity.SettlingVatBlockEntity;
import br.com.quantumindustries.block.entity.SteamBoilerBlockEntity;
import br.com.quantumindustries.block.entity.WireDrawingBenchBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-only inventory bridge. A packet is removed from its origin exactly once. */
public final class ItemRouting {
    private ItemRouting() {}

    public static boolean isEndpoint(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof MechanicalConveyorBlockEntity || entity instanceof ElectricSawmillBlockEntity || entity instanceof PneumaticTubeBlockEntity
                || entity instanceof GravitationalSorterBlockEntity
                || entity instanceof ElectromechanicalPressBlockEntity
                || entity instanceof JawCrusherBlockEntity
                || entity instanceof WireDrawingBenchBlockEntity
                || entity instanceof ElectricAlloySmelterBlockEntity
                || entity instanceof ElectricFurnaceBlockEntity
                || entity instanceof BellowsAlloyKilnBlockEntity
                || entity instanceof BellowsBrazierBlockEntity
                || entity instanceof MechanicalSawmillBlockEntity
                || entity instanceof ManualGrainMillBlockEntity
                || entity instanceof SteamBoilerBlockEntity
                || entity instanceof SettlingVatBlockEntity
                || entity instanceof Container;
    }

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
        if (blockEntity instanceof ElectricSawmillBlockEntity saw) return previewStack(saw.previewOutput(), maximum);
        if (blockEntity instanceof MechanicalSawmillBlockEntity saw) return previewStack(saw.output(), maximum);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) {
            return previewStack(press.output(), maximum);
        }
        if (blockEntity instanceof JawCrusherBlockEntity crusher) {
            return previewStack(crusher.output(), maximum);
        }
        if (blockEntity instanceof WireDrawingBenchBlockEntity wireBench) {
            return previewStack(wireBench.output(), maximum);
        }
        if (blockEntity instanceof ElectricFurnaceBlockEntity furnace) return previewStack(furnace.output(), maximum);
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

    /** Select a source slot only after checking the destination, without removing rejected items. */
    public static ItemStack extractMatching(ServerLevel level, BlockPos pos, int maximum, Predicate<ItemStack> accepted) {
        if (!level.hasChunkAt(pos) || maximum <= 0) return ItemStack.EMPTY;
        var entity = level.getBlockEntity(pos);
        if (entity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (entity instanceof AbstractFurnaceBlockEntity && i != 2) continue;
                ItemStack candidate = previewStack(container.getItem(i), maximum);
                if (candidate.isEmpty() || !accepted.test(candidate)) continue;
                ItemStack taken = container.removeItem(i, candidate.getCount());
                container.setChanged();
                return taken;
            }
            return ItemStack.EMPTY;
        }
        ItemStack candidate = preview(level, pos, maximum);
        return !candidate.isEmpty() && accepted.test(candidate) ? extract(level, pos, maximum) : ItemStack.EMPTY;
    }

    /** Bounded, non-mutating look-ahead; occupied tubes do not hide a downstream smelter. */
    public static boolean hasAcceptingRoute(ServerLevel level, BlockPos start, Direction inlet, ItemStack candidate) {
        record Step(BlockPos pos, Direction inlet) {}
        var queue = new java.util.ArrayDeque<Step>();
        var seen = new java.util.HashSet<Step>();
        queue.add(new Step(start, inlet));
        boolean foundSmelter = false;
        while (!queue.isEmpty() && seen.size() < 256) {
            var step = queue.removeFirst();
            if (!seen.add(step) || !(level.getBlockEntity(step.pos()) instanceof PneumaticTubeBlockEntity tube)) continue;
            for (Direction side : Direction.values()) {
                if (side == step.inlet() || !tube.connectionMode(side).allowsOutput()) continue;
                BlockPos next = step.pos().relative(side);
                if (!level.hasChunkAt(next)) continue;
                var target = level.getBlockEntity(next);
                if (target instanceof PneumaticTubeBlockEntity nextTube) {
                    if (nextTube.connectionMode(side.getOpposite()).allowsInput()) queue.add(new Step(next, side.getOpposite()));
                } else if (target instanceof SteamBoilerBlockEntity boiler) {
                    foundSmelter = true;
                    if (boiler.canInsertFuel(candidate, side.getOpposite())) return true;
                } else if (target instanceof ElectricAlloySmelterBlockEntity smelter) {
                    foundSmelter = true;
                    if (smelter.canInsertIngredient(candidate)) return true;
                } else if (target instanceof ElectricFurnaceBlockEntity furnace) {
                    foundSmelter = true;
                    if (furnace.canInsert(candidate)) return true;
                } else if (target instanceof ElectricSawmillBlockEntity saw) {
                    foundSmelter = true;
                    if (saw.canInsert(candidate)) return true;
                } else if (target instanceof MechanicalSawmillBlockEntity saw) {
                    foundSmelter = true;
                    if (saw.canInsertAutomated(candidate)) return true;
                } else if (target instanceof ElectromechanicalPressBlockEntity press && !press.automationAuthorized()
                        || target instanceof JawCrusherBlockEntity crusher && !crusher.automationAuthorized()
                        || target instanceof WireDrawingBenchBlockEntity wire && !wire.automationAuthorized()
                        || target instanceof BellowsAlloyKilnBlockEntity kiln && !kiln.automationAuthorized()
                        || target instanceof BellowsBrazierBlockEntity brazier && !brazier.automationAuthorized()
                        || target instanceof ManualGrainMillBlockEntity mill && !mill.automationAuthorized()
                        || target instanceof SettlingVatBlockEntity vat && !vat.automationAuthorized()) {
                    foundSmelter = true;
                } else if (isEndpoint(level, next)) {
                    // Preserve existing transport rules for other machine types.
                    return true;
                }
            }
        }
        return !foundSmelter;
    }

    /** Return rejected in-flight material to a reachable pull inventory; never drop or delete it. */
    public static boolean returnToSource(ServerLevel level, BlockPos start, Direction inlet, ItemStack packet) {
        record Step(BlockPos pos, Direction side) {}
        var queue = new java.util.ArrayDeque<Step>();
        var seen = new java.util.HashSet<BlockPos>();
        queue.add(new Step(start, inlet));
        while (!queue.isEmpty() && seen.size() < 256) {
            var step = queue.removeFirst();
            if (!seen.add(step.pos()) || !(level.getBlockEntity(step.pos()) instanceof PneumaticTubeBlockEntity tube)) continue;
            for (Direction side : Direction.values()) {
                if (step.side() != null && side != step.side()) continue;
                if (!tube.connectionMode(side).allowsInput()) continue;
                BlockPos next = step.pos().relative(side);
                if (!level.hasChunkAt(next)) continue;
                var target = level.getBlockEntity(next);
                if (target instanceof PneumaticTubeBlockEntity previous) {
                    if (previous.connectionMode(side.getOpposite()).allowsOutput()) queue.add(new Step(next, null));
                } else if (tube.connectionMode(side) == ConnectionMode.PULL && target instanceof Container
                        && !(target instanceof AbstractFurnaceBlockEntity)) {
                    if (insert(level, next, packet, side.getOpposite())) return true;
                }
            }
        }
        return false;
    }

    public static ItemStack extract(ServerLevel level, BlockPos pos, int maximum) {
        if (!level.hasChunkAt(pos) || maximum <= 0) return ItemStack.EMPTY;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElectricSawmillBlockEntity saw) return saw.extractOutput(maximum);
        if (blockEntity instanceof MechanicalSawmillBlockEntity saw) return saw.extractOutput(maximum);
        if (blockEntity instanceof ElectromechanicalPressBlockEntity press) return press.extractOutput(maximum);
        if (blockEntity instanceof JawCrusherBlockEntity crusher) return crusher.extractOutput(maximum);
        if (blockEntity instanceof WireDrawingBenchBlockEntity wireBench) return wireBench.extractOutput(maximum);
        if (blockEntity instanceof ElectricFurnaceBlockEntity furnace) return furnace.extractOutput(maximum);
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
        if (entity instanceof MechanicalConveyorBlockEntity conveyor) return conveyor.accept(packet, from);
        if (entity instanceof PneumaticTubeBlockEntity tube) return tube.accept(packet, from);
        if (entity instanceof GravitationalSorterBlockEntity sorter) return sorter.accept(packet, from);
        if (entity instanceof ElectricSawmillBlockEntity saw) return insertLoop(packet, saw::insert);
        if (entity instanceof MechanicalSawmillBlockEntity saw) return insertLoop(packet, saw::insertAutomated);
        if (entity instanceof ElectromechanicalPressBlockEntity press) return insertLoop(packet, press::insertIngot);
        if (entity instanceof JawCrusherBlockEntity crusher) return insertLoop(packet, crusher::insert);
        if (entity instanceof WireDrawingBenchBlockEntity wireBench) return insertLoop(packet, wireBench::insert);
        if (entity instanceof ElectricFurnaceBlockEntity furnace) return insertLoop(packet, furnace::insert);
        if (entity instanceof ElectricAlloySmelterBlockEntity smelter) return insertLoop(packet, smelter::insertIngredient);
        if (entity instanceof BellowsAlloyKilnBlockEntity kiln) return insertLoop(packet, kiln::insert);
        if (entity instanceof BellowsBrazierBlockEntity brazier) return insertLoop(packet, brazier::insert);
        if (entity instanceof ManualGrainMillBlockEntity mill) return insertLoop(packet, mill::insert);
        if (entity instanceof SettlingVatBlockEntity vat) return insertLoop(packet, vat::addIngredient);
        if (entity instanceof SteamBoilerBlockEntity boiler)
            return insertLoop(packet, stack -> boiler.canInsertFuel(stack, from) && boiler.addFuel(stack));
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
