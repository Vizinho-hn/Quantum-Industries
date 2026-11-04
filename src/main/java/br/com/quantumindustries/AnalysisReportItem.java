package br.com.quantumindustries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class AnalysisReportItem extends Item {
    public AnalysisReportItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            CompoundTag root = player.getPersistentData();
            if (!root.contains(Player.PERSISTED_NBT_TAG))
                root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
            CompoundTag saved = root.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
            boolean known = saved.getIntOr("quantumResearch0", 0) >= 1;
            if (!known) saved.putInt("quantumResearch0", 1);
            player.sendSystemMessage(Component.literal(known
                    ? "Você já conhece a condutividade do cobre. Pode compartilhar este relatório."
                    : "Descoberta registrada: condutividade do cobre. Desenvolva a aplicação na aba Fabricação."));
        }
        return InteractionResult.SUCCESS;
    }
}


