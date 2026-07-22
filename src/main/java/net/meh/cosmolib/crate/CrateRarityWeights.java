package net.meh.cosmolib.crate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.meh.cosmolib.CosmoLib;
import net.meh.cosmolib.cosmetic.CosmeticRarity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.util.RandomSource;

/**
 * Global percentage-based rarity weight table used by all crates when rolling items.
 *
 * <p>Weights are loaded from {@code config/cosmolib/crate_rarity_weights.json} at startup.
 * If the file is absent it is created with the default values. All five weights must sum
 * to 100.0 — an {@link IllegalStateException} is thrown on load if they do not.
 *
 * <p>Call {@link #load()} during {@code FMLCommonSetupEvent.enqueueWork()}.
 */
public final class CrateRarityWeights {

    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH =
            Paths.get("config", "cosmolib", "crate_rarity_weights.json");

    // Cached weights — set by load()
    private static float common    = 60.0f;
    private static float rare      = 25.0f;
    private static float epic      = 10.0f;
    private static float legendary =  4.0f;
    private static float limited   =  1.0f;

    private CrateRarityWeights() {}

    /**
     * Reads {@code config/cosmolib/crate_rarity_weights.json}, populating the in-memory
     * weight cache. Creates the file with default values if absent.
     *
     * @throws IllegalStateException if the loaded weights do not sum to 100.0 (within 0.01)
     */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            writeDefault();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            common    = json.get("common").getAsFloat();
            rare      = json.get("rare").getAsFloat();
            epic      = json.get("epic").getAsFloat();
            legendary = json.get("legendary").getAsFloat();
            limited   = json.get("limited").getAsFloat();
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to read crate_rarity_weights.json; using defaults", e);
            resetDefaults();
            return;
        }
        float sum = common + rare + epic + legendary + limited;
        if (Math.abs(sum - 100.0f) > 0.01f) {
            throw new IllegalStateException(
                    "[CosmoLib] crate_rarity_weights.json weights must sum to 100.0 but got " + sum
                    + " (common=" + common + ", rare=" + rare + ", epic=" + epic
                    + ", legendary=" + legendary + ", limited=" + limited + ")");
        }
        CosmoLib.LOGGER.info("[CosmoLib] Loaded crate rarity weights from {}", CONFIG_PATH);
    }

    /**
     * Rolls a rarity using weighted random selection.
     *
     * <p>{@code luckBonus} is added to the RARE weight and subtracted from COMMON,
     * clamped so COMMON never drops below 5.0.
     *
     * @param random    the RNG to use
     * @param luckBonus bonus to add to RARE (taken from COMMON); 0 = standard weights
     * @return the rolled {@link CosmeticRarity}
     */
    public static CosmeticRarity roll(RandomSource random, float luckBonus) {
        float adjCommon = Math.max(5.0f, common - luckBonus);
        float moved     = common - adjCommon;          // actual amount shifted
        float adjRare   = rare + moved;
        return pick(random, adjCommon, adjRare, epic, legendary, limited);
    }

    /**
     * Rolls a rarity from RARE or higher only, redistributing COMMON across the
     * remaining tiers. Used for the guaranteed first slot.
     *
     * @param random    the RNG to use
     * @param luckBonus bonus applied before COMMON is excluded
     * @return a {@link CosmeticRarity} that is RARE or higher
     */
    public static CosmeticRarity rollRareOrHigher(RandomSource random, float luckBonus) {
        float adjCommon = Math.max(5.0f, common - luckBonus);
        float moved     = common - adjCommon;
        float adjRare   = rare + moved;

        // Exclude COMMON entirely — scale remaining weights to 100 %
        float total = adjRare + epic + legendary + limited;
        if (total <= 0) return CosmeticRarity.RARE;

        float r = random.nextFloat() * total;
        if (r < adjRare)                        return CosmeticRarity.RARE;
        if (r < adjRare + epic)                 return CosmeticRarity.EPIC;
        if (r < adjRare + epic + legendary)     return CosmeticRarity.LEGENDARY;
        return CosmeticRarity.LIMITED;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private static CosmeticRarity pick(RandomSource random,
            float wCommon, float wRare, float wEpic, float wLegendary, float wLimited) {
        float total = wCommon + wRare + wEpic + wLegendary + wLimited;
        float r = random.nextFloat() * total;
        if (r < wCommon)                                  return CosmeticRarity.COMMON;
        if (r < wCommon + wRare)                          return CosmeticRarity.RARE;
        if (r < wCommon + wRare + wEpic)                  return CosmeticRarity.EPIC;
        if (r < wCommon + wRare + wEpic + wLegendary)     return CosmeticRarity.LEGENDARY;
        return CosmeticRarity.LIMITED;
    }

    private static void resetDefaults() {
        common    = 60.0f;
        rare      = 25.0f;
        epic      = 10.0f;
        legendary =  4.0f;
        limited   =  1.0f;
    }

    private static void writeDefault() {
        resetDefaults();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Could not create config/cosmolib/ directory", e);
        }
        JsonObject json = new JsonObject();
        json.addProperty("common",    common);
        json.addProperty("rare",      rare);
        json.addProperty("epic",      epic);
        json.addProperty("legendary", legendary);
        json.addProperty("limited",   limited);
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(json, writer);
            CosmoLib.LOGGER.info("[CosmoLib] Created default crate_rarity_weights.json at {}", CONFIG_PATH);
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Could not write default crate_rarity_weights.json", e);
        }
    }
}
