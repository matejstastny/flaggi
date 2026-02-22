package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.FontUtil;

public class DebugGame extends Renderable {

	private final String text = "IN GAME";

	public DebugGame() {
		super(900, PanelRegion.CENTER, UiTags.GAME);
	}

	@Override
	public void render(VhGraphics g, Container focusCycleRootAncestor) {
		g.setColor(Color.BLACK);
		g.setFont(Constants.FONT, 5);
		int[] pos = FontUtil.getCenteredPosition(px(100), px(100), g.raw().getFontMetrics(), text);
		g.drawString(text, pos[0], pos[1]);
	}
}
