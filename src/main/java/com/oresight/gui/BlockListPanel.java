package com.oresight.gui;

import com.oresight.OreSightClient;
import com.oresight.config.BlockEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-rolled scrollable block list rather than subclassing one of
 * Minecraft's generic entry-list widgets. Those have had their constructor
 * signature shuffled a few times across versions; a plain ClickableWidget
 * with manual row math is a smaller, more stable surface to depend on.
 *
 * Layout per row: [toggle swatch][color swatch][block id text]
 */
public class BlockListPanel extends ClickableWidget {

    private static final int ROW_HEIGHT = 20;
    private static final int TOGGLE_SIZE = 14;
    private static final int SWATCH_SIZE = 14;

    private final ConfigScreen screen;
    private final List<String> allIds = new ArrayList<>();
    private final List<String> filteredIds = new ArrayList<>();
    private String filter = "";
    private double scroll = 0;

    public BlockListPanel(ConfigScreen screen, int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal("Block list"));
        this.screen = screen;
        refresh();
    }

    /** Call after the underlying block map changes (add/remove/reset). */
    public void refresh() {
        allIds.clear();
        allIds.addAll(OreSightClient.highlightManager.cfg().blocks.keySet());
        applyFilter();
    }

    public void setFilter(String text) {
        this.filter = text == null ? "" : text.toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        filteredIds.clear();
        for (String id : allIds) {
            if (filter.isEmpty() || id.toLowerCase().contains(filter)) {
                filteredIds.add(id);
            }
        }
        clampScroll();
    }

    private void clampScroll() {
        double maxScroll = Math.max(0, filteredIds.size() * ROW_HEIGHT - this.getHeight());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x66000000);

        var config = OreSightClient.highlightManager.cfg();
        int firstRow = (int) (scroll / ROW_HEIGHT);
        int rowOffset = (int) (scroll % ROW_HEIGHT);

        int rowY = getY() - rowOffset;
        for (int i = firstRow; i < filteredIds.size() && rowY < getY() + getHeight(); i++) {
            String id = filteredIds.get(i);
            BlockEntry entry = config.blocks.get(id);
            if (entry == null) {
                rowY += ROW_HEIGHT;
                continue;
            }

            boolean hovered = mouseX >= getX() && mouseX <= getX() + getWidth()
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            int bg = hovered ? 0x33FFFFFF : (i % 2 == 0 ? 0x22000000 : 0x00000000);
            context.fill(getX(), rowY, getX() + getWidth(), rowY + ROW_HEIGHT, bg);

            int toggleX = getX() + 4;
            int toggleY = rowY + (ROW_HEIGHT - TOGGLE_SIZE) / 2;
            int toggleColor = entry.enabled ? 0xFF55CC55 : 0xFFCC5555;
            context.fill(toggleX, toggleY, toggleX + TOGGLE_SIZE, toggleY + TOGGLE_SIZE, toggleColor);
            context.drawBorder(toggleX, toggleY, TOGGLE_SIZE, TOGGLE_SIZE, 0xFF000000);

            int swatchX = toggleX + TOGGLE_SIZE + 6;
            int swatchY = toggleY;
            context.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | entry.color);
            context.drawBorder(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 0xFF000000);

            int textX = swatchX + SWATCH_SIZE + 8;
            int textColor = entry.enabled ? 0xFFFFFF : 0xAAAAAA;
            context.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(id),
                    textX, rowY + (ROW_HEIGHT - 8) / 2, textColor, false);

            rowY += ROW_HEIGHT;
        }

        context.disableScissor();
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int relativeY = (int) (mouseY - getY() + scroll);
        int row = relativeY / ROW_HEIGHT;
        if (row < 0 || row >= filteredIds.size()) {
            return false;
        }

        String id = filteredIds.get(row);
        BlockEntry entry = OreSightClient.highlightManager.cfg().blocks.get(id);
        if (entry == null) {
            return false;
        }

        int swatchX = getX() + 4 + TOGGLE_SIZE + 6;
        if (mouseX >= swatchX && mouseX <= swatchX + SWATCH_SIZE) {
            MinecraftClient.getInstance().setScreen(new ColorPickerScreen(screen, entry, () -> {
                OreSightClient.highlightManager.configManager().save();
            }));
        } else {
            entry.enabled = !entry.enabled;
            OreSightClient.highlightManager.configManager().save();
            OreSightClient.highlightManager.invalidateAll();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll -= verticalAmount * ROW_HEIGHT * 2;
        clampScroll();
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.getMessage());
    }
}
