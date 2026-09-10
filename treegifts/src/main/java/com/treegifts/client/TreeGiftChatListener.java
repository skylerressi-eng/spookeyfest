package com.treegifts.client;

import com.treegifts.TreeGiftsMod;
import com.treegifts.core.TreeGiftChatParser;
import com.treegifts.core.TreeGiftResult;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to incoming game chat, reconstructs the §-formatted string (so the
 * rarity colour survives), and feeds it to {@link TreeGiftChatParser}. When a
 * real Tree Gift block completes, the parsed drops are handed to
 * {@link RealTreeGiftReveal}.
 *
 * This is the ONLY trigger for a reveal: no reveal ever happens without a real
 * Tree Gift chat message. Local block-breaking is deliberately NOT used to
 * generate results.
 */
public final class TreeGiftChatListener {

    private static final char SECTION = '§';

    /** Map from a legacy colour's RGB value to its §-code char. */
    private static final Map<Integer, Character> COLOR_TO_CODE = new HashMap<>();
    static {
        for (ChatFormatting f : ChatFormatting.values()) {
            Integer rgb = f.getColor();
            if (rgb != null) COLOR_TO_CODE.put(rgb, f.getChar());
        }
    }

    private final TreeGiftChatParser parser = new TreeGiftChatParser();
    private String lastSignature = "";
    private long lastSignatureTime = 0L;

    public void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // action-bar text, never a gift
            try {
                onLine(legacyString(message));
            } catch (Throwable t) {
                // Never let a chat quirk break the game; just log it.
                TreeGiftsMod.LOGGER.debug("[Tree Gifts] chat parse skipped: {}", t.toString());
            }
        });
    }

    private void onLine(String legacy) {
        List<TreeGiftResult> results = parser.feedLine(legacy);
        if (results.isEmpty()) return;

        // Duplicate protection: identical block within a short window is ignored
        // (the same message can occasionally be delivered twice).
        String sig = results.toString();
        long now = System.currentTimeMillis();
        if (sig.equals(lastSignature) && now - lastSignatureTime < 3000L) return;
        lastSignature = sig;
        lastSignatureTime = now;

        for (TreeGiftResult r : results) {
            TreeGiftsMod.LOGGER.info("[Tree Gifts] real drop: {}", r);
            RealTreeGiftReveal.INSTANCE.enqueue(r);
        }
    }

    /**
     * Rebuild a §-formatted string from a Component, preserving colour + styles so
     * the parser can read rarity from the colour. Uses the vanilla visit() walk.
     */
    static String legacyString(Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, text) -> {
            appendStyle(sb, style);
            sb.append(text);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static void appendStyle(StringBuilder sb, Style style) {
        TextColor color = style.getColor();
        if (color != null) {
            Character code = COLOR_TO_CODE.get(color.getValue());
            if (code != null) sb.append(SECTION).append(code.charValue());
        }
        if (style.isBold()) sb.append(SECTION).append('l');
        if (style.isStrikethrough()) sb.append(SECTION).append('m');
        if (style.isUnderlined()) sb.append(SECTION).append('n');
        if (style.isItalic()) sb.append(SECTION).append('o');
        if (style.isObfuscated()) sb.append(SECTION).append('k');
    }
}
