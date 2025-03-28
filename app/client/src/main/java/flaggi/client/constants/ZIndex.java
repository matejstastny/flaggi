/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 7/25/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.constants;

public class ZIndex {

	// Private constructor to prevent instantiation
	private ZIndex() {
		throw new UnsupportedOperationException("ZIndex is a constants class and cannot be instantiated.");
	}

	// Debug --------------------------------------------------------------------

	public static final int SCREEN_TEST = 100;

	// Global -------------------------------------------------------------------

	public static final int BACKGROUND = 1;
	public static final int CONNECTION = 3;

	// Menu ---------------------------------------------------------------------

	public static final int MENU_SCREEN = 2;
	public static final int TOAST = 30;
	public static final int PAUSE_SCREEN = 20;

	// Game ---------------------------------------------------------------------

	public static final int FLOOR = 2;
	public static final int PLAYER = 11;
	public static final int OTHER_PLAYERS = 10;
	public static final int ENVIRONMENT_TOP = 12;
	public static final int ENVIRONMENT_BOTTOM = 9;
	public static final int HUD = 18;

}
