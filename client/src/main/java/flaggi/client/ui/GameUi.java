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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flaggi.client.common.Sprite;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.proto.ServerMessages.PlayerAnimation;
import flaggi.proto.ServerMessages.PlayerSkin;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.ImageUtil;

public class GameUi extends Renderable {

	private static final double ZOOM = 0.5;
	private static final int SPRITE_FPS = 8;

	private double camCenterX = 0, camCenterY = 0;
	private List<ServerGameObject> objects;
	private ServerGameObject player;
	private int[] roomSize;
	private Image floor;

	private final Map<String, Sprite> playerSprites = new HashMap<>();
	private Sprite treeSprite;
	private Sprite bulletSprite;
	private Sprite flagBlueSprite;
	private Sprite flagRedSprite;

	// Constructor --------------------------------------------------------------

	public GameUi(int[] roomSize) {
		super(ZIndex.GAME, PanelRegion.FULLSCREEN, UiTags.GAME);
		this.roomSize = roomSize;
		try {
			this.floor = ImageUtil.createTiledImage(ImageUtil.getImageFromResource("sprites/floor-tile/floor-tile.png"), roomSize[0], roomSize[1]);
		} catch (IOException e) {
			Logger.log(LogLevel.WRN, "IOException while tiling floor image", e);
		}
		try {
			treeSprite = new Sprite("tree", 0);
			bulletSprite = new Sprite("bullet", 0);
			flagBlueSprite = new Sprite("flag-blue", 0);
			flagRedSprite = new Sprite("flag-red", 0);
		} catch (Exception e) {
			Logger.log(LogLevel.WRN, "Failed to load static sprites", e);
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
		// World ---------
		AffineTransform original = g.raw().getTransform();

		int cx = px(50), cy = px(50);
		g.raw().translate(cx, cy);
		g.raw().scale(ZOOM, ZOOM);
		g.raw().translate(-cx, -cy);
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

	private void renderGameObject(ServerGameObject o, VhGraphics g, Container root) {
		switch (o.getType()) {
		case PLAYER -> renderPlayer(o, g, root);
		case TREE -> {
			if (treeSprite != null)
				treeSprite.render(g.raw(), px(o.getX()), px(o.getY()), root);
		}
		case BULLET -> {
			if (bulletSprite != null)
				bulletSprite.render(g.raw(), px(o.getX()), px(o.getY()), root);
		}
		case FLAG -> {
			Sprite s = (o.getSkin() == PlayerSkin.SKIN_RED) ? flagRedSprite : flagBlueSprite;
			if (s != null)
				s.render(g.raw(), px(o.getX()), px(o.getY()), root);
		}
		default -> {
		}
		}
	}

	private void renderPlayer(ServerGameObject o, VhGraphics g, Container root) {
		Sprite sprite = playerSprites.computeIfAbsent(o.getUsername(), k -> new Sprite(skinToFolder(o.getSkin()), SPRITE_FPS));
		sprite.setAnimation(animToFolder(o.getAnimation()));
		sprite.tick();

		Graphics2D g2 = g.raw();
		AffineTransform saved = g2.getTransform();
		g2.translate(px(o.getX()), px(o.getY()));
		if (o.getFacingLeft())
			g2.scale(-1, 1);
		sprite.render(g2, 0, 0, root);
		g2.setTransform(saved);
	}

	private static String skinToFolder(PlayerSkin s) {
		return switch (s) {
		case SKIN_RED -> "player-red";
		case SKIN_JESTER -> "player-jester";
		case SKIN_VENOM -> "player-venom";
		default -> "player-blue";
		};
	}

	private static String animToFolder(PlayerAnimation a) {
		return switch (a) {
		case ANIM_WALK_UP -> "walk-up";
		case ANIM_WALK_DOWN -> "walk-down";
		case ANIM_WALK_SIDE -> "walk-side";
		case ANIM_WALK_DIAGONAL -> "walk-diagonal";
		default -> "idle";
		};
	}
}
