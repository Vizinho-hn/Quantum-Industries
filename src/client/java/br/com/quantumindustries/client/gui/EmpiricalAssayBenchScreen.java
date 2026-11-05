package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.assay.*;
import br.com.quantumindustries.assay.AssayCatalog.*;
import br.com.quantumindustries.client.assay.*;
import br.com.quantumindustries.menu.EmpiricalAssayBenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Live view of the same apparatus, with no parallel fake experiment in the GUI. */
public final class EmpiricalAssayBenchScreen extends AbstractContainerScreen<EmpiricalAssayBenchMenu> {
    private static final Identifier BACKGROUND=Identifier.fromNamespaceAndPath("quantumindustries","textures/gui/empirical_workshop.png");
    private static final int LIGHT=0xFFE0D9BE,INK=0xFF392F27,MUTED=0xFFA9AAA0;
    private final Button[] methods=new Button[AssayCatalog.METHOD_COUNT];
    private Button action;
    public EmpiricalAssayBenchScreen(EmpiricalAssayBenchMenu menu,Inventory inventory,Component title){super(menu,inventory,title,380,236);titleLabelY=-100;inventoryLabelY=-100;}
    private Component tr(String key,Object... args){return Component.translatable("gui.quantumindustries.assay."+key,args);}
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private Button add(Component label,int x,int y,int w,int h,Runnable run){
        return addRenderableWidget(new Button(Button.builder(label,b->run.run()).bounds(leftPos+x,topPos+y,w,h)){
            @Override protected void extractDefaultLabel(net.minecraft.client.gui.ActiveTextCollector collector){}
            @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial){
                int edge=isHoveredOrFocused()?0xFFBEC6AE:0xFF70746C;
                g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),edge);
                g.fill(getX()+1,getY()+1,getX()+getWidth()-1,getY()+getHeight()-1,active?0xFF343832:0xFF282C28);
                g.pose().pushMatrix();g.pose().translate(getX()+getWidth()/2f,getY()+(getHeight()-6)/2f);g.pose().scale(.75f,.75f);
                g.text(font,getMessage(),-font.width(getMessage())/2,0,active?LIGHT:0xFF777E73,false);g.pose().popMatrix();
            }
        });
    }
    @Override protected void init(){
        super.init();
        for(var method:Method.values())methods[method.ordinal()]=add(tr("short."+method.id()),83+method.ordinal()*48,132,47,18,()->send(1+method.ordinal()));
        action=add(tr("perform"),283,158,88,20,()->send(0));
        add(Component.literal("‹"),287,128,16,16,()->send(10+(menu.viewedMaterial().ordinal()+AssayCatalog.MATERIAL_COUNT-1)%AssayCatalog.MATERIAL_COUNT));
        add(Component.literal("›"),353,128,16,16,()->send(10+(menu.viewedMaterial().ordinal()+1)%AssayCatalog.MATERIAL_COUNT));
    }
    @Override protected void containerTick(){super.containerTick();action.active=menu.ready();for(var button:methods)button.active=!menu.running();}
    private void text(GuiGraphicsExtractor g,Component s,int x,int y,int color){g.text(font,s,leftPos+x,topPos+y,color,false);}
    private void small(GuiGraphicsExtractor g,Component s,int x,int y,int maxWidth,int color){
        g.pose().pushMatrix();g.pose().translate(leftPos+x,topPos+y);g.pose().scale(.75f,.75f);
        String v=s.getString();int w=(int)(maxWidth/.75f);if(font.width(v)>w)v=font.plainSubstrByWidth(v,w-font.width("…"))+"…";
        g.text(font,v,0,0,color,false);g.pose().popMatrix();
    }
    private void rect(GuiGraphicsExtractor g,int x,int y,int w,int h,int c){g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,c);}
    private Component status(){
        if(!menu.editable()&&!menu.running())return tr("occupied",menu.bench()==null?"":menu.bench().researcherName());
        if(menu.running())return tr("running_live");
        if(menu.reservedMaterial()==null&&AssayCatalog.material(menu.getSlot(0).getItem())==null)return tr("no_sample");
        if(menu.knows(menu.reservedMaterial()!=null?menu.reservedMaterial():AssayCatalog.material(menu.getSlot(0).getItem()),menu.selectedMethod()))return tr("already_known");
        if(menu.missingTools()!=0)return tr("required."+menu.selectedMethod().id());
        return tr("ready");
    }
    @Override public void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        g.blit(RenderPipelines.GUI_TEXTURED,BACKGROUND,leftPos,topPos,0f,0f,380,236,380,236);
        small(g,Component.literal("QUANTUM INDUSTRIES"),13,11,115,LIGHT);
        text(g,tr("title"),145,10,LIGHT);small(g,tr("era"),145,23,210,MUTED);
        small(g,tr("sample_label"),17,43,58,LIGHT);small(g,tr("tools_label"),12,98,62,LIGHT);
        Material material=AssayCatalog.material(menu.experimentSample());
        if(material==null)material=menu.reservedMaterial();
        small(g,material==null?tr("no_sample"):Component.translatable(material.translationKey()),10,84,64,LIGHT);
        float ticks=menu.progressTicks()+(menu.running()?partial:0);
        var motion=new AssayMotion(menu.running()?menu.selectedMethod():null,ticks);
        var actualSample=new net.minecraft.client.renderer.item.ItemStackRenderState();
        minecraft.getItemModelResolver().updateForTopItem(actualSample,menu.experimentSample(),net.minecraft.world.item.ItemDisplayContext.FIXED,minecraft.level,null,0);
        // Actual 3D geometry; only the viewing camera changes between this and the world.
        g.submitPictureInPictureRenderState(new AssayPictureState(leftPos+86,topPos+49,leftPos+270,topPos+114,menu.selectedMethod()==Method.HEATING||menu.selectedMethod()==Method.REACTION?85f:65f,motion,material,menu.selectedMethod(),menu.bench()==null?0:menu.bench().toolMask(),actualSample));
        small(g,Component.translatable(menu.selectedMethod().translationKey()),90,41,168,LIGHT);
        rect(g,89,125,178,3,0xFF353E3E);
        if(menu.running())rect(g,89,125,(int)(178*Math.min(1,ticks/200)),3,0xFFAEC6AD);
        if(menu.running()){
            small(g,tr("phase."+motion.phase()),90,116,85,LIGHT);
            small(g,tr("remaining",Math.max(0,(200-menu.progressTicks()+19)/20)),184,116,82,LIGHT);
        }
        for(var method:Method.values())if(menu.selectedMethod()==method)rect(g,84+method.ordinal()*48,150,45,1,0xFFD9E5D0);
        small(g,tr("notebook"),291,43,78,INK);
        small(g,Component.translatable(menu.viewedMaterial().translationKey()),291,55,78,INK);
        for(var method:Method.values()){
            int y=70+method.ordinal()*14;boolean known=menu.knows(menu.viewedMaterial(),method);
            if(known&&menu.selectedMethod()==method)rect(g,289,y-1,80,9,0x22345C3B);
            small(g,Component.literal(known?"✓ ":"□ ").append(tr("short."+method.id())),291,y,78,known?0xFF345C3B:INK);
        }
        small(g,Component.literal(menu.discoveries()+"/4 · "+menu.totalDiscoveries()+"/40"),310,133,42,INK);

        small(g,status(),285,185,85,LIGHT);
        small(g,tr("world_hint"),285,202,85,MUTED);
        small(g,tr("automatic_record"),285,215,85,MUTED);
        // Slots use dark recessed metal rather than the editor's default light squares.
        for(var slot:menu.slots){
            rect(g,slot.x-1,slot.y-1,18,18,0xFF161A19);rect(g,slot.x,slot.y,16,16,0xFF34372F);
            rect(g,slot.x,slot.y,16,1,0xFF101511);rect(g,slot.x,slot.y+15,16,1,0xFF77796A);
        }
        rect(g,0,0,380,2,0xFF777B70);rect(g,0,234,380,2,0xFF171B19);
        rect(g,0,0,2,236,0xFF777B70);rect(g,378,0,2,236,0xFF171B19);
        super.extractContents(g,mouseX,mouseY,partial);extractTooltip(g,mouseX,mouseY);
        for(var method:Method.values())if(mouseX>=leftPos+288&&mouseX<leftPos+370&&mouseY>=topPos+69+method.ordinal()*14&&mouseY<topPos+83+method.ordinal()*14){
            var observation=menu.knows(menu.viewedMaterial(),method)?Component.translatable(menu.viewedMaterial().observationKey(method)):tr("unknown");
            g.setTooltipForNextFrame(observation,mouseX,mouseY);
        }
        if(mouseX>=leftPos+282&&mouseX<leftPos+374&&mouseY>=topPos+180&&mouseY<topPos+200)g.setTooltipForNextFrame(status(),mouseX,mouseY);
        for(int i=1;i<4;i++){
            var slot=menu.getSlot(i);if(!slot.hasItem()&&mouseX>=leftPos+slot.x&&mouseX<leftPos+slot.x+16&&mouseY>=topPos+slot.y&&mouseY<topPos+slot.y+16)
                g.setTooltipForNextFrame(Component.translatable(new String[]{"","item.quantumindustries.magnifying_glass","item.quantumindustries.manual_bellows","item.quantumindustries.assay_flask"}[i]),mouseX,mouseY);
        }
    }
}
