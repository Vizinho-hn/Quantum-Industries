package br.com.quantumindustries.client.gui;

import br.com.quantumindustries.menu.MachineProcessingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Era II casting chamber, drawn on the native pixel grid around real menu slots. */
public final class MachineProcessingScreen extends AbstractContainerScreen<MachineProcessingMenu> {
    private static final int STEEL=0xFF353B47, EDGE=0xFF7B88A1, DARK=0xFF14161A, COPPER=0xFFA34322;
    private static final int PAPER=0xFFDFD9CE, INK=0xFF292D36, AMBER=0xFFF58426;

    public MachineProcessingScreen(MachineProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 328, 238);
        titleLabelY=-10000;
        inventoryLabelY=-10000;
    }

    private void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(leftPos+x, topPos+y, leftPos+x+w, topPos+y+h, color);
    }
    private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        box(g,x,y,w,h,DARK); box(g,x+1,y+1,w-2,h-2,EDGE); box(g,x+3,y+3,w-6,h-6,fill);
    }
    private void label(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font,text,leftPos+x,topPos+y,color,false);
    }
    private void center(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        label(g,text,x-font.width(text)/2,y,color);
    }
    @Override protected void init() {
        super.init();
        if (menu.crusher()) addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.quantumindustries.crusher.crank"), button -> {
                    if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                }).bounds(leftPos + 204, topPos + 45, 101, 18).build());
    }

    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        panel(g,0,0,328,238,menu.crusher()?0xFF422C18:STEEL);
        for (int y : new int[]{5,28,139,233}) {
            box(g,5,y,318,2,COPPER);
            for(int x=7;x<320;x+=31) { box(g,x,y,2,2,PAPER); }
        }
        panel(g,37,6,254,23,PAPER);
        center(g,title,164,14,INK);
        panel(g,11,35,306,101,0xFF22262E);
        panel(g,20,44,18,74,DARK);
        int energyHeight=Math.clamp(menu.energy()*64/menu.capacity(),0,64);
        box(g,25,49+64-energyHeight,8,energyHeight,AMBER);
        for(int y=54;y<113;y+=10) box(g,25,y,3,1,COPPER);
        label(g,Component.literal(menu.energy()+" / "+menu.capacity()+" FE"),14,119,PAPER);
        if (menu.crusher()) panel(g,135,44,24,24,DARK);
        else { panel(g,108,44,24,24,DARK); panel(g,162,44,24,24,DARK); }
        for(int x:menu.crusher()?new int[]{146}:new int[]{119,173}) {
            box(g,x,71,2,9,0xFFE0BA67);
            box(g,x-3,77,8,2,0xFFE0BA67);
            box(g,x-1,79,4,2,0xFFE0BA67);
        }
        panel(g,106,84,86,28,COPPER);
        box(g,113,89,72,14,DARK);
        if(menu.crusher()) {
            for(int x=116;x<183;x+=10) {
                box(g,x,89,6,menu.working()?8:5,EDGE);
                box(g,x+3,99,6,menu.working()?6:4,0xFFBFB8AD);
            }
        } else {
            box(g,117,92,64,9,menu.working()?AMBER:0xFF5E2919);
            for(int x=118;x<182;x+=9) box(g,x,96,4,3,menu.working()?0xFFFFD266:0xFF853D27);
        }
        box(g,112,109,74,3,EDGE);
        for(int x:new int[]{101,194}) {
            box(g,x,90,4,19,COPPER);box(g,x-1,94,6,3,0xFFBD9242);box(g,x-1,102,6,3,0xFFBD9242);
        }
        box(g,203,97,39,5,0xFFE0BA67);
        box(g,237,93,3,13,0xFFE0BA67);
        box(g,240,96,3,7,0xFFE0BA67);
        box(g,243,98,2,3,0xFFE0BA67);
        panel(g,255,84,32,32,PAPER);
        var recipe=menu.recipe();
        if(menu.crusher()) center(g,Component.literal("3 FE/t"),225,72,PAPER);
        if(recipe!=null) center(g,Component.literal(String.format(java.util.Locale.ROOT,"%.1f FE/t",(float)recipe.totalFe()/recipe.workTicks())),225,72,PAPER);
        panel(g,106,117,180,12,DARK);
        int percent=menu.duration()==0?0:Math.clamp(menu.progress()*100/menu.duration(),0,100);
        box(g,110,121,172*percent/100,4,AMBER);
        center(g,Component.literal(percent+"%"),305,120,PAPER);
        Component status;
        if (!menu.permitted()) {
            status = Component.translatable("gui.quantumindustries.processing.research_required");
        } else if (!menu.result().isEmpty()) {
            if (menu.crusher()) {
                String key = menu.working() ? "crushing_material" : "manual_material";
                status = Component.translatable("gui.quantumindustries.crusher." + key,
                        menu.getSlot(0).getItem().getHoverName());
            } else {
                String key = menu.working() ? "melting" : "paused_material";
                status = Component.translatable("gui.quantumindustries.smelter." + key,
                        Component.translatable("gui.quantumindustries.smelter.alloy." + recipe.alloyId()));
            }
        } else if (menu.getSlot(menu.inputCount()).hasItem()) {
            status = Component.translatable("gui.quantumindustries.smelter.result_material",
                    menu.getSlot(menu.inputCount()).getItem().getHoverName());
        } else {
            status = Component.translatable("gui.quantumindustries.smelter.waiting_pair");
        }
        box(g,11,128,306,13,DARK);
        center(g,status,164,130,PAPER);
        panel(g,68,141,192,70,PAPER);
        center(g,Component.translatable("container.inventory"),164,145,INK);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slotFrame(g,82+col*18,155+row*18);
        for(int col=0;col<9;col++)slotFrame(g,82+col*18,213);
        super.extractContents(g,mouseX,mouseY,partial);
        extractTooltip(g,mouseX,mouseY);
    }
    private void slotFrame(GuiGraphicsExtractor g,int x,int y) {
        box(g,x,y,18,18,0xFF968F84);box(g,x+1,y+1,17,17,DARK);box(g,x+1,y+1,16,16,0xFF474D59);
    }
}
