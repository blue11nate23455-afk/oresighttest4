package com.oresight;

import com.oresight.config.ConfigManager;
import com.oresight.gui.ConfigScreen;
import com.oresight.highlight.HighlightManager;
import com.oresight.hud.HudOverlay;
import com.oresight.render.HighlightRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OreSightClient implements ClientModInitializer {

    public static final String MOD_ID = "oresight";

    public static ConfigManager configManager;
    public static HighlightManager highlightManager;

    public static KeyBinding toggleKey;
    public static KeyBinding menuKey;
    public static KeyBinding hideKey;

    @Override
    public void onInitializeClient() {
        configManager = new ConfigManager();
        configManager.load();

        highlightManager = new HighlightManager(configManager);

        // Default keys: only "toggle" gets a default bind (O). The other two
        // start unbound so they never collide with something the player is
        // already using; all three can be rebound in Options > Controls
        // under the "OreSight" category, per requirement 10.
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oresight.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.oresight.general"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oresight.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.oresight.general"));
        hideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oresight.hide", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.oresight.general"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            while (toggleKey.wasPressed()) {
                configManager.getConfig().enabled = !configManager.getConfig().enabled;
                configManager.save();
            }
            while (menuKey.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }

            highlightManager.setTemporarilyHidden(hideKey.isPressed());
            highlightManager.tick(client);
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(new HighlightRenderer(highlightManager));
        HudRenderCallback.EVENT.register(new HudOverlay());
    }
}
