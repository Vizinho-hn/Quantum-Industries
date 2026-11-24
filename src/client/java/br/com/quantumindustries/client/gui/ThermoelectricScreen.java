package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.ThermoelectricMenu;
import br.com.quantumindustries.energy.ThermalHistory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

public final class ThermoelectricScreen extends AbstractContainerScreen<ThermoelectricMenu> {
    private static final int HOT=0xFFF09654,COLD=0xFF7BBED8,EFF=0xFFE0BA67,POWER=0xFF9BCB9A,FG=0xFFE0D5C8;
    public ThermoelectricScreen(ThermoelectricMenu m,Inventory i,Component t){
        super(m,i,t,328,238);titleLabelY=inventoryLabelY=-10000;
    }
    private void box(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){
        if(w>0&&h>0)g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,color);
    }
    private void text(GuiGraphicsExtractor g,Component t,int x,int y,int color){g.text(font,t,leftPos+x,topPos+y,color,false);}
    private Component tr(String key,Object... args){return Component.translatable("gui.quantumindustries.thermal."+key,args);}
    private void plot(GuiGraphicsExtractor g,int channel,int x,int y,int w,int h,int min,int max,int color){
        int prevX=0,prevY=0;
        for(int i=0;i<menu.count();i++){
            int px=x+(ThermalHistory.SIZE-menu.count()+i)*(w-1)/(ThermalHistory.SIZE-1);
            int py=y+h-1-Math.clamp((menu.sample(channel,i)-min)*(h-1)/(max-min),0,h-1);
            if(i>0){
                for(int k=prevX;k<=px;k++){
                    int yy=prevY+(py-prevY)*(k-prevX)/Math.max(1,px-prevX);
                    box(g,k,yy,1,1,color);
                }
                box(g,px,Math.min(py,prevY),1,Math.abs(py-prevY)+1,color);
            }else box(g,px,py,1,1,color);
            prevX=px;prevY=py;
        }
    }
    private void graph(GuiGraphicsExtractor g,int x,int y,int w,int h){
        box(g,x,y,w,h,0xFF10151C);
        for(int i=1;i<4;i++)box(g,x,y+i*h/4,w,1,0xFF353B47);
    }
    private String decimal(int v){return String.format(Locale.ROOT,"%.2f",v/100.0);}
    @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float pt){
        box(g,0,0,328,238,0xFF14161A);box(g,1,1,326,236,0xFF9D6945);box(g,3,3,322,232,0xFF292E37);
        box(g,8,8,312,17,0xFF15191F);text(g,title,14,12,0xFFF4DCB9);
        text(g,tr("hot",menu.value(1)),14,29,HOT);
        text(g,tr("cold",menu.value(2)),117,29,COLD);
        text(g,tr("ambient",menu.value(3)),218,29,FG);
        text(g,tr("efficiency",decimal(menu.value(4))),14,40,EFF);
        text(g,tr("net",decimal(menu.value(5))),171,40,POWER);
        text(g,tr("temperature_graph"),14,52,FG);
        text(g,tr("performance_graph"),171,52,FG);
        graph(g,14,64,143,43);graph(g,171,64,143,43);
        plot(g,0,14,64,143,43,-60,300,HOT);plot(g,1,14,64,143,43,-60,300,COLD);
        plot(g,2,171,64,143,43,0,10000,EFF);plot(g,3,171,64,143,43,-400,600,POWER);
        text(g,Component.literal("-60 / 300 C"),14,108,FG);
        text(g,Component.literal("0-100% | -4/+6 FE/t"),171,108,FG);
        for(var slot:menu.slots){box(g,slot.x-1,slot.y-1,18,18,0xFF8C98A7);box(g,slot.x,slot.y,16,16,0xFF171C23);}
        text(g,tr("modules"),14,116,FG);
        text(g,tr("buffer",menu.value(0)),101,123,FG);
        text(g,tr("coolant",menu.value(6)),218,123,COLD);
        text(g,tr("exchange",menu.value(8),menu.value(9)),101,135,FG);
        text(g,Component.translatable("container.inventory"),83,146,FG);
        super.extractContents(g,mx,my,pt);extractTooltip(g,mx,my);
    }
}
