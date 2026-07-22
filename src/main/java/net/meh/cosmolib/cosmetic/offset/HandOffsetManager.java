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
 * Singleton cache and persistence layer for per-cosmetic hand offsets,
 * stored separately for the left arm and right arm.
 *
 * <p>JSON format (one entry per cosmetic):
 * <pre>{@code
 * {
 *   "cosmolib:cosmo_cane": {
 *     "left":  { "x": -0.3, "y": 0.625, "z": -0.12, "rotX": -90, "rotY": 0, "rotZ": -180 },
 *     "right": { "x":  0.3, "y": 0.625, "z": -0.12, "rotX": -90, "rotY": 0, "rotZ": -180 }
 *   }
 * }
 * }</pre>
 *
 * <p>Cosmetics absent from the file fall back to {@link HandOffsetData#defaults()}.
 * Old flat-format entries (pre-split) are loaded as the left arm offset.
 */
public final class HandOffsetManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_SUBPATH = "cosmolib/hand_offsets.json";

    /** Programmatically registered defaults — never cleared by {@link #load()}. */
    private static final Map<String, HandOffsetData> BUILTIN_LEFT  = new HashMap<>();
    private static final Map<String, HandOffsetData> BUILTIN_RIGHT = new HashMap<>();

    /** JSON-loaded overrides — cleared and rebuilt on each {@link #load()} call. */
    private static final Map<String, HandOffsetData> LEFT_CACHE  = new HashMap<>();
    private static final Map<String, HandOffsetData> RIGHT_CACHE = new HashMap<>();

    private HandOffsetManager() {}

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public static void load() {
        LEFT_CACHE.clear();
        RIGHT_CACHE.clear();
        Path file = configFile();

        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    w.write("{}");
                }
                CosmoLib.LOGGER.info("[CosmoLib] Created empty hand_offsets.json at {}", file);
            } catch (IOException e) {
                CosmoLib.LOGGER.error("[CosmoLib] Failed to create hand_offsets.json: {}", e.getMessage(), e);
            }
            return;
        }

        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (var entry : root.entrySet()) {
                String key = entry.getKey();
                JsonObject obj = entry.getValue().getAsJsonObject();

                if (obj.has("left") || obj.has("right")) {
                    // New nested format
                    if (obj.has("left"))  LEFT_CACHE.put(key,  parseOffset(obj.getAsJsonObject("left")));
                    if (obj.has("right")) RIGHT_CACHE.put(key, parseOffset(obj.getAsJsonObject("right")));
                } else {
                    // Legacy flat format — treat as left arm
                    LEFT_CACHE.put(key, parseOffset(obj));
                }
            }
            CosmoLib.LOGGER.info("[CosmoLib] Loaded hand offsets ({} left, {} right) from {}",
                    LEFT_CACHE.size(), RIGHT_CACHE.size(), file);
        } catch (IOException | IllegalStateException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to read hand_offsets.json: {}", e.getMessage(), e);
        }
    }

    public static void save() {
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            // Collect all unique cosmetic IDs
            java.util.TreeSet<String> ids = new java.util.TreeSet<>();
            ids.addAll(LEFT_CACHE.keySet());
            ids.addAll(RIGHT_CACHE.keySet());

            JsonObject root = new JsonObject();
            for (String id : ids) {
                JsonObject cosmeticObj = new JsonObject();
                if (LEFT_CACHE.containsKey(id))  cosmeticObj.add("left",  writeOffset(LEFT_CACHE.get(id)));
                if (RIGHT_CACHE.containsKey(id)) cosmeticObj.add("right", writeOffset(RIGHT_CACHE.get(id)));
                root.add(id, cosmeticObj);
            }
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            CosmoLib.LOGGER.info("[CosmoLib] Saved hand_offsets.json ({} left, {} right entries)",
                    LEFT_CACHE.size(), RIGHT_CACHE.size());
        } catch (IOException e) {
            CosmoLib.LOGGER.error("[CosmoLib] Failed to write hand_offsets.json: {}", e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Programmatic registration (call before load() in mod setup)
    // ------------------------------------------------------------------

    /**
     * Registers default hand offsets for a cosmetic in code.
     *
     * <p>Call this during your mod's common setup (before {@link #load()}) so the
     * renderer has values even when no JSON config file is present.
     * Any matching entry in {@code hand_offsets.json} will override these defaults.
     *
     * @param id    the cosmetic item's resource location (e.g. {@code mymod:my_staff})
     * @param left  left-arm offset data
     * @param right right-arm offset data
     */
    public static void register(ResourceLocation id, HandOffsetData left, HandOffsetData right) {
        BUILTIN_LEFT.put(id.toString(), left);
        BUILTIN_RIGHT.put(id.toString(), right);
    }

    // ------------------------------------------------------------------
    // Cache access — per side
    // ------------------------------------------------------------------

    public static HandOffsetData getLeft(ResourceLocation id) {
        String key = id.toString();
        HandOffsetData d = LEFT_CACHE.get(key);
        if (d != null) return d;
        d = BUILTIN_LEFT.get(key);
        return d != null ? d : HandOffsetData.defaults();
    }

    public static HandOffsetData getRight(ResourceLocation id) {
        String key = id.toString();
        HandOffsetData d = RIGHT_CACHE.get(key);
        if (d != null) return d;
        d = BUILTIN_RIGHT.get(key);
        return d != null ? d : HandOffsetData.defaults();
    }

    public static void setLeft(ResourceLocation id, double x, double y, double z,
                               double rotX, double rotY, double rotZ) {
        LEFT_CACHE.put(id.toString(), new HandOffsetData(x, y, z, rotX, rotY, rotZ));
    }

    public static void setRight(ResourceLocation id, double x, double y, double z,
                                double rotX, double rotY, double rotZ) {
        RIGHT_CACHE.put(id.toString(), new HandOffsetData(x, y, z, rotX, rotY, rotZ));
    }

    public static void cancelLeft(ResourceLocation id, HandOffsetData original) {
        LEFT_CACHE.put(id.toString(), original);
    }

    public static void cancelRight(ResourceLocation id, HandOffsetData original) {
        RIGHT_CACHE.put(id.toString(), original);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static HandOffsetData parseOffset(JsonObject o) {
        return new HandOffsetData(
                o.has("x")    ? o.get("x").getAsDouble()    : HandOffsetData.DEFAULT_X,
                o.has("y")    ? o.get("y").getAsDouble()    : HandOffsetData.DEFAULT_Y,
                o.has("z")    ? o.get("z").getAsDouble()    : HandOffsetData.DEFAULT_Z,
                o.has("rotX") ? o.get("rotX").getAsDouble() : HandOffsetData.DEFAULT_ROTX,
                o.has("rotY") ? o.get("rotY").getAsDouble() : HandOffsetData.DEFAULT_ROTY,
                o.has("rotZ") ? o.get("rotZ").getAsDouble() : HandOffsetData.DEFAULT_ROTZ);
    }

    private static JsonObject writeOffset(HandOffsetData d) {
        JsonObject o = new JsonObject();
        o.addProperty("x",    d.x);
        o.addProperty("y",    d.y);
        o.addProperty("z",    d.z);
        o.addProperty("rotX", d.rotX);
        o.addProperty("rotY", d.rotY);
        o.addProperty("rotZ", d.rotZ);
        return o;
    }

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_SUBPATH);
    }
}
