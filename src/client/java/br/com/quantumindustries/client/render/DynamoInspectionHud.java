package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class DynamoInspectionHud {
    private DynamoInspectionHud() {}

    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath("quantumindustries", "energy_inspection"),
                (graphics, delta) -> draw(graphics, delta.getGameTimeDeltaPartialTick(false)));
    }

    private static void draw(GuiGraphicsExtractor g, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui
                || !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get())) return;
        if (!(player.pick(6.0, partialTick, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof EnergyInspectable target)) return;
        int energy = target.energy(), max = Math.max(1, target.capacity());
        int percent = energy * 100 / max;
        Component detail = target.inspectDetail();
        int boxBottom = detail == null ? 61 : 73;
        g.fill(7, 7, 165, boxBottom, 0xFFB37B55);
        g.fill(8, 8, 164, boxBottom - 1, 0xE8201C19);
        g.text(minecraft.font, target.inspectTitle(), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.energy", energy, max), 14, 25, 0xFFF4EEE6, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.charge", percent), 14, 36, 0xFFF4EEE6, false);
        if (detail != null) {
            g.text(minecraft.font, detail, 14, 47, 0xFFAEC6AD, false);
        }
        int barTop = detail == null ? 48 : 60;
        int color = percent <= 20 ? 0xFFC16A4B : percent <= 70 ? 0xFFCEA457 : 0xFF83A276;
        g.fill(14, barTop, 156, barTop + 6, 0xFF45382F);
        if (energy > 0) g.fill(14, barTop, 14 + 142 * energy / max, barTop + 6, color);
    }
}
