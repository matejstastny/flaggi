// ------------------------------------------------------------------------------
// MenuBackground.java - Menu background widget
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 03-25-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Image;
import java.io.IOException;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.ImageUtil;

/**
 * Main menu background
 */
public class MenuBackground extends Renderable {

	public Image background;

	public MenuBackground() {
		super(ZIndex.BACKGROUND, PanelRegion.BACKGROUND, UiTags.MAIN_MENU);
		try {
			this.background = ImageUtil.getImageFromResource("ui/menu_screen.png");
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "Failed to load background texture.");
		}
	}

	@Override
	public void render(VhGraphics g, Container focusCycleRootAncestor) {
		drawBackground(g.raw(), background, focusCycleRootAncestor);
	}
}
