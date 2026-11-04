package br.com.quantumindustries;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = QuantumIndustries.MODID, dist = Dist.CLIENT)
public class QuantumClient {
    public QuantumClient(IEventBus bus) { bus.addListener(this::screens); }
    private void screens(RegisterMenuScreensEvent event) {
        event.register(ModItems.RESEARCH_MENU.get(), ResearchScreen::new);
    }
}
