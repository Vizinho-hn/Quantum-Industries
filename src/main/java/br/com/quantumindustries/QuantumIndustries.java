package br.com.quantumindustries;

import br.com.quantumindustries.core.ModCreativeTabs;
import br.com.quantumindustries.core.ModMenuTypes;
import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.recipe.ModRecipes;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(QuantumIndustries.MODID)
public class QuantumIndustries {
    public static final String MODID = "quantumindustries";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuantumIndustries(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Quantum Industries - Common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Quantum Industries - Server starting");
    }
}
