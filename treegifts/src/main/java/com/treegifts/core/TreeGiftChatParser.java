package com.treegifts.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel SkyBlock's real Tree Gift chat message into ONE locked
 * {@link TreeGiftResult} per gift — the headline drop to reveal. This is the only
 * source of a reveal's item + rarity; there is no randomness here or downstream.
 *
 * The message (on Galatea) is a block bracketed by dark-green bars and headed by
 * "TREE GIFT". Its visible lines only spell out the rare "bonus gift" and phantom
 * drops; the guaranteed items (Forest Essence, Foraging XP, …) live in the HOVER
 * of the "+N rewards gained!" line, so the client feeds those hover lines in too
 * (see TreeGiftChatListener). That's why a plain Fig/Helix tree still reveals.
 *
 * The §-colour of an item's name is its rarity — we decode the colour active at
 * the item's first visible character and never guess from the name.
 *
 * Headline choice per gift:
 *   1. the rarest bonus/phantom drop (rare, colour-coded), else
 *   2. the biggest guaranteed item (Forest Essence / XP / …), else
 *   3. a generic "Tree Gift" summarising the reward count.
 * So every real Tree Gift yields exactly one reveal.
 *
 * Formats verified against SkyHanni's ForagingTracker regex tests. Pure Java.
 * Not thread-safe: feed lines from the client thread only.
 */
public final class TreeGiftChatParser {

    private static final char SECTION = '§';
    private static final char BAR = '▬';

    private static final Pattern CONTRIBUTION =
            Pattern.compile("You helped cut ([0-9.]+)% of the (.+?) Tree\\.");
    private static final Pattern REWARDS_GAINED =
            Pattern.compile("\\+([0-9,]+) rewards gained!");
    private static final Pattern BONUS_REWARD =
            Pattern.compile("^(.+?) \\(([0-9.]+)%\\)$");
    private static final Pattern PHANTOM =
            Pattern.compile("^An? (.+?) fell from the Tree!$");
    // Guaranteed / hover items: "Forest Essence x4", "Foraging Experience x1,000",
    // "Tender Wood x0-2".
    private static final Pattern GUARANTEED =
            Pattern.compile("^(.+?) x(?:[0-9]+-)?([0-9,]+)$");

    private boolean inGift = false;
    private boolean sawHeader = false;
    private String treeType = "";
    private final List<String> lines = new ArrayList<>();

    public void reset() {
        inGift = false;
        sawHeader = false;
        treeType = "";
        lines.clear();
    }

    public boolean inGift() { return inGift; }

    /**
     * Feed one raw (§-formatted) line. A line may itself contain newlines (some
     * clients deliver the whole block as one component); each is handled.
     * Returns the single headline drop when a Tree Gift block just closed.
     */
    public List<TreeGiftResult> feedLine(String raw) {
        if (raw == null) return List.of();
        if (raw.indexOf('\n') >= 0) {
            List<TreeGiftResult> all = new ArrayList<>();
            for (String part : raw.split("\n", -1)) all.addAll(feedOne(part));
            return all;
        }
        return feedOne(raw);
    }

    private List<TreeGiftResult> feedOne(String raw) {
        String stripped = strip(raw).trim();

        if (isBorder(stripped)) {
            if (inGift) {
                List<TreeGiftResult> out = sawHeader ? parseBlock() : List.of();
                reset();
                return out;
            }
            inGift = true; sawHeader = false; treeType = ""; lines.clear();
            return List.of();
        }

        // Also start a block on the header alone, in case a border is missed.
        if (!inGift && stripped.toUpperCase().contains("TREE GIFT")) {
            inGift = true; sawHeader = true; treeType = ""; lines.clear();
        }

        if (inGift) {
            if (stripped.toUpperCase().contains("TREE GIFT")) sawHeader = true;
            Matcher c = CONTRIBUTION.matcher(stripped);
            if (c.find()) treeType = c.group(2).trim();
            lines.add(raw);
            if (lines.size() > 96) reset(); // runaway guard
        }
        return List.of();
    }

