// ------------------------------------------------------------------------------
// ToastManager.java - description TODO
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 01-09-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;

/**
 * A manager class for displaying toast messages on the screen. Toasts are small
 * pop-up messages that appear on the screen for a short period of time. The
 * ToastManager class is responsible for rendering the toasts and managing their
 * display duration.
 */
public class ToastManager extends Renderable {

	private final static int MAX_TOASTS = 6;
	private final List<Toast> toasts;

	// Constructor --------------------------------------------------------------

	public ToastManager() {
		super(ZIndex.TOAST, PanelRegion.TOP_RIGHT, UiTags.TOASTS, UiTags.ALWAYS_VISIBLE);
		this.toasts = new ArrayList<>();
	}

	public void newToast(ToastCategory category, String message) {
		toasts.add(new Toast(category, message, System.currentTimeMillis()));
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, Container focusCycleRootAncestor) {
		for (int i = toasts.size() - 1; i >= 0 && i >= toasts.size() - MAX_TOASTS; i--) {
			Toast toast = toasts.get(i);
			if (System.currentTimeMillis() - toast.timestamp >= Constants.TOAST_DISPLAY_DURATION_SEC * 1000) {
				toasts.remove(i);
				continue;
			}

			int width = px(94);
			int height = px(14.8);
			int arc = px(3);
			int x = px(5);
			int y = px(2) * (toasts.size() - i) + height * (toasts.size() - i - 1);

			// Background
			g.setColor(toast.category.getColor());
			g.fillRoundRect(x, y, width, height, arc, arc);

			// Border
			g.setColor(new Color(0, 0, 0, 220));
			g.setStroke(new BasicStroke(px(1)));
			g.drawRoundRect(x, y, width, height, arc, arc);

			// Text
			g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(4)));
			g.setColor(Color.WHITE);
			g.drawString(toast.message, x + px(3), y + px(8.7));
		}
	}

	// Toast item ---------------------------------------------------------------

	private static class Toast {

		private final String message;
		private final long timestamp;
		private final ToastCategory category;

		public Toast(ToastCategory category, String message, long timestamp) {
			this.category = category;
			this.message = message;
			this.timestamp = timestamp;
		}
	}

	// Toast categories ---------------------------------------------------------

	public enum ToastCategory {
		INFO(Color.GRAY), WARNING(Color.ORANGE), ERROR(Color.RED), SUCCESS(new Color(0, 153, 0));

		private final Color color;

		ToastCategory(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}
	}
}
