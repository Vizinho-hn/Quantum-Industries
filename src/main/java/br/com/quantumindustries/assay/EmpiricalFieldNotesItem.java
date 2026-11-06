package br.com.quantumindustries.assay;

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

/** Server-authoritative, portable record of a complete 4/4 assay. */
public final class EmpiricalFieldNotesItem extends Item {
    private static final String MATERIAL_TAG = "quantum_assay_material";

    public EmpiricalFieldNotesItem(Properties properties) { super(properties); }

    public static ItemStack create(AssayCatalog.Material material) {
        ItemStack stack = new ItemStack(ModItems.EMPIRICAL_FIELD_NOTES.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(MATERIAL_TAG, material.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.quantumindustries.empirical_field_notes.named",
                Component.translatable(material.translationKey())));
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(MATERIAL_TAG).orElse("");
        AssayCatalog.Material material = null;
        for (var candidate : AssayCatalog.Material.values()) {
            if (candidate.id().equals(id)) { material = candidate; break; }
        }
        if (material == null) {
            player.displayClientMessage(Component.translatable("message.quantumindustries.field_notes.invalid"), true);
            return InteractionResult.SUCCESS;
        }
        if (!AssayProfile.of(player).learnComplete(material)) {
            player.displayClientMessage(Component.translatable("message.quantumindustries.field_notes.known"), true);
            return InteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.quantumindustries.field_notes.learned",
                Component.translatable(material.translationKey())), true);
        return InteractionResult.SUCCESS;
    }
}
