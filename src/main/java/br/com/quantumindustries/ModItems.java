package br.com.quantumindustries;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(QuantumIndustries.MODID);

    // Analisador Científico - item principal do primeiro ciclo
    public static final DeferredItem<Item> SCIENTIFIC_ANALYZER = ITEMS.register("scientific_analyzer",
            () -> new Item(new Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}