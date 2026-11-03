package br.com.quantumindustries;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Primeira interação de protótipo: pesquisas por itens nas duas mãos.
 * Ainda não é a HUD definitiva, nem salva progresso individual.
 */
public final class ResearchTableBlock extends Block {
    public ResearchTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal(
                    "Mesa de Pesquisa: segure dois ingredientes, um em cada mão, e clique na mesa."));
            player.sendSystemMessage(Component.literal(
                    "Papel+Carvão | Ferro+Carvão | Cobre+Redstone | Quartzo+Ametista"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack offhand = player.getOffhandItem();
        String research = identify(held.getItem(), offhand.getItem());
        if (research == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal(
                        "Pesquisa indisponível. Use papel+carvão, ferro+carvão, cobre+redstone ou quartzo+ametista."));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                offhand.shrink(1);
            }
            player.sendSystemMessage(Component.literal("Pesquisa experimental concluída: " + research));
            player.sendSystemMessage(Component.literal(
                    "Protótipo: conclusão não é salva. A HUD e a persistência serão implementadas na próxima etapa."));
        }
        return InteractionResult.SUCCESS;
    }

    private static String identify(Item first, Item second) {
        if (matches(first, second, Items.PAPER, Items.COAL)) return "Fundamentos";
        if (matches(first, second, Items.IRON_INGOT, Items.COAL)) return "Metalurgia";
        if (matches(first, second, Items.COPPER_INGOT, Items.REDSTONE)) return "Condutividade";
        if (matches(first, second, Items.QUARTZ, Items.AMETHYST_SHARD)) return "Cristalografia";
        return null;
    }

    private static boolean matches(Item a, Item b, Item expectedA, Item expectedB) {
        return (a == expectedA && b == expectedB) || (a == expectedB && b == expectedA);
    }
}
