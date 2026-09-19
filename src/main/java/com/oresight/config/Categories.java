package com.oresight.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of the blocks OreSight knows about out of the box, their
 * default colors, whether they start enabled, and which category/preset
 * button(s) they belong to.
 *
 * This only seeds the *default* config the first time the mod runs. After
 * that, the user's saved config.json is the source of truth -- including
 * any modded block ids they add themselves through the search box (the
 * Blocks screen lets you type any block id, vanilla or modded, and toggle
 * it on even if it isn't in this default list).
 */
public final class Categories {

    public static final String ALL_ORES = "All Ores";
    public static final String VALUABLE_ORES = "Valuable Ores";
    public static final String COMMON_ORES = "Common Ores";
    public static final String ANCIENT_DEBRIS = "Ancient Debris";
    public static final String STRUCTURES = "Structures / Special Blocks";
    public static final String CONTAINERS = "Containers";

    public static final Map<String, Integer> DEFAULT_COLORS = new LinkedHashMap<>();
    public static final Map<String, List<String>> PRESETS = new LinkedHashMap<>();
    private static final List<String> DEFAULT_ENABLED = new ArrayList<>();

    private Categories() {}

    private static void reg(String id, int color, boolean defaultOn, String... categories) {
        DEFAULT_COLORS.put(id, color);
        if (defaultOn) {
            DEFAULT_ENABLED.add(id);
        }
        for (String c : categories) {
            PRESETS.computeIfAbsent(c, k -> new ArrayList<>()).add(id);
        }
    }

    static {
        // Valuable ores
        reg("minecraft:diamond_ore", 0x00FFFF, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:deepslate_diamond_ore", 0x00FFFF, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:emerald_ore", 0x00FF66, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:deepslate_emerald_ore", 0x00FF66, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:gold_ore", 0xFFD700, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:deepslate_gold_ore", 0xFFD700, true, ALL_ORES, VALUABLE_ORES);
        reg("minecraft:nether_gold_ore", 0xFFD700, false, ALL_ORES, VALUABLE_ORES);

        // Common ores
        reg("minecraft:iron_ore", 0xFFFFFF, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:deepslate_iron_ore", 0xFFFFFF, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:copper_ore", 0xFF8C00, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:deepslate_copper_ore", 0xFF8C00, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:redstone_ore", 0xFF3333, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:deepslate_redstone_ore", 0xFF3333, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:lapis_ore", 0x2255FF, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:deepslate_lapis_ore", 0x2255FF, true, ALL_ORES, COMMON_ORES);
        reg("minecraft:coal_ore", 0x555555, false, ALL_ORES, COMMON_ORES);
        reg("minecraft:deepslate_coal_ore", 0x555555, false, ALL_ORES, COMMON_ORES);

        // Ancient debris
        reg("minecraft:ancient_debris", 0xAA33FF, true, ANCIENT_DEBRIS);

        // Structures / special blocks
        reg("minecraft:spawner", 0xFF33FF, false, STRUCTURES);
        reg("minecraft:end_portal_frame", 0xFFAA00, false, STRUCTURES);
        reg("minecraft:beacon", 0xFFFFAA, false, STRUCTURES);
        reg("minecraft:nether_portal", 0x9900FF, false, STRUCTURES);

        // Containers
        reg("minecraft:chest", 0xC08040, false, CONTAINERS);
        reg("minecraft:trapped_chest", 0xC08040, false, CONTAINERS);
        reg("minecraft:barrel", 0xC08040, false, CONTAINERS);
        reg("minecraft:ender_chest", 0x228888, false, CONTAINERS);
        reg("minecraft:shulker_box", 0x99CC66, false, CONTAINERS);
    }

    public static LinkedHashMap<String, BlockEntry> buildDefaultBlockMap() {
        LinkedHashMap<String, BlockEntry> map = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : DEFAULT_COLORS.entrySet()) {
            boolean on = DEFAULT_ENABLED.contains(e.getKey());
            map.put(e.getKey(), new BlockEntry(e.getKey(), on, e.getValue()));
        }
        return map;
    }
}
