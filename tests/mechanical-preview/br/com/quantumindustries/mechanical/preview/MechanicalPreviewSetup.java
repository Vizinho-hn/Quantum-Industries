package br.com.quantumindustries.mechanical.preview;

import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.MechanicalNetwork;
import br.com.quantumindustries.research.WorldResearchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Visual fixture only. Survival acceptance runs separately without administrative research unlocks. */
@EventBusSubscriber(modid="quantumindustries")
public final class MechanicalPreviewSetup {
    public static volatile int stage;
    private static int ticks, applied;
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!Boolean.getBoolean("quantumindustries.previewMechanical") || !(event.getEntity() instanceof ServerPlayer player))return;
        var level=player.level();
        if(ticks==0)player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SPECTATOR);
        if(++ticks==60){
            for(int x=-6;x<=18;x++)for(int z=-8;z<=12;z++)for(int y=97;y<=105;y++)
                level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
            for(int x=-2;x<=7;x++)for(int z=-3;z<=5;z++){
                level.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                for(int y=100;y<=104;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
            }
            level.setBlock(new BlockPos(0,100,0),ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),3);
            for(int x=1;x<=2;x++)level.setBlock(new BlockPos(x,100,0),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
            level.setBlock(new BlockPos(3,100,0),ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState(),3);
            level.setBlock(new BlockPos(3,100,3),ModBlocks.RESEARCH_TABLE_TIER_1.get().defaultBlockState(),3);
            level.setBlock(new BlockPos(0,99,0),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8),3);
            level.setBlock(new BlockPos(0,99,-1),Blocks.WATER.defaultBlockState(),3);
            level.setBlock(new BlockPos(2,103,1),Blocks.LIGHT.defaultBlockState(),3);
            player.connection.teleport(4.6,100,-3.4,43,22);
            WorldResearchData.get(level).unlockAll(player);
            var saw=(MechanicalSawmillBlockEntity)level.getBlockEntity(new BlockPos(3,100,0));
            var logs=new ItemStack(Items.OAK_LOG,16);
            for(int i=0;i<16;i++)saw.insert(player,logs);
            stage=1;
        }
        if(applied==stage)return;
        applied=stage;
        if(stage==2)level.destroyBlock(new BlockPos(2,100,0),false);
        if(stage==3){
            BlockPos pos=new BlockPos(3,100,3);
            player.connection.teleport(3.5,100,2,0,0);
            player.openMenu((MenuProvider)level.getBlockEntity(pos),pos);
        }
        MechanicalNetwork.invalidate(level);
    }
}
