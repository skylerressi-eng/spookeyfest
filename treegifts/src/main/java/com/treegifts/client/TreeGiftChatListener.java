package com.treegifts.client;

import com.treegifts.TreeGiftsMod;
import com.treegifts.core.TreeGiftChatParser;
import com.treegifts.core.TreeGiftResult;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to incoming game chat, reconstructs the §-formatted text (so rarity
 * colours survive), pulls in the HOVER contents of the "+N rewards gained!" line
 * (where the guaranteed items live), and feeds it all to {@link TreeGiftChatParser}.
 *
 * This is the ONLY trigger for a reveal — no reveal without a real Tree Gift chat
 * message. Set {@code -Dtreegifts.debug=true} to log every line the mod sees,
 * which is the fastest way to diagnose a format change.
 */
public final class TreeGiftChatListener {

    private static final char SECTION = '§';
    private static final boolean DEBUG = Boolean.getBoolean("treegifts.debug");

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
            if (overlay) return;
            try {
                onMessage(message);
            } catch (Throwable t) {
                TreeGiftsMod.LOGGER.debug("[Tree Gifts] chat handling skipped: {}", t.toString());
            }
        });
    }

    private void onMessage(Component message) {
        String main = legacyString(message);
        List<String> hoverLines = hoverLines(message);

        List<TreeGiftResult> emitted = new ArrayList<>();
        boolean insertedHover = false;
        for (String line : main.split("\n", -1)) {
            if (DEBUG) TreeGiftsMod.LOGGER.info("[Tree Gifts][chat] {}", TreeGiftChatParser.strip(line));
            emitted.addAll(parser.feedLine(line));
            if (!insertedHover && !hoverLines.isEmpty()
                    && TreeGiftChatParser.strip(line).toLowerCase().contains("rewards gained")) {
                for (String hl : hoverLines) {
                    if (DEBUG) TreeGiftsMod.LOGGER.info("[Tree Gifts][hover] {}", TreeGiftChatParser.strip(hl));
                    emitted.addAll(parser.feedLine(hl));
                }
                insertedHover = true;
            }
        }
        if (!insertedHover && !hoverLines.isEmpty() && parser.inGift()) {
            for (String hl : hoverLines) emitted.addAll(parser.feedLine(hl));
        }

        if (emitted.isEmpty()) return;

        // De-dupe: identical result within a short window (same message twice).
        String sig = emitted.toString();
        long now = System.currentTimeMillis();
        if (sig.equals(lastSignature) && now - lastSignatureTime < 3000L) return;
        lastSignature = sig;
        lastSignatureTime = now;

        for (TreeGiftResult r : emitted) {
            TreeGiftsMod.LOGGER.info("[Tree Gifts] real drop revealed: {}", r);
            RealTreeGiftReveal.INSTANCE.enqueue(r);
        }
    }

    /** Collect the §-text of every SHOW_TEXT hover in the message's component tree. */
    private static List<String> hoverLines(Component message) {
        List<String> out = new ArrayList<>();
        for (Component part : message.toFlatList()) {
            Style style = part.getStyle();
            HoverEvent hover = style == null ? null : style.getHoverEvent();
            if (hover instanceof HoverEvent.ShowText st) {
                for (String line : legacyString(st.value()).split("\n", -1)) out.add(line);
            }
        }
        return out;
    }

    /** Rebuild a §-formatted string from a Component, preserving colour + styles. */
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
