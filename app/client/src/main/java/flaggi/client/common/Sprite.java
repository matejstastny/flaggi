/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 12/1/2024
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.common;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ImageUtil;

public class Sprite {

	public static final String TEXTURE_RESOURCE_DIRECTORY = "sprites/";
	public static final String DEFAULT_TEXTURE_EXTENSION = ".png";

	private final ScheduledExecutorService frameScheduler;
	private final Map<String, List<Image>> animations;
	private String selectedAnimationName;
	private int frameIndex;
	private int fps;

	// Constructor --------------------------------------------------------------

	public Sprite() {
		this.frameScheduler = Executors.newSingleThreadScheduledExecutor();
		this.animations = new HashMap<>();
	}

	// New animations -----------------------------------------------------------

	/**
	 * Adds a new animation to the sprite.
	 *
	 * @param name         - name of the animation. Will be used to select it later.
	 * @param frameNames   - names of the frames. Will be prefixed by
	 *                     {@link #TEXTURE_RESOURCE_DIRECTORY} and suffixed by
	 *                     {@link #DEFAULT_TEXTURE_EXTENSION}.
	 * @param resourcePath - path to the sprite textures directory in the jar file.
	 *                     Pass in {@code ""} if they are located in the root. If
	 *                     you pass a path that doesn't end with "/" it will be
	 *                     appended.
	 * @param fileType     - file extension for all of the frames. (".png", ".jpeg"
	 *                     ...). If the extension doesn't start with a dot, it will
	 *                     be appended.
	 */
	public void addAnimation(String name, List<String> frameNames) {
		addAnimation(name, frameNames, null, null);
	}

	/**
	 * Adds a new animation to the sprite.
	 *
	 * @param name          - name of the animation. Will be used to select it
	 *                      later.
	 * @param frameNames    - names of the frames. Will be prefixed by
	 * @param directoryPath
	 * @param fileExtension
	 */
	public void addAnimation(String name, List<String> frameNames, String directoryPath, String fileExtension) {
		try {
			animations.put(name, loadFrames(frameNames, directoryPath, fileExtension));
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "Failed to load animation: '" + name + "'. Resource does not exist.");
		}
	}

	// Setters ------------------------------------------------------------------

	public void play() {
		if (this.selectedAnimationName == null)
			throw new IllegalStateException("No animation set.");
		restartFrameUpdater();
	}

	public void stop() {
		this.frameScheduler.shutdownNow();
	}

	public void setAnimation(String name) {
		if (!this.animations.containsKey(name)) {
			throw new IllegalArgumentException("Animation not found: '" + name + "'. Available: " + getAnimationNames());
		}
		this.selectedAnimationName = name;
		this.frameIndex = 0;
	}

	public void setFps(int fps) {
		if (fps <= 0)
			throw new IllegalArgumentException("FPS must be greater than 0");
		this.fps = fps;
		restartFrameUpdater();
	}

	// Rendering ----------------------------------------------------------------

	public void render(Graphics2D g, int x, int y, Container root) {
		render(g, x, y, root, false);
	}

	public void render(Graphics2D g, int x, int y, Container root, boolean invert) {
		render(g, x, y, root, this.selectedAnimationName, this.frameIndex, invert);
	}

	public void render(Graphics2D g, int x, int y, Container root, String animationName, int frameIndex, boolean invert) {
		if (animationName == null || !animations.containsKey(animationName))
			return;
		List<Image> frames = animations.get(animationName);
		if (frames.isEmpty() || frameIndex >= frames.size())
			return;

		Image img = invert ? ImageUtil.flipImageVertically(frames.get(frameIndex)) : frames.get(frameIndex);
		g.drawImage(img, x, y, root);
	}

	// Accesors -----------------------------------------------------------------

	public int getWidth() {
		return getCurrentFrameImage() != null ? getCurrentFrameImage().getWidth(null) : 0;
	}

	public int getHeight() {
		return getCurrentFrameImage() != null ? getCurrentFrameImage().getHeight(null) : 0;
	}

	public String getAnimationData() {
		return this.selectedAnimationName + ":" + this.frameIndex;
	}

	// Private ------------------------------------------------------------------

	private List<Image> loadFrames(List<String> frameNames, String directory, String fileType) throws IOException {
		if (frameNames == null) {
			throw new IllegalArgumentException("frameNames cannot be null");
		}

		List<Image> frames = new ArrayList<>();
		String basePath = (directory == null || directory.isEmpty()) ? TEXTURE_RESOURCE_DIRECTORY : directory;
		basePath = basePath.endsWith("/") ? basePath : basePath.concat("/");
		String ext = (fileType == null || fileType.isEmpty()) ? DEFAULT_TEXTURE_EXTENSION : fileType;
		ext = ext.startsWith(".") || ext.isEmpty() ? ext : ".".concat(ext);

		for (String frameName : frameNames) {
			StringBuilder fullPathBuilder = new StringBuilder(basePath);
			fullPathBuilder.append(frameName).append(ext);
			String fullPath = fullPathBuilder.toString();

			try {
				Image image = ImageUtil.getImageFromFile(fullPath);
				if (image != null) {
					frames.add(image);
				} else {
					Logger.log(LogLevel.WARN, "Failed to load image: '" + fullPath + "'");
				}
			} catch (IOException e) {
				Logger.log(LogLevel.ERROR, "Error loading image: '" + fullPath + "'", e);
				throw e;
			}
		}
		return frames;
	}

	private void restartFrameUpdater() {
		this.stop();
		frameScheduler.scheduleAtFixedRate(this::updateFrame, 0, 1000 / fps, TimeUnit.MILLISECONDS);
	}

	private void updateFrame() {
		List<Image> frames = animations.get(selectedAnimationName);
		if (frames != null && !frames.isEmpty()) {
			frameIndex = (frameIndex + 1) % frames.size();
		}
	}

	private Image getCurrentFrameImage() {
		if (selectedAnimationName != null && animations.containsKey(selectedAnimationName)) {
			List<Image> frames = animations.get(selectedAnimationName);
			if (frames != null && !frames.isEmpty() && frameIndex < frames.size()) {
				return frames.get(frameIndex);
			}
		}
		return null;
	}

	private String getAnimationNames() {
		return String.join(", ", animations.keySet());
	}
}
