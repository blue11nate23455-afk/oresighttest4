package com.oresight.render;

import com.oresight.config.BlockEntry;
import com.oresight.config.ModConfig;
import com.oresight.highlight.ChunkScanner;
import com.oresight.highlight.HighlightManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Registered on WorldRenderEvents.AFTER_TRANSLUCENT. For every currently
 * highlighted block (already range/mode filtered by HighlightManager) this
 * draws an outline and/or filled/glow box using the "always render" layers
 * from RenderLayers, which is what makes it visible through terrain while
 * the terrain itself stays completely normal and opaque.
 */
public class HighlightRenderer implements WorldRenderEvents.AfterTranslucent {

    private final HighlightManager highlightManager;

    public HighlightRenderer(HighlightManager highlightManager) {
        this.highlightManager = highlightManager;
    }

    @Override
    public void afterTranslucent(WorldRenderContext context) {
        ModConfig cfg = highlightManager.cfg();
        if (!cfg.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        List<ChunkScanner.Hit> hits = highlightManager.collectHits(client);
        if (hits.isEmpty()) {
            return;
        }

        VertexConsumerProvider providerRaw = context.consumers();
        if (!(providerRaw instanceof VertexConsumerProvider.Immediate)) {
            return;
        }
        VertexConsumerProvider.Immediate consumers = (VertexConsumerProvider.Immediate) providerRaw;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float pulse = 1.0f;
        if (cfg.pulseEnabled) {
            float t = (System.currentTimeMillis() % 100000L) / 1000f * cfg.pulseSpeed;
            pulse = 0.75f + 0.25f * (float) Math.sin(t * Math.PI);
        }

        boolean drawOutline = cfg.highlightStyle.equals("OUTLINE") || cfg.highlightStyle.equals("OUTLINE_GLOW");
        boolean drawSolid = cfg.highlightStyle.equals("SOLID");
        boolean drawGlow = cfg.highlightStyle.equals("GLOW") || cfg.highlightStyle.equals("OUTLINE_GLOW");

        for (ChunkScanner.Hit hit : hits) {
            BlockEntry entry = cfg.blocks.get(hit.blockId);
            if (entry == null) {
                continue;
            }

            double rx = hit.pos.getX() - camPos.x;
            double ry = hit.pos.getY() - camPos.y;
            double rz = hit.pos.getZ() - camPos.z;

            float fade = fadeFactor(cfg, rx, ry, rz);
            if (fade <= 0.001f) {
                continue;
            }

            float r = MathHelper.clamp(((entry.color >> 16) & 0xFF) / 255f * cfg.brightness, 0f, 1f);
            float g = MathHelper.clamp(((entry.color >> 8) & 0xFF) / 255f * cfg.brightness, 0f, 1f);
            float b = MathHelper.clamp((entry.color & 0xFF) / 255f * cfg.brightness, 0f, 1f);

            if (drawSolid) {
                float a = cfg.fillOpacity * fade * pulse;
                if (a > 0.001f) {
                    VertexConsumer buf = consumers.getBuffer(RenderLayers.HIGHLIGHT_QUADS);
                    BoxRenderer.drawFilledBox(buf, posMatrix,
                            (float) rx, (float) ry, (float) rz,
                            (float) rx + 1f, (float) ry + 1f, (float) rz + 1f,
                            r, g, b, a);
                }
            }

            if (drawGlow) {
                float expand = 0.06f + 0.10f * cfg.glowIntensity;
                float a = cfg.glowIntensity * 0.5f * fade * pulse;
                if (a > 0.001f) {
                    VertexConsumer buf = consumers.getBuffer(RenderLayers.HIGHLIGHT_QUADS);
                    BoxRenderer.drawFilledBox(buf, posMatrix,
                            (float) rx - expand, (float) ry - expand, (float) rz - expand,
                            (float) rx + 1f + expand, (float) ry + 1f + expand, (float) rz + 1f + expand,
                            r, g, b, a);
                }
            }

            if (drawOutline) {
                float a = cfg.outlineOpacity * fade * pulse;
                if (a > 0.001f) {
                    VertexConsumer buf = consumers.getBuffer(RenderLayers.HIGHLIGHT_LINES);
                    BoxRenderer.drawOutlineBox(buf, posMatrix,
                            (float) rx, (float) ry, (float) rz,
                            (float) rx + 1f, (float) ry + 1f, (float) rz + 1f,
                            r, g, b, a, cfg.outlineThickness);
                }
            }
        }

        consumers.draw(RenderLayers.HIGHLIGHT_QUADS);
        consumers.draw(RenderLayers.HIGHLIGHT_LINES);
    }

    private float fadeFactor(ModConfig cfg, double rx, double ry, double rz) {
        if (!cfg.distanceFade) {
            return 1.0f;
        }
        double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
        double range = Math.max(1, cfg.rangeBlocks);
        double fadeStart = range * 0.6;
        if (dist <= fadeStart) {
            return 1.0f;
        }
        double t = (dist - fadeStart) / (range - fadeStart);
        return (float) MathHelper.clamp(1.0 - t, 0.0, 1.0);
    }
}
