package flaggi.client.common;

import flaggi.client.App;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Sprite - loads and manages sprite animations from the resources folder.
 *
 * <p>FOLDER STRUCTURE:
 *
 * <p>Animated sprite (has named subfolders, one per animation): sprites/player-blue/ idle/ 1.png,
 * 2.png walk-up/ 1.png, 2.png, 3.png, 4.png walk-down/ ...
 *
 * <p>Static sprite (no subfolders, just PNGs directly in the folder): sprites/bullet/ bullet.png
 *
 * <p>USAGE: Sprite s = new Sprite("player-blue", 8); // 8 fps s.setAnimation("walk-up"); // in your
 * game loop: s.tick(); s.render(g, x, y, root);
 *
 * <p>Animation advances via tick(), which should be called once per game loop frame. No background
 * threads are used.
 */
public class Sprite {

    public static final String SPRITES_RESOURCE_PATH = "sprites/";

    /** The "animation name" used internally for static (single-frame) sprites. */
    private static final String DEFAULT_ANIMATION = "default";

    private final Map<String, Animation> animations = new HashMap<>();
    private String currentAnimation = DEFAULT_ANIMATION;

	// Tick-based animation state - no threads, no races.
	private int tickCounter = 0;
	private int currentFrameIndex = 0;
	private int ticksPerFrame;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param spriteName folder name inside sprites/, e.g. "player-blue" or "bullet"
     * @param fps animation speed; ignored for static sprites
     */
    public Sprite(String spriteName, int fps) {
        this.ticksPerFrame = Math.max(1, fps > 0 ? 60 / fps : 1);
        try {
            loadAnimations(spriteName);
        } catch (IOException e) {
            Logger.log(LogLevel.ERR, "Failed to load sprite: " + spriteName, e);
            App.handleFatalError();
        }
    }

		// List subfolders - these are animation names (e.g. "idle", "walk-up").
		List<String> subfolders = FileUtil.listResourceFiles(basePath, "");

		if (subfolders.isEmpty()) {
			// STATIC SPRITE: no subfolders - load PNGs directly from the folder.
			Animation anim = loadAnimation(basePath);
			animations.put(DEFAULT_ANIMATION, anim);
			currentAnimation = DEFAULT_ANIMATION;
		} else {
			// ANIMATED SPRITE: each subfolder is one named animation.
			for (String animName : subfolders) {
				Animation anim = loadAnimation(basePath + "/" + animName);
				animations.put(animName, anim);
			}
			// Start on the first animation found.
			currentAnimation = subfolders.get(0);
		}
	}

        // Check if the folder exists at all.
        URL folderUrl = getClass().getClassLoader().getResource(basePath);
        if (folderUrl == null) {
            throw new IOException("Sprite folder not found: " + basePath);
        }

        // List subfolders — these are animation names (e.g. "idle", "walk-up").
        List<String> subfolders = FileUtil.listResourceFiles(basePath, "");

        if (subfolders.isEmpty()) {
            // STATIC SPRITE: no subfolders — load PNGs directly from the folder.
            Animation anim = loadAnimation(basePath);
            animations.put(DEFAULT_ANIMATION, anim);
            currentAnimation = DEFAULT_ANIMATION;
        } else {
            // ANIMATED SPRITE: each subfolder is one named animation.
            for (String animName : subfolders) {
                Animation anim = loadAnimation(basePath + "/" + animName);
                animations.put(animName, anim);
            }
            // Start on the first animation found.
            currentAnimation = subfolders.get(0);
        }
    }

    /**
     * Loads all .png files from a folder as frames of one animation. Files are sorted naturally so
     * 1.png, 2.png, 10.png order correctly.
     */
    private Animation loadAnimation(String folderPath) throws IOException {
        List<Image> frames = new ArrayList<>();
        List<String> frameFiles = FileUtil.listResourceFiles(folderPath, ".png");
        frameFiles.sort(Comparator.naturalOrder());

        for (String file : frameFiles) {
            URL url = getClass().getClassLoader().getResource(folderPath + "/" + file);
            if (url != null) {
                frames.add(ImageIO.read(url));
            }
        }

        if (frames.isEmpty()) {
            throw new IOException("No PNG frames found in: " + folderPath);
        }

        return new Animation(frames);
    }

    // -------------------------------------------------------------------------
    // Game loop
    // -------------------------------------------------------------------------

    /**
     * Call this once per game loop tick to advance the animation. Does nothing for static
     * (single-frame) sprites.
     */
    public void tick() {
        Animation anim = animations.get(currentAnimation);
        if (anim == null || anim.frames.size() <= 1) return;

        tickCounter++;
        if (tickCounter >= ticksPerFrame) {
            tickCounter = 0;
            currentFrameIndex = (currentFrameIndex + 1) % anim.frames.size();
        }
    }

    // -------------------------------------------------------------------------
    // Controls
    // -------------------------------------------------------------------------

    /**
     * Switches to a named animation and resets the frame counter. Only switches if the animation is
     * actually different (avoids restarting mid-cycle).
     *
     * @param name animation folder name, e.g. "walk-up", "idle"
     * @throws IllegalArgumentException if the animation doesn't exist on this sprite
     */
    public void setAnimation(String name) {
        if (!animations.containsKey(name)) {
            Logger.log(
                    LogLevel.ERR,
                    "Failed to load sprite",
                    new IllegalArgumentException(
                            "Animation '" + name + "' not found on sprite. Available: " + animations.keySet()));
        }
        if (!name.equals(currentAnimation)) {
            currentAnimation = name;
            currentFrameIndex = 0;
            tickCounter = 0;
        }
    }

    /**
     * Changes the animation speed.
     *
     * @param fps frames per second (assumes 60hz game loop)
     */
    public void setFps(int fps) {
        this.ticksPerFrame = Math.max(1, fps > 0 ? 60 / fps : 1);
        tickCounter = 0;
    }

    /**
     * Jumps to a specific frame and freezes animation there. Call setFps() or let tick() resume if
     * you want it playing again.
     *
     * @param frameIndex zero-based frame index
     */
    public void freezeAtFrame(int frameIndex) {
        Animation anim = animations.get(currentAnimation);
        if (anim == null || frameIndex < 0 || frameIndex >= anim.frames.size()) {
            throw new IllegalArgumentException("Frame index " + frameIndex + " out of range");
        }
        currentFrameIndex = frameIndex;
        ticksPerFrame = Integer.MAX_VALUE; // effectively stops ticking
    }

    /** Returns true if this sprite has an animation by the given name. */
    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    /** Returns the name of the currently active animation. */
    public String getCurrentAnimation() {
        return currentAnimation;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /** Draws the current frame centered on (x, y). */
    public void render(Graphics2D g, int x, int y, Container root) {
        Image img = getCurrentFrame();
        if (img == null) return;
        int w = img.getWidth(root);
        int h = img.getHeight(root);
        g.drawImage(img, x - w / 2, y - h / 2, root);
    }

    /** Draws the current frame with the top-left corner at (x, y). */
    public void renderTopLeft(Graphics2D g, int x, int y, Container root) {
        Image img = getCurrentFrame();
        if (img != null) {
            g.drawImage(img, x, y, root);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Image getCurrentFrame() {
        Animation anim = animations.get(currentAnimation);
        if (anim == null || anim.frames.isEmpty()) return null;
        return anim.frames.get(currentFrameIndex);
    }

    // -------------------------------------------------------------------------
    // Animation (internal data class)
    // -------------------------------------------------------------------------

    private static class Animation {
        final List<Image> frames;

        Animation(List<Image> frames) {
            this.frames = frames;
        }
    }
}
