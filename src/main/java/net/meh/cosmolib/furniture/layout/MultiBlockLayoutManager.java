package net.meh.cosmolib.furniture.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.meh.cosmolib.CosmoLib;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Singleton cache for all {@link MultiBlockLayout} definitions.
 *
 * <p>Layouts are stored as JSON files in:
 * <pre>config/cosmolib/furniture_layouts/&lt;furnitureId&gt;.json</pre>
 *
 * <h3>Cross-mod usage</h3>
 * Any mod can contribute layouts by dropping JSON files into the folder above
 * (or via the in-game {@link net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem}).
 * The layout is keyed by the block's registry <em>path</em> (e.g. {@code "ancient_arbor"}),
 * so keep block names unique across mods or use the explicit-diagonal variant of
 * {@link MultiBlockLayout#of} to avoid collisions.
 *
 * <h3>Loading</h3>
 * Call {@link #load()} once during {@code FMLCommonSetupEvent} (handled automatically
 * by {@link CosmoLib}).  Call {@link #reload()} at any time to pick up newly saved
 * layouts without restarting the game — the
 * {@link net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem} does this after saving.
 */
public final class MultiBlockLayoutManager {

    /** Relative path within the game's config directory. */
    private static final String LAYOUTS_SUBDIR = "cosmolib/furniture_layouts";

    private static final Map<String, MultiBlockLayout> CACHE = new HashMap<>();

    private MultiBlockLayoutManager() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Returns the layout for the given furniture id, or {@link Optional#empty()} if none exists.
     *
     * @param furnitureId registry path of the block (e.g. {@code "ancient_arbor"})
     */
    public static Optional<MultiBlockLayout> get(String furnitureId) {
        return Optional.ofNullable(CACHE.get(furnitureId));
    }

    /** Read-only snapshot of all cached layouts, keyed by furniture id. */
    public static Map<String, MultiBlockLayout> getAll() {
        return Collections.unmodifiableMap(CACHE);
    }

    /**
     * Loads (or reloads) all layout JSON files.
     * Safe to call multiple times — clears and repopulates the cache each time.
     *
     * <p>Load order (later entries override earlier ones):
     * <ol>
     *   <li>Built-in layouts shipped inside mod JARs at
     *       {@code assets/<modid>/furniture_layouts/*.json}</li>
     *   <li>Config-file layouts at
     *       {@code config/cosmolib/furniture_layouts/*.json} (player/server overrides)</li>
     * </ol>
     */
    public static void load() {
        CACHE.clear();

        // 1. Load built-in layouts from every loaded mod's JAR
        loadBuiltinLayouts();
        int builtinCount = CACHE.size();

        // 2. Load config-file layouts on top (overrides built-ins)
        Path dir = layoutsDir();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .filter(p -> !p.getFileName().toString().startsWith(".")) // skip temp files
                     .forEach(MultiBlockLayoutManager::loadFile);
            } catch (IOException e) {
                CosmoLib.LOGGER.error("[CosmoLib] Failed to list furniture_layouts directory: {}", e.getMessage(), e);
            }
        }

        CosmoLib.LOGGER.info("[CosmoLib] Loaded {} furniture layout(s) ({} built-in, {} from config).",
                CACHE.size(), builtinCount, CACHE.size() - builtinCount);
    }

    /**
     * Scans every loaded mod's JAR for built-in furniture layouts at
     * {@code assets/<modid>/furniture_layouts/*.json} and loads them into the cache.
     *
     * <p>Any mod can ship layouts this way — they are automatically discovered without
     * requiring players to copy files into the config directory.  Config-file entries
     * loaded afterwards will override these defaults.
     */
    private static void loadBuiltinLayouts() {
        ModList.get().getMods().forEach(mod -> {
            String modId = mod.getModId();
            try {
                Path dir = mod.getOwningFile().getFile()
                        .findResource("assets", modId, "furniture_layouts");
                if (!Files.isDirectory(dir)) return;
                try (Stream<Path> files = Files.list(dir)) {
                    files.filter(p -> p.toString().endsWith(".json"))
                         .filter(p -> !p.getFileName().toString().startsWith("."))
                         .forEach(MultiBlockLayoutManager::loadFile);
                } catch (IOException e) {
                    CosmoLib.LOGGER.error("[CosmoLib] Failed to list built-in furniture_layouts for '{}': {}",
                            modId, e.getMessage(), e);
                }
            } catch (Exception ignored) {
                // Most mods won't have this directory — skip silently.
            }
        });
    }

    /**
     * Alias for {@link #load()} — re-reads all files without restarting the game.
     * Called automatically by {@link net.meh.cosmolib.furniture.tool.BoundingBoxSelectorItem}
     * after saving a layout.
     */
    public static void reload() {
        load();
    }

    /**
     * Writes a layout to disk and updates the cache.
     * Creates the {@code furniture_layouts} directory if it does not exist.
     *
     * @param layout the layout to persist
     */
    public static void save(MultiBlockLayout layout) {
        Path dir = layoutsDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Could not create furniture_layouts directory: {}", e.getMessage(), e);
            return;
        }
        Path file = dir.resolve(layout.getFurnitureId() + ".json");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(layout.toJsonString());
            CACHE.put(layout.getFurnitureId(), layout);
            CosmoLib.LOGGER.info("[CosmoLib] Saved layout for '{}' → {}", layout.getFurnitureId(), file);
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to write layout for '{}': {}", layout.getFurnitureId(), e.getMessage(), e);
        }
    }

    /**
     * Writes a temporary crash-recovery file for an in-progress session.
     * The file is named {@code .temp_<suffix>.json} and is deleted on session end.
     *
     * @param suffix   unique suffix (e.g. player UUID string)
     * @param layout   partial layout to persist
     */
    public static void saveTemp(String suffix, MultiBlockLayout layout) {
        Path dir = layoutsDir();
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(".temp_" + suffix + ".json");
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(layout.toJsonString());
            }
        } catch (IOException e) {
            CosmoLib.LOGGER.warn("[CosmoLib] Could not write temp layout for {}: {}", suffix, e.getMessage());
        }
    }

    /**
     * Deletes the temporary crash-recovery file for a session.
     *
     * @param suffix the same suffix used in {@link #saveTemp}
     */
    public static void deleteTemp(String suffix) {
        Path file = layoutsDir().resolve(".temp_" + suffix + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            CosmoLib.LOGGER.warn("[CosmoLib] Could not delete temp file for {}: {}", suffix, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private static Path layoutsDir() {
        return FMLPaths.CONFIGDIR.get().resolve(LAYOUTS_SUBDIR);
    }

    private static void loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            MultiBlockLayout layout = MultiBlockLayout.fromJson(obj);
            CACHE.put(layout.getFurnitureId(), layout);
        } catch (Exception e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to load layout from {}: {}", file.getFileName(), e.getMessage(), e);
        }
    }
}
