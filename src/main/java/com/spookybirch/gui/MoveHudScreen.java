package com.spookybirch.gui;

import com.spookybirch.core.SpookyConfig;
import com.spookybirch.core.SpookyState;
import com.spookybirch.hud.SpookyHud;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Open with /spooky move. Click-drag the panel to reposition it, scroll to
 * scale, and it saves when you close. This is the Birch-style "edit HUD" mode.
 */
public class MoveHudScreen extends GuiScreen {

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        SpookyConfig cfg = SpookyConfig.INSTANCE;

        // Live preview using the current state.
        List<SpookyHud.Line> lines = SpookyHud.build(cfg, SpookyState.INSTANCE);
        int[] size = SpookyHud.panelSize(lines, cfg.hudScale);

        // Outline the draggable region.
        drawRect(cfg.hudX - 2, cfg.hudY - 2, cfg.hudX + size[0], cfg.hudY + size[1], 0x66FF8C1A);

        GlStateManager.pushMatrix();
        GlStateManager.translate(cfg.hudX, cfg.hudY, 0);
        GlStateManager.scale(cfg.hudScale, cfg.hudScale, 1f);
        int y = 0;
        int lineH = mc.fontRendererObj.FONT_HEIGHT + 2;
        for (SpookyHud.Line l : lines) {
            mc.fontRendererObj.drawStringWithShadow(l.text, 0, y, l.color);
            y += lineH;
        }
        GlStateManager.popMatrix();

        String help = "Drag to move  •  Scroll to scale (" + String.format("%.2f", cfg.hudScale) + "x)  •  Esc to save";
        drawCenteredString(mc.fontRendererObj, help, width / 2, height - 20, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            SpookyConfig cfg = SpookyConfig.INSTANCE;
            dragging = true;
            dragOffsetX = mouseX - cfg.hudX;
            dragOffsetY = mouseY - cfg.hudY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        if (dragging) {
            SpookyConfig cfg = SpookyConfig.INSTANCE;
            cfg.hudX = Math.max(0, mouseX - dragOffsetX);
            cfg.hudY = Math.max(0, mouseY - dragOffsetY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            SpookyConfig cfg = SpookyConfig.INSTANCE;
            cfg.hudScale += (wheel > 0 ? 0.1f : -0.1f);
            if (cfg.hudScale < 0.5f) cfg.hudScale = 0.5f;
            if (cfg.hudScale > 2.5f) cfg.hudScale = 2.5f;
        }
    }

    @Override
    public void onGuiClosed() {
        SpookyConfig.INSTANCE.save();
    }
}
