package br.com.quantumindustries.client.kinetic;

import br.com.quantumindustries.block.MechanicalShaftBlock;
import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.block.entity.CompactGearboxBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity;
import br.com.quantumindustries.block.entity.PrimitiveWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.MediumWaterWheelBlockEntity;
import br.com.quantumindustries.block.entity.LargeWaterWheelBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.KineticMath;
import br.com.quantumindustries.mechanical.MechanicalParticipant;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Native cached-mesh renderer; all animation transforms use a detached render snapshot. */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class QksBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, QksRenderState> {
    private static final String MOD_ID = "quantumindustries";
    private static final Identifier WORKSHOP_TEXTURE = Identifier.fromNamespaceAndPath(
            MOD_ID, "textures/block/mechanical_workshop.png");
    private static final Identifier DYNAMO_TEXTURE = Identifier.fromNamespaceAndPath(
            MOD_ID, "textures/block/dynamo_materials.png");
    private static final String BASE = "base", ROTOR = "rotor", CRANK = "crank", CARRIAGE = "carriage";
    private static final Identifier WHEEL_TEXTURE =
            Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/water_wheel_d.png");
    private static final double UNITS_PER_BLOCK = 16.0;
    private static final double CARRIAGE_CENTER = 0.1, CARRIAGE_AMPLITUDE = 1.7;
    private static final double CARRIAGE_CYCLES_PER_TICK = 0.05;
    private final String modelPath;
    private final Identifier texture;
    private final Direction.Axis rotorLocalAxis;

    public QksBlockEntityRenderer(String modelPath, Identifier texture, Direction.Axis rotorLocalAxis) {
        this.modelPath = modelPath;
        this.texture = texture;
        this.rotorLocalAxis = rotorLocalAxis;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        ClientKineticTracker.init();
        event.registerBlockEntityRenderer(ModBlocks.COMPACT_GEARBOX_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("compact_gearbox",
                        Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/compact_gearbox.png"), Direction.Axis.X));
        event.registerBlockEntityRenderer(ModBlocks.ELECTRIC_SAWMILL_ENTITY.get(), context -> new QksBlockEntityRenderer<>("electric_sawmill", Identifier.fromNamespaceAndPath(MOD_ID,"textures/block/electric_sawmill.png"), Direction.Axis.X));
        event.registerBlockEntityRenderer(ModBlocks.SMALL_COGWHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("small_cogwheel", Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/small_cogwheel.png"), Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.LARGE_COGWHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("large_cogwheel", Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/large_cogwheel.png"), Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.IRON_FLYWHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("iron_flywheel", Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/iron_flywheel.png"), Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_PULLEY_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("mechanical_pulley", Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/mechanical_pulley.png"), Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.MEDIUM_WATER_WHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("medium_water_wheel", WHEEL_TEXTURE, Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.LARGE_WATER_WHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("large_water_wheel", WHEEL_TEXTURE, Direction.Axis.Z));

        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_SHAFT_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("mechanical_shaft_block",
                        Identifier.fromNamespaceAndPath(MOD_ID, "textures/block/mechanical_shaft_a.png"), Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.PRIMITIVE_WATER_WHEEL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("primitive_water_wheel", WHEEL_TEXTURE, Direction.Axis.Z));
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_SAWMILL_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("mechanical_sawmill", WORKSHOP_TEXTURE, Direction.Axis.X));
        event.registerBlockEntityRenderer(ModBlocks.MANUAL_DYNAMO_ENTITY.get(),
                context -> new QksBlockEntityRenderer<>("manual_dynamo", DYNAMO_TEXTURE, Direction.Axis.X));
    }

    @Override public QksRenderState createRenderState() { return new QksRenderState(); }

    @Override
    public void extractRenderState(T entity, QksRenderState state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        ClientKineticTracker.applyPending(entity);
        state.modelPath = modelPath;
        state.texture = texture;
        state.rotorLocalAxis = rotorLocalAxis;
        double time = entity.getLevel() == null ? 0.0 : entity.getLevel().getGameTime() + partialTick;
        if (entity instanceof RotationalBlockEntity rot) {
            state.axle = rot.axle();
            state.angleDegrees = rot.rotation(time);
            state.crankVisible = rot.installed();
            state.baseVisible = !(rot instanceof MechanicalShaftBlockEntity)
                    || MechanicalShaftBlock.isGrounded(rot.getLevel(), rot.getBlockPos(), state.axle);
            state.cutting = rot instanceof MechanicalSawmillBlockEntity saw && saw.rotating() && !saw.input().isEmpty();
            state.carriageOffsetZ = state.cutting ? (float) ((CARRIAGE_CENTER + CARRIAGE_AMPLITUDE
                    * Math.sin(time * CARRIAGE_CYCLES_PER_TICK * Math.PI * 2.0)) / UNITS_PER_BLOCK) : 0.0f;
            state.facing = entity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : switch (state.axle) { case X -> Direction.WEST; case Y -> Direction.UP; case Z -> Direction.NORTH; };
            if (rot instanceof MechanicalShaftBlockEntity shaft) {
                Direction negativeFace = state.facing;
                var negative = mechanicalNeighbor(shaft, negativeFace);
                var positive = mechanicalNeighbor(shaft, negativeFace.getOpposite());
                state.shaftNegativeTipScale = connectedPortScale(negative, negativeFace.getOpposite());
                state.shaftPositiveTipScale = connectedPortScale(positive, negativeFace);
                state.shaftNegativeTipReach = wheelTipReach(negative);
                state.shaftPositiveTipReach = wheelTipReach(positive);
            }
        } else if (entity instanceof ManualDynamoBlockEntity dynamo) {
            state.axle = dynamo.axle();
            state.facing = dynamo.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            state.angleDegrees = dynamo.rotation(time);
            state.crankVisible = dynamo.installed();
            state.baseVisible = true;
            state.cutting = false;
            state.carriageOffsetZ = 0.0f;
        } else if (entity instanceof br.com.quantumindustries.block.entity.ElectricSawmillBlockEntity saw) {
            state.axle = Direction.Axis.X; state.facing = saw.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            state.angleDegrees = saw.visualProgress(partialTick) * 36;
            state.crankVisible = false; state.baseVisible = true; state.cutting = saw.working();
            state.carriageOffsetZ = saw.working() ? saw.visualProgress(partialTick) / 40f * .25f : 0;
        } else {
            throw new IllegalArgumentException("Unsupported QKS entity " + entity.getClass().getName());
        }
        if (entity instanceof br.com.quantumindustries.block.entity.CogwheelBlockEntity gear
                && ((entity.getBlockPos().getX() + entity.getBlockPos().getY() + entity.getBlockPos().getZ()) & 1) != 0)
            state.angleDegrees += (float) Math.toDegrees(Math.PI / gear.teeth());
        if (entity instanceof CompactGearboxBlockEntity gearbox) {
            state.modelPath = modelPath + "_" + gearbox.excludedAxis().getName();
            state.facing = Direction.NORTH; // Geometry already uses world axes.
        }
        state.beltEnd = null;
        if (entity instanceof br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity pulley
                && pulley.partnerPos() != null && entity.getBlockPos().compareTo(pulley.partnerPos()) < 0
                && entity.getLevel() != null && entity.getLevel().hasChunkAt(pulley.partnerPos())
                && entity.getLevel().getBlockEntity(pulley.partnerPos()) instanceof br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity other
                && pulley.validPartner(other)) {
            state.beltEnd = Vec3.atLowerCornerOf(pulley.partnerPos().subtract(entity.getBlockPos()));
            state.beltCrossed = pulley.crossed();
        }
        state.worldAxisSign = worldAxisSign(rotorLocalAxis, state.facing);
    }

    /** Converts positive world-axis rotation into the oriented model's local right-hand rule. */
    public static float worldAxisSign(Direction.Axis localAxis, Direction facing) {
        return switch (localAxis) {
            case Z -> KineticMath.localRotorSign(facing);
            case X -> (facing == Direction.NORTH || facing == Direction.EAST) ? 1.0f : -1.0f;
            case Y -> throw new IllegalArgumentException("Unsupported QKS local rotor axis Y");
        };
    }

    private static MechanicalParticipant mechanicalNeighbor(RotationalBlockEntity machine, Direction face) {
        var level = machine.getLevel();
        var neighborPos = machine.getBlockPos().relative(face);
        if (level == null || !machine.connectsMechanical(face) || !level.hasChunkAt(neighborPos)
                || !(level.getBlockEntity(neighborPos) instanceof MechanicalParticipant neighbor)
                || !neighbor.connectsMechanical(face.getOpposite())) return null;
        return neighbor;
    }

    private static float connectedPortScale(MechanicalParticipant neighbor, Direction portFace) {
        if (neighbor == null) return 1.0f;
        int width = Math.clamp(neighbor.mechanicalPortSize(portFace), 3, MechanicalShaftBlock.PORT_SIZE);
        return (float) width / MechanicalShaftBlock.PORT_SIZE;
    }

    // Hub face depths in model units, matching the three style D wheel rigs.
    private static float wheelTipReach(MechanicalParticipant neighbor) {
        if (neighbor instanceof PrimitiveWaterWheelBlockEntity) return 8 - 3.86f;
        if (neighbor instanceof MediumWaterWheelBlockEntity) return 8 - 5.56f;
        if (neighbor instanceof LargeWaterWheelBlockEntity) return 8 - 7.41f;
        return 0;
    }

    @Override
    public void submit(QksRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.beltEnd != null) QksBeltRenderer.submit(state, pose, collector);
        QksCompiledModel model = QksModelCache.get(state.modelPath);
        pose.pushPose();
        if (state.facing == Direction.UP) {
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            pose.translate(-0.5f, -0.5f, -0.5f);
        } else {
            float yRot = switch (state.facing) {
                case EAST -> -90.0f; case SOUTH -> 180.0f; case WEST -> 90.0f; default -> 0.0f;
            };
            if (yRot != 0.0f) {
                pose.translate(0.5f, 0.5f, 0.5f);
                pose.mulPose(Axis.YP.rotationDegrees(yRot));
                pose.translate(-0.5f, -0.5f, -0.5f);
            }
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(state.texture), (passPose, consumer) -> {
            PoseStack work = new PoseStack();
            work.last().pose().set(passPose.pose());
            work.last().normal().set(passPose.normal());
            for (String root : model.rootBoneNames()) submitBone(model, root, state, work, consumer);
        });
        pose.popPose();
    }

    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(T entity) {
        var bounds = new net.minecraft.world.phys.AABB(entity.getBlockPos());
        if (entity instanceof MechanicalShaftBlockEntity shaft) {
            Direction.Axis axis = shaft.axle();
            return bounds.inflate(axis == Direction.Axis.X ? .3 : 0, axis == Direction.Axis.Y ? .3 : 0,
                    axis == Direction.Axis.Z ? .3 : 0);
        }
        if (entity instanceof br.com.quantumindustries.block.entity.RimWaterWheelBlockEntity wheel)
            return bounds.inflate(wheel.rimRadius() + .5);
        if (entity instanceof br.com.quantumindustries.block.entity.MechanicalPulleyBlockEntity pulley && pulley.partnerPos() != null)
            return bounds.minmax(new net.minecraft.world.phys.AABB(pulley.partnerPos())).inflate(.5);
        return bounds;
    }

    private static void submitBone(QksCompiledModel model, String name, QksRenderState state,
            PoseStack work, VertexConsumer consumer) {
        if (name.equals(CRANK) && !state.crankVisible) return;
        if (state.modelPath.equals("electric_sawmill") && name.equals(CARRIAGE) && !state.cutting) return;
        var bone = model.bone(name);
        work.pushPose();
        if (name.equals(CARRIAGE)) work.translate(0.0f, 0.0f, state.carriageOffsetZ);
        work.translate(bone.pivotX(), bone.pivotY(), bone.pivotZ());
        if (bone.defaultRotZ() != 0) work.mulPose(Axis.ZP.rotationDegrees(bone.defaultRotZ()));
        if (bone.defaultRotY() != 0) work.mulPose(Axis.YP.rotationDegrees(bone.defaultRotY()));
        if (bone.defaultRotX() != 0) work.mulPose(Axis.XP.rotationDegrees(bone.defaultRotX()));
        if (name.equals(ROTOR)) {
            Axis axis = state.rotorLocalAxis == Direction.Axis.X ? Axis.XP : Axis.ZP;
            work.mulPose(axis.rotationDegrees(state.angleDegrees * state.worldAxisSign));
        }
        if (name.equals("shaft_tip_negative") || name.equals("shaft_tip_positive")) {
            boolean negative = name.equals("shaft_tip_negative");
            float width = negative ? state.shaftNegativeTipScale : state.shaftPositiveTipScale;
            float reach = negative ? state.shaftNegativeTipReach : state.shaftPositiveTipReach;
            // Scale along the axis about the inner edge, keeping the tip joined to the shaft body.
            float lengthScale = 1 + reach / 2.55f;
            float innerEdge = negative ? 2.55f / 16 : 13.45f / 16;
            work.translate(0, 0, (1 - lengthScale) * (innerEdge - bone.pivotZ()));
            work.scale(width, width, lengthScale);
        }
        if (name.startsWith("gearbox_axle_")) {
            Direction.Axis worldAxis = switch (name) {
                case "gearbox_axle_x" -> Direction.Axis.X;
                case "gearbox_axle_y" -> Direction.Axis.Y;
                case "gearbox_axle_z" -> Direction.Axis.Z;
                default -> throw new IllegalArgumentException("Unknown gearbox axle " + name);
            };
            Axis axis = switch (worldAxis) {
                case X -> Axis.XP;
                case Y -> Axis.YP;
                case Z -> Axis.ZP;
            };
            // The two port ratios are +1 and -1 around the single synchronized QKS phase.
            work.mulPose(axis.rotationDegrees(worldAxis == state.axle
                    ? state.angleDegrees : -state.angleDegrees));
        }
        work.translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
        if (!name.equals(BASE) || state.baseVisible) {
            QksCompiledModel.submitBone(bone, work.last(), consumer, state.lightCoords);
        }
        // Hidden base geometry must not hide the dynamo's child rotor.
        for (String child : model.childBoneNames(name)) submitBone(model, child, state, work, consumer);
        work.popPose();
    }
}
