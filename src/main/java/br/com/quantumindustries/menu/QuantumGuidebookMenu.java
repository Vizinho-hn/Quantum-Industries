package br.com.quantumindustries.menu;

import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.ResearchCatalog;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public final class QuantumGuidebookMenu extends AbstractContainerMenu {
    private final int[] studies = new int[ResearchCatalog.STUDIES.size()];

    public QuantumGuidebookMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.QUANTUM_GUIDEBOOK_MENU.get(), containerId);
        Player owner = inventory.player;
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 0 || index >= studies.length) return 0;
                if (owner.level() instanceof ServerLevel server) {
                    return WorldResearchData.get(server).knows(owner, index) ? 1 : 0;
                }
                return studies[index];
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < studies.length) {
                    studies[index] = value;
                }
            }

            @Override
            public int getCount() {
                return studies.length;
            }
        };
        addDataSlots(data);
    }

    public boolean knows(int studyIndex) {
        return studyIndex >= 0 && studyIndex < studies.length && studies[studyIndex] == 1;
    }

    public boolean isUnlocked(GuidebookCatalog.Chapter chapter) {
        if (chapter == null) return false;
        if (chapter.studyKey() == null) return true;
        int idx = ResearchCatalog.indexOf(chapter.studyKey());
        return knows(idx);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
