package br.com.quantumindustries.core;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.QuantumIndustries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, QuantumIndustries.MODID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.quantumindustries.main"))
                .icon(() -> new ItemStack(ModItems.SCIENTIFIC_ANALYZER.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.SCIENTIFIC_ANALYZER.get());
                    output.accept(ModItems.EMPIRICAL_ASSAY_BENCH.get());
                    output.accept(ModItems.COPPER_WIRE.get());
                    output.accept(ModItems.TEST_PROBES.get());
                    output.accept(ModItems.RESEARCH_DATA.get());
                })
                .build());
    }

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
