package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.block.entity.MachineStatusInspectable;
import br.com.quantumindustries.fluid.FluidInspectable;
import br.com.quantumindustries.logistics.ConfigurableConduit;
import br.com.quantumindustries.logistics.ConnectionTarget;
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
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) return;
        if (!(player.pick(6.0, partialTick, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;

        var blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        boolean hasGoggles = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENERGY_INSPECTION_GOGGLES.get());

        if (hasGoggles) {
            if (blockEntity instanceof ConfigurableConduit conduit) drawConnection(g, minecraft, conduit, hit);
            if (blockEntity instanceof FluidInspectable fluid) {
                drawFluid(g, minecraft, fluid);
                return;
            }
            if (blockEntity instanceof EnergyInspectable target) {
                drawEnergy(g, minecraft, target);
                return;
            }
        }

        if (blockEntity instanceof MachineStatusInspectable machine) {
            drawCompactStatus(g, minecraft, machine);
        }
    }

    private static void drawConnection(GuiGraphicsExtractor g, Minecraft minecraft,
            ConfigurableConduit conduit, BlockHitResult hit) {
        var side = ConnectionTarget.face(hit.getBlockPos(), hit.getLocation(), hit.getDirection());
        var mode = conduit.connectionMode(side);
        int color = switch (mode) {
            case PULL -> 0xFF6DBDE0;
            case PUSH -> 0xFFE4AE62;
            case NONE -> 0xFFB4A99A;
            case AUTO -> 0xFF83A276;
        };
        var label = Component.translatable("gui.quantumindustries.connection.changed",
                Component.translatable("gui.quantumindustries.connection.face." + side.getName()), mode.label());
        int width = Math.max(168, minecraft.font.width(label) + 16);
        g.fill(7, 84, 7 + width, 105, 0xFFB37B55);
        g.fill(8, 85, 6 + width, 104, 0xED201C19);
        g.text(minecraft.font, label, 14, 91, color, false);
    }

    private static void drawEnergy(GuiGraphicsExtractor g, Minecraft minecraft, EnergyInspectable target) {
        int energy = target.energy(), max = Math.max(1, target.capacity());
        int percent = energy * 100 / max;
        Component detail = target.inspectDetail();
        int boxBottom = detail == null ? 61 : 73;
        g.fill(7, 7, 175, boxBottom, 0xFFB37B55);
        g.fill(8, 8, 174, boxBottom - 1, 0xE8201C19);
        g.text(minecraft.font, target.inspectTitle(), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.energy", energy, max), 14, 25, 0xFFF4EEE6, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.dynamo.charge", percent), 14, 36, 0xFFF4EEE6, false);
        if (detail != null) {
            g.text(minecraft.font, detail, 14, 47, 0xFFAEC6AD, false);
        }
        int barTop = detail == null ? 48 : 60;
        int color = percent <= 20 ? 0xFFC16A4B : percent <= 70 ? 0xFFCEA457 : 0xFF83A276;
        g.fill(14, barTop, 166, barTop + 6, 0xFF45382F);
        if (energy > 0) g.fill(14, barTop, 14 + 152 * energy / max, barTop + 6, color);
    }

    private static void drawFluid(GuiGraphicsExtractor g, Minecraft minecraft, FluidInspectable fluid) {
        int amount = fluid.storedFluid(), capacity = Math.max(1, fluid.fluidCapacity());
        g.fill(7, 7, 175, 75, 0xFFB37B55);
        g.fill(8, 8, 174, 74, 0xE8201C19);
        g.text(minecraft.font, fluid.fluidTitle(), 14, 13, 0xFFF4DCB9, false);
        g.text(minecraft.font, Component.translatable("gui.quantumindustries.fluid.amount", amount, capacity),
                14, 27, 0xFFF4EEE6, false);
        g.text(minecraft.font, fluid.fluidDetail(), 14, 42, 0xFFAEC6AD, false);
        g.fill(14, 60, 166, 66, 0xFF45382F);
        if (amount > 0) g.fill(14, 60, 14 + 152 * amount / capacity, 66, 0xFF6AA6D9);
    }

    private static void drawCompactStatus(GuiGraphicsExtractor g, Minecraft minecraft, MachineStatusInspectable machine) {
        Component title = machine.statusTitle();
        Component detail = machine.statusDetail();
        MachineStatusInspectable.State state = machine.statusState();

        int titleW = minecraft.font.width(title);
        int detailW = minecraft.font.width(detail);
        int innerW = Math.max(118, Math.max(titleW, detailW) + 12);
        int cx = g.guiWidth() / 2;
        int top = g.guiHeight() / 2 + 16;
        int left = cx - innerW / 2;
        int right = left + innerW;
        int bottom = top + 31;

        int borderColor = switch (state) {
            case WORKING -> 0xFFE59B3A;
            case READY   -> 0xFF72C466;
            case BLOCKED -> 0xFFD9644A;
            case IDLE    -> 0xFF8A7968;
        };
        int detailColor = switch (state) {
            case WORKING -> 0xFFFFD885;
            case READY   -> 0xFFA6F29B;
            case BLOCKED -> 0xFFFFA58A;
            case IDLE    -> 0xFFC7BFB6;
        };

        g.fill(left - 1, top - 1, right + 1, bottom + 1, borderColor);
        g.fill(left, top, right, bottom, 0xDC1B1815);
        g.text(minecraft.font, title, left + 6, top + 4, 0xFFF4DCB9, false);
        g.text(minecraft.font, detail, left + 6, top + 14, detailColor, false);

        int barLeft = left + 6;
        int barRight = right - 6;
        int barTop = top + 25;
        g.fill(barLeft, barTop, barRight, barTop + 3, 0xFF3A3028);
        float progress = Math.clamp(machine.statusProgress(), 0.0f, 1.0f);
        if (state == MachineStatusInspectable.State.BLOCKED) {
            g.fill(barLeft, barTop, barLeft + Math.max(8, (barRight - barLeft) / 4), barTop + 3, 0xFFD9644A);
        } else if (progress > 0.0f) {
            int fillW = Math.max(2, Math.round((barRight - barLeft) * progress));
            g.fill(barLeft, barTop, barLeft + fillW, barTop + 3, borderColor);
        }
    }
}
