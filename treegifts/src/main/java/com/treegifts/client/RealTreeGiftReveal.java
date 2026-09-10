package com.treegifts.client;

import com.treegifts.core.TreeGiftResult;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns the queue of REAL Tree Gift drops waiting to be revealed, and shows them
 * one at a time.
 *
 * The results here always come from {@link com.treegifts.core.TreeGiftChatParser}
 * (i.e. from Hypixel's chat) — this class never creates or randomises a result.
 * If several notable drops arrive at once, they're queued and revealed in order
 * rather than clobbering each other.
 */
public final class RealTreeGiftReveal {

    public static final RealTreeGiftReveal INSTANCE = new RealTreeGiftReveal();

    private final Deque<TreeGiftResult> queue = new ArrayDeque<>();
    private boolean showing = false;
    /** The most recent real drop, so the keybind can replay it (never random). */
    private TreeGiftResult last;

    private RealTreeGiftReveal() {}

    /** Queue a real drop for revealing (called on the client thread). */
    public void enqueue(TreeGiftResult result) {
        if (result == null) return;
        last = result;
        queue.addLast(result);
        showNextIfIdle();
    }

    /** Replay the last real drop, if any (used by the keybind — no randomness). */
    public boolean replayLast() {
        if (last == null) return false;
        queue.addLast(last);
        showNextIfIdle();
        return true;
    }

    /** Called by the reveal screen when it closes, to advance the queue. */
    void onRevealClosed() {
        showing = false;
        showNextIfIdle();
    }

    private void showNextIfIdle() {
        if (showing) return;
        TreeGiftResult next = queue.pollFirst();
        if (next == null) return;
        showing = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            // If the player already has something else open, requeue and wait.
            if (mc.screen != null && !(mc.screen instanceof TreeGiftScreen)) {
                showing = false;
                queue.addFirst(next);
                return;
            }
            mc.setScreen(new TreeGiftScreen(next));
        });
    }

    public int queued() {
        return queue.size();
    }
}
