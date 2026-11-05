package br.com.quantumindustries.assay;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Isolated preview world only; never compiled into the normal JAR. */
@EventBusSubscriber(modid="quantumindustries")
public final class AssayPreviewSetup {
    private static int ticks;
    public static volatile boolean installTools;
    private static boolean toolsInstalled;
    public static volatile int photoStage;
    private static int appliedPhotoStage;

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!Boolean.getBoolean("quantumindustries.previewAssay") || !(event.getEntity() instanceof ServerPlayer p)) return;
        if(installTools&&!toolsInstalled){
            var entity=p.level().getBlockEntity(new BlockPos(0,100,0));
            if(entity instanceof br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity b){
                b.inventory().setItem(1,new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
                b.inventory().setItem(2,new ItemStack(ModItems.MANUAL_BELLOWS.get()));
                b.inventory().setItem(3,new ItemStack(ModItems.ASSAY_FLASK.get()));toolsInstalled=true;
            }
        }
        if (photoStage!=appliedPhotoStage) {
            appliedPhotoStage=photoStage;
            p.openMenu(new SimpleMenuProvider((id,inv,who)->new EmpiricalAssayBenchMenu(id,inv,new BlockPos(0,100,0)),Component.literal("Assay QA")),new BlockPos(0,100,0));
        }
        ++ticks;
        if (ticks!=80 && ticks!=100) return;
        BlockPos pos=new BlockPos(0,100,0);
        if (ticks==80) {
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)p.level().setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            for(int x=-3;x<=5;x++)for(int z=-3;z<=3;z++)
                if(p.level().getBlockState(pos.offset(x,0,z)).is(ModBlocks.EMPIRICAL_ASSAY_BENCH.get()))
                    p.level().setBlock(pos.offset(x,0,z),Blocks.AIR.defaultBlockState(),2);
            p.level().setBlock(pos,ModBlocks.EMPIRICAL_ASSAY_BENCH.get().defaultBlockState(),2);
            p.level().setBlock(pos.east(),ModBlocks.EMPIRICAL_ASSAY_BENCH.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART,
                    net.minecraft.world.level.block.state.properties.BedPart.HEAD),2);
            for(var item:p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(8)))item.discard();
            p.connection.teleport(3.4,100,-2.8,35,18);
            p.getPersistentData().remove(AssayProfile.KEY);
            p.getInventory().clearContent();
            return;
        }
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new EmpiricalAssayBenchMenu(id,inv,pos),Component.literal("Assay QA")),pos);
        if(p.containerMenu instanceof EmpiricalAssayBenchMenu m) {
            m.getSlot(0).set(new ItemStack(Items.RAW_COPPER));
            m.broadcastChanges();
        }
    }
}
