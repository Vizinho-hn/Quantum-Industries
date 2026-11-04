package br.com.quantumindustries.assay;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared identifiers for server validation, notebook selection and localization. */
public final class AssayCatalog {
    public static final int MATERIAL_COUNT=10, METHOD_COUNT=3, TEST_TICKS=60;
    private AssayCatalog(){}
    public enum Method {
        MECHANICAL("mechanical"),HEATING("heating"),GALVANIC("galvanic");
        private final String id;
        Method(String id){this.id=id;}
        public String id(){return id;}
        public String translationKey(){return "gui.quantumindustries.assay.method."+id;}
    }
    public enum Material {
        COPPER("copper",Items.COPPER_INGOT),IRON("iron",Items.IRON_INGOT),GOLD("gold",Items.GOLD_INGOT),
        COAL("coal",Items.COAL),DIAMOND("diamond",Items.DIAMOND),QUARTZ("quartz",Items.QUARTZ),
        REDSTONE("redstone",Items.REDSTONE),LAPIS("lapis",Items.LAPIS_LAZULI),
        EMERALD("emerald",Items.EMERALD),AMETHYST("amethyst",Items.AMETHYST_SHARD);
        private final String id;
        private final Item item;
        Material(String id,Item item){this.id=id;this.item=item;}
        public String id(){return id;}
        public String translationKey(){return "gui.quantumindustries.assay.material."+id;}
        public String observationKey(Method method){return "gui.quantumindustries.assay.observation."+id+"."+method.id();}
        public ItemStack sample(){return new ItemStack(item);}
    }
    public static Material material(ItemStack stack){for(var m:Material.values())if(stack.is(m.item))return m;return null;}
}
