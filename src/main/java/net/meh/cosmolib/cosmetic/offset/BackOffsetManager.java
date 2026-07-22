package net.meh.cosmolib.cosmetic.offset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.meh.cosmolib.CosmoLib;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton cache and persistence layer for per-cosmetic back Y-offset values.
 *
 * <p>All offsets are stored in a single flat JSON file:
 * <pre>config/cosmolib/back_offsets.json</pre>
 *
 * <p>The file maps cosmetic registry IDs (e.g. {@code "cosmolib:cosmo_robe"}) to
 * double Y-offset values.  Cosmetics absent from the file fall back to
 * {@link BackOffsetData#DEFAULT_Y}, preserving pre-system render behaviour.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@link #load()} — called once during {@code FMLCommonSetupEvent} in
 *       {@link net.meh.cosmolib.CosmoLib}.</li>
 *   <li>{@link #getY(ResourceLocation)} — called every frame by
 *       {@link net.meh.cosmolib.cosmetic.client.CosmeticPlayerLayer}.</li>
 *   <li>{@link #setY(ResourceLocation, double)} — called on each scroll tick by the
 *       {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} to push the live
 *       value into the cache so renders update immediately.</li>
 *   <li>{@link #save()} — called on Ctrl+Click confirm to flush the cache to disk.</li>
 *   <li>{@link #cancelChanges(ResourceLocation, double)} — called on Shift+Click
 *       cancel to revert the in-memory value without touching disk.</li>
 * </ol>
 */
public final class BackOffsetManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Path within {@code FMLPaths.CONFIGDIR}: {@code cosmolib/back_offsets.json}. */
    private static final String CONFIG_SUBPATH = "cosmolib/back_offsets.json";

    /**
     * Code-registered defaults: populated by {@link #registerDefault} calls during
     * mod setup, before {@link #load()} runs.  Never cleared — these come from the
     * mod's own source code and act as the fallback when no config entry exists.
     */
    private static final Map<String, Double> DEFAULTS = new HashMap<>();

    /**
     * Live config-file cache: populated by {@link #load()}.  Overrides DEFAULTS.
     * Cleared and reloaded each call to {@link #load()}.
     */
    private static final Map<String, Double> CACHE = new HashMap<>();

    private BackOffsetManager() {}

    // ------------------------------------------------------------------
    // Code-registered defaults (called by mod authors in their mod setup)
    // ------------------------------------------------------------------

    /**
     * Registers a hardcoded back Y-offset for a cosmetic item.
     *
     * <p>Call this in your mod's {@code FMLCommonSetupEvent} (or constructor) for every
     * BACK cosmetic you publish, using the value you dialled in with the BackswagTuner:
     *
     * <pre>{@code
     * BackOffsetManager.registerDefault(
     *     ResourceLocation.fromNamespaceAndPath("mymod", "my_backpack"), 0.35);
     * }</pre>
     *
     * <p>The config file ({@code config/cosmolib/back_offsets.json}) can still override
     * this value — it takes priority.  But if no config entry exists, this default is
     * used, so players never need to ship or maintain the config file themselves.
     *
     * @param cosmeticId the full registry ID of the BACK cosmetic item
     * @param y          the Y offset in block units (same value the tuner saved)
     */
    public static void registerDefault(ResourceLocation cosmeticId, double y) {
        DEFAULTS.put(cosmeticId.toString(), y);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * Loads (or creates) the {@code back_offsets.json} config file and populates
     * the in-memory cache.  Safe to call multiple times — clears and repopulates
     * the cache each invocation.
     *
     * <p>If the file does not yet exist, an empty JSON object ({@code {}}) is written
     * so the file is present for future manual editing.
     */
    public static void load() {
        CACHE.clear();
        Path file = configFile();

        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    w.write("{}");
                }
                CosmoLib.LOGGER.info("[CosmoLib] Created empty back_offsets.json at {}", file);
            } catch (IOException e) {
                CosmoLib.LOGGER.error("[CosmoLib] Failed to create back_offsets.json: {}",
                        e.getMessage(), e);
            }
            return; // empty file → empty cache → all cosmetics use DEFAULT_Y
        }

        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : obj.entrySet()) {
                CACHE.put(entry.getKey(), entry.getValue().getAsDouble());
            }
            CosmoLib.LOGGER.info("[CosmoLib] Loaded {} back offset(s) from {}",
                    CACHE.size(), file);
        } catch (IOException | IllegalStateException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to read back_offsets.json: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * Writes the current in-memory cache to {@code back_offsets.json}, pretty-printed
     * and sorted by key for deterministic output.
     *
     * <p>Called explicitly by the
     * {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} on Ctrl+Click confirm —
     * never written automatically.
     */
    public static void save() {
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            CACHE.entrySet().stream()
                 .sorted(Map.Entry.comparingByKey())
                 .forEach(e -> obj.addProperty(e.getKey(), e.getValue()));
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
            CosmoLib.LOGGER.info("[CosmoLib] Saved back_offsets.json ({} entries)", CACHE.size());
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to write back_offsets.json: {}",
                    e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Cache access
    // ------------------------------------------------------------------

    /**
     * Returns the Y offset for the given cosmetic, or {@link BackOffsetData#DEFAULT_Y}
     * if no entry exists in the cache.
     *
     * <p>Called every render frame — allocates nothing.
     *
     * @param cosmeticId full registry ID of the cosmetic item (e.g. {@code "cosmolib:cosmo_robe"})
     * @return Y translation value in block units
     */
    /**
     * Returns the Y offset for the given cosmetic.
     *
     * <p>Priority (highest → lowest):
     * <ol>
     *   <li>Config file ({@code back_offsets.json}) — server/player override</li>
     *   <li>Code-registered default ({@link #registerDefault}) — set by the cosmetic's mod author</li>
     *   <li>{@link BackOffsetData#DEFAULT_Y} — global fallback</li>
     * </ol>
     *
     * @param cosmeticId full registry ID of the cosmetic item
     * @return Y translation value in block units
     */
    public static double getY(ResourceLocation cosmeticId) {
        String key = cosmeticId.toString();
        if (CACHE.containsKey(key))    return CACHE.get(key);
        if (DEFAULTS.containsKey(key)) return DEFAULTS.get(key);
        return BackOffsetData.DEFAULT_Y;
    }

    /**
     * Updates the Y offset for the given cosmetic in memory only.
     * Does <em>not</em> write to disk — call {@link #save()} explicitly to persist.
     *
     * <p>Called on every scroll tick by the
     * {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} so the render updates live.
     *
     * @param cosmeticId full registry ID of the cosmetic item
     * @param y          new Y offset value
     */
    public static void setY(ResourceLocation cosmeticId, double y) {
        CACHE.put(cosmeticId.toString(), y);
    }

    /**
     * Restores the in-memory Y offset for the given cosmetic to the provided original
     * value, discarding any live scroll adjustments.  Does <em>not</em> write to disk.
     *
     * <p>Called by the
     * {@link net.meh.cosmolib.cosmetic.tool.BackswagTunerItem} on Shift+Click cancel.
     *
     * @param cosmeticId full registry ID of the cosmetic item
     * @param originalY  the Y offset to restore (snapshot taken at session start)
     */
    public static void cancelChanges(ResourceLocation cosmeticId, double originalY) {
        CACHE.put(cosmeticId.toString(), originalY);
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_SUBPATH);
    }
}
