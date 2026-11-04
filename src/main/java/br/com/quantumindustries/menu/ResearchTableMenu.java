package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.IContainerFactory;

/**
 * Research-only menu. No item slots are exposed: research consumes materials
 * directly from the player's own inventory, validated on the server.
 */
public final class ResearchTableMenu extends AbstractContainerMenu implements IContainerFactory<ResearchTableMenu> {
    public static final int RESEARCH_CONDUCTIVITY = 0;
    public static final int RESEARCH_COPPER_WIRE = 1;

    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;

    public ResearchTableMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, resolveBlockEntity(inventory, extraData), new SimpleContainerData(2));
    }

    public ResearchTableMenu(int containerId, Inventory inventory, ResearchTableBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), containerId);
        this.data = data;
        this.levelAccess = entity == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inventory.player.level(), entity.getBlockPos());
        addDataSlots(data);
    }

    private static ResearchTableBlockEntity resolveBlockEntity(Inventory inventory, FriendlyByteBuf extraData) {
        if (extraData.readableBytes() < Long.BYTES) return null;
        BlockPos pos = extraData.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof ResearchTableBlockEntity entity
                ? entity : null;
    }

    public static ResearchTableMenu createMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        return new ResearchTableMenu(id, inventory, extraData);
    }

    @Override
    public ResearchTableMenu create(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        return createMenu(id, inventory, extraData);
    }

    public boolean hasConductivity() { return data.get(0) != 0; }
    public boolean hasCopperWire() { return data.get(1) != 0; }

    public boolean isRevealed(int id) {
        return id == RESEARCH_CONDUCTIVITY || id == RESEARCH_COPPER_WIRE && hasConductivity();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player) || !(player.level() instanceof ServerLevel serverLevel)) return false;
        WorldResearchData research = WorldResearchData.get(serverLevel);
        if (id == RESEARCH_CONDUCTIVITY) {
            if (research.hasConductivity() || !spend(player.getInventory(), new Cost(Items.COPPER_INGOT, 3),
                    new Cost(Items.REDSTONE, 1), new Cost(Items.PAPER, 1))) return false;
            research.unlockConductivity();
        } else if (id == RESEARCH_COPPER_WIRE) {
            if (!research.hasConductivity() || research.hasCopperWire() ||
                    !spend(player.getInventory(), new Cost(Items.COPPER_INGOT, 2),
                            new Cost(Items.PAPER, 1))) return false;
            research.unlockCopperWire();
        } else return false;
        broadcastChanges();
        return true;
    }

    private record Cost(Item item, int amount) {}

    /** Two-phase withdrawal: verify all item counts first, then consume atomically. */
    private static boolean spend(Inventory inventory, Cost... costs) {
        for (Cost cost : costs) {
            int found = 0;
            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                if (inventory.getItem(i).is(cost.item())) {
                    found += inventory.getItem(i).getCount();
                }
            }
            if (found < cost.amount()) return false;
        }
        for (Cost cost : costs) {
            int remaining = cost.amount();
            for (int i = 0; i < inventory.getContainerSize() && remaining > 0; ++i) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(cost.item())) {
                    int taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
        }
        inventory.setChanged();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.RESEARCH_TABLE.get());
    }
}
