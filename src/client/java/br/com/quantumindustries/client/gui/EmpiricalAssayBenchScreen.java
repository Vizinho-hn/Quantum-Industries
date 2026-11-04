package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
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
    public EmpiricalAssayBenchScreen(EmpiricalAssayBenchMenu menu,Inventory inv,Component title){
        super(menu,inv,title,350,236);titleLabelY=-100;inventoryLabelY=-100;
    }
    @Override protected void init(){
        super.init();
        action=addRenderableWidget(new Button(Button.builder(Component.literal("Realizar ensaio"),b->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+254,topPos+157,88,18)) {
            @Override protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector collector) {}
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt) {
                extractDefaultSprite(g);
                g.pose().pushMatrix();g.pose().translate(getX()+getWidth()/2f,getY()+6);g.pose().scale(.75f,.75f);
                g.text(font,getMessage(),-font.width(getMessage())/2,0,active?0xFFE6D299:0xFF999185,false);
                g.pose().popMatrix();
            }
        });
    }
    @Override protected void containerTick(){super.containerTick();action.active=menu.ready()&&menu.discoveries()<3;}
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
        text(g,"Analisador empírico",123,9,0xFFE5D39C);text(g,"Manual / Era 0",154,21,GOLD);
        frame(g,5,39,65,102,0xFF35291E);
        text(g,"Amostra",16,44,GOLD);text(g,"Ferramentas",12,93,GOLD);
        frame(g,76,39,165,104,0xFF171B1C);
        g.pose().pushMatrix();g.pose().translate(leftPos+80,topPos+43);g.pose().scale(157f/1586f,96f/992f);
        g.blit(RenderPipelines.GUI_TEXTURED,INSTRUMENTS,0,0,0f,0f,1586,992,1586,992);
        g.pose().popMatrix();
        for(int i=0;i<3;i++){
            int y=45+i*32;
            rect(g,83,y,145,9,0xE8171B1C);
            text(g,new String[]{"1. Prensa e proveta","2. Ensaio de chama","3. Ensaio galvânico"}[i],84,y,0xFFE0CC93);
        }
        frame(g,245,39,99,104,0xFFAB874D);
        // Torn paper edge, warm ageing and binding pins.
        rect(g,251,44,87,95,PAPER);
        for(int i=0;i<8;i++){rect(g,250+(i%2),48+i*11,2,4,0xFFB3945B);rect(g,337-(i%2),50+i*11,2,3,0xFFB3945B);}
        for(int i=0;i<12;i++){int x=254+(i*31)%75,y=52+(i*19)%80;rect(g,x,y,2,1,0xFFCDB47C);}
        ring(g,257,49,2,0xFF9A783F);ring(g,331,49,2,0xFF9A783F);
        text(g,"Caderno",269,47,INK);rect(g,255,58,78,1,0xFFAF9562);
        String[][] notes={{"Dúctil","Fio metálico"},{"Chama verde","Indício de Cu"},{"Condutor","Galvânico"}};
        for(int i=0;i<3;i++){
            int y=66+i*20;
            if(menu.discoveries()>i){line(g,254,y+3,257,y+6,0xFF456644);line(g,257,y+6,263,y,0xFF456644);}
            else text(g,"[ ]",253,y,0xFF9E875C);
            text(g,notes[i][0],273,y,INK);text(g,notes[i][1],254,y+10,0xFF796443);
            if(menu.discoveries()>i){line(g,274,y+17,318,y+17,0xFFAA8D58);}
        }
        text(g,menu.discoveries()+"/3 descobertas",258,132,INK);
        text(g,menu.ready()?(menu.discoveries()==3?"Registrado":"Preparado"):"Falta ferramenta",253,179,GOLD);
        gear(g,26,200,17);gear(g,51,219,12);gear(g,14,229,6);
        // Crank and brass piping echo the mechanical controls in the reference.
        rect(g,283,192,4,29,GOLD);rect(g,283,192,41,4,GOLD);rect(g,320,185,4,11,GOLD);
        rect(g,267,218,19,4,GOLD);ring(g,267,220,4,EDGE);rect(g,252,218,14,5,0xFF70502D);
        frame(g,78,151,169,83,0xFF201B16);
        text(g,"Inventário",133,144,GOLD);
        for(int i=0;i<menu.slots.size();i++){
            var s=menu.slots.get(i);frame(g,s.x-2,s.y-2,20,20,0xFF201E1A);
        }
        text(g,"Cobre",23,75,GOLD);text(g,"1",14,128,GOLD);text(g,"2",35,128,GOLD);text(g,"3",56,128,GOLD);
        super.extractContents(g,mouseX,mouseY,dt);extractTooltip(g,mouseX,mouseY);
        for(int i=0;i<4;i++){
            var slot=menu.slots.get(i);
            if(!slot.hasItem()&&mouseX>=leftPos+slot.x&&mouseX<leftPos+slot.x+16&&mouseY>=topPos+slot.y&&mouseY<topPos+slot.y+16)
                g.setTooltipForNextFrame(Component.literal(new String[]{"Amostra: lingote de cobre","1. Vidro (inspeção)","2. Pederneira e aço (ignição)","3. Pepita de ferro (peso)"}[i]),mouseX,mouseY);
        }
    }
    private void scene(GuiGraphicsExtractor g,int stage,int y){
        boolean moving=false;
        int pulse=0;
        if(stage==0){
            rect(g,111,y,5,15,GOLD);rect(g,112,y,64,4,GOLD);rect(g,171,y,5,15,GOLD);
            rect(g,112,y+13,64,3,GOLD);rect(g,128,y+5,4,9,0xFF9EA8AB);rect(g,158-pulse,y+5,4,9,0xFF9EA8AB);
            rect(g,137,y+8,16,5,0xFFCD8053);rect(g,100,y+7,30,2,0xFF808C8D);
            for(int i=0;i<6;i++)rect(g,104+i*4,y+5,1,6,0xFFABAD9F);
            rect(g,203,y-1,12,18,0xFF738F93);rect(g,205,y+7-pulse,8,8+pulse,0xFF5C9AA5);
            for(int i=0;i<4;i++)rect(g,212,y+i*4,3,1,0xFFD7D6B9);
            // Machined edges, heavy base, feed screw and connecting hydraulic pipe.
            rect(g,107,y+16,73,2,0xFF695637);rect(g,114,y+1,59,1,0xFFE2CA83);
            rect(g,119,y+4,2,9,0xFF483F2A);rect(g,164,y+4,2,9,0xFF483F2A);
            rect(g,133,y+11,25,2,0xFFE9A175);rect(g,97,y+3,2,10,GOLD);
            line(g,177,y+8,187,y+8,0xFF92A6A5);line(g,187,y+8,187,y+15,0xFF92A6A5);line(g,187,y+15,202,y+15,0xFF92A6A5);
            rect(g,201,y-2,16,2,GOLD);rect(g,201,y+17,16,2,GOLD);
            for(int i=0;i<3;i++)rect(g,206+i%2*3,y+9+i*2,1,1,0xFFB9E2DA);
            rect(g,111,y+2,2,11,0xFF6D5935);rect(g,173,y+2,2,11,0xFF6D5935);
            rect(g,129,y+5,1,8,0xFFD8DED5);rect(g,159,y+5,1,8,0xFFD8DED5);
            rect(g,203,y,1,15,0xFFD1E4D7);rect(g,214,y,1,15,0xFF354C4F);
            for(int x:new int[]{114,170}){rect(g,x,y+1,2,2,0xFFF1D794);rect(g,x,y+14,2,2,0xFF8C713B);}
        }else if(stage==1){
            rect(g,101,y+10,21,5,GOLD);rect(g,121,y+7,21,3,GOLD);
            int color=menu.discoveries()>1?0xFF6BD084:0xFFB1955C;
            for(int i=0;i<14;i++){int h=2+(int)(3*Math.abs(Math.sin(i*.8)));rect(g,143+i*2,y+8-h,3,h*2,color);}
            rect(g,180,y+9,16,5,0xFFCD8053);rect(g,178,y+14,23,2,0xFF666263);
            rect(g,95,y+12,7,4,0xFF80653B);rect(g,107,y+3,3,8,GOLD);rect(g,105,y+1,7,3,0xFFDDC078);
            for(int i=0;i<6;i++)rect(g,119+i*3,y+6,1,5,0xFF6A512F);
            if(menu.discoveries()>1){
                for(int i=0;i<11;i++){int h=2+(int)(Math.abs(Math.sin(i*.9))*3);rect(g,149+i*2,y+8-h,2,h,0xFFBEF7A0);}
            }
            rect(g,196,y+5,12,11,0xFF3A3430);rect(g,197,y+9,10,3,0xFFC86B2D);
            rect(g,209,y-1,2,14,0xFF7F6B49);rect(g,202,y-1,9,2,0xFF7F6B49);
            for(int i=0;i<3;i++){int rise=i*3;rect(g,184+i*4,y+3-rise,3,2,0xFF555650);}
            rect(g,102,y+10,18,1,0xFFE6CC86);rect(g,124,y+7,16,1,0xFFE6CC86);
            rect(g,181,y+9,14,1,0xFFE4AB7B);rect(g,181,y+13,14,1,0xFF915237);
        }else{
            rect(g,128,y+1,57,14,0xFF426467);rect(g,130,y+3,53,10,0xFF91B3AC);
            rect(g,121,y+4,5,7,GOLD);rect(g,110,y+7,13,2,GOLD);rect(g,107,y+5+pulse,4,7,GOLD);
            rect(g,154,y+2,2,12,GOLD);rect(g,145,y+7,20,1,0xFFB15B3E);
            rect(g,204,y+3,16,12,0xFF64716A);rect(g,206,y+5,12,8,PAPER);rect(g,211,y+5,2,8,0xFFB15B3E);
            rect(g,126,y,61,1,0xFFCCB877);rect(g,126,y+15,61,2,GOLD);
            for(int i=0;i<6;i++)line(g,137+i*6,y+3,133+i*6,y+12,0xFF5E8280);
            ring(g,211,y+8,8,GOLD);ring(g,211,y+8,6,PAPER);
            int needle=menu.discoveries()>=3?-5:2;
            line(g,211,y+8,214,y+8+needle,0xFFAE503A);line(g,211,y+8,208,y+8-needle,0xFF526969);
            line(g,186,y+8,199,y+8,0xFFB3A377);
            rect(g,131,y+3,50,1,0xFFCBDAD0);rect(g,131,y+12,50,1,0xFF587572);
            rect(g,129,y+1,2,14,0xFFD3BD7C);rect(g,183,y+1,2,14,0xFF80693D);
            for(int x:new int[]{137,177})rect(g,x,y+16,3,2,0xFF5F5035);
            ring(g,111,y+8,3,0xFFE4C883);
            if(moving&&menu.discoveries()>=3){line(g,188,y+8,191,y+5,0xFF99CDE3);line(g,191,y+5,193,y+11,0xFF99CDE3);line(g,193,y+11,197,y+7,0xFF99CDE3);}
        }
    }
}
