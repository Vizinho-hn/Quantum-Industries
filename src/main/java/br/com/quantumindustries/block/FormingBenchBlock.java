package br.com.quantumindustries.block;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FormingBenchBlock extends Block {
    public FormingBenchBlock(Properties properties) { super(properties); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            server.sendOverlayMessage(Component.literal("Martelo na mão principal e lingote na outra. Bata 3 vezes."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useItemOn(ItemStack hammer, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !hammer.is(ModItems.MANUAL_HAMMER.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack metal = player.getOffhandItem();
        int material = metal.is(Items.IRON_INGOT) ? 1 : metal.is(Items.COPPER_INGOT) ? 2 : 0;
        if (material == 0 || !WorldResearchData.get(serverLevel).knows(player, 5)) {
            server.sendOverlayMessage(Component.literal(material == 0 ? "Segure ferro ou cobre na mão secundária."
                    : "Pesquise Chapas Metálicas antes de martelar."));
            return InteractionResult.SUCCESS;
        }
        if (player.getCooldowns().isOnCooldown(hammer)) return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(hammer, 8);
        CompoundTag work = hammer.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int strikes = work.getIntOr("forming_metal", 0) == material
                ? Math.max(0, Math.min(2, work.getIntOr("forming_strikes", 0))) + 1 : 1;
        work.putInt("forming_metal", material);
        work.putInt("forming_strikes", strikes == 3 ? 0 : strikes);
        hammer.set(DataComponents.CUSTOM_DATA, CustomData.of(work));
        if (strikes == 3) {
            metal.shrink(1);
            ItemStack sheet = new ItemStack(material == 1 ? ModItems.IRON_SHEET.get() : ModItems.COPPER_SHEET.get());
            if (!player.addItem(sheet) && !sheet.isEmpty()) player.drop(sheet, false);
        }
        hammer.hurtAndBreak(1, player, hand);
        server.sendOverlayMessage(Component.literal(strikes == 3 ? "Chapa pronta."
                : "Conformação: " + strikes + " / 3 golpes"));
        return InteractionResult.SUCCESS;
    }
}
