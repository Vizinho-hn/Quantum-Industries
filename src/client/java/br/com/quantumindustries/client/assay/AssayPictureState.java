package br.com.quantumindustries.client.assay;

import br.com.quantumindustries.assay.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public record AssayPictureState(int x0,int y0,int x1,int y1,float scale,AssayMotion motion,AssayCatalog.Material material,AssayCatalog.Method focus,int tools,net.minecraft.client.renderer.item.ItemStackRenderState sample) implements PictureInPictureRenderState {
    @Override public ScreenRectangle scissorArea(){return new ScreenRectangle(x0,y0,x1-x0,y1-y0);}
    @Override public ScreenRectangle bounds(){return scissorArea();}
}
