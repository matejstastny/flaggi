// ------------------------------------------------------------------------------
// Sprite.java - Sprite data structure class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 12-01-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.common;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import flaggi.client.App;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;

public class Sprite {

	public static final String SPRITES_RESOURCE_PATH = "sprites/";

	private final Map<String, Animation> animations = new HashMap<>();
	private String currentAnimation;
	private int currentFrameIndex;
	private ScheduledExecutorService frameScheduler;

	// Constructor --------------------------------------------------------------

	public Sprite(String spriteName) {
		try {
			loadAnimations(spriteName);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to load sprite: " + spriteName, e);
			App.handleFatalError();
		}
	}

	// Loading ------------------------------------------------------------------

	/**
	 * Tries to load sprite files into animations. Takes the spriteName parameter,
	 * and looks for a folder with that name in the resource directory thats defined
	 * by SPRITS_RESOURCE_PATH. If it finds it it looks for directories inside of
	 * it, that will be the animation directories that store all the textures. If it
	 * doesn't find any, it just assumes the sprite directory itself is an animation
	 * (used for no-animation or one-animation sprites).
	 *
	 * @param spriteName - name of the sprite texture directory in
	 *                   SPRITS_RESOURCE_PATH
	 * @throws IOException If the sprite directory doesn't exist
	 */
	private void loadAnimations(String spriteName) throws IOException {
		ClassLoader cl = getClass().getClassLoader();
		URL spriteFolderUrl = cl.getResource(SPRITES_RESOURCE_PATH + spriteName);

		if (spriteFolderUrl == null) {
			throw new IOException("Sprite folder not found: " + spriteName);
		}

		List<String> animationNames = FileUtil.listResourceFiles(SPRITES_RESOURCE_PATH + spriteName, "");
		if (animationNames.isEmpty()) {
			animations.put("default", loadAnimation(SPRITES_RESOURCE_PATH + spriteName, 1));
		} else {
			for (String animName : animationNames) {
				animations.put(animName, loadAnimation(SPRITES_RESOURCE_PATH + spriteName + "/" + animName, 1));
			}
		}

		currentAnimation = animations.keySet().iterator().next();
	}

	private Animation loadAnimation(String folderPath, int fps) throws IOException {
		List<Image> frames = new ArrayList<>();
		List<String> frameFiles = FileUtil.listResourceFiles(folderPath, ".png");

		frameFiles.sort(Comparator.naturalOrder());

		for (String file : frameFiles) {
			URL url = getClass().getClassLoader().getResource(folderPath + "/" + file);
			if (url != null) {
				frames.add(ImageIO.read(url));
			}
		}

		return new Animation(frames, fps);
	}

	// Controllers --------------------------------------------------------------

	public void setAnimation(String name) {
		if (!animations.containsKey(name)) {
			throw new IllegalArgumentException("Animation not found: " + name);
		}
		this.currentAnimation = name;
		this.currentFrameIndex = 0;
		restartFrameScheduler();
	}

	/**
	 * Sets the animation frame index to a specific number, and stops the animation.
	 * If passed index -1, the animation will continue playing.
	 *
	 * @param frameIndex
	 * @throws IllegalArgumentException if target frame index is out of range.
	 */
	public void setFrameNumber(int frameIndex) throws IllegalArgumentException {
		if (currentAnimation.length() < frameIndex && frameIndex >= 0) {
			stop();
			this.currentFrameIndex = frameIndex;
		} else if (frameIndex == -1) {
			play();
		} else {
			throw new IllegalArgumentException("Frame index " + frameIndex + "invalid for lenght " + currentAnimation.length());
		}
	}

	public void play() {
		restartFrameScheduler();
	}

	public void stop() {
		if (frameScheduler != null) {
			frameScheduler.shutdownNow();
		}
	}

	public void setFpsForCurrentAnimation(int fps) {
		animations.get(currentAnimation).setFps(fps);
		restartFrameScheduler();
	}

	// Rendering ----------------------------------------------------------------

	public void render(Graphics2D g, int x, int y, Container root) {
		Image img = getCurrentFrame();
		if (img != null) {
			g.drawImage(img, x - img.getWidth(root), y - img.getHeight(root), root);
		}
	}

	// Private ------------------------------------------------------------------

	private Image getCurrentFrame() {
		Animation anim = animations.get(currentAnimation);
		if (anim == null || anim.frames.isEmpty())
			return null;
		return anim.frames.get(currentFrameIndex);
	}

	private void restartFrameScheduler() {
		stop();
		Animation anim = animations.get(currentAnimation);
		if (anim == null || anim.frames.size() <= 1)
			return;

		frameScheduler = Executors.newSingleThreadScheduledExecutor();
		frameScheduler.scheduleAtFixedRate(() -> {
			currentFrameIndex = (currentFrameIndex + 1) % anim.frames.size();
		}, 0, 1000 / anim.fps, TimeUnit.MILLISECONDS);
	}

	// Animation struct ---------------------------------------------------------

	private static class Animation {
		private final List<Image> frames;
		private int fps;

		Animation(List<Image> frames, int fps) {
			this.frames = frames;
			this.fps = fps;
		}

		void setFps(int fps) {
			if (fps <= 0)
				throw new IllegalArgumentException("FPS must be > 0");
			this.fps = fps;
		}
	}
}
