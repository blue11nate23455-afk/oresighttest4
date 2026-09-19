package com.oresight.config;

/**
 * A single block's highlight configuration: which block it is, whether
 * highlighting is currently on for it, and what color to draw it in.
 * Instances are (de)serialized directly by Gson, so keep field names stable.
 */
public class BlockEntry {

    /** Full block id, e.g. "minecraft:diamond_ore" or "somemod:weird_ore". */
    public String blockId;

    public boolean enabled;

    /** Packed 0xRRGGBB color, no alpha (alpha comes from the appearance sliders). */
    public int color;

    public BlockEntry() {
        // required by Gson
    }

    public BlockEntry(String blockId, boolean enabled, int color) {
        this.blockId = blockId;
        this.enabled = enabled;
        this.color = color;
    }
}
