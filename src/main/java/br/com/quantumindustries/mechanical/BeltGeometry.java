package br.com.quantumindustries.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Placement constraints shared by belt selection and loaded endpoint validation. */
public final class BeltGeometry {
    public static final int MIN_DISTANCE = 2, MAX_DISTANCE = 12;
    private BeltGeometry() {}
    public static boolean validAlignment(BlockPos a, BlockPos b, Direction.Axis axis) {
        int x = b.getX() - a.getX(), y = b.getY() - a.getY(), z = b.getZ() - a.getZ();
        int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
        return distance >= MIN_DISTANCE && distance <= MAX_DISTANCE
                && (x == 0 ? 0 : 1) + (y == 0 ? 0 : 1) + (z == 0 ? 0 : 1) == 1
                && axis.choose(x, y, z) == 0;
    }
}
