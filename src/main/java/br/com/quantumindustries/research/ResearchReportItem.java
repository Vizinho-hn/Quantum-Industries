package br.com.quantumindustries.research;

import br.com.quantumindustries.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class ResearchReportItem extends Item {
    public ResearchReportItem(Properties properties) { super(properties); }

    public static ItemStack create(int research) {
        if (!ResearchCatalog.valid(research)) return ItemStack.EMPTY;
        var study = ResearchCatalog.STUDIES.get(research);
        ItemStack stack = new ItemStack(ModItems.RESEARCH_DATA.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("quantum_research_id", study.id().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.quantumindustries.research_report.named", Component.translatable(study.name())));
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String storedId = tag.getString("quantum_research_id")
                .orElseGet(() -> LegacyResearch.key(tag.getIntOr("quantum_research", -1)));
        var id = ResearchCatalog.identifier(storedId);
        WorldResearchData research = WorldResearchData.get(server);
        if (ResearchCatalog.find(id) == null) return message(player, "message.quantumindustries.report.blank");
        if (research.knows(player, id)) return message(player, "message.quantumindustries.report.known");
        if (!research.learn(player, id)) return message(player, "message.quantumindustries.report.requirements");
        stack.shrink(1);
        return message(player, "message.quantumindustries.report.learned");
    }

    private static InteractionResult message(Player player, String text) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(text));
        }
        return InteractionResult.SUCCESS;
    }
}
