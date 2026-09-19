package com.oresight.config;

import java.util.LinkedHashMap;

/**
 * Everything about OreSight's behaviour that gets saved to disk.
 * Plain data holder -- Gson reads/writes this directly, so avoid renaming
 * fields casually (it would silently reset users' saved settings).
 */
public class ModConfig {

    // ---- General ----
    public boolean enabled = true;

    /** "THROUGH_TERRAIN" or "VISIBLE_ONLY" */
    public String mode = "THROUGH_TERRAIN";

    public int rangeBlocks = 48;
    public boolean distanceFade = true;
    public boolean hudEnabled = true;
    public boolean showBlockNameOnLook = false;

    // ---- Appearance ----
    /** "OUTLINE", "SOLID", "GLOW", or "OUTLINE_GLOW" */
    public String highlightStyle = "OUTLINE_GLOW";

    public float brightness = 1.0f;
    public float fillOpacity = 0.22f;
    public float outlineOpacity = 1.0f;
    public float outlineThickness = 2.0f;
    public float glowIntensity = 0.55f;

    // ---- Animation ----
    public boolean pulseEnabled = true;
    public float pulseSpeed = 1.0f;

    // ---- Blocks ----
    /** Keyed by block id (e.g. "minecraft:diamond_ore"). */
    public LinkedHashMap<String, BlockEntry> blocks = new LinkedHashMap<>();

    // ---- Profiles ----
    public String activeProfile = "Default";
}
