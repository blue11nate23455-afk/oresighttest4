package com.oresight.gui;

import com.oresight.OreSightClient;
import com.oresight.config.BlockEntry;
import com.oresight.config.Categories;
import com.oresight.config.ConfigManager;
import com.oresight.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * The whole settings menu: a row of tab buttons up top, per-tab controls in
 * the middle, and a Reset/Done bar at the bottom. Widgets are rebuilt from
 * scratch on every tab switch rather than hidden/shown -- simpler to reason
 * about and this screen is only opened occasionally, so it's not worth
 * optimizing.
 */
public class ConfigScreen extends Screen {

    private enum Tab { GENERAL, BLOCKS, APPEARANCE, ANIMATION, PROFILES }

    private final Screen parent;
    private Tab currentTab = Tab.GENERAL;

    private BlockListPanel blockListPanel;
    private TextFieldWidget searchField;
    private TextFieldWidget addBlockField;
    private TextFieldWidget profileNameField;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("oresight.title"));
        this.parent = parent;
    }

    private ModConfig cfg() {
        return OreSightClient.highlightManager.cfg();
    }

    private ConfigManager configManager() {
        return OreSightClient.highlightManager.configManager();
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        blockListPanel = null;

        int tabY = 24;
        int tabWidth = 90;
        int startX = this.width / 2 - (tabWidth * 5) / 2;
        addTabButton(startX, tabY, tabWidth, "oresight.tab.general", Tab.GENERAL);
        addTabButton(startX + tabWidth, tabY, tabWidth, "oresight.tab.blocks", Tab.BLOCKS);
        addTabButton(startX + tabWidth * 2, tabY, tabWidth, "oresight.tab.appearance", Tab.APPEARANCE);
        addTabButton(startX + tabWidth * 3, tabY, tabWidth, "oresight.tab.animation", Tab.ANIMATION);
        addTabButton(startX + tabWidth * 4, tabY, tabWidth, "oresight.tab.profiles", Tab.PROFILES);

        switch (currentTab) {
            case GENERAL -> buildGeneralTab();
            case BLOCKS -> buildBlocksTab();
            case APPEARANCE -> buildAppearanceTab();
            case ANIMATION -> buildAnimationTab();
            case PROFILES -> buildProfilesTab();
        }

        int bottomY = this.height - 26;
        addDrawableChild(ButtonWidget.builder(Text.translatable("oresight.profiles.reset"), b -> {
                    configManager().resetToDefaults();
                    OreSightClient.highlightManager.invalidateAll();
                    rebuild();
                })
                .dimensions(this.width / 2 - 154, bottomY, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("oresight.done"), b -> close())
                .dimensions(this.width / 2 + 4, bottomY, 150, 20)
                .build());
    }

    private void addTabButton(int x, int y, int w, String key, Tab tab) {
        boolean active = currentTab == tab;
        ButtonWidget button = ButtonWidget.builder(Text.translatable(key), b -> {
                    currentTab = tab;
                    rebuild();
                })
                .dimensions(x, y, w - 2, 20)
                .build();
        button.active = !active;
        addDrawableChild(button);
    }

    // ---------------------------------------------------------------- GENERAL

    private void buildGeneralTab() {
        int x = this.width / 2 - 150;
        int y = 60;
        int rowH = 24;
        ModConfig cfg = cfg();

        addDrawableChild(ButtonWidget.builder(
                        onOffLabel("oresight.general.enabled", cfg.enabled),
                        b -> {
                            cfg.enabled = !cfg.enabled;
                            configManager().save();
                            b.setMessage(onOffLabel("oresight.general.enabled", cfg.enabled));
                        })
                .dimensions(x, y, 300, 20).build());
        y += rowH;

        addDrawableChild(CyclingButtonWidget.<String>builder(m -> Text.translatable(
                        m.equals("THROUGH_TERRAIN") ? "oresight.general.mode.through" : "oresight.general.mode.visible"))
                .values("THROUGH_TERRAIN", "VISIBLE_ONLY")
                .initially(cfg.mode)
                .build(x, y, 300, 20, Text.translatable("oresight.general.mode"), (b, v) -> {
                    cfg.mode = v;
                    configManager().save();
                }));
        y += rowH;

        addDrawableChild(new SliderWidget(x, y, 300, 20,
                Text.translatable("oresight.general.range").append(Text.literal(": " + cfg.rangeBlocks)),
                (cfg.rangeBlocks - 8) / (256.0 - 8.0)) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("oresight.general.range").append(Text.literal(": " + cfg.rangeBlocks)));
            }

            @Override
            protected void applyValue() {
                cfg.rangeBlocks = (int) (8 + value * (256 - 8));
                configManager().save();
            }
        });
        y += rowH;

        addDrawableChild(ButtonWidget.builder(
                        onOffLabel("oresight.general.fade", cfg.distanceFade),
                        b -> {
                            cfg.distanceFade = !cfg.distanceFade;
                            configManager().save();
                            b.setMessage(onOffLabel("oresight.general.fade", cfg.distanceFade));
                        })
                .dimensions(x, y, 300, 20).build());
        y += rowH;

        addDrawableChild(ButtonWidget.builder(
                        onOffLabel("oresight.general.hud", cfg.hudEnabled),
                        b -> {
                            cfg.hudEnabled = !cfg.hudEnabled;
                            configManager().save();
                            b.setMessage(onOffLabel("oresight.general.hud", cfg.hudEnabled));
                        })
                .dimensions(x, y, 300, 20).build());
        y += rowH;

        addDrawableChild(ButtonWidget.builder(
                        onOffLabel("oresight.general.blockname", cfg.showBlockNameOnLook),
                        b -> {
                            cfg.showBlockNameOnLook = !cfg.showBlockNameOnLook;
                            configManager().save();
                            b.setMessage(onOffLabel("oresight.general.blockname", cfg.showBlockNameOnLook));
                        })
                .dimensions(x, y, 300, 20).build());
    }

    // ---------------------------------------------------------------- BLOCKS

    private void buildBlocksTab() {
        int x = this.width / 2 - 160;
        int top = 52;

        searchField = new TextFieldWidget(this.textRenderer, x, top, 220, 18,
                Text.translatable("oresight.blocks.search"));
        searchField.setPlaceholder(Text.translatable("oresight.blocks.search"));
        searchField.setChangedListener(s -> blockListPanel.setFilter(s));
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("oresight.blocks.enableall"), b -> {
                    cfg().blocks.values().forEach(e -> e.enabled = true);
                    configManager().save();
                    OreSightClient.highlightManager.invalidateAll();
                    blockListPanel.refresh();
                })
                .dimensions(x + 224, top, 68, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("oresight.blocks.disableall"), b -> {
                    cfg().blocks.values().forEach(e -> e.enabled = false);
                    configManager().save();
                    OreSightClient.highlightManager.invalidateAll();
                    blockListPanel.refresh();
                })
                .dimensions(x + 296, top, 84, 18).build());

        // Category / preset quick-enable buttons.
        int catY = top + 22;
        int catX = x;
        for (String category : Categories.PRESETS.keySet()) {
            int w = Math.min(110, 12 + this.textRenderer.getWidth(category));
            addDrawableChild(ButtonWidget.builder(Text.literal(category), b -> {
                        for (String id : Categories.PRESETS.get(category)) {
                            BlockEntry entry = cfg().blocks.computeIfAbsent(id, k ->
                                    new BlockEntry(id, false, Categories.DEFAULT_COLORS.getOrDefault(id, 0xFFFFFF)));
                            entry.enabled = true;
                        }
                        configManager().save();
                        OreSightClient.highlightManager.invalidateAll();
                        blockListPanel.refresh();
                    })
                    .dimensions(catX, catY, w, 16).build());
            catX += w + 4;
            if (catX > this.width / 2 + 160) {
                catX = x;
                catY += 18;
            }
        }

        int listTop = catY + 22;
        int listBottom = this.height - 84;
        blockListPanel = new BlockListPanel(this, x, listTop, 380, Math.max(40, listBottom - listTop));
        addDrawableChild(blockListPanel);

        int addY = this.height - 58;
        addBlockField = new TextFieldWidget(this.textRenderer, x, addY, 260, 18,
                Text.literal("modid:block_id"));
        addBlockField.setPlaceholder(Text.literal("modid:block_id (add a modded/vanilla block)"));
        addDrawableChild(addBlockField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
                    String id = addBlockField.getText().trim();
                    if (!id.isEmpty() && id.contains(":")) {
                        cfg().blocks.computeIfAbsent(id, k -> new BlockEntry(id, true, 0xFFFFFF));
                        configManager().save();
                        OreSightClient.highlightManager.invalidateAll();
                        addBlockField.setText("");
                        blockListPanel.refresh();
                    }
                })
                .dimensions(x + 264, addY, 60, 18).build());
    }

    // ------------------------------------------------------------ APPEARANCE

    private void buildAppearanceTab() {
        int x = this.width / 2 - 150;
        int y = 60;
        int rowH = 24;
        ModConfig cfg = cfg();

        addDrawableChild(CyclingButtonWidget.<String>builder(this::styleLabel)
                .values("OUTLINE", "SOLID", "GLOW", "OUTLINE_GLOW")
                .initially(cfg.highlightStyle)
                .build(x, y, 300, 20, Text.translatable("oresight.appearance.style"), (b, v) -> {
                    cfg.highlightStyle = v;
                    configManager().save();
                }));
        y += rowH;

        y = addFloatSlider(x, y, rowH, "oresight.appearance.brightness", cfg.brightness, 0.2f, 2.0f,
                v -> cfg.brightness = v);
        y = addFloatSlider(x, y, rowH, "oresight.appearance.fillopacity", cfg.fillOpacity, 0f, 1f,
                v -> cfg.fillOpacity = v);
        y = addFloatSlider(x, y, rowH, "oresight.appearance.outlineopacity", cfg.outlineOpacity, 0f, 1f,
                v -> cfg.outlineOpacity = v);
        y = addFloatSlider(x, y, rowH, "oresight.appearance.outlinethickness", cfg.outlineThickness, 1f, 6f,
                v -> cfg.outlineThickness = v);
        addFloatSlider(x, y, rowH, "oresight.appearance.glowintensity", cfg.glowIntensity, 0f, 1f,
                v -> cfg.glowIntensity = v);
    }

    private Text styleLabel(String v) {
        return switch (v) {
            case "OUTLINE" -> Text.translatable("oresight.appearance.style.outline");
            case "SOLID" -> Text.translatable("oresight.appearance.style.solid");
            case "GLOW" -> Text.translatable("oresight.appearance.style.glow");
            default -> Text.translatable("oresight.appearance.style.outlineglow");
        };
    }

    // ------------------------------------------------------------- ANIMATION

    private void buildAnimationTab() {
        int x = this.width / 2 - 150;
        int y = 60;
        int rowH = 24;
        ModConfig cfg = cfg();

        addDrawableChild(ButtonWidget.builder(
                        onOffLabel("oresight.animation.pulse", cfg.pulseEnabled),
                        b -> {
                            cfg.pulseEnabled = !cfg.pulseEnabled;
                            configManager().save();
                            b.setMessage(onOffLabel("oresight.animation.pulse", cfg.pulseEnabled));
                        })
                .dimensions(x, y, 300, 20).build());
        y += rowH;

        addFloatSlider(x, y, rowH, "oresight.animation.speed", cfg.pulseSpeed, 0.2f, 3.0f,
                v -> cfg.pulseSpeed = v);
    }

    // -------------------------------------------------------------- PROFILES

    private void buildProfilesTab() {
        int x = this.width / 2 - 150;
        int y = 56;

        profileNameField = new TextFieldWidget(this.textRenderer, x, y, 220, 18, Text.literal("Profile name"));
        profileNameField.setText(cfg().activeProfile);
        addDrawableChild(profileNameField);
        addDrawableChild(ButtonWidget.builder(Text.translatable("oresight.profiles.save"), b -> {
                    String name = profileNameField.getText().trim();
                    if (!name.isEmpty()) {
                        cfg().activeProfile = name;
                        configManager().saveProfile(name);
                        rebuild();
                    }
                })
                .dimensions(x + 224, y, 76, 18).build());

        y += 28;
        List<String> profiles = configManager().listProfiles();
        for (String name : profiles) {
            addDrawableChild(ButtonWidget.builder(
                            Text.literal((name.equals(cfg().activeProfile) ? "* " : "") + name),
                            b -> {
                                configManager().loadProfile(name);
                                OreSightClient.highlightManager.invalidateAll();
                                rebuild();
                            })
                    .dimensions(x, y, 300, 18).build());
            y += 20;
            if (y > this.height - 100) {
                break; // keep it simple: no scrolling list for profiles
            }
        }
    }

    // --------------------------------------------------------------- helpers

    private int addFloatSlider(int x, int y, int rowH, String key, float current, float min, float max,
                                java.util.function.Consumer<Float> setter) {
        addDrawableChild(new SliderWidget(x, y, 300, 20,
                Text.translatable(key).append(Text.literal(": " + String.format(Locale.ROOT, "%.2f", current))),
                (current - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                float v = min + (float) value * (max - min);
                setMessage(Text.translatable(key).append(Text.literal(": " + String.format(Locale.ROOT, "%.2f", v))));
            }

            @Override
            protected void applyValue() {
                float v = min + (float) value * (max - min);
                setter.accept(v);
                configManager().save();
            }
        });
        return y + rowH;
    }

    private Text onOffLabel(String key, boolean on) {
        return Text.translatable(key).append(Text.literal(": " + (on ? "ON" : "OFF")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void removed() {
        // Requirement: settings auto-save when leaving the menu.
        configManager().save();
        super.removed();
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
