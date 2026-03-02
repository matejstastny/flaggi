// ------------------------------------------------------------------------------
// Constants.java - Constants class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 11-04-2024 (2.0: 02-25-2025) (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.constants;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import flaggi.client.App;
import flaggi.shared.common.ConfigManager;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;
import flaggi.shared.util.FontUtil;
import flaggi.shared.util.ImageUtil;

public class Constants {

	// Private constructor to prevent instantiation
	private Constants() {
		throw new UnsupportedOperationException("Constants is a constants class and cannot be instantiated.");
	}

	// Paths -------------------------------------------------------------------

	public static final String APP_DATA_DIR_NAME = "flaggi.client";
	public static final String LOG_FILE = String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "logs", "latest.log");
	public static final ConfigManager CONFIG = getConfigManager();

	public static final String ICONS_RES_DIR = "icons";
	public static final String SPRITES_RES_DIR = "sprites";
	public static final String FONTS_RES_DIR = "fonts";
	public static final String UI_RES_DIR = "ui";

	// Network ------------------------------------------------------------------

	public static final int TCP_PORT = CONFIG.getIntValue("tcp.port");
	public static final int SERVER_TIMEOUT_MS = 5000;

	// Window -------------------------------------------------------------------

	public static String WINDOW_NAME = "Flaggi";
	public static boolean WINDOW_RESIZABLE = true;
	public static int[] BASE_WINDOW_SIZE = { 1200, 600 };
	public static int FRAMERATE = 120;
	public static final long TOAST_DISPLAY_DURATION_SEC = 5;

	// Textures -----------------------------------------------------------------

	public static final Image ICON_MAC = getImage(ICONS_RES_DIR + "/icon_mac.png");
	public static final Image ICON_WIN = getImage(ICONS_RES_DIR + "/icon_win.png");

	// Fonts -----------------------------------------------------------------

	public static final Font FONT = getFont(FONTS_RES_DIR + "/PixelifySans-VariableFont_wght.ttf");

	// Debug --------------------------------------------------------------------

	public static LogLevel[] IGNORED_LOG_LEVES = { LogLevel.TCP, LogLevel.UDP };
	public static boolean LOG_MEM_USAGE = false;
	public static int MEM_LOG_INTERVAL_SEC = 3;
	public static boolean HITBOXES_ENABLED = false;

	// Game ---------------------------------------------------------------------

	public static final int UPDATE_INTERVAL_MS = 16;
	public static final String MENU_NAME_FIELD = CONFIG.getStringValue("username");
	public static final String MENU_IP_FIELD = CONFIG.getStringValue("server.ip");
	public static final int LOBBY_UPDATE_FETCH_INTERVAL_SEC = 3;

	// Internal -----------------------------------------------------------------

	private static ConfigManager getConfigManager() {
		try {
			return new ConfigManager(String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "config.properties"), "/configs/config.properties");
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "An IOException occured when initializing the ConfigManager", e);
			App.handleFatalError();
			return null;
		}
	}

	private static Image getImage(String path) {
		Image img = null;
		try {
			img = ImageUtil.getImageFromResource(path);
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "Texture not found: '" + path + "'!");
		}
		return img;
	}

	private static Font getFont(String path) {
		Font font = Font.getFont("Arial");
		try {
			font = FontUtil.createFontFromResource(path);
		} catch (IOException e) {
			Logger.log(LogLevel.ERR, "Font not found: '" + path + "'!");
		} catch (FontFormatException e) {
			Logger.log(LogLevel.ERR, "Font format exception for: '" + path + "'!");
		}
		return font;
	}

}
