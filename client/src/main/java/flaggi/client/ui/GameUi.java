// ------------------------------------------------------------------------------
// GameUi.java - Handles rendering and updating the game UI
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.ImageUtil;

public class GameUi extends Renderable {

	private double camCenterX = 0, camCenterY = 0;
	private List<ServerGameObject> objects;
	private ServerGameObject player;
	private int[] roomSize;
	private Image floor;

	// Constructor --------------------------------------------------------------

	public GameUi(int[] roomSize) {
		super(ZIndex.GAME, PanelRegion.FULLSCREEN, UiTags.GAME);
		this.roomSize = roomSize;
		try {
			this.floor = ImageUtil.createTiledImage(ImageUtil.getImageFromResource("sprites/floor-tile/floor-tile.png"), roomSize[0], roomSize[1]);
		} catch (IOException e) {
			Logger.log(LogLevel.WRN, "IOException while tiling floor image", e);
		}
	}

	public void update(ServerStateUpdate update) {
		this.camCenterX = update.getMe().getX();
		this.camCenterY = update.getMe().getY();
		this.objects = update.getOtherList();
		this.player = update.getMe();
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(VhGraphics g, Container focusCycleRootAncestor) {
		// UI ------------

		// World ---------
		AffineTransform original = g.raw().getTransform();
		g.raw().translate(px(-camCenterX + 50), px(-camCenterY + 50));

		renderFloor(g, focusCycleRootAncestor);
		if (this.objects != null) {
			this.objects.stream().forEach(o -> renderGameObject(o, g, focusCycleRootAncestor));
		}
		if (player != null) {
			renderGameObject(player, g, focusCycleRootAncestor);
		}

		g.raw().setTransform(original);
	}

	private void renderFloor(VhGraphics g, Container focusCycleRootAncestor) {
		g.setColor(Color.GRAY);
		g.drawImage(this.floor, 0, 0, roomSize[0], roomSize[1]);
		g.setColor(Color.RED);
	}

	private void renderGameObject(ServerGameObject o, VhGraphics g, Container focusCycleRootAncestor) {

		// Hitbox KEEP ON BOTTOM
		g.setColor(Color.RED);
		g.setStroke(1);
		g.drawRect(o.getX() + o.getCollX(), o.getY() + o.getCollY(), o.getCollWidth(), o.getCollWidth());
	}
}
