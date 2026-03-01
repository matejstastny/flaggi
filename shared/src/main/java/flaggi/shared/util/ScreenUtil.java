// ------------------------------------------------------------------------------
// ScreenUtil.java - Screen utility class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 11-08-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.util;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * Utility class for getting screen-related information such as dimensions.
 * Supports single and multi-monitor setups.
 */
public class ScreenUtil {

	// Private constructor to prevent instantiation
	private ScreenUtil() {
		throw new UnsupportedOperationException("ScreenUtil is a utility class and cannot be instantiated.");
	}

	// Single-screen -------------------------------------------------------------

	/**
	 * Gets the screen dimensions of the user's primary screen using
	 * {@code Toolkit}.
	 *
	 * @return a {@code Position} object containing the screen's width and height.
	 */
	public static int[] getScreenDimensions() {
		// Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		// return new int[] { screenSize.width, screenSize.height };
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		return new int[] { gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight() };
	}

	/**
	 * Gets the center position of the user's primary screen.
	 *
	 * @return a {@code Position} object representing the center of the screen.
	 */
	public static int[] getScreenCenter() {
		int[] dimensions = getScreenDimensions();
		return new int[] { dimensions[0] / 2, dimensions[1] / 2 };
	}

	// Multi-screen --------------------------------------------------------------

	/**
	 * Returns a list of dimensions for all connected screens. Supports
	 * multi-monitor setups.
	 *
	 * @return an array of {@code Position} objects for each screen's dimensions.
	 */
	public static int[][] getAllScreensDimensions() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screenDevices = ge.getScreenDevices();
		int[][] screens = new int[screenDevices.length][2];

		for (int i = 0; i < screenDevices.length; i++) {
			Dimension screenSize = screenDevices[i].getDefaultConfiguration().getBounds().getSize();
			screens[i][0] = screenSize.width;
			screens[i][1] = screenSize.height;
		}
		return screens;
	}

}
