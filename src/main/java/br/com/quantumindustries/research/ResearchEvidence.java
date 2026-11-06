package br.com.quantumindustries.research;

import br.com.quantumindustries.assay.AssayCatalog;
import br.com.quantumindustries.assay.AssayProfile;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Scientific requirements are read from the player's discoveries, never consumed. */
public interface ResearchEvidence {
    boolean satisfied(Player player);
    Component description();

    record Assay(AssayCatalog.Material material, AssayCatalog.Method method) implements ResearchEvidence {
        public Assay {
            Objects.requireNonNull(material);
            Objects.requireNonNull(method);
        }

        @Override public boolean satisfied(Player player) {
            return AssayProfile.of(player).knows(material, method);
        }

        @Override public Component description() {
            return Component.translatable("gui.quantumindustries.research.evidence.assay",
                    Component.translatable(method.translationKey()), Component.translatable(material.translationKey()));
        }
    }

    record AnyOf(List<ResearchEvidence> alternatives) implements ResearchEvidence {
        public AnyOf {
            alternatives = List.copyOf(alternatives);
            if (alternatives.isEmpty()) throw new IllegalArgumentException("Evidence alternatives cannot be empty");
        }

        @Override public boolean satisfied(Player player) {
            return alternatives.stream().anyMatch(evidence -> evidence.satisfied(player));
        }

        @Override public Component description() {
            var result = Component.empty();
            for (int i = 0; i < alternatives.size(); i++) {
                if (i > 0) result.append(Component.translatable("gui.quantumindustries.research.evidence.or"));
                result.append(alternatives.get(i).description());
            }
            return result;
        }
    }
}
