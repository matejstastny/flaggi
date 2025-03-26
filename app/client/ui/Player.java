/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flaggi.client.common.Sprite;
import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.util.FileUtil;
import flaggi.shared.util.FontUtil;

/**
 * Represents a player in the game, handling rendering, movement, and
 * animations.
 */
public class Player extends Renderable {

    private static final String PLAYER_ANIMATIONS_DIR = "sprites/player";
    private static final String DEFAULT_ENEMY_SKIN = "default_red";
    private static final String DEFAULT_SKIN = "default_blue";
    private static Map<String, List<String>> PLAYER_ANIMATIONS;

    private final Sprite avatar;
    private final Sprite flag;
    private final String name;
    private final int[] position;
    private final int id;

    private int health = 100;
    private boolean facingRight = true;
    private boolean hasFlag = false;
    private String frameData;

    // Constructor --------------------------------------------------------------

    public Player(int[] position, String name, String skinName, int id) {
        this(position, name, id, null);
        this.setZIndex(ZIndex.PLAYER);
        this.removeTag(UiTags.ENEMY_PLAYER);
        initializeAvatar(skinName);
    }

    public Player(int[] position, String name, int id, String frameData) {
        super(ZIndex.OTHER_PLAYERS, UiTags.GAME_ELEMENTS, UiTags.ENEMY_PLAYER);
        this.position = Arrays.copyOf(position, position.length);
        this.name = name;
        this.id = id;
        this.frameData = frameData;
        this.avatar = new Sprite();
        this.flag = new Sprite();

        setupSprites();
        determineZIndex();
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] offset, Container focusCycleRootAncestor) {
        if (hasFlag) {
            flag.render(g, position[0], position[1], focusCycleRootAncestor, facingRight);
        }

        if (isEnemy()) {
            renderEnemy(g, offset, focusCycleRootAncestor);
        } else {
            avatar.render(g, position[0], position[1], focusCycleRootAncestor, facingRight);
        }

        drawPlayerName(g, offset);
        drawHealthBar(g, offset);
        drawHitbox(g, offset);
    }

    private void renderEnemy(Graphics2D g, int[] offset, Container focusCycleRootAncestor) {
        String[] data = frameData.split(":");
        String animation = data[0];
        int frame = Integer.parseInt(data[1]);
        boolean flipped = Boolean.parseBoolean(data[2]);
        avatar.render(g, position[0] + offset[0], position[1] + offset[1], focusCycleRootAncestor, animation, frame, flipped);
    }

    private void drawPlayerName(Graphics2D g, int[] offset) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        int[] textPos = FontUtil.calculateCenteredPosition(55, 5, g.getFontMetrics(), name);
        g.drawString(name, offset[0] + position[0] + textPos[0], offset[1] + position[1] - 30);
    }

    private void drawHealthBar(Graphics2D g, int[] offset) {
        int width = 50;
        int height = 5;
        int x = offset[0] + position[0] - width / 2 + 27;
        int y = offset[1] + position[1] - 20;

        g.setColor(Color.GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(isEnemy() ? Color.RED : Color.BLUE);
        g.fillRect(x, y, (int) ((health / 100.0) * width), height);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
    }

    private void drawHitbox(Graphics2D g, int[] offset) {
        if (Constants.HITBOXES_ENABLED) {
            g.setColor(Color.RED);
            g.drawRect(position[0] + 7 + (isEnemy() ? offset[0] : 0), position[1] + 7 + (isEnemy() ? offset[1] : 0), 53, 93);
        }
    }

    // Setters ------------------------------------------------------------------

    public void changeAnimation(String action) {
        String animation = (isEnemy() ? DEFAULT_SKIN : DEFAULT_ENEMY_SKIN) + "_" + action;
        if (!avatar.getAnimationData().startsWith(animation)) {
            avatar.setAnimation(animation);
            avatar.setFps(action.equals("idle") ? 2 : 4);
        }
    }

    public void setFrameData(String frameData) {
        this.frameData = frameData;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public void setHasFlag(boolean hasFlag) {
        this.hasFlag = hasFlag;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(100, health));
    }

    // Accesors -----------------------------------------------------------------

    public boolean isEnemy() {
        return frameData != null;
    }

    // Private ------------------------------------------------------------------

    private static void loadPlayerAnimations() {
        PLAYER_ANIMATIONS = new HashMap<>();
        for (String skin : FileUtil.retrieveJarDirectoryList(PLAYER_ANIMATIONS_DIR)) {
            PLAYER_ANIMATIONS.put(skin + "_idle", Arrays.asList("idle_1", "idle_2"));
            PLAYER_ANIMATIONS.put(skin + "_walk_side", Arrays.asList("walk_side", "walk_side_l", "walk_side_r"));
            PLAYER_ANIMATIONS.put(skin + "_walk_up", Arrays.asList("walk_up", "walk_up_l", "walk_up_r"));
            PLAYER_ANIMATIONS.put(skin + "_walk_down", Arrays.asList("walk_down", "walk_down_l", "walk_down_r"));
        }
    }

    private void initializeAvatar(String skinName) {
        avatar.setAnimation(skinName + "_idle");
        avatar.setFps(2);
        avatar.play();
    }

    private void setupSprites() {
        flag.addAnimation("flag_blue", Arrays.asList("flag-blue"));
        flag.addAnimation("flag_red", Arrays.asList("flag-red"));
        flag.setAnimation("flag_red");

        if (PLAYER_ANIMATIONS == null) {
            loadPlayerAnimations();
        }
        addAvatarAnimations(avatar);
    }

    private void determineZIndex() {
        if (frameData != null) {
            addTag(UiTags.ENEMY_PLAYER);
        } else {
            setZIndex(ZIndex.PLAYER);
        }
    }

    private void addAvatarAnimations(Sprite avatar) {
        PLAYER_ANIMATIONS.keySet().forEach(name -> avatar.addAnimation(name, PLAYER_ANIMATIONS.get(name), PLAYER_ANIMATIONS_DIR, ".png"));
    }

}
