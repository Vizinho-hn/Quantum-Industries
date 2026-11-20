package br.com.quantumindustries.mechanical;

import java.util.Objects;
import net.minecraft.core.BlockPos;

public record KineticLink(BlockPos target, double ratio, double efficiency) {
    public KineticLink {
        Objects.requireNonNull(target, "target");
        target = target.immutable();
        if (!Double.isFinite(ratio) || ratio == 0) throw new IllegalArgumentException("Invalid ratio");
        if (!Double.isFinite(efficiency) || efficiency <= 0 || efficiency > 1) throw new IllegalArgumentException("Invalid efficiency");
    }
}
