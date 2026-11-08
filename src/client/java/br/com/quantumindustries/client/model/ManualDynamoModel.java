package br.com.quantumindustries.client.model;

import br.com.quantumindustries.block.entity.ManualDynamoBlockEntity;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public final class ManualDynamoModel extends GeoModel<ManualDynamoBlockEntity> {
    // GeckoLibResources keys the model/animation caches by the identifier with the
    // geckolib/ prefix and the .geo/.animation suffix stripped, so these two carry the
    // bare path only. The texture is consumed verbatim as a resource path, hence the
    // explicit textures/ prefix and .png suffix.
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("quantumindustries", "block/manual_dynamo");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("quantumindustries", "textures/block/dynamo_materials.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath("quantumindustries", "block/manual_dynamo");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ManualDynamoBlockEntity animatable) {
        return ANIMATION;
    }
}
