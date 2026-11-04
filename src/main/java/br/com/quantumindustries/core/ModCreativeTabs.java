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
                    output.accept(ModItems.SCIENTIFIC_ANALYZER_TIER_1.get());
                    output.accept(ModItems.SCIENTIFIC_ANALYZER.get());
                    output.accept(ModItems.EMPIRICAL_ASSAY_BENCH.get());
                    output.accept(ModItems.COPPER_WIRE.get());
                    output.accept(ModItems.TEST_PROBES.get());
                    output.accept(ModItems.RESEARCH_DATA.get());
                    output.accept(ModItems.MANUAL_HAMMER.get());
                    output.accept(ModItems.IRON_SHEET.get());
                    output.accept(ModItems.COPPER_SHEET.get());
                    output.accept(ModItems.IRON_GEAR.get());
                    output.accept(ModItems.MECHANICAL_SHAFT.get());
                    output.accept(ModItems.HAND_CRANK.get());
                    output.accept(ModItems.INSULATED_COPPER_WIRE.get());
                    output.accept(ModItems.COPPER_COIL.get());
                    output.accept(ModItems.ELECTROMECHANICAL_FRAME.get());
                    output.accept(ModItems.FORMING_BENCH.get());
                    output.accept(ModItems.MANUAL_DYNAMO.get());
                    output.accept(ModItems.MAGNIFYING_GLASS.get());
                    output.accept(net.minecraft.world.item.Items.FLINT_AND_STEEL);
                    output.accept(ModItems.BRASS_WEIGHTS.get());
                })
                .build());
    }

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
