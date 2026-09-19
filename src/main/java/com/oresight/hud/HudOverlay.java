package com.oresight.hud;

import com.oresight.OreSightClient;
import com.oresight.config.BlockEntry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.hit.BlockHitResult;
import net.minecraft.hit.HitResult;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Two small pieces of on-screen UI, both optional and independently toggled:
 *   1. A tiny "Block Highlighting: ON/OFF" corner indicator (requirement 9).
 *   2. The name of a highlighted block when the player looks directly at it
 *      (requirement 14) -- only that one block's name, not a label spammed
 *      over every highlight on screen.
 */
public class HudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        var cfg = OreSightClient.highlightManager.cfg();
        MinecraftClient client = MinecraftClient.getInstance();

        if (cfg.hudEnabled) {
            String label = cfg.enabled ? "Block Highlighting: ON" : "Block Highlighting: OFF";
            int color = cfg.enabled ? 0x55FF55 : 0xFF5555;
            context.drawTextWithShadow(client.textRenderer, Text.literal(label), 6, 6, color);
        }

        if (cfg.enabled && cfg.showBlockNameOnLook) {
            renderLookedAtBlockName(context, client, cfg);
        }
    }

    private void renderLookedAtBlockName(DrawContext context, MinecraftClient client, com.oresight.config.ModConfig cfg) {
        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit) || client.world == null) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        BlockEntry entry = cfg.blocks.get(id);
        if (entry == null || !entry.enabled) {
            return;
        }

        Text name = state.getBlock().getName();
        int textWidth = client.textRenderer.getWidth(name);
        int x = (client.getWindow().getScaledWidth() - textWidth) / 2;
        int y = client.getWindow().getScaledHeight() / 2 + 12;
        context.drawTextWithShadow(client.textRenderer, name, x, y, 0xFFFFFF);
    }
}
