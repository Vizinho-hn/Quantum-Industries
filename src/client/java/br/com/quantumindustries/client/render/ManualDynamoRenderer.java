package br.com.quantumindustries.client.render;

import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import br.com.quantumindustries.client.model.ManualDynamoModel;
import br.com.quantumindustries.core.ModBlocks;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "quantumindustries", value = Dist.CLIENT)
public final class ManualDynamoRenderer extends GeoBlockRenderer<ManualDynamoBlockEntity, ManualDynamoRenderer.State> {

    public ManualDynamoRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new ManualDynamoModel());
    }

    public static final class State extends BlockEntityRenderState implements GeoRenderState {
        boolean installed;
        float angle;
        Direction facing = Direction.NORTH;
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
                    State.class
                );
            } catch (Throwable ignored) {
            }
            try {
                for (java.lang.reflect.Field field : BlockEntityRenderState.class.getDeclaredFields()) {
                    if (field.getName().contains("data") && Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        f = field;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
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
                } catch (Throwable ignored) {
                }
            }
            if (DATA_FIELD != null) {
                try {
                    return (Map<DataTicket<?>, Object>) DATA_FIELD.get(this);
                } catch (Throwable ignored) {
                }
            }
            if (fallback == null) fallback = new HashMap<>();
            return fallback;
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ManualDynamoBlockEntity dynamo, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay) {
        super.extractRenderState(dynamo, state, partialTick, camera, overlay);
        state.installed = dynamo.installed();
        state.facing = dynamo.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.angle = dynamo.getLevel() == null
                ? 0f
                : dynamo.rotation(dynamo.getLevel().getGameTime() + partialTick);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<State> renderPass, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPass, snapshots);
        State state = renderPass.renderState();
        snapshots.ifPresent("crank", b -> b.skipRender(!state.installed));
        float axisSign = (state.facing == Direction.NORTH || state.facing == Direction.EAST) ? 1f : -1f;
        float rad = (float) Math.toRadians(state.angle * axisSign);
        snapshots.ifPresent("rotor", b -> b.setRotX(rad));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MANUAL_DYNAMO_ENTITY.get(), ManualDynamoRenderer::new);
    }
}
