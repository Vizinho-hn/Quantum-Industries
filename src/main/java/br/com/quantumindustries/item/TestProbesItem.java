package br.com.quantumindustries.item;

import br.com.quantumindustries.block.entity.EnergyInspectable;
import br.com.quantumindustries.block.entity.MachineStatusInspectable;
import br.com.quantumindustries.inspection.InspectionRegistry;
import br.com.quantumindustries.inspection.InspectionSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/**
 * Diagnostic probes to inspect blocks, machines, and conduits in Quantum Industries.
 */
public class TestProbesItem extends Item {
    public TestProbesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        var snapshot = InspectionRegistry.inspect(server, pos, false);
        var entity = server.getBlockEntity(pos);
        Component detail = entity instanceof MachineStatusInspectable machine ? machine.statusDetail()
                : entity instanceof EnergyInspectable energy ? energy.inspectDetail() : null;
        if (snapshot.kind() == InspectionSnapshot.Kind.NONE && detail == null) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.quantumindustries.probe.no_reading"));
            return InteractionResult.SUCCESS;
        }

        var report = Component.empty();
        switch (snapshot.kind()) {
            case ENERGY -> report.append(Component.translatable("message.quantumindustries.probe.energy",
                    snapshot.localCapacity(), snapshot.localLoad(), snapshot.nodeCount()));
            case FLUID -> report.append(Component.translatable("message.quantumindustries.probe.fluid",
                    snapshot.localCapacity(), snapshot.localLoad(), snapshot.nodeCount()));
            case MECHANICAL -> report.append(Component.translatable("message.quantumindustries.probe.mechanical",
                    Math.round(snapshot.rpm()), snapshot.load(), snapshot.capacity(), snapshot.nodeCount()));
            case PNEUMATIC -> report.append(Component.translatable("message.quantumindustries.probe.pneumatic",
                    snapshot.localCapacity(), snapshot.nodeCount()));
            case NONE -> { }
        }
        if (detail != null) {
            if (snapshot.kind() != InspectionSnapshot.Kind.NONE) report.append(" / ");
            report.append(detail);
        }
        serverPlayer.sendOverlayMessage(report);
        ItemStack stack = context.getItemInHand();
        stack.hurtAndBreak(1, serverPlayer, player.getEquipmentSlotForItem(stack));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipOutput, TooltipFlag tooltipFlag) {
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_0"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_1"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.testprobes.line_2"));
        tooltipOutput.accept(Component.translatable("tooltip.quantumindustries.probe.cycles", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipOutput, tooltipFlag);
    }
}
