package br.com.quantumindustries.assay;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Real dedicated-server regressions, opt-in via tests/assay.init.gradle. */
@EventBusSubscriber(modid = "quantumindustries")
public final class AssayRuntimeVerification {
    private static int checks;
    private static final BlockPos BENCH = new BlockPos(0, 100, 0);
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static FakePlayer player(ServerLevel level) {
        FakePlayer p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "AssayTest"));
        p.setPos(.5, 100, .5);
        return p;
    }
    private static EmpiricalAssayBenchMenu open(FakePlayer p, BlockPos pos) {
        p.level().setBlock(pos, ModBlocks.EMPIRICAL_ASSAY_BENCH.get().defaultBlockState(), 2);
        EmpiricalAssayBenchMenu m = new EmpiricalAssayBenchMenu(1, p.getInventory(), pos);
        p.containerMenu = m;
        return m;
    }
    private static void tools(EmpiricalAssayBenchMenu m) {
        m.getSlot(1).set(new ItemStack(ModItems.MAGNIFYING_GLASS.get()));
        m.getSlot(2).set(new ItemStack(Items.FLINT_AND_STEEL));
        m.getSlot(3).set(new ItemStack(ModItems.BRASS_WEIGHTS.get()));
    }
    private static void ticks(FakePlayer p, int n) {
        for (int i=0; i<n; i++) NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }
    private static void finish(FakePlayer p, EmpiricalAssayBenchMenu m, AssayCatalog.Method method) {
        check(m.clickMenuButton(p, 1+method.ordinal()), "select method");
        check(m.clickMenuButton(p, 0), "start valid test");
        ticks(p, 59);
        check(!m.knows(m.reservedMaterial(), method), "no result before 60 ticks");
        ticks(p, 1);
    }
    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyAssay")) return;
        Path result=Path.of("../result.txt");
        try {
            run(event.getServer().overworld());
            Files.writeString(result, "PASS: "+checks+" dedicated-server assertions\n");
            System.out.println("ASSAY_VERIFICATION_PASS "+checks);
        } catch (Throwable failure) {
            Files.writeString(result, "FAIL: "+failure+"\n");
            failure.printStackTrace();
        } finally {
            event.getServer().halt(false);
        }
    }
    private static void verifyBenchStorage(ServerLevel level) {
        BlockPos pos=BENCH.offset(12,0,0);
        FakePlayer p=player(level);p.setPos(12.5,100,.5);
        var menu=open(p,pos);tools(menu);menu.getSlot(0).set(new ItemStack(Items.IRON_INGOT));
        menu.removed(p);
        check(p.getInventory().countItem(ModItems.MAGNIFYING_GLASS.get())==0,"closing does not return tools");
        var reopened=open(p,pos);
        check(reopened.getSlot(0).getItem().is(Items.IRON_INGOT),"unreserved iron remains on bench");
        for(int i=1;i<4;i++)check(!reopened.getSlot(i).getItem().isEmpty(),"tool retained "+i);
        var bench=(br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity)level.getBlockEntity(pos);
        var saved=bench.saveWithFullMetadata(level.registryAccess());
        var loaded=(br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity)
            net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,bench.getBlockState(),saved,level.registryAccess());
        check(loaded!=null,"bench NBT loads");
        for(int i=0;i<4;i++)check(ItemStack.isSameItemSameComponents(bench.inventory().getItem(i),loaded.inventory().getItem(i)),"saved slot "+i);
        var viewer=player(level);viewer.setPos(12.5,100,.5);var shared=open(viewer,pos);
        shared.getSlot(0).set(new ItemStack(Items.GOLD_INGOT));
        check(reopened.getSlot(0).getItem().is(Items.GOLD_INGOT),"viewers share physical inputs");
        shared.getSlot(0).set(ItemStack.EMPTY);
        check(reopened.getSlot(0).getItem().isEmpty(),"withdrawal cannot duplicate sample");
        shared.getSlot(0).set(new ItemStack(Items.GOLD_INGOT));
        var box=new net.minecraft.world.phys.AABB(pos).inflate(2);
        int before=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).size();
        level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box);
        check(drops.size()-before==4,"breaking bench drops exactly four stored stacks");
        check(drops.stream().filter(e->e.getItem().is(Items.GOLD_INGOT)).mapToInt(e->e.getItem().getCount()).sum()==1,"gold dropped once");
        check(!reopened.stillValid(p),"breaking closes access");
    }
    private static void run(ServerLevel level) throws Exception {
        // The verification world is reused. Reset only the test fixtures, not menu-open behavior.
        for(int offset:new int[]{0,3,12}) {
            var existing=level.getBlockEntity(BENCH.offset(offset,0,0));
            if(existing instanceof br.com.quantumindustries.block.entity.EmpiricalAssayBenchBlockEntity bench)
                bench.inventory().clearContent();
        }
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BENCH).inflate(16)))item.discard();
        verifyBenchStorage(level);
        check(AssayCatalog.Material.values().length==10, "10 materials");
        check(AssayCatalog.Method.values().length==3, "3 methods");
        for (var rejected : new net.minecraft.world.item.Item[]{Items.CHARCOAL, Items.RAW_COPPER, Items.RAW_IRON, Items.RAW_GOLD, Items.COPPER_BLOCK, Items.COAL_BLOCK, Items.DIAMOND_BLOCK})
            check(AssayCatalog.material(new ItemStack(rejected))==null, "reject "+rejected);
        FakePlayer p=player(level), other=player(level);
        EmpiricalAssayBenchMenu m=open(p,BENCH);
        check(!m.clickMenuButton(p,0), "no sample/tools");
        check(!m.clickMenuButton(other,0), "other player cannot use menu");
        check(!m.clickMenuButton(p,999), "invalid button");
        for (var material:AssayCatalog.Material.values()) {
            check(AssayCatalog.material(material.sample())==material, "sample matches "+material);
            m.clickMenuButton(p,10+material.ordinal());
            m.getSlot(0).set(material.sample());
            m.getSlot(1).set(ItemStack.EMPTY);
            m.clickMenuButton(p,1);
            check(!m.clickMenuButton(p,0), "magnifier mandatory");
            check(!m.getSlot(0).getItem().isEmpty(), "invalid start cannot reserve");
            tools(m);
            m.getSlot(3).set(ItemStack.EMPTY);
            check(!m.clickMenuButton(p,0), "weights mandatory for press");
            tools(m);
            m.clickMenuButton(p,2);
            m.getSlot(2).set(ItemStack.EMPTY);
            check(!m.clickMenuButton(p,0), "flint and steel mandatory for heating");
            tools(m);
            m.clickMenuButton(p,1);
            check(m.clickMenuButton(p,0), "start press");
            check(m.getSlot(0).getItem().isEmpty(), "reserve exactly one");
            check(m.reservedMaterial()==material, "reserve identity");
            check(!m.clickMenuButton(p,0), "duplicate start rejected");
            ticks(p,30);
            check(!m.knows(material,AssayCatalog.Method.MECHANICAL), "no early grant");
            m.getSlot(1).set(ItemStack.EMPTY);
            ticks(p,1);
            check(!m.running(), "lost tools cancel timer");
            check(m.reservedMaterial()==material, "interruption retains reserve");
            tools(m);
            finish(p,m,AssayCatalog.Method.MECHANICAL);
            check(m.knows(material,AssayCatalog.Method.MECHANICAL), "press discovered");
            check(!m.clickMenuButton(p,0), "known test cannot grant again");
            m.removed(p);
            check(m.getSlot(1).getItem().is(ModItems.MAGNIFYING_GLASS.get()), "tools remain on bench after close");
            BlockPos second=BENCH.offset(3,0,0);
            p.setPos(3.5,100,.5);
            m=open(p,second);
            check(m.reservedMaterial()==material, "resume at another bench");
            tools(m);
            finish(p,m,AssayCatalog.Method.HEATING);
            check(m.knows(material,AssayCatalog.Method.HEATING), "heating discovered");
            m.getSlot(2).set(ItemStack.EMPTY);m.getSlot(3).set(ItemStack.EMPTY);
            finish(p,m,AssayCatalog.Method.GALVANIC);
            check(m.knows(material,AssayCatalog.Method.GALVANIC), "galvanic with magnifier only");
            check(m.reservedMaterial()==null, "third result consumes reserve");
            m.clickMenuButton(p,10+material.ordinal());
            check(m.discoveries()==3, "material complete");
            p.setPos(.5,100,.5);
            m.removed(p);m=open(p,BENCH);
        }
        check(m.totalDiscoveries()==30, "all 30 discoveries");
        var independent=open(other,BENCH);
        check(independent.totalDiscoveries()==0 && independent.reservedMaterial()==null,"player independence");
        for (int legacy=-1;legacy<=4;legacy++) {
            FakePlayer old=player(level);old.getPersistentData().putInt("quantum_empirical_copper",legacy);
            var migrated=open(old,BENCH);
            check(migrated.totalDiscoveries()==Math.clamp(legacy,0,3),"legacy migration clamp "+legacy);
            for(var material:AssayCatalog.Material.values()) for(var method:AssayCatalog.Method.values())
                check(migrated.knows(material,method)==(material==AssayCatalog.Material.COPPER&&method.ordinal()<Math.clamp(legacy,0,3)),"exact migration");
            migrated.removed(old);migrated=open(old,BENCH);
            check(migrated.totalDiscoveries()==Math.clamp(legacy,0,3),"idempotent migration");
        }
        FakePlayer pending=player(level);var interrupted=open(pending,BENCH);tools(interrupted);
        interrupted.getSlot(0).set(new ItemStack(Items.COPPER_INGOT,2));
        interrupted.clickMenuButton(pending,1);check(interrupted.clickMenuButton(pending,0),"stack start");
        check(interrupted.getSlot(0).getItem().getCount()==1,"only one removed from stack");
        ticks(pending,20);interrupted.removed(pending);
        Path save=Path.of("../profile.nbt");NbtIo.writeCompressed(pending.getPersistentData(),save);
        CompoundTag disk=NbtIo.readCompressed(save,NbtAccounter.unlimitedHeap());
        FakePlayer reconnected=player(level);reconnected.getPersistentData().merge(disk);
        var resumed=open(reconnected,BENCH);
        check(resumed.reservedMaterial()==AssayCatalog.Material.COPPER,"disk/reconnect reserve");
        check(!resumed.running()&&resumed.progressTicks()==0,"timer not persisted");
        tools(resumed);finish(reconnected,resumed,AssayCatalog.Method.MECHANICAL);
        FakePlayer clone=player(level);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone,reconnected,true));
        var cloned=open(clone,BENCH);
        check(cloned.knows(AssayCatalog.Material.COPPER,AssayCatalog.Method.MECHANICAL),"death preserves discovery");
        check(cloned.reservedMaterial()==AssayCatalog.Material.COPPER,"death preserves cycle");
        tools(cloned);cloned.clickMenuButton(clone,2);cloned.clickMenuButton(clone,0);
        clone.setPos(100,100,100);ticks(clone,60);
        check(!cloned.knows(AssayCatalog.Material.COPPER,AssayCatalog.Method.HEATING),"lost access prevents grant");
    }
}
