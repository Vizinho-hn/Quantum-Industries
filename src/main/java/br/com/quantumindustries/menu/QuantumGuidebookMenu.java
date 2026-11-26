package br.com.quantumindustries.menu;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.AssayCatalog;
import br.com.quantumindustries.assay.AssayProfile;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.ResearchCatalog;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class QuantumGuidebookMenu extends AbstractContainerMenu {
    private static final int EXTRA_FIELDS = 4;
    private static final int FIELD_HAS_ASSAY_BENCH = 0;
    private static final int FIELD_HAS_RESERVED_SAMPLE = 1;
    private static final int FIELD_DISCOVERY_COUNT = 2;
    private static final int FIELD_HAS_ANALYZER = 3;

    private final int studyCount = ResearchCatalog.STUDIES.size();
    private final int[] synced = new int[studyCount + EXTRA_FIELDS];

    public QuantumGuidebookMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.QUANTUM_GUIDEBOOK_MENU.get(), containerId);
        Player owner = inventory.player;
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 0 || index >= synced.length) return 0;
                if (owner.level() instanceof ServerLevel server) {
                    if (index < studyCount) {
                        return WorldResearchData.get(server).knows(owner, index) ? 1 : 0;
                    }
                    int extra = index - studyCount;
                    return switch (extra) {
                        case FIELD_HAS_ASSAY_BENCH -> (hasItem(owner, ModItems.EMPIRICAL_ASSAY_BENCH.get())
                                || countDiscoveries(owner) > 0
                                || AssayProfile.of(owner).reserved() != null) ? 1 : 0;
                        case FIELD_HAS_RESERVED_SAMPLE -> AssayProfile.of(owner).reserved() != null ? 1 : 0;
                        case FIELD_DISCOVERY_COUNT -> countDiscoveries(owner);
                        case FIELD_HAS_ANALYZER -> (hasItem(owner, ModItems.SCIENTIFIC_ANALYZER_TIER_1.get())
                                || hasItem(owner, ModItems.SCIENTIFIC_ANALYZER.get())) ? 1 : 0;
                        default -> 0;
                    };
                }
                return synced[index];
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < synced.length) {
                    synced[index] = value;
                }
            }

            @Override
            public int getCount() {
                return synced.length;
            }
        };
        addDataSlots(data);
    }

    private static boolean hasItem(Player player, Item item) {
        return player.getInventory().countItem(item) > 0;
    }

    private static int countDiscoveries(Player player) {
        AssayProfile profile = AssayProfile.of(player);
        int total = 0;
        for (AssayCatalog.Material material : AssayCatalog.Material.values()) {
            total += Integer.bitCount(profile.mask(material));
        }
        return total;
    }

    public boolean knows(int studyIndex) {
        return studyIndex >= 0 && studyIndex < studyCount && synced[studyIndex] == 1;
    }

    public boolean knows(String studyKey) {
        return knows(ResearchCatalog.indexOf(studyKey));
    }

    /** Authoritative crafting/research unlock state for a chapter. */
    public boolean isUnlocked(GuidebookCatalog.Chapter chapter) {
        if (chapter == null) return false;
        return chapter.requiredStudies().stream().allMatch(key -> knows(ResearchCatalog.indexOf(key)));
    }

    /**
     * Manual visibility: in a new world, Material Analyzer instructions are readable immediately
     * so the player knows how to research and build their first laboratory without lifting any
     * real crafting/research gate.
     */
    public boolean isPageAvailable(GuidebookCatalog.Chapter chapter) {
        if (chapter == null) return false;
        return "material_observation".equals(chapter.id()) || isUnlocked(chapter);
    }

    public boolean hasAssayBench() { return synced[studyCount + FIELD_HAS_ASSAY_BENCH] != 0; }
    public boolean hasReservedSample() { return synced[studyCount + FIELD_HAS_RESERVED_SAMPLE] != 0; }
    public int discoveryCount() { return synced[studyCount + FIELD_DISCOVERY_COUNT]; }
    public boolean hasResearchAnalyzer() { return synced[studyCount + FIELD_HAS_ANALYZER] != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
