package br.com.quantumindustries;

import br.com.quantumindustries.research.GuidebookCatalog;
import br.com.quantumindustries.research.ResearchCatalog;
import br.com.quantumindustries.research.ResearchReportItem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "quantumindustries")
public final class LocalizationRuntimeVerification {
    private static boolean ran;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void language(String locale) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (var input = LocalizationRuntimeVerification.class.getResourceAsStream(
                "/assets/quantumindustries/lang/" + locale + ".json")) {
            Language.loadFromJson(input, entries::put);
        }
        Language.inject(new Language() {
            public String getOrDefault(String key, String fallback) { return entries.getOrDefault(key, fallback); }
            public boolean has(String key) { return entries.containsKey(key); }
            public boolean isDefaultRightToLeft() { return false; }
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
            }
        });
    }

    private static void verify() throws Exception {
        var report = ResearchReportItem.create(ResearchCatalog.indexOf("conductivity"));
        var wire = ModItems.COPPER_WIRE.get().getDefaultInstance();
        var tooltip = new ArrayList<Component>();
        wire.getItem().appendHoverText(wire, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, tooltip::add, TooltipFlag.NORMAL);
        Component title = Component.translatable(GuidebookCatalog.introVault().title());
        for (String locale : new String[]{"en_us", "pt_br", "en_us"}) {
            language(locale);
            boolean pt = locale.equals("pt_br");
            check(wire.getHoverName().getString().equals(pt ? "Fio de cobre" : "Copper Wire"), "wire name " + locale);
            check(report.getHoverName().getString().equals(pt ? "Relatório: Condutividade" : "Report: Conductivity"), "report switches language " + locale);
            check(title.getString().equals(pt ? "O Vault Caído & Primeiros Passos" : "The Fallen Vault & First Steps"), "chapter switches language " + locale);
            check(tooltip.getFirst().getString().contains(pt ? "Condutor de Cobre" : "Copper Conductor"), "stored tooltip component switches language " + locale);
            check(tooltip.get(1).getString().contains("98% IACS") && !tooltip.get(1).getString().contains("98%%"), "literal percent formatting " + locale);
            for (var chapter : GuidebookCatalog.chapters()) {
                var keys = new ArrayList<>(chapter.paragraphs());
                keys.add(chapter.title()); keys.add(chapter.subtitle()); keys.add(chapter.operationSummary());
                for (String key : keys) check(!Component.translatable(key).getString().equals(key), "missing chapter " + key);
            }
            for (var study : ResearchCatalog.STUDIES) {
                for (String key : new String[]{study.name(), study.description(), study.reward()}) {
                    check(!Component.translatable(key).getString().equals(key), "missing study " + key);
                }
            }
            for (var item : BuiltInRegistries.ITEM) {
                if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("quantumindustries")) continue;
                var lines = new ArrayList<Component>();
                item.appendHoverText(item.getDefaultInstance(), Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
                for (var line : lines) {
                    String value = line.getString();
                    check(!value.contains("quantumindustries.") && !value.contains("%s"), "unresolved tooltip " + value);
                }
            }
        }
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("quantumindustries.verifyLocalization") || ran) return;
        ran = true;
        Language previous = Language.getInstance();
        String result;
        try {
            verify();
            result = "PASS: EN-US -> PT-BR -> EN-US; item names, report, tooltips, chapters and studies\n";
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL: " + error + "\n";
        } finally {
            Language.inject(previous);
        }
        Files.writeString(Path.of("../result.txt"), result);
        event.getServer().halt(false);
    }
}
