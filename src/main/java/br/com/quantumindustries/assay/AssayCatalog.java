package br.com.quantumindustries.assay;

import br.com.quantumindustries.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared identifiers for server validation, notebook selection and localization. */
public final class AssayCatalog {
    public static final int MATERIAL_COUNT=13, METHOD_COUNT=4, TEST_TICKS=200;
    private AssayCatalog(){}
    public enum Method {
        INSPECTION("inspection"),HEATING("heating"),REACTION("reaction"),MECHANICAL("mechanical");
        private final String id;
        Method(String id){this.id=id;}
        public String id(){return id;}
        public String translationKey(){return "gui.quantumindustries.assay.method."+id;}
    }
    public static final int COMPLETE_MASK=(1<<METHOD_COUNT)-1;
    public static Method method(String id){for(var m:Method.values())if(m.id().equals(id))return m;return null;}
    public enum Material {
        COPPER("copper",()->Items.COPPER_INGOT),IRON("iron",()->Items.IRON_INGOT),GOLD("gold",()->Items.GOLD_INGOT),
        COAL("coal",()->Items.COAL),DIAMOND("diamond",()->Items.DIAMOND),QUARTZ("quartz",()->Items.QUARTZ),
        REDSTONE("redstone",()->Items.REDSTONE),LAPIS("lapis",()->Items.LAPIS_LAZULI),
        EMERALD("emerald",()->Items.EMERALD),AMETHYST("amethyst",()->Items.AMETHYST_SHARD),
        TIN("tin",()->ModItems.INGOTS.get("tin").get()),
        ZINC("zinc",()->ModItems.INGOTS.get("zinc").get()),
        LEAD("lead",()->ModItems.INGOTS.get("lead").get());
        private final String id;
        private final Supplier<Item> item;
        Material(String id,Supplier<Item> item){this.id=id;this.item=item;}
        public String id(){return id;}
        public Item item(){return item.get();}
        public String translationKey(){return "gui.quantumindustries.assay.material."+id;}
        public String observationKey(Method method){return "gui.quantumindustries.assay.observation."+id+"."+method.id();}
        public int reactionColor(){return switch(this){case COPPER->0xFF5486B0;case IRON->0xFFB18A43;case COAL->0xFF33343C;case REDSTONE->0xFFB23B36;case LAPIS->0xFF4063A8;case TIN->0xFF8AA8B8;case ZINC->0xFF9FB8AD;case LEAD->0xFF6A7080;default->0xFF497FA5;};}
        public boolean reactive(){return this==COPPER||this==IRON||this==COAL||this==REDSTONE||this==LAPIS||this==TIN||this==ZINC||this==LEAD;}
        public boolean malleable(){return this==COPPER||this==IRON||this==GOLD||this==TIN||this==ZINC||this==LEAD;}
        public int color(){return switch(this){case COPPER->0xFFD77A42;case IRON->0xFFB8BFC0;case GOLD->0xFFECC34C;case COAL->0xFF343039;case DIAMOND->0xFF63DCD1;case QUARTZ->0xFFE9D8C8;case REDSTONE->0xFFAF3030;case LAPIS->0xFF3A64B6;case EMERALD->0xFF39BE7D;case AMETHYST->0xFFB489D6;case TIN->0xFFD0DCE5;case ZINC->0xFFA8B8B0;case LEAD->0xFF5C6274;};}
        public ItemStack sample(){return new ItemStack(item.get());}
    }
    public static Material material(ItemStack stack){
        if(stack.is(Items.RAW_COPPER))return Material.COPPER;
        if(stack.is(Items.RAW_IRON))return Material.IRON;
        if(stack.is(Items.RAW_GOLD))return Material.GOLD;
        if(ModItems.RAW_MATERIALS.containsKey("tin")&&stack.is(ModItems.RAW_MATERIALS.get("tin").get()))return Material.TIN;
        if(ModItems.RAW_MATERIALS.containsKey("zinc")&&stack.is(ModItems.RAW_MATERIALS.get("zinc").get()))return Material.ZINC;
        if(ModItems.RAW_MATERIALS.containsKey("lead")&&stack.is(ModItems.RAW_MATERIALS.get("lead").get()))return Material.LEAD;
        if(ModItems.METAL_DUSTS.containsKey("copper")&&stack.is(ModItems.METAL_DUSTS.get("copper").get()))return Material.COPPER;
        if(ModItems.METAL_DUSTS.containsKey("iron")&&stack.is(ModItems.METAL_DUSTS.get("iron").get()))return Material.IRON;
        if(ModItems.METAL_DUSTS.containsKey("gold")&&stack.is(ModItems.METAL_DUSTS.get("gold").get()))return Material.GOLD;
        if(ModItems.METAL_DUSTS.containsKey("tin")&&stack.is(ModItems.METAL_DUSTS.get("tin").get()))return Material.TIN;
        if(ModItems.METAL_DUSTS.containsKey("zinc")&&stack.is(ModItems.METAL_DUSTS.get("zinc").get()))return Material.ZINC;
        if(ModItems.METAL_DUSTS.containsKey("lead")&&stack.is(ModItems.METAL_DUSTS.get("lead").get()))return Material.LEAD;
        for(var m:Material.values())if(stack.is(m.item()))return m;
        return null;
    }
}
