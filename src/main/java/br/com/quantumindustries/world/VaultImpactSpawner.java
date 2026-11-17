package br.com.quantumindustries.world;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.QuantumIndustries;
import br.com.quantumindustries.block.QuantumVaultBlock;
import br.com.quantumindustries.block.entity.QuantumVaultBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = QuantumIndustries.MODID)
public final class VaultImpactSpawner {
    private VaultImpactSpawner() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel server)) return;
        if (server.dimension() != Level.OVERWORLD) return;

        WorldResearchData data = WorldResearchData.get(server);
        if (data.isVaultSpawned()) return;

        BlockPos target = BlockPos.containing(player.getX() + 12.0, player.getY(), player.getZ() + 8.0);
        BlockPos surface = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        BlockPos basinFloor = surface.below();

        RandomSource random = RandomSource.create(surface.asLong());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floorPos = basinFloor.offset(dx, 0, dz);
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                BlockState scorched;
                if (dx == 0 && dz == 0) {
                    scorched = Blocks.MAGMA_BLOCK.defaultBlockState();
                } else if (dist == 1) {
                    scorched = (dx + dz) % 2 == 0
                            ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState();
                } else {
                    scorched = random.nextBoolean()
                            ? Blocks.BASALT.defaultBlockState()
                            : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                }
                server.setBlock(floorPos, scorched, Block.UPDATE_ALL);
                BlockPos abovePos = floorPos.above();
                if (dx != 0 || dz != 0) {
                    server.setBlock(abovePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        server.setBlock(surface.offset(1, 0, 1), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
        server.setBlock(surface.offset(-1, 0, -1), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
        server.setBlock(surface, ModBlocks.QUANTUM_VAULT.get().defaultBlockState().setValue(QuantumVaultBlock.OPEN, true), Block.UPDATE_ALL);
        if (server.getBlockEntity(surface) instanceof QuantumVaultBlockEntity vaultBe) {
            vaultBe.setInitialItem(new ItemStack(ModItems.QUANTUM_GUIDEBOOK.get()));
        }
        server.playSound(null, surface, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8f, 0.7f);

        data.markVaultSpawned();
        if (player instanceof net.minecraft.server.level.ServerPlayer op) {
            op.sendOverlayMessage(Component.translatable("message.quantumindustries.vault_landed"));
        }
    }
}
