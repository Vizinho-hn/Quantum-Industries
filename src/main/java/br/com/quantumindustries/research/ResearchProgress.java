package br.com.quantumindustries.research;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ResearchProgress {
    private final Set<Identifier> completed = new HashSet<>();
    private final Set<String> unrecognized = new HashSet<>();

    static ResearchProgress decode(Collection<String> stored) {
        ResearchProgress progress = new ResearchProgress();
        for (String value : stored) {
            Identifier id = ResearchIds.parse(value);
            if (id == null) progress.unrecognized.add(value);
            else progress.completed.add(id);
        }
        return progress;
    }

    boolean knows(Identifier id) {
        return id != null && completed.contains(id);
    }

    boolean learn(Identifier id) {
        return id != null && completed.add(id);
    }

    boolean addLegacy(String key) {
        return learn(ResearchIds.of(key));
    }

    List<String> encode() {
        List<String> encoded = new ArrayList<>(completed.size() + unrecognized.size());
        completed.forEach(id -> encoded.add(id.toString()));
        encoded.addAll(unrecognized);
        encoded.sort(String::compareTo);
        return List.copyOf(encoded);
    }
}
