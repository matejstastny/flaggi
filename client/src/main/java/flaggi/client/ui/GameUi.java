// ------------------------------------------------------------------------------
// GameUi.java - Handles rendering and updating the game UI
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import java.util.List;

import flaggi.client.constants.Constants;
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

	private int[] roomSize;
	private List<ServerGameObject> objects;
	private int cameraX, cameraY;

	// Constructor --------------------------------------------------------------

	public GameUi(int[] roomSize) {
		super(ZIndex.GAME, PanelRegion.FULLSCREEN, UiTags.GAME);
		this.roomSize = roomSize;
	}

	// Public -------------------------------------------------------------------

	public void updateGameUi(ServerStateUpdate update) {
		if (update == null) {
			return;
		}
		// TODO Upate UT
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(VhGraphics g, Container focusCycleRootAncestor) {
		// renderFloor(g, focusCycleRootAncestor);
		// objects.forEach(object -> renderServerGameObject(object, g,
		// focusCycleRootAncestor));
	}

	private void renderServerGameObject(ServerGameObject object, Graphics2D g, Container focusCycleRootAncestor) {
		// TODO
		// if (object == null) {
		// return;
		// }
		// Image sprite = getSprite(object.getSpriteName(), object.getAnimationName(),
		// object.getFrame());
		// if (sprite != null) {
		// int xPos = object.getX() - cameraX;
		// int yPos = object.getY() - cameraY;
		// g.drawImage(sprite, xPos, yPos, focusCycleRootAncestor);
		// }
	}

	private void renderFloor(Graphics2D g, Container focusCycleRootAncestor) {
		Image floorSprite;
		try {
			floorSprite = ImageUtil.createRepeatedImage(getSprite("floor", "default", 0), roomSize[0], roomSize[1]);
		} catch (IOException e) {
			floorSprite = null;
			Logger.log(LogLevel.ERROR, "Failed to create repeated floor sprite: " + e.getMessage());
		}
		if (floorSprite != null) {
			g.drawImage(floorSprite, -cameraX, -cameraY, focusCycleRootAncestor);
		}
	}

	// Helpers ------------------------------------------------------------------

	private Image getSprite(String spriteName, String animationName, int frameIndex) {
		String path = Constants.SPRITES_RES_DIR + "/" + spriteName + "/" + animationName + "/" + frameIndex + ".png";
		Image sprite = null;
		try {
			sprite = ImageUtil.getImageFromResource(path);
		} catch (IOException e) {
			sprite = null;
		}
		if (sprite == null) {
			Logger.log(LogLevel.ERROR, "Failed to load sprite: " + path);
		}
		return sprite;
	}
}
