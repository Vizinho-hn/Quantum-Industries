package br.com.quantumindustries.assay;

import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="quantumindustries")
public final class AssayEvents {
    private AssayEvents(){}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        var player=event.getEntity();
        if(!player.level().isClientSide()&&player.containerMenu instanceof EmpiricalAssayBenchMenu menu)menu.serverTick(player);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        // This profile is independent from both vanilla inventory and existing research.
        event.getOriginal().getPersistentData().getCompound(AssayProfile.KEY).ifPresent(profile->
            event.getEntity().getPersistentData().put(AssayProfile.KEY,profile.copy()));
        // A player who has not yet opened a new bench still needs their legacy count.
        event.getOriginal().getPersistentData().getInt("quantum_empirical_copper").ifPresent(count->
            event.getEntity().getPersistentData().putInt("quantum_empirical_copper",count));
    }
}
