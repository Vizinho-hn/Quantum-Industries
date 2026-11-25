package br.com.quantumindustries.fluid;

import br.com.quantumindustries.block.JawCrusherBlock;
import br.com.quantumindustries.block.entity.*;
import br.com.quantumindustries.core.ModBlocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Expose the modern 26.1 fluid capability to other mods and vanilla bucket interactions. */
public final class ModFluidCapabilities {
    private ModFluidCapabilities() {}
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void registerEnergy(
            RegisterCapabilitiesEvent event, net.minecraft.world.level.block.entity.BlockEntityType<T> type,
            java.util.function.Function<T, net.neoforged.neoforge.transfer.energy.EnergyHandler> handler) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) ->
                side == null || br.com.quantumindustries.energy.EnergyPorts.connects(be.getBlockState(), side)
                        ? handler.apply(be) : null);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        registerEnergy(event, ModBlocks.MANUAL_DYNAMO_ENTITY.get(), ManualDynamoBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.THERMOELECTRIC_GENERATOR_ENTITY.get(), ThermoelectricGeneratorBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.ROTARY_CONVERSION_ENTITY.get(), RotaryConversionBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.PIEZO_SWITCH_ENTITY.get(), PiezoSwitchBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.LOAD_RELAY_ENTITY.get(), LoadRelayBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.VOLTAIC_PILE_ENTITY.get(), VoltaicPileBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.LEAD_ACID_ACCUMULATOR_ENTITY.get(), LeadAcidAccumulatorBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.ELECTROMECHANICAL_PRESS_ENTITY.get(), ElectromechanicalPressBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.WIRE_DRAWING_BENCH_ENTITY.get(), WireDrawingBenchBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(), ElectricAlloySmelterBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.BELLOWS_EJECTOR_ENTITY.get(), BellowsEjectorBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.PERISTALTIC_PUMP_ENTITY.get(), PeristalticPumpBlockEntity::energyPort);
        registerEnergy(event, ModBlocks.ELECTRIC_SAWMILL_ENTITY.get(), ElectricSawmillBlockEntity::energyPort);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.GRAVITATIONAL_SORTER_ENTITY.get(),
                (be, side) -> be.itemPort());
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) -> {
            var crusher = JawCrusherBlock.machineAt(level, pos, state);
            return crusher == null ? null : crusher.itemPort(state, side);
        }, ModBlocks.JAW_CRUSHER.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.MANUAL_GRAIN_MILL_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.WIRE_DRAWING_BENCH_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.ELECTROMECHANICAL_PRESS_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.BELLOWS_BRAZIER_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.ELECTRIC_ALLOY_SMELTER_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.ELECTRIC_SAWMILL_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.SETTLING_VAT_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlocks.BELLOWS_ALLOY_KILN_ENTITY.get(), (be, side) -> be.itemPort());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.PERISTALTIC_PUMP_ENTITY.get(),
                PeristalticPumpBlockEntity::fluidInlet);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.FLUID_PIPE_ENTITY.get(),
                (be, side) -> be.sidedTank(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.SETTLING_VAT_ENTITY.get(),
                (be, side) -> be.tank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlocks.STEAM_BOILER_ENTITY.get(),
                (be, side) -> side == be.outlet() ? null : new BoilerWaterInlet(be, side));
    }
}
