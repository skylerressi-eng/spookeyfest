package com.treegifts.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel SkyBlock's real Tree Gift chat message into locked
 * {@link TreeGiftResult}s. This is the ONLY source of a reveal's item + rarity —
 * there is no randomness here or downstream.
 *
 * The message (on Galatea) is a block bracketed by dark-green bars and headed by
 * "TREE GIFT", e.g. (formatting shown with §):
 * <pre>
 *   §2§l▬▬▬▬… (64)
 *                    §r§9§lTREE GIFT
 *        §r§7You helped cut §r§a100% §r§7of the §r§aFig Tree§r§7.
 *   §f              §e+5 rewards gained! §8(hover)
 *                    §r§d§lBONUS GIFT
 *            §r§7§r§aStretching Sticks §r§8(§r§a20%§r§8)
 *            §r§7§r§cTree the Fish §r§8(§r§a0.05%§r§8)
 *   §r§7A §r§dPhanpyre §r§7fell from the Tree!
 *   §2§l▬▬▬▬… (64)
 * </pre>
 * Crucially, the §-colour of an item's name is its rarity, so we decode the
 * colour active at the item's first visible character (never guess from a name).
 *
 * Formats verified against SkyHanni's ForagingTracker regex tests. Pure Java.
 *
 * Not thread-safe: feed lines from one thread (the client thread).
 */
public final class TreeGiftChatParser {

    private static final char SECTION = '§'; // the § legacy-formatting marker
    private static final char BAR = '▬';     // the ▬ bar used in the gift border

    // Patterns run on the colour-STRIPPED, trimmed line.
    private static final Pattern CONTRIBUTION =
            Pattern.compile("You helped cut ([0-9.]+)% of the (.+?) Tree\\.");
    private static final Pattern BONUS_REWARD =
            Pattern.compile("^(.+?) \\(([0-9.]+)%\\)$");
    private static final Pattern PHANTOM =
            Pattern.compile("^An? (.+?) fell from the Tree!$");

    private boolean inGift = false;
    private boolean sawHeader = false;
    private String treeType = "";
    private final List<String> lines = new ArrayList<>();

    /** Reset any partial state (e.g. on world change). */
    public void reset() {
        inGift = false;
        sawHeader = false;
        treeType = "";
        lines.clear();
    }

    /**
     * Feed one raw (§-formatted) chat line. Returns the notable drops when a Tree
     * Gift block has just closed; otherwise an empty list. "Notable" = bonus-gift
     * and phantom drops (the rare, rarity-coloured ones worth revealing).
     */
    public List<TreeGiftResult> feedLine(String raw) {
        if (raw == null) return List.of();
        String stripped = strip(raw).trim();

        if (isBorder(stripped)) {
            if (inGift) {
                // Closing border — finish the block iff it was really a Tree Gift.
                List<TreeGiftResult> out = sawHeader ? parseBlock() : List.of();
                reset();
                return out;
            }
            // Opening border.
            inGift = true;
            sawHeader = false;
            treeType = "";
            lines.clear();
            return List.of();
        }

        if (inGift) {
            if (stripped.toUpperCase().contains("TREE GIFT")) {
                sawHeader = true;
            }
            Matcher c = CONTRIBUTION.matcher(stripped);
            if (c.find()) {
                treeType = c.group(2).trim();
            }
            lines.add(raw);
            // Guard against a runaway buffer if a closing border is ever missed.
            if (lines.size() > 64) {
                reset();
            }
        }
        return List.of();
    }

    private List<TreeGiftResult> parseBlock() {
        List<TreeGiftResult> out = new ArrayList<>();
        for (String raw : lines) {
            Decoded d = decode(raw);
            String plain = d.plain.trim();

            Matcher ph = PHANTOM.matcher(plain);
            if (ph.matches()) {
                String name = ph.group(1).trim();
                out.add(build(raw, d, name, -1.0, true));
                continue;
            }
            Matcher b = BONUS_REWARD.matcher(plain);
            if (b.matches()) {
                String name = b.group(1).trim();
                double pct = parseDouble(b.group(2));
                // Skip the odd non-item line that could look like "(x%)".
                if (!name.isEmpty() && !name.equalsIgnoreCase("BONUS GIFT")) {
                    out.add(build(raw, d, name, pct, false));
                }
            }
        }
        return out;
    }

    private TreeGiftResult build(String raw, Decoded d, String name, double pct, boolean phantom) {
        Rarity rarity = rarityOfName(d, name);
        return new TreeGiftResult(name, rarity, -1, pct, treeType, phantom, ItemIcons.iconFor(name));
    }

    /** Rarity = the colour active at the item name's first visible character. */
    private Rarity rarityOfName(Decoded d, String name) {
        int idx = d.plain.indexOf(name);
        if (idx < 0 || idx >= d.colors.length) return Rarity.COMMON;
        return Rarity.fromColorCode(d.colors[idx]);
    }

    // ---- formatting helpers (no Minecraft) ----

    /** A line with all § codes removed. */
    static String strip(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == SECTION && i + 1 < raw.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Decode a §-line into its visible text plus the colour code active at each char. */
    static Decoded decode(String raw) {
        StringBuilder plain = new StringBuilder(raw.length());
        StringBuilder cols = new StringBuilder(raw.length());
        char cur = 'f';
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == SECTION && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(++i));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    cur = code; // colour
                } else if (code == 'r') {
                    cur = 'f'; // reset
                }
                // 'k'-'o' are style codes; they don't change the colour.
                continue;
            }
            plain.append(c);
            cols.append(cur);
        }
        return new Decoded(plain.toString(), cols.toString().toCharArray());
    }

    private static boolean isBorder(String stripped) {
        if (stripped.length() < 20) return false;
        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) != BAR) return false;
        }
        return true;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1.0; }
    }

    static final class Decoded {
        final String plain;
        final char[] colors;
        Decoded(String plain, char[] colors) { this.plain = plain; this.colors = colors; }
    }
}
