package br.com.quantumindustries.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Wrench and goggles select the same arm, including hits on an arm's side wall. */
public final class ConnectionTarget {
    private ConnectionTarget() {}

    public static Direction face(BlockPos pos, Vec3 hit, Direction surface) {
        double x = hit.x - pos.getX() - .5;
        double y = hit.y - pos.getY() - .5;
        double z = hit.z - pos.getZ() - .5;
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (Math.max(ax, Math.max(ay, az)) <= .25) return surface;
        if (ax >= ay && ax >= az) return x < 0 ? Direction.WEST : Direction.EAST;
        if (ay >= az) return y < 0 ? Direction.DOWN : Direction.UP;
        return z < 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
