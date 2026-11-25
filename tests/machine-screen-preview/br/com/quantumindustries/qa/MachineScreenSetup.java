package br.com.quantumindustries.qa;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.energy.CableNetwork;
import br.com.quantumindustries.logistics.*;
import br.com.quantumindustries.material.AlloyCatalog;
import br.com.quantumindustries.menu.MachineProcessingMenu;
import br.com.quantumindustries.research.WorldResearchData;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="quantumindustries")
public final class MachineScreenSetup {
    private static final BlockPos SMELTER=new BlockPos(0,100,0), CRUSHER=new BlockPos(3,100,0), BATTERY=new BlockPos(8,100,8);
    public static volatile int stage;
    public static volatile String failure;
    private static int ticks, checks;
    private static boolean crusherOpened;
    private static void check(boolean ok,String label){ checks++;if(!ok)throw new AssertionError(label); }
    private static void open(ServerPlayer player, BlockPos pos) {
        player.level().getBlockState(pos).useWithoutItem(player.level(),player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) throws Exception {
        if(!Boolean.getBoolean("quantumindustries.previewMachineScreen") || !(event.getEntity() instanceof ServerPlayer player) || failure!=null)return;
        try {
            if(++ticks==1)player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
            var level=(net.minecraft.server.level.ServerLevel)player.level();
            if(ticks==50){
                for(int x=-2;x<=5;x++)for(int z=-2;z<=3;z++){
                    level.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
                    for(int y=100;y<=103;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                player.getInventory().clearContent();
                player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                player.connection.teleport(1.5,100,2,180,20);
                WorldResearchData.get(level).unlockAll(player);
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                level.setBlock(SMELTER,ModBlocks.ELECTRIC_ALLOY_SMELTER.get().defaultBlockState(),3);
                level.setBlock(CRUSHER,ModBlocks.JAW_CRUSHER.get().defaultBlockState(),3);
                var smelter=(ElectricAlloySmelterBlockEntity)level.getBlockEntity(SMELTER);
                open(player,SMELTER);
                check(player.containerMenu instanceof MachineProcessingMenu,"normal smelter interaction opens its menu");
                var menu=(MachineProcessingMenu)player.containerMenu;
                menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT,3));
                menu.getSlot(4).set(new ItemStack(ModItems.INGOTS.get("tin").get()));
                menu.quickMoveStack(player,3);menu.quickMoveStack(player,4);
                check(smelter.inputA().getCount()==3 && smelter.inputB().getCount()==1,"shift click edits the real machine inputs");
                check(!menu.getSlot(2).mayPlace(new ItemStack(Items.STONE)),"output slot rejects insertion");
                var guest = new net.neoforged.neoforge.common.util.FakePlayer(level,
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "MenuGuestQA"));
                var guestMenu = new MachineProcessingMenu(999, guest.getInventory(), SMELTER, false);
                check(!guestMenu.getSlot(0).mayPlace(new ItemStack(Items.COPPER_INGOT)),"menu insertion requires the acting player's research");
                var recipe=AlloyCatalog.findMatch(smelter.inputA(),smelter.inputB(),2);
                check(recipe!=null,"existing alloy recipe recognized");
                for(int i=0;i<30;i++)smelter.receiveEnergy(64);
                for(int i=0;i<recipe.workTicks();i++)smelter.tick();
                check(smelter.output().getCount()==4 && smelter.energy()==1200-recipe.totalFe(),"recipe yield and FE cost preserved");
                int before=player.getInventory().countItem(recipe.assemble().getItem());
                menu.quickMoveStack(player,2);menu.quickMoveStack(player,2);
                check(smelter.output().isEmpty() && player.getInventory().countItem(recipe.assemble().getItem())==before+4,"output extraction happens exactly once");
                menu.getSlot(0).set(new ItemStack(Items.COPPER_INGOT,12));
                menu.getSlot(1).set(new ItemStack(ModItems.INGOTS.get("tin").get(),4));
                player.closeContainer();
                check(smelter.inputA().getCount()==12,"closing keeps stored inputs");
                open(player,CRUSHER);
                var crusher=(JawCrusherBlockEntity)level.getBlockEntity(CRUSHER);
                ((MachineProcessingMenu)player.containerMenu).getSlot(0).set(new ItemStack(Items.RAW_COPPER));
                check(!player.containerMenu.clickMenuButton(player,0),"manual button requires the actual crank");
                player.getInventory().add(new ItemStack(ModItems.HAND_CRANK.get()));
                check(player.containerMenu.clickMenuButton(player,0) && !player.containerMenu.clickMenuButton(player,0),"manual button enforces cooldown");
                for(int x=0;x<=2;x++)level.setBlock(BATTERY.east(x),Blocks.AIR.defaultBlockState(),3);
                for(int x=0;x<=2;x++)level.setBlock(BATTERY.east(x),x==1?ModBlocks.INSULATED_CONDUIT.get().defaultBlockState():ModBlocks.LEAD_ACID_ACCUMULATOR.get().defaultBlockState(),3);
                var cable=(ConfigurableConduit)level.getBlockEntity(BATTERY.east());
                for(var side:Direction.values())cable.connectionModes().set(side,ConnectionMode.NONE);
                cable.connectionModes().set(Direction.WEST,ConnectionMode.AUTO);
                cable.connectionModes().set(Direction.EAST,ConnectionMode.AUTO);cable.connectionChanged();
                var left=(LeadAcidAccumulatorBlockEntity)level.getBlockEntity(BATTERY);
                var right=(LeadAcidAccumulatorBlockEntity)level.getBlockEntity(BATTERY.east(2));
                left.receiveEnergy(64);CableNetwork.transfer(level,BATTERY,left::energy,left::extractEnergy,64);
                check(left.energy()==0 && right.energy()==64,"bidirectional endpoint receives energy into a battery");
                check(ConnectionMode.NONE.next()==ConnectionMode.AUTO,"bidirectional mode is included in wrench cycle");
            }
            if(ticks==58)check(player.containerMenu.clickMenuButton(player,0),"second manual stroke: menu="+player.containerMenu.getClass()+", pos="+player.position()+", crank="+player.getInventory().countItem(ModItems.HAND_CRANK.get())+", progress="+((JawCrusherBlockEntity)level.getBlockEntity(CRUSHER)).progress()+", time="+level.getGameTime());
            if(ticks==66){
                check(player.containerMenu.clickMenuButton(player,0),"third manual stroke");
                var crusher=(JawCrusherBlockEntity)level.getBlockEntity(CRUSHER);
                check(crusher.output().getCount()==2,"manual GUI preserves the three-stroke recipe");
                var right=(LeadAcidAccumulatorBlockEntity)level.getBlockEntity(BATTERY.east(2));
                level.setBlock(BATTERY,Blocks.AIR.defaultBlockState(),3);
                level.setBlock(BATTERY,ModBlocks.ELECTROMECHANICAL_PRESS.get().defaultBlockState(),3);
                right.tick();
                var consumer=(EnergyInspectable)level.getBlockEntity(BATTERY);
                check(consumer.energy()>0 && consumer.energy()+right.energy()==64,"same battery endpoint supplies a real consumer without creating FE");
                crusher.menuInventory().setItem(0,new ItemStack(Items.RAW_COPPER,12));
                for(int i=0;i<12;i++)crusher.receiveEnergy(48);
                open(player,SMELTER);stage=1;
                Files.writeString(Path.of("screen-server-checks.txt"),"PASS: "+checks+" menu and battery assertions\n");
            }
            if(stage==2 && !crusherOpened){open(player,CRUSHER);crusherOpened=true;}
        }catch(Throwable error){failure=error.toString();Files.writeString(Path.of("screen-server-checks.txt"),"FAIL: "+error);error.printStackTrace();}
    }
}
