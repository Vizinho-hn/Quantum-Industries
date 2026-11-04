package br.com.quantumindustries.assay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Stable string keys, deliberately independent from technological research. */
public final class AssayProfile {
    public static final String KEY = "quantum_assay_v1";
    private final CompoundTag root;
    private final CompoundTag discoveries;
    private AssayProfile(Player player) {
        CompoundTag data=player.getPersistentData();
        root=data.getCompound(KEY).orElseGet(CompoundTag::new);
        discoveries=root.getCompound("discoveries").orElseGet(CompoundTag::new);
        if(!root.getBoolean("legacy_migrated").orElse(false)) {
            int legacy=Math.clamp(data.getInt("quantum_empirical_copper").orElse(0),0,3);
            for(int i=0;i<legacy;i++)discoveries.putBoolean(key(AssayCatalog.Material.COPPER,AssayCatalog.Method.values()[i]),true);
            root.putBoolean("legacy_migrated",true);
        }
        root.put("discoveries",discoveries);data.put(KEY,root);
    }
    public static AssayProfile of(Player player){return new AssayProfile(player);}
    private static String key(AssayCatalog.Material m,AssayCatalog.Method method){return m.id()+"/"+method.id();}
    public boolean knows(AssayCatalog.Material m,AssayCatalog.Method method){return discoveries.getBoolean(key(m,method)).orElse(false);}
    public int mask(AssayCatalog.Material m){int mask=0;for(var method:AssayCatalog.Method.values())if(knows(m,method))mask|=1<<method.ordinal();return mask;}
    public AssayCatalog.Material reserved(){String id=root.getString("reserved_material").orElse("");for(var m:AssayCatalog.Material.values())if(m.id().equals(id))return m;return null;}
    public boolean reserve(AssayCatalog.Material m){if(reserved()!=null||mask(m)==7)return false;root.putString("reserved_material",m.id());return true;}
    public boolean complete(AssayCatalog.Material m,AssayCatalog.Method method){
        if(reserved()!=m||knows(m,method))return false;
        discoveries.putBoolean(key(m,method),true);
        if(mask(m)==7)root.remove("reserved_material");
        return true;
    }
}
