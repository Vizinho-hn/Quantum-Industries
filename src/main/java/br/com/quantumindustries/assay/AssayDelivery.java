package br.com.quantumindustries.assay;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** Idempotent, persisted delivery of results to disconnected authors. */
public final class AssayDelivery extends SavedData {
    private final Map<String,List<String>> pending;
    private static final Codec<AssayDelivery> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING.listOf()).xmap(AssayDelivery::new,d->d.pending);
    public static final SavedDataType<AssayDelivery> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("quantumindustries","assay_delivery"),()->new AssayDelivery(Map.of()),CODEC);
    private AssayDelivery(Map<String,List<String>> data){pending=new HashMap<>();data.forEach((k,v)->pending.put(k,new ArrayList<>(v)));}
    public static AssayDelivery of(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public void enqueue(String owner,AssayCatalog.Material material,AssayCatalog.Method method){
        String key=material.id()+"/"+method.id();var list=pending.computeIfAbsent(owner,k->new ArrayList<>());
        if(!list.contains(key)){list.add(key);setDirty();}
    }
    public void deliver(Player player){
        var list=pending.remove(player.getUUID().toString());if(list==null)return;
        var profile=AssayProfile.of(player);
        for(String key:list){String[] parts=key.split("/");if(parts.length!=2)continue;
            for(var material:AssayCatalog.Material.values())if(material.id().equals(parts[0])){
                var method=AssayCatalog.method(parts[1]);if(method!=null)profile.complete(material,method);
            }
        }
        setDirty();
    }
}
