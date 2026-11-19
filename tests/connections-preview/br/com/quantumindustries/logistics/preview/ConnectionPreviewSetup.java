package br.com.quantumindustries.logistics.preview;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class ConnectionPreviewSetup {
    public static volatile int stage;
    private static int applied = -1, ticks;
    public static final BlockPos PIPE = new BlockPos(8, 100, 8);

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.previewConnections") || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (ticks++ == 0) player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
        if (ticks == 50) {
            var level = player.level();
            for (int x = 8; x <= 13; x++) for (int z = 5; z <= 11; z++) {
                level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 100; y <= 104; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(PIPE, ModBlocks.GLASS_PIPE.get().defaultBlockState(), 3);
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ENERGY_INSPECTION_GOGGLES.get()));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONFIGURATION_WRENCH.get()));
            player.connection.teleport(11, 100, 8.5, 90, 26.5f);
            stage = 1;
        }
        if (stage == 0 || stage == applied) return;
        applied = stage;
        var conduit = (ConfigurableConduit) player.level().getBlockEntity(PIPE);
        var mode = switch (stage) {
            case 1 -> ConnectionMode.PULL;
            case 2 -> ConnectionMode.PUSH;
            case 3 -> ConnectionMode.NONE;
            default -> ConnectionMode.AUTO;
        };
        conduit.connectionModes().set(Direction.EAST, mode);
        conduit.connectionChanged();
    }
}
