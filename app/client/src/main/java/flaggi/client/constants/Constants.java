/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/25/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.constants;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import flaggi.shared.common.ConfigManager;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;
import flaggi.shared.util.ImageUtil;

public class Constants {

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Constants is a constants class and cannot be instantiated.");
    }

    // Paths -------------------------------------------------------------------

    public static final String APP_DATA_DIR_NAME = "kireiiiiiiii.flaggi.client";
    public static final String LOG_FILE = String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "logs", "latest.txt");
    public static final ConfigManager CONFIG = new ConfigManager(String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "configs", "config.properties"), "/configs/config.properties");

    public static final String ICONS_RES_DIR = "icons";
    public static final String SPRITES_RES_DIR = "sprites";
    public static final String FONTS_RES_DIR = "fonts";
    public static final String UI_RES_DIR = "ui";

    // Network ------------------------------------------------------------------

    public static final int TCP_PORT = CONFIG.getIntValue("tcp.port");

    // Window -------------------------------------------------------------------

    public static String WINDOW_NAME = "Flaggi";
    public static boolean WINDOW_RESIZABLE = true;
    public static int[] BASE_WINDOW_SIZE = { 1200, 600 };
    public static int FRAMERATE = 120;

    // Textures -----------------------------------------------------------------

    public static final Image ICON_MAC = getImage(ICONS_RES_DIR + "/icon_mac.png");
    public static final Image ICON_WIN = getImage(ICONS_RES_DIR + "/icon_win.png");

    // Debug --------------------------------------------------------------------

    public static boolean HITBOXES_ENABLED = false;
    public static boolean LOG_MEM_USAGE = true;
    public static int MEM_LOG_INTERVAL_SEC = 3;

    // Game ---------------------------------------------------------------------

    public static final String MENU_NAME_FIELD = CONFIG.getStringValue("username");
    public static final String MENU_IP_FIELD = CONFIG.getStringValue("server.ip");
    public static final int LOBBY_UPDATE_FETCH_INTERVAL_SEC = 3;

    // Internal -----------------------------------------------------------------

    private static Image getImage(String path) {
        Image img = null;
        try {
            img = ImageUtil.getImageFromFile(path);
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, "Texture not found: '" + path + "'!");
        }
        return img;
    }

}
