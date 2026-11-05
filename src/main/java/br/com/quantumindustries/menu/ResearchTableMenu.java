package br.com.quantumindustries.menu;

import br.com.quantumindustries.block.entity.ResearchTableBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.research.WorldResearchData;
import br.com.quantumindustries.research.ResearchCatalog;
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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Research-only menu. No item slots are exposed: research consumes materials
 * directly from the player's own inventory, validated on the server.
 */
public final class ResearchTableMenu extends AbstractContainerMenu implements IContainerFactory<ResearchTableMenu> {



    public static final int REPORT_DEPOSIT = ResearchCatalog.STUDIES.size();
    public static final int REPORT_WITHDRAW = REPORT_DEPOSIT + 3;
    public static final int REPORT_WRITE = REPORT_WITHDRAW + 3;
    private static final Item[] SUPPLIES = {Items.BLACK_DYE, Items.FEATHER, Items.PAPER};

    public static final int DATA_SIZE = 3 + 2 * ResearchCatalog.STUDIES.size();
    private final ResearchTableBlockEntity entity;
    private final ContainerData data;
    private final ContainerLevelAccess levelAccess;
    private final Player owner;

    public ResearchTableMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, resolveBlockEntity(inventory, extraData), new SimpleContainerData(DATA_SIZE));
    }

    public ResearchTableMenu(int containerId, Inventory inventory, ResearchTableBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESEARCH_TABLE_MENU.get(), containerId);
        this.owner = inventory.player;
        this.entity = entity;
        this.data = new ContainerData() {
            @Override public int get(int index) {
                if (index < 0 || index >= DATA_SIZE) return 0;
                if (!(inventory.player.level() instanceof ServerLevel server)) return data.get(index);
                if (index < 3) return entity == null ? 0 : entity.reportSupplies().getItem(index).getCount();
                int id = (index - 3) / 2;
                WorldResearchData research = WorldResearchData.get(server);
                if (index % 2 == 1) {
                    if (research.knows(inventory.player, id)) return 2;
                    return research.prerequisites(inventory.player, id) ? 1 : 0;
                }
                int mask = 0;
                var study = ResearchCatalog.STUDIES.get(id);
                for (int i = 0; i < study.costs().size(); i++) {
                    var cost = study.costs().get(i);
                    boolean empirical = study.key().equals("material_malleability") && cost.item() == Items.COPPER_INGOT
                            && hasCopperObservation(inventory.player);
                    if (empirical || count(inventory, cost.item()) >= cost.amount()) mask |= 1 << i;
                }
                return mask;
            }
            @Override public void set(int index, int value) { data.set(index, value); }
            @Override public int getCount() { return DATA_SIZE; }
        };
        this.levelAccess = entity == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inventory.player.level(), entity.getBlockPos());
        addDataSlots(this.data);
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

    public boolean complete(int id) { return ResearchCatalog.valid(id) && data.get(3 + id * 2) == 2; }
    public boolean isRevealed(int id) { return ResearchCatalog.valid(id) && data.get(3 + id * 2) != 0; }
    public boolean hasCost(int id, int cost) {
        return ResearchCatalog.valid(id) && cost >= 0 && cost < ResearchCatalog.STUDIES.get(id).costs().size()
                && cost < Integer.SIZE && (data.get(4 + id * 2) & (1 << cost)) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player != owner || player.containerMenu != this || !stillValid(player)
                || !(player.level() instanceof ServerLevel serverLevel) || id < 0) return false;
        WorldResearchData research = WorldResearchData.get(serverLevel);
        if (id >= REPORT_DEPOSIT && id < REPORT_WITHDRAW) {
            return transferSupply(player, id - REPORT_DEPOSIT, true);
        }
        if (id >= REPORT_WITHDRAW && id < REPORT_WRITE) {
            return transferSupply(player, id - REPORT_WITHDRAW, false);
        }
        if (id >= REPORT_WRITE && id < REPORT_WRITE + ResearchCatalog.STUDIES.size()) {
            return writeReport(player, research, id - REPORT_WRITE);
        }
        if (!ResearchCatalog.valid(id)) return false;
        var study = ResearchCatalog.STUDIES.get(id);
        if (!research.canLearn(player, study.id())) return false;
        // An existing empirical copper observation can substitute for the deformation sample.
        boolean empirical = study.key().equals("material_malleability") && hasCopperObservation(player);
        Cost[] costs = study.costs().stream()
                .filter(cost -> !empirical || cost.item() != Items.COPPER_INGOT)
                .map(cost -> new Cost(cost.item(), cost.amount())).toArray(Cost[]::new);
        SpendPlan payment = prepareSpend(player.getInventory(), costs);
        if (payment == null || !payment.commit()) return false;
        if (!research.learn(player, study.id())) {
            payment.rollback();
            return false;
        }
        broadcastChanges();
        return true;
    }

    public int supply(int index) { return data.get(index); }

    private boolean transferSupply(Player player, int index, boolean deposit) {
        if (entity == null || index < 0 || index >= SUPPLIES.length) return false;
        SimpleContainer supplies = entity.reportSupplies();
        ItemStack stored = supplies.getItem(index);
        Inventory inventory = player.getInventory();
        if (deposit) {
            Item item = SUPPLIES[index];
            int amount = Math.min(new ItemStack(item).getMaxStackSize() - stored.getCount(), count(inventory, item));
            SpendPlan payment = prepareSpend(inventory, new Cost(item, amount));
            if (amount <= 0 || payment == null || !payment.commit()) return false;
            supplies.setItem(index, new ItemStack(item, stored.getCount() + amount));
        } else {
            if (stored.isEmpty()) return false;
            ItemStack remaining = stored.copy();
            inventory.add(remaining);
            if (remaining.getCount() == stored.getCount()) return false;
            supplies.setItem(index, remaining);
        }
        broadcastChanges();
        return true;
    }

    private boolean writeReport(Player player, WorldResearchData research, int id) {
        if (entity == null || !research.knows(player, id)) return false;
        ItemStack report = br.com.quantumindustries.research.ResearchReportItem.create(id);
        if (report.isEmpty()) return false;
        SimpleContainer supplies = entity.reportSupplies();
        for (int i = 0; i < SUPPLIES.length; i++) {
            if (!supplies.getItem(i).is(SUPPLIES[i])) return false;
        }
        for (int i = 0; i < SUPPLIES.length; i++) supplies.removeItem(i, 1);
        player.getInventory().placeItemBackInInventory(report);
        broadcastChanges();
        return true;
    }

    private static boolean hasCopperObservation(Player player) {
        return br.com.quantumindustries.assay.AssayProfile.of(player).knows(
                br.com.quantumindustries.assay.AssayCatalog.Material.COPPER,
                br.com.quantumindustries.assay.AssayCatalog.Method.MECHANICAL);
    }

    private static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    record Cost(Item item, int amount) {}

    private record Debit(int slot, Item item, int amount, ItemStack before) {}

    static final class SpendPlan {
        private final Inventory inventory;
        private final List<Debit> debits;
        private boolean committed;

        private SpendPlan(Inventory inventory, List<Debit> debits) {
            this.inventory = inventory;
            this.debits = debits;
        }

        boolean commit() {
            if (committed) return false;
            for (Debit debit : debits) {
                ItemStack stack = inventory.getItem(debit.slot());
                if (!stack.is(debit.item()) || stack.getCount() < debit.amount()) return false;
            }
            for (Debit debit : debits) inventory.getItem(debit.slot()).shrink(debit.amount());
            inventory.setChanged();
            committed = true;
            return true;
        }

        void rollback() {
            if (!committed) return;
            for (Debit debit : debits) inventory.setItem(debit.slot(), debit.before().copy());
            inventory.setChanged();
            committed = false;
        }
    }

    static SpendPlan prepareSpend(Inventory inventory, Cost... costs) {
        if (inventory == null || costs == null) return null;
        Map<Item, Long> totals = new IdentityHashMap<>();
        for (Cost cost : costs) {
            if (cost == null || cost.item() == null || cost.amount() <= 0) return null;
            totals.merge(cost.item(), (long) cost.amount(), Long::sum);
        }

        List<Debit> debits = new ArrayList<>();
        for (var entry : totals.entrySet()) {
            long remaining = entry.getValue();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.is(entry.getKey())) continue;
                int amount = (int) Math.min(remaining, stack.getCount());
                if (amount > 0) {
                    debits.add(new Debit(slot, entry.getKey(), amount, stack.copy()));
                    remaining -= amount;
                }
            }
            if (remaining > 0) return null;
        }
        return new SpendPlan(inventory, List.copyOf(debits));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public boolean isTierOne() {
        return entity != null && entity.getBlockState().is(ModBlocks.RESEARCH_TABLE_TIER_1.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && stillValid(levelAccess, player, isTierOne()
                ? ModBlocks.RESEARCH_TABLE_TIER_1.get() : ModBlocks.RESEARCH_TABLE.get());
    }
}
