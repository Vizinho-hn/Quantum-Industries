package br.com.quantumindustries.assay;

import br.com.quantumindustries.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Instrumental reference profiles. Relative gameplay indices, not physical measurements. */
public final class InstrumentalAnalysis {
    public static final String KEY = "quantum_instrumental_v1";
    private InstrumentalAnalysis() {}

    public record Profile(String id, int conductivity, int hardness, int thermalStability) {}
    private static final Profile[] PROFILES = {
        new Profile("copper", 100, 25, 40), new Profile("iron", 17, 60, 60),
        new Profile("gold", 75, 15, 45), new Profile("tin", 15, 12, 25),
        new Profile("zinc", 28, 25, 30), new Profile("lead", 8, 8, 20),
        new Profile("nickel", 24, 55, 65), new Profile("silver", 100, 20, 40),
        new Profile("bronze", 15, 65, 65), new Profile("brass", 28, 45, 50),
        new Profile("steel", 10, 85, 70), new Profile("invar", 3, 55, 100),
        new Profile("electrum", 55, 25, 45), new Profile("constantan", 4, 50, 85)
    };

    public static Profile profile(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(Items.COPPER_INGOT)) return PROFILES[0];
        if (stack.is(Items.IRON_INGOT)) return PROFILES[1];
        if (stack.is(Items.GOLD_INGOT)) return PROFILES[2];
        for (Profile p : PROFILES) {
            var metal = ModItems.INGOTS.get(p.id());
            var alloy = ModItems.ALLOY_INGOTS.get(p.id());
            if ((metal != null && stack.is(metal.get())) || (alloy != null && stack.is(alloy.get()))) return p;
        }
        return null;
    }

    public static java.util.List<Profile> profiles() { return java.util.List.of(PROFILES); }

    public static boolean known(net.minecraft.world.entity.player.Player player, String id) {
        return player.getPersistentData().getCompound(KEY)
                .map(tag -> tag.getBoolean(id).orElse(false)).orElse(false);
    }

    public static boolean known(net.minecraft.world.entity.player.Player player, Profile profile) {
        return player.getPersistentData().getCompound(KEY)
                .map(tag -> tag.getBoolean(profile.id()).orElse(false)).orElse(false);
    }

    /** Server-side record, called only after the interaction validates sample and payment. */
    public static boolean record(ServerPlayer player, Profile profile) {
        if (known(player, profile)) return false;
        CompoundTag records = player.getPersistentData().getCompound(KEY).orElseGet(CompoundTag::new);
        records.putBoolean(profile.id(), true);
        player.getPersistentData().put(KEY, records);
        return true;
    }

    /** First characterization requires paper in the off hand; later consultation is free. */
    public static void analyze(ServerPlayer player, ItemStack sample, Profile profile) {
        if (!known(player, profile)) {
            ItemStack paper = player.getOffhandItem();
            if (!paper.is(Items.PAPER)) {
                player.sendOverlayMessage(Component.translatable("message.quantumindustries.instrumental.paper"));
                return;
            }
            if (!player.getAbilities().instabuild) paper.shrink(1);
            record(player, profile);
        }
        player.sendOverlayMessage(Component.translatable("message.quantumindustries.instrumental.result",
                sample.getHoverName(), profile.conductivity(), profile.hardness(), profile.thermalStability()));
    }
}
