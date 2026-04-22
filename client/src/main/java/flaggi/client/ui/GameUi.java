package flaggi.client.ui;

import flaggi.client.common.Sprite;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.proto.ClientMessages.ClientKey;
import flaggi.proto.ServerMessages.PlayerAnimation;
import flaggi.proto.ServerMessages.PlayerSkin;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.PlayerGameObject;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.ImageUtil;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameUi extends Renderable {

    private static final double ZOOM = 0.5;
    private static final int SPRITE_FPS = 8;

    // Mirrors server Hitboxes
    private static final double PLAYER_HB_OX = -2.5, PLAYER_HB_OY = -2.5, PLAYER_HB_W = 5, PLAYER_HB_H = 5;
    private static final double TREE_HB_OX = -5, TREE_HB_OY = -5, TREE_HB_W = 10, TREE_HB_H = 10;

    private final Object posLock = new Object();
    private double clientX = 0, clientY = 0;
    private long lastRenderNs = System.nanoTime();
    private volatile Set<ClientKey> heldKeys = EnumSet.noneOf(ClientKey.class);
    private static final double SNAP_THRESHOLD_SQ = 60.0 * 60.0;

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
            this.floor = ImageUtil.createTiledImage(
                    ImageUtil.getImageFromResource("sprites/floor-tile/floor-tile.png"), roomSize[0], roomSize[1]);
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

    public void update(ServerStateUpdate update, Set<ClientKey> heldKeys) {
        double sx = update.getMe().getX();
        double sy = update.getMe().getY();
        synchronized (posLock) {
            double errX = sx - clientX;
            double errY = sy - clientY;
            if (errX * errX + errY * errY > SNAP_THRESHOLD_SQ) {
                clientX = sx;
                clientY = sy;
            }
        }
        this.heldKeys = heldKeys;
        this.objects = update.getOtherList();
        this.player = update.getMe();
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(VhGraphics g, Container focusCycleRootAncestor) {
        AffineTransform original = g.raw().getTransform();

        advanceClientPosition();
        double predX, predY;
        synchronized (posLock) {
            predX = clientX;
            predY = clientY;
        }

        int cx = px(50), cy = px(50);
        g.raw().translate(cx, cy);
        g.raw().scale(ZOOM, ZOOM);
        g.raw().translate(-cx, -cy);
        g.raw().translate(px(-predX + 50), px(-predY + 50));

        renderFloor(g, focusCycleRootAncestor);
        if (this.objects != null) {
            this.objects.forEach(o -> renderGameObject(o, g, focusCycleRootAncestor));
        }
        if (player != null) {
            renderLocalPlayer(predX, predY, g, focusCycleRootAncestor);
        }

        g.raw().setTransform(original);
    }

    // Advance clientX/Y by held-key velocity × elapsed render-frame time,
    // with AABB collision against trees and room boundary clamping
    // Mirrors server tryMove logic: diagonal → H-only → V-only
    // Called only from the EDT, so lastRenderNs needs no synchronization.
    private void advanceClientPosition() {
        long now = System.nanoTime();
        double dtTicks = (now - lastRenderNs) / 1_000_000.0 / 16.0;
        lastRenderNs = now;

        Set<ClientKey> keys = this.heldKeys;
        double dx = 0, dy = 0;
        if (keys.contains(ClientKey.KEY_UP)) dy -= PlayerGameObject.PLAYER_SPEED;
        if (keys.contains(ClientKey.KEY_DOWN)) dy += PlayerGameObject.PLAYER_SPEED;
        if (keys.contains(ClientKey.KEY_LEFT)) dx -= PlayerGameObject.PLAYER_SPEED;
        if (keys.contains(ClientKey.KEY_RIGHT)) dx += PlayerGameObject.PLAYER_SPEED;
        if (dx != 0 && dy != 0) {
            double d = PlayerGameObject.PLAYER_SPEED / Math.sqrt(2);
            dx = Math.signum(dx) * d;
            dy = Math.signum(dy) * d;
        }

        if (dx == 0 && dy == 0) return;

        double stepDx = dx * dtTicks;
        double stepDy = dy * dtTicks;

        synchronized (posLock) {
            // Mirror server tryMove: diagonal → H-only → V-only
            if (!clientCollidesWithObstacle(clientX + stepDx, clientY + stepDy)) {
                clientX += stepDx;
                clientY += stepDy;
            } else if (!clientCollidesWithObstacle(clientX + stepDx, clientY)) {
                clientX += stepDx;
            } else if (!clientCollidesWithObstacle(clientX, clientY + stepDy)) {
                clientY += stepDy;
            }
            // else: fully blocked - no movement

            // Mirror server boundary clamp
            clientX = Math.max(0, Math.min(roomSize[0], clientX));
            clientY = Math.max(0, Math.min(roomSize[1], clientY));
        }
    }

    // AABB check - mirrors server collidesWithObstacle(). Reads this.objects
    // snapshot.
    private boolean clientCollidesWithObstacle(double cx, double cy) {
        double px1 = cx + PLAYER_HB_OX;
        double py1 = cy + PLAYER_HB_OY;
        double px2 = px1 + PLAYER_HB_W;
        double py2 = py1 + PLAYER_HB_H;

        List<ServerGameObject> objs = this.objects;
        if (objs == null) return false;
        for (ServerGameObject o : objs) {
            if (o.getType() != flaggi.proto.ServerMessages.GameObjectType.TREE) continue;
            double tx1 = o.getX() + TREE_HB_OX;
            double ty1 = o.getY() + TREE_HB_OY;
            double tx2 = tx1 + TREE_HB_W;
            double ty2 = ty1 + TREE_HB_H;
            if (px1 < tx2 && px2 > tx1 && py1 < ty2 && py2 > ty1) return true;
        }
        return false;
    }

    private void renderLocalPlayer(double x, double y, VhGraphics g, Container root) {
        Sprite sprite = playerSprites.computeIfAbsent(
                player.getUsername(), k -> new Sprite(skinToFolder(player.getSkin()), SPRITE_FPS));
        sprite.setAnimation(animToFolder(player.getAnimation()));
        sprite.tick();

        Graphics2D g2 = g.raw();
        AffineTransform saved = g2.getTransform();
        g2.translate(px(x), px(y));
        if (player.getFacingLeft()) g2.scale(-1, 1);
        sprite.render(g2, 0, 0, root);
        g2.setTransform(saved);
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
                if (treeSprite != null) treeSprite.render(g.raw(), px(o.getX()), px(o.getY()), root);
            }
            case BULLET -> {
                if (bulletSprite != null) bulletSprite.render(g.raw(), px(o.getX()), px(o.getY()), root);
            }
            case FLAG -> {
                Sprite s = (o.getSkin() == PlayerSkin.SKIN_RED) ? flagRedSprite : flagBlueSprite;
                if (s != null) s.render(g.raw(), px(o.getX()), px(o.getY()), root);
            }
            default -> {}
        }
    }

    private void renderPlayer(ServerGameObject o, VhGraphics g, Container root) {
        Sprite sprite =
                playerSprites.computeIfAbsent(o.getUsername(), k -> new Sprite(skinToFolder(o.getSkin()), SPRITE_FPS));
        sprite.setAnimation(animToFolder(o.getAnimation()));
        sprite.tick();

        Graphics2D g2 = g.raw();
        AffineTransform saved = g2.getTransform();
        g2.translate(px(o.getX()), px(o.getY()));
        if (o.getFacingLeft()) g2.scale(-1, 1);
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
