/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 6/21/2025
 * GitHub link: https://github.com/my-daarlin/flaggi
 */

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import java.util.List;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.proto.ServerMessages.RenderableObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ImageUtil;

public class GameUi extends Renderable {

	private int[] roomSize;
	private List<RenderableObject> objects;
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
		this.objects = update.getRenderablesList() == null ? this.objects : update.getRenderablesList();
		this.cameraX = update.getX();
		this.cameraY = update.getY();
	}

	public double getViewHeight(int px) {
		return vh(px);
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, Container focusCycleRootAncestor) {
		// renderFloor(g, focusCycleRootAncestor);
		// objects.forEach(object -> renderGameObject(object, g,
		// focusCycleRootAncestor));
	}

	private void renderGameObject(RenderableObject object, Graphics2D g, Container focusCycleRootAncestor) {
		if (object == null) {
			return;
		}
		Image sprite = getSprite(object.getSpriteName(), object.getAnimationName(), object.getFrame());
		if (sprite != null) {
			int xPos = object.getX() - cameraX;
			int yPos = object.getY() - cameraY;
			g.drawImage(sprite, xPos, yPos, focusCycleRootAncestor);
		}
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
