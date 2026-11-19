package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.MechanicalShaftBlock;
import br.com.quantumindustries.block.entity.MechanicalShaftBlockEntity;
import br.com.quantumindustries.core.ModBlocks;
import br.com.quantumindustries.mechanical.KineticMath;
import br.com.quantumindustries.mechanical.RotationalBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Deterministic client-only kinetic renderer for pure rotational components (shafts, water wheels).
 * Drives the rotor bone directly from world time and signed spinDirection without GeckoLib animation conflicts.
 */
@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class KineticRenderer<T extends RotationalBlockEntity> extends GeoBlockRenderer<T, KineticRenderer.KineticRenderState> {

    private static final class KineticGeoModel<M extends RotationalBlockEntity> extends GeoModel<M> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("quantumindustries", "textures/block/mechanical_workshop.png");
        private final Identifier modelId;
        private final Identifier animId;

        private KineticGeoModel(String path) {
            this.modelId = Identifier.fromNamespaceAndPath("quantumindustries", "block/" + path);
            this.animId = Identifier.fromNamespaceAndPath("quantumindustries", "block/" + path);
        }

        @Override public Identifier getModelResource(GeoRenderState state) { return modelId; }
        @Override public Identifier getTextureResource(GeoRenderState state) { return TEXTURE; }
        @Override public Identifier getAnimationResource(M animatable) { return animId; }
    }

    public static final class KineticRenderState extends BlockEntityRenderState implements GeoRenderState {
        String model = "";
        Direction.Axis axle = Direction.Axis.Z;
        Direction facing = Direction.NORTH;
        int spinDirection = 1;
        float angularVelocity = KineticMath.DEFAULT_DEGREES_PER_TICK;
        float angle;
        boolean active;
        boolean crank;
        boolean grounded = true;

        private static final java.lang.invoke.MethodHandle SUPER_GET_DATA_MAP;
        private static final java.lang.reflect.Field DATA_FIELD;

        static {
            java.lang.invoke.MethodHandle mh = null;
            java.lang.reflect.Field f = null;
            try {
                mh = java.lang.invoke.MethodHandles.lookup().findSpecial(
                        BlockEntityRenderState.class,
                        "getDataMap",
                        java.lang.invoke.MethodType.methodType(Map.class),
                        KineticRenderState.class
                );
            } catch (Throwable ignored) {}
            try {
                for (java.lang.reflect.Field field : BlockEntityRenderState.class.getDeclaredFields()) {
                    if (field.getName().contains("data") && Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        f = field;
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            SUPER_GET_DATA_MAP = mh;
            DATA_FIELD = f;
        }

        private Map<DataTicket<?>, Object> fallback;

        @Override
        @SuppressWarnings("unchecked")
        public Map<DataTicket<?>, Object> getDataMap() {
            if (SUPER_GET_DATA_MAP != null) {
                try {
                    return (Map<DataTicket<?>, Object>) SUPER_GET_DATA_MAP.invoke(this);
                } catch (Throwable ignored) {}
            }
            if (DATA_FIELD != null) {
                try {
                    return (Map<DataTicket<?>, Object>) DATA_FIELD.get(this);
                } catch (Throwable ignored) {}
            }
            if (fallback == null) fallback = new HashMap<>();
            return fallback;
        }
    }

    public KineticRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new KineticGeoModel<>(modelPath));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MECHANICAL_SHAFT_ENTITY.get(),
                ctx -> new KineticRenderer<>(ctx, "mechanical_shaft_block"));
        event.registerBlockEntityRenderer(ModBlocks.PRIMITIVE_WATER_WHEEL_ENTITY.get(),
                ctx -> new KineticRenderer<>(ctx, "primitive_water_wheel"));
    }

    @Override
    public KineticRenderState createRenderState() {
        return new KineticRenderState();
    }

    @Override
    protected Direction getBlockStateDirection(T entity) {
        if (entity instanceof MechanicalShaftBlockEntity) {
            return switch (entity.axle()) {
                case X -> Direction.WEST;
                case Y -> Direction.UP;
                case Z -> Direction.NORTH;
            };
        }
        return super.getBlockStateDirection(entity);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<KineticRenderState> renderPass) {
        KineticRenderState state = renderPass.renderState();
        if (state.model.equals("mechanical_shaft_block") && state.axle == Direction.Axis.Y) {
            var pose = renderPass.poseStack();
            pose.translate(0.0, 0.5, 0.0);
            pose.mulPose(Axis.XP.rotationDegrees(90.0f));
            pose.translate(0.0, -0.5, 0.0);
            return;
        }
        super.adjustRenderPose(renderPass);
    }

    @Override
    public void extractRenderState(T entity, KineticRenderState state, float partial,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        super.extractRenderState(entity, state, partial, camera, overlay);
        state.model = BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getPath();
        state.axle = entity.axle();
        state.spinDirection = entity.spinDirection();
        state.active = entity.rotating();
        state.angularVelocity = KineticMath.DEFAULT_DEGREES_PER_TICK;
        state.angle = entity.getLevel() == null
                ? 0f
                : entity.rotation(entity.getLevel().getGameTime() + partial);
        state.crank = entity.installed();
        state.grounded = !(entity instanceof MechanicalShaftBlockEntity)
                || MechanicalShaftBlock.isGrounded(entity.getLevel(), entity.getBlockPos(), state.axle);
        if (entity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.facing = entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else {
            state.facing = switch (state.axle) {
                case X -> Direction.WEST;
                case Y -> Direction.UP;
                case Z -> Direction.NORTH;
            };
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<KineticRenderState> renderPass, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPass, snapshots);
        KineticRenderState state = renderPass.renderState();
        snapshots.ifPresent("crank", b -> b.skipRender(!state.crank));
        if (state.model.equals("mechanical_shaft_block")) {
            snapshots.ifPresent("base", b -> b.skipRender(!state.grounded));
        }
        float rad = (float) Math.toRadians(state.angle * KineticMath.localRotorSign(state.facing));
        snapshots.ifPresent("rotor", b -> b.setRotZ(rad));
    }
}
