package com.oresight.gui;

import com.oresight.config.BlockEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final BlockEntry entry;
    private final Runnable onSave;

    private int r;
    private int g;
    private int b;

    public ColorPickerScreen(Screen parent, BlockEntry entry, Runnable onSave) {
        super(Text.literal("Color: " + entry.blockId));
        this.parent = parent;
        this.entry = entry;
        this.onSave = onSave;
        this.r = (entry.color >> 16) & 0xFF;
        this.g = (entry.color >> 8) & 0xFF;
        this.b = entry.color & 0xFF;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 50;

        addDrawableChild(channelSlider(x, y, "R", r, v -> r = v));
        addDrawableChild(channelSlider(x, y + 24, "G", g, v -> g = v));
        addDrawableChild(channelSlider(x, y + 48, "B", b, v -> b = v));

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> {
                    entry.color = (r << 16) | (g << 8) | b;
                    onSave.run();
                    close();
                })
                .dimensions(x, y + 84, 200, 20).build());
    }

    private SliderWidget channelSlider(int x, int y, String label, int initial, java.util.function.IntConsumer setter) {
        return new SliderWidget(x, y, 200, 20, Text.literal(label + ": " + initial), initial / 255.0) {
            @Override
            protected void updateMessage() {
                int v = (int) Math.round(value * 255);
                setMessage(Text.literal(label + ": " + v));
            }

            @Override
            protected void applyValue() {
                int v = (int) Math.round(value * 255);
                setter.accept(v);
            }
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int previewColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        int px = this.width / 2 + 110;
        int py = this.height / 2 - 50;
        context.fill(px, py, px + 40, py + 40, previewColor);
        context.drawBorder(px, py, 40, 40, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
