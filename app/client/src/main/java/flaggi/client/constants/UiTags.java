/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 7/25/2024
 * GitHub link: https://github.com/my-daarlin/flaggi
 */

package flaggi.client.constants;

public class UiTags {

	// Private constructor to prevent instantiation
	private UiTags() {
		throw new UnsupportedOperationException("UiIndex is a constants class and cannot be instantiated.");
	}

	// Constants ----------------------------------------------------------------

	public static final String DEBUG = "debug";
	public static final String ALWAYS_VISIBLE = "always";

	public static final String MAIN_MENU = "menu";
	public static final String GAME_ELEMENTS = "game";
	public static final String ENEMY_PLAYER = "enemy";
	public static final String PAUSE_MENU = "pause";
	public static final String LOBBY = "lobby";
	public static final String GAME = "game";

	public static final String ENVIRONMENT = "environment";
	public static final String PROJECTILES = "projectiles";

	public static final String TOASTS = "toasts";

}
