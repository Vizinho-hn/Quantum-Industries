package br.com.quantumindustries.block;

import br.com.quantumindustries.material.MineralCatalog.MineralInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Bloco de minério unificado para minerais naturais do Quantum Industries.
 * Parametrizado por metadados de dureza, resistência, mineralogia e tipo de rocha (Stone vs Deepslate).
 */
public class QuantumOreBlock extends Block {

    private final MineralInfo mineral;
    private final boolean deepslate;

    public QuantumOreBlock(MineralInfo mineral, boolean deepslate, BlockBehaviour.Properties properties) {
        super(properties);
        this.mineral = mineral;
        this.deepslate = deepslate;
    }

    public MineralInfo mineral() {
        return mineral;
    }

    public boolean isDeepslate() {
        return deepslate;
    }

    public static QuantumOreBlock createStone(MineralInfo mineral, BlockBehaviour.Properties baseProps) {
        return new QuantumOreBlock(mineral, false,
                baseProps
                        .strength(mineral.hardness(), mineral.resistance())
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE));
    }

    public static QuantumOreBlock createDeepslate(MineralInfo mineral, BlockBehaviour.Properties baseProps) {
        return new QuantumOreBlock(mineral, true,
                baseProps
                        .strength(mineral.hardness() * 1.5F, mineral.resistance())
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.DEEPSLATE));
    }
}
