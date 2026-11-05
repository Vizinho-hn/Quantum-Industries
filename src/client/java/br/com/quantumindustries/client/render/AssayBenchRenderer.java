package br.com.quantumindustries.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Compatibility name for the former item-only renderer. Registration lives in ClientMenus. */
@Deprecated
public final class AssayBenchRenderer extends br.com.quantumindustries.client.renderer.EmpiricalAssayRenderer {
    public AssayBenchRenderer(BlockEntityRendererProvider.Context context){super(context);}
}
