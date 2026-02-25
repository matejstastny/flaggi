package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;

public class DebugOverlay extends Renderable {

	List<String> data = new ArrayList<>();

	public DebugOverlay() {
		super(100000, PanelRegion.TOP_LEFT, UiTags.GAME);
		this.setVisibility(true);
	}

	public void update(ServerStateUpdate s) {
		this.setVisibility(true);
		data.clear();
		try {
			ServerGameObject g = s.getMe();
			data.add("Username: " + g.getUsername());
			data.add("X position: " + g.getX());
			data.add("Y position: " + g.getY());
			data.add("Y position: " + g.getHp());
		} catch (Exception e) {
			data.add("N/A");
		}
	}

	@Override
	public void render(VhGraphics g, Container focusCycleRootAncestor) {
		g.setColor(Color.BLACK);
		g.setFont(Constants.FONT, 5);
		int height = g.raw().getFontMetrics().getHeight() + px(0.5);
		for (int i = 0; i < data.size(); i++) {
			g.drawString(data.get(i), 2, height * (i + 1));
		}
	}
}
