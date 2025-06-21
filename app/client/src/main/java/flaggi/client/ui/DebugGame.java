package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.util.FontUtil;

// TODO Debbugging class to test entering a game from menu
public class DebugGame extends Renderable {

	private final String text = "IN GAME";

	public DebugGame() {
		super(900, PanelRegion.CENTER, UiTags.GAME);
	}

	@Override
	public void render(Graphics2D g, int[] viewportOffset, Container focusCycleRootAncestor) {
		g.setColor(Color.BLACK);
		g.setFont(Constants.FONT.deriveFont(Font.BOLD, px(5)));
		int[] pos = FontUtil.getCenteredPosition(px(100), px(100), g.getFontMetrics(), text);
		g.drawString(text, pos[0], pos[1]);
	}
}
