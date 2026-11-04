package br.com.quantumindustries.client;

import br.com.quantumindustries.core.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ClientMenus {
    private ClientMenus() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
    }
}
