package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import br.com.quantumindustries.assay.AssayCatalog;
import br.com.quantumindustries.assay.AssayCatalog.Material;
import br.com.quantumindustries.assay.AssayCatalog.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

/** Pixel-drawn mechanical instruments; visual motion never grants server knowledge. */
public class EmpiricalAssayBenchScreen extends AbstractContainerScreen<EmpiricalAssayBenchMenu> {
    private static final Identifier INSTRUMENTS=Identifier.fromNamespaceAndPath("quantumindustries","textures/gui/empirical_instruments.png");
    private static final int INK=0xFF30251D, GOLD=0xFFB39753, EDGE=0xFF46321E, PAPER=0xFFE6D299;
    private Button action;
    private final Button[] panels=new Button[3];
    public EmpiricalAssayBenchScreen(EmpiricalAssayBenchMenu menu,Inventory inv,Component title){
        super(menu,inv,title,350,236);titleLabelY=-100;inventoryLabelY=-100;
    }
    private Component tr(String key,Object... args){return Component.translatable("gui.quantumindustries.assay."+key,args);}
    private void send(int button){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);}
    private Button button(Component label,int x,int y,int w,int h,Runnable run,boolean transparent){
        return addRenderableWidget(new Button(Button.builder(label,b->run.run()).bounds(leftPos+x,topPos+y,w,h)) {
            @Override protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector collector) {}
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
                if(transparent)return;
                extractDefaultSprite(g);
                g.pose().pushMatrix();g.pose().translate(getX()+getWidth()/2f,getY()+(getHeight()-6)/2f);g.pose().scale(.75f,.75f);
                g.text(font,getMessage(),-font.width(getMessage())/2,0,active?0xFFE6D299:0xFF999185,false);g.pose().popMatrix();
            }
        });
    }
    @Override protected void init(){
        super.init();
        action=button(tr("perform"),254,157,88,18,()->send(0),false);
        for(var method:Method.values())panels[method.ordinal()]=button(Component.translatable(method.translationKey()),80,43+method.ordinal()*32,157,31,()->send(1+method.ordinal()),true);
        button(tr("previous"),253,55,12,11,()->send(10+(menu.viewedMaterial().ordinal()+9)%10),false);
        button(tr("next"),324,55,12,11,()->send(10+(menu.viewedMaterial().ordinal()+1)%10),false);
    }
    @Override protected void containerTick(){
        super.containerTick();action.active=menu.ready();
        for(var panel:panels)panel.active=!menu.running();
    }
    private void fitted(GuiGraphicsExtractor g,String s,int x,int y,int width,int color){
        int max=(int)(width/.75f);
        text(g,font.width(s)>max?font.plainSubstrByWidth(s,max-font.width("…"))+"…":s,x,y,color);
    }
    private boolean hovered(int mx,int my,int x,int y,int w,int h){return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;}
    private Component toolStatus(){
        int missing=menu.missingTools();
        if(missing==0)return tr("tools_ready");
        java.util.List<String> tools=new java.util.ArrayList<>();
        if((missing&1)!=0)tools.add(tr("tool.magnifier").getString());
        if((missing&2)!=0)tools.add(tr("tool.flame").getString());
        if((missing&4)!=0)tools.add(tr("tool.weights").getString());
        return tr("missing",String.join(", ",tools));
    }
    private void slotCell(GuiGraphicsExtractor g,int x,int y){
        rect(g,x,y,18,18,EDGE);rect(g,x+1,y+1,16,16,0xFF29231D);
        rect(g,x+1,y,16,1,0xFFC1A36A);rect(g,x,y+1,1,16,0xFF8E7143);
        rect(g,x+1,y+17,16,1,0xFF55402B);rect(g,x+17,y+1,1,16,0xFF55402B);
    }
    private void rect(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,c);}
    private void text(GuiGraphicsExtractor g,String s,int x,int y,int c){
        g.pose().pushMatrix();g.pose().translate(leftPos+x,topPos+y);g.pose().scale(.75f,.75f);
        g.text(font,s,0,0,c,false);g.pose().popMatrix();
    }
    private void frame(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill){
        rect(g,x,y,w,h,EDGE);rect(g,x+1,y+1,w-2,h-2,GOLD);rect(g,x+3,y+3,w-6,h-6,fill);
        for(int dx:new int[]{3,w-6})for(int dy:new int[]{3,h-6}){rect(g,x+dx,y+dy,3,3,0xFF715A30);rect(g,x+dx,y+dy,1,1,0xFFE4CE8A);}
        rect(g,x+6,y+2,w-12,1,0xFFE3CE8C);
        rect(g,x+3,y+h-3,w-6,1,0xFF6A4C2B);
    }
    private void line(GuiGraphicsExtractor g,int x,int y,int xx,int yy,int color){
        int count=Math.max(Math.abs(xx-x),Math.abs(yy-y));
        for(int i=0;i<=count;i++){float t=count==0?0:(float)i/count;rect(g,x+Math.round((xx-x)*t),y+Math.round((yy-y)*t),1,1,color);}
    }
    private void ring(GuiGraphicsExtractor g,int x,int y,int radius,int color){
        for(int i=0;i<48;i++){double a=i*Math.PI/24;rect(g,x+(int)Math.round(Math.cos(a)*radius),y+(int)Math.round(Math.sin(a)*radius),1,1,color);}
    }
    private void gear(GuiGraphicsExtractor g,int x,int y,int radius){
        ring(g,x,y,radius,GOLD);ring(g,x,y,radius-3,EDGE);ring(g,x,y,2,GOLD);
        for(int i=0;i<10;i++){double a=i*Math.PI/5;rect(g,x+(int)(Math.cos(a)*radius)-1,y+(int)(Math.sin(a)*radius)-1,3,3,GOLD);}
    }
    @Override public void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt){
        rect(g,0,0,350,236,0xFF352319);
        for(int y=0;y<236;y+=9){rect(g,3,y,344,1,0xFF211810);for(int x=5;x<345;x+=17)rect(g,x,y+3,10,1,0xFF493123);}
        frame(g,3,3,344,32,0xFF32251B);
        for(int x:new int[]{4,332})for(int y:new int[]{4,222}){rect(g,x,y,14,4,GOLD);rect(g,x,y,4,10,GOLD);}
        text(g,"Quantum",13,9,0xFFE5D39C);text(g,"Industries",13,20,GOLD);
        text(g,tr("title").getString(),123,9,0xFFE5D39C);text(g,tr("era").getString(),154,21,GOLD);
        frame(g,5,39,65,102,0xFF35291E);
        text(g,tr("sample_label").getString(),16,44,GOLD);text(g,tr("tools_label").getString(),12,93,GOLD);
        frame(g,76,39,165,104,0xFF171B1C);
        g.pose().pushMatrix();g.pose().translate(leftPos+80,topPos+43);g.pose().scale(157f/1586f,96f/992f);
        g.blit(RenderPipelines.GUI_TEXTURED,INSTRUMENTS,0,0,0f,0f,1586,992,1586,992);
        g.pose().popMatrix();
        for(int i=0;i<3;i++){
            int y=45+i*32;
            rect(g,83,y,145,9,0xE8171B1C);
            boolean selected=menu.selectedMethod().ordinal()==i;
            if(selected){rect(g,80,y-2,157,1,GOLD);rect(g,80,y-1,2,29,GOLD);}
            text(g,(selected?"> ":"  ")+Component.translatable(Method.values()[i].translationKey()).getString(),84,y,0xFFE0CC93);
        }
        frame(g,245,39,99,104,0xFFAB874D);
        // Torn paper edge, warm ageing and binding pins.
        rect(g,251,44,87,95,PAPER);
        for(int i=0;i<8;i++){rect(g,250+(i%2),48+i*11,2,4,0xFFB3945B);rect(g,337-(i%2),50+i*11,2,3,0xFFB3945B);}
        for(int i=0;i<12;i++){int x=254+(i*31)%75,y=52+(i*19)%80;rect(g,x,y,2,1,0xFFCDB47C);}
        ring(g,257,49,2,0xFF9A783F);ring(g,331,49,2,0xFF9A783F);
        text(g,tr("notebook").getString(),269,46,INK);
        fitted(g,Component.translatable(menu.viewedMaterial().translationKey()).getString(),267,57,54,INK);
        rect(g,255,68,78,1,0xFFAF9562);
        for(var method:Method.values()){
            int y=72+method.ordinal()*18;
            boolean known=menu.knows(menu.viewedMaterial(),method);
            if(known){line(g,254,y+2,257,y+5,0xFF456644);line(g,257,y+5,262,y,0xFF456644);}
            else text(g,"[ ]",253,y,0xFF9E875C);
            fitted(g,tr("short."+method.id()).getString(),266,y,67,INK);
            String observed=known?Component.translatable(menu.viewedMaterial().observationKey(method)).getString():tr("unknown").getString();
            fitted(g,observed,254,y+8,79,0xFF796443);
        }
        text(g,menu.discoveries()+"/3  |  "+menu.totalDiscoveries()+"/30",260,130,INK);
        int progress=menu.progressTicks()*100/AssayCatalog.TEST_TICKS;
        rect(g,254,148,88,4,EDGE);rect(g,254,148,88*progress/100,4,GOLD);
        fitted(g,menu.running()?tr("running",progress).getString():toolStatus().getString(),253,179,89,GOLD);
        gear(g,26,200,17);gear(g,51,219,12);gear(g,14,229,6);
        // Crank and brass piping echo the mechanical controls in the reference.
        rect(g,283,192,4,29,GOLD);rect(g,283,192,41,4,GOLD);rect(g,320,185,4,11,GOLD);
        rect(g,267,218,19,4,GOLD);ring(g,267,220,4,EDGE);rect(g,252,218,14,5,0xFF70502D);
        frame(g,78,151,169,83,0xFF201B16);
        text(g,tr("inventory").getString(),133,144,GOLD);
        for(int i=0;i<menu.slots.size();i++){
            var s=menu.slots.get(i);slotCell(g,s.x-1,s.y-1);
        }
        Material current=menu.reservedMaterial();
        if(current==null)current=AssayCatalog.material(menu.getSlot(0).getItem());
        fitted(g,current==null?tr("no_sample").getString():Component.translatable(current.translationKey()).getString(),10,75,55,GOLD);
        if(menu.reservedMaterial()!=null){
            // A label, never another inventory slot or extractable copy of the sample.
            rect(g,9,84,57,7,0xFF423422);fitted(g,tr("reserved").getString(),10,84,55,0xFFE5D39C);
        }
        text(g,"1",14,128,GOLD);text(g,"2",35,128,GOLD);text(g,"3",56,128,GOLD);
        super.extractContents(g,mouseX,mouseY,dt);extractTooltip(g,mouseX,mouseY);
        for(int i=0;i<4;i++){
            var slot=menu.slots.get(i);
            if(!slot.hasItem()&&hovered(mouseX,mouseY,slot.x,slot.y,16,16))
                g.setTooltipForNextFrame(i==0?tr("sample"):Component.translatable(new String[]{"","item.quantumindustries.magnifying_glass","item.minecraft.flint_and_steel","item.quantumindustries.brass_weights"}[i]),mouseX,mouseY);
        }
        for(var method:Method.values())if(hovered(mouseX,mouseY,252,72+method.ordinal()*18,84,17)){
            Component observation=menu.knows(menu.viewedMaterial(),method)?Component.translatable(menu.viewedMaterial().observationKey(method)):tr("unknown");
            g.setTooltipForNextFrame(Component.translatable(method.translationKey()).append(": ").append(observation),mouseX,mouseY);
        }
        if(hovered(mouseX,mouseY,253,177,89,12))g.setTooltipForNextFrame(toolStatus(),mouseX,mouseY);
        if(hovered(mouseX,mouseY,9,74,57,17)&&current!=null){
            Component label=Component.translatable(current.translationKey());
            if(menu.reservedMaterial()!=null)label=tr("reserved_sample",label);
            g.setTooltipForNextFrame(label,mouseX,mouseY);
        }
        if(hovered(mouseX,mouseY,266,55,57,12))g.setTooltipForNextFrame(Component.translatable(menu.viewedMaterial().translationKey()),mouseX,mouseY);
    }
}
