package com.oresight.highlight;

import com.oresight.config.BlockEntry;
import com.oresight.config.ConfigManager;
import com.oresight.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ties config -> chunk scanning -> the list of blocks the renderer should
 * actually draw this frame (already filtered by range and mode).
 */
public class HighlightManager {

    /** Only scan/keep-alive this many chunks worth of area per tick. */
    private static final int CHUNKS_PER_TICK = 2;

    private final ConfigManager configManager;
    private final ChunkScanner scanner = new ChunkScanner();
    private final Set<ChunkPos> inRange = new HashSet<>();

    private boolean temporarilyHidden = false;

    public HighlightManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public ModConfig cfg() {
        return configManager.getConfig();
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public void setTemporarilyHidden(boolean hidden) {
        this.temporarilyHidden = hidden;
    }

    /** Call this whenever the enabled-block set changes so stale cache entries clear out. */
    public void invalidateAll() {
        scanner.clear();
        inRange.clear();
    }

    public Set<String> enabledBlockIds() {
        Set<String> set = new HashSet<>();
        for (BlockEntry e : cfg().blocks.values()) {
            if (e.enabled) {
                set.add(e.blockId);
            }
        }
        return set;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !cfg().enabled) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        int radiusChunks = Math.max(1, cfg().rangeBlocks / 16 + 1);
        ChunkPos center = new ChunkPos(playerPos);

        Set<ChunkPos> newInRange = new HashSet<>();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                newInRange.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }

        for (ChunkPos p : newInRange) {
            if (!inRange.contains(p)) {
                scanner.requestScan(p);
            }
        }
        for (ChunkPos p : inRange) {
            if (!newInRange.contains(p)) {
                scanner.evict(p);
            }
        }

        inRange.clear();
        inRange.addAll(newInRange);

        scanner.tick(client, enabledBlockIds(), CHUNKS_PER_TICK);
    }

    /** Blocks to actually render this frame, already range/mode filtered. */
    public List<ChunkScanner.Hit> collectHits(MinecraftClient client) {
        List<ChunkScanner.Hit> result = new ArrayList<>();
        if (temporarilyHidden || !cfg().enabled || client.world == null || client.player == null) {
            return result;
        }

        boolean visibleOnly = "VISIBLE_ONLY".equals(cfg().mode);
        BlockPos playerPos = client.player.getBlockPos();
        double rangeSq = (double) cfg().rangeBlocks * cfg().rangeBlocks;

        for (ChunkPos p : inRange) {
            for (ChunkScanner.Hit hit : scanner.getHits(p)) {
                double distSq = hit.pos.getSquaredDistance(playerPos.getX(), playerPos.getY(), playerPos.getZ());
                if (distSq > rangeSq) {
                    continue;
                }
                if (visibleOnly && !isExposed(client, hit.pos)) {
                    continue;
                }
                result.add(hit);
            }
        }
        return result;
    }

    /**
     * "Exposed" mode: true if at least one neighboring block is non-opaque
     * (air, glass, water, etc). This is the same heuristic vanilla uses for
     * "this block could be seen if you were standing right next to it" --
     * it's not a full line-of-sight raycast from the player's eyes, just a
     * cheap and standard approximation of "not fully buried".
     */
    private boolean isExposed(MinecraftClient client, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = client.world.getBlockState(pos.offset(dir));
            if (!neighbor.isOpaque()) {
                return true;
            }
        }
        return false;
    }
}