    private List<TreeGiftResult> parseBlock() {
        List<TreeGiftResult> rare = new ArrayList<>();       // bonus + phantom (colour-coded)
        List<TreeGiftResult> guaranteed = new ArrayList<>(); // essence / xp / whispers / ...
        int rewardCount = -1;

        for (String raw : lines) {
            Decoded d = decode(raw);
            String plain = d.plain.trim();

            Matcher rg = REWARDS_GAINED.matcher(plain);
            if (rg.find()) rewardCount = parseInt(rg.group(1));

            Matcher ph = PHANTOM.matcher(plain);
            if (ph.matches()) {
                rare.add(reward(d, ph.group(1).trim(), -1, -1.0, true));
                continue;
            }
            Matcher b = BONUS_REWARD.matcher(plain);
            if (b.matches()) {
                String name = b.group(1).trim();
                if (!name.isEmpty() && !name.equalsIgnoreCase("BONUS GIFT")) {
                    rare.add(reward(d, name, -1, parseDouble(b.group(2)), false));
                }
                continue;
            }
            Matcher gu = GUARANTEED.matcher(plain);
            if (gu.matches()) {
                String name = gu.group(1).trim();
                if (!name.isEmpty()) guaranteed.add(reward(d, name, parseInt(gu.group(2)), -1.0, false));
            }
        }

        TreeGiftResult headline = pickHeadline(rare, guaranteed, rewardCount);
        return headline == null ? List.of() : List.of(headline);
    }

    /** Choose the single most exciting real drop to reveal. */
    private TreeGiftResult pickHeadline(List<TreeGiftResult> rare, List<TreeGiftResult> guaranteed, int rewardCount) {
        TreeGiftResult best = null;
        for (TreeGiftResult r : rare) {
            if (best == null
                    || r.rarity.ordinal() > best.rarity.ordinal()
                    || (r.rarity.ordinal() == best.rarity.ordinal() && rarer(r, best))) {
                best = r;
            }
        }
        if (best != null) return best;

        for (TreeGiftResult g : guaranteed) {
            if (best == null || g.amount > best.amount) best = g;
        }
        if (best != null) return best;

        if (rewardCount > 0) {
            return new TreeGiftResult("Tree Gift", Rarity.COMMON, rewardCount, -1.0, treeType, false,
                    ItemIcons.HARDENED_WOOD);
        }
        return null;
    }

    private static boolean rarer(TreeGiftResult a, TreeGiftResult b) {
        double da = a.dropChancePercent <= 0 ? Double.MAX_VALUE : a.dropChancePercent;
        double db = b.dropChancePercent <= 0 ? Double.MAX_VALUE : b.dropChancePercent;
        return da < db;
    }

    private TreeGiftResult reward(Decoded d, String name, int amount, double pct, boolean phantom) {
        return new TreeGiftResult(name, rarityOfName(d, name), amount, pct, treeType, phantom,
                ItemIcons.iconFor(name));
    }

    private Rarity rarityOfName(Decoded d, String name) {
        int idx = d.plain.indexOf(name);
        if (idx < 0 || idx >= d.colors.length) return Rarity.COMMON;
        return Rarity.fromColorCode(d.colors[idx]);
    }

    // ---- formatting helpers (no Minecraft) ----

    public static String strip(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == SECTION && i + 1 < raw.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    static Decoded decode(String raw) {
        StringBuilder plain = new StringBuilder(raw.length());
        StringBuilder cols = new StringBuilder(raw.length());
        char cur = 'f';
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == SECTION && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(++i));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) cur = code;
                else if (code == 'r') cur = 'f';
                continue;
            }
            plain.append(c);
            cols.append(cur);
        }
        return new Decoded(plain.toString(), cols.toString().toCharArray());
    }

    private static boolean isBorder(String stripped) {
        if (stripped.length() < 20) return false;
        for (int i = 0; i < stripped.length(); i++) if (stripped.charAt(i) != BAR) return false;
        return true;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1.0; }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.replace(",", "")); } catch (NumberFormatException e) { return -1; }
    }

    static final class Decoded {
        final String plain;
        final char[] colors;
        Decoded(String plain, char[] colors) { this.plain = plain; this.colors = colors; }
    }
}
