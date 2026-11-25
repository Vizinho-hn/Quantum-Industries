package br.com.quantumindustries.client.kinetic;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/** Client snapshot containing only the data needed by the native kinetic renderer. */
public final class QksRenderState extends BlockEntityRenderState {
    public String modelPath;
    public Identifier texture;
    public Direction facing = Direction.NORTH;
    public Direction.Axis axle = Direction.Axis.Z;
    public Direction.Axis rotorLocalAxis = Direction.Axis.Z;
    public net.minecraft.world.phys.Vec3 beltEnd;
    public boolean beltCrossed;
    public float angleDegrees;
    public boolean crankVisible = true;
    public boolean baseVisible = true;
    public boolean cutting;
    public float carriageOffsetZ;
    public float worldAxisSign = 1.0f;
    public float shaftNegativeTipReach;
    public float shaftPositiveTipReach;
    public float shaftNegativeTipScale = 1.0f;
    public float shaftPositiveTipScale = 1.0f;
}
