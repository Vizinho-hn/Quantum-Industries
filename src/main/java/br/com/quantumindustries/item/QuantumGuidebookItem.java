package br.com.quantumindustries.item;

import br.com.quantumindustries.menu.QuantumGuidebookMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class QuantumGuidebookItem extends Item {
    public QuantumGuidebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new QuantumGuidebookMenu(id, inv),
                    Component.translatable("item.quantumindustries.quantum_guidebook")
            ));
        }
        return InteractionResult.SUCCESS;
    }
}
