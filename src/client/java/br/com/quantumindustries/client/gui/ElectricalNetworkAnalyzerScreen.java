package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.ElectricalNetworkAnalyzerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

public final class ElectricalNetworkAnalyzerScreen extends AbstractContainerScreen<ElectricalNetworkAnalyzerMenu>{
    private static final int FG=0xFFE0D5C8, MUTED=0xFFA7ADB5, COPPER=0xFFD4673B, GREEN=0xFF9BCB9A,
            GOLD=0xFFE0BA67, RED=0xFFE16B52, BLUE=0xFF7BBED8, DARK=0xFF10151C;
    public ElectricalNetworkAnalyzerScreen(ElectricalNetworkAnalyzerMenu menu,Inventory inv,Component title){
        super(menu,inv,title,328,238);titleLabelY=inventoryLabelY=-10000;
    }
    private void box(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){
        if(w>0&&h>0)g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,c);
    }
    private void text(GuiGraphicsExtractor g,Component t,int x,int y,int c){g.text(font,t,leftPos+x,topPos+y,c,false);}
    private Component tr(String key,Object... args){return Component.translatable("gui.quantumindustries.network_analyzer."+key,args);}
    private String rate(int perSecond){return String.format(Locale.ROOT,"%.2f",perSecond/20.0);}
    private String compact(int value){return value>=100000?String.format(Locale.ROOT,"%.1fk",value/1000.0):Integer.toString(value);}
    private int cableColor(int utilization){return utilization>=100?RED:utilization>=85?GOLD:utilization>0?COPPER:0xFF66717E;}
    private void plot(GuiGraphicsExtractor g,int channel,int max,int color){
        int lastX=0,lastY=0;
        for(int i=0;i<menu.count();i++){
            int x=14+(ElectricalNetworkAnalyzerMenu.HISTORY-menu.count()+i)*149/(ElectricalNetworkAnalyzerMenu.HISTORY-1);
            int y=141-(int)Math.min(57,(long)menu.history(channel,i)*57/Math.max(1,max));
            if(i>0){
                for(int px=lastX;px<=x;px++){
                    int py=lastY+(y-lastY)*(px-lastX)/Math.max(1,x-lastX);
                    box(g,px,py,1,1,color);
                }
                box(g,x,Math.min(y,lastY),1,Math.abs(y-lastY)+1,color);
            }else box(g,x,y,1,1,color);
            lastX=x;lastY=y;
        }
    }
    private void map(GuiGraphicsExtractor g){
        box(g,207,78,99,99,DARK);
        // Project only actual sampled nodes onto XZ, not inferred edges.
        // Draw busier conductors last, then endpoint markers.
        for(int pass=0;pass<4;pass++){
            for(int i=0;i<Math.clamp(menu.value(13),0,128);i++){
                int style=menu.mapStyle(i),p=menu.mapPosition(i);
                int category=style==101?3:style>=85?2:style>0?1:0;
                if(category!=pass)continue;
                int x=207+3*(p/64),y=78+3*(p%64);
                if(style==101)box(g,x,y,3,3,BLUE);else box(g,x,y,3,3,cableColor(style));
            }
        }
        box(g,207+16*3,78+16*3,3,3,0xFFF5F0E6);
    }
    @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float pt){
        box(g,0,0,328,238,0xFF14161A);box(g,1,1,326,236,0xFF9D6945);box(g,3,3,322,232,0xFF292E37);
        box(g,8,8,312,17,0xFF15191F);text(g,title,14,12,0xFFF4DCB9);
        text(g,tr("injected",rate(menu.value(6))),14,29,GREEN);
        text(g,tr("delivered",rate(menu.value(7))),171,29,BLUE);
        text(g,tr("loss",rate(menu.value(8))),14,41,RED);
        text(g,tr("storage",compact(menu.value(4)),compact(menu.value(5))),171,41,FG);
        text(g,tr("counts",menu.value(2),menu.value(3)),14,53,FG);
        text(g,tr("saturated",menu.value(9)),171,53,menu.value(9)>0?RED:FG);
        text(g,tr(menu.value(0)==0?"disconnected":menu.value(1)!=0?"partial":"connected"),14,65,menu.value(1)!=0||menu.value(0)==0?GOLD:GREEN);
        text(g,tr("history"),14,76,FG);
        box(g,14,84,150,59,DARK);
        int max=20;
        for(int i=0;i<menu.count();i++)for(int c=0;c<3;c++)max=Math.max(max,menu.history(c,i));
        for(int j=1;j<4;j++)box(g,14,84+58*j/4,150,1,0xFF353B47);
        plot(g,0,max,GREEN);plot(g,1,max,BLUE);plot(g,2,max,RED);
        text(g,Component.literal("0 - "+rate(max)+" FE/t"),14,144,MUTED);
        text(g,tr("map"),207,65,FG);map(g);
        text(g,tr("map_legend"),207,180,MUTED);
        text(g,tr("map_cable"),207,192,COPPER);
        text(g,tr("map_endpoint"),207,204,BLUE);
        text(g,tr("map_hot"),207,216,RED);
        text(g,tr("bottlenecks"),14,157,FG);
        for(int i=0;i<Math.clamp(menu.value(12),0,ElectricalNetworkAnalyzerMenu.TOP);i++){
            int peak=menu.top(i,3),limit=menu.top(i,4);
            int util=limit>0?Math.clamp(100*peak/limit,0,100):0;
            int y=169+10*i;
            text(g,Component.literal(menu.top(i,0)+","+menu.top(i,1)+","+menu.top(i,2)),14,y,MUTED);
            text(g,Component.literal(peak+"/"+limit+" FE/t"),86,y,cableColor(util));
            box(g,158,y+1,36,6,DARK);box(g,158,y+1,36*util/100,6,cableColor(util));
        }
        super.extractContents(g,mx,my,pt);extractTooltip(g,mx,my);
    }
}
