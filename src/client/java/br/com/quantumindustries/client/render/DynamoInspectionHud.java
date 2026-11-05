package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
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
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof ManualDynamoBlockEntity dynamo)) return;
        int energy = dynamo.energy(), max = ManualDynamoBlockEntity.CAPACITY;
        int percent = energy * 100 / max;
        g.fill(7, 7, 151, 61, 0xFFB37B55);
        g.fill(8, 8, 150, 60, 0xE8201C19);
        g.text(minecraft.font, Component.translatable("block.quantumindustries.manual_dynamo"), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.energy", energy, max), 14, 25, 0xFFF4EEE6, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.charge", percent), 14, 36, 0xFFF4EEE6, false);
        int color = percent <= 20 ? 0xFFC16A4B : percent <= 70 ? 0xFFCEA457 : 0xFF83A276;
        g.fill(14, 48, 144, 54, 0xFF45382F);
        if (energy > 0) g.fill(14, 48, 14 + 130 * energy / max, 54, color);
    }
}
