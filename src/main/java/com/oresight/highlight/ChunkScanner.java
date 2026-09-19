package com.oresight.highlight;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Caches, per chunk, the positions of blocks that match the enabled block
 * set. Scanning is spread across ticks (a couple of chunks per tick) rather
 * than done all at once, and only for chunks the player is actually near --
 * see requirement 15 (performance) in the original spec.
 *
 * This does NOT hook block-update packets (that would need a mixin into
 * ClientWorld, which is one more version-fragile moving part). Instead it
 * periodically re-queues chunks it has already scanned so mined/placed
 * blocks eventually fall in or out of the highlight set. If you want
 * instant updates instead of "within a few seconds", the natural extension
 * is a small mixin on ClientWorld#setBlockState calling
 * ChunkScanner#requestScan for the changed chunk.
 */
public class ChunkScanner {

    public static final class Hit {
        public final BlockPos pos;
        public final String blockId;

        Hit(BlockPos pos, String blockId) {
            this.pos = pos;
            this.blockId = blockId;
        }
    }

    /** How often (ms) a chunk we've already scanned gets automatically re-queued. */
    private static final long RESCAN_INTERVAL_MS = 8000;

    private final Map<Long, List<Hit>> cache = new HashMap<>();
    private final Deque<ChunkPos> scanQueue = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private long lastFullRescan = 0L;

    private static long key(ChunkPos p) {
        return p.toLong();
    }

    public List<Hit> getHits(ChunkPos pos) {
        return cache.getOrDefault(key(pos), Collections.emptyList());
    }

    public void requestScan(ChunkPos pos) {
        long k = key(pos);
        if (queued.add(k)) {
            scanQueue.add(pos);
        }
    }

    public void evict(ChunkPos pos) {
        cache.remove(key(pos));
    }

    public void clear() {
        cache.clear();
        scanQueue.clear();
        queued.clear();
    }

    /**
     * @param chunksPerTick how many chunks to (re)scan this tick. Keep this
     *                      small (1-3) so a big range slider doesn't cause
     *                      stutter when lots of new chunks appear at once
     *                      (e.g. right after a fast fly or teleport).
     */
    public void tick(MinecraftClient client, Set<String> targetBlocks, int chunksPerTick) {
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastFullRescan > RESCAN_INTERVAL_MS) {
            for (Long k : new ArrayList<>(cache.keySet())) {
                requestScan(new ChunkPos(unpackX(k), unpackZ(k)));
            }
            lastFullRescan = now;
        }

        int processed = 0;
        while (processed < chunksPerTick && !scanQueue.isEmpty()) {
            ChunkPos pos = scanQueue.poll();
            queued.remove(key(pos));
            scanChunk(world, pos, targetBlocks);
            processed++;
        }
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private void scanChunk(ClientWorld world, ChunkPos pos, Set<String> targetBlocks) {
        if (targetBlocks.isEmpty()) {
            cache.put(key(pos), Collections.emptyList());
            return;
        }

        WorldChunk chunk = world.getChunk(pos.x, pos.z);
        if (chunk == null) {
            return;
        }

        List<Hit> hits = new ArrayList<>();
        ChunkSection[] sections = chunk.getSectionArray();
        int bottomSectionCoord = world.getBottomSectionCoord();

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) {
                continue;
            }
            int sectionBaseY = (bottomSectionCoord + i) * 16;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) {
                            continue;
                        }
                        String id = Registries.BLOCK.getId(state.getBlock()).toString();
                        if (targetBlocks.contains(id)) {
                            hits.add(new Hit(
                                    new BlockPos(pos.getStartX() + x, sectionBaseY + y, pos.getStartZ() + z),
                                    id));
                        }
                    }
                }
            }
        }

        cache.put(key(pos), hits);
    }
}
