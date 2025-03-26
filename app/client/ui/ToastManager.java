/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 1/9/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;

/**
 * A manager class for displaying toast messages on the screen. Toasts are small
 * pop-up messages that appear on the screen for a short period of time. The
 * ToastManager class is responsible for rendering the toasts and managing their
 * display duration.
 */
public class ToastManager extends Renderable {

    private static final int TOAST_WIDTH = 300;
    private static final int TOAST_HEIGHT = 50;
    private static final int TOAST_PADDING = 10;
    private static final int DISPLAY_DURATION = 5000; // 5 seconds
    private final List<Toast> toasts;

    // Constructor --------------------------------------------------------------

    public ToastManager() {
        super(ZIndex.TOAST);
        this.toasts = new ArrayList<>();
    }

    public void addToast(String message) {
        toasts.add(new Toast(message, System.currentTimeMillis()));
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
        int screenWidth = size[0];
        int yOffset = TOAST_PADDING;

        // Iterate over the toasts in reverse order to render the newest on top
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);

            // Check if the toast has expired
            if (System.currentTimeMillis() - toast.timestamp >= DISPLAY_DURATION) {
                toasts.remove(i);
                continue;
            }

            // Draw the toast background
            g.setColor(new Color(50, 50, 50, 220)); // Semi-transparent dark background
            g.fillRoundRect(screenWidth - TOAST_WIDTH - TOAST_PADDING, yOffset, TOAST_WIDTH, TOAST_HEIGHT, 15, 15);

            // Draw the toast border
            g.setColor(new Color(200, 200, 200));
            g.drawRoundRect(screenWidth - TOAST_WIDTH - TOAST_PADDING, yOffset, TOAST_WIDTH, TOAST_HEIGHT, 15, 15);

            // Draw the toast message
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString(toast.message, screenWidth - TOAST_WIDTH - TOAST_PADDING + 15, yOffset + 30);

            yOffset += TOAST_HEIGHT + TOAST_PADDING;
        }
    }

    // Toast item ---------------------------------------------------------------

    private static class Toast {

        private final String message;
        private final long timestamp;

        public Toast(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }

}
