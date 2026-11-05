package br.com.quantumindustries.dynamo;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class DynamoPreviewSetup {
    public static volatile int stage;
    private static int applied;
    private static int ticks;
    private static final BlockPos POS = new BlockPos(0, 100, 0);

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.previewDynamo")
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (++ticks == 60) {
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 4; z++) {
                player.level().setBlock(POS.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                player.level().setBlock(POS.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 3);
            }
            player.level().setBlock(POS.offset(0, 3, 2), Blocks.LIGHT.defaultBlockState(), 3);
            player.level().setBlock(POS.offset(3, 2, 0), Blocks.LIGHT.defaultBlockState(), 3);
            player.level().setBlock(POS, ModBlocks.MANUAL_DYNAMO.get().defaultBlockState(), 3);
            player.connection.teleport(2.15, 100, 3.4, 150, 20);
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            stage = 1;
        }
        if (stage == applied) return;
        applied = stage;
        if (!(player.level().getBlockEntity(POS) instanceof ManualDynamoBlockEntity dynamo)) return;
        if (stage == 2) {
            dynamo.install(false);
            dynamo.turn();
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));
        }
        if (stage == 3) dynamo.turn();
        if (stage == 4) player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        if (stage == 5) player.setItemSlot(EquipmentSlot.MAINHAND,
                new ItemStack(ModItems.PRIMITIVE_MECHANICAL_CORE.get()));
    }
}
