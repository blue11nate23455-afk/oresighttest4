package com.oresight.render;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Plain geometry emission -- builds a filled cube (6 quads) or a wireframe
 * cube (12 edges) into whatever VertexConsumer/RenderLayer it's handed.
 * Deliberately has zero Minecraft-version-specific tricks in it; if
 * something here fails to compile, it's almost certainly because
 * VertexConsumer's vertex()/color()/next() chain got renamed, which is rare.
 */
public final class BoxRenderer {

    private BoxRenderer() {}

    public static void drawFilledBox(VertexConsumer buf, Matrix4f matrix,
                                      float minX, float minY, float minZ,
                                      float maxX, float maxY, float maxZ,
                                      float r, float g, float b, float a) {
        int ri = toByte(r);
        int gi = toByte(g);
        int bi = toByte(b);
        int ai = toByte(a);

        // Culling is disabled on this render layer, so winding order doesn't matter.
        quad(buf, matrix, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, ri, gi, bi, ai); // -X
        quad(buf, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, ri, gi, bi, ai); // +X
        quad(buf, matrix, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, ri, gi, bi, ai); // -Y
        quad(buf, matrix, minX, maxY, maxZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, ri, gi, bi, ai); // +Y
        quad(buf, matrix, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, ri, gi, bi, ai); // -Z
        quad(buf, matrix, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, ri, gi, bi, ai); // +Z
    }

    public static void drawOutlineBox(VertexConsumer buf, Matrix4f matrix,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       float r, float g, float b, float a,
                                       float thickness) {
        int ri = toByte(r);
        int gi = toByte(g);
        int bi = toByte(b);
        int ai = toByte(a);

        edges(buf, matrix, minX, minY, minZ, maxX, maxY, maxZ, ri, gi, bi, ai);

        // "Thickness" fake: draw a second, very slightly expanded wireframe
        // on top instead of touching GL line-width state, which behaves
        // inconsistently across drivers/GL versions. Cheap but reliable.
        if (thickness > 1.5f) {
            float expand = Math.min(0.02f, (thickness - 1.0f) * 0.01f);
            edges(buf, matrix,
                    minX - expand, minY - expand, minZ - expand,
                    maxX + expand, maxY + expand, maxZ + expand,
                    ri, gi, bi, (int) (ai * 0.6f));
        }
    }

    private static void edges(VertexConsumer buf, Matrix4f m,
                               float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ,
                               int r, int g, int b, int a) {
        // bottom face
        line(buf, m, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buf, m, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buf, m, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buf, m, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        // top face
        line(buf, m, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buf, m, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buf, m, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buf, m, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        // vertical edges
        line(buf, m, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buf, m, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buf, m, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buf, m, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void quad(VertexConsumer buf, Matrix4f m,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              int r, int g, int b, int a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).next();
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).next();
        buf.vertex(m, x3, y3, z3).color(r, g, b, a).next();
        buf.vertex(m, x4, y4, z4).color(r, g, b, a).next();
    }

    private static void line(VertexConsumer buf, Matrix4f m,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              int r, int g, int b, int a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).next();
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).next();
    }

    private static int toByte(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255f)));
    }
}
