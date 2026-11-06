package br.com.quantumindustries.mechanical;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.material.MaterialProcesses;
import br.com.quantumindustries.menu.*;
import br.com.quantumindustries.recipe.*;
import br.com.quantumindustries.research.*;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runs through the real server machinery in an isolated survival world with staged raw supplies. */
@EventBusSubscriber(modid="quantumindustries")
public final class MechanicalRuntimeVerification {
    private static final UUID ID=UUID.fromString("5eda599e-1064-4d26-af1a-1cf7de6c8c6a");
    private static final BlockPos SAW=new BlockPos(8,100,8), WHEEL=new BlockPos(4,100,8), LAB=new BlockPos(0,100,0);
    private static final BlockPos FAR=new BlockPos(2048,100,2048);
    private static ServerLevel level;
    private static FakePlayer player, guest;
    private static MechanicalSawmillBlockEntity saw;
    private static int step, checks, ticks, wood;
    private static int phaseStarted, pausedProgress;
    private static MechanicalSawmillBlockEntity farBeforeUnload;
    private static boolean active;
    private static final List<String> WOODS=List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","crimson","warped");
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    @SubscribeEvent public static void start(ServerStartedEvent event) throws Exception {
        if(!Boolean.getBoolean("quantumindustries.verifyMechanical"))return;
        level=event.getServer().overworld();level.setChunkForced(0,0,true);
        player=new FakePlayer(level,new GameProfile(ID,"MechanicalSurvivalQA"));
        guest=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"MechanicalGuest"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        guest.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        try {
            check(!player.isCreative(),"operator is survival, not creative");
            verifyRecipesAndMaterials();
            if(Boolean.getBoolean("quantumindustries.verifyMechanicalChunkOnly")) {
                WorldResearchData.get(level).unlockAll(player);setupChunkUnload();saw=farBeforeUnload;step=30;active=true;return;
            }
            if(Files.exists(Path.of("../restart-ready.txt"))) {
                player.getPersistentData().merge(net.minecraft.nbt.NbtIo.readCompressed(Path.of("../player-persistent.nbt"),net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
                player.setPos(8.5,100,9.5);guest.setPos(8.5,100,9.5);
                saw=(MechanicalSawmillBlockEntity)level.getBlockEntity(SAW);
                check(saw!=null && saw.input().getCount()==1 && saw.output().getCount()==6,"full server restart restores both inventory slots");
                check(saw.progress()>=35 && saw.progress()<100 && saw.installed(),"full server restart preserves partial work and installed crank");
                check(WorldResearchData.get(level).knows(player,"hydraulic_power"),"research persists across server restart");
                check(AssayProfile.of(player).knows(AssayCatalog.Material.IRON,AssayCatalog.Method.MECHANICAL),"empirical profile survives the player NBT round trip");
                check(!WorldResearchData.get(level).knows(guest,"mechanical_transmission"),"knowledge stays individual after restart");
                setFlow(true);addOverloadFixtures();pausedProgress=saw.progress();phaseStarted=ticks;step=9;
            } else {
                WorldResearchData.get(level).clearAll(player);
                player.setPos(.5,100,1.5);
                progressThroughScience();
                player.setPos(8.5,100,9.5);guest.setPos(8.5,100,9.5);
                level.setBlock(SAW,Blocks.AIR.defaultBlockState(),3);
                check(take(ModItems.MECHANICAL_SAWMILL.get()).getCount()==1,"place the sawmill crafted in survival");
                level.setBlock(SAW,ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState(),3);
                saw=(MechanicalSawmillBlockEntity)level.getBlockEntity(SAW);
                var crank=new ItemStack(ModItems.HAND_CRANK.get(),2);
                check(!saw.installCrank(guest,crank) && crank.getCount()==2,"unknown player cannot install a crank");
                check(saw.installCrank(player,crank) && crank.getCount()==1,"one crank paid per installation");
                check(!saw.installCrank(player,crank) && crank.getCount()==1,"duplicate install cannot consume another crank");
                var log=new ItemStack(Items.OAK_LOG,2);
                check(!saw.insert(guest,log) && log.getCount()==2,"unknown player's insertion is rejected without consuming wood");
                check(saw.insert(player,log) && log.getCount()==1,"authorized insertion consumes one log");
                check(!saw.turn(guest),"unknown player cannot turn another player's sawmill");
                check(saw.turn(player) && !saw.turn(player),"same-tick spam never accelerates processing");
                step=1;
            }
            active=true;
        }catch(Throwable error){finish(error);}
    }
    private static void verifyRecipesAndMaterials() {
        var recipes=MechanicalProcesses.all(level);
        check(recipes.size()==11,"all eleven JSON wood families load successfully");
        for(String name:WOODS){
            var logType=name.equals("crimson")||name.equals("warped")?"stem":"log";
            var log=BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+name+"_"+logType));
            var holder=MechanicalProcesses.find(level,new ItemStack(log));
            check(holder!=null,"loaded process for "+name);
            var recipe=holder.value();
            check(recipe.result().is(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+name+"_planks"))) && recipe.result().getCount()==6,"species-correct 50 percent yield "+name);
            check(recipe.inputs().size()==4 && recipe.ticks()==100 && recipe.mechanicalLoad()==6,"shared data covers stripped/log/wood variants "+name);
            for(var item:recipe.inputs())check(MechanicalProcesses.find(level,new ItemStack(item))!=null,"all declared alternatives execute");
        }
        check(MechanicalProcesses.find(level,new ItemStack(Items.COAL))==null,"non-wood cannot create planks");
        check(MaterialProcesses.supports(AssayCatalog.Material.IRON,MaterialProcesses.Process.TRANSMISSION),"iron process support");
        check(MaterialProcesses.supports(AssayCatalog.Material.GOLD,MaterialProcesses.Process.WIRE),"gold future ductility support");
        check(!MaterialProcesses.supports(null,MaterialProcesses.Process.SHEET),"unsupported material lookup is safe");
        check(MaterialProcesses.profile(new ItemStack(Items.OAK_LOG)).processes().contains(MaterialProcesses.Process.SAWING),"wood is suitable for the sawing process");
        check(MaterialProcesses.profile(new ItemStack(Items.STONE)).processes().contains(MaterialProcesses.Process.ABRASIVE),"stone supports future simple abrasive processes");
        check(MaterialProcesses.profile(new ItemStack(Items.COAL)).processes().contains(MaterialProcesses.Process.FUEL),"coal is a fuel rather than a shaft material");
    }
    private static void learn(String key) {
        player.setPos(.5,100,1.5);
        var research=WorldResearchData.get(level);
        var entity=(ResearchTableBlockEntity)level.getBlockEntity(LAB);
        var menu=new ResearchTableMenu(12,player.getInventory(),entity,new SimpleContainerData(ResearchTableMenu.DATA_SIZE));
        player.containerMenu=menu;
        int id=ResearchCatalog.indexOf(key);
        var before=new HashMap<Item,Integer>();
        for(var cost:ResearchCatalog.STUDIES.get(id).costs()) {
            before.put(cost.item(),player.getInventory().countItem(cost.item()));
            player.getInventory().add(new ItemStack(cost.item(),cost.amount()));
        }
        check(menu.clickMenuButton(player,id) && research.knows(player,key),"survival research payment and evidence: "+key);
        before.forEach((item,count)->check(player.getInventory().countItem(item)==count,"research consumes declared cost once "+key));
        player.containerMenu=player.inventoryMenu;
    }
    private static void assay(AssayCatalog.Material material,AssayCatalog.Method method,int offset) {
        BlockPos pos=LAB.east(offset);
        level.setBlock(pos,ModBlocks.EMPIRICAL_ASSAY_BENCH.get().defaultBlockState(),3);
        player.setPos(pos.getX()+.5,100,pos.getZ()+1.5);
        var bench=(EmpiricalAssayBenchBlockEntity)level.getBlockEntity(pos);
        if(bench.researchMaterial()==null)bench.inventory().setItem(0,material.sample());
        bench.inventory().setItem(1,new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
        var menu=new EmpiricalAssayBenchMenu(13,player.getInventory(),pos);
        player.containerMenu=menu;
        check(menu.clickMenuButton(player,1+method.ordinal()) && menu.clickMenuButton(player,0),"start actual assay menu "+material+"/"+method);
        menu.removed(player);player.containerMenu=player.inventoryMenu;
        for(int i=0;i<200;i++)bench.tick();
        AssayDelivery.of(level).deliver(player);
        check(AssayProfile.of(player).knows(material,method),"experiment grants the reusable individual discovery");
    }
    private static void progressThroughScience() {
        level.setBlock(LAB.east(2),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(LAB,ModBlocks.RESEARCH_TABLE_TIER_1.get().defaultBlockState(),3);
        check(!WorldResearchData.get(level).knows(player,"basic_mechanics"),"starts without research");
        learn("material_observation");
        assay(AssayCatalog.Material.IRON,AssayCatalog.Method.INSPECTION,2);
        assay(AssayCatalog.Material.IRON,AssayCatalog.Method.MECHANICAL,2);
        learn("material_malleability");learn("manual_forming");learn("basic_mechanics");learn("mechanical_transmission");
        var recipes=FoundationRecipe.previews(ResearchCatalog.indexOf("mechanical_transmission"));
        var sawPreview=recipes.stream().filter(p->p.output().is(ModItems.MECHANICAL_SAWMILL.get())).findFirst().orElseThrow();
        check(sawPreview.requiredResearch().equals(List.of("mechanical_transmission","metal_sheets")),"preview states all sawmill craft knowledge");
        check(!foundation("mechanical_sawmill").unlocked(player),"transmission alone does not hide a sheet-knowledge bypass");
        learn("metal_sheets");
        check(foundation("mechanical_sawmill").unlocked(player) && !foundation("mechanical_sawmill").unlocked(guest),"craft gate and preview agree per player");
        for(var item:List.of(Items.IRON_INGOT,Items.IRON_NUGGET,Items.OAK_PLANKS,Items.STICK))player.getInventory().add(new ItemStack(item,64));
        craft("iron_gear");craft("mechanical_shaft");craft("hand_crank");
        craft("manual_hammer");craft("forming_bench");formSheets();craft("iron_gear");craft("mechanical_sawmill");
        check(Integer.bitCount(AssayProfile.of(player).mask(AssayCatalog.Material.IRON))==2,"only relevant iron discoveries required, not four");
    }
    private static FoundationRecipe foundation(String path) {
        var holder=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.fromNamespaceAndPath("quantumindustries",path))).orElseThrow();
        return (FoundationRecipe)holder.value();
    }
    private static void craft(String path) {
        var recipe=foundation(path);
        FoundationRecipe.Preview preview=null;
        for(int i=0;i<ResearchCatalog.STUDIES.size();i++)for(var p:FoundationRecipe.previews(i))
            if(BuiltInRegistries.ITEM.getKey(p.output().getItem()).getPath().equals(path))preview=p;
        if(preview==null)throw new AssertionError("missing craft preview "+path);
        BlockPos table=LAB.south();level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        var menu=new CraftingMenu(8,player.getInventory(),ContainerLevelAccess.create(level,table));
        player.containerMenu=menu;
        int index=0;
        for(var ingredient:preview.ingredients()) {
            int x=index%preview.width(),y=index/preview.width();index++;
            menu.getSlot(1+y*3+x).set(ingredient.isEmpty()?ItemStack.EMPTY:take(ingredient.getItem()));
        }
        menu.slotsChanged(menu.getInputGridSlots().getFirst().container);
        check(menu.getSlot(0).getItem().is(preview.output().getItem()),"real survival crafting result "+path);
        var result=menu.getSlot(0).safeTake(preview.output().getCount(),preview.output().getCount(),player);
        check(result.getCount()==preview.output().getCount(),"craft produces correct count "+path);
        check(menu.getInputGridSlots().stream().allMatch(slot->slot.getItem().isEmpty()),"craft consumes each supplied ingredient once "+path);
        player.getInventory().add(result);player.containerMenu=player.inventoryMenu;
    }
    private static ItemStack take(Item item) {
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(item))return player.getInventory().removeItem(i,1);
        throw new AssertionError("Missing survival ingredient: "+BuiltInRegistries.ITEM.getKey(item));
    }
    private static void formSheets() {
        var hammer=take(ModItems.MANUAL_HAMMER.get());
        var original=player.getMainHandItem();
        player.setItemSlot(EquipmentSlot.MAINHAND,hammer);
        if(!original.isEmpty())player.getInventory().placeItemBackInInventory(original);
        var first=take(Items.IRON_INGOT);first.grow(take(Items.IRON_INGOT).getCount());
        player.setItemSlot(EquipmentSlot.OFFHAND,first);
        var pos=LAB.south(3);
        check(take(ModItems.FORMING_BENCH.get()).getCount()==1,"use the crafted forming bench");
        level.setBlock(pos,ModBlocks.FORMING_BENCH.get().defaultBlockState(),3);
        for(int i=0;i<6;i++){
            for(int t=0;t<9;t++)player.getCooldowns().tick();
            level.getBlockState(pos).useItemOn(hammer,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        }
        check(first.isEmpty() && player.getInventory().countItem(ModItems.IRON_SHEET.get())==2,"hammering two raw ingots yields the sawmill sheets");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if(!active)return;
        try {
            ticks++;
            if(ticks%100==0)Files.writeString(Path.of("../progress.txt"),"step="+step+" tick="+ticks+" wood="+wood+" input="+saw.input().getCount()+" output="+saw.output().getCount()+" progress="+saw.progress()+"\n");
            if(ticks>5000)throw new AssertionError("mechanical test timed out at step "+step);
            if(step==1) {
                if(ticks<22)check(saw.output().isEmpty(),"manual pulse never completes instantly");
                if(ticks==25) {check(saw.progress()<=20 && saw.sourceCapacity()==0 && !saw.rotating(),"manual pulse expires without interaction; partial="+saw.progress());step=2;}
            } else if(step==2) {
                if(ticks%15==0)saw.turn(player);
                if(saw.output().getCount()==6) {
                    check(saw.input().isEmpty(),"manual operation consumes exactly one log");
                    learn("hydraulic_power");craft("mechanical_shaft");craft("mechanical_shaft");craft("mechanical_shaft_block");craft("mechanical_shaft_block");craft("mechanical_shaft");craft("iron_gear");craft("iron_gear");craft("primitive_water_wheel");
                    player.setPos(8.5,100,9.5);
                    take(ModItems.PRIMITIVE_WATER_WHEEL.get());
                    level.setBlock(WHEEL,ModBlocks.PRIMITIVE_WATER_WHEEL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST),3);
                    for(int x=5;x<=7;x++){
                        take(ModItems.MECHANICAL_SHAFT_BLOCK.get());
                        level.setBlock(new BlockPos(x,100,8),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
                    }
                    var wheel=(PrimitiveWaterWheelBlockEntity)level.getBlockEntity(WHEEL);
                    level.setBlock(WHEEL.below(),Blocks.WATER.defaultBlockState(),3);
                    check(wheel.sourceCapacity()==0,"still water cannot power the wheel");
                    setFlow(true);check(wheel.sourceCapacity()==18,"moving rim contact yields 18 mechanical units");
                    var log=new ItemStack(Items.OAK_LOG);check(saw.insert(player,log),"insert second log for persistence");
                    step=3;
                }
            } else if(step==3 && saw.progress()>=40) {
                setFlow(false);
                var removed=saw.detachCrank(player);check(!removed.isEmpty(),"retrieve paid crank");
                check(saw.installCrank(player,removed),"reinstall without adding a manual pulse");
                check(!MechanicalNetwork.state(level,SAW).powered(),"no hidden electrical or infinite source remains");
                net.minecraft.nbt.NbtIo.writeCompressed(player.getPersistentData(),Path.of("../player-persistent.nbt"));
                Files.writeString(Path.of("../restart-ready.txt"),"PASS write: "+checks+" checks; partial="+saw.progress()+"\n");
                finish(null);
            } else if(step==9 && ticks-phaseStarted>=20) {
                check(saw.progress()==pausedProgress && !MechanicalNetwork.state(level,SAW).powered(),"four consumers exceed 18 units and all stall");
                var excess=(MechanicalSawmillBlockEntity)level.getBlockEntity(SAW.east(6));
                check(excess.extract(player,true).getCount()==1,"remove one load atomically");
                MechanicalNetwork.invalidate(level);check(MechanicalNetwork.state(level,SAW).load()==18,"three consumers share exactly 18 units");step=10;
            } else if(step==10 && saw.output().getCount()==12 && extra(2).output().getCount()==6 && extra(4).output().getCount()==6) {
                check(saw.input().isEmpty(),"restart resumes and completes exactly once");
                check(saw.extract(player,false).getCount()==12 && saw.extract(player,false).isEmpty(),"result withdrawal is atomic");
                check(extra(2).extract(player,false).getCount()==6 && extra(4).extract(player,false).getCount()==6,"all three admitted consumers receive the same operation speed");
                for(int x=9;x<=14;x++)level.destroyBlock(new BlockPos(x,100,8),false);
                wood=0;insertWood();step=11;
            } else if(step==11 && saw.output().getCount()==6) {
                var expected=BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+WOODS.get(wood)+"_planks"));
                check(saw.output().is(expected),"world processing produces the correct species "+WOODS.get(wood));
                check(saw.extract(player,false).getCount()==6,"extract correct output once");
                if(++wood<WOODS.size())insertWood();else{insertWoodForInterruption();step=12;}
            } else if(step==12 && saw.progress()>=30) {
                int before=saw.progress();
                check(saw.extract(guest,false).isEmpty() && saw.progress()==before,"output extraction during unfinished work cannot steal input or duplicate it");
                pausedProgress=before;setFlow(false);step=20;
            } else if(step==20) {
                check(saw.progress()==pausedProgress && !saw.rotating(),"removing water stops actual production");
                setFlow(true);
                level.destroyBlock(new BlockPos(6,100,8),false);
                check(!MechanicalNetwork.state(level,SAW).powered(),"removing a shaft disconnects immediately");
                Files.writeString(Path.of("../pause-progress.txt"),Integer.toString(pausedProgress));step=13;
            } else if(step==13) {
                int before=Integer.parseInt(Files.readString(Path.of("../pause-progress.txt")));
                check(saw.progress()==before && !saw.rotating(),"disconnected operation pauses without consuming wood");
                level.setBlock(new BlockPos(6,100,8),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Z),3);
                check(!MechanicalNetwork.state(level,SAW).powered(),"perpendicular shaft does not create a magical right-angle connection");
                level.setBlock(new BlockPos(6,100,8),ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
                MechanicalNetwork.invalidate(level);step=14;
            } else if(step==14 && saw.output().getCount()==6) {
                verifyBreakAndPersistence();
                setupChunkUnload();phaseStarted=ticks;step=30;
            } else if(step==30 && !level.hasChunkAt(FAR)) {
                check(MechanicalNetwork.state(level,FAR).nodes().isEmpty() && !level.hasChunkAt(FAR),"querying an unloaded network does not load its chunk");
                phaseStarted=ticks;step=31;
            } else if(step==31 && ticks-phaseStarted>=80) {
                check(!level.hasChunkAt(FAR),"remote chunk stays unloaded without machines requesting it");
                level.setChunkForced(128,128,true);level.getChunkAt(FAR);
                step=32;
            } else if(step==32) {
                var reloaded=(MechanicalSawmillBlockEntity)level.getBlockEntity(FAR);
                check(reloaded!=null,"block entity available after real chunk reload");
                check(!reloaded.isRemoved() && reloaded.input().getCount()==1 && reloaded.installed(),"real chunk reload restores input and crank: removed="+reloaded.isRemoved()+" input="+reloaded.input().getCount()+" crank="+reloaded.installed());
                check(MechanicalNetwork.state(level,FAR).capacity()==0,"expired/manual source is not recreated by chunk reload");
                String label=Boolean.getBoolean("quantumindustries.verifyMechanicalChunkOnly")?"chunk-only diagnostic":"survival-mode science/crafting, 11 wood families, manual and hydraulic drive, restart, chunk reload, overload and shared-machine safety";
                Files.writeString(Path.of("../complete.txt"),"PASS: "+checks+" server assertions, "+label+"\n");
                finish(null);
            } else if(step==30 && ticks-phaseStarted>1200) {
                throw new AssertionError("fixture chunk did not unload within 1200 ticks");
            }
        }catch(Throwable error){finish(error);}
    }
    private static void insertWood(){
        String name=WOODS.get(wood);String type=name.equals("crimson")||name.equals("warped")?"stem":"log";
        var stack=new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:"+name+"_"+type)));
        check(saw.insert(player,stack) && stack.isEmpty(),"automatic input accepted "+name);
    }
    private static void insertWoodForInterruption(){check(saw.insert(player,new ItemStack(Items.OAK_LOG)),"prepare interruption");}
    private static void setFlow(boolean on) {
        level.setBlock(WHEEL.below(),on?Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,8):Blocks.AIR.defaultBlockState(),3);
        level.setBlock(WHEEL.below().north(),on?Blocks.WATER.defaultBlockState():Blocks.STONE.defaultBlockState(),3);
        MechanicalNetwork.invalidate(level);
    }
    private static void verifyBreakAndPersistence(){
        check(saw.insert(player,new ItemStack(Items.OAK_LOG)),"stage a same-species input for full-buffer check");
        var data=saw.saveWithoutMetadata(level.registryAccess());
        data.getCompound("output").orElseThrow().putInt("count",64);
        var restored=new MechanicalSawmillBlockEntity(SAW,saw.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data));
        restored.setLevel(level);
        check(restored.output().getCount()==64 && restored.installed(),"machine save codec preserves a full output buffer and crank");
        restored.tick();check(restored.output().getCount()==64 && restored.input().getCount()==1 && restored.progress()==0,"full output buffer cannot consume a log or overfill");
        check(saw.extract(player,true).getCount()==1,"restore fixture input before shared-machine check");
        check(saw.insert(player,new ItemStack(Items.BIRCH_LOG)),"different input waits behind full species output");
        WorldResearchData.get(level).unlockAll(guest);
        check(saw.insert(guest,new ItemStack(Items.BIRCH_LOG)) && saw.input().getCount()==2,"two qualified server players share one atomic input queue");
        check(saw.extract(guest,true).getCount()==2 && saw.extract(player,true).isEmpty(),"competing input withdrawals return the queue exactly once");
        check(saw.insert(player,new ItemStack(Items.BIRCH_LOG)),"prepare final single input");
        int before=saw.progress();saw.tick();check(saw.progress()==before,"incompatible output never destroys or converts another species");
        int old=drops();level.destroyBlock(SAW,true);int delta=drops()-old;
        check(delta==8,"breaking returns six planks, one input and one paid crank exactly once");
        saw.preRemoveSideEffects(SAW,saw.getBlockState());check(drops()-old==8,"repeated removal cannot duplicate stored items");
    }
    private static MechanicalSawmillBlockEntity extra(int dx){return (MechanicalSawmillBlockEntity)level.getBlockEntity(SAW.east(dx));}
    private static void addOverloadFixtures(){
        for(int dx=1;dx<=6;dx++){
            var pos=SAW.east(dx);level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos,dx%2==0?ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState():
                    ModBlocks.MECHANICAL_SHAFT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),3);
            if(dx%2==0)check(extra(dx).insert(player,new ItemStack(Items.OAK_LOG)),"attach a load fixture");
        }
        MechanicalNetwork.invalidate(level);
    }
    private static void setupChunkUnload(){
        level.setChunkForced(128,128,true);level.getChunkAt(FAR);
        level.setBlock(FAR,Blocks.AIR.defaultBlockState(),3);
        level.setBlock(FAR,ModBlocks.MECHANICAL_SAWMILL.get().defaultBlockState(),3);
        farBeforeUnload=(MechanicalSawmillBlockEntity)level.getBlockEntity(FAR);
        player.setPos(FAR.getX()+.5,100,FAR.getZ()+1.5);
        check(farBeforeUnload.installCrank(player,new ItemStack(ModItems.HAND_CRANK.get())) && farBeforeUnload.insert(player,new ItemStack(Items.BIRCH_LOG)),"stage persistent remote chunk fixture");
        player.setPos(8.5,100,9.5);level.setChunkForced(128,128,false);
    }
    private static int drops(){return level.getEntitiesOfClass(ItemEntity.class,new AABB(SAW).inflate(2)).stream()
            .filter(entity->!entity.getItem().is(ModItems.MECHANICAL_SAWMILL.get())).mapToInt(entity->entity.getItem().getCount()).sum();}
    private static void finish(Throwable error) throws Exception {
        active=false;
        Files.writeString(Path.of("../result.txt"),error==null?"PASS: step="+step+" assertions="+checks+"\n":"FAIL: step="+step+" "+error+"\n");
        if(error!=null)error.printStackTrace();
        level.getServer().halt(false);
    }
}
