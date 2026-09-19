package com.oresight.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * ============================================================================
 *  MOST VERSION-SENSITIVE FILE IN THE PROJECT -- READ THIS FIRST
 * ============================================================================
 * Everything else in OreSight is ordinary Fabric API usage that tends to be
 * stable release-to-release. This file is not: it reaches into
 * RenderLayer/RenderPhase internals (via the access widener at
 * src/main/resources/oresight.accesswidener) to build a render layer whose
 * depth-test phase is "always pass". That's the actual mechanism that lets
 * a highlighted ore render *through* the stone in front of it instead of
 * being hidden behind it -- the same basic trick most ESP/X-ray mods use.
 *
 * Mojang/Yarn occasionally rename fields or methods inside RenderLayer and
 * RenderPhase between versions. I can't compile-test this against the exact
 * 1.21.11 build from this sandbox (no Maven/Mojang network access here), so
 * if the build fails specifically in this file:
 *
 *   1. Run the Gradle task "genSources" (Fabric Loom adds it) and open the
 *      decompiled RenderLayer.java / RenderPhase.java in your IDE.
 *   2. Find the constant that means "always pass the depth test" (it has
 *      been ALWAYS_DEPTH_TEST for a long time) and the protected/package
 *      "of(...)" factory + MultiPhaseParameters.Builder on RenderLayer.
 *   3. Update the accessible-entries in oresight.accesswidener and the
 *      calls below to match whatever the exact names are for your build.
 *
 * Everything downstream (HighlightRenderer) just calls the two public
 * methods at the bottom of this class, so a fix here is self-contained.
 * ============================================================================
 */
public final class RenderLayers {

    private RenderLayers() {}

    private static final RenderLayer.MultiPhaseParameters THROUGH_WALLS_PARAMS =
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .build(false);

    /** Filled quads (used for the "solid" and "glow" highlight styles). */
    public static final RenderLayer HIGHLIGHT_QUADS = RenderLayer.of(
            "oresight_highlight_quads",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false,
            true,
            THROUGH_WALLS_PARAMS);

    /** Wireframe lines (used for the "outline" highlight style). */
    public static final RenderLayer HIGHLIGHT_LINES = RenderLayer.of(
            "oresight_highlight_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.LINES,
            256,
            false,
            true,
            THROUGH_WALLS_PARAMS);
}
