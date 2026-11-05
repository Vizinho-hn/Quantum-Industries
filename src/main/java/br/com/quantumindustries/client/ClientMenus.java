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
    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(br.com.quantumindustries.core.ModBlocks.EMPIRICAL_ASSAY_ENTITY.get(),br.com.quantumindustries.client.renderer.EmpiricalAssayRenderer::new);
    }

    @SubscribeEvent
    public static void registerPictures(net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(br.com.quantumindustries.client.assay.AssayPictureState.class,br.com.quantumindustries.client.assay.AssayPictureRenderer::new);
    }
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
        event.register(ModMenuTypes.EMPIRICAL_ASSAY_MENU.get(), br.com.quantumindustries.client.gui.EmpiricalAssayBenchScreen::new);
    }
}
