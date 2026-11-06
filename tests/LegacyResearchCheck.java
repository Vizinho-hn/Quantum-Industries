package br.com.quantumindustries.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class LegacyResearchCheck {
    public static void main(String[] args) {
        String[] expected = {"conductivity", "copper_wire", "material_observation",
                "material_malleability", "manual_forming", "metal_sheets",
                "electrical_insulation", "basic_mechanics", "mechanical_transmission",
                "basic_magnetism", "copper_winding", "electromechanical_structure",
                "manual_dynamo_design", "electromagnetic_induction"};
        for (int i = 0; i < expected.length; i++) {
            if (!LegacyResearch.decode(1 << i).equals(Set.of(expected[i]))
                    || !LegacyResearch.key(i).equals(expected[i])) throw new AssertionError(expected[i]);
        }
        if (!LegacyResearch.decode(0).isEmpty() || !LegacyResearch.decode(1 << 31).isEmpty()
                || !LegacyResearch.key(-1).isEmpty()
                || !LegacyResearch.decode(3).equals(Set.of("conductivity", "copper_wire"))) {
            throw new AssertionError("Invalid legacy decode");
        }
        verifyStableIdentifiers();
        verifyLegacySaveMigration();
        verifyModernProgressPrecedence();
        verifyProgressBeyondBitMask();
    }

    private static void verifyStableIdentifiers() {
        for (String key : List.of("conductivity", "copper_wire")) {
            Identifier expected = ResearchIds.of(key);
            if (!ResearchIds.parse(key).equals(expected)
                    || !ResearchIds.parse(expected.toString()).equals(expected)) {
                throw new AssertionError("Research identifier did not round-trip: " + key);
            }
        }

        ResearchProgress progress = ResearchProgress.decode(
                List.of("conductivity", "quantumindustries:copper_wire", "future:unreleased", "INVALID ID"));
        if (!progress.knows(ResearchIds.of("conductivity"))
                || !progress.knows(ResearchIds.of("copper_wire"))
                || !progress.encode().contains("future:unreleased")
                || !progress.encode().contains("INVALID ID")) {
            throw new AssertionError("Old aliases or unrecognized research IDs were not preserved");
        }
    }

    private static void verifyLegacySaveMigration() {
        String uuid = "00000000-0000-0000-0000-000000000001";
        JsonObject legacy = new JsonObject();
        legacy.addProperty("conductivity", true);
        legacy.addProperty("copper_wire", true);
        JsonObject players = new JsonObject();
        players.addProperty(uuid, 5);
        players.addProperty("00000000-0000-0000-0000-000000000002", 1 << 31);
        legacy.add("players", players);

        WorldResearchData migrated = decode(legacy);
        if (!migrated.knows(uuid, ResearchIds.of("conductivity"))
                || !migrated.knows(uuid, ResearchIds.of("material_observation"))
                || migrated.knows(uuid, ResearchIds.of("copper_wire"))) {
            throw new AssertionError("Legacy player bits mapped to the wrong research IDs");
        }
        if (migrated.knows("00000000-0000-0000-0000-000000000002",
                ResearchIds.of("conductivity"))) {
            throw new AssertionError("Unknown legacy bits granted research");
        }

        JsonObject saved = encode(migrated);
        if (!saved.get("conductivity").getAsBoolean() || !saved.get("copper_wire").getAsBoolean()
                || saved.getAsJsonObject("players").get(uuid).getAsInt() != 5) {
            throw new AssertionError("Legacy fields were not retained during migration");
        }
        WorldResearchData reopened = decode(saved);
        if (!reopened.knows(uuid, ResearchIds.of("material_observation"))) {
            throw new AssertionError("Migrated progress was lost after save/reload");
        }
    }

    /** A deliberately empty modern entry represents cleared research, not an incomplete migration. */
    private static void verifyModernProgressPrecedence() {
        String uuid = "00000000-0000-0000-0000-000000000004";
        JsonObject saved = new JsonObject();
        JsonObject players = new JsonObject();
        players.addProperty(uuid, 3);
        saved.add("players", players);
        JsonObject modern = new JsonObject();
        modern.add(uuid, new JsonArray());
        saved.add("completed_research", modern);

        WorldResearchData cleared = decode(saved);
        if (cleared.knows(uuid, ResearchIds.of("conductivity"))
                || cleared.knows(uuid, ResearchIds.of("copper_wire"))) {
            throw new AssertionError("Old research resurrected after clearing modern progress");
        }
        WorldResearchData reloaded = decode(encode(cleared));
        if (reloaded.knows(uuid, ResearchIds.of("conductivity"))
                || reloaded.knows(uuid, ResearchIds.of("copper_wire"))) {
            throw new AssertionError("Cleared modern research resurrected after save/reload");
        }

        JsonArray onlyModern = new JsonArray();
        onlyModern.add("quantumindustries:material_observation");
        modern.add(uuid, onlyModern);
        WorldResearchData updated = decode(saved);
        if (!updated.knows(uuid, ResearchIds.of("material_observation"))
                || updated.knows(uuid, ResearchIds.of("conductivity"))
                || updated.knows(uuid, ResearchIds.of("copper_wire"))) {
            throw new AssertionError("Legacy bits overrode an existing modern research list");
        }
    }

    private static void verifyProgressBeyondBitMask() {
        String uuid = "00000000-0000-0000-0000-000000000003";
        JsonArray identifiers = new JsonArray();
        List<Identifier> expected = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Identifier id = ResearchIds.of("future_research_" + i);
            expected.add(id);
            identifiers.add(id.toString());
        }
        JsonObject progress = new JsonObject();
        progress.add(uuid, identifiers);
        JsonObject save = new JsonObject();
        save.add("completed_research", progress);

        WorldResearchData decoded = decode(save);
        for (Identifier id : expected) {
            if (!decoded.knows(uuid, id)) throw new AssertionError("Research beyond bit 31 was lost: " + id);
        }
        WorldResearchData reloaded = decode(encode(decoded));
        for (Identifier id : expected) {
            if (!reloaded.knows(uuid, id)) throw new AssertionError("Research save round-trip failed: " + id);
        }
    }

    private static WorldResearchData decode(JsonObject input) {
        return WorldResearchData.CODEC.parse(JsonOps.INSTANCE, input).result().orElseThrow();
    }

    private static JsonObject encode(WorldResearchData data) {
        return WorldResearchData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow().getAsJsonObject();
    }
}
