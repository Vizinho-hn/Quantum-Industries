package br.com.quantumindustries.client.render;

import br.com.quantumindustries.ModItems;
import br.com.quantumindustries.block.entity.MechanicalSawmillBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Inventory and activity are synchronized from the server, never inferred from rotor animation. */
public final class SawmillInspectionHud {
    private static final int COPPER=0xFFB37B55, PAPER=0xFFF4DCB9, TEXT=0xFFF4EEE6, GOLD=0xFFE8BC65;
    private SawmillInspectionHud() {}
    public static int draw(GuiGraphicsExtractor g, Minecraft game, MechanicalSawmillBlockEntity saw) {
        var state=saw.processingState();
        var in=Component.translatable("gui.quantumindustries.sawmill_hud.input", transport(saw.inputTransportState(),true));
        var cut=Component.translatable("gui.quantumindustries.sawmill_hud.saw", Component.translatable("gui.quantumindustries.sawmill_hud.state."+state.name().toLowerCase(java.util.Locale.ROOT)));
        var out=Component.translatable("gui.quantumindustries.sawmill_hud.output",transport(saw.outputTransportState(),false));
        var logs=Component.translatable("gui.quantumindustries.sawmill_hud.logs",saw.input().getCount());
        var planks=Component.translatable("gui.quantumindustries.sawmill_hud.planks",saw.output().getCount());
        var hint=Component.translatable(!saw.automationBound()?"gui.quantumindustries.sawmill_hud.authorize":saw.outputTransportState()==0?"gui.quantumindustries.sawmill_hud.connect":"gui.quantumindustries.sawmill_hud.connected");
        var title=Component.translatable("block.quantumindustries.mechanical_sawmill");
        int width=Math.max(280,game.font.width(title)+80), height=196;
        for(var line:List.of(in,cut,out,logs,planks,hint))width=Math.max(width,game.font.width(line)+72);
        float scale=Math.min(1f,Math.min((g.guiWidth()-14f)/width,(g.guiHeight()-14f)/height));
        g.pose().pushMatrix();
        try{
            g.pose().translate(7,7).scale(scale);
            g.fill(2,0,width-2,height,COPPER);g.fill(0,2,width,height-2,COPPER);
            g.fill(2,2,width-2,height-2,0xED201C19);
            g.fill(7,31,width-7,32,COPPER);g.fill(7,103,width-7,104,COPPER);g.fill(7,169,width-7,170,COPPER);
            g.fakeItem(new ItemStack(ModItems.BRONZE_GEAR.get()),12,9);
            g.text(game.font,title,36,13,PAPER,false);g.text(game.font,"QI ///",width-43,13,COPPER,false);
            arrowIcon(g,game,13,43);g.text(game.font,in,36,45,activityColor(saw.inputTransportState()),false);
            g.fakeItem(new ItemStack(ModItems.BRONZE_GEAR.get()),12,61);g.text(game.font,cut,36,65,state==MechanicalSawmillBlockEntity.ProcessingState.CUTTING?GOLD:TEXT,false);
            arrowIcon(g,game,13,83);g.text(game.font,out,36,85,activityColor(saw.outputTransportState()),false);
            g.fakeItem(saw.input().isEmpty()?new ItemStack(Items.OAK_LOG):saw.input(),width-35,41);
            g.fakeItem(saw.output().isEmpty()?new ItemStack(Items.OAK_PLANKS):saw.output(),width-35,81);
            g.fakeItem(saw.input().isEmpty()?new ItemStack(Items.OAK_LOG):saw.input(),12,111);g.text(game.font,logs,36,115,TEXT,false);
            g.fakeItem(saw.output().isEmpty()?new ItemStack(Items.OAK_PLANKS):saw.output(),12,131);g.text(game.font,planks,36,135,TEXT,false);
            g.text(game.font,hint,12,156,0xFFB7C19B,false);
            int barRight=width-98;g.fill(12,179,barRight,187,0xFF554336);
            int duration=saw.processingDuration();float progress=duration<=0?0:Math.clamp(saw.progress()/(float)duration,0,1);
            if(progress>0)g.fill(12,179,12+Math.round((barRight-12)*progress),187,GOLD);
            int chip=state==MechanicalSawmillBlockEntity.ProcessingState.CUTTING?0xFF83C66D:state==MechanicalSawmillBlockEntity.ProcessingState.OUTPUT_FULL?0xFFE18565:GOLD;
            g.fill(width-87,175,width-10,191,chip);g.fill(width-86,176,width-11,190,0xFF201C19);
            var label=Component.translatable("gui.quantumindustries.sawmill_hud.badge."+state.name().toLowerCase(java.util.Locale.ROOT));
            g.text(game.font,label,width-82,179,chip,false);
        }finally{g.pose().popMatrix();}
        return 7+Math.round(height*scale);
    }
    private static Component transport(int state,boolean input){return Component.translatable("gui.quantumindustries.sawmill_hud.transport."+(state==2?(input?"receiving":"sending"):state==3?"blocked":state==1?"waiting":"missing"));}
    private static int activityColor(int state){return state==2?0xFF9DD46C:state==3?0xFFE18565:state==1?GOLD:0xFFC3B49F;}
    private static void arrowIcon(GuiGraphicsExtractor g,Minecraft game,int x,int y){
        g.fill(x,y,x+12,y+12,COPPER);g.fill(x+1,y+1,x+11,y+11,0xFF201C19);
        g.text(game.font,">",x+5,y+2,GOLD,false);
    }
}
